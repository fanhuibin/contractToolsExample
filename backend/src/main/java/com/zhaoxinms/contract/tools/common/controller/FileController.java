package com.zhaoxinms.contract.tools.common.controller;

import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 文件管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {

    private final FileInfoService fileInfoService;

    /**
     * 分页查询文件列表
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> getFilePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String originalName) {
        
        try {
            Map<String, Object> result = fileInfoService.getFileInfoPage(page, size);
            
            // 如果提供了搜索条件，进行过滤
            if (originalName != null && !originalName.trim().isEmpty()) {
                List<FileInfo> allFiles = (List<FileInfo>) result.get("records");
                List<FileInfo> filteredFiles = fileInfoService.searchByOriginalName(originalName);
                result.put("records", filteredFiles);
                result.put("total", filteredFiles.size());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error("获取文件列表失败");
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileInfo> uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "subPath", required = false) String subPath) {
        try {
            FileInfo fileInfo = fileInfoService.uploadFile(file, subPath);
            return Result.success(fileInfo);
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        FileInfo fileInfo = fileInfoService.getById(id);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get("./uploads", fileInfo.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + fileInfo.getOriginalName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("文件下载失败，ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteFile(@PathVariable Long id) {
        boolean success = fileInfoService.deleteFile(id);
        if (success) {
            return Result.success("文件删除成功");
        }
        return Result.error("文件删除失败");
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{id}")
    public Result<FileInfo> getFileInfo(@PathVariable Long id) {
        FileInfo fileInfo = fileInfoService.getById(id);
        if (fileInfo != null) {
            return Result.success(fileInfo);
        }
        return Result.error("文件不存在");
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("文件管理模块测试成功！");
    }
} 