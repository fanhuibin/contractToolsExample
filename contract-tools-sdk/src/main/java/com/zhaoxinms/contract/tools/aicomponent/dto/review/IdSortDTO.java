package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class IdSortDTO {
    @NotNull
    private Long id;
    @NotNull
    private Integer sortOrder;
}


