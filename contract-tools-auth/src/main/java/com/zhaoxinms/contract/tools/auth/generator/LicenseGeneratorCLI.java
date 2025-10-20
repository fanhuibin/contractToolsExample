package com.zhaoxinms.contract.tools.auth.generator;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.generator.LicenseGenerator.KeyGenerateResult;
import com.zhaoxinms.contract.tools.auth.generator.LicenseGenerator.LicenseGenerateRequest;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader;
import com.zhaoxinms.contract.tools.auth.generator.LicenseReader.LicenseReadResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * License生成器命令行工具
 */
public class LicenseGeneratorCLI {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        LicenseGenerator generator = new LicenseGenerator();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== 赵鑫合同工具套件 License 生成器 ===");
        System.out.println();
        
        while (true) {
            System.out.println("请选择操作:");
            System.out.println("1. 生成密钥对");
            System.out.println("2. 生成License文件");
            System.out.println("3. 验证并显示License详细信息");
            System.out.println("4. 查看硬件信息");
            System.out.println("5. 退出");
            System.out.print("请输入选项 (1-5): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    generateKeyPair(generator, scanner);
                    break;
                case "2":
                    generateLicense(generator, scanner);
                    break;
                case "3":
                    validateAndShowLicense(scanner);
                    break;
                case "4":
                    showHardwareInfo(generator);
                    break;
                case "5":
                    System.out.println("退出程序");
                    System.exit(0);
                    break;
                default:
                    System.out.println("无效选项，请重新输入");
            }
            
            System.out.println();
        }
    }
    
    /**
     * 生成密钥对
     */
    private static void generateKeyPair(LicenseGenerator generator, Scanner scanner) {
        System.out.println("\n=== 生成密钥对 ===");
        
        System.out.print("请输入密钥文件输出目录 (默认当前目录): ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = ".";
        }
        
        KeyGenerateResult result = generator.generateKeyPair(outputDir);
        if (result != null) {
            System.out.println("✓ 密钥对生成成功!");
            System.out.println("公钥文件: " + result.getPublicKeyPath());
            System.out.println("私钥文件: " + result.getPrivateKeyPath());
            System.out.println("\n公钥内容 (配置到应用程序中):");
            System.out.println(result.getPublicKeyContent());
        } else {
            System.out.println("✗ 密钥对生成失败");
        }
    }
    
    /**
     * 生成License
     */
    private static void generateLicense(LicenseGenerator generator, Scanner scanner) {
        System.out.println("\n=== 生成License ===");
        
        LicenseGenerateRequest request = new LicenseGenerateRequest();
        
        // 基本信息
        System.out.print("许可证编号: ");
        request.setLicenseCode(scanner.nextLine().trim());
        
        System.out.print("公司名称: ");
        request.setCompanyName(scanner.nextLine().trim());
        
        System.out.print("联系人: ");
        request.setContactPerson(scanner.nextLine().trim());
        
        System.out.print("联系电话: ");
        request.setContactPhone(scanner.nextLine().trim());
        
        // 模块授权
        System.out.println("\n可用模块:");
        ModuleType[] modules = ModuleType.values();
        for (int i = 0; i < modules.length; i++) {
            System.out.println((i + 1) + ". " + modules[i].getName() + " (" + modules[i].getCode() + ")");
        }
        System.out.print("请输入授权模块编号，多个用逗号分隔 (例: 1,3,5): ");
        String moduleInput = scanner.nextLine().trim();
        
        Set<ModuleType> authorizedModules = new HashSet<>();
        if (!moduleInput.isEmpty()) {
            String[] moduleNumbers = moduleInput.split(",");
            for (String number : moduleNumbers) {
                try {
                    int index = Integer.parseInt(number.trim()) - 1;
                    if (index >= 0 && index < modules.length) {
                        authorizedModules.add(modules[index]);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("无效的模块编号: " + number);
                }
            }
        }
        request.setAuthorizedModules(authorizedModules);
        
        // 用户数限制
        System.out.print("最大用户数 (默认为1): ");
        String maxUsersInput = scanner.nextLine().trim();
        int maxUsers = 1;
        if (!maxUsersInput.isEmpty()) {
            try {
                maxUsers = Integer.parseInt(maxUsersInput);
            } catch (NumberFormatException e) {
                System.out.println("无效的用户数，使用默认值1");
            }
        }
        request.setMaxUsers(maxUsers);
        
        // 时间设置
        request.setStartDate(LocalDateTime.now());
        
        System.out.print("到期时间 (格式: yyyy-MM-dd HH:mm:ss): ");
        String expireDateInput = scanner.nextLine().trim();
        try {
            LocalDateTime expireDate = LocalDateTime.parse(expireDateInput, DATE_FORMATTER);
            request.setExpireDate(expireDate);
        } catch (Exception e) {
            System.out.println("无效的时间格式，使用默认值(1年后)");
            request.setExpireDate(LocalDateTime.now().plusYears(1));
        }
        
        // 硬件绑定
        System.out.print("是否绑定硬件 (y/n, 默认n): ");
        String hardwareBoundInput = scanner.nextLine().trim().toLowerCase();
        boolean hardwareBound = "y".equals(hardwareBoundInput) || "yes".equals(hardwareBoundInput);
        request.setHardwareBound(hardwareBound);
        
        // 私钥设置
        System.out.print("私钥文件路径: ");
        request.setPrivateKeyPath(scanner.nextLine().trim());
        
        // 输出路径
        System.out.print("License文件输出路径 (默认 license.lic): ");
        String outputPath = scanner.nextLine().trim();
        if (outputPath.isEmpty()) {
            outputPath = "license.lic";
        }
        request.setOutputPath(outputPath);
        
        // 生成License
        boolean success = generator.generateLicense(request);
        if (success) {
            System.out.println("✓ License生成成功!");
            System.out.println("文件路径: " + outputPath);
            System.out.println("\n许可证信息:");
            System.out.println("编号: " + request.getLicenseCode());
            System.out.println("公司: " + request.getCompanyName());
            System.out.println("授权模块: " + request.getAuthorizedModules().size() + " 个");
            System.out.println("到期时间: " + request.getExpireDate().format(DATE_FORMATTER));
            System.out.println("硬件绑定: " + (hardwareBound ? "是" : "否"));
        } else {
            System.out.println("✗ License生成失败");
        }
    }
    
    /**
     * 验证并显示License详细信息
     */
    private static void validateAndShowLicense(Scanner scanner) {
        System.out.println("\n=== 验证并显示License详细信息 ===");
        
        System.out.print("请输入License文件路径 (默认: license.lic): ");
        String licenseFilePath = scanner.nextLine().trim();
        if (licenseFilePath.isEmpty()) {
            licenseFilePath = "license.lic";
        }
        
        System.out.print("请输入公钥文件路径 (默认: public.key, 留空跳过签名验证): ");
        String publicKeyPath = scanner.nextLine().trim();
        if (publicKeyPath.isEmpty()) {
            publicKeyPath = "public.key";
        }
        
        LicenseReader reader = new LicenseReader();
        LicenseReadResult result = reader.readLicense(licenseFilePath, publicKeyPath);
        
        System.out.println();
        reader.printLicenseInfo(result);
        
        // 如果License有效，提供更多操作选项
        if (result.isSuccess() && result.isSignatureValid() && result.isLicenseValid()) {
            System.out.println();
            System.out.println("=== 额外验证 ===");
            
            // 硬件匹配验证
            if (result.getLicenseInfo().getHardwareBound() != null && 
                result.getLicenseInfo().getHardwareBound()) {
                
                System.out.print("是否验证当前硬件匹配? (y/n): ");
                String checkHardware = scanner.nextLine().trim().toLowerCase();
                
                if ("y".equals(checkHardware) || "yes".equals(checkHardware)) {
                    try {
                        LicenseGenerator generator = new LicenseGenerator();
                        java.util.List<String> currentHardware = generator.collectHardwareInfo();
                        java.util.List<String> boundHardware = result.getLicenseInfo().getBoundHardwareInfo();
                        
                        boolean matched = false;
                        if (boundHardware != null && currentHardware != null) {
                            for (String hardware : currentHardware) {
                                if (boundHardware.contains(hardware)) {
                                    matched = true;
                                    break;
                                }
                            }
                        }
                        
                        System.out.println("硬件匹配验证: " + (matched ? "✅ 匹配" : "❌ 不匹配"));
                        
                        if (!matched) {
                            System.out.println("\n当前服务器硬件信息:");
                            for (int i = 0; i < currentHardware.size(); i++) {
                                System.out.println("  " + (i + 1) + ". " + currentHardware.get(i));
                            }
                            System.out.println("\nLicense绑定的硬件信息:");
                            for (int i = 0; i < boundHardware.size(); i++) {
                                System.out.println("  " + (i + 1) + ". " + boundHardware.get(i));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("硬件验证失败: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * 显示硬件信息
     */
    private static void showHardwareInfo(LicenseGenerator generator) {
        System.out.println("\n=== 当前服务器硬件信息 ===");
        
        try {
            java.util.List<String> hardwareInfo = generator.collectHardwareInfo();
            if (hardwareInfo.isEmpty()) {
                System.out.println("未能获取到硬件信息");
            } else {
                System.out.println("硬件信息列表:");
                for (int i = 0; i < hardwareInfo.size(); i++) {
                    System.out.println((i + 1) + ". " + hardwareInfo.get(i));
                }
            }
        } catch (Exception e) {
            System.out.println("获取硬件信息失败: " + e.getMessage());
        }
    }
}
