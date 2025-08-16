package com.zhaoxinms.contract.tools.ocrcompare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.zhaoxinms.contract.tools.ocrcompare.facade.JavaOCR;
import com.zhaoxinms.contract.tools.ocrcompare.service.OCRTaskService;
import com.zhaoxinms.contract.tools.ocrcompare.model.OCRTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR任务控制器
 */
@RestController
@RequestMapping("/api/ocr")
public class OCRController {
    
    @Autowired
    private OCRTaskService ocrTaskService;
    
    @Autowired
    private JavaOCR javaOCR;
    
    /**
     * 提交OCR任务
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, String> request) {
        String pdfPath = request.get("pdfPath");
        
        if (pdfPath == null || pdfPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("PDF路径不能为空"));
        }
        
        try {
            String taskId = ocrTaskService.submitOCRTask(pdfPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "任务提交成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("提交任务失败: " + e.getMessage()));
        }
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        OCRTask task = ocrTaskService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", task.getTaskId());
        response.put("status", task.getStatus().name());
        response.put("statusDesc", task.getStatus().getDescription());
        response.put("progress", task.getProgress());
        response.put("currentPage", task.getCurrentPage());
        response.put("totalPages", task.getTotalPages());
        response.put("message", task.getMessage());
        response.put("createdTime", task.getCreatedTime());
        response.put("startTime", task.getStartTime());
        response.put("completedTime", task.getCompletedTime());
        
        if (task.getErrorMessage() != null) {
            response.put("errorMessage", task.getErrorMessage());
        }
        
        if (task.getResultPath() != null) {
            response.put("resultPath", task.getResultPath());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取任务结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Object> getTaskResult(@PathVariable String taskId) {
        OCRTask task = ocrTaskService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (task.getStatus() != OCRTask.TaskStatus.COMPLETED) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "任务尚未完成，当前状态: " + task.getStatus().getDescription());
            response.put("status", task.getStatus().name());
            return ResponseEntity.ok(response);
        }
        
        try {
            Object result = javaOCR.readOCRResult(taskId);
            if (result == null) {
                return ResponseEntity.internalServerError().body(createErrorResponse("读取结果文件失败"));
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("获取结果失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<OCRTask>> getAllTasks() {
        List<OCRTask> tasks = ocrTaskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * 清理过期任务
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupTasks() {
        try {
            ocrTaskService.cleanupExpiredTasks();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "清理完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("清理失败: " + e.getMessage()));
        }
    }
    
    /**
     * Python脚本查询任务进度接口
     */
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<String> getTaskProgress(@PathVariable String taskId) {
        String progress = ocrTaskService.queryTaskProgress(taskId);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
