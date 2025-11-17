package com.zhaoxin.tools.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自定义字段配置模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldsConfig {
    
    /**
     * 基础字段
     */
    private List<FieldDefinition> baseFields;
    
    /**
     * 条款字段
     */
    private List<FieldDefinition> clauseFields;
    
    /**
     * 相对方字段
     */
    private List<FieldDefinition> counterpartyFields;
    
    /**
     * 印章字段
     */
    private List<FieldDefinition> sealFields;
    
    /**
     * 字段定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDefinition {
        /**
         * 字段唯一标识（tag）
         */
        private String tag;
        
        /**
         * 字段显示名称
         */
        private String name;

        /**
         * 字段代码（用于模板变量引用）
         */
        private String code;
        
        /**
         * 字段类型：text（纯文本）、richText（富文本）、table（表格）
         */
        private String type;
        
        /**
         * 字段描述
         */
        private String description;
        
        /**
         * 是否为富文本
         */
        private Boolean richText;
        
        /**
         * 分类：base、clause、party、seal
         */
        private String category;

        /**
         * 示例值
         */
        private String sampleValue;
    }
}

