package com.zhaoxinms.contract.tools.auth.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.core.utils.SignatureUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * License快捷生成器
 * 提供4种预设授权场景的快速生成方法
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Slf4j
public class LicenseQuickGenerator {
    
    private final ObjectMapper objectMapper;
    
    public LicenseQuickGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 预设场景1：授权智能文档解析和智能文档抽取
     * 适用于：文档处理场景（OCR + 规则提取）
     */
    public GenerateResult generateScenario1_ParseAndExtract(
            String companyName,
            String contactPerson,
            String contactPhone,
            String licenseType,
            int duration,
            boolean bindHardware,
            String hardwareInfoFile,
            java.util.List<String> hardwareInfoManual,
            String privateKeyPath,
            String outputPath) {
        
        Set<ModuleType> modules = new HashSet<>();
        modules.add(ModuleType.SMART_DOCUMENT_PARSE);      // 智能文档解析
        modules.add(ModuleType.SMART_DOCUMENT_EXTRACTION);  // 智能文档抽取
        
        // 获取硬件信息
        Set<String> hardwareInfo = null;
        if (bindHardware) {
            hardwareInfo = loadHardwareInfo(hardwareInfoFile, hardwareInfoManual);
        }
        
        return generateLicense(
            "PARSE_EXTRACT_" + System.currentTimeMillis(),
            companyName,
            contactPerson,
            contactPhone,
            modules,
            licenseType,
            duration,
            bindHardware,
            hardwareInfo,
            privateKeyPath,
            outputPath
        );
    }
    
    /**
     * 预设场景2：授权智能文档解析和智能文档比对
     * 适用于：文档比对场景（OCR + GPU比对）
     */
    public GenerateResult generateScenario2_ParseAndCompare(
            String companyName,
            String contactPerson,
            String contactPhone,
            String licenseType,
            int duration,
            boolean bindHardware,
            String hardwareInfoFile,
            java.util.List<String> hardwareInfoManual,
            String privateKeyPath,
            String outputPath) {
        
        Set<ModuleType> modules = new HashSet<>();
        modules.add(ModuleType.SMART_DOCUMENT_PARSE);    // 智能文档解析
        modules.add(ModuleType.SMART_DOCUMENT_COMPARE);  // 智能文档比对
        
        // 获取硬件信息
        Set<String> hardwareInfo = null;
        if (bindHardware) {
            hardwareInfo = loadHardwareInfo(hardwareInfoFile, hardwareInfoManual);
        }
        
        return generateLicense(
            "PARSE_COMPARE_" + System.currentTimeMillis(),
            companyName,
            contactPerson,
            contactPhone,
            modules,
            licenseType,
            duration,
            bindHardware,
            hardwareInfo,
            privateKeyPath,
            outputPath
        );
    }
    
    /**
     * 预设场景3：授权智能合同合成和文档在线编辑
     * 适用于：合同制作场景（合成 + OnlyOffice编辑）
     */
    public GenerateResult generateScenario3_ComposeAndEdit(
            String companyName,
            String contactPerson,
            String contactPhone,
            String licenseType,
            int duration,
            boolean bindHardware,
            String hardwareInfoFile,
            java.util.List<String> hardwareInfoManual,
            String privateKeyPath,
            String outputPath) {
        
        Set<ModuleType> modules = new HashSet<>();
        modules.add(ModuleType.SMART_CONTRACT_SYNTHESIS);  // 智能合同合成
        modules.add(ModuleType.DOCUMENT_ONLINE_EDIT);      // 文档在线编辑
        modules.add(ModuleType.DOCUMENT_FORMAT_CONVERT);   // 文档格式转换（编辑场景常用）
        
        // 获取硬件信息
        Set<String> hardwareInfo = null;
        if (bindHardware) {
            hardwareInfo = loadHardwareInfo(hardwareInfoFile, hardwareInfoManual);
        }
        
        return generateLicense(
            "COMPOSE_EDIT_" + System.currentTimeMillis(),
            companyName,
            contactPerson,
            contactPhone,
            modules,
            licenseType,
            duration,
            bindHardware,
            hardwareInfo,
            privateKeyPath,
            outputPath
        );
    }
    
