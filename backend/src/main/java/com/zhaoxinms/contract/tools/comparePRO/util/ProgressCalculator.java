package com.zhaoxinms.contract.tools.comparePRO.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import com.zhaoxinms.contract.tools.comparePRO.config.PerformanceConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 进度计算器 - 基于配置文件的性能参数提供精确的进度估算
 * 
 * @version 2.0
 */
@Component
public class ProgressCalculator {
    
    @Autowired
    private PerformanceConfig performanceConfig;

    /**
     * 计算当前任务的线性进度信息  
     */
    public ProgressInfo calculateProgress(CompareTask task) {
        if (task == null) {
            return new ProgressInfo(0.0, 0L, "任务不存在", "未知");
        }

        CompareTask.Status status = task.getStatus();
        LocalDateTime startTime = task.getStartTime();

        // 任务未开始
        if (startTime == null || status == CompareTask.Status.PENDING) {
            return new ProgressInfo(0.0, 0L, "等待处理", "准备中");
        }

        // 任务已完成
        if (status == CompareTask.Status.COMPLETED) {
            long actualDuration = task.getTotalDuration() != null ? task.getTotalDuration() : 0L;
            return new ProgressInfo(100.0, actualDuration, "已完成", "任务完成");
        }

        // 任务失败
        if (status == CompareTask.Status.FAILED) {
            return new ProgressInfo(0.0, 0L, "处理失败", "任务失败");
        }

        // 任务进行中
        long totalElapsedTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
        int estimatedPages = estimatePageCount(task);
        
        // 计算阶段信息（使用任务特定的页数）
        StageInfo stageInfo = calculateStageInfo(task, estimatedPages, totalElapsedTime);
        
        // 基于阶段信息计算进度
        double currentProgress = calculateStageBasedProgress(task, totalElapsedTime, stageInfo);
        
        // 计算预估总时间（使用任务特定的页数）
        long estimatedTotalTime = estimateTotalTimeForTask(task);
        
        // 创建包含阶段信息的ProgressInfo
        return new ProgressInfo(
            currentProgress,
            Math.max(0, estimatedTotalTime - totalElapsedTime),
            formatProgress(currentProgress),
            getCurrentStepDescription(task),
            formatDuration(estimatedTotalTime),
            stageInfo.minProgress,
            stageInfo.maxProgress,
            stageInfo.estimatedTime,
            stageInfo.elapsedTime
        );
    }

    /**
     * 阶段信息
     */
    private static class StageInfo {
        final double minProgress;
        final double maxProgress;
        final long estimatedTime;
        final long elapsedTime;
        
        StageInfo(double minProgress, double maxProgress, long estimatedTime, long elapsedTime) {
            this.minProgress = minProgress;
            this.maxProgress = maxProgress;
            this.estimatedTime = estimatedTime;
            this.elapsedTime = elapsedTime;
        }
    }

    /**
     * 计算阶段信息
     */
    private StageInfo calculateStageInfo(CompareTask task, int estimatedPages, long totalElapsedTime) {
        String currentStepName = getCurrentStepName(task);
        
        // 定义阶段范围
        Map<String, double[]> stageRanges = new LinkedHashMap<>();
        stageRanges.put("INIT", new double[]{0.0, 1.0});
        stageRanges.put("OCR_FIRST_DOC", new double[]{1.0, 50.0});
        stageRanges.put("OCR_SECOND_DOC", new double[]{50.0, 85.0});
        stageRanges.put("OCR_COMPLETE", new double[]{85.0, 86.0});
        stageRanges.put("TEXT_COMPARE", new double[]{86.0, 87.0});
        stageRanges.put("DIFF_ANALYSIS", new double[]{87.0, 90.0});
        stageRanges.put("BLOCK_MERGE", new double[]{90.0, 91.0});
        stageRanges.put("OCR_VALIDATION", new double[]{91.0, 96.0});
        stageRanges.put("RESULT_GENERATION", new double[]{96.0, 98.0});
        stageRanges.put("TASK_COMPLETE", new double[]{98.0, 100.0});
        
        double[] range = stageRanges.getOrDefault(currentStepName, new double[]{0.0, 100.0});
        double minProgress = range[0];
        double maxProgress = range[1];
        
        // 计算当前阶段的预估时间（使用对应文档的页数）
        long stageEstimatedTime = getStepEstimatedTimeForTask(task, currentStepName);
        
        // 计算当前阶段已用时间（基于前面所有步骤的预估时间）
        long stageElapsedTime = calculateStageElapsedTimeForTask(task, currentStepName, totalElapsedTime);
        
        return new StageInfo(minProgress, maxProgress, stageEstimatedTime, stageElapsedTime);
    }


