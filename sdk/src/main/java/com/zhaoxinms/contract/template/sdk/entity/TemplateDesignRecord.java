package com.zhaoxinms.contract.template.sdk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_design_record")
public class TemplateDesignRecord {
    @TableId
    private String id;

    private String templateId;

    private String fileId;

    private String elementsJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


