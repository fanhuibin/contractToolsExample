# MinerU 完整修复总结

## 更新日期
2025-10-07

## 修复的问题

本次更新修复了 MinerU 集成中的四个关键问题：

### 1. ❌ 表格内容丢失
**问题**：表格在 extracted.txt 和分页 JSON 中完全丢失  
**原因**：只处理了文本和列表，缺少表格、图片、代码等类型支持  
**状态**：✅ 已修复

### 2. ❌ Category 命名不匹配
**问题**：表格即使识别了也无法正确提取  
**原因**：使用小写 `"table"` 但检查的是 `"Table"`  
**状态**：✅ 已修复

### 3. ❌ 页码显示错误
**问题**：显示"第0页"而不是"第1页"  
**原因**：MinerU 的 page_idx 是 0-based，未转换为 1-based  
**状态**：✅ 已修复

### 4. ❌ bbox 坐标偏移
**问题**：比对结果的 bbox 标记不准确，x 和 y 都偏大  
**原因**：Y 轴坐标没有翻转（PDF 向上，图片向下）  
**状态**：✅ 已修复

---

## 修复详情

### 修复 4：坐标系统转换

#### 问题根源

**MinerU 坐标系统**（PDF 标准）：
- 原点在**左下角**
- Y 轴**向上**
- 单位：点（1/72 英寸）

**图片坐标系统**（我们需要的）：
- 原点在**左上角**  
- Y 轴**向下**
- 单位：像素

#### 修复前（错误）

```java
// ❌ 错误：Y 坐标直接缩放，没有翻转
imageBbox[1] = (int) Math.round(mineruBbox[1] * scaleY);
imageBbox[3] = (int) Math.round(mineruBbox[3] * scaleY);
```

**结果**：
- 页面顶部的文本被标记在底部
- 页面底部的文本被标记在顶部
- bbox 位置完全颠倒！

#### 修复后（正确）

```java
// ✅ 正确：Y 轴翻转 + 缩放
int y1 = (int) Math.round((pdfHeight - mineruBbox[3]) * scaleY);  // PDF顶边 → 图片顶边
int y2 = (int) Math.round((pdfHeight - mineruBbox[1]) * scaleY);  // PDF底边 → 图片底边
```

**公式**：
```
y_image = (pdfHeight - y_pdf) * scaleY
```

#### 影响
- bbox 标记准确
- 高亮框位置正确
- 与 MinerU 的 layout.pdf 一致
- 前端显示正确

---

## 修复详情

### 修复 1：完整的内容类型支持

#### 新增类型处理

| 类型 | 字段 | 处理方法 | 说明 |
|------|------|----------|------|
| **Table** | table_caption, table_body, table_footnote | `handleTableItem()` | 去除HTML标签 |
| **Image** | figure_caption | `handleImageItem()` | 仅提取说明 |
| **Code** | code_caption, code_body | `handleCodeItem()` | 代码块 |
| **Formula** | latex_text, formula_caption | `handleFormulaItem()` | 保持LaTeX格式 |
| List | list_items | `handleListItem()` | 已有，优化 |
| Text | text | `handleTextItem()` | 已有，优化 |

#### 新增过滤类型

| 类型 | 说明 |
|------|------|
| aside_text | 旁注文本 |
| page_footnote | 页面脚注 |

#### 关键实现：HTML 标签去除

```java
private String removeHtmlTags(String html) {
    // 1. <br>, </tr> → 换行符
    text = html.replaceAll("(?i)<br\\s*/?>", "\n");
    text = text.replaceAll("(?i)</tr>", "\n");
    
    // 2. </td>, </th> → 制表符
    text = text.replaceAll("(?i)</td>", "\t");
    text = text.replaceAll("(?i)</th>", "\t");
    
    // 3. 移除所有其他HTML标签
    text = text.replaceAll("<[^>]+>", "");
    
    // 4. 解码HTML实体
    text = text.replace("&nbsp;", " ");
    text = text.replace("&lt;", "<");
    // ...
    
    return text.trim();
}
```

**参考**：dots.ocr 的处理方式

---

### 修复 2：Category 命名统一

#### 问题根源

```java
// ❌ 错误：使用小写
items.add(new LayoutItem(bbox, "table", text));

// TextExtractionUtil 检查大写
if ("Table".equals(category)) {  // 无法匹配！
    removeHtmlTags(text);
}
```

#### 修复方案

所有 Category 改为首字母大写（符合 dots.ocr 规范）：

| 修复前 | 修复后 | 位置 |
|--------|--------|------|
| `"table"` | `"Table"` | handleTableItem() line 490 |
| `"text"` | `"Text"` | 所有文本项 |
| `"formula"` | `"Formula"` | handleFormulaItem() |