    /**
     * 基于阶段信息计算当前进度（支持页面级别的精确进度）
     */
    private double calculateStageBasedProgress(CompareTask task, long totalElapsedTime, StageInfo stageInfo) {
        String currentStepName = getCurrentStepName(task);
        
        // 对OCR步骤使用页面级别的精确进度
        if ("OCR_FIRST_DOC".equals(currentStepName) || "OCR_SECOND_DOC".equals(currentStepName)) {
            return calculateOCRPageProgress(task, stageInfo, currentStepName);
        }
        
        // 对其他步骤使用时间基础的进度
        return calculateTimeBasedProgress(stageInfo);
    }

    /**
     * 基于页面进度计算OCR步骤的精确进度
     */
    private double calculateOCRPageProgress(CompareTask task, StageInfo stageInfo, String stepName) {
        int currentDocPages = 0;
        int completedPages = 0;
        int currentPage = 0;
        
        if ("OCR_FIRST_DOC".equals(stepName)) {
            currentDocPages = task.getOldDocPages(); // 使用当前文档的实际页数
            completedPages = task.getCompletedPagesOld();
            currentPage = task.getCurrentPageOld();
        } else if ("OCR_SECOND_DOC".equals(stepName)) {
            currentDocPages = task.getNewDocPages(); // 使用当前文档的实际页数
            completedPages = task.getCompletedPagesNew();
            currentPage = task.getCurrentPageNew();
        }
        
        if (currentDocPages <= 0) {
            // 如果没有页面信息，回退到时间基础计算
            return calculateTimeBasedProgress(stageInfo);
        }
        
        // 计算页面进度比例（基于当前文档的实际页数）
        double pageProgressRatio = (double) completedPages / currentDocPages;
        
        // 如果有当前页面信息，可以添加部分进度
        if (currentPage > completedPages && currentPage <= currentDocPages) {
            // 假设当前页面完成了50%
            pageProgressRatio += 0.5 / currentDocPages;
        }
        
        // 限制在0-1范围内
        pageProgressRatio = Math.min(1.0, Math.max(0.0, pageProgressRatio));
        
        // 在阶段范围内插值
        double stageProgress = stageInfo.minProgress + 
            (stageInfo.maxProgress - stageInfo.minProgress) * pageProgressRatio;
        
        return Math.min(stageProgress, stageInfo.maxProgress);
    }

    /**
     * 基于时间计算进度
     */
    private double calculateTimeBasedProgress(StageInfo stageInfo) {
        // 如果阶段还没开始，返回最小进度
        if (stageInfo.elapsedTime <= 0) {
            return stageInfo.minProgress;
        }
        
        // 计算阶段内的进度比例
        double stageProgressRatio = 0.0;
        if (stageInfo.estimatedTime > 0) {
            stageProgressRatio = Math.min(1.0, (double) stageInfo.elapsedTime / stageInfo.estimatedTime);
        }
        
        // 在阶段范围内插值
        double stageProgress = stageInfo.minProgress + 
            (stageInfo.maxProgress - stageInfo.minProgress) * stageProgressRatio;
        
        return Math.min(stageProgress, stageInfo.maxProgress);
    }

    /**
     * 获取单个步骤的预估时间（基于配置文件）
     */
    private long getStepEstimatedTime(String stepName, int pages) {
        return performanceConfig.getStepEstimatedTime(stepName, pages);
    }
    
