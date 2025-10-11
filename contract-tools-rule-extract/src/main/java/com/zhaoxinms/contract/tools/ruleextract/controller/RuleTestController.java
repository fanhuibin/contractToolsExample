package com.zhaoxinms.contract.tools.ruleextract.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.CommonPatterns;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.EnhancedRuleEngine;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.ExtractionResult;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.ExtractionRule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则测试控制器
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@RestController
@RequestMapping("/api/rule-extract/test")
@RequiredArgsConstructor
public class RuleTestController {

    private final EnhancedRuleEngine enhancedRuleEngine;

    /**
     * 测试提取规则
     */
    @PostMapping("/extract")
    public Map<String, Object> testExtract(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            ExtractionRule rule = parseRule(request);
            Boolean debug = (Boolean) request.getOrDefault("debug", false);

            ExtractionResult result = enhancedRuleEngine.extract(text, rule, debug);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", result);
            return response;
        } catch (Exception e) {
            log.error("测试提取失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * 获取常用Pattern列表
     */
    @GetMapping("/patterns")
    public Map<String, Object> getCommonPatterns() {
        try {
            Map<String, CommonPatterns.PatternTemplate> patterns = CommonPatterns.getAllPatterns();

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", patterns);
            return response;
        } catch (Exception e) {
            log.error("获取Pattern列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * 解析规则
     */
    private ExtractionRule parseRule(Map<String, Object> request) {
        ExtractionRule rule = new ExtractionRule();

        // 从request中解析规则
        String ruleTypeStr = (String) request.get("ruleType");
        if (ruleTypeStr != null) {
            rule.setRuleType(com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.RuleType.valueOf(ruleTypeStr));
        }

        Object configObj = request.get("config");
        if (configObj instanceof Map) {
            com.alibaba.fastjson2.JSONObject config = new com.alibaba.fastjson2.JSONObject((Map<String, Object>) configObj);
            rule.setConfig(config);
        }

        rule.setEnabled(true);
        rule.setPriority(10);

        return rule;
    }
}

