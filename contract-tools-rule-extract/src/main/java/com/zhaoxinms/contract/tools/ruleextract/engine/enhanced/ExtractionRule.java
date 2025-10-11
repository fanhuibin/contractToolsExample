package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.io.Serializable;

/**
 * 增强的提取规则
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class ExtractionRule implements Serializable {

    /**
     * 规则ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则类型
     */
    private RuleType ruleType;

    /**
     * 规则配置（JSON格式，根据ruleType不同而不同）
     */
    private JSONObject config;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority = 10;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 描述
     */
    private String description;
}

