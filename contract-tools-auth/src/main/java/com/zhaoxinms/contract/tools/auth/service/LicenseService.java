package com.zhaoxinms.contract.tools.auth.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.config.AuthProperties;
import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;
import com.zhaoxinms.contract.tools.auth.core.service.AServerInfos;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;
import com.zhaoxinms.contract.tools.auth.core.utils.SignatureUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;
import com.zhaoxinms.contract.tools.auth.model.LicenseValidationResult;

import lombok.extern.slf4j.Slf4j;

/**
 * License服务类 - 提供完整的License验证和管理功能
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseService {

    @Autowired
    private AuthProperties authProperties;
    
    private final ObjectMapper objectMapper;
    
    public LicenseService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 验证License
     */
    public LicenseValidationResult validateLicense() {
        try {
            // 读取License文件
            String licenseFilePath = authProperties.getLicense().getFilePath();
            if (!Files.exists(Paths.get(licenseFilePath))) {
                return LicenseValidationResult.failure("License文件不存在: " + licenseFilePath);
            }
            
            String licenseContent = new String(Files.readAllBytes(Paths.get(licenseFilePath)));
            if (CommonUtils.isEmpty(licenseContent)) {
                return LicenseValidationResult.failure("License文件内容为空");
            }
            
            // 解析License内容
            String[] parts = licenseContent.split("\\.");
            if (parts.length != 2) {
                return LicenseValidationResult.failure("License文件格式错误");
            }
            
            String licenseData = new String(Base64.getDecoder().decode(parts[0]));
            String signature = parts[1];
            
            // 反序列化License信息
            LicenseInfo licenseInfo = objectMapper.readValue(licenseData, LicenseInfo.class);
            
            // 验证签名
            if (!verifySignature(licenseData, signature)) {
                return LicenseValidationResult.failure("License签名验证失败");
            }
            
            // 验证License有效性
            if (!licenseInfo.isValid()) {
                return LicenseValidationResult.failure("License已过期");
            }
            
            // 验证硬件绑定
            if (licenseInfo.getHardwareBound() != null && licenseInfo.getHardwareBound()) {
                if (!validateHardwareBinding(licenseInfo)) {
                    return LicenseValidationResult.failure("硬件信息不匹配");
                }
            }
            
            return LicenseValidationResult.success(licenseInfo);
            
        } catch (Exception e) {
            LoggerHelper.error("验证License失败", e);
            return LicenseValidationResult.failure("License验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查模块权限
     */
    public boolean hasModulePermission(ModuleType moduleType) {
        if (!authProperties.isEnabled()) {
            return true; // 授权模块未启用时，允许所有功能
        }
        
        LicenseValidationResult result = validateLicense();
        if (!result.isValid()) {
            LoggerHelper.warn("License验证失败: " + result.getErrorMessage());
            return false;
        }
        
        return result.getLicenseInfo().hasModulePermission(moduleType);
    }

    /**
     * 检查功能权限（兼容旧接口）
     */
    public boolean hasFeature(String feature) {
        ModuleType moduleType = ModuleType.fromCode(feature);
        if (moduleType != null) {
            return hasModulePermission(moduleType);
        }
        
        // 如果不是预定义的模块，则检查License中的自定义功能
        LicenseValidationResult result = validateLicense();
        if (!result.isValid()) {
            return false;
        }
        
        // 这里可以添加自定义功能检查逻辑
        return false;
    }

    /**
     * 获取License信息
     */
    public LicenseInfo getLicenseInfo() {
        if (!authProperties.isEnabled()) {
            // 返回无限制的License信息
            LicenseInfo unlimited = new LicenseInfo();
            unlimited.setLicenseCode("UNLIMITED");
            unlimited.setStartDate(LocalDateTime.now().minusYears(1));
            unlimited.setExpireDate(LocalDateTime.now().plusYears(100));
            return unlimited;
        }
        
        LicenseValidationResult result = validateLicense();
        return result.isValid() ? result.getLicenseInfo() : null;
    }

    /**
     * 验证License是否有效
     */
    public boolean isLicenseValid() {
        if (!authProperties.isEnabled()) {
            return true;
        }
        
        return validateLicense().isValid();
    }

    /**
     * 验证签名
     */
    private boolean verifySignature(String data, String signature) {
        try {
            String publicKeyStr = authProperties.getSignature().getPublicKey();
            if (CommonUtils.isEmpty(publicKeyStr)) {
                // 尝试从文件读取公钥
                String publicKeyPath = authProperties.getSignature().getPublicKeyPath();
                if (Files.exists(Paths.get(publicKeyPath))) {
                    publicKeyStr = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
                } else {
                    LoggerHelper.error("公钥未配置且公钥文件不存在");
                    return false;
                }
            }
            
            PublicKey publicKey = SignatureUtils.stringToPublicKey(publicKeyStr);
            return SignatureUtils.verify(data, signature, publicKey);
            
        } catch (Exception e) {
            LoggerHelper.error("验证签名时发生错误", e);
            return false;
        }
    }

    /**
     * 验证硬件绑定
     */
    private boolean validateHardwareBinding(LicenseInfo licenseInfo) {
        try {
            if (licenseInfo.getBoundHardwareInfo() == null || licenseInfo.getBoundHardwareInfo().isEmpty()) {
                return true; // 没有绑定硬件信息时通过验证
            }
            
            // 获取当前硬件信息
            AServerInfos serverInfos = AServerInfos.getServer(null);
            List<String> currentMacAddresses = serverInfos.getMacAddress();
            String currentCpuSerial = serverInfos.getCPUSerial();
            String currentMainBoardSerial = serverInfos.getMainBoardSerial();
            
            // 检查MAC地址
            if (currentMacAddresses != null) {
                for (String mac : currentMacAddresses) {
                    if (licenseInfo.getBoundHardwareInfo().contains(mac)) {
                        return true;
                    }
                }
            }
            
            // 检查CPU序列号
            if (CommonUtils.isNotEmpty(currentCpuSerial) && 
                licenseInfo.getBoundHardwareInfo().contains(currentCpuSerial)) {
                return true;
            }
            
            // 检查主板序列号
            if (CommonUtils.isNotEmpty(currentMainBoardSerial) && 
                licenseInfo.getBoundHardwareInfo().contains(currentMainBoardSerial)) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LoggerHelper.error("验证硬件绑定时发生错误", e);
            return false;
        }
    }
}