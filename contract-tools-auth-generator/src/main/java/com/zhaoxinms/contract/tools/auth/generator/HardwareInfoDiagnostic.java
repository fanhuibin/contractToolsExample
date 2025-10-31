package com.zhaoxinms.contract.tools.auth.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.core.service.AServerInfos;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

/**
 * 硬件信息诊断工具
 * 用于排查授权验证失败的硬件信息不匹配问题
 */
public class HardwareInfoDiagnostic {
    
    private final ObjectMapper objectMapper;
    
    public HardwareInfoDiagnostic() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public static void main(String[] args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║        硬件信息诊断工具 - 授权验证问题排查                    ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        HardwareInfoDiagnostic diagnostic = new HardwareInfoDiagnostic();
        String path = "D:\\git\\zhaoxin-contract-tool-set\\contract-tools-auth-generator\\licenses\\我是人_20251029_171418\\license.lic";
        
        // 1. 显示当前系统硬件信息
        diagnostic.displayCurrentHardwareInfo();
        
        // 2. 如果提供了授权文件路径，显示授权文件中的硬件信息
        if (path.length() > 0) {
            String licenseFilePath = path;
            diagnostic.displayLicenseHardwareInfo(licenseFilePath);
            diagnostic.compareHardwareInfo(licenseFilePath);
        } else {
            System.out.println("\n提示：如需对比授权文件中的硬件信息，请运行：");
            System.out.println("  java HardwareInfoDiagnostic <授权文件路径>");
            System.out.println("\n示例：");
            System.out.println("  java HardwareInfoDiagnostic D:\\licenses\\我是人_20251029_171418\\license.lic");
        }
    }
    
