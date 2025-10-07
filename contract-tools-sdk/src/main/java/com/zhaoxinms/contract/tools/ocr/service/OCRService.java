package com.zhaoxinms.contract.tools.ocr.service;

import java.io.File;
import java.util.List;

/**
 * OCR识别服务接口
 * 统一封装阿里云OCR和DotSOCR的识别能力
 */
public interface OCRService {
    
    /**
     * OCR识别结果
     */
    class OCRResult {
        private String content;
        private double confidence;
        private List<OCRBlock> blocks;
        private String provider;
        
        public OCRResult(String content, double confidence, String provider) {
            this.content = content;
            this.confidence = confidence;
            this.provider = provider;
        }
        
        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public List<OCRBlock> getBlocks() { return blocks; }
        public void setBlocks(List<OCRBlock> blocks) { this.blocks = blocks; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }
    
    /**
     * OCR识别的文本块
     */
    class OCRBlock {
        private String text;
        private double confidence;
        private int x, y, width, height;
        
        public OCRBlock(String text, double confidence, int x, int y, int width, int height) {
            this.text = text;
            this.confidence = confidence;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        // Getters and setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
    
    /**
     * 识别PDF文件中的文本
     * 
     * @param pdfFile PDF文件
     * @return OCR识别结果
     */
    OCRResult recognizePdf(File pdfFile);
    
    /**
     * 识别图片文件中的文本
     * 
     * @param imageFile 图片文件
     * @return OCR识别结果
     */
    OCRResult recognizeImage(File imageFile);
    
    /**
     * 获取OCR提供者名称
     * 
     * @return 提供者名称（如 "aliyun", "dotsocr"）
     */
    String getProviderName();
    
    /**
     * 检查服务是否可用
     * 
     * @return true如果服务可用
     */
    boolean isAvailable();
}
