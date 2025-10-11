package com.zhaoxinms.contract.tools.ruleextract.engine.matcher;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * 关键词匹配器
 * 规则格式：
 * {
 *   "type": "keyword",
 *   "keywords": ["合同编号", "合同号"],
 *   "direction": "after",
 *   "maxDistance": 50,
 *   "stopWords": ["：", ":", "\n"]
 * }
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class KeywordMatcher {

    /**
     * 使用关键词匹配
     * 
     * @param content 文本内容
     * @param ruleConfig 规则配置
     * @return 匹配结果
     */
    public MatchResult match(String content, JSONObject ruleConfig) {
        if (StrUtil.isBlank(content)) {
            return MatchResult.failed();
        }

        try {
            JSONArray keywordsArray = ruleConfig.getJSONArray("keywords");
            if (keywordsArray == null || keywordsArray.isEmpty()) {
                log.warn("关键词规则缺少keywords参数");
                return MatchResult.failed();
            }

            // 获取关键词列表
            Set<String> keywords = new HashSet<>();
            for (int i = 0; i < keywordsArray.size(); i++) {
                keywords.add(keywordsArray.getString(i));
            }

            // 获取方向（before/after，默认为after）
            String direction = ruleConfig.getString("direction");
            if (StrUtil.isBlank(direction)) {
                direction = "after";
            }

            // 获取最大距离（默认50个字符）
            Integer maxDistance = ruleConfig.getInteger("maxDistance");
            if (maxDistance == null) {
                maxDistance = 50;
            }

            // 获取停止词
            Set<String> stopWords = new HashSet<>();
            JSONArray stopWordsArray = ruleConfig.getJSONArray("stopWords");
            if (stopWordsArray != null) {
                for (int i = 0; i < stopWordsArray.size(); i++) {
                    stopWords.add(stopWordsArray.getString(i));
                }
            }

            // 查找关键词
            for (String keyword : keywords) {
                int index = content.indexOf(keyword);
                if (index >= 0) {
                    return extractValue(content, index, keyword.length(), direction, maxDistance, stopWords);
                }
            }

            return MatchResult.failed();

        } catch (Exception e) {
            log.error("关键词匹配失败", e);
            return MatchResult.failed();
        }
    }

    /**
     * 从关键词位置提取值
     */
    private MatchResult extractValue(String content, int keywordIndex, int keywordLength,
                                    String direction, int maxDistance, Set<String> stopWords) {
        int startPos;
        int endPos;

        if ("after".equals(direction)) {
            // 从关键词后面提取
            startPos = keywordIndex + keywordLength;
            endPos = Math.min(startPos + maxDistance, content.length());

            // 跳过停止词
            while (startPos < endPos && containsStopWord(content.substring(startPos, startPos + 1), stopWords)) {
                startPos++;
            }

            // 查找结束位置
            for (int i = startPos; i < endPos; i++) {
                if (containsStopWord(content.substring(i, i + 1), stopWords)) {
                    endPos = i;
                    break;
                }
            }

        } else {
            // 从关键词前面提取
            endPos = keywordIndex;
            startPos = Math.max(endPos - maxDistance, 0);

            // 跳过停止词
            while (endPos > startPos && containsStopWord(content.substring(endPos - 1, endPos), stopWords)) {
                endPos--;
            }

            // 查找开始位置
            for (int i = endPos - 1; i >= startPos; i--) {
                if (containsStopWord(content.substring(i, i + 1), stopWords)) {
                    startPos = i + 1;
                    break;
                }
            }
        }

        if (startPos < endPos) {
            String value = content.substring(startPos, endPos).trim();
            if (StrUtil.isNotBlank(value)) {
                MatchResult result = MatchResult.success(value, startPos, endPos);
                result.setConfidence(90); // 关键词匹配置信度稍低
                return result;
            }
        }

        return MatchResult.failed();
    }

    /**
     * 判断是否包含停止词
     */
    private boolean containsStopWord(String text, Set<String> stopWords) {
        for (String stopWord : stopWords) {
            if (text.contains(stopWord)) {
                return true;
            }
        }
        return false;
    }
}
