package com.zhaoxinms.contract.tools.auth;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.generator.LicenseGenerator;
import com.zhaoxinms.contract.tools.auth.generator.LicenseGenerator.KeyGenerateResult;
import com.zhaoxinms.contract.tools.auth.generator.LicenseGenerator.LicenseGenerateRequest;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader.LicenseReadResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * License生成示例
 * 这是一个完整的示例，演示如何生成密钥对和License文件
 */
public class LicenseGenerationExample {
    
    /**
     * 生成密钥对示例
     */
    @Test
    public void generateKeyPairExample() {
        LicenseGenerator generator = new LicenseGenerator();
        
        // 生成密钥对
        KeyGenerateResult result = generator.generateKeyPair("./keys");
        
        if (result != null) {
            System.out.println("=== 密钥对生成成功 ===");
            System.out.println("公钥文件: " + result.getPublicKeyPath());
            System.out.println("私钥文件: " + result.getPrivateKeyPath());
            System.out.println();
            System.out.println("=== 公钥内容（配置到应用程序中）===");
            System.out.println(result.getPublicKeyContent());
            System.out.println();
            System.out.println("=== 私钥内容（保密，仅用于生成License）===");
            System.out.println(result.getPrivateKeyContent());
        } else {
            System.out.println("密钥对生成失败");
        }
    }
    
    /**
     * 生成License文件示例
     */
    @Test
    public void generateLicenseExample() {
        LicenseGenerator generator = new LicenseGenerator();
        
        // 创建License生成请求
        LicenseGenerateRequest request = new LicenseGenerateRequest();
        
        // 基本信息
        request.setLicenseCode("LC2024001");
        request.setCompanyName("测试公司有限公司");
        request.setContactPerson("张三");
        request.setContactPhone("13800138000");
        
        // 授权模块
        Set<ModuleType> authorizedModules = Set.of(
            ModuleType.DOCUMENT_ONLINE_EDIT,        // 文档在线编辑
            ModuleType.SMART_CONTRACT_SYNTHESIS,    // 智能合同合成
            ModuleType.SMART_DOCUMENT_COMPARE       // 智能文档比对
        );
        request.setAuthorizedModules(authorizedModules);
        
        // 时间设置
        request.setStartDate(LocalDateTime.now());
        request.setExpireDate(LocalDateTime.now().plusYears(1)); // 1年后到期
        
        // 用户限制
        request.setMaxUsers(10);
        
        // 硬件绑定
        request.setHardwareBound(true);
        
        // 签名配置（使用之前生成的私钥）
        request.setPrivateKeyPath("./keys/private.key");
        
        // 输出文件
        request.setOutputPath("./license.lic");
        
        // 生成License
        boolean success = generator.generateLicense(request);
        
        if (success) {
            System.out.println("=== License生成成功 ===");
            System.out.println("文件路径: " + request.getOutputPath());
            System.out.println();
            System.out.println("=== License信息 ===");
            System.out.println("许可证编号: " + request.getLicenseCode());
            System.out.println("公司名称: " + request.getCompanyName());
            System.out.println("联系人: " + request.getContactPerson());
            System.out.println("联系电话: " + request.getContactPhone());
            System.out.println("授权模块: " + request.getAuthorizedModules().size() + " 个");
            System.out.println("最大用户数: " + request.getMaxUsers());
            System.out.println("开始时间: " + request.getStartDate());
            System.out.println("到期时间: " + request.getExpireDate());
            System.out.println("硬件绑定: " + (request.getHardwareBound() ? "是" : "否"));
            
            System.out.println();
            System.out.println("=== 应用程序配置示例 ===");
            System.out.println("在application.yml中添加以下配置:");
            System.out.println("zhaoxin:");
            System.out.println("  auth:");
            System.out.println("    enabled: true");
            System.out.println("    license:");
            System.out.println("      filePath: \"license.lic\"");
            System.out.println("      hardwareBound: true");
            System.out.println("    signature:");
            System.out.println("      publicKeyPath: \"public.key\"");
            
        } else {
            System.out.println("License生成失败");
        }
    }
    
