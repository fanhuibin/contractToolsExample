package com.zhaoxinms.contract.tools.ruleextract.constants;

/**
 * AI 提示词模板常量
 * 
 * @author 山西肇新科技有限公司
 */
public class AIPromptTemplates {
    
    /**
     * 基础字段提取模板
     */
    public static final String BASIC_EXTRACTION_TEMPLATE = 
        "# 合同信息抽取模板生成器\n\n" +
        "## 角色\n" +
        "你是一个专业的合同分析专家，擅长识别文档结构并设计数据抽取规则。\n\n" +
        "## 任务\n" +
        "根据用户提供的合同文档和字段列表，生成标准的JSON格式抽取模板。\n\n" +
        "## 需要提取的字段\n" +
        "{FIELD_LIST}\n\n" +
        "## 合同原文\n" +
        "{DOCUMENT_CONTENT}\n\n" +
        "## 输出格式（必须严格遵守）\n" +
        "```json\n" +
        "{\n" +
        "  \"templateName\": \"根据合同类型命名（如：采购合同模板）\",\n" +
        "  \"description\": \"简要说明模板用途\",\n" +
        "  \"fields\": [\n" +
        "    {\n" +
        "      \"fieldName\": \"字段英文名（驼峰命名，如contractNo）\",\n" +
        "      \"fieldLabel\": \"字段中文名（如合同编号）\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"定位关键词（如'合同编号：'）\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 50\n" +
        "      },\n" +
        "      \"confidence\": 85\n" +
        "    }\n" +
        "  ]\n" +
        "}\n" +
        "```\n\n" +
        "## 规则设计原则\n" +
        "1. **keyword**: 选择唯一且稳定的关键词，建议包含冒号或等特殊符号\n" +
        "2. **offset**: 关键词后偏移字符数（通常为0，表示紧跟关键词）\n" +
        "3. **length**: 提取最大长度（宁可设置大一些，如50）\n" +
        "4. **type**: 默认使用 \"keyword\"，最准确\n" +
        "5. **confidence**: 根据关键词唯一性打分，0-100\n\n" +
        "## 常见字段类型的正则表达式参考\n" +
        "- 日期：`\\\\d{4}年\\\\d{1,2}月\\\\d{1,2}日` 或 `\\\\d{4}-\\\\d{2}-\\\\d{2}`\n" +
        "- 金额：`\\\\d+(\\\\.\\\\d{2})?` 或 `¥?\\\\d{1,3}(,\\\\d{3})*(\\\\.\\\\d{2})?`\n" +
        "- 编号：`[A-Z]{2}-\\\\d{4}-\\\\d{3}` （根据实际格式调整）\n" +
        "- 电话：`1[3-9]\\\\d{9}` 或 `\\\\d{3,4}-\\\\d{7,8}`\n\n" +
        "## 重要提示\n" +
        "1. 只输出JSON，不要添加任何解释文字\n" +
        "2. 确保JSON格式完全正确，可以直接解析\n" +
        "3. 如果某个字段在文档中找不到明确位置，设置 required: false 和 confidence: 0\n" +
        "4. 每个字段必须有 extractRules\n" +
        "5. JSON 输出不要包含在 ```json 代码块中，直接输出原始JSON\n";
    
    /**
     * 高级提取模板（包含表格）
     */
    public static final String ADVANCED_EXTRACTION_TEMPLATE = 
        "# 高级合同信息抽取模板生成器（支持表格）\n\n" +
        "## 角色\n" +
        "你是一个专业的合同分析专家，擅长处理复杂文档结构，包括表格数据提取。\n\n" +
        "## 任务\n" +
        "根据用户提供的合同文档和字段列表，生成标准的JSON格式抽取模板。支持表格数据提取。\n\n" +
        "## 需要提取的字段\n" +
        "{FIELD_LIST}\n\n" +
        "## 合同原文\n" +
        "{DOCUMENT_CONTENT}\n\n" +
        "## 输出格式\n" +
        "```json\n" +
        "{\n" +
        "  \"templateName\": \"模板名称\",\n" +
        "  \"description\": \"模板描述\",\n" +
        "  \"fields\": [\n" +
        "    {\n" +
        "      \"fieldName\": \"字段名\",\n" +
        "      \"fieldLabel\": \"显示名\",\n" +
        "      \"fieldType\": \"text|number|date|table\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword|table\",\n" +
        "        \"keyword\": \"定位关键词\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 50,\n" +
        "        \"tableRules\": {\n" +
        "          \"tableKeyword\": \"表格标题关键词\",\n" +
        "          \"columns\": [\"列名1\", \"列名2\"]\n" +
        "        }\n" +
        "      },\n" +
        "      \"confidence\": 85\n" +
        "    }\n" +
        "  ]\n" +
        "}\n" +
        "```\n\n" +
        "## 表格提取说明\n" +
        "- fieldType 设置为 \"table\"\n" +
        "- extractRules.type 设置为 \"table\"\n" +
        "- 使用 tableRules 定义表格提取规则\n" +
        "- tableKeyword: 表格前的标题关键词\n" +
        "- columns: 需要提取的列名列表\n\n" +
        "## 注意事项\n" +
        "1. 只输出JSON，不要添加解释\n" +
        "2. 确保JSON格式正确\n" +
        "3. 表格字段要明确标注 fieldType: \"table\"\n";
    
