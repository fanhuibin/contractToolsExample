# MinerU Page Index 修复（0-based → 1-based）

## 问题描述

**症状**：
- 比对结果显示"第0页"而不是"第1页"
- 页码显示不一致
- 与 dots.ocr 格式不兼容

**根本原因**：
- MinerU API 返回的 `page_idx` 是 **0-based**（从 0 开始）
- dots.ocr 和其他系统使用的页码是 **1-based**（从 1 开始）
- 创建 `PageLayout` 对象时直接使用了 0-based 索引

## 索引对比

### MinerU (0-based)
```json
{
  "type": "text",
  "text": "Hello World",
  "page_idx": 0,    ← 第一页是 0
  "bbox": [100, 200, 500, 400]
}
```

### dots.ocr & PageLayout (1-based)
```java
PageLayout layout = new PageLayout(
    1,              ← 第一页是 1
    items,
    imageWidth,
    imageHeight
);
```

## 修复内容

### 1. 核心修复

**文件**：`MinerUOCRService.java`

**修改位置**：`convertToPageLayouts()` 方法 (line 388-389)

**修改前**：
```java
for (int i = 0; i < totalPages; i++) {
    // ...
    layouts[i] = new TextExtractionUtil.PageLayout(i, items, imgW, imgH);
    //                                             ↑ 0-based（错误）
}
```

**修改后**：
```java
for (int i = 0; i < totalPages; i++) {
    // ...
    // 注意：MinerU 的 page_idx 是 0-based，但 PageLayout.page 应该是 1-based（与 dots.ocr 一致）
    layouts[i] = new TextExtractionUtil.PageLayout(i + 1, items, imgW, imgH);
    //                                             ↑ 1-based（正确）
}
```

### 2. 其他正确的地方（无需修改）

#### 日志输出（已经正确）
```java
// line 266, 363
log.info("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页, 类型:{}, 内容:{}", pageIdx + 1, itemType, itemText);
//                                                              ↑ 正确：0-based + 1 = 1-based
```

#### 图片文件命名（已经正确）
```java
// line 202
File imageFile = new File(imagesDir, "page-" + (i + 1) + ".png");
//                                            ↑ 正确：文件名从 page-1.png 开始
```

## 影响范围

### 1. PageLayout 的 page 字段

**定义**：
```java
public static class PageLayout {
    public final int page;  // 应该是 1-based
    // ...
}
```

**用途**：
- 显示比对结果的页码
- 生成差异报告
- 用户界面显示

### 2. 数据结构对应关系

| 数据来源 | 索引类型 | 第一页 | 第二页 | 用途 |
|---------|---------|--------|--------|------|
| MinerU `page_idx` | 0-based | 0 | 1 | 内部处理 |
| Java 数组索引 | 0-based | 0 | 1 | 数据存储 |
| `PageLayout.page` | 1-based | 1 | 2 | **显示和比对** |
| 用户界面 | 1-based | 1 | 2 | 用户看到的 |
| dots.ocr | 1-based | 1 | 2 | 兼容性 |

### 3. 转换规则

```java
// MinerU page_idx → PageLayout page
int mineruPageIdx = 0;        // 0-based（来自 MinerU）
int arrayIndex = mineruPageIdx;  // 0-based（存储在数组中）
int pageNumber = arrayIndex + 1; // 1-based（显示给用户）

PageLayout layout = new PageLayout(pageNumber, items, w, h);
```

## 验证测试

### 1. 单页文档
- MinerU 返回：`page_idx: 0`
- 应该显示：`第1页`
- PageLayout.page：`1`

### 2. 多页文档（3页）
| MinerU page_idx | 数组索引 | PageLayout.page | 显示 |
|----------------|---------|----------------|------|
| 0 | layouts[0] | 1 | 第1页 |
| 1 | layouts[1] | 2 | 第2页 |
| 2 | layouts[2] | 3 | 第3页 |

### 3. 比对结果
```
修改前：
- 差异出现在第0页（❌ 错误）

修改后：
- 差异出现在第1页（✅ 正确）
```

