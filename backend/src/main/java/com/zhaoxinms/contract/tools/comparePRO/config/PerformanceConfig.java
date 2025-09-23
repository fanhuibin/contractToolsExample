package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR性能参数配置
 * 
 * 用于配置各个处理步骤的时间参数，支持根据不同部署环境调整
 */
@Component
@ConfigurationProperties(prefix = "zxcm.compare.zxocr.performance")
public class PerformanceConfig {
    
    // 基础性能参数
    private long baseOverhead = 2000;              // 基础开销时间（初始化等）
    private long pageSetupTime = 100;              // 每页设置时间
    
    // OCR识别性能参数（每页时间）
    private long ocrFirstDocPerPage = 3000;        // OCR第一个文档每页时间（毫秒）
    private long ocrSecondDocPerPage = 2500;       // OCR第二个文档每页时间（毫秒）
    private long ocrFirstDocMinTime = 10000;       // OCR第一个文档最小时间
    private long ocrSecondDocMinTime = 8000;       // OCR第二个文档最小时间
    
    // 其他步骤性能参数
    private long initTime = 2000;                  // 初始化时间
    private long ocrCompleteTime = 1000;           // OCR完成时间
    private long textComparePerPage = 50;          // 文本比对每页时间
    private long textCompareMinTime = 2000;        // 文本比对最小时间
    private long diffAnalysisPerPage = 100;        // 差异分析每页时间
    private long diffAnalysisMinTime = 3000;       // 差异分析最小时间
    private long blockMergePerPage = 20;           // 块合并每页时间
    private long blockMergeMinTime = 1000;         // 块合并最小时间
    private long ocrValidationPerPage = 200;       // OCR验证每页时间
    private long ocrValidationMinTime = 2000;      // OCR验证最小时间
    private long resultGenerationPerPage = 30;     // 结果生成每页时间
    private long resultGenerationMinTime = 1500;   // 结果生成最小时间
    private long taskCompleteTime = 500;           // 任务完成时间
    
    // 性能调整系数
    private double ocrBufferFactor = 1.5;          // OCR步骤缓冲系数（避免预估过于乐观）
    private boolean regressionFallback = true;     // 是否启用线性回归作为备选
    
    // Getters and Setters
    
    public long getBaseOverhead() {
        return baseOverhead;
    }
    
    public void setBaseOverhead(long baseOverhead) {
        this.baseOverhead = baseOverhead;
    }
    
    public long getPageSetupTime() {
        return pageSetupTime;
    }
    
    public void setPageSetupTime(long pageSetupTime) {
        this.pageSetupTime = pageSetupTime;
    }
    
    public long getOcrFirstDocPerPage() {
        return ocrFirstDocPerPage;
    }
    
    public void setOcrFirstDocPerPage(long ocrFirstDocPerPage) {
        this.ocrFirstDocPerPage = ocrFirstDocPerPage;
    }
    
    public long getOcrSecondDocPerPage() {
        return ocrSecondDocPerPage;
    }
    
    public void setOcrSecondDocPerPage(long ocrSecondDocPerPage) {
        this.ocrSecondDocPerPage = ocrSecondDocPerPage;
    }
    
    public long getOcrFirstDocMinTime() {
        return ocrFirstDocMinTime;
    }
    
    public void setOcrFirstDocMinTime(long ocrFirstDocMinTime) {
        this.ocrFirstDocMinTime = ocrFirstDocMinTime;
    }
    
    public long getOcrSecondDocMinTime() {
        return ocrSecondDocMinTime;
    }
    
    public void setOcrSecondDocMinTime(long ocrSecondDocMinTime) {
        this.ocrSecondDocMinTime = ocrSecondDocMinTime;
    }
    
