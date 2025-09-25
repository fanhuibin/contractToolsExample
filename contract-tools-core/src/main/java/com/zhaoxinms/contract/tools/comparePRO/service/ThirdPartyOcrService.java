package com.zhaoxinms.contract.tools.comparePRO.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.client.ThirdPartyOcrClient;
import com.zhaoxinms.contract.tools.comparePRO.config.ThirdPartyOcrConfig;
import com.zhaoxinms.contract.tools.comparePRO.model.CharBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 第三方OCR服务
 * 提供基于阿里云Dashscope的图像文本识别功能
 */
@Service
@ConditionalOnProperty(name = "zxcm.compare.third-party-ocr.enabled", havingValue = "true")
public class ThirdPartyOcrService {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyOcrService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // JSON代码块提取的正则表达式
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Autowired
    private ThirdPartyOcrClient thirdPartyOcrClient;

    @Autowired
    private ThirdPartyOcrConfig config;

    /**
     * 检查第三方OCR服务是否可用
     */
    public boolean isAvailable() {
        try {
            return thirdPartyOcrClient.health();
        } catch (Exception e) {
            logger.warn("第三方OCR服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 对图像进行OCR识别
     * 
     * @param imageBytes 图像字节数组
     * @param mimeType 图像MIME类型
     * @param pageNumber 页面编号
     * @param imageWidth 图像宽度（用于坐标转换）
     * @param imageHeight 图像高度（用于坐标转换）
     * @return 识别结果的CharBox列表
     * @throws IOException 当OCR识别失败时抛出
     */
    public List<CharBox> performOCR(byte[] imageBytes, String mimeType, int pageNumber, int imageWidth, int imageHeight) throws IOException {
        logger.debug("开始第三方OCR识别，图像大小: {} bytes", imageBytes.length);
        
        try {
            // 使用默认提示词进行OCR识别
            String rawResult = thirdPartyOcrClient.ocrImageBytesWithDefaultPrompt(
                    imageBytes, 
                    null,  // 使用默认模型
                    mimeType, 
                    true   // 只提取文本内容
            );
            
            logger.debug("第三方OCR原始响应: {}", rawResult);
            
            // 解析结果（包含坐标转换）
            List<CharBox> charBoxes = parseOcrResult(rawResult, pageNumber, imageWidth, imageHeight);
            
            logger.info("第三方OCR识别完成，识别到 {} 个字符", charBoxes.size());
            return charBoxes;
            
        } catch (Exception e) {
            logger.error("第三方OCR识别失败", e);
            throw new IOException("第三方OCR识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析第三方OCR的JSON响应结果
     * 
     * @param rawResult 原始JSON字符串响应
     * @param pageNumber 页面编号
     * @param imageWidth 图像宽度（用于坐标转换）
     * @param imageHeight 图像高度（用于坐标转换）
     * @return 解析后的CharBox列表
     * @throws IOException 当JSON解析失败时抛出
     */
    private List<CharBox> parseOcrResult(String rawResult, int pageNumber, int imageWidth, int imageHeight) throws IOException {
        List<CharBox> charBoxes = new ArrayList<>();
        
        if (rawResult == null || rawResult.trim().isEmpty()) {
            logger.warn("第三方OCR返回空结果");
            return charBoxes;
        }
        
        try {
            // 输出完整的原始响应用于调试
            logger.info("=== 阿里云千问VL完整原始响应开始 ===");
            logger.info("响应长度: {} 字符", rawResult.length());
            logger.info("原始响应内容:\n{}", rawResult);
            logger.info("=== 阿里云千问VL完整原始响应结束 ===");
            
            // 首先尝试提取JSON代码块
            String jsonContent = extractJsonFromResponse(rawResult);
            
            logger.info("=== 提取的JSON内容开始 ===");
            logger.info("JSON长度: {} 字符", jsonContent.length());
            logger.info("提取的JSON内容:\n{}", jsonContent);
            logger.info("=== 提取的JSON内容结束 ===");
            
            // 解析JSON数组（增加错误处理）
            JsonNode rootNode;
            try {
                rootNode = objectMapper.readTree(jsonContent);
                logger.info("JSON解析成功");
            } catch (Exception e) {
                logger.warn("初次JSON解析失败，错误信息: {}", e.getMessage());
                logger.info("=== 开始JSON修复过程 ===");
                
                // 尝试更强大的JSON修复
                String repairedJson = repairBrokenJson(jsonContent);
                
                logger.info("=== 修复后的JSON内容开始 ===");
                logger.info("修复后JSON长度: {} 字符", repairedJson.length());
                logger.info("修复后JSON内容:\n{}", repairedJson);
                logger.info("=== 修复后的JSON内容结束 ===");
                
                rootNode = objectMapper.readTree(repairedJson);
                logger.info("JSON修复并解析成功");
            }
            
            if (!rootNode.isArray()) {
                logger.warn("第三方OCR响应不是JSON数组格式: {}", jsonContent);
                return charBoxes;
            }
            
            // 转换每个识别块
            for (JsonNode blockNode : rootNode) {
                List<CharBox> blockCharBoxes = convertToCharBoxes(blockNode, pageNumber, imageWidth, imageHeight);
                charBoxes.addAll(blockCharBoxes);
            }
            
            logger.debug("成功解析 {} 个文本块", charBoxes.size());
            
        } catch (Exception e) {
            logger.error("解析第三方OCR结果失败: {}", rawResult, e);
            throw new IOException("解析第三方OCR结果失败: " + e.getMessage(), e);
        }
        
        return charBoxes;
    }

    /**
     * 从响应中提取JSON内容
     * 支持提取markdown代码块中的JSON或直接的JSON，并处理Unicode转义字符和格式错误
     */
    private String extractJsonFromResponse(String response) {
        String extracted = null;
        
        // 首先尝试提取代码块中的JSON
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            extracted = matcher.group(1).trim();
            logger.debug("从代码块中提取JSON");
        } else {
            // 如果没有代码块，尝试查找JSON数组的开始和结束
            String trimmed = response.trim();
            int startIndex = trimmed.indexOf('[');
            int endIndex = trimmed.lastIndexOf(']');
            
            if (startIndex >= 0 && endIndex > startIndex) {
                extracted = trimmed.substring(startIndex, endIndex + 1);
                logger.debug("直接提取JSON数组");
            } else {
                // 最后尝试返回原始响应
                extracted = response;
                logger.debug("使用原始响应作为JSON");
            }
        }
        
        // 处理Unicode转义字符和JSON格式问题
        String cleaned = cleanAndFixJson(extracted);
        
        logger.debug("JSON清理完成，原始长度: {}, 清理后长度: {}", extracted.length(), cleaned.length());
        
        return cleaned;
    }
    
    /**
     * 清理和修复JSON字符串
     * 处理Unicode转义字符、多余的花括号和其他格式问题
     */
    private String cleanAndFixJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return jsonStr;
        }
        
        try {
            // 1. 处理Unicode转义字符
            String cleaned = unescapeUnicode(jsonStr);
            
            // 2. 修复常见的JSON格式错误
            cleaned = fixJsonSyntaxErrors(cleaned);
            
            // 3. 移除多余的空白字符
            cleaned = cleaned.replaceAll("\\s+", " ").trim();
            
            logger.debug("JSON清理步骤完成");
            
            return cleaned;
            
        } catch (Exception e) {
            logger.warn("JSON清理过程中出现错误: {}, 返回原始字符串", e.getMessage());
            return jsonStr;
        }
    }
    
    /**
     * 处理Unicode转义字符
     */
    private String unescapeUnicode(String str) {
        if (str == null) {
            return null;
        }
        
        // 处理常见的Unicode转义字符
        String result = str;
        result = result.replace("\\u0009", "\t");  // 制表符
        result = result.replace("\\u000A", "\n");  // 换行符
        result = result.replace("\\u000D", "\r");  // 回车符
        result = result.replace("\\u0020", " ");   // 空格
        result = result.replace("\\u0022", "\"");  // 双引号
        result = result.replace("\\u005C", "\\");  // 反斜杠
        
        // 处理其他Unicode转义字符的通用方法
        java.util.regex.Pattern unicodePattern = java.util.regex.Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
        java.util.regex.Matcher matcher = unicodePattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int code = Integer.parseInt(matcher.group(1), 16);
                matcher.appendReplacement(sb, String.valueOf((char) code));
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, matcher.group(0)); // 如果解析失败，保持原样
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();
        
        return result;
    }
    
    /**
     * 修复JSON语法错误
     */
    private String fixJsonSyntaxErrors(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        
        String fixed = jsonStr;
        
        // 1. 修复多余的花括号（如 "{{" 或 "}}" ）
        fixed = fixed.replaceAll("\\{\\{", "{");
        fixed = fixed.replaceAll("\\}\\}", "}");
        
        // 2. 修复对象开始时的多余花括号（如示例中第8行的问题）
        // 查找类似 "},\n{{"bbox_2d" 的模式并修复为 "},\n{"bbox_2d"
        fixed = fixed.replaceAll("(\\},\\s*)(\\{)(\\{)(\"[^\"]+\")", "$1$2$4");
        
        // 3. 修复缺失的逗号
        fixed = fixed.replaceAll("(\\})\\s*(\\{)", "$1,$2");
        
        // 4. 修复多余的逗号
        fixed = fixed.replaceAll(",\\s*([\\]}])", "$1");
        
        // 5. 修复引号问题
        fixed = fixed.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)(\\s*:)", "$1\"$2\"$3");
        
        // 6. 移除控制字符（保留必要的空格、换行、制表符）
        fixed = fixed.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        return fixed;
    }
    
