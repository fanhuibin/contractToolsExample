package com.zhaoxinms.contract.template.sdk.config;

import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Backend配置类
 * 从SDK配置中读取zxcm前缀的配置，传递给Backend项目
 */
@Configuration
public class BackendConfig {

    /**
     * 创建ZxcmConfig实例，从配置文件读取zxcm前缀的配置
     */
    @Bean
    @ConfigurationProperties(prefix = "zxcm")
    public ZxcmConfig zxcmConfig() {
        return new ZxcmConfig();
    }
    
    /**
     * 创建RestTemplate实例，用于HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 