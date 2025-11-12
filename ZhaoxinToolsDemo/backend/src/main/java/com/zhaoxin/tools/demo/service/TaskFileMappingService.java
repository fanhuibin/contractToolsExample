package com.zhaoxin.tools.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxin.tools.demo.model.TaskFileMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务文件名映射服务
 * 用于保存和查询任务ID与原始文件名的映射关系
 */
@Slf4j
@Service
public class TaskFileMappingService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path mappingFilePath;
    private final Map<String, TaskFileMapping> mappingCache = new ConcurrentHashMap<>();
    
    public TaskFileMappingService() {
        // 使用项目根目录下的 task-mappings.json 文件
        this.mappingFilePath = Paths.get("task-mappings.json").toAbsolutePath().normalize();
        loadMappings();
    }
    
    /**
     * 保存任务文件名映射
     */
    public void saveMapping(String taskId, String oldFileName, String newFileName) {
        TaskFileMapping mapping = new TaskFileMapping(
            taskId,
            oldFileName,
            newFileName,
            System.currentTimeMillis()
        );
        
        mappingCache.put(taskId, mapping);
        persistMappings();
        
        log.info("保存任务文件名映射: taskId={}, oldFileName={}, newFileName={}", 
                taskId, oldFileName, newFileName);
    }
    
    /**
     * 获取任务的原始文件名
     */
    public TaskFileMapping getMapping(String taskId) {
        return mappingCache.get(taskId);
    }
    
    /**
     * 删除任务映射
     */
    public void deleteMapping(String taskId) {
        mappingCache.remove(taskId);
        persistMappings();
        log.info("删除任务文件名映射: taskId={}", taskId);
    }
    
    /**
     * 获取所有映射
     */
    public Map<String, TaskFileMapping> getAllMappings() {
        return new ConcurrentHashMap<>(mappingCache);
    }
    
    /**
     * 从文件加载映射
     */
    private void loadMappings() {
        try {
            File file = mappingFilePath.toFile();
            if (file.exists()) {
                List<TaskFileMapping> mappings = objectMapper.readValue(
                    file, 
                    new TypeReference<List<TaskFileMapping>>() {}
                );
                
                for (TaskFileMapping mapping : mappings) {
                    mappingCache.put(mapping.getTaskId(), mapping);
                }
                
                log.info("加载任务文件名映射: 共{}条记录", mappings.size());
            } else {
                log.info("任务映射文件不存在，将创建新文件");
            }
        } catch (IOException e) {
            log.error("加载任务文件名映射失败", e);
        }
    }
    
    /**
     * 持久化映射到文件
     */
    private synchronized void persistMappings() {
        try {
            List<TaskFileMapping> mappings = new ArrayList<>(mappingCache.values());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(mappingFilePath.toFile(), mappings);
            
            log.debug("持久化任务文件名映射: 共{}条记录", mappings.size());
        } catch (IOException e) {
            log.error("持久化任务文件名映射失败", e);
        }
    }
}

