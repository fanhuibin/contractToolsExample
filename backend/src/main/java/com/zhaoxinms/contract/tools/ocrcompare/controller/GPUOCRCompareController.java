package com.zhaoxinms.contract.tools.ocrcompare.controller;

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

import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.ocrcompare.compare.GPUOCRCompareOptions;
import com.zhaoxinms.contract.tools.ocrcompare.compare.GPUOCRCompareService;
import com.zhaoxinms.contract.tools.ocrcompare.compare.GPUOCRCompareTask;
import com.zhaoxinms.contract.tools.ocrcompare.concurrent.GPUOCRTaskQueue;

/**
 * GPU OCR比对控制器 - 基于DotsOcrCompareDemoTest的完整比对功能
 */
@RestController
@RequestMapping("/api/gpu-ocr-compare")
public class GPUOCRCompareController {

    @Autowired
    private GPUOCRCompareService gpuOcrCompareService;

    /**
     * 提交GPU OCR比对任务（使用文件上传）
     */
    @PostMapping("/submit")
    public ResponseEntity<Result<Map<String, String>>> submitCompareTask(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(value = "ignoreHeaderFooter", defaultValue = "true") boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightPercent", defaultValue = "12.0") double headerHeightPercent,
            @RequestParam(value = "footerHeightPercent", defaultValue = "12.0") double footerHeightPercent,
            @RequestParam(value = "ignoreCase", defaultValue = "true") boolean ignoreCase,
            @RequestParam(value = "ignoredSymbols", defaultValue = "_＿") String ignoredSymbols,
            @RequestParam(value = "ignoreSpaces", defaultValue = "false") boolean ignoreSpaces,
            @RequestParam(value = "ignoreSeals", defaultValue = "true") boolean ignoreSeals) {

        try {
            // 创建比对选项
            GPUOCRCompareOptions options = new GPUOCRCompareOptions();
            options.setIgnoreHeaderFooter(ignoreHeaderFooter);
            options.setHeaderHeightPercent(headerHeightPercent);
            options.setFooterHeightPercent(footerHeightPercent);
            options.setIgnoreCase(ignoreCase);
            options.setIgnoredSymbols(ignoredSymbols);
            options.setIgnoreSpaces(ignoreSpaces);
            options.setIgnoreSeals(ignoreSeals);

            // 提交比对任务
            String taskId = gpuOcrCompareService.submitCompareTask(oldFile, newFile, options);

            Map<String, String> data = new HashMap<>();
            data.put("taskId", taskId);

            return ResponseEntity.ok(Result.success("GPU OCR比对任务提交成功", data));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("提交GPU OCR比对任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取比对任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Result<GPUOCRCompareTask>> getTaskStatus(@PathVariable String taskId) {
        try {
            GPUOCRCompareTask task = gpuOcrCompareService.getTaskStatus(taskId);
            return ResponseEntity.ok(Result.success("获取任务状态成功", task));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取任务状态失败: " + e.getMessage()));
        }
    }


    /**
     * 获取所有比对任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<Result<List<GPUOCRCompareTask>>> getAllTasks() {
        try {
            List<GPUOCRCompareTask> tasks = gpuOcrCompareService.getAllTasks();
            return ResponseEntity.ok(Result.success("获取任务列表成功", tasks));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取任务列表失败: " + e.getMessage()));
        }
    }

    /**
     * 删除比对任务
     */
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Result<Void>> deleteTask(@PathVariable String taskId) {
        try {
            boolean deleted = gpuOcrCompareService.deleteTask(taskId);
            if (deleted) {
                return ResponseEntity.ok(Result.success("删除成功", null));
            } else {
                return ResponseEntity.ok(Result.error(404, "任务不存在或已删除"));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("删除任务失败: " + e.getMessage()));
        }
    }

    /**
     * 调试模式：使用已有的OCR任务ID进行比对
     */
    @PostMapping("/debug-compare")
    public ResponseEntity<Result<Map<String, String>>> debugCompare(@RequestBody Map<String, Object> request) {
        try {
            String taskId = (String) request.get("taskId");
            @SuppressWarnings("unchecked")
            Map<String, Object> optionsMap = (Map<String, Object>) request.get("options");

            GPUOCRCompareOptions options = new GPUOCRCompareOptions();
            if (optionsMap != null) {
                options.setIgnoreHeaderFooter(Boolean.TRUE.equals(optionsMap.get("ignoreHeaderFooter")));
                options.setHeaderHeightPercent(((Number) optionsMap.getOrDefault("headerHeightPercent", 12.0)).doubleValue());
                options.setFooterHeightPercent(((Number) optionsMap.getOrDefault("footerHeightPercent", 12.0)).doubleValue());
                options.setIgnoreCase(Boolean.TRUE.equals(optionsMap.get("ignoreCase")));
                options.setIgnoredSymbols((String) optionsMap.getOrDefault("ignoredSymbols", "_＿"));
                options.setIgnoreSpaces(Boolean.TRUE.equals(optionsMap.get("ignoreSpaces")));
                options.setIgnoreSeals(Boolean.TRUE.equals(optionsMap.get("ignoreSeals")));
            }

            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error("taskId不能为空"));
            }

            String debugTaskId = gpuOcrCompareService.debugCompareWithTaskId(taskId, options);

            Map<String, String> data = new HashMap<>();
            data.put("taskId", debugTaskId);

            return ResponseEntity.ok(Result.success("调试比对任务提交成功", data));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("调试比对失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Canvas版本的比对结果（包含图片信息和原始坐标）
     */
    @GetMapping("/canvas-result/{taskId}")
    public ResponseEntity<Result<Map<String, Object>>> getCanvasCompareResult(@PathVariable String taskId) {
        try {
            GPUOCRCompareTask task = gpuOcrCompareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != GPUOCRCompareTask.Status.COMPLETED) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
                responseData.put("status", task.getStatus().name());
                return ResponseEntity.ok(new Result<>(202, "任务尚未完成", responseData));
            }

            // 获取Canvas版本的比对结果
            Map<String, Object> canvasResult = gpuOcrCompareService.getCanvasFrontendResult(taskId);
            if (canvasResult != null) {
                return ResponseEntity.ok(Result.success("获取Canvas比对结果成功", canvasResult));
            }

            return ResponseEntity.ok(Result.error("Canvas比对结果不存在"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取Canvas比对结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文档图片信息
     */
    @GetMapping("/images/{taskId}/{mode}")
    public ResponseEntity<Result<GPUOCRCompareService.DocumentImageInfo>> getDocumentImages(
            @PathVariable String taskId, 
            @PathVariable String mode) {
        try {
            GPUOCRCompareTask task = gpuOcrCompareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != GPUOCRCompareTask.Status.COMPLETED) {
                return ResponseEntity.ok(Result.error(202, "任务尚未完成"));
            }

            // 验证mode参数
            if (!mode.equals("old") && !mode.equals("new")) {
                return ResponseEntity.badRequest().body(Result.error("mode参数必须是old或new"));
            }

            GPUOCRCompareService.DocumentImageInfo imageInfo = gpuOcrCompareService.getDocumentImageInfo(taskId, mode);
            return ResponseEntity.ok(Result.success("获取文档图片信息成功", imageInfo));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取文档图片信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取原始坐标数据（用于调试坐标转换）
     */
    @GetMapping("/debug/raw-coords/{taskId}")
    public ResponseEntity<Result<Map<String, Object>>> getRawCoordinates(@PathVariable String taskId) {
        try {
            GPUOCRCompareTask task = gpuOcrCompareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != GPUOCRCompareTask.Status.COMPLETED) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
                responseData.put("status", task.getStatus().name());
                return ResponseEntity.ok(new Result<>(202, "任务尚未完成", responseData));
            }

            // 获取原始坐标数据（未经转换的）
            Map<String, Object> rawResult = gpuOcrCompareService.getRawFrontendResult(taskId);
            if (rawResult != null) {
                return ResponseEntity.ok(Result.success("获取原始坐标数据成功", rawResult));
            }

            return ResponseEntity.ok(Result.error("原始坐标数据不存在"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取原始坐标数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取任务队列状态
     */
    @GetMapping("/queue/stats")
    public ResponseEntity<Result<GPUOCRTaskQueue.TaskQueueStats>> getQueueStats() {
        try {
            GPUOCRTaskQueue.TaskQueueStats stats = gpuOcrCompareService.getQueueStats();
            return ResponseEntity.ok(Result.success("获取队列状态成功", stats));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取队列状态失败: " + e.getMessage()));
        }
    }

    /**
     * 检查队列是否繁忙
     */
    @GetMapping("/queue/busy")
    public ResponseEntity<Result<Map<String, Object>>> checkQueueBusy() {
        try {
            boolean isBusy = gpuOcrCompareService.isQueueBusy();
            GPUOCRTaskQueue.TaskQueueStats stats = gpuOcrCompareService.getQueueStats();
            
            Map<String, Object> data = new HashMap<>();
            data.put("isBusy", isBusy);
            data.put("queueSize", stats.getCurrentQueueSize());
            data.put("activeThreads", stats.getActiveThreads());
            data.put("maxThreads", stats.getMaxThreads());
            
            return ResponseEntity.ok(Result.success("获取队列繁忙状态成功", data));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取队列繁忙状态失败: " + e.getMessage()));
        }
    }

    /**
     * 动态调整最大并发线程数
     */
    @PostMapping("/queue/adjust-concurrency")
    public ResponseEntity<Result<String>> adjustMaxConcurrency(@RequestParam int maxThreads) {
        try {
            if (maxThreads < 1 || maxThreads > 20) {
                return ResponseEntity.badRequest().body(Result.error("线程数必须在1-20之间"));
            }
            
            gpuOcrCompareService.adjustMaxConcurrency(maxThreads);
            return ResponseEntity.ok(Result.success("调整并发线程数成功", "最大线程数已设置为: " + maxThreads));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("调整并发线程数失败: " + e.getMessage()));
        }
    }
}
