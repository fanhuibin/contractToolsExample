package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;

/**
 * 条款字段DTO
 */
@Data
public class ClauseField {
    /**
     * 条款ID
     */
    private String id;
    
    /**
     * 条款名称
     */
    private String name;
    
    /**
     * 条款代码
     */
    private String code;
    
    /**
     * 条款内容（支持表达式插入基础字段或相对方字段）
     */
    private String content;
    
    /**
     * 条款类型
     */
    private String type;
    
    /**
     * 条款类型名称
     */
    private String typeName;
} 