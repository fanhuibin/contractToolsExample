# 合同OCR文本提取功能文档

## 功能概述

合同OCR文本提取功能是一个独立的文档OCR识别模块，提供PDF文档的文本提取服务，支持页眉页脚过滤和图文对照显示。该功能基于现有的MinerU OCR引擎，提供高质量的文本识别和位置信息。

## 主要特性

1. **PDF上传与OCR识别**
   - 支持PDF格式文档上传
   - 基于MinerU OCR引擎进行高质量文本识别
   - 支持复杂文档内容（表格、公式、图片等）

2. **页眉页脚过滤**
   - 可选择忽略页眉页脚内容
   - 支持自定义页眉页脚高度百分比
   - 提高文本提取的准确性

3. **图文对照显示**
   - 左侧显示PDF原始图像
   - 右侧显示提取的文本内容
   - 支持Markdown格式和HTML表格显示
   - 支持图片与文本的bbox关联

4. **大文档支持**
   - 支持上百页的大型文档
   - 虚拟滚动技术优化性能
   - 按需加载页面图像

5. **多种显示模式**
   - 左右分栏模式：同时显示图片和文本
   - 仅图片模式：专注查看文档图像
   - 仅文本模式：专注查看提取文本

6. **文本操作**
   - 一键复制全部文本
   - 下载文本文件
   - 支持文本搜索和高亮

## 技术架构

### 后端架构

```
contract-tools-sdk/
└── src/main/java/com/zhaoxinms/contract/tools/ocr/
    ├── controller/
    │   └── OcrExtractController.java      # OCR提取控制器
    ├── service/
    │   ├── OcrExtractService.java         # 服务接口
    │   └── impl/
    │       └── OcrExtractServiceImpl.java # 服务实现
    └── model/                              # 数据模型（复用extract包）
```

### 前端架构

```
frontend/src/
├── api/
│   └── ocr-extract.ts                     # API接口定义
└── views/ocr/
    ├── OcrExtract.vue                     # 主页面
    └── components/
        └── MarkdownViewer.vue             # Markdown文本渲染器
```

### 核心依赖

- **后端**：
  - UnifiedOCRService：统一OCR服务
  - MinerUOCRService：MinerU OCR引擎
  - CharBox模型：字符位置信息

- **前端**：
  - CanvasViewer：图片显示组件（复用）
  - Element Plus：UI组件库
  - Vue 3：前端框架

## API接口

### 1. 上传PDF进行OCR提取

**请求**
```
POST /api/ocr/extract/upload
Content-Type: multipart/form-data

参数：
- file: PDF文件
- ignoreHeaderFooter: 是否忽略页眉页脚（默认：true）
- headerHeightPercent: 页眉高度百分比（默认：12.0）
- footerHeightPercent: 页脚高度百分比（默认：12.0）
```

**响应**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "uuid",
    "message": "文件上传成功，开始OCR提取..."
  }
}
```

### 2. 查询任务状态

**请求**
```
GET /api/ocr/extract/status/{taskId}
```

**响应**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "uuid",
    "status": "processing",
    "progress": 50,
    "message": "正在进行OCR识别...",
    "fileName": "contract.pdf"
  }
}
```

状态值：
- `processing`: 处理中
- `completed`: 完成
- `failed`: 失败

### 3. 获取OCR结果

**请求**
```
GET /api/ocr/extract/result/{taskId}
```

**响应**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ocrText": "提取的文本内容...",
    "totalPages": 10,
    "textLength": 5000,
    "charBoxCount": 5000,
    "provider": "mineru",
    "metadata": {
      "totalPages": 10,
      "imagesDir": "/path/to/images",
      "pageDimensions": [...]
    },
    "charBoxes": [...]  // 如果文件较小，直接返回
  }
}
```

### 4. 获取页面图片

**请求**
```
GET /api/ocr/extract/page-image/{taskId}/{pageNum}
```

**响应**
```
Content-Type: image/png 或 image/jpeg
图片二进制数据
```

### 5. 获取CharBox数据

**请求**
```
GET /api/ocr/extract/charboxes/{taskId}
```

**响应**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "page": 1,
      "ch": "合",
      "bbox": [10.5, 20.3, 30.2, 40.1],
      "category": "Text"
    },
    ...
  ]
}
```

### 6. 删除任务

**请求**
```
DELETE /api/ocr/extract/task/{taskId}
```

