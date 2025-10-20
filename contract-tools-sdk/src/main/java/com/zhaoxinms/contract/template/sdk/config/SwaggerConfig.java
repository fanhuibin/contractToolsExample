package com.zhaoxinms.contract.template.sdk.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger配置类
 * 
 * 根据配置文件启用/禁用Swagger文档，支持企业信息自定义和密码保护
 * 
 * @author zhaoxin
 * @since 2024-10-18
 */
@Configuration
@EnableSwagger2
@ConditionalOnProperty(prefix = "zxcm.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    @Autowired
    private SwaggerProperties swaggerProperties;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描指定包路径下的API
                .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getApi().getBasePackage()))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 构建API文档信息
     */
    private ApiInfo apiInfo() {
        SwaggerProperties.Company company = swaggerProperties.getCompany();
        SwaggerProperties.Api api = swaggerProperties.getApi();
        
        return new ApiInfoBuilder()
                .title(api.getTitle())
                .description(api.getDescription())
                .contact(new Contact(company.getName(), company.getUrl(), company.getEmail()))
                .version(api.getVersion())
                .build();
    }
} 