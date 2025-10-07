package com.zhaoxinms.contract.tools.extract.core;

import com.zhaoxinms.contract.tools.extract.core.alignment.TextAligner;
import com.zhaoxinms.contract.tools.extract.core.chunking.ChunkProcessor;
import com.zhaoxinms.contract.tools.extract.core.chunking.TextChunk;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;
import com.zhaoxinms.contract.tools.extract.core.format.FormatHandler;
import com.zhaoxinms.contract.tools.extract.core.overlap.OverlapDetector;
import com.zhaoxinms.contract.tools.extract.providers.LLMProvider;
import com.zhaoxinms.contract.tools.extract.prompting.PromptBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 文本提取引擎
 * 核心提取逻辑，对应Python版本的Annotator类
 */
@Slf4j
public class ExtractEngine {
    
    private final LLMProvider llmProvider;
    private final FormatHandler formatHandler;
    private final PromptBuilder promptBuilder;
    private final TextAligner textAligner;
    private final OverlapDetector overlapDetector;
    
    // 默认参数
    private static final String DEFAULT_FORMAT = "json";
    private static final Double DEFAULT_TEMPERATURE = 0.1; // 低温度确保一致性
    private static final Integer DEFAULT_MAX_TOKENS = 2000;
    private static final Double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;
    
    public ExtractEngine(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.formatHandler = new FormatHandler();
        this.promptBuilder = new PromptBuilder();
        this.textAligner = new TextAligner();
        this.overlapDetector = new OverlapDetector();
    }
    
    /**
     * 从单个文档中提取信息
     */
    public List<Extraction> extract(Document document, ExtractionSchema schema) throws ExtractException {
        return extract(document, schema, new ExtractionOptions());
    }
    
