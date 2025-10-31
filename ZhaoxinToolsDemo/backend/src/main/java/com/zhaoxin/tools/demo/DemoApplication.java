package com.zhaoxin.tools.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 肇新工具集 Demo 主应用
 * 
 * @author Zhaoxin Team
 * @version 1.0.0
 */
@SpringBootApplication
public class DemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("肇新工具集 Demo 后端服务已启动");
        System.out.println("访问地址: http://localhost:8091");
        System.out.println("========================================\n");
    }
}

