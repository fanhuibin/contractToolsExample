package com.zhaoxinms.contract.tools.ruleextract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板导入结果
 * 
 * @author 山西肇新科技有限公司
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateImportResult {
    
    /**
     * 模板ID
     */
    private String templateId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 字段数量
     */
    private Integer fieldCount;
    
    /**
     * 警告信息
     */
    private List<String> warnings;
    
    /**
     * 下一步操作提示
     */
    private String nextStep;
}

