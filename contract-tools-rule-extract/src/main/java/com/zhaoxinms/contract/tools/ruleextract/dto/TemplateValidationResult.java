package com.zhaoxinms.contract.tools.ruleextract.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板验证结果
 * 
 * @author 山西肇新科技有限公司
 */
@Data
public class TemplateValidationResult {
    
    /**
     * 是否验证通过
     */
    private boolean valid = true;
    
    /**
     * 错误列表
     */
    private List<String> errors = new ArrayList<>();
    
    /**
     * 警告列表
     */
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 添加错误
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
    
    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}

