package com.zhaoxinms.contract.tools.api.config;

import com.zhaoxinms.contract.tools.api.interceptor.TraceIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final TraceIdInterceptor traceIdInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册TraceID拦截器（对所有请求生效）
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
    }
}

