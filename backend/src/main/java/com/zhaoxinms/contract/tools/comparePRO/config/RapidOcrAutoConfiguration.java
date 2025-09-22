package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RapidOCR自动配置类
 */
@Configuration
@EnableConfigurationProperties(RapidOcrConfig.class)
@ConditionalOnProperty(prefix = "zxcm.compare.rapidocr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RapidOcrAutoConfiguration {

    @Bean
    public com.zhaoxinms.contract.tools.comparePRO.client.RapidOcrClient rapidOcrClient(RapidOcrConfig config) {
        return com.zhaoxinms.contract.tools.comparePRO.client.RapidOcrClient.builder()
                .baseUrl(config.getBaseUrl())
                .timeout(config.getTimeout())
                .verboseLogging(config.isVerboseLogging())
                .maxConcurrency(config.getMaxConcurrency())
                .build();
    }
}
