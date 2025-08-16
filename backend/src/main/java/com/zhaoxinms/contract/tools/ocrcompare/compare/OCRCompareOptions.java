package com.zhaoxinms.contract.tools.ocrcompare.compare;

/**
 * OCR比对选项配置类
 */
public class OCRCompareOptions {
    
    private boolean ignoreCase = false;           // 是否忽略大小写
    private boolean ignoreWhitespace = false;    // 是否忽略空白字符
    private boolean ignorePunctuation = false;   // 是否忽略标点符号
    private boolean ignoreHeaderFooter = true;   // 是否忽略页眉页脚
    private boolean ignoreSpaces = false;        // 是否忽略空格
    private double similarityThreshold = 0.8;    // 相似度阈值
    private int maxDiffLength = 1000;            // 最大差异长度
    private boolean enableHighlight = true;      // 是否启用高亮显示
    private String highlightColor = "#FFD700";   // 高亮颜色
    private boolean saveAnnotatedPdf = true;     // 是否保存标注后的PDF
    
    public OCRCompareOptions() {}
    
    public OCRCompareOptions(boolean ignoreCase, boolean ignoreWhitespace, 
                           boolean ignorePunctuation, double similarityThreshold) {
        this.ignoreCase = ignoreCase;
        this.ignoreWhitespace = ignoreWhitespace;
        this.ignorePunctuation = ignorePunctuation;
        this.similarityThreshold = similarityThreshold;
    }
    
    // Getters and Setters
    public boolean isIgnoreCase() { return ignoreCase; }
    public void setIgnoreCase(boolean ignoreCase) { this.ignoreCase = ignoreCase; }
    
    public boolean isIgnoreWhitespace() { return ignoreWhitespace; }
    public void setIgnoreWhitespace(boolean ignoreWhitespace) { this.ignoreWhitespace = ignoreWhitespace; }
    
    public boolean isIgnorePunctuation() { return ignorePunctuation; }
    public void setIgnorePunctuation(boolean ignorePunctuation) { this.ignorePunctuation = ignorePunctuation; }
    
    public boolean isIgnoreHeaderFooter() { return ignoreHeaderFooter; }
    public void setIgnoreHeaderFooter(boolean ignoreHeaderFooter) { this.ignoreHeaderFooter = ignoreHeaderFooter; }
    
    public boolean isIgnoreSpaces() { return ignoreSpaces; }
    public void setIgnoreSpaces(boolean ignoreSpaces) { this.ignoreSpaces = ignoreSpaces; }
    
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    
    public int getMaxDiffLength() { return maxDiffLength; }
    public void setMaxDiffLength(int maxDiffLength) { this.maxDiffLength = maxDiffLength; }
    
    public boolean isEnableHighlight() { return enableHighlight; }
    public void setEnableHighlight(boolean enableHighlight) { this.enableHighlight = enableHighlight; }
    
    public String getHighlightColor() { return highlightColor; }
    public void setHighlightColor(String highlightColor) { this.highlightColor = highlightColor; }
    
    public boolean isSaveAnnotatedPdf() { return saveAnnotatedPdf; }
    public void setSaveAnnotatedPdf(boolean saveAnnotatedPdf) { this.saveAnnotatedPdf = saveAnnotatedPdf; }
    
    /**
     * 创建默认比对选项
     */
    public static OCRCompareOptions createDefault() {
        return new OCRCompareOptions();
    }
    
    /**
     * 创建严格比对选项
     */
    public static OCRCompareOptions createStrict() {
        OCRCompareOptions options = new OCRCompareOptions();
        options.setIgnoreCase(false);
        options.setIgnoreWhitespace(false);
        options.setIgnorePunctuation(false);
        options.setSimilarityThreshold(0.95);
        return options;
    }
    
    /**
     * 创建宽松比对选项
     */
    public static OCRCompareOptions createLoose() {
        OCRCompareOptions options = new OCRCompareOptions();
        options.setIgnoreCase(true);
        options.setIgnoreWhitespace(true);
        options.setIgnorePunctuation(true);
        options.setSimilarityThreshold(0.7);
        return options;
    }
    
    @Override
    public String toString() {
        return "OCRCompareOptions{" +
                "ignoreCase=" + ignoreCase +
                ", ignoreWhitespace=" + ignoreWhitespace +
                ", ignorePunctuation=" + ignorePunctuation +
                ", ignoreHeaderFooter=" + ignoreHeaderFooter +
                ", ignoreSpaces=" + ignoreSpaces +
                ", similarityThreshold=" + similarityThreshold +
                ", maxDiffLength=" + maxDiffLength +
                ", enableHighlight=" + enableHighlight +
                ", highlightColor='" + highlightColor + '\'' +
                ", saveAnnotatedPdf=" + saveAnnotatedPdf +
                '}';
    }
}
