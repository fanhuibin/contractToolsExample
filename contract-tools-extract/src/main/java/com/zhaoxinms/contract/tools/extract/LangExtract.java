package com.zhaoxinms.contract.tools.extract;

import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;
import com.zhaoxinms.contract.tools.extract.providers.LLMProvider;
import com.zhaoxinms.contract.tools.extract.providers.aliyun.AliyunLLMProvider;
import com.zhaoxinms.contract.tools.extract.providers.ollama.OllamaLLMProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LangExtract主要API入口
 * 提供简化的静态方法，对应Python版本的extract函数
 */
public class LangExtract {
    
    private static LLMProvider defaultProvider;
    private static ExtractEngine defaultEngine;
    
    /**
     * 设置默认的LLM提供商
     */
    public static void setDefaultProvider(LLMProvider provider) {
        defaultProvider = provider;
        defaultEngine = new ExtractEngine(provider);
    }
    
    /**
     * 设置阿里云作为默认提供商
     */
    public static void setAliyunProvider(String apiKey) {
        setAliyunProvider(apiKey, "qwen-turbo");
    }
    
    /**
     * 设置阿里云作为默认提供商（指定模型）
     */
    public static void setAliyunProvider(String apiKey, String model) {
        AliyunLLMProvider provider = AliyunLLMProvider.builder()
            .apiKey(apiKey)
            .model(model)
            .build();
        setDefaultProvider(provider);
    }
    
    /**
     * 设置Ollama作为默认提供商
     */
    public static void setOllamaProvider(String baseUrl, String model) {
        OllamaLLMProvider provider = new OllamaLLMProvider(baseUrl, model);
        setDefaultProvider(provider); 
    }
    
    /**
     * 主要的提取方法 - 从单个文档提取信息
     */
    public static List<Extraction> extract(Document document, ExtractionSchema schema) throws ExtractException {
        ensureDefaultEngine();
        return defaultEngine.extract(document, schema);
    }
    
    /**
     * 主要的提取方法 - 从单个文档提取信息（带选项）
     */
    public static List<Extraction> extract(Document document, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        ensureDefaultEngine();
        return defaultEngine.extract(document, schema, options);
    }
    
    /**
     * 主要的提取方法 - 从多个文档批量提取信息
     */
    public static Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema) throws ExtractException {
        ensureDefaultEngine();
        return defaultEngine.extractBatch(documents, schema);
    }
    
    /**
     * 主要的提取方法 - 从多个文档批量提取信息（带选项）
     */
    public static Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        ensureDefaultEngine();
        return defaultEngine.extractBatch(documents, schema, options);
    }
    
    /**
     * 便捷方法 - 从文本字符串提取信息
     */
    public static List<Extraction> extract(String text, ExtractionSchema schema) throws ExtractException {
        Document document = Document.builder()
            .id("text-" + System.currentTimeMillis())
            .content(text)
            .createdAt(System.currentTimeMillis())
            .build();
        
        return extract(document, schema);
    }
    
    /**
     * 便捷方法 - 从文本字符串提取信息（带选项）
     */
    public static List<Extraction> extract(String text, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) throws ExtractException {
        Document document = Document.builder()
            .id("text-" + System.currentTimeMillis())
            .content(text)
            .createdAt(System.currentTimeMillis())
            .build();
        
        return extract(document, schema, options);
    }
    
    /**
     * 多轮提取 - 实现LangExtract的Sequential Passes功能
     * 通过多次提取提高召回率，然后合并非重叠结果
     */
    public static List<Extraction> extractWithMultiplePasses(String text, ExtractionSchema schema, int passes) 
            throws ExtractException {
        return extractWithMultiplePasses(text, schema, new ExtractEngine.ExtractionOptions(), passes);
    }
    
    /**
     * 多轮提取（高级选项）
     */
    public static List<Extraction> extractWithMultiplePasses(String text, ExtractionSchema schema, 
                                                           ExtractEngine.ExtractionOptions options, int passes) 
            throws ExtractException {
        if (defaultEngine == null) {
            throw new ExtractException("LangExtract未初始化，请先调用setAliyunProvider()设置API Key");
        }
        
        Document document = Document.builder()
            .id(UUID.randomUUID().toString())
            .content(text)
            .type("text")
            .createdAt(System.currentTimeMillis())
            .build();
        
        return defaultEngine.extractWithMultiplePasses(document, schema, options, passes);
    }
    
    /**
     * 创建新的提取引擎实例
     */
    public static ExtractEngine createEngine(LLMProvider provider) {
        return new ExtractEngine(provider);
    }
    
    /**
     * 创建阿里云提取引擎
     */
    public static ExtractEngine createAliyunEngine(String apiKey) {
        return createAliyunEngine(apiKey, "qwen-turbo");
    }
    
    /**
     * 创建阿里云提取引擎（指定模型）
     */
    public static ExtractEngine createAliyunEngine(String apiKey, String model) {
        AliyunLLMProvider provider = AliyunLLMProvider.builder()
            .apiKey(apiKey)
            .model(model)
            .build();
        return new ExtractEngine(provider);
    }
    
    /**
     * 获取当前默认提供商
     */
    public static LLMProvider getDefaultProvider() {
        return defaultProvider;
    }
    
    /**
     * 获取当前默认引擎
     */
    public static ExtractEngine getDefaultEngine() {
        ensureDefaultEngine();
        return defaultEngine;
    }
    
    /**
     * 确保默认引擎已初始化
     */
    private static void ensureDefaultEngine() {
        if (defaultEngine == null) {
            throw new IllegalStateException("默认LLM提供商未设置，请先调用 setDefaultProvider() 或 setAliyunProvider()");
        }
    }
}
