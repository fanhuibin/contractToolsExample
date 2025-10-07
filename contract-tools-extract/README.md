# 合同工具集 - 文本信息提取模块

## 概述

`contract-tools-extract` 是一个基于大语言模型(LLM)的文本结构化信息提取模块，完全复刻了Google LangExtract的功能，并使用Java实现，对接阿里云通义千问服务。

## 功能特性

- ✅ **智能文本提取**: 使用LLM从非结构化文本中提取结构化信息
- ✅ **灵活的模式定义**: 支持自定义提取字段和数据类型
- ✅ **多格式支持**: 支持JSON和YAML输出格式
- ✅ **批量处理**: 支持多文档批量提取
- ✅ **阿里云集成**: 无缝对接阿里云通义千问API
- ✅ **Spring Boot集成**: 提供自动配置和REST API
- ✅ **类型安全**: 完整的Java类型系统支持
- ✅ **高度可配置**: 支持温度、token数量等参数调节

## 支持的字段类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `STRING` | 字符串 | "张三" |
| `INTEGER` | 整数 | 25 |
| `FLOAT` | 浮点数 | 123.45 |
| `BOOLEAN` | 布尔值 | true |
| `DATE` | 日期 | "2024-01-01" |
| `DATETIME` | 日期时间 | "2024-01-01T12:00:00" |
| `EMAIL` | 邮箱地址 | "user@example.com" |
| `URL` | 网址 | "https://example.com" |
| `PHONE` | 电话号码 | "13800138000" |
| `CURRENCY` | 货币金额 | "50万元" |
| `ARRAY` | 数组 | ["值1", "值2"] |
| `OBJECT` | 对象 | {"key": "value"} |

## 快速开始

### 1. 配置依赖

在你的Maven项目中添加依赖：

```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-extract</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置阿里云API

在 `application.yml` 中配置：

```yaml
zhaoxin:
  extract:
    enabled: true
    aliyun:
      api-key: "your-aliyun-api-key"
      model: "qwen-plus"  # 可选: qwen-turbo, qwen-plus, qwen-max
    default-options:
      format: "json"
      temperature: 0.1
      max-tokens: 2000
      confidence-threshold: 0.5
```

### 3. 基础使用

```java
import com.zhaoxinms.contract.tools.extract.LangExtract;
import com.zhaoxinms.contract.tools.extract.core.data.*;

// 设置API Key
LangExtract.setAliyunProvider("your-api-key");

// 定义提取模式
ExtractionSchema schema = ExtractionSchema.builder()
    .name("合同信息提取")
    .description("从合同文本中提取关键信息")
    .build();

schema.addField(FieldDefinition.builder()
    .name("contract_title")
    .description("合同标题")
    .type(FieldType.STRING)
    .required(true)
    .build());

schema.addField(FieldDefinition.builder()
    .name("party_a")
    .description("甲方名称")
    .type(FieldType.STRING)
    .required(true)
    .build());

// 执行提取
String contractText = "销售合同\n甲方：北京科技有限公司...";
List<Extraction> results = LangExtract.extract(contractText, schema);

// 处理结果
for (Extraction extraction : results) {
    System.out.println(extraction.getField() + ": " + extraction.getValue());
}
```

## 高级用法

### 自定义提取选项

```java
ExtractEngine.ExtractionOptions options = new ExtractEngine.ExtractionOptions()
    .format("json")
    .temperature(0.05)  // 更低的温度确保一致性
    .maxTokens(1500)
    .confidenceThreshold(0.8)
    .failFast(true);

List<Extraction> results = LangExtract.extract(text, schema, options);
```

### 批量处理

```java
List<Document> documents = Arrays.asList(
    Document.builder().id("doc1").content("文档1内容").build(),
    Document.builder().id("doc2").content("文档2内容").build()
);

Map<String, List<Extraction>> results = LangExtract.extractBatch(documents, schema);
```

### Spring Boot集成

```java
@RestController
public class MyController {
    
    @Autowired
    private ExtractService extractService;
    
    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody String text) {
        try {
            List<Extraction> results = extractService.extractFromText(text, schema);
            return ResponseEntity.ok(results);
        } catch (ExtractException e) {
            return ResponseEntity.badRequest().body("提取失败: " + e.getMessage());
        }
    }
}
```

## REST API

模块提供了完整的REST API接口：

### 文本提取

```bash
POST /api/extract/text
Content-Type: application/json

{
  "text": "待提取的文本内容",
  "schema": {
    "name": "信息提取",
    "description": "提取关键信息",
    "fields": [
      {
        "name": "name",
        "description": "姓名",
        "type": "STRING",
        "required": true
      }
    ]
  }
}
```

### 文档提取

```bash
POST /api/extract/document
Content-Type: application/json

{
  "document": {
    "id": "doc-001",
    "content": "文档内容",
    "type": "contract",
    "metadata": {}
  },
  "schema": { ... }
}
```

### 批量提取

```bash
POST /api/extract/batch
Content-Type: application/json

{
  "documents": [
    {
      "id": "doc-001",
      "content": "文档1内容"
    },
    {
      "id": "doc-002", 
      "content": "文档2内容"
    }
  ],
  "schema": { ... }
}
```

### 高级提取

```bash
POST /api/extract/advanced
Content-Type: application/json

