package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OCR比对任务实体类
 */
public class OCRCompareTask {
    
    public enum TaskStatus {
        PENDING("待处理"),
        OCR_PROCESSING("OCR识别中"),
        COMPARING("比对中"),
        COMPLETED("已完成"),
        FAILED("失败");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private String taskId;
    private String oldFileName;
    private String newFileName;
    private String oldFilePath;
    private String newFilePath;
    private OCRCompareOptions options;
    private TaskStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime startTime;
    private LocalDateTime completedTime;
    private double progress;
    private int currentStep;
    private String currentStepDescription;
    private String errorMessage;
    
    // OCR任务ID
    private String oldOcrTaskId;
    private String newOcrTaskId;
    
    // OCR进度
    private double oldOcrProgress;
    private double newOcrProgress;
    
    public OCRCompareTask() {
        this.status = TaskStatus.PENDING;
        this.createdTime = LocalDateTime.now();
        this.progress = 0.0;
        this.currentStep = 0;
    }
    
    public OCRCompareTask(String taskId, String oldFileName, String newFileName, 
                         String oldFilePath, String newFilePath, OCRCompareOptions options) {
        this();
        this.taskId = taskId;
        this.oldFileName = oldFileName;
        this.newFileName = newFileName;
        this.oldFilePath = oldFilePath;
        this.newFilePath = newFilePath;
        this.options = options;
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getOldFileName() { return oldFileName; }
    public void setOldFileName(String oldFileName) { this.oldFileName = oldFileName; }
    
    public String getNewFileName() { return newFileName; }
    public void setNewFileName(String newFileName) { this.newFileName = newFileName; }
    
    public String getOldFilePath() { return oldFilePath; }
    public void setOldFilePath(String oldFilePath) { this.oldFilePath = oldFilePath; }
    
    public String getNewFilePath() { return newFilePath; }
    public void setNewFilePath(String newFilePath) { this.newFilePath = newFilePath; }
    
    public OCRCompareOptions getOptions() { return options; }
    public void setOptions(OCRCompareOptions options) { this.options = options; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    
    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
    
    public String getCurrentStepDescription() { return currentStepDescription; }
    public void setCurrentStepDescription(String currentStepDescription) { this.currentStepDescription = currentStepDescription; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getOldOcrTaskId() { return oldOcrTaskId; }
    public void setOldOcrTaskId(String oldOcrTaskId) { this.oldOcrTaskId = oldOcrTaskId; }
    
    public String getNewOcrTaskId() { return newOcrTaskId; }
    public void setNewOcrTaskId(String newOcrTaskId) { this.newOcrTaskId = newOcrTaskId; }
    
    public double getOldOcrProgress() { return oldOcrProgress; }
    public void setOldOcrProgress(double oldOcrProgress) { this.oldOcrProgress = oldOcrProgress; }
    
    public double getNewOcrProgress() { return newOcrProgress; }
    public void setNewOcrProgress(double newOcrProgress) { this.newOcrProgress = newOcrProgress; }
    
    // 业务方法
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED;
    }
    
    public boolean isProcessing() {
        return status == TaskStatus.OCR_PROCESSING || status == TaskStatus.COMPARING;
    }
    
    public void updateProgress(double progress, String stepDescription) {
        this.progress = progress;
        this.currentStepDescription = stepDescription;
    }
    
    public void setCurrentStep(int step, String description) {
        this.currentStep = step;
        this.currentStepDescription = description;
    }
    
    public void updateOCRProgress(String type, double progress) {
        if ("old".equals(type)) {
            this.oldOcrProgress = progress;
        } else if ("new".equals(type)) {
            this.newOcrProgress = progress;
        }
        
        // 计算总体OCR进度
        double totalOcrProgress = (oldOcrProgress + newOcrProgress) / 2.0;
        this.progress = totalOcrProgress * 0.6; // OCR占总进度的60%
    }
    
    @Override
    public String toString() {
        return "OCRCompareTask{" +
                "taskId='" + taskId + '\'' +
                ", oldFileName='" + oldFileName + '\'' +
                ", newFileName='" + newFileName + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                ", currentStep=" + currentStep +
                ", currentStepDescription='" + currentStepDescription + '\'' +
                '}';
    }
}
