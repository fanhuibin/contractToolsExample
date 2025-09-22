package com.zhaoxinms.contract.tools.ocr.rapidocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RapidOCR自动配置类
 */
@Configuration
@EnableConfigurationProperties(RapidOcrConfig.class)
@ConditionalOnProperty(prefix = "rapidocr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RapidOcrAutoConfiguration {

    @Bean
    public RapidOcrClient rapidOcrClient(RapidOcrConfig config) {
        return RapidOcrClient.builder()
                .baseUrl(config.getBaseUrl())
                .timeout(config.getTimeout())
                .verboseLogging(config.isVerboseLogging())
                .maxConcurrency(config.getMaxConcurrency())
                .build();
    }
}
