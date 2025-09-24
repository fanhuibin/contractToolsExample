package com.zhaoxinms.contract.tools.comparePRO.util;

import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * 文件下载工具类 - 用于从URL下载文件并转换为MultipartFile
 */
public class FileDownloadUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadUtil.class);
    
    // 最大文件大小 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    // 下载超时时间 60秒
    private static final int DOWNLOAD_TIMEOUT = 60 * 1000;
    
    /**
     * 从URL下载文件并转换为MultipartFile
     * 
     * @param fileUrl 文件URL
     * @param fieldName 表单字段名
     * @return MultipartFile对象
     * @throws Exception 下载异常
     */
    public static MultipartFile downloadFromUrl(String fileUrl, String fieldName) throws Exception {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("文件URL不能为空");
        }
        
        logger.info("开始下载文件: {}", fileUrl);
        
        // 验证URL格式
        URL url;
        try {
            url = new URL(fileUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的文件URL: " + fileUrl);
        }
        
        // 只支持HTTP和HTTPS协议
        String protocol = url.getProtocol().toLowerCase();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IllegalArgumentException("仅支持HTTP和HTTPS协议");
        }
        
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        
        try {
            // 建立连接
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT);
            connection.setRequestProperty("User-Agent", "ZhaoxinContractComparePro/1.0");
            
            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP响应错误: " + responseCode + " - " + connection.getResponseMessage());
            }
            
            // 检查内容类型
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase().contains("pdf")) {
                logger.warn("文件可能不是PDF格式，Content-Type: {}", contentType);
            }
            
            // 检查文件大小
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE) {
                throw new IOException("文件大小超过限制，最大支持50MB");
            }
            
            // 下载文件
            inputStream = connection.getInputStream();
            outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                
                // 实时检查文件大小
                if (totalBytesRead > MAX_FILE_SIZE) {
                    throw new IOException("文件大小超过限制，最大支持50MB");
                }
                
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] fileData = outputStream.toByteArray();
            
            // 验证文件是否为PDF
            if (!isPdfFile(fileData)) {
                throw new IllegalArgumentException("文件不是有效的PDF格式");
            }
            
            // 生成文件名
            String originalFilename = extractFilenameFromUrl(fileUrl);
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                originalFilename = "document_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            }
            
            logger.info("文件下载成功: {}, 大小: {} bytes", originalFilename, fileData.length);
            
            // 创建自定义MultipartFile实现
            return new ByteArrayMultipartFile(
                fieldName,
                originalFilename,
                "application/pdf",
                fileData
            );
            
        } finally {
            // 清理资源
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 验证文件是否为PDF格式
     */
    private static boolean isPdfFile(byte[] fileData) {
        if (fileData == null || fileData.length < 4) {
            return false;
        }
        
        // PDF文件头部特征: %PDF
        return fileData[0] == 0x25 && // %
               fileData[1] == 0x50 && // P
               fileData[2] == 0x44 && // D
               fileData[3] == 0x46;   // F
    }
    
    /**
     * 从URL中提取文件名
     */
    private static String extractFilenameFromUrl(String fileUrl) {
        try {
            String path = new URL(fileUrl).getPath();
            if (path != null && path.length() > 0) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    return path.substring(lastSlash + 1);
                }
            }
        } catch (Exception e) {
            logger.debug("无法从URL提取文件名: {}", fileUrl);
        }
        return null;
    }
    
    /**
     * 自定义MultipartFile实现
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String fieldName;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;
        
        public ByteArrayMultipartFile(String fieldName, String originalFilename, String contentType, byte[] content) {
            this.fieldName = fieldName;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content.clone();
        }
        
        @Override
        public String getName() {
            return fieldName != null ? fieldName : "";
        }
        
        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content != null ? content.clone() : new byte[0];
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content != null ? content : new byte[0]);
        }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
