package com.zhaoxinms.contract.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 合同工具集应用启动类
 * 
 * @author zhaoxinms
 */
@SpringBootApplication
public class ContractToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractToolsApplication.class, args);
        System.out.println("合同工具集应用启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
    }
} 