**响应**
```json
{
  "code": 200,
  "message": "任务删除成功"
}
```

## 使用说明

### 1. 访问功能

在系统首页点击"OCR文本提取"卡片，或直接访问路由 `/ocr-extract`

### 2. 上传文档

1. 点击上传区域或拖拽PDF文件到上传区域
2. 配置OCR选项：
   - 勾选"忽略页眉页脚"（推荐开启）
   - 设置页眉高度百分比（默认12%）
   - 设置页脚高度百分比（默认12%）
3. 系统自动开始OCR识别

### 3. 查看进度

上传后系统会显示处理进度：
- 文件上传
- OCR识别
- 处理结果
- 完成

### 4. 查看结果

识别完成后，可以：
- 在左侧查看PDF原始图像
- 在右侧查看提取的文本
- 切换显示模式（左右分栏/仅图片/仅文本）
- 复制文本到剪贴板
- 下载文本文件
- 点击文本查看对应的图片位置（TODO）
- 点击图片查看对应的文本内容（TODO）

## 配置说明

### 后端配置

在 `application.yml` 中配置：

```yaml
file:
  upload:
    root-path: ./uploads  # 文件上传根路径
```

OCR提取任务数据存储在：`{root-path}/ocr-extract-tasks/{taskId}/`

### 文件存储结构

```
uploads/
└── ocr-extract-tasks/
    └── {taskId}/
        ├── source.pdf              # 原始PDF文件
        ├── ocr_text.txt           # 提取的文本
        ├── char_boxes.json        # CharBox数据
        ├── metadata.json          # 元数据
        └── images/                # 页面图片
            ├── page-1.png
            ├── page-2.png
            └── ...
```

## 与现有功能的区别

| 特性 | OCR文本提取 | 规则提取 | 智能信息提取 |
|------|------------|---------|------------|
| **主要用途** | 纯OCR文本识别 | 基于模板规则提取字段 | AI智能提取字段 |
| **AI依赖** | 无 | 需要LLM | 需要LLM |
| **页眉页脚** | 支持忽略 | 支持忽略 | 不支持 |
| **图文对照** | 支持 | 支持 | 支持 |
| **字段提取** | 无 | 有（基于规则） | 有（AI智能） |
| **表格识别** | 有（Markdown/HTML） | 有 | 有 |
| **结果输出** | 纯文本 | 结构化字段 | 结构化字段 |

## 性能优化

1. **异步处理**：OCR识别采用异步方式，不阻塞上传请求
2. **虚拟滚动**：大文档使用虚拟滚动，只渲染可见页面
3. **按需加载**：图片按需加载，减少内存占用
4. **缓存机制**：任务状态使用内存缓存，减少IO操作
5. **文件压缩**：页面图片使用PNG/JPEG压缩

## 后续优化方向

1. **文本与图片关联**
   - [ ] 点击右侧文本，左侧图片高亮对应区域
   - [ ] 点击左侧图片，右侧文本滚动到对应位置

2. **增强编辑功能**
   - [ ] 支持文本编辑和修正
   - [ ] 支持表格格式调整
   - [ ] 支持导出多种格式（Word、Markdown等）

3. **批量处理**
   - [ ] 支持批量上传多个PDF
   - [ ] 支持批量导出

4. **历史记录**
   - [ ] 保存提取历史
   - [ ] 支持历史记录查询和重新查看

5. **OCR引擎选择**
   - [ ] 支持多种OCR引擎切换
   - [ ] 支持自定义OCR参数

## 故障排查

### 常见问题

1. **OCR识别失败**
   - 检查MinerU OCR服务是否正常运行
   - 检查PDF文件是否损坏
   - 查看后端日志获取详细错误信息

2. **图片无法显示**
   - 检查图片文件路径是否正确
   - 检查文件权限
   - 清除浏览器缓存重试

3. **任务状态长时间未更新**
   - 检查后端异步任务是否正常执行
   - 查看任务目录是否有写权限
   - 检查OCR服务是否超时

4. **CharBox数据为空**
   - 检查OCR结果中是否包含位置信息
   - 确认metadata中是否有charBoxes字段
   - 查看char_boxes.json文件是否生成

## 技术支持

如有问题，请联系开发团队或查看相关文档：
- 项目架构：`docs/architecture.md`
- 统一OCR服务：`docs/unified-ocr-service.md`
- API文档：`docs/api-documentation.md`

