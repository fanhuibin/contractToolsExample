package com.zhaoxinms.contract.tools.ocr.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.common.ocr.OCRProvider;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.model.MinerURecognitionResult;
import com.zhaoxinms.contract.tools.comparePRO.service.MinerUOCRService;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.extract.model.CharBox;
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
        log.info("使用 MinerU OCR 识别PDF: {}", pdfFile.getName());
        
        if (mineruOcrService == null) {
            throw new RuntimeException("MinerU OCR 服务未启用，请检查配置");
        }
        
        try {
            // 创建持久化输出目录（保存在 rule-extract-data 下）
            String taskId = UUID.randomUUID().toString();
            String dataRoot = System.getProperty("user.dir") + File.separator + "rule-extract-data";
            File ocrOutputDir = new File(dataRoot, "ocr-output");
            if (!ocrOutputDir.exists()) {
                ocrOutputDir.mkdirs();
            }
            File taskOutputDir = new File(ocrOutputDir, taskId);
            taskOutputDir.mkdirs();
            
            // 创建默认选项
            CompareOptions options = new CompareOptions();
            
            // 调用 MinerU 进行 PDF 识别
            MinerURecognitionResult mineruResult = mineruOcrService.recognizePdf(
                pdfFile, 
                taskId, 
                taskOutputDir,
                "extract", // 文档模式：extract 表示用于智能提取
                options
            );
            
            // 从结果中提取 PageLayout 数组
            TextExtractionUtil.PageLayout[] pageLayouts = mineruResult.layouts;
            
            // 提取文本和CharBox数据
            StringBuilder allText = new StringBuilder();
            List<CharBox> charBoxes = new ArrayList<>();
            
            for (int i = 0; i < pageLayouts.length; i++) {
                TextExtractionUtil.PageLayout layout = pageLayouts[i];
                
                // 添加页面分隔符
                if (allText.length() > 0) {
                    String separator = "\n\n--- 第" + layout.page + "页 ---\n";
                    allText.append(separator);
                    
                    // 为分隔符的每个字符创建CharBox（保持索引对齐）
                    // 使用前一页或当前页的页码，bbox设为空（不会显示）
                    for (int j = 0; j < separator.length(); j++) {
                        CharBox separatorCharBox = new CharBox(
                            layout.page,  // 使用当前页
                            separator.charAt(j),
                            new double[]{0, 0, 0, 0},  // 空bbox，不会显示
                            "Separator"
                        );
                        charBoxes.add(separatorCharBox);
                    }
                }
                
                // 提取页面文本
                for (TextExtractionUtil.LayoutItem item : layout.items) {
                    if (item.text != null && !item.text.trim().isEmpty()) {
                        String text = item.text.trim();
                        allText.append(text).append("\n");
                        
                        // 创建字符级CharBox数据（用于精确位置标注）
                        for (int j = 0; j < text.length(); j++) {
                            CharBox charBox = new CharBox(
                                layout.page,
                                text.charAt(j),
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
            
            // 将图片路径和CharBox信息保存到metadata（作为JSONObject）
            com.alibaba.fastjson2.JSONObject metadata = new com.alibaba.fastjson2.JSONObject();
            metadata.put("totalPages", pageLayouts.length);
            metadata.put("pageImagePaths", pageImagePaths);
            metadata.put("imagesDir", imagesDir.getAbsolutePath());
            metadata.put("taskId", taskId);
            
            // 序列化CharBox数据
            metadata.put("charBoxes", com.alibaba.fastjson2.JSON.toJSONString(charBoxes));
            
            result.setMetadata((Object) metadata);
            
            log.info("MinerU PDF识别完成，页数: {}, 文本长度: {}, 图片数: {}, CharBox数: {}", 
                pageLayouts.length, allText.length(), pageImagePaths.size(), charBoxes.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("MinerU OCR识别失败，文件: {}", pdfFile.getName(), e);
            throw new RuntimeException("MinerU OCR识别失败: " + e.getMessage(), e);
        }
    }
    
    public String getProviderName() {
        return "mineru";
    }
    
    public boolean isAvailable() {
        return mineruOcrService != null;
    }

    /**
     * 增强OCR识别 - 返回详细位置信息
     * 支持智能信息提取的位置映射功能
     */
    public EnhancedOCRResult recognizePdfWithPositions(File pdfFile, String taskId) {
        log.info("MinerU OCR 开始增强识别PDF文件: {}, 任务ID: {}", pdfFile.getAbsolutePath(), taskId);
        
        if (mineruOcrService == null) {
            throw new IllegalStateException("MinerU OCR 服务未启用，请检查配置");
        }
        
        try {
            // 创建输出目录
            File outputDir = new File("uploads/extract-tasks/" + taskId);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 创建默认选项
            CompareOptions options = new CompareOptions();
            
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
            throw new RuntimeException("MinerU 增强OCR识别失败: " + e.getMessage(), e);
        }
    }
}
