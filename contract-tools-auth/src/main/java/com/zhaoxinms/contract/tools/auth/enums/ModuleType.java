package com.zhaoxinms.contract.tools.auth.enums;

/**
 * 系统模块类型枚举
 */
public enum ModuleType {
    
    /**
     * 智能文档抽取模块（规则提取）
     */
    SMART_DOCUMENT_EXTRACTION("smart_document_extraction", "智能文档抽取"),
    
    /**
     * 智能文档比对模块（GPU OCR比对）
     */
    SMART_DOCUMENT_COMPARE("smart_document_compare", "智能文档比对"),
    
    /**
     * 智能合同合成模块
     */
    SMART_CONTRACT_SYNTHESIS("smart_contract_synthesis", "智能合同合成"),
    
    /**
     * 智能文档解析模块（OCR文本提取）
     */
    SMART_DOCUMENT_PARSE("smart_document_parse", "智能文档解析"),
    
    /**
     * 文档在线编辑模块（OnlyOffice）
     */
    DOCUMENT_ONLINE_EDIT("document_online_edit", "文档在线编辑"),
    
    /**
     * 文档格式转换模块
     */
    DOCUMENT_FORMAT_CONVERT("document_format_convert", "文档格式转换");
    
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
