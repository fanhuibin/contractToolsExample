package com.zhaoxinms.contract.tools.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 合同工具集后端应用启动类
 * 
 * 支持两种运行模式：
 * 1. 完整模式：包含授权管理功能（通过配置zhaoxin.auth.enabled=true启用）
 * 2. 简单模式：仅包含核心业务功能
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhaoxinms.contract.tools.backend",
    "com.zhaoxinms.contract.tools",  // 扫描核心模块
    "com.zhaoxinms.contract.tools.auth"  // 扫描授权模块（如果存在）
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
