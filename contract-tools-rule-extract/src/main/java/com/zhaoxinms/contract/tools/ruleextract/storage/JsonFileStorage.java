package com.zhaoxinms.contract.tools.ruleextract.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON文件存储服务
 * 使用配置的上传路径存储数据
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class JsonFileStorage {

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;
    
    // 数据存储根目录（延迟初始化）
    private String dataRoot;
    
    // 模板目录
    private String templatesDir;
    
    // 任务目录
    private String tasksDir;
    
    // 上传文件目录
    private String uploadsDir;

    /**
     * 初始化目录结构
     */
    @PostConstruct
    public void init() {
        try {
            // 使用配置的上传路径作为基础目录
            // 移除尾部的分隔符（如果存在）
            String rootPath = uploadRootPath.endsWith(File.separator) 
                ? uploadRootPath.substring(0, uploadRootPath.length() - 1) 
                : uploadRootPath;
            
            // 转换为绝对路径
            File rootFile = new File(rootPath);
            String absoluteRootPath = rootFile.getAbsolutePath();
            
            dataRoot = absoluteRootPath + File.separator + "rule-extract-data";
            templatesDir = dataRoot + File.separator + "templates";
            tasksDir = dataRoot + File.separator + "tasks";
            uploadsDir = dataRoot + File.separator + "uploads";
            
            // 创建目录
            FileUtil.mkdir(dataRoot);
            FileUtil.mkdir(templatesDir);
            FileUtil.mkdir(tasksDir);
            FileUtil.mkdir(uploadsDir);
            
            log.info("JSON文件存储服务初始化成功");
            log.info("配置的上传根路径: {}", uploadRootPath);
            log.info("绝对路径: {}", absoluteRootPath);
            log.info("数据存储目录: {}", dataRoot);
            log.info("模板目录: {}", templatesDir);
            log.info("任务目录: {}", tasksDir);
            log.info("上传目录: {}", uploadsDir);
        } catch (Exception e) {
            log.error("初始化存储目录失败", e);
            throw new RuntimeException("初始化存储目录失败", e);
        }
    }

    /**
     * 保存对象为JSON文件
     */
    public <T> void save(String category, String id, T data) {
        try {
            String dir = getCategoryDir(category);
            
            // 确保目录存在
            File dirFile = new File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
                log.info("创建目录: {}", dir);
            }
            
            String filePath = dir + File.separator + id + ".json";
            File file = new File(filePath);
            
            // 序列化为JSON（格式化输出）
            String json = JSON.toJSONString(data, JSONWriter.Feature.PrettyFormat);
            
            // 写入文件
            FileUtil.writeString(json, file, StandardCharsets.UTF_8);
            
            // 验证文件是否真的被创建
            if (file.exists()) {
                log.info("保存数据成功: category={}, id={}, path={}, size={} bytes", 
                    category, id, filePath, file.length());
            } else {
                log.error("文件保存失败，文件不存在: {}", filePath);
                throw new RuntimeException("文件保存失败");
            }
        } catch (Exception e) {
            log.error("保存数据失败: category={}, id={}", category, id, e);
            throw new RuntimeException("保存数据失败", e);
        }
    }

    /**
     * 读取JSON文件为对象
     */
    public <T> T load(String category, String id, Class<T> clazz) {
        try {
            String dir = getCategoryDir(category);
            String filePath = dir + File.separator + id + ".json";
            
            File file = new File(filePath);
            
            // 详细的调试信息
            log.debug("尝试读取文件: category={}, id={}, path={}", category, id, filePath);
            log.debug("文件绝对路径: {}", file.getAbsolutePath());
            log.debug("文件是否存在: {}", file.exists());
            log.debug("父目录是否存在: {}", file.getParentFile().exists());
            
            if (!file.exists()) {
                // 列出目录中的所有文件
                File parentDir = file.getParentFile();
                if (parentDir.exists()) {
                    String[] files = parentDir.list();
                    log.debug("目录中的文件: {}", files != null ? String.join(", ", files) : "空");
                }
                log.debug("文件不存在: category={}, id={}, path={}", category, id, filePath);
                return null;
            }
            
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
            log.debug("成功读取文件: category={}, id={}, size={} bytes", category, id, file.length());
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("读取数据失败: category={}, id={}", category, id, e);
            return null;
        }
    }

    /**
     * 删除数据文件
     */
    public void delete(String category, String id) {
        try {
            String dir = getCategoryDir(category);
            String filePath = dir + File.separator + id + ".json";
            
            File file = new File(filePath);
            if (file.exists()) {
                FileUtil.del(file);
                log.debug("删除数据: category={}, id={}", category, id);
            }
        } catch (Exception e) {
            log.error("删除数据失败: category={}, id={}", category, id, e);
            throw new RuntimeException("删除数据失败", e);
        }
    }

    /**
     * 列出某个分类下的所有数据
     */
    public <T> List<T> list(String category, Class<T> clazz) {
        try {
            String dir = getCategoryDir(category);
            File dirFile = new File(dir);
            
            if (!dirFile.exists()) {
                return new ArrayList<>();
            }
            
            File[] files = dirFile.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
                return new ArrayList<>();
            }
            
            List<T> result = new ArrayList<>();
            for (File file : files) {
                try {
                    String json = FileUtil.readString(file, StandardCharsets.UTF_8);
                    T obj = JSON.parseObject(json, clazz);
                    result.add(obj);
                } catch (Exception e) {
                    log.error("读取文件失败: {}", file.getName(), e);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("列出数据失败: category={}", category, e);
            return new ArrayList<>();
        }
    }

    /**
     * 检查数据是否存在
     */
    public boolean exists(String category, String id) {
        String dir = getCategoryDir(category);
        String filePath = dir + File.separator + id + ".json";
        return new File(filePath).exists();
    }

    /**
     * 生成唯一ID
     */
    public String generateId() {
        return IdUtil.simpleUUID();
    }

    /**
     * 获取分类目录
     */
    private String getCategoryDir(String category) {
        // 确保目录已初始化
        if (dataRoot == null) {
            throw new IllegalStateException("存储服务未初始化，请检查配置");
        }
        
        switch (category) {
            case "template":
                return templatesDir;
            case "task":
                return tasksDir;
            default:
                throw new IllegalArgumentException("未知的分类: " + category);
        }
    }

    /**
     * 保存上传的文件
     */
    public String saveUploadFile(String taskId, String fileName, byte[] fileData) {
        try {
            String taskUploadDir = uploadsDir + File.separator + taskId;
            FileUtil.mkdir(taskUploadDir);
            
            String filePath = taskUploadDir + File.separator + fileName;
            File file = new File(filePath);
            FileUtil.writeBytes(fileData, filePath);
            
            log.info("保存上传文件: taskId={}, fileName={}, 相对路径={}, 绝对路径={}, 文件大小={} bytes", 
                taskId, fileName, filePath, file.getAbsolutePath(), file.length());
            return filePath;
        } catch (Exception e) {
            log.error("保存上传文件失败", e);
            throw new RuntimeException("保存上传文件失败", e);
        }
    }

    /**
     * 获取上传文件路径
     */
    public String getUploadFilePath(String taskId, String fileName) {
        return uploadsDir + File.separator + taskId + File.separator + fileName;
    }

    /**
     * 获取数据根目录
     */
    public String getDataRoot() {
        return dataRoot;
    }

    /**
     * 获取模板目录
     */
    public String getTemplatesDir() {
        return templatesDir;
    }

    /**
     * 获取任务目录
     */
    public String getTasksDir() {
        return tasksDir;
    }

    /**
     * 获取上传目录
     */
    public String getUploadsDir() {
        return uploadsDir;
    }
}

