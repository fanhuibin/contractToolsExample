package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 第三方OCR服务配置
 * 基于阿里云Dashscope的配置参数
 */
@ConfigurationProperties(prefix = "zxcm.compare.third-party-ocr")
public class ThirdPartyOcrConfig {

    /**
     * 是否启用第三方OCR服务
     */
    private boolean enabled = false;

    /**
     * API基础URL
     */
    private String baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1";

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 默认模型名称
     */
    private String defaultModel = "qwen3-vl-235b-a22b-instruct";

    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofMinutes(2);

    /**
     * 最大并发数
     */
    private int maxConcurrency = 5;

    /**
     * 是否启用详细日志
     */
    private boolean verboseLogging = false;

    /**
     * 最小像素数 (像素数量)
     */
    private int minPixels = 512 * 32 * 32;

    /**
     * 最大像素数 (像素数量)
     */
    private int maxPixels = 2048 * 32 * 32;

    /**
     * 图像处理配置
     */
    private ImageProcessing imageProcessing = new ImageProcessing();

    public static class ImageProcessing {
        /**
         * 渲染DPI
         */
        private int renderDpi = 160;

        /**
         * 保存OCR图像
         */
        private boolean saveOcrImages = true;

        public int getRenderDpi() {
            return renderDpi;
        }

        public void setRenderDpi(int renderDpi) {
            this.renderDpi = renderDpi;
        }

        public boolean isSaveOcrImages() {
            return saveOcrImages;
        }

        public void setSaveOcrImages(boolean saveOcrImages) {
            this.saveOcrImages = saveOcrImages;
        }
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    public int getMinPixels() {
        return minPixels;
    }

    public void setMinPixels(int minPixels) {
        this.minPixels = minPixels;
    }

    public int getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(int maxPixels) {
        this.maxPixels = maxPixels;
    }

    public ImageProcessing getImageProcessing() {
        return imageProcessing;
    }

    public void setImageProcessing(ImageProcessing imageProcessing) {
        this.imageProcessing = imageProcessing;
    }
}
