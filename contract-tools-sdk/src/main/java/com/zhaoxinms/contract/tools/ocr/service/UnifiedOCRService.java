package com.zhaoxinms.contract.tools.ocr.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.common.ocr.OCRProvider;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.model.MinerURecognitionResult;
import com.zhaoxinms.contract.tools.comparePRO.service.MinerUOCRService;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.extract.model.CharBox;
import com.zhaoxinms.contract.tools.extract.model.TextBox;
import com.zhaoxinms.contract.tools.extract.model.EnhancedOCRResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一的OCR服务 - 基于 MinerU OCR
 * 
 * 本服务已简化为只支持 MinerU OCR，所有旧的 OCR 服务（DotsOCR、RapidOCR、阿里云OCR）已移除。
 * MinerU 提供更高质量的 OCR 识别，支持表格、公式、图片等复杂内容。
 */
@Slf4j
@Service
public class UnifiedOCRService implements OCRProvider {
    
    @Autowired(required = false)
    private MinerUOCRService mineruOcrService;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;
    
    @PostConstruct
    public void init() {
        log.info("初始化统一OCR服务 - 使用 MinerU OCR");
        
        if (mineruOcrService == null) {
            log.warn("MinerU OCR 服务未启用，请检查配置");
        } else {
            log.info("MinerU OCR 服务已就绪");
        }
    }
    
    @Override
    public OCRProvider.OCRResult recognizePdf(File pdfFile) {
        return recognizePdf(pdfFile, true, 12.0, 12.0);
    }
    
    /**
     * 识别PDF（支持页眉页脚设置）
     * 目录结构：rule-extract-data/ocr-output/{年月}/{任务id}/
     */
    public OCRProvider.OCRResult recognizePdf(File pdfFile, boolean ignoreHeaderFooter, 
                                             double headerHeightPercent, double footerHeightPercent) {
        // 使用自动生成的taskId（带年月前缀）
        String originalTaskId = UUID.randomUUID().toString();
        String taskId = FileStorageUtils.generateFileId(originalTaskId);
        String yearMonthPath = FileStorageUtils.getYearMonthPathFromFileId(taskId);
        
        // 构建输出目录：rule-extract-data/ocr-output/{年月}/{原始任务id}/
        File ocrOutputDir = new File(uploadRootPath, "rule-extract-data/ocr-output/" + yearMonthPath);
        if (!ocrOutputDir.exists()) {
            ocrOutputDir.mkdirs();
        }
        File taskOutputDir = new File(ocrOutputDir, originalTaskId);
        taskOutputDir.mkdirs();
        
        return recognizePdf(pdfFile, taskId, taskOutputDir, ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);
    }
    
