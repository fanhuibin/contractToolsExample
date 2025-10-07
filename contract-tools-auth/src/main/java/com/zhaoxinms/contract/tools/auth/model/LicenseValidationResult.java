package com.zhaoxinms.contract.tools.auth.model;

import lombok.Data;

/**
 * License验证结果
 */
@Data
public class LicenseValidationResult {
    
    /**
     * 验证是否通过
     */
    private boolean valid;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * License信息
     */
    private LicenseInfo licenseInfo;
    
    public static LicenseValidationResult success(LicenseInfo licenseInfo) {
        LicenseValidationResult result = new LicenseValidationResult();
        result.setValid(true);
        result.setLicenseInfo(licenseInfo);
        return result;
    }
    
    public static LicenseValidationResult failure(String errorMessage) {
        LicenseValidationResult result = new LicenseValidationResult();
        result.setValid(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
