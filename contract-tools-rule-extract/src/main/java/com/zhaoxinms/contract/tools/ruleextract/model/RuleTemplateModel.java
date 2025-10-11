package com.zhaoxinms.contract.tools.ruleextract.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则模板模型（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class RuleTemplateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private String id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编号（唯一）
     */
    private String templateCode;

    /**
     * 模板类型（可选）
     */
    private String templateType;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 状态：active-启用, inactive-禁用, draft-草稿
     */
    private String status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 字段列表
     */
    private List<FieldDefinitionModel> fields = new ArrayList<>();

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

