package com.zhaoxinms.contract.tools.auth.core.service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import com.zhaoxinms.contract.tools.auth.core.helper.LoggerHelper;
import com.zhaoxinms.contract.tools.auth.core.model.LicenseExtraParam;
import com.zhaoxinms.contract.tools.auth.core.utils.CommonUtils;

/**
 * <p>服务器硬件信息抽象类 -- 模板方法，将通用的方法抽离到父类中</p>
 *
 * @author appleyk
 * @version V.0.2.1
 * @blob https://blog.csdn.net/appleyk
 * @date created on  10:42 下午 2020/8/21
 */
public abstract class AServerInfos {

    private static class GxServerInfosContainer {
        private static List<String> ipAddress = null;
        private static List<String> macAddress = null;
        private static String cpuSerial = null;
        private static String mainBoardSerial = null;
    }

    /**
     * <p>组装需要额外校验的License参数</p>
     *
     * @return LicenseExtraParam 自定义校验参数
     */
    public LicenseExtraParam getServerInfos() {
        LicenseExtraParam result = new LicenseExtraParam();
        try {
            initServerInfos();
            result.setOsName(getDetailedOsName());
            result.setIpAddress(GxServerInfosContainer.ipAddress);
            result.setMacAddress(GxServerInfosContainer.macAddress);
            result.setCpuSerial(GxServerInfosContainer.cpuSerial);
            result.setMainBoardSerial(GxServerInfosContainer.mainBoardSerial);
        } catch (Exception e) {
            LoggerHelper.error("获取服务器硬件信息失败", e);
        }
        return result;
    }
    
    /**
     * 获取详细的操作系统名称（包括版本号）
     * 
     * @return 操作系统详细信息
     */
    private String getDetailedOsName() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        
        LoggerHelper.info("OS检测 - osName: " + osName + ", osVersion: " + osVersion + ", osArch: " + osArch);
        
        // Windows系统判断
        if (osName != null && osName.toLowerCase().contains("windows")) {
            return getWindowsDetailedName(osName, osVersion, osArch);
        }
        
        // Linux系统判断
        if (osName != null && osName.toLowerCase().contains("linux")) {
            return getLinuxDetailedName(osName, osVersion, osArch);
        }
        
        // Mac系统判断
        if (osName != null && (osName.toLowerCase().contains("mac") || osName.toLowerCase().contains("darwin"))) {
            return getMacDetailedName(osName, osVersion, osArch);
        }
        
