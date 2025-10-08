package com.zhaoxinms.contract.tools.comparePRO.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * ZXOCR配置类（高级合同比对功能）
 */
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ZxOcrConfig.class);
    
    /**
     * 默认OCR服务类型（全局配置）
     * 可选值: mineru, dotsocr, thirdparty
     * 默认: mineru
     */
    private String defaultOcrService = "mineru";

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
     * 渲染DPI（影响前端显示清晰度和识别精度）
     * 150: 快速预览，文件小但不清晰
     * 160: 平衡清晰度和文件大小（默认）
     * 200: 标准清晰度，适合屏幕显示（推荐配合 PNG）
     * 300: 高清显示，文件较大
     * 400+: 专业印刷级别
     */
    private int renderDpi = 160;
    
    /**
     * 图片格式（PNG 或 JPEG）
     * PNG: 无损格式，画布缩放时最清晰（推荐）
     * JPEG: 有损压缩，文件小但缩放时可能模糊
     */
    private String imageFormat = "PNG";
    
    /**
     * JPEG 质量（0.0-1.0，仅 JPEG 格式有效）
     * PNG 格式下此参数无效
     * 0.85: 推荐，文件小且质量好
     * 0.90: 高质量，文件稍大
     * 0.95: 接近无损
     */
    private float jpegQuality = 0.85f;

    /**
     * 是否保存OCR识别图片（默认关闭）
     */
    private boolean saveOcrImages = false;

    /**
     * MinerU OCR配置
     */
    private MinerUConfig mineru = new MinerUConfig();

    public String getDefaultOcrService() {
        return defaultOcrService;
    }

    public void setDefaultOcrService(String defaultOcrService) {
        this.defaultOcrService = defaultOcrService;
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

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public float getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(float jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public boolean isSaveOcrImages() {
        return saveOcrImages;
    }

    public void setSaveOcrImages(boolean saveOcrImages) {
        this.saveOcrImages = saveOcrImages;
    }

    public MinerUConfig getMineru() {
        return mineru;
    }

    public void setMineru(MinerUConfig mineru) {
        this.mineru = mineru;
    }

    /**
     * MinerU配置内部类
     */
    public static class MinerUConfig {
        /**
         * MinerU API地址
         */
        private String apiUrl = "http://192.168.0.100:8000";

        /**
         * vLLM Server地址
         */
        private String vllmServerUrl = "http://192.168.0.100:30000";

        /**
         * Backend模式：pipeline, vlm-http-client, vlm-vllm-async-engine等
         */
        private String backend = "vlm-http-client";

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getVllmServerUrl() {
            return vllmServerUrl;
        }

        public void setVllmServerUrl(String vllmServerUrl) {
            this.vllmServerUrl = vllmServerUrl;
        }

        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }

        @Override
        public String toString() {
            return "MinerUConfig{" +
                    "apiUrl='" + apiUrl + '\'' +
                    ", vllmServerUrl='" + vllmServerUrl + '\'' +
                    ", backend='" + backend + '\'' +
                    '}';
        }
    }
    
    /**
     * 配置加载完成后输出日志，用于验证配置是否正确加载
     */
    @PostConstruct
    public void logConfig() {
        log.info("╔════════════════════════════════════════════════════════════════");
        log.info("║ ZxOcrConfig 配置已加载 (来自: contract-tools-core)");
        log.info("╠════════════════════════════════════════════════════════════════");
        log.info("║ 📍 配置前缀: zxcm.compare.zxocr");
        log.info("║ 🎨 渲染DPI: {}", renderDpi);
        log.info("║ 🖼️  图片格式: {}", imageFormat);
        log.info("║ 📊 JPEG质量: {}", jpegQuality);
        log.info("║ 📁 上传路径: {}", uploadPath);
        log.info("║ 🔧 OCR服务: {} @ {}", defaultOcrService, ocrBaseUrl);
        log.info("╚════════════════════════════════════════════════════════════════");
    }
}
