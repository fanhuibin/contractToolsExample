package com.zhaoxinms.contract.tools.aicomponent.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zhaoxinms.contract.tools.aicomponent.constants.AiConstants;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.service.impl.DefaultOpenAiServiceImpl;
import com.zhaoxinms.contract.tools.aicomponent.service.impl.DisabledOpenAiServiceImpl;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;

import okhttp3.OkHttpClient;

/**
 * AI客户端 自动配置
 *
 * @author zhaoxinms
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = AiConstants.CONFIGURATION_PREFIX, name = "enabled", havingValue = "false", matchIfMissing = true)
    public OpenAiService getDisabledOpenAiService() {
        return new DisabledOpenAiServiceImpl();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = AiConstants.CONFIGURATION_PREFIX, name = "enabled", havingValue = "true")
    public static class AiEnabledConfiguration {

        @Bean
        public AiProperties getAiProperties() {
            return new AiProperties();
        }

        @Bean(AiConstants.DEFAULT_HTTP_CLIENT_BEAN_NAME)
        public OkHttpClient getOkHttpClient(AiProperties aiProperties) {
            return new OkHttpClient.Builder()
                    .connectTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .readTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .build();
        }

        @Bean("defaultOpenAiServiceImpl")
        public OpenAiService getDefaultOpenAiService(OkHttpClient okHttpClient, AiProperties aiProperties, AiLimitUtil aiLimitUtil) {
            return new DefaultOpenAiServiceImpl(okHttpClient, aiProperties, aiLimitUtil);
        }

        @Bean
        public AiLimitUtil getAiLimitUtil(AiProperties aiProperties) {
            return new AiLimitUtil(aiProperties);
        }
    }
}