## 与其他组件的一致性

### 1. dots.ocr
```python
# dots.ocr 也是 1-based
for page_num in range(1, total_pages + 1):
    process_page(page_num)  # page_num 从 1 开始
```

### 2. CompareService
```java
// CompareService 创建 PageLayout 也是 1-based
return new TextExtractionUtil.PageLayout(page, items, imgW, imgH);
// page 参数已经是 1-based
```

### 3. 前端显示
```typescript
// 前端显示时也期望 1-based
<div>第 {{ pageNumber }} 页</div>  // pageNumber 从 1 开始
```

## 代码审查清单

在处理 MinerU 数据时，请注意：

- [ ] ✅ 从 MinerU 获取的 `page_idx` 是 0-based
- [ ] ✅ 存储在 HashMap/Array 中使用 0-based 索引
- [ ] ✅ 创建 PageLayout 时转换为 1-based（`pageIdx + 1`）
- [ ] ✅ 日志输出使用 1-based 显示（`pageIdx + 1`）
- [ ] ✅ 文件命名使用 1-based（`page-1.png`）
- [ ] ✅ 返回给前端的数据使用 1-based

## 常见陷阱

### ❌ 错误示例 1：直接使用 MinerU 的 page_idx
```java
// 错误！
int pageIdx = item.get("page_idx").asInt();  // 0
PageLayout layout = new PageLayout(pageIdx, items, w, h);  // page = 0 (错误)
```

### ✅ 正确示例 1：转换为 1-based
```java
// 正确！
int pageIdx = item.get("page_idx").asInt();  // 0
PageLayout layout = new PageLayout(pageIdx + 1, items, w, h);  // page = 1 (正确)
```

### ❌ 错误示例 2：循环中混淆
```java
// 错误！
for (int i = 0; i < totalPages; i++) {
    // 使用 i 作为页码（0-based）
    System.out.println("Processing page " + i);  // 输出 "page 0"（错误）
}
```

### ✅ 正确示例 2：明确转换
```java
// 正确！
for (int i = 0; i < totalPages; i++) {
    int pageNumber = i + 1;  // 转换为 1-based
    System.out.println("Processing page " + pageNumber);  // 输出 "page 1"（正确）
}
```

## 调试技巧

### 1. 添加断言
```java
assert pageLayout.page >= 1 : "Page number should be 1-based";
assert pageLayout.page <= totalPages : "Page number out of range";
```

### 2. 日志验证
```java
log.debug("MinerU page_idx: {}, PageLayout.page: {}", 
    mineruPageIdx,     // 0
    mineruPageIdx + 1  // 1
);
```

### 3. 单元测试
```java
@Test
public void testPageIndexConversion() {
    // MinerU 第一页
    int mineruPageIdx = 0;
    
    // 转换为 PageLayout
    PageLayout layout = new PageLayout(mineruPageIdx + 1, items, w, h);
    
    // 验证
    assertEquals(1, layout.page);  // 应该是 1，不是 0
}
```

## 相关文档

- [MINERU_CONTENT_TYPES_SUPPORT.md](./MINERU_CONTENT_TYPES_SUPPORT.md) - 内容类型支持
- [MINERU_CATEGORY_FIX.md](./MINERU_CATEGORY_FIX.md) - Category 命名修复
- [MINERU_TABLE_FIX_SUMMARY.md](./MINERU_TABLE_FIX_SUMMARY.md) - 表格处理总结

## 更新日期

2025-10-07

## 修复状态

✅ PageLayout 创建时已转换为 1-based
✅ 日志输出已使用 1-based
✅ 文件命名已使用 1-based
✅ 与 dots.ocr 格式保持一致

## 总结

这个修复确保了：
1. **用户体验**：显示"第1页"而不是"第0页"
2. **一致性**：与 dots.ocr 和其他系统保持一致
3. **正确性**：比对结果的页码准确无误
4. **可维护性**：代码中明确注释了转换逻辑

记住：**MinerU 的 page_idx 是 0-based，但 PageLayout.page 必须是 1-based！**

