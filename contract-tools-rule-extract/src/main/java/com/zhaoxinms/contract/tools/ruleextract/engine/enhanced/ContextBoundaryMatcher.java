package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上下文边界匹配器
 * 
 * 配置格式：
 * {
 *   "startBoundary": "合同主要内容如下：",
 *   "endBoundary": "甲方（盖章）",
 *   "startOffset": 0,              // 开始边界偏移
 *   "endOffset": 0,                // 结束边界偏移
 *   "extractPattern": ".*?",       // 提取范围内匹配的pattern
 *   "multiline": true,             // 是否多行
 *   "greedy": false,               // 是否贪婪匹配
 *   "occurrence": 1                // 提取第几个匹配项
 * }
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class ContextBoundaryMatcher {

    public ExtractionResult extract(String text, JSONObject config, boolean debug) {
        ExtractionResult result = new ExtractionResult();
        result.setSuccess(false);
        result.setDebugInfo(new ArrayList<>());

        try {
            // 获取配置
            String startBoundary = config.getString("startBoundary");
            String endBoundary = config.getString("endBoundary");
            
            if (StrUtil.isBlank(startBoundary) && StrUtil.isBlank(endBoundary)) {
                return ExtractionResult.failure("开始边界和结束边界不能同时为空");
            }

            Integer startOffset = getOrDefault(config, "startOffset", 0);
            Integer endOffset = getOrDefault(config, "endOffset", 0);
            String extractPattern = config.getString("extractPattern");
            Boolean multiline = getOrDefault(config, "multiline", false);
            Boolean greedy = getOrDefault(config, "greedy", false);
            Integer occurrence = getOrDefault(config, "occurrence", 1);

            if (debug) {
                result.addDebugInfo("开始边界: " + startBoundary);
                result.addDebugInfo("结束边界: " + endBoundary);
            }

            // 确定提取范围
            int startPos = 0;
            int endPos = text.length();

            if (StrUtil.isNotBlank(startBoundary)) {
                startPos = text.indexOf(startBoundary);
                if (startPos == -1) {
                    return ExtractionResult.failure("未找到开始边界: " + startBoundary);
                }
                startPos += startBoundary.length() + startOffset;
            }

            if (StrUtil.isNotBlank(endBoundary)) {
                endPos = text.indexOf(endBoundary, startPos);
                if (endPos == -1) {
                    return ExtractionResult.failure("未找到结束边界: " + endBoundary);
                }
                endPos += endOffset;
            }

            if (startPos >= endPos) {
                return ExtractionResult.failure("开始位置大于结束位置");
            }

            String rangeText = text.substring(startPos, endPos);

            if (debug) {
                result.addDebugInfo("范围文本长度: " + rangeText.length());
                result.addDebugInfo("范围文本预览: " + rangeText.substring(0, Math.min(100, rangeText.length())));
            }

            // 如果有提取pattern，应用pattern
            String extracted = rangeText;
            
            if (StrUtil.isNotBlank(extractPattern)) {
                extracted = extractByPattern(rangeText, extractPattern, multiline, greedy, occurrence, debug, result);
                if (extracted == null) {
                    return ExtractionResult.failure("Pattern未匹配到内容");
                }
            }

            result.setSuccess(true);
            result.setValue(extracted.trim());
            result.setConfidence(90);
            result.setStartPosition(startPos);
            result.setEndPosition(endPos);

            if (debug) {
                result.addDebugInfo("提取成功: " + extracted.trim());
            }

            return result;

        } catch (Exception e) {
            log.error("上下文边界提取失败", e);
            return ExtractionResult.failure("提取失败: " + e.getMessage());
        }
    }

    /**
     * 按pattern提取（支持occurrence）
     */
    private String extractByPattern(String text, String patternStr, boolean multiline, 
                                    boolean greedy, int occurrence, boolean debug, ExtractionResult result) {
        try {
            int flags = 0;
            if (multiline) {
                flags = Pattern.MULTILINE | Pattern.DOTALL;
            }

            if (!greedy && !patternStr.contains("?")) {
                // 如果是非贪婪且pattern没有?，添加非贪婪修饰符
                patternStr = patternStr.replace("*", "*?").replace("+", "+?");
            }

            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(text);

            int count = 0;
            while (matcher.find()) {
                count++;
                if (count == occurrence) {
                    return matcher.group();
                }
            }

            if (debug && count > 0) {
                result.addDebugInfo("找到 " + count + " 个匹配，但请求第 " + occurrence + " 个");
            } else if (debug) {
                result.addDebugInfo("Pattern未匹配: " + patternStr);
            }

        } catch (Exception e) {
            if (debug) {
                result.addDebugInfo("Pattern匹配失败: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 获取配置值或默认值
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrDefault(JSONObject config, String key, T defaultValue) {
        if (config == null) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}

