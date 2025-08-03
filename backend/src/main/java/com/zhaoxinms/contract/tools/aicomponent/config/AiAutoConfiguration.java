package com.zhaoxinms.contract.tools.aicomponent.config;

import com.zhaoxinms.contract.tools.aicomponent.constants.AiConstants;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.service.impl.DefaultOpenAiServiceImpl;
import com.zhaoxinms.contract.tools.aicomponent.service.impl.DisabledOpenAiServiceImpl;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import com.zhaoxinms.contract.tools.aicomponent.util.StringUtil;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;
 
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
        @ConfigurationProperties(prefix = AiConstants.CONFIGURATION_PREFIX)
        public AiProperties getAiProperties() {
            return new AiProperties();
        }

        @Bean
        public OpenAiService getDefaultOpenAiService(@Qualifier(AiConstants.DEFAULT_HTTP_CLIENT_BEAN_NAME) OkHttpClient okHttpClient, AiProperties aiProperties) {
            return new DefaultOpenAiServiceImpl(okHttpClient, aiProperties);
        }

        @Bean
        public AiLimitUtil getAiLimitUtil(AiProperties aiProperties) {
            return new AiLimitUtil(aiProperties);
        }

        /**
         * 默认okhttpclient
         */
        @Bean(name = AiConstants.DEFAULT_HTTP_CLIENT_BEAN_NAME)
        @ConditionalOnMissingBean(name = AiConstants.DEFAULT_HTTP_CLIENT_BEAN_NAME)
        public OkHttpClient getDefaultOpenAiOkHttpClient(AiProperties aiProperties) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            if (aiProperties.getProxy() != null
                    && StringUtil.isNotEmpty(aiProperties.getProxy().getHost()) && aiProperties.getProxy().getPort() != null) {
                // 设置代理
                builder.proxy(new Proxy(aiProperties.getProxy().getType(), new InetSocketAddress(aiProperties.getProxy().getHost(), aiProperties.getProxy().getPort())));
                // 设置代理认证
                if (StringUtil.isNotEmpty(aiProperties.getProxy().getUsername()) && StringUtil.isNotEmpty(aiProperties.getProxy().getPassword())) {
                    builder.proxyAuthenticator(Authenticator.JAVA_NET_AUTHENTICATOR);
                    java.net.Authenticator.setDefault(new java.net.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            // 返回代理的用户名和密码
                            return new PasswordAuthentication(aiProperties.getProxy().getUsername(), aiProperties.getProxy().getPassword().toCharArray());
                        }
                    });
                }
            }

            return builder
                    .callTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .connectTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .readTimeout(aiProperties.getTimeout(), TimeUnit.SECONDS)
                    .build();
        }
    }
}