package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_prompt")
public class ReviewPrompt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pointId;
    private String promptKey;
    private String name;
    private String message;
    private String statusType; // INFO/WARNING/ERROR
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


