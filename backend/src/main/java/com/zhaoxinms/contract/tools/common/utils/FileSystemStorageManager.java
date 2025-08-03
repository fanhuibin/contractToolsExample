package com.zhaoxinms.contract.tools.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 文件系统存储管理器
 * 使用文件系统存储文件信息，处理并发访问
 */
@Slf4j
@Component
public class FileSystemStorageManager {
    
    @Value("${file.upload.root-path:./uploads}")
    private String rootPath;
    
    @Value("${file.storage.metadata-file:file_metadata.json}")
    private String metadataFile;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Long, FileInfo> fileInfoCache = new ConcurrentHashMap<>();
    
    /**
     * 保存文件信息
     * @param fileInfo 文件信息
     */
    public void saveFileInfo(FileInfo fileInfo) {
        lock.writeLock().lock();
        try {
            // 更新缓存
            fileInfoCache.put(fileInfo.getId(), fileInfo);
            
            // 写入文件系统
            writeToFile();
            
            log.info("文件信息保存成功，ID: {}", fileInfo.getId());
        } catch (Exception e) {
            log.error("保存文件信息失败，ID: {}", fileInfo.getId(), e);
            throw new RuntimeException("保存文件信息失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 根据ID获取文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    public FileInfo getFileInfo(Long id) {
        lock.readLock().lock();
        try {
            // 先从缓存获取
            FileInfo fileInfo = fileInfoCache.get(id);
            if (fileInfo != null) {
                return fileInfo;
            }
            
            // 缓存未命中，从文件系统加载
            loadFromFile();
            return fileInfoCache.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 删除文件信息
     * @param id 文件ID
     * @return 是否删除成功
     */
    public boolean deleteFileInfo(Long id) {
        lock.writeLock().lock();
        try {
            FileInfo fileInfo = fileInfoCache.get(id);
            if (fileInfo == null) {
                return false;
            }
            
            // 标记为已删除
            fileInfo.setStatus(1);
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            // 更新缓存和文件系统
            fileInfoCache.put(id, fileInfo);
            writeToFile();
            
            log.info("文件信息删除成功，ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除文件信息失败，ID: {}", id, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取所有文件信息
     * @return 文件信息列表
     */
    public List<FileInfo> getAllFileInfo() {
        lock.readLock().lock();
        try {
            loadFromFile();
            return new ArrayList<>(fileInfoCache.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据原始文件名搜索文件信息
     * @param originalName 原始文件名
     * @return 匹配的文件信息列表
     */
    public List<FileInfo> searchByOriginalName(String originalName) {
        lock.readLock().lock();
        try {
            loadFromFile();
            List<FileInfo> result = new ArrayList<>();
            for (FileInfo fileInfo : fileInfoCache.values()) {
                if (fileInfo.getOriginalName() != null && 
                    fileInfo.getOriginalName().contains(originalName)) {
                    result.add(fileInfo);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 分页获取文件信息
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getFileInfoPage(int page, int size) {
        lock.readLock().lock();
        try {
            loadFromFile();
            
            List<FileInfo> allFiles = new ArrayList<>(fileInfoCache.values());
            // 过滤已删除的文件
            allFiles.removeIf(file -> file.getStatus() != null && file.getStatus() == 1);
            
            // 按上传时间倒序排序
            allFiles.sort((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()));
            
            int total = allFiles.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            
            List<FileInfo> pageData = new ArrayList<>();
            if (start < total) {
                pageData = allFiles.subList(start, end);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", pageData);
            result.put("total", total);
            result.put("current", page);
            result.put("size", size);
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 写入文件系统
     */
    private void writeToFile() {
        Path metadataPath = Paths.get(rootPath, metadataFile);
        
        try {
            // 确保目录存在
            Files.createDirectories(metadataPath.getParent());
            
            // 使用文件锁确保并发安全
            try (FileChannel channel = FileChannel.open(metadataPath, 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 FileLock lock = channel.lock()) {
                
                String json = objectMapper.writeValueAsString(fileInfoCache);
                channel.write(java.nio.ByteBuffer.wrap(json.getBytes()));
            }
            
        } catch (IOException e) {
            log.error("写入文件信息失败", e);
            throw new RuntimeException("写入文件信息失败", e);
        }
    }
    
    /**
     * 从文件系统加载
     */
    private void loadFromFile() {
        Path metadataPath = Paths.get(rootPath, metadataFile);
        
        if (!Files.exists(metadataPath)) {
            return;
        }
        
        try {
            // 使用文件锁确保并发安全
            try (FileChannel channel = FileChannel.open(metadataPath, StandardOpenOption.READ);
                 FileLock lock = channel.lock(0L, Long.MAX_VALUE, true)) {
                
                byte[] bytes = new byte[(int) channel.size()];
                channel.read(java.nio.ByteBuffer.wrap(bytes));
                String json = new String(bytes);
                
                if (!json.trim().isEmpty()) {
                    Map<Long, FileInfo> loadedData = objectMapper.readValue(json, 
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, Long.class, FileInfo.class));
                    fileInfoCache.putAll(loadedData);
                }
            }
            
        } catch (IOException e) {
            log.error("加载文件信息失败", e);
            // 加载失败时不清空缓存，保持现有数据
        }
    }
    
    /**
     * 初始化存储目录
     */
    public void initialize() {
        try {
            Path root = Paths.get(rootPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            log.info("文件存储目录初始化完成: {}", root.toAbsolutePath());
        } catch (IOException e) {
            log.error("初始化文件存储目录失败", e);
            throw new RuntimeException("初始化文件存储目录失败", e);
        }
    }
} 