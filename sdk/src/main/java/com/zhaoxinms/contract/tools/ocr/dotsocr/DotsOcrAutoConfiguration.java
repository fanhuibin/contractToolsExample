package com.zhaoxinms.contract.tools.ocr.dotsocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Dots.OCR 自动配置（可选启用）
 *
 * 配置开关：zxcm.dotsocr.enabled=true
 * 属性前缀：zxcm.dotsocr.*
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnProperty(prefix = "zxcm.dotsocr", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DotsOcrAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "zxcm.dotsocr")
    public DotsOcrProperties dotsOcrProperties() {
        return new DotsOcrProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = "dotsOcrOkHttpClient")
    public OkHttpClient dotsOcrOkHttpClient(DotsOcrProperties props) {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper dotsOcrObjectMapper() { return new ObjectMapper(); }

    @Bean
    @ConditionalOnMissingBean
    public DotsOcrClient dotsOcrClient(DotsOcrProperties props, OkHttpClient dotsOcrOkHttpClient, ObjectMapper dotsOcrObjectMapper) {
        return DotsOcrClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultModel(props.getModel())
                .apiKey(props.getApiKey())
                .httpClient(dotsOcrOkHttpClient)
                .build();
    }
}