    /**
     * 修复严重损坏的JSON
     * 专门处理阿里云千问VL返回的多余花括号问题
     */
    private String repairBrokenJson(String brokenJson) {
        if (brokenJson == null || brokenJson.trim().isEmpty()) {
            return brokenJson;
        }
        
        logger.debug("开始高级JSON修复，原始长度: {}", brokenJson.length());
        
        try {
            // 1. 先处理Unicode转义字符
            String repaired = unescapeUnicode(brokenJson);
            
            // 2. 使用正则表达式修复特定的格式问题
            repaired = fixSpecificJsonIssues(repaired);
            
            logger.debug("高级JSON修复完成，修复后长度: {}", repaired.length());
            
            return repaired;
            
        } catch (Exception e) {
            logger.error("高级JSON修复失败: {}", e.getMessage());
            // 作为最后的手段，尝试简单的字符替换修复
            return simpleJsonRepair(brokenJson);
        }
    }
    
    /**
     * 修复特定的JSON格式问题
     * 主要处理阿里云返回的双花括号问题：[ { {"bbox_2d": -> [ {"bbox_2d":
     */
    private String fixSpecificJsonIssues(String json) {
        String fixed = json;
        
        logger.debug("开始修复特定JSON问题，原始内容: {}", json);
        
        // 1. 修复最常见的问题：[ { {"bbox_2d": 变成 [ {"bbox_2d":
        // 匹配 [ 空格 { 空格 { " 的模式
        fixed = fixed.replaceAll("(\\[\\s*\\{\\s*)(\\{)(\")", "$1$3");
        
        // 2. 修复对象之间的双花括号：}, { {"bbox_2d": 变成 }, {"bbox_2d":
        fixed = fixed.replaceAll("(\\},\\s*\\{\\s*)(\\{)(\")", "$1$3");
        
        // 3. 修复行内的双花括号：{ {"bbox_2d": 变成 {"bbox_2d":
        fixed = fixed.replaceAll("(\\{\\s*)(\\{)(\")", "$1$3");
        
        // 4. 修复多余的花括号组合：{{ 变成 {
        fixed = fixed.replaceAll("\\{\\{", "{");
        
        // 5. 修复多余的花括号组合：}} 变成 }
        fixed = fixed.replaceAll("\\}\\}", "}");
        
        logger.debug("JSON修复完成，修复后内容: {}", fixed);
        
        return fixed;
    }
    
    
    /**
     * 简单的JSON修复方法（最后的手段）
     * 使用字符级别的处理，更精确地修复JSON格式问题
     */
    private String simpleJsonRepair(String json) {
        logger.debug("使用简单JSON修复方法");
        
        if (json == null || json.trim().isEmpty()) {
            return json;
        }
        
        // 处理Unicode
        String repaired = unescapeUnicode(json);
        
        // 使用字符级别的修复
        StringBuilder result = new StringBuilder();
        char[] chars = repaired.toCharArray();
        
        boolean inString = false;
        boolean escapeNext = false;
        char prevChar = ' ';
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            char nextChar = (i < chars.length - 1) ? chars[i + 1] : ' ';
            
            // 处理字符串内容
            if (c == '"' && !escapeNext) {
                inString = !inString;
                result.append(c);
                prevChar = c;
                escapeNext = false;
                continue;
            }
            
            if (inString) {
                if (c == '\\' && !escapeNext) {
                    escapeNext = true;
                } else {
                    escapeNext = false;
                }
                result.append(c);
                prevChar = c;
                continue;
            }
            
            // 在JSON结构中，修复多余的花括号
            if (c == '{' && nextChar == '{') {
                // 跳过第二个花括号
                result.append(c);
                prevChar = c;
                continue;
            }
            
            if (c == '{' && prevChar == '{') {
                // 跳过这个花括号，它是多余的
                continue;
            }
            
            // 处理其他字符
            result.append(c);
            prevChar = c;
        }
        
