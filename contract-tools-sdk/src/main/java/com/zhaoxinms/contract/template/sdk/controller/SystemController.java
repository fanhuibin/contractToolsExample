package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.config.DemoModeConfig;
import com.zhaoxinms.contract.constant.SystemConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统信息控制器
 * 
 * @author 肇新科技
 */
@Api(tags = "系统信息")
@RestController
@RequestMapping("/api/system")
public class SystemController {
    
    @Autowired
    private DemoModeConfig demoModeConfig;
    
    /**
     * 获取系统版本信息
     */
    @ApiOperation("获取系统版本信息")
    @GetMapping("/version")
    public ApiResponse<SystemInfo> getVersion() {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setVersion(SystemConstants.VERSION);
        systemInfo.setName(SystemConstants.NAME);
        systemInfo.setBuildDate(SystemConstants.BUILD_DATE);
        
        return ApiResponse.success(systemInfo);
    }
    
    /**
     * 获取系统配置信息
     */
    @ApiOperation("获取系统配置信息")
    @GetMapping("/config")
    public ApiResponse<SystemConfig> getConfig() {
        SystemConfig config = new SystemConfig();
        config.setDemoMode(demoModeConfig.isDemoMode());
        config.setHideHistory(demoModeConfig.shouldHideHistory());
        
        return ApiResponse.success(config);
    }
    
    /**
     * 系统信息DTO
     */
    @Data
    public static class SystemInfo {
        /**
         * 系统版本号
         */
        private String version;
        
        /**
         * 系统名称
         */
        private String name;
        
        /**
         * 构建日期
         */
        private String buildDate;
    }
    
    /**
     * 系统配置DTO
     */
    @Data
    public static class SystemConfig {
        /**
         * 是否为演示模式
         */
        private boolean demoMode;
        
        /**
         * 是否隐藏历史数据和任务历史功能
         */
        private boolean hideHistory;
    }
}

