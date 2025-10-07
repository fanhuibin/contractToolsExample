# MinerU Category 命名修复

## 问题描述

**症状**：表格内容在 `extracted.txt` 和分页 JSON 中丢失

**根本原因**：Category 命名不匹配

- MinerUOCRService 使用小写：`"table"`, `"text"`, `"formula"`
- TextExtractionUtil 检查大写：`"Table"`, `"Text"`, `"Formula"`
- dots.ocr 标准使用首字母大写

## 修复内容

### 1. Category 命名统一

修改所有 LayoutItem 的 category 为首字母大写，符合 dots.ocr 标准：

| 修改前 | 修改后 | 位置 |
|--------|--------|------|
| `"table"` | `"Table"` | 表格主体 |
| `"text"` | `"Text"` | 所有文本项 |
| `"formula"` | `"Formula"` | 公式 |

### 2. 受影响的方法

```java
// MinerUOCRService.java

handleTableItem()      // line 490: "table" → "Table"
handleTextItem()       // line 678: "text" → "Text"  
handleListItem()       // line 653: "text" → "Text"
handleCodeItem()       // line 607: "text" → "Text"
handleFormulaItem()    // line 707,712: "formula" → "Formula"
handleImageItem()      // caption: "text" → "Text"
```

### 3. dots.ocr Category 标准

根据 `dots.ocr-master/dots_ocr/utils/prompts.py`：

```python
categories = [
    'Caption',
    'Footnote', 
    'Formula',
    'List-item',
    'Page-footer',
    'Page-header',
    'Picture',
    'Section-header',
    'Table',
    'Text',
    'Title'
]
```

**规则**：所有 category 首字母大写

### 4. TextExtractionUtil 的检查逻辑

```java
// TextExtractionUtil.java line 299
if ("Table".equals(category)) {
    s = removeHtmlTags(s);  // 去除HTML标签
}
```

只有 category 完全匹配 `"Table"` 时，才会去除 HTML 标签。

### 5. 添加的调试日志

```java
log.debug("处理 MinerU 内容项，类型: {}", itemType);
log.info("🔍 检测到表格类型");
log.info("📊 处理表格项，bbox: [...]");
log.info("📝 表格去除HTML后文本长度: {}, 预览: {}", ...);
log.warn("⚠️  表格缺少 table_body 字段");
```

这些日志帮助追踪：
- 内容项的类型识别
- 表格的检测和处理
- HTML 去除的结果
- 缺失字段的警告

## 测试验证

### 预期行为

1. **表格识别**
   ```
   🔍 检测到表格类型
   📊 处理表格项，bbox: [100.0, 200.0, 500.0, 400.0]
   ```

2. **HTML 去除**
   ```
   📝 表格去除HTML后文本长度: 245, 预览: 产品\t销量\t价格\nA\t100\t25.5\n...
   ```

3. **文本提取**
   - `extracted.txt` 应包含表格的纯文本内容
   - 分页 JSON 应包含表格数据

### 如何验证

1. **查看日志**
   ```bash
   grep "检测到表格" logs/*.log
   grep "表格去除HTML" logs/*.log
   ```

2. **检查输出文件**
   ```bash
   # 查看 extracted.txt
   cat path/to/file.pdf.extracted.txt
   
   # 查看分页 JSON
   cat path/to/ocr/mineru_processed_old_filtered.json
   ```

3. **检查 LayoutItem**
   - category 应为 `"Table"`（大写）
   - text 应为去除 HTML 的纯文本

## 修复前后对比

### 修复前
```java
// category 小写
items.add(new TextExtractionUtil.LayoutItem(imageBbox, "table", cleanText));
```

**结果**：
- TextExtractionUtil 不识别 `"table"`
- 不执行 `removeHtmlTags()`
- 保留原始 HTML，无法正确提取

### 修复后
```java
// category 大写
items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Table", cleanText));
```

**结果**：
- TextExtractionUtil 识别 `"Table"`
- 执行 `removeHtmlTags()`
- 提取纯文本，正常输出

## 相关代码

### MinerUOCRService.java
- `convertToLayoutItems()` - 类型分发
- `handleTableItem()` - 表格处理
- `handleTextItem()` - 文本处理
- `handleFormulaItem()` - 公式处理

### TextExtractionUtil.java
- `applyTextProcessingRules()` - 文本规则处理
- `removeHtmlTags()` - HTML 去除

### dots.ocr
- `dots_ocr/utils/prompts.py` - Category 定义
- `dots_ocr/utils/layout_utils.py` - Category 颜色映射

## 注意事项

### 1. 严格匹配
Java 的 `String.equals()` 区分大小写，必须完全匹配。

### 2. 一致性
所有创建 LayoutItem 的地方都必须使用统一的 category 命名。

### 3. 扩展性
新增类型时，参考 dots.ocr 的命名规范：
- 使用首字母大写
- 多词用连字符：`"Page-header"`, `"List-item"`

## 未来改进

### 1. 常量定义
```java
public class LayoutCategory {
    public static final String TABLE = "Table";
    public static final String TEXT = "Text";
    public static final String FORMULA = "Formula";
    // ...
}
```

### 2. 枚举类型
```java
public enum LayoutCategory {
    TABLE("Table"),
    TEXT("Text"),
    FORMULA("Formula");
    
    private final String value;
    // ...
}
```

### 3. 验证
```java
private void validateCategory(String category) {
    if (!VALID_CATEGORIES.contains(category)) {
        log.warn("未知的 category: {}", category);
    }
}
```

## 相关文档

- [MINERU_CONTENT_TYPES_SUPPORT.md](./MINERU_CONTENT_TYPES_SUPPORT.md) - 类型支持文档
- [MINERU_TABLE_FIX_SUMMARY.md](./MINERU_TABLE_FIX_SUMMARY.md) - 表格处理总结

## 更新日期

2025-10-07

## 修复状态

✅ Category 命名已统一
✅ 调试日志已添加
✅ 表格应能正常提取

## 测试建议

使用包含表格的 PDF 文件测试：
1. 运行比对功能
2. 查看日志输出
3. 检查 extracted.txt 是否包含表格内容
4. 检查分页 JSON 是否有表格数据

如果仍有问题，请提供：
- 日志文件
- mineru_raw_*.json
- extracted.txt

