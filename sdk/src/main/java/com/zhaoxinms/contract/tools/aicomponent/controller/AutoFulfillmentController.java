package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/ai/auto-fulfillment")
@RequiredArgsConstructor
public class AutoFulfillmentController {

    private final AutoFulfillmentService autoFulfillmentService;
    private final AiLimitUtil aiLimitUtil;

    private final Map<String, Map<String, Object>> tasks = new ConcurrentHashMap<>();
    private static final String[] SUPPORTED_EXTENSIONS = new String[]{ ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png" };

    @PostMapping("/extract")
    public Result<Map<String, Object>> extract(@RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "templateId", required = false) Long templateId,
                                               @RequestParam(value = "prompt", required = false) String prompt,
                                               @RequestParam(value = "taskTypes", required = false) String taskTypes,
                                               @RequestParam(value = "keywords", required = false) String keywords) {
        if (!aiLimitUtil.tryAcquire("system")) {
            return Result.error("请求过于频繁，请稍后再试");
        }
        try {
            if (file.isEmpty()) return Result.error("请选择文件");
            String fileName = file.getOriginalFilename();
            if (fileName == null || !isSupported(fileName)) return Result.error("不支持的文件格式");
            String taskId = UUID.randomUUID().toString();
            Map<String, Object> status = new HashMap<>();
            status.put("status", "processing");
            status.put("fileName", fileName);
            status.put("fileSize", file.getSize());
            status.put("startTime", System.currentTimeMillis());
            tasks.put(taskId, status);

            byte[] bytes = file.getBytes();
            new Thread(() -> {
                Path tempFile = null;
                try {
                    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "auto-fulfillment");
                    if (!Files.exists(tempDir)) Files.createDirectories(tempDir);
                    tempFile = tempDir.resolve(fileName);
                    Files.write(tempFile, bytes);
                    String result = autoFulfillmentService.processFile(tempFile, prompt, templateId);
                    status.put("status", "completed");
                    status.put("result", result);
                    status.put("endTime", System.currentTimeMillis());
                } catch (Exception e) {
                    log.error("自动履约任务识别失败", e);
                    status.put("status", "failed");
                    status.put("error", e.getMessage());
                    status.put("endTime", System.currentTimeMillis());
                } finally {
                    if (tempFile != null) try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                }
            }).start();

            Map<String, Object> resp = new HashMap<>();
            resp.put("taskId", taskId);
            return Result.success("任务已提交", resp);
        } catch (Exception e) {
            log.error("处理自动履约任务识别请求失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @GetMapping("/status/{taskId}")
    public Result<Map<String, Object>> status(@PathVariable String taskId) {
        if (!tasks.containsKey(taskId)) return Result.error("任务不存在");
        Map<String, Object> s = tasks.get(taskId);
        Map<String, Object> r = new HashMap<>();
        r.put("task", s);
        String st = (String) s.get("status");
        if (("completed".equals(st) || "failed".equals(st)) && s.containsKey("endTime")) {
            long end = (long) s.get("endTime");
            if (System.currentTimeMillis() - end > 1000 * 60 * 30) tasks.remove(taskId);
        }
        return Result.success("成功", r);
    }

    private boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) if (lower.endsWith(ext)) return true;
        return false;
    }
}


