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
    private String ocrModel = "model";

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

    /**
     * 渲染DPI（影响识别清晰度，demo一般较高）
     */
    private int renderDpi = 200;

    /**
     * 最小像素总数（小于则按比例放大，0 表示不启用）
     */
    private long minPixels = 3136;

    /**
     * 最大像素总数（大于则按比例缩小，建议 11289600）
     */
    private long maxPixels = 11289600;

    /**
     * 是否通过 Gradio 队列服务调用（demo_gradio.py）
     */
    private boolean useGradioQueue = false;

    /**
     * Gradio 基础地址，例如 http://192.168.0.100:80
     */
    private String gradioBaseUrl = "http://192.168.0.100:80";

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

    public int getRenderDpi() {
        return renderDpi;
    }

    public void setRenderDpi(int renderDpi) {
        this.renderDpi = renderDpi;
    }

    public long getMinPixels() {
        return minPixels;
    }

    public void setMinPixels(long minPixels) {
        this.minPixels = minPixels;
    }

    public long getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(long maxPixels) {
        this.maxPixels = maxPixels;
    }

    public boolean isUseGradioQueue() {
        return useGradioQueue;
    }

    public void setUseGradioQueue(boolean useGradioQueue) {
        this.useGradioQueue = useGradioQueue;
    }

    public String getGradioBaseUrl() {
        return gradioBaseUrl;
    }

    public void setGradioBaseUrl(String gradioBaseUrl) {
        this.gradioBaseUrl = gradioBaseUrl;
    }
}