    /**
     * 从单个文档中提取信息（带选项）
     */
    public List<Extraction> extract(Document document, ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        if (document == null || document.isEmpty()) {
            throw new ExtractException("文档不能为空");
        }
        
        if (schema == null || !schema.isValid()) {
            throw new ExtractException("提取模式无效");
        }
        
        try {
            log.info("开始提取文档 {} 的信息，模式: {}", document.getId(), schema.getName());
            
            // 检查是否启用分块处理
            if (options.isEnableChunking()) {
                int maxCharBuffer = options.getMaxCharBuffer();
                if (ChunkProcessor.needsChunking(document, maxCharBuffer)) {
                    log.info("启用分块处理 - 文档长度 {} 超过缓冲区大小 {}，分割为多个块", 
                        document.getContent().length(), maxCharBuffer);
                    return extractWithChunking(document, schema, options);
                } else {
                    log.info("启用分块处理 - 但文档长度 {} 在缓冲区范围内，直接处理", 
                        document.getContent().length());
                    return extractDirect(document, schema, options);
                }
            } else {
                log.info("使用默认模式 - 直接处理整个文档（长度: {} 字符）", 
                    document.getContent().length());
                return extractDirect(document, schema, options);
            }
            
        } catch (Exception e) {
            log.error("提取失败: {}", e.getMessage());
            throw new ExtractException("提取失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 直接提取（不分块）
     */
    private List<Extraction> extractDirect(Document document, ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        // 构建提示
        String prompt = promptBuilder.buildExtractionPrompt(document, schema, options);
        String systemPrompt = promptBuilder.buildSystemPrompt(schema, options);
        
        log.debug("生成的提示长度: {}", prompt.length());
        
        // 调用LLM
        String response = llmProvider.chat(
            prompt, 
            systemPrompt, 
            options.getTemperature(), 
            options.getMaxTokens()
        );
        
        log.debug("LLM响应长度: {}", response.length());
        
        // 解析响应
        Map<String, Object> parsedResponse = formatHandler.parseResponse(response, options.getFormat());
        
        // 转换为Extraction对象
        List<Extraction> extractions = convertToExtractions(document, schema, parsedResponse, options);
        
        log.info("成功提取 {} 个字段", extractions.size());
        
        return extractions;
    }
    
    /**
     * 分块提取（支持并行处理）
     */
    private List<Extraction> extractWithChunking(Document document, ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        // 创建文本块
        List<TextChunk> chunks = ChunkProcessor.createChunks(document, options.getMaxCharBuffer());
        log.info("文档分割为 {} 个文本块，使用 {} 个并行工作线程", chunks.size(), options.getMaxWorkers());
        
        // 检查是否使用并行处理
        if (options.getMaxWorkers() > 1 && chunks.size() > 1) {
            return extractChunksInParallel(chunks, document, schema, options);
        } else {
            return extractChunksSequentially(chunks, document, schema, options);
        }
    }
    
    /**
     * 并行处理文本块
     */
    private List<Extraction> extractChunksInParallel(List<TextChunk> chunks, Document document, 
                                                    ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        
        int actualWorkers = Math.min(options.getMaxWorkers(), chunks.size());
        ExecutorService executor = Executors.newFixedThreadPool(actualWorkers);
        
        try {
            List<Future<List<Extraction>>> futures = new ArrayList<>();
            
            // 提交每个块的处理任务
            for (int i = 0; i < chunks.size(); i++) {
                final TextChunk chunk = chunks.get(i);
                final int chunkIndex = i;
                
                Future<List<Extraction>> future = executor.submit(() -> {
                    try {
                        log.debug("🔄 并行处理文本块 {}/{}: {}", chunkIndex + 1, chunks.size(), chunk.getChunkId());
                        
                        // 创建临时文档对象用于块处理
                        Document chunkDocument = Document.builder()
                            .id(chunk.getChunkId())
                            .content(chunk.getChunkText())
                            .type(document.getType())
                            .metadata(document.getMetadata())
                            .build();
                        
                        // 对文本块进行提取
                        List<Extraction> blockExtractions = extractDirect(chunkDocument, schema, options);
                        
                        log.debug("✅ 文本块 {} 提取到 {} 个结果", chunkIndex + 1, blockExtractions.size());
                        return blockExtractions;
                        
                    } catch (Exception e) {
                        log.warn("❌ 文本块 {} 处理失败: {}", chunkIndex + 1, e.getMessage());
                        return Collections.<Extraction>emptyList();
                    }
                });
                
                futures.add(future);
            }
            
            // 收集所有结果
            List<List<Extraction>> chunkExtractions = new ArrayList<>();
            for (Future<List<Extraction>> future : futures) {
                try {
                    chunkExtractions.add(future.get(60, TimeUnit.SECONDS)); // 60秒超时
                } catch (TimeoutException e) {
                    log.warn("文本块处理超时，使用空结果");
                    chunkExtractions.add(Collections.emptyList());
                    future.cancel(true);
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("文本块处理异常: {}", e.getMessage());
                    chunkExtractions.add(Collections.emptyList());
                }
            }
            
            log.info("🚀 并行提取完成，使用了 {} 个工作线程", actualWorkers);
            
            // 合并结果
            return mergeAndDeduplicateResults(chunks, chunkExtractions);
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 串行处理文本块（备用方案）
     */
    private List<Extraction> extractChunksSequentially(List<TextChunk> chunks, Document document, 
                                                      ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        
        log.info("📝 使用串行处理模式");
        List<List<Extraction>> chunkExtractions = new ArrayList<>();
        
        // 串行处理每个文本块
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            log.debug("处理文本块 {}/{}: {}", i + 1, chunks.size(), chunk);
            
            try {
                // 创建临时文档对象用于块处理
                Document chunkDocument = Document.builder()
                    .id(chunk.getChunkId())
                    .content(chunk.getChunkText())
                    .type(document.getType())
                    .metadata(document.getMetadata())
                    .build();
                
                // 对文本块进行提取
                List<Extraction> blockExtractions = extractDirect(chunkDocument, schema, options);
                chunkExtractions.add(blockExtractions);
                
                log.debug("文本块 {} 提取到 {} 个结果", i + 1, blockExtractions.size());
                
            } catch (Exception e) {
                log.warn("文本块 {} 处理失败: {}", i + 1, e.getMessage());
                chunkExtractions.add(Collections.emptyList());
            }
        }
        
        // 合并结果
        return mergeAndDeduplicateResults(chunks, chunkExtractions);
    }
    
    /**
     * 合并和去重结果
     */
    private List<Extraction> mergeAndDeduplicateResults(List<TextChunk> chunks, List<List<Extraction>> chunkExtractions) {
        // 合并所有块的提取结果
        List<Extraction> mergedExtractions = ChunkProcessor.mergeChunkExtractions(chunks, chunkExtractions);
        
        // 去重和重叠检测
        List<List<Extraction>> extractionPasses = Collections.singletonList(mergedExtractions);
        List<Extraction> finalExtractions = overlapDetector.mergeNonOverlappingExtractions(extractionPasses);
        
        log.info("🎯 分块提取完成，合并后获得 {} 个结果，去重后 {} 个结果", 
            mergedExtractions.size(), finalExtractions.size());
        
        return finalExtractions;
    }
    
    /**
     * 从多个文档中批量提取信息
     */
    public Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema) throws ExtractException {
        return extractBatch(documents, schema, new ExtractionOptions());
    }
    
    /**
     * 从多个文档中批量提取信息（带选项）
     */
    public Map<String, List<Extraction>> extractBatch(List<Document> documents, ExtractionSchema schema, ExtractionOptions options) throws ExtractException {
        Map<String, List<Extraction>> results = new LinkedHashMap<>();
        
        for (Document document : documents) {
            try {
                List<Extraction> extractions = extract(document, schema, options);
                results.put(document.getId(), extractions);
            } catch (ExtractException e) {
                log.error("文档 {} 提取失败: {}", document.getId(), e.getMessage());
                if (options.isFailFast()) {
                    throw e;
                } else {
                    results.put(document.getId(), Collections.emptyList());
                }
            }
        }
        
        return results;
    }
    
    /**
     * 将解析后的响应转换为Extraction对象
     */
    private List<Extraction> convertToExtractions(Document document, ExtractionSchema schema, 
                                                  Map<String, Object> parsedResponse, ExtractionOptions options) {
        List<Extraction> extractions = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        
        for (ExtractionSchema.FieldDefinition field : schema.getFields()) {
            String fieldName = field.getName();
            Object value = parsedResponse.get(fieldName);
            
            // 如果字段是必需的但值为空，使用默认值
            if (value == null && field.isRequired() && field.getDefaultValue() != null) {
                value = field.getDefaultValue();
            }
            
            // 跳过空值（除非是必需字段）
            if (value == null && !field.isRequired()) {
                continue;
            }
            
            // 执行字符级位置锚定
            String valueStr = value != null ? value.toString() : "";
            CharInterval charInterval = textAligner.findTextPosition(document.getContent(), valueStr);
            
            // 计算置信度 - 如果有字符位置锚定，使用对齐置信度；否则使用默认值
            double confidence = options.getConfidenceThreshold(); // 默认置信度
            if (charInterval != null && charInterval.isValid() && charInterval.getAlignmentConfidence() != null) {
                confidence = charInterval.getAlignmentConfidence(); // 使用文本对齐置信度
            }
            
            // 创建Extraction对象
            Extraction extraction = Extraction.builder()
                .id(UUID.randomUUID().toString())
                .documentId(document.getId())
                .field(fieldName)
                .value(value)
                .charInterval(charInterval)
                .confidence(confidence) // 使用计算出的置信度
                .method(llmProvider.getProviderName() + ":" + llmProvider.getCurrentModel())
                .createdAt(timestamp)
                .build();
            
            // 添加元数据
            extraction.addMetadata("schema", schema.getName());
            extraction.addMetadata("field_type", field.getType().getValue());
            extraction.addMetadata("required", field.isRequired());
            
            // 如果找到了字符位置，添加位置相关元数据
            if (charInterval != null && charInterval.isValid()) {
                extraction.addMetadata("position_found", true);
                extraction.addMetadata("alignment_confidence", charInterval.getAlignmentConfidence());
                extraction.addMetadata("text_length", charInterval.getLength());
                log.debug("字段 '{}' 找到位置: {}", fieldName, charInterval);
            } else {
                extraction.addMetadata("position_found", false);
                log.warn("字段 '{}' 未找到在原文中的位置: {}", fieldName, valueStr);
            }
            
            extractions.add(extraction);
        }
        
        // 过滤低置信度结果
        List<Extraction> filteredExtractions = extractions.stream()
            .filter(e -> e.isConfidentEnough(options.getConfidenceThreshold()))
            .collect(Collectors.toList());
        
        log.info("单次提取完成，获得 {} 个结果（过滤前 {}）", filteredExtractions.size(), extractions.size());
        
        return filteredExtractions;
    }
    
    /**
     * 多轮提取 - 实现LangExtract的Sequential Passes功能
     * 通过多次提取提高召回率，然后合并非重叠结果
     */
    public List<Extraction> extractWithMultiplePasses(Document document, ExtractionSchema schema, 
                                                     ExtractionOptions options, int passes) throws ExtractException {
        if (passes <= 1) {
            return extract(document, schema, options);
        }
        
        log.info("开始多轮提取，共 {} 轮", passes);
        
        List<List<Extraction>> allPassResults = new ArrayList<>();
        
        for (int pass = 1; pass <= passes; pass++) {
            log.debug("执行第 {} 轮提取", pass);
            
            try {
                List<Extraction> passResult = extract(document, schema, options);
                
                // 为每个结果添加轮次信息
                for (Extraction extraction : passResult) {
                    extraction.addMetadata("extraction_pass", pass);
                }
                
                allPassResults.add(passResult);
                
                log.debug("第 {} 轮提取完成，获得 {} 个结果", pass, passResult.size());
                
            } catch (Exception e) {
                log.warn("第 {} 轮提取失败: {}", pass, e.getMessage());
                if (options.isFailFast()) {
                    throw e;
                }
                // 继续下一轮
            }
        }
        
        // 合并非重叠结果
        List<Extraction> mergedResults = overlapDetector.mergeNonOverlappingExtractions(allPassResults);
        
        log.info("多轮提取完成，合并后获得 {} 个非重叠结果", mergedResults.size());
        
        return mergedResults;
    }
    
    /**
     * 提取选项配置类
     */
    public static class ExtractionOptions {
        private String format = DEFAULT_FORMAT;
        private Double temperature = DEFAULT_TEMPERATURE;
        private Integer maxTokens = DEFAULT_MAX_TOKENS;
        private Double confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
        private boolean failFast = false;
        private boolean enableChunking = false; // 默认关闭分块，直接处理整个文档
        private int maxCharBuffer = 1000; // 最大字符缓冲区大小（仅在启用分块时生效）
        private int maxWorkers = 4; // 并行工作线程数（仅在启用分块时生效）
        private int batchLength = 10; // 批处理长度（仅在启用分块时生效）
        private Map<String, Object> additionalParams = new HashMap<>();
        
        // Getters and Setters
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        
        public Double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        
        public boolean isFailFast() { return failFast; }
        public void setFailFast(boolean failFast) { this.failFast = failFast; }
        
        public boolean isEnableChunking() { return enableChunking; }
        public void setEnableChunking(boolean enableChunking) { this.enableChunking = enableChunking; }
        
        public int getMaxCharBuffer() { return maxCharBuffer; }
        public void setMaxCharBuffer(int maxCharBuffer) { this.maxCharBuffer = maxCharBuffer; }
        
        public int getMaxWorkers() { return maxWorkers; }
        public void setMaxWorkers(int maxWorkers) { this.maxWorkers = maxWorkers; }
        
        public int getBatchLength() { return batchLength; }
        public void setBatchLength(int batchLength) { this.batchLength = batchLength; }
        
        public Map<String, Object> getAdditionalParams() { return additionalParams; }
        public void setAdditionalParams(Map<String, Object> additionalParams) { this.additionalParams = additionalParams; }
        
        // 流式API
        public ExtractionOptions format(String format) { this.format = format; return this; }
        public ExtractionOptions temperature(Double temperature) { this.temperature = temperature; return this; }
        public ExtractionOptions maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public ExtractionOptions confidenceThreshold(Double threshold) { this.confidenceThreshold = threshold; return this; }
        public ExtractionOptions failFast(boolean failFast) { this.failFast = failFast; return this; }
        public ExtractionOptions enableChunking(boolean enableChunking) { this.enableChunking = enableChunking; return this; }
        public ExtractionOptions maxCharBuffer(int maxCharBuffer) { this.maxCharBuffer = maxCharBuffer; return this; }
        public ExtractionOptions maxWorkers(int maxWorkers) { this.maxWorkers = maxWorkers; return this; }
        public ExtractionOptions batchLength(int batchLength) { this.batchLength = batchLength; return this; }
        
        // 便利方法
        /**
         * 启用分块处理，使用默认设置
         */
        public ExtractionOptions enableChunking() { 
            this.enableChunking = true; 
            return this; 
        }
        
        /**
         * 启用分块处理并设置块大小
         */
        public ExtractionOptions enableChunking(int maxCharBuffer) { 
            this.enableChunking = true; 
            this.maxCharBuffer = maxCharBuffer;
            return this; 
        }
        
        /**
         * 启用分块处理并设置完整参数
         */
        public ExtractionOptions enableChunking(int maxCharBuffer, int maxWorkers) { 
            this.enableChunking = true; 
            this.maxCharBuffer = maxCharBuffer;
            this.maxWorkers = maxWorkers;
            return this; 
        }
        
        /**
         * 禁用分块处理，直接处理整个文档
         */
        public ExtractionOptions disableChunking() { 
            this.enableChunking = false; 
            return this; 
        }
    }
}
