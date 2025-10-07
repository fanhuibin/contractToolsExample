# 📝 MinerU列表项（listItems）支持

## 📋 问题背景

MinerU识别结果中，列表类型的内容会使用 `listItems` 字段，而不是单纯的 `text` 字段。

### MinerU返回的列表格式

```json
{
  "listItems": [
    "2、货到甲方后，甲方按送货单内容收货，确认产品符合要求后甲方在验收单上签字确认...",
    "3、乙方应保证所提供产品为报价单中所规定之原厂产品，质量要符合报价单中规定的标准..."
  ],
  "bbox": [320, 471, 1322, 1037],
  "type": "list"
}
```

### 普通文本格式（对比）

```json
{
  "bbox": [320, 202, 1322, 420],
  "text": "或以订单甲方要求时间为准，如遇采购方有急用商品订单...",
  "type": "text"
}
```

---

## ✅ 实现方案

### 1. MinerUOCRService - 提取listItems

**文件**: `MinerUOCRService.java`

**代码位置**: `convertMinerUToCharBox()` 方法

```java
// 提取list_items（如果是列表类型）
if (item.has("list_items")) {
    JsonNode listItemsNode = item.get("list_items");
    List<String> listItems = new ArrayList<>();
    if (listItemsNode.isArray()) {
        for (JsonNode listItem : listItemsNode) {
            listItems.add(listItem.asText());
        }
    }
    charBox.put("listItems", listItems);
}
```

**输出** (`mineru_processed_*_*.json`):
```json
{
  "bbox": [320, 471, 1322, 1037],
  "type": "list",
  "listItems": [
    "2、货到甲方后...",
    "3、乙方应保证..."
  ]
}
```

### 2. CompareService - 展开listItems为CharBox

**文件**: `CompareService.java`

**方法**: `convertToCharBoxList(Map<String, Object> item, int pageIdx)`

#### 处理流程

```
列表类型数据
    ↓
检测到 listItems 字段
    ↓
计算每个列表项的垂直位置（平均分配）
    ↓
为每个列表项创建独立的CharBox
    ↓
将每个列表项拆分为单个字符
    ↓
最终生成多个CharBox用于比对
```

#### 代码实现

```java
private List<CharBox> convertToCharBoxList(Map<String, Object> item, int pageIdx) {
    List<CharBox> charBoxes = new ArrayList<>();
    
    // 检查是否有listItems（列表类型）
    @SuppressWarnings("unchecked")
    List<String> listItems = (List<String>) item.get("listItems");
    
    if (listItems != null && !listItems.isEmpty()) {
        // 处理列表类型：展开每个列表项
        int[] bbox = (int[]) item.get("bbox");
        
        // 计算每个列表项的大致高度
        double totalHeight = bbox[3] - bbox[1];
        double itemHeight = totalHeight / listItems.size();
        
        // 为每个列表项创建CharBox
        for (int itemIdx = 0; itemIdx < listItems.size(); itemIdx++) {
            String itemText = listItems.get(itemIdx);
            
            // 计算列表项的bbox（垂直方向平均分配）
            int[] itemBbox = new int[4];
            itemBbox[0] = bbox[0];                           // x1相同
            itemBbox[1] = (int) (bbox[1] + itemIdx * itemHeight);  // y1
            itemBbox[2] = bbox[2];                           // x2相同
            itemBbox[3] = (int) (bbox[1] + (itemIdx + 1) * itemHeight);  // y2
            
            // 将列表项拆分为字符
            charBoxes.addAll(splitTextToCharBoxes(itemText, itemBbox, pageIdx));
        }
        
        return charBoxes;
    }
    
    // 处理普通文本（text字段）
    String text = (String) item.get("text");
    int[] bbox = (int[]) item.get("bbox");
    charBoxes.addAll(splitTextToCharBoxes(text, bbox, pageIdx));
    
    return charBoxes;
}
```

### 3. 辅助方法 - splitTextToCharBoxes

将文本块拆分为单个字符的CharBox：

