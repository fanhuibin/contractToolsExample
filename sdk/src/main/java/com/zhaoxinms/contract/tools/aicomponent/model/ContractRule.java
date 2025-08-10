package com.zhaoxinms.contract.tools.aicomponent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_rule")
public class ContractRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 保留 contractType 仅用于展示/兼容，唯一性由 templateId 取代
     */
    private String contractType;
    private String name;
    @TableField("content")
    private String contentJson;
    /**
     * 新增字段：规则所属模板ID（唯一约束）
     */
    @TableField("template_id")
    private Long templateId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


