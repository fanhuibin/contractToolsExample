package com.zhaoxinms.contract.tools.ocrcompare.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OCR HTTP客户端
 * 用于调用Python OCR服务的REST接口
 */
@Component
public class OCRHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(OCRHttpClient.class);

    @Value("${ocr.service.url:http://localhost:9898}")
    private String ocrServiceUrl;

    @Value("${ocr.service.timeout:30000}")
    private int timeoutMs;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OCRHttpClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).build();
        this.objectMapper = new ObjectMapper();
    }

    public String submitOCRTask(String filePath, String fileType, Map<String, Object> options) throws Exception {
        String jsonBody = createSubmitRequestBody(filePath, fileType, options);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ocrServiceUrl + "/api/ocr/submit"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.get("success").asBoolean()) {
                String taskId = responseJson.get("task_id").asText();
                logger.info("OCR任务提交成功，任务ID: {}", taskId);
                return taskId;
            } else {
                throw new RuntimeException("OCR任务提交失败: " + responseJson.get("error").asText());
            }
        } else {
            throw new RuntimeException("OCR任务提交失败，HTTP状态码: " + response.statusCode());
        }
    }

    public CompletableFuture<String> submitOCRTaskAsync(String filePath, String fileType, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            try { return submitOCRTask(filePath, fileType, options); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    public JsonNode getTaskStatus(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ocrServiceUrl + "/api/ocr/status/" + taskId))
                .timeout(Duration.ofMillis(timeoutMs))
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.get("success").asBoolean()) { return responseJson.get("task"); }
            throw new RuntimeException("查询任务状态失败: " + responseJson.get("error").asText());
        } else if (response.statusCode() == 404) { throw new RuntimeException("任务不存在: " + taskId); }
        else { throw new RuntimeException("查询任务状态失败，HTTP状态码: " + response.statusCode()); }
    }

    public JsonNode getOCRResult(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ocrServiceUrl + "/api/ocr/result/" + taskId))
                .timeout(Duration.ofMillis(timeoutMs))
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.get("success").asBoolean()) { return responseJson.get("result"); }
            throw new RuntimeException("获取OCR结果失败: " + responseJson.get("error").asText());
        } else if (response.statusCode() == 400) { throw new RuntimeException("任务尚未完成: " + taskId); }
        else if (response.statusCode() == 404) { throw new RuntimeException("任务或结果不存在: " + taskId); }
        else { throw new RuntimeException("获取OCR结果失败，HTTP状态码: " + response.statusCode()); }
    }

    public JsonNode waitForTaskCompletion(String taskId, int timeoutMinutes) throws Exception {
        long start = System.currentTimeMillis();
        long timeoutMsLocal = timeoutMinutes * 60L * 1000L;
        while (System.currentTimeMillis() - start < timeoutMsLocal) {
            JsonNode status = getTaskStatus(taskId);
            String s = status.path("status").asText();
            if ("completed".equals(s)) { return getOCRResult(taskId); }
            if ("failed".equals(s)) { throw new RuntimeException(status.path("error_message").asText("未知错误")); }
            Thread.sleep(2000);
        }
        throw new RuntimeException("等待任务完成超时: " + taskId);
    }

    public JsonNode getOCRHistory(int page, int size, String status) throws Exception {
        StringBuilder url = new StringBuilder(ocrServiceUrl + "/api/ocr/history?page=" + page + "&size=" + size);
        if (status != null && !status.isBlank()) { url.append("&status=").append(status); }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url.toString())).timeout(Duration.ofMillis(timeoutMs)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.get("success").asBoolean()) { return responseJson.get("data"); }
            throw new RuntimeException("查询历史任务失败: " + responseJson.get("error").asText());
        }
        throw new RuntimeException("查询历史任务失败，HTTP状态码: " + response.statusCode());
    }

    public JsonNode clearOCRHistory(String[] taskIds, boolean clearAll) throws Exception {
        Map<String, Object> body = clearAll ? Map.of("clear_all", true) : Map.of("task_ids", taskIds);
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ocrServiceUrl + "/api/ocr/clear"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.get("success").asBoolean()) { return responseJson; }
            throw new RuntimeException("清除历史数据失败: " + responseJson.get("error").asText());
        }
        throw new RuntimeException("清除历史数据失败，HTTP状态码: " + response.statusCode());
    }

    public boolean healthCheck() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ocrServiceUrl + "/health")).timeout(Duration.ofMillis(5000)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) { logger.warn("OCR服务健康检查失败: {}", e.getMessage()); return false; }
    }

    private String createSubmitRequestBody(String filePath, String fileType, Map<String, Object> options) throws IOException {
        Map<String, Object> requestBody = Map.of(
                "file_source", "local",
                "file_path", filePath,
                "file_type", fileType,
                "options", options != null ? options : Map.of("dpi", 150, "min_score", 0.5)
        );
        return objectMapper.writeValueAsString(requestBody);
    }

    public String getOcrServiceUrl() { return ocrServiceUrl; }
    public int getTimeoutMs() { return timeoutMs; }
}


