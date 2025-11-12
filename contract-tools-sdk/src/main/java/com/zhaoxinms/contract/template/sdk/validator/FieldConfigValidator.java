package com.zhaoxinms.contract.template.sdk.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 字段配置校验器
 * 用于校验本地 JSON 配置和第三方模板配置
 */
public class FieldConfigValidator {
    
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_CODE_LENGTH = 100;
    
    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * 校验 JSON 字符串格式的配置
     */
    public static ValidationResult validateJsonConfig(String jsonContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonContent);
            return validateJsonNode(root);
        } catch (Exception e) {
            return ValidationResult.failure("JSON 格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验 JsonNode 格式的配置
     */
    public static ValidationResult validateJsonNode(JsonNode root) {
        StringBuilder errors = new StringBuilder();
        
        // 1. 检查四种字段类型是否都存在
        if (!root.has("baseFields")) {
            errors.append("缺少 baseFields 字段；");
        }
        if (!root.has("counterpartyFields")) {
            errors.append("缺少 counterpartyFields 字段；");
        }
        if (!root.has("clauseFields")) {
            errors.append("缺少 clauseFields 字段；");
        }
        if (!root.has("sealFields")) {
            errors.append("缺少 sealFields 字段；");
        }
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        // 2. 检查是否为数组类型
        if (!root.get("baseFields").isArray()) {
            errors.append("baseFields 必须是数组；");
        }
        if (!root.get("counterpartyFields").isArray()) {
            errors.append("counterpartyFields 必须是数组；");
        }
        if (!root.get("clauseFields").isArray()) {
            errors.append("clauseFields 必须是数组；");
        }
        if (!root.get("sealFields").isArray()) {
            errors.append("sealFields 必须是数组；");
        }
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        // 3. 用于检查 code 和 id 重复
        Set<String> allCodes = new HashSet<>();
        Set<String> allIds = new HashSet<>();
        
        // 4. 校验各类字段
        validateBaseFields(root.get("baseFields"), allIds, allCodes, errors);
        validateCounterpartyFields(root.get("counterpartyFields"), allIds, allCodes, errors);
        validateClauseFields(root.get("clauseFields"), allIds, allCodes, errors);
        validateSealFields(root.get("sealFields"), allIds, allCodes, errors);
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 校验 Map 格式的配置（用于第三方 API）
     */
    public static ValidationResult validateMapConfig(Map<String, Object> config) {
        StringBuilder errors = new StringBuilder();
        
        // 1. 检查四种字段类型是否都存在
        if (!config.containsKey("baseFields")) {
            errors.append("缺少 baseFields 字段；");
        }
        if (!config.containsKey("counterpartyFields")) {
            errors.append("缺少 counterpartyFields 字段；");
        }
        if (!config.containsKey("clauseFields")) {
            errors.append("缺少 clauseFields 字段；");
        }
        if (!config.containsKey("sealFields")) {
            errors.append("缺少 sealFields 字段；");
        }
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        // 2. 检查是否为列表类型
        if (!(config.get("baseFields") instanceof List)) {
            errors.append("baseFields 必须是数组；");
        }
        if (!(config.get("counterpartyFields") instanceof List)) {
            errors.append("counterpartyFields 必须是数组；");
        }
        if (!(config.get("clauseFields") instanceof List)) {
            errors.append("clauseFields 必须是数组；");
        }
        if (!(config.get("sealFields") instanceof List)) {
            errors.append("sealFields 必须是数组；");
        }
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        // 3. 用于检查 code 和 id 重复
        Set<String> allCodes = new HashSet<>();
        Set<String> allIds = new HashSet<>();
        
        // 4. 校验各类字段
        validateBaseFieldsList(castToListMap(config.get("baseFields")), allIds, allCodes, errors);
        validateCounterpartyFieldsList(castToListMap(config.get("counterpartyFields")), allIds, allCodes, errors);
        validateClauseFieldsList(castToListMap(config.get("clauseFields")), allIds, allCodes, errors);
        validateSealFieldsList(castToListMap(config.get("sealFields")), allIds, allCodes, errors);
        
        if (errors.length() > 0) {
            return ValidationResult.failure(errors.toString());
        }
        
        return ValidationResult.success();
    }
    
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castToListMap(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }
    
    /**
     * 校验基础字段
     * 必填字段：id, name, code
     * 可选字段：isRichText, sampleValue
     */
    private static void validateBaseFields(JsonNode fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (JsonNode field : fields) {
            String id = getTextValue(field, "id");
            String name = getTextValue(field, "name");
            String code = getTextValue(field, "code");
            
            validateCommonFields(id, name, code, "baseFields", index, allIds, allCodes, errors);
            
            index++;
        }
    }
    
    private static void validateBaseFieldsList(List<Map<String, Object>> fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (Map<String, Object> field : fields) {
            String id = getStringValue(field, "id");
            String name = getStringValue(field, "name");
            String code = getStringValue(field, "code");
            
            validateCommonFields(id, name, code, "baseFields", index, allIds, allCodes, errors);
            
            index++;
        }
    }
    
    /**
     * 校验相对方字段
     * 必填字段：id, name, code, counterpartyIndex
     * 可选字段：sampleValue
     */
    private static void validateCounterpartyFields(JsonNode fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (JsonNode field : fields) {
            String id = getTextValue(field, "id");
            String name = getTextValue(field, "name");
            String code = getTextValue(field, "code");
            
            validateCommonFields(id, name, code, "counterpartyFields", index, allIds, allCodes, errors);
            
            // 校验 counterpartyIndex
            if (!field.has("counterpartyIndex") || field.get("counterpartyIndex").isNull()) {
                errors.append(String.format("[counterpartyFields#%d] counterpartyIndex 不能为空；", index));
            } else if (!field.get("counterpartyIndex").isNumber()) {
                errors.append(String.format("[counterpartyFields#%d] counterpartyIndex 必须是数字；", index));
            }
            
            index++;
        }
    }
    
    private static void validateCounterpartyFieldsList(List<Map<String, Object>> fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (Map<String, Object> field : fields) {
            String id = getStringValue(field, "id");
            String name = getStringValue(field, "name");
            String code = getStringValue(field, "code");
            
            validateCommonFields(id, name, code, "counterpartyFields", index, allIds, allCodes, errors);
            
            // 校验 counterpartyIndex
            if (!field.containsKey("counterpartyIndex") || field.get("counterpartyIndex") == null) {
                errors.append(String.format("[counterpartyFields#%d] counterpartyIndex 不能为空；", index));
            }
            
            index++;
        }
    }
    
    /**
     * 校验条款字段
     * 必填字段：id, name, code, content
     * 可选字段：type, typeName, sampleValue
     */
    private static void validateClauseFields(JsonNode fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (JsonNode field : fields) {
            String id = getTextValue(field, "id");
            String name = getTextValue(field, "name");
            String code = getTextValue(field, "code");
            String content = getTextValue(field, "content");
            
            validateCommonFields(id, name, code, "clauseFields", index, allIds, allCodes, errors);
            
            // 校验 content
            if (!StringUtils.hasText(content)) {
                errors.append(String.format("[clauseFields#%d] content 不能为空；", index));
            }
            
            index++;
        }
    }
    
    private static void validateClauseFieldsList(List<Map<String, Object>> fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (Map<String, Object> field : fields) {
            String id = getStringValue(field, "id");
            String name = getStringValue(field, "name");
            String code = getStringValue(field, "code");
            String content = getStringValue(field, "content");
            
            validateCommonFields(id, name, code, "clauseFields", index, allIds, allCodes, errors);
            
            // 校验 content
            if (!StringUtils.hasText(content)) {
                errors.append(String.format("[clauseFields#%d] content 不能为空；", index));
            }
            
            index++;
        }
    }
    
    /**
     * 校验印章字段
     * 必填字段：id, name, code, type
     * 可选字段：orderIndex
     */
    private static void validateSealFields(JsonNode fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (JsonNode field : fields) {
            String id = getTextValue(field, "id");
            String name = getTextValue(field, "name");
            String code = getTextValue(field, "code");
            String type = getTextValue(field, "type");
            
            validateCommonFields(id, name, code, "sealFields", index, allIds, allCodes, errors);
            
            // 校验 type
            if (!StringUtils.hasText(type)) {
                errors.append(String.format("[sealFields#%d] type 不能为空；", index));
            }
            
            index++;
        }
    }
    
    private static void validateSealFieldsList(List<Map<String, Object>> fields, Set<String> allIds, Set<String> allCodes, StringBuilder errors) {
        int index = 1;
        for (Map<String, Object> field : fields) {
            String id = getStringValue(field, "id");
            String name = getStringValue(field, "name");
            String code = getStringValue(field, "code");
            String type = getStringValue(field, "type");
            
            validateCommonFields(id, name, code, "sealFields", index, allIds, allCodes, errors);
            
            // 校验 type
            if (!StringUtils.hasText(type)) {
                errors.append(String.format("[sealFields#%d] type 不能为空；", index));
            }
            
            index++;
        }
    }
    
    /**
     * 校验通用字段（id, name, code）
     */
    private static void validateCommonFields(String id, String name, String code, 
                                            String fieldType, int index,
                                            Set<String> allIds, Set<String> allCodes, 
                                            StringBuilder errors) {
        // 校验 id
        if (!StringUtils.hasText(id)) {
            errors.append(String.format("[%s#%d] id 不能为空；", fieldType, index));
        } else {
            if (id.length() > MAX_CODE_LENGTH) {
                errors.append(String.format("[%s#%d] id 长度不能超过 %d；", fieldType, index, MAX_CODE_LENGTH));
            }
            if (!allIds.add(id)) {
                errors.append(String.format("[%s#%d] id 重复: %s；", fieldType, index, id));
            }
        }
        
        // 校验 name
        if (!StringUtils.hasText(name)) {
            errors.append(String.format("[%s#%d] name 不能为空；", fieldType, index));
        } else if (name.length() > MAX_NAME_LENGTH) {
            errors.append(String.format("[%s#%d] name 长度不能超过 %d；", fieldType, index, MAX_NAME_LENGTH));
        }
        
        // 校验 code
        if (!StringUtils.hasText(code)) {
            errors.append(String.format("[%s#%d] code 不能为空；", fieldType, index));
        } else {
            if (code.length() > MAX_CODE_LENGTH) {
                errors.append(String.format("[%s#%d] code 长度不能超过 %d；", fieldType, index, MAX_CODE_LENGTH));
            }
            if (!CODE_PATTERN.matcher(code).matches()) {
                errors.append(String.format("[%s#%d] code 格式不正确（只能包含字母、数字、下划线，且必须以字母开头）: %s；", fieldType, index, code));
            }
            if (!allCodes.add(code)) {
                errors.append(String.format("[%s#%d] code 重复: %s；", fieldType, index, code));
            }
        }
    }
    
    /**
     * 从 JsonNode 获取文本值
     */
    private static String getTextValue(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }
    
    /**
     * 从 Map 获取字符串值
     */
    private static String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }
}

