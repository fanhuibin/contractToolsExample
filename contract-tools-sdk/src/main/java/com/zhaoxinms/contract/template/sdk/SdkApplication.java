package com.zhaoxinms.contract.template.sdk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * SDK项目主启动类
 * 
 * 支持两种依赖模式：
 * 1. 仅依赖核心库（contract-tools-core）- 轻量级模式
 * 2. 依赖完整后端（包含授权模块）- 完整功能模式
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.zhaoxinms.contract.template.sdk", 
                "com.zhaoxinms.contract.tools",
                "com.zhaoxinms.contract.tools.auth"  // 如果启用授权模块
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.zhaoxinms\\.contract\\.tools\\.controller\\.FileDownloadController")
        }
)
@MapperScan(basePackages = {
    "com.zhaoxinms.contract.template.sdk.mapper",
    "com.zhaoxinms.contract.tools.aicomponent.mapper"
})
@EnableTransactionManagement
public class SdkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdkApplication.class, args);
    }
}  