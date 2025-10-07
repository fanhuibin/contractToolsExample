package com.zhaoxinms.contract.tools.extract.core.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 格式处理器
 * 处理JSON和YAML格式的解析和生成，对应Python版本的FormatHandler
 * 支持DeepSeek-R1模型的思考内容过滤功能
 */
@Slf4j
public class FormatHandler {
    
    private static final String JSON_FORMAT = "json";
    private static final String YAML_FORMAT = "yaml";
    private static final String YML_FORMAT = "yml";
    
    // 代码块正则表达式
    private static final Pattern FENCE_PATTERN = Pattern.compile(
        "```(?<lang>[A-Za-z0-9_+-]+)?(?:\\s*\\n)?(?<body>[\\s\\S]*?)```",
        Pattern.MULTILINE
    );
    
    // DeepSeek-R1思考标签正则表达式
    // 支持完整的<think>...</think>标签对
    private static final Pattern THINK_PATTERN = Pattern.compile(
        "<think>([\\s\\S]*?)</think>",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 处理不完整的<think>标签（没有结束标签的情况）
    private static final Pattern INCOMPLETE_THINK_PATTERN = Pattern.compile(
        "<think>([\\s\\S]*?)$",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    
    public FormatHandler() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * 解析响应内容，提取结构化数据
     */
    public Map<String, Object> parseResponse(String response, String format) throws ExtractException {
        try {
            log.debug("原始响应长度: {}", response.length());
            
            // 首先过滤DeepSeek-R1的思考内容
            String filteredResponse = filterThinkingContent(response);
            log.debug("过滤思考内容后长度: {}", filteredResponse.length());
            
            // 然后尝试提取代码块
            String content = extractCodeBlock(filteredResponse, format);
            if (content == null) {
                content = filteredResponse.trim();
            }
            
            log.debug("最终解析内容长度: {}", content.length());
            
            // 根据格式解析
            return parseContent(content, format);
            
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            throw new ExtractException("解析响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 过滤DeepSeek-R1的思考内容
     * DeepSeek-R1会在响应中包含<think>...</think>标签，需要过滤掉
     * 支持完整标签对和不完整标签（被截断的情况）
     */
    private String filterThinkingContent(String response) throws ExtractException {
        if (response == null || response.isEmpty()) {
            return response;
        }
        
        String original = response;
        
        // 1. 首先移除完整的<think>...</think>标签对
        String filtered = THINK_PATTERN.matcher(response).replaceAll("");
        
        // 2. 然后处理不完整的<think>标签（没有结束标签的情况）
        filtered = INCOMPLETE_THINK_PATTERN.matcher(filtered).replaceAll("");
        
        // 3. 清理多余的空白字符
        filtered = filtered.trim();
        
        // 4. 如果过滤后为空或只有很少内容，尝试查找JSON/YAML代码块
        if (filtered.isEmpty() || filtered.length() < 10) {
            log.warn("过滤思考内容后剩余内容很少，尝试从原始响应中提取代码块");
            String codeBlock = extractCodeBlockFromRawResponse(original);
            if (codeBlock != null && !codeBlock.isEmpty()) {
                filtered = codeBlock;
                log.info("从原始响应中成功提取代码块，长度: {}", filtered.length());
            } else {
                // 最后的备用方案：如果完全没有有效内容，返回空的JSON对象
                log.error("无法从DeepSeek响应中提取任何有效内容，可能模型输出被截断或格式异常");
                log.debug("原始响应内容: {}", original.length() > 500 ? original.substring(0, 500) + "..." : original);
                throw new ExtractException("DeepSeek模型响应格式异常，无法提取有效的JSON/YAML内容。可能原因：1) 响应被截断 2) 模型输出格式错误 3) 思考内容没有正确结束");
            }
        }
        
        log.debug("思考内容过滤: 原长度={}, 过滤后长度={}", original.length(), filtered.length());
        
        return filtered;
    }
    
    /**
     * 从原始响应中强制提取代码块，即使包含思考内容
     */
    private String extractCodeBlockFromRawResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        Matcher matcher = FENCE_PATTERN.matcher(response);
        
        // 查找任何代码块，不管是否在思考标签内
        while (matcher.find()) {
            String lang = matcher.group("lang");
            String body = matcher.group("body");
            
            if (body != null && !body.trim().isEmpty()) {
                // 检查是否是有效的JSON/YAML
                String trimmedBody = body.trim();
                if (isLikelyJson(trimmedBody) || isLikelyYaml(trimmedBody)) {
                    log.debug("从原始响应中找到可能的{}代码块，长度: {}", lang != null ? lang : "未知", trimmedBody.length());
                    return trimmedBody;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查字符串是否像JSON格式
     */
    private boolean isLikelyJson(String content) {
        content = content.trim();
        return (content.startsWith("{") && content.endsWith("}")) || 
               (content.startsWith("[") && content.endsWith("]"));
    }
    
    /**
     * 检查字符串是否像YAML格式
     */
    private boolean isLikelyYaml(String content) {
        // 简单的YAML格式检查
        return content.contains(":") && !content.trim().startsWith("{");
    }
    
    /**
     * 从响应中提取代码块
     */
    private String extractCodeBlock(String response, String expectedFormat) {
        Matcher matcher = FENCE_PATTERN.matcher(response);
        
        while (matcher.find()) {
            String lang = matcher.group("lang");
            String body = matcher.group("body");
            
            // 如果指定了语言标签，检查是否匹配
            if (lang != null) {
                if (isFormatMatch(lang.toLowerCase(), expectedFormat)) {
                    return body.trim();
                }
            } else if (body != null && !body.trim().isEmpty()) {
                // 没有语言标签，尝试自动检测格式
                if (isValidFormat(body.trim(), expectedFormat)) {
                    return body.trim();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查格式是否匹配
     */
    private boolean isFormatMatch(String lang, String expectedFormat) {
        if (expectedFormat == null) {
            return true;
        }
        
        switch (expectedFormat.toLowerCase()) {
            case JSON_FORMAT:
                return "json".equals(lang) || "jsonl".equals(lang);
            case YAML_FORMAT:
            case YML_FORMAT:
                return "yaml".equals(lang) || "yml".equals(lang);
            default:
                return lang.equals(expectedFormat.toLowerCase());
        }
    }
    
    /**
     * 验证内容是否符合指定格式
     */
    private boolean isValidFormat(String content, String format) {
        try {
            parseContent(content, format);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 解析内容为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String content, String format) throws ExtractException {
        try {
            ObjectMapper mapper = getMapper(format);
            Object parsed = mapper.readValue(content, Object.class);
            
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            } else {
                throw new ExtractException("解析结果不是Map类型");
            }
            
        } catch (JsonProcessingException e) {
            throw new ExtractException("JSON/YAML解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取对应格式的ObjectMapper
     */
    private ObjectMapper getMapper(String format) {
        if (format == null) {
            return jsonMapper;
        }
        
        switch (format.toLowerCase()) {
            case YAML_FORMAT:
            case YML_FORMAT:
                return yamlMapper;
            default:
                return jsonMapper;
        }
    }
    
    /**
     * 将对象序列化为指定格式
     */
    public String serialize(Object obj, String format) throws ExtractException {
        try {
            ObjectMapper mapper = getMapper(format);
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ExtractException("序列化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成格式化的提示模板
     */
    public String generateFormatPrompt(String format) {
        String formatName = format != null ? format.toUpperCase() : "JSON";
        
        switch (formatName) {
            case "YAML":
            case "YML":
                return "请以YAML格式返回结果，使用```yaml代码块包围:\n```yaml\n# 在这里填写YAML格式的结果\n```";
            default:
                return "请以JSON格式返回结果，使用```json代码块包围:\n```json\n{\n  // 在这里填写JSON格式的结果\n}\n```";
        }
    }
    
    /**
     * 检测内容格式
     */
    public String detectFormat(String content) {
        content = content.trim();
        
        // 检查是否以JSON格式开始
        if (content.startsWith("{") || content.startsWith("[")) {
            try {
                jsonMapper.readTree(content);
                return JSON_FORMAT;
            } catch (JsonProcessingException e) {
                // 继续检查其他格式
            }
        }
        
        // 检查是否为YAML格式
        try {
            yamlMapper.readTree(content);
            return YAML_FORMAT;
        } catch (JsonProcessingException e) {
            // 继续检查其他格式
        }
        
        // 默认返回JSON
        return JSON_FORMAT;
    }
}
