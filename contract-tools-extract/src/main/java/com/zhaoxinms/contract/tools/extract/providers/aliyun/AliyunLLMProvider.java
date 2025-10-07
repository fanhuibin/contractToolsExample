package com.zhaoxinms.contract.tools.extract.providers.aliyun;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ProviderException;
import com.zhaoxinms.contract.tools.extract.providers.LLMProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * 阿里云通义千问LLM提供商
 * 实现与阿里云DashScope API的对接
 */
@Slf4j
public class AliyunLLMProvider implements LLMProvider {
    
    private static final String PROVIDER_NAME = "Aliyun-DashScope";
    
    // 支持的模型列表
    private static final String[] SUPPORTED_MODELS = {
        "qwen-turbo",
        "qwen-plus", 
        "qwen-max",
        "qwen-max-1201",
        "qwen-max-longcontext"
    };
    
    private final String apiKey;
    private String currentModel;
    private final Generation generation;
    
    // 默认参数
    private static final Double DEFAULT_TEMPERATURE = 0.7;
    private static final Integer DEFAULT_MAX_TOKENS = 2000;
    
    public AliyunLLMProvider(String apiKey) {
        this(apiKey, SUPPORTED_MODELS[0]); // 默认使用qwen-turbo
    }
    
    public AliyunLLMProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.currentModel = model;
        this.generation = new Generation();
        
        // 设置API Key
        System.setProperty("DASHSCOPE_API_KEY", apiKey);
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public String chat(String prompt, String systemPrompt, Double temperature, Integer maxTokens) throws ProviderException {
        try {
            List<Message> messages = new ArrayList<>();
            
            // 添加系统提示
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build());
            }
            
            // 添加用户提示
            messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build());
            
            // 构建请求参数
            GenerationParam param = GenerationParam.builder()
                .model(currentModel)
                .messages(messages)
                .temperature(temperature != null ? temperature.floatValue() : DEFAULT_TEMPERATURE.floatValue())
                .maxTokens(maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .apiKey(apiKey)  // 显式设置API Key
                .build();
            
            // 发送请求
            GenerationResult result = generation.call(param);
            
            // 检查响应
            if (result == null || result.getOutput() == null || result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
                throw new ProviderException(PROVIDER_NAME, "API返回空响应");
            }
            
            // 提取响应内容
            String responseContent = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            log.debug("阿里云API调用成功，模型: {}, 响应长度: {}", currentModel, responseContent.length());
            
            return responseContent;
            
        } catch (ApiException e) {
            log.error("阿里云API调用失败: {}", e.getMessage());
            throw new ProviderException(PROVIDER_NAME, "API_ERROR", "API调用失败: " + e.getMessage(), e);
        } catch (NoApiKeyException e) {
            log.error("阿里云API Key未设置: {}", e.getMessage());
            throw new ProviderException(PROVIDER_NAME, "API_KEY_MISSING", "API Key未设置或无效", e);
        } catch (InputRequiredException e) {
            log.error("阿里云API请求参数错误: {}", e.getMessage());
            throw new ProviderException(PROVIDER_NAME, "INVALID_INPUT", "请求参数错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("阿里云API调用异常: {}", e.getMessage());
            throw new ProviderException(PROVIDER_NAME, "UNKNOWN_ERROR", "未知错误: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 发送一个简单的测试请求
            String testResponse = chat("测试连接", "你是一个AI助手", 0.1, 10);
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("阿里云LLM提供商不可用: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String[] getSupportedModels() {
        return SUPPORTED_MODELS.clone();
    }
    
    @Override
    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        
        // 检查模型是否支持
        boolean supported = Arrays.asList(SUPPORTED_MODELS).contains(model);
        if (!supported) {
            log.warn("模型 {} 可能不受支持，支持的模型: {}", model, Arrays.toString(SUPPORTED_MODELS));
        }
        
        this.currentModel = model;
        log.info("切换到模型: {}", model);
    }
    
    @Override
    public String getCurrentModel() {
        return currentModel;
    }
    
    /**
     * 获取API Key（用于测试）
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 构建器类
     */
    public static class Builder {
        private String apiKey;
        private String model = SUPPORTED_MODELS[0];
        
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public AliyunLLMProvider build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API Key不能为空");
            }
            return new AliyunLLMProvider(apiKey, model);
        }
    }
}
