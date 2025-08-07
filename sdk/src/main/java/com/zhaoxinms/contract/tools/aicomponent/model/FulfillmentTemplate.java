package com.zhaoxinms.contract.tools.aicomponent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同履约任务模板实体类
 * 用于存储和管理不同类型的履约任务模板配置
 */
@Data
@Accessors(chain = true)
@TableName("ai_fulfillment_template")
public class FulfillmentTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 合同类型
     */
    @TableField("contract_type")
    private String contractType;

    /**
     * 任务类型列表（JSON存储）
     */
    @TableField("task_types")
    private List<String> taskTypes;

    /**
     * 关键词列表（JSON存储）
     */
    @TableField("keywords")
    private List<String> keywords;

    /**
     * 时间规则列表（JSON存储）
     */
    @TableField("time_rules")
    private List<String> timeRules;

    /**
     * 模板类型：系统/用户自定义
     */
    @TableField("type")
    private String type;

    /**
     * 是否为默认模板
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 创建用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
