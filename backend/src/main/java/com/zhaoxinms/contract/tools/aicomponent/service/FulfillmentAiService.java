package com.zhaoxinms.contract.tools.aicomponent.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentExtractResult;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 履约任务AI识别服务
 * 使用通义千问Long模型进行智能识别
 */
@Service
@Slf4j
public class FulfillmentAiService {

    @Value("${ai.fulfillment.prompt.template}")
    private String promptTemplate;

    @Autowired
    private FulfillmentTemplateService templateService;

    /**
     * 从合同文件中提取履约任务
     * @param file 合同文件
     * @param templateId 模板ID
     * @return 履约任务列表
     */
    public List<FulfillmentExtractResult> extractFulfillmentTasks(MultipartFile file, Long templateId) {
        try {
            // 读取文件内容
            String contractText = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 获取模板
            FulfillmentTemplate template = templateId != null 
                ? templateService.getById(templateId) 
                : null;

            // 构建提示词
            String prompt = buildPrompt(contractText, template);

            // 调用通义千问模型
            List<FulfillmentExtractResult> tasks = callTongYiQianwenLong(prompt);

            // 日志记录
            log.info("提取履约任务成功：文件={}, 模板={}, 任务数={}", 
                file.getOriginalFilename(), 
                template != null ? template.getName() : "无", 
                tasks.size()
            );

            return tasks;
        } catch (IOException e) {
            log.error("提取履约任务失败：文件读取错误", e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /**
     * 构建AI提示词
     * @param contractText 合同文本
     * @param template 模板（可选）
     * @return 构建后的提示词
     */
    private String buildPrompt(String contractText, FulfillmentTemplate template) {
        // 基础提示词模板
        String basePrompt = StrUtil.isBlank(promptTemplate) 
            ? defaultPromptTemplate() 
            : promptTemplate;

        // 如果有模板，根据模板定制提示词
        if (template != null) {
            basePrompt += "\n\n特定模板要求：\n";
            
            if (!template.getTaskTypes().isEmpty()) {
                basePrompt += "- 任务类型限定：" + 
                    template.getTaskTypes().stream()
                        .collect(Collectors.joining("、")) + "\n";
            }
            
            if (!template.getKeywords().isEmpty()) {
                basePrompt += "- 关键词：" + 
                    template.getKeywords().stream()
                        .collect(Collectors.joining("、")) + "\n";
            }
            
            if (!template.getTimeRules().isEmpty()) {
                basePrompt += "- 时间规则：" + 
                    template.getTimeRules().stream()
                        .collect(Collectors.joining("、")) + "\n";
            }
        }

        // 替换合同文本
        basePrompt = basePrompt.replace("${CONTRACT_TEXT}", contractText);

        return basePrompt;
    }

    /**
     * 默认提示词模板
     * @return 默认提示词
     */
    private String defaultPromptTemplate() {
        return "你是一个专业的合同履约任务生成助手。请从以下合同文本中智能识别和生成履约任务：\n\n" +
            "${CONTRACT_TEXT}\n\n" +
            "请按照以下规则提取履约任务：\n" +
            "1. 识别合同中的关键履约事件\n" +
            "2. 提取每个任务的具体时间和方式\n" +
            "3. 输出结构化的JSON数据\n\n" +
            "JSON格式要求：\n" +
            "[\n" +
            "  {\n" +
            "    \"contractName\": \"原始合同名称\",\n" +
            "    \"fulfillmentName\": \"履约任务名称\",\n" +
            "    \"dueDate\": \"完成日期(YYYY-MM-DD)\",\n" +
            "    \"fulfillmentMethod\": \"履约具体方式\"\n" +
            "  }\n" +
            "]";
    }

    /**
     * 调用通义千问Long模型
     * @param prompt 提示词
     * @return 履约任务列表
     */
    private List<FulfillmentExtractResult> callTongYiQianwenLong(String prompt) {
        try {
            // 初始化通义千问生成服务
            Generation gen = new Generation();

            // 构建消息
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                .role(Role.SYSTEM)
                .content("你是一个专业的合同履约任务智能识别助手。")
                .build());
            messages.add(Message.builder()
                .role(Role.USER)
                .content(prompt)
                .build());

            // 构建生成参数
            GenerationParam param = GenerationParam.builder()
                .model("qwen-long")
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .temperature(0.7)
                .build();

            // 调用模型
            GenerationResult result = gen.call(param);

            // 解析结果
            String jsonResult = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            // 尝试解析JSON
            List<FulfillmentExtractResult> tasks = parseTasksFromJson(jsonResult);

            log.info("通义千问Long模型识别完成，识别任务数：{}", tasks.size());
            return tasks;

        } catch (Exception e) {
            log.error("调用通义千问Long模型失败", e);
            throw new RuntimeException("AI识别失败", e);
        }
    }

    /**
     * 解析JSON格式的任务列表
     * @param jsonResult JSON字符串
     * @return 履约任务列表
     */
    private List<FulfillmentExtractResult> parseTasksFromJson(String jsonResult) {
        try {
            // 尝试解析JSON
            List<FulfillmentExtractResult> tasks = JSONUtil.toList(
                JSONUtil.parseArray(jsonResult), 
                FulfillmentExtractResult.class
            );

            // 如果解析失败或为空，返回空列表
            return tasks != null && !tasks.isEmpty() ? tasks : new ArrayList<>();
        } catch (Exception e) {
            log.warn("JSON解析失败，尝试从文本中提取：{}", jsonResult);
            // 如果JSON解析失败，可以添加更复杂的文本解析逻辑
            return new ArrayList<>();
        }
    }
}