    /**
     * 显示当前系统硬件信息
     */
    public void displayCurrentHardwareInfo() {
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("【当前系统硬件信息】");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        try {
            AServerInfos serverInfos = AServerInfos.getServer(null);
            
            // 1. MAC地址
            List<String> macAddresses = serverInfos.getMacAddress();
            System.out.println("\n1. MAC地址：");
            if (macAddresses != null && !macAddresses.isEmpty()) {
                for (int i = 0; i < macAddresses.size(); i++) {
                    System.out.println("   [" + (i + 1) + "] " + macAddresses.get(i));
                }
            } else {
                System.out.println("   ❌ 未获取到MAC地址");
            }
            
            // 2. CPU序列号
            String cpuSerial = serverInfos.getCPUSerial();
            System.out.println("\n2. CPU序列号：");
            if (CommonUtils.isNotEmpty(cpuSerial)) {
                System.out.println("   " + cpuSerial);
            } else {
                System.out.println("   ❌ 未获取到CPU序列号");
            }
            
            // 3. 主板序列号
            String mainBoardSerial = serverInfos.getMainBoardSerial();
            System.out.println("\n3. 主板序列号：");
            if (CommonUtils.isNotEmpty(mainBoardSerial)) {
                System.out.println("   " + mainBoardSerial);
            } else {
                System.out.println("   ❌ 未获取到主板序列号");
            }
            
            System.out.println("\n════════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 获取硬件信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示授权文件中的硬件信息
     */
    public void displayLicenseHardwareInfo(String licenseFilePath) {
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("【授权文件中的硬件信息】");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        try {
            File licenseFile = new File(licenseFilePath);
            if (!licenseFile.exists()) {
                System.err.println("❌ 授权文件不存在: " + licenseFilePath);
                return;
            }
            
            // 读取授权文件
            String licenseContent = new String(Files.readAllBytes(licenseFile.toPath()));
            String[] parts = licenseContent.split("\\.");
            if (parts.length != 2) {
                System.err.println("❌ 授权文件格式错误");
                return;
            }
            
            // 解析License信息
            String licenseData = new String(Base64.getDecoder().decode(parts[0]));
            LicenseInfo licenseInfo = objectMapper.readValue(licenseData, LicenseInfo.class);
            
            // 显示硬件绑定状态
            if (licenseInfo.getHardwareBound() == null || !licenseInfo.getHardwareBound()) {
                System.out.println("\n✓ 此授权文件未绑定硬件");
                System.out.println("════════════════════════════════════════════════════════════════");
                return;
            }
            
            System.out.println("\n⚠ 此授权文件已绑定硬件");
            
            // 显示绑定的硬件信息
            List<String> boundInfo = licenseInfo.getBoundHardwareInfo();
            if (boundInfo == null || boundInfo.isEmpty()) {
                System.out.println("❌ 授权文件中没有硬件信息（可能已损坏）");
            } else {
                System.out.println("\n绑定的硬件信息（" + boundInfo.size() + " 项）：");
                for (int i = 0; i < boundInfo.size(); i++) {
                    System.out.println("   [" + (i + 1) + "] " + boundInfo.get(i));
                }
            }
            
            System.out.println("\n════════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 读取授权文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 对比当前硬件信息与授权文件中的硬件信息
     */
    public void compareHardwareInfo(String licenseFilePath) {
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("【硬件信息对比结果】");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        try {
            // 获取当前硬件信息
            AServerInfos serverInfos = AServerInfos.getServer(null);
            List<String> currentMacAddresses = serverInfos.getMacAddress();
            String currentCpuSerial = serverInfos.getCPUSerial();
            String currentMainBoardSerial = serverInfos.getMainBoardSerial();
            
            // 读取授权文件中的硬件信息
            File licenseFile = new File(licenseFilePath);
            String licenseContent = new String(Files.readAllBytes(licenseFile.toPath()));
            String[] parts = licenseContent.split("\\.");
            String licenseData = new String(Base64.getDecoder().decode(parts[0]));
            LicenseInfo licenseInfo = objectMapper.readValue(licenseData, LicenseInfo.class);
            
            if (licenseInfo.getHardwareBound() == null || !licenseInfo.getHardwareBound()) {
                System.out.println("\n✓ 授权未绑定硬件，无需对比");
                System.out.println("════════════════════════════════════════════════════════════════");
                return;
            }
            
            List<String> boundInfo = licenseInfo.getBoundHardwareInfo();
            if (boundInfo == null || boundInfo.isEmpty()) {
                System.out.println("\n❌ 授权文件中没有硬件信息");
                System.out.println("════════════════════════════════════════════════════════════════");
                return;
            }
            
            // 对比结果
            boolean matched = false;
            
            // 检查MAC地址
            System.out.println("\n检查MAC地址：");
            if (currentMacAddresses != null) {
                for (String mac : currentMacAddresses) {
                    boolean macMatched = containsHardwareInfo(boundInfo, "macAddress", mac);
                    System.out.println("   当前: " + mac + " -> " + (macMatched ? "✓ 匹配" : "✗ 不匹配"));
                    if (macMatched) matched = true;
                }
            } else {
                System.out.println("   ❌ 当前系统未获取到MAC地址");
            }
            
            // 检查CPU序列号
            System.out.println("\n检查CPU序列号：");
            if (CommonUtils.isNotEmpty(currentCpuSerial)) {
                boolean cpuMatched = containsHardwareInfo(boundInfo, "cpuSerial", currentCpuSerial);
                System.out.println("   当前: " + currentCpuSerial + " -> " + (cpuMatched ? "✓ 匹配" : "✗ 不匹配"));
                if (cpuMatched) matched = true;
            } else {
                System.out.println("   ❌ 当前系统未获取到CPU序列号");
            }
            
            // 检查主板序列号
            System.out.println("\n检查主板序列号：");
            if (CommonUtils.isNotEmpty(currentMainBoardSerial)) {
                boolean boardMatched = containsHardwareInfo(boundInfo, "mainBoardSerial", currentMainBoardSerial);
                System.out.println("   当前: " + currentMainBoardSerial + " -> " + (boardMatched ? "✓ 匹配" : "✗ 不匹配"));
                if (boardMatched) matched = true;
            } else {
                System.out.println("   ❌ 当前系统未获取到主板序列号");
            }
            
            // 最终结果
            System.out.println("\n" + "─".repeat(64));
            if (matched) {
                System.out.println("✅ 验证结果：硬件信息匹配（至少一项匹配即可通过）");
            } else {
                System.out.println("❌ 验证结果：硬件信息不匹配");
                System.out.println("\n可能的原因：");
                System.out.println("  1. 授权码不是在当前机器上生成的");
                System.out.println("  2. 系统硬件信息获取失败");
                System.out.println("  3. 机器硬件发生了变更");
                System.out.println("\n解决方案：");
                System.out.println("  方案1：在当前机器上重新生成授权码");
                System.out.println("  方案2：生成不绑定硬件的授权码");
                System.out.println("  方案3：使用机器信息文件生成授权（推荐）");
            }
            
            System.out.println("════════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 对比失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查硬件信息列表中是否包含指定的硬件信息
     * 处理授权文件中的格式：prefix:value
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

