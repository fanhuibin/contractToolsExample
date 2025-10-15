package com.zhaoxinms.contract.tools.extract.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.extract.LangExtract;
import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.model.EnhancedOCRResult;
import com.zhaoxinms.contract.tools.extract.service.ContractExtractService;
import com.zhaoxinms.contract.tools.extract.util.PositionMapper;
import com.zhaoxinms.contract.tools.extract.utils.SchemaBuilder;
import com.zhaoxinms.contract.tools.extract.visualization.SimpleVisualization;
import com.zhaoxinms.contract.tools.ocr.service.OCRService;
import com.zhaoxinms.contract.tools.ocr.service.UnifiedOCRService;

import lombok.extern.slf4j.Slf4j;

/**
 * 合同信息提取服务实现
 * 集成OCR识别和LangExtract信息提取功能
 */
@Slf4j
@Service
public class ContractExtractServiceImpl implements ContractExtractService {
    
    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;
    
    @Value("${zhaoxin.extract.aliyun.api-key}")
    private String aliyunApiKey;
    
    @Value("${zhaoxin.extract.aliyun.model:qwen-plus}")
    private String aliyunModel;
    
    @Autowired
    private UnifiedOCRService unifiedOCRService;
    
    @Autowired
    private ObjectMapper objectMapper;
    

    // 异步任务执行器
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    // 任务状态存储
    private final Map<String, Map<String, Object>> taskStatus = new ConcurrentHashMap<>();
    
    // 任务结果存储
    private final Map<String, ExtractResult> taskResults = new ConcurrentHashMap<>();
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    
    @PostConstruct
    public void init() {
        // 初始化LangExtract
        if (aliyunApiKey != null && !aliyunApiKey.trim().isEmpty()) {
            log.info("初始化LangExtract阿里云提供者，模型: {}", aliyunModel);
            LangExtract.setAliyunProvider(aliyunApiKey, aliyunModel);
            log.info("LangExtract初始化完成");
        } else {
            log.error("阿里云API Key未配置，请设置 zhaoxin.extract.aliyun.api-key");
            throw new IllegalStateException("阿里云API Key未配置，LangExtract无法初始化");
        }
    }
    
    @Override
    public String extractFromFile(MultipartFile file, ExtractOptions options) {
        String taskId = generateTaskId();
        log.info("开始文件提取任务: {}, 文件名: {}, 大小: {} bytes", taskId, file.getOriginalFilename(), file.getSize());
        
        // 初始化任务状态
        initializeTaskStatus(taskId, "从上传文件提取", file.getOriginalFilename(), options.getSchemaType());
        
        try {
            // 立即保存上传文件，避免异步处理时临时文件被清理
            updateTaskStatus(taskId, "saving_file", "保存上传文件...", 10);
            File localFile = saveUploadedFile(file, taskId);
            log.info("文件已同步保存: {}", localFile.getAbsolutePath());
            
            // 异步执行提取任务
            CompletableFuture.runAsync(() -> {
                try {
                    // 执行提取
                    executeExtraction(taskId, localFile, options);
                    
                } catch (Exception e) {
                    log.error("文件提取任务失败: {}", taskId, e);
                    updateTaskStatus(taskId, "failed", "提取失败: " + e.getMessage(), 0);
                }
            }, executorService);
            
        } catch (Exception e) {
            log.error("保存上传文件失败: {}", taskId, e);
            updateTaskStatus(taskId, "failed", "保存文件失败: " + e.getMessage(), 0);
        }
        
        return taskId;
    }
    
    @Override
    public String extractFromLocalFile(File file, ExtractOptions options) {
        String taskId = generateTaskId();
        log.info("开始本地文件提取任务: {}, 文件: {}", taskId, file.getAbsolutePath());
        
        // 初始化任务状态
        initializeTaskStatus(taskId, "从本地文件提取", file.getName(), options.getSchemaType());
        
        // 异步执行提取任务
        CompletableFuture.runAsync(() -> {
            try {
                executeExtraction(taskId, file, options);
            } catch (Exception e) {
                log.error("本地文件提取任务失败: {}", taskId, e);
                updateTaskStatus(taskId, "failed", "提取失败: " + e.getMessage(), 0);
            }
        }, executorService);
        
        return taskId;
    }
    
    @Override
    public String extractFromText(String text, ExtractOptions options) {
        String taskId = generateTaskId();
        log.info("开始文本提取任务: {}, 文本长度: {} 字符", taskId, text.length());
        
        // 初始化任务状态
        initializeTaskStatus(taskId, "从文本内容提取", "直接文本输入", options.getSchemaType());
        
        // 异步执行提取任务
        CompletableFuture.runAsync(() -> {
            try {
                executeTextExtraction(taskId, text, options);
            } catch (Exception e) {
                log.error("文本提取任务失败: {}", taskId, e);
                updateTaskStatus(taskId, "failed", "提取失败: " + e.getMessage(), 0);
            }
        }, executorService);
        
        return taskId;
    }
    