{
  "text": "待提取的文本",
  "schema": { ... },
  "options": {
    "format": "json",
    "temperature": 0.1,
    "maxTokens": 2000,
    "confidenceThreshold": 0.8
  }
}
```

### 服务状态

```bash
GET /api/extract/status
```

## 实际应用场景

### 1. 合同信息提取

```java
ExtractionSchema contractSchema = ExtractionSchema.builder()
    .name("合同信息提取")
    .build();

contractSchema.addField(FieldDefinition.builder()
    .name("contract_title").type(FieldType.STRING).required(true).build());
contractSchema.addField(FieldDefinition.builder()
    .name("party_a").type(FieldType.STRING).required(true).build());
contractSchema.addField(FieldDefinition.builder()
    .name("party_b").type(FieldType.STRING).required(true).build());
contractSchema.addField(FieldDefinition.builder()
    .name("contract_amount").type(FieldType.CURRENCY).build());
contractSchema.addField(FieldDefinition.builder()
    .name("signing_date").type(FieldType.DATE).build());
```

### 2. 发票信息提取

```java
ExtractionSchema invoiceSchema = ExtractionSchema.builder()
    .name("发票信息提取")
    .build();

invoiceSchema.addField(FieldDefinition.builder()
    .name("invoice_number").type(FieldType.STRING).required(true).build());
invoiceSchema.addField(FieldDefinition.builder()
    .name("invoice_date").type(FieldType.DATE).required(true).build());
invoiceSchema.addField(FieldDefinition.builder()
    .name("seller_name").type(FieldType.STRING).required(true).build());
invoiceSchema.addField(FieldDefinition.builder()
    .name("total_amount").type(FieldType.CURRENCY).required(true).build());
```

### 3. 人员简历提取

```java
ExtractionSchema resumeSchema = ExtractionSchema.builder()
    .name("简历信息提取")
    .build();

resumeSchema.addField(FieldDefinition.builder()
    .name("name").type(FieldType.STRING).required(true).build());
resumeSchema.addField(FieldDefinition.builder()
    .name("age").type(FieldType.INTEGER).build());
resumeSchema.addField(FieldDefinition.builder()
    .name("email").type(FieldType.EMAIL).build());
resumeSchema.addField(FieldDefinition.builder()
    .name("skills").type(FieldType.ARRAY)
    .examples(Arrays.asList(Arrays.asList("Java", "Python", "Spring")))
    .build());
```

## 性能优化建议

1. **温度设置**: 对于结构化提取，建议使用较低的温度值(0.05-0.2)以确保结果一致性
2. **批量处理**: 处理多个文档时使用批量API可以提高效率
3. **置信度阈值**: 根据业务需求调整置信度阈值，过滤低质量结果
4. **模型选择**: 
   - `qwen-turbo`: 速度快，成本低，适合简单提取
   - `qwen-plus`: 平衡性能和成本，推荐日常使用
   - `qwen-max`: 最高精度，适合复杂场景

## 错误处理

```java
try {
    List<Extraction> results = LangExtract.extract(text, schema);
} catch (ExtractException e) {
    log.error("提取失败: {}", e.getMessage());
    // 处理提取异常
} catch (ProviderException e) {
    log.error("LLM服务异常: {}", e.getMessage());
    // 处理服务提供商异常
}
```

## 与Python版本的对比

| 功能 | Python LangExtract | Java Contract-Tools-Extract |
|------|-------------------|----------------------------|
| 核心提取功能 | ✅ | ✅ |
| 模式定义 | ✅ | ✅ |
| 批量处理 | ✅ | ✅ |
| 多格式支持 | ✅ | ✅ |
| LLM提供商 | Google, OpenAI等 | 阿里云通义千问 |
| Spring Boot集成 | ❌ | ✅ |
| REST API | ❌ | ✅ |
| 类型安全 | 部分 | ✅ |

## 注意事项

1. **API Key安全**: 请妥善保管阿里云API Key，不要在代码中硬编码
2. **成本控制**: LLM调用会产生费用，建议设置合理的token限制
3. **数据隐私**: 文本内容会发送到阿里云服务，请确保符合数据安全要求
4. **网络连接**: 需要稳定的网络连接访问阿里云API

## 故障排除

### 常见问题

1. **API Key无效**
   ```
   错误: API_KEY_MISSING: API Key未设置或无效
   解决: 检查配置文件中的api-key设置
   ```

2. **网络连接问题**
   ```
   错误: 网络连接超时
   解决: 检查网络连接和防火墙设置
   ```

3. **提取结果为空**
   ```
   原因: 文本内容不包含目标信息或模式定义不准确
   解决: 检查文本内容和字段定义，添加更多提示信息
   ```

4. **格式解析失败**
   ```
   原因: LLM返回的格式不符合预期
   解决: 降低温度参数，优化提示词
   ```

## 开发计划

- [ ] 支持更多LLM提供商(OpenAI, 文心一言等)
- [ ] 增加结果缓存机制
- [ ] 支持流式处理
- [ ] 添加更多预定义模式
- [ ] 性能监控和统计

## 许可证

Apache License 2.0
