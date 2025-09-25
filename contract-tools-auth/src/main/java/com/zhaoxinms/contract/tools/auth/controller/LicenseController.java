package com.zhaoxinms.contract.tools.auth.controller;

import com.zhaoxinms.contract.tools.auth.service.LicenseService;
import com.zhaoxinms.contract.tools.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 授权管理控制器
 * 提供授权状态查询接口
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    /**
     * 获取授权信息
     */
    @GetMapping("/license-info")
    public Result<LicenseService.LicenseInfo> getLicenseInfo() {
        try {
            LicenseService.LicenseInfo info = licenseService.getLicenseInfo();
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("获取授权信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查指定功能的授权状态
     */
    @GetMapping("/check-feature")
    public Result<Boolean> checkFeature(@RequestParam String feature) {
        try {
            boolean hasFeature = licenseService.hasFeature(feature);
            return Result.success(hasFeature);
        } catch (Exception e) {
            return Result.error("检查功能授权失败: " + e.getMessage());
        }
    }

    /**
     * 检查授权是否有效
     */
    @GetMapping("/validate")
    public Result<Boolean> validateLicense() {
        try {
            boolean isValid = licenseService.isLicenseValid();
            return Result.success(isValid);
        } catch (Exception e) {
            return Result.error("授权验证失败: " + e.getMessage());
        }
    }
}
