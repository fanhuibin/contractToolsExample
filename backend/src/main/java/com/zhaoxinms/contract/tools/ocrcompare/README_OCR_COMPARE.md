# 🔍 **OCR版本文档比对系统**

## 📋 **系统概述**

基于RapidOCR的智能文档比对系统，实现了PDF文档的OCR识别、文本比对和结果标注的完整流程。

### **核心特性**
- ✅ **OCR识别**: 基于RapidOCR进行高精度文字识别
- ✅ **实时进度**: 前端实时显示OCR识别和比对进度
- ✅ **智能比对**: 复用现有比对算法，支持多种比对选项
- ✅ **PDF标注**: 在原PDF上高亮显示差异内容
- ✅ **在线查看**: 基于PDF.js的在线查看和导航功能

## 🏗️ **系统架构**

```
前端 (Vue3)
    ↓ HTTP API
后端 (Spring Boot)
    ↓ 文件操作
OCR服务 (Python RapidOCR)
    ↓ 识别结果
比对算法 (Java DiffUtil)
    ↓ 比对结果
PDF标注 (PDFBox)
    ↓ 标注文件
前端展示 (PDF.js)
```

## 🚀 **快速开始**

### **1. 环境要求**
- Java 8+
- Spring Boot 2.5+
- Python 3.7+
- RapidOCR-onnxruntime
- Vue 3 + Element Plus

### **2. 后端配置**

在 `application-ocr.yml` 中配置OCR参数：

```yaml
ocr:
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
    executable: python
    script: rapid_pdf_ocr.py
  settings:
    dpi: 150
    min-score: 0.5
    debug-mode: false
```

### **3. 前端路由**

系统已自动配置以下路由：
- `/ocr-compare` - OCR比对上传页面
- `/ocr-compare/result/:taskId` - OCR比对结果页面

### **4. API接口**

#### **提交比对任务**
```http
POST /api/ocr-compare/upload
Content-Type: multipart/form-data

oldFile: [PDF文件]
newFile: [PDF文件]
ignoreHeaderFooter: true
headerHeightMm: 20
footerHeightMm: 20
ignoreCase: true
ignoredSymbols: "_＿"
```

#### **查询任务状态**
```http
GET /api/ocr-compare/task/{taskId}/status
```

#### **获取比对结果**
```http
GET /api/ocr-compare/task/{taskId}/result
```

## 💻 **使用流程**

### **步骤1: 上传文件**
1. 访问 `/ocr-compare` 页面
2. 选择两个PDF文件（原始文件和新文件）
3. 配置比对选项（可选）
4. 点击"开始OCR比对"

### **步骤2: 监控进度**
- 系统会显示实时进度条
- 包含以下步骤：
  1. OCR识别旧文档 (0-40%)
  2. OCR识别新文档 (40-80%)
  3. 文档比对 (80-95%)
  4. PDF标注 (95-100%)

### **步骤3: 查看结果**
- 任务完成后自动跳转到结果页面
- 左右分屏显示两个PDF
- 右侧面板显示差异列表
- 支持差异导航和过滤

## 🔧 **核心组件**

### **后端组件**

#### **OCRCompareController**
- REST API控制器
- 处理文件上传和任务管理

#### **OCRCompareService**
- 核心业务逻辑
- 任务调度和流程控制

#### **OCRTextPositionProcessor**
- OCR结果处理器
- 文本提取和位置信息处理

#### **OCRPDFAnnotator**
- PDF标注器
- 基于比对结果添加高亮标注

### **前端组件**

#### **OCRCompare.vue**
- 文件上传和任务监控页面
- 实时进度显示

#### **OCRCompareResult.vue**
- 比对结果展示页面
- PDF查看和差异导航

## 📊 **数据流程**

### **1. 文件上传阶段**
```
用户上传PDF → 保存到临时目录 → 创建比对任务 → 返回任务ID
```

### **2. OCR识别阶段**
```
提交OCR任务 → Python脚本识别 → 返回JSON结果 → 更新任务进度
```

### **3. 文档比对阶段**
```
解析OCR结果 → 提取文本内容 → 执行比对算法 → 生成差异列表
```

### **4. PDF标注阶段**
```
分析差异位置 → 在PDF上添加标注 → 保存标注文件 → 生成访问URL
```

### **5. 结果展示阶段**
```
前端获取结果 → 加载PDF文件 → 显示差异列表 → 支持导航功能
```

## ⚙️ **配置选项**

### **比对选项**
- `ignoreHeaderFooter`: 忽略页眉页脚
- `headerHeightMm`: 页眉高度(毫米)
- `footerHeightMm`: 页脚高度(毫米)
- `ignoreCase`: 忽略大小写
- `ignoredSymbols`: 忽略的符号集

### **OCR选项**
- `dpi`: 图片DPI (默认150)
- `minScore`: 最小置信度 (默认0.5)
- `fastMode`: 快速模式
- `debugMode`: 调试模式

### **标注颜色**
- `insertRGB`: 新增内容颜色 (绿色)
- `deleteRGB`: 删除内容颜色 (红色)

## 🐛 **故障排除**

### **常见问题**

#### **1. OCR识别失败**
- 检查Python环境和RapidOCR安装
- 确认PDF文件格式正确
- 查看OCR任务日志

#### **2. 进度不更新**
- 检查Python脚本输出缓冲设置
- 确认WebSocket连接正常
- 查看浏览器控制台错误

#### **3. PDF无法显示**
- 检查PDF文件路径和权限
- 确认PDF.js配置正确
- 查看网络请求状态

### **日志位置**
- Java日志: 控制台输出
- Python日志: `compareScript/logs/` 目录
- OCR结果: `uploads/ocr-compare/results/` 目录

## 📈 **性能优化**

### **OCR识别优化**
- 合理设置DPI值 (推荐150-300)
- 使用快速模式处理简单文档
- 批量处理多个文档

### **比对算法优化**
- 启用大小写忽略减少差异
- 配置符号忽略过滤噪音
- 合理设置页眉页脚区域

### **前端性能优化**
- 使用虚拟滚动处理大量差异
- 延迟加载PDF内容
- 缓存比对结果

## 🔒 **安全考虑**

### **文件安全**
- 上传文件大小限制
- 文件类型验证
- 临时文件自动清理

### **路径安全**
- 防止路径遍历攻击
- 文件访问权限控制
- 任务ID格式验证

### **数据隐私**
- 敏感文档处理完成后自动删除
- 不记录文档内容到日志
- 支持自定义存储位置

## 🚀 **扩展功能**

### **支持的扩展**
- 多种文档格式支持 (Word, Excel等)
- 批量比对处理
- 比对结果导出
- 自定义比对规则
- 集成企业SSO认证

### **API扩展**
- WebHook通知
- 批量任务API
- 结果统计API
- 任务调度API

## 📞 **技术支持**

如遇到问题，请检查：
1. 环境配置是否正确
2. 依赖组件是否正常运行
3. 日志文件中的错误信息
4. 网络连接和权限设置

更多技术细节请参考源码注释和相关文档。
