# MinerU 表格及其他类型处理完整更新

## 更新日期
2025-10-07

## 问题描述
MinerU 的比对功能丢失了表格等内容，只处理了普通文本和列表项，缺少对表格、图片、代码等特殊格式的支持。

## 解决方案

根据 [MinerU 官方文档](https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1)，实现了对 `content_list.json` 中所有内容类型的完整支持。

## 更新内容

### ✅ 1. 表格处理（Table）

**新增功能**：
- 支持 `table_caption`（表格标题）
- 支持 `table_body`（表格主体）
- 支持 `table_footnote`（表格注释）

**HTML 标签去除**：
```java
private String removeHtmlTags(String html)
```

参考 dots.ocr 的处理方式，将 HTML 表格转换为纯文本：
- `<br>`, `</tr>` → 换行符
- `</td>`, `</th>` → 制表符
- 移除所有其他 HTML 标签
- 解码 HTML 实体（`&nbsp;`, `&lt;`, `&gt;`, `&amp;`, `&quot;`, `&apos;`）
- 清理多余的空白字符

**示例**：
```
输入: <table><tr><th>产品</th><th>销量</th></tr><tr><td>A</td><td>100</td></tr></table>
输出: 产品\t销量\nA\t100\n
```

### ✅ 2. 图片处理（Image）

**新增功能**：
- 支持 `figure_caption`（图片说明）
- 图片本身不提取文本，只提取说明文字

**处理方法**：
```java
private List<TextExtractionUtil.LayoutItem> handleImageItem(...)
```

### ✅ 3. 代码处理（Code）

**新增功能**：
- 支持 `code_caption`（代码标题）
- 支持 `code_body`（代码主体）
- 支持两种子类型：
  - `sub_type: "code"` - 普通代码块
  - `sub_type: "algorithm"` - 算法伪代码

**处理方法**：
```java
private List<TextExtractionUtil.LayoutItem> handleCodeItem(...)
```

### ✅ 4. 公式处理（Formula）

**新增功能**：
- 支持 `isolate_formula` / `isolated`（行间公式）
- 支持 `formula_caption`（公式标号）
- 保持 LaTeX 格式

**处理方法**：
```java
private List<TextExtractionUtil.LayoutItem> handleFormulaItem(...)
```

### ✅ 5. 列表处理（List）

**已有功能增强**：
- 支持 `list_items`
- 自动拆分为独立的文本项
- 每项自动添加换行符

### ✅ 6. 文本处理（Text）

**已有功能增强**：
- 支持 `text`（普通文本）
- 支持 `title`（标题）
- 支持 `plain_text`（纯文本）

### ✅ 7. 丢弃类型过滤（Discarded Types）

**扩展过滤类型**：
- `header`（页眉）
- `footer`（页脚）
- `page_number`（页码）
- `aside_text`（旁注文本）**NEW**
- `page_footnote`（页面脚注）**NEW**

**更新方法**：
```java
private boolean isHeaderFooterOrPageNumber(JsonNode item)
```

## 代码变更

### 主要文件
`contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/service/MinerUOCRService.java`

### 新增方法

| 方法名 | 行数 | 功能 |
|--------|------|------|
| `handleTableItem()` | ~440-508 | 处理表格类型 |
| `handleImageItem()` | ~510-554 | 处理图片类型 |
| `handleCodeItem()` | ~556-604 | 处理代码类型 |
| `handleListItem()` | ~606-649 | 处理列表类型 |
| `handleTextItem()` | ~651-681 | 处理文本类型 |
| `handleFormulaItem()` | ~683-737 | 处理公式类型 |
| `extractBbox()` | ~739-752 | 提取并修正bbox |
| `convertAndValidateBbox()` | ~754-769 | 转换并验证bbox |
| `removeHtmlTags()` | ~771-807 | 去除HTML标签 |

### 修改方法

| 方法名 | 变更内容 |
|--------|----------|
| `convertToLayoutItems()` | 添加对所有类型的分发处理 |
| `isHeaderFooterOrPageNumber()` | 扩展过滤类型 |

## 类型支持对照表

