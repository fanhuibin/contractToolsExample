package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;

/**
 * 模板设计响应DTO
 */
@Data
public class TemplateDesignResponse {
    /**
     * 设计会话ID
     */
    private String sessionId;
    
    /**
     * 编辑页面URL
     */
    private String editUrl;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;
} 