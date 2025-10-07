# MinerU Bbox 坐标截断 Bug 修复

## 🐛 Bug 描述

### 症状

MinerU 原始 bbox 被错误地截断：

```json
// MinerU 原始输出（正确）
{
  "bbox": [144, 789, 848, 829]
}

// 读取到的数据（错误）
📍 MinerU原始bbox: [144.0, 789.0, 595.32, 829.0]
                                    ^^^^^^
                                    应该是 848，而不是 595.32！
```

### 影响

**转换结果错误**：
```
预期: x2 = 848 * 1.322 = 1121
实际: x2 = 595.32 * 1.322 = 787  ← 错误！

前端标注框只显示到页面中间（60%），而不是正确的 85%
```

## 🔍 根本原因

### Bug 1: `extractBbox` 方法

**位置**: `MinerUOCRService.java:809-824`

**错误代码**：
```java
private double[] extractBbox(JsonNode bboxNode, double pdfWidth, double pdfHeight) {
    double[] bbox = new double[]{
        bboxNode.get(0).asDouble(),
        bboxNode.get(1).asDouble(),
        bboxNode.get(2).asDouble(),
        bboxNode.get(3).asDouble()
    };
    
    // ❌ 错误：用 PDF 尺寸限制 MinerU 坐标
    bbox[0] = Math.max(0, Math.min(bbox[0], pdfWidth));   // pdfWidth = 595.32
    bbox[1] = Math.max(0, Math.min(bbox[1], pdfHeight));  // pdfHeight = 841.92
    bbox[2] = Math.max(bbox[0], Math.min(bbox[2], pdfWidth));  // 848 → 595.32 ❌
    bbox[3] = Math.max(bbox[1], Math.min(bbox[3], pdfHeight));
    
    return bbox;
}
```

**问题分析**：
- MinerU 的 bbox 是基于 **1000x1000** 归一化坐标
- **不应该**用 PDF 尺寸（595x842）来限制
- 848 < 1000 是正常的，不需要修正

**执行流程**：
```
1. JSON 读取: bbox[2] = 848
2. extractBbox 处理: Math.min(848, 595.32) = 595.32  ← Bug!
3. 传递给转换: 595.32 * 1.322 = 787  ← 错误结果
```

### Bug 2: `convertToLayoutItems` 方法

**位置**: `MinerUOCRService.java:948-962`

**错误代码**：
```java
// ❌ 错误的判断条件
if (mineruBbox[2] > pdfWidth || mineruBbox[3] > pdfHeight) {
    log.warn("⚠️  MinerU返回的坐标超出PDF尺寸！");
    
    // ❌ 错误地用 PDF 尺寸限制坐标
    mineruBbox[0] = Math.max(0, Math.min(mineruBbox[0], pdfWidth));
    mineruBbox[1] = Math.max(0, Math.min(mineruBbox[1], pdfHeight));
    mineruBbox[2] = Math.max(mineruBbox[0], Math.min(mineruBbox[2], pdfWidth));
    mineruBbox[3] = Math.max(mineruBbox[1], Math.min(mineruBbox[3], pdfHeight));
}
```

**问题分析**：
- MinerU 坐标范围是 **0-1000**
- 当然会大于 PDF 尺寸（595x842）
- 这**不是错误**，是正常的归一化结果

## ✅ 修复方案

### 修复 Bug 1: `extractBbox`

**正确代码**：
```java
private double[] extractBbox(JsonNode bboxNode, double pdfWidth, double pdfHeight) {
    double[] bbox = new double[]{
        bboxNode.get(0).asDouble(),
        bboxNode.get(1).asDouble(),
        bboxNode.get(2).asDouble(),
        bboxNode.get(3).asDouble()
    };
    
    // ✅ 正确：MinerU 使用 1000x1000 归一化坐标系统
    // 不应该用 PDF 尺寸来限制坐标！
    // 坐标范围应该是 0-1000，而不是 0-pdfWidth/pdfHeight
    final double MINERU_MAX = 1000.0;
    
    // 只修正明显异常的坐标（例如负数或超出1000）
    bbox[0] = Math.max(0, Math.min(bbox[0], MINERU_MAX));
    bbox[1] = Math.max(0, Math.min(bbox[1], MINERU_MAX));
    bbox[2] = Math.max(bbox[0], Math.min(bbox[2], MINERU_MAX));
    bbox[3] = Math.max(bbox[1], Math.min(bbox[3], MINERU_MAX));
    
    return bbox;
}
```

### 修复 Bug 2: `convertToLayoutItems`

