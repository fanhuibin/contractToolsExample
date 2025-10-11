package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强的规则引擎
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedRuleEngine {

    private final KeywordAnchorMatcher keywordAnchorMatcher;
    private final ContextBoundaryMatcher contextBoundaryMatcher;
    private final TableCellMatcher tableCellMatcher;

    /**
     * 提取信息
     *
     * @param text    要提取的文本
     * @param rule    提取规则
     * @param debug   是否开启调试模式
     * @return 提取结果
     */
    public ExtractionResult extract(String text, ExtractionRule rule, boolean debug) {
        if (StrUtil.isBlank(text)) {
            return ExtractionResult.failure("文本不能为空");
        }

        if (rule == null || rule.getRuleType() == null) {
            return ExtractionResult.failure("规则不能为空");
        }

        if (!rule.getEnabled()) {
            return ExtractionResult.failure("规则未启用");
        }

        try {
            switch (rule.getRuleType()) {
                case KEYWORD_ANCHOR:
                    return keywordAnchorMatcher.extract(text, rule.getConfig(), debug);

                case CONTEXT_BOUNDARY:
                    return contextBoundaryMatcher.extract(text, rule.getConfig(), debug);

                case REGEX_PATTERN:
                    return extractByRegexPattern(text, rule.getConfig(), debug);

                case TABLE_CELL:
                    return tableCellMatcher.extract(text, rule.getConfig(), debug);

                default:
                    return ExtractionResult.failure("不支持的规则类型: " + rule.getRuleType());
            }
        } catch (Exception e) {
            log.error("规则引擎提取失败", e);
            return ExtractionResult.failure("提取失败: " + e.getMessage());
        }
    }

    /**
     * 批量提取（应用多个规则，返回第一个成功的）
     */
    public ExtractionResult extractWithRules(String text, List<ExtractionRule> rules, boolean debug) {
        if (rules == null || rules.isEmpty()) {
            return ExtractionResult.failure("规则列表为空");
        }

        // 按优先级排序
        List<ExtractionRule> sortedRules = new ArrayList<>(rules);
        sortedRules.sort((r1, r2) -> r2.getPriority().compareTo(r1.getPriority()));

        ExtractionResult lastResult = null;
        for (ExtractionRule rule : sortedRules) {
            if (!rule.getEnabled()) {
                continue;
            }

            ExtractionResult result = extract(text, rule, debug);
            if (result.getSuccess()) {
                result.setMatchedRuleId(rule.getId());
                return result;
            }

            lastResult = result;
        }

        // 所有规则都失败
        return lastResult != null ? lastResult : ExtractionResult.failure("所有规则都未匹配");
    }

    /**
     * 纯正则提取（支持occurrence和returnAll）
     */
    private ExtractionResult extractByRegexPattern(String text, JSONObject config, boolean debug) {
        ExtractionResult result = new ExtractionResult();
        result.setSuccess(false);
        result.setDebugInfo(new ArrayList<>());

        try {
            String pattern = config.getString("pattern");
            if (StrUtil.isBlank(pattern)) {
                return ExtractionResult.failure("正则表达式不能为空");
            }

            Integer group = config.getInteger("group");
            if (group == null) {
                group = 0;  // 默认group 0
            }

            Boolean multiline = config.getBoolean("multiline");
            if (multiline == null) {
                multiline = false;
            }

            Integer occurrence = config.getInteger("occurrence");
            if (occurrence == null) {
                occurrence = 1;
            }

            Boolean returnAll = config.getBoolean("returnAll");
            if (returnAll == null) {
                returnAll = false;
            }

            if (debug) {
                result.addDebugInfo("正则: " + pattern);
                result.addDebugInfo("捕获组: " + group);
                result.addDebugInfo("occurrence: " + occurrence);
            }

            int flags = 0;
            if (multiline) {
                flags = Pattern.MULTILINE | Pattern.DOTALL;
            }

            Pattern p = Pattern.compile(pattern, flags);
            Matcher matcher = p.matcher(text);

            List<String> allMatches = new ArrayList<>();
            String value = null;
            int count = 0;

            while (matcher.find()) {
                String matched = matcher.group(group);
                allMatches.add(matched);
                count++;

                if (count == occurrence) {
                    value = matched;
                    result.setStartPosition(matcher.start(group));
                    result.setEndPosition(matcher.end(group));
                }
            }

            if (returnAll && !allMatches.isEmpty()) {
                value = allMatches.get(0);
                result.setAllMatches(allMatches);
            } else if (value == null && count > 0) {
                if (debug) {
                    result.addDebugInfo("找到 " + count + " 个匹配，但请求第 " + occurrence + " 个");
                }
                return ExtractionResult.failure("找到 " + count + " 个匹配，但请求第 " + occurrence + " 个");
            }

            if (value != null) {
                result.setSuccess(true);
                result.setValue(value);
                result.setConfidence(100);

                if (debug) {
                    result.addDebugInfo("匹配成功: " + value);
                    if (returnAll) {
                        result.addDebugInfo("所有匹配项数量: " + allMatches.size());
                    }
                }

                return result;
            }

            if (debug) {
                result.addDebugInfo("正则未匹配");
            }

            return ExtractionResult.failure("正则表达式未匹配");

        } catch (Exception e) {
            log.error("正则提取失败", e);
            return ExtractionResult.failure("正则提取失败: " + e.getMessage());
        }
    }
}

