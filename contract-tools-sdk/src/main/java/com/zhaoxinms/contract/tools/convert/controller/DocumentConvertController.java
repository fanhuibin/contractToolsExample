package com.zhaoxinms.contract.tools.convert.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService;

import lombok.extern.slf4j.Slf4j;

/**
 * 文档格式转换控制器
 * 支持Word、Excel等格式转换为PDF
 */
@Slf4j
@RestController
@RequestMapping("/api/convert")
public class DocumentConvertController {

    @Autowired
    private ChangeFileToPDFService changeFileToPDFService;
    
    @Autowired
    private ZxcmConfig zxcmConfig;

    /**
     * 上传并转换文档为PDF
     * @param file 上传的文档文件（支持.doc, .docx, .xls, .xlsx等）
     * @return 转换结果，包含下载链接
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> convertToPdf(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择要转换的文件");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                result.put("code", 400);
                result.put("message", "文件名无效"); 
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证文件格式
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!isSupportedFormat(extension)) {
                result.put("code", 400);
                result.put("message", "不支持的文件格式。支持的格式：doc, docx, xls, xlsx, ppt, pptx");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            log.info("收到文档转换请求: 文件名={}, 大小={} bytes, 格式={}", 
                originalFilename, file.getSize(), extension);
            
            // 获取基础路径并转换为绝对路径
            String rootPath = zxcmConfig.getFileUpload().getRootPath();
            File rootFile = new File(rootPath);
            String absoluteRootPath = rootFile.getAbsolutePath();
            
            // 保存上传的文件到临时目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uploadDir = absoluteRootPath + File.separator + "temp" + File.separator + datePath;
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
            
            // 生成目标PDF文件路径
            String pdfFileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
            String pdfDir = absoluteRootPath + File.separator + "converted" + File.separator + datePath;
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
                result.put("code", 500);
                result.put("message", "文档转换失败，请检查OnlyOffice服务状态");
                result.put("data", null);
                return ResponseEntity.ok(result);
            }
            
            log.info("文档转换成功: {}", convertedPath);
            
            // 删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
                log.info("已删除临时文件: {}", tempFilePath);
            }
            
            // 返回下载链接
            String downloadUrl = "/api/convert/download/" + pdfFileName;
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("downloadUrl", downloadUrl);
            data.put("fileName", pdfFileName);
            data.put("originalName", originalFilename.replaceFirst("\\.[^.]+$", ".pdf"));
            
            result.put("code", 200);
            result.put("message", "转换成功");
            result.put("data", data);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("文档转换异常", e);
            result.put("code", 500);
            result.put("message", "文档转换失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 下载转换后的PDF文件
     * @param fileName PDF文件名
     * @param response HTTP响应
     */
    @GetMapping("/download/{fileName}")
    public void downloadPdf(
            @PathVariable("fileName") String fileName,
            HttpServletResponse response) {
        
        try {
            // 安全检查：防止路径遍历攻击
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // 查找文件（在最近3天的目录中查找）
            File pdfFile = findConvertedFile(fileName);
            
            if (pdfFile == null || !pdfFile.exists()) {
                log.warn("PDF文件不存在: {}", fileName);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            log.info("下载PDF文件: {}", pdfFile.getAbsolutePath());
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setContentLengthLong(pdfFile.length());
            
            // 输出文件内容
            try (InputStream inputStream = new FileInputStream(pdfFile)) {
                StreamUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }
            
            log.info("PDF文件下载完成: {}", fileName);
            
        } catch (Exception e) {
            log.error("下载PDF文件失败: {}", fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 提供临时文件访问（供OnlyOffice转换服务使用）
     * @param fileName 临时文件名
     * @param response HTTP响应
     */
    @GetMapping("/temp-file/{fileName}")
    public void getTempFile(
            @PathVariable("fileName") String fileName,
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
     * 在最近几天的目录中查找转换后的文件
     */
    private File findConvertedFile(String fileName) {
        String rootPath = zxcmConfig.getFileUpload().getRootPath();
        File rootFile = new File(rootPath);
        String absoluteRootPath = rootFile.getAbsolutePath();
        String baseDir = absoluteRootPath + File.separator + "converted";
        return findFileInRecentDays(baseDir, fileName, 3);
    }
    
    /**
     * 在最近几天的目录中查找临时文件
     */
    private File findTempFile(String fileName) {
        String rootPath = zxcmConfig.getFileUpload().getRootPath();
        File rootFile = new File(rootPath);
        String absoluteRootPath = rootFile.getAbsolutePath();
        String baseDir = absoluteRootPath + File.separator + "temp";
        return findFileInRecentDays(baseDir, fileName, 3);
    }
    
    /**
     * 在最近几天的日期目录中查找文件
     */
    private File findFileInRecentDays(String baseDir, String fileName, int days) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        
        for (int i = 0; i < days; i++) {
            String datePath = now.minusDays(i).format(formatter);
            String filePath = baseDir + File.separator + datePath + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                return file;
            }
        }
        
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

