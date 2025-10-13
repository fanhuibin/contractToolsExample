package com.zhaoxinms.contract.tools.extract.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.comparePRO.model.MinerURecognitionResult;
import com.zhaoxinms.contract.tools.comparePRO.service.MinerUOCRService;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.extract.config.ExtractProperties;
import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;  

import lombok.extern.slf4j.Slf4j;

/**
 * PDF 文档智能提取服务
 * 集成 MinerU OCR，支持从 PDF 直接提取结构化信息
 * 
 * @author zhaoxin
 */
@Slf4j
@Service
public class PdfExtractService {
    
    @Autowired(required = false)
    private MinerUOCRService minerUOCRService;
    
    private final ExtractEngine extractEngine;
    private final ExtractProperties properties;
    
    public PdfExtractService(ExtractEngine extractEngine, ExtractProperties properties) {
        this.extractEngine = extractEngine;
        this.properties = properties;
    }
    
    /**
     * 从 PDF 文件中提取结构化信息
     * 
     * @param pdfFile PDF 文件
     * @param schema 提取模式
     * @return 提取结果列表
     * @throws ExtractException 提取失败
     */
    public List<Extraction> extractFromPdf(File pdfFile, ExtractionSchema schema) throws ExtractException {
        return extractFromPdf(pdfFile, schema, createDefaultOptions());
    }
    
