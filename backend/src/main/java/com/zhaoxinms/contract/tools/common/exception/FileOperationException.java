package com.zhaoxinms.contract.tools.common.exception;

/**
 * 文件操作异常
 * 用于封装文件保存、读取等操作中的异常信息
 */
public class FileOperationException extends RuntimeException {
    
    private final String fileId;
    private final String operation;
    
    public FileOperationException(String fileId, String operation, String message) {
        super(String.format("文件操作失败 [fileId=%s, operation=%s]: %s", fileId, operation, message));
        this.fileId = fileId;
        this.operation = operation;
    }
    
    public FileOperationException(String fileId, String operation, String message, Throwable cause) {
        super(String.format("文件操作失败 [fileId=%s, operation=%s]: %s", fileId, operation, message), cause);
        this.fileId = fileId;
        this.operation = operation;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public String getOperation() {
        return operation;
    }
}