        String finalResult = result.toString();
        
        // 移除多余的控制字符
        finalResult = finalResult.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        logger.debug("简单JSON修复完成，原始长度: {}, 修复后长度: {}", json.length(), finalResult.length());
        
        return finalResult;
    }

    /**
     * 将第三方OCR的JSON节点转换为CharBox对象列表
     * 
     * 第三方格式: { "bbox_2d": [x1, y1, x2, y2], "category": "Section-header", "text": "第十五条 不可抗力" }
     * 注意：阿里云千问返回的bbox坐标可能是归一化的（0-1之间），需要转换为绝对像素坐标
     * CharBox格式: 每个字符一个CharBox对象，使用public final字段
     */
    private List<CharBox> convertToCharBoxes(JsonNode blockNode, int pageNumber, int imageWidth, int imageHeight) {
        List<CharBox> charBoxes = new ArrayList<>();
        
        try {
            // 提取边界框
            JsonNode bboxNode = blockNode.get("bbox_2d");
            if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() != 4) {
                logger.warn("无效的边界框数据: {}", bboxNode);
                return charBoxes;
            }
            
            double[] rawBbox = new double[4];
            for (int i = 0; i < 4; i++) {
                rawBbox[i] = bboxNode.get(i).asDouble();
            }
            
            // 转换坐标：如果是归一化坐标（0-1之间），转换为绝对像素坐标
            double[] bbox = convertToAbsoluteCoordinates(rawBbox, imageWidth, imageHeight);
            
            logger.debug("坐标转换: 原始bbox={}, 图像尺寸={}x{}, 转换后bbox={}", 
                    java.util.Arrays.toString(rawBbox), imageWidth, imageHeight, java.util.Arrays.toString(bbox));
            
            // 提取类别
            String category = blockNode.has("category") ? blockNode.get("category").asText() : "Text";
            String mappedCategory = mapCategoryToStandard(category);
            
            // 提取文本内容
            String text = "";
            if (blockNode.has("text")) {
                JsonNode textNode = blockNode.get("text");
                if (!textNode.isNull()) {
                    text = textNode.asText();
                }
            }
            
            // 跳过Picture类别的空文本块
            if ("Picture".equals(category) && (text == null || text.trim().isEmpty())) {
                logger.debug("跳过Picture类别的空文本块");
                return charBoxes;
            }
            
            // 将文本拆分为字符，每个字符创建一个CharBox
            // 这样做是为了与现有的文本处理逻辑兼容
            if (text != null && !text.trim().isEmpty()) {
                char[] chars = text.toCharArray();
                double charWidth = (bbox[2] - bbox[0]) / chars.length; // 平均字符宽度
                
                for (int i = 0; i < chars.length; i++) {
                    // 计算每个字符的边界框
                    double[] charBbox = new double[4];
                    charBbox[0] = bbox[0] + i * charWidth;        // x1
                    charBbox[1] = bbox[1];                        // y1
                    charBbox[2] = bbox[0] + (i + 1) * charWidth;  // x2
                    charBbox[3] = bbox[3];                        // y2
                    
                    CharBox charBox = new CharBox(pageNumber, chars[i], charBbox, mappedCategory);
                    charBoxes.add(charBox);
                }
            }
            
            logger.debug("转换文本块: bbox={}, category={}, text='{}', 生成{}个字符", 
                    java.util.Arrays.toString(bbox), mappedCategory, text, charBoxes.size());
            
            return charBoxes;
            
        } catch (Exception e) {
            logger.warn("转换文本块失败: {}", blockNode, e);
            return charBoxes;
        }
    }

    /**
     * 将坐标转换为绝对像素坐标
     * 
     * QWen-VL模型的bbox坐标格式：需要先除以1000再乘以图像尺寸
     * 转换公式：absolute_coord = (raw_coord / 1000) * image_size
     * 
     * @param rawBbox 原始边界框坐标 [x1, y1, x2, y2]
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 转换后的绝对坐标
     */
    private double[] convertToAbsoluteCoordinates(double[] rawBbox, int imageWidth, int imageHeight) {
        double[] absoluteBbox = new double[4];
        
        // QWen-VL坐标转换：先除以1000，再乘以图像尺寸
        // x坐标使用图像宽度，y坐标使用图像高度
        absoluteBbox[0] = (rawBbox[0] / 1000.0) * imageWidth;   // x1
        absoluteBbox[1] = (rawBbox[1] / 1000.0) * imageHeight;  // y1  
        absoluteBbox[2] = (rawBbox[2] / 1000.0) * imageWidth;   // x2
        absoluteBbox[3] = (rawBbox[3] / 1000.0) * imageHeight;  // y2
        
        // 确保坐标的有效性（不超出图像边界）
        absoluteBbox[0] = Math.max(0, Math.min(absoluteBbox[0], imageWidth));   // x1
        absoluteBbox[1] = Math.max(0, Math.min(absoluteBbox[1], imageHeight));  // y1
        absoluteBbox[2] = Math.max(absoluteBbox[0], Math.min(absoluteBbox[2], imageWidth));   // x2
        absoluteBbox[3] = Math.max(absoluteBbox[1], Math.min(absoluteBbox[3], imageHeight));  // y2
        
        logger.debug("QWen-VL坐标转换完成: 原始坐标除以1000后乘以图像尺寸");
        
        return absoluteBbox;
    }
    

    /**
     * 将第三方OCR的类别映射到标准类别
     * 确保与现有系统的类别体系兼容
     */
    private String mapCategoryToStandard(String thirdPartyCategory) {
        if (thirdPartyCategory == null) {
            return "Text";
        }
        
        // 映射规则：第三方类别 -> 标准类别
        switch (thirdPartyCategory) {
            case "Section-header":
                return "Section-header";
            case "Text":
                return "Text";
            case "Title":
                return "Title";
            case "List-item":
                return "List-item";
            case "Table":
                return "Table";
            case "Caption":
                return "Caption";
            case "Footnote":
                return "Footnote";
            case "Formula":
                return "Formula";
            case "Picture":
                return "Picture";
            case "Page-header":
                return "Page-header";
            case "Page-footer":
                return "Page-footer";
            default:
                logger.debug("未知类别 '{}', 映射为 'Text'", thirdPartyCategory);
                return "Text";
        }
    }

    /**
     * 获取配置信息
     */
    public ThirdPartyOcrConfig getConfig() {
        return config;
    }
}
