package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractService;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleLoaderService;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleStoreService;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractTemplateService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.FileCreateParams;
import com.openai.models.FileObject;
import com.openai.models.FilePurpose;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 合同信息提取服务实现类
 * 基于Test.java实现，使用阿里云通义千问API
 * @author zhaoxinms
 */
@Slf4j
@Service
public class ContractExtractServiceImpl implements ContractExtractService {

    @Autowired
    private AiProperties aiProperties;
    
    @Autowired
    @Qualifier("contractExtractTemplateServiceImpl")
    private ContractExtractTemplateService templateService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // RuleLoaderService will be used in next step to load prompt rules; suppress unused for now
    @Autowired
    @SuppressWarnings("unused")
    private RuleLoaderService ruleLoaderService;

    @Autowired
    private RuleStoreService ruleStoreService;

    // RuleEngineService removed: now AI does the normalization/compute via prompt
    
    private OpenAIClient createClient() {
        // 从配置中随机选择一个API密钥
        String apiKey = aiProperties.getApiKey().get(new Random().nextInt(aiProperties.getApiKey().size()));
        
        // 创建客户端
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(aiProperties.getApiHost())
                .build();
    }

    @Override
    public String uploadFile(Path filePath) {
        try {
            log.info("开始上传文件: {}", filePath);
            
            // 创建客户端
            OpenAIClient client = createClient();
            
            // 创建文件上传参数
            FileCreateParams params = FileCreateParams.builder()
                    .file(filePath)
                    .purpose(FilePurpose.of("file-extract"))
                    .build();
            
            // 上传文件
            FileObject fileObject = client.files().create(params);
            log.info("文件上传成功，文件ID: {}", fileObject.id());
            
            return fileObject.id();
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String extractInfo(String fileId, String prompt) {
        return extractInfo(fileId, prompt, null);
    }
    
    /**
     * 使用指定模板提取文件信息
     * 
     * @param fileId 文件ID
     * @param prompt 提示词（可选）
     * @param templateId 模板ID（可选）
     * @return 提取的信息
     */
    @Override
    public String extractInfo(String fileId, String prompt, Long templateId) {
        try {
            log.info("开始提取文件信息，文件ID: {}, 模板ID: {}", fileId, templateId);
            
            // 创建客户端
            OpenAIClient client = createClient();
            
            // 构建提示词
            String finalPrompt = buildPrompt(prompt, templateId);
            
            // 创建聊天请求
            ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个专业的合同分析助手，擅长从文档中提取关键信息。请以JSON格式返回提取结果，确保输出可以被解析为有效的JSON。")
                    .addSystemMessage("fileid://" + fileId)
                    .addUserMessage(finalPrompt)
                    .model(aiProperties.getChat().getMode())
                    .build();
            
            StringBuilder fullResponse = new StringBuilder();
            
            // 使用流式输出
            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(chatParams)) {
                streamResponse.stream().forEach(chunk -> {
                    String content = chunk.choices().get(0).delta().content().orElse("");
                    if (!content.isEmpty()) {
                        fullResponse.append(content);
                    }
                });
            }
            
            log.info("文件信息提取完成");
            return fullResponse.toString();
        } catch (Exception e) {
            log.error("文件信息提取失败", e);
            throw new RuntimeException("文件信息提取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String processFile(Path filePath, String prompt) {
        return processFile(filePath, prompt, null);
    }
    
    /**
     * 使用指定模板处理文件
     * 
     * @param filePath 文件路径
     * @param prompt 提示词（可选）
     * @param templateId 模板ID（可选）
     * @return 提取的信息
     */
    @Override
    public String processFile(Path filePath, String prompt, Long templateId) {
        // 上传文件
        String fileId = uploadFile(filePath);
        
        // 提取信息
        String raw = extractInfo(fileId, prompt, templateId);
        try {
            // parse model output to map
            String jsonCandidate = sanitizeModelJson(raw);
            Map<String, Object> resultMap = objectMapper.readValue(jsonCandidate, new TypeReference<Map<String, Object>>(){});

            // Keep template/contract type resolution if needed for downstream processing
            @SuppressWarnings("unused")
            String contractType = null;
            if (templateId != null) {
                Optional<ContractExtractTemplate> templateOpt = templateService.getTemplateById(templateId);
                if (templateOpt.isPresent()) {
                    contractType = templateOpt.get().getContractType();
                }
            }

            // No local rule processing; return sanitized JSON from model
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
        } catch (Exception ex) {
            log.warn("Failed to normalize/validate extracted result, fallback to raw", ex);
        }
        return raw;
    }

    /**
     * Models sometimes wrap JSON in fenced code blocks like ```json ... ```.
     * This method extracts the inner JSON text if found; otherwise returns the original string.
     */
    private String sanitizeModelJson(String raw) {
        if (raw == null) { return null; }
        String s = raw.trim();
        int start = s.indexOf("```");
        if (start >= 0) {
            int end = s.indexOf("```", start + 3);
            if (end > start) {
                String inside = s.substring(start + 3, end);
                // remove optional language hint like 'json' at the beginning
                inside = inside.replaceFirst("^\\s*[a-zA-Z]+\\s*", "");
                return inside.trim();
            }
            // remove leading fence if only one found
            s = s.substring(start + 3).trim();
            s = s.replaceFirst("^json\\s*", "");
            return s;
        }
        // Also handle the case where language hint appears without fences (rare)
        if (s.startsWith("json\n") || s.startsWith("json\r\n")) {
            return s.substring(5).trim();
        }
        return s;
    }
    
    /**
     * 构建提取提示词
     * 
     * @param prompt 用户提供的提示词
     * @param templateId 模板ID
     * @return 最终提示词
     */
    private String buildPrompt(String prompt, Long templateId) {
        // 如果没有提供模板ID，沿用传入提示词或默认提示
        if (templateId == null) {
            if (prompt == null || prompt.trim().isEmpty()) {
                return "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。请以JSON格式返回结果。";
            }
            return prompt + " 请以JSON格式返回结果。";
        }

        try {
            // 获取模板
            Optional<ContractExtractTemplate> templateOpt = templateService.getTemplateById(templateId);
            if (!templateOpt.isPresent()) {
                log.warn("模板不存在，ID: {}", templateId);
                return prompt != null && !prompt.trim().isEmpty() ? 
                       prompt + " 请以JSON格式返回结果。" : 
                       "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。请以JSON格式返回结果。";
            }
            
            ContractExtractTemplate template = templateOpt.get();
            
            // 解析模板字段
            List<String> dbFields = objectMapper.readValue(template.getFields(), new TypeReference<List<String>>() {});

            // 新逻辑：优先按模板ID加载规则
            String contractType = template.getContractType(); // reserved for future use
            log.debug("Building prompt by templateId={}, contractType={}", templateId, contractType);
            StringBuilder promptBuilder = new StringBuilder();

            var rulesOpt = ruleStoreService.readRuleByTemplateId(templateId);
            if (rulesOpt.isPresent() && rulesOpt.get().has("prompt")) {
                var ps = objectMapper.convertValue(rulesOpt.get().get("prompt"), com.zhaoxinms.contract.tools.aicomponent.rules.PromptSpec.class);

                // 全局约束
                if (ps.getGlobal() != null && !ps.getGlobal().isEmpty()) {
                    for (String line : ps.getGlobal()) {
                        promptBuilder.append(line).append("\n");
                    }
                }

                // 合并字段清单（DB 字段 + prompt.fields 的键）
                java.util.LinkedHashSet<String> finalFields = new java.util.LinkedHashSet<>(dbFields);
                if (ps.getFields() != null && !ps.getFields().isEmpty()) {
                    finalFields.addAll(ps.getFields().keySet());
                }

                promptBuilder.append("请从文档中提取以下信息（仅输出这些键）：");
                for (String f : finalFields) {
                    promptBuilder.append("\n- ").append(f);
                }

                // 字段级规则
                if (ps.getFields() != null && !ps.getFields().isEmpty()) {
                    promptBuilder.append("\n\n字段规则：");
                    for (var entry : ps.getFields().entrySet()) {
                        String fname = entry.getKey();
                        List<String> rules = entry.getValue();
                        if (rules == null || rules.isEmpty()) continue;
                        promptBuilder.append("\n- ").append(fname).append("：");
                        for (String r : rules) {
                            promptBuilder.append("\n  • ").append(r);
                        }
                    }
                }

                // 负面约束
                if (ps.getNegative() != null && !ps.getNegative().isEmpty()) {
                    promptBuilder.append("\n\n禁止项：");
                    for (String line : ps.getNegative()) {
                        promptBuilder.append("\n- ").append(line);
                    }
                }

                // 输出格式
                if (ps.getFormat() != null && !ps.getFormat().isEmpty()) {
                    promptBuilder.append("\n\n输出格式要求：");
                    for (String line : ps.getFormat()) {
                        promptBuilder.append("\n- ").append(line);
                    }
                }

                // 用户额外提示
                if (StringUtils.hasText(prompt)) {
                    promptBuilder.append("\n\n用户额外要求：").append(prompt);
                }
                return promptBuilder.toString();
            }

            // 若无 prompt 配置，退化为旧逻辑
            StringBuilder fallback = new StringBuilder();
            fallback.append("请从文档中提取以下信息：");
            for (String field : dbFields) {
                fallback.append("\n- ").append(field);
            }
            fallback.append("\n\n请以JSON格式返回结果，确保每个字段都有对应的键值对。如果无法提取某个字段的信息，请将其值设为null。");
            if (StringUtils.hasText(prompt)) {
                fallback.append("\n\n用户额外要求：").append(prompt);
            }
            return fallback.toString();
        } catch (JsonProcessingException e) {
            log.error("解析模板字段失败", e);
            return prompt != null && !prompt.trim().isEmpty() ? 
                   prompt + " 请以JSON格式返回结果。" : 
                   "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。请以JSON格式返回结果。";
        }
    }
}