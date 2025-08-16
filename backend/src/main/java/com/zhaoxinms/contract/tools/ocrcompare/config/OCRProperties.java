package com.zhaoxinms.contract.tools.ocrcompare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR配置属性（仅REST相关配置 + 通用设置/任务配置）
 */
@Component
@ConfigurationProperties(prefix = "ocr")
public class OCRProperties {

    private Settings settings = new Settings();
    private Task task = new Task();

    // 任务配置
    public static class Task {
        private int timeout = 20;                // 分钟
        private int resultRetentionDays = 7;     // 天
        private int statusCheckInterval = 5;     // 秒

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public int getResultRetentionDays() { return resultRetentionDays; }
        public void setResultRetentionDays(int resultRetentionDays) { this.resultRetentionDays = resultRetentionDays; }

        public int getStatusCheckInterval() { return statusCheckInterval; }
        public void setStatusCheckInterval(int statusCheckInterval) { this.statusCheckInterval = statusCheckInterval; }
    }

    // 设置配置
    public static class Settings {
        private int dpi = 150;
        private double minScore = 0.5;
        private boolean fastMode = false;
        private boolean debugMode = false;
        private boolean enableLogging = true;

        public int getDpi() { return dpi; }
        public void setDpi(int dpi) { this.dpi = dpi; }

        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }

        public boolean isFastMode() { return fastMode; }
        public void setFastMode(boolean fastMode) { this.fastMode = fastMode; }

        public boolean isDebugMode() { return debugMode; }
        public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

        public boolean isEnableLogging() { return enableLogging; }
        public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
    }

    public Settings getSettings() { return settings; }
    public void setSettings(Settings settings) { this.settings = settings; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
}


