package com.zhaoxinms.contract.tools.extract.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文本提取模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "zhaoxin.extract")
public class ExtractProperties {
    
    /**
     * 是否启用文本提取功能
     */
    private boolean enabled = true;
    
    /**
     * 阿里云配置
     */
    private Aliyun aliyun = new Aliyun();
    
    /**
     * 默认提取选项
     */
    private DefaultOptions defaultOptions = new DefaultOptions();
    
    @Data
    public static class Aliyun {
        /**
         * 阿里云API Key
         */
        private String apiKey;
        
        /**
         * 默认使用的模型
         */
        private String model = "qwen-turbo";
        
        /**
         * API端点（可选）
         */
        private String endpoint;
        
        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 30;
        
        /**
         * 读取超时时间（秒）
         */
        private int readTimeout = 60;
    }
    
    @Data
    public static class DefaultOptions {
        /**
         * 默认输出格式
         */
        private String format = "json";
        
        /**
         * 默认温度参数
         */
        private Double temperature = 0.1;
        
        /**
         * 默认最大token数
         */
        private Integer maxTokens = 2000;
        
        /**
         * 默认置信度阈值
         */
        private Double confidenceThreshold = 0.5;
        
        /**
         * 是否快速失败
         */
        private boolean failFast = false;
    }
}
