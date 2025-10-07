# MinerU 图片生成优化方案

## 现状分析

### 当前性能
```
8个页面，每页 1360x1760
总耗时：约 16 秒（平均每页 2秒）
```

### 当前实现
```java
for (int i = 0; i < pageCount; i++) {
    BufferedImage image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
    ImageIO.write(image, "PNG", imageFile);
}
```

**问题**：串行处理，每页依次渲染

## 优化方案

### 方案1：并行渲染（推荐）⚡

**优点**：
- ✅ 可以提速 **3-4倍**（取决于CPU核心数）
- ✅ 无需改变逻辑，只需并行化
- ✅ 对结果无影响

**实现**：
```java
private List<Map<String, Object>> generatePageImages(File pdfFile, File outputDir, String taskId, String docMode) throws IOException {
    List<Map<String, Object>> pageImages = Collections.synchronizedList(new ArrayList<>());
    File imagesDir = new File(outputDir, "images/" + docMode);
    if (!imagesDir.exists()) {
        imagesDir.mkdirs();
    }
    
    int renderDpi = zxOcrConfig.getRenderDpi();
    
    try (PDDocument document = PDDocument.load(pdfFile)) {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        
        // 并行渲染所有页面
        IntStream.range(0, pageCount).parallel().forEach(i -> {
            try {
                BufferedImage image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
                File imageFile = new File(imagesDir, "page-" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", imageFile);
                
                Map<String, Object> pageInfo = new HashMap<>();
                pageInfo.put("pageIndex", i);
                pageInfo.put("imagePath", imageFile.getAbsolutePath());
                pageInfo.put("imageWidth", image.getWidth());
                pageInfo.put("imageHeight", image.getHeight());
                pageImages.add(pageInfo);
                
                log.debug("生成页面图片: {}, 尺寸: {}x{}", imageFile.getName(), image.getWidth(), image.getHeight());
            } catch (IOException e) {
                throw new RuntimeException("渲染页面" + i + "失败", e);
            }
        });
        
        // 按页面索引排序
        pageImages.sort(Comparator.comparingInt(p -> (Integer) p.get("pageIndex")));
    }
    
    return pageImages;
}
```

**预期效果**：
```
8个页面：16秒 → 4-5秒（提速70%）
```

### 方案2：降低 DPI

**当前 DPI**：检查配置
```bash
grep renderDpi contract-tools-*/src/main/resources/*.yml
```

**建议**：
- 用于标注展示：**150 DPI** 即可（当前可能是 200-300）
- 降低 DPI：可以减少 30-50% 渲染时间

**示例**：
```yaml
# application.yml
zx-ocr:
  render-dpi: 150  # 从 200/300 降低到 150
```

### 方案3：使用 MinerU 生成的图片（需验证）

MinerU 在处理 PDF 时可能已经生成了页面图片。

**检查**：
```bash
# 查看 MinerU 输出目录
ls -la /path/to/mineru/output/images/
```

**如果存在**，可以直接复制使用，无需重新渲染：
```java
if (mineruImagesExist) {
    // 直接使用 MinerU 的图片
    copyMinerUImages(mineruOutputDir, imagesDir);
} else {
    // 降级：自己渲染
    generatePageImages(...);
}
```

### 方案4：缓存已生成的图片

**实现**：
```java
File imageFile = new File(imagesDir, "page-" + (i + 1) + ".png");
if (imageFile.exists()) {
    log.debug("复用已有图片: {}", imageFile.getName());
    // 直接读取尺寸
    BufferedImage existingImage = ImageIO.read(imageFile);
    // ... 构建 pageInfo
} else {
    // 生成新图片
    BufferedImage image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
    ImageIO.write(image, "PNG", imageFile);
}
```

## 组合优化（最佳实践）

```java
private List<Map<String, Object>> generatePageImages(...) {
    // 1. 检查缓存
    if (allImagesExist(imagesDir, pageCount)) {
        return loadExistingImages(imagesDir, pageCount);
    }
    
    // 2. 并行渲染（方案1）
    try (PDDocument document = PDDocument.load(pdfFile)) {
        int pageCount = document.getNumberOfPages();
        PDFRenderer renderer = new PDFRenderer(document);
        
        // 3. 使用合理的 DPI（方案2）
        int renderDpi = Math.min(zxOcrConfig.getRenderDpi(), 150);
        
        List<Map<String, Object>> pageImages = Collections.synchronizedList(new ArrayList<>());
        
        IntStream.range(0, pageCount).parallel().forEach(i -> {
            File imageFile = new File(imagesDir, "page-" + (i + 1) + ".png");
            
            // 4. 单页缓存检查（方案4）
            if (imageFile.exists()) {
                log.debug("复用已有图片: {}", imageFile.getName());
                try {
                    BufferedImage image = ImageIO.read(imageFile);
                    addPageInfo(pageImages, i, imageFile, image);
                } catch (IOException e) {
                    log.warn("读取已有图片失败，重新生成: {}", e.getMessage());
                    renderAndSave(renderer, i, imageFile, renderDpi, pageImages);
                }
            } else {
                renderAndSave(renderer, i, imageFile, renderDpi, pageImages);
            }
        });
        
        pageImages.sort(Comparator.comparingInt(p -> (Integer) p.get("pageIndex")));
        return pageImages;
    }
}
```

## 性能对比

| 方案 | 8页耗时 | 提速 | 复杂度 |
|------|---------|------|--------|
| 当前（串行） | 16秒 | - | - |
| 方案1（并行） | 4-5秒 | 70% | 低 |
| 方案2（降低DPI） | 8-12秒 | 25-50% | 极低 |
| 方案1+2 | 2-3秒 | 85% | 低 |
| 方案1+2+4 | 0-3秒 | 100%/85% | 中 |

## 推荐实施顺序

1. **立即实施**：方案1（并行渲染）
   - 改动小，效果好
   - 代码改动约 10 行

2. **配置调整**：方案2（降低DPI到150）
   - 零代码改动
   - 修改配置文件即可

3. **后续优化**：方案4（缓存）
   - 避免重复生成
   - 开发调试时效果明显

4. **可选**：方案3（使用MinerU图片）
   - 需要验证 MinerU 是否提供
   - 如果有，效果最佳

---

**建议**：先实施 **方案1 + 方案2**，可以立即将 16秒 降低到 **2-3秒**！

