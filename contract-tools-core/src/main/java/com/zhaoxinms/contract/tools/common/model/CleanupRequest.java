package com.zhaoxinms.contract.tools.common.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 系统文件清理请求
 * 
 * @author AI Assistant
 * @since 2025-10-29
 */
public class CleanupRequest {
    
    /**
     * 清理模式：preview(预览) 或 execute(执行)
     */
    private String mode = "preview";
    
    /**
     * 要清理的模块列表（为空则清理所有模块）
     * 可选值：rule-extract, compare-pro, ocr-extract, compose, onlyoffice-demo, temp-uploads, file-info
     */
    private List<String> modules;
    
    /**
     * 清理开始日期（包含）
     * 格式：yyyy-MM-dd
     * 例如：2023-01-01
     */
    private LocalDate startDate;
    
    /**
     * 清理结束日期（包含）
     * 格式：yyyy-MM-dd
     * 例如：2023-12-31
     */
    private LocalDate endDate;
    
    /**
     * 是否清理数据库记录（file_info表）
     * 默认：true
     */
    private Boolean cleanDatabase = true;
    
    /**
     * 是否清理文件系统
     * 默认：true
     */
    private Boolean cleanFileSystem = true;
    
    /**
     * 确认标识（执行模式必须为true）
     */
    private Boolean confirmed = false;
    
    // Getters and Setters
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public List<String> getModules() {
        return modules;
    }
    
    public void setModules(List<String> modules) {
        this.modules = modules;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getCleanDatabase() {
        return cleanDatabase;
    }
    
    public void setCleanDatabase(Boolean cleanDatabase) {
        this.cleanDatabase = cleanDatabase;
    }
    
    public Boolean getCleanFileSystem() {
        return cleanFileSystem;
    }
    
    public void setCleanFileSystem(Boolean cleanFileSystem) {
        this.cleanFileSystem = cleanFileSystem;
    }
    
    public Boolean getConfirmed() {
        return confirmed;
    }
    
    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (startDate == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        
        if (endDate == null) {
            throw new IllegalArgumentException("结束日期不能为空");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("结束日期不能是未来日期");
        }
        
        if ("execute".equals(mode) && !Boolean.TRUE.equals(confirmed)) {
            throw new IllegalArgumentException("执行清理必须确认操作（confirmed=true）");
        }
        
        if (!Boolean.TRUE.equals(cleanDatabase) && !Boolean.TRUE.equals(cleanFileSystem)) {
            throw new IllegalArgumentException("至少需要清理数据库或文件系统中的一项");
        }
    }
}

