package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractService;
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
import org.springframework.stereotype.Service;

import java.nio.file.Path;
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
        try {
            log.info("开始提取文件信息，文件ID: {}", fileId);
            
            // 创建客户端
            OpenAIClient client = createClient();
            
            // 如果没有提供提示，使用默认提示
            if (prompt == null || prompt.trim().isEmpty()) {
                prompt = "请提取这份文件中的关键信息，包括合同名称、合同双方、合同金额、签订日期、合同期限等。";
            }
            
            // 创建聊天请求
            ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个专业的合同分析助手，擅长从文档中提取关键信息。")
                    .addSystemMessage("fileid://" + fileId)
                    .addUserMessage(prompt)
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
        // 上传文件
        String fileId = uploadFile(filePath);
        
        // 提取信息
        return extractInfo(fileId, prompt);
    }
}