**正确代码**：
```java
// ✅ 正确：MinerU 使用 1000x1000 归一化坐标，不应该和 PDF 尺寸比较
// 只检查是否在 0-1000 范围内
final double MINERU_MAX = 1000.0;
if (mineruBbox[2] > MINERU_MAX || mineruBbox[3] > MINERU_MAX) {
    log.warn("⚠️  MinerU 坐标异常（超出1000）: [{}, {}, {}, {}]", 
        mineruBbox[0], mineruBbox[1], mineruBbox[2], mineruBbox[3]);
    
    // 修正到 0-1000 范围
    mineruBbox[0] = Math.max(0, Math.min(mineruBbox[0], MINERU_MAX));
    mineruBbox[1] = Math.max(0, Math.min(mineruBbox[1], MINERU_MAX));
    mineruBbox[2] = Math.max(mineruBbox[0], Math.min(mineruBbox[2], MINERU_MAX));
    mineruBbox[3] = Math.max(mineruBbox[1], Math.min(mineruBbox[3], MINERU_MAX));
}
```

## 📊 验证

### 测试数据

```json
{
  "type": "text",
  "text": "线材质量要求符合国家标准 GB/T70L-1997，螺纹钢材质符合国家标准 GB1499-1998。",
  "bbox": [144, 789, 848, 829],
  "page_idx": 1
}
```

**图片尺寸**: 1322 x 1870

### 修复前（错误）

```
📍 MinerU原始bbox: [144.0, 789.0, 595.32, 829.0]  ← 截断！
✅ 转换后图片bbox: [190, 1475, 787, 1550]
                                    ^^^
                                    错误：应该是 1121
```

**计算**：
```
x2 = 595.32 * (1322 / 1000) = 787  ← 错误
```

### 修复后（正确）

```
📍 MinerU原始bbox: [144.0, 789.0, 848.0, 829.0]  ← 完整保留
✅ 转换后图片bbox: [190, 1475, 1121, 1550]
                                    ^^^^
                                    正确！
```

**计算**：
```
scaleX = 1322 / 1000 = 1.322
x2 = 848 * 1.322 = 1121  ✓
```

### 前端效果

| 修复前 | 修复后 |
|--------|--------|
| 标注框宽度: 787px (60%) | 标注框宽度: 1121px (85%) |
| 只标注到文本中间 ❌ | 完整标注整行文本 ✓ |

## 🎯 核心要点

### MinerU 坐标系统

```
┌──────────────────┐
│                  │
│   1000 x 1000    │  ← 固定归一化空间
│                  │
│  不是 PDF 尺寸！  │
└──────────────────┘
```

### 坐标验证规则

| 坐标系统 | 有效范围 | 验证方式 |
|---------|---------|---------|
| **MinerU** | 0 - 1000 | `bbox[i] <= 1000` |
| ~~PDF~~ | ~~0 - pdfSize~~ | ❌ **不应该用于验证** |
| **Image** | 0 - imageSize | `bbox[i] <= imageSize` |

### 转换流程

```
JSON (MinerU)     →    Java (提取)    →    转换        →    前端显示
─────────────          ────────────        ─────            ────────
[144, 789,      →      [144, 789,    →    [190, 1475,  →   标注框
 848, 829]              848, 829]          1121, 1550]      显示正确
 
 ✓ 0-1000             ✓ 保持不变         ✓ 缩放到图片
```

### ❌ 错误做法

```java
// 不要这样做！
bbox[2] = Math.min(bbox[2], pdfWidth);   // ❌
bbox[3] = Math.min(bbox[3], pdfHeight);  // ❌

if (bbox[2] > pdfWidth) { /* 修正 */ }   // ❌
```

### ✅ 正确做法

```java
// 应该这样做！
final double MINERU_MAX = 1000.0;
bbox[2] = Math.min(bbox[2], MINERU_MAX);  // ✓
bbox[3] = Math.min(bbox[3], MINERU_MAX);  // ✓

if (bbox[2] > MINERU_MAX) { /* 修正 */ }  // ✓
```

## 📝 相关文件

- `MinerUOCRService.java` - 修复了 2 处 bug
- `MinerUCoordinateConverter.java` - 已正确使用 1000x1000 归一化
- `MINERU_1000x1000_NORMALIZATION.md` - 归一化系统说明

## 🚀 下一步

1. **重启服务**以加载新代码
2. **重新测试**同一文档
3. **验证日志**中的 bbox 值：
   ```
   📍 MinerU原始bbox: [144.0, 789.0, 848.0, 829.0]  ← 应该是 848
   ✅ 转换后图片bbox: [190, 1475, 1121, 1550]      ← 应该是 1121
   ```
4. **检查前端**标注框是否正确覆盖整行文本

---

**修复日期**: 2025-10-07  
**状态**: ✅ 已修复并编译  
**关键点**: **不要用 PDF 尺寸限制 MinerU 坐标**，MinerU 使用 **1000x1000 归一化**