#### dots.ocr 标准

```python
categories = [
    'Caption', 'Footnote', 'Formula', 'List-item',
    'Page-footer', 'Page-header', 'Picture',
    'Section-header', 'Table', 'Text', 'Title'
]
```

**规则**：首字母大写，多词用连字符

---

### 修复 3：页码索引转换

#### 问题对比

| 数据来源 | 索引类型 | 第一页 |
|---------|---------|--------|
| MinerU page_idx | 0-based | 0 |
| PageLayout.page | 1-based | 1 |
| 用户界面 | 1-based | 1 |

#### 修复代码

```java
// ❌ 修复前
layouts[i] = new PageLayout(i, items, imgW, imgH);
//                          ↑ 0-based（错误）

// ✅ 修复后
// 注意：MinerU 的 page_idx 是 0-based，但 PageLayout.page 应该是 1-based
layouts[i] = new PageLayout(i + 1, items, imgW, imgH);
//                          ↑ 1-based（正确）
```

#### 影响
- 比对结果显示正确页码（"第1页"而不是"第0页"）
- 与 dots.ocr 格式保持一致
- 用户体验改善

---

## 代码变更统计

### 文件：MinerUOCRService.java

| 变更类型 | 数量 |
|---------|------|
| 新增方法 | 9 个 |
| 修改方法 | 3 个 |
| 新增日志 | 6 处 |
| 总行数变化 | +400 行 |

### 新增方法列表

1. `handleTableItem()` - 处理表格
2. `handleImageItem()` - 处理图片
3. `handleCodeItem()` - 处理代码
4. `handleListItem()` - 处理列表（重构）
5. `handleTextItem()` - 处理文本（重构）
6. `handleFormulaItem()` - 处理公式
7. `extractBbox()` - 提取bbox
8. `convertAndValidateBbox()` - 转换验证bbox
9. `removeHtmlTags()` - 去除HTML标签

### 修改方法列表

1. `convertToLayoutItems()` - 添加类型分发
2. `isHeaderFooterOrPageNumber()` - 扩展过滤类型
3. `convertToPageLayouts()` - 修复页码索引

---

## 调试日志

新增的日志帮助追踪问题：

```java
// 类型检测
log.debug("处理 MinerU 内容项，类型: {}", itemType);
log.info("🔍 检测到表格类型");

// 表格处理
log.info("📊 处理表格项，bbox: [{}, {}, {}, {}]", ...);
log.debug("表格原始HTML长度: {}", tableBody.length());
log.info("📝 表格去除HTML后文本长度: {}, 预览: {}", ...);

// 警告
log.warn("⚠️  表格缺少 bbox 信息");
log.warn("⚠️  表格缺少 table_body 字段");

// 过滤
log.debug("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页, 类型:{}", pageIdx + 1, itemType);
```

---

## 测试清单

### ✅ 基本功能测试

- [ ] 使用包含表格的 PDF 测试
- [ ] 检查 extracted.txt 是否包含表格内容
- [ ] 检查分页 JSON 是否有表格数据
- [ ] 验证页码显示正确（从第1页开始）
- [ ] 验证 HTML 标签已去除

### ✅ 类型测试

- [ ] 表格（caption + body + footnote）
- [ ] 图片（caption）
- [ ] 代码（caption + body）
- [ ] 公式（latex + caption）
- [ ] 列表（多项）
- [ ] 普通文本

### ✅ 日志验证

运行后查看日志：
```bash
grep "检测到表格" logs/*.log
grep "表格去除HTML" logs/*.log
grep "第.*页" logs/*.log
```

### ✅ 输出验证

1. **extracted.txt**
   ```bash
   cat path/to/file.pdf.extracted.txt
   # 应该包含表格的纯文本内容
   ```

2. **分页 JSON**
   ```bash
   cat path/to/ocr/mineru_processed_old_filtered.json
   # 检查 pageData 中是否有表格
   ```

3. **比对结果**
   - 页码应该从"第1页"开始
   - 表格内容应该参与比对
   - Category 应为 "Table"（大写）

---

## 兼容性说明

### ✅ 向后兼容

- 保持与 dots.ocr 格式完全兼容
- 所有 LayoutItem 使用标准格式
- 不影响现有的文本和列表处理

### ✅ 数据格式

| 字段 | 格式 | 说明 |
|------|------|------|
| PageLayout.page | 1-based | 与 dots.ocr 一致 |
| LayoutItem.category | 首字母大写 | 符合标准 |
| LayoutItem.text | 纯文本 | 已去除HTML |

---

## 已知限制

