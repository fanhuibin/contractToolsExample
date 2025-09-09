package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.time.LocalDateTime;

/**
 * GPU OCR比对任务
 */
public class GPUOCRCompareTask {

    public enum Status {
        PENDING("等待中"),
        OCR_PROCESSING("OCR处理中"),
        COMPARING("比对中"),
        ANNOTATING("标注中"),
        COMPLETED("完成"),
        FAILED("失败"),
        TIMEOUT("超时");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private String taskId;
    private Status status;
    private int progress;
    private int totalSteps = 13; // OCR + 比对 + 标注 + 保存结果等步骤
    private int currentStep = 0;
    private String currentStepDesc = "";
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String errorMessage;
    private String oldFileName;
    private String newFileName;
    private String oldPdfUrl;
    private String newPdfUrl;
    private String annotatedOldPdfUrl;
    private String annotatedNewPdfUrl;

    public GPUOCRCompareTask() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.status = Status.PENDING;
        this.progress = 0;
    }

    public GPUOCRCompareTask(String taskId) {
        this();
        this.taskId = taskId;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedTime = LocalDateTime.now();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public String getCurrentStepDesc() {
        return currentStepDesc;
    }

    public void setCurrentStepDesc(String currentStepDesc) {
        this.currentStepDesc = currentStepDesc;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedTime = LocalDateTime.now();
    }

    public String getOldFileName() {
        return oldFileName;
    }

    public void setOldFileName(String oldFileName) {
        this.oldFileName = oldFileName;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public String getOldPdfUrl() {
        return oldPdfUrl;
    }

    public void setOldPdfUrl(String oldPdfUrl) {
        this.oldPdfUrl = oldPdfUrl;
    }

    public String getNewPdfUrl() {
        return newPdfUrl;
    }

    public void setNewPdfUrl(String newPdfUrl) {
        this.newPdfUrl = newPdfUrl;
    }

    public String getAnnotatedOldPdfUrl() {
        return annotatedOldPdfUrl;
    }

    public void setAnnotatedOldPdfUrl(String annotatedOldPdfUrl) {
        this.annotatedOldPdfUrl = annotatedOldPdfUrl;
    }

    public String getAnnotatedNewPdfUrl() {
        return annotatedNewPdfUrl;
    }

    public void setAnnotatedNewPdfUrl(String annotatedNewPdfUrl) {
        this.annotatedNewPdfUrl = annotatedNewPdfUrl;
    }

    // 辅助方法
    public void updateProgress(int step, String stepDesc) {
        this.currentStep = step;
        this.currentStepDesc = stepDesc;
        this.progress = (int) ((double) step / totalSteps * 100);
        this.updatedTime = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isProcessing() {
        return status == Status.OCR_PROCESSING || status == Status.COMPARING || status == Status.ANNOTATING;
    }
}
