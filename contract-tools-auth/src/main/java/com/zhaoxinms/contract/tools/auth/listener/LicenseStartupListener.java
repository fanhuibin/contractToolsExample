package com.zhaoxinms.contract.tools.auth.listener;

import com.zhaoxinms.contract.tools.auth.config.AuthProperties;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;
import com.zhaoxinms.contract.tools.auth.model.LicenseValidationResult;
import com.zhaoxinms.contract.tools.auth.service.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 授权启动监听器
 * 在应用启动时强制验证公钥完整性，如果公钥有问题直接终止启动
 * 
 * @author zhaoxin
 * @since 2025-01-20
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LicenseStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private LicenseService licenseService;
    
    @Autowired
    private AuthProperties authProperties;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    /**
     * 在Bean初始化后立即验证公钥
     * 这个阶段比ApplicationReadyEvent更早，可以真正阻止应用启动
     */
    @PostConstruct
    public void validatePublicKeyOnStartup() {
        log.info("========================================");
        log.info("      公钥安全性验证（启动检查）");
        log.info("========================================");
        
        try {
            String publicKeyPath = authProperties.getSignature().getPublicKeyPath();
            
            // 验证公钥路径配置
            if (publicKeyPath == null || publicKeyPath.trim().isEmpty()) {
                String errorMsg = "【致命错误】公钥路径未配置（zhaoxin.auth.signature.public-key-path），系统无法启动";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            if (!publicKeyPath.startsWith("classpath:")) {
                String errorMsg = "【致命错误】出于安全考虑，公钥只能从classpath加载（当前配置: " + publicKeyPath + "），系统无法启动";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            // 验证公钥文件存在
            Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
            if (publicKeyResource == null || !publicKeyResource.exists()) {
                String errorMsg = "【致命错误】公钥文件不存在: " + publicKeyPath + "，系统无法启动";
                log.error(errorMsg);
                log.error("请确保公钥文件已正确部署到resources目录");
                throw new IllegalStateException(errorMsg);
            }
            
            log.info("✓ 公钥文件存在: {}", publicKeyPath);
            
            // 加载公钥内容
            String publicKeyContent;
            try (InputStream inputStream = publicKeyResource.getInputStream()) {
                publicKeyContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
            
            if (publicKeyContent == null || publicKeyContent.trim().isEmpty()) {
                String errorMsg = "【致命错误】公钥文件内容为空: " + publicKeyPath + "，系统无法启动";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            log.info("✓ 公钥文件内容读取成功（长度: {} 字符）", publicKeyContent.length());
            
            // 验证公钥指纹文件存在
            String fingerprintPath = publicKeyPath + ".fingerprint";
            Resource fingerprintResource = resourceLoader.getResource(fingerprintPath);
            if (fingerprintResource == null || !fingerprintResource.exists()) {
                String errorMsg = "【致命错误】公钥指纹文件不存在: " + fingerprintPath + "，系统无法启动";
                log.error(errorMsg);
                log.error("请确保以下文件都已部署：");
                log.error("  1. " + publicKeyPath);
                log.error("  2. " + fingerprintPath);
                throw new IllegalStateException(errorMsg);
            }
            
            log.info("✓ 公钥指纹文件存在: {}", fingerprintPath);
            
            // 加载并验证指纹
            String expectedFingerprint;
            try (InputStream inputStream = fingerprintResource.getInputStream()) {
                expectedFingerprint = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8).trim();
            }
            
            if (expectedFingerprint == null || expectedFingerprint.isEmpty()) {
                String errorMsg = "【致命错误】公钥指纹文件内容为空: " + fingerprintPath + "，系统无法启动";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            // 计算当前公钥的指纹
            String currentFingerprint = calculateFingerprint(publicKeyContent);
            
            // 验证指纹是否匹配
            if (!expectedFingerprint.equals(currentFingerprint)) {
                String errorMsg = "【致命错误】公钥指纹验证失败！公钥文件已被篡改，系统拒绝启动";
                log.error(errorMsg);
                log.error("期望指纹: {}", expectedFingerprint);
                log.error("实际指纹: {}", currentFingerprint);
                log.error("========================================");
                log.error("⚠️  安全警告：公钥文件可能被非法修改！");
                log.error("========================================");
                throw new IllegalStateException(errorMsg);
            }
            
            log.info("✓ 公钥指纹验证通过");
            log.info("  指纹值: {}", currentFingerprint.substring(0, 16) + "...");
            log.info("========================================");
            log.info("✅ 公钥安全性验证完成，系统继续启动");
            log.info("========================================");
            
        } catch (IllegalStateException e) {
            // 重新抛出，阻止应用启动
            throw e;
        } catch (Exception e) {
            String errorMsg = "【致命错误】公钥验证过程发生异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
    
    /**
     * 计算公钥指纹（SHA-256）
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
            log.error("计算公钥指纹失败", e);
            throw new IllegalStateException("计算公钥指纹失败: " + e.getMessage(), e);
        }
    }
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("========================================");
        log.info("      系统授权信息验证");
        log.info("========================================");
        
        try {
            // 验证授权
            LicenseValidationResult result = licenseService.validateLicense();
            
            if (result.isValid()) {
                LicenseInfo licenseInfo = result.getLicenseInfo();
                displayLicenseInfo(licenseInfo);
            } else {
                displayValidationFailure(result);
            }
            
        } catch (RuntimeException e) {
            // 公钥加载或指纹验证失败 - 致命错误，必须终止启动
            if (e.getMessage() != null && e.getMessage().contains("【致命错误】")) {
                log.error("========================================");
                log.error("  ❌ 系统启动失败");
                log.error("========================================");
                log.error("  {}", e.getMessage());
                log.error("========================================");
                log.error("");
                log.error("系统检测到严重的安全问题，拒绝启动！");
                log.error("请联系技术支持解决此问题。");
                log.error("");
                log.error("========================================");
                
                // 抛出异常，终止应用启动
                throw new RuntimeException("系统启动失败: " + e.getMessage(), e);
            }
            
            // 其他运行时异常
            log.error("授权验证失败", e);
            log.error("========================================");
            log.error("  ❌ 授权验证失败");
            log.error("  错误: {}", e.getMessage());
            log.error("========================================");
            
            // 也终止启动
            throw new RuntimeException("授权验证失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("授权验证失败", e);
            log.error("========================================");
            log.error("  ❌ 授权验证失败");
            log.error("  错误: {}", e.getMessage());
            log.error("========================================");
            
            // 终止启动
            throw new RuntimeException("授权验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示授权信息
     */
    private void displayLicenseInfo(LicenseInfo licenseInfo) {
        log.info("✅ 授权验证通过");
        log.info("----------------------------------------");
        log.info("  授权单位: {}", licenseInfo.getCompanyName());
        log.info("  授权码: {}", licenseInfo.getLicenseCode());
        log.info("  联系人: {}", licenseInfo.getContactPerson());
        
        if (licenseInfo.getContactPhone() != null && !licenseInfo.getContactPhone().isEmpty()) {
            log.info("  联系电话: {}", licenseInfo.getContactPhone());
        }
        
        log.info("  生效时间: {}", licenseInfo.getStartDate().format(DATE_FORMATTER));
        
        // 显示到期时间
        if (licenseInfo.getExpireDate() != null) {
            log.info("  到期时间: {}", licenseInfo.getExpireDate().format(DATE_FORMATTER));
            
            // 计算剩余天数
            long daysRemaining = java.time.Duration.between(
                java.time.LocalDateTime.now(), 
                licenseInfo.getExpireDate()
            ).toDays();
            
            if (daysRemaining > 0) {
                log.info("  剩余天数: {} 天", daysRemaining);
                
                // 即将到期提醒
                if (daysRemaining <= 30) {
                    log.warn("  ⚠️  授权即将到期，请及时续期！");
                }
            }
        } else {
            log.info("  授权类型: 永久授权");
        }
        
        // 显示硬件绑定状态
        if (licenseInfo.getHardwareBound() != null && licenseInfo.getHardwareBound()) {
            log.info("  硬件绑定: 已启用");
        }
        
        log.info("----------------------------------------");
        
        // 显示授权模块
        Set<ModuleType> authorizedModules = licenseInfo.getAuthorizedModules();
        if (authorizedModules != null && !authorizedModules.isEmpty()) {
            log.info("  授权模块 ({}个):", authorizedModules.size());
            for (ModuleType module : authorizedModules) {
                log.info("    ✓ {}", module.getName());
            }
        } else {
            log.warn("  ⚠️  未授权任何模块");
        }
        
        log.info("========================================");
    }
    
    /**
     * 显示验证失败信息
     */
    private void displayValidationFailure(LicenseValidationResult result) {
        log.error("========================================");
        log.error("  ❌ 授权验证失败");
        log.error("----------------------------------------");
        log.error("  失败原因: {}", result.getErrorMessage());
        log.error("========================================");
        log.error("");
        log.error("请检查授权文件配置：");
        log.error("  1. 开发环境：将授权文件放到 src/main/resources/ 目录");
        log.error("  2. 生产环境：将授权文件放到以下位置之一：");
        log.error("     - JAR包同级目录: ./license.lic");
        log.error("     - config目录: ./config/license.lic");
        log.error("");
        log.error("========================================");
    }
}

