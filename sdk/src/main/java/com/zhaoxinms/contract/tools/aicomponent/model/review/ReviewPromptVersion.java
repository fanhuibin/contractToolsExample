package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("review_prompt_version")
public class ReviewPromptVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private String versionCode;
    private Boolean isPublished;
    private String contentText;
    private String remark;
}


