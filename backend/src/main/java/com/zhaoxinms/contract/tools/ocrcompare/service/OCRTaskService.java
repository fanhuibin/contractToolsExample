package com.zhaoxinms.contract.tools.ocrcompare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.ocrcompare.client.OCRHttpClient;
import com.zhaoxinms.contract.tools.ocrcompare.config.OCRProperties;
import com.zhaoxinms.contract.tools.ocrcompare.model.OCRTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class OCRTaskService {

    @Autowired
    private OCRProperties ocrProperties;

    @Autowired
    private OCRHttpClient ocrHttpClient;

    private final ConcurrentHashMap<String, OCRTask> tasks = new ConcurrentHashMap<String, OCRTask>();

    public String submitOCRTask(String pdfPath) {
        if (!ocrHttpClient.healthCheck()) {
            throw new IllegalStateException("OCR服务不可用，请确保Python OCR服务已启动");
        }
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            throw new IllegalArgumentException("PDF文件不存在: " + pdfPath);
        }
        if (pdfFile.length() == 0) { throw new IllegalArgumentException("PDF文件为空: " + pdfPath); }

        String taskId = generateTaskId();
        OCRTask task = new OCRTask(taskId, pdfPath);
        tasks.put(taskId, task);
        CompletableFuture.runAsync(() -> executeOCRTask(task));
        return taskId;
    }

    public OCRTask getTaskStatus(String taskId) { return tasks.get(taskId); }
    public List<OCRTask> getAllTasks() { return new ArrayList<>(tasks.values()); }

    public void cleanupExpiredTasks() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(ocrProperties.getTask().getResultRetentionDays());
        tasks.entrySet().removeIf(e -> e.getValue().getCreatedTime().isBefore(expireTime));
    }

    private void executeOCRTask(OCRTask task) {
        try {
            task.setStatus(OCRTask.TaskStatus.PROCESSING);
            task.setStartTime(LocalDateTime.now());

            Map<String, Object> options = new HashMap<>();
            options.put("dpi", ocrProperties.getSettings().getDpi());
            options.put("min_score", ocrProperties.getSettings().getMinScore());

            String remoteTaskId = ocrHttpClient.submitOCRTask(task.getPdfPath(), "pdf", options);
            task.setRemoteTaskId(remoteTaskId);
            JsonNode result = ocrHttpClient.waitForTaskCompletion(remoteTaskId, ocrProperties.getTask().getTimeout());
            processOCRResult(task, result);
        } catch (Exception e) {
            task.setStatus(OCRTask.TaskStatus.FAILED);
            task.setErrorMessage("OCR执行异常: " + e.getMessage());
            task.setCompletedTime(LocalDateTime.now());
        }
    }

    private void processOCRResult(OCRTask task, JsonNode result) {
        task.setStatus(OCRTask.TaskStatus.COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        
        // 获取结果路径
        String resultPath = result.path("result_path").asText();
        if (resultPath != null && !resultPath.trim().isEmpty()) {
            // 保存结果路径，用于后续获取详细结果
            task.setResultPath(resultPath);
        }
        
        // 获取文本内容并保存到任务中
        String textContent = result.path("text_content").asText("");
        task.setMessage("OCR识别完成，文本长度: " + textContent.length() + " 字符");
        
        // 保存文本内容到任务中，避免重复调用HTTP接口
        if (textContent != null && !textContent.trim().isEmpty()) {
            task.setTextContent(textContent);
        }
        
        // 设置进度为100%
        task.setProgress(100.0);
        task.setCurrentPage(1);
        task.setTotalPages(1);
    }

    private String generateTaskId() { return "OCR_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8); }

    public String queryTaskProgress(String taskId) {
        OCRTask task = tasks.get(taskId);
        if (task == null) { return "TASK_NOT_FOUND"; }
        return String.format("%s|%.2f|%d|%d|%s", task.getStatus().name(), task.getProgress(), task.getCurrentPage(), task.getTotalPages(), task.getMessage() != null ? task.getMessage() : "");
    }
    
    /**
     * 通过HTTP接口获取OCR结果
     */
    public com.fasterxml.jackson.databind.JsonNode getOCRResultViaHttp(String taskId) {
        try {
            // 使用远端Python任务ID
            OCRTask t = tasks.get(taskId);
            String remote = t != null && t.getRemoteTaskId() != null ? t.getRemoteTaskId() : taskId;
            return ocrHttpClient.getOCRResult(remote);
        } catch (Exception e) {
            // 记录错误但不抛出异常，让调用方处理
            return null;
        }
    }

    /**
     * 优先从本地 result_path 读取 OCR 结果（JSON），若不存在再尝试通过 HTTP 获取。
     */
    public com.fasterxml.jackson.databind.JsonNode getOCRResultJson(String taskId) {
        try {
            OCRTask task = tasks.get(taskId);
            if (task != null && task.getResultPath() != null && !task.getResultPath().trim().isEmpty()) {
                java.io.File f = new java.io.File(task.getResultPath());
                if (f.exists() && f.isFile()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    return mapper.readTree(f);
                }
            }
        } catch (Exception ignore) {}
        // 回退：尝试 HTTP（注意：若传入的是本地taskId可能无效）
        try {
            return ocrHttpClient.getOCRResult(taskId);
        } catch (Exception e) {
            return null;
        }
    }
}


