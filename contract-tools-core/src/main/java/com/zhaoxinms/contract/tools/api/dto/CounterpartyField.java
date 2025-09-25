package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;

/**
 * 相对方字段DTO
 */
@Data
public class CounterpartyField {
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
     * 第几个相对方
     */
    private Integer counterpartyIndex;

    /**
     * 测试值（用于前端调试预览）
     */
    private String sampleValue;
} 