package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhaoxinms.contract.tools.common.Result;
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
@RequestMapping("/api/ai/pdf")
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
    public Result<Map<String, Object>> extractText(@RequestParam("file") MultipartFile file) {
        log.info("收到PDF抽取请求，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        // 检查限流
        if (!aiLimitUtil.tryAcquire("system")) {
            return Result.error("请求过于频繁，请稍后再试");
        }
        
        try {
            // 检查文件类型
            if (file.isEmpty()) {
                return Result.error("请选择文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                return Result.error("只支持PDF文件格式");
            }
            
            // 检查文件大小
            if (file.getSize() > aiProperties.getPdf().getMaxFileSize()) {
                return Result.error("文件大小超过限制：" + aiProperties.getPdf().getMaxFileSize() / 1024 / 1024 + "MB");
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
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            return Result.success("PDF抽取任务已提交", result);
        } catch (Exception e) {
            log.error("处理PDF抽取请求时发生错误", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取抽取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/status/{taskId}")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        if (!extractTasks.containsKey(taskId)) {
            return Result.error("任务不存在");
        }
        
        Map<String, Object> taskStatus = extractTasks.get(taskId);
        Map<String, Object> result = new HashMap<>();
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
        
        return Result.success("成功", result);
    }
    
    /**
     * 创建响应对象
     *
     * @param success 是否成功
     * @param message 消息
     * @param data 数据
     * @return 响应对象
     */
    // 统一返回格式，删除自定义的 createResponse
}