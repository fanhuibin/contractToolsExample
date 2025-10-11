package com.zhaoxinms.contract.tools.ruleextract.engine.matcher;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式匹配器
 * 规则格式：
 * {
 *   "type": "regex",
 *   "pattern": "合同编号[:：]\\s*([A-Z0-9-]+)",
 *   "group": 1,
 *   "flags": "i"
 * }
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class RegexMatcher {

    /**
     * 使用正则表达式匹配
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
            String pattern = ruleConfig.getString("pattern");
            if (StrUtil.isBlank(pattern)) {
                log.warn("正则表达式规则缺少pattern参数");
                return MatchResult.failed();
            }

            // 获取捕获组索引（默认为1）
            Integer group = ruleConfig.getInteger("group");
            if (group == null) {
                group = 1;
            }

            // 获取标志位
            int flags = 0;
            String flagsStr = ruleConfig.getString("flags");
            if (StrUtil.isNotBlank(flagsStr)) {
                if (flagsStr.contains("i")) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                if (flagsStr.contains("m")) {
                    flags |= Pattern.MULTILINE;
                }
                if (flagsStr.contains("s")) {
                    flags |= Pattern.DOTALL;
                }
            }

            Pattern p = Pattern.compile(pattern, flags);
            Matcher m = p.matcher(content);

            if (m.find()) {
                String value = m.group(group);
                int startPos = m.start(group);
                int endPos = m.end(group);

                MatchResult result = MatchResult.success(value, startPos, endPos);
                result.setConfidence(100);
                return result;
            }

            return MatchResult.failed();

        } catch (Exception e) {
            log.error("正则表达式匹配失败", e);
            return MatchResult.failed();
        }
    }
}
