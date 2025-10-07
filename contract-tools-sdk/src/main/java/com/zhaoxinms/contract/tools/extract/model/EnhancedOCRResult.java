package com.zhaoxinms.contract.tools.extract.model;

import java.util.List;

/**
 * 增强的OCR结果
 * 包含文本内容和详细的位置信息
 */
public class EnhancedOCRResult {
    
    /**
     * 识别出的纯文本内容
     */
    private final String content;
    
    /**
     * OCR提供者
     */
    private final String provider;
    
    /**
     * 字符级别的位置信息
     */
    private final List<CharBox> charBoxes;
    
    /**
     * 图片保存路径（如果有）
     */
    private final String imagesPath;
    
    /**
     * 总页数
     */
    private final int totalPages;

    public EnhancedOCRResult(String content, String provider, List<CharBox> charBoxes, String imagesPath, int totalPages) {
        this.content = content;
        this.provider = provider;
        this.charBoxes = charBoxes;
        this.imagesPath = imagesPath;
        this.totalPages = totalPages;
    }

    public String getContent() {
        return content;
    }

    public String getProvider() {
        return provider;
    }

    public List<CharBox> getCharBoxes() {
        return charBoxes;
    }

    public String getImagesPath() {
        return imagesPath;
    }

    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public String toString() {
        return String.format("EnhancedOCRResult{provider='%s', contentLength=%d, charBoxes=%d, totalPages=%d, imagesPath='%s'}", 
            provider, content != null ? content.length() : 0, charBoxes != null ? charBoxes.size() : 0, totalPages, imagesPath);
    }
}
