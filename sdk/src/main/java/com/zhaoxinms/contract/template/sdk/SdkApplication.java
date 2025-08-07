package com.zhaoxinms.contract.template.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * SDK项目主启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhaoxinms.contract.template.sdk",
    "com.zhaoxinms.contract.tools"
})
public class SdkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdkApplication.class, args);
    }
} 