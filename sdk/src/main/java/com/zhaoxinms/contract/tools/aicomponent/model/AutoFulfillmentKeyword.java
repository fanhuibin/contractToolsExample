package com.zhaoxinms.contract.tools.aicomponent.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("auto_fulfillment_keyword")
public class AutoFulfillmentKeyword {
    @TableId
    private Long id;
    private String name;
}


