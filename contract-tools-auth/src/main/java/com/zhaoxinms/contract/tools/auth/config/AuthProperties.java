package com.zhaoxinms.contract.tools.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 授权模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "zhaoxin.auth")
public class AuthProperties {
    
    /**
     * 是否启用授权功能
     */
    private boolean enabled = false;
    
    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();
    
    /**
     * 会话配置
     */
    private Session session = new Session();
    
    @Data
    public static class Jwt {
        /**
         * JWT密钥
         */
        private String secret = "zhaoxin-contract-tools-default-secret-key";
        
        /**
         * JWT过期时间（秒）
         */
        private long expiration = 86400; // 24小时
        
        /**
         * 刷新令牌过期时间（秒）
         */
        private long refreshExpiration = 604800; // 7天
    }
    
    @Data
    public static class Session {
        /**
         * 会话超时时间（秒）
         */
        private long timeout = 1800; // 30分钟
        
        /**
         * 最大并发会话数
         */
        private int maxConcurrentSessions = 1;
    }
}
