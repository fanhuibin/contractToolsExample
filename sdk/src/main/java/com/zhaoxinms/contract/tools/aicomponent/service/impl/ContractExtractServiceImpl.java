package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractService;
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
        return extractInfo(fileId, prompt, templateId);
    }
    
    /**
     * 构建提取提示词
     * 
     * @param prompt 用户提供的提示词
     * @param templateId 模板ID
     * @return 最终提示词
     */
    private String buildPrompt(String prompt, Long templateId) {
        // 如果没有提供模板ID和提示词，使用默认提示
        if (templateId == null && (prompt == null || prompt.trim().isEmpty())) {
            return "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。请以JSON格式返回结果。";
        }
        
        // 如果提供了提示词但没有模板ID，直接使用提示词
        if (templateId == null) {
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
            List<String> fields = objectMapper.readValue(template.getFields(), new TypeReference<List<String>>() {});
            
            // 构建提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请从文档中提取以下信息：");
            
            for (String field : fields) {
                promptBuilder.append("\n- ").append(field);
            }
            
            promptBuilder.append("\n\n请以JSON格式返回结果，确保每个字段都有对应的键值对。如果无法提取某个字段的信息，请将其值设为null。");
            
            // 如果用户提供了额外的提示，添加到最后
            if (StringUtils.hasText(prompt)) {
                promptBuilder.append("\n\n用户额外要求：").append(prompt);
            }
            
            return promptBuilder.toString();
        } catch (JsonProcessingException e) {
            log.error("解析模板字段失败", e);
            return prompt != null && !prompt.trim().isEmpty() ? 
                   prompt + " 请以JSON格式返回结果。" : 
                   "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。请以JSON格式返回结果。";
        }
    }
}