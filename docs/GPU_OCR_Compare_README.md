# GPU OCR 文档比对功能

## 概述

GPU OCR 文档比对功能基于先进的AI模型和GPU加速技术，为用户提供高效、准确的文档比对服务。该功能集成了完整的OCR文字识别、差异分析和PDF标注功能。

## 核心特性

### 🚀 性能优势
- **GPU加速**: 利用GPU并行处理能力，大幅提升OCR识别速度
- **智能识别**: 基于深度学习模型，提供高精度的文字识别
- **实时处理**: 支持实时进度监控和状态更新

### 🎯 功能特性
- **多格式支持**: 支持PDF、Word、Excel等常见文档格式
- **智能过滤**: 自动过滤页眉页脚、符号、空格等干扰内容
- **精确标注**: 在PDF上精确标注差异位置
- **调试模式**: 支持使用已有OCR结果进行调试

### 📊 比对流程
1. **文件上传**: 用户上传两个文档文件
2. **GPU OCR识别**: 使用GPU加速进行文字识别
3. **文本比对**: 基于diff算法进行差异分析
4. **结果过滤**: 应用自定义过滤规则
5. **PDF标注**: 在原始PDF上标注差异位置
6. **结果展示**: 提供直观的比对结果界面

## 技术架构

### 后端架构

#### Controller层
- `GPUOCRCompareController`: 主要的REST API接口
  - `POST /api/gpu-ocr-compare/submit`: 提交比对任务
  - `GET /api/gpu-ocr-compare/task/{taskId}`: 获取任务状态
  - `GET /api/gpu-ocr-compare/result/{taskId}`: 获取比对结果
  - `GET /api/gpu-ocr-compare/tasks`: 获取所有任务
  - `DELETE /api/gpu-ocr-compare/task/{taskId}`: 删除任务
  - `POST /api/gpu-ocr-compare/debug-compare`: 调试模式比对

#### 服务层
- `GPUOCRCompareService`: 核心业务逻辑服务
  - 任务管理: 创建、监控、删除比对任务
  - 异步处理: 使用线程池进行异步任务执行
  - 结果缓存: 任务状态和结果的内存缓存

#### 数据模型
- `GPUOCRCompareTask`: 任务状态和进度信息
- `GPUOCRCompareResult`: 比对结果数据结构
- `GPUOCRCompareOptions`: 比对配置选项

### 前端架构

#### 主要组件
- `GPUOCRCompare.vue`: 比对任务提交和监控界面
- `GPUOCRCompareResult.vue`: 比对结果展示界面

#### 功能模块
- **文件上传**: 支持拖拽和点击上传
- **进度监控**: 实时显示处理进度和状态
- **结果导航**: 支持差异项快速定位
- **PDF查看**: 集成PDF.js进行文档查看
- **调试支持**: 提供调试模式功能

## 使用指南

### 基本使用流程

1. **访问比对页面**
   ```
   路由: /gpu-ocr-compare
   ```

2. **上传文档**
   - 点击或拖拽上传两个需要比对的文档
   - 支持PDF、Word、Excel格式
   - 文件大小限制根据后端配置

3. **配置比对选项**
   - 忽略页眉页脚: 设置页眉页脚高度
   - 忽略大小写: 是否进行大小写敏感比对
   - 忽略符号: 设置需要忽略的符号集
   - 忽略空格: 是否忽略空格差异
   - 忽略印章: 是否过滤印章相关内容

4. **开始比对**
   - 点击"开始GPU OCR比对"按钮
   - 系统将显示处理进度

5. **查看结果**
   - 比对完成后点击"查看比对结果"
   - 在结果页面中查看详细差异
   - 支持差异项筛选和快速定位

### 高级功能

#### 调试模式
- 在比对页面点击"调试模式"按钮
- 输入已完成的OCR任务ID
- 直接使用已有OCR结果进行比对
- 适用于测试和调试场景

#### 任务管理
- 在任务历史中查看所有比对任务
- 支持任务状态监控和删除
- 可以同时运行多个比对任务

## API 文档

### 提交比对任务
```http
POST /api/gpu-ocr-compare/submit
Content-Type: multipart/form-data

Form Data:
- oldFile: 原文档文件
- newFile: 新文档文件
- ignoreHeaderFooter: 是否忽略页眉页脚
- headerHeightMm: 页眉高度(mm)
- footerHeightMm: 页脚高度(mm)
- ignoreCase: 是否忽略大小写
- ignoredSymbols: 忽略符号集
- ignoreSpaces: 是否忽略空格
- ignoreSeals: 是否忽略印章
```

响应示例:
```json
{
  "success": true,
  "taskId": "uuid-string",
  "message": "GPU OCR比对任务提交成功"
}
```

