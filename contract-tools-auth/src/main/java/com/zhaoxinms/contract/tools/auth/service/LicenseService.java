package com.zhaoxinms.contract.tools.auth.service;

import com.zhaoxinms.contract.tools.auth.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 授权码检查服务
 * 提供基础的授权验证功能，不依赖Spring Security
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseService {

    @Autowired
    private AuthProperties authProperties;

    /**
     * 检查是否有指定功能的授权
     * 
     * @param feature 功能名称
     * @return 是否有授权
     */
    public boolean hasFeature(String feature) {
        if (!authProperties.isEnabled()) {
            return true; // 授权模块未启用时，允许所有功能
        }
        
        AuthProperties.License license = authProperties.getLicense();
        if (license == null) {
            log.warn("授权配置为空");
            return false;
        }
        
        // 检查授权是否过期
        if (license.getExpiration() > 0 && Instant.now().toEpochMilli() > license.getExpiration()) {
            log.warn("授权已过期");
            return false;
        }
        
        // 检查功能授权
        if (license.getFeatures() == null || license.getFeatures().length == 0) {
            log.warn("未配置功能授权");
            return false;
        }
        
        Set<String> authorizedFeatures = new HashSet<>(Arrays.asList(license.getFeatures()));
        return authorizedFeatures.contains(feature) || authorizedFeatures.contains("*");
    }
    
    /**
     * 检查授权码是否有效
     * 
     * @return 是否有效
     */
    public boolean isLicenseValid() {
        if (!authProperties.isEnabled()) {
            return true; // 授权模块未启用时，认为授权有效
        }
        
        AuthProperties.License license = authProperties.getLicense();
        if (license == null || license.getCode() == null || license.getCode().trim().isEmpty()) {
            return false;
        }
        
        // 检查是否过期
        if (license.getExpiration() > 0 && Instant.now().toEpochMilli() > license.getExpiration()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取授权信息
     * 
     * @return 授权信息
     */
    public LicenseInfo getLicenseInfo() {
        if (!authProperties.isEnabled()) {
            return LicenseInfo.unlimited();
        }
        
        AuthProperties.License license = authProperties.getLicense();
        if (license == null) {
            return LicenseInfo.invalid();
        }
        
        LicenseInfo info = new LicenseInfo();
        info.setValid(isLicenseValid());
        info.setExpiration(license.getExpiration());
        info.setFeatures(license.getFeatures());
        info.setMaxUsers(license.getMaxUsers());
        
        return info;
    }
    
    /**
     * 授权信息DTO
     */
    public static class LicenseInfo {
        private boolean valid;
        private long expiration;
        private String[] features;
        private int maxUsers;
        
        public static LicenseInfo unlimited() {
            LicenseInfo info = new LicenseInfo();
            info.setValid(true);
            info.setExpiration(0);
            info.setFeatures(new String[]{"*"});
            info.setMaxUsers(-1);
            return info;
        }
        
        public static LicenseInfo invalid() {
            LicenseInfo info = new LicenseInfo();
            info.setValid(false);
            info.setExpiration(0);
            info.setFeatures(new String[0]);
            info.setMaxUsers(0);
            return info;
        }
        
        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
        
        public String[] getFeatures() { return features; }
        public void setFeatures(String[] features) { this.features = features; }
        
        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    }
}
