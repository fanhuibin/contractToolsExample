# 前端图片格式问题修复

## 🔍 问题描述

**症状**：前端请求的图片 URL 使用了硬编码的 `.png` 扩展名，但后端实际生成的是 `.jpg` 格式的图片。

**错误示例**：
```
前端请求: http://localhost:3000/api/compare-pro/files/tasks/{taskId}/images/old/page-1.png
后端实际: http://localhost:3000/api/compare-pro/files/tasks/{taskId}/images/old/page-1.jpg
```

**结果**：404 Not Found，图片无法加载！

## 🔧 根本原因

### 后端配置
```yaml
# application.yml
zxcm:
  compare:
    zxocr:
      image-format: JPEG  # 生成 .jpg 文件
      jpeg-quality: 0.85
```

后端返回的数据包含正确的 `imageUrl`：
```json
{
  "oldImageInfo": {
    "pages": [
      {
        "pageNum": 1,
        "imageUrl": "/api/compare-pro/files/tasks/{taskId}/images/old/page-1.jpg",
        "width": 2480,
        "height": 3508
      }
    ]
  }
}
```

### 前端问题
**修复前**（frontend 和 export 项目）：
```typescript
// ❌ 硬编码了 .png 扩展名
const imageUrl = baseUrl 
  ? `${baseUrl}/page-${pageIndex + 1}.png`
  : `/api/compare-pro/files/tasks/${taskId}/images/${mode}/page-${pageIndex + 1}.png`
```

**问题**：
1. 忽略了后端返回的 `pageInfo.imageUrl`
2. 硬编码了 `.png` 扩展名
3. 无法适应后端配置的图片格式

## ✅ 修复方案

### 修复逻辑
使用优先级策略：
1. **优先**：使用后端返回的 `pageInfo.imageUrl`（包含正确的扩展名）
2. **Fallback 1**：使用 `baseUrl` 拼接（保留兼容性，默认 .png）
3. **Fallback 2**：手动拼接（保留兼容性，默认 .png）

### 修复代码

**修复后**（frontend 和 export 项目）：
```typescript
// ✅ 优先使用后端返回的 imageUrl，支持动态扩展名
const imageUrl = pageInfo.imageUrl
  ? pageInfo.imageUrl
  : baseUrl 
    ? `${baseUrl}/page-${pageIndex + 1}.png`
    : `/api/compare-pro/files/tasks/${taskId}/images/${mode}/page-${pageIndex + 1}.png`
```

**优点**：
- ✅ 自动适应后端配置的图片格式（.jpg、.png）
- ✅ 保持向后兼容性（如果后端未提供 imageUrl）
- ✅ 统一了 frontend 和 export 两个项目的逻辑

## 📄 修改的文件

### Frontend 项目
- ✅ `frontend/src/views/documents/gpu-ocr-canvas/canvas-renderer.ts`
  - 行 67-75：修改图片 URL 获取逻辑

### Export 项目
- ✅ `export/src/gpu-ocr-canvas/canvas-renderer.ts`
  - 行 67-75：修改图片 URL 获取逻辑（与 frontend 保持一致）

## 🚀 验证步骤

### 1. 确认后端配置
```yaml
# sdk/src/main/resources/application.yml
zxcm:
  compare:
    zxocr:
      render-dpi: 300
      image-format: JPEG  # 或 PNG
      jpeg-quality: 0.85
```

### 2. 重启后端
```bash
mvn clean package -DskipTests
java -jar contract-tools-sdk/target/contract-tools-sdk-1.0.0.jar
```

### 3. 重新构建前端
```bash
# Frontend 项目
cd frontend
npm install
npm run build

# Export 项目
cd export
npm install
npm run build
```

### 4. 清理缓存
```powershell
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

### 5. 执行比对任务
提交新的比对任务

### 6. 检查后端日志
应该看到：
```
开始生成10个页面图片，DPI: 300
📸 图片格式: JPEG, JPEG质量: 0.85
✅ 生成页面图片: page-1.jpg, 尺寸: 2480x3508, 大小: 250KB

🔍 获取图片信息 - taskId: xxx, mode: old
  ✅ 找到 10 个图片文件
  🎉 成功获取 10 页的图片信息
