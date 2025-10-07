# MinerU 坐标缩放比例修复

## 问题发现

**症状**：bbox 坐标偏大，x 和 y 都不准确

**根本原因**：缩放比例计算错误

## MinerU 坐标系统的真相

### ❌ 错误的理解（修复前）

我们以为 MinerU 返回的坐标是基于 PDF 页面尺寸：
```
PDF: 595 x 842 点
MinerU bbox: [100, 200, 500, 400]
缩放: scale = imageSize / pdfSize
```

### ✅ 正确的理解（修复后）

**MinerU 使用"高度归一化为 1000"的坐标系统**：

```
MinerU 坐标系统:
- 高度固定: 1000
- 宽度按比例: 保持与原 PDF 相同的宽高比

示例（A4 纸张）:
- PDF 尺寸: 595 x 842 点
- 宽高比: 595/842 = 0.707
- MinerU 坐标空间: 707 x 1000
```

## 修复前后对比

### 修复前（错误）

```java
// ❌ 错误：基于 PDF 尺寸计算
double scaleX = imageWidth / pdfWidth;   // 1322 / 595 = 2.222
double scaleY = imageHeight / pdfHeight; // 1871 / 842 = 2.222
```

**问题**：
- 假设 MinerU 坐标范围是 0-595 (x) 和 0-842 (y)
- 实际上 MinerU 坐标范围是 0-707 (x) 和 0-1000 (y)
- 导致缩放后的坐标偏大

### 修复后（正确）

```java
// ✅ 正确：基于 MinerU 归一化高度
final double MINERU_NORMALIZED_HEIGHT = 1000.0;
double scale = imageHeight / MINERU_NORMALIZED_HEIGHT; // 1871 / 1000 = 1.871

// X 和 Y 使用相同的缩放比例
double scaleX = scale;  // 1.871
double scaleY = scale;  // 1.871
```

**原理**：
- MinerU 的高度归一化为 1000
- 图片高度是 1871 像素
- 缩放比例 = 1871 / 1000 = 1.871
- X 和 Y 使用相同比例（保持宽高比）

## 实际例子

### 例子 1：A4 纸张

**PDF 尺寸**：595 x 842 点

**图片尺寸**：1322 x 1871 像素 (200 DPI)

**MinerU bbox**：`[100, 200, 600, 400]`

#### 修复前（错误）

```
scaleX = 1322 / 595 = 2.222
scaleY = 1871 / 842 = 2.222

转换后:
x1 = 100 * 2.222 = 222  ✅ 看起来还行
y1 = 200 * 2.222 = 444  ❌ 太大了！
x2 = 600 * 2.222 = 1333 ❌ 超出图片宽度了！
y2 = 400 * 2.222 = 888  ✅ 看起来还行
```

**问题**：x2 = 1333 超过了图片宽度 1322！

#### 修复后（正确）

```
scale = 1871 / 1000 = 1.871

转换后:
x1 = 100 * 1.871 = 187  ✅ 正确
y1 = 200 * 1.871 = 374  ✅ 正确
x2 = 600 * 1.871 = 1123 ✅ 在图片范围内
y2 = 400 * 1.871 = 748  ✅ 正确
```

### 例子 2：验证宽高比

**MinerU 坐标空间**：
```
宽高比 = 595 / 842 = 0.707
MinerU 宽度 = 1000 * 0.707 = 707
MinerU 坐标空间: 707 x 1000
```

**如果 MinerU bbox 的最大值接近 707 和 1000**：
```
bbox: [0, 0, 707, 1000]  // 整个页面

转换后:
x2 = 707 * 1.871 = 1323 ≈ 1322 (图片宽度) ✅
y2 = 1000 * 1.871 = 1871 (图片高度) ✅
```

完美匹配！

## 为什么 MinerU 使用高度 1000？

### 优点

1. **统一标准**：不管 PDF 是 A4、Letter 还是其他尺寸，高度都是 1000
2. **易于处理**：整数坐标，避免浮点精度问题
3. **保持比例**：宽高比不变，只是归一化了尺寸

### 对比其他方案

