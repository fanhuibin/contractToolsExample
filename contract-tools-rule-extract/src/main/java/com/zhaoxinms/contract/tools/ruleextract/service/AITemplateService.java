package com.zhaoxinms.contract.tools.ruleextract.service;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.model.MinerURecognitionResult;
import com.zhaoxinms.contract.tools.comparePRO.service.MinerUOCRService;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.ruleextract.dto.*;
import com.zhaoxinms.contract.tools.ruleextract.model.ExtractionRuleModel;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleTemplateModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 模板服务
 * 
 * @author 山西肇新科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AITemplateService {

    private final ObjectMapper objectMapper;
    private final TemplateService templateService;
    private final MinerUOCRService minerUOCRService;

    /**
     * 提取文档文本（使用 MinerU OCR）
     */
    public DocumentTextResult extractDocumentText(MultipartFile file, String format) throws Exception {
        String fileName = file.getOriginalFilename();
        log.info("开始提取文档文本（使用 MinerU OCR）: {}", fileName);
        
        // 保存上传的文件到临时目录
        File tempFile = saveTempFile(file);
        File tempOutputDir = null;
        
        try {
            // 创建临时输出目录
            tempOutputDir = Files.createTempDirectory("ai-template-ocr-").toFile();
            
            // 创建 MinerU 选项（不过滤页眉页脚，AI 需要完整内容）
            CompareOptions options = new CompareOptions();
            options.setIgnoreHeaderFooter(false);
            options.setHeaderHeightPercent(0.0);
            options.setFooterHeightPercent(0.0);
            
            // 调用 MinerU OCR 服务识别 PDF
            log.info("调用 MinerU OCR 服务识别 PDF...");
            String taskId = UUID.randomUUID().toString();
            MinerURecognitionResult mineruResult = minerUOCRService.recognizePdf(
                tempFile,
                taskId,
                tempOutputDir,
                "ai-template",  // 文档模式
                options
            );
            
            // 从识别结果中提取文本
            String textContent = extractTextFromMinerUResult(mineruResult);
            log.info("MinerU OCR 识别完成，提取文本长度: {} 字符", textContent.length());
            
            // 从 MinerU 结果中获取页数
            int pageCount = mineruResult.layouts.length;
            log.debug("文档页数: {}", pageCount);
            
            // 构建结果
            DocumentTextResult.DocumentTextResultBuilder builder = DocumentTextResult.builder()
                .fileName(fileName)
                .extractTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .textContent(textContent)
                .pageCount(pageCount);
            
            // 如果是 structured 格式，构建结构化数据
            if ("structured".equals(format)) {
                DocumentTextResult.StructuredData structuredData = buildStructuredData(textContent);
                builder.structuredData(structuredData);
            }
            
            return builder.build();
            
        } finally {
            // 清理临时文件和目录
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile.getAbsolutePath(), e);
                }
            }
            
            if (tempOutputDir != null && tempOutputDir.exists()) {
                try {
                    deleteDirectory(tempOutputDir);
                    log.debug("临时输出目录已删除: {}", tempOutputDir.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("删除临时输出目录失败: {}", tempOutputDir.getAbsolutePath(), e);
                }
            }
        }
    }
    
    /**
     * 从 MinerU 识别结果中提取文本
     */
    private String extractTextFromMinerUResult(MinerURecognitionResult result) {
        StringBuilder allText = new StringBuilder();
        
        TextExtractionUtil.PageLayout[] layouts = result.layouts;
        for (int i = 0; i < layouts.length; i++) {
            TextExtractionUtil.PageLayout layout = layouts[i];
            
            // 添加页面间分隔
            if (allText.length() > 0 && i > 0) {
                allText.append("\n\n");
            }
            
            // 提取每个页面的文本
            for (TextExtractionUtil.LayoutItem item : layout.items) {
                if (item.text != null && !item.text.trim().isEmpty()) {
                    allText.append(item.text.trim()).append("\n");
                }
            }
        }
        
        return allText.toString();
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.delete(file.toPath());
                }
            }
        }
        Files.delete(directory.toPath());
    }
    
    /**
     * 保存上传文件到临时目录
     */
    private File saveTempFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 创建临时文件
        File tempFile = File.createTempFile("ai-template-", extension);
        file.transferTo(tempFile);
        log.debug("文件已保存到临时目录: {}", tempFile.getAbsolutePath());
        
        return tempFile;
    }

    /**
     * 构建结构化数据
     */
    private DocumentTextResult.StructuredData buildStructuredData(String textContent) {
        List<DocumentTextResult.PageContent> pages = new ArrayList<>();
        
        // 简单按段落分组（实际应该按页分）
        String[] paragraphs = textContent.split("\n\n");
        int pageNo = 1;
        StringBuilder pageContent = new StringBuilder();
        int charCount = 0;
        
        for (String para : paragraphs) {
            pageContent.append(para).append("\n\n");
            charCount += para.length();
            
            // 每1500字符作为一页
            if (charCount >= 1500) {
                List<String> keywords = extractKeywords(pageContent.toString());
                pages.add(DocumentTextResult.PageContent.builder()
                    .pageNo(pageNo++)
                    .content(pageContent.toString())
                    .keywords(keywords)
                    .build());
                
                pageContent = new StringBuilder();
                charCount = 0;
            }
        }
        
        // 最后一页
        if (pageContent.length() > 0) {
            List<String> keywords = extractKeywords(pageContent.toString());
            pages.add(DocumentTextResult.PageContent.builder()
                .pageNo(pageNo)
                .content(pageContent.toString())
                .keywords(keywords)
                .build());
        }
        
        return DocumentTextResult.StructuredData.builder()
            .pages(pages)
            .build();
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();
        
        // 常见的合同关键词模式
        String[] patterns = {
            "合同编号[：:：]",
            "甲方[：:：]",
            "乙方[：:：]",
            "签订日期[：:：]",
            "合同金额[：:：]",
            "付款方式[：:：]"
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(content);
            if (m.find()) {
                keywords.add(m.group().replaceAll("[：:：]", ""));
            }
        }
        
        return keywords;
    }

    /**
     * 验证模板JSON
     */
    public TemplateValidationResult validateTemplateJSON(String jsonContent) {
        TemplateValidationResult result = new TemplateValidationResult();
        
        try {
            // 解析 JSON
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // 必填字段检查
            if (!root.has("templateName") || root.get("templateName").asText().isEmpty()) {
                result.addError("缺少必填字段: templateName");
            }
            
            if (!root.has("fields")) {
                result.addError("缺少必填字段: fields");
            } else {
                JsonNode fields = root.get("fields");
                if (!fields.isArray()) {
                    result.addError("fields 必须是数组");
                } else if (fields.size() == 0) {
                    result.addError("fields 不能为空，至少需要一个字段");
                } else {
                    // 验证每个字段
                    for (int i = 0; i < fields.size(); i++) {
                        validateField(fields.get(i), i, result);
                    }
                }
            }
            
            result.setValid(result.getErrors().isEmpty());
            
        } catch (Exception e) {
            result.addError("JSON 解析失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 验证单个字段
     */
    private void validateField(JsonNode field, int index, TemplateValidationResult result) {
        String prefix = "字段[" + index + "] ";
        
        // 必填字段
        if (!field.has("fieldName") || field.get("fieldName").asText().isEmpty()) {
            result.addError(prefix + "缺少 fieldName");
        }
        
        if (!field.has("fieldLabel") || field.get("fieldLabel").asText().isEmpty()) {
            result.addError(prefix + "缺少 fieldLabel");
        }
        
        if (!field.has("extractRules")) {
            result.addError(prefix + "缺少 extractRules");
        } else {
            validateExtractRules(field.get("extractRules"), prefix, result);
        }
        
        // 可选但建议的字段
        if (!field.has("fieldType")) {
            result.addWarning(prefix + "建议添加 fieldType 字段");
        }
        
        if (!field.has("required")) {
            result.addWarning(prefix + "建议添加 required 字段");
        }
        
        // 检查置信度
        if (field.has("confidence")) {
            int confidence = field.get("confidence").asInt();
            if (confidence < 60) {
                result.addWarning(prefix + "置信度较低 (" + confidence + "%)，建议人工核验");
            }
        }
    }

    /**
     * 验证提取规则
     */
    private void validateExtractRules(JsonNode rules, String prefix, TemplateValidationResult result) {
        if (!rules.has("type")) {
            result.addError(prefix + "extractRules 缺少 type 字段");
        }
        
        String type = rules.has("type") ? rules.get("type").asText() : "";
        
        if ("keyword".equals(type)) {
            if (!rules.has("keyword") || rules.get("keyword").asText().isEmpty()) {
                result.addError(prefix + "keyword 类型规则必须有 keyword 字段");
            }
        } else if ("regex".equals(type)) {
            if (!rules.has("pattern") || rules.get("pattern").asText().isEmpty()) {
                result.addError(prefix + "regex 类型规则必须有 pattern 字段");
            }
        } else if ("table".equals(type)) {
            // 验证表格规则
            if (!rules.has("tableRules")) {
                result.addError(prefix + "table 类型规则必须有 tableRules 字段");
            } else {
                JsonNode tableRules = rules.get("tableRules");
                
                // 验证 tableKeyword
                if (!tableRules.has("tableKeyword") || tableRules.get("tableKeyword").asText().isEmpty()) {
                    result.addWarning(prefix + "tableRules 建议添加 tableKeyword 用于定位表格");
                }
                
                // 验证 columns
                if (!tableRules.has("columns")) {
                    result.addError(prefix + "tableRules 必须有 columns 字段（表格列名数组）");
                } else {
                    JsonNode columns = tableRules.get("columns");
                    if (!columns.isArray()) {
                        result.addError(prefix + "tableRules.columns 必须是数组");
                    } else if (columns.size() == 0) {
                        result.addError(prefix + "tableRules.columns 不能为空，至少需要一个列名");
                    }
                }
            }
        } else {
            result.addWarning(prefix + "未知的规则类型: " + type + "，支持的类型：keyword, regex, table");
        }
    }

    /**
     * 导入模板
     */
    public TemplateImportResult importTemplate(String jsonContent) throws Exception {
        // 解析 AI 模板
        AITemplateDTO aiTemplate = objectMapper.readValue(jsonContent, AITemplateDTO.class);
        
        log.info("导入 AI 模板: {}, 字段数: {}", aiTemplate.getTemplateName(), aiTemplate.getFields().size());
        
        // 转换为系统模板模型
        RuleTemplateModel template = convertToRuleTemplateModel(aiTemplate);
        
        // 保存模板
        String templateId = templateService.createTemplate(template);
        log.info("模板保存成功: id={}, name={}", templateId, template.getTemplateName());
        
        // 收集警告信息
        List<String> warnings = new ArrayList<>();
        for (AITemplateDTO.AIFieldDTO field : aiTemplate.getFields()) {
            if (field.getConfidence() != null && field.getConfidence() < 60) {
                warnings.add("字段 [" + field.getFieldLabel() + "] 置信度较低 (" + field.getConfidence() + "%)，建议核验");
            }
        }
        
        return TemplateImportResult.builder()
            .templateId(templateId)
            .templateName(aiTemplate.getTemplateName())
            .fieldCount(aiTemplate.getFields().size())
            .warnings(warnings)
            .nextStep("请在模板编辑器中测试和优化规则")
            .build();
    }
    
    /**
     * 将 AI 模板 DTO 转换为规则模板模型
     */
    private RuleTemplateModel convertToRuleTemplateModel(AITemplateDTO aiTemplate) {
        RuleTemplateModel template = new RuleTemplateModel();
        
        // 基本信息
        template.setTemplateName(aiTemplate.getTemplateName());
        template.setDescription(aiTemplate.getDescription());
        template.setStatus("draft");
        template.setCreatedBy("AI生成");
        
        // 生成模板编号
        String templateCode = generateTemplateCode(aiTemplate.getTemplateName());
        template.setTemplateCode(templateCode);
        
        // 转换字段列表
        List<FieldDefinitionModel> fields = new ArrayList<>();
        int sortOrder = 1;
        for (AITemplateDTO.AIFieldDTO aiField : aiTemplate.getFields()) {
            FieldDefinitionModel field = convertToFieldDefinitionModel(aiField, sortOrder++);
            fields.add(field);
        }
        template.setFields(fields);
        
        return template;
    }
    
    /**
     * 转换字段定义
     */
    private FieldDefinitionModel convertToFieldDefinitionModel(AITemplateDTO.AIFieldDTO aiField, int sortOrder) {
        FieldDefinitionModel field = new FieldDefinitionModel();
        
        // 基本信息
        field.setFieldName(aiField.getFieldLabel());
        field.setFieldCode(aiField.getFieldName());
        field.setFieldType(aiField.getFieldType() != null ? aiField.getFieldType() : "text");
        field.setIsRequired(aiField.getRequired() != null ? aiField.getRequired() : false);
        field.setSortOrder(sortOrder);
        
        // 提取规则
        if (aiField.getExtractRules() != null) {
            ExtractionRuleModel rule = convertToExtractionRuleModel(aiField);
            field.setRules(Collections.singletonList(rule));
        }
        
        return field;
    }
    
    /**
     * 转换提取规则
     */
    private ExtractionRuleModel convertToExtractionRuleModel(AITemplateDTO.AIFieldDTO aiField) {
        ExtractionRuleModel rule = new ExtractionRuleModel();
        
        AITemplateDTO.ExtractRules extractRules = aiField.getExtractRules();
        
        // 规则基本信息
        rule.setRuleName(aiField.getFieldLabel() + " - AI生成规则");
        rule.setPriority(1);
        rule.setIsEnabled(true);
        
        // 转换规则类型：小写 → 大写枚举值
        String aiRuleType = extractRules.getType() != null ? extractRules.getType().toLowerCase() : "keyword";
        String systemRuleType;
        Map<String, Object> ruleContent = new HashMap<>();
        
        if ("keyword".equals(aiRuleType)) {
            // AI 的 "keyword" → 系统的 "KEYWORD_ANCHOR"
            systemRuleType = "KEYWORD_ANCHOR";
            
            // 构建 KEYWORD_ANCHOR 规则配置
            // 前端期望的字段名：anchor, direction, extractMethod, pattern, maxLength, maxDistance 等
            ruleContent.put("anchor", extractRules.getKeyword()); // keyword → anchor
            ruleContent.put("direction", "after"); // 默认向后提取
            ruleContent.put("maxDistance", 200); // 最大搜索距离
            ruleContent.put("delimiter", "："); // 默认分隔符
            ruleContent.put("multiline", false); // 是否多行
            ruleContent.put("matchMode", "single"); // 匹配模式
            ruleContent.put("occurrence", 1); // 出现次数
            ruleContent.put("returnAll", false); // 是否返回全部
            
            // 根据是否有 pattern 决定提取方法
            if (extractRules.getPattern() != null && !extractRules.getPattern().isEmpty()) {
                // 有正则表达式，使用正则提取
                ruleContent.put("extractMethod", "regex");
                ruleContent.put("pattern", extractRules.getPattern());
            } else {
                // 无正则表达式，使用固定长度提取
                ruleContent.put("extractMethod", "fixed");
                ruleContent.put("maxLength", extractRules.getLength() != null ? extractRules.getLength() : 50);
            }
            
        } else if ("regex".equals(aiRuleType)) {
            // AI 的 "regex" → 系统的 "REGEX_PATTERN"
            systemRuleType = "REGEX_PATTERN";
            
            // 构建 REGEX_PATTERN 规则配置
            ruleContent.put("pattern", extractRules.getPattern());
            
        } else if ("table".equals(aiRuleType)) {
            // AI 的 "table" → 系统的 "TABLE_CELL"
            systemRuleType = "TABLE_CELL";
            
            // 构建 TABLE_CELL 规则配置（整表模式）
            ruleContent.put("extractMode", "table"); // 整表模式
            ruleContent.put("format", "json"); // 默认 JSON 格式
            ruleContent.put("occurrence", 1); // 出现次数
            
            // 处理 tableRules
            if (extractRules.getTableRules() != null) {
                AITemplateDTO.TableRules tableRules = extractRules.getTableRules();
                
                // 表头特征：将 columns 数组转为 | 分隔的字符串
                if (tableRules.getColumns() != null && !tableRules.getColumns().isEmpty()) {
                    String headerPattern = String.join("|", tableRules.getColumns());
                    ruleContent.put("headerPattern", headerPattern);
                } else {
                    ruleContent.put("headerPattern", "");
                }
                
                // 表格关键词（用于定位表格）
                if (tableRules.getTableKeyword() != null) {
                    ruleContent.put("tableKeyword", tableRules.getTableKeyword());
                }
            } else {
                // 没有 tableRules，使用 keyword 作为表头特征
                ruleContent.put("headerPattern", extractRules.getKeyword() != null ? extractRules.getKeyword() : "");
            }
            
        } else {
            // 未知类型，默认使用 KEYWORD_ANCHOR
            log.warn("未知的 AI 规则类型: {}, 默认使用 KEYWORD_ANCHOR", extractRules.getType());
            systemRuleType = "KEYWORD_ANCHOR";
            ruleContent.put("anchor", extractRules.getKeyword() != null ? extractRules.getKeyword() : "");
            ruleContent.put("direction", "after");
            ruleContent.put("extractMethod", "fixed");
            ruleContent.put("maxLength", 50);
        }
        
        rule.setRuleType(systemRuleType);
        rule.setRuleContent(JSON.toJSONString(ruleContent));
        
        log.debug("AI规则转换: {} → {} | 配置: {}", aiRuleType, systemRuleType, ruleContent);
        
        return rule;
    }
    
    /**
     * 生成模板编号
     */
    private String generateTemplateCode(String templateName) {
        // 使用模板名称拼音首字母 + 时间戳
        String code = "AI_TEMPLATE_" + System.currentTimeMillis();
        return code;
    }
}

