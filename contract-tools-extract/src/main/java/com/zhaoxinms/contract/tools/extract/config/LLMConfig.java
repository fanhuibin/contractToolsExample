package com.zhaoxinms.contract.tools.extract.config;

import lombok.Data;

/**
 * LLM配置类
 */
@Data
public class LLMConfig {
    
    /**
     * 默认使用的LLM提供者: aliyun, ollama
     */
    private String provider = "ollama";
    
    /**
     * 阿里云配置
     */
    private AliyunConfig aliyun = new AliyunConfig();
    
    /**
     * Ollama配置
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    @Data
    public static class AliyunConfig {
        private String apiKey = "sk-2c162cad693a4aa5b1f96c2bfcf2a56a";
        private String model = "qwen-plus";
        private Double temperature = 0.1;
        private Integer maxTokens = 4000;
    }
    
    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://192.168.0.100:11434/";
        private String model = "deepseek-r1:8b";
        private Double temperature = 0.1;
        private Integer maxTokens = 8000; // DeepSeek-R1需要更多tokens来完整输出思考过程和结果
    }
}
