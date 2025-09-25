package com.zhaoxinms.contract.tools.comparePRO.model;

/**
 * 合同比对URL请求参数类 - 用于外部系统对接
 * 支持JSON + URL格式的文档比对请求
 */
public class CompareUrlRequest {
    
    // 必需参数
    private String oldFileUrl;
    private String newFileUrl;
    
    // 比对选项（可选）
    private Boolean ignoreHeaderFooter = true;
    private Double headerHeightPercent = 12.0;
    private Double footerHeightPercent = 12.0;
    private Boolean ignoreCase = true;
    private String ignoredSymbols = "_＿";
    private Boolean ignoreSpaces = false;
    private Boolean ignoreSeals = true;
    private Boolean removeWatermark = false;
    private String watermarkRemovalStrength = "smart";
    
    public CompareUrlRequest() {}
    
    /**
     * 转换为CompareOptions对象
     */
    public CompareOptions toCompareOptions() {
        CompareOptions options = new CompareOptions();
        if (ignoreHeaderFooter != null) options.setIgnoreHeaderFooter(ignoreHeaderFooter);
        if (headerHeightPercent != null) options.setHeaderHeightPercent(headerHeightPercent);
        if (footerHeightPercent != null) options.setFooterHeightPercent(footerHeightPercent);
        if (ignoreCase != null) options.setIgnoreCase(ignoreCase);
        if (ignoredSymbols != null) options.setIgnoredSymbols(ignoredSymbols);
        if (ignoreSpaces != null) options.setIgnoreSpaces(ignoreSpaces);
        if (ignoreSeals != null) options.setIgnoreSeals(ignoreSeals);
        if (removeWatermark != null) options.setRemoveWatermark(removeWatermark);
        if (watermarkRemovalStrength != null) options.setWatermarkRemovalStrength(watermarkRemovalStrength);
        return options;
    }
    
    // Getters and Setters
    public String getOldFileUrl() {
        return oldFileUrl;
    }
    
    public void setOldFileUrl(String oldFileUrl) {
        this.oldFileUrl = oldFileUrl;
    }
    
    public String getNewFileUrl() {
        return newFileUrl;
    }
    
    public void setNewFileUrl(String newFileUrl) {
        this.newFileUrl = newFileUrl;
    }
    
    public Boolean getIgnoreHeaderFooter() {
        return ignoreHeaderFooter;
    }
    
    public void setIgnoreHeaderFooter(Boolean ignoreHeaderFooter) {
        this.ignoreHeaderFooter = ignoreHeaderFooter;
    }
    
    public Double getHeaderHeightPercent() {
        return headerHeightPercent;
    }
    
    public void setHeaderHeightPercent(Double headerHeightPercent) {
        this.headerHeightPercent = headerHeightPercent;
    }
    
    public Double getFooterHeightPercent() {
        return footerHeightPercent;
    }
    
    public void setFooterHeightPercent(Double footerHeightPercent) {
        this.footerHeightPercent = footerHeightPercent;
    }
    
    public Boolean getIgnoreCase() {
        return ignoreCase;
    }
    
    public void setIgnoreCase(Boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
    
    public String getIgnoredSymbols() {
        return ignoredSymbols;
    }
    
    public void setIgnoredSymbols(String ignoredSymbols) {
        this.ignoredSymbols = ignoredSymbols;
    }
    
    public Boolean getIgnoreSpaces() {
        return ignoreSpaces;
    }
    
    public void setIgnoreSpaces(Boolean ignoreSpaces) {
        this.ignoreSpaces = ignoreSpaces;
    }
    
    public Boolean getIgnoreSeals() {
        return ignoreSeals;
    }
    
    public void setIgnoreSeals(Boolean ignoreSeals) {
        this.ignoreSeals = ignoreSeals;
    }
    
    public Boolean getRemoveWatermark() {
        return removeWatermark;
    }
    
    public void setRemoveWatermark(Boolean removeWatermark) {
        this.removeWatermark = removeWatermark;
    }
    
    public String getWatermarkRemovalStrength() {
        return watermarkRemovalStrength;
    }
    
    public void setWatermarkRemovalStrength(String watermarkRemovalStrength) {
        this.watermarkRemovalStrength = watermarkRemovalStrength;
    }
}
