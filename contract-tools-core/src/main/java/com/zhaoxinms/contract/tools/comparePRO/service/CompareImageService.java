package com.zhaoxinms.contract.tools.comparePRO.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

/**
 * 比对图片管理服务
 * 
 * 职责:
 * - 获取文档图片信息
 * - 复制和管理任务图片
 * - 提供图片路径和元数据
 * 
 * 重构说明:
 * 本服务从 CompareService 中分离出来，专门处理比对过程中的图片管理。
 * 统一管理图片相关操作，提高代码复用性和可维护性。
 * 
 * @author AI Assistant
 * @since 2025-10-08
 */
@Service
public class CompareImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompareImageService.class);
    
    @Autowired
    private ZxOcrConfig gpuOcrConfig;
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    /**
     * 文档图片信息
     */
    public static class DocumentImageInfo {
        private int totalPages;
        private List<PageImageInfo> pages;
        
        public DocumentImageInfo() {
            this.pages = new ArrayList<>();
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public List<PageImageInfo> getPages() {
            return pages;
        }
        
        public void setPages(List<PageImageInfo> pages) {
            this.pages = pages;
        }
    }
    
    /**
     * 页面图片信息
     */
    public static class PageImageInfo {
        private int pageNumber;
        private String imageUrl;
        private int width;
        private int height;
        
        public PageImageInfo(int pageNumber, String imageUrl, int width, int height) {
            this.pageNumber = pageNumber;
            this.imageUrl = imageUrl;
            this.width = width;
            this.height = height;
        }
        
        public int getPageNumber() {
            return pageNumber;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
    }
    
    /**
     * 获取文档图片信息
     * 
     * @param taskId 任务ID
     * @param mode 文档模式（old/new）
     * @return 文档图片信息
     */
    public DocumentImageInfo getDocumentImageInfo(String taskId, String mode) throws Exception {
        DocumentImageInfo info = new DocumentImageInfo();
        
        // 使用 gpuOcrConfig 的 uploadPath，与生成图片时保持一致
        String uploadRootPath = gpuOcrConfig.getUploadPath();
        Path imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
        
        logger.debug("获取文档图片信息: taskId={}, mode={}, imagesDir={}", taskId, mode, imagesDir);
        
        if (!Files.exists(imagesDir)) {
            logger.warn("图片目录不存在: {}", imagesDir);
            return info;
        }
        
        // 使用 Files.list 并支持多种图片格式
        List<PageImageInfo> pages = new ArrayList<>();
        try (var stream = Files.list(imagesDir)) {
            stream.filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
            })
            .sorted((p1, p2) -> {
                // 按页码排序
                String name1 = p1.getFileName().toString();
                String name2 = p2.getFileName().toString();
                int page1 = extractPageNumber(name1);
                int page2 = extractPageNumber(name2);
                return Integer.compare(page1, page2);
            })
            .forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();
                    int pageNumber = extractPageNumber(fileName);
                    
                    // 读取图片尺寸
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(path.toFile());
                    int width = img != null ? img.getWidth() : 0;
                    int height = img != null ? img.getHeight() : 0;
                    
                    // 构建图片 URL（相对路径）
                    String imageUrl = "/api/compare-pro/files/tasks/" + taskId + "/images/" + mode + "/" + fileName;
                    
                    pages.add(new PageImageInfo(pageNumber, imageUrl, width, height));
                    
                    logger.debug("  - 页面 {}: {}x{}, {}", pageNumber, width, height, imageUrl);
                } catch (Exception e) {
                    logger.warn("读取图片失败: {}", path, e);
                }
            });
        }
        
        info.setPages(pages);
        info.setTotalPages(pages.size());
        
        logger.info("获取文档图片信息完成: taskId={}, mode={}, totalPages={}", taskId, mode, pages.size());
        
        return info;
    }
    
    /**
     * 生成实际图片信息（用于导出）
     * 
     * @param mode 文档模式（old/new）
     * @param taskId 任务ID
     * @return 图片信息Map
     */
    public Map<String, Object> generateActualImageInfo(String mode, String taskId) {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> pages = new ArrayList<>();
        
        // 使用 gpuOcrConfig 的 uploadPath，与生成图片时保持一致
        String uploadRootPath = gpuOcrConfig.getUploadPath();
        Path imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
        
        logger.debug("生成实际图片信息: taskId={}, mode={}, imagesDir={}", taskId, mode, imagesDir);
        
        if (!Files.exists(imagesDir)) {
            logger.warn("图片目录不存在: {}", imagesDir);
            info.put("totalPages", 0);
            info.put("pages", pages);
            return info;
        }
        
        try (var stream = Files.list(imagesDir)) {
            stream.filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
            })
            .sorted((p1, p2) -> {
                String name1 = p1.getFileName().toString();
                String name2 = p2.getFileName().toString();
                int page1 = extractPageNumber(name1);
                int page2 = extractPageNumber(name2);
                return Integer.compare(page1, page2);
            })
            .forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();
                    int pageNumber = extractPageNumber(fileName);
                    
                    // 读取图片尺寸
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(path.toFile());
                    int width = img != null ? img.getWidth() : 0;
                    int height = img != null ? img.getHeight() : 0;
                    
                    Map<String, Object> pageInfo = new HashMap<>();
                    pageInfo.put("pageNumber", pageNumber);
                    pageInfo.put("imageUrl", "./images/" + mode + "/" + fileName);
                    pageInfo.put("width", width);
                    pageInfo.put("height", height);
                    
                    pages.add(pageInfo);
                    
                    logger.debug("  - 页面 {}: {}x{}", pageNumber, width, height);
                } catch (Exception e) {
                    logger.warn("读取图片失败: {}", path, e);
                }
            });
        } catch (Exception e) {
            logger.error("遍历图片目录失败: {}", imagesDir, e);
        }
        
        info.put("totalPages", pages.size());
        info.put("pages", pages);
        
        logger.info("生成实际图片信息完成: taskId={}, mode={}, totalPages={}", taskId, mode, pages.size());
        
        return info;
    }
    
    /**
     * 从指定路径复制任务图片
     * 
     * @param taskPath 任务路径
     * @param oldImagesDir 旧文档图片目标目录
     * @param newImagesDir 新文档图片目标目录
     * @return 复制的图片数量
     */
    public int copyTaskImagesFromPath(Path taskPath, Path oldImagesDir, Path newImagesDir) throws IOException {
        int totalCopied = 0;
        
        // 检查是否存在images子目录结构
        Path oldImagesPath = taskPath.resolve("images").resolve("old");
        Path newImagesPath = taskPath.resolve("images").resolve("new");
        
        if (Files.exists(oldImagesPath) && Files.exists(newImagesPath)) {
            logger.info("复制图片文件: 从 {} 和 {}", oldImagesPath, newImagesPath);
            
            int oldCopied = copyImagesFromDirectory(oldImagesPath, oldImagesDir, "page-*.*");
            int newCopied = copyImagesFromDirectory(newImagesPath, newImagesDir, "page-*.*");
            
            totalCopied = oldCopied + newCopied;
            
            logger.info("图片复制完成: old={}, new={}, total={}", oldCopied, newCopied, totalCopied);
        } else {
            logger.warn("未找到标准图片目录结构");
        }
        
        return totalCopied;
    }
    
    /**
     * 从目录复制图片文件
     * 
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @param pattern 文件名模式
     * @return 复制的文件数量
     */
    public int copyImagesFromDirectory(Path sourceDir, Path targetDir, String pattern) throws IOException {
        if (!Files.exists(sourceDir)) {
            logger.warn("源目录不存在: {}", sourceDir);
            return 0;
        }
        
        Files.createDirectories(targetDir);
        
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, pattern)) {
            for (Path sourceFile : stream) {
                if (Files.isRegularFile(sourceFile)) {
                    Path targetFile = targetDir.resolve(sourceFile.getFileName());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                    logger.debug("复制图片: {} -> {}", sourceFile.getFileName(), targetFile);
                }
            }
        }
        
        logger.info("从 {} 复制了 {} 个图片文件到 {}", sourceDir, count, targetDir);
        
        return count;
    }
    
    /**
     * 获取图片目录路径
     * 
     * @param taskId 任务ID
     * @param mode 文档模式（old/new）
     * @return 图片目录路径
     */
    public Path getImageDirectory(String taskId, String mode) {
        String uploadRootPath = gpuOcrConfig.getUploadPath();
        return Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
    }
    
    /**
     * 列出图片文件
     * 
     * @param taskId 任务ID
     * @param mode 文档模式（old/new）
     * @return 图片文件名列表
     */
    public List<String> listImageFiles(String taskId, String mode) {
        List<String> files = new ArrayList<>();
        Path imagesDir = getImageDirectory(taskId, mode);
        
        if (!Files.exists(imagesDir)) {
            return files;
        }
        
        try (var stream = Files.list(imagesDir)) {
            stream.filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
            })
            .forEach(path -> files.add(path.getFileName().toString()));
        } catch (Exception e) {
            logger.error("列出图片文件失败: {}", imagesDir, e);
        }
        
        return files;
    }
    
    /**
     * 从文件名中提取页码
     * 
     * @param fileName 文件名（如 page-1.png）
     * @return 页码
     */
    private int extractPageNumber(String fileName) {
        try {
            // 提取 page-N 中的 N
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = baseName.split("-");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[parts.length - 1]);
            }
        } catch (Exception e) {
            logger.warn("无法从文件名提取页码: {}", fileName);
        }
        return 0;
    }
}

