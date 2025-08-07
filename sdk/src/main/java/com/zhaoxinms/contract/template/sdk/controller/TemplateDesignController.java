package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 模板设计API控制器
 * SDK项目的主要控制器，与Frontend配合实现功能
 */
@RestController
@RequestMapping("/api/template")
@Api(tags = "模板设计API")
public class TemplateDesignController { 

    @Autowired
    private TemplateDesignService templateDesignService;
    
    @Autowired
    private FileInfoService fileInfoService;
    
    @Value("${zxcm.file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @GetMapping("/fields")
    @ApiOperation("获取字段信息")
    public Result<FieldResponse> getFields() {
        try {
            FieldResponse response = templateDesignService.getFields();
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取字段信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/file/download/{fileId}")
    @ApiOperation("下载文件")
    public ResponseEntity<Resource> downloadFile(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            // 获取文件信息
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("文件不存在，文件ID: " + fileId);
            }
            
            // 构建文件路径 - 使用配置的上传根路径
            Path filePath = Paths.get(uploadRootPath, fileInfo.getFileName());
            File file = filePath.toFile();
            
            if (!file.exists()) {
                throw new RuntimeException("文件不存在于磁盘，文件路径: " + filePath);
            }
            
            // 创建文件资源
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileInfo.getOriginalName() + "\"");
            
            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(fileInfo.getFileExtension());
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }

    @PostMapping("/design/start")
    @ApiOperation("发起模板设计")
    public Result<TemplateDesignResponse> startTemplateDesign(
            @ApiParam(value = "模板设计请求", required = true) 
            @Valid @RequestBody TemplateDesignRequest request) {
        try {
            TemplateDesignResponse response = templateDesignService.startTemplateDesign(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("发起模板设计失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }
        
        switch (fileExtension.toLowerCase()) {
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":
                return "application/msword";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls":
                return "application/vnd.ms-excel";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }
} 