package com.zhaoxinms.contract.tools.auth.enums;

/**
 * 系统模块类型枚举
 */
public enum ModuleType {
    
    /**
     * 合同模板设计模块
     */
    CONTRACT_TEMPLATE_DESIGN("contract_template_design", "合同模板设计模块"),
    
    /**
     * 合同合成模块
     */
    CONTRACT_SYNTHESIS("contract_synthesis", "合同合成模块"),
    
    /**
     * 合同比对PRO模块
     */
    CONTRACT_COMPARE_PRO("contract_compare_pro", "合同比对PRO模块"),
    
    /**
     * 合同信息抽取模块
     */
    CONTRACT_INFO_EXTRACTION("contract_info_extraction", "合同信息抽取模块"),
    
    /**
     * 合同智能审核模块
     */
    CONTRACT_INTELLIGENT_REVIEW("contract_intelligent_review", "合同智能审核模块"),
    
    /**
     * 履约任务生成模块
     */
    PERFORMANCE_TASK_GENERATION("performance_task_generation", "履约任务生成模块");
    
    private final String code;
    private final String name;
    
    ModuleType(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 根据代码获取模块类型
     */
    public static ModuleType fromCode(String code) {
        for (ModuleType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
