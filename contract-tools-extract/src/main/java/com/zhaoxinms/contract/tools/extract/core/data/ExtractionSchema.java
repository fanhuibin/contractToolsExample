package com.zhaoxinms.contract.tools.extract.core.data;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 提取模式定义
 * 定义要从文本中提取的字段和规则
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractionSchema {
    
    /**
     * 模式名称
     */
    private String name;
    
    /**
     * 模式描述
     */
    private String description;
    
    /**
     * 字段定义列表
     */
    @Builder.Default
    private List<FieldDefinition> fields = new ArrayList<>();
    
    /**
     * 模式版本
     */
    private String version;
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 字段定义
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldDefinition {
        
        /**
         * 字段名
         */
        private String name;
        
        /**
         * 字段描述
         */
        private String description;
        
        /**
         * 字段类型
         */
        private FieldType type;
        
        /**
         * 是否必需
         */
        @Builder.Default
        private boolean required = false;
        
        /**
         * 默认值
         */
        private Object defaultValue;
        
        /**
         * 验证规则
         */
        @Builder.Default
        private Map<String, Object> validation = new HashMap<>();
        
        /**
         * 示例值
         */
        @Builder.Default
        private List<Object> examples = new ArrayList<>();
        
        /**
         * 提取提示
         */
        private String hint;
    }
    
    /**
     * 字段类型枚举
     */
    public enum FieldType {
        STRING("string"),
        INTEGER("integer"),
        FLOAT("float"),
        BOOLEAN("boolean"),
        DATE("date"),
        DATETIME("datetime"),
        ARRAY("array"),
        OBJECT("object"),
        EMAIL("email"),
        URL("url"),
        PHONE("phone"),
        CURRENCY("currency");
        
        private final String value;
        
        FieldType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static FieldType fromValue(String value) {
            for (FieldType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return STRING; // 默认类型
        }
    }
    
    /**
     * 添加字段定义
     */
    public void addField(FieldDefinition field) {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        fields.add(field);
    }
    
    /**
     * 获取字段定义
     */
    public FieldDefinition getField(String fieldName) {
        return fields.stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取必需字段
     */
    public List<FieldDefinition> getRequiredFields() {
        return fields.stream()
                .filter(FieldDefinition::isRequired)
                .collect(Collectors.toList());
    }
    
    /**
     * 验证模式完整性
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && 
               fields != null && !fields.isEmpty();
    }
}
