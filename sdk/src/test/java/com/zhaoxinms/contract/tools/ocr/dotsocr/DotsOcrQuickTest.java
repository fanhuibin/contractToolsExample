package com.zhaoxinms.contract.tools.ocr.dotsocr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 快速验证 Dots.OCR 的测试类
 * 运行前请确保 Dots.OCR 服务已启动（参考 https://www.dotsocr.net/blog/2）
 * 默认地址 http://localhost:8000
 */
public class DotsOcrQuickTest {

    private DotsOcrClient newClient() {
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .build();
        return DotsOcrClient.builder()
                .baseUrl("http://192.168.0.100:8000")
                .defaultModel("model")
                .verboseLogging(true)
                .httpClient(http)
                .build();
    }

    @Test
    public void testHealthAndModels() throws Exception {
        DotsOcrClient client = newClient();
        boolean ok = client.health();
        System.out.println("/health = " + ok);
        List<String> models = client.listModels();
        System.out.println("/v1/models = " + models);
        assertNotNull(models);
    }

    /**
     * 演示：以本地图片进行OCR
     * 将本地图片转为 dataURL，随后调用 chatCompletions。
     * 默认禁用该测试，避免CI环境误触发。
     */
   
    @Test
    public void testOcrLocalImage() throws Exception {
        Path img = Path.of("C:\\Users\\范慧斌\\Desktop\\a.png");
        byte[] bytes = Files.readAllBytes(img);
        DotsOcrClient client = newClient();
        String text = client.ocrImageBytes(bytes, "Extract all text", null, "image/png", true);
        System.out.println("OCR text =\n" + text);
        assertNotNull(text);
    }

    // 保留两个测试：健康检查/模型列表（默认启用），本地图片OCR（手动启用）

    // ----------------------------
    // dots.ocr 服务端到端 DEMO（基于 /v1/chat/completions）
    // 提交识别（异步执行） -> 轮询进度（本地模拟） -> 获取JSON结果
    // ----------------------------

    // 保留OkHttp客户端构造；当前demo直接使用 DotsOcrClient 封装的 /v1/chat/completions

