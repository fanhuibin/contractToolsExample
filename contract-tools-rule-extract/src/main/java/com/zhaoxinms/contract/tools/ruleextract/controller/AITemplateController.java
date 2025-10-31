package com.zhaoxinms.contract.tools.ruleextract.controller;

import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.ruleextract.constants.AIPromptTemplates;
import com.zhaoxinms.contract.tools.ruleextract.dto.*;
import com.zhaoxinms.contract.tools.ruleextract.service.AITemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 模板生成助手 Controller
 * 
 * @author 山西肇新科技有限公司
 */
@Slf4j
@Api(tags = "AI模板生成助手")
@RestController
@RequestMapping("/api/rule-extract/ai-template")
@RequiredArgsConstructor
@RequireFeature(module = ModuleType.SMART_DOCUMENT_EXTRACTION, message = "智能文档抽取功能需要授权")
public class AITemplateController {

    private final AITemplateService aiTemplateService;

    /**
     * 提取文档文本（用于AI分析）
     */
    @PostMapping("/extract-document-text")
    @ApiOperation(value = "提取文档文本", notes = "提取文档原文用于AI模板生成")
    public Map<String, Object> extractDocumentText(
            @ApiParam(value = "文档文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "输出格式", example = "plain") @RequestParam(value = "format", defaultValue = "plain") String format) {
        
        try {
            log.info("提取文档文本: {}, 格式: {}", file.getOriginalFilename(), format);
            
            DocumentTextResult result = aiTemplateService.extractDocumentText(file, format);
            
            return success(result);
            
        } catch (Exception e) {
            log.error("提取文档文本失败", e);
            return error("提取文档文本失败: " + e.getMessage());
        }
    }

    /**
     * 获取提示词模板
     */
    @GetMapping("/prompt-templates")
    @ApiOperation(value = "获取提示词模板", notes = "获取预定义的AI提示词模板")
    public Map<String, Object> getPromptTemplates() {
        try {
            List<Map<String, Object>> templates = new ArrayList<>();
            
            // 标准提取模板（支持表格）
            Map<String, Object> defaultTemplate = new HashMap<>();
            defaultTemplate.put("id", "default");
            defaultTemplate.put("name", "标准提取模板");
            defaultTemplate.put("description", "支持文本字段和表格数据提取");
            defaultTemplate.put("promptText", AIPromptTemplates.ADVANCED_EXTRACTION_TEMPLATE);
            defaultTemplate.put("example", AIPromptTemplates.JSON_EXAMPLE);
            templates.add(defaultTemplate);
            
            Map<String, Object> data = new HashMap<>();
            data.put("templates", templates);
            data.put("usageGuide", AIPromptTemplates.TEMPLATE_USAGE_GUIDE);
            
            return success(data);
            
        } catch (Exception e) {
            log.error("获取提示词模板失败", e);
            return error("获取提示词模板失败: " + e.getMessage());
        }
    }

    /**
     * 生成完整提示词
     */
    @PostMapping("/generate-prompt")
    @ApiOperation(value = "生成完整提示词", notes = "根据文档内容和字段列表生成完整的AI提示词")
    public Map<String, Object> generatePrompt(
            @ApiParam(value = "提示词模板ID", example = "default") @RequestParam("templateId") String templateId,
            @ApiParam(value = "字段列表", example = "合同编号\n甲方名称\n合同金额") @RequestParam("fieldList") String fieldList,
            @ApiParam(value = "文档内容", example = "合同文本...") @RequestParam("documentContent") String documentContent) {
        
        try {
            // 统一使用标准提取模板（支持表格）
            String promptTemplate = AIPromptTemplates.ADVANCED_EXTRACTION_TEMPLATE;
            
            String fullPrompt = promptTemplate
                .replace("{FIELD_LIST}", fieldList)
                .replace("{DOCUMENT_CONTENT}", documentContent);
            
            Map<String, Object> data = new HashMap<>();
            data.put("fullPrompt", fullPrompt);
            data.put("promptLength", fullPrompt.length());
            data.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return success(data);
            
        } catch (Exception e) {
            log.error("生成提示词失败", e);
            return error("生成提示词失败: " + e.getMessage());
        }
    }

    /**
     * 验证模板JSON格式
     */
    @PostMapping("/validate-json")
    @ApiOperation(value = "验证模板JSON", notes = "验证AI生成的JSON格式是否正确")
    public Map<String, Object> validateJSON(
            @ApiParam(value = "JSON内容", required = true) @RequestBody Map<String, String> request) {
        
        try {
            String jsonContent = request.get("jsonContent");
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return error("JSON内容不能为空");
            }
            
            TemplateValidationResult result = aiTemplateService.validateTemplateJSON(jsonContent);
            
            return success(result);
            
        } catch (Exception e) {
            log.error("验证JSON失败", e);
            TemplateValidationResult result = new TemplateValidationResult();
            result.addError("JSON格式错误: " + e.getMessage());
            return success(result);
        }
    }

    /**
     * 导入AI生成的模板
     */
    @PostMapping("/import-template")
    @ApiOperation(value = "导入AI生成的模板", notes = "将AI生成的JSON导入为抽取模板")
    public Map<String, Object> importTemplate(
            @ApiParam(value = "JSON内容", required = true) @RequestBody Map<String, String> request) {
        
        try {
            String jsonContent = request.get("jsonContent");
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return error("JSON内容不能为空");
            }
            
            // 先验证
            TemplateValidationResult validation = aiTemplateService.validateTemplateJSON(jsonContent);
            if (!validation.isValid()) {
                return error("JSON格式验证失败", validation.getErrors());
            }
            
            // 导入模板
            TemplateImportResult result = aiTemplateService.importTemplate(jsonContent);
            
            return success(result);
            
        } catch (Exception e) {
            log.error("导入模板失败", e);
            return error("导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 成功响应
     */
    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", data);
        return response;
    }

    /**
     * 错误响应
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    /**
     * 错误响应（带详情）
     */
    private Map<String, Object> error(String message, List<String> errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("success", false);
        response.put("message", message);
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        response.put("data", data);
        return response;
    }
}

