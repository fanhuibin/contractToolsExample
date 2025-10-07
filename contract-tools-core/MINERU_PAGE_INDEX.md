# 📄 MinerU 页码索引说明

## 📋 核心要点

**MinerU 的 `page_idx` 从 0 开始，与Java数组索引一致**

---

## 🔢 页码索引对照表

| MinerU `page_idx` | PDF实际页码 | 用户看到的 |
|------------------|-----------|----------|
| 0 | 第1页 | "第1页" |
| 1 | 第2页 | "第2页" |
| 2 | 第3页 | "第3页" |
| ... | ... | ... |

---

## 💻 代码中的处理

### 1. 内部使用（保持0开始）

所有内部数据结构和计算使用从0开始的索引：

```java
// MinerU返回的page_idx（从0开始）
int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;

// 使用0开始的索引访问数组
pageImages.get(pageIdx)           // ✅ 正确
pdfPageSizes.get(pageIdx)         // ✅ 正确
pageData.put(pageIdx, ...)        // ✅ 正确

// CharBox构造函数也使用0开始的索引
new CharBox(pageIdx, ch, bbox, type)  // ✅ 正确
```

### 2. 日志显示（转换为1开始）

当需要向用户展示时，转换为从1开始：

```java
// ✅ 正确：日志中显示为"第1页"而不是"第0页"
log.info("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页", pageIdx + 1);

// ✅ 正确：错误消息中显示为"第1页"
throw new Exception("第" + (pageIdx + 1) + "页识别失败");
```

### 3. 前端显示（通常保持0开始）

前端JavaScript通常也使用0开始的索引：

```javascript
// pages数组使用0开始的索引
pages[0]  // 第1页
pages[1]  // 第2页

// 但显示给用户时加1
<div>第{index + 1}页</div>
```

---

## 📊 示例场景

### 场景1：处理6页PDF

**MinerU返回**：
```json
{
  "content_list": [
    {"page_idx": 0, "text": "第1页内容"},
    {"page_idx": 1, "text": "第2页内容"},
    {"page_idx": 2, "text": "第3页内容"},
    {"page_idx": 3, "text": "第4页内容"},
    {"page_idx": 4, "text": "第5页内容"},
    {"page_idx": 5, "text": "第6页内容"}
  ]
}
```

**内部存储**：
```java
Map<Integer, List<Map<String, Object>>> pageData = new HashMap<>();
pageData.put(0, page0Data);  // ✅ 第1页
pageData.put(1, page1Data);  // ✅ 第2页
pageData.put(2, page2Data);  // ✅ 第3页
pageData.put(3, page3Data);  // ✅ 第4页
pageData.put(4, page4Data);  // ✅ 第5页
pageData.put(5, page5Data);  // ✅ 第6页
```

**日志输出**：
```
🚫 过滤 MinerU 识别的页眉页脚 - 第1页, 类型:header  ← pageIdx=0，显示为"第1页"
🚫 过滤 MinerU 识别的页眉页脚 - 第2页, 类型:header  ← pageIdx=1，显示为"第2页"
```

### 场景2：图片生成

**代码**：
```java
for (int i = 0; i < totalPages; i++) {
    PDPage page = document.getPage(i);  // ✅ PDFBox也是从0开始
    
    String fileName = String.format("page-%d.png", i + 1);  // ✅ 文件名从1开始
    // 生成 page-1.png, page-2.png, ...
    
    Map<String, Object> pageInfo = new HashMap<>();
    pageInfo.put("pageIndex", i);  // ✅ 内部索引从0开始
    pageInfo.put("imagePath", ...);
    
    log.debug("生成页面图片: {}, 页码索引: {}", fileName, i);  
    // 输出: "生成页面图片: page-1.png, 页码索引: 0"
}
```

---

## ⚠️ 常见错误

### ❌ 错误1：日志中显示page_idx

```java
// ❌ 错误：显示 "第0页"
log.info("处理第{}页", pageIdx);

// ✅ 正确：显示 "第1页"
log.info("处理第{}页", pageIdx + 1);
```

