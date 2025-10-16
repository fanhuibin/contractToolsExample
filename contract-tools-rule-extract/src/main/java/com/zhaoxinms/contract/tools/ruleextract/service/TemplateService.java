package com.zhaoxinms.contract.tools.ruleextract.service;

import cn.hutool.core.util.StrUtil;
import com.zhaoxinms.contract.tools.ruleextract.model.ExtractionRuleModel;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleTemplateModel;
import com.zhaoxinms.contract.tools.ruleextract.storage.JsonFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板管理服务（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final JsonFileStorage storage;

    /**
     * 创建模板
     */
    public String createTemplate(RuleTemplateModel template) {
        // 校验编号唯一性
        if (StrUtil.isNotBlank(template.getTemplateCode())) {
            if (isTemplateCodeExists(template.getTemplateCode())) {
                throw new IllegalArgumentException("模板编号已存在：" + template.getTemplateCode());
            }
        }
        
        String id = storage.generateId();
        template.setId(id);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        if (template.getVersion() == null) {
            template.setVersion(1);
        }
        if (StrUtil.isBlank(template.getStatus())) {
            template.setStatus("draft");
        }
        
        storage.save("template", id, template);
        log.info("创建模板成功: id={}, code={}, name={}", id, template.getTemplateCode(), template.getTemplateName());
        return id;
    }

    /**
     * 更新模板
     */
    public void updateTemplate(RuleTemplateModel template) {
        if (StrUtil.isBlank(template.getId())) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        RuleTemplateModel existing = storage.load("template", template.getId(), RuleTemplateModel.class);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        // 如果修改了编号，检查唯一性
        if (StrUtil.isNotBlank(template.getTemplateCode()) 
            && !template.getTemplateCode().equals(existing.getTemplateCode())) {
            if (isTemplateCodeExists(template.getTemplateCode())) {
                throw new IllegalArgumentException("模板编号已存在：" + template.getTemplateCode());
            }
        }

        // 更新时间和版本
        template.setUpdatedAt(LocalDateTime.now());
        template.setVersion(existing.getVersion() + 1);
        template.setCreatedAt(existing.getCreatedAt());
        template.setCreatedBy(existing.getCreatedBy());

        storage.save("template", template.getId(), template);
        log.info("更新模板成功: id={}, code={}, name={}", template.getId(), template.getTemplateCode(), template.getTemplateName());
    }

    /**
     * 删除模板
     */
    public void deleteTemplate(String id) {
        RuleTemplateModel template = storage.load("template", id, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        storage.delete("template", id);
        log.info("删除模板成功: id={}, name={}", id, template.getTemplateName());
    }

    /**
     * 查询模板详情
     */
    public RuleTemplateModel getTemplate(String id) {
        return storage.load("template", id, RuleTemplateModel.class);
    }

    /**
     * 查询模板列表
     */
    public List<RuleTemplateModel> listTemplates(String templateType, String status) {
        List<RuleTemplateModel> templates = storage.list("template", RuleTemplateModel.class);
        
        // 过滤（空字符串也视为无过滤条件）
        return templates.stream()
            .filter(t -> StrUtil.isBlank(templateType) || templateType.equals(t.getTemplateType()))
            .filter(t -> StrUtil.isBlank(status) || status.equals(t.getStatus()))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 按创建时间倒序
            .collect(Collectors.toList());
    }

    /**
     * 启用模板
     */
    public void enableTemplate(String id) {
        RuleTemplateModel template = storage.load("template", id, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        template.setStatus("active");
        template.setUpdatedAt(LocalDateTime.now());
        storage.save("template", id, template);
        log.info("启用模板: id={}, name={}", id, template.getTemplateName());
    }

    /**
     * 禁用模板
     */
    public void disableTemplate(String id) {
        RuleTemplateModel template = storage.load("template", id, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        template.setStatus("inactive");
        template.setUpdatedAt(LocalDateTime.now());
        storage.save("template", id, template);
        log.info("禁用模板: id={}, name={}", id, template.getTemplateName());
    }

    /**
     * 复制模板
     */
    public String copyTemplate(String id, String newName) {
        // 第一次加载源模板
        RuleTemplateModel source = storage.load("template", id, RuleTemplateModel.class);
        if (source == null) {
            throw new IllegalArgumentException("源模板不存在");
        }

        log.info("复制模板开始: sourceId={}, 源模板字段数量={}", id, 
            source.getFields() != null ? source.getFields().size() : 0);

        // 立即再次加载源模板，确保没有被修改
        RuleTemplateModel sourceCheck = storage.load("template", id, RuleTemplateModel.class);
        log.info("二次检查源模板: 字段数量={}", 
            sourceCheck.getFields() != null ? sourceCheck.getFields().size() : 0);

        // 使用JSON深拷贝来避免引用问题
        String sourceJson = com.alibaba.fastjson2.JSON.toJSONString(source);
        log.info("源模板JSON长度: {}", sourceJson.length());
        
        RuleTemplateModel copy = com.alibaba.fastjson2.JSON.parseObject(sourceJson, RuleTemplateModel.class);
        
        // 修改副本的基本信息
        String newId = storage.generateId();
        copy.setId(newId);
        copy.setTemplateName(newName);
        
        // 生成新的模板编号
        String newTemplateCode = generateCopyTemplateCode(source.getTemplateCode());
        copy.setTemplateCode(newTemplateCode);
        
        copy.setStatus("draft");
        copy.setVersion(1);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        
        // 为所有字段和规则生成新的ID
        if (copy.getFields() != null) {
            log.info("JSON拷贝后字段数量: {}", copy.getFields().size());
            for (FieldDefinitionModel field : copy.getFields()) {
                field.setId(storage.generateId());
                if (field.getRules() != null) {
                    for (ExtractionRuleModel rule : field.getRules()) {
                        rule.setId(storage.generateId());
                    }
                }
            }
        }

        log.info("保存新模板前: 新模板字段数量={}", 
            copy.getFields() != null ? copy.getFields().size() : 0);
        
        // 保存前再次检查源模板
        RuleTemplateModel sourceBeforeSave = storage.load("template", id, RuleTemplateModel.class);
        log.info("保存前检查源模板: 字段数量={}", 
            sourceBeforeSave.getFields() != null ? sourceBeforeSave.getFields().size() : 0);
        
        storage.save("template", newId, copy);
        
        // 验证保存后的状态
        RuleTemplateModel savedCopy = storage.load("template", newId, RuleTemplateModel.class);
        RuleTemplateModel reloadedSource = storage.load("template", id, RuleTemplateModel.class);
        
        log.info("复制模板成功: sourceId={}, newId={}, templateCode={}, newName={}", 
            id, newId, newTemplateCode, newName);
        log.info("保存后验证: 新模板字段数量={}, 原模板字段数量={}", 
            savedCopy.getFields() != null ? savedCopy.getFields().size() : 0,
            reloadedSource.getFields() != null ? reloadedSource.getFields().size() : 0);
        
        return newId;
    }
    
    /**
     * 生成复制模板的编号
     */
    private String generateCopyTemplateCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return "TEMPLATE_COPY_" + System.currentTimeMillis();
        }
        
        // 如果已经是复制的模板（包含_COPY_后缀），提取基础编号
        String baseCode = sourceCode;
        if (sourceCode.contains("_COPY_")) {
            baseCode = sourceCode.substring(0, sourceCode.indexOf("_COPY_"));
        }
        
        // 生成新的编号：基础编号 + _COPY_ + 时间戳
        return baseCode + "_COPY_" + System.currentTimeMillis();
    }
    
    /**
     * 深拷贝字段定义
     */
    private FieldDefinitionModel deepCopyField(FieldDefinitionModel source) {
        FieldDefinitionModel copy = new FieldDefinitionModel();
        
        copy.setId(storage.generateId()); // 生成新的字段ID
        copy.setFieldName(source.getFieldName());
        copy.setFieldCode(source.getFieldCode());
        copy.setFieldType(source.getFieldType());
        copy.setFieldCategory(source.getFieldCategory());
        copy.setIsRequired(source.getIsRequired());
        copy.setOutputFormat(source.getOutputFormat());
        copy.setDescription(source.getDescription());
        copy.setDefaultValue(source.getDefaultValue());
        copy.setValidationRule(source.getValidationRule());
        copy.setSortOrder(source.getSortOrder());
        
        // 深拷贝规则列表
        if (source.getRules() != null) {
            List<ExtractionRuleModel> copiedRules = new ArrayList<>();
            for (ExtractionRuleModel sourceRule : source.getRules()) {
                ExtractionRuleModel copiedRule = deepCopyRule(sourceRule);
                copiedRules.add(copiedRule);
            }
            copy.setRules(copiedRules);
        }
        
        return copy;
    }
    
    /**
     * 深拷贝提取规则
     */
    private ExtractionRuleModel deepCopyRule(ExtractionRuleModel source) {
        ExtractionRuleModel copy = new ExtractionRuleModel();
        
        copy.setId(storage.generateId()); // 生成新的规则ID
        copy.setRuleName(source.getRuleName());
        copy.setRuleType(source.getRuleType());
        copy.setRuleContent(source.getRuleContent()); // JSON字符串可以直接复制
        copy.setPriority(source.getPriority());
        copy.setIsEnabled(source.getIsEnabled());
        copy.setDescription(source.getDescription());
        
        return copy;
    }

    /**
     * 添加字段到模板
     */
    public void addField(String templateId, FieldDefinitionModel field) {
        RuleTemplateModel template = storage.load("template", templateId, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        // 生成字段ID
        if (StrUtil.isBlank(field.getId())) {
            field.setId(storage.generateId());
        }

        template.getFields().add(field);
        template.setUpdatedAt(LocalDateTime.now());
        storage.save("template", templateId, template);
        
        log.info("添加字段成功: templateId={}, fieldId={}, fieldName={}", 
            templateId, field.getId(), field.getFieldName());
    }

    /**
     * 更新字段
     */
    public void updateField(String templateId, FieldDefinitionModel field) {
        RuleTemplateModel template = storage.load("template", templateId, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        // 查找并更新字段
        boolean found = false;
        for (int i = 0; i < template.getFields().size(); i++) {
            if (template.getFields().get(i).getId().equals(field.getId())) {
                template.getFields().set(i, field);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("字段不存在");
        }

        template.setUpdatedAt(LocalDateTime.now());
        storage.save("template", templateId, template);
        
        log.info("更新字段成功: templateId={}, fieldId={}", templateId, field.getId());
    }

    /**
     * 删除字段
     */
    public void deleteField(String templateId, String fieldId) {
        RuleTemplateModel template = storage.load("template", templateId, RuleTemplateModel.class);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        template.getFields().removeIf(f -> f.getId().equals(fieldId));
        template.setUpdatedAt(LocalDateTime.now());
        storage.save("template", templateId, template);
        
        log.info("删除字段成功: templateId={}, fieldId={}", templateId, fieldId);
    }

    /**
     * 检查模板编号是否已存在
     */
    private boolean isTemplateCodeExists(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            return false;
        }
        
        List<RuleTemplateModel> templates = storage.list("template", RuleTemplateModel.class);
        return templates.stream()
            .anyMatch(t -> templateCode.equals(t.getTemplateCode()));
    }
}

