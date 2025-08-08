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
    private String contractType;
    private String name;
    @TableField("content")
    private String contentJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


