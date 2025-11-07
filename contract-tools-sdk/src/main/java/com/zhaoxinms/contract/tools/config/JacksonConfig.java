package com.zhaoxinms.contract.tools.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 * 解决前端 JavaScript 精度丢失问题
 * 
 * @author 山西肇新科技有限公司
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 Jackson ObjectMapper
     * 将 Long 类型序列化为 String，避免 JavaScript 精度丢失
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();
        
        // Long 类型序列化为 String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        
        // 注册模块
        objectMapper.registerModule(simpleModule);
        
        return objectMapper;
    }
}

