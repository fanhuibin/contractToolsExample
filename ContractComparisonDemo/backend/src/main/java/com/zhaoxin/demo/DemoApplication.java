package com.zhaoxin.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 合同比对集成示例应用
 * 
 * @author Zhaoxin Team
 */
@SpringBootApplication
public class DemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("========================================");
        System.out.println("合同比对 Demo 已启动");
        System.out.println("访问地址: http://localhost:8090");
        System.out.println("========================================");
    }
}

