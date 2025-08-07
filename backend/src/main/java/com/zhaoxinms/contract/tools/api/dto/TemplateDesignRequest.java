package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 模板设计请求DTO
 */
@Data
public class TemplateDesignRequest {
    /**
     * 模板ID
     */
    @NotBlank(message = "模板ID不能为空")
    private String templateId;
    
    /**
     * 回调地址
     */
    @NotBlank(message = "回调地址不能为空")
    private String callbackUrl;
    
    /**
     * 后端地址
     */
    @NotBlank(message = "后端地址不能为空")
    private String backendUrl;
} 