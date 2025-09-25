package com.zhaoxinms.contract.tools.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/download")
public class FileDownloadController {

    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @GetMapping("/temp")
    public ResponseEntity<FileSystemResource> download(@RequestParam("path") String relativePath) {
        try {
            if (!StringUtils.hasText(relativePath)) {
                return ResponseEntity.notFound().build();
            }
            // 规范化相对路径，禁止 .. 越权
            String safe = relativePath.replace("\\", "/");
            while (safe.startsWith("/")) safe = safe.substring(1);
            if (safe.contains("..")) return ResponseEntity.status(403).build();

            Path root = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            Path target = root.resolve(safe).normalize();
            if (!target.startsWith(root)) {
                return ResponseEntity.status(403).build();
            }
            File file = target.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            String filename = file.getName();
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
            FileSystemResource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
