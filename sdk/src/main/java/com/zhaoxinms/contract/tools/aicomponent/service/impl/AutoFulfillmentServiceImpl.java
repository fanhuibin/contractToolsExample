package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentService;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleStoreService;
import com.zhaoxinms.contract.tools.aicomponent.rules.PromptSpec;
import com.zhaoxinms.contract.tools.aicomponent.mapper.AutoFulfillmentTaskTypeMapper;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentTemplateService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.FileCreateParams;
import com.openai.models.FileObject;
import com.openai.models.FilePurpose;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoFulfillmentServiceImpl implements AutoFulfillmentService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final AutoFulfillmentTemplateService templateService;
    private final RuleStoreService ruleStoreService;
    private final AutoFulfillmentTaskTypeMapper taskTypeMapper;

    private OpenAIClient createClient() {
        String apiKey = aiProperties.getApiKey().get(new Random().nextInt(aiProperties.getApiKey().size()));
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(aiProperties.getApiHost())
                .build();
    }

    @Override
    public String processFile(Path filePath, String prompt, Long templateId) {
        try {
            log.info("自动履约任务识别: {} templateId={}", filePath, templateId);
            OpenAIClient client = createClient();

            FileCreateParams params = FileCreateParams.builder()
                    .file(filePath)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();
            FileObject fileObject = client.files().create(params);

            String finalPrompt = buildPrompt(prompt, templateId, null, null);
            ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个合同履约任务识别助手，请输出JSON。")
                    .addSystemMessage("fileid://" + fileObject.id())
                    .addUserMessage(finalPrompt)
                    .model(aiProperties.getChat().getMode())
                    .build();

            StringBuilder fullResponse = new StringBuilder();
            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(chatParams)) {
                streamResponse.stream().forEach(chunk -> {
                    String content = chunk.choices().get(0).delta().content().orElse("");
                    if (!content.isEmpty()) fullResponse.append(content);
                });
            }

            // 尝试标准化JSON
            String raw = fullResponse.toString();
            try {
                String jsonCandidate = sanitizeModelJson(raw);
                Map<String, Object> resultMap = objectMapper.readValue(jsonCandidate, new TypeReference<Map<String, Object>>(){});
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
            } catch (Exception ignore) {}
            return raw;
        } catch (Exception e) {
            throw new RuntimeException("自动履约任务识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String processFile(Path filePath, String prompt, Long templateId, List<Long> taskTypeIds, List<String> keywords) {
        try {
            log.info("自动履约任务识别: {} templateId={} taskTypes={} keywords={}", filePath, templateId, taskTypeIds, keywords);
            OpenAIClient client = createClient();

            FileCreateParams params = FileCreateParams.builder()
                    .file(filePath)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();
            FileObject fileObject = client.files().create(params);

            String finalPrompt = buildPrompt(prompt, templateId, taskTypeIds, keywords);
            ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个合同履约任务识别助手，请输出JSON。")
                    .addSystemMessage("fileid://" + fileObject.id())
                    .addUserMessage(finalPrompt)
                    .model(aiProperties.getChat().getMode())
                    .build();

            StringBuilder fullResponse = new StringBuilder();
            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(chatParams)) {
                streamResponse.stream().forEach(chunk -> {
                    String content = chunk.choices().get(0).delta().content().orElse("");
                    if (!content.isEmpty()) fullResponse.append(content);
                });
            }

            String raw = fullResponse.toString();
            try {
                String jsonCandidate = sanitizeModelJson(raw);
                Map<String, Object> resultMap = objectMapper.readValue(jsonCandidate, new TypeReference<Map<String, Object>>(){})
                ;
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
            } catch (Exception ignore) {}
            return raw;
        } catch (Exception e) {
            throw new RuntimeException("自动履约任务识别失败: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String prompt, Long templateId, List<Long> taskTypeIds, List<String> keywords) {
        if (templateId == null) {
            return (prompt != null && !prompt.trim().isEmpty())
                    ? prompt + " 请以JSON格式返回结果。"
                    : "请识别并输出合同中涉及的履约任务，输出JSON数组";
        }
        Optional<AutoFulfillmentTemplate> tplOpt = templateService.getTemplateById(templateId);
        if (tplOpt.isEmpty()) {
            return (prompt != null && !prompt.trim().isEmpty())
                    ? prompt + " 请以JSON格式返回结果。"
                    : "请识别并输出合同中涉及的履约任务，输出JSON数组";
        }
        AutoFulfillmentTemplate tpl = tplOpt.get();
        List<String> fields;
        try {
            fields = objectMapper.readValue(tpl.getFields(), new TypeReference<List<String>>(){});
        } catch (Exception e) {
            fields = Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder();

        // 先尝试按模板规则构建（与合同抽取一致）
        try {
            var ruleOpt = ruleStoreService.readRuleByTemplateId(templateId);
            if (ruleOpt.isPresent() && ruleOpt.get().has("prompt")) {
                var ps = objectMapper.convertValue(ruleOpt.get().get("prompt"), PromptSpec.class);
                // 合并字段集合：模板字段 + 规则字段键
                java.util.LinkedHashSet<String> finalFields = new java.util.LinkedHashSet<>(fields);
                if (ps.getFields() != null && !ps.getFields().isEmpty()) finalFields.addAll(ps.getFields().keySet());

                // 附加输出键
                finalFields.add("任务类型");
                finalFields.add("关联关键词");

                if (ps.getGlobal() != null) for (String g : ps.getGlobal()) sb.append(g).append("\n");
                sb.append("请从文档中提取以下信息（仅输出这些键）：");
                for (String f : finalFields) sb.append("\n- ").append(f);
                if (ps.getFields() != null && !ps.getFields().isEmpty()) {
                    sb.append("\n\n字段规则：");
                    for (var e : ps.getFields().entrySet()) {
                        if (e.getValue() == null || e.getValue().isEmpty()) continue;
                        sb.append("\n- ").append(e.getKey()).append("：");
                        for (String r : e.getValue()) sb.append("\n  • ").append(r);
                    }
                }
                if (ps.getNegative() != null && !ps.getNegative().isEmpty()) {
                    sb.append("\n\n禁止项：");
                    for (String n : ps.getNegative()) sb.append("\n- ").append(n);
                }
                if (ps.getFormat() != null && !ps.getFormat().isEmpty()) {
                    sb.append("\n\n输出格式要求：");
                    for (String fm : ps.getFormat()) sb.append("\n- ").append(fm);
                }
            } else {
                // 无规则：基础字段 + 附加输出键
                sb.append("请从文档中识别并输出以下履约信息（仅输出这些键）：");
                for (String f : fields) sb.append("\n- ").append(f);
                sb.append("\n- 任务类型\n- 关联关键词");
            }
        } catch (Exception ex) {
            // 规则异常时降级
            sb.append("请从文档中识别并输出以下履约信息（仅输出这些键）：");
            for (String f : fields) sb.append("\n- ").append(f);
            sb.append("\n- 任务类型\n- 关联关键词");
        }

        // 任务类型与关键词提示
        if (taskTypeIds != null && !taskTypeIds.isEmpty()) {
            try {
                var types = taskTypeMapper.selectBatchIds(taskTypeIds);
                if (types != null && !types.isEmpty()) {
                    sb.append("\n\n识别范围仅聚焦以下任务类型：");
                    for (var t : types) sb.append(t.getName()).append("，");
                    if (sb.charAt(sb.length()-1) == '，') sb.deleteCharAt(sb.length()-1);
                }
            } catch (Exception ignore) {
                sb.append("\n\n识别范围仅聚焦以下任务类型：").append(taskTypeIds.toString());
            }
        }
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("\n\n优先关注关键词：").append(String.join("，", keywords)).append("（非必须命中）");
        }
        if (prompt != null && !prompt.trim().isEmpty()) sb.append("\n\n用户额外要求：").append(prompt);
        return sb.toString();
    }

    private String sanitizeModelJson(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int start = s.indexOf("```");
        if (start >= 0) {
            int end = s.indexOf("```", start + 3);
            if (end > start) {
                String inside = s.substring(start + 3, end);
                inside = inside.replaceFirst("^\\s*[a-zA-Z]+\\s*", "");
                return inside.trim();
            }
            s = s.substring(start + 3).trim();
            s = s.replaceFirst("^json\\s*", "");
            return s;
        }
        if (s.startsWith("json\n") || s.startsWith("json\r\n")) {
            return s.substring(5).trim();
        }
        return s;
    }

    @Override
    public java.util.List<String> processFileBatch(Path filePath, String prompt, List<Long> templateIds, List<Long> taskTypeIds, List<String> keywords) {
        java.util.List<String> results = new java.util.ArrayList<>();
        if (templateIds == null || templateIds.isEmpty()) return results;
        for (Long tid : templateIds) {
            try {
                results.add(processFile(filePath, prompt, tid, taskTypeIds, keywords));
            } catch (Exception e) {
                results.add("{\"templateId\":" + tid + ",\"error\":\"" + e.getMessage().replace("\"","'") + "\"}");
            }
        }
        return results;
    }

    @Override
    public String processFileMerged(Path filePath, String prompt, List<Long> templateIds, List<Long> taskTypeIds, List<String> keywords) {
        try {
            if (templateIds == null || templateIds.isEmpty()) {
                throw new IllegalArgumentException("templateIds is empty for merged processing");
            }

            // 1) 聚合字段与规则
            LinkedHashSet<String> mergedFields = new LinkedHashSet<>();
            Map<String, LinkedHashSet<String>> mergedRulesByField = new LinkedHashMap<>();
            LinkedHashSet<String> mergedGlobal = new LinkedHashSet<>();
            LinkedHashSet<String> mergedNegative = new LinkedHashSet<>();
            LinkedHashSet<String> mergedFormat = new LinkedHashSet<>();

            for (Long tid : templateIds) {
                try {
                    Optional<AutoFulfillmentTemplate> tplOpt = templateService.getTemplateById(tid);
                    if (tplOpt.isPresent()) {
                        AutoFulfillmentTemplate tpl = tplOpt.get();
                        try {
                            List<String> fs = objectMapper.readValue(tpl.getFields(), new TypeReference<List<String>>(){});
                            if (fs != null) mergedFields.addAll(fs);
                        } catch (Exception ignore) {}
                    }
                    var ruleOpt = ruleStoreService.readRuleByTemplateId(tid);
                    if (ruleOpt.isPresent() && ruleOpt.get().has("prompt")) {
                        var ps = objectMapper.convertValue(ruleOpt.get().get("prompt"), PromptSpec.class);
                        if (ps.getFields() != null && !ps.getFields().isEmpty()) {
                            // 规则字段键加入并集
                            mergedFields.addAll(ps.getFields().keySet());
                            // 同名字段规则并集去重
                            for (var e : ps.getFields().entrySet()) {
                                if (e.getValue() == null || e.getValue().isEmpty()) continue;
                                mergedRulesByField.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>()).addAll(e.getValue());
                            }
                        }
                        if (ps.getGlobal() != null) mergedGlobal.addAll(ps.getGlobal());
                        if (ps.getNegative() != null) mergedNegative.addAll(ps.getNegative());
                        if (ps.getFormat() != null) mergedFormat.addAll(ps.getFormat());
                    }
                } catch (Exception ex) {
                    log.warn("merge rules from template {} failed: {}", tid, ex.getMessage());
                }
            }

            // 附加输出键
            mergedFields.add("任务类型");
            mergedFields.add("关联关键词");

            // 2) 构造合并Prompt
            String finalPrompt = buildMergedPrompt(mergedFields, mergedRulesByField, mergedGlobal, mergedNegative, mergedFormat, taskTypeIds, keywords, prompt);

            // 3) 调用一次模型
            OpenAIClient client = createClient();
            FileCreateParams params = FileCreateParams.builder()
                    .file(filePath)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();
            FileObject fileObject = client.files().create(params);

            ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个合同履约任务识别助手，请输出JSON。")
                    .addSystemMessage("fileid://" + fileObject.id())
                    .addUserMessage(finalPrompt)
                    .model(aiProperties.getChat().getMode())
                    .build();

            StringBuilder fullResponse = new StringBuilder();
            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(chatParams)) {
                streamResponse.stream().forEach(chunk -> {
                    String content = chunk.choices().get(0).delta().content().orElse("");
                    if (!content.isEmpty()) fullResponse.append(content);
                });
            }

            String raw = fullResponse.toString();
            try {
                String jsonCandidate = sanitizeModelJson(raw);
                Map<String, Object> resultMap = objectMapper.readValue(jsonCandidate, new TypeReference<Map<String, Object>>(){});
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
            } catch (Exception ignore) {}
            return raw;
        } catch (Exception e) {
            throw new RuntimeException("自动履约任务合并识别失败: " + e.getMessage(), e);
        }
    }

    private String buildMergedPrompt(Set<String> mergedFields,
                                     Map<String, LinkedHashSet<String>> mergedRulesByField,
                                     Set<String> mergedGlobal,
                                     Set<String> mergedNegative,
                                     Set<String> mergedFormat,
                                     List<Long> taskTypeIds,
                                     List<String> keywords,
                                     String userPrompt) {
        StringBuilder sb = new StringBuilder();
        if (mergedGlobal != null && !mergedGlobal.isEmpty()) {
            for (String g : mergedGlobal) sb.append(g).append("\n");
        }
        sb.append("请从文档中提取以下信息（仅输出这些键）：");
        for (String f : mergedFields) sb.append("\n- ").append(f);
        if (mergedRulesByField != null && !mergedRulesByField.isEmpty()) {
            sb.append("\n\n字段规则：");
            for (var e : mergedRulesByField.entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) continue;
                sb.append("\n- ").append(e.getKey()).append("：");
                for (String r : e.getValue()) sb.append("\n  • ").append(r);
            }
        }
        if (mergedNegative != null && !mergedNegative.isEmpty()) {
            sb.append("\n\n禁止项：");
            for (String n : mergedNegative) sb.append("\n- ").append(n);
        }
        if (mergedFormat != null && !mergedFormat.isEmpty()) {
            sb.append("\n\n输出格式要求：");
            for (String fm : mergedFormat) sb.append("\n- ").append(fm);
        }
        // 同字段多个候选值的输出方式
        sb.append("\n\n当同一字段存在多个候选值时，请将该字段输出为数组；若仅有一个值则输出单值。请输出一个JSON对象。");

        // 任务类型与关键词提示
        if (taskTypeIds != null && !taskTypeIds.isEmpty()) {
            try {
                var types = taskTypeMapper.selectBatchIds(taskTypeIds);
                if (types != null && !types.isEmpty()) {
                    sb.append("\n\n识别范围仅聚焦以下任务类型：");
                    for (var t : types) sb.append(t.getName()).append("，");
                    if (sb.charAt(sb.length()-1) == '，') sb.deleteCharAt(sb.length()-1);
                }
            } catch (Exception ignore) {
                sb.append("\n\n识别范围仅聚焦以下任务类型：").append(taskTypeIds.toString());
            }
        }
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("\n\n优先关注关键词：").append(String.join("，", keywords)).append("（非必须命中）");
        }
        if (userPrompt != null && !userPrompt.trim().isEmpty()) sb.append("\n\n用户额外要求：").append(userPrompt);
        return sb.toString();
    }
}


