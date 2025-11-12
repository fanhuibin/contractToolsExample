package com.zhaoxinms.contract.tools.ocr.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.extract.model.TextBox;
import com.zhaoxinms.contract.tools.extract.util.PositionMapper;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.ocr.service.OcrExtractService;
import com.zhaoxinms.contract.tools.ocr.service.UnifiedOCRService;
import com.zhaoxinms.contract.tools.common.ocr.OCRProvider;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OCR提取服务实现
 */
@Slf4j
@Service
public class OcrExtractServiceImpl implements OcrExtractService {

    @Autowired
    private UnifiedOCRService unifiedOCRService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    // 任务状态缓存
    private final Map<String, Map<String, Object>> taskStatusCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化，确保上传根目录存在
     */
    @javax.annotation.PostConstruct
    public void init() {
        // 处理相对路径，转换为绝对路径
        File uploadDir = new File(uploadRootPath);
        
        // 如果是相对路径，转换为项目根目录下的绝对路径
        if (!uploadDir.isAbsolute()) {
            // 获取用户工作目录（通常是项目根目录）
            String userDir = System.getProperty("user.dir");
            uploadDir = new File(userDir, uploadRootPath);
            // 更新为绝对路径
            uploadRootPath = uploadDir.getAbsolutePath();
            log.info("相对路径转换为绝对路径: {}", uploadRootPath);
        }
        
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            log.info("创建上传根目录: {}, 结果: {}", uploadDir.getAbsolutePath(), created);
        }
        
        File ocrExtractDir = new File(uploadRootPath, "ocr-extract-tasks");
        if (!ocrExtractDir.exists()) {
            boolean created = ocrExtractDir.mkdirs();
            log.info("创建OCR提取任务目录: {}, 结果: {}", ocrExtractDir.getAbsolutePath(), created);
        }
        
