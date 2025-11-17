package com.zhaoxin.tools.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理HTTP请求异常，避免打印冗长的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理HTTP服务器错误（如502 Bad Gateway）
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException e) {
        // 只记录关键信息，不打印HTML内容
        log.error("外部API调用失败: {} - {}", e.getStatusCode(), e.getStatusText());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "外部服务暂时不可用，请稍后重试");
        response.put("code", e.getStatusCode().value());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * 处理网络连接异常
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException e) {
        log.error("网络连接异常: {}", e.getMessage().split("\n")[0]);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "网络连接失败，请检查网络配置");
        response.put("code", 503);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * 处理其他REST客户端异常
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientException(RestClientException e) {
        String errorMsg = e.getMessage();
        
        // 避免打印HTML内容
        if (errorMsg != null && errorMsg.contains("<!DOCTYPE html>")) {
            log.error("API调用返回错误页面");
        } else {
            log.error("API调用异常: {}", errorMsg != null ? errorMsg.split("\n")[0] : "未知错误");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "API调用失败，请稍后重试");
        response.put("code", 500);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统内部错误");
        response.put("code", 500);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
