# 📦 MinerU CharBox 和 BBox 处理说明

## 🎯 核心原则

**与 dots.ocr 保持一致：文本拆分成字符，但所有字符共享相同的整体bbox**

---

## 📋 问题背景

### 用户反馈

**MinerU返回的数据**:
```json
{
  "bbox": [324, 1017, 1322, 1195],
  "text": "23 wxc 2025-03-06",
  "type": "text"
}
```

**错误的处理方式**（之前）:
```json
// ❌ 错误：每个字符有不同的bbox
[
  [324, 1017, 382, 1195],   // '2'
  [382, 1017, 441, 1195],   // '3'
  [441, 1017, 500, 1195],   // ' '
  // ... 每个字符bbox不同
]
```

**前端收到的文本**（之前）:
```json
{
  "allTextA": ["2", "3", " ", "w", "x", "c", " ", "2", "0", "2", "5", "-", "0", "3", "-", "0", "6"]
}
```

**问题**：
1. ❌ bbox被错误地拆分
2. ❌ 文本被拆成单个字符
3. ❌ 与dots.ocr行为不一致

---

## ✅ 正确的处理方式

### dots.ocr 的处理方式

**代码**: `TextExtractionUtil.java`

```java
// 保持每个字符所属的 bbox 等于布局项的整体 bbox，便于基于 bbox 的换行
for (int i = 0; i < s.length(); i++) {
    char ch = s.charAt(i);
    out.add(new CharBox(page, ch, it.bbox, it.category));
    //                          ^^^^^^^^ 所有字符使用相同的bbox！
}
```

### 修复后的 MinerU 处理方式

**代码**: `CompareService.java`

```java
/**
 * 【重要】与dots.ocr保持一致：每个字符使用相同的整体bbox
 */
private List<CharBox> splitTextToCharBoxes(String text, int[] bbox, int pageIdx) {
    List<CharBox> charBoxes = new ArrayList<>();
    
    // 转换为double[] bbox
    double[] charBbox = new double[]{
        (double) bbox[0],
        (double) bbox[1],
        (double) bbox[2],
        (double) bbox[3]
    };
    
    // 为每个字符创建CharBox，所有字符共享相同的bbox
    for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        CharBox charBox = new CharBox(pageIdx, ch, charBbox, "text");
        //                                        ^^^^^^^^ 相同的bbox
        charBoxes.add(charBox);
    }
    
    return charBoxes;
}
```

### 现在的结果

**CharBox列表**（内部）:
```
CharBox(page=3, ch='2', bbox=[324, 1017, 1322, 1195])
CharBox(page=3, ch='3', bbox=[324, 1017, 1322, 1195])  ← 相同的bbox
CharBox(page=3, ch=' ', bbox=[324, 1017, 1322, 1195])  ← 相同的bbox
CharBox(page=3, ch='w', bbox=[324, 1017, 1322, 1195])  ← 相同的bbox
...
CharBox(page=3, ch='6', bbox=[324, 1017, 1322, 1195])  ← 相同的bbox
```

**前端收到的数据**:
```json
{
  "allTextA": ["2", "3", " ", "w", "x", "c", " ", "2", "0", "2", "5", "-", "0", "3", "-", "0", "6"],
  "bbox": [324, 1017, 1322, 1195]  // ← 所有字符共享这个bbox
}
```

---

## 🤔 为什么要这样处理？

### 1. 字符级比对

**需要拆分成字符**：
```
旧文本: "23 wxc 2025-03-06"
新文本: "24 wxc 2025-03-07"  // 第2个字符和最后一个字符不同
```

拆分成字符后，可以精确定位差异：
```
字符索引0: '2' vs '2' ✅ 相同
字符索引1: '3' vs '4' ❌ 不同  ← 精确定位
字符索引2: ' ' vs ' ' ✅ 相同
...
字符索引16: '6' vs '7' ❌ 不同  ← 精确定位
```

### 2. 前端换行判断

**前端需要根据bbox判断换行**：

```javascript
// 前端逻辑
for (let i = 0; i < chars.length; i++) {
    if (i > 0 && chars[i].bbox !== chars[i-1].bbox) {
        // bbox不同，说明是新的文本块，可能需要换行
        addLineBreak();
    }
    renderChar(chars[i]);
}
```

**如果每个字符bbox都不同**（错误方式）:
```javascript
// ❌ 错误：每个字符都会换行！
"2\n3\n \nw\nx\nc\n ..."
```

**如果所有字符bbox相同**（正确方式）:
```javascript
// ✅ 正确：整个文本块在同一行
"23 wxc 2025-03-06"
```

### 3. 高亮显示

**前端高亮差异**：

```javascript
// 当某个字符被标记为差异
if (char.isDiff) {
    // 使用bbox绘制高亮框
    drawHighlight(char.bbox);  
    // ← 因为所有字符bbox相同，会高亮整个文本块
}
```

**效果**:
- ✅ 高亮整个日期块 "23 wxc 2025-03-06"
- ❌ 不会只高亮单个字符 "3"

---

## 📊 数据流对比

### dots.ocr 流程

```
OCR识别结果
  ↓
{text: "23 wxc 2025-03-06", bbox: [324, 1017, 1322, 1195]}
  ↓
拆分为字符（所有字符共享相同bbox）
  ↓
[
  CharBox(ch='2', bbox=[324, 1017, 1322, 1195]),
  CharBox(ch='3', bbox=[324, 1017, 1322, 1195]),
  ...
]
  ↓
前端显示为一个文本块
```

### MinerU 流程（修复后）