    /**
     * 执行文件提取（包含OCR步骤）
     */
    private void executeExtraction(String taskId, File file, ExtractOptions options) throws Exception {
        // 1. 增强OCR识别（包含位置信息，支持页眉页脚过滤）
        updateTaskStatus(taskId, "ocr_processing", "正在进行OCR识别...", 30);
        EnhancedOCRResult enhancedOcrResult = unifiedOCRService.recognizePdfWithPositions(
            file, taskId, 
            options.isIgnoreHeaderFooter(),
            options.getHeaderHeightPercent(),
            options.getFooterHeightPercent()
        );
        log.info("统一OCR增强识别完成，任务: {}, 提供者: {}, 文本长度: {}, CharBox数量: {}, 忽略页眉页脚: {}", 
            taskId, enhancedOcrResult.getProvider(), enhancedOcrResult.getContent().length(), 
            enhancedOcrResult.getCharBoxes().size(), options.isIgnoreHeaderFooter());
        
        // 2. 创建文档对象
        updateTaskStatus(taskId, "creating_document", "创建文档对象...", 40);
        Document document = createDocumentFromEnhancedOCR(file, enhancedOcrResult);
        
        // 3. 执行信息提取
        executeDocumentExtraction(taskId, document, options, enhancedOcrResult);
    }
    
    /**
     * 执行文本提取（跳过OCR步骤）
     */
    private void executeTextExtraction(String taskId, String text, ExtractOptions options) throws Exception {
        // 1. 创建文档对象
        updateTaskStatus(taskId, "creating_document", "创建文档对象...", 30);
        Document document = createDocumentFromText(text, taskId);
        
        // 2. 执行信息提取
        OCRService.OCRResult mockOcrResult = new OCRService.OCRResult(text, 1.0, "direct_text");
        executeDocumentExtraction(taskId, document, options, mockOcrResult);
    }
    
    /**
     * 执行文档信息提取的核心逻辑（增强版本，包含位置映射）
     */
    private void executeDocumentExtraction(String taskId, Document document, ExtractOptions options, EnhancedOCRResult enhancedOcrResult) throws Exception {
        // 1. 获取提取模式
        updateTaskStatus(taskId, "loading_schema", "加载提取模式...", 50);
        ExtractionSchema schema = getExtractionSchema(options.getSchemaType());
        
        // 2. 配置LLM提供者
        updateTaskStatus(taskId, "configuring_llm", "配置语言模型...", 60);
        
        // 3. 执行信息提取
        updateTaskStatus(taskId, "extracting", "正在提取信息...", 70);
        List<Extraction> extractions = LangExtract.extractWithMultiplePasses(document.getContent(), schema, options.getExtractionPasses());
        
        // 创建ExtractResult
        ExtractResult result = new ExtractResult(taskId);
        result.setDocument(document);
        result.setExtractions(extractions);
        result.setSchema(schema);
        
        // 设置OCR相关信息（兼容老版本API）
        result.setOcrProvider(enhancedOcrResult.getProvider());
        result.setOcrConfidence(1.0); // 增强OCR没有置信度概念，设为1.0
        
        // 添加元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("extractionTime", LocalDateTime.now());
        metadata.put("schemaType", options.getSchemaType());
        metadata.put("extractionPasses", options.getExtractionPasses());
        metadata.put("chunkingEnabled", false);
        metadata.put("llmProvider", "aliyun");
        metadata.put("documentLength", document.getContent().length());
        metadata.put("extractionCount", extractions.size());
        metadata.put("enhancedOcr", true); // 标记为增强OCR
        metadata.put("totalPages", enhancedOcrResult.getTotalPages());
        metadata.put("charBoxCount", enhancedOcrResult.getCharBoxes().size());
        result.setMetadata(metadata);
        
        // 4. 位置映射 - 将提取结果映射到OCR位置
        updateTaskStatus(taskId, "position_mapping", "正在映射位置信息...", 80);
        List<PositionMapper.BboxMapping> bboxMappings = null;
        if (!enhancedOcrResult.getCharBoxes().isEmpty() && result.getExtractions() != null) {
            // 收集所有提取结果的字符区间
            List<CharInterval> allIntervals = new ArrayList<>();
            for (Extraction extraction : result.getExtractions()) {
                if (extraction.getCharInterval() != null) {
                    allIntervals.add(extraction.getCharInterval());
                }
            }
            
            // 执行位置映射
            bboxMappings = PositionMapper.mapIntervalsToBboxes(
                allIntervals, 
                enhancedOcrResult.getCharBoxes(), 
                enhancedOcrResult.getContent()
            );
            
            log.info("位置映射完成，任务: {}, 映射了 {} 个区间", taskId, bboxMappings.size());
        }
        
        // 5. 保存结果
        updateTaskStatus(taskId, "saving_results", "保存结果...", 90);
        saveEnhancedTaskResults(taskId, result, enhancedOcrResult, bboxMappings);
        
        // 6. 跳过HTML可视化生成（由前端负责）
        // generateVisualizationWithBbox(taskId, document, result, bboxMappings, enhancedOcrResult);
        
        // 7. 完成任务
        updateTaskStatus(taskId, "completed", "提取完成", 100);
        log.info("信息提取任务完成: {}", taskId);
    }

