package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PromptDTO {
    private Long id;

    @NotNull
    private Long pointId;

    @NotBlank
    @Size(max = 128)
    private String promptKey;

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotBlank
    private String message;

    @NotBlank
    @Size(max = 16)
    private String statusType; // INFO/WARNING/ERROR

    private Integer sortOrder;
    private Boolean enabled;
}


