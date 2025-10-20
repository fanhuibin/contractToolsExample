package com.zhaoxinms.contract.tools.auth.aspect;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.service.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 授权检查切面
 * 拦截带有@RequireFeature注解的方法，进行授权检查
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseAspect {

    @Autowired
    private LicenseService licenseService;

    @Around("@annotation(requireFeature)")
    public Object checkFeatureAuthorization(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        // 优先检查模块授权
        ModuleType moduleType = requireFeature.module();
        if (moduleType != null) {
            if (!licenseService.hasModulePermission(moduleType)) {
                String message = requireFeature.message();
                log.warn("模块 '{}' 授权检查失败: {}", moduleType.getName(), message);
                // 使用授权错误码，前端会特殊处理
                throw BusinessException.of(ApiCode.MODULE_UNAUTHORIZED, 
                    message + "：" + moduleType.getName());
            }
            log.debug("模块 '{}' 授权检查通过", moduleType.getName());
        } else if (CommonUtils.isNotEmpty(requireFeature.value())) {
            // 兼容旧版本的功能授权检查
            String feature = requireFeature.value();
            if (!licenseService.hasFeature(feature)) {
                String message = requireFeature.message();
                log.warn("功能 '{}' 授权检查失败: {}", feature, message);
                // 使用授权错误码，前端会特殊处理
                throw BusinessException.of(ApiCode.MODULE_UNAUTHORIZED, 
                    message + "：" + feature);
            }
            log.debug("功能 '{}' 授权检查通过", feature);
        } else {
            log.warn("@RequireFeature注解必须指定module或value参数");
            throw BusinessException.of(ApiCode.SERVER_ERROR, "授权配置错误");
        }
        
        return joinPoint.proceed();
    }
    
    @Around("@within(requireFeature)")
    public Object checkClassFeatureAuthorization(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        return checkFeatureAuthorization(joinPoint, requireFeature);
    }
}