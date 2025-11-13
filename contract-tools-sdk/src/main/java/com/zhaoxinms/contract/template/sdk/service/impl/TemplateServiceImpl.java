package com.zhaoxinms.contract.template.sdk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.template.sdk.validator.FieldConfigValidator;
import com.zhaoxinms.contract.tools.api.dto.*;
import com.zhaoxinms.contract.tools.api.service.TemplateService;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板服务实现类
 * 此实现类提供具体的字段信息查询逻辑
 * 从JSON配置文件读取字段数据
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FieldResponse getFields() {
        try {
            // 从JSON文件读取配置
            return loadFieldsFromJson();
        } catch (Exception e) {
            logger.error("从JSON文件加载字段配置失败，返回空配置", e);
            // 如果读取失败，返回空配置
            FieldResponse response = new FieldResponse();
            response.setBaseFields(new ArrayList<>());
            response.setCounterpartyFields(new ArrayList<>());
            response.setClauseFields(new ArrayList<>());
            response.setSealFields(new ArrayList<>());
            return response;
        }
    }
    
    /**
     * 从JSON文件加载字段配置
     */
    private FieldResponse loadFieldsFromJson() throws IOException {
        // 获取文件路径：root-path/templates/fields.json
        String rootPath = zxcmConfig.getFileUpload().getRootPath();
        String jsonFilePath = rootPath + File.separator + "templates" + File.separator + "fields.json";
        
        logger.info("开始加载字段配置文件，路径: {}", jsonFilePath);
        
        File jsonFile = new File(jsonFilePath);
        logger.info("文件绝对路径: {}", jsonFile.getAbsolutePath());
        logger.info("文件是否存在: {}", jsonFile.exists());
        
        if (!jsonFile.exists()) {
            logger.warn("字段配置文件不存在: {}", jsonFile.getAbsolutePath());
            // 尝试创建目录
            File parentDir = jsonFile.getParentFile();
            if (!parentDir.exists()) {
                logger.info("创建目录: {}", parentDir.getAbsolutePath());
                parentDir.mkdirs();
            }
            return createEmptyResponse();
        }
        
        // 读取JSON文件
        logger.info("开始读取JSON文件内容");
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)), "UTF-8");
        logger.info("JSON文件内容长度: {} 字符", jsonContent.length());
        
        // 校验 JSON 配置
        logger.info("开始校验JSON配置");
        FieldConfigValidator.ValidationResult validationResult = FieldConfigValidator.validateJsonConfig(jsonContent);
        if (!validationResult.isValid()) {
            logger.error("字段配置校验失败: {}", validationResult.getErrorMessage());
            throw new RuntimeException("字段配置文件校验失败: " + validationResult.getErrorMessage());
        }
        logger.info("JSON配置校验通过");
        
        // 解析JSON
        logger.info("开始解析JSON");
        FieldsConfig config = objectMapper.readValue(jsonContent, FieldsConfig.class);
        
        // 转换为DTO对象
        FieldResponse response = new FieldResponse();
        response.setBaseFields(convertBaseFields(config.getBaseFields()));
        response.setCounterpartyFields(convertCounterpartyFields(config.getCounterpartyFields()));
        response.setClauseFields(convertClauseFields(config.getClauseFields()));
        response.setSealFields(convertSealFields(config.getSealFields()));
        
        logger.info("成功加载字段配置: 基础字段{}个, 相对方字段{}个, 条款字段{}个, 印章字段{}个",
                response.getBaseFields().size(),
                response.getCounterpartyFields().size(),
                response.getClauseFields().size(),
                response.getSealFields().size());
        
        return response;
    }
    
    /**
     * 转换基础字段
     */
    private List<BaseField> convertBaseFields(List<BaseFieldConfig> configs) {
        List<BaseField> fields = new ArrayList<>();
        if (configs != null) {
            for (BaseFieldConfig config : configs) {
                BaseField field = new BaseField();
                field.setId(config.getId());
                field.setName(config.getName());
                field.setCode(config.getCode());
                field.setIsRichText(config.getIsRichText());
                field.setSampleValue(config.getSampleValue());
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * 转换相对方字段
     */
    private List<CounterpartyField> convertCounterpartyFields(List<CounterpartyFieldConfig> configs) {
        List<CounterpartyField> fields = new ArrayList<>();
        if (configs != null) {
            for (CounterpartyFieldConfig config : configs) {
                CounterpartyField field = new CounterpartyField();
                field.setId(config.getId());
                field.setName(config.getName());
                field.setCode(config.getCode());
                field.setCounterpartyIndex(config.getCounterpartyIndex());
                field.setSampleValue(config.getSampleValue());
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * 转换条款字段
     */
    private List<ClauseField> convertClauseFields(List<ClauseFieldConfig> configs) {
        List<ClauseField> fields = new ArrayList<>();
        if (configs != null) {
            for (ClauseFieldConfig config : configs) {
                ClauseField field = new ClauseField();
                field.setId(config.getId());
                field.setName(config.getName());
                field.setCode(config.getCode());
                field.setContent(config.getContent());
                field.setType(config.getType());
                field.setTypeName(config.getTypeName());
                field.setSampleValue(config.getSampleValue());
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * 转换印章字段
     */
    private List<SealField> convertSealFields(List<SealFieldConfig> configs) {
        List<SealField> fields = new ArrayList<>();
        if (configs != null) {
            for (SealFieldConfig config : configs) {
                SealField field = new SealField();
                field.setId(config.getId());
                field.setName(config.getName());
                field.setCode(config.getCode());
                field.setType(config.getType());
                field.setOrderIndex(config.getOrderIndex());
                field.setWidth(config.getWidth());
                field.setHeight(config.getHeight());
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * 创建空响应
     */
    private FieldResponse createEmptyResponse() {
        FieldResponse response = new FieldResponse();
        response.setBaseFields(new ArrayList<>());
        response.setCounterpartyFields(new ArrayList<>());
        response.setClauseFields(new ArrayList<>());
        response.setSealFields(new ArrayList<>());
        return response;
    }
    
    /**
     * JSON配置类 - 用于解析JSON文件
     */
    private static class FieldsConfig {
        private List<BaseFieldConfig> baseFields;
        private List<CounterpartyFieldConfig> counterpartyFields;
        private List<ClauseFieldConfig> clauseFields;
        private List<SealFieldConfig> sealFields;
        
        public List<BaseFieldConfig> getBaseFields() {
            return baseFields;
        }
        
        public void setBaseFields(List<BaseFieldConfig> baseFields) {
            this.baseFields = baseFields;
        }
        
        public List<CounterpartyFieldConfig> getCounterpartyFields() {
            return counterpartyFields;
        }
        
        public void setCounterpartyFields(List<CounterpartyFieldConfig> counterpartyFields) {
            this.counterpartyFields = counterpartyFields;
        }
        
        public List<ClauseFieldConfig> getClauseFields() {
            return clauseFields;
        }
        
        public void setClauseFields(List<ClauseFieldConfig> clauseFields) {
            this.clauseFields = clauseFields;
        }
        
        public List<SealFieldConfig> getSealFields() {
            return sealFields;
        }
        
        public void setSealFields(List<SealFieldConfig> sealFields) {
            this.sealFields = sealFields;
        }
    }
    
    /**
     * 基础字段配置
     */
    private static class BaseFieldConfig {
        private String id;
        private String name;
        private String code;
        private Boolean isRichText;
        private String sampleValue;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public Boolean getIsRichText() {
            return isRichText;
        }
        
        public void setIsRichText(Boolean isRichText) {
            this.isRichText = isRichText;
        }
        
        public String getSampleValue() {
            return sampleValue;
        }
        
        public void setSampleValue(String sampleValue) {
            this.sampleValue = sampleValue;
        }
    }
    
    /**
     * 相对方字段配置
     */
    private static class CounterpartyFieldConfig {
        private String id;
        private String name;
        private String code;
        private Integer counterpartyIndex;
        private String sampleValue;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public Integer getCounterpartyIndex() {
            return counterpartyIndex;
        }
        
        public void setCounterpartyIndex(Integer counterpartyIndex) {
            this.counterpartyIndex = counterpartyIndex;
        }
        
        public String getSampleValue() {
            return sampleValue;
        }
        
        public void setSampleValue(String sampleValue) {
            this.sampleValue = sampleValue;
        }
    }
    
    /**
     * 条款字段配置
     */
    private static class ClauseFieldConfig {
        private String id;
        private String name;
        private String code;
        private String content;
        private String type;
        private String typeName;
        private String sampleValue;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
        
        public String getSampleValue() {
            return sampleValue;
        }
        
        public void setSampleValue(String sampleValue) {
            this.sampleValue = sampleValue;
        }
    }
    
    /**
     * 印章字段配置
     */
    private static class SealFieldConfig {
        private String id;
        private String name;
        private String code;
        private String type;
        private Integer orderIndex;
        private Float width;
        private Float height;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Integer getOrderIndex() {
            return orderIndex;
        }
        
        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }
        
        public Float getWidth() {
            return width;
        }
        
        public void setWidth(Float width) {
            this.width = width;
        }
        
        public Float getHeight() {
            return height;
        }
        
        public void setHeight(Float height) {
            this.height = height;
        }
    }

    @Override
    public FieldResponse getFieldsByTemplateId(String templateId) {
        // 根据模板ID返回不同的字段配置
        FieldResponse response = getFields(); // 默认返回所有字段
        
        // 这里可以根据templateId进行不同的字段配置
        if ("template_simple".equals(templateId)) {
            // 简单模板，只返回基础字段
            response.setCounterpartyFields(new ArrayList<>());
            response.setClauseFields(new ArrayList<>());
        } else if ("template_complex".equals(templateId)) {
            // 复杂模板，返回所有字段
            // 保持默认配置
        }
        
        return response;
    }
} 