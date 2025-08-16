package com.zhaoxinms.contract.tools.ocrcompare.model;

import java.time.LocalDateTime;

public class OCRTask {

    public enum TaskStatus { PENDING("待处理"), PROCESSING("处理中"), COMPLETED("已完成"), FAILED("失败"), TIMEOUT("超时");
        private final String description; TaskStatus(String d){this.description=d;} public String getDescription(){return description;} }

    private String taskId;
    private String pdfPath;
    private TaskStatus status;
    private String message;
    private LocalDateTime createdTime;
    private LocalDateTime startTime;
    private LocalDateTime completedTime;
    private int currentPage;
    private int totalPages;
    private double progress;
    private String resultPath;
    // 远端Python OCR服务的任务ID（用于HTTP查询json_data）
    private String remoteTaskId;
    private String textContent;  // 保存OCR识别的文本内容
    private String errorMessage;

    public OCRTask() {}
    public OCRTask(String taskId, String pdfPath) { this.taskId = taskId; this.pdfPath = pdfPath; this.status = TaskStatus.PENDING; this.createdTime = LocalDateTime.now(); this.progress = 0.0; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public String getResultPath() { return resultPath; }
    public void setResultPath(String resultPath) { this.resultPath = resultPath; }
    public String getRemoteTaskId() { return remoteTaskId; }
    public void setRemoteTaskId(String remoteTaskId) { this.remoteTaskId = remoteTaskId; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isCompleted() { return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.TIMEOUT; }
    public boolean isProcessing() { return status == TaskStatus.PROCESSING; }
    public void updateProgress(int currentPage, int totalPages) { this.currentPage = currentPage; this.totalPages = totalPages; if (totalPages > 0) { this.progress = (double) currentPage / totalPages * 100; } }
    public String toString() { return "OCRTask{" + "taskId='" + taskId + '\'' + ", pdfPath='" + pdfPath + '\'' + ", status=" + status + ", progress=" + progress + ", currentPage=" + currentPage + ", totalPages=" + totalPages + '}'; }
}


