package com.zhaoxin.demo.controller;

import com.zhaoxin.demo.model.request.CompareRequest;
import com.zhaoxin.demo.model.response.ApiResponse;
import com.zhaoxin.demo.service.CompareApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 比对 API 代理控制器
 * 
 * 所有请求都转发到肇新 API
 * CORS 配置由 WebConfig 统一管理
 */
@Slf4j
@RestController
@RequestMapping("/api/compare")
public class CompareProxyController {
    
    private final CompareApiClient apiClient;
    
    public CompareProxyController(CompareApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * 提交比对任务
     */
    @PostMapping("/submit")
    public ApiResponse submitCompare(@RequestBody CompareRequest request) {
        log.info("收到比对请求: {}", request);
        return apiClient.submitCompare(
            request.getOldFileUrl(), 
            request.getNewFileUrl(),
            request.getRemoveWatermark(),
            request.getOldFileName(),
            request.getNewFileName()
        );
    }
    
    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse getTaskStatus(@PathVariable String taskId) {
        log.info("查询任务状态: taskId={}", taskId);
        return apiClient.getTaskStatus(taskId);
    }
    
    /**
     * 获取比对结果
     */
    @GetMapping("/result/{taskId}")
    public ApiResponse getResult(@PathVariable String taskId) {
        log.info("获取比对结果: taskId={}", taskId);
        return apiClient.getResult(taskId);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
        log.info("删除任务: taskId={}", taskId);
        apiClient.deleteTask(taskId);
        return new ApiResponse<>(200, "删除成功", null);
    }
    
    /**
     * 获取所有任务历史
     */
    @GetMapping("/tasks")
    public ApiResponse getAllTasks() {
        log.info("获取任务历史列表");
        return apiClient.getAllTasks();
    }
    
    /**
     * 导出比对报告
     */
    @PostMapping("/export-report")
    public ResponseEntity<byte[]> exportReport(@RequestBody Map<String, Object> exportData) {
        log.info("导出比对报告: {}", exportData);
        return apiClient.exportReport(exportData);
    }
}