| MinerU 类型 | 字段 | 处理状态 | 处理方法 |
|------------|------|---------|---------|
| text | text, bbox | ✅ | handleTextItem |
| title | text, bbox | ✅ | handleTextItem |
| plain_text | text, bbox | ✅ | handleTextItem |
| list | list_items[], bbox | ✅ | handleListItem |
| table | table_caption[], table_body, table_footnote[], bbox | ✅ | handleTableItem |
| image | figure_caption[], bbox | ✅ | handleImageItem |
| code | code_caption[], code_body, bbox | ✅ | handleCodeItem |
| isolate_formula | latex_text, formula_caption[], bbox | ✅ | handleFormulaItem |
| isolated | latex_text, formula_caption[], bbox | ✅ | handleFormulaItem |
| header | text, bbox | ✅ 过滤 | isHeaderFooterOrPageNumber |
| footer | text, bbox | ✅ 过滤 | isHeaderFooterOrPageNumber |
| page_number | text, bbox | ✅ 过滤 | isHeaderFooterOrPageNumber |
| aside_text | text, bbox | ✅ 过滤 | isHeaderFooterOrPageNumber |
| page_footnote | text, bbox | ✅ 过滤 | isHeaderFooterOrPageNumber |

## 测试建议

### 1. 表格测试用例
```json
{
  "type": "table",
  "table_caption": ["表1：销售数据"],
  "table_body": "<table><tr><th>产品</th><th>销量</th></tr><tr><td>A</td><td>100</td></tr></table>",
  "table_footnote": ["* 预估值"],
  "bbox": [100, 200, 500, 400],
  "page_idx": 0
}
```

**期望输出**：
1. Caption: "表1：销售数据\n"
2. Body: "产品\t销量\nA\t100\n"
3. Footnote: "* 预估值\n"

### 2. 代码测试用例
```json
{
  "type": "code",
  "sub_type": "algorithm",
  "code_caption": ["Algorithm 1: Quick Sort"],
  "code_body": "function quickSort(arr)\n  if length(arr) <= 1\n    return arr\n  ...",
  "bbox": [100, 200, 500, 400],
  "page_idx": 0
}
```

### 3. 列表测试用例
```json
{
  "type": "list",
  "sub_type": "text",
  "list_items": ["第一项", "第二项", "第三项"],
  "bbox": [100, 200, 500, 300],
  "page_idx": 0
}
```

### 4. 公式测试用例
```json
{
  "type": "isolate_formula",
  "latex_text": "E = mc^2",
  "formula_caption": ["(1)"],
  "bbox": [100, 200, 500, 250],
  "page_idx": 0
}
```

## 兼容性说明

### ✅ 向后兼容
- 保持与现有 dots.ocr 格式完全兼容
- 所有 LayoutItem 使用标准格式
- 不影响现有的文本和列表处理逻辑

### ✅ 坐标转换
- 所有类型都使用统一的坐标转换逻辑
- 自动修正超出边界的坐标
- 验证并修正舍入误差

## 性能考虑

### HTML 标签处理
- 使用正则表达式批量处理
- 避免逐字符解析
- 对大表格可能需要优化

### 内存优化
- 按需创建 LayoutItem
- 及时释放临时对象

## 已知限制

1. **Caption 位置估算**
   - 表格/图片/代码的 caption bbox 是估算的（10-15% 高度）
   - 不是精确的 MinerU 识别位置

2. **公式标号位置**
   - 假设在公式右侧 50px
   - 可能不适合所有排版

3. **复杂 HTML**
   - 非常复杂的表格可能丢失部分格式
   - 嵌套表格支持有限

4. **子类型识别**
   - 代码的 sub_type (code/algorithm) 当前未区分处理
   - 列表的 sub_type (text/ref_text) 当前未区分处理

## 配置选项

```java
CompareOptions options = new CompareOptions();
options.setIgnoreHeaderFooter(true);  // 过滤页眉页脚等丢弃类型
```

## 日志输出

### 调试日志
```
处理图片，bbox: [100.0, 200.0, 500.0, 400.0]
```

### 过滤日志
```
🚫 过滤 MinerU 识别的页眉页脚 - 第1页, 类型:header
```

## 文档

- **功能说明**：[MINERU_CONTENT_TYPES_SUPPORT.md](./MINERU_CONTENT_TYPES_SUPPORT.md)
- **更新总结**：本文档

## 参考资料

1. [MinerU 输出文件格式文档](https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1)
2. dots.ocr 的 format_transformer.py
3. 现有的 TextExtractionUtil.java

## 下一步建议

1. **测试验证**
   - 使用包含表格的 PDF 测试
   - 验证 HTML 标签去除效果
   - 检查坐标转换准确性

2. **性能优化**
   - 监控大表格处理性能
   - 考虑缓存正则表达式

3. **功能增强**
   - 支持更复杂的表格格式
   - 改进 caption 位置估算
   - 添加更多 HTML 实体解码

4. **错误处理**
   - 添加异常捕获
   - 记录处理失败的情况
   - 提供降级处理方案

## 完成状态

✅ 所有 TODO 项已完成
✅ 代码已更新
✅ 文档已创建
✅ 类型检查通过

## 作者

AI Assistant (Claude Sonnet 4.5)

## 更新时间

2025-10-07

