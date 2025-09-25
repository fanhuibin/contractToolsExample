package com.zhaoxinms.contract.tools.comparePRO.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zhaoxinms.contract.tools.comparePRO.config.GpuOcrConfig;

/**
 * GPU OCR任务队列管理器
 * 提供高效的并发任务处理能力，支持任务优先级、队列管理和资源监控
 * 
 * @author zhaoxin
 * @version 1.0
 */
@Component
public class CompareTaskQueue {
    
    @Autowired
    private GpuOcrConfig config;
    
    // 线程池配置参数（从配置文件读取）
    private int corePoolSize;
    private int maxPoolSize;
    private long keepAliveTime;
    private int queueCapacity;
    private String threadNamePrefix;
    private boolean allowCoreThreadTimeout;
    private double busyThreshold;
    private boolean enableDetailedLogging;
    
    // 线程池执行器
    private ThreadPoolExecutor executor;
    
    // 任务队列
    private BlockingQueue<Runnable> taskQueue;
    
    // 统计信息
    private final AtomicLong totalSubmitted = new AtomicLong(0);
    private final AtomicLong totalCompleted = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    
    // 自定义线程工厂
    private class GPUOCRThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, threadNamePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    // 自定义拒绝策略
    private static class GPUOCRRejectedExecutionHandler implements RejectedExecutionHandler {
        private final AtomicLong rejectedCount;
        
        public GPUOCRRejectedExecutionHandler(AtomicLong rejectedCount) {
            this.rejectedCount = rejectedCount;
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedCount.incrementAndGet();
            
            // 记录拒绝日志
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.err.printf("[%s] GPU OCR任务被拒绝执行，当前队列大小: %d, 活跃线程: %d%n",
                timestamp, executor.getQueue().size(), executor.getActiveCount());
            
            // 尝试在调用线程中执行（降级策略）
            if (!executor.isShutdown()) {
                try {
                    System.out.println("尝试在调用线程中执行被拒绝的任务...");
                    r.run();
                } catch (Exception e) {
                    System.err.println("在调用线程中执行任务失败: " + e.getMessage());
                }
            }
        }
    }
    
    public CompareTaskQueue() {
        // 构造函数中不做初始化，等待Spring注入完成后再初始化
    }
    
    /**
     * Spring Bean初始化后调用
     */
    @javax.annotation.PostConstruct
    public void init() {
        // 从配置文件读取参数
        loadConfiguration();
        
        // 初始化任务队列
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        
        // 初始化线程池
        initializeThreadPool();
        
        System.out.println("GPU OCR任务队列初始化完成:");
        System.out.println("  - 核心线程数: " + corePoolSize);
        System.out.println("  - 最大线程数: " + maxPoolSize);
        System.out.println("  - 队列容量: " + queueCapacity);
        System.out.println("  - 线程空闲时间: " + keepAliveTime + "秒");
        System.out.println("  - 线程名称前缀: " + threadNamePrefix);
        System.out.println("  - 详细日志: " + (enableDetailedLogging ? "启用" : "禁用"));
        System.out.println("  - 最大处理能力: " + (maxPoolSize + queueCapacity) + "个并发任务");
    }
    
    /**
     * 从配置文件加载配置参数
     */
    private void loadConfiguration() {
        this.corePoolSize = config.getThreadPool().getCorePoolSize();
        this.maxPoolSize = config.getThreadPool().getMaxPoolSize();
        this.keepAliveTime = config.getThreadPool().getKeepAliveTime();
        this.queueCapacity = config.getThreadPool().getQueueCapacity();
        this.threadNamePrefix = config.getThreadPool().getThreadNamePrefix();
        this.allowCoreThreadTimeout = config.getThreadPool().isAllowCoreThreadTimeout();
        this.busyThreshold = config.getTask().getBusyThreshold();
        this.enableDetailedLogging = config.getMonitoring().isEnableDetailedLogging();
        
        // 参数验证
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("核心线程数必须大于0，当前配置: " + corePoolSize);
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("最大线程数不能小于核心线程数，当前配置: maxPoolSize=" + maxPoolSize + ", corePoolSize=" + corePoolSize);
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0，当前配置: " + queueCapacity);
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("线程空闲时间不能为负数，当前配置: " + keepAliveTime);
        }
        
