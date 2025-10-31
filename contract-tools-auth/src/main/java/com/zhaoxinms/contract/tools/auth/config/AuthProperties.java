package com.zhaoxinms.contract.tools.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 授权配置属性
 * <p>授权模块强制启用，证书文件路径已硬编码，不可配置</p>
 * 
 * @author 山西肇新科技有限公司
 */
@Data
@ConfigurationProperties(prefix = "zhaoxin.auth")
public class AuthProperties {
    
    /**
     * 授权文件路径（硬编码，不可配置）
     * <p>加载优先级：</p>
     * <ol>
     *   <li>jar包同级目录: ./license.lic</li>
     *   <li>config目录: ./config/license.lic</li>
     *   <li>classpath: classpath:license.lic (开发环境)</li>
     * </ol>
     */
    public static final String LICENSE_FILE_PATH = "license.lic";
    
    /**
     * 公钥文件路径（硬编码，不可配置）
     * <p>加载优先级：</p>
     * <ol>
     *   <li>jar包同级目录: ./public.key</li>
     *   <li>config目录: ./config/public.key</li>
     *   <li>classpath: classpath:public.key (开发环境)</li>
     * </ol>
     */
    public static final String PUBLIC_KEY_PATH = "public.key";
    
    /**
     * 私钥文件路径（硬编码，仅用于授权生成器）
     */
    public static final String PRIVATE_KEY_PATH = "private.key";
}