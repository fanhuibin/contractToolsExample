package com.zhaoxinms.contract.tools.comparePRO.controller;

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
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.service.CompareService;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import com.zhaoxinms.contract.tools.comparePRO.util.CompareTaskQueue;

/**
 * GPU OCR比对控制器 - 基于DotsOcrCompareDemoTest的完整比对功能
 */
@RestController
@RequestMapping("/api/compare-pro")
public class GPUCompareController {

    @Autowired
    private CompareService compareService;

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
            @RequestParam(value = "ignoreSeals", defaultValue = "true") boolean ignoreSeals,
            @RequestParam(value = "removeWatermark", defaultValue = "false") boolean removeWatermark,
            @RequestParam(value = "watermarkRemovalStrength", defaultValue = "smart") String watermarkRemovalStrength) {

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

            // 提交比对任务
            String taskId = compareService.submitCompareTask(oldFile, newFile, options);

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
    public ResponseEntity<Result<CompareTask>> getTaskStatus(@PathVariable String taskId) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);
            return ResponseEntity.ok(Result.success("获取任务状态成功", task));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("获取任务状态失败: " + e.getMessage()));
        }
    }


    /**
     * 获取所有比对任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<Result<List<CompareTask>>> getAllTasks() {
        try {
            List<CompareTask> tasks = compareService.getAllTasks();
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
            boolean deleted = compareService.deleteTask(taskId);
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

            CompareOptions options = new CompareOptions();
            if (optionsMap != null) {
                options.setIgnoreHeaderFooter(Boolean.TRUE.equals(optionsMap.get("ignoreHeaderFooter")));
                options.setHeaderHeightPercent(((Number) optionsMap.getOrDefault("headerHeightPercent", 12.0)).doubleValue());
                options.setFooterHeightPercent(((Number) optionsMap.getOrDefault("footerHeightPercent", 12.0)).doubleValue());
                options.setIgnoreCase(Boolean.TRUE.equals(optionsMap.get("ignoreCase")));
                options.setIgnoredSymbols((String) optionsMap.getOrDefault("ignoredSymbols", "_＿"));
                options.setIgnoreSpaces(Boolean.TRUE.equals(optionsMap.get("ignoreSpaces")));
                options.setIgnoreSeals(Boolean.TRUE.equals(optionsMap.get("ignoreSeals")));
                options.setRemoveWatermark(Boolean.TRUE.equals(optionsMap.get("removeWatermark")));
                options.setWatermarkRemovalStrength((String) optionsMap.getOrDefault("watermarkRemovalStrength", "smart"));
            }

            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error("taskId不能为空"));
            }

            String debugTaskId = compareService.debugCompareWithTaskId(taskId, options);

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
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
                responseData.put("status", task.getStatus().name());
                return ResponseEntity.ok(new Result<>(202, "任务尚未完成", responseData));
            }

            // 获取Canvas版本的比对结果
            Map<String, Object> canvasResult = compareService.getCanvasFrontendResult(taskId);
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
    public ResponseEntity<Result<CompareService.DocumentImageInfo>> getDocumentImages(
            @PathVariable String taskId, 
            @PathVariable String mode) {
        try {
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                return ResponseEntity.ok(Result.error(202, "任务尚未完成"));
            }

            // 验证mode参数
            if (!mode.equals("old") && !mode.equals("new")) {
                return ResponseEntity.badRequest().body(Result.error("mode参数必须是old或new"));
            }

            CompareService.DocumentImageInfo imageInfo = compareService.getDocumentImageInfo(taskId, mode);
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
            CompareTask task = compareService.getTaskStatus(taskId);

            if (task == null) {
                return ResponseEntity.ok(Result.error(404, "任务不存在"));
            }

            if (task.getStatus() != CompareTask.Status.COMPLETED) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "比对任务尚未完成，当前状态: " + task.getStatus().getDescription());
                responseData.put("status", task.getStatus().name());
                return ResponseEntity.ok(new Result<>(202, "任务尚未完成", responseData));
            }

            // 获取原始坐标数据（未经转换的）
            Map<String, Object> rawResult = compareService.getRawFrontendResult(taskId);
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
    public ResponseEntity<Result<CompareTaskQueue.TaskQueueStats>> getQueueStats() {
        try {
            CompareTaskQueue.TaskQueueStats stats = compareService.getQueueStats();
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
            boolean isBusy = compareService.isQueueBusy();
            CompareTaskQueue.TaskQueueStats stats = compareService.getQueueStats();
            
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
            
            compareService.adjustMaxConcurrency(maxThreads);
            return ResponseEntity.ok(Result.success("调整并发线程数成功", "最大线程数已设置为: " + maxThreads));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("调整并发线程数失败: " + e.getMessage()));
        }
    }
}