        // 其他系统，返回系统名称 + 版本 + 架构
        return osName + " " + osVersion + " (" + osArch + ")";
    }
    
    /**
     * 获取Windows系统详细信息
     */
    private String getWindowsDetailedName(String osName, String osVersion, String osArch) {
        // Windows 11 的内部版本号是 10.0.22000 或更高
        if (osVersion != null && osVersion.startsWith("10.0")) {
            try {
                String[] versionParts = osVersion.split("\\.");
                LoggerHelper.info("Windows版本解析 - 版本部分: " + String.join(", ", versionParts));
                
                if (versionParts.length >= 3) {
                    int buildNumber = Integer.parseInt(versionParts[2]);
                    LoggerHelper.info("Windows构建号: " + buildNumber);
                    
                    // Windows 11 的构建号从 22000 开始
                    if (buildNumber >= 22000) {
                        LoggerHelper.info("检测为 Windows 11");
                        return "Windows 11 (" + osArch + ")";
                    } else {
                        LoggerHelper.info("检测为 Windows 10 (构建号 < 22000)");
                        return "Windows 10 (" + osArch + ")";
                    }
                } else {
                    LoggerHelper.warn("Windows版本部分不足3段: " + versionParts.length);
                }
            } catch (NumberFormatException e) {
                LoggerHelper.error("解析Windows构建号失败: " + e.getMessage(), e);
            } catch (Exception e) {
                LoggerHelper.error("解析Windows版本失败", e);
            }
        }
        
        // 无法准确识别，返回原始信息
        return osName + " (" + osArch + ")";
    }
    
    /**
     * 获取Linux系统详细信息
     */
    private String getLinuxDetailedName(String osName, String osVersion, String osArch) {
        try {
            // 尝试读取 /etc/os-release 获取更详细的信息
            java.io.File osReleaseFile = new java.io.File("/etc/os-release");
            if (osReleaseFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(osReleaseFile))) {
                    String line;
                    String prettyName = null;
                    String name = null;
                    String version = null;
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("PRETTY_NAME=")) {
                            prettyName = line.substring(12).replaceAll("\"", "");
                        } else if (line.startsWith("NAME=")) {
                            name = line.substring(5).replaceAll("\"", "");
                        } else if (line.startsWith("VERSION=")) {
                            version = line.substring(8).replaceAll("\"", "");
                        }
                    }
                    
                    // 优先使用 PRETTY_NAME
                    if (prettyName != null && !prettyName.isEmpty()) {
                        LoggerHelper.info("Linux发行版检测: " + prettyName);
                        return prettyName + " (" + osArch + ")";
                    }
                    
                    // 否则组合 NAME + VERSION
                    if (name != null && version != null) {
                        return name + " " + version + " (" + osArch + ")";
                    }
                }
            }
            
            // 尝试通过 lsb_release 命令获取信息
            Process process = Runtime.getRuntime().exec("lsb_release -d");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains(":")) {
                    String distribution = line.substring(line.indexOf(":") + 1).trim();
                    LoggerHelper.info("Linux发行版检测(lsb): " + distribution);
                    return distribution + " (" + osArch + ")";
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            LoggerHelper.warn("获取Linux详细信息失败: " + e.getMessage());
        }
        
        // 返回基本信息
        return osName + " " + osVersion + " (" + osArch + ")";
    }
    
    /**
     * 获取Mac系统详细信息
     */
    private String getMacDetailedName(String osName, String osVersion, String osArch) {
        try {
            // 尝试获取 macOS 版本名称
            Process process = Runtime.getRuntime().exec("sw_vers -productVersion");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                if (version != null && !version.isEmpty()) {
                    LoggerHelper.info("macOS版本检测: " + version);
                    return "macOS " + version + " (" + osArch + ")";
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LoggerHelper.warn("获取macOS详细信息失败: " + e.getMessage());
        }
        
        // 返回基本信息
        return osName + " " + osVersion + " (" + osArch + ")";
    }

    /**
     * <p>初始化服务器硬件信息，并将信息缓存到内存</p>
     *
     * @throws Exception 默认异常
     */
    private void initServerInfos() throws Exception {
        if (GxServerInfosContainer.ipAddress == null) {
            GxServerInfosContainer.ipAddress = this.getIpAddress();
        }
        if (GxServerInfosContainer.macAddress == null) {
            GxServerInfosContainer.macAddress = this.getMacAddress();
        }
        if (GxServerInfosContainer.cpuSerial == null) {
            GxServerInfosContainer.cpuSerial = this.getCPUSerial();
        }
        if (GxServerInfosContainer.mainBoardSerial == null) {
            GxServerInfosContainer.mainBoardSerial = this.getMainBoardSerial();
        }
    }

    /**
     * <p>获取IP地址</p>
     *
     * @return List<String> IP地址
     * @throws Exception 默认异常
     */
    public List<String> getIpAddress() throws Exception {
        /** 获取所有网络接口 */
        List<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (CommonUtils.isNotEmpty(inetAddresses)) {
            return inetAddresses.stream().map(InetAddress::getHostAddress).distinct().map(String::toLowerCase).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * <p>获取Mac地址</p>
     *
     * @return List<String> Mac地址
     * @throws Exception 默认异常
     */
    public List<String> getMacAddress() throws Exception {
        /** 获取所有网络接口 */
        List<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (CommonUtils.isNotEmpty(inetAddresses)) {
            return inetAddresses.stream().map(this::getMacByInetAddress).distinct().collect(Collectors.toList());
        }
        return null;
    }

    /**
     * <p>获取服务器信息</p>
     *
     * @param osName 系统类型
     * @return AGxServerInfos 服务信息
     */
    public static AServerInfos getServer(String osName) {
        if ("".equals(osName) || osName == null) {
            osName = System.getProperty("os.name").toLowerCase();
        }
        AServerInfos abstractServerInfos;
        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractServerInfos = new WindowsServerInfos();
        } else if (osName.startsWith("linux")) {
            abstractServerInfos = new LinuxServerInfos();
        } else {//其他服务器类型
            abstractServerInfos = new LinuxServerInfos();
        }
        return abstractServerInfos;
    }

    /**
     * <p>获取服务器临时磁盘位置</p>
     */
    public static String getServerTempPath() {

        String property = System.getProperty("user.dir");
        return property;

        //String osName = System.getProperty("os.name").toLowerCase();
        ////根据不同操作系统类型选择不同的数据获取方法
        //if (osName.startsWith("windows")) {
        //    return property.substring(0,property.indexOf(":")+1);
        //} else if (osName.startsWith("linux")) {
        //    return "/home";
        //}else{//其他服务器类型
        //    return "/home";
        //}
    }

    /**
     * <p>获取CPU序列号</p>
     *
     * @return String CPU序列号
     * @throws Exception 默认异常
     */
    public abstract String getCPUSerial() throws Exception;

    /**
     * <p>获取主板序列号</p>
     *
     * @return String 主板序列号
     * @throws Exception 默认异常
     */
    public abstract String getMainBoardSerial() throws Exception;

    /**
     * <p>获取当前服务器所有符合条件的网络地址</p>
     *
     * @return List<InetAddress> 网络地址列表
     * @throws Exception 默认异常
     */
    private List<InetAddress> getLocalAllInetAddress() throws Exception {
        List<InetAddress> result = new ArrayList<>(4);
        // 遍历所有的网络接口
        for (Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
            NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
            // 在所有的接口下再遍历IP
            for (Enumeration addresses = ni.getInetAddresses(); addresses.hasMoreElements(); ) {
                InetAddress address = (InetAddress) addresses.nextElement();
                //排除LoopbackAddress、SiteLocalAddress、LinkLocalAddress、MulticastAddress类型的IP地址
                if (!address.isLoopbackAddress() /*&& !inetAddr.isSiteLocalAddress()*/
                        && !address.isLinkLocalAddress() && !address.isMulticastAddress()) {
                    result.add(address);
                }
            }
        }
        return result;
    }

    /**
     * <p>获取某个网络地址对应的Mac地址</p>
     *
     * @param inetAddr 网络地址
     * @return String Mac地址
     */
    private String getMacByInetAddress(InetAddress inetAddr) {
        try {
            if (inetAddr == null) {
                return null;
            }
            
            NetworkInterface network = NetworkInterface.getByInetAddress(inetAddr);
            if (network == null) {
                return null;
            }
            
            byte[] mac = network.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                return null;
            }
            
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    stringBuilder.append("-");
                }
                /** 将十六进制byte转化为字符串 */
                String temp = Integer.toHexString(mac[i] & 0xff);
                if (temp.length() == 1) {
                    stringBuilder.append("0").append(temp);
                } else {
                    stringBuilder.append(temp);
                }
            }
            return stringBuilder.toString().toUpperCase();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

}
