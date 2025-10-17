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
 * AI服务默认实现 - 使用OkHttp手动调用API
 *
 * @author zhaoxinms
 */
@Slf4j
public class DefaultOpenAiServiceImpl implements OpenAiService {

    private final OkHttpClient okHttpClient;
    private final AiProperties aiProperties;
    private final AiLimitUtil aiLimitUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public DefaultOpenAiServiceImpl(OkHttpClient okHttpClient, AiProperties aiProperties, AiLimitUtil aiLimitUtil) {
        this.okHttpClient = okHttpClient;
        this.aiProperties = aiProperties;
        this.aiLimitUtil = aiLimitUtil;
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

        if (!aiLimitUtil.tryAcquire("system")) {
            return "请求过于频繁，请稍后再试";
        }

        try {
            Map<String, Object> requestMap = new HashMap<>();
            List<Map<String, Object>> formattedMessages = new ArrayList<>();
            
            for (ChatMessage message : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", message.getRole());
                
                // 处理多模态内容
                if (message.getContent() instanceof String) {
                    // 文本消息
                    msgMap.put("content", message.getContent());
                } else if (message.getContent() instanceof List) {
                    // 多模态消息（图片+文本）
                    msgMap.put("content", message.getContent());
                } else {
                    // 其他类型，转换为字符串
                    msgMap.put("content", String.valueOf(message.getContent()));
                }
                
                formattedMessages.add(msgMap);
            }

            requestMap.put("model", aiProperties.getModel().getMode());
            requestMap.put("messages", formattedMessages);
            requestMap.put("max_tokens", aiProperties.getModel().getMaxTokens());
            requestMap.put("temperature", aiProperties.getModel().getTemperature());
            requestMap.put("top_p", aiProperties.getModel().getTopP());

            String jsonBody = objectMapper.writeValueAsString(requestMap);
            log.debug("Sending request to AI API: {}", jsonBody);

            List<String> apiKeys = aiProperties.getApiKey();
            if (CollectionUtils.isEmpty(apiKeys)) {
                return "API密钥未配置";
            }
            String apiKey = apiKeys.get(new Random().nextInt(apiKeys.size()));

            Request request = new Request.Builder()
                    .url(aiProperties.getApiHost() + "/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                log.debug("AI API response: {}", responseBody);

                if (!response.isSuccessful()) {
                    return "API调用失败，状态码: " + response.code() + ", 响应: " + responseBody;
                }

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                    JsonNode choice = jsonNode.get("choices").get(0);
                    if (choice.has("message") && choice.get("message").has("content")) {
                        String content = choice.get("message").get("content").asText();
                        if (content != null && !content.trim().isEmpty()) {
                            return content;
                        }
                    }
                }

                return "AI返回结果为空";
            }
        } catch (Exception e) {
            log.error("调用AI API时发生错误: {}", e.getMessage(), e);
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