package com.zhaoxinms.contract.tools.common.service.impl;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.utils.FileManager;
import com.zhaoxinms.contract.tools.common.utils.FileSystemStorageManager;
import com.zhaoxinms.contract.tools.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件信息Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {

    private final FileManager fileManager;
    private final FileSystemStorageManager storageManager;
    private final SnowflakeIdGenerator idGenerator;

    @PostConstruct
    public void init() {
        storageManager.initialize();
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, String subPath) throws IOException {
        // 上传文件到磁盘
        String relativePath = fileManager.uploadFile(file, subPath);
        
        // 创建文件信息记录
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(idGenerator.nextId());
        fileInfo.setOriginalName(file.getOriginalFilename());
        fileInfo.setFileName(relativePath.substring(relativePath.lastIndexOf("/") + 1));
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(file.getContentType());
        fileInfo.setFileExtension(getFileExtension(file.getOriginalFilename()));
        fileInfo.setFileMd5(generateMd5()); // 这里可以计算真实的MD5值
        fileInfo.setStatus(0);
        fileInfo.setUploadTime(LocalDateTime.now());
        
        // 保存到文件系统
        storageManager.saveFileInfo(fileInfo);
        
        log.info("文件上传成功，ID: {}, 原始文件名: {}", fileInfo.getId(), fileInfo.getOriginalName());
        return fileInfo;
    }

    @Override
    public FileInfo uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    @Override
    public boolean deleteFile(Long id) {
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            return false;
        }
        
        // 删除磁盘文件
        boolean diskDeleted = fileManager.deleteFile(fileInfo.getFilePath());
        
        // 删除文件信息
        boolean infoDeleted = storageManager.deleteFileInfo(id);
        
        return diskDeleted && infoDeleted;
    }

    @Override
    public FileInfo getById(Long id) {
        return storageManager.getFileInfo(id);
    }

    @Override
    public FileInfo getByFilePath(String filePath) {
        // 从所有文件信息中查找匹配的文件路径
        List<FileInfo> allFiles = storageManager.getAllFileInfo();
        return allFiles.stream()
                .filter(file -> file.getFilePath() != null && file.getFilePath().equals(filePath))
                .filter(file -> file.getStatus() == null || file.getStatus() == 0)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateFileContent(Long fileId, java.io.InputStream inputStream) throws IOException {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        // 使用FileManager更新文件内容
        boolean success = fileManager.updateFileContent(fileInfo.getFilePath(), inputStream);
        
        if (success) {
            // 更新文件信息
            fileInfo.setUpdateTime(LocalDateTime.now());
            storageManager.saveFileInfo(fileInfo);
        }
        
        return success;
    }

    @Override
    public Map<String, Object> getFileInfoPage(int page, int size) {
        return storageManager.getFileInfoPage(page, size);
    }

    @Override
    public List<FileInfo> searchByOriginalName(String originalName) {
        return storageManager.searchByOriginalName(originalName);
    }
    
    @Override
    public FileInfo generateOnlyofficeKey(Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        // 生成新的OnlyOffice key
        String onlyofficeKey = generateOnlyOfficeKey();
        fileInfo.setOnlyofficeKey(onlyofficeKey);
        fileInfo.setUpdateTime(LocalDateTime.now());
        
        // 保存更新后的文件信息
        storageManager.saveFileInfo(fileInfo);
        
        log.info("为文件生成OnlyOffice key，文件ID: {}, key: {}", fileId, onlyofficeKey);
        return fileInfo;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    /**
     * 生成MD5值（简化实现）
     */
    private String generateMd5() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成OnlyOffice key
     * OnlyOffice要求key是唯一的，每次文档编辑时都需要新的key
     */
    private String generateOnlyOfficeKey() {
        return System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
} 