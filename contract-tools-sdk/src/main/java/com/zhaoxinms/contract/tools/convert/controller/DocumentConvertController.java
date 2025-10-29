package com.zhaoxinms.contract.tools.convert.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

/**
 * 文档格式转换控制器
 * 
 * 支持Word、Excel、PPT等格式转换为PDF
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@RestController
@RequestMapping("/api/convert")
@RequireFeature(module = ModuleType.DOCUMENT_FORMAT_CONVERT, message = "文档格式转换功能需要授权")
@Api(tags = "文档格式转换")
public class DocumentConvertController {

    @Autowired
    private ChangeFileToPDFService changeFileToPDFService;
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 上传并转换文档为PDF
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传并转换文档为PDF", notes = "支持doc、docx、xls、xlsx、ppt、pptx格式")
    public ApiResponse<Map<String, Object>> convertToPdf(
            @ApiParam(value = "文档文件", required = true) @RequestParam("file") MultipartFile file) {
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ApiCode.FILE_EMPTY, "请选择要转换的文件");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "文件名无效");
        }
        
        // 验证文件格式
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isSupportedFormat(extension)) {
            throw BusinessException.of(ApiCode.FILE_TYPE_NOT_SUPPORTED, 
                "不支持的文件格式。支持的格式：doc, docx, xls, xlsx, ppt, pptx");
        }
        
        log.info("收到文档转换请求: 文件名={}, 大小={} bytes, 格式={}", 
            originalFilename, file.getSize(), extension);
        
        try {
            // 获取基础路径并转换为绝对路径
            String rootPath = zxcmConfig.getFileUpload().getRootPath();
            File rootFile = new File(rootPath);
            String absoluteRootPath = rootFile.getAbsolutePath();
            
            // 保存上传的文件到临时目录（使用统一的临时上传目录）
            String uploadDir = FileStorageUtils.getTempUploadPath(absoluteRootPath);
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }
            
            String tempFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            String tempFilePath = uploadDir + File.separator + tempFileName;
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);
            
            log.info("临时文件已保存: {}, 绝对路径: {}", tempFilePath, tempFile.getAbsolutePath());
            
            // 构建文件访问URL（通过下载临时文件接口）
            String fileUrl = zxcmConfig.getApplication().getBaseUrl() 
                + "/api/convert/temp-file/" + tempFileName 
                + "?name=" + java.net.URLEncoder.encode(originalFilename, "UTF-8");
            
            // 生成目标PDF文件路径（使用模块专有目录：doc-convert/{年}/{月}/）
            String pdfFileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
            String pdfDir = FileStorageUtils.buildModulePath(absoluteRootPath, "doc-convert");
            File pdfDirFile = new File(pdfDir);
            if (!pdfDirFile.exists()) {
                pdfDirFile.mkdirs();
            }
            String destPdfPath = pdfDir + File.separator + pdfFileName;
            
            log.info("开始转换文档: {} -> {}", fileUrl, destPdfPath);
            
            // 调用转换服务
            String convertedPath = changeFileToPDFService.covertToPdf(fileUrl, destPdfPath);
            
            if (convertedPath == null || convertedPath.isEmpty()) {
                // 删除临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                throw BusinessException.of(ApiCode.CONVERT_FAILED, "文档转换失败，请检查OnlyOffice服务状态");
            }
            
            log.info("文档转换成功: {}", convertedPath);
            
            // 删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
                log.info("已删除临时文件: {}", tempFilePath);
            }
            
            // 注册PDF文件到数据库（指定模块为 doc-convert，使用相对路径）
            File convertedFile = new File(convertedPath);
            String pdfOriginalName = originalFilename.replaceFirst("\\.[^.]+$", ".pdf");
            FileInfo fileInfo = fileInfoService.registerFile(
                pdfOriginalName, 
                "pdf", 
                convertedFile.getAbsolutePath(),  // 会自动转换为相对路径
                convertedFile.length(),
                "doc-convert"  // 指定模块
            );
            
            log.info("PDF文件已注册，文件ID: {}, 原始名称: {}", fileInfo.getId(), pdfOriginalName);
            
            // 返回下载链接（使用文件ID）
            String downloadUrl = "/api/convert/download/" + fileInfo.getId();
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("fileId", fileInfo.getId());
            data.put("downloadUrl", downloadUrl);
            data.put("fileName", pdfFileName);
            data.put("originalName", pdfOriginalName);
            
            return ApiResponse.success("转换成功", data);
            
        } catch (BusinessException e) {
            throw e;  // 重新抛出业务异常
        } catch (Exception e) {
            log.error("文档转换异常", e);
            throw BusinessException.of(ApiCode.CONVERT_FAILED, "文档转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载转换后的PDF文件（通过文件ID）
     */
    @GetMapping("/download/{fileId}")
    @ApiOperation(value = "下载转换后的PDF", notes = "根据文件ID下载已转换的PDF文件")
    public void downloadPdf(
            @ApiParam(value = "文件ID", required = true) @PathVariable("fileId") String fileId,
            HttpServletResponse response) {
        
        try {
            // 通过文件ID获取文件信息
            FileInfo fileInfo = fileInfoService.getById(fileId);
            
            if (fileInfo == null) {
                log.warn("文件不存在，文件ID: {}", fileId);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 获取文件磁盘绝对路径（自动将相对路径转换为绝对路径）
            String filePath = fileInfoService.getFileDiskPath(fileId);
            if (filePath == null || filePath.isEmpty()) {
                log.error("文件路径为空，文件ID: {}", fileId);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
            File pdfFile = new File(filePath);
            if (!pdfFile.exists()) {
                log.error("PDF文件不存在: {}", filePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            log.info("下载PDF文件，文件ID: {}, 原始名称: {}, 路径: {}", 
                fileId, fileInfo.getOriginalName(), pdfFile.getAbsolutePath());
            
            // 设置响应头
            response.setContentType("application/pdf");
            // 使用原始文件名，正确处理中文文件名编码
            String originalName = fileInfo.getOriginalName() != null ? fileInfo.getOriginalName() : "document.pdf";
            String encodedFileName = java.net.URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(pdfFile.length());
            
            // 输出文件内容
            try (InputStream inputStream = new FileInputStream(pdfFile)) {
                StreamUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }
            
            log.info("PDF文件下载完成，文件ID: {}, 文件名: {}", fileId, originalName);
            
        } catch (Exception e) {
            log.error("下载PDF文件失败，文件ID: {}", fileId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 提供临时文件访问（供OnlyOffice转换服务使用）
     */
    @GetMapping("/temp-file/{fileName}")
    @ApiOperation(value = "获取临时文件", notes = "内部接口，供OnlyOffice转换服务使用")
    public void getTempFile(
            @ApiParam(value = "临时文件名", required = true) @PathVariable("fileName") String fileName,
            HttpServletResponse response) {
        
        try {
            // 安全检查
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // 查找临时文件（在最近3天的目录中查找）
            File tempFile = findTempFile(fileName);
            
            if (tempFile == null || !tempFile.exists()) {
                log.warn("临时文件不存在: {}", fileName);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            log.info("提供临时文件: {}", tempFile.getAbsolutePath());
            
            // 根据文件扩展名设置Content-Type
            String extension = getFileExtension(fileName);
            String contentType = getContentType(extension);
            response.setContentType(contentType);
            response.setContentLengthLong(tempFile.length());
            
            // 输出文件内容
            try (InputStream inputStream = new FileInputStream(tempFile)) {
                StreamUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }
            
        } catch (Exception e) {
            log.error("提供临时文件失败: {}", fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 在最近几天的目录中查找临时文件
     */
    private File findTempFile(String fileName) {
        String rootPath = zxcmConfig.getFileUpload().getRootPath();
        File rootFile = new File(rootPath);
        String absoluteRootPath = rootFile.getAbsolutePath();
        // 使用新的临时上传目录结构：temp-uploads
        String baseDir = FileStorageUtils.getTempUploadRoot(absoluteRootPath);
        return findFileInRecentDays(baseDir, fileName, 3);
    }
    
    /**
     * 在最近几天的日期目录中查找文件
     */
    private File findFileInRecentDays(String baseDir, String fileName, int days) {
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = now.minusDays(i);
            String year = String.valueOf(date.getYear());
            String month = String.format("%02d", date.getMonthValue());
            String day = String.format("%02d", date.getDayOfMonth());
            
            // 临时上传目录结构：temp-uploads/{年}/{月}/{日}
            String filePath = baseDir + File.separator + year + File.separator + month + File.separator + day + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                log.info("找到临时文件: {}", filePath);
                return file;
            }
        }
        
        log.warn("未找到临时文件: {}, 已搜索最近{}天", fileName, days);
        return null;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    /**
     * 判断是否为支持的文件格式
     */
    private boolean isSupportedFormat(String extension) {
        return extension.equals("doc") || extension.equals("docx") ||
               extension.equals("xls") || extension.equals("xlsx") ||
               extension.equals("ppt") || extension.equals("pptx");
    }
    
    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String extension) {
        switch (extension.toLowerCase()) {
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
            default:
                return "application/octet-stream";
        }
    }
}

