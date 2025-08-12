package com.zhaoxinms.contract.tools.aicomponent.model.review;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("profile_prompt_version")
public class ProfilePromptVersion {
    @TableId
    private Long profileId; // part of composite PK, mapper按两列处理
    private Long promptId;
    private Long versionId;
}


