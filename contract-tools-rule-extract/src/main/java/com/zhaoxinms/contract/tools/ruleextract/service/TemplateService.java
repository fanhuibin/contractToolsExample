package com.zhaoxinms.contract.tools.ruleextract.service;

import cn.hutool.core.util.StrUtil;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleTemplateModel;
import com.zhaoxinms.contract.tools.ruleextract.storage.JsonFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        RuleTemplateModel source = storage.load("template", id, RuleTemplateModel.class);
        if (source == null) {
            throw new IllegalArgumentException("源模板不存在");
        }

        // 创建副本
        RuleTemplateModel copy = new RuleTemplateModel();
        String newId = storage.generateId();
        
        copy.setId(newId);
        copy.setTemplateName(newName);
        copy.setTemplateType(source.getTemplateType());
        copy.setDescription(source.getDescription());
        copy.setStatus("draft");
        copy.setVersion(1);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        
        // 复制字段和规则
        copy.setFields(source.getFields());

        storage.save("template", newId, copy);
        log.info("复制模板成功: sourceId={}, newId={}, newName={}", id, newId, newName);
        
        return newId;
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