```
MinerU API结果
  ↓
{text: "23 wxc 2025-03-06", bbox: [324, 1017, 1322, 1195]}
  ↓
拆分为字符（所有字符共享相同bbox）← 与dots.ocr一致
  ↓
[
  CharBox(ch='2', bbox=[324, 1017, 1322, 1195]),
  CharBox(ch='3', bbox=[324, 1017, 1322, 1195]),
  ...
]
  ↓
前端显示为一个文本块 ✅
```

---

## 🔍 列表项处理

### 列表项也使用相同逻辑

```java
// 为每个列表项创建CharBox
for (int itemIdx = 0; itemIdx < listItems.size(); itemIdx++) {
    String itemText = listItems.get(itemIdx);
    
    // 计算列表项的bbox（垂直方向平均分配）
    int[] itemBbox = new int[4];
    itemBbox[0] = bbox[0];
    itemBbox[1] = (int) (bbox[1] + itemIdx * itemHeight);
    itemBbox[2] = bbox[2];
    itemBbox[3] = (int) (bbox[1] + (itemIdx + 1) * itemHeight);
    
    // 将列表项拆分为字符，所有字符共享itemBbox
    charBoxes.addAll(splitTextToCharBoxes(itemText, itemBbox, pageIdx));
    //                                                 ^^^^^^^^ 列表项的bbox
}
```

**示例**:

**列表项1**: "盖章之日起生效..."
```
CharBox(ch='盖', bbox=[320, 202, 1322, 404])  // 列表项1的bbox
CharBox(ch='章', bbox=[320, 202, 1322, 404])  // 相同
CharBox(ch='之', bbox=[320, 202, 1322, 404])  // 相同
...
```

**列表项2**: "4、本合同未尽事宜..."
```
CharBox(ch='4', bbox=[320, 404, 1322, 606])   // 列表项2的bbox（不同）
CharBox(ch='、', bbox=[320, 404, 1322, 606])  // 相同
CharBox(ch='本', bbox=[320, 404, 1322, 606])  // 相同
...
```

---

## ⚠️ 常见误解

### 误解1：bbox应该精确到每个字符

**错误想法**:
> "既然是字符级比对，每个字符应该有精确的bbox位置"

**实际情况**:
- OCR只能识别**文本块**的bbox，无法精确到每个字符
- 强行计算每个字符的位置（平均分配）是不准确的
- 前端需要**整体bbox**来判断文本块和换行

### 误解2：前端需要单个字符的bbox

**错误想法**:
> "前端显示需要每个字符的准确位置"

**实际情况**:
- 前端是基于**文本块**绘制的
- 字符拆分只是为了**比对差异**，不是为了单独显示
- 前端通过bbox判断**换行**和**布局**

### 误解3：MinerU返回的bbox应该拆分

**错误想法**:
> "MinerU返回的是整块bbox，应该平均分配给每个字符"

**实际情况**:
- MinerU的bbox就是**整个文本块**的范围
- 应该**保持完整**，不应该拆分
- 这与dots.ocr的处理方式一致

---

## ✅ 验证方法

### 1. 检查CharBox

```java
List<CharBox> charBoxes = splitTextToCharBoxes("23 wxc 2025-03-06", bbox, pageIdx);

// 验证：所有字符bbox应该相同
double[] firstBbox = charBoxes.get(0).bbox;
for (CharBox cb : charBoxes) {
    assert Arrays.equals(cb.bbox, firstBbox) : "所有字符应该共享相同的bbox";
}
```

### 2. 检查前端JSON

```javascript
// 前端收到的数据
{
  "blocks": [
    {
      "text": "23 wxc 2025-03-06",
      "chars": [
        {"ch": "2", "bbox": [324, 1017, 1322, 1195]},
        {"ch": "3", "bbox": [324, 1017, 1322, 1195]},  // ← bbox相同
        {"ch": " ", "bbox": [324, 1017, 1322, 1195]},  // ← bbox相同
        // ...
      ]
    }
  ]
}
```

### 3. 前端显示验证

- ✅ 文本显示为完整的一行
- ✅ 高亮整个文本块
- ✅ 不会出现单个字符独立显示
- ✅ 换行判断正确

---

## 📚 相关代码

### CharBox 定义

```java
public class CharBox {
    public final int page;         // 页码索引（从0开始）
    public final char ch;           // 单个字符
    public final double[] bbox;     // bbox（x1, y1, x2, y2）
    public final String category;   // 类型（text, list等）
}
```

### dots.ocr 处理

**文件**: `TextExtractionUtil.java`

```java
// 行434-438
for (int i = 0; i < s.length(); i++) {
    char ch = s.charAt(i);
    out.add(new CharBox(page, ch, it.bbox, it.category));
    //                          ^^^^^^^ 所有字符使用相同的bbox
}
```

### MinerU 处理（修复后）

**文件**: `CompareService.java`

```java
// 行3828-3834
for (int i = 0; i < text.length(); i++) {
    char ch = text.charAt(i);
    CharBox charBox = new CharBox(pageIdx, ch, charBbox, "text");
    //                                        ^^^^^^^^ 相同的bbox
    charBoxes.add(charBox);
}
```

---

## 🎯 总结

### 核心原则

1. **文本拆分成字符** - 用于字符级比对
2. **所有字符共享相同bbox** - 保持文本块的完整性
3. **与dots.ocr保持一致** - 确保前端兼容

### 关键点

- ✅ **不要**计算每个字符的单独bbox
- ✅ **不要**平均分配bbox宽度
- ✅ **使用**整个文本块的bbox
- ✅ **保持**与dots.ocr一致的行为

### 结果

- ✅ 前端显示正确
- ✅ 文本块保持完整
- ✅ 高亮显示正确
- ✅ 换行判断准确

---

**最后更新**: 2025-10-07  
**状态**: ✅ 已修复，与dots.ocr保持一致

