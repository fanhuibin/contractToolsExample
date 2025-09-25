package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ActionDTO {
    private Long id;

    @NotNull
    private Long promptId;

    @NotBlank
    @Size(max = 64)
    private String actionId;

    @NotBlank
    @Size(max = 32)
    private String actionType; // COPY/REPLACE/LINK

    @NotBlank
    private String actionMessage;

    private Integer sortOrder;
    private Boolean enabled;
}


