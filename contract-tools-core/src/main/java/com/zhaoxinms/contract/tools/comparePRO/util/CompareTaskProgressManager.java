package com.zhaoxinms.contract.tools.comparePRO.util;

import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 比对任务进度管理器
 * 负责统一管理任务进度、日志输出和性能统计
 */
public class CompareTaskProgressManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CompareTaskProgressManager.class);
    
    private final CompareTask task;
    private final boolean debugMode;
    private final Map<String, Long> stepStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> stepDurations = new ConcurrentHashMap<>();
    private long taskStartTime;
    
    public CompareTaskProgressManager(CompareTask task, boolean debugMode) {
        this.task = task;
        this.debugMode = debugMode;
        this.taskStartTime = System.currentTimeMillis();
    }
    
    /**
     * 任务步骤枚举
     */
    public enum TaskStep {
        INIT("初始化", 1),
        OCR_FIRST_DOC("OCR识别原文档", 2), 
        OCR_SECOND_DOC("OCR识别新文档", 3),
        OCR_COMPLETE("OCR识别完成", 4),
        TEXT_COMPARE("文本比对", 5),
        DIFF_ANALYSIS("差异分析", 6),
        BLOCK_MERGE("差异块合并", 7),
        RESULT_GENERATION("结果生成", 8),
        TASK_COMPLETE("任务完成", 9);
        
        private final String description;
        private final int stepNumber;
        
        TaskStep(String description, int stepNumber) {
            this.description = description;
            this.stepNumber = stepNumber;
        }
        
        public String getDescription() { return description; }
        public int getStepNumber() { return stepNumber; }
    }
    
    /**
     * 开始一个步骤
     */
    public void startStep(TaskStep step) {
        long currentTime = System.currentTimeMillis();
        stepStartTimes.put(step.name(), currentTime);
        
        // 更新任务进度
        task.updateProgress(step.getStepNumber(), step.getDescription());
        
        // 日志输出
        if (debugMode) {
            logger.info("[DEBUG] 🚀 开始步骤 {}: {}", step.getStepNumber(), step.getDescription());
        } else {
            logger.info("🚀 步骤 {}: {}", step.getStepNumber(), step.getDescription());
        }
    }
    
    /**
     * 完成一个步骤
     */
    public void completeStep(TaskStep step) {
        Long startTime = stepStartTimes.get(step.name());
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            stepDurations.put(step.name(), duration);
            
            if (debugMode) {
                logger.info("[DEBUG] ✅ 完成步骤 {}: {} (耗时: {}ms)", 
                    step.getStepNumber(), step.getDescription(), duration);
            } else {
                logger.info("✅ 完成步骤 {}: {} ({}ms)", 
                    step.getStepNumber(), step.getDescription(), duration);
            }
        }
    }
    
    /**
     * 记录步骤详细信息（仅调试模式输出）
     */
    public void logStepDetail(String message) {
        if (debugMode) {
            logger.info("[DEBUG] {}", message);
        }
    }
    
    /**
     * 记录步骤详细信息（仅调试模式输出）- 带参数
     */
    public void logStepDetail(String format, Object... args) {
        if (debugMode) {
            logger.info("[DEBUG] " + format, args);
        }
    }
    
    /**
     * 记录基本统计信息（正常和调试模式都输出）
     */
    public void logBasicStats(String message) {
        logger.info(message);
    }
    
    /**
     * 记录基本统计信息（正常和调试模式都输出）- 带参数
     */
    public void logBasicStats(String format, Object... args) {
        logger.info(format, args);
    }
    
    /**
     * 记录文档基本信息
     */
    public void logDocumentInfo(String oldFileName, String newFileName, int oldPages, int newPages) {
        logBasicStats("文档信息: {} ({} 页) vs {} ({} 页)", 
            oldFileName, oldPages, newFileName, newPages);
    }
    
    /**
     * 记录OCR统计信息
     */
    public void logOCRStats(int oldCharCount, int newCharCount, long ocrDuration) {
        logBasicStats("OCR完成: {} 字符 vs {} 字符 (耗时: {}ms)", 
            oldCharCount, newCharCount, ocrDuration);
    }
    
    /**
     * 记录差异分析统计信息
     */
    public void logDiffStats(int rawBlocks, int filteredBlocks, int mergedBlocks) {
        logBasicStats("差异分析: 原始差异块={}, 过滤后={}, 合并后={}", 
            rawBlocks, filteredBlocks, mergedBlocks);
    }
    
    /**
     * 记录验证统计信息
     */
    public void logValidationStats(int totalMerged, int eligible, int totalPages, 
                                   int pageThreshold, boolean triggered, int removed) {
        if (triggered) {
            logger.info("🔍 验证统计: 总块数={}, 符合条件={}, 总页数={}, 页数阈值={}, 移除幻觉块={}", 
                totalMerged, eligible, totalPages, pageThreshold, removed);
        } else {
            logger.info("⚠️ 验证统计: 总块数={}, 符合条件={}, 总页数={}, 页数阈值={} (未触发验证)", 
                totalMerged, eligible, totalPages, pageThreshold);
        }
    }
    
    /**
     * 获取总耗时
     */
    public long getTotalDuration() {
        return System.currentTimeMillis() - taskStartTime;
    }
    
    /**
     * 获取步骤耗时
     */
    public Long getStepDuration(TaskStep step) {
        return stepDurations.get(step.name());
    }
    
    /**
     * 输出任务完成总结
     */
    public void logTaskSummary() {
        long totalTime = getTotalDuration();
        
        if (debugMode) {
            logger.info("[DEBUG] ========== 任务完成总结 ==========");
            logger.info("[DEBUG] 任务ID: {}", task.getTaskId());
            logger.info("[DEBUG] 总耗时: {}ms", totalTime);
            logger.info("[DEBUG] 各步骤耗时:");
            for (TaskStep step : TaskStep.values()) {
                Long duration = stepDurations.get(step.name());
                if (duration != null) {
                    logger.info("[DEBUG]   {}: {}ms", step.getDescription(), duration);
                }
            }
            logger.info("[DEBUG] =====================================");
        } else {
            logger.info("任务 {} 完成，总耗时: {}ms", task.getTaskId(), totalTime);
        }
    }
    
    /**
     * 记录错误信息
     */
    public void logError(String message, Throwable e) {
        logger.error(message, e);
        if (debugMode) {
            logger.error("[DEBUG] 任务失败详情:", e);
        }
    }
    
    /**
     * 是否为调试模式
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * 完成任务并同步时间统计信息到CompareTask
     */
    public void completeTask() {
        long totalDuration = System.currentTimeMillis() - taskStartTime;
        
        // 同步时间统计信息到CompareTask
        task.setStepDurations(new java.util.HashMap<>(stepDurations));
        task.setTotalDuration(totalDuration);
        task.setEndTime(java.time.LocalDateTime.now());
        
        // 添加总体统计信息
        task.addStatistic("totalSteps", stepDurations.size());
        task.addStatistic("taskStartTime", taskStartTime);
        task.addStatistic("totalDurationMs", totalDuration);
        task.addStatistic("totalDurationHuman", formatDuration(totalDuration));
        
        logger.info("任务 {} 完成，总耗时: {}ms", task.getTaskId(), totalDuration);
    }
    
    /**
     * 获取步骤耗时统计Map
     */
    public Map<String, Long> getStepDurationsMap() {
        return new java.util.HashMap<>(stepDurations);
    }
    
    /**
     * 格式化时长为人类可读格式
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    /**
     * 添加失败页面信息到任务
     */
    public void addFailedPages(java.util.List<String> failedPages) {
        if (failedPages != null && !failedPages.isEmpty()) {
            for (String failedPage : failedPages) {
                task.addFailedPage(failedPage);
            }
            
            // 添加统计信息
            task.addStatistic("totalFailedPages", failedPages.size());
            task.addStatistic("failedPagesList", new java.util.ArrayList<>(failedPages));
            
            logger.warn("任务 {} 有 {} 个页面识别失败: {}", task.getTaskId(), failedPages.size(), failedPages);
        }
    }
}
