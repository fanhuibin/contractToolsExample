package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.zhaoxinms.contract.tools.ocr.model.DiffBlock;

/**
 * GPU OCR比对结果
 */
public class GPUOCRCompareResult {

    private String taskId;
    private String oldFileName;
    private String newFileName;
    private String oldPdfUrl;
    private String newPdfUrl;
    private String annotatedOldPdfUrl;
    private String annotatedNewPdfUrl;
    private List<DiffBlock> differences;
    private List<Map<String, Object>> formattedDifferences; // 原始图像坐标格式的差异数据
    // 不再需要PDF页面高度和缩放比例，画布使用图片实际像素尺寸
    private int deleteCount;
    private int insertCount;
    private int ignoreCount;
    private int totalDiffCount;
    private String summary;
    private long processingTimeMs;
    
    // 错误页面记录
    private List<String> failedPages; // 失败的页面信息，格式："文档A-第3页: 超时错误"

    public GPUOCRCompareResult() {
        this.failedPages = new ArrayList<>();
    }

    public GPUOCRCompareResult(String taskId) {
        this.taskId = taskId;
        this.failedPages = new ArrayList<>();
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public List<DiffBlock> getDifferences() {
        return differences;
    }

    public void setDifferences(List<DiffBlock> differences) {
        this.differences = differences;
        updateCounts();
    }

    public List<Map<String, Object>> getFormattedDifferences() {
        return formattedDifferences; 
    }
 
    public void setFormattedDifferences(List<Map<String, Object>> formattedDifferences) {
        this.formattedDifferences = formattedDifferences;
    }

    public int getDeleteCount() {
        return deleteCount;
    }

    public void setDeleteCount(int deleteCount) {
        this.deleteCount = deleteCount;
    }

    public int getInsertCount() {
        return insertCount;
    }

    public void setInsertCount(int insertCount) {
        this.insertCount = insertCount;
    }

    public int getIgnoreCount() {
        return ignoreCount;
    }

    public void setIgnoreCount(int ignoreCount) {
        this.ignoreCount = ignoreCount;
    }

    public int getTotalDiffCount() {
        return totalDiffCount;
    }

    public void setTotalDiffCount(int totalDiffCount) {
        this.totalDiffCount = totalDiffCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public List<String> getFailedPages() {
        return failedPages;
    }
    
    public void setFailedPages(List<String> failedPages) {
        this.failedPages = failedPages;
    }
    
    public void addFailedPage(String failedPageInfo) {
        if (this.failedPages == null) {
            this.failedPages = new ArrayList<>();
        }
        this.failedPages.add(failedPageInfo);
    }

    // 辅助方法
    private void updateCounts() {
        if (differences == null) {
            deleteCount = 0;
            insertCount = 0;
            ignoreCount = 0;
            totalDiffCount = 0;
            return;
        }

        deleteCount = 0;
        insertCount = 0;
        ignoreCount = 0;

        for (DiffBlock diff : differences) {
            switch (diff.type) {
                case DELETED:
                    deleteCount++;
                    break;
                case ADDED:
                    insertCount++;
                    break;
                case IGNORED:
                    ignoreCount++;
                    break;
            }
        }

        totalDiffCount = differences.size();
    }

    public void generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("比对完成，共发现 ").append(totalDiffCount).append(" 处差异：");
        sb.append("删除 ").append(deleteCount).append(" 处，");
        sb.append("新增 ").append(insertCount).append(" 处，");
        sb.append("忽略 ").append(ignoreCount).append(" 处。");
        
        // 添加错误页面信息
        if (failedPages != null && !failedPages.isEmpty()) {
            sb.append("识别失败 ").append(failedPages.size()).append(" 页。");
        }
        
        sb.append("处理耗时 ").append(processingTimeMs).append(" 毫秒。");

        this.summary = sb.toString();
    }
}
