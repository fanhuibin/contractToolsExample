package com.zhaoxinms.contract.tools.aicomponent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auto_fulfillment_history")
public class AutoFulfillmentHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String extractedContent;

    private LocalDateTime extractTime;

    private String userId;
}


