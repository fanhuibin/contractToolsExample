package com.zhaoxinms.contract.tools.common.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 雪花ID生成器
 * 基于Twitter的雪花算法实现
 */
@Component
public class SnowflakeIdGenerator {
    
    /**
     * 开始时间戳 (2024-01-01 00:00:00)
     */
    private final long startTimestamp = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
            .toInstant(ZoneOffset.of("+8")).toEpochMilli();
    
    /**
     * 机器ID所占位数
     */
    private final long workerIdBits = 5L;
    
    /**
     * 数据中心ID所占位数
     */
    private final long dataCenterIdBits = 5L;
    
    /**
     * 序列号所占位数
     */
    private final long sequenceBits = 12L;
    
    /**
     * 支持的最大机器ID
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    
    /**
     * 支持的最大数据中心ID
     */
    private final long maxDataCenterId = ~(-1L << dataCenterIdBits);
    
    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;
    
    /**
     * 数据中心ID向左移17位
     */
    private final long dataCenterIdShift = sequenceBits + workerIdBits;
    
    /**
     * 时间戳向左移22位
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
    
    /**
     * 序列号掩码，用于限定序列号的最大值4095
     */
    private final long sequenceMask = ~(-1L << sequenceBits);
    
    /**
     * 工作机器ID(0~31)
     */
    private long workerId;
    
    /**
     * 数据中心ID(0~31)
     */
    private long dataCenterId;
    
    /**
     * 毫秒内序列号(0~4095)
     */
    private long sequence = 0L;
    
    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator() {
        this(0L, 0L);
    }
    
    /**
     * 构造函数
     * @param workerId 工作机器ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + maxWorkerId + " or less than 0");
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException("DataCenter ID can't be greater than " + maxDataCenterId + " or less than 0");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }
    
    /**
     * 生成下一个ID
     * @return 雪花ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + 
                    (lastTimestamp - timestamp) + " milliseconds");
        }
        
        // 如果是同一时间生成的，则进行毫秒内序列号递增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列号溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列号重置
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - startTimestamp) << timestampLeftShift) |
                (dataCenterId << dataCenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 返回当前时间戳
     * @return 当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
} 