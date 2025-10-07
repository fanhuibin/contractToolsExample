# MinerU 坐标系统修复

## 问题描述

**症状**：比对结果的 bbox 标记不对，x 和 y 都偏大了

**根本原因**：Y 轴坐标没有进行翻转转换

## 坐标系统差异

### PDF 坐标系（MinerU 返回的坐标）

根据 [MinerU 官方文档](https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1)：

```
Y 轴 ↑
     │
     │  ┌────────────┐
     │  │            │
     │  │   内容     │  
     │  │            │
     │  └────────────┘
     │              
     └──────────────→ X 轴
   (0,0)
   原点在左下角
   
坐标单位：点（1/72 英寸）
```

**特点**：
- ✅ 原点在**左下角**
- ✅ X 轴向右
- ✅ Y 轴**向上**
- ✅ 单位是**点**（PDF 标准单位）

### 图片坐标系（我们需要的坐标）

```
   (0,0)
   原点在左上角
     └──────────────→ X 轴
     │              
     │  ┌────────────┐
     │  │            │
     │  │   内容     │  
     │  │            │
     │  └────────────┘
     ↓
   Y 轴

坐标单位：像素
```

**特点**：
- ✅ 原点在**左上角**
- ✅ X 轴向右
- ✅ Y 轴**向下**
- ✅ 单位是**像素**

## 问题示例

### 修复前（错误）

假设 PDF 页面高度 842 点，MinerU 返回的 bbox：

```json
{
  "bbox": [100, 700, 500, 800],
  "text": "测试文本"
}
```

**错误的转换**（只缩放，不翻转）：
```java
// ❌ 错误：直接缩放 Y 坐标
y1 = bbox[1] * scaleY;  // 700 * scale
y2 = bbox[3] * scaleY;  // 800 * scale
```

**结果**：
- 文本应该在页面顶部（距底部 700-800 点）
- 但被标记在页面底部（距顶部 700-800 像素）
- **位置完全错误！**

### 修复后（正确）

**正确的转换**（缩放 + 翻转）：
```java
// ✅ 正确：Y 轴翻转
y1 = (pdfHeight - bbox[3]) * scaleY;  // (842 - 800) * scale = 42 * scale
y2 = (pdfHeight - bbox[1]) * scaleY;  // (842 - 700) * scale = 142 * scale
```

**结果**：
- 文本正确标记在页面顶部
- 距顶部 42-142 像素
- **位置正确！**

## 坐标转换公式

### X 轴转换（简单）

X 轴方向相同，只需缩放：

```java
x1_image = x1_pdf * scaleX
x2_image = x2_pdf * scaleX

其中 scaleX = imageWidth / pdfWidth
```

### Y 轴转换（需要翻转）

Y 轴方向相反，需要翻转 + 缩放：

```java
// PDF bbox: [x1, y1, x2, y2]
// y1 = 底边距离底部的高度
// y2 = 顶边距离底部的高度

// 图片 bbox: [x1, y1, x2, y2]
// y1 = 顶边距离顶部的高度
// y2 = 底边距离顶部的高度

y1_image = (pdfHeight - y2_pdf) * scaleY  // PDF的顶边 → 图片的顶边
y2_image = (pdfHeight - y1_pdf) * scaleY  // PDF的底边 → 图片的底边

其中 scaleY = imageHeight / pdfHeight
```

## 详细转换示例

### 示例1：页面顶部的文本

**PDF 页面**：
- 宽度：595 点
- 高度：842 点

**渲染图片**：
- 宽度：2480 像素（renderDpi = 300）
- 高度：3508 像素

**MinerU 返回的 bbox**（PDF 坐标）：
```json
[100, 700, 500, 800]
```

**含义**（PDF 坐标系）：
- x1=100：左边距左侧 100 点
- y1=700：底边距底部 700 点
- x2=500：右边距左侧 500 点
- y2=800：顶边距底部 800 点

**转换过程**：

```java
scaleX = 2480 / 595 = 4.168...
scaleY = 3508 / 842 = 4.166...

// X 坐标（直接缩放）
x1 = 100 * 4.168 = 417
x2 = 500 * 4.168 = 2084

// Y 坐标（翻转 + 缩放）
y1 = (842 - 800) * 4.166 = 42 * 4.166 = 175   // 顶边
y2 = (842 - 700) * 4.166 = 142 * 4.166 = 592  // 底边

// 最终图片坐标
[417, 175, 2084, 592]
```

**验证**：
- 文本宽度：2084 - 417 = 1667 像素 ✅
- 文本高度：592 - 175 = 417 像素 ✅
- 顶边距顶部：175 像素（接近页面顶部）✅

### 示例2：页面底部的文本

**MinerU 返回的 bbox**（PDF 坐标）：
```json
[100, 50, 500, 100]
```

**含义**（PDF 坐标系）：
- 底边距底部 50 点（接近页面底部）
- 顶边距底部 100 点

**转换过程**：

```java
// Y 坐标（翻转 + 缩放）
y1 = (842 - 100) * 4.166 = 742 * 4.166 = 3091  // 顶边
y2 = (842 - 50) * 4.166 = 792 * 4.166 = 3300   // 底边

// 最终图片坐标
[417, 3091, 2084, 3300]
```

**验证**：
- 底边距底部：3508 - 3300 = 208 像素（接近页面底部）✅

## 修复的代码

### 修复前

