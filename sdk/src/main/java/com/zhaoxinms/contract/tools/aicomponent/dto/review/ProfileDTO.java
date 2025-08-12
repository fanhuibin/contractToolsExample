package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ProfileDTO {
    private Long id;
    @NotBlank
    @Size(max = 64)
    private String profileCode;
    @NotBlank
    @Size(max = 128)
    private String profileName;
    private String description;
    private Boolean isDefault;
}


