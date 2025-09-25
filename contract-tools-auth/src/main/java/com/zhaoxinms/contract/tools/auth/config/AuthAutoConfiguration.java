package com.zhaoxinms.contract.tools.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 授权模块自动配置类
 * 只有当启用授权功能时才加载此配置
 */
@Configuration
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.zhaoxinms.contract.tools.auth")
@EnableConfigurationProperties(AuthProperties.class)
public class AuthAutoConfiguration {
    
    // 授权模块的自动配置将在此处定义
    // 注意：Spring Security的依赖已设置为optional，只有显式启用才会加载
}
