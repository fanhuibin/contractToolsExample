package com.zhaoxinms.contract.tools.comparePRO.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ZXOCR配置类（高级合同比对功能）
 */
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {

    /**
     * 调试模式文件路径
     */
    private String debugFilePath = "./uploads/debug";

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
     * 是否保存OCR识别图片（默认关闭）
     */
    private boolean saveOcrImages = false;

    /**
     * 幻觉校验配置
     */
    private HallucinationValidation hallucinationValidation = new HallucinationValidation();

    /**
     * 幻觉校验配置内部类
     */
    public static class HallucinationValidation {
        /**
         * 是否启用幻觉校验功能
         */
        private boolean enabled = true;

        /**
         * 页数阈值倍数
         * 当符合条件的差异块数量超过 总页数*此倍数 时，跳过RapidOCR校验
         */
        private int pageThresholdMultiplier = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPageThresholdMultiplier() {
            return pageThresholdMultiplier;
        }

        public void setPageThresholdMultiplier(int pageThresholdMultiplier) {
            this.pageThresholdMultiplier = pageThresholdMultiplier;
        }

        @Override
        public String toString() {
            return "HallucinationValidation{" +
                    "enabled=" + enabled +
                    ", pageThresholdMultiplier=" + pageThresholdMultiplier +
                    '}';
        }
    }

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

    public boolean isSaveOcrImages() {
        return saveOcrImages;
    }

    public void setSaveOcrImages(boolean saveOcrImages) {
        this.saveOcrImages = saveOcrImages;
    }

    public HallucinationValidation getHallucinationValidation() {
        return hallucinationValidation;
    }

    public void setHallucinationValidation(HallucinationValidation hallucinationValidation) {
        this.hallucinationValidation = hallucinationValidation;
    }
}
