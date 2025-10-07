# MinerU BBox 偏大问题调试

## 问题描述

**症状**：bbox 的 x 和 y 坐标都偏大了  
**位置方向**：正确（顶部在顶部，底部在底部）  
**只是坐标值**：偏大

## 调试步骤

### 1. 查看调试日志

启用调试日志后，查看以下信息：

```
坐标转换 - PDF尺寸: 595x842, 图片尺寸: 2480x3508, 缩放比例: scaleX=4.168, scaleY=4.166
MinerU原始bbox: [100, 200, 500, 400]
转换后图片bbox: [417, 833, 2084, 1666]
```

**检查要点**：
1. PDF 尺寸是否正确？
2. 图片尺寸是否正确？
3. 缩放比例是否合理？
4. MinerU 原始 bbox 的数值范围？

### 2. 可能的原因

#### 原因 1：MinerU 返回的坐标单位不是 PDF 点

**假设**：MinerU 返回的坐标可能已经是像素坐标，而不是 PDF 点坐标

**验证方法**：
- 检查 MinerU 原始 bbox 的数值范围
- 如果 bbox 值接近图片尺寸（如 2000+），说明已经是像素
- 如果 bbox 值接近 PDF 尺寸（如 500 左右），说明是 PDF 点

**解决方案**：
```java
// 如果 MinerU 返回的已经是像素坐标，不需要缩放
if (mineruBbox[2] > pdfWidth * 2) {
    // 直接使用，不缩放
    imageBbox[0] = (int) Math.round(mineruBbox[0]);
    imageBbox[1] = (int) Math.round(mineruBbox[1]);
    imageBbox[2] = (int) Math.round(mineruBbox[2]);
    imageBbox[3] = (int) Math.round(mineruBbox[3]);
}
```

#### 原因 2：MinerU 基于不同的 DPI

**假设**：MinerU 内部使用 72 DPI（PDF 标准），但我们用 200 DPI 渲染

**计算**：
- PDF 点转 72 DPI 像素：1 点 = 1 像素
- PDF 点转 200 DPI 像素：1 点 = 200/72 ≈ 2.78 像素

**当前缩放**：
```java
scaleX = imageWidth / pdfWidth  // 2480 / 595 = 4.168
```

**如果 MinerU 是 72 DPI**：
```java
// 应该是
scaleX = renderDpi / 72.0  // 200 / 72 = 2.78
```

#### 原因 3：MinerU 返回的是 "归一化" 坐标

**假设**：MinerU 返回的坐标可能是归一化的（0-1之间）

**验证方法**：
- 检查 bbox 值是否都小于 1
- 如果是归一化坐标，需要乘以图片尺寸

**解决方案**：
```java
imageBbox[0] = (int) Math.round(mineruBbox[0] * imageWidth);
imageBbox[1] = (int) Math.round(mineruBbox[1] * imageHeight);
```

#### 原因 4：PDF 页面尺寸获取错误

**验证方法**：
```java
System.out.println("PDF 页面尺寸: " + pdfWidth + " x " + pdfHeight);
System.out.println("图片尺寸: " + imageWidth + " x " + imageHeight);
System.out.println("renderDpi: " + renderDpi);
```

**预期**：
- A4 纸张：595 x 842 点（portrait）
- 200 DPI 渲染：1653 x 2339 像素
- 300 DPI 渲染：2480 x 3508 像素

### 3. 查看 MinerU 原始返回数据

检查 `mineru_raw_*.json` 文件：

```bash
cat uploads/compare-pro/tasks/{taskId}/ocr/mineru_raw_old.json | jq '.results[].content_list[0]'
```

**示例输出**：
```json
{
  "type": "text",
  "text": "Hello World",
  "bbox": [100, 200, 500, 400],  ← 检查这些数值
  "page_idx": 0
}
```

**分析**：
- 如果 bbox[2] ≈ 500，可能是 PDF 点坐标
- 如果 bbox[2] ≈ 2000，可能是像素坐标
- 如果 bbox[2] ≈ 0.8，可能是归一化坐标

### 4. 对比 MinerU 的 layout.pdf

MinerU 会生成 `*_layout.pdf`，其中有可视化的 bbox 标记。

**对比方法**：
1. 打开 `*_layout.pdf`
2. 查看标记的位置
3. 与我们的标记对比
4. 如果 MinerU 的正确，说明我们的转换有问题

### 5. 测试不同的缩放方案

#### 方案 A：当前方案（基于 PDF 尺寸）
```java
scaleX = imageWidth / pdfWidth;
scaleY = imageHeight / pdfHeight;
```

#### 方案 B：基于 DPI
```java
double dpiRatio = renderDpi / 72.0;
scaleX = dpiRatio;
scaleY = dpiRatio;
```

#### 方案 C：不缩放（假设已经是像素）
```java
scaleX = 1.0;
scaleY = 1.0;
```

#### 方案 D：基于 MinerU 文档的说明

查看 MinerU API 文档中关于坐标的说明。

## 快速诊断命令

```bash
# 1. 查看调试日志
grep "坐标转换" logs/*.log | head -5

# 2. 查看 MinerU 原始 bbox
cat uploads/compare-pro/tasks/*/ocr/mineru_raw_old.json | jq '.results[].content_list[0].bbox'

# 3. 查看 PDF 尺寸
grep "PDF尺寸" logs/*.log | head -3

# 4. 查看图片尺寸  
grep "图片尺寸" logs/*.log | head -3
```

## 预期输出

```
坐标转换 - PDF尺寸: 595x842, 图片尺寸: 1653x2339, 缩放比例: scaleX=2.778, scaleY=2.778
MinerU原始bbox: [100, 200, 500, 400]
转换后图片bbox: [278, 556, 1389, 1111]
```

**合理性检查**：
- 图片宽度 1653，bbox 最大 x=1389 ✅
- 图片高度 2339，bbox 最大 y=1111 ✅
- 缩放比例约 2.78 （200 DPI / 72）✅

## 下一步

根据调试日志的输出，确定具体问题：

1. **如果 scaleX > 5**：可能是 renderDpi 太高或 PDF 尺寸错误
2. **如果 MinerU bbox > 1000**：可能 MinerU 返回的已经是像素坐标
3. **如果转换后 bbox 超出图片范围**：缩放计算有误

请提供：
- 调试日志输出
- mineru_raw_*.json 中的 bbox 示例
- PDF 文件的实际尺寸

这样我们可以准确定位问题！

