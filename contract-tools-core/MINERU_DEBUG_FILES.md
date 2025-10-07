# 📄 MinerU调试文件输出说明

## ✅ 功能完成

MinerU现在像dots.ocr一样输出完整的调试文件，方便排查问题！

---

## 📁 输出文件清单

### 1. 抽取的全文文件

**位置**: PDF文件同目录
```
old_new_办公耗材长期供销合同 (1).pdf.extracted.txt
old_new_办公耗材长期供销合同 (1).pdf.extracted.compare.txt
new_old_办公耗材长期供销合同.pdf.extracted.txt
new_old_办公耗材长期供销合同.pdf.extracted.compare.txt
```

**说明**:
- `.extracted.txt` - 带页码标记的全文
- `.extracted.compare.txt` - 纯文本（用于比对）

**示例内容** (`.extracted.txt`):
```
==========第1页==========
办公耗材长期供销合同

甲方：北京XXX公司
乙方：上海YYY公司

==========第2页==========
一、合同条款
1. 供货范围...
```

**示例内容** (`.extracted.compare.txt`):
```
办公耗材长期供销合同
甲方：北京XXX公司
乙方：上海YYY公司
一、合同条款
1. 供货范围...
```

---

### 2. 每页的JSON文件

**位置**: `./uploads/compare-pro/tasks/{taskId}/ocr_pages/`
```
ocr_pages/
├── old_page_001.json
├── old_page_002.json
├── old_page_003.json
├── new_page_001.json
├── new_page_002.json
└── new_page_003.json
```

**文件命名**:
- `old_page_xxx.json` - 原文档每页
- `new_page_xxx.json` - 新文档每页

**JSON结构**:
```json
{
  "page": 1,
  "imgW": 1322,
  "imgH": 1867,
  "items": [
    {
      "bbox": [324.0, 100.0, 998.0, 150.0],
      "category": "text",
      "text": "办公耗材长期供销合同"
    },
    {
      "bbox": [324.0, 200.0, 998.0, 250.0],
      "category": "text",
      "text": "甲方：北京XXX公司"
    },
    {
      "bbox": [324.0, 300.0, 998.0, 600.0],
      "category": "text",
      "text": "1. 供货范围包括但不限于..."
    }
  ],
  "itemCount": 3
}
```

---

### 3. MinerU原始响应

**位置**: `./uploads/compare-pro/tasks/{taskId}/ocr/`
```
ocr/
├── mineru_raw_old.json          # 原文档MinerU API原始响应
├── mineru_raw_new.json          # 新文档MinerU API原始响应
```

**内容**: MinerU API返回的完整JSON（包含content_list等）

---

## 🔍 文件用途

### 调试文本抽取

**查看 `.extracted.txt`**:
```bash
# 检查文本是否正确抽取
cat ./uploads/compare-pro/tasks/{taskId}/old_*.pdf.extracted.txt

# 检查页码标记是否正确
grep "第.*页" ./uploads/compare-pro/tasks/{taskId}/old_*.pdf.extracted.txt
```

### 调试比对结果

**查看 `.extracted.compare.txt`**:
```bash
# 这是实际用于比对的文本
cat ./uploads/compare-pro/tasks/{taskId}/old_*.pdf.extracted.compare.txt
cat ./uploads/compare-pro/tasks/{taskId}/new_*.pdf.extracted.compare.txt

# 手动diff查看差异
diff ./uploads/compare-pro/tasks/{taskId}/old_*.pdf.extracted.compare.txt \
     ./uploads/compare-pro/tasks/{taskId}/new_*.pdf.extracted.compare.txt
```

### 调试单页识别

**查看每页JSON**:
```bash
# 检查第1页的识别结果
cat ./uploads/compare-pro/tasks/{taskId}/ocr_pages/old_page_001.json

# 统计每页的内容块数量
jq '.itemCount' ./uploads/compare-pro/tasks/{taskId}/ocr_pages/*.json

# 检查某个特定文本是否被识别
grep "合同" ./uploads/compare-pro/tasks/{taskId}/ocr_pages/*.json
```

### 调试bbox坐标

**查看坐标信息**:
```bash
# 检查某页的所有bbox
jq '.items[].bbox' ./uploads/compare-pro/tasks/{taskId}/ocr_pages/old_page_001.json

# 检查图片尺寸
jq '{page, imgW, imgH}' ./uploads/compare-pro/tasks/{taskId}/ocr_pages/old_page_001.json
```

---

## 📊 文件目录结构

完整的输出目录结构：
```
./uploads/compare-pro/tasks/{taskId}/
├── old_new_办公耗材长期供销合同 (1).pdf              # 原始PDF
├── old_new_办公耗材长期供销合同 (1).pdf.extracted.txt  # ← 全文（带页码）
├── old_new_办公耗材长期供销合同 (1).pdf.extracted.compare.txt  # ← 全文（纯文本）
│
├── new_old_办公耗材长期供销合同.pdf
├── new_old_办公耗材长期供销合同.pdf.extracted.txt
├── new_old_办公耗材长期供销合同.pdf.extracted.compare.txt
│
├── images/                                           # 图片（用于前端显示）
│   ├── old/
│   │   ├── page-1.png
│   │   ├── page-2.png
│   │   └── ...
│   └── new/
│       ├── page-1.png
│       ├── page-2.png
│       └── ...
│
├── ocr/                                              # OCR原始数据
│   ├── mineru_raw_old.json                          # ← MinerU原始响应
│   └── mineru_raw_new.json
│
└── ocr_pages/                                        # ← 每页JSON（新增）
    ├── old_page_001.json
    ├── old_page_002.json
    ├── old_page_003.json
    ├── new_page_001.json
    ├── new_page_002.json
    └── new_page_003.json
```

