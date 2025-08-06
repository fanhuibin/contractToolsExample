package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合同信息提取控制器
 *
 * @author zhaoxinms
 */
@Slf4j
@RestController
@RequestMapping("/ai/contract")
public class ContractExtractController {

    @Autowired
    private ContractExtractService contractExtractService;
    
    @Autowired
    private AiProperties aiProperties;
    
    @Autowired
    private AiLimitUtil aiLimitUtil;
    
    // 存储抽取任务状态
    private final Map<String, Map<String, Object>> extractTasks = new ConcurrentHashMap<>();

    // 支持的文件类型
    private final String[] SUPPORTED_EXTENSIONS = {
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png"
    };

    /**
     * 从文件中提取合同信息
     *
     * @param file 上传的文件
     * @param prompt 可选的提取提示
     * @return 抽取结果
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractInfo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt) {
        
        log.info("收到合同信息提取请求，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        // 检查限流
        if (!aiLimitUtil.tryAcquire("system")) {
            return ResponseEntity.ok(createResponse(false, "请求过于频繁，请稍后再试", null));
        }
        
        try {
            // 检查文件
            if (file.isEmpty()) {
                return ResponseEntity.ok(createResponse(false, "请选择文件", null));
            }
            
            // 检查文件类型
            String fileName = file.getOriginalFilename();
            if (fileName == null || !isSupportedFileType(fileName)) {
                return ResponseEntity.ok(createResponse(false, "不支持的文件格式，支持的格式有：PDF、Word、Excel、图片", null));
            }
            
            // 检查文件大小 - 增加到30MB
            long maxFileSize = 30 * 1024 * 1024; // 30MB
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.ok(createResponse(false, 
                        "文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB", null));
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
            
            // 异步处理文件提取
            new Thread(() -> {
                try {
                    // 保存文件到临时目录
                    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "contract-extract");
                    if (!Files.exists(tempDir)) {
                        Files.createDirectories(tempDir);
                    }
                    
                    Path tempFile = tempDir.resolve(fileName);
                    Files.write(tempFile, file.getBytes());
                    
                    // 提取信息
                    String extractedInfo = contractExtractService.processFile(tempFile, prompt);
                    
                    // 删除临时文件
                    Files.deleteIfExists(tempFile);
                    
                    // 更新任务状态
                    taskStatus.put("status", "completed");
                    taskStatus.put("result", extractedInfo);
                    taskStatus.put("endTime", System.currentTimeMillis());
                } catch (Exception e) {
                    log.error("合同信息提取失败", e);
                    taskStatus.put("status", "failed");
                    taskStatus.put("error", e.getMessage());
                    taskStatus.put("endTime", System.currentTimeMillis());
                }
            }).start();
            
            // 返回任务ID
            Map<String, Object> result = createResponse(true, "合同信息提取任务已提交", null);
            result.put("taskId", taskId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理合同信息提取请求时发生错误", e);
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
     * 检查文件类型是否支持
     * 
     * @param fileName 文件名
     * @return 是否支持
     */
    private boolean isSupportedFileType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
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