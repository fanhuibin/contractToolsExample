package com.zhaoxinms.contract.tools.auth.aspect;

import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
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
        String feature = requireFeature.value();
        
        if (!licenseService.hasFeature(feature)) {
            String message = requireFeature.message();
            log.warn("功能 '{}' 授权检查失败: {}", feature, message);
            throw new RuntimeException(message + ": " + feature);
        }
        
        log.debug("功能 '{}' 授权检查通过", feature);
        return joinPoint.proceed();
    }
    
    @Around("@within(requireFeature)")
    public Object checkClassFeatureAuthorization(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        return checkFeatureAuthorization(joinPoint, requireFeature);
    }
}
