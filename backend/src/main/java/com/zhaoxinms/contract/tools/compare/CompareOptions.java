package com.zhaoxinms.contract.tools.compare;

/**
 * 合同比对参数配置
 */
public class CompareOptions {
    // 版心设置（用于换算页眉/页脚占比；默认为 A4 高度 297mm）
    private float pageHeightMm = 297f;
    // 是否忽略页眉页脚
    private boolean ignoreHeaderFooter = true;
    // 页眉高度（mm）
    private float headerHeightMm = 20f;
    // 页脚高度（mm）
    private float footerHeightMm = 20f;
    // 是否忽略大小写
    private boolean ignoreCase = true;
    // 是否忽略仅符号/空白的差异（compare 阶段不用，保持 false 以免错位）
    private boolean ignoreSymbols = false;
    // 结果过滤：完全由这些符号组成的差异将被忽略（不返回、不标注）。不影响位置计算。
    // 默认忽略下划线（含半角与全角）
    private String ignoredSymbols = "_＿";
    // 新增（标注到新文档）的高亮颜色 RGB（0~1）
    private float[] insertRGB = new float[]{0.30f, 0.85f, 0.39f}; // 近似 #52c41a
    // 删除（标注到旧文档）的高亮颜色 RGB（0~1）
    private float[] deleteRGB = new float[]{1.00f, 0.30f, 0.31f}; // 近似 #ff4d4f

    public float getPageHeightMm() {
        return pageHeightMm;
    }

    public CompareOptions setPageHeightMm(float pageHeightMm) {
        this.pageHeightMm = pageHeightMm;
        return this;
    }

    public boolean isIgnoreHeaderFooter() {
        return ignoreHeaderFooter;
    }

    public CompareOptions setIgnoreHeaderFooter(boolean ignoreHeaderFooter) {
        this.ignoreHeaderFooter = ignoreHeaderFooter;
        return this;
    }

    public float getHeaderHeightMm() {
        return headerHeightMm;
    }

    public CompareOptions setHeaderHeightMm(float headerHeightMm) {
        this.headerHeightMm = headerHeightMm;
        return this;
    }

    public float getFooterHeightMm() {
        return footerHeightMm;
    }

    public CompareOptions setFooterHeightMm(float footerHeightMm) {
        this.footerHeightMm = footerHeightMm;
        return this;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public CompareOptions setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }

    public boolean isIgnoreSymbols() {
        return ignoreSymbols;
    }

    public CompareOptions setIgnoreSymbols(boolean ignoreSymbols) {
        this.ignoreSymbols = ignoreSymbols;
        return this;
    }

    public String getIgnoredSymbols() {
        return ignoredSymbols;
    }

    public CompareOptions setIgnoredSymbols(String ignoredSymbols) {
        this.ignoredSymbols = (ignoredSymbols == null) ? "" : ignoredSymbols;
        return this;
    }

    public float[] getInsertRGB() {
        return insertRGB;
    }

    public CompareOptions setInsertRGB(float r, float g, float b) {
        this.insertRGB = new float[]{r, g, b};
        return this;
    }

    public float[] getDeleteRGB() {
        return deleteRGB;
    }

    public CompareOptions setDeleteRGB(float r, float g, float b) {
        this.deleteRGB = new float[]{r, g, b};
        return this;
    }

    public float headerRatio() {
        return safeRatio(headerHeightMm);
    }

    public float footerStartRatio() {
        return (pageHeightMm - headerFooterClamp(footerHeightMm)) / pageHeightMm;
    }

    private float safeRatio(float mm) {
        float h = pageHeightMm <= 0 ? 297f : pageHeightMm;
        return headerFooterClamp(mm) / h;
    }

    private float headerFooterClamp(float mm) {
        if (mm < 0) return 0f;
        // 不做上限强制，交给调用处根据页面尺寸自行控制
        return mm;
    }
}


