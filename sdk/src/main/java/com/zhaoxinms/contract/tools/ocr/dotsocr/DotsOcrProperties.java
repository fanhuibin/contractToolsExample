package com.zhaoxinms.contract.tools.ocr.dotsocr;

/**
 * 配置项：Dots.OCR 服务连接参数
 */
public class DotsOcrProperties {

    /** 基础地址，例如 http://localhost:8000 */
    private String baseUrl = "http://localhost:8000";

    /** 默认模型名，例如 dots.ocr */
    private String model = "dots.ocr";

    /** 可选：Bearer Token */
    private String apiKey;

    /** 请求超时（秒） */
    private int timeoutSeconds = 60;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}