```java
public static int[] convertToImageCoordinates(
        double[] mineruBbox,
        double pdfWidth,
        double pdfHeight,
        int imageWidth,
        int imageHeight) {
    
    double scaleX = imageWidth / pdfWidth;
    double scaleY = imageHeight / pdfHeight;
    
    int[] imageBbox = new int[4];
    // ❌ 错误：Y 坐标直接缩放，没有翻转
    imageBbox[0] = (int) Math.round(mineruBbox[0] * scaleX);
    imageBbox[1] = (int) Math.round(mineruBbox[1] * scaleY);  // 错误！
    imageBbox[2] = (int) Math.round(mineruBbox[2] * scaleX);
    imageBbox[3] = (int) Math.round(mineruBbox[3] * scaleY);  // 错误！
    
    return imageBbox;
}
```

### 修复后

```java
public static int[] convertToImageCoordinates(
        double[] mineruBbox,
        double pdfWidth,
        double pdfHeight,
        int imageWidth,
        int imageHeight) {
    
    // 计算缩放比例
    double scaleX = imageWidth / pdfWidth;
    double scaleY = imageHeight / pdfHeight;
    
    // X坐标：直接缩放
    int x1 = (int) Math.round(mineruBbox[0] * scaleX);
    int x2 = (int) Math.round(mineruBbox[2] * scaleX);
    
    // ✅ 正确：Y坐标需要翻转
    // PDF的y1是底边，y2是顶边
    // 图片的y1是顶边，y2是底边
    int y1 = (int) Math.round((pdfHeight - mineruBbox[3]) * scaleY);  // PDF顶边 → 图片顶边
    int y2 = (int) Math.round((pdfHeight - mineruBbox[1]) * scaleY);  // PDF底边 → 图片底边
    
    int[] imageBbox = new int[4];
    imageBbox[0] = x1;
    imageBbox[1] = y1;
    imageBbox[2] = x2;
    imageBbox[3] = y2;
    
    return imageBbox;
}
```

## 测试验证

### 验证方法

1. **查看比对结果的 bbox 标记**
   - 高亮框应该准确覆盖文本
   - 不应该偏移或错位

2. **检查页面顶部和底部的文本**
   - 顶部文本的 y1 应该接近 0
   - 底部文本的 y2 应该接近 imageHeight

3. **对比 MinerU 的 layout.pdf**
   - 可视化文件中的标记位置
   - 与我们的标记位置对比

### 测试用例

```java
@Test
public void testCoordinateConversion() {
    // PDF: 595 x 842 点
    // 图片: 2480 x 3508 像素
    
    // 测试1：顶部文本
    double[] pdfBbox1 = {100, 700, 500, 800};
    int[] imgBbox1 = MinerUCoordinateConverter.convertToImageCoordinates(
        pdfBbox1, 595, 842, 2480, 3508);
    
    // 验证：y1 应该接近 0（顶部）
    assertTrue(imgBbox1[1] < 300);  // 顶边接近页面顶部
    
    // 测试2：底部文本
    double[] pdfBbox2 = {100, 50, 500, 100};
    int[] imgBbox2 = MinerUCoordinateConverter.convertToImageCoordinates(
        pdfBbox2, 595, 842, 2480, 3508);
    
    // 验证：y2 应该接近 imageHeight（底部）
    assertTrue(imgBbox2[3] > 3200);  // 底边接近页面底部
}
```

## 常见问题

### Q1：为什么 X 坐标不需要翻转？

**A**：因为 PDF 和图片的 X 轴方向相同（都是从左到右），只需要缩放。

### Q2：为什么 Y 坐标要用 `pdfHeight - y` ？

**A**：因为 PDF 的 Y 轴从底部向上，图片的 Y 轴从顶部向下，是相反的方向。

示意图：
```
PDF:      0 ─────────→ 842 (从底到顶)
图片:  3508 ←───────── 0   (从底到顶，需要翻转)

所以: 图片Y = pdfHeight - PDF_Y
```

### Q3：bbox 的 [x1, y1, x2, y2] 是什么含义？

**PDF bbox**:
- x1, y1: 左下角
- x2, y2: 右上角

**图片 bbox**:
- x1, y1: 左上角
- x2, y2: 右下角

**注意**：这就是为什么需要翻转！

## 影响范围

### 受影响的功能

- ✅ 比对结果的差异标记
- ✅ 前端高亮显示
- ✅ bbox 可视化
- ✅ 所有依赖坐标的功能

### 修复后的改进

- ✅ bbox 标记准确
- ✅ 高亮框位置正确
- ✅ 与 MinerU 的 layout.pdf 一致
- ✅ 前端显示正确

## 参考资料

1. **MinerU 官方文档**
   - [输出文件格式](https://opendatalab.github.io/MinerU/zh/reference/output_files/#content_listjson_1)
   - 坐标系统说明

2. **PDF 坐标系标准**
   - PDF 使用 PostScript 坐标系
   - 原点在左下角
   - 单位是点（1/72 英寸）

3. **图片坐标系标准**
   - 大多数图像格式
   - 原点在左上角
   - 单位是像素

## 相关问题

- GitHub Issue: [MinerU #1240 - bbox 坐标问题](https://github.com/opendatalab/MinerU/issues/1240)

## 更新日期

2025-10-07

## 修复状态

✅ 已修复 - Y 轴翻转转换已实现

## 总结

**问题**：bbox 的 x 和 y 都偏大
**原因**：Y 轴坐标没有翻转（PDF 向上，图片向下）
**修复**：添加 Y 轴翻转公式 `y_image = (pdfHeight - y_pdf) * scaleY`
**结果**：bbox 标记准确，位置正确

---

**关键公式**：
```java
// X 轴：直接缩放
x_image = x_pdf * scaleX

// Y 轴：翻转 + 缩放
y_image = (pdfHeight - y_pdf) * scaleY
```

**记住**：PDF 的 Y 轴**向上**，图片的 Y 轴**向下**！

