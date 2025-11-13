package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 用于临时文件上传和下载
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    
    @Value("${zhaoxin.demo.backend-url:http://localhost:8091}")
    private String demoBackendUrl;
    
    // 文件存储目录
    private Path fileStorageLocation;
    
    @Value("${file.upload.path:/var/uploads}")
    private String uploadPath;
    
    public FileUploadController() {
        // 延迟初始化，在@PostConstruct中处理
    }
    
    @PostConstruct
    public void init() {
        // 使用配置文件中的路径
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("文件存储目录: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("无法创建文件存储目录", ex);
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("收到文件上传请求: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        // 文件校验
        if (file.isEmpty()) {
            return new ApiResponse<>(400, "文件为空", null);
        }
        
        // 文件大小校验（最大50MB）
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return new ApiResponse<>(400, "文件大小超过限制（最大50MB）", null);
        }
        
        // 文件类型校验 - 仅支持 PDF
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return new ApiResponse<>(400, "不支持的文件类型，仅支持 PDF 文档", null);
        }
        
        try {
            // 生成唯一文件名（UUID）
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // 保存文件
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成文件访问URL（对文件名进行URL编码）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            String fileUrl = demoBackendUrl + "/api/files/download/" + encodedFileName;
            
            log.info("文件上传成功: 原文件名={}, UUID文件名={}", originalFilename, fileName);
            log.info("文件URL: {}", fileUrl);
            
            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("originalName", originalFilename);
            result.put("fileUrl", fileUrl);
            result.put("fileSize", String.valueOf(file.getSize()));
            
            return new ApiResponse<>(200, "上传成功", result);
            
        } catch (IOException ex) {
            log.error("文件上传失败", ex);
            return new ApiResponse<>(500, "文件上传失败: " + ex.getMessage(), null);
        }
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        log.info("下载文件请求: {}", fileName);
        
        try {
            // 加载文件资源
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                log.error("文件不存在: {}", fileName);
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件类型
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                log.warn("无法确定文件类型");
            }
            
            // 默认文件类型
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            log.info("文件下载成功: {}, 类型: {}", fileName, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (MalformedURLException ex) {
            log.error("文件路径错误: {}", fileName, ex);
            return ResponseEntity.badRequest().build();
        }
    }
}

