package com.zhaoxin.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${zhaoxin.frontend.url}")
    private String frontendUrl;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 使用 allowedOriginPatterns 代替 allowedOrigins，支持通配符
                .allowedOriginPatterns(
                    frontendUrl,              // 肇新前端 (from config)
                    "http://localhost:*",     // 所有 localhost 端口
                    "http://127.0.0.1:*"      // 所有 127.0.0.1 端口
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

