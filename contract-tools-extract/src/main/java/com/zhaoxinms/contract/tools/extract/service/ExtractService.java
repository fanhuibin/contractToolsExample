package com.zhaoxinms.contract.tools.extract.service;

import com.zhaoxinms.contract.tools.extract.config.ExtractProperties;
import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文本提取服务
 * 提供Spring Boot集成的服务层接口
 */
@Slf4j
@Service
public class ExtractService {
    
    private final ExtractEngine extractEngine;
    private final ExtractProperties properties;
    
    public ExtractService(ExtractEngine extractEngine, ExtractProperties properties) {
        this.extractEngine = extractEngine;
        this.properties = properties;
    }
    
    /**
     * 从文本中提取信息
     */
    public List<Extraction> extractFromText(String text, ExtractionSchema schema) throws ExtractException {
        Document document = createDocumentFromText(text);
        return extractEngine.extract(document, schema, createDefaultOptions());
    }
    
    /**
     * 从文本中提取信息（自定义选项）
     */
    public List<Extraction> extractFromText(String text, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        Document document = createDocumentFromText(text);
        return extractEngine.extract(document, schema, options);
    }
    
    /**
     * 从文档中提取信息
     */
    public List<Extraction> extractFromDocument(Document document, ExtractionSchema schema) throws ExtractException {
        return extractEngine.extract(document, schema, createDefaultOptions());
    }
    
    /**
     * 从文档中提取信息（自定义选项）
     */
    public List<Extraction> extractFromDocument(Document document, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        return extractEngine.extract(document, schema, options);
    }
    
    /**
     * 批量从文档中提取信息
     */
    public Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema) throws ExtractException {
        return extractEngine.extractBatch(documents, schema, createDefaultOptions());
    }
    
    /**
     * 批量从文档中提取信息（自定义选项）
     */
    public Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        return extractEngine.extractBatch(documents, schema, options);
    }
    
    /**
     * 检查提取服务是否可用
     */
    public boolean isAvailable() {
        try {
            // 简单测试提取功能
            String testText = "这是一个测试文本。";
            ExtractionSchema testSchema = ExtractionSchema.builder()
                .name("test")
                .build();
            
            testSchema.addField(ExtractionSchema.FieldDefinition.builder()
                .name("test_field")
                .type(ExtractionSchema.FieldType.STRING)
                .build());
            
            List<Extraction> result = extractFromText(testText, testSchema);
            return true;
            
        } catch (Exception e) {
            log.warn("提取服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建默认提取选项
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
     * 从文本创建文档对象
     */
    private Document createDocumentFromText(String text) {
        return Document.builder()
            .id("text-" + UUID.randomUUID().toString())
            .content(text)
            .type("text")
            .language("zh")
            .createdAt(System.currentTimeMillis())
            .build();
    }
}
