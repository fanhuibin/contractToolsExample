package com.zhaoxinms.contract.tools.onlyoffice;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.onlyoffice.util.service.DefaultServiceConverter;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.onlyoffice.exception.OnlyOfficeServiceUnavailableException;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChangeFileToPDFService {
    @Autowired
    private ZxcmConfig zxcmConfig;
    @Autowired
    private DefaultServiceConverter covertService;
    @Autowired
    private FileInfoService fileInfoService; // 留作兼容，当前类方法未直接使用
    
    /** 
     * 将文件转换为PDF
     * @param fileUrl 文件URL
     * @param destPdfPath 目标PDF文件完整路径
     * @return 转换后的文件路径
     */
    public String covertToPdf(String fileUrl, String destPdfPath) {
        try {
            // 获取文件扩展名
            String fileExtension = getFileExtension(fileUrl);
            
            // 调用OnlyOffice转换服务（轮询直到转换完成）
            String convertedUrl = null;
            long deadline = System.currentTimeMillis() + 120_000L; // 最长等待120秒
            while (System.currentTimeMillis() < deadline) {
                String url = covertService.getConvertedUri(
                    fileUrl,
                    fileExtension,
                    "pdf",
                    "",
                    "",
                    false,
                    "zh-CN"
                );
                if (url != null && !url.isEmpty()) {
                    convertedUrl = url;
                    break;
                }
                try { Thread.sleep(1500); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); break; }
            }

            if (convertedUrl == null || convertedUrl.isEmpty()) {
                log.error("文件转换失败，转换URL为空，源文件URL: {}", fileUrl);
                return null;
            }

            // 下载转换后的文件
            downloadFile(convertedUrl, destPdfPath);

            // 校验PDF有效性
            File out = new File(destPdfPath);
            if (!out.exists() || out.length() == 0 || !isValidPdf(out)) {
                log.error("转换后的PDF无效或为空，URL: {}，文件: {}", convertedUrl, destPdfPath);
                return null;
            }
            log.info("文件转换成功，源文件URL: {}, 目标文件: {}", fileUrl, destPdfPath);
            return destPdfPath;
            
        } catch (Exception e) {
            // 检查是否为OnlyOffice服务不可用的情况
            if (isOnlyOfficeServiceUnavailable(e)) {
                log.error("OnlyOffice服务不可用，源文件URL: {}, 错误: {}", fileUrl, e.getMessage(), e);
                throw new OnlyOfficeServiceUnavailableException("OnlyOffice服务不可用，请检查服务状态", e);
            }
            log.error("文件转换异常，源文件URL: {}, 错误: {}", fileUrl, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将文件转换为PDF（使用FileInfo对象）
     * @param fileInfo 文件信息
     * @return 转换后的文件路径
     */
    public String covertToPdf(FileInfo fileInfo) {
        // 构建文件下载URL，添加时间戳解决缓存问题
        // 使用 OnlyOffice 回调的下载接口路径
        String fileUrl = zxcmConfig.getApplication().getBaseUrl() + "/api/onlyoffice/callback/download/" + fileInfo.getId() 
            + "?fileId=" + fileInfo.getId() 
            + "&t=" + System.currentTimeMillis();
        
        log.info("PDF转换文件URL: {}", fileUrl);
        
        // 根据文件的 module 字段生成对应模块目录下的路径
        String module = fileInfo.getModule();
        String destPdfPath;
        
        if (module != null && !module.trim().isEmpty()) {
            // 有模块信息，使用模块专有目录: {module}/{年}/{月}/{文件名}.pdf
            String rootPath = zxcmConfig.getFileUpload().getRootPath();
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
            destPdfPath = rootPath + File.separator + module + File.separator + yearMonthPath + File.separator + fileName;
            log.info("使用模块目录生成PDF路径，模块: {}, 路径: {}", module, destPdfPath);
        } else {
            // 无模块信息，使用旧的目录结构（向后兼容）
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            destPdfPath = zxcmConfig.getFileUpload().getRootPath() + "/" + datePath + "/" 
                + UUID.randomUUID().toString().replace("-", "") + ".pdf";
            log.warn("文件无模块信息，使用默认路径: {}", destPdfPath);
        }
        
        return covertToPdf(fileUrl, destPdfPath);
    }
     
    public String covertToPdf(File localFile) throws IOException {
        String fileName = localFile.getName();
        String fileUrl = zxcmConfig.getApplication().getBaseUrl() + "/api/download/temp?path=" + java.net.URLEncoder.encode("compose/"+fileName, "UTF-8");

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String destPdfPath = zxcmConfig.getFileUpload().getRootPath() + File.separator + datePath + File.separator 
            + UUID.randomUUID().toString().replace("-", "") + ".pdf";

        return covertToPdf(fileUrl, destPdfPath);
    }
    
    /**
     * 将DOC文件转换为DOCX
     * @param fileInfo 文件信息
     * @return 转换后的文件路径
     */
    public String covertDocToDocx(FileInfo fileInfo) {
        try {
            // 构建文件下载URL，添加时间戳解决缓存问题
            String downloadUrl = zxcmConfig.getApplication().getBaseUrl() + "/download/" + fileInfo.getId() 
                + "?fileId=" + fileInfo.getId() 
                + "&t=" + System.currentTimeMillis();
            
            // 调用OnlyOffice转换服务
            String convertedUrl = covertService.getConvertedUri(
                downloadUrl,
                "doc", 
                "docx", 
                "", 
                "", 
                false, 
                "zh-CN"
            );
            
            if (convertedUrl != null && !convertedUrl.isEmpty()) {
                // 根据文件的 module 字段生成对应模块目录下的路径
                String module = fileInfo.getModule();
                String destFile;
                
                if (module != null && !module.trim().isEmpty()) {
                    // 有模块信息，使用模块专有目录
                    String rootPath = zxcmConfig.getFileUpload().getRootPath();
                    String yearMonthPath = FileStorageUtils.getYearMonthPath();
                    String fileName = UUID.randomUUID().toString().replace("-", "") + ".docx";
                    destFile = rootPath + File.separator + module + File.separator + yearMonthPath + File.separator + fileName;
                    log.info("使用模块目录生成DOCX路径，模块: {}, 路径: {}", module, destFile);
                } else {
                    // 无模块信息，使用旧的目录结构（向后兼容）
                    String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    destFile = zxcmConfig.getFileUpload().getRootPath() + "/" + datePath + "/" 
                        + UUID.randomUUID().toString().replace("-", "") + ".docx";
                }
                
                // 下载转换后的文件
                downloadFile(convertedUrl, destFile);
                log.info("DOC转DOCX成功，源文件: {}, 目标文件: {}", fileInfo.getOriginalName(), destFile);
                return destFile;
            } else {
                log.error("DOC转DOCX失败，转换URL为空，源文件: {}", fileInfo.getOriginalName());
                return null;
            }
            
        } catch (Exception e) {
            log.error("DOC转DOCX异常，源文件: {}, 错误: {}", fileInfo.getOriginalName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 判断是否为OnlyOffice服务不可用的情况
     * @param e 异常对象
     * @return 是否为OnlyOffice服务不可用
     */
    private boolean isOnlyOfficeServiceUnavailable(Exception e) {
        // 检查异常消息中是否包含OnlyOffice相关的错误信息
        String message = e.getMessage();
        if (message != null) {
            // 检查是否为下载错误（错误码-4）
            if (message.contains("Error download error")) {
                return true;
            }
            // 检查是否为转换服务错误
            if (message.contains("Error occurred in the ConvertService")) {
                return true;
            }
            // 检查是否为网络连接相关错误
            if (message.contains("Connection refused") || 
                message.contains("ConnectException") ||
                message.contains("UnknownHostException") ||
                message.contains("SocketTimeoutException")) {
                return true;
            }
        }
        
        // 检查异常类型
        if (e instanceof java.net.ConnectException ||
            e instanceof java.net.UnknownHostException ||
            e instanceof java.net.SocketTimeoutException) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 下载文件
     * @param url 文件URL
     * @param destFile 目标文件路径
     */
    private void downloadFile(String url, String destFile) throws IOException {
        try {
            // 确保目标目录存在
            File destFileObj = new File(destFile);
            if (!destFileObj.getParentFile().exists()) {
                destFileObj.getParentFile().mkdirs();
            }
            
            // 使用HttpURLConnection下载文件
            java.net.URL fileUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) fileUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            try (java.io.InputStream inputStream = connection.getInputStream();
                 java.io.FileOutputStream outputStream = new java.io.FileOutputStream(destFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("文件下载成功: {} -> {}", url, destFile);
            
        } catch (Exception e) {
            log.error("文件下载失败: {} -> {}, 错误: {}", url, destFile, e.getMessage(), e);
            throw new IOException("文件下载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取文件扩展名
     * @param fileUrl 文件URL或文件名
     * @return 文件扩展名（不包含点）
     */
    private String getFileExtension(String fileUrl) {
        if (fileUrl == null) return "";
        try {
            // 优先从 query 参数 name 中解析
            int q = fileUrl.indexOf('?');
            if (q >= 0) {
                String query = fileUrl.substring(q + 1);
                for (String part : query.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) {
                        String key = part.substring(0, eq);
                        if ("name".equalsIgnoreCase(key)) {
                            String value = part.substring(eq + 1);
                            try { value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name()); } catch (Exception ignore) {}
                            int dot = value.lastIndexOf('.');
                            return dot >= 0 ? value.substring(dot + 1).toLowerCase() : "";
                        }
                    }
                }
            }
            // 回退：取最后一个路径段
            String fileName = fileUrl;
            int slash = fileUrl.lastIndexOf('/');
            if (slash >= 0) fileName = fileUrl.substring(slash + 1);
            int hash = fileName.indexOf('#');
            if (hash >= 0) fileName = fileName.substring(0, hash);
            int qm = fileName.indexOf('?');
            if (qm >= 0) fileName = fileName.substring(0, qm);
            int dot = fileName.lastIndexOf('.');
            return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isValidPdf(File file) {
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            byte[] head = new byte[5];
            int n = in.read(head);
            return n >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
        } catch (Exception e) {
            return false;
        }
    }
}
