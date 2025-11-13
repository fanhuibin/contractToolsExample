package com.zhaoxinms.contract.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 演示模式配置
 * 
 * @author 山西肇新科技有限公司
 */
@Data
@Component
@ConfigurationProperties(prefix = "zxcm.demo-mode")
public class DemoModeConfig {

    /**
     * 是否启用演示模式
     */
    private boolean enabled = false;

    /**
     * 演示模式下最大文件大小（MB）
     */
    private int maxFileSizeMb = 20;

    /**
     * 演示模式下最大页数
     */
    private int maxPages = 20;

    /**
     * 演示模式下是否隐藏历史数据和任务历史功能
     */
    private boolean hideHistory = true;

    /**
     * 获取当前有效的最大文件大小（MB）
     * 如果未启用演示模式，返回 -1 表示不限制
     */
    public int getEffectiveMaxFileSizeMb() {
        return enabled ? maxFileSizeMb : -1;
    }

    /**
     * 获取当前有效的最大页数
     * 如果未启用演示模式，返回 -1 表示不限制
     */
    public int getEffectiveMaxPages() {
        return enabled ? maxPages : -1;
    }

    /**
     * 是否启用了演示模式
     */
    public boolean isDemoMode() {
        return enabled;
    }

    /**
     * 是否应该隐藏历史数据和任务历史功能
     * 只有在演示模式启用且配置了隐藏历史时才返回true
     */
    public boolean shouldHideHistory() {
        return enabled && hideHistory;
    }
}

