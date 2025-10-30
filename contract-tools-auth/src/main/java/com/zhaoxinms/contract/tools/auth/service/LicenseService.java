package com.zhaoxinms.contract.tools.auth.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

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
    
    @Autowired
    private ResourceLoader resourceLoader;
    
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
            Resource resource = loadResource(licenseFilePath);
            if (resource == null || !resource.exists()) {
                return LicenseValidationResult.failure("License文件不存在: " + licenseFilePath);
            }
            
            String licenseContent;
            try (InputStream inputStream = resource.getInputStream()) {
                licenseContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
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
     * 加载资源文件
     * 优先级：
     * 1. jar包同级目录 (./license.lic)
     * 2. jar包同级config目录 (./config/license.lic)
     * 3. classpath (classpath:license.lic)
     */
    private Resource loadResource(String path) {
        try {
            // 如果是classpath:前缀，先尝试外部文件
            if (path.startsWith("classpath:")) {
                String fileName = path.substring("classpath:".length());
                
                // 1. 尝试jar包同级目录
                Resource fileResource = resourceLoader.getResource("file:./" + fileName);
                if (fileResource.exists()) {
                    log.info("从jar包同级目录加载文件: ./{}", fileName);
                    return fileResource;
                }
                
                // 2. 尝试jar包同级config目录
                Resource configResource = resourceLoader.getResource("file:./config/" + fileName);
                if (configResource.exists()) {
                    log.info("从config目录加载文件: ./config/{}", fileName);
                    return configResource;
                }
                
                // 3. 最后使用classpath（开发环境）
                Resource classpathResource = resourceLoader.getResource(path);
                if (classpathResource.exists()) {
                    log.info("从classpath加载文件: {}", path);
                    return classpathResource;
                }
                
                log.warn("文件不存在: {} (已尝试: ./{}, ./config/{}, classpath)", fileName, fileName, fileName);
                return null;
            } else {
                // 非classpath路径，直接加载
                return resourceLoader.getResource(path);
            }
        } catch (Exception e) {
            log.error("加载资源文件失败: {}", path, e);
            return null;
        }
    }

    /**
     * 验证签名
     */
    private boolean verifySignature(String data, String signature) {
        try {
            String publicKeyStr = authProperties.getSignature().getPublicKey();
            if (CommonUtils.isEmpty(publicKeyStr)) {
                // 尝试从文件读取公钥（仅支持 classpath 加载以提高安全性）
                String publicKeyPath = authProperties.getSignature().getPublicKeyPath();
                
                // 安全检查：只允许从 classpath 加载公钥
                if (publicKeyPath == null || !publicKeyPath.startsWith("classpath:")) {
                    String errorMsg = "【致命错误】出于安全考虑，公钥只能从classpath加载（需要以classpath:开头）: " + publicKeyPath;
                    LoggerHelper.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                
                Resource publicKeyResource = loadPublicKeyFromClasspath(publicKeyPath);
                if (publicKeyResource == null || !publicKeyResource.exists()) {
                    String errorMsg = "【致命错误】公钥文件不存在: " + publicKeyPath + "，系统无法启动";
                    LoggerHelper.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                
                try (InputStream inputStream = publicKeyResource.getInputStream()) {
                    publicKeyStr = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                    log.info("从classpath加载公钥: {}", publicKeyPath);
                    
                    // 验证公钥指纹（从classpath加载）- 强制验证
                    String expectedFingerprint = loadPublicKeyFingerprint(publicKeyPath);
                    if (CommonUtils.isEmpty(expectedFingerprint)) {
                        String errorMsg = "【致命错误】公钥指纹文件不存在: " + publicKeyPath + ".fingerprint，系统无法启动";
                        LoggerHelper.error(errorMsg);
                        LoggerHelper.error("请确保以下文件存在：");
                        LoggerHelper.error("  1. " + publicKeyPath);
                        LoggerHelper.error("  2. " + publicKeyPath + ".fingerprint");
                        throw new RuntimeException(errorMsg);
                    }
                    
                    String currentFingerprint = calculateFingerprint(publicKeyStr);
                    if (!expectedFingerprint.equals(currentFingerprint)) {
                        String errorMsg = "【致命错误】公钥指纹验证失败！公钥文件已被篡改，系统拒绝启动";
                        LoggerHelper.error(errorMsg);
                        LoggerHelper.error("期望指纹: " + expectedFingerprint);
                        LoggerHelper.error("实际指纹: " + currentFingerprint);
                        LoggerHelper.error("公钥文件可能被非法修改，请立即检查系统安全性！");
                        throw new RuntimeException(errorMsg);
                    }
                    
                    log.info("✓ 公钥指纹验证通过");
                }
            }
            
            PublicKey publicKey = SignatureUtils.stringToPublicKey(publicKeyStr);
            return SignatureUtils.verify(data, signature, publicKey);
            
        } catch (RuntimeException e) {
            // 直接抛出RuntimeException，让系统启动失败
            throw e;
        } catch (Exception e) {
            LoggerHelper.error("验证签名时发生错误", e);
            throw new RuntimeException("【致命错误】公钥验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载公钥指纹
     * 指纹文件命名规则：公钥文件名 + .fingerprint
     * 例如：publicCerts.store -> publicCerts.store.fingerprint
     */
    private String loadPublicKeyFingerprint(String publicKeyPath) {
        try {
            String fingerprintPath = publicKeyPath + ".fingerprint";
            Resource fingerprintResource = resourceLoader.getResource(fingerprintPath);
            if (fingerprintResource != null && fingerprintResource.exists()) {
                try (InputStream inputStream = fingerprintResource.getInputStream()) {
                    String fingerprint = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8).trim();
                    log.info("从classpath加载公钥指纹: {}", fingerprintPath);
                    return fingerprint;
                }
            }
        } catch (Exception e) {
            log.warn("加载公钥指纹失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 计算公钥指纹（SHA-256哈希）
     */
    private String calculateFingerprint(String publicKeyStr) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKeyStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            LoggerHelper.error("计算公钥指纹失败", e);
            return "";
        }
    }
    
    /**
     * 从classpath加载公钥（安全加载，不允许外部文件覆盖）
     * 
     * @param classpathPath classpath路径
     * @return 公钥资源
     */
    private Resource loadPublicKeyFromClasspath(String classpathPath) {
        try {
            // 强制从 classpath 内部加载，不检查外部文件
            // 这样可以防止用户替换jar包外的公钥文件来绕过授权
            return resourceLoader.getResource(classpathPath);
        } catch (Exception e) {
            LoggerHelper.error("加载公钥失败: " + classpathPath, e);
            return null;
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
            
            List<String> boundInfo = licenseInfo.getBoundHardwareInfo();
            
            // 检查MAC地址
            if (currentMacAddresses != null) {
                for (String mac : currentMacAddresses) {
                    if (containsHardwareInfo(boundInfo, "macAddress", mac)) {
                        return true;
                    }
                }
            }
            
            // 检查CPU序列号
            if (CommonUtils.isNotEmpty(currentCpuSerial) && 
                containsHardwareInfo(boundInfo, "cpuSerial", currentCpuSerial)) {
                return true;
            }
            
            // 检查主板序列号
            if (CommonUtils.isNotEmpty(currentMainBoardSerial) && 
                containsHardwareInfo(boundInfo, "mainBoardSerial", currentMainBoardSerial)) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LoggerHelper.error("验证硬件绑定时发生错误", e);
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