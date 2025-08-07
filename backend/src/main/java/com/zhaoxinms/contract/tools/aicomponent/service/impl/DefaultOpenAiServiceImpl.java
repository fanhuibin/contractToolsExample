package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.constants.AiConstants;
import com.zhaoxinms.contract.tools.aicomponent.service.ChatMessage;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI服务默认实现
 *
 * @author zhaoxinms
 */
@Slf4j
public class DefaultOpenAiServiceImpl implements OpenAiService {

    private final OkHttpClient okHttpClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public DefaultOpenAiServiceImpl(OkHttpClient okHttpClient, AiProperties aiProperties) {
        this.okHttpClient = okHttpClient;
        this.aiProperties = aiProperties;
    }

    @Override
    public String completion(String prompt) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessage.ROLE_USER, prompt));
        return completion(messages);
    }

    @Override
    public String completion(List<ChatMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return "请输入内容";
        }

        if (!AiLimitUtil.tryAcquire("system")) {
            return "请求过于频繁，请稍后再试";
        }

        try {
            // 构建请求体
            Map<String, Object> requestMap = new HashMap<>();
            
            // 设置模型参数
            Map<String, Object> modelParams = new HashMap<>();
            modelParams.put("model", aiProperties.getChat().getMode());
            
            // 转换消息格式
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (ChatMessage message : messages) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", message.getRole());
                msgMap.put("content", message.getContent());
                formattedMessages.add(msgMap);
            }
            
            modelParams.put("messages", formattedMessages);
            modelParams.put("max_tokens", aiProperties.getChat().getMaxTokens());
            modelParams.put("temperature", aiProperties.getChat().getTemperature());
            modelParams.put("seed", aiProperties.getChat().getSeed());
            modelParams.put("top_p", aiProperties.getChat().getTopP());
            
            requestMap.put("model", modelParams);
            
            String jsonBody = objectMapper.writeValueAsString(requestMap);
            log.debug("Sending request to AI API: {}", jsonBody);

            // 获取API密钥
            List<String> apiKeys = aiProperties.getApiKey();
            if (CollectionUtils.isEmpty(apiKeys)) {
                return "API密钥未配置";
            }
            
            // 随机选择一个API密钥
            String apiKey = apiKeys.get(new Random().nextInt(apiKeys.size()));

            // 构建请求
            Request request = new Request.Builder()
                    .url(aiProperties.getApiHost())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            // 发送请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                log.debug("Received response from AI API: {}", responseBody);
                
                if (!response.isSuccessful()) {
                    log.error("API call failed with code: {}, response: {}", response.code(), responseBody);
                    return "API调用失败，错误码: " + response.code();
                }
                
                // 解析响应
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode outputNode = rootNode.path("output");
                String content = outputNode.path("text").asText();
                
                return content;
            }
        } catch (Exception e) {
            log.error("Error in AI completion", e);
            return "AI服务异常: " + e.getMessage();
        }
    }

    @Override
    public String generatorModelStr(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            return "请提供业务名称";
        }
        
        String prompt = AiConstants.GEN_MODEL_QUETION + "\n\n业务需求：" + businessName;
        return completion(prompt);
    }

    @Override
    public String extractTextFromPdf(byte[] pdfContent) {
        if (pdfContent == null || pdfContent.length == 0) {
            return "PDF内容为空";
        }
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfContent))) {
            // 检查页数限制
            int pageCount = document.getNumberOfPages();
            if (pageCount > aiProperties.getPdf().getMaxPages()) {
                return "PDF页数超过限制：" + aiProperties.getPdf().getMaxPages() + "页";
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("Error extracting text from PDF", e);
            return "PDF文本提取失败: " + e.getMessage();
        }
    }
}