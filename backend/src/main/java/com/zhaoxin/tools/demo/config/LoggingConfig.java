package com.zhaoxin.tools.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 日志配置类
 * 用于控制HTTP请求和响应的日志输出
 */
@Configuration
public class LoggingConfig {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);
    
    /**
     * 配置RestTemplate，添加日志拦截器
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 添加请求拦截器，控制日志输出
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add((request, body, execution) -> {
            try {
                // 记录请求信息（简化）
                log.info("HTTP请求: {} {}", request.getMethod(), request.getURI());
                
                // 执行请求
                var response = execution.execute(request, body);
                
                // 记录响应状态（简化）
                log.info("HTTP响应: {} - {}", response.getStatusCode().value(), 
                        response.getStatusCode().getReasonPhrase());
                
                return response;
                
            } catch (Exception e) {
                // 只记录关键错误信息，不打印HTML内容
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("<!DOCTYPE html>")) {
                    // 如果是HTML错误页面，只记录状态码和简要信息
                    log.error("HTTP请求失败: {} {} - 服务器返回错误页面", 
                            request.getMethod(), request.getURI());
                } else {
                    // 其他错误正常记录
                    log.error("HTTP请求失败: {} {} - {}", 
                            request.getMethod(), request.getURI(), 
                            errorMsg != null ? errorMsg.split("\n")[0] : "未知错误");
                }
                throw e;
            }
        });
        
        return restTemplate;
    }
}
