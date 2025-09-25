package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("review_prompt_example")
public class ReviewPromptExample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptVersionId;
    private String userExample;
    private String assistantExample;
    private Integer sortOrder;
}


