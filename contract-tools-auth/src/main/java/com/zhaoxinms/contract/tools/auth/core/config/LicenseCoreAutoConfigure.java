package com.zhaoxinms.contract.tools.auth.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;

/**
 * <p>license-core模块中的Bean实现自动装配 -- 配置类</p>
 *
 * @author appleyk
 * @version V.0.2.1
 * @blob https://blog.csdn.net/appleyk
 * @date created on 10:24 下午 2020/8/21
 */
@Configuration
public class LicenseCoreAutoConfigure {
    public LicenseCoreAutoConfigure(){
        LoggerHelper.info("============ license-core-spring-boot-starter initialization！ ===========");
    }
}
