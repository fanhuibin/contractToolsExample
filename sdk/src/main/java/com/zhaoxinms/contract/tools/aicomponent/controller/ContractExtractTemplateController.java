package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractTemplateService;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 合同提取信息模板控制器
 *
 * @author zhaoxinms
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/contract/template")
@RequiredArgsConstructor
public class ContractExtractTemplateController {

    private final ContractExtractTemplateService templateService;

    /**
     * 获取所有模板
     *
     * @param userId 用户ID
     * @return 模板列表
     */
    @GetMapping("/list")
    public Result<List<ContractExtractTemplate>> getAllTemplates(
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("获取所有模板，用户ID: {}", userId);
        
        try {
            List<ContractExtractTemplate> templates;
            
            if (userId != null && !userId.isEmpty()) {
                // 获取系统模板和指定用户的自定义模板
                templates = templateService.findAllSystemAndUserTemplates(userId);
            } else {
                // 只获取系统模板
                templates = templateService.getSystemTemplates();
            }
            
            return Result.success("获取模板列表成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据合同类型获取模板
     *
     * @param contractType 合同类型
     * @param userId 用户ID（可选）
     * @return 模板列表
     */
    @GetMapping("/type/{contractType}")
    public Result<List<ContractExtractTemplate>> getTemplatesByContractType(
            @PathVariable String contractType,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("获取合同类型模板，类型: {}, 用户ID: {}", contractType, userId);
        
        try {
            List<ContractExtractTemplate> templates;
            
            if (userId != null && !userId.isEmpty()) {
                // 获取系统和用户的指定类型模板
                templates = templateService.getTemplatesByContractTypeAndUser(contractType, userId);
            } else {
                // 只获取系统的指定类型模板
                templates = templateService.getTemplatesByContractType(contractType);
            }
            
            return Result.success("获取模板列表成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有合同类型
     *
     * @return 合同类型列表
     */
    @GetMapping("/contract-types")
    public Result<Map<String, String>> getAllContractTypes() {
        try {
            Map<String, String> contractTypes = templateService.getAllContractTypes();
            return Result.success("获取合同类型列表成功", contractTypes);
        } catch (Exception e) {
            log.error("获取合同类型列表失败", e);
            return Result.error("获取合同类型列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定ID的模板
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public Result<ContractExtractTemplate> getTemplateById(@PathVariable Long id) {
        try {
            return templateService.getTemplateById(id)
                    .map(template -> Result.success("获取模板成功", template))
                    .orElse(Result.error("模板不存在"));
        } catch (Exception e) {
            log.error("获取模板失败", e);
            return Result.error("获取模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建模板
     *
     * @param template 模板信息
     * @return 创建的模板
     */
    @PostMapping("/create")
    public Result<ContractExtractTemplate> createTemplate(@RequestBody ContractExtractTemplate template) {
        try {
            // 设置创建时间和更新时间
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            
            // 设置为用户模板
            template.setType("user");
            
            // 创建模板
            ContractExtractTemplate createdTemplate = templateService.createTemplate(template);
            return Result.success("创建模板成功", createdTemplate);
        } catch (Exception e) {
            log.error("创建模板失败", e);
            return Result.error("创建模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新模板
     *
     * @param id 模板ID
     * @param template 更新的模板信息
     * @return 更新后的模板
     */
    @PutMapping("/{id}")
    public Result<ContractExtractTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody ContractExtractTemplate template) {
        try {
            // 检查是否是系统模板
            templateService.getTemplateById(id).ifPresent(existingTemplate -> {
                if ("system".equals(existingTemplate.getType())) {
                    throw new IllegalArgumentException("系统模板不允许修改");
                }
            });
            
            // 更新模板
            ContractExtractTemplate updatedTemplate = templateService.updateTemplate(id, template);
            return Result.success("更新模板成功", updatedTemplate);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新模板失败", e);
            return Result.error("更新模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除模板
     *
     * @param id 模板ID
     * @return 操作结果
     */
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
    
    /**
     * 复制模板
     *
     * @param id 源模板ID
     * @param newName 新模板名称
     * @param userId 用户ID
     * @return 复制的新模板
     */
    @PostMapping("/{id}/copy")
    public Result<ContractExtractTemplate> copyTemplate(
            @PathVariable Long id,
            @RequestParam String newName,
            @RequestParam String userId) {
        try {
            ContractExtractTemplate copiedTemplate = templateService.copyTemplate(id, newName, userId);
            return Result.success("复制模板成功", copiedTemplate);
        } catch (Exception e) {
            log.error("复制模板失败", e);
            return Result.error("复制模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置默认模板
     *
     * @param id 模板ID
     * @param contractType 合同类型
     * @return 设置为默认的模板
     */
    @PostMapping("/{id}/set-default")
    public Result<ContractExtractTemplate> setDefaultTemplate(
            @PathVariable Long id,
            @RequestParam String contractType) {
        try {
            ContractExtractTemplate defaultTemplate = templateService.setDefaultTemplate(id, contractType);
            return Result.success("设置默认模板成功", defaultTemplate);
        } catch (Exception e) {
            log.error("设置默认模板失败", e);
            return Result.error("设置默认模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定合同类型的默认模板
     *
     * @param contractType 合同类型
     * @return 默认模板
     */
    @GetMapping("/default/{contractType}")
    public Result<ContractExtractTemplate> getDefaultTemplate(@PathVariable String contractType) {
        try {
            return templateService.getDefaultTemplate(contractType)
                    .map(template -> Result.success("获取默认模板成功", template))
                    .orElse(Result.error("未找到默认模板"));
        } catch (Exception e) {
            log.error("获取默认模板失败", e);
            return Result.error("获取默认模板失败: " + e.getMessage());
        }
    }
}