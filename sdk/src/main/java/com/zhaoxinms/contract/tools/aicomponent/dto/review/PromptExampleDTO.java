package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

@Data
public class PromptExampleDTO {
    private Long promptVersionId;
    private String userExample;
    private String assistantExample;
    private Integer sortOrder;
}


