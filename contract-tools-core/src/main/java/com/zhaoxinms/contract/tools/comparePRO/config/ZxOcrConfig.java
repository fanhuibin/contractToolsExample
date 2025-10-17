package com.zhaoxinms.contract.tools.comparePRO.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * ZXOCR配置类（智能文档比对功能）
 */
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ZxOcrConfig.class);
    
    /**
     * ZXOCR API地址
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

    /**
     * 并行处理线程数（控制同时处理的比对任务数量）
     */
    private int parallelThreads = 20;

    /**
     * 渲染DPI（影响前端显示清晰度和识别精度）
     * 160: 平衡清晰度和文件大小（推荐）
     * 200: 标准清晰度，适合屏幕显示
     * 300: 高清显示，文件较大
     */
    private int renderDpi = 160;
    
    /**
     * 图片格式：PNG 无损格式
     */
    private String imageFormat = "PNG";

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
    
    /**
     * 配置加载完成后输出日志，用于验证配置是否正确加载
     */
    @PostConstruct
    public void logConfig() {
        log.info("╔════════════════════════════════════════════════════════════════");
        log.info("║ ZxOcrConfig 配置已加载");
        log.info("╠════════════════════════════════════════════════════════════════");
        log.info("║ 📍 配置前缀: zxcm.compare.zxocr");
        log.info("║ 🔧 API地址: {}", apiUrl);
        log.info("║ 🖥️  vLLM地址: {}", vllmServerUrl);
        log.info("║ ⚙️  Backend: {}", backend);
        log.info("║ 🎨 渲染DPI: {}", renderDpi);
        log.info("║ 🖼️  图片格式: {}", imageFormat);
        log.info("║ 🔀 并行线程: {}", parallelThreads);
        log.info("╚════════════════════════════════════════════════════════════════");
    }
}
