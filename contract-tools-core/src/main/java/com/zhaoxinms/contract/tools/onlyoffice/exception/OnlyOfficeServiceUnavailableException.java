package com.zhaoxinms.contract.tools.onlyoffice.exception;

/**
 * OnlyOffice服务不可用异常
 * 用于明确标识OnlyOffice服务不可用的情况
 */
public class OnlyOfficeServiceUnavailableException extends RuntimeException {
    
    public OnlyOfficeServiceUnavailableException(String message) {
        super(message);
    }
    
    public OnlyOfficeServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
