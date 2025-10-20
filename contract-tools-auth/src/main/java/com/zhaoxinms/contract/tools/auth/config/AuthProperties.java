package com.zhaoxinms.contract.tools.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证配置属性
 */
@Data
@ConfigurationProperties(prefix = "zhaoxin.auth")
public class AuthProperties {
    
    /**
     * 是否启用认证
     */
    private boolean enabled = false;
    
    /**
     * 许可证配置
     */
    private License license = new License();
    
    /**
     * 签名配置
     */
    private Signature signature = new Signature();
    
    @Data
    public static class License {
        /**
         * 许可证代码
         */
        private String code;
        
        /**
         * 许可证文件路径
         */
        private String filePath = "license.lic";
        
        /**
         * 到期时间
         */
        private LocalDateTime expiration;
        
        /**
         * 授权功能列表（模块代码）
         */
        private List<String> modules;
        
        /**
         * 最大用户数
         */
        private Integer maxUsers = 1;
        
        /**
         * 是否绑定硬件
         */
        private Boolean hardwareBound = true;
    }
    
    @Data
    public static class Signature {
        /**
         * 公钥字符串（用于验证签名）
         */
        private String publicKey;
        
        /**
         * 私钥字符串（用于生成签名，仅在生成license时使用）
         */
        private String privateKey;
        
        /**
         * 公钥文件路径
         */
        private String publicKeyPath = "public.key";
        
        /**
         * 私钥文件路径
         */
        private String privateKeyPath = "private.key";
    }
}