    /**
     * 模板说明文档
     */
    public static final String TEMPLATE_USAGE_GUIDE = 
        "# AI 模板生成助手使用说明\n\n" +
        "## 操作流程\n\n" +
        "### 步骤1: 上传合同文档\n" +
        "- 支持 PDF、Word 格式\n" +
        "- 系统会自动提取文档文本内容\n" +
        "- 提取后可复制文本内容\n\n" +
        "### 步骤2: 准备 AI 提示词\n" +
        "1. 输入需要提取的字段列表（每行一个）\n" +
        "2. 选择提示词模板（基础/高级）\n" +
        "3. 系统自动生成完整提示词\n" +
        "4. 点击\"复制提示词\"按钮\n\n" +
        "### 步骤3: 使用 AI 工具生成 JSON\n" +
        "1. 打开 AI 工具（ChatGPT、通义千问、文心一言等）\n" +
        "2. 粘贴完整提示词\n" +
        "3. 等待 AI 生成 JSON 配置\n" +
        "4. 复制 AI 返回的 JSON 内容\n\n" +
        "### 步骤4: 导入模板\n" +
        "1. 粘贴 AI 生成的 JSON 到输入框\n" +
        "2. 点击\"验证 JSON 格式\"检查格式\n" +
        "3. 确认无误后点击\"导入模板\"\n" +
        "4. 系统创建模板并进入编辑器\n\n" +
        "### 步骤5: 测试优化\n" +
        "1. 在模板编辑器中测试提取效果\n" +
        "2. 根据实际情况微调规则\n" +
        "3. 保存并发布模板\n\n" +
        "## 常见问题\n\n" +
        "### Q: AI 生成的 JSON 格式错误怎么办？\n" +
        "A: 使用\"验证 JSON 格式\"功能检查，根据错误提示修正。\n\n" +
        "### Q: 提取规则不准确怎么办？\n" +
        "A: 导入后在模板编辑器中手动调整关键词和正则表达式。\n\n" +
        "### Q: 支持哪些 AI 工具？\n" +
        "A: 支持所有主流 AI 工具：ChatGPT、Claude、通义千问、文心一言、讯飞星火等。\n\n" +
        "### Q: 文档太长怎么办？\n" +
        "A: 建议只复制关键页面（如前3页），包含主要字段即可。\n";
    
    /**
     * JSON 示例模板
     */
    public static final String JSON_EXAMPLE = 
        "{\n" +
        "  \"templateName\": \"采购合同模板\",\n" +
        "  \"description\": \"标准采购合同信息提取\",\n" +
        "  \"fields\": [\n" +
        "    {\n" +
        "      \"fieldName\": \"contractNo\",\n" +
        "      \"fieldLabel\": \"合同编号\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"合同编号：\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 30\n" +
        "      },\n" +
        "      \"confidence\": 95\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"partyA\",\n" +
        "      \"fieldLabel\": \"甲方名称\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"甲方：\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 50\n" +
        "      },\n" +
        "      \"confidence\": 90\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"contractAmount\",\n" +
        "      \"fieldLabel\": \"合同金额\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"合同金额：\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 30,\n" +
        "        \"pattern\": \"\\\\d+(\\\\.\\\\d{2})?\"\n" +
        "      },\n" +
        "      \"confidence\": 85\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"signDate\",\n" +
        "      \"fieldLabel\": \"签订日期\",\n" +
        "      \"fieldType\": \"date\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"签订日期：\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 20,\n" +
        "        \"pattern\": \"\\\\d{4}年\\\\d{1,2}月\\\\d{1,2}日\"\n" +
        "      },\n" +
        "      \"confidence\": 88\n" +
        "    }\n" +
        "  ]\n" +
        "}";
}

