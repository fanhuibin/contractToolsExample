package com.zhaoxinms.contract.tools.ruleextract.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhaoxinms.contract.tools.common.ocr.OCRProvider;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.EnhancedRuleEngine;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.ExtractionResult;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.ExtractionRule;
import com.zhaoxinms.contract.tools.ruleextract.engine.enhanced.RuleType;
import com.zhaoxinms.contract.tools.ruleextract.model.ExtractionRuleModel;
import com.zhaoxinms.contract.tools.ruleextract.model.FieldDefinitionModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleExtractTaskModel;
import com.zhaoxinms.contract.tools.ruleextract.model.RuleTemplateModel;
import com.zhaoxinms.contract.tools.ruleextract.storage.JsonFileStorage;
import com.zhaoxinms.contract.tools.ruleextract.utils.FormatConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则抽取服务（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExtractService {

    private final JsonFileStorage storage;
    private final OCRProvider ocrProvider;
    private final EnhancedRuleEngine enhancedRuleEngine;

    /**
     * 创建抽取任务
     */
    public String createTask(MultipartFile file, String templateId, String ocrProvider) {
        return createTask(file, templateId, ocrProvider, true, 12.0, 12.0);
    }

    /**
     * 创建抽取任务（支持页眉页脚设置）
     */
    public String createTask(MultipartFile file, String templateId, String ocrProvider,
                           boolean ignoreHeaderFooter, double headerHeightPercent, double footerHeightPercent) {
        try {
            // 验证模板
            RuleTemplateModel template = storage.load("template", templateId, RuleTemplateModel.class);
            if (template == null) {
                throw new IllegalArgumentException("模板不存在");
            }

            // 生成任务ID
            String taskId = storage.generateId();

            // 保存上传的文件
            String filePath = storage.saveUploadFile(taskId, file.getOriginalFilename(), file.getBytes());

            // 创建任务记录
            RuleExtractTaskModel task = new RuleExtractTaskModel();
            task.setTaskId(taskId);
            task.setTemplateId(templateId);
            task.setTemplateName(template.getTemplateName());
            task.setFileName(file.getOriginalFilename());
            task.setFilePath(filePath);
            task.setFileSize(file.getSize());
            task.setOcrProvider(ocrProvider != null ? ocrProvider : "mineru");
            task.setIgnoreHeaderFooter(ignoreHeaderFooter);
            task.setHeaderHeightPercent(headerHeightPercent);
            task.setFooterHeightPercent(footerHeightPercent);
            task.setStatus("pending");
            task.setProgress(0);
            task.setMessage("任务已创建，等待处理");
            task.setCreatedAt(LocalDateTime.now());
            task.setStartedAt(LocalDateTime.now());

            storage.save("task", taskId, task);
            log.info("创建抽取任务: taskId={}, templateId={}, fileName={}", taskId, templateId, file.getOriginalFilename());

            // 异步处理任务
            processTaskAsync(taskId);

            return taskId;
        } catch (Exception e) {
            log.error("创建任务失败", e);
            throw new RuntimeException("创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理任务
     */
    @Async
    public void processTaskAsync(String taskId) {
        try {
            processTask(taskId);
        } catch (Exception e) {
            log.error("处理任务失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "failed", 0, "处理失败: " + e.getMessage(), e.getMessage());
        }
    }

    /**
     * 处理任务
     */
    private void processTask(String taskId) {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }

        try {
            // 1. 更新状态：文件已上传
            updateTaskStatus(taskId, "file_uploaded", 10, "文件已上传", null);

            // 2. OCR处理
            updateTaskStatus(taskId, "ocr_processing", 20, "OCR处理中...", null);
            OCRProvider.OCRResult ocrResult = performOCR(task);
            String ocrText = ocrResult.getContent();
            
            // 保存OCR文本和结果路径
            task = storage.load("task", taskId, RuleExtractTaskModel.class);
            
            // 保存OCR文本到文件
            try {
                String ocrTextDir = storage.getDataRoot() + File.separator + "ocr-texts";
                FileUtil.mkdir(ocrTextDir);
                String ocrTextPath = ocrTextDir + File.separator + taskId + ".txt";
                FileUtil.writeUtf8String(ocrText, ocrTextPath);
                task.setOcrResultPath(ocrTextPath);
                log.info("保存OCR文本: {}", ocrTextPath);
            } catch (Exception e) {
                log.warn("保存OCR文本失败: {}", e.getMessage());
            }
            
            storage.save("task", taskId, task);
            updateTaskStatus(taskId, "ocr_processing", 50, "OCR处理完成", null);

            // 3. 位置映射（如果需要）
            updateTaskStatus(taskId, "position_mapping", 60, "位置映射中...", null);
            // TODO: 处理位置映射
            updateTaskStatus(taskId, "position_mapping", 70, "位置映射完成", null);

            // 4. 提取信息
            updateTaskStatus(taskId, "extracting", 75, "信息提取中...", null);
            List<JSONObject> results = extractInformation(task, ocrText);
            updateTaskStatus(taskId, "extracting", 95, "信息提取完成", null);

            // 5. 保存结果
            saveResults(taskId, results, ocrResult);
            
            // 6. 完成
            task = storage.load("task", taskId, RuleExtractTaskModel.class);
            task.setCompletedAt(LocalDateTime.now());
            task.setDurationSeconds((int) Duration.between(task.getStartedAt(), task.getCompletedAt()).getSeconds());
            task.setStatus("completed");
            task.setProgress(100);
            task.setMessage("处理完成");
            storage.save("task", taskId, task);

            log.info("任务处理完成: taskId={}, duration={}s", taskId, task.getDurationSeconds());

        } catch (Exception e) {
            log.error("处理任务失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "failed", 0, "处理失败", e.getMessage());
        }
    }

    /**
     * 执行OCR
     */
    private OCRProvider.OCRResult performOCR(RuleExtractTaskModel task) {
        try {
            log.info("开始OCR处理: taskId={}, provider={}, file={}, 忽略页眉页脚={}", 
                task.getTaskId(), task.getOcrProvider(), task.getFileName(), task.getIgnoreHeaderFooter());
            
            File pdfFile = new File(task.getFilePath());
            log.info("PDF文件路径: 相对={}, 绝对={}, 存在={}", 
                task.getFilePath(), pdfFile.getAbsolutePath(), pdfFile.exists());
            
            if (!pdfFile.exists()) {
                throw new RuntimeException("文件不存在: " + task.getFilePath());
            }

            // 调用OCR服务（通过反射支持taskId和输出目录，避免循环依赖）
            OCRProvider.OCRResult ocrResult;
            try {
                // 【修复】尝试调用支持taskId和输出目录的方法（确保图片和taskId一致）
                java.lang.reflect.Method method = ocrProvider.getClass().getMethod(
                    "recognizePdf", 
                    File.class, String.class, File.class, boolean.class, double.class, double.class
                );
                
                // 创建OCR输出目录（使用rule-extract的taskId）
                File ocrOutputDir = storage.getOcrOutputDir(task.getTaskId());
                if (!ocrOutputDir.exists()) {
                    ocrOutputDir.mkdirs();
                }
                
                ocrResult = (OCRProvider.OCRResult) method.invoke(
                    ocrProvider,
                    pdfFile,
                    task.getTaskId(), // 传递rule-extract的taskId
                    ocrOutputDir, // 传递输出目录
                    task.getIgnoreHeaderFooter() != null ? task.getIgnoreHeaderFooter() : true,
                    task.getHeaderHeightPercent() != null ? task.getHeaderHeightPercent() : 12.0,
                    task.getFooterHeightPercent() != null ? task.getFooterHeightPercent() : 12.0
                );
                log.info("使用taskId和输出目录调用OCR服务成功: taskId={}, outputDir={}", task.getTaskId(), ocrOutputDir.getAbsolutePath());
            } catch (NoSuchMethodException e) {
                // Fallback: 尝试调用只带页眉页脚参数的方法
                log.info("OCR服务不支持taskId参数，尝试只使用页眉页脚参数");
                try {
                    java.lang.reflect.Method method2 = ocrProvider.getClass().getMethod(
                        "recognizePdf", 
                        File.class, boolean.class, double.class, double.class
                    );
                    ocrResult = (OCRProvider.OCRResult) method2.invoke(
                        ocrProvider,
                        pdfFile,
                        task.getIgnoreHeaderFooter() != null ? task.getIgnoreHeaderFooter() : true,
                        task.getHeaderHeightPercent() != null ? task.getHeaderHeightPercent() : 12.0,
                        task.getFooterHeightPercent() != null ? task.getFooterHeightPercent() : 12.0
                    );
                    log.info("使用页眉页脚参数调用OCR服务成功");
                } catch (Exception e2) {
                    // 最终fallback：使用默认方法
                    log.info("OCR服务不支持页眉页脚参数，使用默认方法");
                    ocrResult = ocrProvider.recognizePdf(pdfFile);
                }
            } catch (Exception e) {
                log.warn("调用带参数的OCR方法失败，使用默认方法: {}", e.getMessage());
                ocrResult = ocrProvider.recognizePdf(pdfFile);
            }
            String content = ocrResult.getContent();
            
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("OCR识别结果为空");
            }

            log.info("OCR处理完成: taskId={}, textLength={}", task.getTaskId(), content.length());
            return ocrResult;

        } catch (Exception e) {
            log.error("OCR处理失败: taskId={}", task.getTaskId(), e);
            throw new RuntimeException("OCR处理失败: " + e.getMessage());
        }
    }

    /**
     * 提取信息
     */
    private List<JSONObject> extractInformation(RuleExtractTaskModel task, String content) {
        // 加载模板
        RuleTemplateModel template = storage.load("template", task.getTemplateId(), RuleTemplateModel.class);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        List<JSONObject> results = new ArrayList<>();

        // 遍历每个字段进行提取
        for (FieldDefinitionModel field : template.getFields()) {
            JSONObject result = new JSONObject();
            result.put("fieldCode", field.getFieldCode());
            result.put("fieldName", field.getFieldName());
            result.put("fieldType", field.getFieldType());
            
            try {
                // 获取字段的规则（按优先级排序）
                List<ExtractionRuleModel> rules = field.getRules().stream()
                    .filter(ExtractionRuleModel::getIsEnabled)
                    .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                    .collect(Collectors.toList());

                if (rules.isEmpty()) {
                    log.warn("字段 {} 没有启用的规则", field.getFieldName());
                    result.put("rawValue", "");
                    result.put("value", "");
                    result.put("matchedRule", "");
                    result.put("status", "no_rules");
                    results.add(result);
                    continue;
                }

                // 尝试每个规则直到成功
                boolean matched = false;
                for (ExtractionRuleModel rule : rules) {
                    try {
                        ExtractionResult extractionResult = matchByRuleWithPosition(content, rule);
                        if (extractionResult != null && extractionResult.getSuccess() && 
                            extractionResult.getValue() != null && !extractionResult.getValue().trim().isEmpty()) {
                            
                            // 栏位验证功能已移除
                            
                            String rawValue = extractionResult.getValue();
                            result.put("rawValue", rawValue);
                            result.put("matchedRule", rule.getRuleName());
                            
                            // 格式转换
                            Object formattedValue = rawValue;
                            try {
                                if (field.getFieldType() != null) {
                                    FormatConverter.FieldType fieldType = FormatConverter.FieldType.valueOf(
                                        field.getFieldType().toUpperCase()
                                    );
                                    formattedValue = FormatConverter.convert(rawValue, fieldType, field.getOutputFormat());
                                }
                            } catch (Exception e) {
                                log.warn("字段类型转换失败: field={}, type={}, error={}", 
                                    field.getFieldName(), field.getFieldType(), e.getMessage());
                                // 转换失败时使用原始值
                            }
                            
                            result.put("value", formattedValue);
                            result.put("status", "success");
                            
                            // 保存位置信息
                            if (extractionResult.getStartPosition() != null && extractionResult.getEndPosition() != null) {
                                JSONObject charInterval = new JSONObject();
                                charInterval.put("startPos", extractionResult.getStartPosition());
                                charInterval.put("endPos", extractionResult.getEndPosition());
                                charInterval.put("sourceText", extractionResult.getValue());
                                result.put("charInterval", charInterval);
                            }
                            
                            // 保存表格数据（如果有）
                            if (extractionResult.getTableData() != null) {
                                result.put("tableData", extractionResult.getTableData());
                            }
                            
                            matched = true;
                            log.info("字段提取成功: field={}, value={}, startPos={}, endPos={}, rule={}", 
                                field.getFieldName(), extractionResult.getValue(), 
                                extractionResult.getStartPosition(), extractionResult.getEndPosition(),
                                rule.getRuleName());
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("规则匹配失败: field={}, rule={}, error={}", 
                            field.getFieldName(), rule.getRuleName(), e.getMessage());
                    }
                }
                
                // 如果没有匹配到，也要添加字段（值为空）
                if (!matched) {
                    result.put("rawValue", "");
                    result.put("value", "");
                    result.put("matchedRule", "");
                    result.put("status", "not_found");
                    log.info("字段未提取到值: field={}", field.getFieldName());
                }
                
                results.add(result);
                
            } catch (Exception e) {
                log.error("字段提取失败: field={}", field.getFieldName(), e);
                result.put("rawValue", "");
                result.put("value", "");
                result.put("matchedRule", "");
                result.put("status", "error");
                result.put("error", e.getMessage());
                results.add(result);
            }
        }

        return results;
    }

    /**
     * 使用规则匹配文本（使用增强引擎，返回位置信息）
     */
    private ExtractionResult matchByRuleWithPosition(String content, ExtractionRuleModel rule) {
        try {
            // 转换为增强引擎的规则格式
            ExtractionRule enhancedRule = new ExtractionRule();
            enhancedRule.setId(rule.getId());
            enhancedRule.setEnabled(rule.getIsEnabled());
            enhancedRule.setPriority(rule.getPriority());
            
            // 转换规则类型
            try {
                enhancedRule.setRuleType(RuleType.valueOf(rule.getRuleType()));
            } catch (Exception e) {
                log.warn("未知的规则类型: {}, 尝试旧格式兼容", rule.getRuleType());
                // 兼容旧的规则类型
                switch (rule.getRuleType().toLowerCase()) {
                    case "regex":
                        enhancedRule.setRuleType(RuleType.REGEX_PATTERN);
                        break;
                    case "keyword":
                        enhancedRule.setRuleType(RuleType.KEYWORD_ANCHOR);
                        break;
                    default:
                        log.error("无法识别的规则类型: {}", rule.getRuleType());
                        return null;
                }
            }
            
            // 解析配置
            JSONObject config = JSON.parseObject(rule.getRuleContent());
            enhancedRule.setConfig(config);
            
            // 使用增强引擎提取
            ExtractionResult result = enhancedRuleEngine.extract(content, enhancedRule, false);
            
            return result;
        } catch (Exception e) {
            log.error("规则匹配失败: ruleType={}, error={}", rule.getRuleType(), e.getMessage(), e);
            return null;
        }
    }


    /**
     * 保存提取结果
     */
    private void saveResults(String taskId, List<JSONObject> results, OCRProvider.OCRResult ocrResult) {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            return;
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("taskId", taskId);
        resultJson.put("extractedAt", LocalDateTime.now());
        resultJson.put("totalFields", results.size());
        resultJson.put("extractResults", results);  // 改为 extractResults
        
        // 保存OCR文本到resultJson
        if (ocrResult != null && ocrResult.getContent() != null) {
            resultJson.put("ocrText", ocrResult.getContent());
        }
        
        // 保存OCR元数据（从metadata中提取）
        JSONObject metaJson = null;
        String textBoxesJson = null; // 使用TextBox格式（文本块级别，高效）
        
        if (ocrResult != null && ocrResult.getMetadata() != null) {
            try {
                Object metadata = ocrResult.getMetadata();
                log.info("OCR metadata 类型: {}", metadata.getClass().getName());
                
                // 尝试将metadata作为JSONObject处理
                if (metadata instanceof JSONObject) {
                    metaJson = (JSONObject) metadata;
                } else if (metadata instanceof String) {
                    // 如果是JSON字符串，尝试解析
                    metaJson = JSON.parseObject((String) metadata);
                }
                
                if (metaJson != null) {
                    log.info("Metadata keys: {}", metaJson.keySet());
                    log.info("Metadata textBoxes字段存在: {}", metaJson.containsKey("textBoxes"));
                    log.info("Metadata pageDimensions字段存在: {}", metaJson.containsKey("pageDimensions"));
                    
                    if (metaJson.containsKey("totalPages")) {
                        resultJson.put("totalPages", metaJson.getInteger("totalPages"));
                        log.info("保存totalPages: {}", metaJson.getInteger("totalPages"));
                    }
                    if (metaJson.containsKey("pageImagePaths")) {
                        resultJson.put("pageImagePaths", metaJson.get("pageImagePaths"));
                    }
                    if (metaJson.containsKey("textBoxes")) {
                        // 使用TextBox格式（文本块级别，更高效）
                        textBoxesJson = metaJson.getString("textBoxes");
                        task.setCharBoxes(textBoxesJson); // 复用CharBoxes字段存储TextBox数据
                        log.info("保存TextBox数据，长度: {}", textBoxesJson.length());
                    } else if (metaJson.containsKey("charBoxes")) {
                        // 兼容旧的CharBox格式（不推荐，性能差）
                        String charBoxesJson = metaJson.getString("charBoxes");
                        task.setCharBoxes(charBoxesJson);
                        log.warn("使用旧的CharBox格式（性能较差），建议升级到TextBox格式");
                    } else {
                        log.warn("Metadata中没有textBoxes字段，无法生成bbox数据");
                    }
                    
                    if (metaJson.containsKey("pageDimensions")) {
                        // 保存页面尺寸信息到task
                        String pageDimensionsStr = JSON.toJSONString(metaJson.get("pageDimensions"));
                        task.setPageDimensions(pageDimensionsStr);
                        log.info("保存页面尺寸信息: {}", pageDimensionsStr);
                    } else {
                        log.warn("Metadata中没有pageDimensions字段");
                    }
                } else {
                    log.warn("无法解析metadata为JSONObject");
                }
            } catch (Exception e) {
                log.error("提取OCR元数据失败: {}", e.getMessage(), e);
            }
        } else {
            log.warn("OCR结果或metadata为空");
        }
        
        // 生成BboxMappings（从提取结果和TextBox数据中）
        try {
            if (textBoxesJson != null && !textBoxesJson.isEmpty() && ocrResult != null && ocrResult.getContent() != null) {
                List<JSONObject> bboxMappings = generateBboxMappingsFromTextBoxes(results, textBoxesJson, ocrResult.getContent());
                if (!bboxMappings.isEmpty()) {
                    task.setBboxMappings(JSON.toJSONString(bboxMappings));
                    log.info("成功生成 {} 个BboxMapping", bboxMappings.size());
                } else {
                    log.warn("未能生成任何BboxMapping");
                }
            } else {
                log.warn("TextBox数据或OCR结果为空，无法生成BboxMappings");
            }
        } catch (Exception e) {
            log.error("生成BboxMappings失败: {}", e.getMessage(), e);
        }

        task.setResultJson(resultJson.toJSONString());
        storage.save("task", taskId, task);
    }
    
    /**
     * TextBox数据辅助类（文本块级别，性能优于字符级别）
     */
    private static class TextBoxData {
        Integer page;
        String text;
        Integer startPos;
        Integer endPos;
        double[] bbox;
    }

    /**
     * 从TextBox数据生成BboxMappings（文本块级别，高效）
     */
    private List<JSONObject> generateBboxMappingsFromTextBoxes(List<JSONObject> results, String textBoxesJson, String fullText) {
        List<JSONObject> bboxMappings = new ArrayList<>();
        
        if (textBoxesJson == null || textBoxesJson.isEmpty()) {
            log.warn("TextBox数据为空");
            return bboxMappings;
        }
        
        // 解析TextBox数据
        List<TextBoxData> textBoxes = new ArrayList<>();
        try {
            log.info("开始解析TextBox数据，字符串长度: {}", textBoxesJson.length());
            com.alibaba.fastjson2.JSONArray textBoxArray = JSON.parseArray(textBoxesJson);
            for (int i = 0; i < textBoxArray.size(); i++) {
                com.alibaba.fastjson2.JSONObject textBoxObj = textBoxArray.getJSONObject(i);
                TextBoxData textBox = new TextBoxData();
                textBox.page = textBoxObj.getInteger("page");
                textBox.text = textBoxObj.getString("text");
                textBox.startPos = textBoxObj.getInteger("startPos");
                textBox.endPos = textBoxObj.getInteger("endPos");
                com.alibaba.fastjson2.JSONArray bboxArray = textBoxObj.getJSONArray("bbox");
                if (bboxArray != null && bboxArray.size() == 4) {
                    textBox.bbox = new double[]{
                        bboxArray.getDoubleValue(0),
                        bboxArray.getDoubleValue(1),
                        bboxArray.getDoubleValue(2),
                        bboxArray.getDoubleValue(3)
                    };
                }
                textBoxes.add(textBox);
            }
            log.info("成功解析 {} 个TextBox数据", textBoxes.size());
        } catch (Exception e) {
            log.error("解析TextBox数据失败: {}", e.getMessage(), e);
            return bboxMappings;
        }
        
        log.info("开始从 {} 个TextBox生成BboxMappings，提取结果数: {}", textBoxes.size(), results.size());
        
        // 为每个提取结果生成bbox映射
        for (JSONObject result : results) {
            if (result.containsKey("charInterval") && result.getJSONObject("charInterval") != null) {
                JSONObject charInterval = result.getJSONObject("charInterval");
                Integer startPos = charInterval.getInteger("startPos");
                Integer endPos = charInterval.getInteger("endPos");
                String value = result.getString("value");
                
                if (startPos != null && endPos != null && startPos >= 0 && endPos <= fullText.length() && value != null) {
                    JSONObject mapping = new JSONObject();
                    mapping.put("startPos", startPos);
                    mapping.put("endPos", endPos);
                    mapping.put("value", value);
                    mapping.put("fieldName", result.getString("fieldName"));
                    mapping.put("fieldCode", result.getString("fieldCode"));
                    
                    // 查找与该区间重叠的所有TextBox
                    List<JSONObject> bboxes = new ArrayList<>();
                    java.util.Set<Integer> pageSet = new java.util.HashSet<>();
                    
                    for (TextBoxData textBox : textBoxes) {
                        // 检查TextBox的范围是否与提取结果的范围重叠
                        if (textBox.startPos != null && textBox.endPos != null &&
                            textBox.startPos < endPos && textBox.endPos > startPos) {
                            
                            JSONObject bbox = new JSONObject();
                            bbox.put("page", textBox.page);
                            bbox.put("bbox", textBox.bbox);
                            bboxes.add(bbox);
                            pageSet.add(textBox.page);
                            
                            log.debug("  字段 {} 匹配TextBox: page={}, [{},{}] 与 [{},{}] 重叠",
                                result.getString("fieldName"), textBox.page, 
                                textBox.startPos, textBox.endPos, startPos, endPos);
                        }
                    }
                    
                    mapping.put("bboxes", bboxes);
                    mapping.put("pages", new ArrayList<>(pageSet));
                    
                    bboxMappings.add(mapping);
                    
                    log.info("  字段 '{}': 找到 {} 个bbox，涉及 {} 个页面",
                        result.getString("fieldName"), bboxes.size(), pageSet.size());
                }
            }
        }
        
        log.info("生成BboxMappings完成，共 {} 个映射", bboxMappings.size());
        return bboxMappings;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, int progress, String message, String errorMessage) {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            return;
        }

        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage(message);
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        
        storage.save("task", taskId, task);
    }

    /**
     * 查询任务状态
     */
    public RuleExtractTaskModel getTaskStatus(String taskId) {
        return storage.load("task", taskId, RuleExtractTaskModel.class);
    }

    /**
     * 查询任务结果
     */
    public JSONObject getTaskResult(String taskId) {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if (!"completed".equals(task.getStatus())) {
            throw new IllegalStateException("任务尚未完成");
        }

        JSONObject result = new JSONObject();
        result.put("taskId", task.getTaskId());
        result.put("templateId", task.getTemplateId());
        result.put("templateName", task.getTemplateName());
        result.put("fileName", task.getFileName());
        result.put("status", task.getStatus());
        result.put("completedAt", task.getCompletedAt());
        result.put("durationSeconds", task.getDurationSeconds());
        
        // 添加OCR结果路径和文本
        if (task.getOcrResultPath() != null) {
            result.put("ocrResultPath", task.getOcrResultPath());
            // 尝试读取OCR文本
            try {
                java.io.File ocrFile = new java.io.File(task.getOcrResultPath());
                if (ocrFile.exists()) {
                    String ocrText = new String(java.nio.file.Files.readAllBytes(ocrFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    result.put("ocrText", ocrText);
                    
                    // 尝试解析OCR JSON以获取页数
                    try {
                        JSONObject ocrJson = JSON.parseObject(ocrText);
                        if (ocrJson.containsKey("totalPages")) {
                            result.put("totalPages", ocrJson.getInteger("totalPages"));
                        } else if (ocrJson.containsKey("pageCount")) {
                            result.put("totalPages", ocrJson.getInteger("pageCount"));
                        }
                    } catch (Exception e) {
                        // OCR结果可能不是JSON格式，忽略
                    }
                }
            } catch (Exception e) {
                log.warn("读取OCR文本失败: {}", e.getMessage());
            }
        }
        
        // 添加位置映射和字符框数据
        if (task.getBboxMappings() != null) {
            try {
                result.put("bboxMappings", JSON.parseObject(task.getBboxMappings()));
            } catch (Exception e) {
                result.put("bboxMappings", task.getBboxMappings());
            }
        }
        
        if (task.getCharBoxes() != null) {
            try {
                result.put("charBoxes", JSON.parseArray(task.getCharBoxes()));
            } catch (Exception e) {
                result.put("charBoxes", task.getCharBoxes());
            }
        }
        
        if (task.getResultJson() != null) {
            JSONObject extractResultJson = JSON.parseObject(task.getResultJson());
            // 将整个结果JSON合并到返回结果中
            if (extractResultJson.containsKey("extractResults")) {
                result.put("extractResults", extractResultJson.get("extractResults"));
            }
            if (extractResultJson.containsKey("totalPages")) {
                result.put("totalPages", extractResultJson.getInteger("totalPages"));
            }
            if (extractResultJson.containsKey("ocrText")) {
                result.put("ocrText", extractResultJson.getString("ocrText"));
            }
            if (extractResultJson.containsKey("pageImagePaths")) {
                result.put("pageImagePaths", extractResultJson.get("pageImagePaths"));
            }
        }
        
        log.info("返回任务结果: taskId={}, totalPages={}, extractResults={}", 
            taskId, result.get("totalPages"), result.containsKey("extractResults"));

        return result;
    }

    /**
     * 取消任务
     */
    public void cancelTask(String taskId) {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
            throw new IllegalStateException("任务已结束，无法取消");
        }

        task.setStatus("cancelled");
        task.setMessage("任务已取消");
        storage.save("task", taskId, task);
        
        log.info("取消任务: taskId={}", taskId);
    }

    /**
     * 查询任务列表
     */
    public List<RuleExtractTaskModel> listTasks(String templateId, String status) {
        List<RuleExtractTaskModel> tasks = storage.list("task", RuleExtractTaskModel.class);
        
        return tasks.stream()
            .filter(t -> templateId == null || templateId.equals(t.getTemplateId()))
            .filter(t -> status == null || status.equals(t.getStatus()))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(100) // 限制返回数量
            .collect(Collectors.toList());
    }

    /**
     * 获取页面图片
     */
    public Resource getPageImage(String taskId, int pageNumber) throws Exception {
        RuleExtractTaskModel task = storage.load("task", taskId, RuleExtractTaskModel.class);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        // 首先尝试从resultJson中获取pageImagePaths
        if (task.getResultJson() != null) {
            try {
                JSONObject resultJson = JSON.parseObject(task.getResultJson());
                if (resultJson.containsKey("pageImagePaths")) {
                    List<String> pageImagePaths = resultJson.getList("pageImagePaths", String.class);
                    if (pageImagePaths != null && pageNumber > 0 && pageNumber <= pageImagePaths.size()) {
                        String imagePath = pageImagePaths.get(pageNumber - 1);
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            log.debug("从resultJson找到页面图片: {}", imagePath);
                            Path path = Paths.get(imagePath);
                            Resource resource = new UrlResource(path.toUri());
                            if (resource.exists() && resource.isReadable()) {
                                return resource;
                            }
                        }
                    }
                }
                
                // 如果有imagesDir，尝试从该目录查找
                if (resultJson.containsKey("imagesDir")) {
                    String imagesDir = resultJson.getString("imagesDir");
                    File imageFile = findImageInDir(new File(imagesDir), pageNumber);
                    if (imageFile != null && imageFile.exists()) {
                        log.debug("从imagesDir找到页面图片: {}", imageFile.getAbsolutePath());
                        Path path = Paths.get(imageFile.getAbsolutePath());
                        Resource resource = new UrlResource(path.toUri());
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从resultJson获取图片失败: {}", e.getMessage());
            }
        }

        // 回退方案：从OCR输出目录查找
        File ocrOutputDir = new File(storage.getDataRoot(), "ocr-output");
        
        // 尝试多种可能的图片路径
        List<File> possibleDirs = new ArrayList<>();
        
        // 1. 如果有ocrResultPath，从中推断目录
        if (task.getOcrResultPath() != null) {
            File ocrFile = new File(task.getOcrResultPath());
            File ocrDir = ocrFile.getParentFile();
            possibleDirs.add(new File(ocrDir, "images/extract"));
            possibleDirs.add(new File(ocrDir, "images"));
            possibleDirs.add(ocrDir);
        }
        
        // 2. 扫描ocr-output目录下的所有任务目录
        if (ocrOutputDir.exists()) {
            File[] taskDirs = ocrOutputDir.listFiles(File::isDirectory);
            if (taskDirs != null) {
                for (File taskDir : taskDirs) {
                    possibleDirs.add(new File(taskDir, "images/extract"));
                    possibleDirs.add(new File(taskDir, "images"));
                }
            }
        }

        for (File dir : possibleDirs) {
            File imageFile = findImageInDir(dir, pageNumber);
            if (imageFile != null && imageFile.exists()) {
                log.debug("找到页面图片: {}", imageFile.getAbsolutePath());
                Path path = Paths.get(imageFile.getAbsolutePath());
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            }
        }

        throw new IllegalStateException("页面图片不存在: taskId=" + taskId + ", page=" + pageNumber);
    }
    
    /**
     * 在指定目录中查找页面图片
     */
    private File findImageInDir(File dir, int pageNumber) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        // 尝试多种可能的文件名格式
        String[] possibleNames = {
            "page-" + pageNumber + ".png",
            "page-" + pageNumber + ".jpg",
            "page_" + pageNumber + ".png",
            "page_" + pageNumber + ".jpg",
            String.format("page-%03d.png", pageNumber),
            String.format("page-%03d.jpg", pageNumber)
        };
        
        for (String name : possibleNames) {
            File imageFile = new File(dir, name);
            if (imageFile.exists()) {
                return imageFile;
            }
        }
        
        return null;
    }
    
    // 栏位验证功能已移除
}

