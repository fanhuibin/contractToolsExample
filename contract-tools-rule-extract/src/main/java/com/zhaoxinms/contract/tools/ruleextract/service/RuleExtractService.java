package com.zhaoxinms.contract.tools.ruleextract.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
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

            // 调用OCR服务（通过反射支持页眉页脚设置，避免循环依赖）
            OCRProvider.OCRResult ocrResult;
            try {
                // 尝试调用带页眉页脚参数的方法（如果OCR服务支持）
                java.lang.reflect.Method method = ocrProvider.getClass().getMethod(
                    "recognizePdf", 
                    File.class, boolean.class, double.class, double.class
                );
                ocrResult = (OCRProvider.OCRResult) method.invoke(
                    ocrProvider,
                    pdfFile,
                    task.getIgnoreHeaderFooter() != null ? task.getIgnoreHeaderFooter() : true,
                    task.getHeaderHeightPercent() != null ? task.getHeaderHeightPercent() : 12.0,
                    task.getFooterHeightPercent() != null ? task.getFooterHeightPercent() : 12.0
                );
                log.info("使用页眉页脚参数调用OCR服务成功");
            } catch (NoSuchMethodException e) {
                // 如果不支持，使用默认方法
                log.info("OCR服务不支持页眉页脚参数，使用默认方法");
                ocrResult = ocrProvider.recognizePdf(pdfFile);
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
        String charBoxesJson = null;
        
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
                    log.info("Metadata charBoxes字段存在: {}", metaJson.containsKey("charBoxes"));
                    log.info("Metadata pageDimensions字段存在: {}", metaJson.containsKey("pageDimensions"));
                    
                    if (metaJson.containsKey("totalPages")) {
                        resultJson.put("totalPages", metaJson.getInteger("totalPages"));
                        log.info("保存totalPages: {}", metaJson.getInteger("totalPages"));
                    }
                    if (metaJson.containsKey("pageImagePaths")) {
                        resultJson.put("pageImagePaths", metaJson.get("pageImagePaths"));
                    }
                    if (metaJson.containsKey("charBoxes")) {
                        // 保存CharBox数据到task
                        charBoxesJson = metaJson.getString("charBoxes");
                        task.setCharBoxes(charBoxesJson);
                        log.info("保存CharBox数据，长度: {}", charBoxesJson.length());
                    } else {
                        log.warn("Metadata中没有charBoxes字段");
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
        
        // 生成BboxMappings（从提取结果和CharBox数据中）
        try {
            List<JSONObject> bboxMappings = generateBboxMappings(results, ocrResult, charBoxesJson);
            if (!bboxMappings.isEmpty()) {
                task.setBboxMappings(JSON.toJSONString(bboxMappings));
                log.info("成功生成 {} 个BboxMapping", bboxMappings.size());
            }
        } catch (Exception e) {
            log.warn("生成BboxMappings失败: {}", e.getMessage(), e);
        }

        task.setResultJson(resultJson.toJSONString());
        storage.save("task", taskId, task);
    }
    
    /**
     * 从提取结果生成BboxMappings（使用CharBox数据）
     */
    private List<JSONObject> generateBboxMappings(List<JSONObject> results, OCRProvider.OCRResult ocrResult, String charBoxesJson) {
        List<JSONObject> bboxMappings = new ArrayList<>();
        
        if (ocrResult == null || ocrResult.getContent() == null) {
            return bboxMappings;
        }
        
        String fullText = ocrResult.getContent();
        
        // 解析CharBox数据
        List<CharBoxData> charBoxes = null;
        if (charBoxesJson != null && !charBoxesJson.isEmpty()) {
            try {
                log.info("开始解析CharBox数据，字符串长度: {}", charBoxesJson.length());
                com.alibaba.fastjson2.JSONArray charBoxArray = JSON.parseArray(charBoxesJson);
                charBoxes = new ArrayList<>();
                for (int i = 0; i < charBoxArray.size(); i++) {
                    com.alibaba.fastjson2.JSONObject charBoxObj = charBoxArray.getJSONObject(i);
                    CharBoxData charBox = new CharBoxData();
                    charBox.page = charBoxObj.getInteger("page");
                    charBox.ch = charBoxObj.getString("ch");
                    com.alibaba.fastjson2.JSONArray bboxArray = charBoxObj.getJSONArray("bbox");
                    if (bboxArray != null && bboxArray.size() == 4) {
                        charBox.bbox = new double[]{
                            bboxArray.getDoubleValue(0),
                            bboxArray.getDoubleValue(1),
                            bboxArray.getDoubleValue(2),
                            bboxArray.getDoubleValue(3)
                        };
                    }
                    charBoxes.add(charBox);
                }
                log.info("成功解析 {} 个CharBox数据", charBoxes.size());
            } catch (Exception e) {
                log.error("解析CharBox数据失败: {}", e.getMessage(), e);
            }
        } else {
            log.warn("CharBox数据为空，无法生成bbox坐标");
        }
        
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
                    
                    // 从CharBox数据中提取bbox坐标
                    if (charBoxes != null && !charBoxes.isEmpty()) {
                        List<JSONObject> bboxes = extractBboxesFromCharBoxes(charBoxes, startPos, endPos);
                        mapping.put("bboxes", bboxes);
                        
                        // 提取页面列表
                        java.util.Set<Integer> pageSet = new java.util.HashSet<>();
                        for (JSONObject bbox : bboxes) {
                            if (bbox.containsKey("page")) {
                                pageSet.add(bbox.getInteger("page"));
                            }
                        }
                        mapping.put("pages", new ArrayList<>(pageSet));
                    } else {
                        mapping.put("bboxes", new JSONArray());
                        mapping.put("pages", new JSONArray());
                    }
                    
                    bboxMappings.add(mapping);
                }
            }
        }
        
        return bboxMappings;
    }
    
    /**
     * 从CharBox数据中提取指定范围的bbox坐标
     */
    private List<JSONObject> extractBboxesFromCharBoxes(List<CharBoxData> charBoxes, int startPos, int endPos) {
        List<JSONObject> bboxes = new ArrayList<>();
        
        if (startPos >= charBoxes.size() || endPos > charBoxes.size() || startPos >= endPos) {
            return bboxes;
        }
        
        // 合并相邻字符的bbox（按页面分组）
        java.util.Map<Integer, List<double[]>> pageBoxes = new java.util.LinkedHashMap<>();
        
        for (int i = startPos; i < endPos && i < charBoxes.size(); i++) {
            CharBoxData charBox = charBoxes.get(i);
            if (charBox.bbox == null || charBox.page == null) continue;
            
            // 跳过换行符
            if ("\n".equals(charBox.ch)) continue;
            
            // 跳过空bbox（分隔符等）
            if (charBox.bbox[0] == 0 && charBox.bbox[1] == 0 && 
                charBox.bbox[2] == 0 && charBox.bbox[3] == 0) continue;
            
            pageBoxes.computeIfAbsent(charBox.page, k -> new ArrayList<>()).add(charBox.bbox);
        }
        
        // 为每个页面合并bbox
        for (java.util.Map.Entry<Integer, List<double[]>> entry : pageBoxes.entrySet()) {
            Integer page = entry.getKey();
            List<double[]> boxes = entry.getValue();
            
            if (boxes.isEmpty()) continue;
            
            // 计算合并后的bbox
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            
            for (double[] box : boxes) {
                minX = Math.min(minX, box[0]);
                minY = Math.min(minY, box[1]);
                maxX = Math.max(maxX, box[2]);
                maxY = Math.max(maxY, box[3]);
            }
            
            JSONObject bboxInfo = new JSONObject();
            bboxInfo.put("page", page);
            bboxInfo.put("bbox", new double[]{minX, minY, maxX, maxY});
            bboxes.add(bboxInfo);
        }
        
        return bboxes;
    }
    
    /**
     * CharBox数据辅助类
     */
    private static class CharBoxData {
        Integer page;
        String ch;
        double[] bbox;
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

