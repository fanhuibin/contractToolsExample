package com.zhaoxinms.contract.tools.api.dto;

import lombok.Data;

/**
 * 印章字段DTO
 */
@Data
public class SealField {
    /**
     * 印章ID
     */
    private String id;

    /**
     * 印章名称
     */
    private String name;

    /**
     * 印章代码
     */
    private String code;

    /**
     * 印章类型（公司章、合同章、财务章等）
     */
    private String type;

    /**
     * 序号或顺序（用于多方盖章的顺序控制，可选）
     */
    private Integer orderIndex;
}


