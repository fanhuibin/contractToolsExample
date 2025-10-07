package com.zhaoxinms.contract.tools.extract.controller;

import com.zhaoxinms.contract.tools.extract.service.ContractExtractService;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 合同信息提取控制器
 * 基于LangExtract的智能信息提取API
 */
@Slf4j
@RestController
@RequestMapping("/api/extract")
public class ContractInfoExtractController {
    
    @Autowired
    private ContractExtractService extractService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 上传文件并提取信息
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> extractFromUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "schemaType", defaultValue = "contract") String schemaType,
            @RequestParam(value = "extractionPasses", defaultValue = "3") int extractionPasses,
            @RequestParam(value = "enableChunking", defaultValue = "false") boolean enableChunking,
            @RequestParam(value = "maxCharBuffer", defaultValue = "2000") int maxCharBuffer,
            @RequestParam(value = "enableVisualization", defaultValue = "true") boolean enableVisualization,
            @RequestParam(value = "llmProvider", defaultValue = "auto") String llmProvider) {
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".pdf") && 
                                           !originalFilename.toLowerCase().endsWith(".png") &&
                                           !originalFilename.toLowerCase().endsWith(".jpg") &&
                                           !originalFilename.toLowerCase().endsWith(".jpeg"))) {
                return Result.error("仅支持PDF、PNG、JPG、JPEG格式的文件");
            }
            
            log.info("收到文件提取请求: 文件名={}, 大小={} bytes, 模式={}", 
                originalFilename, file.getSize(), schemaType);
            
            // 创建提取选项
            ContractExtractService.ExtractOptions options = new ContractExtractService.ExtractOptions();
            options.setSchemaType(schemaType);
            options.setExtractionPasses(extractionPasses);
            options.setEnableChunking(enableChunking);
            options.setMaxCharBuffer(maxCharBuffer);
            options.setEnableVisualization(enableVisualization);
            options.setLlmProvider(llmProvider);
            
            // 启动提取任务
            String taskId = extractService.extractFromFile(file, options);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("message", "信息提取任务已启动");
            response.put("fileName", originalFilename);
            response.put("schemaType", schemaType);
            response.put("estimatedTime", estimateProcessingTime(file.getSize(), enableChunking));
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("文件提取失败", e);
            return Result.error("文件提取失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文本直接提取信息
     */
    @PostMapping("/text")
    public Result<Map<String, Object>> extractFromText(
            @RequestParam("text") String text,
            @RequestParam(value = "schemaType", defaultValue = "contract") String schemaType,
            @RequestParam(value = "extractionPasses", defaultValue = "3") int extractionPasses,
            @RequestParam(value = "enableChunking", defaultValue = "false") boolean enableChunking,
            @RequestParam(value = "maxCharBuffer", defaultValue = "2000") int maxCharBuffer,
            @RequestParam(value = "enableVisualization", defaultValue = "true") boolean enableVisualization,
            @RequestParam(value = "llmProvider", defaultValue = "auto") String llmProvider) {
        
        try {
            if (text == null || text.trim().isEmpty()) {
                return Result.error("文本内容不能为空");
            }
            
            if (text.length() > 500000) { // 限制500KB文本
                return Result.error("文本内容过长，请使用文件上传方式");
            }
            
            log.info("收到文本提取请求: 文本长度={} 字符, 模式={}", text.length(), schemaType);
            
            // 创建提取选项
            ContractExtractService.ExtractOptions options = new ContractExtractService.ExtractOptions();
            options.setSchemaType(schemaType);
            options.setExtractionPasses(extractionPasses);
            options.setEnableChunking(enableChunking);
            options.setMaxCharBuffer(maxCharBuffer);
            options.setEnableVisualization(enableVisualization);
            options.setLlmProvider(llmProvider);
            
            // 启动提取任务
            String taskId = extractService.extractFromText(text, options);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("message", "信息提取任务已启动");
            response.put("textLength", text.length());
            response.put("schemaType", schemaType);
            response.put("estimatedTime", estimateProcessingTime(text.length(), enableChunking));
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("文本提取失败", e);
            return Result.error("文本提取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable("taskId") String taskId) {
        try {
            Map<String, Object> status = extractService.getTaskStatus(taskId);
            if (status == null) {
                return Result.error("任务不存在: " + taskId);
            }
            
            return Result.success(status);
            
        } catch (Exception e) {
            log.error("获取任务状态失败: {}", taskId, e);
            return Result.error("获取任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取提取结果
     */
    @GetMapping("/result/{taskId}")
    public Result<Map<String, Object>> getExtractResult(@PathVariable("taskId") String taskId) {
        try {
            ContractExtractService.ExtractResult result = extractService.getResult(taskId);
            if (result == null) {
                return Result.error("结果不存在或任务未完成: " + taskId);
            }
            
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", result.getTaskId());
            response.put("document", buildDocumentInfo(result));
            response.put("extractions", buildExtractionsInfo(result));
            response.put("statistics", buildStatistics(result));
            response.put("metadata", result.getMetadata());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("获取提取结果失败: {}", taskId, e);
            return Result.error("获取提取结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可视化HTML
     */
    @GetMapping("/visualization/{taskId}")
    public void getVisualization(@PathVariable("taskId") String taskId, HttpServletResponse response) {
        try {
            ContractExtractService.ExtractResult result = extractService.getResult(taskId);
            if (result == null || result.getHtmlVisualization() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("可视化结果不存在或任务未完成: " + taskId);
                }
                return;
            }
            
            // 返回HTML内容
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            
            try (PrintWriter writer = response.getWriter()) {
                writer.write(result.getHtmlVisualization());
            }
            
        } catch (Exception e) {
            log.error("获取可视化结果失败: {}", taskId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("获取可视化结果失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }
    
    /**
     * 获取支持的提取模式
     */
    @GetMapping("/schemas")
    public Result<Map<String, Object>> getSupportedSchemas() {
        try {
            List<String> schemas = extractService.getSupportedSchemas();
            
            Map<String, Object> response = new HashMap<>();
            response.put("schemas", schemas);
            response.put("defaultSchema", "contract");
            response.put("descriptions", getSchemaDescriptions());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("获取支持的模式失败", e);
            return Result.error("获取支持的模式失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskId}")
    public Result<Map<String, Object>> cancelTask(@PathVariable("taskId") String taskId) {
        try {
            boolean cancelled = extractService.cancelTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("cancelled", cancelled);
            response.put("message", cancelled ? "任务已取消" : "任务无法取消（可能已完成或不存在）");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("取消任务失败: {}", taskId, e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }
    
    // ===== 辅助方法 =====
    
    private int estimateProcessingTime(long fileSize, boolean enableChunking) {
        // 简单的处理时间估算（秒）
        int baseTime = enableChunking ? 30 : 10; // 分块模式需要更多时间
        int sizeMultiplier = (int) (fileSize / 1024 / 1024); // MB
        return Math.max(baseTime + sizeMultiplier * 5, 5); // 最少5秒
    }
    
    private Map<String, Object> buildDocumentInfo(ContractExtractService.ExtractResult result) {
        Map<String, Object> docInfo = new HashMap<>();
        docInfo.put("id", result.getDocument().getId());
        docInfo.put("type", result.getDocument().getType());
        docInfo.put("contentLength", result.getDocument().getContent().length());
        docInfo.put("ocrProvider", result.getOcrProvider());
        docInfo.put("ocrConfidence", result.getOcrConfidence());
        return docInfo;
    }
    
    private Map<String, Object> buildExtractionsInfo(ContractExtractService.ExtractResult result) {
        Map<String, Object> extractionsInfo = new HashMap<>();
        extractionsInfo.put("count", result.getExtractions().size());
        extractionsInfo.put("items", result.getExtractions());
        extractionsInfo.put("schema", result.getSchema().getName());
        return extractionsInfo;
    }
    
    private Map<String, Object> buildStatistics(ContractExtractService.ExtractResult result) {
        Map<String, Object> stats = new HashMap<>();
        
        long withPositionCount = result.getExtractions().stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .count();
        
        long highConfidenceCount = result.getExtractions().stream()
            .filter(e -> e.isConfidentEnough(0.8))
            .count();
        
        double avgConfidence = result.getExtractions().stream()
            .mapToDouble(e -> e.getConfidence() != null ? e.getConfidence() : 0.0)
            .average().orElse(0.0);
        
        stats.put("totalFields", result.getExtractions().size());
        stats.put("positionedFields", withPositionCount);
        stats.put("highConfidenceFields", highConfidenceCount);
        stats.put("averageConfidence", avgConfidence);
        stats.put("positionAccuracy", result.getExtractions().isEmpty() ? 0 : (double) withPositionCount / result.getExtractions().size());
        
        return stats;
    }
    
    private Map<String, String> getSchemaDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("contract", "合同信息提取 - 提取甲乙方、金额、日期等信息");
        descriptions.put("invoice", "发票信息提取 - 提取开票单位、金额、税额等信息");
        descriptions.put("resume", "简历信息提取 - 提取姓名、联系方式、工作经历等信息");
        descriptions.put("news", "新闻信息提取 - 提取标题、时间、地点、人物等信息");
        descriptions.put("general", "通用信息提取 - 适用于各类文档的基础信息提取");
        return descriptions;
    }
    
    /**
     * 访问任务文件（图像等）
     * 用于前端Canvas模式显示PDF页面图像
     */
    @GetMapping("/files/tasks/{taskId}/images/{fileName}")
    public void getTaskFile(
            @PathVariable String taskId,
            @PathVariable String fileName,
            HttpServletResponse response) throws IOException {
        
        log.debug("访问任务文件: {} - {}", taskId, fileName);
        
        try {
            // 构建文件路径
            File taskDir = new File("uploads/extract-tasks/" + taskId);
            File imagesDir = new File(taskDir, "images");
            File requestedFile = new File(imagesDir, fileName);
            
            // 安全检查：确保文件在允许的目录内
            if (!requestedFile.getCanonicalPath().startsWith(imagesDir.getCanonicalPath())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            // 检查文件是否存在
            if (!requestedFile.exists() || !requestedFile.isFile()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 设置响应头
            String contentType = getContentType(fileName);
            response.setContentType(contentType);
            response.setContentLength((int) requestedFile.length());
            
            // 设置缓存头
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setDateHeader("Expires", System.currentTimeMillis() + 3600 * 1000);
            
            // 输出文件内容
            try (FileInputStream fis = new FileInputStream(requestedFile);
                 OutputStream os = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
            log.debug("成功返回任务文件: {} - {}", taskId, fileName);
            
        } catch (Exception e) {
            log.error("访问任务文件失败: {} - {}", taskId, fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 获取任务的CharBox数据
     */
    @GetMapping("/charboxes/{taskId}")
    public Result<Object> getTaskCharBoxes(@PathVariable String taskId) {
        try {
            File taskDir = new File("uploads/extract-tasks/" + taskId);
            File charBoxFile = new File(taskDir, "char_boxes.json");
            
            if (!charBoxFile.exists()) {
                log.warn("CharBox文件不存在: {}", charBoxFile.getAbsolutePath());
                return Result.success(new ArrayList<>());
            }
            
            // 读取CharBox数据
            String charBoxJson = Files.readString(charBoxFile.toPath(), StandardCharsets.UTF_8);
            Object charBoxData = objectMapper.readValue(charBoxJson, Object.class);
            
            log.debug("成功获取CharBox数据: {}", taskId);
            return Result.success(charBoxData);
            
        } catch (Exception e) {
            log.error("获取CharBox数据失败: {}", taskId, e);
            return Result.error("获取CharBox数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务的位置映射数据
     */
    @GetMapping("/bbox-mappings/{taskId}")
    public Result<Object> getTaskBboxMappings(@PathVariable String taskId) {
        try {
            File taskDir = new File("uploads/extract-tasks/" + taskId);
            File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
            
            if (!bboxMappingFile.exists()) {
                log.warn("BboxMapping文件不存在: {}", bboxMappingFile.getAbsolutePath());
                return Result.success(new ArrayList<>());
            }
            
            // 读取BboxMapping数据
            String bboxMappingJson = Files.readString(bboxMappingFile.toPath(), StandardCharsets.UTF_8);
            Object bboxMappingData = objectMapper.readValue(bboxMappingJson, Object.class);
            
            log.debug("成功获取BboxMapping数据: {}", taskId);
            return Result.success(bboxMappingData);
            
        } catch (Exception e) {
            log.error("获取BboxMapping数据失败: {}", taskId, e);
            return Result.error("获取BboxMapping数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "html":
                return "text/html";
            default:
                return "application/octet-stream";
        }
    }
}