    private OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    private boolean healthOk(OkHttpClient http, String baseUrl) {
        try {
            Request req = new Request.Builder().url(baseUrl + "/health").get().build();
            try (Response resp = http.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            // fallback: 尝试 /v1/models
            try {
                Request req2 = new Request.Builder().url(baseUrl + "/v1/models").get().build();
                try (Response resp2 = http.newCall(req2).execute()) {
                    return resp2.isSuccessful();
                }
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    // （移除未使用的HTTP工具方法，避免lint告警）

    @Test
    public void testDotsOCR_EndToEnd_JSON() throws Exception {
        // 配置：可用 -Sdotsocr.baseUrl 与 -Sdotsocr.file 覆盖
        String baseUrl = System.getProperty("dotsocr.baseUrl", "http://192.168.0.100:8000");
        String file = System.getProperty("dotsocr.file", "C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test1.pdf");

        OkHttpClient http = httpClient();
        ObjectMapper om = new ObjectMapper();
        if (!healthOk(http, baseUrl)) {
            System.out.println("[Skip] dots.ocr server is not reachable: " + baseUrl + " (skip demo)");
            return;
        }

        // 1) 构建客户端与提示词
        String taskId = UUID.randomUUID().toString().replace("-", "");
        System.out.println("Submit OK, taskId=" + taskId);
        Path p = Paths.get(file);
        String prompt = "Please output the layout information from the PDF image, including each layout element's bbox, its category, and the corresponding text content within the bbox.\n\n" +
                "1. Bbox format: [x1, y1, x2, y2]\n\n" +
                "2. Layout Categories: The possible categories are ['Caption', 'Footnote', 'Formula', 'List-item', 'Page-footer', 'Page-header', 'Picture', 'Section-header', 'Table', 'Text', 'Title'].\n\n" +
                "3. Text Extraction & Formatting Rules:\n" +
                "    - Picture: For the 'Picture' category, the text field should be omitted.\n" +
                "    - Formula: Format its text as LaTeX.\n" +
                "    - Table: Format its text as HTML.\n" +
                "    - All Others (Text, Title, etc.): Format their text as Markdown.\n\n" +
                "4. Constraints:\n" +
                "    - The output text must be the original text from the image, with no translation.\n" +
                "    - All layout elements must be sorted according to human reading order.\n\n" +
                "5. Final Output: The entire output must be a single JSON object.";

        DotsOcrClient client = DotsOcrClient.builder()
                .baseUrl(baseUrl)
                .defaultModel("model")
                .httpClient(http)
                .build();

        long start = System.currentTimeMillis();

        // 2) 识别：PDF按页渲染并逐页识别；图片则单页识别
        com.fasterxml.jackson.databind.node.ArrayNode pagesArray = om.createArrayNode();
        String nameLower = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nameLower.endsWith(".pdf")) {
            java.util.List<byte[]> images = renderAllPagesToPngBytes(p);
            int total = images.size();
            int parallel = 4;
            try {
                String pstr = System.getProperty("dotsocr.parallel");
                if (pstr != null && !pstr.isBlank()) parallel = Math.max(1, Integer.parseInt(pstr.trim()));
            } catch (Exception ignore) {}

            ExecutorService pool = Executors.newFixedThreadPool(Math.min(parallel, Math.max(1, total)));
            ExecutorCompletionService<com.fasterxml.jackson.databind.node.ObjectNode> ecs = new ExecutorCompletionService<>(pool);

            for (int i = 0; i < total; i++) {
                final int pageIndex = i;
                final byte[] img = images.get(i);
                ecs.submit(() -> {
                    String rawResp = client.ocrImageBytes(img, prompt, null, "image/png", false);
                    JsonNode envelope = om.readTree(rawResp);
                    String content = envelope.path("choices").path(0).path("message").path("content").asText("");
                    if (content == null || content.isBlank()) throw new RuntimeException("模型未返回内容(page=" + (pageIndex + 1) + ")");
                    JsonNode layout = om.readTree(content);
                    com.fasterxml.jackson.databind.node.ObjectNode pageNode = om.createObjectNode();
                    pageNode.put("page", pageIndex + 1);
                    pageNode.set("layout", layout);
                    return pageNode;
                });
            }

            com.fasterxml.jackson.databind.node.ObjectNode[] ordered = new com.fasterxml.jackson.databind.node.ObjectNode[total];
            int finished = 0;
            while (finished < total) {
                Future<com.fasterxml.jackson.databind.node.ObjectNode> fut = ecs.take();
                com.fasterxml.jackson.databind.node.ObjectNode node = fut.get();
                int idx = Math.max(1, node.path("page").asInt(1)) - 1;
                ordered[idx] = node;
                finished++;
                int percent = (int) Math.round(finished * 100.0 / total);
                System.out.println(String.format(Locale.ROOT, "Progress: %d/%d (%d%%)", finished, total, percent));
            }
            pool.shutdownNow();

            for (int i = 0; i < total; i++) {
                if (ordered[i] != null) pagesArray.add(ordered[i]);
            }
        } else {
            byte[] bytes = Files.readAllBytes(p);
            String mime = guessMime(p);
            String rawResp = client.ocrImageBytes(bytes, prompt, null, mime, false);
            JsonNode envelope = om.readTree(rawResp);
            String content = envelope.path("choices").path(0).path("message").path("content").asText("");
            if (content == null || content.isBlank()) throw new RuntimeException("模型未返回内容");
            JsonNode layout = om.readTree(content);
            com.fasterxml.jackson.databind.node.ObjectNode pageNode = om.createObjectNode();
            pageNode.put("page", 1);
            pageNode.set("layout", layout);
            pagesArray.add(pageNode);
        }

        long costMs = System.currentTimeMillis() - start;

        // 3) 组装输出并保存到磁盘（默认：同目录 .layout.json；可用 -Sdotsocr.out 覆盖）
        com.fasterxml.jackson.databind.node.ObjectNode output = om.createObjectNode();
        output.put("file", p.toAbsolutePath().toString());
        output.put("model", client.getDefaultModel());
        output.put("elapsedMs", costMs);
        output.set("pages", pagesArray);

        String pretty = om.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        String outPath = System.getProperty("dotsocr.out");
        if (outPath == null || outPath.isBlank()) {
            String base = p.toAbsolutePath().toString();
            outPath = base + ".layout.json";
        }
        java.nio.file.Files.write(java.nio.file.Path.of(outPath), pretty.getBytes(StandardCharsets.UTF_8));

        System.out.println("JSON Result saved to: " + outPath);
        System.out.println(String.format(java.util.Locale.ROOT, "DotsOCR total cost: %d ms", costMs));

        assertNotNull(pagesArray);
    }

    private static String guessMime(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        // 若传入PDF，建议先将单页渲染为图像再识别；此处默认按PNG处理
        return "image/png";
    }

    // （移除未使用方法，避免lint告警）

    private static java.util.List<byte[]> renderAllPagesToPngBytes(Path pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            java.util.List<byte[]> list = new java.util.ArrayList<>();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    list.add(baos.toByteArray());
                }
            }
            return list;
        }
    }
}


