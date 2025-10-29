package com.zhaoxin.demo.model.exception;

/**
 * API 调用异常
 */
public class ApiException extends RuntimeException {
    
    private Integer code;
    
    public ApiException(String message) {
        super(message);
        this.code = 500;
    }
    
    public ApiException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
    
    public Integer getCode() {
        return code;
    }
}

