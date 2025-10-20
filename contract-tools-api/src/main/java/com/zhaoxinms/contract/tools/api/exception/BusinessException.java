package com.zhaoxinms.contract.tools.api.exception;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import lombok.Getter;

/**
 * 业务异常
 * 
 * 用于主动抛出业务逻辑异常，会被全局异常处理器捕获
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    
    public BusinessException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.code = apiCode.getCode();
        this.message = apiCode.getMessage();
    }
    
    public BusinessException(ApiCode apiCode, String message) {
        super(message);
        this.code = apiCode.getCode();
        this.message = message;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 快捷方法 - 抛出业务错误
     */
    public static BusinessException of(ApiCode apiCode) {
        return new BusinessException(apiCode);
    }
    
    /**
     * 快捷方法 - 抛出自定义消息的业务错误
     */
    public static BusinessException of(ApiCode apiCode, String message) {
        return new BusinessException(apiCode, message);
    }
    
    /**
     * 快捷方法 - 模板不存在
     */
    public static BusinessException templateNotFound(String templateId) {
        return new BusinessException(ApiCode.TEMPLATE_NOT_FOUND, 
                "模板不存在: " + templateId);
    }
    
    /**
     * 快捷方法 - 任务不存在
     */
    public static BusinessException taskNotFound(String taskId) {
        return new BusinessException(ApiCode.BUSINESS_ERROR, 
                "任务不存在: " + taskId);
    }
    
    /**
     * 快捷方法 - 文件不存在
     */
    public static BusinessException fileNotFound(String fileName) {
        return new BusinessException(ApiCode.FILE_NOT_FOUND, 
                "文件不存在: " + fileName);
    }
}

