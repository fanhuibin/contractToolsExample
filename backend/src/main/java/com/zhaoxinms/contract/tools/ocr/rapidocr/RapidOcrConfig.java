package com.zhaoxinms.contract.tools.ocr.rapidocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RapidOCR配置类
 */
@ConfigurationProperties(prefix = "rapidocr")
public class RapidOcrConfig {

    /**
     * RapidOCR API服务地址
     */
    private String baseUrl = "http://192.168.0.100:9005";

    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofMinutes(2);

    /**
     * 是否开启详细日志
     */
    private boolean verboseLogging = false;

    /**
     * 最大并发数
     */
    private int maxConcurrency = 10;

    /**
     * 是否启用RapidOCR（总开关）
     */
    private boolean enabled = true;

    /**
     * 默认是否使用文本检测
     */
    private boolean defaultUseDetection = true;

    /**
     * 默认是否使用方向分类
     */
    private boolean defaultUseClassification = true;

    /**
     * 默认是否使用文本识别
     */
    private boolean defaultUseRecognition = true;

    // Getters and Setters

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefaultUseDetection() {
        return defaultUseDetection;
    }

    public void setDefaultUseDetection(boolean defaultUseDetection) {
        this.defaultUseDetection = defaultUseDetection;
    }

    public boolean isDefaultUseClassification() {
        return defaultUseClassification;
    }

    public void setDefaultUseClassification(boolean defaultUseClassification) {
        this.defaultUseClassification = defaultUseClassification;
    }

    public boolean isDefaultUseRecognition() {
        return defaultUseRecognition;
    }

    public void setDefaultUseRecognition(boolean defaultUseRecognition) {
        this.defaultUseRecognition = defaultUseRecognition;
    }

    @Override
    public String toString() {
        return "RapidOcrConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", timeout=" + timeout +
                ", verboseLogging=" + verboseLogging +
                ", maxConcurrency=" + maxConcurrency +
                ", enabled=" + enabled +
                ", defaultUseDetection=" + defaultUseDetection +
                ", defaultUseClassification=" + defaultUseClassification +
                ", defaultUseRecognition=" + defaultUseRecognition +
                '}';
    }
}
