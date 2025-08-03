package com.zhaoxinms.contract.tools.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件管理工具类
 * 用于管理本地磁盘文件的存储、读取、删除等操作
 */
@Slf4j
@Component
public class FileManager {

    @Value("${file.upload.root-path:./uploads}")
    private String rootPath;

    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize; // 默认10MB

    /**
     * 上传文件
     * @param file 上传的文件
     * @param subPath 子目录路径（可选）
     * @return 文件存储的相对路径
     */
    public String uploadFile(MultipartFile file, String subPath) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制：" + maxFileSize + " bytes");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFileName = generateFileName(fileExtension);

        // 构建存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = subPath != null ? subPath + "/" + datePath : datePath;
        Path targetPath = Paths.get(rootPath, relativePath);
        
        // 创建目录
        Files.createDirectories(targetPath);

        // 保存文件
        Path filePath = targetPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath);

        log.info("文件上传成功: {} -> {}", originalFilename, filePath);
        return relativePath + "/" + newFileName;
    }

    /**
     * 上传文件（使用默认路径）
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * 删除文件
     * @param relativePath 文件的相对路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(rootPath, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", filePath);
                return true;
            } else {
                log.warn("文件不存在: {}", filePath);
                return false;
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", relativePath, e);
            return false;
        }
    }

    /**
     * 获取文件
     * @param relativePath 文件的相对路径
     * @return 文件对象
     */
    public File getFile(String relativePath) {
        Path filePath = Paths.get(rootPath, relativePath);
        File file = filePath.toFile();
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * 检查文件是否存在
     * @param relativePath 文件的相对路径
     * @return 是否存在
     */
    public boolean fileExists(String relativePath) {
        Path filePath = Paths.get(rootPath, relativePath);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    /**
     * 重命名文件
     * @param oldRelativePath 原文件相对路径
     * @param newFileName 新文件名
     * @return 新的相对路径
     */
    public String renameFile(String oldRelativePath, String newFileName) throws IOException {
        Path oldPath = Paths.get(rootPath, oldRelativePath);
        if (!Files.exists(oldPath)) {
            throw new IllegalArgumentException("原文件不存在: " + oldRelativePath);
        }

        Path parentDir = oldPath.getParent();
        Path newPath = parentDir.resolve(newFileName);
        
        if (Files.exists(newPath)) {
            throw new IllegalArgumentException("目标文件已存在: " + newFileName);
        }

        Files.move(oldPath, newPath);
        
        String newRelativePath = parentDir.relativize(Paths.get(rootPath)).resolve(newFileName).toString();
        log.info("文件重命名成功: {} -> {}", oldRelativePath, newRelativePath);
        return newRelativePath;
    }

    /**
     * 获取文件大小
     * @param relativePath 文件的相对路径
     * @return 文件大小（字节）
     */
    public long getFileSize(String relativePath) {
        try {
            Path filePath = Paths.get(rootPath, relativePath);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            log.error("获取文件大小失败: {}", relativePath, e);
        }
        return -1;
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
     * 生成唯一文件名
     */
    private String generateFileName(String fileExtension) {
        return UUID.randomUUID().toString().replace("-", "") + fileExtension;
    }

    /**
     * 获取根目录路径
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * 更新文件内容
     * @param relativePath 文件的相对路径
     * @param inputStream 文件输入流
     * @return 是否更新成功
     */
    public boolean updateFileContent(String relativePath, java.io.InputStream inputStream) throws IOException {
        try {
            Path filePath = Paths.get(rootPath, relativePath);
            
            // 确保目录存在
            Files.createDirectories(filePath.getParent());
            
            // 使用Files.copy覆盖文件内容
            Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            log.info("文件内容更新成功: {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("文件内容更新失败: {}", relativePath, e);
            return false;
        }
    }
    
    /**
     * 获取最大文件大小
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
} 