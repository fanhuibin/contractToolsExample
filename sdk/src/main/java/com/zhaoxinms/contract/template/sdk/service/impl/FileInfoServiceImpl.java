package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.exception.FileOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件信息Service实现类
 * SDK项目中的具体实现
 */
@Slf4j
@Service
public class FileInfoServiceImpl implements FileInfoService {

    @Value("${server.port:8081}")
    private String serverPort;
    
    @Value("${zxcm.file.upload.root-path:./uploads}")
    private String uploadRootPath;

    // 模拟数据存储（实际项目中应该使用数据库）
    private final ConcurrentHashMap<String, FileInfo> fileInfoMap = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(10000);

    @PostConstruct
    public void init() {
        // 获取uploads目录的绝对路径
        String absUploadPath = Paths.get(uploadRootPath).toAbsolutePath().toString();
        try {
            java.io.File uploadDir = new java.io.File(absUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
        } catch (Exception ignore) {}
        // 文件1
        FileInfo fileInfo1 = new FileInfo();
        fileInfo1.setId(1L);
        fileInfo1.setOriginalName("房屋建筑和市政基础设施项目工程建设全过程咨询服务合同示范文本.docx");
        fileInfo1.setFileName("房屋建筑和市政基础设施项目工程建设全过程咨询服务合同示范文本.docx");
        fileInfo1.setFileExtension("docx");
        fileInfo1.setFileSize(1024L * 1024L); // 1MB
        fileInfo1.setStorePath(absUploadPath + "/房屋建筑和市政基础设施项目工程建设全过程咨询服务合同示范文本.docx");
        fileInfo1.setStatus(0);
        fileInfo1.setCreateTime(LocalDateTime.now());
        fileInfo1.setUpdateTime(LocalDateTime.now());
        fileInfo1.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
        fileInfoMap.put("1", fileInfo1);
        
        // 文件2
        FileInfo fileInfo2 = new FileInfo();
        fileInfo2.setId(2L);
        fileInfo2.setOriginalName("test_document.docx");
        fileInfo2.setFileName("test_document.docx");
        fileInfo2.setFileExtension("docx");
        fileInfo2.setFileSize(512L * 1024L);
        fileInfo2.setStorePath(absUploadPath + "/test_document.docx");
        fileInfo2.setStatus(0);
        fileInfo2.setCreateTime(LocalDateTime.now());
        fileInfo2.setUpdateTime(LocalDateTime.now());
        fileInfo2.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
        fileInfoMap.put("2", fileInfo2);
        
        // 文件3
        FileInfo fileInfo3 = new FileInfo();
        fileInfo3.setId(3L);
        fileInfo3.setOriginalName("test_document1.docx");
        fileInfo3.setFileName("test_document1.docx");
        fileInfo3.setFileExtension("docx");
        fileInfo3.setFileSize(256L * 1024L);
        fileInfo3.setStorePath(absUploadPath + "/test_document1.docx");
        fileInfo3.setStatus(0);
        fileInfo3.setCreateTime(LocalDateTime.now());
        fileInfo3.setUpdateTime(LocalDateTime.now());
        fileInfo3.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
        fileInfoMap.put("3", fileInfo3);
        
        // 文件4
        FileInfo fileInfo4 = new FileInfo();
        fileInfo4.setId(4L);
        fileInfo4.setOriginalName("test_document2.docx");
        fileInfo4.setFileName("test_document2.docx");
        fileInfo4.setFileExtension("docx");
        fileInfo4.setFileSize(1024L * 1024L);
        fileInfo4.setStorePath(absUploadPath + "/test_document2.docx");
        fileInfo4.setStatus(0);
        fileInfo4.setCreateTime(LocalDateTime.now());
        fileInfo4.setUpdateTime(LocalDateTime.now());
        fileInfo4.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
        fileInfoMap.put("4", fileInfo4);

        // 保证存在并注册模板设计固定文件 templateDesign.docx
        try {
            java.io.File templateFile = new java.io.File(absUploadPath + "/templateDesign.docx");
            if (!templateFile.exists()) {
                java.io.File candidate = findFirstExisting(
                    absUploadPath + "/templateDesign.docx",
                    absUploadPath + "/test_document.docx",
                    Paths.get("sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                    Paths.get("..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                    Paths.get("..", "..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("..", "..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString()
                );
                if (candidate != null && candidate.exists()) {
                    Files.copy(candidate.toPath(), templateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            if (templateFile.exists()) {
                FileInfo tplInfo = new FileInfo();
                tplInfo.setId(9999L);
                tplInfo.setOriginalName("templateDesign.docx");
                tplInfo.setFileName("templateDesign.docx");
                tplInfo.setFileExtension("docx");
                tplInfo.setFileSize(templateFile.length());
                tplInfo.setStorePath(templateFile.getAbsolutePath());
                tplInfo.setStatus(0);
                tplInfo.setCreateTime(LocalDateTime.now());
                tplInfo.setUpdateTime(LocalDateTime.now());
                tplInfo.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
                fileInfoMap.put("9999", tplInfo);
            }
        } catch (Exception e) {
            log.warn("初始化templateDesign.docx失败: {}", e.getMessage());
        }
    }

    public FileInfo saveNewFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        long newId = idSequence.incrementAndGet();
        String idStr = String.valueOf(newId);
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }

        java.nio.file.Path targetPath = Paths.get(uploadRootPath, idStr + "." + fileExtension);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(newId);
        fileInfo.setOriginalName(originalFilename);
        fileInfo.setFileName(targetPath.getFileName().toString());
        fileInfo.setFileExtension(fileExtension);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setStorePath(targetPath.toAbsolutePath().toString());
        fileInfo.setStatus(0);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setOnlyofficeKey(generateOnlyOfficeKeyForFile(idStr));
        
        fileInfoMap.put(idStr, fileInfo);
        log.info("创建新文件记录，文件ID: {}", idStr);
        return fileInfo;
    }

    /**
     * 将已存在的磁盘文件注册为系统文件（复制到 uploads 下并分配新ID）。
     */
    public FileInfo registerClonedFile(java.nio.file.Path sourcePath, String originalName) throws IOException {
        if (sourcePath == null || !java.nio.file.Files.exists(sourcePath)) {
            throw new IllegalArgumentException("源文件不存在: " + String.valueOf(sourcePath));
        }
        String ext = "";
        String name = sourcePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) ext = name.substring(dot + 1);

        long newId = idSequence.incrementAndGet();
        String idStr = String.valueOf(newId);
        java.nio.file.Path targetPath = Paths.get(uploadRootPath, idStr + (ext.isEmpty() ? "" : "." + ext));
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(newId);
        fileInfo.setOriginalName(originalName != null ? originalName : name);
        fileInfo.setFileName(targetPath.getFileName().toString());
        fileInfo.setFileExtension(ext);
        fileInfo.setFileSize(Files.size(targetPath));
        fileInfo.setStorePath(targetPath.toAbsolutePath().toString());
        fileInfo.setStatus(0);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setOnlyofficeKey(generateOnlyOfficeKeyForFile(idStr));

        fileInfoMap.put(idStr, fileInfo);
        log.info("注册克隆文件，文件ID: {} -> {}", idStr, targetPath);
        return fileInfo;
    }

    @Override
    public FileInfo getById(String id) {
        FileInfo fileInfo = fileInfoMap.get(id);
        if (fileInfo != null && fileInfo.getStatus() != null && fileInfo.getStatus() == 0) {
            return fileInfo;
        }
        // 动态注册模板设计示例文件
        if ("templateDesign".equals(id)) {
            // 优先返回已注册的9999
            FileInfo tpl = fileInfoMap.get("9999");
            if (tpl != null && tpl.getStatus() != null && tpl.getStatus() == 0) {
                return tpl;
            }
            String absPath = java.nio.file.Paths.get(uploadRootPath).toAbsolutePath().toString();
            java.io.File tplFile = new java.io.File(absPath + "/templateDesign.docx");
            if (!tplFile.exists()) {
                java.io.File candidate = findFirstExisting(
                    absPath + "/templateDesign.docx",
                    absPath + "/test_document.docx",
                    Paths.get("sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                    Paths.get("..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                    Paths.get("..", "..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                    Paths.get("..", "..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString()
                );
                try {
                    if (candidate != null && candidate.exists()) {
                        Files.copy(candidate.toPath(), new java.io.File(absPath + "/templateDesign.docx").toPath(), StandardCopyOption.REPLACE_EXISTING);
                        tplFile = new java.io.File(absPath + "/templateDesign.docx");
                    }
                } catch (Exception copyErr) {
                    log.warn("复制模板设计文件失败: {}", copyErr.getMessage());
                }
            }
            if (tplFile.exists()) {
                FileInfo created = new FileInfo();
                created.setId(9999L);
                created.setOriginalName("templateDesign.docx");
                created.setFileName("templateDesign.docx");
                created.setFileExtension("docx");
                created.setFileSize(tplFile.length());
                created.setStorePath(tplFile.getAbsolutePath());
                created.setStatus(0);
                created.setCreateTime(LocalDateTime.now());
                created.setUpdateTime(LocalDateTime.now());
                created.setOnlyofficeKey("demo_key_" + System.currentTimeMillis());
                fileInfoMap.put("9999", created);
                return created;
            }
        }
        return null;
    }

    private java.io.File findFirstExisting(String... candidates) {
        if (candidates == null) return null;
        for (String p : candidates) {
            try {
                if (p == null) continue;
                java.io.File f = new java.io.File(p);
                if (f.exists() && f.isFile()) {
                    return f;
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    @Override
    public String getFileDownloadUrl(String fileId) {
        // 构建文件下载URL
        return "http://localhost:" + serverPort + "/api/template/file/download/" + fileId;
    }

    @Override
    public String getFileDiskPath(String fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return null;
        }
        return fileInfo.getStorePath();
    }

    @Override
    public boolean saveFile(String fileId, InputStream inputStream) throws IOException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        
        if (inputStream == null) {
            throw new IllegalArgumentException("文件输入流不能为空");
        }
        
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        String filePath = fileInfo.getStorePath();
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalStateException("文件存储路径为空，文件ID: " + fileId);
        }
        
        java.io.File targetFile = new java.io.File(filePath);
        
        // 确保父目录存在
        java.io.File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (!dirsCreated) {
                throw new IOException("无法创建文件存储目录: " + parentDir.getAbsolutePath());
            }
            log.info("创建文件存储目录: {}", parentDir.getAbsolutePath());
        }
        
        // 创建临时文件，避免写入过程中文件损坏
        java.io.File tempFile = new java.io.File(filePath + ".tmp");
        
        try {
            // 将输入流内容写入临时文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                 java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                bos.flush();
                log.info("成功写入临时文件，文件ID: {}, 大小: {} bytes", fileId, totalBytes);
            }
            
            // 原子性地替换原文件
            if (targetFile.exists()) {
                // 备份原文件
                java.io.File backupFile = new java.io.File(filePath + ".bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                
                boolean renamed = targetFile.renameTo(backupFile);
                if (!renamed) {
                    log.warn("无法创建备份文件: {}", backupFile.getAbsolutePath());
                }
            }
            
            // 将临时文件重命名为目标文件
            boolean success = tempFile.renameTo(targetFile);
            if (!success) {
                throw new IOException("无法将临时文件重命名为目标文件: " + filePath);
            }
            
            // 更新文件信息
            fileInfo.setFileSize(targetFile.length());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfoMap.put(fileId, fileInfo);
            
            log.info("文件保存成功，文件ID: {}, 路径: {}, 大小: {} bytes", 
                    fileId, filePath, targetFile.length());
            
            // 删除备份文件
            java.io.File backupFile = new java.io.File(filePath + ".bak");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            
            return true;
            
        } catch (Exception e) {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            
            // 如果有备份文件，尝试恢复
            java.io.File backupFile = new java.io.File(filePath + ".bak");
            if (backupFile.exists()) {
                backupFile.renameTo(targetFile);
                log.info("已从备份文件恢复: {}", filePath);
            }
            
            log.error("文件保存失败，文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new FileOperationException(fileId, "SAVE", "文件保存失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public FileInfo generateOnlyofficeKey(String fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        // 生成新的OnlyOffice key，格式为：文件id + 分隔符 + 雪花id
        String onlyofficeKey = generateOnlyOfficeKeyForFile(fileId);
        fileInfo.setOnlyofficeKey(onlyofficeKey);
        fileInfo.setUpdateTime(LocalDateTime.now());
        
        // 保存更新后的文件信息
        fileInfoMap.put(fileId, fileInfo);
        
        log.info("为文件生成OnlyOffice key，文件ID: {}, key: {}", fileId, onlyofficeKey);
        return fileInfo;
    }
    
    @Override
    public List<FileInfo> getAllFiles() {
        return new ArrayList<>(fileInfoMap.values());
    }

    /**
     * 为指定文件ID生成OnlyOffice key，格式为：文件id + 分隔符 + 雪花id
     */
    private String generateOnlyOfficeKeyForFile(String fileId) {
        // 使用当前时间戳作为雪花ID的简化版本
        long snowflakeId = System.currentTimeMillis();
        return fileId + "_" + snowflakeId;
    }
} 