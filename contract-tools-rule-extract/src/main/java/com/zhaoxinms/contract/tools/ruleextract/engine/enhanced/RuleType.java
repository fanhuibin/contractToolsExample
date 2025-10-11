package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

/**
 * 增强的规则类型
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
public enum RuleType {
    /**
     * 关键词锚点 - 基于关键词定位后提取
     */
    KEYWORD_ANCHOR,

    /**
     * 上下文边界 - 提取两个边界之间的内容
     */
    CONTEXT_BOUNDARY,

    /**
     * 纯正则表达式
     */
    REGEX_PATTERN,

    /**
     * 多步骤规则 - 组合多个步骤
     */
    MULTI_STEP,

    /**
     * 位置提取 - 基于行号或字符位置
     */
    POSITION_BASED,

    /**
     * 表格单元格提取
     */
    TABLE_CELL
}

