package com.zhaoxinms.contract.tools.comparePRO.controller;

import org.springframework.beans.factory.annotation.Value;
import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
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
 * 高级合同比对文件控制器 - 专门处理文件访问和下载
 */
@RestController
@RequestMapping("/api/compare-pro/files")
public class GPUFileController {

    @Autowired
    private ZxOcrConfig zxOcrConfig;
    
    @Autowired
    private ZxcmConfig zxcmConfig;

    /**
     * 测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("GPUFileController is working!");
    }

    /**
     * 访问比对文件 - 支持调试模式和任务文件访问
     * 支持路径格式：
     * 1. 调试文件：test1.pdf, test1.annotated.pdf
     * 2. 任务文件：compare-pro/tasks/taskId/images/old/page-1.png
     */
    @GetMapping("/tasks/{taskId}/images/{mode}/{fileName}")
    public ResponseEntity<Resource> serveTaskImage(
            @PathVariable String taskId,
            @PathVariable String mode, 
            @PathVariable String fileName) {
        String path = "tasks/" + taskId + "/images/" + mode + "/" + fileName;
        return serveCompareProFile(path);
    }

    @GetMapping("/{path:.+}")
    public ResponseEntity<Resource> serveCompareProFile(@PathVariable String path) {
        try {
            System.out.println("🌐 接收到文件请求: " + path);
            Path filePath = null;

            // 如果是调试模式使用的文件，检查配置路径
            if ("test1.pdf".equals(path) || "test2.pdf".equals(path)) {
                filePath = Paths.get(zxOcrConfig.getDebugFilePath(), path);
            } else if (path.endsWith(".annotated.pdf") && (path.startsWith("test1") || path.startsWith("test2"))) {
                // 处理标注PDF文件
                String baseName = path.replace(".annotated.pdf", ".pdf");
                Path baseFilePath = Paths.get(zxOcrConfig.getDebugFilePath(), baseName);
                filePath = Paths.get(baseFilePath.toString() + ".annotated.pdf");
            } else {
                // 处理任务相关文件（图片、PDF等）
                // 路径格式：tasks/taskId/images/old/page-1.png
                String uploadPath = zxcmConfig.getFileUpload().getRootPath();
                // 在任务文件路径前添加compare-pro前缀
                String fullPath = "compare-pro/" + path;
                filePath = Paths.get(uploadPath, fullPath);
                
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            File file = filePath.toFile();

            // 创建文件资源
            Resource resource = new FileSystemResource(file);

            // 根据文件类型设置Content-Type
            String contentType = determineContentType(path);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", Paths.get(path).getFileName().toString()); // inline表示在浏览器中显示，而不是下载
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