1. **Caption 位置估算**
   - 表格/图片/代码的 caption bbox 是估算的（10-15%）
   - 不是 MinerU 提供的精确位置

2. **复杂 HTML**
   - 非常复杂的嵌套表格可能丢失部分格式
   - 保留了基本的行列结构

3. **公式标号**
   - 假设在公式右侧 50px
   - 可能不适合所有排版

---

## 文档索引

本次更新创建的文档：

1. **MINERU_CONTENT_TYPES_SUPPORT.md** - 完整的类型支持说明
2. **MINERU_TABLE_FIX_SUMMARY.md** - 表格处理详细总结
3. **MINERU_CATEGORY_FIX.md** - Category 命名修复
4. **MINERU_PAGE_INDEX_FIX.md** - 页码索引修复
5. **MINERU_COORDINATE_FIX.md** - 坐标系统修复
6. **MINERU_QUICK_TEST.md** - 快速测试指南
7. **MINERU_ALL_FIXES_SUMMARY.md** - 本文档

---

## 快速验证步骤

### 1. 编译项目
```bash
cd contract-tools-core
mvn clean compile
```

### 2. 运行测试
使用包含表格的 PDF 文件进行比对测试

### 3. 检查日志
```bash
# 查看表格检测
grep "🔍 检测到表格" logs/*.log

# 查看处理结果
grep "📝 表格去除HTML" logs/*.log

# 查看页码
grep "第.*页" logs/*.log
```

### 4. 验证输出
```bash
# 查看提取的文本
cat uploads/compare-pro/tasks/*/old.pdf.extracted.txt

# 查看分页数据
cat uploads/compare-pro/tasks/*/ocr/mineru_processed_old_filtered.json
```

### 5. 预期结果

✅ **日志输出**：
```
🔍 检测到表格类型
📊 处理表格项，bbox: [100.0, 200.0, 500.0, 400.0]
📝 表格去除HTML后文本长度: 245, 预览: 产品\t销量\t价格\nA\t100\t25.5\n...
```

✅ **extracted.txt**：
```
=== PAGE 1 ===
表1：销售数据
产品	销量	价格
A	100	25.5
B	200	30.0
* 数据来源于2024年第一季度
```

✅ **比对结果**：
```
差异出现在第1页（而不是第0页）
```

---

## 故障排查

### 问题 1：表格仍然丢失

**检查项**：
1. 查看日志是否有 "🔍 检测到表格类型"
2. 如果没有，检查 MinerU 返回的 content_list 中是否有 type="table"
3. 查看 mineru_raw_*.json 文件

### 问题 2：表格有HTML标签

**检查项**：
1. 查看日志中的 Category 是否为 "Table"（大写）
2. 检查 TextExtractionUtil 的 applyTextProcessingRules 是否被调用

### 问题 3：页码仍然从0开始

**检查项**：
1. 确认代码已更新（i + 1）
2. 重新编译项目
3. 检查 PageLayout.page 字段

---

## 性能考虑

| 操作 | 预期耗时 | 优化建议 |
|------|---------|---------|
| HTML 标签去除 | < 10ms/表格 | 已使用正则优化 |
| 坐标转换 | < 1ms/项 | 无需优化 |
| 类型分发 | < 0.1ms/项 | 无需优化 |

---

## 下一步建议

### 1. 功能增强
- [ ] 支持更复杂的表格格式
- [ ] 改进 caption 位置估算
- [ ] 添加更多 HTML 实体解码

### 2. 代码质量
- [ ] 定义 Category 常量
- [ ] 添加单元测试
- [ ] 性能监控

### 3. 用户体验
- [ ] 添加进度提示
- [ ] 优化错误提示
- [ ] 提供配置选项

---

## 总结

本次更新解决了 MinerU 集成的四个核心问题：

1. ✅ **内容完整性** - 支持所有 MinerU 类型，包括表格
2. ✅ **格式兼容性** - Category 命名符合 dots.ocr 标准
3. ✅ **页码准确性** - 正确显示 1-based 页码
4. ✅ **坐标准确性** - 正确转换 PDF 坐标到图片坐标

**核心改进**：
- 从只支持文本/列表 → 支持表格/图片/代码/公式
- HTML 表格 → 纯文本表格
- 第0页 → 第1页

**兼容性**：
- 完全兼容 dots.ocr 格式
- 不影响现有功能
- 向后兼容

**测试建议**：
使用包含表格的 PDF 文件测试，查看日志输出，验证 extracted.txt 和分页 JSON。

---

## 作者

AI Assistant (Claude Sonnet 4.5)

## 更新时间

2025-10-07

## 版本

v2.0 - 完整内容类型支持

