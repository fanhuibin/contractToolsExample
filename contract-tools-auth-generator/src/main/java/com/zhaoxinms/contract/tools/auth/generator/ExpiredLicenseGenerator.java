package com.zhaoxinms.contract.tools.auth.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhaoxinms.contract.tools.auth.core.service.AServerInfos;
import com.zhaoxinms.contract.tools.auth.core.utils.SignatureUtils;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.auth.model.LicenseInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 过期授权生成器 - 用于测试过期授权的处理逻辑
 * 
 * @author zhaoxin
 * @since 2025-10-29
 */
public class ExpiredLicenseGenerator {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                                                              ║");
            System.out.println("║              过期授权生成器 - 测试用                          ║");
            System.out.println("║                                                              ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
            
            // 创建过期授权信息
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setLicenseCode("EXPIRED_TEST_" + System.currentTimeMillis());
            licenseInfo.setCompanyName("测试公司");
            licenseInfo.setContactPerson("测试人员");
            licenseInfo.setContactPhone("13800138000");
            
            // 授权模块：智能文档比对 和 智能文档解析
            Set<ModuleType> modules = new HashSet<>();
            modules.add(ModuleType.SMART_DOCUMENT_COMPARE);  // 智能文档比对
            modules.add(ModuleType.SMART_DOCUMENT_PARSE);    // 智能文档解析
            licenseInfo.setAuthorizedModules(modules);
            licenseInfo.setMaxUsers(10);
            
            // 设置时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now.minusDays(7); // 7天前开始
            
            // 设置到期时间为今天 18:15
            LocalDateTime expireDate = LocalDateTime.now()
                .withHour(18)
                .withMinute(20)
                .withSecond(0)
                .withNano(0);
            
            licenseInfo.setStartDate(startDate);
            licenseInfo.setExpireDate(expireDate);
            licenseInfo.setCreateTime(startDate);
            
            System.out.println("生成时间：" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("开始时间：" + startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("到期时间：" + expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 检查是否已过期
            boolean isExpired = now.isAfter(expireDate);
            System.out.println("\n授权状态：" + (isExpired ? "❌ 已过期" : "✓ 未过期"));
            System.out.println();
            
            // 硬件绑定（获取当前机器硬件信息）
            System.out.println("正在获取当前机器硬件信息...");
            AServerInfos serverInfos = AServerInfos.getServer(null);
            List<String> hardwareInfo = new ArrayList<>();
            
            // MAC地址
            List<String> macAddresses = serverInfos.getMacAddress();
            if (macAddresses != null && !macAddresses.isEmpty()) {
                String mac = macAddresses.get(0);
                hardwareInfo.add("macAddress:" + mac);
                System.out.println("  MAC地址: " + mac);
            }
            
            // CPU序列号
            String cpuSerial = serverInfos.getCPUSerial();
            if (cpuSerial != null && !cpuSerial.trim().isEmpty()) {
                hardwareInfo.add("cpuSerial:" + cpuSerial);
                System.out.println("  CPU序列号: " + cpuSerial);
            }
            
            // 主板序列号
            String mainBoardSerial = serverInfos.getMainBoardSerial();
            if (mainBoardSerial != null && !mainBoardSerial.trim().isEmpty()) {
                hardwareInfo.add("mainBoardSerial:" + mainBoardSerial);
                System.out.println("  主板序列号: " + mainBoardSerial);
            }
            
            licenseInfo.setHardwareBound(true);
            licenseInfo.setBoundHardwareInfo(hardwareInfo);
            System.out.println("\n✓ 已绑定硬件信息（" + hardwareInfo.size() + " 项）\n");
            
            // 序列化License信息
            String licenseJson = objectMapper.writeValueAsString(licenseInfo);
            String licenseData = Base64.getEncoder().encodeToString(licenseJson.getBytes());
            
            // 读取私钥
            String privateKeyPath = "./keys/private.key";
            File privateKeyFile = new File(privateKeyPath);
            if (!privateKeyFile.exists()) {
                System.err.println("❌ 私钥文件不存在: " + privateKeyPath);
                System.out.println("\n请先运行 LicenseQuickGeneratorCLI 生成密钥对！");
                return;
            }
            
            String privateKeyStr = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            PrivateKey privateKey = SignatureUtils.stringToPrivateKey(privateKeyStr);
            
            // 生成签名
            String signature = SignatureUtils.sign(licenseJson, privateKey);
            
            // 生成License文件内容
            String licenseContent = licenseData + "." + signature;
            
            // 创建输出文件夹
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputDir = "./licenses/过期测试_" + timestamp;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存License文件
            String licenseFilePath = outputDir + "/license.lic";
            Files.write(Paths.get(licenseFilePath), licenseContent.getBytes());
            
            // 生成详情文本文件
            String infoFilePath = outputDir + "/license-info.txt";
            StringBuilder info = new StringBuilder();
            info.append("═══════════════════════════════════════════════════════════════\n");
            info.append("                    过期授权测试文件                          \n");
            info.append("═══════════════════════════════════════════════════════════════\n\n");
            info.append("授权码      : ").append(licenseInfo.getLicenseCode()).append("\n");
            info.append("授权单位    : ").append(licenseInfo.getCompanyName()).append("\n");
            info.append("联系人      : ").append(licenseInfo.getContactPerson()).append("\n");
            info.append("联系电话    : ").append(licenseInfo.getContactPhone()).append("\n");
            info.append("最大用户数  : ").append(licenseInfo.getMaxUsers()).append("\n\n");
            
            info.append("───────────────────────────────────────────────────────────────\n");
            info.append("时间信息\n");
            info.append("───────────────────────────────────────────────────────────────\n");
            info.append("生成时间    : ").append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            info.append("开始时间    : ").append(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            info.append("到期时间    : ").append(expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            info.append("当前状态    : ").append(isExpired ? "已过期 ❌" : "未过期 ✓").append("\n\n");
            
            info.append("───────────────────────────────────────────────────────────────\n");
            info.append("授权模块\n");
            info.append("───────────────────────────────────────────────────────────────\n");
            for (ModuleType module : modules) {
                info.append("  ✓ ").append(module.getName()).append(" (").append(module.getCode()).append(")\n");
            }
            info.append("\n");
            
            info.append("───────────────────────────────────────────────────────────────\n");
            info.append("硬件绑定\n");
            info.append("───────────────────────────────────────────────────────────────\n");
            info.append("已绑定硬件  : 是\n");
            info.append("绑定信息    : \n");
            for (String hw : hardwareInfo) {
                info.append("  - ").append(hw).append("\n");
            }
            info.append("\n");
            
            info.append("═══════════════════════════════════════════════════════════════\n");
            info.append("注意：此授权仅用于测试过期授权的处理逻辑！\n");
            info.append("═══════════════════════════════════════════════════════════════\n");
            
            Files.write(Paths.get(infoFilePath), info.toString().getBytes("UTF-8"));
            
            System.out.println("✅ 过期授权生成成功！\n");
            System.out.println("输出目录：" + new File(outputDir).getAbsolutePath());
            System.out.println("授权文件：" + licenseFilePath);
            System.out.println("详情文件：" + infoFilePath);
            System.out.println("\n请将 license.lic 文件复制到后端项目的 resources 目录进行测试。");
            System.out.println("════════════════════════════════════════════════════════════════\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ 生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

