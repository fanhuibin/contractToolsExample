package com.zhaoxinms.contract.tools.ocrcompare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OCR比对文件下载控制器
 */
@RestController
@RequestMapping("/api/ocr-compare/files")
public class OCRCompareFileController {
    
    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;
    
    /**
     * 下载标注后的旧文档PDF
     */
    @GetMapping("/{taskId}/old_annotated.pdf")
    public ResponseEntity<Resource> downloadOldAnnotatedPdf(@PathVariable String taskId) {
        return downloadAnnotatedPdf(taskId, "old_annotated.pdf");
    }
    
    /**
     * 下载标注后的新文档PDF
     */
    @GetMapping("/{taskId}/new_annotated.pdf")
    public ResponseEntity<Resource> downloadNewAnnotatedPdf(@PathVariable String taskId) {
        return downloadAnnotatedPdf(taskId, "new_annotated.pdf");
    }
    
    /**
     * 下载标注后的PDF文件
     */
    private ResponseEntity<Resource> downloadAnnotatedPdf(String taskId, String fileName) {
        try {
            // 构建文件路径
            Path filePath = Paths.get(uploadRootPath, "ocr-compare", "results", taskId, fileName);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建文件资源
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 下载原始旧文档
     */
    @GetMapping("/{taskId}/old_original")
    public ResponseEntity<Resource> downloadOldOriginal(@PathVariable String taskId) {
        return downloadOriginalFile(taskId, "old");
    }
    
    /**
     * 下载原始新文档
     */
    @GetMapping("/{taskId}/new_original")
    public ResponseEntity<Resource> downloadNewOriginal(@PathVariable String taskId) {
        return downloadOriginalFile(taskId, "new");
    }
    
    /**
     * 下载原始文件
     */
    private ResponseEntity<Resource> downloadOriginalFile(String taskId, String fileType) {
        try {
            // 构建文件路径
            Path filePath = Paths.get(uploadRootPath, "ocr_uploads", taskId + "_" + fileType);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建文件资源
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", file.getName());
            headers.setContentLength(file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{taskId}/info")
    public ResponseEntity<Object> getFileInfo(@PathVariable String taskId) {
        try {
            // 构建文件路径
            Path oldPdfPath = Paths.get(uploadRootPath, "ocr-compare", "results", taskId, "old_annotated.pdf");
            Path newPdfPath = Paths.get(uploadRootPath, "ocr-compare", "results", taskId, "new_annotated.pdf");
            
            File oldPdf = oldPdfPath.toFile();
            File newPdf = newPdfPath.toFile();
            
            java.util.Map<String, Object> fileInfo = new java.util.HashMap<>();
            fileInfo.put("taskId", taskId);
            fileInfo.put("oldAnnotatedPdf", oldPdf.exists() ? oldPdf.length() : -1);
            fileInfo.put("newAnnotatedPdf", newPdf.exists() ? newPdf.length() : -1);
            fileInfo.put("oldPdfUrl", "/api/ocr-compare/files/" + taskId + "/old_annotated.pdf");
            fileInfo.put("newPdfUrl", "/api/ocr-compare/files/" + taskId + "/new_annotated.pdf");
            
            return ResponseEntity.ok(fileInfo);
            
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "获取文件信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
