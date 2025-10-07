package com.zhaoxinms.contract.tools.auth.annotation;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;

import java.lang.annotation.*;

/**
 * 模块授权注解
 * 标记在方法上，表示该方法需要特定的模块授权才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {
    
    /**
     * 需要的模块类型
     * @return 模块类型
     */
    ModuleType module();
    
    /**
     * 需要的功能名称（兼容旧版本）
     * @return 功能名称
     */
    String value() default "";
    
    /**
     * 当授权失败时的错误消息
     * @return 错误消息
     */
    String message() default "权限不足，无法访问该功能";
}