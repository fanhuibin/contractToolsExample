package com.zhaoxinms.contract.tools.ruleextract.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 提取规则模型（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class ExtractionRuleModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 规则内容
     */
    private String ruleContent;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 规则描述
     */
    private String description;
}

