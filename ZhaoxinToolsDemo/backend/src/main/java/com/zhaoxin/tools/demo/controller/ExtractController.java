package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.service.ZhaoxinApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 智能文档抽取控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/extract")
public class ExtractController {
    
    private final ZhaoxinApiClient apiClient;
    
    public ExtractController(ZhaoxinApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * 上传文档并开始抽取
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadAndExtract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") String templateId) {
        
        log.info("收到文档抽取请求: templateId={}, fileName={}", templateId, file.getOriginalFilename());
        return apiClient.submitExtractTask(file, templateId);
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public Map<String, Object> getStatus(@PathVariable String taskId) {
        log.info("查询任务状态: taskId={}", taskId);
        return apiClient.getExtractStatus(taskId);
    }
    
    /**
     * 获取抽取结果
     */
    @GetMapping("/result/{taskId}")
    public Map<String, Object> getResult(@PathVariable String taskId) {
        log.info("获取抽取结果: taskId={}", taskId);
        return apiClient.getExtractResult(taskId);
    }
    
    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskId}")
    public ResponseEntity<Void> cancelTask(@PathVariable String taskId) {
        log.info("取消任务: taskId={}", taskId);
        apiClient.cancelExtractTask(taskId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取任务列表
     */
    @GetMapping("/tasks")
    public Map<String, Object> getTasks() {
        log.info("获取任务列表");
        return apiClient.getExtractTasks();
    }
    
    /**
     * 获取页面图片
     */
    @GetMapping("/page-image/{taskId}/{pageNumber}")
    public ResponseEntity<byte[]> getPageImage(
            @PathVariable String taskId,
            @PathVariable int pageNumber) {
        
        log.info("获取页面图片: taskId={}, pageNumber={}", taskId, pageNumber);
        byte[] imageData = apiClient.getPageImage(taskId, pageNumber);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageData);
    }
    
    /**
     * 获取模板列表
     */
    @GetMapping("/templates")
    public Map<String, Object> getTemplates() {
        log.info("获取模板列表");
        return apiClient.getTemplates();
    }
}

