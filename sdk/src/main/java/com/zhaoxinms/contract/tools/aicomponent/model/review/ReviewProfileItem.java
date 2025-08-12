package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("review_profile_item")
public class ReviewProfileItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long profileId;
    private Long clauseTypeId;
    private Long pointId;
    private Integer sortOrder;
}


