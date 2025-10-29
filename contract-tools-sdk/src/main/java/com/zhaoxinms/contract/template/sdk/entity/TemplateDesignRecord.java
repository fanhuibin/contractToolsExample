package com.zhaoxinms.contract.template.sdk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_design_record")
public class TemplateDesignRecord {
    @TableId
    private String id;  // 自动生成的唯一ID

    private String templateCode;  // 模板编码（多个版本共用）

    private String templateName;  // 模板名称

    private String version;  // 版本号（如：1.0, 1.1, 2.0）

    private String templateId;  // 旧字段，保留兼容性

    private String fileId;

    private String elementsJson;

    private String status;  // 状态：DRAFT-草稿, PUBLISHED-已发布, DISABLED-已禁用, DELETED-已删除

    private String description;  // 模板描述

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;  // 创建人

    private String updatedBy;  // 更新人
}