    public long getInitTime() {
        return initTime;
    }
    
    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }
    
    public long getOcrCompleteTime() {
        return ocrCompleteTime;
    }
    
    public void setOcrCompleteTime(long ocrCompleteTime) {
        this.ocrCompleteTime = ocrCompleteTime;
    }
    
    public long getTextComparePerPage() {
        return textComparePerPage;
    }
    
    public void setTextComparePerPage(long textComparePerPage) {
        this.textComparePerPage = textComparePerPage;
    }
    
    public long getTextCompareMinTime() {
        return textCompareMinTime;
    }
    
    public void setTextCompareMinTime(long textCompareMinTime) {
        this.textCompareMinTime = textCompareMinTime;
    }
    
    public long getDiffAnalysisPerPage() {
        return diffAnalysisPerPage;
    }
    
    public void setDiffAnalysisPerPage(long diffAnalysisPerPage) {
        this.diffAnalysisPerPage = diffAnalysisPerPage;
    }
    
    public long getDiffAnalysisMinTime() {
        return diffAnalysisMinTime;
    }
    
    public void setDiffAnalysisMinTime(long diffAnalysisMinTime) {
        this.diffAnalysisMinTime = diffAnalysisMinTime;
    }
    
    public long getBlockMergePerPage() {
        return blockMergePerPage;
    }
    
    public void setBlockMergePerPage(long blockMergePerPage) {
        this.blockMergePerPage = blockMergePerPage;
    }
    
    public long getBlockMergeMinTime() {
        return blockMergeMinTime;
    }
    
    public void setBlockMergeMinTime(long blockMergeMinTime) {
        this.blockMergeMinTime = blockMergeMinTime;
    }
    
    public long getOcrValidationPerPage() {
        return ocrValidationPerPage;
    }
    
    public void setOcrValidationPerPage(long ocrValidationPerPage) {
        this.ocrValidationPerPage = ocrValidationPerPage;
    }
    
    public long getOcrValidationMinTime() {
        return ocrValidationMinTime;
    }
    
    public void setOcrValidationMinTime(long ocrValidationMinTime) {
        this.ocrValidationMinTime = ocrValidationMinTime;
    }
    
    public long getResultGenerationPerPage() {
        return resultGenerationPerPage;
    }
    
    public void setResultGenerationPerPage(long resultGenerationPerPage) {
        this.resultGenerationPerPage = resultGenerationPerPage;
    }
    
    public long getResultGenerationMinTime() {
        return resultGenerationMinTime;
    }
    
    public void setResultGenerationMinTime(long resultGenerationMinTime) {
        this.resultGenerationMinTime = resultGenerationMinTime;
    }
    
    public long getTaskCompleteTime() {
        return taskCompleteTime;
    }
    
    public void setTaskCompleteTime(long taskCompleteTime) {
        this.taskCompleteTime = taskCompleteTime;
    }
    
    public double getOcrBufferFactor() {
        return ocrBufferFactor;
    }
    
    public void setOcrBufferFactor(double ocrBufferFactor) {
        this.ocrBufferFactor = ocrBufferFactor;
    }
    
    public boolean isRegressionFallback() {
        return regressionFallback;
    }
    
    public void setRegressionFallback(boolean regressionFallback) {
        this.regressionFallback = regressionFallback;
    }
    
    /**
     * 根据步骤名称和页数获取预估时间
     */
    public long getStepEstimatedTime(String stepName, int pages) {
        switch (stepName) {
            case "INIT": 
                return initTime;
            case "OCR_FIRST_DOC": 
                return Math.max(pages * ocrFirstDocPerPage, ocrFirstDocMinTime);
            case "OCR_SECOND_DOC": 
                return Math.max(pages * ocrSecondDocPerPage, ocrSecondDocMinTime);
            case "OCR_COMPLETE": 
                return ocrCompleteTime;
            case "TEXT_COMPARE": 
                return Math.max(pages * textComparePerPage, textCompareMinTime);
            case "DIFF_ANALYSIS": 
                return Math.max(pages * diffAnalysisPerPage, diffAnalysisMinTime);
            case "BLOCK_MERGE": 
                return Math.max(pages * blockMergePerPage, blockMergeMinTime);
            case "OCR_VALIDATION": 
                return Math.max(pages * ocrValidationPerPage, ocrValidationMinTime);
            case "RESULT_GENERATION": 
                return Math.max(pages * resultGenerationPerPage, resultGenerationMinTime);
            case "TASK_COMPLETE": 
                return taskCompleteTime;
            default: 
                return Math.max(pages * 1000L, 5000L);
        }
    }
}
