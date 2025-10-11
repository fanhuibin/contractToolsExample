package com.zhaoxinms.contract.tools.ruleextract.engine.matcher;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 模式匹配器
 * 规则格式：
 * {
 *   "type": "pattern",
 *   "before": "甲方：",
 *   "after": "\n",
 *   "multiline": false
 * }
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class PatternMatcher {

    /**
     * 使用模式匹配
     * 匹配before和after之间的内容
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
            String before = ruleConfig.getString("before");
            String after = ruleConfig.getString("after");
            Boolean multiline = ruleConfig.getBoolean("multiline");
            if (multiline == null) {
                multiline = false;
            }

            if (StrUtil.isBlank(before) && StrUtil.isBlank(after)) {
                log.warn("模式规则需要至少指定before或after参数");
                return MatchResult.failed();
            }

            int startPos = 0;
            int endPos = content.length();

            // 查找开始位置
            if (StrUtil.isNotBlank(before)) {
                int beforeIndex = content.indexOf(before);
                if (beforeIndex < 0) {
                    return MatchResult.failed();
                }
                startPos = beforeIndex + before.length();
            }

            // 查找结束位置
            if (StrUtil.isNotBlank(after)) {
                int afterIndex = content.indexOf(after, startPos);
                if (afterIndex < 0) {
                    // 如果找不到结束标记，根据multiline决定是否失败
                    if (!multiline) {
                        return MatchResult.failed();
                    }
                } else {
                    endPos = afterIndex;
                }
            }

            // 提取值
            if (startPos < endPos) {
                String value = content.substring(startPos, endPos).trim();
                
                // 如果不是多行模式，只取第一行
                if (!multiline && value.contains("\n")) {
                    int lineEnd = value.indexOf("\n");
                    value = value.substring(0, lineEnd).trim();
                    endPos = startPos + lineEnd;
                }

                if (StrUtil.isNotBlank(value)) {
                    MatchResult result = MatchResult.success(value, startPos, endPos);
                    result.setConfidence(95); // 模式匹配置信度较高
                    return result;
                }
            }

            return MatchResult.failed();

        } catch (Exception e) {
            log.error("模式匹配失败", e);
            return MatchResult.failed();
        }
    }
}
