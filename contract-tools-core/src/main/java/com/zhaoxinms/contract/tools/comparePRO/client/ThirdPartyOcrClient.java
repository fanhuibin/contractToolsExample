package com.zhaoxinms.contract.tools.comparePRO.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * 第三方OCR客户端 - 基于阿里云Dashscope的图像识别服务
 * 
 * 使用阿里云通义千问VL模型进行图像文本识别
 * API格式遵循OpenAI兼容模式
 */
public class ThirdPartyOcrClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyOcrClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean verboseLogging;
    private final Semaphore concurrencyLimiter;
    private final int minPixels;
    private final int maxPixels;

    public static Builder builder() { 
        return new Builder(); 
    }

    public static class Builder {
        private String baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1";
        private String apiKey = null;
        private String defaultModel = "qwen3-vl-235b-a22b-instruct";
        private Duration timeout = Duration.ofMinutes(2);
        private OkHttpClient httpClient;
        private boolean verboseLogging = false;
        private int maxConcurrency = 10;
        private int minPixels = 512 * 32 * 32;  // 512*32*32
        private int maxPixels = 2048 * 32 * 32; // 2048*32*32

        public Builder baseUrl(String baseUrl) { 
            this.baseUrl = baseUrl; 
            return this; 
        }
        
        public Builder apiKey(String apiKey) { 
            this.apiKey = apiKey; 
            return this; 
        }
        
        public Builder defaultModel(String defaultModel) { 
            this.defaultModel = defaultModel; 
            return this; 
        }
        
        public Builder timeout(Duration timeout) { 
            this.timeout = timeout; 
            return this; 
        }
        
        public Builder httpClient(OkHttpClient httpClient) { 
            this.httpClient = httpClient; 
            return this; 
        }
        
        public Builder verboseLogging(boolean verboseLogging) { 
            this.verboseLogging = verboseLogging; 
            return this; 
        }
        
        public Builder maxConcurrency(int maxConcurrency) { 
            this.maxConcurrency = maxConcurrency; 
            return this; 
        }
        
        public Builder minPixels(int minPixels) { 
            this.minPixels = minPixels; 
            return this; 
        }
        
        public Builder maxPixels(int maxPixels) { 
            this.maxPixels = maxPixels; 
            return this; 
        }

        public ThirdPartyOcrClient build() {
            OkHttpClient client = this.httpClient != null ? this.httpClient : new OkHttpClient.Builder()
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .build();
            return new ThirdPartyOcrClient(baseUrl, apiKey, defaultModel, client, new ObjectMapper(), 
                    verboseLogging, maxConcurrency, minPixels, maxPixels);
        }
    }

    public ThirdPartyOcrClient(String baseUrl, String apiKey, String defaultModel, OkHttpClient httpClient, 
                              ObjectMapper objectMapper, boolean verboseLogging, int maxConcurrency, 
                              int minPixels, int maxPixels) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.defaultModel = defaultModel == null ? "qwen3-vl-235b-a22b-instruct" : defaultModel;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.verboseLogging = verboseLogging;
        int permits = maxConcurrency <= 0 ? 1 : maxConcurrency;
        this.concurrencyLimiter = new Semaphore(permits);
        this.minPixels = minPixels <= 0 ? 512 * 32 * 32 : minPixels;
        this.maxPixels = maxPixels <= 0 ? 2048 * 32 * 32 : maxPixels;
    }

    public String getBaseUrl() { 
        return baseUrl; 
    }
    
    public String getDefaultModel() { 
        return defaultModel; 
    }
    
    public int getMinPixels() { 
        return minPixels; 
    }
    
    public int getMaxPixels() { 
        return maxPixels; 
    }

    /**
     * 健康检查 - 通过调用模型列表API来验证服务可用性
     */
    public boolean health() throws IOException {
        try {
            // 尝试获取模型列表来验证API可用性
            listModels();
            return true;
        } catch (Exception e) {
            logger.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取可用模型列表
     */
    public List<String> listModels() throws IOException {
        Request.Builder rb = new Request.Builder()
                .url(baseUrl + "/models")
                .get()
                .addHeader("Authorization", "Bearer " + apiKey);
                
        try (Response resp = httpClient.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("GET /models failed: " + resp.code());
            }
            
            String body = resp.body() != null ? resp.body().string() : "{}";
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return Collections.emptyList();
            }
            
            List<String> ids = new ArrayList<>();
            for (JsonNode n : data) {
                if (n.hasNonNull("id")) {
                    ids.add(n.get("id").asText());
                }
            }
            return ids;
        }
    }

    /**
     * 调用聊天完成API
     */
    public JsonNode chatCompletions(JsonNode requestBody) throws IOException {
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody rb = RequestBody.create(json, JSON);
        Request.Builder rq = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(rb)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json");
                
        Request request = rq.build();
        if (verboseLogging) {
            logRequest("POST", request.url().toString(), request.headers(), json);
        }
        
        concurrencyLimiter.acquireUninterruptibly();
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (verboseLogging) {
                logResponse(resp.code(), resp.headers(), body);
            }
            
            if (!resp.isSuccessful()) {
                throw new IOException("POST /chat/completions failed: " + resp.code() + " - " + body);
            }
            
            String safeBody = body == null || body.isEmpty() ? "{}" : body;
            return objectMapper.readTree(safeBody);
        } finally {
            concurrencyLimiter.release();
        }
    }

    /**
     * 通过图像字节数组进行OCR识别
     * 
     * @param imageBytes 图像字节数组
     * @param prompt 提示词
     * @param model 模型名称，为null时使用默认模型
     * @param mimeType MIME类型，为null时使用image/jpeg
     * @param extractTextOnly 是否只提取文本内容
     * @return OCR识别结果
     */
    public String ocrImageBytes(byte[] imageBytes, String prompt, String model, String mimeType, boolean extractTextOnly) throws IOException {
        String mt = (mimeType == null || mimeType.isBlank()) ? "image/jpeg" : mimeType;
        String dataUrl = toDataUrl(imageBytes, mt);
        return ocrImageByUrl(dataUrl, prompt, model, extractTextOnly);
    }

    /**
     * 通过图像URL进行OCR识别
     * 
     * @param imageUrl 图像URL或data URL
     * @param prompt 提示词
     * @param model 模型名称，为null时使用默认模型
     * @param extractTextOnly 是否只提取文本内容
     * @return OCR识别结果
     */
    public String ocrImageByUrl(String imageUrl, String prompt, String model, boolean extractTextOnly) throws IOException {
        if (model == null || model.isBlank()) {
            model = this.defaultModel;
        }
        
        JsonNode req = buildChatCompletionRequest(model, prompt, imageUrl);
        JsonNode resp = chatCompletions(req);
        
        if (!extractTextOnly) {
            return resp.toString();
        }
        
        return extractAssistantText(resp);
    }

    /**
     * 使用默认的OCR提示词进行图像识别
     * 
     * @param imageBytes 图像字节数组
     * @param model 模型名称，为null时使用默认模型
     * @param mimeType MIME类型，为null时使用image/jpeg
     * @param extractTextOnly 是否只提取文本内容
     * @return OCR识别结果
     */
    public String ocrImageBytesWithDefaultPrompt(byte[] imageBytes, String model, String mimeType, boolean extractTextOnly) throws IOException {
        String prompt = buildDefaultOCRPrompt();
        return ocrImageBytes(imageBytes, prompt, model, mimeType, extractTextOnly);
    }

    /**
     * 使用默认的OCR提示词进行图像识别（通过URL）
     * 
     * @param imageUrl 图像URL
     * @param model 模型名称，为null时使用默认模型
     * @param extractTextOnly 是否只提取文本内容
     * @return OCR识别结果
     */
    public String ocrImageByUrlWithDefaultPrompt(String imageUrl, String model, boolean extractTextOnly) throws IOException {
        String prompt = buildDefaultOCRPrompt();
        return ocrImageByUrl(imageUrl, prompt, model, extractTextOnly);
    }

    /**
     * 构建第三方OCR默认提示词
     * 与小红书类似的提示词，但针对合同文档优化，输出第三方格式的JSON
     * 
     * @return 默认OCR提示词
     */
    public String buildDefaultOCRPrompt() {
        return "请识别图像中的所有文本内容，并按照阅读顺序输出JSON格式的结果。\n\n"
                + "输出要求：\n"
                + "1. 检测每个文本块的边界框坐标，格式为[x1, y1, x2, y2]\n"
                + "2. 识别文本块的类别，可选类别包括：['Section-header', 'Text', 'Title', 'List-item', 'Table', 'Caption', 'Footnote', 'Formula', 'Picture', 'Page-header', 'Page-footer']\n"
                + "3. 提取文本块内的准确文本内容\n\n"
                + "格式规则：\n"
                + "- 边界框：[x1, y1, x2, y2] 其中(x1,y1)为左上角，(x2,y2)为右下角\n"
                + "- 类别：根据文本的视觉特征和位置判断类别\n"
                + "- 文本：保持原始文本，不要翻译或修改\n"
                + "- Picture类别：文本字段可以省略\n"
                + "- Formula类别：以LaTeX格式表示\n"
                + "- Table类别：以纯文本格式表示，保持行列结构\n\n"
                + "输出格式：\n"
                + "```json\n"
                + "[\n"
                + "  {\n"
                + "    \"bbox_2d\": [x1, y1, x2, y2],\n"
                + "    \"category\": \"类别名称\",\n"
                + "    \"text\": \"文本内容\"\n"
                + "  }\n"
                + "]\n"
                + "```\n\n"
                + "注意：\n"
                + "- 必须按照人类阅读顺序排列所有文本块\n"
                + "- 输出的文本必须是图像中的原始文本，不得翻译\n"
                + "- 整个输出必须是有效的JSON数组格式\n"
                + "- 确保所有坐标和文本内容准确无误";
    }

    /**
     * 从响应中提取助手回复的文本内容
     */
    private String extractAssistantText(JsonNode resp) {
        if (resp == null) {
            logger.warn("响应为空，无法提取助手文本");
            return "";
        }
        
        // 输出完整的API响应用于调试
        logger.info("=== 阿里云API完整响应开始 ===");
        logger.info("API响应内容:\n{}", resp.toPrettyString());
        logger.info("=== 阿里云API完整响应结束 ===");
        
        JsonNode choices = resp.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode msg = choices.get(0).get("message");
            if (msg != null) {
                JsonNode content = msg.get("content");
                if (content != null && !content.isNull()) {
                    String extractedText = content.asText("");
                    logger.info("=== 提取的助手回复文本开始 ===");
                    logger.info("文本长度: {} 字符", extractedText.length());
                    logger.info("提取的文本内容:\n{}", extractedText);
                    logger.info("=== 提取的助手回复文本结束 ===");
                    return extractedText;
                }
            }
        }
        
        logger.warn("无法从标准格式提取助手文本，返回原始响应");
        return resp.toString();
    }

    /**
     * 构建聊天完成请求
     */
    private JsonNode buildChatCompletionRequest(String model, String prompt, String imageUrl) throws JsonProcessingException {
        Map<String, Object> root = new HashMap<>();
        root.put("model", model);
        root.put("max_tokens", 8000);  // 为JSON输出保留足够token
        root.put("temperature", 0.1f); // 低温度确保输出稳定
        root.put("top_p", 1.0f);

        List<Map<String, Object>> content = new ArrayList<>();
        
        // 添加图像部分
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", imageUrl);
        imageUrlMap.put("min_pixels", minPixels);
        imageUrlMap.put("max_pixels", maxPixels);
        imagePart.put("image_url", imageUrlMap);
        content.add(imagePart);
        
        // 添加文本提示词
        if (prompt != null && !prompt.isBlank()) {
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt);
            content.add(textPart);
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        root.put("messages", Collections.singletonList(userMsg));
        return objectMapper.valueToTree(root);
    }

    /**
     * 将字节数组转换为data URL
     */
    private static String toDataUrl(byte[] bytes, String mimeType) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeType + ";base64," + b64;
    }

    /**
     * 记录请求日志
     */
    private void logRequest(String method, String url, Headers headers, String body) {
        try {
            logger.info("ThirdPartyOCR Request -> {} {}", method, url);
            if (headers != null) {
                for (String name : headers.names()) {
                    String value = headers.get(name);
                    if ("Authorization".equalsIgnoreCase(name) && value != null) {
                        value = redactToken(value);
                    }
                    logger.info("  {}: {}", name, value);
                }
            }
            if (body != null && body.length() < 1000) { // 限制日志大小
                logger.info("Request Body:\n{}", body);
            } else {
                logger.info("Request Body: {} characters", body != null ? body.length() : 0);
            }
        } catch (Exception e) {
            logger.warn("Failed to log request: {}", e.getMessage());
        }
    }

    /**
     * 记录响应日志
     */
    private void logResponse(int code, Headers headers, String body) {
        try {
            logger.info("ThirdPartyOCR Response <- status {}", code);
            if (headers != null) {
                for (String name : headers.names()) {
                    logger.info("  {}: {}", name, headers.get(name));
                }
            }
            if (body != null && body.length() < 1000) { // 限制日志大小
                logger.info("Response Body:\n{}", body);
            } else {
                logger.info("Response Body: {} characters", body != null ? body.length() : 0);
            }
        } catch (Exception e) {
            logger.warn("Failed to log response: {}", e.getMessage());
        }
    }


    /**
     * 隐藏敏感token信息
     */
    private String redactToken(String value) {
        if (value == null) return null;
        int idx = value.indexOf(' ');
        if (idx > 0 && value.length() > idx + 6) {
            String scheme = value.substring(0, idx + 1);
            String tail = value.substring(idx + 1);
            String masked = tail.length() <= 8 ? "********" : tail.substring(0, 4) + "***" + tail.substring(tail.length() - 4);
            return scheme + masked;
        }
        return "***";
    }
}
