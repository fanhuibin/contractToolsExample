package com.zhaoxinms.contract.tools.ocrcompare.compare;

/**
 * GPU OCR比对选项
 */
public class GPUOCRCompareOptions {

    private boolean ignoreHeaderFooter = false;
    private double headerHeightPercent = 10;
    private double footerHeightPercent = 10;
    private boolean ignoreCase = true;
    private String ignoredSymbols = "_＿";
    private boolean ignoreSpaces = false;
    private boolean ignoreSeals = true;

    public GPUOCRCompareOptions() {}

    public static GPUOCRCompareOptions createDefault() {
        return new GPUOCRCompareOptions();
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
}
