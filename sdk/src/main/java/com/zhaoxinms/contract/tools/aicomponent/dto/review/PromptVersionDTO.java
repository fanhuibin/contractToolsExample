package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

@Data
public class PromptVersionDTO {
    private Long promptId;
    private String versionCode;
    private Boolean isPublished;
    private String contentText;
    private String remark;
}


