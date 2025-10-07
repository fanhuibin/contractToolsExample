package com.zhaoxinms.contract.tools.extract.core.exceptions;

/**
 * LLM提供商相关异常
 */
public class ProviderException extends ExtractException {
    
    private final String provider;
    private final String errorCode;
    
    public ProviderException(String provider, String message) {
        super(message);
        this.provider = provider;
        this.errorCode = null;
    }
    
    public ProviderException(String provider, String errorCode, String message) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
    }
    
    public ProviderException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = null;
    }
    
    public ProviderException(String provider, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = errorCode;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
