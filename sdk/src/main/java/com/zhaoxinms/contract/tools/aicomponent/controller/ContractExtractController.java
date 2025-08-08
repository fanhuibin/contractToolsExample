package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractHistoryService;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
@RequestMapping("/api/ai/contract")
public class ContractExtractController {

    private final ContractExtractService contractExtractService;
    private final AiLimitUtil aiLimitUtil;
    private final ContractExtractHistoryService contractExtractHistoryService;

    @Autowired
    public ContractExtractController(
            @Qualifier("contractExtractServiceImpl") ContractExtractService contractExtractService,
            AiLimitUtil aiLimitUtil,
            @Qualifier("contractExtractHistoryServiceImpl") ContractExtractHistoryService contractExtractHistoryService) {
        this.contractExtractService = contractExtractService;
        this.aiLimitUtil = aiLimitUtil;
        this.contractExtractHistoryService = contractExtractHistoryService;
    }

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
    public Result<Map<String, Object>> extractInfo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam(value = "templateId", required = false) Long templateId) {
        
        log.info("收到合同信息提取请求，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        // 检查限流
        if (!aiLimitUtil.tryAcquire("system")) {
            return Result.error("请求过于频繁，请稍后再试");
        }
        
        try {
            // 检查文件
            if (file.isEmpty()) {
                return Result.error("请选择文件");
            }
            
            // 检查文件类型
            String fileName = file.getOriginalFilename();
            if (fileName == null || !isSupportedFileType(fileName)) {
                return Result.error("不支持的文件格式，支持的格式有：PDF、Word、Excel、图片");
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
            String finalFileName = fileName;
            byte[] fileBytes = file.getBytes(); // 先读取文件内容，避免文件流冲突
            new Thread(() -> {
                Path tempFile = null;
                try {
                    // 保存文件到临时目录
                    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "contract-extract");
                    if (!Files.exists(tempDir)) {
                        Files.createDirectories(tempDir);
                    }
                    
                    tempFile = tempDir.resolve(finalFileName);
                    Files.write(tempFile, fileBytes);
                    
                    // 提取信息
                    String extractedInfo = contractExtractService.processFile(tempFile, prompt, templateId);
                    
                    // 更新任务状态
                    taskStatus.put("status", "completed");
                    taskStatus.put("result", extractedInfo);
                    taskStatus.put("endTime", System.currentTimeMillis());

                    // 保存历史记录
                    // TODO: 获取真实用户ID
                    String userId = "default-user"; 
                    contractExtractHistoryService.saveHistory(finalFileName, extractedInfo, userId);
                    log.info("提取记录已保存，文件名: {}", finalFileName);

                } catch (Exception e) {
                    log.error("合同信息提取失败", e);
                    taskStatus.put("status", "failed");
                    taskStatus.put("error", e.getMessage());
                    taskStatus.put("endTime", System.currentTimeMillis());
                } finally {
                    // 确保临时文件被删除
                    if (tempFile != null) {
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (Exception e) {
                            log.warn("删除临时文件失败: {}", tempFile, e);
                        }
                    }
                }
            }).start();
            
            // 返回任务ID
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            return Result.success("合同信息提取任务已提交", result);
        } catch (Exception e) {
            log.error("处理合同信息提取请求时发生错误", e);
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
    // 统一结果返回，删除自定义的createResponse
}