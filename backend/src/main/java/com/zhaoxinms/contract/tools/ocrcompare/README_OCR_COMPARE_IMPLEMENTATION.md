# OCR合同比对功能实现说明

## 概述

本实现基于非OCR版本的比对逻辑，为OCR合同比对提供了完整的文本比对、差异标注和结果回写功能。

## 核心功能

### 1. OCR文本识别
- 使用 `OCRTaskService` 对两个文档进行OCR识别
- 支持多种文档格式（PDF、Word、Excel等）
- 异步处理，支持进度监控

### 2. 文本比对算法
- 基于Google的diff-match-patch算法（`DiffUtil`）
- 支持忽略大小写、忽略符号等配置选项
- 智能语义清理，提高比对准确性

### 3. 差异结果回写
- 将比对结果以标注形式回写到PDF文件
- 删除内容用红色高亮标注
- 新增内容用绿色高亮标注
- 支持精确定位和内容说明

## 实现架构

### 核心类

#### OCRCompareService
- 主要的比对服务类
- 协调OCR识别、文本比对、结果回写等流程
- 支持异步任务处理

#### 关键方法

1. **performTextComparison()** - 执行文本比对
   ```java
   private List<CompareResult> performTextComparison(String oldText, String newText, OCRCompareOptions options)
   ```

2. **annotatePDFWithResults()** - 将比对结果回写到PDF
   ```java
   private void annotatePDFWithResults(String sourcePdfPath, String targetPdfPath, 
                                     List<CompareResult> results, String operationType)
   ```

3. **addAnnotationToPDF()** - 向PDF添加标注
   ```java
   private void addAnnotationToPDF(PDDocument document, CompareResult result, String operationType)
   ```

### 比对流程

```
1. 文件上传 → 2. OCR识别 → 3. 文本比对 → 4. 结果回写 → 5. 生成标注PDF
```

#### 详细步骤

1. **文件上传和验证**
   - 检查文件格式和大小
   - 保存到临时目录

2. **OCR识别**
   - 异步提交OCR任务
   - 监控OCR进度
   - 获取识别文本结果

3. **文本比对**
   - 使用DiffUtil进行文本差异分析
   - 应用配置选项（忽略大小写、符号等）
   - 生成差异结果列表

4. **结果回写**
   - 创建结果目录
   - 将差异标注回写到PDF
   - 生成带标注的PDF文件

5. **结果输出**
   - 返回标注后的PDF文件URL
   - 提供差异详情数据
   - 支持前端预览和下载

## 配置选项

### OCRCompareOptions
- `ignoreCase`: 是否忽略大小写
- `ignoreSpaces`: 是否忽略空格
- `ignoreHeaderFooter`: 是否忽略页眉页脚

### 比对算法配置
- `Diff_Timeout`: 比对超时时间
- `Diff_EditCost`: 编辑操作成本
- `Match_Threshold`: 匹配阈值

## 输出结果

### 文件输出
- `old_annotated.pdf`: 带删除标注的旧文档
- `new_annotated.pdf`: 带新增标注的新文档

### 数据输出
```json
{
  "taskId": "OCR_任务ID",
  "oldPdfUrl": "/api/ocr-compare/files/任务ID/old_annotated.pdf",
  "newPdfUrl": "/api/ocr-compare/files/任务ID/new_annotated.pdf",
  "differences": [
    {
      "operation": "DELETE",
      "text": "删除的文本内容",
      "page": 1,
      "x": 100,
      "y": 200
    },
    {
      "operation": "INSERT", 
      "text": "新增的文本内容",
      "page": 1,
      "x": 100,
      "y": 200
    }
  ],
  "summary": {
    "totalDifferences": 2,
    "deletions": 1,
    "insertions": 1
  }
}
```

## 技术特点

### 1. 异步处理
- 使用CompletableFuture实现异步任务
- 支持任务状态监控和进度跟踪
- 避免长时间阻塞用户请求

### 2. 错误处理
- 完善的异常捕获和处理
- 详细的错误日志记录
- 优雅的任务失败处理

### 3. 性能优化
- 文件流式处理，避免内存溢出
- 智能的文本预处理和过滤
- 支持大文档的比对处理

### 4. 扩展性
- 模块化的设计架构
- 可配置的比对选项
- 支持自定义标注样式

## 使用示例

### 提交比对任务
```java
OCRCompareOptions options = new OCRCompareOptions();
options.setIgnoreCase(true);
options.setIgnoreSpaces(false);

String taskId = ocrCompareService.submitCompareTask(oldFile, newFile, options);
```

### 查询任务状态
```java
OCRCompareTask task = ocrCompareService.getTaskStatus(taskId);
if (task.getStatus() == OCRCompareTask.TaskStatus.COMPLETED) {
    OCRCompareResult result = ocrCompareService.getCompareResult(taskId);
    // 处理比对结果
}
```

## 注意事项

### 1. 依赖要求
- 需要PDFBox库支持PDF操作
- 需要OCR服务正常运行
- 需要足够的磁盘空间存储结果文件

### 2. 性能考虑
- 大文档比对可能需要较长时间
- OCR识别是性能瓶颈，建议异步处理
- 结果文件会占用额外存储空间

### 3. 安全考虑
- 文件上传需要验证格式和大小
- 临时文件需要及时清理
- 结果文件访问需要权限控制

## 未来改进

### 1. 精确定位
- 集成OCR位置信息，实现精确定位
- 支持多页文档的差异标注
- 优化标注的视觉效果

### 2. 智能比对
- 支持语义层面的内容比对
- 集成AI模型提高比对准确性
- 支持表格、图片等复杂内容

### 3. 用户体验
- 实时进度显示
- 支持比对结果的在线预览
- 提供差异内容的导出功能

## 总结

本实现提供了完整的OCR合同比对功能，包括：

✅ **OCR文本识别** - 支持多种文档格式的文本提取
✅ **智能文本比对** - 基于成熟算法的差异分析
✅ **结果可视化** - PDF标注和差异高亮显示
✅ **异步处理** - 支持大文档的长时间处理
✅ **配置灵活** - 可自定义比对选项和标注样式

通过这个实现，用户可以：
1. 上传两个版本的合同文档
2. 系统自动进行OCR识别和文本比对
3. 获得带标注的PDF文件，清晰显示所有差异
4. 通过API获取详细的比对结果数据

这为合同版本管理、法律文档审查等场景提供了强大的技术支持。
