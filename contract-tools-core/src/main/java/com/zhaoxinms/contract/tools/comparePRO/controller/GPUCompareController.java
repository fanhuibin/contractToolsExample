package com.zhaoxinms.contract.tools.comparePRO.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import io.swagger.annotations.Api;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareUrlRequest;
import com.zhaoxinms.contract.tools.comparePRO.service.CompareImageService;
import com.zhaoxinms.contract.tools.comparePRO.service.CompareService;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import com.zhaoxinms.contract.tools.comparePRO.util.CompareTaskQueue;
import com.zhaoxinms.contract.tools.comparePRO.util.FileDownloadUtil;

/**
 * 智能文档比对控制器
 * 
 * 基于GPU OCR的文档智能比对功能
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@RestController
@RequestMapping("/api/compare-pro")
@RequireFeature(module = ModuleType.SMART_DOCUMENT_COMPARE, message = "智能文档比对功能需要授权")
@Api(tags = "智能文档比对")
public class GPUCompareController {

    private static final Logger log = LoggerFactory.getLogger(GPUCompareController.class);

    @Autowired
    private CompareService compareService;
    
    @Autowired
    private CompareImageService imageService;
    

    /**
     * 提交GPU OCR比对任务（使用文件上传）
     */
    @PostMapping("/submit")
    public ApiResponse<String> submitCompareTask(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(value = "ignoreHeaderFooter", defaultValue = "true") boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightPercent", defaultValue = "12.0") double headerHeightPercent,
            @RequestParam(value = "footerHeightPercent", defaultValue = "12.0") double footerHeightPercent,
            @RequestParam(value = "ignoreCase", defaultValue = "true") boolean ignoreCase,
            @RequestParam(value = "ignoredSymbols", defaultValue = "_＿") String ignoredSymbols,
            @RequestParam(value = "ignoreSpaces", defaultValue = "false") boolean ignoreSpaces,
            @RequestParam(value = "ignoreSeals", defaultValue = "true") boolean ignoreSeals,
            @RequestParam(value = "removeWatermark", defaultValue = "false") boolean removeWatermark,
            @RequestParam(value = "watermarkRemovalStrength", defaultValue = "smart") String watermarkRemovalStrength,
            @RequestParam(value = "ocrServiceType", defaultValue = "dotsocr") String ocrServiceType) {

        try {
            // 调试日志：记录接收到的去水印参数
            System.out.println("Controller接收到的去水印参数: " + removeWatermark + ", 强度: " + watermarkRemovalStrength);
            
            // 创建比对选项
            CompareOptions options = new CompareOptions();
            options.setIgnoreHeaderFooter(ignoreHeaderFooter);
            options.setHeaderHeightPercent(headerHeightPercent);
            options.setFooterHeightPercent(footerHeightPercent);
            options.setIgnoreCase(ignoreCase);
            options.setIgnoredSymbols(ignoredSymbols);
            options.setIgnoreSpaces(ignoreSpaces);
            options.setIgnoreSeals(ignoreSeals);
            options.setRemoveWatermark(removeWatermark);
            options.setWatermarkRemovalStrength(watermarkRemovalStrength);
            options.setOcrServiceType(ocrServiceType);

            // 提交比对任务
            String taskId = compareService.submitCompareTask(oldFile, newFile, options);

            // 直接返回taskId字符串
            return ApiResponse.success("GPU OCR比对任务提交成功", taskId);

        } catch (Exception e) {
            return ApiResponse.<String>serverError().errorDetail("提交GPU OCR比对任务失败: " + e.getMessage());
        }
    }

    /**
     * 提交合同比对任务（使用JSON + URL格式）- 对外接口
     */
    @PostMapping("/submit-url")
    public ApiResponse<String> submitCompareTaskByUrl(
            @RequestBody CompareUrlRequest request) {

        try {
            // 验证必需参数
            if (request.getOldFileUrl() == null || request.getOldFileUrl().trim().isEmpty()) {
                return ApiResponse.paramError("缺少必需参数: oldFileUrl");
            }
            if (request.getNewFileUrl() == null || request.getNewFileUrl().trim().isEmpty()) {
                return ApiResponse.paramError("缺少必需参数: newFileUrl");
            }

            // 下载文件
            MultipartFile oldFile;
            MultipartFile newFile;
            
            try {
                oldFile = FileDownloadUtil.downloadFromUrl(request.getOldFileUrl(), "oldFile");
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message.contains("文件格式") || message.contains("PDF格式")) {
                    return ApiResponse.paramError("原文档格式不支持，仅支持PDF格式");
                }
                return ApiResponse.paramError("原文档URL无效: " + e.getMessage());
            } catch (IOException e) {
                String message = e.getMessage();
                if (message.contains("文件大小超过") || message.contains("超过限制")) {
                    return ApiResponse.paramError("原文档文件过大，最大支持50MB");
                } else if (message.contains("超时") || message.contains("timeout")) {
                    return ApiResponse.paramError("原文档下载超时");
                }
                return ApiResponse.paramError("原文档下载失败");
            } catch (Exception e) {
                return ApiResponse.paramError("无法访问原文档URL: " + request.getOldFileUrl());
            }
            
            try {
                newFile = FileDownloadUtil.downloadFromUrl(request.getNewFileUrl(), "newFile");
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message.contains("文件格式") || message.contains("PDF格式")) {
                    return ApiResponse.paramError("新文档格式不支持，仅支持PDF格式");
                }
                return ApiResponse.paramError("新文档URL无效: " + e.getMessage());
            } catch (IOException e) {
                String message = e.getMessage();
                if (message.contains("文件大小超过") || message.contains("超过限制")) {
                    return ApiResponse.paramError("新文档文件过大，最大支持50MB");
                } else if (message.contains("超时") || message.contains("timeout")) {
                    return ApiResponse.paramError("新文档下载超时");
                }
                return ApiResponse.paramError("新文档下载失败");
            } catch (Exception e) {
                return ApiResponse.paramError("无法访问新文档URL: " + request.getNewFileUrl());
            }

            // 转换比对选项
            CompareOptions options = request.toCompareOptions();

            // 提交比对任务
            String taskId = compareService.submitCompareTask(oldFile, newFile, options);

            // 直接返回taskId字符串
            return ApiResponse.success("合同比对pro版任务提交成功", taskId);

        } catch (Exception e) {
            return ApiResponse.<String>serverError().errorDetail("提交合同比对任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取比对任务状态（包含进度信息）
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);
            if (task == null) {
                return ApiResponse.notFound("任务不存在");
            }
            
            // 构建返回数据
            Map<String, Object> responseData = new HashMap<>();
            
            // 基本任务信息
            responseData.put("taskId", task.getTaskId());
            responseData.put("status", task.getStatus().name());
            responseData.put("statusDescription", task.getStatus().getDescription());
            responseData.put("oldFileName", task.getOldFileName());
            responseData.put("newFileName", task.getNewFileName());
            responseData.put("currentStep", task.getCurrentStep());
            responseData.put("currentStepDesc", task.getCurrentStepDesc());
            
            // 简化的进度信息（仅基于步骤）
            responseData.put("progress", task.getProgress());
            
            // 页面级别进度信息
            responseData.put("totalPages", task.getTotalPages());
            responseData.put("oldDocPages", task.getOldDocPages());
            responseData.put("newDocPages", task.getNewDocPages());
            responseData.put("currentPageOld", task.getCurrentPageOld());
            responseData.put("currentPageNew", task.getCurrentPageNew());
            responseData.put("completedPagesOld", task.getCompletedPagesOld());
            responseData.put("completedPagesNew", task.getCompletedPagesNew());
            
            // OCR预估时间信息
            if (task.getEstimatedOcrTimeOld() != null) {
                responseData.put("estimatedOcrTimeOld", task.getEstimatedOcrTimeOld());
            }
            if (task.getEstimatedOcrTimeNew() != null) {
                responseData.put("estimatedOcrTimeNew", task.getEstimatedOcrTimeNew());
            }
            
            // 时间统计（如果任务已完成或进行中）
            if (task.getStartTime() != null) {
                responseData.put("startTime", task.getStartTime().toString());
            }
            if (task.getEndTime() != null) {
                responseData.put("endTime", task.getEndTime().toString());
            }
            if (task.getTotalDuration() != null) {
                responseData.put("totalDuration", task.getTotalDuration());
            }
            if (task.getStepDurations() != null && !task.getStepDurations().isEmpty()) {
                responseData.put("stepDurations", task.getStepDurations());
            }
            
            // 错误信息（如果有）
            if (task.getErrorMessage() != null && !task.getErrorMessage().isEmpty()) {
                responseData.put("errorMessage", task.getErrorMessage());
            }
            
            // 失败页面信息（如果有）
            if (task.getFailedPages() != null && !task.getFailedPages().isEmpty()) {
                responseData.put("failedPages", task.getFailedPages());
                responseData.put("failedPagesCount", task.getFailedPages().size());
            }
            
            return ApiResponse.success("获取任务状态成功", responseData);

        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail("获取任务状态失败: " + e.getMessage());
        }
    }


    /**
     * 获取所有比对任务（简化版本，只返回核心信息）
     */
    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> getAllTasks() {
        try {
            List<CompareTask> tasks = compareService.getAllTasks();
            List<Map<String, Object>> simplifiedTasks = new ArrayList<>();
            
            for (CompareTask task : tasks) {
                Map<String, Object> taskInfo = new HashMap<>();
                
                // 基本信息
                taskInfo.put("taskId", task.getTaskId());
                taskInfo.put("oldFileName", task.getOldFileName());
                taskInfo.put("newFileName", task.getNewFileName());
                taskInfo.put("startTime", task.getStartTime());
                taskInfo.put("endTime", task.getEndTime());
                
                // 差异总数和结果页地址 - 只有完成的任务才有
                if (task.isCompleted()) {
                    // 从比对结果中获取差异总数
                    com.zhaoxinms.contract.tools.comparePRO.model.CompareResult result = compareService.getCompareResult(task.getTaskId());
                    if (result != null) {
                        taskInfo.put("differenceCount", result.getTotalDiffCount());
                    } else {
                        taskInfo.put("differenceCount", 0);
                    }
                    taskInfo.put("resultUrl", "/api/compare-pro/canvas-result/" + task.getTaskId());
                } else {
                    taskInfo.put("differenceCount", null);
                    taskInfo.put("resultUrl", null);
                }
                
                simplifiedTasks.add(taskInfo);
            }
            
            // 按开始时间倒序排列（最新的在前面）
            simplifiedTasks.sort((a, b) -> {
                java.time.LocalDateTime timeA = (java.time.LocalDateTime) a.get("startTime");
                java.time.LocalDateTime timeB = (java.time.LocalDateTime) b.get("startTime");
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            });
            
            return ApiResponse.success("获取任务列表成功", simplifiedTasks);

        } catch (Exception e) {
            return ApiResponse.<List<Map<String, Object>>>serverError().errorDetail("获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除比对任务
     */
    @DeleteMapping("/task/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
        try {
            boolean deleted = compareService.deleteTask(taskId);
            if (deleted) {
                return ApiResponse.success("删除成功", null);
            } else {
                return ApiResponse.<Void>notFound("任务不存在或已删除");
            }

        } catch (Exception e) {
            return ApiResponse.<Void>serverError().errorDetail("删除任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取Canvas版本的比对结果（包含图片信息和原始坐标）
     */
    @GetMapping("/canvas-result/{taskId}")
    public ApiResponse<Map<String, Object>> getCanvasCompareResult(@PathVariable String taskId) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ApiResponse.notFound("任务不存在");
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                return ApiResponse.fail(202, "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
            }

            // 获取Canvas版本的比对结果
            Map<String, Object> canvasResult = compareService.getCanvasFrontendResult(taskId);
            if (canvasResult != null) {
                return ApiResponse.success("获取Canvas比对结果成功", canvasResult);
            }

            return ApiResponse.<Map<String, Object>>businessError("Canvas比对结果不存在");

        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail("获取Canvas比对结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档图片信息
     */
    @GetMapping("/images/{taskId}/{mode}")
    public ApiResponse<CompareImageService.DocumentImageInfo> getDocumentImages(
            @PathVariable String taskId, 
            @PathVariable String mode) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ApiResponse.notFound("任务不存在");
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                return ApiResponse.businessError("任务尚未完成");
            }

            // 验证mode参数
            if (!mode.equals("old") && !mode.equals("new")) {
                return ApiResponse.paramError("mode参数必须是old或new");
            }

            CompareImageService.DocumentImageInfo imageInfo = imageService.getDocumentImageInfo(taskId, mode);
            return ApiResponse.success("获取文档图片信息成功", imageInfo);

        } catch (Exception e) {
            return ApiResponse.<CompareImageService.DocumentImageInfo>serverError().errorDetail("获取文档图片信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取原始坐标数据（用于调试坐标转换）
     */
    @GetMapping("/debug/raw-coords/{taskId}")
    public ApiResponse<Map<String, Object>> getRawCoordinates(@PathVariable String taskId) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ApiResponse.notFound("任务不存在");
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
                responseData.put("status", task.getStatus().name());
                return ApiResponse.fail(202, "任务尚未完成");
            }

            // 获取原始坐标数据（未经转换的）
            Map<String, Object> rawResult = compareService.getRawFrontendResult(taskId);
            if (rawResult != null) {
                return ApiResponse.success("获取原始坐标数据成功", rawResult);
            }

            return ApiResponse.<Map<String, Object>>businessError("原始坐标数据不存在");

        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail("获取原始坐标数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务队列状态
     */
    @GetMapping("/queue/stats")
    public ApiResponse<CompareTaskQueue.TaskQueueStats> getQueueStats() {
        try {
            CompareTaskQueue.TaskQueueStats stats = compareService.getQueueStats();
            return ApiResponse.success("获取队列状态成功", stats);

        } catch (Exception e) {
            return ApiResponse.<CompareTaskQueue.TaskQueueStats>serverError().errorDetail("获取队列状态失败: " + e.getMessage());
        }
    }

    /**
     * 检查队列是否繁忙
     */
    @GetMapping("/queue/busy")
    public ApiResponse<Map<String, Object>> checkQueueBusy() {
        try {
            boolean isBusy = compareService.isQueueBusy();
            CompareTaskQueue.TaskQueueStats stats = compareService.getQueueStats();
            
            Map<String, Object> data = new HashMap<>();
            data.put("isBusy", isBusy);
            data.put("queueSize", stats.getCurrentQueueSize());
            data.put("activeThreads", stats.getActiveThreads());
            data.put("maxThreads", stats.getMaxThreads());
            
            return ApiResponse.success("获取队列繁忙状态成功", data);

        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail("获取队列繁忙状态失败: " + e.getMessage());
        }
    }

    /**
     * 动态调整最大并发线程数
     */
    @PostMapping("/queue/adjust-concurrency")
    public ApiResponse<String> adjustMaxConcurrency(@RequestParam int maxThreads) {
        try {
            if (maxThreads < 1 || maxThreads > 20) {
                return ApiResponse.paramError("线程数必须在1-20之间");
            }
            
            compareService.adjustMaxConcurrency(maxThreads);
            return ApiResponse.success("调整并发线程数成功", "最大线程数已设置为: " + maxThreads);

        } catch (Exception e) {
            return ApiResponse.<String>serverError().errorDetail("调整并发线程数失败: " + e.getMessage());
        }
    }

    /**
     * 导出比对报告
     */
    @PostMapping("/export-report")
    public ResponseEntity<?> exportReport(@RequestBody ExportRequest request) {
        try {
            // 验证请求参数
            if (request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.paramError("任务ID不能为空"));
            }
            
            if (request.getFormats() == null || request.getFormats().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.paramError("导出格式不能为空"));
            }

            // 转换为service层DTO
            com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest serviceRequest = convertToServiceRequest(request);
            
            // 调用导出服务
            byte[] exportData = compareService.exportReport(serviceRequest);
            
            // 确定文件名和Content-Type
            String filename;
            String contentType;
            
            if (request.getFormats().size() == 1) {
                String format = request.getFormats().get(0);
                if ("html".equals(format)) {
                    filename = "比对报告_" + request.getTaskId() + ".zip";
                    contentType = "application/zip";
                } else if ("doc".equals(format)) {
                    filename = "比对报告_" + request.getTaskId() + ".docx";
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.paramError("不支持的导出格式: " + format));
                }
            } else {
                // 多种格式，返回ZIP
                filename = "比对报告_" + request.getTaskId() + ".zip";
                contentType = "application/zip";
            }

            // 对文件名进行URL编码以支持中文
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .header("Content-Type", contentType)
                    .body(exportData);

        } catch (Exception e) {
            log.error("导出比对报告失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError().errorDetail("导出失败: " + e.getMessage()));
        }
    }

    /**
     * 保存用户修改（忽略差异、添加备注）
     */
    @PostMapping("/save-user-modifications/{taskId}")
    public ApiResponse<Void> saveUserModifications(
            @PathVariable String taskId,
            @RequestBody UserModificationsRequest request) {
        try {
            compareService.saveUserModifications(taskId, request);
            return ApiResponse.success("用户修改已保存", null);
        } catch (Exception e) {
            log.error("保存用户修改失败", e);
            return ApiResponse.<Void>serverError().errorDetail("保存用户修改失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户修改（页面刷新后恢复状态）
     */
    @GetMapping("/get-user-modifications/{taskId}")
    public ApiResponse<UserModificationsRequest> getUserModifications(@PathVariable String taskId) {
        try {
            UserModificationsRequest modifications = compareService.getUserModifications(taskId);
            return ApiResponse.success("获取用户修改成功", modifications);
        } catch (Exception e) {
            log.error("获取用户修改失败", e);
            return ApiResponse.<UserModificationsRequest>serverError().errorDetail("获取用户修改失败: " + e.getMessage());
        }
    }

    /**
     * 用户修改请求DTO
     */
    public static class UserModificationsRequest {
        private List<Integer> ignoredDifferences;
        private Map<Integer, String> remarks;

        // Getters and setters
        public List<Integer> getIgnoredDifferences() { return ignoredDifferences; }
        public void setIgnoredDifferences(List<Integer> ignoredDifferences) { this.ignoredDifferences = ignoredDifferences; }
        public Map<Integer, String> getRemarks() { return remarks; }
        public void setRemarks(Map<Integer, String> remarks) { this.remarks = remarks; }
    }

    /**
     * 导出请求DTO
     */
    public static class ExportRequest {
        private String taskId;
        private List<String> formats;
        private boolean includeIgnored = false;
        private boolean includeRemarks = true;

        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public List<String> getFormats() { return formats; }
        public void setFormats(List<String> formats) { this.formats = formats; }
        public boolean isIncludeIgnored() { return includeIgnored; }
        public void setIncludeIgnored(boolean includeIgnored) { this.includeIgnored = includeIgnored; }
        public boolean isIncludeRemarks() { return includeRemarks; }
        public void setIncludeRemarks(boolean includeRemarks) { this.includeRemarks = includeRemarks; }
    }
    
    /**
     * 将Controller层DTO转换为Service层DTO
     */
    private com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest convertToServiceRequest(ExportRequest controllerRequest) {
        com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest serviceRequest = 
            new com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest();
        
        serviceRequest.setTaskId(controllerRequest.getTaskId());
        serviceRequest.setFormats(controllerRequest.getFormats());
        serviceRequest.setIncludeImages(true); // 默认包含图片
        serviceRequest.setTitle("比对报告"); // 默认标题
        
        // 设置导出选项
        com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest.ExportOptions options = 
            new com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest.ExportOptions();
        options.setIncludeStatistics(true);
        options.setIncludeDetailedDiffs(!controllerRequest.isIncludeIgnored()); // 取反，因为语义相反
        options.setIncludePagePreview(controllerRequest.isIncludeRemarks());
        
        serviceRequest.setOptions(options);
        
        return serviceRequest;
    }
}
