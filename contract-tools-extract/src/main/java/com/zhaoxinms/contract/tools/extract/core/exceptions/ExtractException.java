package com.zhaoxinms.contract.tools.extract.core.exceptions;

/**
 * 提取过程中的异常
 * 对应Python版本的LangExtractError
 */
public class ExtractException extends Exception {
    
    public ExtractException(String message) {
        super(message);
    }
    
    public ExtractException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExtractException(Throwable cause) {
        super(cause);
    }
}
