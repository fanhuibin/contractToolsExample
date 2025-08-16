package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.zhaoxinms.contract.tools.ocrcompare.service.OCRTaskService;
import com.zhaoxinms.contract.tools.ocrcompare.model.OCRTask;

import java.io.IOException;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

// 新增导入
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.compare.DiffUtil.Operation;
import com.zhaoxinms.contract.tools.compare.result.CompareResult;
import com.zhaoxinms.contract.tools.compare.result.Position;
import cn.hutool.core.util.StrUtil;

/**
 * OCR比对服务
 */
@Service
public class OCRCompareService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRCompareService.class);
    
    @Autowired
    private OCRTaskService ocrTaskService;
    
    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;
    
    // 任务存储（实际项目中应该使用数据库）
    private final ConcurrentHashMap<String, OCRCompareTask> tasks = new ConcurrentHashMap<>();
    
    // 结果存储（实际项目中应该使用数据库）
    private final ConcurrentHashMap<String, OCRCompareResult> results = new ConcurrentHashMap<>();
    
    /**
     * 提交比对任务（使用MultipartFile）
     */
    public String submitCompareTask(MultipartFile oldFile, MultipartFile newFile, 
                                   OCRCompareOptions options) throws IOException {
        String oldFileName = oldFile.getOriginalFilename();
        String newFileName = newFile.getOriginalFilename();
        
        if (oldFileName == null || newFileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 生成任务ID
        String taskId = generateTaskId();
        
        // 创建上传目录
        createDirectories();
        
        // 保存文件到临时目录
        String oldFilePath = saveFile(oldFile, taskId + "_old");
        String newFilePath = saveFile(newFile, taskId + "_new");
        
        // 创建比对任务
        OCRCompareTask task = new OCRCompareTask(
            taskId,
            oldFileName,
            newFileName,
            oldFilePath,
            newFilePath,
            options
        );
        
        tasks.put(taskId, task);
        
        // 异步执行比对任务
        CompletableFuture.runAsync(() -> executeCompareTask(task));
        
        return taskId;
    }
    
    /**
     * 提交比对任务（使用文件路径）
     */
    public String submitCompareTaskWithPaths(String oldFilePath, String newFilePath, 
                                           OCRCompareOptions options) {
        // 生成任务ID
        String taskId = generateTaskId();
        
        // 创建比对任务，直接使用传入的文件路径
        OCRCompareTask task = new OCRCompareTask(
            taskId,
            new File(oldFilePath).getName(),
            new File(newFilePath).getName(),
            oldFilePath,
            newFilePath,
            options
        );
        
        tasks.put(taskId, task);
        
        // 异步执行比对任务
        CompletableFuture.runAsync(() -> executeCompareTask(task));
        
        return taskId;
    }
    
    /**
     * 查询任务状态
     */
    public OCRCompareTask getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<OCRCompareTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * 获取比对结果
     */
    public OCRCompareResult getCompareResult(String taskId) {
        return results.get(taskId);
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        OCRCompareTask task = tasks.remove(taskId);
        if (task != null) {
            results.remove(taskId);
            // 清理文件
            cleanupTaskFiles(task);
            return true;
        }
        return false;
    }
    
    /**
     * 执行OCR比对任务
     */
    private void executeCompareTask(OCRCompareTask task) {
        try {
            task.setStatus(OCRCompareTask.TaskStatus.OCR_PROCESSING);
            task.setStartTime(LocalDateTime.now());
            task.setCurrentStep(1, "开始OCR识别");
            
            // 步骤1: 检查文件是否存在
            task.updateProgress(5.0, "检查文档格式...");
            String oldFilePath = task.getOldFilePath();
            String newFilePath = task.getNewFilePath();
            
            // 检查文件是否存在
            if (!new File(oldFilePath).exists() || !new File(newFilePath).exists()) {
                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                task.setErrorMessage("源文件不存在");
                return;
            }
            
            // 步骤2: OCR识别旧文档
            task.setCurrentStep(2, "OCR识别旧文档");
            task.updateProgress(20.0, "OCR识别旧文档...");
            String oldOcrTaskId = ocrTaskService.submitOCRTask(oldFilePath);
            task.setOldOcrTaskId(oldOcrTaskId);
            
            // 步骤3: OCR识别新文档
            task.setCurrentStep(3, "OCR识别新文档");
            task.updateProgress(35.0, "OCR识别新文档...");
            String newOcrTaskId = ocrTaskService.submitOCRTask(newFilePath);
            task.setNewOcrTaskId(newOcrTaskId);
            
            // 等待OCR任务完成并监控进度
            waitForOCRCompletion(task);
            
            if (task.getStatus() == OCRCompareTask.TaskStatus.FAILED) {
                return;
            }
            
            // 步骤4: 执行文本比对
            task.setCurrentStep(4, "执行文本比对");
            task.updateProgress(70.0, "执行文本比对...");
            
            // 获取OCR结果
            String oldText = getOCRResultText(task.getOldOcrTaskId());
            String newText = getOCRResultText(task.getNewOcrTaskId());
            
            if (oldText == null || newText == null) {
                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                task.setErrorMessage("OCR识别结果为空");
                return;
            }
            
            // 执行真正的文本比对
            List<CompareResult> compareResults = performTextComparison(oldText, newText, task.getOptions());
            
            // 解析OCR结果中的位置信息（结合PDF页尺寸进行坐标换算）
            enrichCompareResultsWithPositionInfo(
                compareResults,
                task.getOldOcrTaskId(),
                task.getNewOcrTaskId(),
                oldFilePath,
                newFilePath
            );
            
            // 步骤5: 将比对结果回写到PDF文件
            task.setCurrentStep(5, "回写比对结果到PDF");
            task.updateProgress(85.0, "回写比对结果到PDF...");
            
            String resultDir = uploadRootPath + "/ocr-compare/results/" + task.getTaskId();
            java.nio.file.Path resultDirPath = java.nio.file.Paths.get(resultDir);
            java.nio.file.Files.createDirectories(resultDirPath);
            
            String annotatedOldPdfPath = resultDir + "/old_annotated.pdf";
            String annotatedNewPdfPath = resultDir + "/new_annotated.pdf";
            
            // 回写比对结果到PDF
            annotatePDFWithResults(oldFilePath, annotatedOldPdfPath, compareResults, "DELETE");
            annotatePDFWithResults(newFilePath, annotatedNewPdfPath, compareResults, "INSERT");
            
            // 创建OCR比对结果
            OCRCompareResult result = new OCRCompareResult();
            result.setTaskId(task.getTaskId());
            result.setOldPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/old_annotated.pdf");
            result.setNewPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/new_annotated.pdf");
            
            // 转换比对结果为OCR比对结果格式
            List<Map<String, Object>> differences = convertCompareResultsToDifferences(compareResults);
            result.setDifferences(differences);
            
            // 计算相似度（简化计算，基于差异数量）
            double similarity = 1.0;
            if (differences != null && !differences.isEmpty()) {
                // 这里可以根据实际需求调整相似度计算逻辑
                similarity = Math.max(0.0, 1.0 - (differences.size() * 0.1));
            }
            result.setSimilarity(similarity);
            
            // 生成比对摘要
            result.generateSummary();
            
            // 设置前端需要的summary字段
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDifferences", differences != null ? differences.size() : 0);
            
            // 统计删除和新增的数量
            int deletions = 0, insertions = 0;
            if (differences != null) {
                for (Map<String, Object> diff : differences) {
                    String operation = (String) diff.get("operation");
                    if ("DELETE".equals(operation)) {
                        deletions++;
                    } else if ("INSERT".equals(operation)) {
                        insertions++;
                    }
                }
            }
            summary.put("deletions", deletions);
            summary.put("insertions", insertions);
            
            result.setSummary(summary);
            
            // 步骤6: 保存比对结果
            task.setCurrentStep(6, "保存比对结果");
            task.updateProgress(95.0, "保存比对结果...");
            
            results.put(task.getTaskId(), result);
            log.info("OCR比对结果生成完成：taskId={} diffs={} oldPdf={} newPdf={}", task.getTaskId(),
                differences == null ? 0 : differences.size(), result.getOldPdfUrl(), result.getNewPdfUrl());
            task.setStatus(OCRCompareTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            task.updateProgress(100.0, "比对完成");
            
            log.info("OCR比对任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            task.setStatus(OCRCompareTask.TaskStatus.FAILED);
            task.setErrorMessage("OCR比对执行异常: " + e.getMessage());
            log.error("OCR比对任务执行失败: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * 执行文本比对
     */
    private List<CompareResult> performTextComparison(String oldText, String newText, OCRCompareOptions options) {
        try {
            // 使用DiffUtil进行文本比对
            DiffUtil dmp = new DiffUtil();
            
            // 预处理文本
            String left = oldText == null ? "" : oldText;
            String right = newText == null ? "" : newText;
            
            // 根据选项进行文本预处理
            if (options.isIgnoreCase()) {
                left = left.toLowerCase();
                right = right.toLowerCase();
            }
            
            // 执行文本比对
            List<DiffUtil.Diff> diff = dmp.diff_main(left, right);
            // 转换为LinkedList以匹配方法签名
            java.util.LinkedList<DiffUtil.Diff> diffList = new java.util.LinkedList<>(diff);
            dmp.diff_cleanupSemantic(diffList);
            
            // 转换为比对结果
            List<CompareResult> results = new ArrayList<>();
            for (DiffUtil.Diff d : diffList) {
                if (d.operation == Operation.INSERT || d.operation == Operation.DELETE) {
                    // 过滤掉纯符号的差异
                    String realText = d.text.replaceAll("¶", "").replaceAll("\r", "").replaceAll("\n", "");
                    if (StrUtil.isNotBlank(realText)) {
                        // 创建比对结果，使用OCR结果中的真实位置信息
                        // 这里需要根据OCR结果中的位置信息创建Position对象
                        // 暂时使用默认值，后续会通过解析OCR结果来填充
                        Position oldPos = new Position(null, 1);
                        Position newPos = new Position(null, 1);
                        CompareResult result = new CompareResult(oldPos, newPos, d);
                        results.add(result);
                    }
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("文本比对失败", e);
            throw new RuntimeException("文本比对失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析OCR结果中的位置信息
     * 这个方法需要根据OCR结果JSON来解析每个差异项的具体位置
     */
    private void enrichCompareResultsWithPositionInfo(List<CompareResult> results, String oldOcrTaskId, String newOcrTaskId,
                                                      String oldPdfPath, String newPdfPath) {
        try {
            // 获取OCR结果的详细信息
            // 严格通过 HTTP 获取 OCR 结果，兼容远程独立部署
            com.fasterxml.jackson.databind.JsonNode oldOcrResult = ocrTaskService.getOCRResultViaHttp(oldOcrTaskId);
            com.fasterxml.jackson.databind.JsonNode newOcrResult = ocrTaskService.getOCRResultViaHttp(newOcrTaskId);
            
            if (oldOcrResult != null && newOcrResult != null) {
                // 解析OCR结果中的页面和位置信息
                log.info("开始解析OCR结果位置信息... oldTaskId={}, newTaskId={}", oldOcrTaskId, newOcrTaskId);
                
                // HTTP 返回结构：{"success":true, "task":{...}, "result": { json_data, text_content, result_path }}
                // 此处传入的是 ocrTaskService.getOCRResultViaHttp 返回的 result 节点，需要取 json_data
                com.fasterxml.jackson.databind.JsonNode oldJsonData = oldOcrResult.path("json_data");
                com.fasterxml.jackson.databind.JsonNode newJsonData = newOcrResult.path("json_data");
                if (oldJsonData.isMissingNode() || newJsonData.isMissingNode()) {
                    log.warn("OCR结果缺少 json_data 字段，oldHasJsonData={} newHasJsonData={}", !oldJsonData.isMissingNode(), !newJsonData.isMissingNode());
                }
                // 解析旧/新文档的OCR结果
                List<PageTextInfo> oldPageInfos = parseOCRResultToPageInfos(oldJsonData);
                List<PageTextInfo> newPageInfos = parseOCRResultToPageInfos(newJsonData);
                log.info("OCR解析页信息：oldPages={}, newPages={}", oldPageInfos.size(), newPageInfos.size());

                // 读取PDF页尺寸（pt）用于坐标换算
                float[] oldPageWidths = null, oldPageHeights = null;
                float[] newPageWidths = null, newPageHeights = null;
                try (PDDocument oldDoc = PDDocument.load(new File(oldPdfPath));
                     PDDocument newDoc = PDDocument.load(new File(newPdfPath))) {
                    int oldN = oldDoc.getNumberOfPages();
                    oldPageWidths = new float[oldN];
                    oldPageHeights = new float[oldN];
                    for (int i = 0; i < oldN; i++) {
                        PDPage p = oldDoc.getPage(i);
                        PDRectangle mb = p.getMediaBox();
                        oldPageWidths[i] = mb.getWidth();
                        oldPageHeights[i] = mb.getHeight();
                    }
                    int newN = newDoc.getNumberOfPages();
                    newPageWidths = new float[newN];
                    newPageHeights = new float[newN];
                    for (int i = 0; i < newN; i++) {
                        PDPage p = newDoc.getPage(i);
                        PDRectangle mb = p.getMediaBox();
                        newPageWidths[i] = mb.getWidth();
                        newPageHeights[i] = mb.getHeight();
                    }
                } catch (Exception ignore) {}
                
                // 为每个比对结果分配位置信息
                int assignedOld = 0;
                int assignedNew = 0;
                for (CompareResult result : results) {
                    String diffText = result.getDiff().text;
                    Operation operation = result.getDiff().operation;
                    
                    if (operation == Operation.DELETE) {
                        // 在旧文档中查找位置
                        RawMatchMulti multi = findRawMatchMultiLine(diffText, oldPageInfos);
                        RawMatch match = null;
                        if (multi != null && multi.rectsPx != null && !multi.rectsPx.isEmpty()) {
                            // 用第一段作为主定位，同时把所有段加入 Position.rects 以便分段高亮
                            float[] r0 = multi.rectsPx.get(0);
                            match = new RawMatch(multi.pageIndex, r0[0], r0[1], multi.imageWidth, multi.imageHeight, r0[2], r0[3]);
                        } else {
                            match = findRawMatchInPages(diffText, oldPageInfos);
                        }
                        if (match != null) {
                            log.debug("DELETE 命中: text='{}' page={} x={} y={} imgW={} imgH={} ",
                                abbreviate(diffText, 60), match.pageIndex, match.x, match.y, match.imageWidth, match.imageHeight);
                            int pageIdx0 = Math.max(0, match.pageIndex - 1);
                            float pageW = (oldPageWidths != null && pageIdx0 < oldPageWidths.length) ? oldPageWidths[pageIdx0] : 595f;
                            float pageH = (oldPageHeights != null && pageIdx0 < oldPageHeights.length) ? oldPageHeights[pageIdx0] : 842f;
                            // OCR 坐标是像素，从左上为(0,0)。转换到 PDF：x 线性缩放；y 需转为“自上而下”的 yDirAdj 值
                            float scaleX = pageW / Math.max(1f, match.imageWidth);
                            float scaleY = pageH / Math.max(1f, match.imageHeight);
                            float x = match.x * scaleX;
                            float yTop = match.y * scaleY; // 顶部Y（自上而下）
                            Position oldPos = new Position(x, yTop, pageW, pageH, pageIdx0);
                            // 多段：追加 rects
                            if (multi != null && multi.rectsPx != null && !multi.rectsPx.isEmpty()) {
                                for (float[] rp : multi.rectsPx) {
                                    oldPos.addRect(rp[0] * scaleX, rp[1] * scaleY, rp[2] * scaleX, rp[3] * scaleY);
                                }
                            }
                            if (match.boxWidthPx > 0 && match.boxHeightPx > 0) {
                                oldPos.setRectWidth(match.boxWidthPx * scaleX);
                                oldPos.setRectHeight(match.boxHeightPx * scaleY);
                            }
                            result.setOldPosition(oldPos);
                            assignedOld++;
                        } else {
                            log.debug("DELETE 未命中: text='{}'", abbreviate(diffText, 60));
                        }
                    } else if (operation == Operation.INSERT) {
                        // 在新文档中查找位置
                        RawMatchMulti multi = findRawMatchMultiLine(diffText, newPageInfos);
                        RawMatch match = null;
                        if (multi != null && multi.rectsPx != null && !multi.rectsPx.isEmpty()) {
                            float[] r0 = multi.rectsPx.get(0);
                            match = new RawMatch(multi.pageIndex, r0[0], r0[1], multi.imageWidth, multi.imageHeight, r0[2], r0[3]);
                        } else {
                            match = findRawMatchInPages(diffText, newPageInfos);
                        }
                        if (match != null) {
                            log.debug("INSERT 命中: text='{}' page={} x={} y={} imgW={} imgH={} ",
                                abbreviate(diffText, 60), match.pageIndex, match.x, match.y, match.imageWidth, match.imageHeight);
                            int pageIdx0 = Math.max(0, match.pageIndex - 1);
                            float pageW = (newPageWidths != null && pageIdx0 < newPageWidths.length) ? newPageWidths[pageIdx0] : 595f;
                            float pageH = (newPageHeights != null && pageIdx0 < newPageHeights.length) ? newPageHeights[pageIdx0] : 842f;
                            float scaleX = pageW / Math.max(1f, match.imageWidth);
                            float scaleY = pageH / Math.max(1f, match.imageHeight);
                            float x = match.x * scaleX;
                            float yTop = match.y * scaleY;
                            Position newPos = new Position(x, yTop, pageW, pageH, pageIdx0);
                            if (multi != null && multi.rectsPx != null && !multi.rectsPx.isEmpty()) {
                                for (float[] rp : multi.rectsPx) {
                                    newPos.addRect(rp[0] * scaleX, rp[1] * scaleY, rp[2] * scaleX, rp[3] * scaleY);
                                }
                            }
                            if (match.boxWidthPx > 0 && match.boxHeightPx > 0) {
                                newPos.setRectWidth(match.boxWidthPx * scaleX);
                                newPos.setRectHeight(match.boxHeightPx * scaleY);
                            }
                            result.setNewPosition(newPos);
                            assignedNew++;
                        } else {
                            log.debug("INSERT 未命中: text='{}'", abbreviate(diffText, 60));
                        }
                    }
                }
                
                log.info("OCR位置信息解析完成，共处理 {} 个比对结果，定位成功：old={}，new={}", results.size(), assignedOld, assignedNew);
            }
        } catch (Exception e) {
            log.warn("解析OCR位置信息失败，使用默认位置", e);
        }
    }
    
    /**
     * 解析OCR结果JSON为页面文本信息列表
     */
    private List<PageTextInfo> parseOCRResultToPageInfos(com.fasterxml.jackson.databind.JsonNode ocrResult) {
        List<PageTextInfo> pageInfos = new ArrayList<>();
        
        try {
            if (ocrResult == null) {
                log.warn("OCR结果为空，无法解析页信息");
                return pageInfos;
            }
            com.fasterxml.jackson.databind.JsonNode pages = ocrResult.path("pages");
            if (pages.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode page : pages) {
                    int pageIndex = page.path("page_index").asInt(1);
                    com.fasterxml.jackson.databind.JsonNode items = page.path("items");
                    int imgW = page.path("image_width").asInt(0);
                    int imgH = page.path("image_height").asInt(0);
                    if (imgW <= 0 || imgH <= 0) {
                        log.debug("OCR页图像尺寸缺失或异常: page={} image_width={} image_height={}", pageIndex, imgW, imgH);
                    }
                    
                    List<TextItemInfo> textItems = new ArrayList<>();
                    if (items.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode item : items) {
                            String text = item.path("text").asText("");
                            com.fasterxml.jackson.databind.JsonNode box = item.path("box");
                            
                            if (!text.isEmpty() && box.isArray() && box.size() >= 4) {
                                // 解析坐标框信息
                                // box 是四点矩形：[[x1,y1],[x2,y2],[x3,y3],[x4,y4]]，取外接矩形
                                double xMin = Math.min(Math.min(box.get(0).get(0).asDouble(0), box.get(1).get(0).asDouble(0)),
                                                       Math.min(box.get(2).get(0).asDouble(0), box.get(3).get(0).asDouble(0)));
                                double xMax = Math.max(Math.max(box.get(0).get(0).asDouble(0), box.get(1).get(0).asDouble(0)),
                                                       Math.max(box.get(2).get(0).asDouble(0), box.get(3).get(0).asDouble(0)));
                                double yMin = Math.min(Math.min(box.get(0).get(1).asDouble(0), box.get(1).get(1).asDouble(0)),
                                                       Math.min(box.get(2).get(1).asDouble(0), box.get(3).get(1).asDouble(0)));
                                double yMax = Math.max(Math.max(box.get(0).get(1).asDouble(0), box.get(1).get(1).asDouble(0)),
                                                       Math.max(box.get(2).get(1).asDouble(0), box.get(3).get(1).asDouble(0)));
                                float x = (float)xMin;
                                float y = (float)yMin; // OCR 坐标以左上为原点，y 越大越靠下
                                float width = (float)(xMax - xMin);
                                float height = (float)(yMax - yMin);
                                
                                // 尝试解析字符级位置信息 chars
                                StringBuilder charText = new StringBuilder();
                                java.util.List<Float> xs = new java.util.ArrayList<>();
                                java.util.List<Float> ys = new java.util.ArrayList<>();
                                java.util.List<Float> x2s = new java.util.ArrayList<>();
                                java.util.List<Float> bys = new java.util.ArrayList<>();
                                com.fasterxml.jackson.databind.JsonNode chars = item.path("chars");
                                if (chars.isArray()) {
                                    for (com.fasterxml.jackson.databind.JsonNode ch : chars) {
                                        String c = ch.path("ch").asText("");
                                        com.fasterxml.jackson.databind.JsonNode cbox = ch.path("box");
                                        if (c != null && !c.isBlank() && cbox.isArray() && cbox.size() >= 4 && cbox.get(0).isArray()) {
                                            // 四点：lt, rt, rb, lb
                                            float cx = (float) cbox.get(0).get(0).asDouble(0);
                                            float cy = (float) cbox.get(0).get(1).asDouble(0);
                                            float cx2 = (float) cbox.get(1).get(0).asDouble(0);
                                            // bottom y 可取 rb.y 与 lb.y 的较大值
                                            float cby = (float) Math.max(cbox.get(2).get(1).asDouble(0), cbox.get(3).get(1).asDouble(0));
                                            charText.append(c);
                                            xs.add(cx);
                                            ys.add(cy);
                                            x2s.add(cx2);
                                            bys.add(cby);
                                        }
                                    }
                                }
                                float[] xarr = null;
                                float[] yarr = null;
                                float[] x2arr = null;
                                float[] byarr = null;
                                if (!xs.isEmpty()) {
                                    xarr = new float[xs.size()];
                                    for (int i = 0; i < xs.size(); i++) {
                                        xarr[i] = xs.get(i);
                                    }
                                }
                                if (!ys.isEmpty()) {
                                    yarr = new float[ys.size()];
                                    for (int i = 0; i < ys.size(); i++) {
                                        yarr[i] = ys.get(i);
                                    }
                                }
                                if (!x2s.isEmpty()) {
                                    x2arr = new float[x2s.size()];
                                    for (int i = 0; i < x2s.size(); i++) x2arr[i] = x2s.get(i);
                                }
                                if (!bys.isEmpty()) {
                                    byarr = new float[bys.size()];
                                    for (int i = 0; i < bys.size(); i++) byarr[i] = bys.get(i);
                                }
                                TextItemInfo textItem = new TextItemInfo(text, x, y, width, height, imgW, imgH,
                                                                         charText.toString(), xarr, yarr, x2arr, byarr);
                                textItems.add(textItem);
                            }
                        }
                    } else {
                        log.debug("OCR页无 items 数据: page={}", pageIndex);
                    }
                    
                    PageTextInfo pageInfo = new PageTextInfo(pageIndex, imgW, imgH, textItems);
                    pageInfos.add(pageInfo);
                }
            } else {
                log.warn("OCR结果 'pages' 字段不是数组: {}", ocrResult.toString());
            }
        } catch (Exception e) {
            log.warn("解析OCR结果JSON失败", e);
        }
        
        return pageInfos;
    }
    
    /**
     * 在页面文本信息中查找指定文本的位置
     */
    // 原始匹配结果（用于后续按PDF页尺寸换算）
    private static class RawMatch {
        final int pageIndex; // 1-based
        final float x; // 像素坐标（左上为原点）
        final float y; // 像素坐标（左上为原点）
        final int imageWidth;
        final int imageHeight;
        RawMatch(int pageIndex, float x, float y, int imageWidth, int imageHeight) {
            this(pageIndex, x, y, imageWidth, imageHeight, 0, 0);
        }
        final float boxWidthPx;
        final float boxHeightPx;
        RawMatch(int pageIndex, float x, float y, int imageWidth, int imageHeight, float boxWidthPx, float boxHeightPx) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.boxWidthPx = boxWidthPx;
            this.boxHeightPx = boxHeightPx;
        }
    }

    /**
     * 多段原始匹配（用于跨行分段矩形）
     */
    private static class RawMatchMulti {
        final int pageIndex; // 1-based
        final int imageWidth;
        final int imageHeight;
        final java.util.List<float[]> rectsPx; // 每个为 [x, yTop, w, h]，像素坐标
        RawMatchMulti(int pageIndex, int imageWidth, int imageHeight, java.util.List<float[]> rectsPx) {
            this.pageIndex = pageIndex;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.rectsPx = rectsPx;
        }
    }

    /**
     * 在OCR页文本中查找目标文本，返回第一个匹配的外接框左上角像素坐标与图像尺寸
     */
    private RawMatch findRawMatchInPages(String targetText, List<PageTextInfo> pageInfos) {
        if (targetText == null) return null;
        String cleanTargetText = normalize(targetText);
        if (cleanTargetText.isEmpty()) return null;

        for (PageTextInfo pageInfo : pageInfos) {
            for (TextItemInfo textItem : pageInfo.getTextItems()) {
                String t = normalize(textItem.getText());
                if (t == null || t.isEmpty()) continue;
                // 优先字符级匹配，提高定位精度
                String ct = textItem.getCharText();
                if (ct != null && !ct.isEmpty() && textItem.getCharXs() != null && textItem.getCharYs() != null) {
                    String nc = normalize(ct);
                    int idx = nc.indexOf(cleanTargetText);
                    if (idx >= 0 && idx < textItem.getCharXs().length && idx < textItem.getCharYs().length) {
                        // 取从 idx 开始，长度为 cleanTargetText.length 的字符序列的整体包围盒
                        int end = Math.min(idx + cleanTargetText.length() - 1, textItem.getCharXs().length - 1);
                        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                        for (int k = idx; k <= end; k++) {
                            float ltX = textItem.getCharXs()[k];
                            float ltY = textItem.getCharYs()[k];
                            float rtX = (textItem.getCharX2s() != null && k < textItem.getCharX2s().length) ? textItem.getCharX2s()[k] : (ltX + 2);
                            float bY = (textItem.getCharBottomYs() != null && k < textItem.getCharBottomYs().length) ? textItem.getCharBottomYs()[k] : (ltY + textItem.getHeight());
                            minX = Math.min(minX, Math.min(ltX, rtX));
                            maxX = Math.max(maxX, Math.max(ltX, rtX));
                            minY = Math.min(minY, ltY);
                            maxY = Math.max(maxY, bY);
                        }
                        float bw = Math.max(2f, maxX - minX);
                        float bh = Math.max(2f, maxY - minY);
                        return new RawMatch(pageInfo.getPageIndex(), minX, minY, pageInfo.getImageWidth(), pageInfo.getImageHeight(), bw, bh);
                    }
                }
                // 回退：行级包含
                if (t.contains(cleanTargetText) || cleanTargetText.contains(t)) {
                    return new RawMatch(pageInfo.getPageIndex(), textItem.getX(), textItem.getY(), pageInfo.getImageWidth(), pageInfo.getImageHeight(), textItem.getWidth(), textItem.getHeight());
                }
            }
        }
        return null;
    }

    /**
     * 将字符级匹配扩展为“跨行分段”：
     * - 当命中的字符序列在同一行：合并为单段 [minX, minY, w, h]
     * - 当换行：按行拆分，返回多段
     */
    private RawMatchMulti findRawMatchMultiLine(String targetText, List<PageTextInfo> pageInfos) {
        if (targetText == null) return null;
        String cleanTargetText = normalize(targetText);
        if (cleanTargetText.isEmpty()) return null;
        for (PageTextInfo pageInfo : pageInfos) {
            for (TextItemInfo textItem : pageInfo.getTextItems()) {
                String ct = textItem.getCharText();
                if (ct == null || ct.isEmpty() || textItem.getCharXs() == null || textItem.getCharYs() == null) continue;
                String nc = normalize(ct);
                int idx = nc.indexOf(cleanTargetText);
                if (idx < 0) continue;
                int end = Math.min(idx + cleanTargetText.length() - 1, textItem.getCharXs().length - 1);
                // 按 yTop 分组（同一行的 yTop 近似相等，容差使用 2px）
                java.util.Map<Integer, java.util.List<Integer>> groups = new java.util.LinkedHashMap<>();
                final float tol = 2f;
                for (int k = idx; k <= end; k++) {
                    float yTop = textItem.getCharYs()[k];
                    // 归一化key
                    int key = Math.round(yTop / tol);
                    groups.computeIfAbsent(key, kk -> new java.util.ArrayList<>()).add(k);
                }
                java.util.List<float[]> rects = new java.util.ArrayList<>();
                for (java.util.List<Integer> ks : groups.values()) {
                    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                    for (int id : ks) {
                        float ltX = textItem.getCharXs()[id];
                        float ltY = textItem.getCharYs()[id];
                        float rtX = (textItem.getCharX2s() != null && id < textItem.getCharX2s().length) ? textItem.getCharX2s()[id] : (ltX + 2);
                        float bY = (textItem.getCharBottomYs() != null && id < textItem.getCharBottomYs().length) ? textItem.getCharBottomYs()[id] : (ltY + textItem.getHeight());
                        minX = Math.min(minX, Math.min(ltX, rtX));
                        maxX = Math.max(maxX, Math.max(ltX, rtX));
                        minY = Math.min(minY, ltY);
                        maxY = Math.max(maxY, bY);
                    }
                    rects.add(new float[]{minX, minY, Math.max(2f, maxX - minX), Math.max(2f, maxY - minY)});
                }
                return new RawMatchMulti(pageInfo.getPageIndex(), pageInfo.getImageWidth(), pageInfo.getImageHeight(), rects);
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String r = s.replaceAll("¶", "").replaceAll("\r", "").replaceAll("\n", "");
        // 进一步压缩空白
        r = r.replaceAll("\\s+", "").trim();
        return r;
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
    
    /**
     * 页面文本信息类
     */
    private static class PageTextInfo {
        private final int pageIndex; // 1-based
        private final int imageWidth; // OCR图像宽（px）
        private final int imageHeight; // OCR图像高（px）
        private final List<TextItemInfo> textItems;
        
        public PageTextInfo(int pageIndex, int imageWidth, int imageHeight, List<TextItemInfo> textItems) {
            this.pageIndex = pageIndex;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.textItems = textItems;
        }
        
        public int getPageIndex() { return pageIndex; }
        public int getImageWidth() { return imageWidth; }
        public int getImageHeight() { return imageHeight; }
        public List<TextItemInfo> getTextItems() { return textItems; }
    }
    
    /**
     * 文本项信息类
     */
    private static class TextItemInfo {
        private final String text;
        private final float x; // 左上角x（px）
        private final float y; // 左上角y（px）
        private final float width;
        private final float height;
        private final int imageWidth;
        private final int imageHeight;
        // 字符级信息（可选，来自 chars）
        private final String charText; // 跳过空白后的字符序列
        private final float[] charXs;  // 与 charText 对齐的每个字符左上角x
        private final float[] charYs;  // 与 charText 对齐的每个字符左上角y（top）
        private final float[] charX2s; // 与 charText 对齐的每个字符右上角x（top-right）
        private final float[] charBottomYs; // 与 charText 对齐的每个字符底部y（bottom）
        
        public TextItemInfo(String text, float x, float y, float width, float height,
                             int imageWidth, int imageHeight,
                             String charText, float[] charXs, float[] charYs, float[] charX2s, float[] charBottomYs) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.charText = charText;
            this.charXs = charXs;
            this.charYs = charYs;
            this.charX2s = charX2s;
            this.charBottomYs = charBottomYs;
        }
        
        public String getText() { return text; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public int getImageWidth() { return imageWidth; }
        public int getImageHeight() { return imageHeight; }
        public String getCharText() { return charText; }
        public float[] getCharXs() { return charXs; }
        public float[] getCharYs() { return charYs; }
        public float[] getCharX2s() { return charX2s; }
        public float[] getCharBottomYs() { return charBottomYs; }
    }
    
    /**
     * 将比对结果回写到PDF文件
     */
    private void annotatePDFWithResults(String sourcePdfPath, String targetPdfPath, 
                                      List<CompareResult> results, String operationType) {
        try (PDDocument document = PDDocument.load(new File(sourcePdfPath))) {
            
            // 根据操作类型过滤结果
            List<CompareResult> filteredResults = new ArrayList<>();
            for (CompareResult result : results) {
                if (operationType.equals(result.getDiff().operation.toString())) {
                    filteredResults.add(result);
                }
            }
            
            // 为每个差异添加标注
            for (CompareResult result : filteredResults) {
                // 使用OCR结果中的真实位置信息进行标注
                addAnnotationToPDF(document, result, operationType);
            }
            
            // 保存标注后的PDF
            document.save(targetPdfPath);
            log.info("PDF标注完成: {} -> {}", sourcePdfPath, targetPdfPath);
            
        } catch (Exception e) {
            log.error("PDF标注失败: {} -> {}", sourcePdfPath, targetPdfPath, e);
            throw new RuntimeException("PDF标注失败: " + e.getMessage());
        }
    }
    
    /**
     * 向PDF添加标注
     */
    private void addAnnotationToPDF(PDDocument document, CompareResult result, String operationType) {
        try {
            // 按操作选择位置
            Position pos = "DELETE".equals(operationType) ? result.getOldPosition() : result.getNewPosition();
            if (pos == null) return;
            int pageIdx = Math.max(0, pos.getPage()); // Position.page 为0-based
            if (pageIdx >= document.getNumberOfPages()) pageIdx = document.getNumberOfPages() - 1;
            if (pageIdx < 0) return;

            PDPage page = document.getPage(pageIdx);
                
                // 创建文本标注
                PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(
                    PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                
                // 设置颜色
                float[] color;
                if ("DELETE".equals(operationType)) {
                    color = new float[]{1.0f, 0.0f, 0.0f}; // 红色
                } else {
                    color = new float[]{0.0f, 1.0f, 0.0f}; // 绿色
                }
                
                PDColor hl = new PDColor(color, PDDeviceRGB.INSTANCE);
                markup.setColor(hl);
                
                // 根据 Position 构建矩形（将 pos.y 视为“顶部Y”，若有 rectWidth/rectHeight 则精确使用）
                float px = Math.max(0, pos.getX());
                float yTop = Math.max(0, pos.getY()); // 顶部Y（自上而下）
                float pageH = page.getMediaBox().getHeight();
                float rectW = pos.getRectWidth() > 0 ? pos.getRectWidth() : 140f;
                float rectH = pos.getRectHeight() > 0 ? pos.getRectHeight() : 22f;

                // 如果有多段矩形，依次绘制多条标注；否则绘制单条
                java.util.List<float[]> rects = pos.getRects();
                if (rects != null && !rects.isEmpty()) {
                    // 设置内容
                    String content = operationType.equals("DELETE") ? "删除：" : "新增：";
                    content += result.getDiff().text;
                    // 添加到页面
                    List<PDAnnotation> annotations = page.getAnnotations();
                    if (annotations == null) {
                        annotations = new ArrayList<>();
                    }
                    for (float[] r : rects) {
                        float rx = r[0], ryTop = r[1], rw = r[2], rh = r[3];
                        PDRectangle pr = new PDRectangle();
                        pr.setLowerLeftX(rx);
                        pr.setLowerLeftY(Math.max(0, pageH - (ryTop + rh)));
                        pr.setUpperRightX(rx + rw);
                        pr.setUpperRightY(Math.max(0, Math.min(pageH, pageH - ryTop)));
                        PDAnnotationTextMarkup m = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                        m.setColor(hl);
                        m.setRectangle(pr);
                        float[] qu = new float[]{pr.getLowerLeftX(), pr.getUpperRightY(), pr.getUpperRightX(), pr.getUpperRightY(), pr.getLowerLeftX(), pr.getLowerLeftY(), pr.getUpperRightX(), pr.getLowerLeftY()};
                        m.setQuadPoints(qu);
                        m.setSubtype("Highlight");
                        m.setModifiedDate(java.util.Calendar.getInstance().getTime().toString());
                        m.setContents(content);
                        annotations.add(m);
                    }
                    page.setAnnotations(annotations);
                    return;
                } else {
                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX(px);
                    position.setLowerLeftY(Math.max(0, pageH - (yTop + rectH)));
                    position.setUpperRightX(px + rectW);
                    position.setUpperRightY(Math.max(0, Math.min(pageH, pageH - yTop)));
                    markup.setRectangle(position);
                    // 设置 QuadPoints 与矩形一致
                    float[] quads = new float[8];
                    quads[0] = position.getLowerLeftX();
                    quads[1] = position.getUpperRightY();
                    quads[2] = position.getUpperRightX();
                    quads[3] = position.getUpperRightY();
                    quads[4] = position.getLowerLeftX();
                    quads[5] = position.getLowerLeftY();
                    quads[6] = position.getUpperRightX();
                    quads[7] = position.getLowerLeftY();
                    markup.setQuadPoints(quads);
                }
                markup.setSubtype("Highlight");
                markup.setModifiedDate(java.util.Calendar.getInstance().getTime().toString());
                
                // 设置内容
                String content = operationType.equals("DELETE") ? "删除：" : "新增：";
                content += result.getDiff().text;
                markup.setContents(content);
                
                // 添加到页面
                List<PDAnnotation> annotations = page.getAnnotations();
                if (annotations == null) {
                    annotations = new ArrayList<>();
                }
                annotations.add(markup);
                page.setAnnotations(annotations);
            
        } catch (Exception e) {
            log.warn("添加PDF标注失败", e);
        }
    }
    
    /**
     * 转换比对结果为OCR比对结果格式
     */
    private List<Map<String, Object>> convertCompareResultsToDifferences(List<CompareResult> compareResults) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        for (CompareResult result : compareResults) {
            Map<String, Object> diff = new HashMap<>();
            diff.put("operation", result.getDiff().operation.toString());
            diff.put("text", result.getDiff().text);
            
            // 从OCR结果中获取真实的位置信息
            Position oldPos = result.getOldPosition();
            Position newPos = result.getNewPosition();
            
            // 输出嵌套的 oldPosition/newPosition，以便前端精准联动
            if (oldPos != null) {
                Map<String, Object> op = new HashMap<>();
                op.put("page", oldPos.getPage());
                op.put("x", oldPos.getX());
                op.put("y", oldPos.getY());
                op.put("pageWidth", oldPos.getPageWidth());
                op.put("pageHeight", oldPos.getPageHeight());
                diff.put("oldPosition", op);
            }
            if (newPos != null) {
                Map<String, Object> np = new HashMap<>();
                np.put("page", newPos.getPage());
                np.put("x", newPos.getX());
                np.put("y", newPos.getY());
                np.put("pageWidth", newPos.getPageWidth());
                np.put("pageHeight", newPos.getPageHeight());
                diff.put("newPosition", np);
            }
            
            differences.add(diff);
        }
        
        return differences;
    }
    
    /**
     * 等待OCR任务完成并监控进度
     */
    private void waitForOCRCompletion(OCRCompareTask task) {
        try {
            boolean oldCompleted = false;
            boolean newCompleted = false;
            
            // 最大等待时间（分钟）
            int maxWaitMinutes = 10;
            long startTime = System.currentTimeMillis();
            long maxWaitTime = maxWaitMinutes * 60 * 1000;
            
            while (!oldCompleted || !newCompleted) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > maxWaitTime) {
                    task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                    task.setErrorMessage("OCR任务超时（超过" + maxWaitMinutes + "分钟）");
                    return;
                }
                
                // 检查旧文档OCR进度
                if (!oldCompleted && task.getOldOcrTaskId() != null) {
                    OCRTask oldTask = ocrTaskService.getTaskStatus(task.getOldOcrTaskId());
                    if (oldTask != null) {
                        double oldProgress = oldTask.getProgress();
                        task.updateOCRProgress("old", oldProgress);
                        
                        // 更新总体进度
                        if (oldProgress > 0) {
                            double overallProgress = 20.0 + (oldProgress * 0.4); // 20-60% 范围
                            task.updateProgress(overallProgress, "OCR识别旧文档中...");
                        }
                        
                        if (oldTask.isCompleted()) {
                            oldCompleted = true;
                            if (oldTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
                                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                                task.setErrorMessage("旧文档OCR识别失败: " + oldTask.getErrorMessage());
                                return;
                            }
                        }
                    }
                }
                
                // 检查新文档OCR进度
                if (!newCompleted && task.getNewOcrTaskId() != null) {
                    OCRTask newTask = ocrTaskService.getTaskStatus(task.getNewOcrTaskId());
                    if (newTask != null) {
                        double newProgress = newTask.getProgress();
                        task.updateOCRProgress("new", newProgress);
                        
                        // 更新总体进度
                        if (newProgress > 0) {
                            double overallProgress = 40.0 + (newProgress * 0.4); // 40-80% 范围
                            task.updateProgress(overallProgress, "OCR识别新文档中...");
                        }
                        
                        if (newTask.isCompleted()) {
                            newCompleted = true;
                            if (newTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
                                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                                task.setErrorMessage("新文档OCR识别失败: " + newTask.getErrorMessage());
                                return;
                            }
                        }
                    }
                }
                
                // 等待一段时间再检查
                Thread.sleep(2000);
            }
            
            // 两个OCR任务都完成了
            task.updateProgress(60.0, "OCR识别完成，准备执行比对...");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.setStatus(OCRCompareTask.TaskStatus.FAILED);
            task.setErrorMessage("OCR任务被中断");
        }
    }
    
    /**
     * 获取OCR识别结果文本
     */
    private String getOCRResultText(String ocrTaskId) {
        if (ocrTaskId == null) {
            return null;
        }
        
        OCRTask ocrTask = ocrTaskService.getTaskStatus(ocrTaskId);
        if (ocrTask == null || !ocrTask.isCompleted() || ocrTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
            return null;
        }
        
        try {
            // 首先检查OCRTask中是否已经保存了文本内容
            String savedTextContent = ocrTask.getTextContent();
            if (savedTextContent != null && !savedTextContent.trim().isEmpty()) {
                log.info("使用已保存的OCR结果，任务ID: {}, 文本长度: {} 字符", ocrTaskId, savedTextContent.length());
                return savedTextContent;
            }
            
            // 如果没有保存的文本内容，通过HTTP接口获取OCR结果
            log.info("OCRTask中未保存文本内容，通过HTTP接口获取，任务ID: {}", ocrTaskId);
            com.fasterxml.jackson.databind.JsonNode result = ocrTaskService.getOCRResultViaHttp(ocrTaskId);
            if (result != null) {
                String textContent = result.path("text_content").asText("");
                if (textContent != null && !textContent.trim().isEmpty()) {
                    log.info("成功通过HTTP接口获取OCR结果，任务ID: {}, 文本长度: {} 字符", ocrTaskId, textContent.length());
                    // 将获取到的文本内容保存到OCRTask中，避免下次重复调用
                    ocrTask.setTextContent(textContent);
                    return textContent;
                } else {
                    log.warn("HTTP接口返回的OCR结果文本内容为空，任务ID: {}", ocrTaskId);
                    return null;
                }
            } else {
                log.warn("无法通过HTTP接口获取OCR结果，任务ID: {}", ocrTaskId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取OCR结果失败，任务ID: {}", ocrTaskId, e);
            return null;
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "OCR_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() {
        // 创建上传目录
        File uploadDir = new File(uploadRootPath, "ocr_uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }
    
    /**
     * 保存上传的文件
     */
    private String saveFile(MultipartFile file, String fileName) throws IOException {
        File uploadDir = new File(uploadRootPath, "ocr_uploads");
        File targetFile = new File(uploadDir, fileName);
        file.transferTo(targetFile);
        return targetFile.getAbsolutePath();
    }
    
    /**
     * 清理任务相关文件
     */
    private void cleanupTaskFiles(OCRCompareTask task) {
        try {
            // 清理旧文件
            if (task.getOldFilePath() != null) {
                File oldFile = new File(task.getOldFilePath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            
            // 清理新文件
            if (task.getNewFilePath() != null) {
                File newFile = new File(task.getNewFilePath());
                if (newFile.exists()) {
                    newFile.delete();
                }
            }
        } catch (Exception e) {
            log.warn("清理任务文件失败", e);
        }
    }
}