    /**
     * 为特定任务获取步骤的预估时间（考虑不同文档的页数）
     */
    private long getStepEstimatedTimeForTask(CompareTask task, String stepName) {
        int pages = 0;
        
        switch (stepName) {
            case "OCR_FIRST_DOC":
                pages = task.getOldDocPages();
                break;
            case "OCR_SECOND_DOC":
                pages = task.getNewDocPages();
                break;
            default:
                pages = task.getTotalPages(); // 其他步骤使用总页数
                break;
        }
        
        return getStepEstimatedTime(stepName, pages);
    }
    
    /**
     * 为特定任务计算当前阶段的已用时间
     */
    private long calculateStageElapsedTimeForTask(CompareTask task, String currentStepName, long totalElapsedTime) {
        // 步骤顺序
        String[] stepOrder = {"INIT", "OCR_FIRST_DOC", "OCR_SECOND_DOC", "OCR_COMPLETE", 
                             "TEXT_COMPARE", "DIFF_ANALYSIS", "BLOCK_MERGE", "OCR_VALIDATION", 
                             "RESULT_GENERATION", "TASK_COMPLETE"};
        
        // 计算前面所有步骤的预估时间
        long previousStepsTime = 0;
        for (String step : stepOrder) {
            if (step.equals(currentStepName)) {
                break;
            }
            previousStepsTime += getStepEstimatedTimeForTask(task, step);
        }
        
        // 当前阶段已用时间 = 总已用时间 - 前面所有步骤的时间
        long stageElapsedTime = totalElapsedTime - previousStepsTime;
        
        // 确保不为负数，并且不超过当前阶段的预估时间
        long stageEstimatedTime = getStepEstimatedTimeForTask(task, currentStepName);
        stageElapsedTime = Math.max(0, Math.min(stageElapsedTime, stageEstimatedTime));
        
        return stageElapsedTime;
    }

    /**
     * 智能估算页数
     */
    private int estimatePageCount(CompareTask task) {
        // 1. 如果已有总页数信息，直接使用
        if (task.getTotalPages() > 0) {
            return task.getTotalPages();
        }

        // 2. 从文件名中提取页数信息
        String oldFileName = task.getOldFileName();
        String newFileName = task.getNewFileName();
        
        int pagesFromOldFile = extractPageCountFromFileName(oldFileName);
        int pagesFromNewFile = extractPageCountFromFileName(newFileName);
        
        if (pagesFromOldFile > 0 || pagesFromNewFile > 0) {
            return Math.max(pagesFromOldFile, pagesFromNewFile);
        }

        // 3. 默认估算
        return 25; // 默认25页
    }

    /**
     * 从文件名中提取页数
     */
    private int extractPageCountFromFileName(String fileName) {
        if (fileName == null) return 0;
        
        // 匹配 "数字页" 或 "数字p" 或 "数字P" 的模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)[页pP]");
        java.util.regex.Matcher matcher = pattern.matcher(fileName);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        return 0;
    }

    
    /**
     * 为特定任务估算总时间（考虑不同文档的页数）
     */
    private long estimateTotalTimeForTask(CompareTask task) {
        long totalTime = 0;
        String[] steps = {"INIT", "OCR_FIRST_DOC", "OCR_SECOND_DOC", "OCR_COMPLETE", 
                         "TEXT_COMPARE", "DIFF_ANALYSIS", "BLOCK_MERGE", "OCR_VALIDATION", 
                         "RESULT_GENERATION", "TASK_COMPLETE"};
        
        for (String step : steps) {
            totalTime += getStepEstimatedTimeForTask(task, step);
        }
        
        return totalTime;
    }

    /**
     * 获取当前步骤描述
     */
    private String getCurrentStepDescription(CompareTask task) {
        String currentStep = task.getCurrentStepDesc();
        if (currentStep == null) {
            return "准备中";
        }
        
        switch (currentStep) {
            case "初始化": return "正在初始化处理环境";
            case "OCR识别第一个文档": return "正在OCR识别原文档";
            case "OCR识别第二个文档": return "正在OCR识别新文档";
            case "OCR识别完成": return "OCR识别完成";
            case "文本比对": return "正在进行文本比对";
            case "差异分析": return "正在分析文档差异";
            case "差异块合并": return "正在合并差异块";
            case "OCR验证": return "正在进行OCR验证";
            case "结果生成": return "正在生成比对结果";
            case "任务完成": return "任务即将完成";
            default: return currentStep;
        }
    }

