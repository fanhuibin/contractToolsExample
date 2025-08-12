package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PointDTO {
    private Long id;

    @NotNull
    private Long clauseTypeId;

    @NotBlank
    @Size(max = 64)
    private String pointCode;

    @NotBlank
    @Size(max = 128)
    private String pointName;

    @NotBlank
    @Size(max = 128)
    private String algorithmType;

    @Size(max = 255)
    private String remark;

    private Integer sortOrder;
    private Boolean enabled;
}


