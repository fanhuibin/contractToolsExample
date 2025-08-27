package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ClauseTypeDTO {
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String clauseCode;

    @NotBlank
    @Size(max = 128)
    private String clauseName;

    private Integer sortOrder;
    private Boolean enabled;
    @Size(max = 255)
    private String remark;
}