| 方案 | 优点 | 缺点 |
|------|------|------|
| PDF 原始尺寸 | 精确 | 不同页面尺寸不一致 |
| **归一化高度 1000** | **统一标准** | **需要知道这个规则** |
| 归一化 0-1 | 数学上优雅 | 浮点精度问题 |
| 像素坐标 | 直接使用 | 依赖渲染 DPI |

## 代码变更

### MinerUCoordinateConverter.java

```java
/**
 * MinerU 使用高度归一化为 1000 的坐标系统
 * 缩放比例 = 图片高度 / 1000
 */
public static int[] convertToImageCoordinates(
        double[] mineruBbox,
        double pdfWidth,
        double pdfHeight,
        int imageWidth,
        int imageHeight) {
    
    // MinerU 的归一化高度
    final double MINERU_NORMALIZED_HEIGHT = 1000.0;
    
    // 计算缩放比例（X 和 Y 相同，保持宽高比）
    double scale = imageHeight / MINERU_NORMALIZED_HEIGHT;
    
    // 转换坐标
    int[] imageBbox = new int[4];
    imageBbox[0] = (int) Math.round(mineruBbox[0] * scale);
    imageBbox[1] = (int) Math.round(mineruBbox[1] * scale);
    imageBbox[2] = (int) Math.round(mineruBbox[2] * scale);
    imageBbox[3] = (int) Math.round(mineruBbox[3] * scale);
    
    return imageBbox;
}
```

## 验证方法

### 1. 查看 MinerU bbox 最大值

```bash
# 查看 content_list 中 bbox 的最大值
jq '[.[].bbox[2]] | max' mineru_content_list_old.json  # x 最大值
jq '[.[].bbox[3]] | max' mineru_content_list_old.json  # y 最大值
```

**预期**：
- x 最大值应该接近 `1000 * (pdfWidth / pdfHeight)`
- y 最大值应该接近 `1000`

**示例**（A4 纸）：
```
x 最大值: ~707 (= 1000 * 595/842)
y 最大值: ~1000
```

### 2. 查看日志输出

```
坐标转换 - PDF尺寸: 595.0x842.0, 图片尺寸: 1322x1871, MinerU归一化高度: 1000.0, 缩放比例: scale=1.871
MinerU原始bbox: [100, 200, 600, 400]
转换后图片bbox: [187, 374, 1123, 748]
```

**检查**：
- scale 应该等于 `imageHeight / 1000`
- 转换后的坐标不应超出图片范围

### 3. 对比 layout.pdf

MinerU 生成的 `*_layout.pdf` 中的标记位置应该与我们的一致。

## 不同 DPI 的计算

### 200 DPI (A4)

```
PDF: 595 x 842 点
图片: 1653 x 2339 像素
scale = 2339 / 1000 = 2.339
```

### 300 DPI (A4)

```
PDF: 595 x 842 点
图片: 2480 x 3508 像素
scale = 3508 / 1000 = 3.508
```

### Letter 纸张 (200 DPI)

```
PDF: 612 x 792 点
图片: 1700 x 2200 像素
scale = 2200 / 1000 = 2.2
```

## 常见问题

### Q1: 为什么不用 imageWidth / 1000？

**A**: 因为 MinerU 是基于**高度**归一化，不是宽度。宽度是根据宽高比自动计算的。

### Q2: 如果 PDF 是横向的怎么办？

**A**: MinerU 始终使用高度 1000。如果 PDF 是横向（宽 > 高），那么：
```
假设 PDF: 842 x 595 (横向 A4)
MinerU: 1415 x 1000
scale = imageHeight / 1000
```

### Q3: scaleX 和 scaleY 为什么相同？

**A**: 因为要保持宽高比。MinerU 的归一化是等比例的，不是独立缩放 X 和 Y。

## 总结

### 核心发现

**MinerU 坐标 = 基于高度 1000 的归一化坐标**

### 正确的缩放公式

```java
scale = imageHeight / 1000.0
scaleX = scaleY = scale
```

### 修复效果

- ✅ bbox 坐标准确
- ✅ 不会超出图片范围
- ✅ 与 MinerU 的 layout.pdf 一致
- ✅ 前端显示正确

---

**更新日期**: 2025-10-07  
**状态**: ✅ 已修复

**关键公式**: `scale = imageHeight / 1000`

