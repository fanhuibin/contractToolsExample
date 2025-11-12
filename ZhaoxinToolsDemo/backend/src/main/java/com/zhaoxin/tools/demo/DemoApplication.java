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
        SpringApplication app = new SpringApplication(DemoApplication.class);
        var context = app.run(args);
        
        // 从配置中获取端口号
        String port = context.getEnvironment().getProperty("server.port", "8091");
        String demoBackendUrl = context.getEnvironment().getProperty("zhaoxin.demo.backend-url", "http://localhost:" + port);
        
        System.out.println("\n========================================");
        System.out.println("肇新工具集 Demo 后端服务已启动");
        System.out.println("访问地址: " + demoBackendUrl);
        System.out.println("========================================\n");
    }
}

