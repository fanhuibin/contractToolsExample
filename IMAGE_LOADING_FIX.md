# 图片加载问题修复总结

## 🔍 问题分析

**症状**：前端返回的 `newImageInfo` 和 `oldImageInfo` 都是空的 `{ totalPages: 0, pages: [] }`

**根本原因**：
1. ❌ **路径不一致**：读取图片时使用了错误的路径配置
2. ❌ **格式不支持**：只支持 `.png`，但实际生成的是 `.jpg`

## 🔧 问题详情

### 问题 1: 路径配置不一致

**生成图片时**（MinerUOCRService.java）：
```java
String uploadRootPath = zxOcrConfig.getUploadPath();  // 正确
Path imageDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
```

**读取图片时**（CompareService.java - 修复前）：
```java
String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();  // ❌ 错误！
Path imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
```

**结果**：两个配置可能指向不同目录，导致找不到图片！

### 问题 2: 只支持 PNG 格式

**读取图片时**（修复前）：
```java
stream.filter(path -> path.toString().toLowerCase().endsWith(".png"))  // ❌ 只支持 PNG
```

**实际生成的格式**：
```
配置中使用 image-format: JPEG，实际生成的是 .jpg 文件
```

**结果**：过滤条件不匹配，找不到 JPEG 图片！

## ✅ 修复方案

### 修复 1: 统一路径配置

**修改前**：
```java
public DocumentImageInfo getDocumentImageInfo(String taskId, String mode) {
    String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();  // ❌
    // ...
}
```

**修改后**：
```java
public DocumentImageInfo getDocumentImageInfo(String taskId, String mode) {
    String uploadRootPath = gpuOcrConfig.getUploadPath();  // ✅
    Path imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
    
    logger.info("🔍 获取图片信息 - taskId: {}, mode: {}, 路径: {}", taskId, mode, imagesDir);
    // ...
}
```

### 修复 2: 支持多种图片格式

**修改前**：
```java
stream.filter(path -> path.toString().toLowerCase().endsWith(".png"))  // ❌
```

**修改后**：
```java
stream.filter(path -> {
    String fileName = path.toString().toLowerCase();
    return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");  // ✅
})
```

### 修复 3: 添加详细日志

```java
logger.info("🔍 获取图片信息 - taskId: {}, mode: {}, 路径: {}", taskId, mode, imagesDir);
logger.info("  ✅ 找到 {} 个图片文件", imageFiles.size());
logger.debug("    页面 {}: {} ({}x{})", pageNumber, fileName, width, height);
logger.info("  🎉 成功获取 {} 页的图片信息", docInfo.getTotalPages());
```

## 🚀 验证步骤

### 1. 清理所有缓存（必须！）
```powershell
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

### 2. 重启应用
```bash
mvn clean package -DskipTests
java -jar contract-tools-sdk/target/contract-tools-sdk-1.0.0.jar
```

### 3. 检查启动日志
应该看到配置加载日志：
```
╔════════════════════════════════════════════════════════════════
║ ZxOcrConfig 配置已加载 (来自: contract-tools-core)
╠════════════════════════════════════════════════════════════════
║ 📍 配置前缀: zxcm.compare.zxocr
║ 🎨 渲染DPI: 300
║ 🖼️  图片格式: JPEG
║ 📊 JPEG质量: 0.85
║ 📁 上传路径: ./uploads
╚════════════════════════════════════════════════════════════════
```

### 4. 执行比对任务
提交新的比对任务，查看日志应该包含：

**生成图片阶段**：
```
开始生成10个页面图片，DPI: 300
📸 图片格式: JPEG, JPEG质量: 0.85
✅ 生成页面图片: page-1.jpg, 尺寸: 2480x3508, 大小: 250KB
✅ 生成页面图片: page-2.jpg, 尺寸: 2480x3508, 大小: 245KB
...
```

**获取图片信息阶段**：
```
🔍 获取图片信息 - taskId: xxx, mode: old, 路径: ./uploads/compare-pro/tasks/xxx/images/old
  ✅ 找到 10 个图片文件
    页面 1: page-1.jpg (2480x3508)
    页面 2: page-2.jpg (2480x3508)
    ...
  🎉 成功获取 10 页的图片信息

🔍 获取图片信息 - taskId: xxx, mode: new, 路径: ./uploads/compare-pro/tasks/xxx/images/new
  ✅ 找到 10 个图片文件
    页面 1: page-1.jpg (2480x3508)
    ...
  🎉 成功获取 10 页的图片信息
