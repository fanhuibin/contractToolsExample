package com.zhaoxinms.contract.tools.ruleextract.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhaoxinms.contract.tools.ruleextract.model.ExtractionRuleModel;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.engine.matcher.*;
import com.zhaoxinms.contract.tools.ruleextract.utils.FormatConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则引擎主类
 * 负责协调各种规则匹配器进行信息提取
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class RuleEngine {

    private final RegexMatcher regexMatcher;
    private final KeywordMatcher keywordMatcher;
    private final PositionMatcher positionMatcher;
    private final PatternMatcher patternMatcher;

    public RuleEngine() {
        this.regexMatcher = new RegexMatcher();
        this.keywordMatcher = new KeywordMatcher();
        this.positionMatcher = new PositionMatcher();
        this.patternMatcher = new PatternMatcher();
    }

    /**
     * 提取结果
     */
    public static class ExtractionResult {
        private final String fieldCode;
        private final String fieldName;
        private final String fieldType;
        private final Object value;
        private final String rawValue;
        private final Integer startPos;
        private final Integer endPos;
        private final Integer confidence;
        private final String matchedRule;

        public ExtractionResult(String fieldCode, String fieldName, String fieldType, 
                              Object value, String rawValue, Integer startPos, Integer endPos,
                              Integer confidence, String matchedRule) {
            this.fieldCode = fieldCode;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.value = value;
            this.rawValue = rawValue;
            this.startPos = startPos;
            this.endPos = endPos;
            this.confidence = confidence;
            this.matchedRule = matchedRule;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("fieldCode", fieldCode);
            json.put("fieldName", fieldName);
            json.put("fieldType", fieldType);
            json.put("value", value);
            json.put("rawValue", rawValue);
            json.put("startPos", startPos);
            json.put("endPos", endPos);
            json.put("confidence", confidence);
            json.put("matchedRule", matchedRule);
            return json;
        }

        public String getFieldCode() {
            return fieldCode;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldType() {
            return fieldType;
        }

        public Object getValue() {
            return value;
        }

        public String getRawValue() {
            return rawValue;
        }

        public Integer getStartPos() {
            return startPos;
        }

        public Integer getEndPos() {
            return endPos;
        }

        public Integer getConfidence() {
            return confidence;
        }

        public String getMatchedRule() {
            return matchedRule;
        }
    }

    /**
     * 提取文档中的所有字段
     * 
     * @param content 文档内容
     * @param fields 字段定义列表
     * @return 提取结果列表
     */
    public List<ExtractionResult> extract(String content, List<FieldDefinitionModel> fields) {
        List<ExtractionResult> results = new ArrayList<>();

        for (FieldDefinitionModel field : fields) {
            List<ExtractionRuleModel> rules = field.getRules();
            if (rules == null || rules.isEmpty()) {
                log.warn("字段 {} 没有配置提取规则", field.getFieldName());
                continue;
            }

            // 按优先级排序规则，过滤启用的规则
            rules = rules.stream()
                .filter(ExtractionRuleModel::getIsEnabled)
                .sorted(Comparator.comparing(ExtractionRuleModel::getPriority).reversed())
                .collect(Collectors.toList());

            // 尝试每个规则直到成功
            for (ExtractionRuleModel rule : rules) {
                try {
                    ExtractionResult result = extractByRule(content, field, rule);
                    if (result != null) {
                        results.add(result);
                        break; // 成功匹配后跳出
                    }
                } catch (Exception e) {
                    log.error("规则执行失败: field={}, rule={}", field.getFieldName(), rule.getRuleName(), e);
                }
            }
        }

        return results;
    }

    /**
     * 使用指定规则提取字段值
     */
    private ExtractionResult extractByRule(String content, FieldDefinitionModel field, ExtractionRuleModel rule) {
        String ruleType = rule.getRuleType();
        JSONObject ruleContent = JSON.parseObject(rule.getRuleContent());

        MatchResult matchResult = null;

        switch (ruleType.toLowerCase()) {
            case "regex":
                matchResult = regexMatcher.match(content, ruleContent);
                break;
            case "keyword":
                matchResult = keywordMatcher.match(content, ruleContent);
                break;
            case "position":
                matchResult = positionMatcher.match(content, ruleContent);
                break;
            case "pattern":
                matchResult = patternMatcher.match(content, ruleContent);
                break;
            default:
                log.warn("未知的规则类型: {}", ruleType);
                return null;
        }

        if (matchResult == null || !matchResult.isMatched()) {
            return null;
        }

        // 格式转换
        String rawValue = matchResult.getValue();
        Object formattedValue = rawValue;
        
        try {
            FormatConverter.FieldType fieldType = FormatConverter.FieldType.valueOf(
                field.getFieldType().toUpperCase()
            );
            formattedValue = FormatConverter.convert(rawValue, fieldType, field.getOutputFormat());
        } catch (Exception e) {
            log.warn("字段类型转换失败: field={}, type={}, error={}", 
                field.getFieldName(), field.getFieldType(), e.getMessage());
            // 转换失败时使用原始值
        }

        return new ExtractionResult(
            field.getFieldCode(),
            field.getFieldName(),
            field.getFieldType(),
            formattedValue,
            rawValue,
            matchResult.getStartPos(),
            matchResult.getEndPos(),
            matchResult.getConfidence(),
            rule.getRuleName()
        );
    }

    /**
     * 测试规则
     * 
     * @param content 测试内容
     * @param rule 规则
     * @return 匹配结果
     */
    public MatchResult testRule(String content, ExtractionRuleModel rule) {
        String ruleType = rule.getRuleType();
        JSONObject ruleContent = JSON.parseObject(rule.getRuleContent());

        switch (ruleType.toLowerCase()) {
            case "regex":
                return regexMatcher.match(content, ruleContent);
            case "keyword":
                return keywordMatcher.match(content, ruleContent);
            case "position":
                return positionMatcher.match(content, ruleContent);
            case "pattern":
                return patternMatcher.match(content, ruleContent);
            default:
                log.warn("未知的规则类型: {}", ruleType);
                return null;
        }
    }

    /**
     * 测试规则（使用字符串形式的规则内容）
     * 
     * @param content 测试内容
     * @param ruleType 规则类型
     * @param ruleContent 规则内容（JSON字符串）
     * @return 匹配结果
     */
    public MatchResult testRule(String content, String ruleType, String ruleContent) {
        JSONObject ruleJson = JSON.parseObject(ruleContent);

        switch (ruleType.toLowerCase()) {
            case "regex":
                return regexMatcher.match(content, ruleJson);
            case "keyword":
                return keywordMatcher.match(content, ruleJson);
            case "position":
                return positionMatcher.match(content, ruleJson);
            case "pattern":
                return patternMatcher.match(content, ruleJson);
            default:
                log.warn("未知的规则类型: {}", ruleType);
                return null;
        }
    }
}
