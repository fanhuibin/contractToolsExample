package com.zhaoxinms.contract.template.sdk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * SDK项目主启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhaoxinms.contract.template.sdk",
    "com.zhaoxinms.contract.tools"
})
@EnableJpaRepositories(basePackages = {
    "com.zhaoxinms.contract.template.sdk",
    "com.zhaoxinms.contract.tools"
})
@EntityScan(basePackages = {
    "com.zhaoxinms.contract.template.sdk",
    "com.zhaoxinms.contract.tools"
})
@MapperScan(basePackages = {
    "com.zhaoxinms.contract.template.sdk",
    "com.zhaoxinms.contract.tools"
})
@EnableTransactionManagement
public class SdkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdkApplication.class, args);
    }
} 