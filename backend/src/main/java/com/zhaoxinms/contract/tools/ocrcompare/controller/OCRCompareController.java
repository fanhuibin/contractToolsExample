package com.zhaoxinms.contract.tools.ocrcompare.controller;

import com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareService;
import com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareTask;
import com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareResult;
import com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR比对控制器
 */
@RestController
@RequestMapping("/api/ocr-compare")
public class OCRCompareController {
    
    @Autowired
    private OCRCompareService ocrCompareService;
    
    /**
     * 提交比对任务（使用文件上传）
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitCompareTask(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(value = "ignoreCase", defaultValue = "false") boolean ignoreCase,
            @RequestParam(value = "ignoreWhitespace", defaultValue = "false") boolean ignoreWhitespace,
            @RequestParam(value = "ignorePunctuation", defaultValue = "false") boolean ignorePunctuation,
            @RequestParam(value = "similarityThreshold", defaultValue = "0.8") double similarityThreshold) {
        
        try {
            // 创建比对选项
            OCRCompareOptions options = new OCRCompareOptions();
            options.setIgnoreCase(ignoreCase);
            options.setIgnoreWhitespace(ignoreWhitespace);
            options.setIgnorePunctuation(ignorePunctuation);
            options.setSimilarityThreshold(similarityThreshold);
            
            // 提交比对任务
            String taskId = ocrCompareService.submitCompareTask(oldFile, newFile, options);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "比对任务提交成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提交比对任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 提交比对任务（使用文件路径）
     */
    @PostMapping("/submit-paths")
    public ResponseEntity<Map<String, Object>> submitCompareTaskWithPaths(
            @RequestBody Map<String, String> request) {
        
        String oldFilePath = request.get("oldFilePath");
        String newFilePath = request.get("newFilePath");
        
        if (oldFilePath == null || newFilePath == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件路径不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 创建默认比对选项
            OCRCompareOptions options = OCRCompareOptions.createDefault();
            
            // 提交比对任务
            String taskId = ocrCompareService.submitCompareTaskWithPaths(oldFilePath, newFilePath, options);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "比对任务提交成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提交比对任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 查询比对任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getCompareTaskStatus(@PathVariable String taskId) {
        OCRCompareTask task = ocrCompareService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", task.getTaskId());
        response.put("status", task.getStatus().name());
        response.put("statusDesc", task.getStatus().getDescription());
        response.put("progress", task.getProgress());
        response.put("currentStep", task.getCurrentStep());
        response.put("currentStepDescription", task.getCurrentStepDescription());
        response.put("createdTime", task.getCreatedTime());
        response.put("startTime", task.getStartTime());
        response.put("completedTime", task.getCompletedTime());
        response.put("oldOcrProgress", task.getOldOcrProgress());
        response.put("newOcrProgress", task.getNewOcrProgress());
        
        if (task.getErrorMessage() != null) {
            response.put("errorMessage", task.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取比对结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Object> getCompareResult(@PathVariable String taskId) {
        OCRCompareTask task = ocrCompareService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (task.getStatus() != OCRCompareTask.TaskStatus.COMPLETED) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
            response.put("status", task.getStatus().name());
            return ResponseEntity.ok(response);
        }
        
        OCRCompareResult result = ocrCompareService.getCompareResult(taskId);
        if (result == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "比对结果不存在");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有比对任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<OCRCompareTask>> getAllCompareTasks() {
        List<OCRCompareTask> tasks = ocrCompareService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * 删除比对任务
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteCompareTask(@PathVariable String taskId) {
        boolean deleted = ocrCompareService.deleteTask(taskId);
        
        Map<String, Object> response = new HashMap<>();
        if (deleted) {
            response.put("success", true);
            response.put("message", "任务删除成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "任务不存在或删除失败");
            return ResponseEntity.notFound().build();
        }
    }
}
