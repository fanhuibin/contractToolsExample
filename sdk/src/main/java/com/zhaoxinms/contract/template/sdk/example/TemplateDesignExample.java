package com.zhaoxinms.contract.template.sdk.example;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 模板设计SDK使用示例
 */
@Component
public class TemplateDesignExample implements CommandLineRunner {

    @Autowired
    @Qualifier("templateServiceImpl")
    private TemplateService templateService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== SDK项目启动成功 ===");
        
        // 1. 获取字段信息
        System.out.println("\n=== 获取字段信息 ===");
        FieldResponse fieldResponse = templateService.getFields();
        
        System.out.println("基础字段：");
        fieldResponse.getBaseFields().forEach(field -> 
            System.out.println("  - " + field.getName() + " (" + field.getCode() + ")"));
        
        System.out.println("相对方字段：");
        fieldResponse.getCounterpartyFields().forEach(field -> 
            System.out.println("  - " + field.getName() + " (" + field.getCode() + ")"));
        
        System.out.println("条款字段：");
        fieldResponse.getClauseFields().forEach(field -> 
            System.out.println("  - " + field.getName() + " (" + field.getCode() + ")"));
        
        // 2. 根据模板ID获取字段信息
        System.out.println("\n=== 根据模板ID获取字段信息 ===");
        FieldResponse simpleTemplateFields = templateService.getFieldsByTemplateId("template_simple");
        System.out.println("简单模板字段数量：");
        System.out.println("  基础字段：" + simpleTemplateFields.getBaseFields().size());
        System.out.println("  相对方字段：" + simpleTemplateFields.getCounterpartyFields().size());
        System.out.println("  条款字段：" + simpleTemplateFields.getClauseFields().size());
        
        System.out.println("\n=== SDK项目示例运行完成 ===");
    }
} 