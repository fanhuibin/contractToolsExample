package com.zhaoxinms.contract.tools.ocrcompare.controller;

import org.springframework.beans.factory.annotation.Value;
import com.zhaoxinms.contract.tools.ocrcompare.config.GPUOCRConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GPU OCR文件控制器 - 专门处理GPU OCR的文件访问和下载
 */
@RestController
@RequestMapping("/api/gpu-ocr/files")
public class GPUFileController {

    @Autowired
    private GPUOCRConfig gpuOcrConfig;

    /**
     * 直接访问GPU OCR文件 - 支持调试模式的文件访问
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveGPUOCRFile(@PathVariable String filename) {
        try {
            // 构建文件路径 - 优先检查固定路径（用于调试模式）
            Path filePath = null;

            // 如果是调试模式使用的文件，检查配置路径
            if ("test1.pdf".equals(filename) || "test2.pdf".equals(filename)) {
                filePath = Paths.get(gpuOcrConfig.getDebugFilePath(), filename);
            } else if ("test1.annotated.pdf".equals(filename) || "test2.annotated.pdf".equals(filename)) {
                // 处理标注PDF文件
                String baseName = filename.replace(".annotated.pdf", ".pdf");
                Path baseFilePath = Paths.get(gpuOcrConfig.getDebugFilePath(), baseName);
                // 标注PDF文件的路径是原始PDF文件路径加上.annotated.pdf
                filePath = Paths.get(baseFilePath.toString() + ".annotated.pdf");
            } else {
                // 其他文件可以根据需要添加逻辑
                return ResponseEntity.notFound().build();
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            File file = filePath.toFile();

            // 创建文件资源
            Resource resource = new FileSystemResource(file);

            // 根据文件类型设置Content-Type
            String contentType = determineContentType(filename);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", filename); // inline表示在浏览器中显示，而不是下载
            headers.setContentLength(file.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件名确定Content-Type
     */
    private String determineContentType(String filename) {
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
}