    /**
     * 执行文档信息提取的核心逻辑（原版本，兼容性保留）
     */
    private void executeDocumentExtraction(String taskId, Document document, ExtractOptions options, OCRService.OCRResult ocrResult) throws Exception {
        // 1. 获取提取模式
        updateTaskStatus(taskId, "loading_schema", "加载提取模式...", 50);
        ExtractionSchema schema = getExtractionSchema(options.getSchemaType());
        
        // 2. 配置LLM提供者
        updateTaskStatus(taskId, "configuring_llm", "配置LLM提供者...", 60);
        configureLLMProvider(options.getLlmProvider());
        
        // 3. 执行信息提取
        updateTaskStatus(taskId, "extracting", "正在提取信息...", 70);
        List<Extraction> extractions;
        
        if (options.isEnableChunking()) {
            log.info("启用分块模式进行提取，任务: {}", taskId);
            // 使用分块模式
            ExtractEngine.ExtractionOptions engineOptions = new ExtractEngine.ExtractionOptions()
                .enableChunking(options.getMaxCharBuffer())
                .confidenceThreshold(0.5);
            
            ExtractEngine engine = new ExtractEngine(LangExtract.getDefaultProvider());
            extractions = engine.extract(document, schema, engineOptions);
        } else {
            log.info("使用默认模式进行提取，任务: {}", taskId);
            // 使用默认模式（多轮提取）
            extractions = LangExtract.extractWithMultiplePasses(
                document.getContent(), 
                schema, 
                options.getExtractionPasses()
            );
        }
        
        log.info("信息提取完成，任务: {}, 提取到 {} 个字段", taskId, extractions.size());
        
        // 4. 生成可视化HTML（可选）
        String htmlVisualization = null;
        if (options.isEnableVisualization()) {
            updateTaskStatus(taskId, "generating_visualization", "生成可视化报告...", 85);
            htmlVisualization = SimpleVisualization.generateSimpleHTML(document, schema, extractions);
            log.info("可视化报告生成完成，任务: {}", taskId);
        }
        
        // 5. 创建结果对象
        updateTaskStatus(taskId, "finalizing", "整理结果...", 95);
        ExtractResult result = new ExtractResult(taskId);
        result.setDocument(document);
        result.setSchema(schema);
        result.setExtractions(extractions);
        result.setOcrProvider(ocrResult.getProvider());
        result.setOcrConfidence(ocrResult.getConfidence());
        result.setHtmlVisualization(htmlVisualization);
        
        // 添加元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("extractionTime", LocalDateTime.now());
        metadata.put("schemaType", options.getSchemaType());
        metadata.put("extractionPasses", options.getExtractionPasses());
        metadata.put("chunkingEnabled", options.isEnableChunking());
        metadata.put("llmProvider", options.getLlmProvider());
        metadata.put("documentLength", document.getContent().length());
        metadata.put("extractionCount", extractions.size());
        result.setMetadata(metadata);
        
        // 6. 保存结果并更新状态
        taskResults.put(taskId, result);
        
        // 保存任务数据到文件系统（参考合同比对功能）
        saveTaskDataToFile(taskId, result, ocrResult);
        
        updateTaskStatus(taskId, "completed", "提取完成", 100);
        
        log.info("提取任务完成: {}", taskId);
    }
    
    @Override
    public ExtractResult getResult(String taskId) {
        // 首先尝试从内存获取（老版本或刚完成的任务）
        ExtractResult memoryResult = taskResults.get(taskId);
        if (memoryResult != null) {
            return memoryResult;
        }
        
        // 如果内存中没有，尝试从文件系统加载（增强版任务）
        return loadResultFromFile(taskId);
    }
    
    @Override
    public Map<String, Object> getTaskStatus(String taskId) {
        Map<String, Object> status = taskStatus.get(taskId);
        if (status == null) {
            // 尝试从文件系统加载任务状态
            return loadTaskStatusFromFile(taskId);
        }
        return status;
    }
    
    @Override
    public List<String> getSupportedSchemas() {
        return Arrays.asList("contract", "invoice", "resume", "news", "general");
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        Map<String, Object> status = taskStatus.get(taskId);
        if (status != null && !"completed".equals(status.get("status")) && !"failed".equals(status.get("status"))) {
            updateTaskStatus(taskId, "cancelled", "任务已取消", 0);
            return true;
        }
        return false;
    }
    
    // ===== 辅助方法 =====
    
