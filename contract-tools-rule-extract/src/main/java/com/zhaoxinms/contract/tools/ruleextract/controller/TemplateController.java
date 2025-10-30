package com.zhaoxinms.contract.tools.ruleextract.controller;

import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleTemplateModel;
import com.zhaoxinms.contract.tools.ruleextract.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板管理控制器（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@RestController
@RequestMapping("/api/rule-extract/templates")
@RequiredArgsConstructor
@RequireFeature(module = ModuleType.SMART_DOCUMENT_EXTRACTION, message = "智能文档抽取功能需要授权")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 创建模板
     */
    @PostMapping
    public Map<String, Object> createTemplate(@RequestBody RuleTemplateModel template) {
        try {
            String id = templateService.createTemplate(template);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "创建成功");
            result.put("data", Map.of("id", id));
            return result;
        } catch (Exception e) {
            log.error("创建模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateTemplate(@PathVariable String id, @RequestBody RuleTemplateModel template) {
        try {
            template.setId(id);
            templateService.updateTemplate(template);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "更新成功");
            return result;
        } catch (Exception e) {
            log.error("更新模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTemplate(@PathVariable String id) {
        try {
            templateService.deleteTemplate(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "删除成功");
            return result;
        } catch (Exception e) {
            log.error("删除模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询模板详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getTemplate(@PathVariable String id) {
        try {
            RuleTemplateModel template = templateService.getTemplate(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", template);
            return result;
        } catch (Exception e) {
            log.error("查询模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询模板列表
     */
    @GetMapping
    public Map<String, Object> listTemplates(
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String status) {
        try {
            List<RuleTemplateModel> templates = templateService.listTemplates(templateType, status);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", templates);
            return result;
        } catch (Exception e) {
            log.error("查询模板列表失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 启用模板
     */
    @PostMapping("/{id}/enable")
    public Map<String, Object> enableTemplate(@PathVariable String id) {
        try {
            templateService.enableTemplate(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "启用成功");
            return result;
        } catch (Exception e) {
            log.error("启用模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 禁用模板
     */
    @PostMapping("/{id}/disable")
    public Map<String, Object> disableTemplate(@PathVariable String id) {
        try {
            templateService.disableTemplate(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "禁用成功");
            return result;
        } catch (Exception e) {
            log.error("禁用模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 复制模板
     */
    @PostMapping("/{id}/copy")
    public Map<String, Object> copyTemplate(@PathVariable String id, @RequestParam String newName) {
        try {
            String newId = templateService.copyTemplate(id, newName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "复制成功");
            result.put("data", Map.of("id", newId));
            return result;
        } catch (Exception e) {
            log.error("复制模板失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 添加字段
     */
    @PostMapping("/{templateId}/fields")
    public Map<String, Object> addField(@PathVariable String templateId, @RequestBody FieldDefinitionModel field) {
        try {
            templateService.addField(templateId, field);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "添加字段成功");
            result.put("data", Map.of("id", field.getId()));
            return result;
        } catch (Exception e) {
            log.error("添加字段失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 更新字段
     */
    @PutMapping("/{templateId}/fields/{fieldId}")
    public Map<String, Object> updateField(
            @PathVariable String templateId,
            @PathVariable String fieldId,
            @RequestBody FieldDefinitionModel field) {
        try {
            field.setId(fieldId);
            templateService.updateField(templateId, field);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "更新字段成功");
            return result;
        } catch (Exception e) {
            log.error("更新字段失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 删除字段
     */
    @DeleteMapping("/{templateId}/fields/{fieldId}")
    public Map<String, Object> deleteField(@PathVariable String templateId, @PathVariable String fieldId) {
        try {
            templateService.deleteField(templateId, fieldId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "删除字段成功");
            return result;
        } catch (Exception e) {
            log.error("删除字段失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }
}

