package com.zhaoxinms.contract.tools.ruleextract.controller;

import com.alibaba.fastjson2.JSONObject;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleExtractTaskModel;
import com.zhaoxinms.contract.tools.ruleextract.service.RuleExtractService;
import com.zhaoxinms.contract.tools.config.DemoModeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则抽取控制器（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@RestController
@RequestMapping("/api/rule-extract/extract")
@RequiredArgsConstructor
@RequireFeature(module = ModuleType.SMART_DOCUMENT_EXTRACTION, message = "智能文档抽取功能需要授权")
public class ExtractController {

    private final RuleExtractService extractService;
    private final DemoModeConfig demoModeConfig;

    /**
     * 上传文件并开始抽取
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadAndExtract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") String templateId,
            @RequestParam(value = "ocrProvider", required = false) String ocrProvider,
            @RequestParam(value = "ignoreHeaderFooter", required = false, defaultValue = "true") boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightPercent", required = false, defaultValue = "6.0") double headerHeightPercent,
            @RequestParam(value = "footerHeightPercent", required = false, defaultValue = "6.0") double footerHeightPercent) {
        try {
            log.info("收到规则提取请求: file={}, templateId={}, ignoreHeaderFooter={}", 
                file.getOriginalFilename(), templateId, ignoreHeaderFooter);
            String taskId = extractService.createTask(file, templateId, ocrProvider, 
                ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "任务创建成功");
            result.put("data", Map.of("taskId", taskId));
            return result;
        } catch (Exception e) {
            log.error("创建任务失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public Map<String, Object> getTaskStatus(@PathVariable String taskId) {
        try {
            RuleExtractTaskModel task = extractService.getTaskStatus(taskId);
            
            if (task == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 404);
                result.put("message", "任务不存在");
                return result;
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", task.getTaskId());
            data.put("status", task.getStatus());
            data.put("progress", task.getProgress());
            data.put("message", task.getMessage());
            data.put("fileName", task.getFileName());
            data.put("createdAt", task.getCreatedAt());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", data);
            return result;
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询任务结果
     */
    @GetMapping("/result/{taskId}")
    public Map<String, Object> getTaskResult(@PathVariable String taskId) {
        try {
            JSONObject taskResult = extractService.getTaskResult(taskId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", taskResult);
            return result;
        } catch (Exception e) {
            log.error("查询任务结果失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskId}")
    public Map<String, Object> cancelTask(@PathVariable String taskId) {
        try {
            extractService.cancelTask(taskId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "任务已取消");
            return result;
        } catch (Exception e) {
            log.error("取消任务失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询任务列表
     */
    @GetMapping("/tasks")
    public Map<String, Object> listTasks(
            @RequestParam(required = false) String templateId,
            @RequestParam(required = false) String status) {
        try {
            // 演示模式下隐藏任务历史
            if (demoModeConfig.shouldHideHistory()) {
                log.info("演示模式已启用且配置隐藏历史数据，返回空列表");
                Map<String, Object> result = new HashMap<>();
                result.put("code", 200);
                result.put("message", "演示模式下不显示任务历史");
                result.put("data", List.of());
                return result;
            }
            
            List<RuleExtractTaskModel> tasks = extractService.listTasks(templateId, status);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", tasks);
            return result;
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 获取页面图片
     */
    @GetMapping("/page-image/{taskId}/{pageNumber}")
    public ResponseEntity<Resource> getPageImage(
            @PathVariable String taskId,
            @PathVariable int pageNumber) {
        try {
            Resource imageResource = extractService.getPageImage(taskId, pageNumber);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"page-" + pageNumber + ".png\"")
                    .body(imageResource);
        } catch (Exception e) {
            log.error("获取页面图片失败: taskId={}, page={}", taskId, pageNumber, e);
            return ResponseEntity.notFound().build();
        }
    }
}

