# OCR文本获取功能测试指南

## 问题描述

在OCR合同比对功能中，发现获取到的OCR识别结果文本是模拟数据：
- `oldText`: "OCR识别的文本内容 - 任务ID: OCR_1755230568612_7d97e815"
- `newText`: "OCR识别的文本内容 - 任务ID: OCR_1755230568613_f8bf3654"

## 问题分析

### 1. 原因分析
- `getOCRResultText()` 方法返回的是硬编码的模拟文本
- 没有读取真正的OCR识别结果文件
- OCR任务完成后，结果文件路径可能为空或无效

### 2. 修复内容
- 修改 `getOCRResultText()` 方法，从OCR结果文件中读取真实文本
- 支持读取 `combined.txt` 和分页文本文件
- 添加详细的日志记录和错误处理

## 测试步骤

### 1. 检查OCR任务状态
```bash
# 查看OCR任务是否正常完成
curl -X GET "http://localhost:8080/api/ocr-compare/task/{taskId}"
```

### 2. 检查OCR结果文件
```bash
# 查看OCR任务的结果路径
# 检查 resultPath 字段是否包含有效的文件路径
```

### 3. 验证文件内容
```bash
# 检查OCR结果目录中是否存在以下文件：
# - combined.txt (合并文本)
# - page_001.txt, page_002.txt 等 (分页文本)
```

### 4. 测试文本提取
```bash
# 提交新的OCR比对任务
curl -X POST "http://localhost:8080/api/ocr-compare/upload" \
  -F "oldFile=@old_contract.pdf" \
  -F "newFile=@new_contract.pdf" \
  -F "useOCR=true"
```

## 预期结果

### 修复前
```json
{
  "oldText": "OCR识别的文本内容 - 任务ID: OCR_1755230568612_7d97e815",
  "newText": "OCR识别的文本内容 - 任务ID: OCR_1755230568613_f8bf3654"
}
```

### 修复后
```json
{
  "oldText": "实际的合同文本内容...",
  "newText": "实际的合同文本内容..."
}
```

## 调试信息

### 1. 日志检查
查看应用日志，寻找以下关键信息：
```
成功读取OCR结果文件: /path/to/ocr/results, 内容长度: 1234 字符
成功读取OCR结果文件: /path/to/ocr/results, 文件数量: 3, 内容长度: 1234 字符
未找到OCR结果文本文件，任务ID: xxx, 结果路径: /path/to/result
读取OCR结果文件失败，任务ID: xxx, 结果路径: /path/to/result
```

### 2. 文件路径验证
确保OCR结果文件路径正确：
- 检查 `OCRTask.resultPath` 字段
- 验证文件路径是否存在
- 确认文件权限和可读性

### 3. 文件格式验证
确保OCR结果文件格式正确：
- 文本文件编码为UTF-8
- 文件内容不为空
- 文件结构符合预期

## 常见问题

### 1. 结果路径为空
**症状**: `resultPath` 字段为null或空字符串
**原因**: OCR任务未正确设置结果路径
**解决**: 检查OCR任务服务，确保任务完成后设置正确的结果路径

### 2. 文件不存在
**症状**: 结果路径存在但文件不存在
**原因**: OCR脚本未生成结果文件或路径配置错误
**解决**: 检查OCR脚本执行情况，验证输出路径配置

### 3. 文件读取失败
**症状**: 文件存在但读取失败
**原因**: 文件权限、编码或格式问题
**解决**: 检查文件权限，确认文件编码为UTF-8

### 4. 文本内容为空
**症状**: 成功读取文件但内容为空
**原因**: OCR识别失败或结果文件格式错误
**解决**: 检查OCR脚本日志，验证识别结果

## 验证方法

### 1. 单元测试
```java
@Test
public void testGetOCRResultText() {
    // 创建模拟的OCR任务
    OCRTask mockTask = new OCRTask();
    mockTask.setResultPath("/path/to/ocr/results");
    
    // 测试文本提取
    String result = getOCRResultText("test_task_id");
    assertNotNull(result);
    assertFalse(result.contains("OCR识别的文本内容 - 任务ID"));
}
```

### 2. 集成测试
```java
@Test
public void testOCRCompareWorkflow() {
    // 提交OCR比对任务
    String taskId = submitCompareTask(oldFile, newFile, options);
    
    // 等待任务完成
    waitForTaskCompletion(taskId);
    
    // 获取比对结果
    OCRCompareResult result = getCompareResult(taskId);
    
    // 验证结果
    assertNotNull(result);
    assertNotNull(result.getDifferences());
}
```

## 总结

通过修复 `getOCRResultText()` 方法，OCR合同比对功能现在能够：

✅ **读取真实OCR结果** - 从OCR结果文件中提取实际识别的文本
✅ **支持多种格式** - 兼容combined.txt和分页文本文件
✅ **提供详细日志** - 便于调试和问题排查
✅ **错误处理完善** - 优雅处理各种异常情况

修复后，比对功能将使用真实的OCR识别文本进行差异分析，而不是模拟数据。
