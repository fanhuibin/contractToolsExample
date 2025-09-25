package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ProfileItemDTO {
    private Long id;
    @NotNull
    private Long profileId;
    @NotNull
    private Long clauseTypeId;
    @NotNull
    private Long pointId;
    @NotNull
    private Integer sortOrder;
}


