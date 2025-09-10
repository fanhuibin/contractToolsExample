package com.zhaoxinms.contract.tools.ocrcompare.controller;

import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GPU OCR文件下载控制器
 * 处理 /api/gpu-ocr/files 路径的文件下载请求
 */
@RestController
@RequestMapping("/api/gpu-ocr/files")
public class GPUOCRFileController {

    @Autowired
    private ZxcmConfig zxcmConfig;

    @GetMapping("/**")
    public ResponseEntity<FileSystemResource> downloadFile(HttpServletRequest request) {
        try {
            // 从请求URI中提取文件路径
            String requestURI = request.getRequestURI();
            String contextPath = request.getContextPath();
            
            // 移除context path和controller mapping
            String filePath = requestURI;
            if (contextPath != null && !contextPath.isEmpty()) {
                filePath = filePath.substring(contextPath.length());
            }
            filePath = filePath.substring("/api/gpu-ocr/files/".length());
            
            if (!StringUtils.hasText(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 规范化相对路径，禁止 .. 越权
            String safe = filePath.replace("\\", "/");
            while (safe.startsWith("/")) safe = safe.substring(1);
            if (safe.contains("..")) {
                System.err.println("检测到路径越权尝试: " + safe);
                return ResponseEntity.status(403).build();
            }

            // 构建完整文件路径
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path root = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            Path target = root.resolve(safe).normalize();
            
            // 安全检查：确保文件在上传根目录内
            if (!target.startsWith(root)) {
                System.err.println("文件路径超出允许范围: " + target);
                return ResponseEntity.status(403).build();
            }

            File file = target.toFile();
            if (!file.exists() || !file.isFile()) {
                System.err.println("文件不存在: " + target.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(file.length());
            
            // 设置文件名，支持中文
            String fileName = file.getName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
            headers.setContentDispositionFormData("inline", encodedFileName);

            System.out.println("提供文件下载: " + target.toAbsolutePath());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new FileSystemResource(file));

        } catch (Exception e) {
            System.err.println("文件下载失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
