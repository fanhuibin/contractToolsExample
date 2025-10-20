package com.zhaoxinms.contract.template.sdk.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger Web MVC配置类
 * 
 * 用于注册Swagger相关的拦截器
 * 
 * @author zhaoxin
 * @since 2024-10-18
 */
@Configuration
public class SwaggerWebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private SwaggerInterceptor swaggerInterceptor;

    /**
     * 注册Swagger访问拦截器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册Swagger访问拦截器
        registry.addInterceptor(swaggerInterceptor)
                .addPathPatterns(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/doc.html",
                    "/swagger-resources/**",
                    "/v2/api-docs",
                    "/v3/api-docs",
                    "/webjars/**"
                );
    }
}

