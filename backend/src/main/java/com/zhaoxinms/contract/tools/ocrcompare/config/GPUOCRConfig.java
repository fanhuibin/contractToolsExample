package com.zhaoxinms.contract.tools.ocrcompare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GPU OCR配置类
 */
@Configuration
@ConfigurationProperties(prefix = "gpu.ocr")
public class GPUOCRConfig {

    /**
     * 调试模式文件路径
     */
    private String debugFilePath = "C:\\Users\\范慧斌\\Desktop\\hetong比对前端";

    /**
     * OCR服务基础URL
     */
    private String ocrBaseUrl = "http://192.168.0.100:8000";

    /**
     * OCR模型名称
     */
    private String ocrModel = "dots.ocr";

    /**
     * 文件上传路径
     */
    private String uploadPath = "./uploads";

    /**
     * OCR比较结果存储路径
     */
    private String resultPath = "./uploads/ocr-compare/results";

    /**
     * 是否保存渲染的图像
     */
    private boolean saveRenderedImages = false;

    /**
     * 并行处理线程数
     */
    private int parallelThreads = 4;

    public String getDebugFilePath() {
        return debugFilePath;
    }

    public void setDebugFilePath(String debugFilePath) {
        this.debugFilePath = debugFilePath;
    }

    public String getOcrBaseUrl() {
        return ocrBaseUrl;
    }

    public void setOcrBaseUrl(String ocrBaseUrl) {
        this.ocrBaseUrl = ocrBaseUrl;
    }

    public String getOcrModel() {
        return ocrModel;
    }

    public void setOcrModel(String ocrModel) {
        this.ocrModel = ocrModel;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    public boolean isSaveRenderedImages() {
        return saveRenderedImages;
    }

    public void setSaveRenderedImages(boolean saveRenderedImages) {
        this.saveRenderedImages = saveRenderedImages;
    }

    public int getParallelThreads() {
        return parallelThreads;
    }

    public void setParallelThreads(int parallelThreads) {
        this.parallelThreads = parallelThreads;
    }
}
