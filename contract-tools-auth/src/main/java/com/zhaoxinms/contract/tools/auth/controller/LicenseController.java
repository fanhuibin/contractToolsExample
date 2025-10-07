package com.zhaoxinms.contract.tools.auth.controller;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;
import com.zhaoxinms.contract.tools.auth.model.LicenseValidationResult;
import com.zhaoxinms.contract.tools.auth.service.LicenseService;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader.LicenseReadResult;
import com.zhaoxinms.contract.tools.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    
    @Autowired
    private AuthProperties authProperties;

    /**
     * 获取授权信息
     */
    @GetMapping("/license-info")
    public ResponseEntity<Map<String, Object>> getLicenseInfo() {
        try {
            LicenseInfo info = licenseService.getLicenseInfo();
            Map<String, Object> response = new HashMap<>();
            if (info != null) {
                response.put("success", true);
                response.put("data", info);
            } else {
                response.put("success", false);
                response.put("message", "未找到有效的授权信息");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取授权信息失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 检查指定模块的授权状态
     */
    @GetMapping("/check-module")
    public ResponseEntity<Map<String, Object>> checkModule(@RequestParam String moduleCode) {
        try {
            ModuleType moduleType = ModuleType.fromCode(moduleCode);
            Map<String, Object> response = new HashMap<>();
            
            if (moduleType == null) {
                response.put("success", false);
                response.put("message", "无效的模块代码: " + moduleCode);
                return ResponseEntity.ok(response);
            }
            
            boolean hasPermission = licenseService.hasModulePermission(moduleType);
            response.put("success", true);
            response.put("data", hasPermission);
            response.put("moduleName", moduleType.getName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查模块授权失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 检查指定功能的授权状态（兼容旧接口）
     */
    @GetMapping("/check-feature")
    public ResponseEntity<Map<String, Object>> checkFeature(@RequestParam String feature) {
        try {
            boolean hasFeature = licenseService.hasFeature(feature);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hasFeature);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查功能授权失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 验证授权
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateLicense() {
        try {
            LicenseValidationResult result = licenseService.validateLicense();
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isValid());
            
            if (result.isValid()) {
                response.put("data", result.getLicenseInfo());
            } else {
                response.put("message", result.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "授权验证失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取所有可用模块
     */
    @GetMapping("/modules")
    public ResponseEntity<Map<String, Object>> getAvailableModules() {
        try {
            ModuleType[] modules = ModuleType.values();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", modules);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模块列表失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 批量检查模块权限
     */
    @PostMapping("/check-modules")
    public ResponseEntity<Map<String, Object>> checkModules(@RequestBody String[] moduleCodes) {
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", modulePermissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量检查模块权限失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取License文件详细信息（包含签名验证和硬件匹配验证）
     */
    @GetMapping("/license-details")
    public ResponseEntity<Map<String, Object>> getLicenseDetails() {
        try {
            LicenseReader reader = new LicenseReader();
            String licenseFilePath = authProperties.getLicense().getFilePath();
            String publicKeyPath = authProperties.getSignature().getPublicKeyPath();
            
            LicenseReadResult result = reader.readLicense(licenseFilePath, publicKeyPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            
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
                response.put("data", details);
            } else {
                response.put("message", result.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取License详细信息失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 验证硬件匹配
     */
    @GetMapping("/hardware-validation")
    public ResponseEntity<Map<String, Object>> validateHardware() {
        try {
            LicenseInfo licenseInfo = licenseService.getLicenseInfo();
            Map<String, Object> response = new HashMap<>();
            
            if (licenseInfo == null) {
                response.put("success", false);
                response.put("message", "无法获取License信息");
                return ResponseEntity.ok(response);
            }
            
            if (licenseInfo.getHardwareBound() == null || !licenseInfo.getHardwareBound()) {
                response.put("success", true);
                response.put("message", "License未启用硬件绑定");
                response.put("data", Map.of(
                    "hardwareBound", false,
                    "matched", true
                ));
                return ResponseEntity.ok(response);
            }
            
            // 获取当前硬件信息
            com.zhaoxinms.contract.tools.auth.core.service.AServerInfos serverInfos = 
                com.zhaoxinms.contract.tools.auth.core.service.AServerInfos.getServer(null);
            java.util.List<String> currentHardware = new java.util.ArrayList<>();
            
            // 收集MAC地址
            java.util.List<String> macAddresses = serverInfos.getMacAddress();
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
            java.util.List<String> boundHardware = licenseInfo.getBoundHardwareInfo();
            if (boundHardware != null && !boundHardware.isEmpty()) {
                for (String hardware : currentHardware) {
                    if (boundHardware.contains(hardware)) {
                        matched = true;
                        break;
                    }
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("hardwareBound", true);
            data.put("matched", matched);
            data.put("currentHardware", currentHardware);
            data.put("boundHardware", boundHardware);
            
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "硬件验证失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}