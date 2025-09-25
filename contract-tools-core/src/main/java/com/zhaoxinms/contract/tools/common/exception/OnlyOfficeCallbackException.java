package com.zhaoxinms.contract.tools.common.exception;

/**
 * OnlyOffice回调异常
 * 用于封装OnlyOffice回调处理中的异常信息
 */
public class OnlyOfficeCallbackException extends RuntimeException {
    
    private final String fileId;
    private final String callbackType;
    private final int errorCode;
    
    public OnlyOfficeCallbackException(String fileId, String callbackType, String message) {
        this(fileId, callbackType, 1, message);
    }
    
    public OnlyOfficeCallbackException(String fileId, String callbackType, int errorCode, String message) {
        super(String.format("OnlyOffice回调处理失败 [fileId=%s, type=%s, code=%d]: %s", 
                fileId, callbackType, errorCode, message));
        this.fileId = fileId;
        this.callbackType = callbackType;
        this.errorCode = errorCode;
    }
    
    public OnlyOfficeCallbackException(String fileId, String callbackType, String message, Throwable cause) {
        this(fileId, callbackType, 1, message, cause);
    }
    
    public OnlyOfficeCallbackException(String fileId, String callbackType, int errorCode, String message, Throwable cause) {
        super(String.format("OnlyOffice回调处理失败 [fileId=%s, type=%s, code=%d]: %s", 
                fileId, callbackType, errorCode, message), cause);
        this.fileId = fileId;
        this.callbackType = callbackType;
        this.errorCode = errorCode;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public String getCallbackType() {
        return callbackType;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
