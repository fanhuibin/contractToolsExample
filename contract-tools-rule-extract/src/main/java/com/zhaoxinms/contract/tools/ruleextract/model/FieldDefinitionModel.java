package com.zhaoxinms.contract.tools.ruleextract.model;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段定义模型（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class FieldDefinitionModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段ID
     */
    private String id;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段编码
     */
    private String fieldCode;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 字段分类
     */
    private String fieldCategory;

    /**
     * 输出格式
     */
    private String outputFormat;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 验证规则
     */
    private String validationRule;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 提取规则列表
     */
    private List<ExtractionRuleModel> rules = new ArrayList<>();
}

