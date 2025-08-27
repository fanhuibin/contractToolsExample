package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_point")
public class ReviewPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clauseTypeId;
    private String pointCode;
    private String pointName;
    private String algorithmType;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


