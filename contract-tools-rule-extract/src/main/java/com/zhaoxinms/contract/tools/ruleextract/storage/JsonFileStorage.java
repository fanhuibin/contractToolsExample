package com.zhaoxinms.contract.tools.ruleextract.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            // 注意：不再在init时创建年月子目录，而是在保存时动态创建
            
            // 只创建根目录
            FileUtil.mkdir(dataRoot);
            
            log.info("JSON文件存储服务初始化成功");
            log.info("配置的上传根路径: {}", uploadRootPath);
            log.info("绝对路径: {}", absoluteRootPath);
            log.info("数据存储目录: {}", dataRoot);
            log.info("注意：模板和任务目录将按照 年/月/任务ID 结构动态创建");
        } catch (Exception e) {
            log.error("初始化存储目录失败", e);
            throw new RuntimeException("初始化存储目录失败", e);
        }
    }

    /**
     * 保存对象为JSON文件
     * 目录结构：
     * - template: templates/{年}/{月}/{模板id}/data.json （规则模板，长期保存）
     * - task: {年}/{月}/{任务id}/task/data.json （临时数据，可按年月删除）
     */
    public <T> void save(String category, String id, T data) {
        try {
            // 从id中提取年月信息和原始任务id
            String yearMonth = FileStorageUtils.extractYearMonth(id);
            String originalId = FileStorageUtils.extractOriginalId(id);
            String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
            
            String dir;
            if ("template".equals(category)) {
                // template单独存储：templates/年/月/模板id/
                dir = dataRoot + File.separator + "templates" + File.separator + yearMonthPath + File.separator + originalId;
            } else {
                // 其他数据（task等）：年/月/任务id/category/
                dir = dataRoot + File.separator + yearMonthPath + File.separator + originalId + File.separator + category;
            }
            
            // 确保目录存在
            File dirFile = new File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
                log.info("创建目录: {}", dir);
            }
            
            // 文件名固定为data.json
            String filePath = dir + File.separator + "data.json";
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
     * 目录结构：
     * - template: templates/{年}/{月}/{模板id}/data.json
     * - task: {年}/{月}/{任务id}/task/data.json
     */
    public <T> T load(String category, String id, Class<T> clazz) {
        try {
            // 从id中提取年月信息和原始任务id
            String yearMonth = FileStorageUtils.extractYearMonth(id);
            String originalId = FileStorageUtils.extractOriginalId(id);
            String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
            
            String filePath;
            if ("template".equals(category)) {
                // template路径：templates/年/月/模板id/data.json
                filePath = dataRoot + File.separator + "templates" + File.separator + yearMonthPath 
                    + File.separator + originalId + File.separator + "data.json";
            } else {
                // 其他数据路径：年/月/任务id/category/data.json
                filePath = dataRoot + File.separator + yearMonthPath + File.separator + originalId 
                    + File.separator + category + File.separator + "data.json";
            }
            
            File file = new File(filePath);
            
            // 详细的调试信息
            log.debug("尝试读取文件: category={}, id={}, path={}", category, id, filePath);
            log.debug("文件绝对路径: {}", file.getAbsolutePath());
            log.debug("文件是否存在: {}", file.exists());
            
            if (!file.exists()) {
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
            // 从id中提取年月信息和原始任务id
            String yearMonth = FileStorageUtils.extractYearMonth(id);
            String originalId = FileStorageUtils.extractOriginalId(id);
            String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
            
            String targetDir;
            if ("template".equals(category)) {
                // 删除整个template目录：templates/年/月/模板id/
                targetDir = dataRoot + File.separator + "templates" + File.separator + yearMonthPath + File.separator + originalId;
            } else {
                // 删除category子目录：年/月/任务id/category/
                targetDir = dataRoot + File.separator + yearMonthPath + File.separator + originalId + File.separator + category;
            }
            
            File targetDirFile = new File(targetDir);
            if (targetDirFile.exists()) {
                FileUtil.del(targetDirFile);
                log.debug("删除目录: category={}, id={}, dir={}", category, id, targetDir);
            }
            
            // 如果不是template且任务目录下已经没有其他文件，删除整个任务目录
            if (!"template".equals(category)) {
                String taskDir = dataRoot + File.separator + yearMonthPath + File.separator + originalId;
                File taskDirFile = new File(taskDir);
                if (taskDirFile.exists() && taskDirFile.isDirectory()) {
                    File[] files = taskDirFile.listFiles();
                    if (files == null || files.length == 0) {
                        FileUtil.del(taskDirFile);
                        log.debug("删除空的任务目录: id={}, dir={}", id, taskDir);
                    }
                }
            }
        } catch (Exception e) {
            log.error("删除数据失败: category={}, id={}", category, id, e);
            throw new RuntimeException("删除数据失败", e);
        }
    }

    /**
     * 列出某个分类下的所有数据
     * - template: 从 templates/{年}/{月}/ 目录查找
     * - 其他: 从 {年}/{月}/ 目录查找
     */
    public <T> List<T> list(String category, Class<T> clazz) {
        try {
            List<T> result = new ArrayList<>();
            
            File searchRoot;
            if ("template".equals(category)) {
                // template从templates目录查找
                searchRoot = new File(dataRoot, "templates");
            } else {
                // 其他从根目录查找
                searchRoot = new File(dataRoot);
            }
            
            if (!searchRoot.exists()) {
                return new ArrayList<>();
            }
            
            // 遍历年目录
            File[] yearDirs = searchRoot.listFiles(File::isDirectory);
            if (yearDirs != null) {
                for (File yearDir : yearDirs) {
                    // 遍历月目录
                    File[] monthDirs = yearDir.listFiles(File::isDirectory);
                    if (monthDirs != null) {
                        for (File monthDir : monthDirs) {
                            // 遍历任务/模板目录
                            File[] itemDirs = monthDir.listFiles(File::isDirectory);
                            if (itemDirs != null) {
                                for (File itemDir : itemDirs) {
                                    File dataFile;
                                    if ("template".equals(category)) {
                                        // template直接在目录下
                                        dataFile = new File(itemDir, "data.json");
                                    } else {
                                        // 其他在category子目录下
                                        dataFile = new File(itemDir, category + File.separator + "data.json");
                                    }
                                    
                                    if (dataFile.exists()) {
                                        try {
                                            String json = FileUtil.readString(dataFile, StandardCharsets.UTF_8);
                                            T obj = JSON.parseObject(json, clazz);
                                            result.add(obj);
                                        } catch (Exception e) {
                                            log.error("读取文件失败: {}", dataFile.getAbsolutePath(), e);
                                        }
                                    }
                                }
                            }
                        }
                    }
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
        // 从id中提取年月信息和原始任务id
        String yearMonth = FileStorageUtils.extractYearMonth(id);
        String originalId = FileStorageUtils.extractOriginalId(id);
        String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
        
        String filePath;
        if ("template".equals(category)) {
            // template路径：templates/年/月/模板id/data.json
            filePath = dataRoot + File.separator + "templates" + File.separator + yearMonthPath 
                + File.separator + originalId + File.separator + "data.json";
        } else {
            // 其他路径：年/月/任务id/category/data.json
            filePath = dataRoot + File.separator + yearMonthPath + File.separator + originalId 
                + File.separator + category + File.separator + "data.json";
        }
        return new File(filePath).exists();
    }

    /**
     * 生成唯一ID（带年月前缀）
     */
    public String generateId() {
        String originalId = IdUtil.simpleUUID();
        return FileStorageUtils.generateFileId(originalId);
    }


    /**
     * 保存上传的文件
     * 保存到任务目录下：{年月}/{任务id}/uploads/{fileName}
     */
    public String saveUploadFile(String taskId, String fileName, byte[] fileData) {
        try {
            // 从taskId中提取年月信息和原始任务id
            String yearMonth = FileStorageUtils.extractYearMonth(taskId);
            String originalTaskId = FileStorageUtils.extractOriginalId(taskId);
            String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
            
            // 构建上传目录：年/月/任务id/uploads/
            String taskUploadDir = dataRoot + File.separator + yearMonthPath + File.separator 
                + originalTaskId + File.separator + "uploads";
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
        // 从taskId中提取年月信息和原始任务id
        String yearMonth = FileStorageUtils.extractYearMonth(taskId);
        String originalTaskId = FileStorageUtils.extractOriginalId(taskId);
        String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
        
        // 构建上传目录：年/月/任务id/uploads/
        return dataRoot + File.separator + yearMonthPath + File.separator 
            + originalTaskId + File.separator + "uploads" + File.separator + fileName;
    }

    /**
     * 获取数据根目录
     */
    public String getDataRoot() {
        return dataRoot;
    }

    /**
     * 获取分类基础目录
     * - template: 返回 templates/ 目录
     * - 其他: 返回根目录
     */
    public String getCategoryBaseDir(String category) {
        if ("template".equals(category)) {
            return dataRoot + File.separator + "templates";
        } else {
            return dataRoot;
        }
    }

    /**
     * 获取模板目录（不再使用，保留以兼容旧代码）
     * @deprecated 使用getCategoryBaseDir("template")代替
     */
    @Deprecated
    public String getTemplatesDir() {
        return getCategoryBaseDir("template");
    }

    /**
     * 获取任务目录（不再使用，保留以兼容旧代码）
     * @deprecated 使用getCategoryBaseDir("task")代替
     */
    @Deprecated
    public String getTasksDir() {
        return getCategoryBaseDir("task");
    }

    /**
     * 获取上传目录（不再使用，保留以兼容旧代码）
     * @deprecated 使用getUploadFilePath代替
     */
    @Deprecated
    public String getUploadsDir() {
        String yearMonthPath = FileStorageUtils.getYearMonthPath();
        return dataRoot + File.separator + "tasks" + File.separator + yearMonthPath;
    }

    /**
     * 获取OCR输出目录（用于存储页面图片等OCR中间结果）
     * 目录结构：{年月}/{任务id}/ocr-output/
     */
    public File getOcrOutputDir(String taskId) {
        // 从taskId中提取年月信息和原始任务id
        String yearMonth = FileStorageUtils.extractYearMonth(taskId);
        String originalTaskId = FileStorageUtils.extractOriginalId(taskId);
        String yearMonthPath = FileStorageUtils.getYearMonthPath(yearMonth);
        
        // 构建OCR输出目录：年/月/任务id/ocr-output/
        String taskOcrDir = dataRoot + File.separator + yearMonthPath + File.separator 
            + originalTaskId + File.separator + "ocr-output";
        return new File(taskOcrDir);
    }
}

