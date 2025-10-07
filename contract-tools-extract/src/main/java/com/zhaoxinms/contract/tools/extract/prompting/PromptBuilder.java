package com.zhaoxinms.contract.tools.extract.prompting;

import com.zhaoxinms.contract.tools.extract.core.ExtractEngine;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.format.FormatHandler;

import java.util.stream.Collectors;

/**
 * 提示构建器
 * 负责构建用于LLM的提示文本
 */
public class PromptBuilder {
    
    private final FormatHandler formatHandler;
    
    public PromptBuilder() {
        this.formatHandler = new FormatHandler();
    }
    
    /**
     * 构建系统提示
     */
    public String buildSystemPrompt(ExtractionSchema schema, ExtractEngine.ExtractionOptions options) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个专业的文本信息提取专家。你的任务是从给定的文本中准确提取指定的结构化信息。\n\n");
        
        prompt.append("提取规则:\n");
        prompt.append("1. 仔细阅读和理解输入文本\n");
        prompt.append("2. 根据字段定义准确提取信息\n");
        prompt.append("3. 如果某个字段在文本中找不到对应信息，返回null\n");
        prompt.append("4. 保持提取结果的准确性和一致性\n");
        prompt.append("5. 严格按照指定的数据类型返回结果\n\n");
        
        // 添加格式要求
        prompt.append(formatHandler.generateFormatPrompt(options.getFormat()));
        
        return prompt.toString();
    }
    
    /**
     * 构建提取提示
     */
    public String buildExtractionPrompt(Document document, ExtractionSchema schema, ExtractEngine.ExtractionOptions options) {
        StringBuilder prompt = new StringBuilder();
        
        // 添加模式描述
        prompt.append("## 提取任务\n");
        if (schema.getDescription() != null) {
            prompt.append("任务描述: ").append(schema.getDescription()).append("\n\n");
        }
        
        // 添加字段定义
        prompt.append("## 需要提取的字段\n");
        for (ExtractionSchema.FieldDefinition field : schema.getFields()) {
            prompt.append("**").append(field.getName()).append("**");
            if (field.isRequired()) {
                prompt.append(" (必需)");
            }
            prompt.append("\n");
            
            if (field.getDescription() != null) {
                prompt.append("- 描述: ").append(field.getDescription()).append("\n");
            }
            
            prompt.append("- 类型: ").append(field.getType().getValue()).append("\n");
            
            if (field.getHint() != null) {
                prompt.append("- 提示: ").append(field.getHint()).append("\n");
            }
            
            if (!field.getExamples().isEmpty()) {
                prompt.append("- 示例: ");
                prompt.append(field.getExamples().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
                prompt.append("\n");
            }
            
            if (field.getDefaultValue() != null) {
                prompt.append("- 默认值: ").append(field.getDefaultValue()).append("\n");
            }
            
            prompt.append("\n");
        }
        
        // 添加文档内容
        prompt.append("## 待处理文本\n");
        prompt.append("```\n");
        prompt.append(document.getContent());
        prompt.append("\n```\n\n");
        
        // 重要：添加原文引用要求（LangExtract的核心特性）
        prompt.append("## 重要要求\n");
        prompt.append("**字符级精确引用**: 对于每个提取的值，请确保该值是从原文中直接引用的确切文本片段。\n");
        prompt.append("- 不要修改、总结或重新表述原文内容\n");
        prompt.append("- 保持原文的完整性和准确性，包括标点符号和格式\n");
        prompt.append("- 如果原文中没有找到相关信息，请返回null\n");
        prompt.append("- 对于数字，保持原文格式（如：\"50万元\" 而不是 \"500000\"）\n");
        prompt.append("- 对于日期，保持原文格式（如：\"2024年1月15日\" 而不是 \"2024-01-15\"）\n\n");
        
        // 添加输出要求
        prompt.append("## 输出要求\n");
        prompt.append("请按照以下格式返回提取结果，确保每个值都是原文的直接引用:\n\n");
        
        // 生成示例输出格式
        StringBuilder exampleOutput = new StringBuilder();
        if ("json".equals(options.getFormat())) {
            exampleOutput.append("```json\n{\n");
            for (int i = 0; i < schema.getFields().size(); i++) {
                ExtractionSchema.FieldDefinition field = schema.getFields().get(i);
                exampleOutput.append("  \"").append(field.getName()).append("\": ");
                
                // 根据类型生成示例值
                switch (field.getType()) {
                    case STRING:
                    case EMAIL:
                    case URL:
                    case PHONE:
                        exampleOutput.append("\"提取的文本值\"");
                        break;
                    case INTEGER:
                        exampleOutput.append("123");
                        break;
                    case FLOAT:
                    case CURRENCY:
                        exampleOutput.append("123.45");
                        break;
                    case BOOLEAN:
                        exampleOutput.append("true");
                        break;
                    case DATE:
                        exampleOutput.append("\"2024-01-01\"");
                        break;
                    case DATETIME:
                        exampleOutput.append("\"2024-01-01T12:00:00\"");
                        break;
                    case ARRAY:
                        exampleOutput.append("[\"值1\", \"值2\"]");
                        break;
                    case OBJECT:
                        exampleOutput.append("{}");
                        break;
                    default:
                        exampleOutput.append("\"值\"");
                        break;
                }
                
                if (i < schema.getFields().size() - 1) {
                    exampleOutput.append(",");
                }
                exampleOutput.append("\n");
            }
            exampleOutput.append("}\n```");
        } else {
            exampleOutput.append("```yaml\n");
            for (ExtractionSchema.FieldDefinition field : schema.getFields()) {
                exampleOutput.append(field.getName()).append(": ");
                
                switch (field.getType()) {
                    case STRING:
                    case EMAIL:
                    case URL:
                    case PHONE:
                        exampleOutput.append("\"提取的文本值\"");
                        break;
                    case INTEGER:
                        exampleOutput.append("123");
                        break;
                    case FLOAT:
                    case CURRENCY:
                        exampleOutput.append("123.45");
                        break;
                    case BOOLEAN:
                        exampleOutput.append("true");
                        break;
                    case DATE:
                        exampleOutput.append("\"2024-01-01\"");
                        break;
                    case DATETIME:
                        exampleOutput.append("\"2024-01-01T12:00:00\"");
                        break;
                    case ARRAY:
                        exampleOutput.append("\n  - \"值1\"\n  - \"值2\"");
                        break;
                    case OBJECT:
                        exampleOutput.append("{}");
                        break;
                    default:
                        exampleOutput.append("\"值\"");
                        break;
                }
                exampleOutput.append("\n");
            }
            exampleOutput.append("```");
        }
        
        prompt.append(exampleOutput);
        
        return prompt.toString();
    }
    
    /**
     * 构建验证提示
     */
    public String buildValidationPrompt(Document document, ExtractionSchema schema, Object extractedData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请验证以下提取结果是否准确:\n\n");
        
        prompt.append("## 原始文本\n");
        prompt.append("```\n").append(document.getContent()).append("\n```\n\n");
        
        prompt.append("## 提取结果\n");
        prompt.append("```json\n").append(extractedData.toString()).append("\n```\n\n");
        
        prompt.append("请检查:\n");
        prompt.append("1. 提取的信息是否在原始文本中存在\n");
        prompt.append("2. 数据类型是否正确\n");
        prompt.append("3. 必需字段是否都已提取\n");
        prompt.append("4. 提取结果是否符合字段定义\n\n");
        
        prompt.append("如果发现错误，请返回修正后的结果。如果正确，请返回 'VALID'。");
        
        return prompt.toString();
    }
}
