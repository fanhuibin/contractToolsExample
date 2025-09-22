# GPUOCRCompareService 日志输出清理总结

## 修改概述

本次修改的目标是统一管理 `GPUOCRCompareService` 中的日志输出，将杂乱的 `System.out.println` 输出替换为通过 `CompareTaskProgressManager` 管理的结构化日志。

## 主要修改内容

### 1. 添加日志基础设施
- 添加 `Logger` 和 `LoggerFactory` 导入
- 为 `GPUOCRCompareService` 类添加静态 logger 字段

### 2. 清理的输出类型

#### 2.1 文件操作相关输出
**修改前:**
```java
System.out.println("文件已保存到系统上传目录:");
System.out.println("  原文档: " + oldFilePath.toAbsolutePath());
System.out.println("  新文档: " + newFilePath.toAbsolutePath());
```

**修改后:**
```java
logger.info("文件已保存到系统上传目录:");
logger.info("  原文档: {}", oldFilePath.toAbsolutePath());
logger.info("  新文档: {}", newFilePath.toAbsolutePath());
```

#### 2.2 预处理信息输出
**修改前:**
```java
System.out.println("[PREPROCESS] before length=" + text.length());
System.out.println("[PREPROCESS] after length=" + normalized.length());
```

**修改后:**
```java
logger.debug("[PREPROCESS] before length={}", text.length());
logger.debug("[PREPROCESS] after length={}", normalized.length());
```

#### 2.3 OCR过程详细输出
**修改前:**
```java
System.out.println("文档页数: " + pageCount + ", 使用固定DPI: " + dpi);
System.out.println("第" + page + "页图片尺寸: " + imgW + "x" + imgH);
System.out.println("OCR单页完成: file=" + fileName + ", page=" + page + ", 用时=" + pageCost + "ms");
System.out.println("OCR识别完成: file=" + fileName + ", 页数=" + pages + ", 总用时=" + ocrAllCost + "ms, 平均每页=" + avg + "ms");
```

**修改后:**
```java
// 通过 progressManager 的调试模式输出
if (progressManager != null) {
    progressManager.logStepDetail("第{}页图片尺寸: {}x{}", page, imgW, imgH);
    progressManager.logStepDetail("OCR单页完成: file={}, page={}, 用时={}ms", fileName, page, pageCost);
    progressManager.logStepDetail("OCR识别完成: file={}, 页数={}, 总用时={}ms, 平均每页={:.1f}ms", fileName, pages, ocrAllCost, avg);
}
```

#### 2.4 图片信息获取输出
**修改前:**
```java
System.out.println("获取文档图片信息 - 任务ID: " + taskId + ", 模式: " + mode);
System.out.println("上传根路径: " + uploadRootPath);
System.out.println("图片目录路径: " + imagesDir);
System.out.println("图片目录是否存在: " + Files.exists(imagesDir));
System.out.println("获取文档图片信息完成: " + mode + ", 共" + docInfo.getTotalPages() + "页");
```

**修改后:**
```java
logger.debug("获取文档图片信息 - 任务ID: {}, 模式: {}", taskId, mode);
logger.debug("上传根路径: {}", uploadRootPath);
logger.debug("图片目录路径: {}", imagesDir);
logger.debug("图片目录是否存在: {}", Files.exists(imagesDir));
logger.debug("获取文档图片信息完成: {}, 共{}页", mode, docInfo.getTotalPages());
```

#### 2.5 处理流程相关输出
**修改前:**
```java
System.out.println("转换DiffBlock格式，merged大小: " + merged.size());
System.out.println("创建前端结果对象...");
System.out.println("保存结果到缓存...");
```

**修改后:**
```java
// 转换DiffBlock格式的信息通过进度管理器输出
// 创建前端结果对象的信息通过进度管理器输出  
// 保存结果到缓存的信息通过进度管理器输出
```

### 3. 方法签名增强

#### 3.1 增加 progressManager 参数支持
为以下方法添加了 `CompareTaskProgressManager` 参数的重载版本：
- `recognizePdfAsCharSeq()`
- `parseOnePage()`
- `calculatePageHeights()`

