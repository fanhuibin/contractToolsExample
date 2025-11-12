package com.zhaoxinms.contract.tools.ruleextract.constants;

/**
 * AI 提示词模板常量
 * 
 * @author 山西肇新科技有限公司
 */
public class AIPromptTemplates {
    
    /**
     * 标准提取模板（支持文本字段和表格数据提取）
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
        "        \"occurrence\": 1,\n" +
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
        "## 关键词锚点提取规则\n" +
        "- type: 必须设置为 \"keyword\"（关键词锚点）\n" +
        "- keyword: 定位关键词（必填，用于在文档中定位字段位置）\n" +
        "- pattern: 正则表达式（可选但强烈推荐）\n" +
        "  * 不写 pattern 时，提取关键词后到下一个标点的所有内容\n" +
        "  * 建议根据字段类型设置精确的 pattern，提高准确性\n" +
        "- offset: 偏移量（通常为0，表示紧跟关键词）\n" +
        "- length: 提取最大长度（用于限制提取范围，建议50-100）\n" +
        "- occurrence: 第几个匹配项（可选，默认1）\n" +
        "  * 当关键词在文档中出现多次时使用\n" +
        "  * 例如：【地址:】出现3次，设置 occurrence=2 提取第2个\n" +
        "  * 例如：【日期:】出现多次，分别设置 occurrence=1,2,3 提取不同位置的日期\n\n" +
        "## 常用正则表达式（pattern 字段）\n" +
        "- 日期：\\\\d{4}年\\\\d{1,2}月\\\\d{1,2}日 或 \\\\d{4}-\\\\d{2}-\\\\d{2}\n" +
        "- 金额（简单）：[\\\\d,]+(?:\\\\.\\\\d{1,2})?\n" +
        "- 金额（带货币符号）：[¥￥][\\\\d,]+(?:\\\\.\\\\d{1,2})?\n" +
        "- 中文大写金额：[壹贰叁肆伍陆柒捌玖拾佰仟万亿元整]+\n" +
        "- 编号代码：[A-Z0-9-]+\n" +
        "- 电话号码：\\\\d{7,12} 或 \\\\d{3,4}-?\\\\d{7,8}\n" +
        "- 邮编：\\\\d{6}\n" +
        "- 通用文本（到换行）：[^\\\\r\\\\n]+\n" +
        "- 通用文本（到括号）：[^（(）)]+\n\n" +
        "## 特殊场景处理\n" +
        "### 场景1：括号内内容提取\n" +
        "当字段值在括号内时（如：`总价大写: 人民币陆萬陆仟陆佰元整 (¥66,600.00 元)`）\n" +
        "```json\n" +
        "{\n" +
        "  \"fieldName\": \"totalAmountNumber\",\n" +
        "  \"extractRules\": {\n" +
        "    \"type\": \"keyword\",\n" +
        "    \"keyword\": \"(\",\n" +
        "    \"pattern\": \"[¥￥][\\\\d,]+(?:\\\\.\\\\d{1,2})?\",\n" +
        "    \"occurrence\": 1\n" +
        "  }\n" +
        "}\n" +
        "```\n\n" +
        "### 场景2：同一行多个值提取\n" +
        "示例：`总价大写: 人民币陆萬陆仟陆佰元整 (¥66,600.00 元)`\n" +
        "- 大写：keyword=\"总价大写:\", pattern=\"[壹贰叁肆伍陆柒捌玖拾佰仟万亿元整]+\"\n" +
        "- 小写：keyword=\"(\", pattern=\"[¥￥][\\\\d,]+(?:\\\\.\\\\d{1,2})?\"\n" +
        "⚠️ 关键点：\n" +
        "- 大写金额直接用\"总价大写:\"作为关键词\n" +
        "- 小写金额用\"(\"作为关键词，因为金额在括号内\n" +
        "- 两个字段使用不同的关键词，避免冲突\n\n" +
        "## 表格提取说明\n" +
        "- fieldType 设置为 \"table\"\n" +
        "- extractRules.type 设置为 \"table\"\n" +
        "- 使用 tableRules 定义表格提取规则\n" +
        "- tableKeyword: 表格前的标题关键词\n" +
        "- columns: 需要提取的列名列表\n\n" +
        "## 注意事项\n" +
        "1. ⚠️ 只支持两种提取类型：\n" +
        "   - type=\"keyword\" - 关键词锚点提取（用于普通字段）\n" +
        "   - type=\"table\" - 表格提取（用于表格数据）\n" +
        "   - ❌ 不支持 type=\"regex\" 纯正则提取\n" +
        "2. ⚠️ 所有字段必须使用关键词定位：\n" +
        "   - 找到能唯一定位字段的关键词（如\"合同编号：\"、\"甲方：\"）\n" +
        "   - 使用 pattern 进一步精确提取内容\n" +
        "   - 不同字段必须使用不同的关键词\n" +
        "3. ⚠️ 金额字段推荐 pattern：\n" +
        "   - 简单金额：[\\\\d,]+(?:\\\\.\\\\d{1,2})?\n" +
        "   - 带货币符号：[¥￥][\\\\d,]+(?:\\\\.\\\\d{1,2})?\n" +
        "   - 大写金额：[壹贰叁肆伍陆柒捌玖拾佰仟万亿元整]+\n" +
        "4. ⚠️ 括号内的内容：\n" +
        "   - 使用括号\"(\"作为关键词\n" +
        "   - 配合 pattern 提取具体内容\n" +
        "5. 只输出JSON，不要添加解释\n" +
        "6. 确保JSON格式正确，可以被直接解析\n" +
        "7. 表格字段必须设置 fieldType=\"table\" 和 type=\"table\"\n" +
        "8. 当关键词重复出现时，使用 occurrence 参数指定第几个\n" +
        "9. pattern 中的正则表达式必须使用双反斜杠转义（\\\\d 而不是 \\d）\n";
    
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
        "        \"length\": 30,\n" +
        "        \"occurrence\": 1\n" +
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
        "        \"length\": 50,\n" +
        "        \"occurrence\": 1\n" +
        "      },\n" +
        "      \"confidence\": 90\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"partyBAddress\",\n" +
        "      \"fieldLabel\": \"乙方地址\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": false,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"地址：\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 100,\n" +
        "        \"occurrence\": 2\n" +
        "      },\n" +
        "      \"confidence\": 85,\n" +
        "      \"note\": \"当'地址：'出现多次时，提取第2个\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"contractAmount\",\n" +
        "      \"fieldLabel\": \"合同金额\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"合同金额：\",\n" +
        "        \"pattern\": \"[\\\\d,]+(?:\\\\.\\\\d{1,2})?\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 50,\n" +
        "        \"occurrence\": 1\n" +
        "      },\n" +
        "      \"confidence\": 90,\n" +
        "      \"note\": \"使用简化的金额正则，支持千分位\"\n" +
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
        "        \"length\": 30,\n" +
        "        \"occurrence\": 1,\n" +
        "        \"pattern\": \"\\\\d{4}年\\\\d{1,2}月\\\\d{1,2}日\"\n" +
        "      },\n" +
        "      \"confidence\": 95,\n" +
        "      \"note\": \"使用正则精确匹配日期格式\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"totalAmountCapital\",\n" +
        "      \"fieldLabel\": \"合同总价大写\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"总价大写:\",\n" +
        "        \"pattern\": \"[壹贰叁肆伍陆柒捌玖拾佰仟万亿元整]+\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 100,\n" +
        "        \"occurrence\": 1\n" +
        "      },\n" +
        "      \"confidence\": 90,\n" +
        "      \"note\": \"提取中文大写金额\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"fieldName\": \"totalAmountNumber\",\n" +
        "      \"fieldLabel\": \"合同总价小写\",\n" +
        "      \"fieldType\": \"text\",\n" +
        "      \"required\": true,\n" +
        "      \"extractRules\": {\n" +
        "        \"type\": \"keyword\",\n" +
        "        \"keyword\": \"(\",\n" +
        "        \"pattern\": \"[¥￥][\\\\d,]+(?:\\\\.\\\\d{1,2})?\",\n" +
        "        \"offset\": 0,\n" +
        "        \"length\": 30,\n" +
        "        \"occurrence\": 1\n" +
        "      },\n" +
        "      \"confidence\": 90,\n" +
        "      \"note\": \"使用左括号'('作为关键词，从括号内提取小写金额\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";
}

