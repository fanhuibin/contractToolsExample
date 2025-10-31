package com.zhaoxinms.contract.tools.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 授权模块自动配置类
 * <p>授权模块强制启用，不可禁用</p>
 * 
 * @author 山西肇新科技有限公司
 */
@Configuration
@ComponentScan(basePackages = "com.zhaoxinms.contract.tools.auth")
@EnableConfigurationProperties(AuthProperties.class)
public class AuthAutoConfiguration {
    
    // 授权模块的自动配置
    // 授权模块是强制启用的，不可通过配置禁用
}