    /**
     * 预设场景4：全功能授权
     * 适用于：完整版授权（所有6个模块）
     */
    public GenerateResult generateScenario4_FullFeatures(
            String companyName,
            String contactPerson,
            String contactPhone,
            String licenseType,
            int duration,
            boolean bindHardware,
            String hardwareInfoFile,
            java.util.List<String> hardwareInfoManual,
            String privateKeyPath,
            String outputPath) {
        
        Set<ModuleType> modules = new HashSet<>();
        modules.add(ModuleType.SMART_DOCUMENT_EXTRACTION);  // 智能文档抽取
        modules.add(ModuleType.SMART_DOCUMENT_COMPARE);     // 智能文档比对
        modules.add(ModuleType.SMART_CONTRACT_SYNTHESIS);   // 智能合同合成
        modules.add(ModuleType.SMART_DOCUMENT_PARSE);       // 智能文档解析
        modules.add(ModuleType.DOCUMENT_ONLINE_EDIT);       // 文档在线编辑
        modules.add(ModuleType.DOCUMENT_FORMAT_CONVERT);    // 文档格式转换
        
        // 获取硬件信息
        Set<String> hardwareInfo = null;
        if (bindHardware) {
            hardwareInfo = loadHardwareInfo(hardwareInfoFile, hardwareInfoManual);
        }
        
        return generateLicense(
            "FULL_" + System.currentTimeMillis(),
            companyName,
            contactPerson,
            contactPhone,
            modules,
            licenseType,
            duration,
            bindHardware,
            hardwareInfo,
            privateKeyPath,
            outputPath
        );
    }
    
    /**
     * 生成密钥对
     */
    public KeyPairResult generateKeyPair(String outputDir) {
        try {
            log.info("开始生成RSA密钥对...");
            
            KeyPair keyPair = SignatureUtils.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            
            String publicKeyStr = SignatureUtils.publicKeyToString(publicKey);
            String privateKeyStr = SignatureUtils.privateKeyToString(privateKey);
            
            // 计算公钥指纹（SHA-256）
            String fingerprint = calculatePublicKeyFingerprint(publicKeyStr);
            
            // 创建输出目录
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存公钥
            String publicKeyPath = outputDir + "/public.key";
            Files.write(Paths.get(publicKeyPath), publicKeyStr.getBytes());
            log.info("公钥已保存到: {}", publicKeyPath);
            
            // 保存公钥指纹
            String fingerprintPath = outputDir + "/public.key.fingerprint";
            Files.write(Paths.get(fingerprintPath), fingerprint.getBytes());
            log.info("公钥指纹已保存到: {}", fingerprintPath);
            log.info("公钥指纹值: {}", fingerprint);
            
            // 保存私钥
            String privateKeyPath = outputDir + "/private.key";
            Files.write(Paths.get(privateKeyPath), privateKeyStr.getBytes());
            log.info("私钥已保存到: {}", privateKeyPath);
            
            KeyPairResult result = new KeyPairResult();
            result.setSuccess(true);
            result.setPublicKeyPath(publicKeyPath);
            result.setPrivateKeyPath(privateKeyPath);
            result.setFingerprintPath(fingerprintPath);
            result.setPublicKey(publicKeyStr);
            result.setPrivateKey(privateKeyStr);
            result.setFingerprint(fingerprint);
            
            return result;
            
        } catch (Exception e) {
            log.error("生成密钥对失败", e);
            KeyPairResult result = new KeyPairResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
    
    /**
     * 计算公钥指纹（SHA-256哈希）
     */
    private String calculatePublicKeyFingerprint(String publicKeyStr) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKeyStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算公钥指纹失败", e);
            return "";
        }
    }
    
