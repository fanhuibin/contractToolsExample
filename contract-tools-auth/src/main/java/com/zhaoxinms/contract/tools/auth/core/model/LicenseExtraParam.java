package com.zhaoxinms.contract.tools.auth.core.model;

import lombok.Data;
import java.util.List;

/**
 * License额外参数，用于硬件绑定验证
 * 包含服务器硬件信息和相应的验证开关
 */
@Data
public class LicenseExtraParam {
    
    /**
     * 操作系统名称
     */
    private String osName;
    
    /**
     * IP地址列表
     */
    private List<String> ipAddress;
    
    /**
     * MAC地址列表
     */
    private List<String> macAddress;
    
    /**
     * CPU序列号
     */
    private String cpuSerial;
    
    /**
     * 主板序列号
     */
    private String mainBoardSerial;
    
    /**
     * 是否校验IP地址
     */
    private boolean ipCheck = false;
    
    /**
     * 是否校验MAC地址
     */
    private boolean macCheck = false;
    
    /**
     * 是否校验CPU序列号
     */
    private boolean cpuCheck = false;
    
    /**
     * 是否校验主板序列号
     */
    private boolean boardCheck = false;
    
    // 为了兼容性，提供getter方法
    public boolean isIpCheck() {
        return ipCheck;
    }
    
    public boolean isMacCheck() {
        return macCheck;
    }
    
    public boolean isCpuCheck() {
        return cpuCheck;
    }
    
    public boolean isBoardCheck() {
        return boardCheck;
    }
}