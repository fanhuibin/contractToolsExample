package com.zhaoxin.tools.demo.config;

import com.zhaoxin.tools.demo.model.exception.ApiException;
import com.zhaoxin.tools.demo.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理 API 调用异常
     */
    @ExceptionHandler(ApiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleApiException(ApiException e) {
        log.error("API调用异常", e);
        return new ApiResponse<>(e.getCode(), e.getMessage(), null);
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return new ApiResponse<>(500, "系统内部错误: " + e.getMessage(), null);
    }
}

