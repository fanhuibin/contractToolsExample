package com.zhaoxinms.contract.tools.extract.config;

import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.providers.aliyun.AliyunLLMProvider;
import com.zhaoxinms.contract.tools.extract.service.ExtractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 文本提取模块自动配置
 */
@Slf4j 
@Configuration
@ConditionalOnProperty(prefix = "zhaoxin.extract", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.zhaoxinms.contract.tools.extract")
@EnableConfigurationProperties(ExtractProperties.class)
public class ExtractAutoConfiguration {
    
    /**
     * 创建阿里云LLM提供商
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zhaoxin.extract.aliyun", name = "apiKey")
    public AliyunLLMProvider aliyunLLMProvider(ExtractProperties properties) {
        ExtractProperties.Aliyun aliyunConfig = properties.getAliyun();
        
        log.info("初始化阿里云LLM提供商，模型: {}", aliyunConfig.getModel());
        
        return new AliyunLLMProvider(aliyunConfig.getApiKey(), aliyunConfig.getModel());
    }
    
    /** 
     * 创建提取引擎
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtractEngine extractEngine(ExtractProperties properties) {
        // 检查是否配置了阿里云API Key
        String apiKey = properties.getAliyun().getApiKey();
        
        if (apiKey != null && !apiKey.trim().isEmpty() && !"your-dashscope-api-key-here".equals(apiKey)) {
            log.info("使用阿里云API初始化文本提取引擎，模型: {}", properties.getAliyun().getModel());
            AliyunLLMProvider provider = new AliyunLLMProvider(apiKey, properties.getAliyun().getModel());
            return new ExtractEngine(provider);
        } else {
            log.warn("未配置有效的阿里云API Key，创建默认提取引擎（功能受限）");
            AliyunLLMProvider defaultProvider = new AliyunLLMProvider("default-key", "qwen-turbo");
            return new ExtractEngine(defaultProvider);
        }
    }
    
    /**
     * 创建提取服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtractService extractService(ExtractEngine engine, ExtractProperties properties) {
        log.info("初始化文本提取服务");
        return new ExtractService(engine, properties);
    }
}