### 获取任务状态
```http
GET /api/gpu-ocr-compare/task/{taskId}
```

响应示例:
```json
{
  "success": true,
  "task": {
    "taskId": "uuid-string",
    "status": "COMPLETED",
    "statusDesc": "完成",
    "progress": 100,
    "totalSteps": 8,
    "currentStep": 8,
    "currentStepDesc": "完成比对",
    "createdTime": "2024-01-01T10:00:00",
    "oldFileName": "contract_old.pdf",
    "newFileName": "contract_new.pdf",
    "oldPdfUrl": "/files/old.pdf",
    "newPdfUrl": "/files/new.pdf",
    "annotatedOldPdfUrl": "/files/annotated_old.pdf",
    "annotatedNewPdfUrl": "/files/annotated_new.pdf"
  }
}
```

## 配置说明

### 后端配置

#### GPU OCR服务配置
```yaml
gpu-ocr:
  base-url: "http://192.168.0.100:8000"
  model: "ocr-model"
  timeout: 300000
  render-dpi: 300
```

#### 比对选项默认值
```yaml
compare:
  default-options:
    ignore-header-footer: true
    header-height-mm: 20
    footer-height-mm: 20
    ignore-case: true
    ignored-symbols: "_＿"
    ignore-spaces: false
    ignore-seals: true
```

### 前端配置

#### 路由配置
```typescript
{
  path: '/gpu-ocr-compare',
  name: 'GPUOCRCompare',
  component: () => import('@/views/documents/GPUOCRCompare.vue')
},
{
  path: '/gpu-ocr-compare/result/:taskId',
  name: 'GPUOCRCompareResult',
  component: () => import('@/views/documents/GPUOCRCompareResult.vue')
}
```

## 错误处理

### 常见错误及解决方法

#### 1. 文件上传失败
**错误信息**: "文件上传失败"
**可能原因**:
- 文件大小超过限制
- 文件格式不支持
- 网络连接问题

**解决方案**:
- 检查文件大小和格式
- 确认网络连接正常
- 重试上传操作

#### 2. OCR识别失败
**错误信息**: "OCR识别过程出错"
**可能原因**:
- GPU服务不可用
- 文档质量太差
- 模型加载失败

**解决方案**:
- 检查GPU服务状态
- 尝试使用更清晰的文档
- 联系技术支持

#### 3. 比对超时
**错误信息**: "比对任务超时"
**可能原因**:
- 文档页数过多
- 服务器负载过高
- 网络延迟较大

**解决方案**:
- 尝试分割大文档
- 在低峰期重试
- 联系技术支持

## 性能优化

### 服务端优化
- **异步处理**: 使用线程池进行异步任务处理
- **内存管理**: 及时清理临时文件和缓存
- **连接池**: 使用HTTP连接池复用连接
- **GPU资源**: 合理分配GPU计算资源

### 前端优化
- **分块加载**: 大文件分块上传
- **进度监控**: 实时进度更新减少用户等待焦虑
- **缓存策略**: 合理使用浏览器缓存
- **错误重试**: 自动重试失败的请求

## 扩展开发

### 添加新的过滤规则
1. 在`GPUOCRCompareOptions`中添加新选项
2. 在`DiffProcessingUtil.getIgnoreReason()`中实现过滤逻辑
3. 更新前端配置界面

### 集成新的OCR引擎
1. 创建新的OCR客户端实现
2. 更新`GPUOCRCompareService`中的OCR调用逻辑
3. 添加相应的配置选项

### 自定义标注样式
1. 修改PDF标注逻辑
2. 支持不同的颜色和样式配置
3. 添加标注模板功能

## 监控和日志

### 关键指标监控
- 任务处理成功率
- 平均处理时间
- GPU利用率
- 内存使用情况
- 错误率统计

### 日志记录
- 任务开始和结束日志
- 错误详细信息记录
- 性能指标记录
- 用户操作审计日志

## 安全考虑

### 数据安全
- 文件上传大小限制
- 文件类型验证
- 临时文件安全清理
- 敏感信息过滤

### 访问控制
- API访问权限控制
- 文件访问权限验证
- 操作审计记录

## 故障排除

### 常见问题排查
1. 检查服务状态和日志
2. 验证配置文件正确性
3. 确认网络连接正常
4. 检查磁盘空间充足

### 性能问题诊断
1. 监控系统资源使用
2. 分析处理时间瓶颈
3. 检查GPU使用情况
4. 优化配置参数

---

## 更新日志

### v1.0.0 (2024-01-XX)
- ✅ 初始版本发布
- ✅ 基本GPU OCR比对功能
- ✅ 支持多格式文档
- ✅ 实时进度监控
- ✅ PDF差异标注
- ✅ 调试模式支持
