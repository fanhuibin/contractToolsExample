package com.zhaoxinms.contract.tools.ocr.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
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
public class UnifiedOCRService implements OCRService {
    
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
    public OCRResult recognizePdf(File pdfFile) {
        log.info("使用 MinerU OCR 识别PDF: {}", pdfFile.getName());
        
        if (mineruOcrService == null) {
            throw new RuntimeException("MinerU OCR 服务未启用，请检查配置");
        }
        
        try {
            // 创建临时输出目录
            String taskId = UUID.randomUUID().toString();
            File tempOutputDir = Files.createTempDirectory("mineru-ocr-" + taskId).toFile();
            
            // 创建默认选项
            CompareOptions options = new CompareOptions();
            
            // 调用 MinerU 进行 PDF 识别
            TextExtractionUtil.PageLayout[] pageLayouts = mineruOcrService.recognizePdf(
                pdfFile, 
                taskId, 
                tempOutputDir,
                "extract", // 文档模式：extract 表示用于智能提取
                options
            );
            
            // 提取文本
            StringBuilder allText = new StringBuilder();
            List<OCRBlock> allBlocks = new ArrayList<>();
            
            for (int i = 0; i < pageLayouts.length; i++) {
                TextExtractionUtil.PageLayout layout = pageLayouts[i];
                
                    if (allText.length() > 0) {
                    allText.append("\n\n--- 第").append(layout.page).append("页 ---\n");
                }
                
                // 提取页面文本
                for (TextExtractionUtil.LayoutItem item : layout.items) {
                    if (item.text != null && !item.text.trim().isEmpty()) {
                        allText.append(item.text).append("\n");
                        
                        // 创建文本块
                        OCRBlock block = new OCRBlock(
                            item.text, 
                            0.95, // MinerU 质量很高
                            (int)item.bbox[0], 
                            (int)item.bbox[1], 
                            (int)item.bbox[2], 
                            (int)item.bbox[3]
                        );
                        allBlocks.add(block);
                    }
                }
            }
            
            OCRResult result = new OCRResult(allText.toString(), 0.95, "mineru");
            result.setBlocks(allBlocks);
            
            log.info("MinerU PDF识别完成，页数: {}, 文本长度: {}", pageLayouts.length, allText.length());
            
            // 清理临时目录（可选）
            // 如果需要保留图片供后续使用，则不删除
            
            return result;
            
        } catch (Exception e) {
            log.error("MinerU OCR识别失败，文件: {}", pdfFile.getName(), e);
            throw new RuntimeException("MinerU OCR识别失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public OCRResult recognizeImage(File imageFile) {
        log.info("MinerU OCR 暂不支持单独图片识别，建议将图片转为 PDF: {}", imageFile.getName());
        throw new UnsupportedOperationException("MinerU OCR 暂不支持单独图片识别，请将图片转换为 PDF 后再识别");
    }
    
    @Override
    public String getProviderName() {
        return "mineru";
    }
    
    @Override
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
            TextExtractionUtil.PageLayout[] pageLayouts = mineruOcrService.recognizePdf(
                pdfFile, 
                taskId, 
                outputDir,
                "extract", // 文档模式：extract 表示用于智能提取
                options
            );
            
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