```

### 5. 检查返回数据
API 返回应该包含：
```json
{
  "code": 200,
  "message": "获取Canvas比对结果成功",
  "data": {
    "oldImageInfo": {
      "totalPages": 10,
      "pages": [
        {
          "pageNum": 1,
          "imageUrl": "/api/compare-pro/files/tasks/xxx/images/old/page-1.jpg",
          "width": 2480,
          "height": 3508
        },
        ...
      ]
    },
    "newImageInfo": {
      "totalPages": 10,
      "pages": [
        {
          "pageNum": 1,
          "imageUrl": "/api/compare-pro/files/tasks/xxx/images/new/page-1.jpg",
          "width": 2480,
          "height": 3508
        },
        ...
      ]
    },
    "differences": [...],
    ...
  }
}
```

### 6. 验证图片可访问
在浏览器访问：
```
http://localhost:3000/api/compare-pro/files/tasks/{taskId}/images/old/page-1.jpg
http://localhost:3000/api/compare-pro/files/tasks/{taskId}/images/new/page-1.jpg
```

应该能看到图片！

## 📊 完整的调用链

```
前端请求
  ↓
GET /api/compare-pro/canvas-result/{taskId}
  ↓
GPUCompareController.getCanvasResult()
  ↓
CompareService.getCanvasFrontendResult()
  ↓
CompareService.getDocumentImageInfo(taskId, "old")  ← 修复了这里
CompareService.getDocumentImageInfo(taskId, "new")  ← 修复了这里
  ↓
返回图片信息给前端
```

## 🔍 故障排查

### 问题：日志中没有 "获取图片信息" 的输出

**可能原因**：
1. 日志级别太高
2. 方法没有被调用

**解决**：
```yaml
# application.yml
logging:
  level:
    com.zhaoxinms.contract.tools.comparePRO.service.CompareService: DEBUG
```

### 问题：日志显示 "图片目录不存在"

**检查**：
```powershell
# 查看实际的目录结构
ls .\uploads\compare-pro\tasks\

# 查看具体任务的图片
ls .\uploads\compare-pro\tasks\{taskId}\images\old\
ls .\uploads\compare-pro\tasks\{taskId}\images\new\
```

**确认**：
1. 目录存在吗？
2. 文件格式是什么？（.png 还是 .jpg）
3. 文件名格式正确吗？（page-1.jpg, page-2.jpg...）

### 问题：日志显示 "找到 0 个图片文件"

**原因**：
1. 图片格式不匹配（生成的是 .jpg 但过滤 .png）
2. 文件名格式不对（不是 page-N.xxx）

**检查**：
```powershell
# 查看实际的文件
ls .\uploads\compare-pro\tasks\{taskId}\images\old\

# 应该看到：
# page-1.jpg
# page-2.jpg
# ...
```

### 问题：配置日志显示的 DPI 不是 300

**检查优先级**：
```
1. 环境变量: echo $ZXCM_COMPARE_ZXOCR_RENDER_DPI
2. 命令行参数: 检查启动命令
3. application.yml: 检查配置文件
4. Java 默认值: 检查 ZxOcrConfig.java
```

## 📝 修改的文件列表

### 核心修复
- ✅ `contract-tools-core/src/.../service/CompareService.java`
  - `getDocumentImageInfo()` - 统一路径配置
  - `getDocumentImageInfo()` - 支持多种图片格式
  - 添加详细的调试日志

### 配置统一
- ✅ `contract-tools-core/src/.../config/ZxOcrConfig.java`
  - 添加配置加载日志
  - 统一 DPI 为 300

### 重复配置删除
- ❌ `backend/src/.../config/ZxOcrConfig.java` - 已删除
- ❌ `backend/src/.../config/GpuOcrConfig.java` - 已删除

### 文档
- 📄 `IMAGE_LOADING_FIX.md` - 本文档
- 📄 `SDK_CONFIG_FIX_GUIDE.md` - 配置修复指南
- 📄 `DPI_CONFIG_SUMMARY.md` - DPI 配置总结
- 📄 `IMAGE_OPTIMIZATION_GUIDE.md` - 图片优化指南

## 🎉 总结

### 修复前 ❌
- 路径配置不一致导致找不到图片
- 只支持 PNG 但实际生成 JPEG
- 没有调试日志，问题难以排查
- 多个配置类冲突

### 修复后 ✅
- 统一使用 `gpuOcrConfig.getUploadPath()`
- 支持 PNG、JPG、JPEG 多种格式
- 详细的调试日志
- 删除重复配置类
- YML 配置优先级正确

**现在重启应用，执行新的比对任务，应该能正常加载图片了！** 🚀

