package com.zhaoxinms.contract.tools.extract.service;

import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 合同信息提取服务接口
 * 集成OCR识别和LangExtract信息提取功能
 */
public interface ContractExtractService {
    
    /**
     * 信息提取结果
     */
    class ExtractResult {
        private String taskId;
        private Document document;
        private ExtractionSchema schema;
        private List<Extraction> extractions;
        private String ocrProvider;
        private double ocrConfidence;
        private String htmlVisualization;
        private Map<String, Object> metadata;
        
        public ExtractResult(String taskId) {
            this.taskId = taskId;
        }
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Document getDocument() { return document; }
        public void setDocument(Document document) { this.document = document; }
        public ExtractionSchema getSchema() { return schema; }
        public void setSchema(ExtractionSchema schema) { this.schema = schema; }
        public List<Extraction> getExtractions() { return extractions; }
        public void setExtractions(List<Extraction> extractions) { this.extractions = extractions; }
        public String getOcrProvider() { return ocrProvider; }
        public void setOcrProvider(String ocrProvider) { this.ocrProvider = ocrProvider; }
        public double getOcrConfidence() { return ocrConfidence; }
        public void setOcrConfidence(double ocrConfidence) { this.ocrConfidence = ocrConfidence; }
        public String getHtmlVisualization() { return htmlVisualization; }
        public void setHtmlVisualization(String htmlVisualization) { this.htmlVisualization = htmlVisualization; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 提取选项
     */
    class ExtractOptions {
        private String schemaType = "contract"; // contract, invoice, resume, etc.
        private int extractionPasses = 3;
        private boolean enableChunking = false;
        private int maxCharBuffer = 2000;
        private boolean enableVisualization = true;
        private String llmProvider = "auto"; // auto, aliyun, ollama
        
        // Getters and setters
        public String getSchemaType() { return schemaType; }
        public void setSchemaType(String schemaType) { this.schemaType = schemaType; }
        public int getExtractionPasses() { return extractionPasses; }
        public void setExtractionPasses(int extractionPasses) { this.extractionPasses = extractionPasses; }
        public boolean isEnableChunking() { return enableChunking; }
        public void setEnableChunking(boolean enableChunking) { this.enableChunking = enableChunking; }
        public int getMaxCharBuffer() { return maxCharBuffer; }
        public void setMaxCharBuffer(int maxCharBuffer) { this.maxCharBuffer = maxCharBuffer; }
        public boolean isEnableVisualization() { return enableVisualization; }
        public void setEnableVisualization(boolean enableVisualization) { this.enableVisualization = enableVisualization; }
        public String getLlmProvider() { return llmProvider; }
        public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
    }
    
    /**
     * 从上传的文件提取信息
     * 
     * @param file 上传的PDF文件
     * @param options 提取选项
     * @return 提取任务ID
     */
    String extractFromFile(MultipartFile file, ExtractOptions options);
    
    /**
     * 从本地文件提取信息
     * 
     * @param file 本地PDF文件
     * @param options 提取选项
     * @return 提取任务ID
     */
    String extractFromLocalFile(File file, ExtractOptions options);
    
    /**
     * 从文本内容直接提取信息
     * 
     * @param text 文本内容
     * @param options 提取选项
     * @return 提取任务ID
     */
    String extractFromText(String text, ExtractOptions options);
    
    /**
     * 获取提取结果
     * 
     * @param taskId 任务ID
     * @return 提取结果，如果任务未完成返回null
     */
    ExtractResult getResult(String taskId);
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    Map<String, Object> getTaskStatus(String taskId);
    
    /**
     * 获取支持的提取模式列表
     * 
     * @return 支持的模式列表
     */
    List<String> getSupportedSchemas();
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(String taskId);
}
