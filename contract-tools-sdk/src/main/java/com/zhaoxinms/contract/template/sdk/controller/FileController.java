package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 文件管理控制器
 * SDK项目中的文件管理功能
 */
@RestController
@RequestMapping("/api/file")
@Api(tags = "文件管理API")
public class FileController {


    @Autowired
    private FileInfoService fileInfoService;

    @GetMapping("/download/{fileId}")
    @ApiOperation("下载文件")
    public void downloadFile(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId,
            HttpServletResponse response) {
        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            } 
            
            // 获取文件路径
            String filePath = fileInfoService.getFileDiskPath(fileId);
            if (filePath == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 设置响应头
            String contentType = getContentType(fileInfo.getFileExtension());
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"" + fileInfo.getOriginalName() + "\"");
            response.setHeader("Content-Length", String.valueOf(file.length()));
            
            // 读取文件并输出
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败：" + e.getMessage());
            } catch (IOException ex) {
                // 忽略异常
            }
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
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }

    @GetMapping("/{fileId}")
    @ApiOperation("获取文件信息")
    public Result<FileInfo> getFileInfo(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error("获取文件信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取所有文件列表")
    public Result<List<FileInfo>> getFileList() {
        try {
            // 若找不到模板设计示例文件，则自动注册一个指向 uploads/templateDesign.docx 的记录
            List<FileInfo> files = fileInfoService.getAllFiles();
            boolean hasTemplate = files.stream().anyMatch(f -> "templateDesign.docx".equals(f.getOriginalName()));
            if (!hasTemplate) {
                java.nio.file.Path path = java.nio.file.Paths.get("uploads", "templateDesign.docx");
                java.io.File f = path.toFile();
                if (f.exists()) {
                    FileInfo info = new FileInfo();
                    info.setFileName("templateDesign.docx");
                    info.setOriginalName("templateDesign.docx");
                    info.setFileExtension("docx");
                    info.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
                    info.setFileSize(f.length());
                    info.setStatus(0);
                    info.setStorePath(f.getAbsolutePath());
                    files.add(info);
                }
            }
            return Result.success(files);
        } catch (Exception e) {
            return Result.error("获取文件列表失败：" + e.getMessage());
        }
    }
    

} 