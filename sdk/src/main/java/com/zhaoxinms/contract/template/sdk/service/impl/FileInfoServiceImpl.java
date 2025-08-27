package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.exception.FileOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhaoxinms.contract.template.sdk.entity.FileInfoRecord;
import com.zhaoxinms.contract.template.sdk.mapper.FileInfoRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired(required = false)
    private FileInfoRecordMapper fileInfoRecordMapper;

    @PostConstruct
    public void init() {
        // 仅确保目录存在，不再写入模拟数据
        String absUploadPath = Paths.get(uploadRootPath).toAbsolutePath().toString();
        try {
            java.io.File uploadDir = new java.io.File(absUploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();
        } catch (Exception ignore) {}
    }

    @Override
    public FileInfo getById(String id) {
        if (fileInfoRecordMapper == null) return null;
        FileInfoRecord rec = fileInfoRecordMapper.selectById(id);
        if (rec == null) return null;
        FileInfo info = new FileInfo();
        info.setId(rec.getId());
        info.setOriginalName(rec.getOriginalName());
        info.setFileName(rec.getFileName());
        info.setFileExtension(rec.getFileExtension());
        info.setFileSize(rec.getFileSize());
        info.setStorePath(rec.getStorePath() != null ? rec.getStorePath() : rec.getFilePath());
        info.setStatus(rec.getStatus());
        info.setCreateTime(rec.getCreateTime());
        info.setUpdateTime(rec.getUpdateTime());
        info.setOnlyofficeKey(rec.getOnlyofficeKey());
        return info;
    }

    // no-op helper removed

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
            if (fileInfoRecordMapper != null) {
                FileInfoRecord rec = fileInfoRecordMapper.selectById(fileId);
                if (rec != null) {
                    rec.setFileSize(targetFile.length());
                    rec.setUpdateTime(LocalDateTime.now());
                    fileInfoRecordMapper.updateById(rec);
                }
            }
            
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
        
        if (fileInfoRecordMapper != null) {
            FileInfoRecord rec = fileInfoRecordMapper.selectById(fileId);
            if (rec != null) {
                rec.setOnlyofficeKey(onlyofficeKey);
                rec.setUpdateTime(LocalDateTime.now());
                fileInfoRecordMapper.updateById(rec);
            }
        }
        
        log.info("为文件生成OnlyOffice key，文件ID: {}, key: {}", fileId, onlyofficeKey);
        return fileInfo;
    }
    
    @Override
    public List<FileInfo> getAllFiles() {
        if (fileInfoRecordMapper == null) return new ArrayList<>();
        List<FileInfoRecord> list = fileInfoRecordMapper.selectList(new QueryWrapper<>());
        List<FileInfo> out = new ArrayList<>();
        for (FileInfoRecord r : list) {
            FileInfo i = new FileInfo();
            i.setId(r.getId());
            i.setOriginalName(r.getOriginalName());
            i.setFileName(r.getFileName());
            i.setFileExtension(r.getFileExtension());
            i.setFileSize(r.getFileSize());
            i.setStorePath(r.getStorePath() != null ? r.getStorePath() : r.getFilePath());
            i.setStatus(r.getStatus());
            i.setCreateTime(r.getCreateTime());
            i.setUpdateTime(r.getUpdateTime());
            i.setOnlyofficeKey(r.getOnlyofficeKey());
            out.add(i);
        }
        return out;
    }

    @Override
    public FileInfo registerFile(String originalName, String extension, String absolutePath, long fileSize) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            throw new IllegalArgumentException("absolutePath 不能为空");
        }
        try {
            if (fileInfoRecordMapper == null) throw new IllegalStateException("文件表未配置");
            FileInfoRecord rec = new FileInfoRecord();
            rec.setOriginalName(originalName != null ? originalName : new java.io.File(absolutePath).getName());
            rec.setFileName(rec.getOriginalName());
            rec.setFileExtension(extension);
            rec.setFileSize(fileSize >= 0 ? fileSize : (new java.io.File(absolutePath).length()));
            rec.setStorePath(absolutePath);
            rec.setStatus(0);
            rec.setCreateTime(LocalDateTime.now());
            rec.setUpdateTime(LocalDateTime.now());
            rec.setOnlyofficeKey("reg_" + System.currentTimeMillis());
            fileInfoRecordMapper.insert(rec);
            FileInfo out = new FileInfo();
            out.setId(rec.getId());
            out.setOriginalName(rec.getOriginalName());
            out.setFileName(rec.getFileName());
            out.setFileExtension(rec.getFileExtension());
            out.setFileSize(rec.getFileSize());
            out.setStorePath(rec.getStorePath());
            out.setStatus(rec.getStatus());
            out.setCreateTime(rec.getCreateTime());
            out.setUpdateTime(rec.getUpdateTime());
            out.setOnlyofficeKey(rec.getOnlyofficeKey());
            return out;
        } catch (Exception e) {
            throw new RuntimeException("注册文件失败: " + e.getMessage(), e);
        }
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