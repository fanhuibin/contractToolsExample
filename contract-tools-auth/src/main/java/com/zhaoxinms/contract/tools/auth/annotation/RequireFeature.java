package com.zhaoxinms.contract.tools.auth.annotation;

import java.lang.annotation.*;

/**
 * 功能授权注解
 * 用于标记需要特定功能授权的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {
    
    /**
     * 需要的功能名称
     */
    String value();
    
    /**
     * 授权失败时的错误消息
     */
    String message() default "当前功能需要授权";
}
