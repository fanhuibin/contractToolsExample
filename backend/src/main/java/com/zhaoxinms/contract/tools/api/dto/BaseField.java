package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;

/**
 * 基础字段DTO
 */
@Data
public class BaseField {
    /**
     * 字段ID
     */
    private String id;
    
    /**
     * 字段名称
     */
    private String name;
    
    /**
     * 字段代码
     */
    private String code;
    
    /**
     * 是否富文本（支持表格）
     */
    private Boolean isRichText;
} 