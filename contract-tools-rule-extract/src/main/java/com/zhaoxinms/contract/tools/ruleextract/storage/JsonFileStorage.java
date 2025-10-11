package com.zhaoxinms.contract.tools.ruleextract.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
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
 * 在项目根目录下创建 rule-extract-data 目录存储数据
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class JsonFileStorage {

    // 项目根目录
    private final String projectRoot = System.getProperty("user.dir");
    
    // 数据存储根目录
    private final String dataRoot = projectRoot + File.separator + "rule-extract-data";
    
    // 模板目录
    private final String templatesDir = dataRoot + File.separator + "templates";
    
    // 任务目录
    private final String tasksDir = dataRoot + File.separator + "tasks";
    
    // 上传文件目录
    private final String uploadsDir = dataRoot + File.separator + "uploads";

    /**
     * 初始化目录结构
     */
    @PostConstruct
    public void init() {
        try {
            // 创建目录
            FileUtil.mkdir(dataRoot);
            FileUtil.mkdir(templatesDir);
            FileUtil.mkdir(tasksDir);
            FileUtil.mkdir(uploadsDir);
            
            log.info("JSON文件存储服务初始化成功");
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
            String filePath = dir + File.separator + id + ".json";
            
            // 序列化为JSON（格式化输出）
            String json = JSON.toJSONString(data, JSONWriter.Feature.PrettyFormat);
            
            // 写入文件
            FileUtil.writeString(json, filePath, StandardCharsets.UTF_8);
            
            log.debug("保存数据: category={}, id={}, path={}", category, id, filePath);
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
            if (!file.exists()) {
                return null;
            }
            
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
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
            FileUtil.writeBytes(fileData, filePath);
            
            log.info("保存上传文件: taskId={}, fileName={}", taskId, fileName);
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

