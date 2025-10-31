package com.zhaoxinms.contract.tools.ruleextract.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 生成的模板 DTO
 * 
 * @author 山西肇新科技有限公司
 */
@Data
public class AITemplateDTO {
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 字段列表
     */
    private List<AIFieldDTO> fields;
    
    @Data
    public static class AIFieldDTO {
        /**
         * 字段名称（英文）
         */
        private String fieldName;
        
        /**
         * 字段标签（中文）
         */
        private String fieldLabel;
        
        /**
         * 字段类型
         */
        private String fieldType;
        
        /**
         * 是否必填
         */
        private Boolean required;
        
        /**
         * 提取规则
         */
        private ExtractRules extractRules;
        
        /**
         * 验证规则（可选）
         */
        private Validation validation;
        
        /**
         * AI 置信度（可选）
         */
        private Integer confidence;
    }
    
    @Data
    public static class ExtractRules {
        /**
         * 规则类型：keyword | regex | table
         */
        private String type;
        
        /**
         * 定位关键词
         */
        private String keyword;
        
        /**
         * 偏移量
         */
        private Integer offset;
        
        /**
         * 提取长度
         */
        private Integer length;
        
        /**
         * 正则表达式（可选）
         */
        private String pattern;
        
        /**
         * 位置类型（可选）：before | after | between
         */
        private String position;
        
        /**
         * 结束关键词（可选）
         */
        private String endKeyword;
        
        /**
         * 表格规则（当 type 为 table 时使用）
         */
        private TableRules tableRules;
    }
    
    @Data
    public static class TableRules {
        /**
         * 表格定位关键词
         */
        private String tableKeyword;
        
        /**
         * 表格列名列表
         */
        private List<String> columns;
        
        /**
         * 起始行（可选）
         */
        private Integer startRow;
        
        /**
         * 结束行（可选）
         */
        private Integer endRow;
    }
    
    @Data
    public static class Validation {
        /**
         * 格式验证规则
         */
        private String format;
        
        /**
         * 数值范围
         */
        private String range;
    }
}

