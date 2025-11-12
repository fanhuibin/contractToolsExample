# 模板字段配置说明

## 文件位置
`fields.json` - 模板字段配置文件

## 配置结构

配置文件必须包含以下四种类型的字段数组（可以为空数组，但必须存在）：

```json
{
  "baseFields": [],           // 基础字段
  "counterpartyFields": [],   // 相对方字段
  "clauseFields": [],         // 条款字段
  "sealFields": []            // 印章字段
}
```

## 字段类型及必填项

### 1. 基础字段 (baseFields)

**必填字段：**
- `id`: 字段唯一标识（不能重复）
- `name`: 字段名称（最大100字符）
- `code`: 字段代码（最大100字符，只能包含字母、数字、下划线，必须以字母开头，不能重复）

**可选字段：**
- `isRichText`: 是否为富文本（默认 false）
- `sampleValue`: 示例值

**示例：**
```json
{
  "id": "1",
  "name": "合同名",
  "code": "base_contractName",
  "isRichText": false,
  "sampleValue": "示例-合同名"
}
```

### 2. 相对方字段 (counterpartyFields)

**必填字段：**
- `id`: 字段唯一标识（不能重复）
- `name`: 字段名称（最大100字符）
- `code`: 字段代码（最大100字符，只能包含字母、数字、下划线，必须以字母开头，不能重复）
- `counterpartyIndex`: 相对方索引（数字，表示第几个相对方）

**可选字段：**
- `sampleValue`: 示例值

**示例：**
```json
{
  "id": "101",
  "name": "主体名称",
  "code": "subject_name",
  "counterpartyIndex": 1,
  "sampleValue": "某某科技有限公司"
}
```

### 3. 条款字段 (clauseFields)

**必填字段：**
- `id`: 字段唯一标识（不能重复）
- `name`: 字段名称（最大100字符）
- `code`: 字段代码（最大100字符，只能包含字母、数字、下划线，必须以字母开头，不能重复）
- `content`: 条款内容（支持变量引用，如 ${base_contractName}）

**可选字段：**
- `type`: 条款类型（如 general, payment, address 等）
- `typeName`: 条款类型名称（如 通用条款、付款条款等）
- `sampleValue`: 示例值

**示例：**
```json
{
  "id": "clause_001",
  "name": "第一条",
  "code": "clause_1",
  "content": "甲方：${party_a_name}，乙方：${party_b_name}",
  "type": "general",
  "typeName": "通用条款",
  "sampleValue": "示例条款一"
}
```

### 4. 印章字段 (sealFields)

**必填字段：**
- `id`: 字段唯一标识（不能重复）
- `name`: 字段名称（最大100字符）
- `code`: 字段代码（最大100字符，只能包含字母、数字、下划线，必须以字母开头，不能重复）
- `type`: 印章类型（如 company, finance, contract 等）

**可选字段：**
- `orderIndex`: 印章顺序（数字，用于多方盖章的顺序控制）

**示例：**
```json
{
  "id": "seal_001",
  "name": "公司公章",
  "code": "company_seal",
  "type": "company",
  "orderIndex": 1
}
```

## 校验规则

### 全局规则
1. 配置文件必须是合法的 JSON 格式
2. 必须包含四种字段类型（baseFields、counterpartyFields、clauseFields、sealFields）
3. 每种字段类型必须是数组（可以为空数组）
4. 所有字段的 `id` 不能重复（跨类型检查）
5. 所有字段的 `code` 不能重复（跨类型检查）

### 字段命名规则
- **id**: 任意字符串，但不能重复，长度不超过100字符
- **name**: 任意字符串，长度不超过100字符
- **code**: 
  - 只能包含字母、数字、下划线
  - 必须以字母开头
  - 不能重复（全局唯一）
  - 长度不超过100字符
  - 示例：`base_contractName`、`subject_name`、`clause_1`

### 特殊字段规则
- **counterpartyIndex**: 必须是数字类型
- **isRichText**: 布尔类型（true/false）
- **orderIndex**: 数字类型
- **content**: 条款字段的必填项，不能为空

## 修改配置

修改 `fields.json` 后：
1. 保存文件
2. 重启后端服务
3. 如果配置有误，服务启动时会在日志中显示详细的错误信息

## 错误示例

### 错误1：缺少字段类型
```json
{
  "baseFields": [],
  "counterpartyFields": []
  // 缺少 clauseFields 和 sealFields
}
```
**错误信息**: `缺少 clauseFields 字段；缺少 sealFields 字段；`

### 错误2：code 格式错误
```json
{
  "id": "1",
  "name": "合同名",
  "code": "123_name"  // 不能以数字开头
}
```
**错误信息**: `[baseFields#1] code 格式不正确（只能包含字母、数字、下划线，且必须以字母开头）: 123_name；`

### 错误3：缺少必填字段
```json
{
  "id": "clause_001",
  "name": "第一条",
  "code": "clause_1"
  // 缺少 content 字段
}
```
**错误信息**: `[clauseFields#1] content 不能为空；`

### 错误4：id 或 code 重复
```json
{
  "baseFields": [
    {"id": "1", "name": "字段1", "code": "field1"},
    {"id": "1", "name": "字段2", "code": "field2"}  // id 重复
  ]
}
```
**错误信息**: `[baseFields#2] id 重复: 1；`

## 最佳实践

1. **命名规范**：
   - 基础字段 code 以 `base_` 开头
   - 相对方字段 code 以 `subject_` 开头
   - 条款字段 code 以 `clause_` 开头
   - 印章字段 code 以适当的前缀命名（如 `company_seal`）

2. **保持一致性**：
   - 使用统一的命名风格
   - 相同类型的字段使用相同的前缀

3. **备份配置**：
   - 修改前备份原配置文件
   - 可以创建多个版本的配置文件

4. **测试验证**：
   - 修改后先在测试环境验证
   - 查看日志确认配置加载成功

## 技术支持

如有问题，请查看：
1. 后端日志：`logs/contract-tools-sdk.log`
2. 控制台输出：查找包含 "字段配置" 的日志信息

