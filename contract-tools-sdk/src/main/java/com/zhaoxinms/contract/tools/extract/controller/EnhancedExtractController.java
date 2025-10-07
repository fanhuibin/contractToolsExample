package com.zhaoxinms.contract.tools.extract.controller;

import com.zhaoxinms.contract.tools.extract.service.ContractExtractService;
import com.zhaoxinms.contract.tools.extract.model.EnhancedOCRResult;
import com.zhaoxinms.contract.tools.extract.model.CharBox;
import com.zhaoxinms.contract.tools.extract.util.PositionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强智能信息提取控制器
 * 支持位置映射和Canvas可视化功能
 */
@RestController
@RequestMapping("/api/extract/enhanced")
@Slf4j
public class EnhancedExtractController {

    @Autowired
    private ContractExtractService contractExtractService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;

    /**
     * 上传文件进行增强信息提取
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "schemaType", defaultValue = "contract") String schemaType,
            @RequestParam(value = "ocrProvider", defaultValue = "dotsocr") String ocrProvider) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("接收到增强提取请求，文件: {}, 模式: {}, OCR引擎: {}", 
                file.getOriginalFilename(), schemaType, ocrProvider);

            // 验证文件
            if (file.isEmpty()) {
                response.put("code", 400);
                response.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                response.put("code", 400);
                response.put("message", "只支持PDF格式文件");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建提取选项
            ContractExtractService.ExtractOptions options = new ContractExtractService.ExtractOptions();
            options.setSchemaType(schemaType);

            // 调用增强提取服务（使用现有的服务，它已经支持增强OCR）
            String taskId = contractExtractService.extractFromFile(file, options);

            response.put("code", 200);
            response.put("message", "文件上传成功，开始提取...");
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("增强提取上传失败", e);
            response.put("code", 500);
            response.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取增强提取结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Map<String, Object>> getEnhancedResult(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("获取增强提取结果，任务ID: {}", taskId);

            // 检查任务状态
            Map<String, Object> status = contractExtractService.getTaskStatus(taskId);
            if (status == null) {
                response.put("code", 404);
                response.put("message", "任务不存在");
                return ResponseEntity.notFound().build();
            }

            String taskStatus = (String) status.get("status");
            if (!"completed".equals(taskStatus)) {
                response.put("code", 202);
                response.put("message", "任务尚未完成");
                response.put("data", status);
                return ResponseEntity.accepted().body(response);
            }

            // 加载增强结果数据
            Map<String, Object> enhancedResult = loadEnhancedTaskData(taskId);
            
            response.put("code", 200);
            response.put("message", "获取结果成功");
            response.put("data", enhancedResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取增强提取结果失败，任务ID: {}", taskId, e);
            response.put("code", 500);
            response.put("message", "获取结果失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取任务的CharBox数据
     */
    @GetMapping("/charboxes/{taskId}")
    public ResponseEntity<Map<String, Object>> getCharBoxes(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            File charBoxFile = new File(taskDir, "char_boxes.json");

            if (!charBoxFile.exists()) {
                response.put("code", 404);
                response.put("message", "CharBox数据不存在");
                return ResponseEntity.notFound().build();
            }

            @SuppressWarnings("unchecked")
            List<CharBox> charBoxes = objectMapper.readValue(charBoxFile, List.class);

            response.put("code", 200);
            response.put("message", "获取CharBox数据成功");
            response.put("data", charBoxes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取CharBox数据失败，任务ID: {}", taskId, e);
            response.put("code", 500);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取任务的位置映射数据
     */
    @GetMapping("/bbox-mappings/{taskId}")
    public ResponseEntity<Map<String, Object>> getBboxMappings(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
            File bboxMappingFile = new File(taskDir, "bbox_mappings.json");

            if (!bboxMappingFile.exists()) {
                response.put("code", 404);
                response.put("message", "位置映射数据不存在");
                return ResponseEntity.notFound().build();
            }

            @SuppressWarnings("unchecked")
            List<PositionMapper.BboxMapping> bboxMappings = objectMapper.readValue(bboxMappingFile, List.class);

            response.put("code", 200);
            response.put("message", "获取位置映射数据成功");
            response.put("data", bboxMappings);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取位置映射数据失败，任务ID: {}", taskId, e);
            response.put("code", 500);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 加载增强任务数据
     */
    private Map<String, Object> loadEnhancedTaskData(String taskId) throws IOException {
        File taskDir = new File(uploadRootPath, "extract-tasks/" + taskId);
        
        Map<String, Object> result = new HashMap<>();
        
        // 加载提取结果
        File extractResultFile = new File(taskDir, "extract_result.json");
        if (extractResultFile.exists()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> extractResult = objectMapper.readValue(extractResultFile, Map.class);
            result.put("extractResult", extractResult);
        }
        
        // 加载OCR元数据
        File ocrMetaFile = new File(taskDir, "ocr_metadata.json");
        if (ocrMetaFile.exists()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ocrMetadata = objectMapper.readValue(ocrMetaFile, Map.class);
            result.put("ocrMetadata", ocrMetadata);
        }
        
        // 加载OCR结果
        File ocrResultFile = new File(taskDir, "ocr_result.txt");
        if (ocrResultFile.exists()) {
            String ocrContent = Files.readString(ocrResultFile.toPath());
            Map<String, Object> ocrResult = new HashMap<>();
            ocrResult.put("content", ocrContent);
            // 从元数据中获取其他信息
            if (result.containsKey("ocrMetadata")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) result.get("ocrMetadata");
                ocrResult.put("provider", metadata.get("provider"));
                ocrResult.put("totalPages", metadata.get("totalPages"));
            }
            result.put("ocrResult", ocrResult);
        }
        
        // 加载CharBox数据
        File charBoxFile = new File(taskDir, "char_boxes.json");
        if (charBoxFile.exists()) {
            @SuppressWarnings("unchecked")
            List<CharBox> charBoxes = objectMapper.readValue(charBoxFile, List.class);
            if (result.containsKey("ocrResult")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ocrResult = (Map<String, Object>) result.get("ocrResult");
                ocrResult.put("charBoxes", charBoxes);
            }
        }
        
        // 加载位置映射数据
        File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
        if (bboxMappingFile.exists()) {
            @SuppressWarnings("unchecked")
            List<PositionMapper.BboxMapping> bboxMappings = objectMapper.readValue(bboxMappingFile, List.class);
            result.put("bboxMappings", bboxMappings);
            result.put("hasPositionInfo", true);
        } else {
            result.put("bboxMappings", List.of());
            result.put("hasPositionInfo", false);
        }
        
        return result;
    }
}

