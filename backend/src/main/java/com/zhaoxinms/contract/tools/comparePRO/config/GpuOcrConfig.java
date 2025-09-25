package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GPU OCR配置属性
 * 从SDK配置文件中读取GPU OCR相关配置
 * 
 * @author zhaoxin
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class GpuOcrConfig {
    
    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();
    
    /**
     * 任务处理配置
     */
    private Task task = new Task();
    
    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();
    
    public static class ThreadPool {
        /**
         * 核心线程数
         */
        private int corePoolSize = 2;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 8;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 200;
        
        /**
         * 线程空闲时间（秒）
         */
        private long keepAliveTime = 60L;
        
        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "GPU-OCR-Worker-";
        
        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeout = false;
        
        // Getters and Setters
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
        
        public long getKeepAliveTime() {
            return keepAliveTime;
        }
        
        public void setKeepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
        
        public boolean isAllowCoreThreadTimeout() {
            return allowCoreThreadTimeout;
        }
        
        public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
        }
    }
    
    public static class Task {
        /**
         * 队列繁忙阈值
         */
        private double busyThreshold = 0.8;
        
        /**
         * 任务超时时间（分钟）
         */
        private int timeoutMinutes = 30;
        
        // Getters and Setters
        public double getBusyThreshold() {
            return busyThreshold;
        }
        
        public void setBusyThreshold(double busyThreshold) {
            this.busyThreshold = busyThreshold;
        }
        
        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }
        
        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
    }
    
    public static class Monitoring {
        /**
         * 是否启用详细日志
         */
        private boolean enableDetailedLogging = true;
        
        /**
         * 统计信息输出间隔（秒）
         */
        private int statsIntervalSeconds = 300;
        
        // Getters and Setters
        public boolean isEnableDetailedLogging() {
            return enableDetailedLogging;
        }
        
        public void setEnableDetailedLogging(boolean enableDetailedLogging) {
            this.enableDetailedLogging = enableDetailedLogging;
        }
        
        public int getStatsIntervalSeconds() {
            return statsIntervalSeconds;
        }
        
        public void setStatsIntervalSeconds(int statsIntervalSeconds) {
            this.statsIntervalSeconds = statsIntervalSeconds;
        }
    }
    
    // Main getters and setters
    public ThreadPool getThreadPool() {
        return threadPool;
    }
    
    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }
    
    public Task getTask() {
        return task;
    }
    
    public void setTask(Task task) {
        this.task = task;
    }
    
    public Monitoring getMonitoring() {
        return monitoring;
    }
    
    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }
    
    @Override
    public String toString() {
        return String.format(
            "GpuOcrConfig{" +
            "corePoolSize=%d, maxPoolSize=%d, queueCapacity=%d, " +
            "keepAliveTime=%d, threadNamePrefix='%s', " +
            "busyThreshold=%.2f, timeoutMinutes=%d, " +
            "enableDetailedLogging=%s, statsIntervalSeconds=%d}",
            threadPool.getCorePoolSize(), threadPool.getMaxPoolSize(), 
            threadPool.getQueueCapacity(), threadPool.getKeepAliveTime(),
            threadPool.getThreadNamePrefix(), task.getBusyThreshold(),
            task.getTimeoutMinutes(), monitoring.isEnableDetailedLogging(),
            monitoring.getStatsIntervalSeconds()
        );
    }
}