    /**
     * 从 PDF 文件中提取结构化信息（自定义选项）
     * 
     * @param pdfFile PDF 文件
     * @param schema 提取模式
     * @param options 提取选项
     * @return 提取结果列表
     * @throws ExtractException 提取失败
     */
    public List<Extraction> extractFromPdf(File pdfFile, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        checkMinerUAvailable();
        
        try {
            log.info("开始从 PDF 提取信息: {}", pdfFile.getName());
            
            // 1. 使用 MinerU OCR 识别 PDF
            log.debug("步骤 1/3: 使用 MinerU OCR 识别 PDF");
            String fullText = recognizePdfWithMinerU(pdfFile);
            
            if (fullText == null || fullText.trim().isEmpty()) {
                throw new ExtractException("OCR 识别失败：未能从 PDF 中提取文本");
            }
            
            log.info("OCR 识别完成，提取文本长度: {} 字符", fullText.length());
            
            // 2. 创建文档对象
            log.debug("步骤 2/3: 创建文档对象");
            Document document = createDocumentFromPdf(pdfFile, fullText);
            
            // 3. 使用提取引擎提取结构化信息
            log.debug("步骤 3/3: 提取结构化信息");
            List<Extraction> extractions = extractEngine.extract(document, schema, options);
            
            log.info("PDF 信息提取完成: {}, 提取了 {} 个结果", pdfFile.getName(), extractions.size());
            
            return extractions;
            
        } catch (ExtractException e) {
            throw e;
        } catch (Exception e) {
            log.error("从 PDF 提取信息失败: " + pdfFile.getName(), e);
            throw new ExtractException("从 PDF 提取信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 PDF 文件路径中提取结构化信息
     * 
     * @param pdfPath PDF 文件路径
     * @param schema 提取模式
     * @return 提取结果列表
     * @throws ExtractException 提取失败
     */
    public List<Extraction> extractFromPdfPath(String pdfPath, ExtractionSchema schema) throws ExtractException {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            throw new ExtractException("PDF 文件不存在: " + pdfPath);
        }
        return extractFromPdf(pdfFile, schema);
    }
    
    /**
     * 批量从 PDF 文件中提取信息
     * 
     * @param pdfFiles PDF 文件列表
     * @param schema 提取模式
     * @return 提取结果列表（所有文件的结果合并）
     * @throws ExtractException 提取失败
     */
    public List<Extraction> extractBatchFromPdfs(List<File> pdfFiles, ExtractionSchema schema) throws ExtractException {
        checkMinerUAvailable();
        
        List<Extraction> allExtractions = new ArrayList<>();
        
        for (File pdfFile : pdfFiles) {
            try {
                List<Extraction> extractions = extractFromPdf(pdfFile, schema);
                allExtractions.addAll(extractions);
            } catch (Exception e) {
                log.error("处理 PDF 文件失败: {}", pdfFile.getName(), e);
                if (properties.getDefaultOptions().isFailFast()) {
                    throw new ExtractException("批量提取失败: " + e.getMessage(), e);
                }
                // 如果不是快速失败模式，继续处理下一个文件
            }
        }
        
        return allExtractions;
    }
    
    /**
     * 检查 MinerU OCR 服务是否可用
     * 
     * @return true 如果可用
     */
    public boolean isMinerUAvailable() {
        return minerUOCRService != null;
    }
    
    /**
     * 使用 MinerU OCR 识别 PDF 并提取全文
     * 
     * @param pdfFile PDF 文件
     * @return 识别的文本内容
     * @throws Exception OCR 识别失败
     */
    private String recognizePdfWithMinerU(File pdfFile) throws Exception {
        // 创建临时输出目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), 
                                "extract-" + UUID.randomUUID().toString());
        tempDir.mkdirs();
        
        try {
            // 调用 MinerU OCR 服务
            String taskId = "extract-" + UUID.randomUUID().toString();
              
            MinerURecognitionResult result = minerUOCRService.recognizePdf(
                pdfFile,
                taskId,
                tempDir,
                "extract",  // 文档模式
                null        // 使用默认选项（不需要页眉页脚过滤）
            );
            
            // 从结果中提取 PageLayout 数组
            TextExtractionUtil.PageLayout[] pageLayouts = result.layouts;
            
            // 提取所有页面的文本并合并
            StringBuilder fullText = new StringBuilder();
            
            for (int i = 0; i < pageLayouts.length; i++) {
                TextExtractionUtil.PageLayout page = pageLayouts[i];
                
                if (page != null && page.items != null) {
                    // 提取页面文本
                    for (TextExtractionUtil.LayoutItem item : page.items) {
                        if (item != null && item.text != null && !item.text.isEmpty()) {
                            fullText.append(item.text);
                            // 根据内容类型添加适当的分隔符
                            if ("text".equals(item.category) || "title".equals(item.category)) {
                                fullText.append(" ");
                            }
                        }
                    }
                    
                    // 页面之间添加换行
                    if (i < pageLayouts.length - 1) {
                        fullText.append("\n\n");
                    }
                }
            }
            
            return fullText.toString();
            
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * 从 PDF 创建文档对象
     * 
     * @param pdfFile PDF 文件
     * @param content 提取的文本内容
     * @return 文档对象
     */
    private Document createDocumentFromPdf(File pdfFile, String content) {
        Document document = Document.builder()
            .id("pdf-" + UUID.randomUUID().toString())
            .content(content)
            .type("pdf")
            .language("zh")
            .createdAt(System.currentTimeMillis())
            .build();
        
        // 添加元数据
        document.addMetadata("filename", pdfFile.getName());
        document.addMetadata("filepath", pdfFile.getAbsolutePath());
        document.addMetadata("filesize", pdfFile.length());
        
        return document;
    }
    
    /**
     * 创建默认提取选项
     * 
     * @return 提取选项
     */
    private ExtractEngine.ExtractionOptions createDefaultOptions() {
        ExtractProperties.DefaultOptions defaultOpts = properties.getDefaultOptions();
        
        return new ExtractEngine.ExtractionOptions()
            .format(defaultOpts.getFormat())
            .temperature(defaultOpts.getTemperature())
            .maxTokens(defaultOpts.getMaxTokens())
            .confidenceThreshold(defaultOpts.getConfidenceThreshold())
            .failFast(defaultOpts.isFailFast());
    }
    
    /**
     * 检查 MinerU 是否可用
     * 
     * @throws ExtractException 如果不可用
     */
    private void checkMinerUAvailable() throws ExtractException {
        if (!isMinerUAvailable()) {
            throw new ExtractException(
                "MinerU OCR 服务未配置或不可用。" +
                "请确保 contract-tools-core 模块已正确配置 MinerUOCRService。"
            );
        }
    }
    
    /**
     * 递归删除目录
     * 
     * @param directory 要删除的目录
     */
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        
        directory.delete();
    }
}

