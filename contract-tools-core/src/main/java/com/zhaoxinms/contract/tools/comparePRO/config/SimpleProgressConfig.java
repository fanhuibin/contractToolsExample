package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简化的进度配置 - 只关注OCR时间预估
 * 
 * @author zhaoxin
 * @version 2.0
 * @since 2025-10-13
 */
@Component
@ConfigurationProperties(prefix = "zxcm.compare.progress")
public class SimpleProgressConfig {
    
    // ==================== OCR性能参数 ====================
    
    /**
     * 第一个文档OCR每页耗时（毫秒）
     * 默认: 3000ms (3秒/页)
     */
    private long ocrFirstDocPerPage = 3000;
    
    /**
     * 第二个文档OCR每页耗时（毫秒）
     * 默认: 2500ms (2.5秒/页)
     * 注：第二个文档通常稍快，因为系统已经预热
     */
    private long ocrSecondDocPerPage = 2500;
    
    /**
     * OCR最小时间（毫秒）
     * 即使只有1页，也至少需要这么长时间
     * 默认: 5000ms (5秒)
     */
    private long ocrMinTime = 5000;
    
    /**
     * OCR缓冲系数
     * 用于增加一定的时间缓冲，避免预估过于乐观
     * 默认: 1.2 (增加20%缓冲)
     */
    private double ocrBufferFactor = 1.2;
    
    // ==================== 进度里程碑 ====================
    
    /**
     * 第一个文档完成时的进度百分比
     * 默认: 46.0
     */
    private double firstDocCompleteProgress = 46.0;
    
    /**
     * 第一个文档超时等待的最大进度百分比
     * 当第一个文档预计完成时间到达但后台还未完成时，最多等待到这个百分比
     * 默认: 60.0
     */
    private double firstDocMaxWaitProgress = 60.0;
    
    /**
     * 第二个文档完成时的进度百分比
     * 默认: 96.0
     */
    private double secondDocCompleteProgress = 96.0;
    
    /**
     * 缓慢增长速度系数
     * 当到达预期进度但后台还未完成时，使用原速度的这个倍数缓慢增长
     * 默认: 0.05 (1/20)
     */
    private double slowGrowthFactor = 0.05;
    
    /**
     * 最终冲刺时间（毫秒）
     * 从96%到100%的快速增长时间
     * 默认: 100ms (0.1秒)
     */
    private long finalSprintTime = 100;
    
    // ==================== Getters and Setters ====================
    
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
    
    public long getOcrMinTime() {
        return ocrMinTime;
    }
    
    public void setOcrMinTime(long ocrMinTime) {
        this.ocrMinTime = ocrMinTime;
    }
    
    public double getOcrBufferFactor() {
        return ocrBufferFactor;
    }
    
    public void setOcrBufferFactor(double ocrBufferFactor) {
        this.ocrBufferFactor = ocrBufferFactor;
    }
    
    public double getFirstDocCompleteProgress() {
        return firstDocCompleteProgress;
    }
    
    public void setFirstDocCompleteProgress(double firstDocCompleteProgress) {
        this.firstDocCompleteProgress = firstDocCompleteProgress;
    }
    
    public double getFirstDocMaxWaitProgress() {
        return firstDocMaxWaitProgress;
    }
    
    public void setFirstDocMaxWaitProgress(double firstDocMaxWaitProgress) {
        this.firstDocMaxWaitProgress = firstDocMaxWaitProgress;
    }
    
    public double getSecondDocCompleteProgress() {
        return secondDocCompleteProgress;
    }
    
    public void setSecondDocCompleteProgress(double secondDocCompleteProgress) {
        this.secondDocCompleteProgress = secondDocCompleteProgress;
    }
    
    public double getSlowGrowthFactor() {
        return slowGrowthFactor;
    }
    
    public void setSlowGrowthFactor(double slowGrowthFactor) {
        this.slowGrowthFactor = slowGrowthFactor;
    }
    
    public long getFinalSprintTime() {
        return finalSprintTime;
    }
    
    public void setFinalSprintTime(long finalSprintTime) {
        this.finalSprintTime = finalSprintTime;
    }
    
    /**
     * 计算第一个文档的预估OCR时间
     * 
     * @param pages 页数
     * @return 预估时间（毫秒）
     */
    public long calculateFirstDocOcrTime(int pages) {
        long baseTime = pages * ocrFirstDocPerPage;
        long estimatedTime = (long) (Math.max(baseTime, ocrMinTime) * ocrBufferFactor);
        
        return estimatedTime;
    }
    
    /**
     * 计算第二个文档的预估OCR时间
     * 
     * @param pages 页数
     * @return 预估时间（毫秒）
     */
    public long calculateSecondDocOcrTime(int pages) {
        long baseTime = pages * ocrSecondDocPerPage;
        long estimatedTime = (long) (Math.max(baseTime, ocrMinTime) * ocrBufferFactor);
        
        return estimatedTime;
    }
}

