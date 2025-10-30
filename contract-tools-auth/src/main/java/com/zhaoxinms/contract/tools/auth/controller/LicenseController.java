package com.zhaoxinms.contract.tools.auth.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.auth.config.AuthProperties;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader.LicenseReadResult;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;
import com.zhaoxinms.contract.tools.auth.model.LicenseValidationResult;
import com.zhaoxinms.contract.tools.auth.service.LicenseService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 授权管理控制器
 * 提供授权状态查询接口
 * 
 * @author zhaoxin
 * @since 2025-10-18
 */
@Api(tags = "授权管理")
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseController {

    @Autowired
    private LicenseService licenseService;
    
    @Autowired
    private AuthProperties authProperties;

    /**
     * 获取授权信息
     */
    @GetMapping("/license-info")
    @ApiOperation(value = "获取授权信息", notes = "获取当前系统的授权许可信息")
    public ApiResponse<Map<String, Object>> getLicenseInfo() {
        try {
            LicenseInfo info = licenseService.getLicenseInfo();
            if (info != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("licenseCode", info.getLicenseCode());
                result.put("companyName", info.getCompanyName());
                result.put("contactPerson", info.getContactPerson());
                result.put("contactPhone", info.getContactPhone());
                result.put("startDate", info.getStartDate());
                result.put("expireDate", info.getExpireDate());
                result.put("maxUsers", info.getMaxUsers());
                result.put("hardwareBound", info.getHardwareBound());
                result.put("authorizedModules", info.getAuthorizedModules());
                
                // 计算硬件匹配状态
                boolean hardwareMatched = false;
                if (info.getHardwareBound() != null && info.getHardwareBound()) {
                    hardwareMatched = validateHardwareMatch(info);
                } else {
                    // 未绑定硬件时视为匹配
                    hardwareMatched = true;
                }
                result.put("hardwareMatched", hardwareMatched);
                
                return ApiResponse.success(result);
            } else {
                return ApiResponse.<Map<String, Object>>businessError("未找到有效的授权信息");
            }
        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 检查指定模块的授权状态
     */
    @GetMapping("/check-module")
    @ApiOperation(value = "检查模块授权", notes = "检查指定模块是否已授权")
    public ApiResponse<Map<String, Object>> checkModule(
            @ApiParam(value = "模块代码", required = true, example = "smart_document_extraction")
            @RequestParam String moduleCode) {
        try {
            ModuleType moduleType = ModuleType.fromCode(moduleCode);
            
            if (moduleType == null) {
                return ApiResponse.<Map<String, Object>>paramError("无效的模块代码: " + moduleCode);
            }
            
            boolean hasPermission = licenseService.hasModulePermission(moduleType);
            Map<String, Object> data = new HashMap<>();
            data.put("hasPermission", hasPermission);
            data.put("moduleName", moduleType.getName());
            
            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 检查指定功能的授权状态（兼容旧接口）
     */
    @GetMapping("/check-feature")
    @ApiOperation(value = "检查功能授权", notes = "检查指定功能是否已授权（兼容旧接口）")
    public ApiResponse<Boolean> checkFeature(
            @ApiParam(value = "功能名称", required = true)
            @RequestParam String feature) {
        try {
            boolean hasFeature = licenseService.hasFeature(feature);
            return ApiResponse.success(hasFeature);
        } catch (Exception e) {
            return ApiResponse.<Boolean>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 验证授权
     */
    @GetMapping("/validate")
    @ApiOperation(value = "验证授权", notes = "验证当前授权许可是否有效")
    public ApiResponse<LicenseInfo> validateLicense() {
        try {
            LicenseValidationResult result = licenseService.validateLicense();
            
            if (result.isValid()) {
                return ApiResponse.success(result.getLicenseInfo());
            } else {
                return ApiResponse.<LicenseInfo>businessError(result.getErrorMessage());
            }
        } catch (Exception e) {
            return ApiResponse.<LicenseInfo>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 获取所有可用模块
     */
    @GetMapping("/modules")
    @ApiOperation(value = "获取所有模块", notes = "获取系统所有可用模块列表")
    public ApiResponse<ModuleType[]> getAvailableModules() {
        try {
            ModuleType[] modules = ModuleType.values();
            return ApiResponse.success(modules);
        } catch (Exception e) {
            return ApiResponse.<ModuleType[]>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 批量检查模块权限
     */
    @PostMapping("/check-modules")
    @ApiOperation(value = "批量检查模块权限", notes = "批量检查多个模块的授权状态")
    public ApiResponse<Map<String, Boolean>> checkModules(
            @ApiParam(value = "模块代码列表", required = true)
            @RequestBody String[] moduleCodes) {
        try {
            Map<String, Boolean> modulePermissions = new HashMap<>();
            
            for (String moduleCode : moduleCodes) {
                ModuleType moduleType = ModuleType.fromCode(moduleCode);
                if (moduleType != null) {
                    boolean hasPermission = licenseService.hasModulePermission(moduleType);
                    modulePermissions.put(moduleCode, hasPermission);
                } else {
                    modulePermissions.put(moduleCode, false);
                }
            }
            
            return ApiResponse.success(modulePermissions);
        } catch (Exception e) {
            return ApiResponse.<Map<String, Boolean>>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 获取License文件详细信息（包含签名验证和硬件匹配验证）
     */
    @GetMapping("/license-details")
    @ApiOperation(value = "获取授权详细信息", notes = "获取授权许可的完整信息，包括验证状态")
    public ApiResponse<Map<String, Object>> getLicenseDetails() {
        try {
            LicenseReader reader = new LicenseReader();
            String licenseFilePath = authProperties.getLicense().getFilePath();
            String publicKeyPath = authProperties.getSignature().getPublicKeyPath();
            
            LicenseReadResult result = reader.readLicense(licenseFilePath, publicKeyPath);
            
            if (result.isSuccess()) {
                Map<String, Object> details = new HashMap<>();
                LicenseInfo info = result.getLicenseInfo();
                
                // 基本信息
                details.put("licenseCode", info.getLicenseCode());
                details.put("companyName", info.getCompanyName());
                details.put("contactPerson", info.getContactPerson());
                details.put("contactPhone", info.getContactPhone());
                details.put("createTime", info.getCreateTime());
                details.put("startDate", info.getStartDate());
                details.put("expireDate", info.getExpireDate());
                details.put("maxUsers", info.getMaxUsers());
                details.put("hardwareBound", info.getHardwareBound());
                details.put("authorizedModules", info.getAuthorizedModules());
                details.put("boundHardwareInfo", info.getBoundHardwareInfo());
                
                // 验证状态
                Map<String, Object> validation = new HashMap<>();
                validation.put("signatureValid", result.isSignatureValid());
                validation.put("licenseValid", result.isLicenseValid());
                validation.put("overallValid", result.isSignatureValid() && result.isLicenseValid());
                
                // 时间状态
                if (info.getExpireDate() != null) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long daysUntilExpiry = java.time.Duration.between(now, info.getExpireDate()).toDays();
                    validation.put("daysUntilExpiry", daysUntilExpiry);
                    validation.put("isExpiringSoon", daysUntilExpiry <= 30 && daysUntilExpiry > 0);
                }
                
                details.put("validation", validation);
                return ApiResponse.success(details);
            } else {
                return ApiResponse.<Map<String, Object>>businessError(result.getErrorMessage());
            }
        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail(e.getMessage());
        }
    }

    /**
     * 验证硬件匹配
     */
    @GetMapping("/hardware-validation")
    @ApiOperation(value = "验证硬件匹配", notes = "验证当前服务器硬件是否与授权绑定的硬件匹配")
    public ApiResponse<Map<String, Object>> validateHardware() {
        try {
            LicenseInfo licenseInfo = licenseService.getLicenseInfo();
            
            if (licenseInfo == null) {
                return ApiResponse.<Map<String, Object>>businessError("无法获取License信息");
            }
            
            if (licenseInfo.getHardwareBound() == null || !licenseInfo.getHardwareBound()) {
                Map<String, Object> data = new HashMap<>();
                data.put("hardwareBound", false);
                data.put("matched", true);
                data.put("message", "License未启用硬件绑定");
                return ApiResponse.success(data);
            }
            
            // 获取当前硬件信息
            com.zhaoxinms.contract.tools.auth.core.service.AServerInfos serverInfos = 
                com.zhaoxinms.contract.tools.auth.core.service.AServerInfos.getServer(null);
            List<String> currentHardware = new ArrayList<>();
            
            // 收集MAC地址
            List<String> macAddresses = serverInfos.getMacAddress();
            if (macAddresses != null) {
                currentHardware.addAll(macAddresses);
            }
            
            // 收集CPU序列号
            String cpuSerial = serverInfos.getCPUSerial();
            if (cpuSerial != null && !cpuSerial.trim().isEmpty()) {
                currentHardware.add(cpuSerial);
            }
            
            // 收集主板序列号
            String mainBoardSerial = serverInfos.getMainBoardSerial();
            if (mainBoardSerial != null && !mainBoardSerial.trim().isEmpty()) {
                currentHardware.add(mainBoardSerial);
            }
            
            // 检查匹配
            boolean matched = false;
            List<String> boundHardware = licenseInfo.getBoundHardwareInfo();
            if (boundHardware != null && !boundHardware.isEmpty()) {
                // 检查MAC地址
                if (macAddresses != null) {
                    for (String mac : macAddresses) {
                        if (containsHardwareInfo(boundHardware, "macAddress", mac)) {
                            matched = true;
                            break;
                        }
                    }
                }
                
                // 检查CPU序列号
                if (!matched && cpuSerial != null && !cpuSerial.trim().isEmpty()) {
                    if (containsHardwareInfo(boundHardware, "cpuSerial", cpuSerial)) {
                        matched = true;
                    }
                }
                
                // 检查主板序列号
                if (!matched && mainBoardSerial != null && !mainBoardSerial.trim().isEmpty()) {
                    if (containsHardwareInfo(boundHardware, "mainBoardSerial", mainBoardSerial)) {
                        matched = true;
                    }
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("hardwareBound", true);
            data.put("matched", matched);
            data.put("currentHardware", currentHardware);
            data.put("boundHardware", boundHardware);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            return ApiResponse.<Map<String, Object>>serverError().errorDetail(e.getMessage());
        }
    }
    
    /**
     * 验证硬件是否匹配
     * 
     * @param licenseInfo 授权信息
     * @return 是否匹配
     */
    private boolean validateHardwareMatch(LicenseInfo licenseInfo) {
        try {
            if (licenseInfo.getBoundHardwareInfo() == null || licenseInfo.getBoundHardwareInfo().isEmpty()) {
                return true; // 没有绑定硬件信息时通过验证
            }
            
            // 获取当前硬件信息
            com.zhaoxinms.contract.tools.auth.core.service.AServerInfos serverInfos = 
                com.zhaoxinms.contract.tools.auth.core.service.AServerInfos.getServer(null);
            
            List<String> macAddresses = serverInfos.getMacAddress();
            String cpuSerial = serverInfos.getCPUSerial();
            String mainBoardSerial = serverInfos.getMainBoardSerial();
            List<String> boundHardware = licenseInfo.getBoundHardwareInfo();
            
            // 检查MAC地址
            if (macAddresses != null) {
                for (String mac : macAddresses) {
                    if (containsHardwareInfo(boundHardware, "macAddress", mac)) {
                        return true;
                    }
                }
            }
            
            // 检查CPU序列号
            if (cpuSerial != null && !cpuSerial.trim().isEmpty()) {
                if (containsHardwareInfo(boundHardware, "cpuSerial", cpuSerial)) {
                    return true;
                }
            }
            
            // 检查主板序列号
            if (mainBoardSerial != null && !mainBoardSerial.trim().isEmpty()) {
                if (containsHardwareInfo(boundHardware, "mainBoardSerial", mainBoardSerial)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查硬件信息列表中是否包含指定的硬件信息
     * 支持两种格式：
     * 1. 直接值：value
     * 2. 带前缀：prefix:value
     * 
     * @param boundInfo 授权文件中的硬件信息列表
     * @param prefix 硬件信息前缀（如 macAddress, cpuSerial, mainBoardSerial）
     * @param value 要匹配的硬件信息值
     * @return 是否匹配
     */
    private boolean containsHardwareInfo(List<String> boundInfo, String prefix, String value) {
        if (boundInfo == null || value == null) {
            return false;
        }
        
        // 构建带前缀的格式
        String withPrefix = prefix + ":" + value;
        
        // 检查是否包含（支持带前缀和不带前缀两种格式）
        return boundInfo.contains(value) || boundInfo.contains(withPrefix);
    }
}
