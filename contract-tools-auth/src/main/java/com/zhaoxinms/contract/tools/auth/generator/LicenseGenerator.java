package com.zhaoxinms.contract.tools.auth.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;
import com.zhaoxinms.contract.tools.auth.core.service.AServerInfos;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;
import com.zhaoxinms.contract.tools.auth.core.utils.SignatureUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * License生成器
 * 用于生成和签名License文件
 */
public class LicenseGenerator {
    
    private final ObjectMapper objectMapper;
    
    public LicenseGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 生成License文件
     */
    public boolean generateLicense(LicenseGenerateRequest request) {
        try {
            // 创建License信息
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setLicenseCode(request.getLicenseCode());
            licenseInfo.setCompanyName(request.getCompanyName());
            licenseInfo.setContactPerson(request.getContactPerson());
            licenseInfo.setContactPhone(request.getContactPhone());
            licenseInfo.setAuthorizedModules(request.getAuthorizedModules());
            licenseInfo.setMaxUsers(request.getMaxUsers());
            licenseInfo.setStartDate(request.getStartDate());
            licenseInfo.setExpireDate(request.getExpireDate());
            licenseInfo.setHardwareBound(request.getHardwareBound());
            licenseInfo.setCreateTime(LocalDateTime.now());
            
            // 如果需要硬件绑定，获取当前硬件信息
            if (request.getHardwareBound() != null && request.getHardwareBound()) {
                List<String> hardwareInfo = collectHardwareInfo();
                licenseInfo.setBoundHardwareInfo(hardwareInfo);
            }
            
            // 序列化License信息
            String licenseData = objectMapper.writeValueAsString(licenseInfo);
            String encodedLicenseData = Base64.getEncoder().encodeToString(licenseData.getBytes());
            
            // 使用私钥签名
            PrivateKey privateKey = getPrivateKey(request.getPrivateKeyPath(), request.getPrivateKeyContent());
            String signature = SignatureUtils.sign(licenseData, privateKey);
            
            // 组合License内容
            String licenseContent = encodedLicenseData + "." + signature;
            
            // 写入License文件
            try (FileWriter writer = new FileWriter(request.getOutputPath())) {
                writer.write(licenseContent);
            }
            
            LoggerHelper.info("License文件生成成功: " + request.getOutputPath());
            return true;
            
        } catch (Exception e) {
            LoggerHelper.error("生成License文件失败", e);
            return false;
        }
    }
    
    /**
     * 生成RSA密钥对
     */
    public KeyGenerateResult generateKeyPair(String outputDir) {
        try {
            KeyPair keyPair = SignatureUtils.generateKeyPair();
            
            String publicKeyStr = SignatureUtils.publicKeyToString(keyPair.getPublic());
            String privateKeyStr = SignatureUtils.privateKeyToString(keyPair.getPrivate());
            
            // 保存公钥
            String publicKeyPath = outputDir + "/public.key";
            try (FileWriter writer = new FileWriter(publicKeyPath)) {
                writer.write(publicKeyStr);
            }
            
            // 保存私钥
            String privateKeyPath = outputDir + "/private.key";
            try (FileWriter writer = new FileWriter(privateKeyPath)) {
                writer.write(privateKeyStr);
            }
            
            KeyGenerateResult result = new KeyGenerateResult();
            result.setPublicKeyPath(publicKeyPath);
            result.setPrivateKeyPath(privateKeyPath);
            result.setPublicKeyContent(publicKeyStr);
            result.setPrivateKeyContent(privateKeyStr);
            
            LoggerHelper.info("密钥对生成成功，公钥: " + publicKeyPath + ", 私钥: " + privateKeyPath);
            return result;
            
        } catch (Exception e) {
            LoggerHelper.error("生成密钥对失败", e);
            return null;
        }
    }
    
    /**
     * 收集当前服务器硬件信息
     */
    public List<String> collectHardwareInfo() {
        List<String> hardwareInfo = new ArrayList<>();
        try {
            AServerInfos serverInfos = AServerInfos.getServer(null);
            
            // 收集MAC地址
            List<String> macAddresses = serverInfos.getMacAddress();
            if (macAddresses != null) {
                hardwareInfo.addAll(macAddresses);
            }
            
            // 收集CPU序列号
            String cpuSerial = serverInfos.getCPUSerial();
            if (CommonUtils.isNotEmpty(cpuSerial)) {
                hardwareInfo.add(cpuSerial);
            }
            
            // 收集主板序列号
            String mainBoardSerial = serverInfos.getMainBoardSerial();
            if (CommonUtils.isNotEmpty(mainBoardSerial)) {
                hardwareInfo.add(mainBoardSerial);
            }
            
        } catch (Exception e) {
            LoggerHelper.error("收集硬件信息失败", e);
        }
        return hardwareInfo;
    }
    
    /**
     * 获取私钥
     */
    private PrivateKey getPrivateKey(String privateKeyPath, String privateKeyContent) throws Exception {
        String keyContent = privateKeyContent;
        
        if (CommonUtils.isEmpty(keyContent) && CommonUtils.isNotEmpty(privateKeyPath)) {
            if (Files.exists(Paths.get(privateKeyPath))) {
                keyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            }
        }
        
        if (CommonUtils.isEmpty(keyContent)) {
            throw new IllegalArgumentException("私钥内容或路径必须提供");
        }
        
        return SignatureUtils.stringToPrivateKey(keyContent);
    }
    
    /**
     * License生成请求
     */
    public static class LicenseGenerateRequest {
        private String licenseCode;
        private String companyName;
        private String contactPerson;
        private String contactPhone;
        private Set<ModuleType> authorizedModules;
        private Integer maxUsers;
        private LocalDateTime startDate;
        private LocalDateTime expireDate;
        private Boolean hardwareBound;
        private String privateKeyPath;
        private String privateKeyContent;
        private String outputPath;
        
        // Getters and Setters
        public String getLicenseCode() { return licenseCode; }
        public void setLicenseCode(String licenseCode) { this.licenseCode = licenseCode; }
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        
        public String getContactPerson() { return contactPerson; }
        public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
        
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        
        public Set<ModuleType> getAuthorizedModules() { return authorizedModules; }
        public void setAuthorizedModules(Set<ModuleType> authorizedModules) { this.authorizedModules = authorizedModules; }
        
        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getExpireDate() { return expireDate; }
        public void setExpireDate(LocalDateTime expireDate) { this.expireDate = expireDate; }
        
        public Boolean getHardwareBound() { return hardwareBound; }
        public void setHardwareBound(Boolean hardwareBound) { this.hardwareBound = hardwareBound; }
        
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        
        public String getPrivateKeyContent() { return privateKeyContent; }
        public void setPrivateKeyContent(String privateKeyContent) { this.privateKeyContent = privateKeyContent; }
        
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    }
    
    /**
     * 密钥生成结果
     */
    public static class KeyGenerateResult {
        private String publicKeyPath;
        private String privateKeyPath;
        private String publicKeyContent;
        private String privateKeyContent;
        
        // Getters and Setters
        public String getPublicKeyPath() { return publicKeyPath; }
        public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }
        
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        
        public String getPublicKeyContent() { return publicKeyContent; }
        public void setPublicKeyContent(String publicKeyContent) { this.publicKeyContent = publicKeyContent; }
        
        public String getPrivateKeyContent() { return privateKeyContent; }
        public void setPrivateKeyContent(String privateKeyContent) { this.privateKeyContent = privateKeyContent; }
    }
}
