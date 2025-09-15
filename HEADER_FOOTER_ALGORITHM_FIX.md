# 页眉页脚检测算法修正文档

## 问题分析

用户发现了一个重要的逻辑错误：**页眉页脚检测使用了错误的坐标系统**

### 原始实现的问题

1. **错误的处理链路**：
   ```
   PDF → (DPI渲染) → PNG图片 → (OCR识别) → JSON结果(bbox基于图片坐标)
                                             ↓
                                    页眉页脚检测：❌ 使用PDF页面高度
   ```

2. **坐标系不匹配**：
   - OCR的bbox坐标基于**实际图片尺寸**
   - 检测算法使用**PDF原始尺寸**  
   - 图片可能经过DPI缩放、像素裁剪等处理

3. **尺寸不一致风险**：
   - 动态DPI调整（页数越多DPI越低）
   - 像素裁剪（minPixels/maxPixels配置）
   - 不同页面可能有不同尺寸

## 修正方案

### 1. 在OCR结果中保存图片尺寸

**OCR处理时获取图片真实尺寸**：
```java
// 在parseOnePage方法中
ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
BufferedImage image = ImageIO.read(bais);
if (image != null) {
    // 将图片尺寸信息添加到OCR结果JSON中
    ((ObjectNode) root).put("imageWidth", image.getWidth());
    ((ObjectNode) root).put("imageHeight", image.getHeight());
}
```

### 2. 扩展PageLayout类支持图片尺寸

**新增字段和构造方法**：
```java
public static class PageLayout {
    public final int page;
    public final List<LayoutItem> items;
    public final int imageWidth;  // 实际图片宽度
    public final int imageHeight; // 实际图片高度

    // 新增构造方法：从OCR JSON中提取图片尺寸
    public PageLayout(int page, List<LayoutItem> items, JsonNode ocrRoot) {
        this.page = page;
        this.items = items;
        this.imageWidth = ocrRoot != null ? ocrRoot.path("imageWidth").asInt(0) : 0;
        this.imageHeight = ocrRoot != null ? ocrRoot.path("imageHeight").asInt(0) : 0;
    }
}
```

### 3. 修正检测算法逻辑

**优先使用图片高度，智能回退**：
```java
// 获取当前页面的高度信息（优先使用图片高度，回退到PDF高度）
double currentPageHeight = 0;
if (pl.imageHeight > 0) {
    // 使用OCR结果中的实际图片高度
    currentPageHeight = pl.imageHeight;
    System.out.println("使用图片高度进行页眉页脚检测 - 页面" + pl.page + 
        ", 图片尺寸: " + pl.imageWidth + "x" + pl.imageHeight + "像素");
} else if (pageHeights != null && pl.page >= 1 && pl.page <= pageHeights.length) {
    // 回退到PDF页面高度
    currentPageHeight = pageHeights[pl.page - 1];
    System.out.println("回退使用PDF高度进行页眉页脚检测");
}
```

## 修正后的处理链路

```
PDF → (DPI渲染) → PNG图片 → (OCR识别) → JSON结果(bbox+图片尺寸)
                   ↓                        ↓
              图片尺寸信息 → 页眉页脚检测：✅ 使用图片高度
```

## 技术优势

### 1. 坐标系一致性
- OCR bbox坐标和检测高度都基于同一图片
- 消除DPI缩放和像素裁剪的影响
- 支持每页不同尺寸

### 2. 智能回退机制
- 优先使用精确的图片尺寸
- 回退到PDF高度（向后兼容）
- 最终回退到category检测

### 3. 调试友好
- 详细的日志输出，显示使用的高度信息
- 清晰的检测过程追踪

## 文件修改清单

1. **`GPUOCRCompareService.java`**：
   - `parseOnePage`：添加图片尺寸提取逻辑
   - `parseOnePageFromSavedJson`：支持从JSON读取尺寸信息
   - 调用点更新：使用新的PageLayout构造方法

2. **`TextExtractionUtil.java`**：
   - `PageLayout`类：新增imageWidth/imageHeight字段
   - 页眉页脚检测逻辑：优先使用图片高度

## 验证方法

### 调试日志示例
```
使用图片高度进行页眉页脚检测 - 页面1, 图片尺寸: 1654x2339像素
检测到页眉内容 - 页面1, 文本: '第一章 概述', 顶部百分比: 3.25%, 页眉阈值: 5.0%
检测到页脚内容 - 页面1, 文本: '第 1 页', 底部百分比: 96.8%, 页脚阈值: 95.0%
```

### 测试场景
1. **高DPI文档**：验证图片缩放后的检测准确性
2. **混合页面尺寸**：验证每页独立尺寸处理
3. **像素裁剪**：验证像素限制下的检测效果

## 总结

这次修正解决了页眉页脚检测的根本性问题，确保了坐标系的一致性。通过在OCR阶段保存图片尺寸信息，并在检测时优先使用这些精确数据，大幅提升了检测的准确性和可靠性。同时保持了向后兼容性，为不同场景提供了智能的回退机制。
