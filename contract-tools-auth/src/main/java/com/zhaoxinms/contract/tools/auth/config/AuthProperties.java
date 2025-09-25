package com.zhaoxinms.contract.tools.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 授权模块配置属性
 * 简化版本，只包含基础的授权检查配置
 */
@Data
@ConfigurationProperties(prefix = "zhaoxin.auth")
public class AuthProperties {
    
    /**
     * 是否启用授权功能
     */
    private boolean enabled = false;
    
    /**
     * 授权码配置
     */
    private License license = new License();
    
    @Data
    public static class License {
        /**
         * 授权码
         */
        private String code;
        
        /**
         * 授权过期时间（毫秒时间戳）
         */
        private long expiration = 0;
        
        /**
         * 授权的功能列表
         */
        private String[] features = new String[0];
        
        /**
         * 最大用户数量
         */
        private int maxUsers = -1; // -1表示无限制
    }
}
