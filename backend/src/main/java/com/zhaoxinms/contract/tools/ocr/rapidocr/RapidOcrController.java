package com.zhaoxinms.contract.tools.ocr.rapidocr;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RapidOCR REST API控制器
 * 
 * 提供HTTP接口来调用RapidOCR服务
 */
@RestController
@RequestMapping("/api/rapidocr")
@ConditionalOnProperty(prefix = "rapidocr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RapidOcrController {

    private static final Logger logger = LoggerFactory.getLogger(RapidOcrController.class);

    @Autowired
    private RapidOcrService rapidOcrService;

    /**
     * 检查RapidOCR服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        boolean available = rapidOcrService.isServiceAvailable();
        
        response.put("service", "RapidOCR");
        response.put("status", available ? "UP" : "DOWN");
        response.put("info", rapidOcrService.getServiceInfo());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 通过文件上传进行OCR识别
     */
    @PostMapping(value = "/ocr/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ocrByFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "use_detection", defaultValue = "true") boolean useDetection,
            @RequestParam(value = "use_classification", defaultValue = "true") boolean useClassification,
            @RequestParam(value = "use_recognition", defaultValue = "true") boolean useRecognition,
            @RequestParam(value = "return_raw", defaultValue = "false") boolean returnRaw) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "上传的文件为空"));
            }

            // 保存临时文件
            Path tempFile = Files.createTempFile("rapidocr_", "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            try {
                Map<String, Object> response = new HashMap<>();
                
                if (returnRaw) {
                    // 返回原始JSON结果
                    JsonNode rawResult = rapidOcrService.recognizeFileRaw(
                            tempFile.toFile(), useDetection, useClassification, useRecognition);
                    response.put("result", rawResult);
                } else {
                    // 返回解析后的结果
                    List<RapidOcrClient.RapidOcrTextBox> textBoxes = rapidOcrService.recognizeFile(tempFile.toFile());
                    response.put("text_boxes", textBoxes);
                    response.put("text", rapidOcrService.convertToText(textBoxes));
                    response.put("count", textBoxes.size());
                }
                
                response.put("filename", file.getOriginalFilename());
                response.put("size", file.getSize());
                
                return ResponseEntity.ok(response);
                
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
            
        } catch (IOException e) {
            logger.error("OCR文件识别失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OCR识别失败: " + e.getMessage()));
        }
    }

    /**
     * 通过Base64数据进行OCR识别
     */
    @PostMapping("/ocr/data")
    public ResponseEntity<Map<String, Object>> ocrByData(@RequestBody RapidOcrDataRequest request) {
        try {
            if (request.image_data == null || request.image_data.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "image_data不能为空"));
            }

            // 解码Base64数据
            byte[] imageBytes;
            try {
                imageBytes = java.util.Base64.getDecoder().decode(request.image_data);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "无效的Base64数据"));
            }

            Map<String, Object> response = new HashMap<>();
            
            if (request.return_raw) {
                // 返回原始JSON结果
                JsonNode rawResult = rapidOcrService.recognizeBytesRaw(
                        imageBytes, request.use_detection, request.use_classification, request.use_recognition);
                response.put("result", rawResult);
            } else {
                // 返回解析后的结果
                List<RapidOcrClient.RapidOcrTextBox> textBoxes = rapidOcrService.recognizeBytes(imageBytes);
                response.put("text_boxes", textBoxes);
                response.put("text", rapidOcrService.convertToText(textBoxes));
                response.put("count", textBoxes.size());
            }
            
            response.put("data_size", imageBytes.length);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("OCR数据识别失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OCR识别失败: " + e.getMessage()));
        }
    }

    /**
     * OCR数据请求体
     */
    public static class RapidOcrDataRequest {
        public String image_data;
        public boolean use_detection = true;
        public boolean use_classification = true;
        public boolean use_recognition = true;
        public boolean return_raw = false;
    }
}