### ❌ 错误2：访问数组时加1

```java
// ❌ 错误：会导致越界
pageImages.get(pageIdx + 1)  // 如果pageIdx=5（最后一页），会访问索引6

// ✅ 正确：直接使用pageIdx
pageImages.get(pageIdx)
```

### ❌ 错误3：文件名使用0开始

```java
// ❌ 错误：生成 page-0.png, page-1.png
String fileName = String.format("page-%d.png", pageIdx);

// ✅ 正确：生成 page-1.png, page-2.png
String fileName = String.format("page-%d.png", pageIdx + 1);
```

---

## 📂 文件命名约定

### 图片文件

```
images/old/
  ├── page-1.png   ← pageIdx=0
  ├── page-2.png   ← pageIdx=1
  ├── page-3.png   ← pageIdx=2
  └── ...
```

**代码**：
```java
File imageFile = new File(imagesDir, String.format("page-%d.png", i + 1));
```

### JSON文件

```
ocr/
  ├── mineru_raw_old.json              ← 不带页码
  ├── mineru_processed_old_filtered.json
  └── ...
```

JSON中的page_idx保持0开始：
```json
{
  "pageData": {
    "0": [...],   ← 第1页
    "1": [...],   ← 第2页
    "2": [...]    ← 第3页
  }
}
```

---

## 🔍 调试技巧

### 打印关键索引

```java
log.debug("MinerU page_idx: {}, 实际页码: 第{}页, 数组索引: {}", 
    item.get("page_idx").asInt(),      // 0
    item.get("page_idx").asInt() + 1,  // 1
    item.get("page_idx").asInt()       // 0
);
```

### 验证索引范围

```java
int pageIdx = item.get("page_idx").asInt();
if (pageIdx < 0 || pageIdx >= totalPages) {
    log.error("❌ 无效的page_idx: {}, 总页数: {}", pageIdx, totalPages);
    throw new IllegalArgumentException("page_idx out of range");
}
```

---

## ✅ 最佳实践总结

### DO ✅

1. **内部计算使用0开始**
   ```java
   int pageIdx = item.get("page_idx").asInt();
   CharBox box = new CharBox(pageIdx, ...);
   ```

2. **日志显示加1**
   ```java
   log.info("处理第{}页", pageIdx + 1);
   ```

3. **文件名加1**
   ```java
   String fileName = String.format("page-%d.png", pageIdx + 1);
   ```

4. **数组访问直接用**
   ```java
   pageImages.get(pageIdx);  // 不要加1
   ```

### DON'T ❌

1. **不要在内部索引加1**
   ```java
   pageData.put(pageIdx + 1, data);  // ❌ 错误
   ```

2. **不要在数组访问时加1**
   ```java
   pageImages.get(pageIdx + 1);  // ❌ 错误，会越界
   ```

3. **不要混淆0索引和1索引**
   ```java
   // ❌ 错误：前面是0，后面是1
   for (int i = 0; i < totalPages; i++) {
       process(i + 1);  // 会跳过第1页（索引0）
   }
   ```

---

## 📚 相关代码位置

### MinerUOCRService.java

```java
// 行259-260：获取page_idx（从0开始）
int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;

// 行276：日志显示（转换为1开始）
log.info("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页", pageIdx + 1);

// 行284-285：数据访问（使用0开始）
pageImages.get(pageIdx)
pdfPageSizes.get(pageIdx)
```

### CompareService.java

```java
// 行3720-3724：遍历页面（0开始）
for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
    List<Map<String, Object>> pageItems = pageData.get(pageIdx);
    // ...
}
```

---

**关键原则**: **内部0，展示1**

- **内部处理**: 始终使用从0开始的索引（与Java数组、MinerU一致）
- **用户展示**: 转换为从1开始的页码（符合用户习惯）

---

**最后更新**: 2025-10-07  
**状态**: ✅ 已明确规范

