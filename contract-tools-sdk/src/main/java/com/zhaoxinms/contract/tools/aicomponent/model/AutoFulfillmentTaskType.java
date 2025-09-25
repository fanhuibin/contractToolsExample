package com.zhaoxinms.contract.tools.aicomponent.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("auto_fulfillment_task_type")
public class AutoFulfillmentTaskType {
    @TableId
    private Long id;
    private Long parentId;
    private String name;
    private Integer sortOrder;

    private String code;
}


