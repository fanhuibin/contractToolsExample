package com.zhaoxinms.contract.tools.ocr.dotsocr;

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
import java.nio.charset.StandardCharsets;
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
 * Dots.OCR OpenAI-compatible client.
 *
 * Endpoints:
 * - GET /health           → service health
 * - GET /v1/models       → available models
 * - POST /v1/chat/completions → OCR via VLM messages (text + image_url)
 * - GET /metrics         → Prometheus metrics (text/plain)
 *
 * Reference:
 * - https://www.dotsocr.net/blog/2
 * - https://github.com/rednote-hilab/dots.ocr
 */
public class DotsOcrClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(DotsOcrClient.class);

    private final String baseUrl;
    private final String apiKey; // optional, for deployments requiring auth
    private final String defaultModel;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean verboseLogging;
    private final Semaphore concurrencyLimiter;
    // Rendering preferences for PDF→image (used by callers/tests)
    private final int renderDpi;
    private final boolean saveRenderedImages;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String baseUrl = "http://192.168.0.100:8000";
        private String apiKey = null;
        private String defaultModel = "dots.ocr";
        private Duration timeout = Duration.ofMinutes(5);
        private OkHttpClient httpClient;
        private boolean verboseLogging = false;
        private int maxConcurrency = 10;
        private Integer renderDpi = 150; // default 150 dpi
        private Boolean saveRenderedImages = false; // default not save

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder defaultModel(String defaultModel) { this.defaultModel = defaultModel; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder httpClient(OkHttpClient httpClient) { this.httpClient = httpClient; return this; }
        public Builder verboseLogging(boolean verboseLogging) { this.verboseLogging = verboseLogging; return this; }
        public Builder maxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; return this; }
        public Builder renderDpi(int renderDpi) { this.renderDpi = renderDpi; return this; }
        public Builder saveRenderedImages(boolean saveRenderedImages) { this.saveRenderedImages = saveRenderedImages; return this; }

        public DotsOcrClient build() {
            OkHttpClient client = this.httpClient != null ? this.httpClient : new OkHttpClient.Builder()
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .build();
            return new DotsOcrClient(baseUrl, apiKey, defaultModel, client, new ObjectMapper(), verboseLogging, maxConcurrency,
                    renderDpi == null ? 150 : renderDpi,
                    saveRenderedImages != null && saveRenderedImages);
        }
    }

    public DotsOcrClient(String baseUrl, String apiKey, String defaultModel, OkHttpClient httpClient, ObjectMapper objectMapper, boolean verboseLogging) {
        this(baseUrl, apiKey, defaultModel, httpClient, objectMapper, verboseLogging, 10, 150, false);
    }

    public DotsOcrClient(String baseUrl, String apiKey, String defaultModel, OkHttpClient httpClient, ObjectMapper objectMapper, boolean verboseLogging, int maxConcurrency, int renderDpi, boolean saveRenderedImages) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.apiKey = apiKey; // can be null
        this.defaultModel = defaultModel == null ? "dots.ocr" : defaultModel;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.verboseLogging = verboseLogging;
        int permits = maxConcurrency <= 0 ? 1 : maxConcurrency;
        this.concurrencyLimiter = new Semaphore(permits);
        this.renderDpi = renderDpi <= 0 ? 150 : renderDpi;
        this.saveRenderedImages = saveRenderedImages;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getDefaultModel() { return defaultModel; }
    public int getRenderDpi() { return renderDpi; }
    public boolean isSaveRenderedImages() { return saveRenderedImages; }

    /**
     * GET /health → returns true if 200
     */
    public boolean health() throws IOException {
        Request.Builder rb = new Request.Builder().url(baseUrl + "/health").get();
        addAuthHeaderIfNeeded(rb);
        try (Response resp = httpClient.newCall(rb.build()).execute()) {
            return resp.isSuccessful();
        }
    }

    /**
     * GET /metrics → returns text/plain metrics
     */
    public String metrics() throws IOException {
        Request.Builder rb = new Request.Builder().url(baseUrl + "/metrics").get();
        addAuthHeaderIfNeeded(rb);
        try (Response resp = httpClient.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) throw new IOException("GET /metrics failed: " + resp.code());
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    /**
     * GET /v1/models → returns list of model ids
     */
    public List<String> listModels() throws IOException {
        Request.Builder rb = new Request.Builder().url(baseUrl + "/v1/models").get();
        addAuthHeaderIfNeeded(rb);
        try (Response resp = httpClient.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) throw new IOException("GET /v1/models failed: " + resp.code());
            String body = resp.body() != null ? resp.body().string() : "{}";
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return Collections.emptyList();
            List<String> ids = new ArrayList<>();
            for (JsonNode n : data) {
                if (n.hasNonNull("id")) ids.add(n.get("id").asText());
            }
            return ids;
        }
    }

    /**
     * POST /v1/chat/completions
     * messages follows OpenAI format, including image_url parts.
     */
    public JsonNode chatCompletions(JsonNode requestBody) throws IOException {
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody rb = RequestBody.create(json, JSON);
        Request.Builder rq = new Request.Builder().url(baseUrl + "/v1/chat/completions").post(rb);
        addAuthHeaderIfNeeded(rq);
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
                throw new IOException("POST /v1/chat/completions failed: " + resp.code());
            }
            String safeBody = body == null || body.isEmpty() ? "{}" : body;
            return objectMapper.readTree(safeBody);
        } finally {
            concurrencyLimiter.release();
        }
    }

    /**
     * Convenience: OCR an image by URL (or data URL), with prompt like "Extract all text".
     * Returns assistant content text (first choice), or full JSON when extractTextOnly=false.
     */
    public String ocrImageByUrl(String imageUrl, String prompt, String model, boolean extractTextOnly) throws IOException {
        if (model == null || model.isBlank()) model = this.defaultModel;
        JsonNode req = buildChatCompletionRequest(model, prompt, imageUrl);
        JsonNode resp = chatCompletions(req);
        if (!extractTextOnly) return resp.toString();
        return extractAssistantText(resp);
    }

    /**
     * Convenience: OCR an image by raw bytes, will be encoded as data URL (image/png by default).
     */
    public String ocrImageBytes(byte[] imageBytes, String prompt, String model, String mimeType, boolean extractTextOnly) throws IOException {
        String mt = (mimeType == null || mimeType.isBlank()) ? "image/png" : mimeType;
        String dataUrl = toDataUrl(imageBytes, mt);
        return ocrImageByUrl(dataUrl, prompt, model, extractTextOnly);
    }

    private String extractAssistantText(JsonNode resp) {
        if (resp == null) return "";
        JsonNode choices = resp.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode msg = choices.get(0).get("message");
            if (msg != null) {
                JsonNode content = msg.get("content");
                if (content != null && !content.isNull()) return content.asText("");
            }
        }
        return resp.toString();
    }

    private JsonNode buildChatCompletionRequest(String model, String prompt, String imageUrl) throws JsonProcessingException {
        Map<String, Object> root = new HashMap<>();
        root.put("model", model);

        List<Map<String, Object>> content = new ArrayList<>();
        if (prompt != null && !prompt.isBlank()) {
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt);
            content.add(textPart);
        }
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", imageUrl);
        imagePart.put("image_url", imageUrlMap);
        content.add(imagePart);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        root.put("messages", Collections.singletonList(userMsg));
        return objectMapper.valueToTree(root);
    }

    private static String toDataUrl(byte[] bytes, String mimeType) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        StringBuilder sb = new StringBuilder();
        sb.append("data:").append(mimeType).append(";base64,");
        // avoid large StringBuilder cost if extremely big
        sb.append(b64);
        return sb.toString();
    }

    private void addAuthHeaderIfNeeded(Request.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
    }

    // Simple helper for UTF-8 bytes encoding if needed externally
    public static byte[] utf8Bytes(String s) { return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8); }

    private void logRequest(String method, String url, Headers headers, String body) {
        try {
            logger.info("DotsOCR Request -> {} {}", method, url);
            if (headers != null) {
                for (String name : headers.names()) {
                    String value = headers.get(name);
                    if ("Authorization".equalsIgnoreCase(name) && value != null) {
                        value = redactToken(value);
                    }
                    logger.info("  {}: {}", name, value);
                }
            }
            if (body != null) {
                logger.info("Request Body:\n{}", body);
            }
        } catch (Exception e) {
            logger.warn("Failed to log request: {}", e.getMessage());
        }
    }

    private void logResponse(int code, Headers headers, String body) {
        try {
            logger.info("DotsOCR Response <- status {}", code);
            if (headers != null) {
                for (String name : headers.names()) {
                    logger.info("  {}: {}", name, headers.get(name));
                }
            }
            if (body != null) {
                logger.info("Response Body:\n{}", body);
            }
        } catch (Exception e) {
            logger.warn("Failed to log response: {}", e.getMessage());
        }
    }

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


