package com.zhaoxinms.contract.tools.auth.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;
import com.zhaoxinms.contract.tools.auth.core.utils.SignatureUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * License文件读取和解析工具
 */
public class LicenseReader {
    
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public LicenseReader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 读取并解析License文件
     */
    public LicenseReadResult readLicense(String licenseFilePath, String publicKeyPath) {
        try {
            // 检查License文件是否存在
            if (!Files.exists(Paths.get(licenseFilePath))) {
                return LicenseReadResult.failure("License文件不存在: " + licenseFilePath);
            }
            
            // 读取License文件内容
            String licenseContent = new String(Files.readAllBytes(Paths.get(licenseFilePath)));
            if (CommonUtils.isEmpty(licenseContent)) {
                return LicenseReadResult.failure("License文件内容为空");
            }
            
            // 解析License内容
            String[] parts = licenseContent.split("\\.");
            if (parts.length != 2) {
                return LicenseReadResult.failure("License文件格式错误，应该包含数据部分和签名部分");
            }
            
            String licenseData = new String(Base64.getDecoder().decode(parts[0]));
            String signature = parts[1];
            
            // 反序列化License信息
            LicenseInfo licenseInfo = objectMapper.readValue(licenseData, LicenseInfo.class);
            
            // 验证签名（如果提供了公钥）
            boolean signatureValid = false;
            if (CommonUtils.isNotEmpty(publicKeyPath) && Files.exists(Paths.get(publicKeyPath))) {
                try {
                    String publicKeyContent = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
                    PublicKey publicKey = SignatureUtils.stringToPublicKey(publicKeyContent);
                    signatureValid = SignatureUtils.verify(licenseData, signature, publicKey);
                } catch (Exception e) {
                    LoggerHelper.error("验证签名时发生错误", e);
                }
            }
            
            // 检查License是否有效
            boolean isValid = licenseInfo.isValid();
            
            return LicenseReadResult.success(licenseInfo, signatureValid, isValid);
            
        } catch (Exception e) {
            LoggerHelper.error("读取License文件失败", e);
            return LicenseReadResult.failure("读取License文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化显示License信息
     */
    public void printLicenseInfo(LicenseReadResult result) {
        if (!result.isSuccess()) {
            System.out.println("❌ " + result.getErrorMessage());
            return;
        }
        
        LicenseInfo info = result.getLicenseInfo();
        
        System.out.println("=== License文件详细信息 ===");
        System.out.println();
        
        // 基本信息
        System.out.println("📋 基本信息:");
        System.out.println("  许可证编号: " + info.getLicenseCode());
        System.out.println("  公司名称: " + info.getCompanyName());
        System.out.println("  联系人: " + info.getContactPerson());
        System.out.println("  联系电话: " + info.getContactPhone());
        System.out.println("  创建时间: " + formatDateTime(info.getCreateTime()));
        System.out.println();
        
        // 授权信息
        System.out.println("🔐 授权信息:");
        System.out.println("  生效时间: " + formatDateTime(info.getStartDate()));
        System.out.println("  到期时间: " + formatDateTime(info.getExpireDate()));
        System.out.println("  最大用户数: " + info.getMaxUsers());
        System.out.println("  硬件绑定: " + (info.getHardwareBound() != null && info.getHardwareBound() ? "是" : "否"));
        System.out.println();
        
        // 授权模块
        System.out.println("📦 授权模块:");
        if (info.getAuthorizedModules() != null && !info.getAuthorizedModules().isEmpty()) {
            for (ModuleType module : info.getAuthorizedModules()) {
                System.out.println("  ✓ " + module.getName() + " (" + module.getCode() + ")");
            }
        } else {
            System.out.println("  ❌ 无授权模块");
        }
        System.out.println();
        
        // 硬件绑定信息
        if (info.getHardwareBound() != null && info.getHardwareBound() && 
            info.getBoundHardwareInfo() != null && !info.getBoundHardwareInfo().isEmpty()) {
            System.out.println("💻 绑定硬件信息:");
            for (int i = 0; i < info.getBoundHardwareInfo().size(); i++) {
                System.out.println("  " + (i + 1) + ". " + info.getBoundHardwareInfo().get(i));
            }
            System.out.println();
        }
        
        // 验证状态
        System.out.println("✅ 验证状态:");
        System.out.println("  签名验证: " + (result.isSignatureValid() ? "✓ 通过" : "❌ 失败"));
        System.out.println("  时间有效性: " + (result.isLicenseValid() ? "✓ 有效" : "❌ 已过期"));
        
        LocalDateTime now = LocalDateTime.now();
        if (info.getExpireDate() != null) {
            long daysUntilExpiry = java.time.Duration.between(now, info.getExpireDate()).toDays();
            if (daysUntilExpiry > 0) {
                System.out.println("  剩余天数: " + daysUntilExpiry + " 天");
            } else if (daysUntilExpiry == 0) {
                System.out.println("  ⚠️  今天到期");
            } else {
                System.out.println("  ❌ 已过期 " + Math.abs(daysUntilExpiry) + " 天");
            }
        }
        
        System.out.println("  整体状态: " + (result.isSignatureValid() && result.isLicenseValid() ? 
            "✅ License有效" : "❌ License无效"));
    }
    
    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "未设置";
    }
    
    /**
     * License读取结果
     */
    public static class LicenseReadResult {
        private boolean success;
        private String errorMessage;
        private LicenseInfo licenseInfo;
        private boolean signatureValid;
        private boolean licenseValid;
        
        public static LicenseReadResult success(LicenseInfo licenseInfo, boolean signatureValid, boolean licenseValid) {
            LicenseReadResult result = new LicenseReadResult();
            result.setSuccess(true);
            result.setLicenseInfo(licenseInfo);
            result.setSignatureValid(signatureValid);
            result.setLicenseValid(licenseValid);
            return result;
        }
        
        public static LicenseReadResult failure(String errorMessage) {
            LicenseReadResult result = new LicenseReadResult();
            result.setSuccess(false);
            result.setErrorMessage(errorMessage);
            return result;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LicenseInfo getLicenseInfo() { return licenseInfo; }
        public void setLicenseInfo(LicenseInfo licenseInfo) { this.licenseInfo = licenseInfo; }
        
        public boolean isSignatureValid() { return signatureValid; }
        public void setSignatureValid(boolean signatureValid) { this.signatureValid = signatureValid; }
        
        public boolean isLicenseValid() { return licenseValid; }
        public void setLicenseValid(boolean licenseValid) { this.licenseValid = licenseValid; }
    }
}