    /**
     * 获取当前步骤名称
     */
    private String getCurrentStepName(CompareTask task) {
        String currentStepDesc = task.getCurrentStepDesc();
        return mapStepDescToStepName(currentStepDesc);
    }

    /**
     * 映射步骤描述到步骤名称
     */
    private String mapStepDescToStepName(String stepDesc) {
        if (stepDesc == null) return "INIT";
        
        switch (stepDesc) {
            case "初始化": return "INIT";
            case "OCR识别第一个文档": return "OCR_FIRST_DOC";
            case "OCR识别第二个文档": return "OCR_SECOND_DOC";
            case "OCR识别完成": return "OCR_COMPLETE";
            case "文本比对": return "TEXT_COMPARE";
            case "差异分析": return "DIFF_ANALYSIS";
            case "差异块合并": return "BLOCK_MERGE";
            case "OCR验证": return "OCR_VALIDATION";
            case "结果生成": return "RESULT_GENERATION";
            case "任务完成": return "TASK_COMPLETE";
            default: return "INIT";
        }
    }

    /**
     * 格式化进度
     */
    private String formatProgress(double progress) {
        return String.format("%.1f%%", progress);
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long millis) {
        if (millis <= 0) return "计算中";
        
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "分钟";
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            return hours + "小时" + (remainingMinutes > 0 ? remainingMinutes + "分钟" : "");
        }
    }

    /**
     * 进度信息类
     */
    public static class ProgressInfo {
        private final double progressPercentage;
        private final long remainingTimeMs;
        private final String progressDescription;
        private final String currentStepDescription;
        private final String estimatedTotalTime;
        private final double stageMinProgress;
        private final double stageMaxProgress;
        private final long stageEstimatedTime;
        private final long stageElapsedTime;

        public ProgressInfo(double progressPercentage, long remainingTimeMs, String progressDescription, String currentStepDescription) {
            this(progressPercentage, remainingTimeMs, progressDescription, currentStepDescription, "", 0.0, 100.0, 0L, 0L);
        }

        public ProgressInfo(double progressPercentage, long remainingTimeMs, String progressDescription, 
                           String currentStepDescription, String estimatedTotalTime, double stageMinProgress, 
                           double stageMaxProgress, long stageEstimatedTime, long stageElapsedTime) {
            this.progressPercentage = progressPercentage;
            this.remainingTimeMs = remainingTimeMs;
            this.progressDescription = progressDescription;
            this.currentStepDescription = currentStepDescription;
            this.estimatedTotalTime = estimatedTotalTime;
            this.stageMinProgress = stageMinProgress;
            this.stageMaxProgress = stageMaxProgress;
            this.stageEstimatedTime = stageEstimatedTime;
            this.stageElapsedTime = stageElapsedTime;
        }

        // Getters
        public double getProgressPercentage() { return progressPercentage; }
        public long getRemainingTimeMs() { return remainingTimeMs; }
        public String getProgressDescription() { return progressDescription; }
        public String getCurrentStepDescription() { return currentStepDescription; }
        public String getEstimatedTotalTime() { return estimatedTotalTime; }
        public String getRemainingTimeFormatted() { 
            return remainingTimeMs > 0 ? formatTime(remainingTimeMs) : "计算中"; 
        }
        public double getStageMinProgress() { return stageMinProgress; }
        public double getStageMaxProgress() { return stageMaxProgress; }
        public long getStageEstimatedTime() { return stageEstimatedTime; }
        public long getStageElapsedTime() { return stageElapsedTime; }

        private String formatTime(long millis) {
            if (millis <= 0) return "计算中";
            
            long seconds = millis / 1000;
            if (seconds < 60) {
                return "约" + seconds + "秒";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return "约" + minutes + "分钟";
            } else {
                long hours = seconds / 3600;
                long remainingMinutes = (seconds % 3600) / 60;
                return "约" + hours + "小时" + (remainingMinutes > 0 ? remainingMinutes + "分钟" : "");
            }
        }
    }
}