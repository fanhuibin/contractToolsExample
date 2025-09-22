package com.zhaoxinms.contract.tools.ocr.rapidocr;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * RapidOCR使用示例
 * 
 * 演示如何使用RapidOCR客户端进行OCR识别
 */
public class RapidOcrExample {

    private static final Logger logger = LoggerFactory.getLogger(RapidOcrExample.class);

    public static void main(String[] args) {
        // 创建RapidOCR客户端
        RapidOcrClient client = RapidOcrClient.builder()
                .baseUrl("http://192.168.0.100:9005")
                .verboseLogging(true)
                .build();

        try {
            // 1. 检查服务健康状态
            logger.info("检查RapidOCR服务状态...");
            boolean healthy = client.health();
            logger.info("RapidOCR服务状态: {}", healthy ? "正常" : "异常");

            if (!healthy) {
                logger.error("RapidOCR服务不可用，请检查服务是否启动");
                return;
            }

            // 2. 测试文件OCR识别（如果有测试图片的话）
            String testImagePath = "test.jpg"; // 替换为实际的测试图片路径
            File testFile = new File(testImagePath);
            
            if (testFile.exists()) {
                logger.info("开始测试文件OCR识别: {}", testImagePath);
                
                // 使用默认参数
                List<RapidOcrClient.RapidOcrTextBox> textBoxes = client.ocrFile(testFile);
                logger.info("识别到 {} 个文本块:", textBoxes.size());
                
                for (RapidOcrClient.RapidOcrTextBox box : textBoxes) {
                    logger.info("文本: {} (置信度: {:.4f})", box.text, box.confidence);
                }
                
                // 获取原始JSON结果
                JsonNode rawResult = client.ocrByFile(testFile, true, true, true);
                logger.info("原始JSON结果: {}", rawResult.toString());
                
            } else {
                logger.warn("测试图片文件不存在: {}", testImagePath);
            }

            // 3. 测试Base64数据OCR识别
            if (testFile.exists()) {
                logger.info("开始测试Base64数据OCR识别...");
                
                byte[] imageBytes = Files.readAllBytes(Paths.get(testImagePath));
                List<RapidOcrClient.RapidOcrTextBox> textBoxes = client.ocrBytes(imageBytes);
                
                logger.info("Base64识别到 {} 个文本块:", textBoxes.size());
                for (RapidOcrClient.RapidOcrTextBox box : textBoxes) {
                    logger.info("文本: {} (置信度: {:.4f})", box.text, box.confidence);
                }
            }

        } catch (Exception e) {
            logger.error("RapidOCR测试失败", e);
        }
    }
}
