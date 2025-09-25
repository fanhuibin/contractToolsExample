package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_profile")
public class ReviewProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String profileCode;
    private String profileName;
    private Boolean isDefault;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