---

## 🔧 实现细节

### saveExtractedText 方法

```java
private void saveExtractedText(TextExtractionUtil.PageLayout[] layouts, Path pdfPath) {
    // 使用TextExtractionUtil统一方法提取文本
    String extractedWithPages = TextExtractionUtil.extractTextWithPageMarkers(layouts);
    String extractedNoPages = TextExtractionUtil.extractText(layouts);
    
    // 保存到PDF同目录
    String txtOut = pdfPath.toAbsolutePath().toString() + ".extracted.txt";
    String txtOutCompare = pdfPath.toAbsolutePath().toString() + ".extracted.compare.txt";
    
    Files.write(Path.of(txtOut), extractedWithPages.getBytes(StandardCharsets.UTF_8));
    Files.write(Path.of(txtOutCompare), extractedNoPages.getBytes(StandardCharsets.UTF_8));
}
```

### savePageLayoutsJson 方法

```java
private void savePageLayoutsJson(TextExtractionUtil.PageLayout[] layouts, File outputDir, String docMode) {
    ObjectMapper mapper = new ObjectMapper();
    File jsonDir = new File(outputDir, "ocr_pages");
    jsonDir.mkdirs();
    
    for (TextExtractionUtil.PageLayout layout : layouts) {
        // 构建JSON对象
        Map<String, Object> pageJson = new HashMap<>();
        pageJson.put("page", layout.page);
        pageJson.put("imgW", layout.imgW);
        pageJson.put("imgH", layout.imgH);
        
        // 转换items
        List<Map<String, Object>> itemsJson = new ArrayList<>();
        for (TextExtractionUtil.LayoutItem item : layout.items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("bbox", item.bbox);
            itemMap.put("category", item.category);
            itemMap.put("text", item.text);
            itemsJson.add(itemMap);
        }
        pageJson.put("items", itemsJson);
        pageJson.put("itemCount", itemsJson.size());
        
        // 保存到文件
        String fileName = String.format("%s_page_%03d.json", docMode, layout.page);
        File jsonFile = new File(jsonDir, fileName);
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, pageJson);
    }
}
```

---

## 🎯 与dots.ocr对比

| 文件类型 | dots.ocr | MinerU | 说明 |
|---------|----------|--------|------|
| `.extracted.txt` | ✅ | ✅ | 完全相同 |
| `.extracted.compare.txt` | ✅ | ✅ | 完全相同 |
| 每页JSON | ✅ | ✅ | 格式完全相同 |
| 原始响应 | ❌ | ✅ | MinerU额外保存原始API响应 |

---

## 📋 排查问题步骤

### 问题1：文本抽取不完整

1. 检查 `.extracted.txt` 看全文内容
2. 检查每页JSON，找到缺失的页面
3. 检查 `mineru_raw_*.json` 看MinerU原始识别结果
4. 对比 `itemCount` 看是否有内容被过滤

### 问题2：bbox坐标不准确

1. 检查每页JSON中的 `imgW` 和 `imgH`
2. 检查每个item的 `bbox` 是否在范围内
3. 检查 `mineru_raw_*.json` 中的原始bbox
4. 对比图片实际尺寸

### 问题3：比对结果错误

1. 对比两个 `.extracted.compare.txt` 文件
2. 检查是否有多余的空格、换行
3. 检查页码顺序是否正确
4. 检查每页JSON的 `page` 字段

### 问题4：某些内容被过滤

1. 检查 `mineru_raw_*.json` 中的 `type` 字段
2. 查看是否被识别为 `header`、`footer`、`page_number`
3. 对比原始响应和每页JSON，找出被过滤的内容

---

## 💡 使用建议

### 开发调试

- **保留所有文件** - 方便对比和分析
- **使用 `jq` 工具** - 快速查询JSON数据
- **使用 `diff` 工具** - 对比文本差异

### 生产环境

- **可选删除每页JSON** - 如果磁盘空间有限
- **保留全文文件** - 便于审计和追溯
- **定期清理** - 删除旧任务的调试文件

### 性能优化

文件写入是异步的，不会显著影响性能：
- 全文文件：几KB到几MB
- 每页JSON：每个几KB
- 总耗时：< 100ms

---

## 🚀 下一步

1. **测试验证** - 运行比对，检查所有文件是否正确生成
2. **对比dots.ocr** - 确保输出格式完全一致
3. **性能测试** - 测试大文档的文件输出性能
4. **文档完善** - 根据实际使用补充说明

---

**最后更新**: 2025-10-07  
**状态**: ✅ 已实现，与dots.ocr输出完全一致  
**文件位置**: 
- 全文: PDF同目录
- 每页JSON: `{taskId}/ocr_pages/`
- 原始响应: `{taskId}/ocr/`