        log.info("OCR提取服务初始化完成，上传根目录: {}", uploadDir.getAbsolutePath());
    }

    @Override
    public String extractPdf(MultipartFile file, Boolean ignoreHeaderFooter,
                            Double headerHeightPercent, Double footerHeightPercent) throws Exception {
        
        String taskId = UUID.randomUUID().toString();
        log.info("开始OCR提取任务，任务ID: {}, 文件: {}", taskId, file.getOriginalFilename());

        // 创建任务目录（使用年月路径）
        File taskDir = getTaskDir(taskId);
        log.info("任务目录路径: {}", taskDir.getAbsolutePath());
        
        if (!taskDir.exists()) {
            boolean created = taskDir.mkdirs();
            if (!created) {
                log.error("创建任务目录失败: {}", taskDir.getAbsolutePath());
                throw new RuntimeException("创建任务目录失败: " + taskDir.getAbsolutePath());
            }
            log.info("任务目录创建成功: {}", taskDir.getAbsolutePath());
        }

        // 保存上传的文件
        File pdfFile = new File(taskDir, "source.pdf");
        log.info("准备保存PDF文件到: {}", pdfFile.getAbsolutePath());
        file.transferTo(pdfFile);
        log.info("PDF文件保存成功，大小: {} bytes", pdfFile.length());

        // 初始化任务状态
        Map<String, Object> status = new HashMap<>();
        status.put("taskId", taskId);
        status.put("fileName", file.getOriginalFilename());
        status.put("status", "processing");
        status.put("progress", 10);
        status.put("message", "开始OCR识别...");
        status.put("startTime", System.currentTimeMillis());
        taskStatusCache.put(taskId, status);

        // 异步执行OCR提取
        new Thread(() -> {
            try {
                performOcrExtraction(taskId, pdfFile, ignoreHeaderFooter, 
                                   headerHeightPercent, footerHeightPercent);
            } catch (Exception e) {
                log.error("OCR提取任务失败，任务ID: {}", taskId, e);
                updateTaskStatus(taskId, "failed", 0, "OCR提取失败: " + e.getMessage());
            }
        }).start();

        return taskId;
    }

    /**
     * 执行OCR提取
     */
    private void performOcrExtraction(String taskId, File pdfFile, Boolean ignoreHeaderFooter,
                                     Double headerHeightPercent, Double footerHeightPercent) throws Exception {
        
        File taskDir = getTaskDir(taskId);

        // 更新状态：OCR识别中
        updateTaskStatus(taskId, "processing", 30, "正在进行OCR识别...");

        // 调用统一OCR服务（传入taskId和输出目录，确保中间文件保存到正确位置）
        OCRProvider.OCRResult ocrResult = unifiedOCRService.recognizePdf(
                pdfFile, taskId, taskDir, ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);

        if (ocrResult == null) {
            throw new RuntimeException("OCR识别失败");
        }

        // 更新状态：处理结果
        updateTaskStatus(taskId, "processing", 60, "处理OCR结果...");

        // 保存OCR文本结果
        String ocrText = ocrResult.getContent();
        File ocrTextFile = new File(taskDir, "ocr_text.txt");
        Files.writeString(ocrTextFile.toPath(), ocrText);

        // 获取元数据
        Object metadataObj = ocrResult.getMetadata();
        JSONObject metadata = null;
        List<TextBox> textBoxes = new ArrayList<>();
        int totalPages = 1;
        String imagesDir = "";

        if (metadataObj instanceof JSONObject) {
            metadata = (JSONObject) metadataObj;
            
            // 获取总页数
            totalPages = metadata.getInteger("totalPages");
            
            // 获取图片目录
            imagesDir = metadata.getString("imagesDir");
            log.info("从OCR结果获取图片目录: {}", imagesDir);
            
            // 解析TextBox数据
            String textBoxesJson = metadata.getString("textBoxes");
            if (textBoxesJson != null && !textBoxesJson.isEmpty()) {
                JSONArray textBoxesArray = JSON.parseArray(textBoxesJson);
                for (int i = 0; i < textBoxesArray.size(); i++) {
                    JSONObject textBoxJson = textBoxesArray.getJSONObject(i);
                    int page = textBoxJson.getIntValue("page");
                    String text = textBoxJson.getString("text");
                    JSONArray bboxArray = textBoxJson.getJSONArray("bbox");
                    double[] bbox = new double[4];
                    for (int j = 0; j < 4 && j < bboxArray.size(); j++) {
                        bbox[j] = bboxArray.getDoubleValue(j);
                    }
                    String category = textBoxJson.getString("category");
                    int startPos = textBoxJson.getIntValue("startPos");
                    int endPos = textBoxJson.getIntValue("endPos");
                    
                    textBoxes.add(new TextBox(page, text, bbox, category, startPos, endPos));
                }
            }
        }

        // 更新状态：保存数据
        updateTaskStatus(taskId, "processing", 80, "保存提取数据...");

        // 保存TextBox数据
        File textBoxFile = new File(taskDir, "text_boxes.json");
        objectMapper.writeValue(textBoxFile, textBoxes);
        
        // 详细输出跨页表格信息（用于调试）
        log.info("========== TextBox详细信息（用于调试bbox重复标记问题） ==========");
        for (int i = 0; i < Math.min(textBoxes.size(), 10); i++) {
            TextBox tb = textBoxes.get(i);
            log.info("[{}] 页:{}, 索引:{}-{}, bbox:[{},{},{},{}], 文本:\"{}\"",
                i, tb.page, tb.startPos, tb.endPos,
                (int)tb.bbox[0], (int)tb.bbox[1], (int)tb.bbox[2], (int)tb.bbox[3],
                tb.text.length() > 30 ? tb.text.substring(0, 30) + "..." : tb.text);
        }
        log.info("========== TextBox详细信息结束（共{}个） ==========", textBoxes.size());

        // 获取跨页表格信息（从metadata中）
        JSONArray crossPageTablesArray = null;
        try {
            if (metadata != null) {
                crossPageTablesArray = metadata.getJSONArray("crossPageTables");
                if (crossPageTablesArray != null && !crossPageTablesArray.isEmpty()) {
                    log.info("✅ 成功获取跨页表格信息，跨页表格数: {}", crossPageTablesArray.size());
                    // 输出跨页表格详细信息
                    for (int i = 0; i < crossPageTablesArray.size(); i++) {
                        JSONObject group = crossPageTablesArray.getJSONObject(i);
                        String groupId = group.getString("groupId");
                        JSONObject mainTable = group.getJSONObject("mainTable");
                        JSONArray contParts = group.getJSONArray("continuationParts");
                        log.info("  跨页表格[{}]: groupId={}, 主表页={}, 延续部分数={}", 
                            i, groupId, 
                            mainTable != null ? mainTable.getIntValue("page") : "null",
                            contParts != null ? contParts.size() : 0);
                    }
                } else {
                    log.warn("⚠️  未检测到跨页表格信息（metadata中没有crossPageTables或为空）");
                }
            } else {
                log.warn("⚠️  metadata为null，无法获取跨页表格信息");
            }
        } catch (Exception e) {
            log.error("获取跨页表格信息失败: {}", e.getMessage(), e);
        }
         
        // 创建bbox映射（用于处理跨页表格等复杂情况）
        updateTaskStatus(taskId, "processing", 85, "创建位置映射...");
        List<PositionMapper.BboxMapping> bboxMappings = createBboxMappings(textBoxes, ocrText, crossPageTablesArray);
        
        if (bboxMappings != null && !bboxMappings.isEmpty()) {
            File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
            objectMapper.writeValue(bboxMappingFile, bboxMappings);
            log.info("位置映射完成，任务: {}, 映射了 {} 个文本块", taskId, bboxMappings.size());
            
            // 输出BboxMapping详细信息（前10个）
            log.info("========== BboxMapping详细信息（前10个） ==========");
            for (int i = 0; i < Math.min(bboxMappings.size(), 10); i++) {
                var mapping = bboxMappings.get(i);
                var interval = mapping.getInterval();
                var text = mapping.getText();
                var bboxes = mapping.getBboxes();
                var pages = mapping.getPages();
                
                log.info("[{}] 索引:{}-{}, bbox数:{}, 页码:{}, 文本:\"{}\"",
                    i, interval.getStartPos(), interval.getEndPos(), 
                    bboxes != null ? bboxes.size() : 0,
                    pages,
                    text != null && text.length() > 30 ? text.substring(0, 30) + "..." : text);
                    
                // 输出每个bbox的详细信息
                if (bboxes != null && !bboxes.isEmpty()) {
                    for (int j = 0; j < Math.min(bboxes.size(), 3); j++) {
                        var bbox = bboxes.get(j);
                        log.info("    bbox[{}]: 页{}, [{},{},{},{}]",
                            j, bbox.getPage(),
                            (int)bbox.getBbox()[0], (int)bbox.getBbox()[1],
                            (int)bbox.getBbox()[2], (int)bbox.getBbox()[3]);
                    }
                }
            }
            log.info("========== BboxMapping详细信息结束 ==========");
        }

        // 保存元数据
        Map<String, Object> resultMetadata = new HashMap<>();
        resultMetadata.put("totalPages", totalPages);
        resultMetadata.put("imagesDir", imagesDir);
        resultMetadata.put("textLength", ocrText.length());
        resultMetadata.put("textBoxCount", textBoxes.size());
        // OCR 引擎信息由后端统一管理，不在此处硬编码
        // 前端如需显示可以从系统配置或统一接口获取
        resultMetadata.put("hasPositionInfo", bboxMappings != null && !bboxMappings.isEmpty());
        
        // 页面维度信息
        if (metadata != null && metadata.containsKey("pageDimensions")) {
            resultMetadata.put("pageDimensions", metadata.get("pageDimensions"));
        }
        
        File metadataFile = new File(taskDir, "metadata.json");
        objectMapper.writeValue(metadataFile, resultMetadata);

        // 复制图片到任务目录（如果需要）
        if (imagesDir != null && !imagesDir.isEmpty()) {
            File sourceImagesDir = new File(imagesDir);
            File targetImagesDir = new File(taskDir, "images");
            
            log.info("准备复制图片，源目录: {}, 目标目录: {}", sourceImagesDir.getAbsolutePath(), targetImagesDir.getAbsolutePath());
            
            if (sourceImagesDir.exists() && sourceImagesDir.isDirectory()) {
                copyDirectory(sourceImagesDir, targetImagesDir);
                log.info("图片复制完成，目标目录: {}", targetImagesDir.getAbsolutePath());
                
                // 列出复制后的文件
                File[] copiedFiles = targetImagesDir.listFiles();
                if (copiedFiles != null) {
                    log.info("复制的图片文件数量: {}", copiedFiles.length);
                    for (File f : copiedFiles) {
                        log.info("  - {}", f.getName());
                    }
                }
            } else {
                log.warn("源图片目录不存在或不是目录: {}", sourceImagesDir.getAbsolutePath());
            }
        }

        // 更新状态：完成
        updateTaskStatus(taskId, "completed", 100, "OCR提取完成");

        log.info("OCR提取任务完成，任务ID: {}, 总页数: {}, 文本长度: {}, TextBox数: {}", 
                taskId, totalPages, ocrText.length(), textBoxes.size());
    }
 
    /**
     * 创建Bbox映射（直接从TextBox转换，支持跨页表格）
     * 每个TextBox对应一个BboxMapping，对于跨页表格，会合并多个页的bbox
     */
    private List<PositionMapper.BboxMapping> createBboxMappings(List<TextBox> textBoxes, String fullText, 
                                                                 JSONArray crossPageTablesArray) {
        if (textBoxes == null || textBoxes.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // 第1步：为每个TextBox创建基本的BboxMapping（一对一）
            List<PositionMapper.BboxMapping> mappings = new ArrayList<>();
            Map<String, Integer> textBoxKeyToMappingIndex = new HashMap<>();  // 用于后续查找
            
            for (int i = 0; i < textBoxes.size(); i++) {
                TextBox textBox = textBoxes.get(i);
                if (textBox.text == null || textBox.text.isEmpty()) {
                    continue;
                }
                
                // 创建CharInterval（文本区间）
                CharInterval interval = new CharInterval();
                interval.setStartPos(textBox.startPos);
                interval.setEndPos(textBox.endPos);
                interval.setSourceText(textBox.text);
                interval.setAlignmentConfidence(1.0);
                
                // 创建BboxInfo（使用构造函数）
                PositionMapper.BboxInfo bboxInfo = new PositionMapper.BboxInfo(
                    textBox.page,
                    textBox.bbox,
                    textBox.category != null ? textBox.category : "Text",
                    ' '  // 占位符，TextBox级别不关心具体字符
                );
                
                // 创建BboxMapping（使用构造函数）
                // 注意：这里先创建单bbox的mapping，稍后会为跨页表格补充bbox
                List<PositionMapper.BboxInfo> bboxInfos = new ArrayList<>();
                bboxInfos.add(bboxInfo);
                
                List<Integer> pages = new ArrayList<>();
                pages.add(textBox.page);
                
                PositionMapper.BboxMapping mapping = new PositionMapper.BboxMapping(
                    interval,
                    textBox.text,
                    bboxInfos,
                    pages
                );
                
                mappings.add(mapping);
                
                // 记录bbox坐标作为key（用于后续匹配跨页表格）
                String bboxKey = createBboxKey(textBox.page, textBox.bbox);
                textBoxKeyToMappingIndex.put(bboxKey, mappings.size() - 1);
            }
            
            log.info("✅ Bbox映射创建完成（直接映射），TextBox数: {}, BboxMapping数: {}", 
                textBoxes.size(), mappings.size());
            
            // 第2步：处理跨页表格，为主表格的BboxMapping添加延续部分的bbox
            if (crossPageTablesArray != null && !crossPageTablesArray.isEmpty()) {
                log.info("🔗 开始处理跨页表格，表格组数: {}", crossPageTablesArray.size());
                
                int crossPageTableCount = 0;
                int addedBboxCount = 0;
                
                for (int i = 0; i < crossPageTablesArray.size(); i++) {
                    JSONObject group = crossPageTablesArray.getJSONObject(i);
                    if (group == null) {
                        continue;
                    }
                    
                    String groupId = group.getString("groupId");
                    JSONObject mainTableJson = group.getJSONObject("mainTable");
                    JSONArray contPartsArray = group.getJSONArray("continuationParts");
                    
                    if (mainTableJson == null || contPartsArray == null || contPartsArray.isEmpty()) {
                        log.warn("⚠️  表格组 {} 数据不完整，跳过", groupId);
                        continue;
                    }
                    
                    crossPageTableCount++;
                    
                    // 解析主表格信息
                    int mainTablePage = mainTableJson.getIntValue("page");
                    JSONArray mainTableBboxArray = mainTableJson.getJSONArray("bbox");
                    double[] mainTableBbox = new double[]{
                        mainTableBboxArray.getDoubleValue(0),
                        mainTableBboxArray.getDoubleValue(1),
                        mainTableBboxArray.getDoubleValue(2),
                        mainTableBboxArray.getDoubleValue(3)
                    };
                    
                    // 找到主表格对应的BboxMapping
                    String mainTableKey = createBboxKey(mainTablePage, mainTableBbox);
                    Integer mappingIndex = textBoxKeyToMappingIndex.get(mainTableKey);
                    
                    if (mappingIndex == null) {
                        log.warn("⚠️  未找到主表格对应的BboxMapping，组ID: {}, 页: {}, key: {}", 
                            groupId, mainTablePage, mainTableKey);
                        continue;
                    }
                    
                    PositionMapper.BboxMapping mainMapping = mappings.get(mappingIndex);
                    
                    // 为主表格的BboxMapping添加延续部分的bbox
                    for (int j = 0; j < contPartsArray.size(); j++) {
                        JSONObject contPartJson = contPartsArray.getJSONObject(j);
                        if (contPartJson == null) {
                            continue;
                        }
                        
                        int contPage = contPartJson.getIntValue("page");
                        JSONArray contBboxArray = contPartJson.getJSONArray("bbox");
                        double[] contBbox = new double[]{
                            contBboxArray.getDoubleValue(0),
                            contBboxArray.getDoubleValue(1),
                            contBboxArray.getDoubleValue(2),
                            contBboxArray.getDoubleValue(3)
                        };
                        
                        PositionMapper.BboxInfo contBboxInfo = new PositionMapper.BboxInfo(
                            contPage,
                            contBbox,
                            "Table",
                            ' '
                        );
                        
                        mainMapping.getBboxes().add(contBboxInfo);
                        
                        if (!mainMapping.getPages().contains(contPage)) {
                            mainMapping.getPages().add(contPage);
                        }
                        
                        addedBboxCount++;
                        
                        log.info("  ✅ 为表格组 {} 添加跨页bbox: 页{}, bbox=[{},{},{},{}]",
                            groupId, contPage,
                            (int)contBbox[0], (int)contBbox[1],
                            (int)contBbox[2], (int)contBbox[3]);
                    }
                }
                
                log.info("🔗 跨页表格处理完成，处理了 {} 个跨页表格，添加了 {} 个延续bbox", 
                    crossPageTableCount, addedBboxCount);
            }
            
            return mappings;
            
        } catch (Exception e) {
            log.error("创建Bbox映射失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 创建bbox的唯一键（用于匹配TextBox和TablePart）
     */
    private String createBboxKey(int page, double[] bbox) {
        if (bbox == null || bbox.length < 4) {
            return "";
        }
        // 使用页码和bbox坐标创建唯一键（四舍五入到整数，避免浮点误差）
        return String.format("%d_%.0f_%.0f_%.0f_%.0f", 
            page, bbox[0], bbox[1], bbox[2], bbox[3]);
    }

    /**
     * 复制目录
     */
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(targetDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    Files.copy(file.toPath(), targetFile.toPath(), 
                              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, int progress, String message) {
        Map<String, Object> statusMap = taskStatusCache.get(taskId);
        if (statusMap == null) {
            statusMap = new HashMap<>();
            taskStatusCache.put(taskId, statusMap);
        }
        
        statusMap.put("status", status);
        statusMap.put("progress", progress);
        statusMap.put("message", message);
        statusMap.put("updateTime", System.currentTimeMillis());
        
        if ("completed".equals(status) || "failed".equals(status)) {
            Long startTime = (Long) statusMap.get("startTime");
            if (startTime != null) {
                long duration = (System.currentTimeMillis() - startTime) / 1000;
                statusMap.put("durationSeconds", duration);
            }
        }
    }

    @Override
    public Map<String, Object> getTaskStatus(String taskId) {
        return taskStatusCache.get(taskId);
    }

    @Override
    public Map<String, Object> getTaskResult(String taskId) throws Exception {
        File taskDir = getTaskDir(taskId);
        if (!taskDir.exists()) {
            throw new RuntimeException("任务不存在");
        }

        Map<String, Object> result = new HashMap<>();

        // 加载OCR文本
        File ocrTextFile = new File(taskDir, "ocr_text.txt");
        if (ocrTextFile.exists()) {
            String ocrText = Files.readString(ocrTextFile.toPath());
            result.put("ocrText", ocrText);
        }

        // 加载元数据
        File metadataFile = new File(taskDir, "metadata.json");
        if (metadataFile.exists()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(metadataFile, Map.class);
            result.put("metadata", metadata);
            result.put("totalPages", metadata.get("totalPages"));
            result.put("textLength", metadata.get("textLength"));
            result.put("charBoxCount", metadata.get("charBoxCount"));
            result.put("provider", metadata.get("provider"));
            result.put("pageDimensions", metadata.get("pageDimensions"));
        }

        // 加载TextBox数据（可选，如果文件较大可以通过单独接口获取）
        File textBoxFile = new File(taskDir, "text_boxes.json");
        if (textBoxFile.exists()) {
            long fileSize = textBoxFile.length();
            // 如果文件小于1MB，直接加载
            if (fileSize < 1024 * 1024) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> textBoxes = objectMapper.readValue(textBoxFile, List.class);
                result.put("textBoxes", textBoxes);
            } else {
                // 大文件只返回路径，前端通过单独接口获取
                result.put("textBoxesAvailable", true);
                result.put("textBoxesSize", fileSize);
            }
        }

        return result;
    }

    @Override
    public File getPageImage(String taskId, int pageNum) {
        File taskDir = getTaskDir(taskId);
        File imagesDir = new File(taskDir, "images");
        
        if (!imagesDir.exists()) {
            return null;
        }

        // 尝试多种可能的图片文件名格式
        String[] possibleNames = {
            "page-" + pageNum + ".png",
            "page-" + pageNum + ".jpg",
            "page_" + pageNum + ".png",
            "page_" + pageNum + ".jpg",
            String.format("%03d.png", pageNum),
            String.format("%03d.jpg", pageNum)
        };

        for (String name : possibleNames) {
            File imageFile = new File(imagesDir, name);
            if (imageFile.exists()) {
                return imageFile;
            }
        }

        return null;
    }

    @Override
    public Object getTextBoxes(String taskId) throws Exception {
        File taskDir = getTaskDir(taskId);
        File textBoxFile = new File(taskDir, "text_boxes.json");
        
        if (!textBoxFile.exists()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> textBoxes = objectMapper.readValue(textBoxFile, List.class);
        return textBoxes;
    }

    @Override
    public void deleteTask(String taskId) {
        // 从缓存中删除
        taskStatusCache.remove(taskId);

        // 删除任务目录
        File taskDir = getTaskDir(taskId);
        if (taskDir.exists()) {
            try {
                deleteDirectory(taskDir);
                log.info("删除OCR提取任务，任务ID: {}", taskId);
            } catch (IOException e) {
                log.error("删除任务目录失败，任务ID: {}", taskId, e);
            }
        }
    }

    /**
     * 获取任务目录（使用年月路径）
     */
    private File getTaskDir(String taskId) {
        String yearMonthPath = FileStorageUtils.getYearMonthPathFromFileId(taskId);
        return new File(uploadRootPath, "ocr-extract-tasks/" + yearMonthPath + "/" + taskId);
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("无法删除: " + directory.getAbsolutePath());
        }
    }
}

