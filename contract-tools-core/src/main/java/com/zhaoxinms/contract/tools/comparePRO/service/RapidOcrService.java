package com.zhaoxinms.contract.tools.comparePRO.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.comparePRO.client.RapidOcrClient;
import com.zhaoxinms.contract.tools.comparePRO.config.RapidOcrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RapidOCR服务类
 * 
 * 提供高级的OCR功能接口，包括：
 * - 文件OCR识别
 * - 字节数组OCR识别
 * - 批量OCR处理
 * - 结果格式转换
 */
@Service
@ConditionalOnProperty(prefix = "zxcm.compare.rapidocr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RapidOcrService {

    private static final Logger logger = LoggerFactory.getLogger(RapidOcrService.class);

    @Autowired
    private RapidOcrClient rapidOcrClient;

    @Autowired
    private RapidOcrConfig rapidOcrConfig;

    /**
     * 对图像文件进行OCR识别
     * 
     * @param imageFile 图像文件
     * @return OCR识别结果
     */
    public List<RapidOcrClient.RapidOcrTextBox> recognizeFile(File imageFile) throws IOException {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist: " + imageFile.getAbsolutePath());
        }

        logger.info("开始OCR识别文件: {}", imageFile.getAbsolutePath());
        long startTime = System.currentTimeMillis();

        try {
            List<RapidOcrClient.RapidOcrTextBox> result = rapidOcrClient.ocrFile(imageFile);
            long duration = System.currentTimeMillis() - startTime;
            logger.info("OCR识别完成，耗时: {}ms，识别到 {} 个文本块", duration, result.size());
            return result;
        } catch (IOException e) {
            logger.error("OCR识别失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 对图像字节数组进行OCR识别
     * 
     * @param imageBytes 图像字节数组
     * @return OCR识别结果
     */
    public List<RapidOcrClient.RapidOcrTextBox> recognizeBytes(byte[] imageBytes) throws IOException {
        logger.info("开始OCR识别字节数组，大小: {} bytes", imageBytes.length);
        long startTime = System.currentTimeMillis();

        try {
            List<RapidOcrClient.RapidOcrTextBox> result = rapidOcrClient.ocrBytes(imageBytes);
            long duration = System.currentTimeMillis() - startTime;
            logger.info("OCR识别完成，耗时: {}ms，识别到 {} 个文本块", duration, result.size());
            return result;
        } catch (IOException e) {
            logger.error("OCR识别失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 对图像文件进行OCR识别（自定义参数）
     * 
     * @param imageFile 图像文件
     * @param useDetection 是否使用文本检测
     * @param useClassification 是否使用方向分类
     * @param useRecognition 是否使用文本识别
     * @return OCR识别的原始JSON结果
     */
    public JsonNode recognizeFileRaw(File imageFile, boolean useDetection, boolean useClassification, boolean useRecognition) throws IOException {
        logger.info("开始OCR识别文件（自定义参数）: {}, det={}, cls={}, rec={}", 
                imageFile.getAbsolutePath(), useDetection, useClassification, useRecognition);
        
        return rapidOcrClient.ocrByFile(imageFile, useDetection, useClassification, useRecognition);
    }

    /**
     * 对图像字节数组进行OCR识别（自定义参数）
     * 
     * @param imageBytes 图像字节数组
     * @param useDetection 是否使用文本检测
     * @param useClassification 是否使用方向分类
     * @param useRecognition 是否使用文本识别
     * @return OCR识别的原始JSON结果
     */
    public JsonNode recognizeBytesRaw(byte[] imageBytes, boolean useDetection, boolean useClassification, boolean useRecognition) throws IOException {
        logger.info("开始OCR识别字节数组（自定义参数），大小: {} bytes, det={}, cls={}, rec={}", 
                imageBytes.length, useDetection, useClassification, useRecognition);
        
        return rapidOcrClient.ocrByData(imageBytes, useDetection, useClassification, useRecognition);
    }

    /**
     * 批量处理图像文件
     * 
     * @param imageFiles 图像文件列表
     * @return OCR识别结果列表
     */
    public List<List<RapidOcrClient.RapidOcrTextBox>> recognizeFiles(List<File> imageFiles) throws IOException {
        logger.info("开始批量OCR识别，文件数量: {}", imageFiles.size());
        long startTime = System.currentTimeMillis();

        try {
            List<List<RapidOcrClient.RapidOcrTextBox>> results = imageFiles.stream()
                    .map(file -> {
                        try {
                            return recognizeFile(file);
                        } catch (IOException e) {
                            logger.warn("跳过文件 {} 的OCR识别，原因: {}", file.getAbsolutePath(), e.getMessage());
                            return java.util.Collections.<RapidOcrClient.RapidOcrTextBox>emptyList(); // 返回空列表
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            int totalTextBoxes = results.stream().mapToInt(List::size).sum();
            logger.info("批量OCR识别完成，耗时: {}ms，总共识别到 {} 个文本块", duration, totalTextBoxes);
            
            return results;
        } catch (Exception e) {
            logger.error("批量OCR识别失败: {}", e.getMessage(), e);
            throw new IOException("批量OCR识别失败", e);
        }
    }

    /**
     * 对路径指定的图像文件进行OCR识别
     * 
     * @param imagePath 图像文件路径
     * @return OCR识别结果
     */
    public List<RapidOcrClient.RapidOcrTextBox> recognizePath(Path imagePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        return recognizeBytes(imageBytes);
    }

    /**
     * 检查RapidOCR服务是否可用
     * 
     * @return 服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            return rapidOcrClient.health();
        } catch (Exception e) {
            logger.warn("检查RapidOCR服务可用性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取RapidOCR服务信息
     * 
     * @return 服务信息字符串
     */
    public String getServiceInfo() {
        return String.format("RapidOCR Service [%s] - Available: %s", 
                rapidOcrConfig.getBaseUrl(), 
                isServiceAvailable());
    }

    /**
     * 将OCR结果转换为简单的文本字符串
     * 
     * @param textBoxes OCR识别结果
     * @return 拼接后的文本字符串
     */
    public String convertToText(List<RapidOcrClient.RapidOcrTextBox> textBoxes) {
        return textBoxes.stream()
                .map(box -> box.text)
                .filter(text -> text != null && !text.trim().isEmpty())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * 将OCR结果转换为带坐标信息的文本字符串
     * 
     * @param textBoxes OCR识别结果
     * @return 包含坐标信息的文本字符串
     */
    public String convertToTextWithCoordinates(List<RapidOcrClient.RapidOcrTextBox> textBoxes) {
        StringBuilder sb = new StringBuilder();
        for (RapidOcrClient.RapidOcrTextBox box : textBoxes) {
            if (box.text != null && !box.text.trim().isEmpty()) {
                sb.append(String.format("[%.1f,%.1f]: %s (%.4f)\n", 
                        box.boundingBox[0][0], box.boundingBox[0][1], box.text, box.confidence));
            }
        }
        return sb.toString();
    }
}
