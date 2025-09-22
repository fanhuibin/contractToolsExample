package com.zhaoxinms.contract.tools.ocr.rapidocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * RapidOCR API客户端
 * 
 * 基于RapidOCR API文档实现：https://rapidai.github.io/RapidOCRDocs/main/install_usage/rapidocr_api/usage/
 * 
 * API支持的功能：
 * - POST /ocr 通过文件上传进行OCR识别
 * - POST /ocr_with_data 通过base64数据进行OCR识别
 * 
 * 输出格式：
 * {
 *   "0": {
 *     "rec_txt": "识别的文本",
 *     "dt_boxes": [[x1, y1], [x2, y2], [x3, y3], [x4, y4]], // 四个角点坐标
 *     "score": "0.8176" // 置信度
 *   }
 * }
 */
public class RapidOcrClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(RapidOcrClient.class);

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean verboseLogging;
    private final Semaphore concurrencyLimiter;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String baseUrl = "http://192.168.0.100:9005";
        private Duration timeout = Duration.ofMinutes(2);
        private OkHttpClient httpClient;
        private boolean verboseLogging = false;
        private int maxConcurrency = 10;

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder httpClient(OkHttpClient httpClient) { this.httpClient = httpClient; return this; }
        public Builder verboseLogging(boolean verboseLogging) { this.verboseLogging = verboseLogging; return this; }
        public Builder maxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; return this; }

        public RapidOcrClient build() {
            OkHttpClient client = this.httpClient != null ? this.httpClient : new OkHttpClient.Builder()
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .build();
            return new RapidOcrClient(baseUrl, client, new ObjectMapper(), verboseLogging, maxConcurrency);
        }
    }

    public RapidOcrClient(String baseUrl, OkHttpClient httpClient, ObjectMapper objectMapper, boolean verboseLogging, int maxConcurrency) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.verboseLogging = verboseLogging;
        int permits = maxConcurrency <= 0 ? 1 : maxConcurrency;
        this.concurrencyLimiter = new Semaphore(permits);
    }

    public String getBaseUrl() { return baseUrl; }

    /**
     * 测试服务器API接口是否可用
     * 发送一个简单的GET请求到docs接口
     */
    public String testApiEndpoints() throws IOException {
        StringBuilder result = new StringBuilder();
        
        // 测试根路径
        try {
            Request request = new Request.Builder().url(baseUrl + "/").get().build();
            try (Response resp = httpClient.newCall(request).execute()) {
                result.append(String.format("GET / : %d %s\n", resp.code(), resp.message()));
            }
        } catch (Exception e) {
            result.append(String.format("GET / : ERROR - %s\n", e.getMessage()));
        }
        
        // 测试docs路径
        try {
            Request request = new Request.Builder().url(baseUrl + "/docs").get().build();
            try (Response resp = httpClient.newCall(request).execute()) {
                result.append(String.format("GET /docs : %d %s\n", resp.code(), resp.message()));
            }
        } catch (Exception e) {
            result.append(String.format("GET /docs : ERROR - %s\n", e.getMessage()));
        }
        
        // 测试ocr路径（GET请求，应该返回方法不允许）
        try {
            Request request = new Request.Builder().url(baseUrl + "/ocr").get().build();
            try (Response resp = httpClient.newCall(request).execute()) {
                result.append(String.format("GET /ocr : %d %s\n", resp.code(), resp.message()));
            }
        } catch (Exception e) {
            result.append(String.format("GET /ocr : ERROR - %s\n", e.getMessage()));
        }
        
        return result.toString();
    }

    /**
     * 检查服务健康状态
     * 通过调用根路径或docs路径来检查服务是否可用
     */
    public boolean health() throws IOException {
        // 尝试访问docs页面
        Request request = new Request.Builder()
                .url(baseUrl + "/docs")
                .get()
                .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            if (resp.isSuccessful()) {
                return true;
            }
        }
        
        // 如果docs访问失败，尝试访问根路径
        request = new Request.Builder()
                .url(baseUrl + "/")
                .get()
                .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            return resp.isSuccessful();
        }
    }

    /**
     * 通过文件上传进行OCR识别
     * 
     * @param imageFile 图像文件
     * @param useDetection 是否使用文本检测
     * @param useClassification 是否使用方向分类
     * @param useRecognition 是否使用文本识别
     * @return OCR识别结果JSON
     */
    public JsonNode ocrByFile(File imageFile, boolean useDetection, boolean useClassification, boolean useRecognition) throws IOException {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist: " + imageFile.getAbsolutePath());
        }

        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", imageFile.getName(), fileBody)
                .addFormDataPart("use_det", String.valueOf(useDetection))
                .addFormDataPart("use_cls", String.valueOf(useClassification))
                .addFormDataPart("use_rec", String.valueOf(useRecognition));

        RequestBody requestBody = multipartBuilder.build();
        Request request = new Request.Builder()
                .url(baseUrl + "/ocr")
                .post(requestBody)
                .build();

        if (verboseLogging) {
            logRequest("POST", request.url().toString(), request.headers(), "[Multipart File Upload]");
        }

        concurrencyLimiter.acquireUninterruptibly();
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (verboseLogging) {
                logResponse(resp.code(), resp.headers(), body);
            }
            if (!resp.isSuccessful()) {
                String errorMessage = String.format("POST /ocr failed: %d %s", resp.code(), resp.message());
                if (body != null && !body.trim().isEmpty()) {
                    errorMessage += ", response body: " + body;
                }
                // 添加详细的请求信息用于调试
                errorMessage += String.format("\nRequest URL: %s", request.url());
                errorMessage += String.format("\nRequest method: %s", request.method());
                throw new IOException(errorMessage);
            }
            String safeBody = body == null || body.isEmpty() ? "{}" : body;
            return objectMapper.readTree(safeBody);
        } finally {
            concurrencyLimiter.release();
        }
    }

    /**
     * 通过Base64数据进行OCR识别
     * 
     * @param imageBytes 图像字节数组
     * @param useDetection 是否使用文本检测
     * @param useClassification 是否使用方向分类
     * @param useRecognition 是否使用文本识别
     * @return OCR识别结果JSON
     */
    public JsonNode ocrByData(byte[] imageBytes, boolean useDetection, boolean useClassification, boolean useRecognition) throws IOException {
        String base64Data = Base64.getEncoder().encodeToString(imageBytes);
        
        // 构建JSON请求体
        RapidOcrRequest ocrRequest = new RapidOcrRequest();
        ocrRequest.image_data = base64Data;
        ocrRequest.use_det = useDetection;
        ocrRequest.use_cls = useClassification;
        ocrRequest.use_rec = useRecognition;

        String json = objectMapper.writeValueAsString(ocrRequest);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/ocr_with_data")
                .post(requestBody)
                .build();

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
                String errorMessage = String.format("POST /ocr_with_data failed: %d %s", resp.code(), resp.message());
                if (body != null && !body.trim().isEmpty()) {
                    errorMessage += ", response body: " + body;
                }
                // 添加详细的请求信息用于调试
                errorMessage += String.format("\nRequest URL: %s", request.url());
                errorMessage += String.format("\nRequest method: %s", request.method());
                throw new IOException(errorMessage);
            }
            String safeBody = body == null || body.isEmpty() ? "{}" : body;
            return objectMapper.readTree(safeBody);
        } finally {
            concurrencyLimiter.release();
        }
    }

    /**
     * 解析OCR结果为文本框列表
     * 
     * @param ocrResult OCR API返回的JSON结果
     * @return 识别的文本框列表
     */
    public List<RapidOcrTextBox> parseOcrResult(JsonNode ocrResult) {
        List<RapidOcrTextBox> textBoxes = new ArrayList<>();
        
        if (ocrResult == null || !ocrResult.isObject()) {
            return textBoxes;
        }

        // 遍历每个识别结果
        ocrResult.fields().forEachRemaining(entry -> {
            try {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isObject()) {
                    RapidOcrTextBox textBox = new RapidOcrTextBox();
                    textBox.id = key;
                    
                    // 提取文本内容
                    JsonNode recTxt = value.get("rec_txt");
                    if (recTxt != null) {
                        textBox.text = recTxt.asText("");
                    }
                    
                    // 提取置信度
                    JsonNode score = value.get("score");
                    if (score != null) {
                        textBox.confidence = Float.parseFloat(score.asText("0.0"));
                    }
                    
                    // 提取边界框坐标
                    JsonNode dtBoxes = value.get("dt_boxes");
                    if (dtBoxes != null && dtBoxes.isArray() && dtBoxes.size() == 4) {
                        float[][] boxes = new float[4][2];
                        for (int i = 0; i < 4; i++) {
                            JsonNode point = dtBoxes.get(i);
                            if (point.isArray() && point.size() == 2) {
                                boxes[i][0] = (float) point.get(0).asDouble();
                                boxes[i][1] = (float) point.get(1).asDouble();
                            }
                        }
                        textBox.boundingBox = boxes;
                    }
                    
                    textBoxes.add(textBox);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse OCR result entry: {}", entry.getKey(), e);
            }
        });
        
        return textBoxes;
    }

    /**
     * 便捷方法：使用默认参数进行OCR识别
     */
    public List<RapidOcrTextBox> ocrFile(File imageFile) throws IOException {
        JsonNode result = ocrByFile(imageFile, true, true, true);
        return parseOcrResult(result);
    }

    /**
     * 便捷方法：使用默认参数进行OCR识别
     */
    public List<RapidOcrTextBox> ocrBytes(byte[] imageBytes) throws IOException {
        JsonNode result = ocrByData(imageBytes, true, true, true);
        return parseOcrResult(result);
    }

    private void logRequest(String method, String url, Headers headers, String body) {
        try {
            logger.info("RapidOCR Request -> {} {}", method, url);
            if (headers != null) {
                for (String name : headers.names()) {
                    logger.info("  {}: {}", name, headers.get(name));
                }
            }
            if (body != null) {
                // 对于包含base64数据的请求，只记录前100个字符避免日志过长
                String logBody = body.length() > 200 ? body.substring(0, 200) + "... [truncated]" : body;
                logger.info("Request Body:\n{}", logBody);
            }
        } catch (Exception e) {
            logger.warn("Failed to log request: {}", e.getMessage());
        }
    }

    private void logResponse(int code, Headers headers, String body) {
        try {
            logger.info("RapidOCR Response <- status {}", code);
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

    /**
     * OCR请求数据结构
     */
    public static class RapidOcrRequest {
        public String image_data;
        public boolean use_det = true;
        public boolean use_cls = true;
        public boolean use_rec = true;
    }

    /**
     * OCR识别结果的文本框
     */
    public static class RapidOcrTextBox {
        public String id;
        public String text;
        public float confidence;
        public float[][] boundingBox; // 四个角点：[左上, 右上, 右下, 左下]

        @Override
        public String toString() {
            return String.format("RapidOcrTextBox{id='%s', text='%s', confidence=%.4f, boundingBox=%s}", 
                    id, text, confidence, boundingBoxToString());
        }

        private String boundingBoxToString() {
            if (boundingBox == null) return "null";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < boundingBox.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("[").append(boundingBox[i][0]).append(", ").append(boundingBox[i][1]).append("]");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * 主方法，用于测试RapidOCR客户端
     * 
     * 使用方法：
     * java -cp your-classpath RapidOcrClient [image_path] [server_url]
     * 
     * 参数：
     * - image_path: 图片文件路径（可选，默认查找当前目录下的test.jpg）
     * - server_url: RapidOCR服务地址（可选，默认http://192.168.0.100:9005）
     * 
     * 示例：
     * java RapidOcrClient "C:\\test\\image.jpg"
     * java RapidOcrClient "C:\\test\\image.jpg" "http://localhost:9005"
     */
    public static void main(String[] args) {
        // 解析命令行参数
        String imagePath = args.length > 0 ? args[0] : "D:\\git\\zhaoxin-contract-tool-set\\sdk\\uploads\\gpu-ocr-compare\\tasks\\0a4d6fa0-0608-407c-b7a5-4a6c64e7845c\\images\\new\\page-4.png";
        String serverUrl = args.length > 1 ? args[1] : "http://192.168.0.100:9005";
        
        System.out.println("=== RapidOCR 客户端测试 ===");
        System.out.println("图片路径: " + imagePath);
        System.out.println("服务地址: " + serverUrl);
        System.out.println();

        // 创建客户端
        RapidOcrClient client = RapidOcrClient.builder()
                .baseUrl(serverUrl)
                .verboseLogging(true)
                .build();

        try {
            // 1. 检查服务健康状态
            System.out.println("1. 检查服务状态...");
            long healthStartTime = System.currentTimeMillis();
            boolean healthy = client.health();
            long healthDuration = System.currentTimeMillis() - healthStartTime;
            
            if (healthy) {
                System.out.printf("✓ 服务状态正常 (耗时: %dms)%n", healthDuration);
            } else {
                System.out.printf("✗ 服务不可用 (耗时: %dms)%n", healthDuration);
                
                // 尝试测试各个API端点
                System.out.println("\n测试各个API端点:");
                try {
                    String apiTestResult = client.testApiEndpoints();
                    System.out.println(apiTestResult);
                } catch (Exception e) {
                    System.err.println("API端点测试失败: " + e.getMessage());
                }
                
                System.err.println("请检查RapidOCR服务是否在 " + serverUrl + " 正常运行");
                System.err.println("建议检查事项:");
                System.err.println("1. 确认服务地址是否正确");
                System.err.println("2. 确认端口9005是否被占用");
                System.err.println("3. 尝试在浏览器中访问 " + serverUrl + "/docs");
                return;
            }
            System.out.println();

            // 2. 检查图片文件
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("✗ 图片文件不存在: " + imagePath);
                System.out.println("\n请指定正确的图片路径，支持的格式: JPG, PNG, BMP, GIF等");
                System.out.println("使用方法: java RapidOcrClient \"图片路径\" [服务地址]");
                return;
            }

            if (!imageFile.isFile()) {
                System.err.println("✗ 指定的路径不是文件: " + imagePath);
                return;
            }

            System.out.printf("✓ 图片文件存在: %s (大小: %.2f KB)%n", 
                    imageFile.getName(), imageFile.length() / 1024.0);
            System.out.println();

            // 3. 执行OCR识别
            System.out.println("2. 开始OCR识别...");
            long ocrStartTime = System.currentTimeMillis();
            
            List<RapidOcrTextBox> textBoxes = client.ocrFile(imageFile);
            
            long ocrDuration = System.currentTimeMillis() - ocrStartTime;
            System.out.printf("✓ OCR识别完成 (耗时: %dms)%n", ocrDuration);
            System.out.println();

            // 4. 显示识别结果
            System.out.println("3. 识别结果:");
            System.out.println("识别到 " + textBoxes.size() + " 个文本块:");
            System.out.println("----------------------------------------");

            if (textBoxes.isEmpty()) {
                System.out.println("没有识别到任何文本内容");
            } else {
                for (int i = 0; i < textBoxes.size(); i++) {
                    RapidOcrTextBox box = textBoxes.get(i);
                    System.out.printf("[%d] 文本: %s%n", i + 1, box.text);
                    System.out.printf("    置信度: %.4f%n", box.confidence);
                    if (box.boundingBox != null) {
                        System.out.printf("    坐标: [%.1f,%.1f] → [%.1f,%.1f]%n", 
                                box.boundingBox[0][0], box.boundingBox[0][1],
                                box.boundingBox[2][0], box.boundingBox[2][1]);
                    }
                    System.out.println();
                }

                // 5. 汇总信息
                System.out.println("----------------------------------------");
                System.out.println("4. 汇总信息:");
                
                // 拼接所有文本
                StringBuilder allText = new StringBuilder();
                for (RapidOcrTextBox box : textBoxes) {
                    if (box.text != null && !box.text.trim().isEmpty()) {
                        if (allText.length() > 0) {
                            allText.append("\n");
                        }
                        allText.append(box.text);
                    }
                }
                
                System.out.println("完整文本内容:");
                System.out.println("\"\"\"");
                System.out.println(allText.toString());
                System.out.println("\"\"\"");
                System.out.println();
                
                // 计算平均置信度
                float avgConfidence = 0;
                for (RapidOcrTextBox box : textBoxes) {
                    avgConfidence += box.confidence;
                }
                avgConfidence /= textBoxes.size();
                
                System.out.printf("文本块数量: %d%n", textBoxes.size());
                System.out.printf("平均置信度: %.4f%n", avgConfidence);
                System.out.printf("总字符数: %d%n", allText.length());
            }

            // 6. 性能统计
            System.out.println("----------------------------------------");
            System.out.println("5. 性能统计:");
            System.out.printf("服务健康检查: %dms%n", healthDuration);
            System.out.printf("OCR识别耗时: %dms%n", ocrDuration);
            System.out.printf("总耗时: %dms%n", healthDuration + ocrDuration);
            
            if (imageFile.length() > 0) {
                double throughput = (imageFile.length() / 1024.0) / (ocrDuration / 1000.0);
                System.out.printf("处理速度: %.2f KB/s%n", throughput);
            }

        } catch (Exception e) {
            System.err.println("✗ OCR识别失败:");
            System.err.println("错误信息: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("原因: " + e.getCause().getMessage());
            }
            
            // 显示详细错误信息（如果启用了详细日志）
            System.err.println("\n详细错误堆栈:");
            e.printStackTrace();
            
            System.err.println("\n故障排除建议:");
            System.err.println("1. 检查RapidOCR服务是否在 " + serverUrl + " 正常运行");
            System.err.println("2. 检查网络连接是否正常");
            System.err.println("3. 检查图片文件是否损坏或格式不支持");
            System.err.println("4. 尝试减小图片大小或更换图片格式");
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}
