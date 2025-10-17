package com.zhaoxinms.contract.tools.ocr.controller;

import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.ocr.service.OcrExtractService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * OCR提取控制器
 * 提供PDF文档OCR文本提取功能，支持页眉页脚忽略和bbox可视化
 */
@RestController
@RequestMapping("/api/ocr/extract")
@Slf4j
public class OcrExtractController {

    @Autowired
    private OcrExtractService ocrExtractService;

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    /**
     * 上传PDF文件进行OCR提取
     */
    @PostMapping("/upload")
    public ResponseEntity<Result<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ignoreHeaderFooter", defaultValue = "true") Boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightPercent", defaultValue = "12.0") Double headerHeightPercent,
            @RequestParam(value = "footerHeightPercent", defaultValue = "12.0") Double footerHeightPercent) {

        try {
            log.info("接收到OCR提取请求，文件: {}, 忽略页眉页脚: {}, 页眉高度: {}%, 页脚高度: {}%",
                    file.getOriginalFilename(), ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error("文件不能为空"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Result.error("只支持PDF格式文件"));
            }

            // 调用服务进行OCR提取
            String taskId = ocrExtractService.extractPdf(
                    file, 
                    ignoreHeaderFooter, 
                    headerHeightPercent, 
                    footerHeightPercent
            );

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("message", "文件上传成功，开始OCR提取...");

            return ResponseEntity.ok(Result.success(data));

        } catch (Exception e) {
            log.error("OCR提取上传失败", e);
            return ResponseEntity.status(500).body(Result.error("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Result<Map<String, Object>>> getTaskStatus(@PathVariable String taskId) {
        try {
            log.info("查询OCR提取任务状态，任务ID: {}", taskId);

            Map<String, Object> status = ocrExtractService.getTaskStatus(taskId);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Result.success(status));

        } catch (Exception e) {
            log.error("获取任务状态失败，任务ID: {}", taskId, e);
            return ResponseEntity.status(500).body(Result.error("获取状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取OCR提取结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Result<Map<String, Object>>> getResult(@PathVariable String taskId) {
        try {
            log.info("获取OCR提取结果，任务ID: {}", taskId);

            // 检查任务状态
            Map<String, Object> status = ocrExtractService.getTaskStatus(taskId);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            String taskStatus = (String) status.get("status");
            if (!"completed".equals(taskStatus)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", taskStatus);
                response.put("message", status.get("message"));
                response.put("progress", status.get("progress"));
                return ResponseEntity.ok(Result.success(response));
            }

            // 加载结果数据
            Map<String, Object> result = ocrExtractService.getTaskResult(taskId);

            return ResponseEntity.ok(Result.success(result));

        } catch (Exception e) {
            log.error("获取OCR提取结果失败，任务ID: {}", taskId, e);
            return ResponseEntity.status(500).body(Result.error("获取结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取页面图片
     */
    @GetMapping("/page-image/{taskId}/{pageNum}")
    public ResponseEntity<Resource> getPageImage(
            @PathVariable String taskId,
            @PathVariable int pageNum) {
        try {
            log.debug("获取页面图片，任务ID: {}, 页码: {}", taskId, pageNum);

            File imageFile = ocrExtractService.getPageImage(taskId, pageNum);
            if (imageFile == null || !imageFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(imageFile);
            
            // 根据文件扩展名设置Content-Type
            String contentType = MediaType.IMAGE_PNG_VALUE;
            if (imageFile.getName().toLowerCase().endsWith(".jpg") || 
                imageFile.getName().toLowerCase().endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageFile.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("获取页面图片失败，任务ID: {}, 页码: {}", taskId, pageNum, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取TextBox数据
     */
    @GetMapping("/textboxes/{taskId}")
    public ResponseEntity<Result<Object>> getTextBoxes(@PathVariable String taskId) {
        try {
            Object textBoxes = ocrExtractService.getTextBoxes(taskId);
            if (textBoxes == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Result.success(textBoxes));

        } catch (Exception e) {
            log.error("获取TextBox数据失败，任务ID: {}", taskId, e);
            return ResponseEntity.status(500).body(Result.error("获取数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Bbox映射数据（用于处理跨页表格等）
     */
    @GetMapping("/bbox-mappings/{taskId}")
    public ResponseEntity<Result<Object>> getBboxMappings(@PathVariable String taskId) {
        try {
            log.info("获取Bbox映射数据，任务ID: {}", taskId);
            
            File taskDir = new File(uploadRootPath, "ocr-extract-tasks/" + taskId);
            File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
            
            if (!bboxMappingFile.exists()) {
                log.warn("BboxMapping文件不存在: {}", bboxMappingFile.getAbsolutePath());
                return ResponseEntity.ok(Result.success(new java.util.ArrayList<>()));
            }
            
            // 读取BboxMapping数据
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object bboxMappingData = objectMapper.readValue(bboxMappingFile, Object.class);
            
            log.debug("成功获取BboxMapping数据: {}", taskId);
            return ResponseEntity.ok(Result.success(bboxMappingData));
            
        } catch (Exception e) {
            log.error("获取BboxMapping数据失败: {}", taskId, e);
            return ResponseEntity.status(500).body(Result.error("获取数据失败: " + e.getMessage()));
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Result<String>> deleteTask(@PathVariable String taskId) {
        try {
            log.info("删除OCR提取任务，任务ID: {}", taskId);

            ocrExtractService.deleteTask(taskId);

            return ResponseEntity.ok(Result.success("任务删除成功", null));

        } catch (Exception e) {
            log.error("删除任务失败，任务ID: {}", taskId, e);
            return ResponseEntity.status(500).body(Result.error("删除失败: " + e.getMessage()));
        }
    }
}