```java
private List<CharBox> splitTextToCharBoxes(String text, int[] bbox, int pageIdx) {
    List<CharBox> charBoxes = new ArrayList<>();
    
    // 计算每个字符的平均宽度
    double totalWidth = bbox[2] - bbox[0];
    int charCount = text.length();
    double avgCharWidth = totalWidth / charCount;
    
    // 为每个字符创建CharBox
    for (int i = 0; i < charCount; i++) {
        char ch = text.charAt(i);
        
        // 计算字符位置（简化处理：平均分配）
        double x1 = bbox[0] + (i * avgCharWidth);
        double x2 = bbox[0] + ((i + 1) * avgCharWidth);
        double y1 = bbox[1];
        double y2 = bbox[3];
        
        double[] charBbox = new double[]{x1, y1, x2, y2};
        CharBox charBox = new CharBox(pageIdx, ch, charBbox, "text");
        charBoxes.add(charBox);
    }
    
    return charBoxes;
}
```

---

## 📊 数据转换示例

### 输入（MinerU返回）

```json
{
  "listItems": [
    "2、货到甲方后，甲方按送货单内容收货。",
    "3、乙方应保证所提供产品为原厂产品。"
  ],
  "bbox": [320, 471, 1322, 1037],
  "type": "list"
}
```

**分析**:
- 总高度: 1037 - 471 = 566 像素
- 列表项数量: 2
- 每项高度: 566 / 2 = 283 像素

### 中间处理（垂直分割）

**列表项1**:
```
text: "2、货到甲方后，甲方按送货单内容收货。"
bbox: [320, 471, 1322, 754]  ← 471 + 283
```

**列表项2**:
```
text: "3、乙方应保证所提供产品为原厂产品。"
bbox: [320, 754, 1322, 1037]  ← 754 + 283
```

### 输出（CharBox列表）

**列表项1拆分**:
```
CharBox('2', [320, 471, 352, 754], pageIdx=0)
CharBox('、', [352, 471, 384, 754], pageIdx=0)
CharBox('货', [384, 471, 416, 754], pageIdx=0)
CharBox('到', [416, 471, 448, 754], pageIdx=0)
... (每个字符一个CharBox)
```

**列表项2拆分**:
```
CharBox('3', [320, 754, 352, 1037], pageIdx=0)
CharBox('、', [352, 754, 384, 1037], pageIdx=0)
CharBox('乙', [384, 754, 416, 1037], pageIdx=0)
CharBox('方', [416, 754, 448, 1037], pageIdx=0)
... (每个字符一个CharBox)
```

---

## 🎯 优势

### 1. 精确比对
- 每个列表项独立比对
- 可以检测到单个列表项的变化

### 2. 正确的位置信息
- 每个列表项有正确的垂直位置
- 前端可以准确高亮差异位置

### 3. 兼容性
- 同时支持普通文本（`text`）和列表（`listItems`）
- 不影响现有功能

---

## 🔍 调试验证

### 1. 检查JSON保存

查看 `mineru_processed_*_unfiltered.json`，确认listItems被正确提取：

```json
{
  "pageData": {
    "0": [
      {
        "text": "普通文本",
        "bbox": [320, 202, 1322, 420],
        "type": "text"
      },
      {
        "listItems": [
          "2、列表项1",
          "3、列表项2"
        ],
        "bbox": [320, 471, 1322, 1037],
        "type": "list"
      }
    ]
  }
}
```

### 2. 检查日志

```
MinerU识别完成: 5页, 753个CharBox  ← 包含展开后的列表项字符
```

### 3. 前端验证

- 列表项应该能够独立高亮
- 位置准确无误
- 比对结果正确

---

## ⚠️  限制和注意事项

### 1. 垂直位置估算

当前使用**平均分配**方法估算每个列表项的垂直位置：

```java
double itemHeight = totalHeight / listItems.size();
```

**限制**:
- 假设每个列表项高度相同
- 实际上列表项可能长短不一

**改进方向**:
- 可以根据文本长度按比例分配高度
- 未来MinerU可能会返回每个列表项的独立bbox

### 2. 水平位置

所有列表项共享相同的水平范围（x1, x2）。

### 3. 字符级拆分

字符位置使用平均宽度分配，可能不完全准确。

---

## 📈 测试场景

### 场景1：列表项新增

**旧版本**:
```
1、第一项
2、第二项
```

**新版本**:
```
1、第一项
2、第二项
3、第三项  ← 新增
```

**预期结果**: 检测到新增的列表项

### 场景2：列表项修改

**旧版本**:
```
1、原始内容
```

**新版本**:
```
1、修改后内容
```

**预期结果**: 检测到内容变化

### 场景3：列表项删除

**旧版本**:
```
1、第一项
2、第二项
3、第三项
```

**新版本**:
```
1、第一项
2、第二项
```

**预期结果**: 检测到删除的列表项

---

**最后更新**: 2025-10-07