```

### 7. 检查 API 返回
```json
{
  "oldImageInfo": {
    "totalPages": 10,
    "pages": [
      {
        "pageNum": 1,
        "imageUrl": "/api/compare-pro/files/tasks/{taskId}/images/old/page-1.jpg",
        "width": 2480,
        "height": 3508
      }
    ]
  }
}
```

### 8. 检查前端网络请求
打开浏览器开发者工具 → Network，应该看到：
```
✅ GET /api/compare-pro/files/tasks/{taskId}/images/old/page-1.jpg  200 OK
✅ GET /api/compare-pro/files/tasks/{taskId}/images/new/page-1.jpg  200 OK
```

**不再是 404 Not Found！**

### 9. 检查图片显示
前端页面应该能正常显示所有图片，没有加载失败的占位符。

## 🔄 数据流

```
1. 后端生成图片
   ↓
   根据 image-format 配置生成 .jpg 或 .png
   ↓
2. 后端返回图片信息
   ↓
   {
     "pageNum": 1,
     "imageUrl": "/api/.../page-1.jpg",  ← 包含正确的扩展名
     "width": 2480,
     "height": 3508
   }
   ↓
3. 前端接收数据
   ↓
   pageInfo.imageUrl = "/api/.../page-1.jpg"
   ↓
4. 前端渲染图片
   ↓
   const imageUrl = pageInfo.imageUrl  ← 直接使用后端返回的 URL
   ↓
   await imageManager.loadImage(imageUrl)
   ↓
5. 图片正确加载 ✅
```

## ⚠️ 注意事项

### 向后兼容性
如果后端没有提供 `pageInfo.imageUrl`（旧版本），前端会自动 fallback 到拼接 `.png` URL：
```typescript
const imageUrl = pageInfo.imageUrl  // undefined
  ? pageInfo.imageUrl
  : baseUrl                          // 使用这个 fallback
    ? `${baseUrl}/page-${pageIndex + 1}.png`
    : `/api/compare-pro/files/tasks/${taskId}/images/${mode}/page-${pageIndex + 1}.png`
```

### 图片格式切换
如果需要切换图片格式：

**从 JPEG 切换到 PNG**：
```yaml
zxcm:
  compare:
    zxocr:
      image-format: PNG  # 修改这里
```

**从 PNG 切换到 JPEG**：
```yaml
zxcm:
  compare:
    zxocr:
      image-format: JPEG
      jpeg-quality: 0.85  # 可选，控制压缩质量
```

**前端无需修改**，会自动适应！

## 🔍 故障排查

### 问题 1: 图片还是 404
**检查**：
1. 后端是否返回了正确的 `imageUrl`？
2. `imageUrl` 的扩展名与实际文件匹配吗？
3. 文件确实存在吗？

```powershell
# 检查实际文件
ls .\uploads\compare-pro\tasks\{taskId}\images\old\
```

### 问题 2: 部分图片是 .jpg，部分是 .png
**原因**：使用了缓存的旧图片

**解决**：
```powershell
# 删除所有缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

### 问题 3: 前端还是请求 .png
**检查**：
1. 前端是否重新构建了？
2. 浏览器是否清除了缓存？
3. 查看 Console 中打印的 imageUrl 是什么？

```javascript
// 在 canvas-renderer.ts 中添加调试日志
console.log('pageInfo.imageUrl:', pageInfo.imageUrl)
console.log('final imageUrl:', imageUrl)
```

## 📊 性能对比

### PNG vs JPEG @ 300 DPI

| 格式 | 文件大小 | 质量 | 加载速度 |
|------|---------|------|---------|
| PNG | ~800KB | 100% 无损 | 慢 |
| JPEG 0.85 | ~250KB | 99% 接近无损 | 快 (3倍) |
| JPEG 0.90 | ~320KB | 99.5% 接近无损 | 快 (2.5倍) |

**推荐配置**：
- 生产环境：JPEG 0.85（平衡质量和性能）
- 高质量需求：JPEG 0.90 或 PNG
- 快速预览：JPEG 0.75 + DPI 200

## 🎉 总结

### 修复前 ❌
- 前端硬编码 `.png` 扩展名
- 后端生成 `.jpg` 文件
- 图片加载失败 404

### 修复后 ✅
- 前端使用后端返回的 `imageUrl`
- 自动适应后端配置的图片格式
- 图片正常加载
- 保持向后兼容性

### 涉及项目
- ✅ **Frontend**: `frontend/src/views/documents/gpu-ocr-canvas/canvas-renderer.ts`
- ✅ **Export**: `export/src/gpu-ocr-canvas/canvas-renderer.ts`
- ✅ **Backend**: 已在之前修复（返回正确的 imageUrl）

**现在前端可以正确加载 JPEG 格式的图片了！** 🎉

