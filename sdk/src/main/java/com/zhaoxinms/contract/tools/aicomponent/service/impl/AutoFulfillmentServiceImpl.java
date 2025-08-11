package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentService;
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

            String finalPrompt = buildPrompt(prompt, templateId);
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

    private String buildPrompt(String prompt, Long templateId) {
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
        sb.append("请从文档中识别并输出以下履约信息（仅输出这些键）：");
        for (String f : fields) sb.append("\n- ").append(f);
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
}


