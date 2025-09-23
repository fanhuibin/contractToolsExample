package com.zhaoxinms.contract.tools.comparePRO.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高级合同比对任务
 */
public class CompareTask {

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
    
    // 时间统计信息
    private Map<String, Long> stepDurations = new HashMap<>(); // 各步骤耗时（毫秒）
    private Long totalDuration; // 总耗时（毫秒）
    private LocalDateTime startTime; // 任务开始时间
    private LocalDateTime endTime; // 任务结束时间
    
    // 失败页面信息
    private List<String> failedPages = new ArrayList<>(); // 识别失败的页面列表
    private Map<String, Object> statistics = new HashMap<>(); // 统计信息
    
    // 页面级别进度信息
    private int totalPages; // 总页数（取两个文档的最大值，用于整体进度计算）
    private int oldDocPages; // 旧文档页数
    private int newDocPages; // 新文档页数
    private int currentPageOld; // 当前处理的旧文档页面
    private int currentPageNew; // 当前处理的新文档页面
    private int completedPagesOld; // 已完成的旧文档页面数
    private int completedPagesNew; // 已完成的新文档页面数

    public CompareTask() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.status = Status.PENDING;
        this.progress = 0;
    }

    public CompareTask(String taskId) {
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

    // 新增字段的getter和setter方法
    public Map<String, Long> getStepDurations() {
        return stepDurations;
    }

    public void setStepDurations(Map<String, Long> stepDurations) {
        this.stepDurations = stepDurations;
    }
    
    public void addStepDuration(String stepName, long duration) {
        this.stepDurations.put(stepName, duration);
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        // 自动计算总耗时
        if (this.startTime != null && endTime != null) {
            this.totalDuration = java.time.Duration.between(this.startTime, endTime).toMillis();
        }
    }

    public List<String> getFailedPages() {
        return failedPages;
    }

    public void setFailedPages(List<String> failedPages) {
        this.failedPages = failedPages;
    }
    
    public void addFailedPage(String failedPage) {
        this.failedPages.add(failedPage);
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    public void addStatistic(String key, Object value) { 
        this.statistics.put(key, value); 
    }
    
    // 页面级别进度的getter和setter
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public int getOldDocPages() { return oldDocPages; }
    public void setOldDocPages(int oldDocPages) { this.oldDocPages = oldDocPages; }
    
    public int getNewDocPages() { return newDocPages; }
    public void setNewDocPages(int newDocPages) { this.newDocPages = newDocPages; }
    
    public int getCurrentPageOld() { return currentPageOld; }
    public void setCurrentPageOld(int currentPageOld) { this.currentPageOld = currentPageOld; }
    
    public int getCurrentPageNew() { return currentPageNew; }
    public void setCurrentPageNew(int currentPageNew) { this.currentPageNew = currentPageNew; }
    
    public int getCompletedPagesOld() { return completedPagesOld; }
    public void setCompletedPagesOld(int completedPagesOld) { this.completedPagesOld = completedPagesOld; }
    
    public int getCompletedPagesNew() { return completedPagesNew; }
    public void setCompletedPagesNew(int completedPagesNew) { this.completedPagesNew = completedPagesNew; }
}