    /**
     * 识别PDF（支持指定taskId和输出目录）
     * 
     * @param pdfFile PDF文件
     * @param taskId 任务ID
     * @param taskOutputDir 任务输出目录（中间文件将保存到此目录下的mineru_intermediate子目录）
     * @param ignoreHeaderFooter 是否忽略页眉页脚
     * @param headerHeightPercent 页眉高度百分比
     * @param footerHeightPercent 页脚高度百分比
     */
    public OCRProvider.OCRResult recognizePdf(File pdfFile, String taskId, File taskOutputDir,
                                             boolean ignoreHeaderFooter, 
                                             double headerHeightPercent, double footerHeightPercent) {
        log.info("使用 MinerU OCR 识别PDF: {}, 任务ID: {}, 输出目录: {}, 忽略页眉页脚: {}", 
            pdfFile.getName(), taskId, taskOutputDir.getAbsolutePath(), ignoreHeaderFooter);
        
        if (mineruOcrService == null) {
            throw new RuntimeException("OCR 服务未启用，请检查配置");
        }
        
        try {
            // 确保输出目录存在
            if (!taskOutputDir.exists()) {
                taskOutputDir.mkdirs();
            }
            
            // 创建选项并设置页眉页脚参数
            CompareOptions options = new CompareOptions();
            options.setIgnoreHeaderFooter(ignoreHeaderFooter);
            options.setHeaderHeightPercent(headerHeightPercent);
            options.setFooterHeightPercent(footerHeightPercent);
            
            // 调用 MinerU 进行 PDF 识别
            MinerURecognitionResult mineruResult = mineruOcrService.recognizePdf(
                pdfFile, 
                taskId, 
                taskOutputDir,
                "extract", // 文档模式：extract 表示用于智能提取
                options
            );
            
            // 从结果中提取 PageLayout 数组和跨页表格管理器
            TextExtractionUtil.PageLayout[] pageLayouts = mineruResult.layouts;
            var tableManager = mineruResult.tableManager;
            
            // 输出跨页表格统计
            if (tableManager != null && tableManager.getTableGroupCount() > 0) {
                log.info("📊 跨页表格识别统计: {}", tableManager.getStatistics());
            }
            
            // 提取文本和TextBox数据
            StringBuilder allText = new StringBuilder();
            List<TextBox> textBoxes = new ArrayList<>();
            int currentPos = 0; // 当前字符位置（用于计算字符索引）
            
            for (int i = 0; i < pageLayouts.length; i++) {
                TextExtractionUtil.PageLayout layout = pageLayouts[i];
                
                // 不添加页面分隔符标记，确保跨页文本连续
                if (allText.length() > 0 && i > 0) {
                    allText.append("\n\n");
                    currentPos += 2; // 两个换行符
                }
                
                // 提取页面文本和TextBox
                for (TextExtractionUtil.LayoutItem item : layout.items) {
                    if (item.text != null && !item.text.trim().isEmpty()) {
                        String text = item.text.trim();
                        
                        // 记录当前文本块的起始位置
                        int startPos = currentPos;
                        
                        // 添加文本到总文本中
                        allText.append(text).append("\n");
                        
                        // 计算结束位置（不包括换行符）
                        int endPos = startPos + text.length();
                        
                        // 更新当前位置（包括换行符）
                        currentPos = endPos + 1; // +1 是换行符
                        
                        // 为每个LayoutItem创建一个TextBox，包含字符索引信息
                        // 一个item代表一个文本块（可能是一行文字、一个表格单元格等）
                        // 支持表格跨页：同一文本块可能有多个bbox
                        TextBox textBox = new TextBox(
                            layout.page,
                            text,
                            item.bbox != null ? item.bbox.clone() : new double[]{0, 0, 0, 0},
                            item.category != null ? item.category : "Text",
                            startPos,
                            endPos
                        );
                        textBoxes.add(textBox);
                    }
                }
            }
            
            OCRProvider.OCRResult result = new OCRProvider.OCRResult(allText.toString());
            
            // 构建图片路径信息（保存到metadata）
            File imagesDir = new File(taskOutputDir, "images/extract");
            List<String> pageImagePaths = new ArrayList<>();
            if (imagesDir.exists()) {
                for (int i = 1; i <= pageLayouts.length; i++) {
                    // 尝试多种可能的图片扩展名
                    File pngFile = new File(imagesDir, "page-" + i + ".png");
                    File jpgFile = new File(imagesDir, "page-" + i + ".jpg");
                    
                    if (pngFile.exists()) {
                        pageImagePaths.add(pngFile.getAbsolutePath());
                    } else if (jpgFile.exists()) {
                        pageImagePaths.add(jpgFile.getAbsolutePath());
                    }
                }
            }
            
            // 将图片路径和TextBox信息保存到metadata（作为JSONObject）
            com.alibaba.fastjson2.JSONObject metadata = new com.alibaba.fastjson2.JSONObject();
            metadata.put("totalPages", pageLayouts.length);
            metadata.put("pageImagePaths", pageImagePaths);
            metadata.put("imagesDir", imagesDir.getAbsolutePath());
            metadata.put("taskId", taskId);
            
            // 序列化TextBox数据 - 转换为简单的JSON格式
            com.alibaba.fastjson2.JSONArray textBoxesArray = new com.alibaba.fastjson2.JSONArray();
            for (TextBox textBox : textBoxes) {
                com.alibaba.fastjson2.JSONObject textBoxJson = new com.alibaba.fastjson2.JSONObject();
                textBoxJson.put("page", textBox.page);
                textBoxJson.put("text", textBox.text);
                textBoxJson.put("bbox", textBox.bbox);
                textBoxJson.put("category", textBox.category);
                textBoxJson.put("startPos", textBox.startPos);
                textBoxJson.put("endPos", textBox.endPos);
                textBoxesArray.add(textBoxJson);
            }
            metadata.put("textBoxes", textBoxesArray.toJSONString());
            
            // 保存页面尺寸信息
            com.alibaba.fastjson2.JSONArray pageDimensions = new com.alibaba.fastjson2.JSONArray();
            for (TextExtractionUtil.PageLayout layout : pageLayouts) {
                com.alibaba.fastjson2.JSONObject pageInfo = new com.alibaba.fastjson2.JSONObject();
                pageInfo.put("page", layout.page);
                pageInfo.put("width", layout.imageWidth);
                pageInfo.put("height", layout.imageHeight);
                pageDimensions.add(pageInfo);
            }
            metadata.put("pageDimensions", pageDimensions);
            
            // 序列化跨页表格信息（用于前端标记跨页bbox）
            if (tableManager != null && tableManager.getTableGroupCount() > 0) {
                com.alibaba.fastjson2.JSONArray crossPageTablesArray = new com.alibaba.fastjson2.JSONArray();
                
                for (var group : tableManager.getAllTableGroups()) {
                    if (group.continuationParts.isEmpty()) {
                        continue;  // 不是跨页表格，跳过
                    }
                    
                    com.alibaba.fastjson2.JSONObject groupJson = new com.alibaba.fastjson2.JSONObject();
                    groupJson.put("groupId", group.groupId);
                    
                    // 主表格信息
                    if (group.mainTable != null) {
                        com.alibaba.fastjson2.JSONObject mainTableJson = new com.alibaba.fastjson2.JSONObject();
                        mainTableJson.put("page", group.mainTable.pageIdx + 1);  // 转为1-based
                        mainTableJson.put("bbox", group.mainTable.bbox);
                        groupJson.put("mainTable", mainTableJson);
                    }
                    
                    // 跨页延续部分
                    com.alibaba.fastjson2.JSONArray contPartsArray = new com.alibaba.fastjson2.JSONArray();
                    for (var contPart : group.continuationParts) {
                        com.alibaba.fastjson2.JSONObject contPartJson = new com.alibaba.fastjson2.JSONObject();
                        contPartJson.put("page", contPart.pageIdx + 1);  // 转为1-based
                        contPartJson.put("bbox", contPart.bbox);
                        contPartsArray.add(contPartJson);
                    }
                    groupJson.put("continuationParts", contPartsArray);
                    
                    crossPageTablesArray.add(groupJson);
                }
                
                metadata.put("crossPageTables", crossPageTablesArray);
                log.info("序列化跨页表格信息，跨页表格数: {}", crossPageTablesArray.size());
            }
            
            result.setMetadata((Object) metadata);
            
            log.info("MinerU PDF识别完成，页数: {}, 文本长度: {}, 图片数: {}, TextBox数: {}", 
                pageLayouts.length, allText.length(), pageImagePaths.size(), textBoxes.size());
            log.info("TextBoxes序列化后长度: {}", textBoxesArray.toJSONString().length());
            
            // 输出前几个TextBox作为调试信息
            if (!textBoxes.isEmpty()) {
                log.info("前5个TextBox示例:");
                for (int i = 0; i < Math.min(5, textBoxes.size()); i++) {
                    TextBox tb = textBoxes.get(i);
                    log.info("  TextBox[{}]: page={}, text='{}', bbox=[{},{},{},{}], category={}", 
                        i, tb.page, tb.text.length() > 20 ? tb.text.substring(0, 20) + "..." : tb.text,
                        tb.bbox[0], tb.bbox[1], tb.bbox[2], tb.bbox[3], tb.category);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("MinerU OCR识别失败，文件: {}", pdfFile.getName(), e);
            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }
    }
    
    public String getProviderName() {
        return "OCR";  // 不暴露具体的OCR引擎名称
    }
    
    public boolean isAvailable() {
        return mineruOcrService != null;
    }

    /**
     * 增强OCR识别 - 返回详细位置信息
     * 支持智能信息提取的位置映射功能
     */
    public EnhancedOCRResult recognizePdfWithPositions(File pdfFile, String taskId) {
        return recognizePdfWithPositions(pdfFile, taskId, true, 12.0, 12.0);
    }
    
    /**
     * 增强OCR识别 - 返回详细位置信息（支持页眉页脚设置）
     * 支持智能信息提取的位置映射功能
     * 目录结构：rule-extract-data/ocr-output/{年月}/{任务id}/
     * 
     * @param pdfFile PDF文件
     * @param taskId 任务ID（带年月前缀）
     * @param ignoreHeaderFooter 是否忽略页眉页脚
     * @param headerHeightPercent 页眉高度百分比（默认12%）
     * @param footerHeightPercent 页脚高度百分比（默认12%）
     */
    public EnhancedOCRResult recognizePdfWithPositions(File pdfFile, String taskId, 
            boolean ignoreHeaderFooter, double headerHeightPercent, double footerHeightPercent) {
        log.info("MinerU OCR 开始增强识别PDF文件: {}, 任务ID: {}, 忽略页眉页脚: {}", 
            pdfFile.getAbsolutePath(), taskId, ignoreHeaderFooter);
        
        if (mineruOcrService == null) {
            throw new IllegalStateException("OCR 服务未启用，请检查配置");
        }
        
        try {
            // 创建输出目录（使用年月路径）
            // 提取原始任务ID和年月路径
            String originalTaskId = FileStorageUtils.extractOriginalId(taskId);
            String yearMonthPath = FileStorageUtils.getYearMonthPathFromFileId(taskId);
            
            // 构建输出目录：rule-extract-data/ocr-output/{年月}/{原始任务id}/
            File outputDir = new File(uploadRootPath, "rule-extract-data/ocr-output/" + yearMonthPath + "/" + originalTaskId);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 创建选项并设置页眉页脚参数
            CompareOptions options = new CompareOptions();
            options.setIgnoreHeaderFooter(ignoreHeaderFooter);
            options.setHeaderHeightPercent(headerHeightPercent);
            options.setFooterHeightPercent(footerHeightPercent);
            
            // 调用 MinerU 进行 PDF 识别
            MinerURecognitionResult mineruResult = mineruOcrService.recognizePdf(
                pdfFile, 
                taskId, 
                outputDir,
                "extract", // 文档模式：extract 表示用于智能提取
                options
            );
            
            // 从结果中提取 PageLayout 数组
            TextExtractionUtil.PageLayout[] pageLayouts = mineruResult.layouts;
            
            // 提取文本和 CharBox 数据
            StringBuilder allText = new StringBuilder();
            List<CharBox> charBoxes = new ArrayList<>();
            
            for (TextExtractionUtil.PageLayout layout : pageLayouts) {
                // 提取页面文本
                for (TextExtractionUtil.LayoutItem item : layout.items) {
                    if (item.text != null && !item.text.trim().isEmpty()) {
                        String text = item.text.trim();
                        allText.append(text).append("\n");
                        
                        // 将文本拆分为字符，创建 CharBox
                        for (int i = 0; i < text.length(); i++) {
                                    CharBox charBox = new CharBox(
                                layout.page,
                                text.charAt(i),
                                item.bbox != null ? item.bbox.clone() : new double[]{0, 0, 0, 0},
                                item.category != null ? item.category : "Text"
                                    );
                                    charBoxes.add(charBox);
                                }
                                
                        // 添加换行符
                                CharBox newlineCharBox = new CharBox(
                            layout.page,
                                    '\n',
                            item.bbox != null ? item.bbox.clone() : new double[]{0, 0, 0, 0},
                            item.category != null ? item.category : "Text"
                                );
                                charBoxes.add(newlineCharBox);
                    }
                }
            }  
            
            // 获取图片路径（MinerU 会在 outputDir 中生成页面图片）
            File imagesDir = new File(outputDir, "images");
            String imagesPath = imagesDir.getAbsolutePath();
            
            log.info("MinerU 增强识别完成: {}, 总页数: {}, 文本长度: {}, CharBox数量: {}", 
                pdfFile.getName(), pageLayouts.length, allText.length(), charBoxes.size());
            
            return new EnhancedOCRResult(
                allText.toString(),
                "mineru-enhanced",
                charBoxes,
                imagesPath,
                pageLayouts.length
            );
            
        } catch (Exception e) {
            log.error("MinerU 增强OCR识别失败: {}", pdfFile.getName(), e);
            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }
    }
}