    /**
     * 查看当前硬件信息示例
     */
    @Test
    public void showHardwareInfoExample() {
        LicenseGenerator generator = new LicenseGenerator();
        
        System.out.println("=== 当前服务器硬件信息 ===");
        try {
            java.util.List<String> hardwareInfo = generator.collectHardwareInfo();
            if (hardwareInfo.isEmpty()) {
                System.out.println("未能获取到硬件信息");
            } else {
                System.out.println("硬件信息列表:");
                for (int i = 0; i < hardwareInfo.size(); i++) {
                    System.out.println((i + 1) + ". " + hardwareInfo.get(i));
                }
                System.out.println();
                System.out.println("注意: 如果启用硬件绑定，License必须在包含上述硬件信息的服务器上使用");
            }
        } catch (Exception e) {
            System.out.println("获取硬件信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证License文件示例
     */
    @Test
    public void validateLicenseExample() {
        LicenseReader reader = new LicenseReader();
        
        System.out.println("=== 验证License文件示例 ===");
        
        // 验证License文件
        LicenseReadResult result = reader.readLicense("./license.lic", "./keys/public.key");
        
        // 显示详细信息
        reader.printLicenseInfo(result);
        
        // 编程式检查
        if (result.isSuccess()) {
            System.out.println();
            System.out.println("=== 编程式验证示例 ===");
            
            // 检查特定模块权限
            if (result.getLicenseInfo().getAuthorizedModules() != null) {
                for (ModuleType module : result.getLicenseInfo().getAuthorizedModules()) {
                    System.out.println("✓ 拥有 " + module.getName() + " 模块权限");
                }
            }
            
            // 检查License整体状态
            boolean isFullyValid = result.isSignatureValid() && result.isLicenseValid();
            System.out.println();
            System.out.println("License整体状态: " + (isFullyValid ? "✅ 完全有效" : "❌ 存在问题"));
            
            if (!result.isSignatureValid()) {
                System.out.println("⚠️  签名验证失败，License可能被篡改");
            }
            
            if (!result.isLicenseValid()) {
                System.out.println("⚠️  License已过期或时间无效");
            }
        }
    }
    
    /**
     * 完整的License生命周期示例
     */
    @Test
    public void completeLicenseLifecycleExample() {
        System.out.println("=== 完整License生命周期示例 ===");
        
        // 1. 生成密钥对
        System.out.println("步骤1: 生成密钥对");
        LicenseGenerator generator = new LicenseGenerator();
        KeyGenerateResult keyResult = generator.generateKeyPair("./example-keys");
        
        if (keyResult == null) {
            System.out.println("❌ 密钥对生成失败");
            return;
        }
        System.out.println("✅ 密钥对生成成功");
        
        // 2. 生成License
        System.out.println("\n步骤2: 生成License文件");
        LicenseGenerateRequest request = new LicenseGenerateRequest();
        request.setLicenseCode("DEMO-2024-001");
        request.setCompanyName("演示公司");
        request.setContactPerson("测试用户");
        request.setContactPhone("13800138000");
        request.setAuthorizedModules(Set.of(
            ModuleType.DOCUMENT_ONLINE_EDIT,
            ModuleType.SMART_DOCUMENT_COMPARE
        ));
        request.setStartDate(LocalDateTime.now());
        request.setExpireDate(LocalDateTime.now().plusMonths(3)); // 3个月后到期
        request.setMaxUsers(5);
        request.setHardwareBound(true);
        request.setPrivateKeyContent(keyResult.getPrivateKeyContent());
        request.setOutputPath("./example-license.lic");
        
        boolean generateSuccess = generator.generateLicense(request);
        if (!generateSuccess) {
            System.out.println("❌ License生成失败");
            return;
        }
        System.out.println("✅ License生成成功");
        
        // 3. 验证License
        System.out.println("\n步骤3: 验证License文件");
        LicenseReader reader = new LicenseReader();
        
        // 创建临时公钥文件
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("./example-public.key"), 
                keyResult.getPublicKeyContent().getBytes()
            );
            
            LicenseReadResult result = reader.readLicense("./example-license.lic", "./example-public.key");
            reader.printLicenseInfo(result);
            
            // 清理临时文件
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("./example-public.key"));
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("./example-license.lic"));
            
        } catch (Exception e) {
            System.out.println("❌ 验证过程失败: " + e.getMessage());
        }
        
        System.out.println("\n=== License生命周期演示完成 ===");
    }
}
