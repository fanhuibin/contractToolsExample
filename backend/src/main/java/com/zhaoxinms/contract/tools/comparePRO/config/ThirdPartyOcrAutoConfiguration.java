package com.zhaoxinms.contract.tools.comparePRO.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.client.ThirdPartyOcrClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 第三方OCR服务自动配置类
 */
@Configuration
@EnableConfigurationProperties(ThirdPartyOcrConfig.class)
@ConditionalOnProperty(name = "zxcm.compare.third-party-ocr.enabled", havingValue = "true")
public class ThirdPartyOcrAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyOcrAutoConfiguration.class);

    /**
     * 创建第三方OCR客户端Bean
     */
    @Bean
    public ThirdPartyOcrClient thirdPartyOcrClient(ThirdPartyOcrConfig config) {
        logger.info("正在初始化第三方OCR客户端...");
        
        // 验证配置
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalStateException("第三方OCR服务的API密钥未配置，请设置 zxcm.compare.third-party-ocr.api-key");
        }

        // 创建HTTP客户端
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout())
                .readTimeout(config.getTimeout())
                .writeTimeout(config.getTimeout())
                .build();

        // 创建ThirdPartyOcrClient
        ThirdPartyOcrClient client = new ThirdPartyOcrClient(
                config.getBaseUrl(),
                config.getApiKey(),
                config.getDefaultModel(),
                httpClient,
                new ObjectMapper(),
                config.isVerboseLogging(),
                config.getMaxConcurrency(),
                config.getMinPixels(),
                config.getMaxPixels()
        );

        logger.info("第三方OCR客户端初始化完成");
        logger.info("- 基础URL: {}", config.getBaseUrl());
        logger.info("- 默认模型: {}", config.getDefaultModel());
        logger.info("- 最大并发数: {}", config.getMaxConcurrency());
        logger.info("- 像素范围: {} - {}", config.getMinPixels(), config.getMaxPixels());
        
        return client;
    }
}
