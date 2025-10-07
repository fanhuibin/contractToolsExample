package com.zhaoxinms.contract.tools.extract.providers;

import com.zhaoxinms.contract.tools.extract.core.exceptions.ProviderException;

/**
 * LLM提供商接口
 * 定义与语言模型交互的统一接口
 */
public interface LLMProvider {
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 发送聊天请求
     * 
     * @param prompt 提示文本
     * @param systemPrompt 系统提示（可选）
     * @param temperature 温度参数，控制随机性 (0.0-1.0)
     * @param maxTokens 最大token数量
     * @return 模型响应文本
     * @throws ProviderException 提供商异常
     */
    String chat(String prompt, String systemPrompt, Double temperature, Integer maxTokens) throws ProviderException;
    
    /**
     * 发送聊天请求（简化版本）
     */
    default String chat(String prompt) throws ProviderException {
        return chat(prompt, null, null, null);
    }
    
    /**
     * 发送聊天请求（带系统提示）
     */
    default String chat(String prompt, String systemPrompt) throws ProviderException {
        return chat(prompt, systemPrompt, null, null);
    }
    
    /**
     * 检查提供商是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取支持的模型列表
     */
    String[] getSupportedModels();
    
    /**
     * 设置使用的模型
     */
    void setModel(String model);
    
    /**
     * 获取当前使用的模型
     */
    String getCurrentModel();
}