        System.out.println("GPU OCR线程池配置加载完成: " + config.toString());
    }
    
    /**
     * 初始化线程池
     */
    private void initializeThreadPool() {
        this.executor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            taskQueue,
            new GPUOCRThreadFactory(),
            new GPUOCRRejectedExecutionHandler(totalRejected)
        );
        
        // 设置是否允许核心线程超时
        executor.allowCoreThreadTimeOut(allowCoreThreadTimeout);
    }
    
    /**
     * 提交任务到队列
     * 
     * @param task 要执行的任务
     * @param taskId 任务ID（用于日志）
     * @return 是否成功提交
     */
    public boolean submitTask(Runnable task, String taskId) {
        try {
            // 包装任务，添加统计和日志
            Runnable wrappedTask = wrapTask(task, taskId);
            
            // 提交任务
            executor.execute(wrappedTask);
            totalSubmitted.incrementAndGet();
            
            if (enableDetailedLogging) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                System.out.printf("[%s] 任务 %s 已提交到队列，队列大小: %d, 活跃线程: %d/%d%n",
                    timestamp, taskId, getQueueSize(), getActiveCount(), getMaximumPoolSize());
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.printf("提交任务 %s 失败: %s%n", taskId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 包装任务，添加统计和日志功能
     */
    private Runnable wrapTask(Runnable originalTask, String taskId) {
        return () -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String threadName = Thread.currentThread().getName();
            
            try {
                if (enableDetailedLogging) {
                    System.out.printf("[%s] 线程 %s 开始执行任务 %s%n", timestamp, threadName, taskId);
                }
                long startTime = System.currentTimeMillis();
                
                // 执行原始任务
                originalTask.run();
                
                long endTime = System.currentTimeMillis();
                totalCompleted.incrementAndGet();
                
                if (enableDetailedLogging) {
                    System.out.printf("[%s] 线程 %s 完成任务 %s，耗时: %d毫秒%n", 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                        threadName, taskId, (endTime - startTime));
                }
                
            } catch (Exception e) {
                System.err.printf("[%s] 线程 %s 执行任务 %s 失败: %s%n", 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                    threadName, taskId, e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * 动态调整最大线程数
     * 
     * @param newMaxPoolSize 新的最大线程数
     */
    public void adjustMaxPoolSize(int newMaxPoolSize) {
        if (newMaxPoolSize < corePoolSize) {
            System.err.println("最大线程数不能小于核心线程数: " + corePoolSize);
            return;
        }
        
        if (newMaxPoolSize != executor.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(newMaxPoolSize);
            
            System.out.printf("GPU OCR线程池最大线程数已调整为: %d%n", newMaxPoolSize);
        }
    }
    
    /**
     * 获取队列统计信息
     */
    public TaskQueueStats getStats() {
        return new TaskQueueStats(
            totalSubmitted.get(),
            totalCompleted.get(),
            totalRejected.get(),
            getQueueSize(),
            getActiveCount(),
            getMaximumPoolSize(),
            executor.getCompletedTaskCount(),
            executor.getTaskCount()
        );
    }
    
    /**
     * 获取当前队列大小
     */
    public int getQueueSize() {
        return executor.getQueue().size();
    }
    
    /**
     * 获取活跃线程数
     */
    public int getActiveCount() {
        return executor.getActiveCount();
    }
    
    /**
     * 获取最大线程数
     */
    public int getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }
    
    /**
     * 检查队列是否繁忙
     * 
     * @return 如果队列使用率超过配置的阈值则认为繁忙
     */
    public boolean isBusy() {
        double queueUsage = (double) getQueueSize() / queueCapacity;
        double threadUsage = (double) getActiveCount() / getMaximumPoolSize();
        
        return queueUsage > busyThreshold || threadUsage > busyThreshold;
    }
    
    /**
     * 优雅关闭线程池
     */
    public void shutdown() {
        System.out.println("开始关闭GPU OCR任务队列...");
        
        executor.shutdown();
        
        try {
            // 等待60秒让任务完成
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("强制关闭GPU OCR任务队列...");
                executor.shutdownNow();
                
                // 再等待30秒
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("GPU OCR任务队列无法正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 输出最终统计
        TaskQueueStats finalStats = getStats();
        System.out.println("GPU OCR任务队列关闭完成，最终统计:");
        System.out.println(finalStats);
    }
    
    /**
     * 任务队列统计信息
     */
    public static class TaskQueueStats {
        private final long totalSubmitted;
        private final long totalCompleted;
        private final long totalRejected;
        private final int currentQueueSize;
        private final int activeThreads;
        private final int maxThreads;
        private final long executorCompletedTasks;
        private final long executorTotalTasks;
        
        public TaskQueueStats(long totalSubmitted, long totalCompleted, long totalRejected,
                             int currentQueueSize, int activeThreads, int maxThreads,
                             long executorCompletedTasks, long executorTotalTasks) {
            this.totalSubmitted = totalSubmitted;
            this.totalCompleted = totalCompleted;
            this.totalRejected = totalRejected;
            this.currentQueueSize = currentQueueSize;
            this.activeThreads = activeThreads;
            this.maxThreads = maxThreads;
            this.executorCompletedTasks = executorCompletedTasks;
            this.executorTotalTasks = executorTotalTasks;
        }
        
        // Getters
        public long getTotalSubmitted() { return totalSubmitted; }
        public long getTotalCompleted() { return totalCompleted; }
        public long getTotalRejected() { return totalRejected; }
        public int getCurrentQueueSize() { return currentQueueSize; }
        public int getActiveThreads() { return activeThreads; }
        public int getMaxThreads() { return maxThreads; }
        public long getExecutorCompletedTasks() { return executorCompletedTasks; }
        public long getExecutorTotalTasks() { return executorTotalTasks; }
        
        @Override
        public String toString() {
            return String.format(
                "GPU OCR任务队列统计:\n" +
                "  - 总提交任务数: %d\n" +
                "  - 总完成任务数: %d\n" +
                "  - 总拒绝任务数: %d\n" +
                "  - 当前队列大小: %d\n" +
                "  - 活跃线程数: %d/%d\n" +
                "  - 执行器完成任务数: %d\n" +
                "  - 执行器总任务数: %d",
                totalSubmitted, totalCompleted, totalRejected,
                currentQueueSize, activeThreads, maxThreads,
                executorCompletedTasks, executorTotalTasks
            );
        }
    }
}
