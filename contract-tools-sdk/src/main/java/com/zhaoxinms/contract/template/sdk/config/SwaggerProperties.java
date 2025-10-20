package com.zhaoxinms.contract.template.sdk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Swagger配置属性
 * 
 * @author zhaoxin
 * @since 2024-10-18
 */
@Component
@ConfigurationProperties(prefix = "zxcm.swagger")
public class SwaggerProperties {

    /**
     * 是否启用Swagger文档
     */
    private boolean enabled = true;

    /**
     * 是否需要密码访问
     */
    private boolean requirePassword = false;

    /**
     * 访问密码
     */
    private String password = "zxcm";

    /**
     * 企业信息
     */
    private Company company = new Company();

    /**
     * API文档信息
     */
    private Api api = new Api();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequirePassword() {
        return requirePassword;
    }

    public void setRequirePassword(boolean requirePassword) {
        this.requirePassword = requirePassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    /**
     * 企业信息配置
     */
    public static class Company {
        private String name = "肇新科技";
        private String url = "https://www.zhaoxinms.com";
        private String email = "develop@zhaoxinms.com";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * API文档信息配置
     */
    public static class Api {
        private String title = "肇新合同工具集API文档";
        private String description = "提供合同比对、模板设计、规则提取等功能的RESTful API接口";
        private String version = "1.0.0";
        private String basePackage = "com.zhaoxinms.contract";

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getBasePackage() {
            return basePackage;
        }

        public void setBasePackage(String basePackage) {
            this.basePackage = basePackage;
        }
    }
}

