package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PDF抽取控制器
 *
 * @author zhaoxinms
 */
@Slf4j
@RestController
@RequestMapping("/ai/pdf")
public class PdfExtractController {

    @Autowired
    private OpenAiService openAiService;
    
    @Autowired
    private AiProperties aiProperties;
    
    @Autowired
    private AiLimitUtil aiLimitUtil;
    
    // 存储抽取任务状态
    private final Map<String, Map<String, Object>> extractTasks = new ConcurrentHashMap<>();

    /**
     * 从PDF文件中抽取文本
     *
     * @param file 上传的PDF文件
     * @return 抽取结果
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(@RequestParam("file") MultipartFile file) {
        log.info("收到PDF抽取请求，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        // 检查限流
        if (!aiLimitUtil.tryAcquire("system")) {
            return ResponseEntity.ok(createResponse(false, "请求过于频繁，请稍后再试", null));
        }
        
        try {
            // 检查文件类型
            if (file.isEmpty()) {
                return ResponseEntity.ok(createResponse(false, "请选择文件", null));
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.ok(createResponse(false, "只支持PDF文件格式", null));
            }
            
            // 检查文件大小
            if (file.getSize() > aiProperties.getPdf().getMaxFileSize()) {
                return ResponseEntity.ok(createResponse(false, 
                        "文件大小超过限制：" + aiProperties.getPdf().getMaxFileSize() / 1024 / 1024 + "MB", null));
            }
            
            // 创建任务ID
            String taskId = UUID.randomUUID().toString();
            
            // 创建任务状态
            Map<String, Object> taskStatus = new HashMap<>();
            taskStatus.put("status", "processing");
            taskStatus.put("fileName", fileName);
            taskStatus.put("fileSize", file.getSize());
            taskStatus.put("startTime", System.currentTimeMillis());
            extractTasks.put(taskId, taskStatus);
            
            // 异步处理PDF抽取
            new Thread(() -> {
                try {
                    // 抽取文本
                    String extractedText = openAiService.extractTextFromPdf(file.getBytes());
                    
                    // 更新任务状态
                    taskStatus.put("status", "completed");
                    taskStatus.put("result", extractedText);
                    taskStatus.put("endTime", System.currentTimeMillis());
                } catch (IOException e) {
                    log.error("PDF抽取失败", e);
                    taskStatus.put("status", "failed");
                    taskStatus.put("error", e.getMessage());
                    taskStatus.put("endTime", System.currentTimeMillis());
                }
            }).start();
            
            // 返回任务ID
            Map<String, Object> result = createResponse(true, "PDF抽取任务已提交", null);
            result.put("taskId", taskId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理PDF抽取请求时发生错误", e);
            return ResponseEntity.ok(createResponse(false, "服务器错误: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取抽取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        if (!extractTasks.containsKey(taskId)) {
            return ResponseEntity.ok(createResponse(false, "任务不存在", null));
        }
        
        Map<String, Object> taskStatus = extractTasks.get(taskId);
        Map<String, Object> result = createResponse(true, "成功", null);
        result.put("task", taskStatus);
        
        // 如果任务已完成或失败，并且已经过了一段时间，则从缓存中移除
        String status = (String) taskStatus.get("status");
        if (("completed".equals(status) || "failed".equals(status)) && 
                taskStatus.containsKey("endTime")) {
            long endTime = (long) taskStatus.get("endTime");
            if (System.currentTimeMillis() - endTime > 1000 * 60 * 30) { // 30分钟后清除
                extractTasks.remove(taskId);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 创建响应对象
     *
     * @param success 是否成功
     * @param message 消息
     * @param data 数据
     * @return 响应对象
     */
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}