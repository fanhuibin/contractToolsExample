package com.zhaoxinms.contract.tools.common.util;

/**
 * 雪花算法ID生成器
 * Twitter的Snowflake算法实现
 * 
 * ID结构：64位
 * - 1位：符号位（始终为0）
 * - 41位：时间戳（毫秒级），可使用约69年
 * - 10位：工作机器ID（5位数据中心ID + 5位机器ID）
 * - 12位：序列号（同一毫秒内的序列）
 * 
 * @author 山西肇新科技有限公司
 */
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     */
    private static final long START_TIMESTAMP = 1704038400000L;

    /**
     * 每部分占用的位数
     */
    private static final long DATACENTER_ID_BITS = 5L;  // 数据中心ID所占位数
    private static final long WORKER_ID_BITS = 5L;       // 机器ID所占位数
    private static final long SEQUENCE_BITS = 12L;       // 序列号所占位数

    /**
     * 每部分的最大值
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);  // 31
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);          // 31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);            // 4095

    /**
     * 每部分向左的位移
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                              // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;        // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    /**
     * 数据中心ID (0-31)
     */
    private final long datacenterId;

    /**
     * 机器ID (0-31)
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 单例实例
     */
    private static volatile SnowflakeIdGenerator instance;

    /**
     * 构造函数
     * 
     * @param datacenterId 数据中心ID (0-31)
     * @param workerId 机器ID (0-31)
     */
    private SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("数据中心ID不能大于 %d 或小于 0", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("机器ID不能大于 %d 或小于 0", MAX_WORKER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 获取单例实例（使用默认的数据中心ID和机器ID）
     * 
     * @return SnowflakeIdGenerator实例
     */
    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    // 默认使用数据中心ID=1，机器ID=1
                    instance = new SnowflakeIdGenerator(1L, 1L);
                }
            }
        }
        return instance;
    }

    /**
     * 获取单例实例（自定义数据中心ID和机器ID）
     * 
     * @param datacenterId 数据中心ID (0-31)
     * @param workerId 机器ID (0-31)
     * @return SnowflakeIdGenerator实例
     */
    public static SnowflakeIdGenerator getInstance(long datacenterId, long workerId) {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator(datacenterId, workerId);
                }
            }
        }
        return instance;
    }

    /**
     * 生成下一个ID（线程安全）
     * 
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("系统时钟回退。拒绝生成ID，上次时间戳: %d，当前时间戳: %d",
                    lastTimestamp, timestamp));
        }

        // 如果是同一毫秒内生成的，则进行序列号累加
        if (timestamp == lastTimestamp) {
            // 序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号已经达到最大值，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号重置为0
            sequence = 0L;
        }

        // 更新上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 组合生成64位ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 解析雪花ID，提取时间戳、数据中心ID、机器ID和序列号
     * 
     * @param id 雪花ID
     * @return ID信息的字符串表示
     */
    public static String parseId(long id) {
        long timestamp = ((id >> TIMESTAMP_SHIFT) & ~(-1L << 41L)) + START_TIMESTAMP;
        long datacenterId = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long workerId = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long sequence = id & MAX_SEQUENCE;
        
        return String.format(
            "雪花ID: %d\n时间戳: %d (%s)\n数据中心ID: %d\n机器ID: %d\n序列号: %d",
            id, timestamp, new java.util.Date(timestamp), datacenterId, workerId, sequence);
    }
}

