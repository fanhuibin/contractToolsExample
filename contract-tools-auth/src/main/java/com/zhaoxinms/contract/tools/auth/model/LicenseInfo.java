package com.zhaoxinms.contract.tools.auth.model;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * License信息模型
 */
@Data
public class LicenseInfo {
    
    /**
     * 许可证编号
     */
    private String licenseCode;
    
    /**
     * 公司名称
     */
    private String companyName;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 授权模块列表
     */
    private Set<ModuleType> authorizedModules;
    
    /**
     * 授权用户数量限制
     */
    private Integer maxUsers;
    
    /**
     * 生效时间
     */
    private LocalDateTime startDate;
    
    /**
     * 到期时间
     */
    private LocalDateTime expireDate;
    
    /**
     * 是否绑定硬件
     */
    private Boolean hardwareBound;
    
    /**
     * 绑定的硬件信息
     */
    private List<String> boundHardwareInfo;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 签名
     */
    private String signature;
    
    /**
     * 检查许可证是否有效
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(expireDate);
    }
    
    /**
     * 检查是否有模块权限
     */
    public boolean hasModulePermission(ModuleType moduleType) {
        return isValid() && authorizedModules != null && authorizedModules.contains(moduleType);
    }
}
