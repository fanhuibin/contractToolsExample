# MinerU 1000x1000 归一化坐标系统

## 最终确认

**MinerU 使用 1000x1000 的正方形归一化坐标系统**

## 坐标系统说明

### MinerU 坐标空间

```
所有 PDF 页面都映射到固定的正方形空间：

┌─────────────┐
│             │
│  1000x1000  │  ← 固定大小
│             │
└─────────────┘

不管 PDF 是 A4、Letter 还是其他尺寸
都映射到这个 1000x1000 的空间
```

### 转换公式

```java
// MinerU bbox: [x1, y1, x2, y2] (0-1000 范围)
// 图片 bbox: 需要缩放到实际像素

scaleX = imageWidth / 1000
scaleY = imageHeight / 1000

x_image = x_mineru * scaleX
y_image = y_mineru * scaleY
```

## 示例

### A4 纸张 (595x842 点)

**200 DPI 渲染**：
```
图片尺寸: 1653 x 2339 像素

scaleX = 1653 / 1000 = 1.653
scaleY = 2339 / 1000 = 2.339

MinerU bbox: [100, 200, 600, 800]
转换后:
  x1 = 100 * 1.653 = 165
  y1 = 200 * 2.339 = 468
  x2 = 600 * 1.653 = 992
  y2 = 800 * 2.339 = 1871
```

**300 DPI 渲染**：
```
图片尺寸: 2480 x 3508 像素

scaleX = 2480 / 1000 = 2.480
scaleY = 3508 / 1000 = 3.508

MinerU bbox: [100, 200, 600, 800]
转换后:
  x1 = 100 * 2.480 = 248
  y1 = 200 * 3.508 = 702
  x2 = 600 * 2.480 = 1488
  y2 = 800 * 3.508 = 2806
```

### Letter 纸张 (612x792 点)

**200 DPI 渲染**：
```
图片尺寸: 1700 x 2200 像素

scaleX = 1700 / 1000 = 1.700
scaleY = 2200 / 1000 = 2.200

MinerU bbox: [100, 200, 900, 950]
转换后:
  x1 = 100 * 1.700 = 170
  y1 = 200 * 2.200 = 440
  x2 = 900 * 1.700 = 1530
  y2 = 950 * 2.200 = 2090
```

## 页眉页脚计算

页眉页脚的百分比计算也基于 1000x1000 坐标系：

```java
// 例如：页眉高度 10%
headerHeight = 1000 * 0.10 = 100  (MinerU 坐标)

// 判断是否在页眉区域
if (bbox.y1 <= 100 && bbox.y2 <= 100) {
    // 在页眉区域
}

// 页脚高度 10%
footerTop = 1000 * 0.90 = 900  (MinerU 坐标)

// 判断是否在页脚区域
if (bbox.y1 >= 900 && bbox.y2 <= 1000) {
    // 在页脚区域
}
```

**注意**：
- 页眉页脚的百分比是基于 MinerU 的 1000 高度
- 不是基于实际 PDF 高度或图片高度

## 优点

### 1. 统一标准

所有页面都使用相同的坐标空间，便于：
- 跨页面比较
- 统一的页眉页脚检测
- 简化算法

### 2. 独立缩放

X 和 Y 可以独立缩放，适应不同宽高比的页面：
- 竖向页面：scaleX < scaleY
- 横向页面：scaleX > scaleY
- 正方形页面：scaleX = scaleY

### 3. 整数坐标

使用整数 1000 避免浮点精度问题

## 代码实现

```java
public static int[] convertToImageCoordinates(
        double[] mineruBbox,
        double pdfWidth,
        double pdfHeight,
        int imageWidth,
        int imageHeight) {
    
    // MinerU 使用 1000x1000 正方形归一化
    final double MINERU_NORMALIZED_SIZE = 1000.0;
    
    // X 和 Y 分别独立缩放
    double scaleX = imageWidth / MINERU_NORMALIZED_SIZE;
    double scaleY = imageHeight / MINERU_NORMALIZED_SIZE;
    
    int[] imageBbox = new int[4];
    imageBbox[0] = (int) Math.round(mineruBbox[0] * scaleX);
    imageBbox[1] = (int) Math.round(mineruBbox[1] * scaleY);
    imageBbox[2] = (int) Math.round(mineruBbox[2] * scaleX);
    imageBbox[3] = (int) Math.round(mineruBbox[3] * scaleY);
    
    return imageBbox;
}
```

## 验证

### 整页 bbox

MinerU 整页 bbox 应该接近：
```json
{
  "bbox": [0, 0, 1000, 1000]
}
```

转换后应该接近图片尺寸：
```
x2 ≈ imageWidth
y2 ≈ imageHeight
```

### 对比检查

```bash
# 查看 MinerU bbox 最大值
jq '[.[].bbox[2]] | max' mineru_content_list_old.json  # 应该 ≈ 1000
jq '[.[].bbox[3]] | max' mineru_content_list_old.json  # 应该 ≈ 1000
```

## 总结

**MinerU 坐标系统**：
- ✅ 宽度归一化：1000
- ✅ 高度归一化：1000
- ✅ 正方形坐标空间
- ✅ X 和 Y 独立缩放

**转换公式**：
```
scaleX = imageWidth / 1000
scaleY = imageHeight / 1000
```

**适用范围**：
- ✅ bbox 坐标转换
- ✅ 页眉页脚检测
- ✅ 所有基于坐标的计算

---

**更新日期**: 2025-10-07  
**状态**: ✅ 已确认并实现

**关键点**: MinerU 使用 **1000x1000 正方形归一化**，X 和 Y **独立缩放**

