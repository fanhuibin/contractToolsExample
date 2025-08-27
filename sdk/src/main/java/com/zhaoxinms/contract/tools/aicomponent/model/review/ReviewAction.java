package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_action")
public class ReviewAction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private String actionId;
    private String actionType; // COPY/REPLACE/LINK
    private String actionMessage;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