    private String generateTaskId() {
        return "extract_" + TIMESTAMP_FORMATTER.format(LocalDateTime.now()) + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
    
    private void initializeTaskStatus(String taskId, String description, String fileName, String schemaType) {
        Map<String, Object> status = new HashMap<>();
        status.put("taskId", taskId);
        status.put("status", "initializing");
        status.put("message", "初始化任务...");
        status.put("progress", 0);
        status.put("description", description);
        status.put("fileName", fileName);
        status.put("schemaType", schemaType != null ? schemaType : "unknown");
        status.put("createdAt", LocalDateTime.now());
        status.put("lastUpdated", LocalDateTime.now());
        taskStatus.put(taskId, status);
    }
    
    // 兼容性重载方法
    private void initializeTaskStatus(String taskId, String description, String fileName) {
        initializeTaskStatus(taskId, description, fileName, "unknown");
    }
    
    private void updateTaskStatus(String taskId, String status, String message, int progress) {
        Map<String, Object> statusMap = taskStatus.get(taskId);
        if (statusMap != null) {
            statusMap.put("status", status);
            statusMap.put("message", message);
            statusMap.put("progress", progress);
            statusMap.put("lastUpdated", LocalDateTime.now());
        }
        log.debug("任务状态更新: {} - {} ({}%)", taskId, message, progress);
    }
    
    private File saveUploadedFile(MultipartFile file, String taskId) throws IOException {
        // 创建保存目录
        File uploadDir = new File(uploadRootPath, "extract-tasks/" + taskId);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + uploadDir.getAbsolutePath());
            }
        }
        
        // 保存文件
        String originalFilename = file.getOriginalFilename();
        String fileName = originalFilename != null ? originalFilename : "uploaded_file.pdf";
        File savedFile = new File(uploadDir, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(savedFile)) {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                throw new IOException("上传的文件为空");
            }
            fos.write(fileBytes);
            fos.flush();
        }
        
        // 验证文件是否成功保存
        if (!savedFile.exists() || savedFile.length() == 0) {
            throw new IOException("文件保存失败或文件为空: " + savedFile.getAbsolutePath());
        }
        
        log.info("文件已保存: {} -> {} (大小: {} bytes)", originalFilename, savedFile.getAbsolutePath(), savedFile.length());
        return savedFile;
    }
    
    /**
     * 从增强OCR结果创建文档对象
     */
    private Document createDocumentFromEnhancedOCR(File file, EnhancedOCRResult enhancedOcrResult) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceFile", file.getAbsolutePath());
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("ocrProvider", enhancedOcrResult.getProvider());
        metadata.put("totalPages", enhancedOcrResult.getTotalPages());
        metadata.put("charBoxCount", enhancedOcrResult.getCharBoxes().size());
        if (enhancedOcrResult.getImagesPath() != null) {
            metadata.put("imagesPath", enhancedOcrResult.getImagesPath());
        }
        
        return Document.builder()
            .id("enhanced_" + System.currentTimeMillis())
            .content(enhancedOcrResult.getContent())
            .metadata(metadata)
            .source("enhanced_ocr")
            .type("pdf")
            .build();
    }

    private Document createDocumentFromOCR(File file, OCRService.OCRResult ocrResult) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceFile", file.getAbsolutePath());
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("ocrProvider", ocrResult.getProvider());
        metadata.put("ocrConfidence", ocrResult.getConfidence());
        metadata.put("processedAt", LocalDateTime.now());
        
        return Document.builder()
            .id("file_" + file.getName() + "_" + System.currentTimeMillis())
            .content(ocrResult.getContent())
            .type("contract")
            .metadata(metadata)
            .build();
    }
    
    private Document createDocumentFromText(String text, String taskId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "direct_text");
        metadata.put("taskId", taskId);
        metadata.put("textLength", text.length());
        metadata.put("processedAt", LocalDateTime.now());
        
        return Document.builder()
            .id("text_" + taskId)
            .content(text)
            .type("contract")
            .metadata(metadata)
            .build();
    }
    
    private ExtractionSchema getExtractionSchema(String schemaType) {
        switch (schemaType.toLowerCase()) {
            case "contract":
                return SchemaBuilder.createContractSchema();
            case "invoice":
                return SchemaBuilder.createInvoiceSchema();
            case "resume":
                return SchemaBuilder.createResumeSchema();
            case "news":
                return SchemaBuilder.createNewsSchema();
            case "general":
            default:
                return SchemaBuilder.createContractSchema(); // 默认使用合同模式
        }
    }
    
    private void configureLLMProvider(String provider) {
        // 根据配置选择LLM提供者
        // 这里可以扩展支持动态切换
        log.debug("配置LLM提供者: {}", provider);
        // 当前使用配置文件中的默认提供者
    }
    
    /**
     * 保存任务数据到文件系统
     * 参考合同比对功能的任务存储方式
     */
    private void saveTaskDataToFile(String taskId, ExtractResult result, OCRService.OCRResult ocrResult) {
        try {
            // 创建任务目录
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            if (!taskDir.exists()) {
                taskDir.mkdirs();
            }
            
            // 保存OCR结果
            File ocrResultFile = new File(taskDir, "ocr_result.txt");
            Files.write(ocrResultFile.toPath(), ocrResult.getContent().getBytes(StandardCharsets.UTF_8));
            
            // 保存OCR元数据
            File ocrMetaFile = new File(taskDir, "ocr_metadata.json");
            Map<String, Object> ocrMeta = new HashMap<>();
            ocrMeta.put("provider", ocrResult.getProvider());
            ocrMeta.put("confidence", ocrResult.getConfidence());
            ocrMeta.put("textLength", ocrResult.getContent().length());
            ocrMeta.put("processedAt", LocalDateTime.now().toString());
            if (ocrResult.getBlocks() != null) {
                ocrMeta.put("blockCount", ocrResult.getBlocks().size());
            }
            objectMapper.writeValue(ocrMetaFile, ocrMeta);
            
            // 保存提取结果
            File extractResultFile = new File(taskDir, "extract_result.json");
            Map<String, Object> extractData = new HashMap<>();
            extractData.put("taskId", taskId);
            extractData.put("schemaType", result.getSchema() != null ? result.getSchema().getName() : "unknown");
            extractData.put("extractionCount", result.getExtractions().size());
            extractData.put("metadata", result.getMetadata());
            extractData.put("extractions", result.getExtractions());
            objectMapper.writeValue(extractResultFile, extractData);
            
            // 保存HTML可视化（如果有）
            if (result.getHtmlVisualization() != null && !result.getHtmlVisualization().trim().isEmpty()) {
                File htmlFile = new File(taskDir, "visualization.html");
                Files.write(htmlFile.toPath(), result.getHtmlVisualization().getBytes(StandardCharsets.UTF_8));
            }
            
            // 保存任务状态
            File statusFile = new File(taskDir, "task_status.json");
            Map<String, Object> status = taskStatus.get(taskId);
            if (status != null) {
                objectMapper.writeValue(statusFile, status);
            }
            
            log.info("任务数据已保存到文件系统: {}", taskDir.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("保存任务数据失败: {}", taskId, e);
            // 不抛出异常，避免影响主要流程
        }
    }

    /**
     * 保存增强任务结果（包含位置信息）
     */
    private void saveEnhancedTaskResults(String taskId, ExtractResult result, EnhancedOCRResult enhancedOcrResult, 
                                        List<PositionMapper.BboxMapping> bboxMappings) {
        try {
            // 创建任务目录
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            if (!taskDir.exists()) {
                taskDir.mkdirs();
            }
            
            // 保存OCR结果
            File ocrResultFile = new File(taskDir, "ocr_result.txt");
            Files.write(ocrResultFile.toPath(), enhancedOcrResult.getContent().getBytes(StandardCharsets.UTF_8));
            
            // 保存增强OCR元数据
            File ocrMetaFile = new File(taskDir, "ocr_metadata.json");
            Map<String, Object> ocrMeta = new HashMap<>();
            ocrMeta.put("provider", enhancedOcrResult.getProvider());
            ocrMeta.put("textLength", enhancedOcrResult.getContent().length());
            ocrMeta.put("totalPages", enhancedOcrResult.getTotalPages());
            ocrMeta.put("charBoxCount", enhancedOcrResult.getCharBoxes().size());
            ocrMeta.put("processedAt", LocalDateTime.now().toString());
            if (enhancedOcrResult.getImagesPath() != null) {
                ocrMeta.put("imagesPath", enhancedOcrResult.getImagesPath());
            }
            objectMapper.writeValue(ocrMetaFile, ocrMeta);
            
            // 保存CharBox信息
            if (!enhancedOcrResult.getCharBoxes().isEmpty()) {
                File charBoxFile = new File(taskDir, "char_boxes.json");
                objectMapper.writeValue(charBoxFile, enhancedOcrResult.getCharBoxes());
            }
            
            // 保存位置映射信息
            if (bboxMappings != null && !bboxMappings.isEmpty()) {
                File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
                objectMapper.writeValue(bboxMappingFile, bboxMappings);
            }
            
            // 保存提取结果
            File extractResultFile = new File(taskDir, "extract_result.json");
            Map<String, Object> extractData = new HashMap<>();
            extractData.put("taskId", taskId);
            extractData.put("schemaType", result.getSchema() != null ? result.getSchema().getName() : "unknown");
            extractData.put("extractionCount", result.getExtractions().size());
            extractData.put("metadata", result.getMetadata());
            extractData.put("extractions", result.getExtractions());
            extractData.put("hasPositionInfo", bboxMappings != null && !bboxMappings.isEmpty());
            objectMapper.writeValue(extractResultFile, extractData);
            
            // 保存任务状态
            saveTaskStatus(taskId);
            
            log.info("增强任务结果保存完成: {}", taskId);
            
        } catch (Exception e) {
            log.error("保存增强任务数据失败: {}", taskId, e);
        }
    }

    /**
     * 生成包含位置信息的可视化
     */
    private void generateVisualizationWithBbox(String taskId, Document document, ExtractResult result, 
                                              List<PositionMapper.BboxMapping> bboxMappings, EnhancedOCRResult enhancedOcrResult) {
        try {
            // 生成增强的HTML可视化
            String enhancedHtml = generateEnhancedVisualization(document, result, bboxMappings, enhancedOcrResult);
            
            if (enhancedHtml != null && !enhancedHtml.trim().isEmpty()) {
                // 保存HTML文件
                File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
                File htmlFile = new File(taskDir, "visualization.html");
                Files.write(htmlFile.toPath(), enhancedHtml.getBytes(StandardCharsets.UTF_8));
                
                // 更新结果中的可视化内容
                result.setHtmlVisualization(enhancedHtml);
                
                log.info("增强可视化生成完成: {}", taskId);
            }
            
        } catch (Exception e) {
            log.error("生成增强可视化失败: {}", taskId, e);
        }
    }

    /**
     * 生成增强的HTML可视化（包含位置信息）
     */
    private String generateEnhancedVisualization(Document document, ExtractResult result, 
                                               List<PositionMapper.BboxMapping> bboxMappings, EnhancedOCRResult enhancedOcrResult) {
        try {
            // 基础可视化
            String baseHtml = SimpleVisualization.generateSimpleHTML(document, result.getSchema(), result.getExtractions());
            
            // 如果有位置信息，增强可视化
            if (bboxMappings != null && !bboxMappings.isEmpty()) {
                // TODO: 集成Canvas显示和位置标记
                // 这里暂时返回基础HTML，后续会实现完整的Canvas可视化
                return enhancedVisualizationWithCanvas(baseHtml, bboxMappings, enhancedOcrResult);
            }
            
            return baseHtml;
            
        } catch (Exception e) {
            log.error("生成增强可视化失败", e);
            return SimpleVisualization.generateSimpleHTML(document, result.getSchema(), result.getExtractions());
        }
    }

    /**
     * 生成带Canvas的增强可视化（占位方法）
     */
    private String enhancedVisualizationWithCanvas(String baseHtml, List<PositionMapper.BboxMapping> bboxMappings, 
                                                  EnhancedOCRResult enhancedOcrResult) {
        // TODO: 实现Canvas可视化
        // 1. 左侧Canvas显示PDF图片
        // 2. 在图片上标记bbox位置
        // 3. 右侧显示提取结果
        // 4. 实现点击交互
        
        StringBuilder enhanced = new StringBuilder();
        enhanced.append("<!DOCTYPE html>\n");
        enhanced.append("<html>\n<head>\n");
        enhanced.append("<title>智能信息提取结果 - 增强版</title>\n");
        enhanced.append("<meta charset='UTF-8'>\n");
        enhanced.append("<style>\n");
        enhanced.append(".container { display: flex; height: 100vh; }\n");
        enhanced.append(".left-panel { width: 50%; padding: 20px; overflow: auto; }\n");
        enhanced.append(".right-panel { width: 50%; padding: 20px; overflow: auto; border-left: 1px solid #ccc; }\n");
        enhanced.append(".bbox-highlight { background-color: rgba(255, 255, 0, 0.3); cursor: pointer; }\n");
        enhanced.append("</style>\n");
        enhanced.append("</head>\n<body>\n");
        enhanced.append("<div class='container'>\n");
        enhanced.append("  <div class='left-panel'>\n");
        enhanced.append("    <h3>文档内容（TODO: Canvas显示）</h3>\n");
        enhanced.append("    <p>位置映射数量: ").append(bboxMappings.size()).append("</p>\n");
        enhanced.append("    <p>总页数: ").append(enhancedOcrResult.getTotalPages()).append("</p>\n");
        enhanced.append("  </div>\n");
        enhanced.append("  <div class='right-panel'>\n");
        enhanced.append("    <h3>提取结果</h3>\n");
        enhanced.append(baseHtml);
        enhanced.append("  </div>\n");
        enhanced.append("</div>\n");
        enhanced.append("</body>\n</html>");
        
        return enhanced.toString();
    }

    private void saveTaskStatus(String taskId) {
        try {
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            File statusFile = new File(taskDir, "task_status.json");
            Map<String, Object> status = taskStatus.get(taskId);
            if (status != null) {
                objectMapper.writeValue(statusFile, status);
            }
        } catch (Exception e) {
            log.error("保存任务状态失败: {}", taskId, e);
        }
    }

    /**
     * 从文件系统加载任务结果
     */
    private ExtractResult loadResultFromFile(String taskId) {
        try {
            log.info("尝试从文件系统加载任务结果: {}", taskId);
            
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            log.info("任务目录路径: {}", taskDir.getAbsolutePath());
            
            // 检查任务目录是否存在
            if (!taskDir.exists()) {
                log.warn("任务目录不存在: {}", taskDir.getAbsolutePath());
                return null;
            }
            
            // 列出目录中的文件
            File[] files = taskDir.listFiles();
            if (files != null) {
                log.info("任务目录中的文件: {}", java.util.Arrays.toString(files));
            }
            
            // 检查任务状态 - 放宽检查条件，允许从文件系统恢复
            Map<String, Object> status = taskStatus.get(taskId);
            if (status != null) {
                log.info("内存中的任务状态: {}", status.get("status"));
            } else {
                log.info("内存中没有任务状态，尝试从文件加载");
                // 尝试从文件加载状态
                File statusFile = new File(taskDir, "task_status.json");
                if (statusFile.exists()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fileStatus = objectMapper.readValue(statusFile, Map.class);
                    log.info("从文件加载的任务状态: {}", fileStatus.get("status"));
                    if (!"completed".equals(fileStatus.get("status"))) {
                        log.warn("任务未完成: {}", fileStatus.get("status"));
                        return null;
                    }
                } else {
                    log.warn("状态文件不存在，但继续尝试加载结果");
                }
            }
            
            // 加载提取结果文件
            File extractResultFile = new File(taskDir, "extract_result.json");
            if (!extractResultFile.exists()) {
                log.warn("提取结果文件不存在: {}", extractResultFile.getAbsolutePath());
                return null;
            }
            
            log.info("找到提取结果文件: {}", extractResultFile.getAbsolutePath());
            
            // 解析提取结果
            @SuppressWarnings("unchecked")
            Map<String, Object> extractData = objectMapper.readValue(extractResultFile, Map.class);
            log.info("解析提取结果数据成功，包含字段: {}", extractData.keySet());
            
            // 重建ExtractResult对象
            ExtractResult result = reconstructExtractResult(taskId, extractData, taskDir);
            
            if (result != null) {
                log.info("从文件系统成功加载任务结果: {}", taskId);
            } else {
                log.error("重建ExtractResult对象失败: {}", taskId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("从文件系统加载任务结果失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 从保存的数据重构ExtractResult对象
     */
    @SuppressWarnings("unchecked")
    private ExtractResult reconstructExtractResult(String taskId, Map<String, Object> extractData, File taskDir) throws Exception {
        log.info("开始重构ExtractResult对象: {}", taskId);
        
        ExtractResult result = new ExtractResult(taskId);
        
        // 加载OCR元数据
        File ocrMetaFile = new File(taskDir, "ocr_metadata.json");
        Map<String, Object> ocrMetadata = null;
        if (ocrMetaFile.exists()) {
            log.info("加载OCR元数据文件: {}", ocrMetaFile.getAbsolutePath());
            ocrMetadata = objectMapper.readValue(ocrMetaFile, Map.class);
            log.info("OCR元数据: {}", ocrMetadata.keySet());
        } else {
            log.warn("OCR元数据文件不存在: {}", ocrMetaFile.getAbsolutePath());
        }
        
        // 加载OCR内容
        File ocrResultFile = new File(taskDir, "ocr_result.txt");
        String ocrContent = "";
        if (ocrResultFile.exists()) {
            log.info("加载OCR结果文件: {}", ocrResultFile.getAbsolutePath());
            ocrContent = Files.readString(ocrResultFile.toPath());
            log.info("OCR内容长度: {} 字符", ocrContent.length());
        } else {
            log.warn("OCR结果文件不存在: {}", ocrResultFile.getAbsolutePath());
        }
        
        // 重构Document对象
        Map<String, Object> documentMetadata = new HashMap<>();
        if (ocrMetadata != null) {
            documentMetadata.put("ocrProvider", ocrMetadata.get("provider"));
            documentMetadata.put("totalPages", ocrMetadata.get("totalPages"));
            documentMetadata.put("textLength", ocrMetadata.get("textLength"));
            documentMetadata.put("charBoxCount", ocrMetadata.get("charBoxCount"));
            documentMetadata.put("processedAt", ocrMetadata.get("processedAt"));
        }
        
        Document document = Document.builder()
            .id("loaded_" + taskId)
            .content(ocrContent)
            .metadata(documentMetadata)
            .source("file_system")
            .type("pdf")
            .build();
        
        result.setDocument(document);
        
        // 设置OCR信息
        if (ocrMetadata != null) {
            result.setOcrProvider((String) ocrMetadata.get("provider"));
            result.setOcrConfidence(1.0); // 增强OCR默认置信度
        }
        
        // 重构提取结果
        List<Object> extractionsData = (List<Object>) extractData.get("extractions");
        if (extractionsData != null) {
            List<Extraction> extractions = new ArrayList<>();
            for (Object extractionObj : extractionsData) {
                if (extractionObj instanceof Map) {
                    Map<String, Object> extractionMap = (Map<String, Object>) extractionObj;
                    Extraction extraction = reconstructExtraction(extractionMap);
                    if (extraction != null) {
                        extractions.add(extraction);
                    }
                }
            }
            result.setExtractions(extractions);
            log.info("重构了 {} 个提取项", extractions.size());
        }
        
        // 设置Schema - 从元数据中获取schemaType并重建
        String schemaType = (String) extractData.get("schemaType");
        if (schemaType != null) {
            log.info("重建Schema: {}", schemaType);
            try {
                ExtractionSchema schema = getExtractionSchema(schemaType);
                result.setSchema(schema);
            } catch (Exception e) {
                log.error("重建Schema失败: {}", schemaType, e);
                // 创建一个简单的Schema
                ExtractionSchema fallbackSchema = ExtractionSchema.builder()
                    .name(schemaType)
                    .description("从文件恢复的Schema")
                    .build();
                result.setSchema(fallbackSchema);
            }
        } else {
            log.warn("extractData中没有schemaType字段");
        }
        
        // 不再加载HTML可视化文件（由前端负责渲染）
        // result.setHtmlVisualization(null);
        
        // 设置元数据
        Map<String, Object> metadata = (Map<String, Object>) extractData.get("metadata");
        if (metadata != null) {
            result.setMetadata(metadata);
            log.info("设置元数据: {}", metadata.keySet());
        } else {
            result.setMetadata(new HashMap<>());
            log.warn("没有找到元数据");
        }
        
        log.info("ExtractResult重构完成: taskId={}, extractions={}, schema={}", 
            taskId, result.getExtractions() != null ? result.getExtractions().size() : 0, 
            result.getSchema() != null ? result.getSchema().getName() : "null");
        
        return result;
    }

    /**
     * 从Map数据重构Extraction对象
     */
    @SuppressWarnings("unchecked")
    private Extraction reconstructExtraction(Map<String, Object> extractionMap) {
        try {
            Extraction.ExtractionBuilder builder = Extraction.builder();
            
            // 基本字段
            if (extractionMap.containsKey("id")) {
                builder.id((String) extractionMap.get("id"));
            }
            if (extractionMap.containsKey("field")) {
                builder.field((String) extractionMap.get("field"));
            }
            if (extractionMap.containsKey("value")) {
                builder.value(extractionMap.get("value"));
            }
            if (extractionMap.containsKey("confidence")) {
                Object confidenceObj = extractionMap.get("confidence");
                if (confidenceObj instanceof Number) {
                    builder.confidence(((Number) confidenceObj).doubleValue());
                }
            }
            
            // 重构CharInterval
            if (extractionMap.containsKey("charInterval")) {
                Map<String, Object> intervalMap = (Map<String, Object>) extractionMap.get("charInterval");
                if (intervalMap != null) {
                    CharInterval charInterval = reconstructCharInterval(intervalMap);
                    builder.charInterval(charInterval);
                }
            }
            
            // 元数据
            if (extractionMap.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) extractionMap.get("metadata");
                builder.metadata(metadata != null ? metadata : new HashMap<>());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("重构Extraction对象失败", e);
            return null;
        }
    }

    /**
     * 从Map数据重构CharInterval对象
     */
    @SuppressWarnings("unchecked")
    private CharInterval reconstructCharInterval(Map<String, Object> intervalMap) {
        try {
            CharInterval.CharIntervalBuilder builder = CharInterval.builder();
            
            // 处理 startPos 字段（可能保存为 start 或 startPos）
            if (intervalMap.containsKey("startPos")) {
                Object startObj = intervalMap.get("startPos");
                if (startObj instanceof Number) {
                    builder.startPos(((Number) startObj).intValue());
                }
            } else if (intervalMap.containsKey("start")) {
                Object startObj = intervalMap.get("start");
                if (startObj instanceof Number) {
                    builder.startPos(((Number) startObj).intValue());
                }
            }
            
            // 处理 endPos 字段（可能保存为 end 或 endPos）
            if (intervalMap.containsKey("endPos")) {
                Object endObj = intervalMap.get("endPos");
                if (endObj instanceof Number) {
                    builder.endPos(((Number) endObj).intValue());
                }
            } else if (intervalMap.containsKey("end")) {
                Object endObj = intervalMap.get("end");
                if (endObj instanceof Number) {
                    builder.endPos(((Number) endObj).intValue());
                }
            }
            
            // 处理 sourceText 字段
            if (intervalMap.containsKey("sourceText")) {
                builder.sourceText((String) intervalMap.get("sourceText"));
            }
            
            // 处理 alignmentConfidence 字段
            if (intervalMap.containsKey("alignmentConfidence")) {
                Object confidenceObj = intervalMap.get("alignmentConfidence");
                if (confidenceObj instanceof Number) {
                    builder.alignmentConfidence(((Number) confidenceObj).doubleValue());
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("重构CharInterval对象失败", e);
            return null;
        }
    }
    
    /**
     * 从文件系统加载任务状态
     */
    private Map<String, Object> loadTaskStatusFromFile(String taskId) {
        try {
            File taskDir = new File("uploads/extract-tasks/" + taskId);
            File statusFile = new File(taskDir, "task_status.json");
            
            if (!statusFile.exists()) {
                // 如果任务状态文件不存在，但任务目录存在，说明是已完成的任务
                if (taskDir.exists()) {
                    // 创建一个默认的完成状态
                    Map<String, Object> status = new HashMap<>();
                    status.put("taskId", taskId);
                    status.put("status", "completed");
                    status.put("message", "提取完成");
                    status.put("progress", 100);
                    status.put("description", "合同信息提取");
                    
                    // 尝试从extract_result.json获取更多信息
                    File resultFile = new File(taskDir, "extract_result.json");
                    if (resultFile.exists()) {
                        try {
                            String resultJson = Files.readString(resultFile.toPath(), StandardCharsets.UTF_8);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> resultData = objectMapper.readValue(resultJson, Map.class);
                            status.put("schemaType", resultData.getOrDefault("schemaType", "unknown"));
                        } catch (Exception e) {
                            log.warn("读取提取结果信息失败: {}", e.getMessage());
                        }
                    }
                    
                    status.put("createdAt", LocalDateTime.now());
                    status.put("lastUpdated", LocalDateTime.now());
                    
                    return status;
                }
                return null;
            }
            
            // 从文件加载状态
            String statusJson = Files.readString(statusFile.toPath(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> status = objectMapper.readValue(statusJson, Map.class);
            
            log.debug("从文件加载任务状态: {}", taskId);
            return status;
            
        } catch (Exception e) {
            log.error("从文件加载任务状态失败: {}", taskId, e);
            return null;
        }
    }
}

