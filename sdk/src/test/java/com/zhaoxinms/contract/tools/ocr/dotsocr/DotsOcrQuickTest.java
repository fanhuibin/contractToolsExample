package com.zhaoxinms.contract.tools.ocr.dotsocr;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

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
}


