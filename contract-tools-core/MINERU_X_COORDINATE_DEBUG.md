# MinerU X 坐标归一化调试

## 问题

- ✅ Y 坐标正确（使用 imageHeight / 1000）
- ❌ X 坐标不对

## 可能的归一化方式

### 方案 1：按比例（当前实现）

```
高度归一化: 1000
宽度按比例: 1000 * (pdfWidth / pdfHeight)

A4 示例:
- PDF: 595 x 842
- MinerU: 707 x 1000  (707 = 1000 * 595/842)
```

**如果是这种方式**：
- scaleX = scaleY = imageHeight / 1000
- X 和 Y 应该都正确

### 方案 2：宽度也归一化为 1000

```
宽度归一化: 1000
高度归一化: 1000

所有页面:
- MinerU: 1000 x 1000 (正方形)
```

**如果是这种方式**：
- scaleX = imageWidth / 1000
- scaleY = imageHeight / 1000
- X 和 Y 分别独立缩放

### 方案 3：长边归一化为 1000

```
长边归一化: 1000
短边按比例缩放

A4 竖向 (高>宽):
- PDF: 595 x 842
- MinerU: 707 x 1000

A4 横向 (宽>高):
- PDF: 842 x 595  
- MinerU: 1000 x 707
```

**如果是这种方式**：
- scale = imageMax / 1000
- 其中 imageMax = max(imageWidth, imageHeight)

### 方案 4：宽度固定值归一化

```
宽度归一化: 某个固定值（如 800, 1024）
高度按比例缩放
```

## 如何判断

### 步骤 1：查看实际 bbox 数据

打开 `mineru_content_list_old_stats.txt`，查看几个 bbox：

```bash
cat mineru_content_list_old_stats.txt | grep "bbox:" | head -10
```

**查找整页或接近整页的 bbox**：

示例输出：
```
[1] bbox: [10, 50, 697, 990]   ← 接近整页
[2] bbox: [100, 200, 600, 400]
[3] bbox: [50, 100, 650, 300]
```

### 步骤 2：分析 X 最大值

**如果 X 最大值 ≈ 707**（方案 1）：
- MinerU 宽度 = 1000 * (595/842) = 707
- 使用按比例归一化 ✅

**如果 X 最大值 ≈ 1000**（方案 2）：
- MinerU 宽度 = 1000
- 使用正方形归一化
- 需要独立缩放 X 和 Y

**如果 X 最大值 ≈ 其他值**（方案 3/4）：
- 需要进一步分析

### 步骤 3：计算实际比例

假设您看到的 bbox 是：
```
整页 bbox: [0, 0, X_max, Y_max]
```

计算：
```
宽度归一化值 = X_max
高度归一化值 = Y_max
```

## 快速诊断命令

```bash
# 查看 X 坐标的最大值
jq '[.[].bbox[2]] | max' mineru_content_list_old.json

# 查看 Y 坐标的最大值
jq '[.[].bbox[3]] | max' mineru_content_list_old.json

# 查看几个完整的 bbox
jq '.[0:5] | .[] | {type, bbox}' mineru_content_list_old.json
```

## 预期结果

### 如果是方案 1（按比例）

```json
{
  "x_max": 707,    // ≈ 1000 * 595/842
  "y_max": 1000
}
```

### 如果是方案 2（正方形）

```json
{
  "x_max": 1000,
  "y_max": 1000
}
```

## 修复代码（方案 2）

如果确认是方案 2（宽度也归一化为 1000），修改代码：

```java
public static int[] convertToImageCoordinates(
        double[] mineruBbox,
        double pdfWidth,
        double pdfHeight,
        int imageWidth,
        int imageHeight) {
    
    // MinerU 可能将宽度和高度都归一化为 1000
    final double MINERU_NORMALIZED_WIDTH = 1000.0;
    final double MINERU_NORMALIZED_HEIGHT = 1000.0;
    
    // X 和 Y 分别独立缩放
    double scaleX = imageWidth / MINERU_NORMALIZED_WIDTH;
    double scaleY = imageHeight / MINERU_NORMALIZED_HEIGHT;
    
    log.debug("坐标转换 - 图片尺寸: {}x{}, MinerU归一化: {}x{}, 缩放比例: scaleX={}, scaleY={}", 
        imageWidth, imageHeight, 
        MINERU_NORMALIZED_WIDTH, MINERU_NORMALIZED_HEIGHT,
        String.format("%.3f", scaleX), String.format("%.3f", scaleY));
    
    int[] imageBbox = new int[4];
    imageBbox[0] = (int) Math.round(mineruBbox[0] * scaleX);
    imageBbox[1] = (int) Math.round(mineruBbox[1] * scaleY);
    imageBbox[2] = (int) Math.round(mineruBbox[2] * scaleX);
    imageBbox[3] = (int) Math.round(mineruBbox[3] * scaleY);
    
    return imageBbox;
}
```

## 请提供数据

请运行以下命令并告诉我结果：

```bash
# 1. 查看 X 和 Y 的最大值
jq '[.[].bbox[2]] | max' mineru_content_list_old.json
jq '[.[].bbox[3]] | max' mineru_content_list_old.json

# 2. 查看前 3 个 bbox
jq '.[0:3] | .[] | {type, bbox}' mineru_content_list_old.json
```

或者直接告诉我：

1. **X 最大值**是多少？（应该在 stats.txt 中能看到）
2. **Y 最大值**是多少？
3. **PDF 尺寸**是多少？（从日志中）

根据这些数据，我可以准确判断 MinerU 使用的归一化方式！

