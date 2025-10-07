package com.zhaoxinms.contract.tools.extract.providers.ollama;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ProviderException;
import com.zhaoxinms.contract.tools.extract.providers.LLMProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * Ollama LLM Provider实现
 * 支持本地部署的Ollama服务
 */
@Slf4j
public class OllamaLLMProvider implements LLMProvider {
    
    private final String baseUrl;
    private String model; // 移除final，允许动态切换模型
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // 默认配置
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final double DEFAULT_TEMPERATURE = 0.1;
    private static final int DEFAULT_MAX_TOKENS = 8000; // 为DeepSeek-R1等思考型模型提供更多输出空间
    
    public OllamaLLMProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
        
        log.info("初始化Ollama LLM Provider: baseUrl={}, model={}", baseUrl, model);
    }
    
    @Override
    public String chat(String prompt, String systemPrompt, Double temperature, Integer maxTokens) throws ProviderException {
        try {
            // 处理默认值
            double actualTemperature = (temperature != null) ? temperature : DEFAULT_TEMPERATURE;
            int actualMaxTokens = (maxTokens != null) ? maxTokens : DEFAULT_MAX_TOKENS;
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", this.model);
            
            // 如果有系统提示，将其与用户提示组合
            String fullPrompt = prompt;
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                fullPrompt = systemPrompt + "\n\n" + prompt;
            }
            requestBody.put("prompt", fullPrompt);
            requestBody.put("stream", false);
            
            // 添加选项
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", actualTemperature);
            options.put("num_predict", actualMaxTokens);
            requestBody.put("options", options);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "api/generate"))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            
            log.debug("发送Ollama API请求: model={}, prompt长度={}, maxTokens={}, temperature={}", 
                model, prompt.length(), actualMaxTokens, actualTemperature);
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new ProviderException("ollama", "HTTP_ERROR", 
                    "Ollama API请求失败: " + response.statusCode() + " - " + response.body());
            }
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.body());
            
            if (responseJson.has("error")) {
                String errorMessage = responseJson.get("error").asText();
                throw new ProviderException("ollama", "API_ERROR", "Ollama API错误: " + errorMessage);
            }
            
            if (!responseJson.has("response")) {
                throw new ProviderException("ollama", "RESPONSE_ERROR", "Ollama响应格式错误: 缺少response字段");
            }
            
            String result = responseJson.get("response").asText();
            log.debug("Ollama API调用成功，模型: {}, 响应长度: {}", model, result.length());
            
            // 检查响应是否可能被截断
            if (result.length() >= actualMaxTokens * 3) { // 估算字符数约为token数的3倍
                log.warn("⚠️ DeepSeek响应可能被截断！响应长度: {}, maxTokens: {}", result.length(), actualMaxTokens);
                log.warn("建议增加maxTokens配置以获得完整的思考过程和结果");
            }
            
            // 检查响应是否以不完整的思考标签结尾
            if (result.contains("<think>") && !result.contains("</think>")) {
                log.warn("⚠️ 检测到不完整的思考标签，响应可能被截断");
                log.debug("响应末尾100字符: {}", result.length() > 100 ? 
                    result.substring(result.length() - 100) : result);
            }
            
            return result;
            
        } catch (IOException e) {
            throw new ProviderException("ollama", "IO_ERROR", "网络请求失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("ollama", "INTERRUPTED", "请求被中断", e);
        } catch (Exception e) {
            throw new ProviderException("ollama", "UNKNOWN_ERROR", "未知错误: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public boolean isAvailable() {
        return isServiceAvailable();
    }
    
    @Override
    public String[] getSupportedModels() {
        return getAvailableModels();
    }
    
    @Override
    public void setModel(String model) {
        this.model = model;
        log.info("Ollama模型已切换到: {}", model);
    }
    
    @Override
    public String getCurrentModel() {
        return this.model;
    }
    
    /**
     * 检查Ollama服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            log.warn("Ollama服务检查失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取可用的模型列表
     */
    public String[] getAvailableModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "api/tags"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return new String[0];
            }
            
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.has("models")) {
                JsonNode models = responseJson.get("models");
                String[] modelNames = new String[models.size()];
                for (int i = 0; i < models.size(); i++) {
                    modelNames[i] = models.get(i).get("name").asText();
                }
                return modelNames;
            }
            
        } catch (Exception e) {
            log.warn("获取Ollama模型列表失败: {}", e.getMessage());
        }
        
        return new String[0];
    }
}
