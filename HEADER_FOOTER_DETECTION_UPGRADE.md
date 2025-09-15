# 页眉页脚检测算法升级文档

## 升级概览

本次升级将页眉页脚检测从基于OCR category的方式改为基于bbox位置百分比的精确检测方式，提高了检测准确性和用户可控性。

## 主要改进

### 1. 前端界面优化
- **页眉/页脚高度设置**：从毫米单位改为百分比输入
- **更直观的控制**：用户可设置页面顶部/底部多少百分比的区域视为页眉/页脚
- **默认值调整**：页眉页脚默认各占页面高度的5%

### 2. 后端算法升级
- **基于位置检测**：不再依赖OCR的`category`字段
- **精确百分比计算**：
  - 页眉检测：`bbox顶部Y坐标 / 页面高度 <= 页眉百分比`
  - 页脚检测：`bbox底部Y坐标 / 页面高度 >= (100% - 页脚百分比)`
- **智能回退机制**：当页面高度信息不可用时自动回退到category检测

### 3. 参数传递链路
```
前端百分比输入 → API参数 → GPUOCRCompareOptions → TextExtractionUtil → 位置计算
```

## 技术实现细节

### 前端改动

#### 1. 设置界面更新 (`GPUOCRCompare.vue`)
```typescript
// 新的百分比设置
const settings = reactive({
  ignoreHeaderFooter: true,
  headerHeightPercent: 5.0,  // 替代原来的headerHeightMm
  footerHeightPercent: 5.0,  // 替代原来的footerHeightMm
  // ... 其他设置
})
```

#### 2. API接口类型更新 (`gpu-ocr-compare.ts`)
```typescript
export interface GPUOCRCompareOptions {
  ignoreHeaderFooter?: boolean
  headerHeightPercent?: number  // 新字段
  footerHeightPercent?: number  // 新字段
  // ... 其他字段
}
```

### 后端改动

#### 1. 参数类型更新 (`GPUOCRCompareOptions.java`)
```java
public class GPUOCRCompareOptions {
    private double headerHeightPercent = 5.0;  // 替代headerHeightMm
    private double footerHeightPercent = 5.0;  // 替代footerHeightMm
    // ... getter/setter方法
}
```

#### 2. 控制器参数更新 (`GPUOCRCompareController.java`)
```java
@RequestParam(value = "headerHeightPercent", defaultValue = "5.0") double headerHeightPercent,
@RequestParam(value = "footerHeightPercent", defaultValue = "5.0") double footerHeightPercent,
```

#### 3. 检测算法实现 (`TextExtractionUtil.java`)
```java
// 新增方法支持基于位置的检测
public static List<CharBox> parseTextAndPositionsFromResults(
    PageLayout[] ordered, 
    ExtractionStrategy strategy, 
    boolean ignoreHeaderFooter, 
    double headerHeightPercent, 
    double footerHeightPercent, 
    double[] pageHeights
)

// 检测逻辑
if (currentPageHeight > 0 && it.bbox != null && it.bbox.length >= 4) {
    double bboxMinY = it.bbox[1]; // bbox顶部Y坐标
    double bboxMaxY = it.bbox[3]; // bbox底部Y坐标
    
    double topPercent = (bboxMinY / currentPageHeight) * 100;
    double bottomPercent = (bboxMaxY / currentPageHeight) * 100;
    
    // 页眉检测
    if (topPercent <= headerHeightPercent) {
        isHeaderOrFooter = true;
    }
    // 页脚检测
    else if (bottomPercent >= (100 - footerHeightPercent)) {
        isHeaderOrFooter = true;
    }
}
```

#### 4. 页面高度计算 (`GPUOCRCompareService.java`)
```java
// 新增方法计算每页高度
private double[] calculatePageHeights(Path pdfPath) {
    try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
        int pageCount = doc.getNumberOfPages();
        double[] heights = new double[pageCount];
        
        for (int i = 0; i < pageCount; i++) {
            PDPage page = doc.getPage(i);
            heights[i] = page.getMediaBox().getHeight(); // 页面高度（点单位）
        }
        
        return heights;
    }
}
```

## 使用说明

### 前端操作
1. 进入GPU OCR比对页面
2. 点击"设置"按钮打开比对设置抽屉
3. 在"页眉高度(%)"和"页脚高度(%)"字段设置百分比值
   - 页眉：文档顶部多少百分比的区域视为页眉
   - 页脚：文档底部多少百分比的区域视为页脚
4. 提交比对任务

### 调试信息
算法会在控制台输出详细的检测日志：
```
检测到页眉内容 - 页面1, 文本: '第一章 概述', 顶部百分比: 3.25%, 页眉阈值: 5.0%
检测到页脚内容 - 页面1, 文本: '第 1 页', 底部百分比: 96.8%, 页脚阈值: 95.0%
```

## 兼容性保证

### 回退机制
当页面高度信息不可用时，系统会自动回退到原有的category检测方式：
```java
// 回退到基于category的检测（旧算法）
if (it.category != null && 
    ("Page-header".equals(it.category) || "Page-footer".equals(it.category))) {
    isHeaderOrFooter = true;
}
```

### API兼容性
- 前端向后兼容：新字段为可选字段
- 后端向后兼容：提供默认值和方法重载

## 测试建议

### 测试场景
1. **标准文档**：包含明显页眉页脚的正式文档
2. **边界情况**：页眉页脚位置接近边界值的文档
3. **特殊格式**：页眉页脚格式特殊的文档
4. **多页文档**：验证每页检测的一致性

### 验证方法
1. 比对前后的检测结果差异
2. 观察控制台日志中的检测信息
3. 调整百分比参数验证检测范围变化
4. 对比OCR category检测和位置检测的准确性

## 性能影响

### 计算开销
- 新增PDF页面高度计算：O(页数)复杂度，对整体性能影响微小
- 位置百分比计算：O(1)复杂度，比字符串匹配更高效

### 内存占用
- 每个文档额外存储页面高度数组，内存增加可忽略不计

## 总结

本次升级显著提高了页眉页脚检测的准确性和可控性，同时保持了良好的兼容性和性能表现。用户现在可以根据具体文档格式精确调节检测区域，获得更好的比对体验。