    /**
     * 核心License生成逻辑
     */
    private GenerateResult generateLicense(
            String licenseCode,
            String companyName,
            String contactPerson,
            String contactPhone,
            Set<ModuleType> modules,
            String licenseType,
            int duration,
            boolean bindHardware,
            Set<String> hardwareInfo,
            String privateKeyPath,
            String outputPath) {
        
        try {
            log.info("开始生成License...");
            log.info("授权码: {}", licenseCode);
            log.info("授权单位: {}", companyName);
            log.info("授权模块: {}", modules.size());
            log.info("授权类型: {}, 时长: {}", licenseType, duration);
            
            // 创建License信息
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setLicenseCode(licenseCode);
            licenseInfo.setCompanyName(companyName);
            licenseInfo.setContactPerson(contactPerson);
            licenseInfo.setContactPhone(contactPhone);
            licenseInfo.setAuthorizedModules(modules);
            licenseInfo.setMaxUsers(100); // 默认100用户
            
            LocalDateTime now = LocalDateTime.now();
            licenseInfo.setStartDate(now);
            
            // 根据授权类型计算到期日期
            LocalDateTime expireDate;
            switch (licenseType) {
                case "DAILY":
                    expireDate = now.plusDays(duration);
                    log.info("按天授权: {}天", duration);
                    break;
                case "YEARLY":
                    expireDate = now.plusYears(duration);
                    log.info("按年授权: {}年", duration);
                    break;
                case "PERPETUAL":
                    expireDate = now.plusYears(99); // 永久授权设置为99年后
                    log.info("永久授权");
                    break;
                default:
                    expireDate = now.plusYears(1); // 默认1年
                    log.warn("未知授权类型: {}, 使用默认1年授权", licenseType);
            }
            licenseInfo.setExpireDate(expireDate);
            licenseInfo.setCreateTime(now);
            
            licenseInfo.setHardwareBound(bindHardware);
            if (bindHardware && hardwareInfo != null) {
                licenseInfo.setBoundHardwareInfo(new java.util.ArrayList<>(hardwareInfo));
            }
            
            // 序列化License信息
            String licenseJson = objectMapper.writeValueAsString(licenseInfo);
            String licenseData = Base64.getEncoder().encodeToString(licenseJson.getBytes());
            
            // 读取私钥
            String privateKeyStr = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            PrivateKey privateKey = SignatureUtils.stringToPrivateKey(privateKeyStr);
            
            // 生成签名
            String signature = SignatureUtils.sign(licenseJson, privateKey);
            
            // 生成License文件内容（Base64编码的JSON + 签名）
            String licenseContent = licenseData + "." + signature;
            
            // 创建输出文件夹：公司名+年月日
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            // 清理公司名，移除不安全的文件名字符
            String safeCompanyName = companyName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String outputDir = "./licenses/" + safeCompanyName + "_" + timestamp;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
                log.info("创建授权文件夹: {}", outputDir);
            }
            
            // 保存License文件
            String licenseFilePath = outputDir + "/license.lic";
            Files.write(Paths.get(licenseFilePath), licenseContent.getBytes());
            log.info("License文件已生成: {}", licenseFilePath);
            
            // 生成授权详情文本文件
            String infoFilePath = outputDir + "/license-info.txt";
            String infoContent = generateLicenseInfoText(
                licenseCode, companyName, contactPerson, contactPhone,
                licenseInfo.getStartDate(), licenseInfo.getExpireDate(),
                modules, licenseType, duration, bindHardware
            );
            Files.write(Paths.get(infoFilePath), infoContent.getBytes("UTF-8"));
            log.info("授权详情已生成: {}", infoFilePath);
            
            GenerateResult result = new GenerateResult();
            result.setSuccess(true);
            result.setLicenseCode(licenseCode);
            result.setOutputPath(outputDir);
            result.setOutputDirectory(outputDir);
            result.setLicenseFilePath(licenseFilePath);
            result.setInfoFilePath(infoFilePath);
            result.setCompanyName(companyName);
            result.setStartDate(licenseInfo.getStartDate());
            result.setExpireDate(licenseInfo.getExpireDate());
            result.setAuthorizedModules(modules);
            
            return result;
            
        } catch (Exception e) {
            log.error("生成License失败", e);
            GenerateResult result = new GenerateResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
    
    /**
     * 加载硬件信息
     */
    private Set<String> loadHardwareInfo(String hardwareInfoFile, java.util.List<String> hardwareInfoManual) {
        Set<String> hardwareInfo = new HashSet<>();
        
        // 优先从文件加载
        if (hardwareInfoFile != null && !hardwareInfoFile.isEmpty()) {
            try {
                log.info("从文件加载硬件信息: {}", hardwareInfoFile);
                String json = new String(Files.readAllBytes(Paths.get(hardwareInfoFile)));
                
                // 解析JSON
                java.util.Map<String, Object> data = objectMapper.readValue(json, java.util.Map.class);
                
                // 提取硬件信息
                if (data.containsKey("mainBoardSerial")) {
                    hardwareInfo.add("mainBoardSerial:" + data.get("mainBoardSerial"));
                }
                if (data.containsKey("cpuSerial")) {
                    hardwareInfo.add("cpuSerial:" + data.get("cpuSerial"));
                }
                if (data.containsKey("macAddress")) {
                    Object macObj = data.get("macAddress");
                    if (macObj instanceof java.util.List) {
                        ((java.util.List<?>) macObj).forEach(mac -> {
                            if (mac != null && !mac.toString().isEmpty()) {
                                hardwareInfo.add("macAddress:" + mac);
                            }
                        });
                    }
                }
                
                log.info("从文件加载硬件信息成功: {} 条", hardwareInfo.size());
            } catch (Exception e) {
                log.error("加载硬件信息文件失败: {}", hardwareInfoFile, e);
                throw new RuntimeException("加载硬件信息失败: " + e.getMessage());
            }
        } 
        // 否则使用手动输入
        else if (hardwareInfoManual != null && !hardwareInfoManual.isEmpty()) {
            hardwareInfo.addAll(hardwareInfoManual);
            log.info("使用手动输入的硬件信息: {} 条", hardwareInfo.size());
        }
        
        return hardwareInfo.isEmpty() ? null : hardwareInfo;
    }
    
    /**
     * 生成授权详情文本
     */
    private String generateLicenseInfoText(
            String licenseCode,
            String companyName,
            String contactPerson,
            String contactPhone,
            LocalDateTime startDate,
            LocalDateTime expireDate,
            Set<ModuleType> modules,
            String licenseType,
            int duration,
            boolean bindHardware) {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║                                                              ║\n");
        sb.append("║              肇新合同管理系统 - 授权信息详情                 ║\n");
        sb.append("║                                                              ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n\n");
        
        sb.append("【基本信息】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("授权码      : ").append(licenseCode).append("\n");
        sb.append("授权单位    : ").append(companyName).append("\n");
        sb.append("联系人      : ").append(contactPerson).append("\n");
        sb.append("联系电话    : ").append(contactPhone).append("\n");
        sb.append("\n");
        
        sb.append("【授权期限】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("授权类型    : ");
        switch (licenseType) {
            case "DAILY":
                sb.append("按天授权（").append(duration).append(" 天）\n");
                break;
            case "YEARLY":
                sb.append("按年授权（").append(duration).append(" 年）\n");
                break;
            case "PERPETUAL":
                sb.append("永久授权\n");
                break;
            default:
                sb.append("未知\n");
        }
        sb.append("生效时间    : ").append(startDate.format(formatter)).append("\n");
        sb.append("到期时间    : ").append(expireDate.format(formatter)).append("\n");
        sb.append("硬件绑定    : ").append(bindHardware ? "是" : "否").append("\n");
        sb.append("\n");
        
        sb.append("【授权模块】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("授权模块数  : ").append(modules.size()).append(" 个\n\n");
        
        modules.stream()
            .sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
            .forEach(module -> {
                sb.append("  ✓ ").append(module.getName()).append("\n");
                sb.append("    代码: ").append(module.getCode()).append("\n\n");
            });
        
        sb.append("【部署说明】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("1. 将 license.lic 文件放置到以下位置之一：\n");
        sb.append("   - 开发环境：src/main/resources/license.lic\n");
        sb.append("   - 生产环境：JAR包同级目录 ./license.lic 或 ./config/license.lic\n");
        sb.append("2. 确保系统配置中启用了授权验证功能 (zhaoxin.auth.enabled=true)\n");
        sb.append("3. 重启系统使授权生效\n\n");
        
        sb.append("【注意事项】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("• 请妥善保管授权文件，切勿泄露\n");
        sb.append("• 授权文件仅限授权单位使用\n");
        if (bindHardware) {
            sb.append("• 本授权已绑定硬件，更换硬件后需重新授权\n");
        }
        sb.append("• 如需续期或变更授权，请联系服务商\n\n");
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("生成时间: ").append(LocalDateTime.now().format(formatter)).append("\n");
        sb.append("技术支持: zhaoxinms.com\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        return sb.toString();
    }
    
    /**
     * 生成结果
     */
    public static class GenerateResult {
        private boolean success;
        private String licenseCode;
        private String companyName;
        private LocalDateTime startDate;
        private LocalDateTime expireDate;
        private Set<ModuleType> authorizedModules;
        private String outputPath;              // 保留兼容性，指向文件夹
        private String outputDirectory;         // 文件夹路径
        private String licenseFilePath;         // 授权码文件路径
        private String infoFilePath;            // 授权详情文件路径
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getLicenseCode() { return licenseCode; }
        public void setLicenseCode(String licenseCode) { this.licenseCode = licenseCode; }
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getExpireDate() { return expireDate; }
        public void setExpireDate(LocalDateTime expireDate) { this.expireDate = expireDate; }
        
        public Set<ModuleType> getAuthorizedModules() { return authorizedModules; }
        public void setAuthorizedModules(Set<ModuleType> authorizedModules) { 
            this.authorizedModules = authorizedModules; 
        }
        
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        
        public String getOutputDirectory() { return outputDirectory; }
        public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }
        
        public String getLicenseFilePath() { return licenseFilePath; }
        public void setLicenseFilePath(String licenseFilePath) { this.licenseFilePath = licenseFilePath; }
        
        public String getInfoFilePath() { return infoFilePath; }
        public void setInfoFilePath(String infoFilePath) { this.infoFilePath = infoFilePath; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 密钥对生成结果
     */
    public static class KeyPairResult {
        private boolean success;
        private String publicKeyPath;
        private String privateKeyPath;
        private String fingerprintPath;
        private String publicKey;
        private String privateKey;
        private String fingerprint;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getPublicKeyPath() { return publicKeyPath; }
        public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }
        
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        
        public String getFingerprintPath() { return fingerprintPath; }
        public void setFingerprintPath(String fingerprintPath) { this.fingerprintPath = fingerprintPath; }
        
        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
        
        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}

