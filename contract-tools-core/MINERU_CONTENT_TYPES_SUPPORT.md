# MinerU Content Types 完整支持说明

## 概述

根据 MinerU 官方文档（https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1），
`content_list.json` 包含多种内容类型。本次更新实现了对所有 MinerU 内容类型的完整支持。

## 更新日期

2025-10-07

## 支持的内容类型

### 1. 文本类型（Text Types）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `text` | `text`, `bbox` | 提取纯文本 | 普通文本段落 |
| `title` | `text`, `bbox` | 提取纯文本 | 标题文本 |
| `plain_text` | `text`, `bbox` | 提取纯文本 | 纯文本 |

### 2. 列表类型（List Type）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `list` | `list_items[]`, `bbox` | 拆分为多个文本项 | 列表项，每项单独处理 |

**处理逻辑**：
- 将列表的总 bbox 按列表项数量均分
- 每个列表项创建独立的 LayoutItem
- 自动添加换行符

### 3. 表格类型（Table Type）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `table` | `table_caption[]`, `table_body`, `table_footnote[]`, `bbox` | **去除HTML标签**，转换为纯文本 | 表格及其说明 |

**重要：HTML 标签处理**
```java
// 参考 dots.ocr 的处理方式，去除 HTML 标签
private String removeHtmlTags(String html) {
    // 1. <br>, </tr> → 换行符
    // 2. </td>, </th> → 制表符
    // 3. 移除所有其他 HTML 标签
    // 4. 解码 HTML 实体 (&nbsp;, &lt;, &gt;, etc.)
    // 5. 清理多余空白
}
```

**处理顺序**：
1. `table_caption` - 表格标题（放在表格上方）
2. `table_body` - 表格主体（去除HTML后的纯文本）
3. `table_footnote` - 表格注释（放在表格下方）

### 4. 图片类型（Image Type）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `image` | `figure_caption[]`, `bbox` | 仅提取 caption | 图片本身不提取文本 |

**处理逻辑**：
- 图片本身不提取文本内容
- 只提取 `figure_caption`（图片说明）
- Caption 放在图片 bbox 下方区域

### 5. 代码类型（Code Type）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `code` | `code_caption[]`, `code_body`, `bbox` | 提取代码文本 | 代码块及其标题 |

**包含两种子类型**：
- `sub_type: "code"` - 普通代码块
- `sub_type: "algorithm"` - 算法伪代码

**处理顺序**：
1. `code_caption` - 代码标题（如 "Algorithm 1"）
2. `code_body` - 代码主体

### 6. 公式类型（Formula Type）

| 类型 | 字段 | 处理方式 | 说明 |
|------|------|----------|------|
| `isolate_formula` / `isolated` | `latex_text` 或 `text`, `formula_caption[]`, `bbox` | 保持 LaTeX 格式 | 独立公式（行间公式）|

**处理逻辑**：
- 优先使用 `latex_text` 字段
- 保持 LaTeX 格式，便于后续处理
- 提取 `formula_caption`（公式标号，如 "(1)"）

### 7. 丢弃类型（Discarded Types）

以下类型会被过滤掉（当 `ignoreHeaderFooter` 选项开启时）：

| 类型 | 说明 |
|------|------|
| `header` | 页眉 |
| `footer` | 页脚 |
| `page_number` | 页码 |
| `aside_text` | 旁注文本 |
| `page_footnote` | 页面脚注 |

## 代码实现

### 主要方法

```java
private List<TextExtractionUtil.LayoutItem> convertToLayoutItems(
        JsonNode item,
        Map<String, Object> pageImage,
        double[] pdfPageSize)
```

**处理流程**：
1. 根据 `type` 字段判断内容类型
2. 调用对应的处理方法
3. 返回 LayoutItem 列表

### 类型处理方法

| 方法 | 处理的类型 |
|------|-----------|
| `handleTableItem()` | table |
| `handleImageItem()` | image |
| `handleCodeItem()` | code |
| `handleListItem()` | list |
| `handleFormulaItem()` | isolate_formula, isolated |
| `handleTextItem()` | text, title, plain_text |

### HTML 标签去除

```java
private String removeHtmlTags(String html)
```

**参考 dots.ocr 的处理方式**：
- 表格 HTML → 纯文本
- 保留表格结构（使用制表符和换行符）
- 解码 HTML 实体

## 测试建议

### 1. 表格测试
- [ ] 包含复杂 HTML 标签的表格
- [ ] 带 caption 和 footnote 的表格
- [ ] 多行多列的表格

### 2. 代码测试
- [ ] 普通代码块
- [ ] 算法伪代码
- [ ] 带标题的代码

### 3. 列表测试
- [ ] 有序列表
- [ ] 无序列表
- [ ] 多级列表

### 4. 公式测试
- [ ] LaTeX 公式
- [ ] 带标号的公式
- [ ] 行间公式

### 5. 图片测试
- [ ] 带 caption 的图片
- [ ] 不带 caption 的图片

## 配置选项

在 `CompareOptions` 中：
```java
options.setIgnoreHeaderFooter(true);  // 过滤页眉页脚
```

## 已知限制

1. **Caption 位置估算**：表格、图片、代码的 caption bbox 是基于主体 bbox 估算的（10-15%），不是精确位置
2. **公式标号位置**：公式标号假设在右侧 50px，可能不够精确
3. **HTML 表格格式**：复杂的表格可能丢失部分格式信息

## 参考资料

- [MinerU 输出文件格式文档](https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1)
- dots.ocr 的表格处理方式

## 更新记录

### 2025-10-07
- ✅ 添加表格类型支持（table, table_caption, table_footnote）
- ✅ 实现 HTML 标签去除功能
- ✅ 添加图片类型支持（image, figure_caption）
- ✅ 添加代码类型支持（code, code_caption, code_body）
- ✅ 添加公式类型支持（isolate_formula, formula_caption）
- ✅ 扩展丢弃类型过滤（aside_text, page_footnote）
- ✅ 添加标题类型支持（title）

## 注意事项

1. **性能优化**：大表格的 HTML 处理可能较慢，建议监控性能
2. **日志记录**：调试模式下会记录每种类型的处理情况
3. **向后兼容**：保持与现有 dots.ocr 格式完全兼容

## 示例输出

### 原始 MinerU JSON
```json
{
  "type": "table",
  "table_caption": ["Table 1: Sales Data"],
  "table_body": "<table><tr><th>Product</th><th>Sales</th></tr><tr><td>A</td><td>100</td></tr></table>",
  "table_footnote": ["* Estimated values"],
  "bbox": [100, 200, 500, 400],
  "page_idx": 0
}
```

### 转换后的 LayoutItem
```
1. LayoutItem(type="text", text="Table 1: Sales Data\n")
2. LayoutItem(type="table", text="Product\tSales\nA\t100\n")
3. LayoutItem(type="text", text="* Estimated values\n")
```

## 相关文件

- `MinerUOCRService.java` - 主要实现文件
- `TextExtractionUtil.java` - LayoutItem 定义
- `MinerUCoordinateConverter.java` - 坐标转换工具

