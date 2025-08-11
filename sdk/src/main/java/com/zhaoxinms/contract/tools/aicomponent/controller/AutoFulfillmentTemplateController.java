package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentTemplateService;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/auto-fulfillment/template")
@RequiredArgsConstructor
public class AutoFulfillmentTemplateController {

    private final AutoFulfillmentTemplateService templateService;

    @GetMapping("/list")
    public Result<List<AutoFulfillmentTemplate>> getAllTemplates(@RequestParam(value = "userId", required = false) String userId) {
        try {
            List<AutoFulfillmentTemplate> templates;
            if (userId != null && !userId.isEmpty()) {
                templates = templateService.findAllSystemAndUserTemplates(userId);
            } else {
                templates = templateService.getSystemTemplates();
            }
            return Result.success("获取模板列表成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/type/{contractType}")
    public Result<List<AutoFulfillmentTemplate>> getTemplatesByContractType(@PathVariable String contractType,
                                                                            @RequestParam(value = "userId", required = false) String userId) {
        try {
            List<AutoFulfillmentTemplate> templates = templateService.getTemplatesByContractTypeAndUser(contractType, userId);
            return Result.success("获取模板列表成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/contract-types")
    public Result<Map<String, String>> getAllContractTypes() {
        try {
            return Result.success("获取合同类型列表成功", templateService.getAllContractTypes());
        } catch (Exception e) {
            return Result.error("获取合同类型列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<AutoFulfillmentTemplate> getTemplateById(@PathVariable Long id) {
        return templateService.getTemplateById(id)
                .map(t -> Result.success("获取模板成功", t))
                .orElse(Result.error("模板不存在"));
    }

    @PostMapping("/create")
    public Result<AutoFulfillmentTemplate> createTemplate(@RequestBody AutoFulfillmentTemplate template) {
        try {
            template.setType("user");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            return Result.success("创建模板成功", templateService.createTemplate(template));
        } catch (Exception e) {
            log.error("创建模板失败", e);
            return Result.error("创建模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<AutoFulfillmentTemplate> updateTemplate(@PathVariable Long id, @RequestBody AutoFulfillmentTemplate template) {
        try {
            return Result.success("更新模板成功", templateService.updateTemplate(id, template));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新模板失败", e);
            return Result.error("更新模板失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return Result.success("删除模板成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除模板失败", e);
            return Result.error("删除模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/copy")
    public Result<AutoFulfillmentTemplate> copyTemplate(@PathVariable Long id,
                                                        @RequestParam String newName,
                                                        @RequestParam String userId) {
        try {
            return Result.success("复制模板成功", templateService.copyTemplate(id, newName, userId));
        } catch (Exception e) {
            log.error("复制模板失败", e);
            return Result.error("复制模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/set-default")
    public Result<AutoFulfillmentTemplate> setDefaultTemplate(@PathVariable Long id,
                                                              @RequestParam String contractType) {
        try {
            return Result.success("设置默认模板成功", templateService.setDefaultTemplate(id, contractType));
        } catch (Exception e) {
            log.error("设置默认模板失败", e);
            return Result.error("设置默认模板失败: " + e.getMessage());
        }
    }

    @GetMapping("/default/{contractType}")
    public Result<AutoFulfillmentTemplate> getDefaultTemplate(@PathVariable String contractType) {
        return templateService.getDefaultTemplate(contractType)
                .map(t -> Result.success("获取默认模板成功", t))
                .orElse(Result.error("未找到默认模板"));
    }
}


