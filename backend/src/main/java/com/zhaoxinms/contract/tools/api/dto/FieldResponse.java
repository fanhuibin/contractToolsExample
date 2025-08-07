package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;
import java.util.List;

/**
 * 字段响应DTO
 */
@Data
public class FieldResponse {
    /**
     * 基础字段列表
     */
    private List<BaseField> baseFields;
    
    /**
     * 相对方字段列表
     */
    private List<CounterpartyField> counterpartyFields;
    
    /**
     * 条款字段列表
     */
    private List<ClauseField> clauseFields;
} 