#### 3.2 向后兼容
保留了原有的方法签名，通过重载实现向后兼容：
```java
private RecognitionResult recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt,
        boolean resumeFromStep4, GPUOCRCompareOptions options) throws Exception {
    return recognizePdfAsCharSeq(client, pdf, prompt, resumeFromStep4, options, null);
}

private RecognitionResult recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt,
        boolean resumeFromStep4, GPUOCRCompareOptions options, CompareTaskProgressManager progressManager) throws Exception {
    // 实际实现
}
```

## 输出级别分类

### 正常模式输出 (INFO级别)
- 文档基本信息
- 各步骤进度和耗时
- 统计摘要信息
- 错误信息

### 调试模式输出 (DEBUG级别)
- 详细的OCR过程信息
- 图片尺寸和处理细节
- 文件路径和目录信息
- 预处理长度信息
- JSON修复过程信息

## 效果对比

### 修改前的输出（杂乱）
```
文件已保存到系统上传目录:
  原文档: D:\git\...\old_document.pdf
  新文档: D:\git\...\new_document.pdf
[2025-09-20 17:51:09] 线程 GPU-OCR-Worker-1 开始执行任务
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - 步骤 1: 初始化
[old] 使用固定DPI: 160
[old] OCR图片已保存: .\uploads\...\page-1.png
文档页数: 3, 使用固定DPI: 160
第1页图片尺寸: 1322x1868
OCR单页完成: file=old_document.pdf, page=1, 用时=10317ms
[PREPROCESS] before length=1495
[PREPROCESS] after length=1495
转换DiffBlock格式，merged大小: 9
创建前端结果对象...
保存结果到缓存...
获取文档图片信息 - 任务ID: xxx, 模式: old
Canvas前端结果创建成功，包含图片信息
```

### 修改后的输出（结构化）

#### 正常模式：
```
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - 开始文档比对: old_document.pdf vs new_document.pdf
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - 步骤 1: 初始化
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - 完成步骤 1: 初始化 (5ms)
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - 步骤 2: OCR识别第一个文档
2025-09-20 17:51:33 [GPU-OCR-Worker-1] INFO  - 完成步骤 2: OCR识别第一个文档 (24661ms)
2025-09-20 17:51:33 [GPU-OCR-Worker-1] INFO  - 步骤 3: OCR识别第二个文档
2025-09-20 17:51:56 [GPU-OCR-Worker-1] INFO  - 完成步骤 3: OCR识别第二个文档 (22603ms)
2025-09-20 17:51:56 [GPU-OCR-Worker-1] INFO  - OCR完成: 1495 字符 vs 1445 字符 (耗时: 47269ms)
2025-09-20 17:51:56 [GPU-OCR-Worker-1] INFO  - 差异分析: 原始差异块=94, 过滤后=94, 合并后=9
2025-09-20 17:51:56 [GPU-OCR-Worker-1] INFO  - 验证统计: 总块数=9, 符合条件=9, 总页数=3, 页数阈值=6 (未触发验证)
2025-09-20 17:51:56 [GPU-OCR-Worker-1] INFO  - 任务 xxx 完成，总耗时: 47281ms
```

#### 调试模式：
```
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] 开始调试比对任务: xxx
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] 开始步骤 1: 初始化
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] 文档页数信息: 旧文档3页, 新文档3页, 使用最大值3页
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] 第1页图片尺寸: 1322x1868
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] OCR单页完成: file=old_document.pdf, page=1, 用时=10317ms
2025-09-20 17:51:09 [GPU-OCR-Worker-1] INFO  - [DEBUG] OCR识别完成: file=old_document.pdf, 页数=3, 总用时=18331ms, 平均每页=6110.3ms
```

## 优势

1. **结构化输出**: 统一的日志格式，易于阅读和解析
2. **级别控制**: 通过日志级别控制输出详细程度
3. **性能友好**: 避免在生产环境输出过多调试信息
4. **可维护性**: 集中的日志管理，便于后续修改和扩展
5. **向后兼容**: 保留原有方法签名，不影响现有调用

## 使用方式

### 正常比对（简洁输出）
```java
CompareTaskProgressManager progressManager = new CompareTaskProgressManager(task, false);
```

### 调试比对（详细输出）
```java
CompareTaskProgressManager progressManager = new CompareTaskProgressManager(task, true);
```

通过这次重构，用户在正常使用时将看到简洁、结构化的进度信息，而在需要调试时可以获得详细的技术信息。
