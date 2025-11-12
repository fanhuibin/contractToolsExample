package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.request.CompareRequest;
import com.zhaoxin.tools.demo.model.response.ApiResponse;
import com.zhaoxin.tools.demo.service.ZhaoxinApiClient;
import com.zhaoxin.tools.demo.service.TaskFileMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 智能文档比对控制器
 * 
 * 负责处理文档比对相关的API请求
 */
@Slf4j
@RestController
@RequestMapping("/api/compare")
public class CompareController {
    
    private final ZhaoxinApiClient apiClient;
    private final TaskFileMappingService mappingService;
    
    public CompareController(ZhaoxinApiClient apiClient, TaskFileMappingService mappingService) {
        this.apiClient = apiClient;
        this.mappingService = mappingService;
    }
    
    /**
     * 提交比对任务
     */
    @PostMapping("/submit")
    public ApiResponse submitCompare(@RequestBody CompareRequest request) {
        log.info("收到比对请求: {}", request);
        
        // 提交任务到肇新服务（不传文件名，避免SDK保存UUID文件名）
        ApiResponse response = apiClient.submitCompareTask(
            request.getOldFileUrl(), 
            request.getNewFileUrl(),
            request.getRemoveWatermark(),
            null,  // 不传给SDK
            null   // 不传给SDK
        );
        
        // 如果提交成功，保存taskId和原始文件名的映射
        if (response.getCode() == 200 && response.getData() != null) {
            String taskId = response.getData().toString();
            mappingService.saveMapping(
                taskId, 
                request.getOldFileName(), 
                request.getNewFileName()
            );
            log.info("已保存任务文件名映射: taskId={}", taskId);
        }
        
        return response;
    }
    
    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse getTaskStatus(@PathVariable String taskId) {
        log.info("查询任务状态: taskId={}", taskId);
        ApiResponse response = apiClient.getCompareTaskStatus(taskId);
        
        // 替换为原始文件名
        replaceFileNamesInResponse(response, taskId);
        
        return response;
    }
    
    /**
     * 获取比对结果
     */
    @GetMapping("/result/{taskId}")
    public ApiResponse getResult(@PathVariable String taskId) {
        log.info("获取比对结果: taskId={}", taskId);
        return apiClient.getCompareResult(taskId);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
        log.info("删除任务: taskId={}", taskId);
        apiClient.deleteCompareTask(taskId);
        
        // 同时删除本地映射
        mappingService.deleteMapping(taskId);
        
        return new ApiResponse<>(200, "删除成功", null);
    }
    
    /**
     * 获取所有任务历史
     */
    @GetMapping("/tasks")
    @SuppressWarnings("unchecked")
    public ApiResponse getAllTasks() {
        log.info("获取任务历史列表");
        ApiResponse response = apiClient.getAllCompareTasks();
        
        // 替换所有任务的文件名
        if (response.getCode() == 200 && response.getData() != null) {
            try {
                if (response.getData() instanceof java.util.List) {
                    java.util.List<Map<String, Object>> tasks = (java.util.List<Map<String, Object>>) response.getData();
                    for (Map<String, Object> task : tasks) {
                        String taskId = (String) task.get("taskId");
                        if (taskId != null) {
                            replaceFileNamesInTask(task, taskId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("替换文件名时出错", e);
            }
        }
        
        return response;
    }
    
    /**
     * 替换响应中的文件名
     */
    @SuppressWarnings("unchecked")
    private void replaceFileNamesInResponse(ApiResponse response, String taskId) {
        if (response.getData() != null && response.getData() instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) response.getData();
            replaceFileNamesInTask(data, taskId);
        }
    }
    
    /**
     * 替换任务对象中的文件名
     */
    private void replaceFileNamesInTask(Map<String, Object> task, String taskId) {
        var mapping = mappingService.getMapping(taskId);
        if (mapping != null) {
            task.put("oldFileName", mapping.getOldFileName());
            task.put("newFileName", mapping.getNewFileName());
            log.debug("已替换文件名: taskId={}, oldFileName={}, newFileName={}", 
                    taskId, mapping.getOldFileName(), mapping.getNewFileName());
        }
    }
    
    /**
     * 导出比对报告
     */
    @PostMapping("/export-report")
    public ResponseEntity<byte[]> exportReport(@RequestBody Map<String, Object> exportData) {
        log.info("导出比对报告: {}", exportData);
        return apiClient.exportCompareReport(exportData);
    }
}

