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

@Data
@Accessors(chain = true)
@TableName("auto_fulfillment_template")
public class AutoFulfillmentTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("category_code")
    private String categoryCode;

    /**
     * Legacy column kept for compatibility with NOT NULL constraints.
     * Always mirror the same value as categoryCode when inserting/updating.
     */
    @TableField("contract_type")
    private String contractType;

    @TableField("fields")
    private String fields;

    @TableField("creator_id")
    private String creatorId;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField("is_default")
    private Boolean isDefault;

    @TableField("description")
    private String description;

    @TableField("task_type_id")
    private Long taskTypeId;
}


