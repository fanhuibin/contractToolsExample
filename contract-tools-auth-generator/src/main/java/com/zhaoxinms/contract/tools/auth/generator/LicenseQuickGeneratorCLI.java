package com.zhaoxinms.contract.tools.auth.generator;

import java.util.Scanner;

/**
 * License快捷生成器 - 命令行工具
 * 
 * 使用方法：
 * java -jar contract-tools-auth-generator-1.0.0-jar-with-dependencies.jar
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
public class LicenseQuickGeneratorCLI {
    
    private static final String BANNER = 
        "\n" +
        "╔══════════════════════════════════════════════════════════════╗\n" +
        "║                                                              ║\n" +
        "║        肇新合同管理系统 - License快捷生成工具                ║\n" +
        "║                                                              ║\n" +
        "║        版本: 1.0.0                                           ║\n" +
        "║        作者: zhaoxinms.com                                   ║\n" +
        "║                                                              ║\n" +
        "╚══════════════════════════════════════════════════════════════╝\n";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        LicenseQuickGenerator generator = new LicenseQuickGenerator();
        
        System.out.println(BANNER);
        
        try {
            // 主菜单
            while (true) {
                System.out.println("\n请选择操作：");
                System.out.println("1. 场景1：授权【智能文档解析 + 智能文档抽取】");
                System.out.println("2. 场景2：授权【智能文档解析 + 智能文档比对】");
                System.out.println("3. 场景3：授权【智能合同合成 + 文档在线编辑】");
                System.out.println("4. 场景4：授权【全部功能】");
                System.out.println("9. 生成RSA密钥对");
                System.out.println("0. 退出");
                System.out.print("\n请输入选项: ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        generateScenario1(scanner, generator);
                        break;
                    case "2":
                        generateScenario2(scanner, generator);
                        break;
                    case "3":
                        generateScenario3(scanner, generator);
                        break;
                    case "4":
                        generateScenario4(scanner, generator);
                        break;
                    case "9":
                        generateKeyPair(scanner, generator);
                        break;
                    case "0":
                        System.out.println("\n感谢使用，再见！");
                        return;
                    default:
                        System.out.println("\n❌ 无效选项，请重新选择");
                }
            }
        } finally {
            scanner.close();
        }
    }
    
    /**
     * 生成密钥对
     */
    private static void generateKeyPair(Scanner scanner, LicenseQuickGenerator generator) {
        System.out.println("\n╔════════════════ 生成RSA密钥对 ════════════════╗");
        
        System.out.print("请输入密钥保存目录 [默认: ./keys]: ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = "./keys";
        }
        
        System.out.println("\n正在生成密钥对...");
        LicenseQuickGenerator.KeyPairResult result = generator.generateKeyPair(outputDir);
        
        if (result.isSuccess()) {
            System.out.println("\n✅ 密钥对生成成功！");
            System.out.println("   公钥路径: " + result.getPublicKeyPath());
            System.out.println("   私钥路径: " + result.getPrivateKeyPath());
            System.out.println("\n⚠️  请妥善保管私钥文件，切勿泄露！");
            System.out.println("   公钥用于系统验证，可以公开");
            System.out.println("   私钥用于生成授权，必须保密");
        } else {
            System.out.println("\n❌ 生成失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 场景1：智能文档解析 + 智能文档抽取
     */
    private static void generateScenario1(Scanner scanner, LicenseQuickGenerator generator) {
        System.out.println("\n╔════════════════ 场景1：文档处理授权 ════════════════╗");
        System.out.println("授权模块：");
        System.out.println("  ✓ 智能文档解析（OCR识别）");
        System.out.println("  ✓ 智能文档抽取（规则提取）");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        
        LicenseInfo info = collectLicenseInfo(scanner);
        if (info == null) return;
        
        System.out.println("\n正在生成License文件...");
        LicenseQuickGenerator.GenerateResult result = generator.generateScenario1_ParseAndExtract(
            info.companyName,
            info.contactPerson,
            info.contactPhone,
            info.licenseType.name(),
            info.duration,
            info.bindHardware,
            info.hardwareInfoFile,
            info.hardwareInfoManual,
            info.privateKeyPath,
            info.outputPath
        );
        
        printResult(result);
    }
    
    /**
     * 场景2：智能文档解析 + 智能文档比对
     */
    private static void generateScenario2(Scanner scanner, LicenseQuickGenerator generator) {
        System.out.println("\n╔════════════════ 场景2：文档比对授权 ════════════════╗");
        System.out.println("授权模块：");
        System.out.println("  ✓ 智能文档解析（OCR识别）");
        System.out.println("  ✓ 智能文档比对（GPU比对）");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        
        LicenseInfo info = collectLicenseInfo(scanner);
        if (info == null) return;
        
        System.out.println("\n正在生成License文件...");
        LicenseQuickGenerator.GenerateResult result = generator.generateScenario2_ParseAndCompare(
            info.companyName,
            info.contactPerson,
            info.contactPhone,
            info.licenseType.name(),
            info.duration,
            info.bindHardware,
            info.hardwareInfoFile,
            info.hardwareInfoManual,
            info.privateKeyPath,
            info.outputPath
        );
        
        printResult(result);
    }
    
    /**
     * 场景3：智能合同合成 + 文档在线编辑
     */
    private static void generateScenario3(Scanner scanner, LicenseQuickGenerator generator) {
        System.out.println("\n╔════════════════ 场景3：合同制作授权 ════════════════╗");
        System.out.println("授权模块：");
        System.out.println("  ✓ 智能合同合成（模板合成）");
        System.out.println("  ✓ 文档在线编辑（OnlyOffice）");
        System.out.println("  ✓ 文档格式转换（格式转换）");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        
        LicenseInfo info = collectLicenseInfo(scanner);
        if (info == null) return;
        
        System.out.println("\n正在生成License文件...");
        LicenseQuickGenerator.GenerateResult result = generator.generateScenario3_ComposeAndEdit(
            info.companyName,
            info.contactPerson,
            info.contactPhone,
            info.licenseType.name(),
            info.duration,
            info.bindHardware,
            info.hardwareInfoFile,
            info.hardwareInfoManual,
            info.privateKeyPath,
            info.outputPath
        );
        
        printResult(result);
    }
    
    /**
     * 场景4：全功能授权
     */
    private static void generateScenario4(Scanner scanner, LicenseQuickGenerator generator) {
        System.out.println("\n╔════════════════ 场景4：全功能授权 ════════════════╗");
        System.out.println("授权模块：");
        System.out.println("  ✓ 智能文档抽取（规则提取）");
        System.out.println("  ✓ 智能文档比对（GPU比对）");
        System.out.println("  ✓ 智能合同合成（模板合成）");
        System.out.println("  ✓ 智能文档解析（OCR识别）");
        System.out.println("  ✓ 文档在线编辑（OnlyOffice）");
        System.out.println("  ✓ 文档格式转换（格式转换）");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        
        LicenseInfo info = collectLicenseInfo(scanner);
        if (info == null) return;
        
        System.out.println("\n正在生成License文件...");
        LicenseQuickGenerator.GenerateResult result = generator.generateScenario4_FullFeatures(
            info.companyName,
            info.contactPerson,
            info.contactPhone,
            info.licenseType.name(),
            info.duration,
            info.bindHardware,
            info.hardwareInfoFile,
            info.hardwareInfoManual,
            info.privateKeyPath,
            info.outputPath
        );
        
        printResult(result);
    }
    
    /**
     * 收集License基本信息
     */
    private static LicenseInfo collectLicenseInfo(Scanner scanner) {
        LicenseInfo info = new LicenseInfo();
        
        System.out.print("\n请输入授权单位名称: ");
        info.companyName = scanner.nextLine().trim();
        if (info.companyName.isEmpty()) {
            System.out.println("❌ 授权单位名称不能为空");
            return null;
        }
        
        System.out.print("请输入联系人姓名: ");
        info.contactPerson = scanner.nextLine().trim();
        if (info.contactPerson.isEmpty()) {
            System.out.println("❌ 联系人姓名不能为空");
            return null;
        }
        
        System.out.print("请输入联系电话: ");
        info.contactPhone = scanner.nextLine().trim();
        
        // 授权类型选择
        System.out.println("\n请选择授权类型：");
        System.out.println("1. 按天授权");
        System.out.println("2. 按年授权（推荐）");
        System.out.println("3. 永久授权");
        System.out.print("请选择 [默认: 2]: ");
        String licenseTypeChoice = scanner.nextLine().trim();
        if (licenseTypeChoice.isEmpty()) {
            licenseTypeChoice = "2";
        }
        
        switch (licenseTypeChoice) {
            case "1":
                info.licenseType = LicenseType.DAILY;
                System.out.print("请输入授权天数 [默认: 30天]: ");
                String days = scanner.nextLine().trim();
                info.duration = days.isEmpty() ? 30 : Integer.parseInt(days);
                break;
            case "2":
                info.licenseType = LicenseType.YEARLY;
                System.out.print("请输入授权年数 [默认: 1年]: ");
                String years = scanner.nextLine().trim();
                info.duration = years.isEmpty() ? 1 : Integer.parseInt(years);
                break;
            case "3":
                info.licenseType = LicenseType.PERPETUAL;
                info.duration = 0; // 永久授权无需时长
                System.out.println("✓ 已选择永久授权");
                break;
            default:
                System.out.println("❌ 无效选项，默认使用按年授权(1年)");
                info.licenseType = LicenseType.YEARLY;
                info.duration = 1;
        }
        
        System.out.print("请输入私钥文件路径 [默认: ./keys/private.key]: ");
        info.privateKeyPath = scanner.nextLine().trim();
        if (info.privateKeyPath.isEmpty()) {
            info.privateKeyPath = "./keys/private.key";
        }
        
        // 硬件绑定选项
        System.out.print("\n是否绑定硬件 [y/N]: ");
        String bindChoice = scanner.nextLine().trim().toLowerCase();
        info.bindHardware = "y".equals(bindChoice) || "yes".equals(bindChoice);
        
        if (info.bindHardware) {
            System.out.println("\n请选择硬件信息输入方式：");
            System.out.println("1. 从机器信息文件导入（推荐）");
            System.out.println("2. 手动输入硬件信息");
            System.out.print("请选择 [默认: 1]: ");
            String hwChoice = scanner.nextLine().trim();
            
            if ("2".equals(hwChoice)) {
                // 手动输入
                info.hardwareInfoFile = null;
                info.hardwareInfoManual = new java.util.ArrayList<>();
                
                System.out.println("\n═══════════════════════════════════════════════");
                System.out.println("请逐项输入硬件信息（所有字段均为必填）");
                System.out.println("说明：授权将绑定到以下硬件信息");
                System.out.println("═══════════════════════════════════════════════");
                
                // 1. 主板序列号
                System.out.println("\n【1/3】主板序列号（mainBoardSerial）");
                System.out.println("  说明：用于唯一标识主板硬件");
                System.out.println("  示例：YU4048248C113773");
                System.out.print("  请输入: ");
                String mainBoardSerial = scanner.nextLine().trim();
                if (mainBoardSerial.isEmpty()) {
                    System.out.println("❌ 主板序列号不能为空");
                    return null;
                }
                info.hardwareInfoManual.add("mainBoardSerial:" + mainBoardSerial);
                
                // 2. CPU序列号
                System.out.println("\n【2/3】CPU序列号（cpuSerial）");
                System.out.println("  说明：用于唯一标识CPU硬件");
                System.out.println("  示例：BFEBFBFF00090675");
                System.out.print("  请输入: ");
                String cpuSerial = scanner.nextLine().trim();
                if (cpuSerial.isEmpty()) {
                    System.out.println("❌ CPU序列号不能为空");
                    return null;
                }
                info.hardwareInfoManual.add("cpuSerial:" + cpuSerial);
                
                // 3. MAC地址
                System.out.println("\n【3/3】MAC地址（macAddress）");
                System.out.println("  说明：用于唯一标识网络接口，可输入多个");
                System.out.println("  格式：支持 XX-XX-XX-XX-XX-XX 或 XX:XX:XX:XX:XX:XX");
                System.out.println("  示例：10-5F-AD-E7-85-47 或 10:5F:AD:E7:85:47");
                System.out.println("  输入：每行一个MAC地址，输入空行结束");
                int macCount = 0;
                while (true) {
                    System.out.print("  MAC地址 " + (macCount + 1) + ": ");
                    String mac = scanner.nextLine().trim();
                    if (mac.isEmpty()) {
                        if (macCount == 0) {
                            System.out.println("❌ 至少需要输入一个MAC地址");
                            return null;
                        }
                        break;
                    }
                    // 验证MAC地址格式（简单验证）
                    if (!mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                        System.out.println("⚠ 警告：MAC地址格式可能不正确，但已记录");
                    }
                    info.hardwareInfoManual.add("macAddress:" + mac);
                    macCount++;
                }
                
                System.out.println("\n✓ 硬件信息收集完成");
                System.out.println("  - 主板序列号: " + mainBoardSerial);
                System.out.println("  - CPU序列号: " + cpuSerial);
                System.out.println("  - MAC地址数量: " + macCount);
                
            } else {
                // 从文件导入
                System.out.print("请输入机器信息文件路径 [默认: ./machine-info.json]: ");
                info.hardwareInfoFile = scanner.nextLine().trim();
                if (info.hardwareInfoFile.isEmpty()) {
                    info.hardwareInfoFile = "./machine-info.json";
                }
            }
        }
        
        // 不再要求用户输入输出路径，自动生成：公司名+年月日文件夹
        info.outputPath = null; // 将在生成时自动创建
        
        return info;
    }
    
    /**
     * 打印生成结果
     */
    private static void printResult(LicenseQuickGenerator.GenerateResult result) {
        if (result.isSuccess()) {
            System.out.println("\n✅ License生成成功！");
            System.out.println("╔═══════════════ License信息 ═══════════════╗");
            System.out.println("  授权码: " + result.getLicenseCode());
            System.out.println("  授权单位: " + result.getCompanyName());
            System.out.println("  生效时间: " + result.getStartDate());
            System.out.println("  到期时间: " + result.getExpireDate());
            System.out.println("  授权模块: " + result.getAuthorizedModules().size() + " 个");
            result.getAuthorizedModules().forEach(module -> 
                System.out.println("    - " + module.getName())
            );
            System.out.println("╚═══════════════════════════════════════════╝");
            System.out.println("\n📁 授权文件已保存到：");
            System.out.println("  文件夹: " + result.getOutputDirectory());
            System.out.println("  授权码: " + result.getLicenseFilePath());
            System.out.println("  详情: " + result.getInfoFilePath());
            System.out.println("\n📋 请将授权文件部署到系统中");
        } else {
            System.out.println("\n❌ License生成失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 授权类型枚举
     */
    private enum LicenseType {
        DAILY,      // 按天
        YEARLY,     // 按年
        PERPETUAL   // 永久
    }
    
    /**
     * License信息收集类
     */
    private static class LicenseInfo {
        String companyName;
        String contactPerson;
        String contactPhone;
        LicenseType licenseType;              // 授权类型
        int duration;                         // 授权时长（天数或年数，永久为0）
        String privateKeyPath;
        String outputPath;
        boolean bindHardware;                 // 是否绑定硬件
        String hardwareInfoFile;              // 硬件信息文件路径
        java.util.List<String> hardwareInfoManual;  // 手动输入的硬件信息
    }
}

