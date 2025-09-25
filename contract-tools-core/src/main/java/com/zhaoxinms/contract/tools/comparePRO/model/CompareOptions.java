package com.zhaoxinms.contract.tools.comparePRO.model;

/**
 * 高级合同比对选项类
 */
public class CompareOptions {

    private boolean ignoreHeaderFooter = false;
    private double headerHeightPercent = 12;
    private double footerHeightPercent = 12;
    private boolean ignoreCase = true;
    private String ignoredSymbols = "_＿";
    private boolean ignoreSpaces = false;
    private boolean ignoreSeals = true;
    private boolean removeWatermark = false;
    private String watermarkRemovalStrength = "smart"; // default, extended, loose, smart
    
    // OCR服务选择
    private String ocrServiceType = "dotsocr"; // dotsocr, thirdparty

    public CompareOptions() {}

    public static CompareOptions createDefault() {
        return new CompareOptions();
    }

    // Getters and Setters
    public boolean isIgnoreHeaderFooter() {
        return ignoreHeaderFooter;
    }

    public void setIgnoreHeaderFooter(boolean ignoreHeaderFooter) {
        this.ignoreHeaderFooter = ignoreHeaderFooter;
    }

    public double getHeaderHeightPercent() {
        return headerHeightPercent;
    }

    public void setHeaderHeightPercent(double headerHeightPercent) {
        this.headerHeightPercent = headerHeightPercent;
    }

    public double getFooterHeightPercent() {
        return footerHeightPercent;
    }

    public void setFooterHeightPercent(double footerHeightPercent) {
        this.footerHeightPercent = footerHeightPercent;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public String getIgnoredSymbols() {
        return ignoredSymbols;
    }

    public void setIgnoredSymbols(String ignoredSymbols) {
        this.ignoredSymbols = ignoredSymbols;
    }

    public boolean isIgnoreSpaces() {
        return ignoreSpaces;
    }

    public void setIgnoreSpaces(boolean ignoreSpaces) {
        this.ignoreSpaces = ignoreSpaces;
    }

    public boolean isIgnoreSeals() {
        return ignoreSeals;
    }

    public void setIgnoreSeals(boolean ignoreSeals) {
        this.ignoreSeals = ignoreSeals;
    }

    public boolean isRemoveWatermark() {
        return removeWatermark;
    }

    public void setRemoveWatermark(boolean removeWatermark) {
        this.removeWatermark = removeWatermark;
    }

    public String getWatermarkRemovalStrength() {
        return watermarkRemovalStrength;
    }

    public void setWatermarkRemovalStrength(String watermarkRemovalStrength) {
        this.watermarkRemovalStrength = watermarkRemovalStrength;
    }

    public String getOcrServiceType() {
        return ocrServiceType;
    }

    public void setOcrServiceType(String ocrServiceType) {
        this.ocrServiceType = ocrServiceType;
    }

    /**
     * 判断是否使用第三方OCR服务
     */
    public boolean isUseThirdPartyOcr() {
        return "thirdparty".equalsIgnoreCase(ocrServiceType);
    }

    /**
     * 判断是否使用DotsOCR服务
     */
    public boolean isUseDotsOcr() {
        return "dotsocr".equalsIgnoreCase(ocrServiceType);
    }
}