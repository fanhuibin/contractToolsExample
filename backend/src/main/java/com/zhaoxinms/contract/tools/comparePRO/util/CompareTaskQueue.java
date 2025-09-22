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

import org.springframework.stereotype.Component;

/**
 * GPU OCR任务队列管理器
 * 提供高效的并发任务处理能力，支持任务优先级、队列管理和资源监控
 * 
 * @author zhaoxin
 * @version 1.0
 */
@Component
public class CompareTaskQueue {
    
    // 核心线程数（始终保持活跃）
    private static final int CORE_POOL_SIZE = 2;
    
    // 最大线程数（根据配置动态调整）
    private int maxPoolSize = 4;
    
    // 线程空闲时间（秒）
    private static final long KEEP_ALIVE_TIME = 60L;
    
    // 任务队列容量
    private static final int QUEUE_CAPACITY = 100;
    
    // 线程池执行器
    private ThreadPoolExecutor executor;
    
    // 任务队列
    private final BlockingQueue<Runnable> taskQueue;
    
    // 统计信息
    private final AtomicLong totalSubmitted = new AtomicLong(0);
    private final AtomicLong totalCompleted = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    
    // 自定义线程工厂
    private static class GPUOCRThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "GPU-OCR-Worker-";
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
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
        // 初始化任务队列
        this.taskQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        
        // 初始化线程池
        initializeThreadPool();
        
        System.out.println("GPU OCR任务队列初始化完成:");
        System.out.println("  - 核心线程数: " + CORE_POOL_SIZE);
        System.out.println("  - 最大线程数: " + maxPoolSize);
        System.out.println("  - 队列容量: " + QUEUE_CAPACITY);
        System.out.println("  - 线程空闲时间: " + KEEP_ALIVE_TIME + "秒");
    }
    
    /**
     * 初始化线程池
     */
    private void initializeThreadPool() {
        this.executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            maxPoolSize,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            taskQueue,
            new GPUOCRThreadFactory(),
            new GPUOCRRejectedExecutionHandler(totalRejected)
        );
        
        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(false);
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
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.printf("[%s] 任务 %s 已提交到队列，队列大小: %d, 活跃线程: %d/%d%n",
                timestamp, taskId, getQueueSize(), getActiveCount(), getMaximumPoolSize());
            
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
                System.out.printf("[%s] 线程 %s 开始执行任务 %s%n", timestamp, threadName, taskId);
                long startTime = System.currentTimeMillis();
                
                // 执行原始任务
                originalTask.run();
                
                long endTime = System.currentTimeMillis();
                totalCompleted.incrementAndGet();
                
                System.out.printf("[%s] 线程 %s 完成任务 %s，耗时: %d毫秒%n", 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                    threadName, taskId, (endTime - startTime));
                
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
        if (newMaxPoolSize < CORE_POOL_SIZE) {
            System.err.println("最大线程数不能小于核心线程数: " + CORE_POOL_SIZE);
            return;
        }
        
        if (newMaxPoolSize != this.maxPoolSize) {
            this.maxPoolSize = newMaxPoolSize;
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
     * @return 如果队列使用率超过80%则认为繁忙
     */
    public boolean isBusy() {
        double queueUsage = (double) getQueueSize() / QUEUE_CAPACITY;
        double threadUsage = (double) getActiveCount() / getMaximumPoolSize();
        
        return queueUsage > 0.8 || threadUsage > 0.8;
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
