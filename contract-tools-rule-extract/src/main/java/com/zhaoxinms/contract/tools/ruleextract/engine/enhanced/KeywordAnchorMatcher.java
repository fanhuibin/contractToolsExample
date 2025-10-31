package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词锚点匹配器
 * 
 * 配置格式：
 * {
 *   "anchor": "合同名称",
 *   "anchorType": "exact",        // exact/fuzzy/regex
 *   "direction": "after",          // before/after/both
 *   "extractMethod": "regex",      // regex/line/paragraph/delimiter
 *   "pattern": "[^，。；\\n]+",
 *   "maxDistance": 100,
 *   "trim": true,
 *   "ignoreCase": false,
 *   "caseSensitive": true,
 *   "multiline": false,
 *   "occurrence": 1,               // 提取第几个匹配项（1-based）
 *   "delimiter": "："
 * }
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class KeywordAnchorMatcher {

    /**
     * 锚点查找结果
     */
    private static class AnchorResult {
        int position;
        String matchedKeyword;
        
        AnchorResult(int position, String matchedKeyword) {
            this.position = position;
            this.matchedKeyword = matchedKeyword;
        }
    }

    public ExtractionResult extract(String text, JSONObject config, boolean debug) {
        ExtractionResult result = new ExtractionResult();
        result.setSuccess(false);
        result.setDebugInfo(new ArrayList<>());

        try {
            // 获取配置
            String anchor = config.getString("anchor");
            if (StrUtil.isBlank(anchor)) {
                return ExtractionResult.failure("锚点关键词不能为空");
            }

            String direction = getOrDefault(config, "direction", "after");
            String extractMethod = getOrDefault(config, "extractMethod", "regex");
            String pattern = config.getString("pattern");
            Integer maxDistance = getOrDefault(config, "maxDistance", 200);
            Boolean trim = getOrDefault(config, "trim", true);
            Boolean ignoreCase = getOrDefault(config, "ignoreCase", false);
            Boolean caseSensitive = getOrDefault(config, "caseSensitive", true);
            Boolean multiline = getOrDefault(config, "multiline", false);
            Integer occurrence = getOrDefault(config, "occurrence", 1);
            Integer anchorOccurrence = getOrDefault(config, "anchorOccurrence", 1);

            // 【关键优化】标准化文本和关键词的标点符号，解决全角/半角标点符号不匹配的问题
            String normalizedText = TextNormalizer.normalizePunctuation(text);
            String normalizedAnchor = TextNormalizer.normalizePunctuation(anchor);
            
            if (debug) {
                result.addDebugInfo("原始锚点: " + anchor);
                if (!anchor.equals(normalizedAnchor)) {
                    result.addDebugInfo("标准化锚点: " + normalizedAnchor + " (标点符号已标准化)");
                } else {
                    result.addDebugInfo("标准化锚点: " + normalizedAnchor);
                }
                result.addDebugInfo("方向: " + direction);
                result.addDebugInfo("提取方法: " + extractMethod);
                result.addDebugInfo("正则表达式: " + pattern);
                if (anchorOccurrence > 1) {
                    result.addDebugInfo("锚点序号: " + anchorOccurrence);
                }
            }

            // 查找锚点位置（使用标准化后的文本和关键词）
            AnchorResult anchorResult = findAnchor(normalizedText, normalizedAnchor, ignoreCase, anchorOccurrence);
            if (anchorResult == null || anchorResult.position == -1) {
                if (debug) {
                    result.addDebugInfo("未找到锚点关键词");
                }
                return ExtractionResult.failure("未找到锚点关键词: " + anchor);
            }

            if (debug) {
                result.addDebugInfo("锚点: " + anchorResult.matchedKeyword);
                result.addDebugInfo("锚点位置: " + anchorResult.position);
            }
            
            // 添加锚点查找的详细日志
            log.info("锚点匹配详情: anchor='{}', anchorOccurrence={}, foundPosition={}, matchedKeyword='{}'", 
                anchor, anchorOccurrence, anchorResult.position, anchorResult.matchedKeyword);
            
            // 显示锚点周围的文本用于调试
            int contextStart = Math.max(0, anchorResult.position - 50);
            int contextEnd = Math.min(normalizedText.length(), anchorResult.position + anchorResult.matchedKeyword.length() + 100);
            String context = normalizedText.substring(contextStart, contextEnd);
            log.info("锚点上下文: {}", context);

            // 根据方向获取搜索范围（使用实际匹配的关键词长度）
            int anchorPos = anchorResult.position;
            int anchorLen = anchorResult.matchedKeyword.length();
            
            // 计算搜索范围在原文中的起始位置
            int searchRangeStartPos = calculateSearchRangeStart(anchorPos, anchorLen, direction, maxDistance);
            
            // 使用标准化后的文本获取搜索范围
            String searchText = getSearchRange(normalizedText, anchorPos, anchorLen, direction, maxDistance);
            if (StrUtil.isBlank(searchText)) {
                return ExtractionResult.failure("搜索范围为空");
            }

            if (debug) {
                result.addDebugInfo("搜索范围: " + searchText.substring(0, Math.min(100, searchText.length())) + "...");
                result.addDebugInfo("搜索范围起始位置: " + searchRangeStartPos);
                result.addDebugInfo("搜索范围长度: " + searchText.length());
            }

            // 根据提取方法提取内容
            String extracted = null;
            
            // 添加搜索范围的详细日志
            log.info("搜索范围详情: method={}, pattern={}, searchText='{}'", 
                extractMethod, pattern, searchText.length() > 200 ? searchText.substring(0, 200) + "..." : searchText);
            
            switch (extractMethod) {
                case "regex":
                    extracted = extractByRegex(searchText, pattern, multiline, occurrence, debug, result);
                    break;
                case "line":
                    extracted = extractByLine(searchText, pattern, multiline, debug, result);
                    break;
                case "delimiter":
                    String delimiter = getOrDefault(config, "delimiter", "：");
                    extracted = extractByDelimiter(searchText, delimiter, pattern, multiline, debug, result);
                    break;
                default:
                    extracted = extractByRegex(searchText, pattern, multiline, occurrence, debug, result);
            }

            if (extracted == null || extracted.trim().isEmpty()) {
                log.warn("提取失败: extracted={}, searchText长度={}", extracted, searchText.length());
                return ExtractionResult.failure("未提取到内容");
            }
            
            log.info("提取成功: extracted='{}'", extracted);

            // 清理结果
            if (trim) {
                extracted = extracted.trim();
            }

            result.setSuccess(true);
            result.setValue(extracted);
            
            // 在搜索范围内查找提取内容的位置，然后加上搜索范围的起始位置得到绝对位置
            int relativePos = searchText.indexOf(extracted);
            if (relativePos != -1) {
                int absoluteStartPos = searchRangeStartPos + relativePos;
                result.setStartPosition(absoluteStartPos);
                result.setEndPosition(absoluteStartPos + extracted.length());
                if (debug) {
                    result.addDebugInfo("提取内容绝对位置: " + absoluteStartPos + " - " + (absoluteStartPos + extracted.length()));
                }
            } else {
                // 如果在搜索范围内找不到（可能是因为trim等处理），尝试在原文中查找
                int actualStartPos = text.indexOf(extracted, searchRangeStartPos);
                if (actualStartPos != -1 && actualStartPos < searchRangeStartPos + searchText.length()) {
                    result.setStartPosition(actualStartPos);
                    result.setEndPosition(actualStartPos + extracted.length());
                } else {
                    // 如果还是找不到，使用搜索范围起始位置作为fallback
                    result.setStartPosition(searchRangeStartPos);
                    result.setEndPosition(searchRangeStartPos + extracted.length());
                }
            }

            if (debug) {
                result.addDebugInfo("提取成功: " + extracted);
            }

            return result;

        } catch (Exception e) {
            log.error("关键词锚点提取失败", e);
            return ExtractionResult.failure("提取失败: " + e.getMessage());
        }
    }

    /**
     * 查找锚点位置（支持用|分隔多个关键词和锚点序号）
     * 返回指定序号的关键词及其位置
     */
    private AnchorResult findAnchor(String text, String anchor, boolean ignoreCase, int anchorOccurrence) {
        // 检查是否包含多个关键词（用|分隔）
        if (anchor.contains("|")) {
            String[] keywords = anchor.split("\\|");
            List<AnchorResult> allMatches = new ArrayList<>();
            
            // 查找所有关键词的所有匹配位置
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (StrUtil.isBlank(keyword)) {
                    continue;
                }
                
                String searchText = ignoreCase ? text.toLowerCase() : text;
                String searchKeyword = ignoreCase ? keyword.toLowerCase() : keyword;
                
                int pos = 0;
                while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
                    allMatches.add(new AnchorResult(pos, keyword));
                    pos += searchKeyword.length();
                }
            }
            
            // 按位置排序
            allMatches.sort((a, b) -> Integer.compare(a.position, b.position));
            
            // 返回指定序号的匹配
            if (anchorOccurrence > 0 && anchorOccurrence <= allMatches.size()) {
                return allMatches.get(anchorOccurrence - 1);
            }
            
            return null;
        }
        
        // 单个关键词，支持序号
        String searchText = ignoreCase ? text.toLowerCase() : text;
        String searchKeyword = ignoreCase ? anchor.toLowerCase() : anchor;
        
        int pos = 0;
        int count = 0;
        List<Integer> allPositions = new ArrayList<>();
        
        while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
            count++;
            allPositions.add(pos);
            log.info("找到锚点 '{}' 第{}次出现，位置: {}", anchor, count, pos);
            
            if (count == anchorOccurrence) {
                log.info("选择第{}次出现的锚点，位置: {}", anchorOccurrence, pos);
                return new AnchorResult(pos, anchor);
            }
            pos += searchKeyword.length();
        }
        
        log.warn("锚点 '{}' 总共找到{}次，但请求第{}次出现。所有位置: {}", 
            anchor, count, anchorOccurrence, allPositions);
        
        return null;
    }

    /**
     * 计算搜索范围在原文中的起始位置
     */
    private int calculateSearchRangeStart(int anchorPos, int anchorLen, String direction, int maxDistance) {
        switch (direction) {
            case "before":
                return Math.max(0, anchorPos - maxDistance);
            case "after":
                return anchorPos + anchorLen;
            case "both":
                return Math.max(0, anchorPos - maxDistance);
            default:
                return anchorPos + anchorLen;
        }
    }
    
    /**
     * 获取搜索范围
     */
    private String getSearchRange(String text, int anchorPos, int anchorLen, String direction, int maxDistance) {
        int textLen = text.length();
        
        switch (direction) {
            case "before":
                int start = Math.max(0, anchorPos - maxDistance);
                return text.substring(start, anchorPos);
                
            case "after":
                int end = Math.min(textLen, anchorPos + anchorLen + maxDistance);
                return text.substring(anchorPos + anchorLen, end);
                
            case "both":
                int startBoth = Math.max(0, anchorPos - maxDistance);
                int endBoth = Math.min(textLen, anchorPos + anchorLen + maxDistance);
                return text.substring(startBoth, endBoth);
                
            default:
                return text.substring(anchorPos + anchorLen, Math.min(textLen, anchorPos + anchorLen + maxDistance));
        }
    }

    /**
     * 正则提取（支持occurrence）
     */
    private String extractByRegex(String text, String patternStr, boolean multiline, int occurrence, boolean debug, ExtractionResult result) {
        if (StrUtil.isBlank(patternStr)) {
            patternStr = ".+?(?=\\s|$|\\n)";  // 默认pattern：提取到空白或结束
        }

        try {
            int flags = 0;
            if (multiline) {
                flags = Pattern.MULTILINE | Pattern.DOTALL;
            }
            
            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(text);
            
            int count = 0;
            while (matcher.find()) {
                count++;
                if (count == occurrence) {
                    String matched = matcher.group();
                    if (debug) {
                        result.addDebugInfo("正则匹配成功: " + matched);
                    }
                    return matched;
                }
            }
            
            if (debug) {
                result.addDebugInfo("正则匹配结果: 找到 " + count + " 个匹配，请求第 " + occurrence + " 个");
                if (count == 0) {
                    result.addDebugInfo("正则匹配失败，可能的原因：");
                    result.addDebugInfo("1. 正则表达式不匹配文本内容");
                    result.addDebugInfo("2. 搜索范围内没有符合条件的文本");
                    result.addDebugInfo("3. 文本格式与预期不符");
                    result.addDebugInfo("4. 搜索文本: '" + text + "'");
                    result.addDebugInfo("5. 正则表达式: '" + patternStr + "'");
                }
            }
        } catch (Exception e) {
            if (debug) {
                result.addDebugInfo("正则匹配失败: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 按行提取（支持正则后处理）
     */
    private String extractByLine(String text, String pattern, boolean multiline, boolean debug, ExtractionResult result) {
        // 首先提取整行
        int newLinePos = text.indexOf('\n');
        String line;
        if (newLinePos > 0) {
            line = text.substring(0, newLinePos);
        } else {
            line = text;
        }
        
        if (debug) {
            result.addDebugInfo("提取的行: " + line);
        }
        
        // 如果有pattern，对行内容应用正则表达式进一步提取
        if (StrUtil.isNotBlank(pattern)) {
            if (debug) {
                result.addDebugInfo("应用正则表达式: " + pattern);
            }
            String extracted = extractByRegex(line, pattern, multiline, 1, debug, result);
            if (extracted != null) {
                return extracted;
            } else {
                if (debug) {
                    result.addDebugInfo("正则表达式匹配失败，不返回原始行内容");
                }
                return null; // 正则匹配失败时返回null，而不是原始行
            }
        }
        
        return line;
    }

    /**
     * 按分隔符提取（支持正则后处理）
     */
    private String extractByDelimiter(String text, String delimiter, String pattern, boolean multiline, boolean debug, ExtractionResult result) {
        // 查找分隔符
        int delimiterPos = text.indexOf(delimiter);
        String extracted;
        
        if (delimiterPos == -1) {
            extracted = text;
        } else {
            // 从分隔符后开始
            String afterDelimiter = text.substring(delimiterPos + delimiter.length());
            
            // 提取到下一个常见分隔符或换行
            String[] endMarkers = {"\n", "。", "；", "，", " "};
            int endPos = afterDelimiter.length();
            
            for (String marker : endMarkers) {
                int pos = afterDelimiter.indexOf(marker);
                if (pos > 0 && pos < endPos) {
                    endPos = pos;
                }
            }
            
            extracted = afterDelimiter.substring(0, endPos);
        }
        
        if (debug) {
            result.addDebugInfo("分隔符提取结果: " + extracted);
        }
        
        // 如果有pattern，对提取的内容应用正则表达式进一步处理
        if (StrUtil.isNotBlank(pattern)) {
            if (debug) {
                result.addDebugInfo("应用正则表达式: " + pattern);
            }
            String regexResult = extractByRegex(extracted, pattern, multiline, 1, debug, result);
            if (regexResult != null) {
                return regexResult;
            } else {
                if (debug) {
                    result.addDebugInfo("正则表达式匹配失败，不返回原始内容");
                }
                return null; // 正则匹配失败时返回null，而不是原始内容
            }
        }
        
        return extracted;
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

