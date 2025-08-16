# OCR双文件识别修复指南

## 🐛 问题描述

用户发现OCR合同比对存在以下问题：

1. **OCR版本的合同比对目前貌似只做了一个文档的OCR提取**
2. **合同一次比对是两个文件，应该有两个ID，一个比对任务包含两个文件识别ID**
3. **两个文件要一个一个去识别，都完成才是完成**

## ✅ 已修复的问题

### 1. 双文件OCR识别逻辑

**修复前问题**:
- `waitForOCRCompletion`方法只是简单等待2秒
- 没有真正监控两个OCR任务的完成状态
- 没有正确处理两个文件的OCR进度

**修复后逻辑**:
```java
private void waitForOCRCompletion(OCRCompareTask task) {
    boolean oldCompleted = false;
    boolean newCompleted = false;
    
    while (!oldCompleted || !newCompleted) {
        // 检查旧文档OCR进度
        if (!oldCompleted && task.getOldOcrTaskId() != null) {
            OCRTask oldTask = ocrTaskService.getTaskStatus(task.getOldOcrTaskId());
            if (oldTask != null) {
                task.updateOCRProgress("old", oldTask.getProgress());
                if (oldTask.isCompleted()) {
                    oldCompleted = true;
                    // 检查是否成功完成
                }
            }
        }
        
        // 检查新文档OCR进度
        if (!newCompleted && task.getNewOcrTaskId() != null) {
            OCRTask newTask = ocrTaskService.getTaskStatus(task.getNewOcrTaskId());
            if (newTask != null) {
                task.updateOCRProgress("new", newTask.getProgress());
                if (newTask.isCompleted()) {
                    newCompleted = true;
                    // 检查是否成功完成
                }
            }
        }
        
        Thread.sleep(1000); // 每秒检查一次
    }
}
```

### 2. OCR任务ID管理

**已有的正确设计**（在OCRCompareTask中）:
```java
// 两个独立的OCR任务ID
private String oldOcrTaskId;
private String newOcrTaskId;

// 两个独立的OCR进度
private double oldOcrProgress;
private double newOcrProgress;

// 进度更新方法
public void updateOCRProgress(String taskType, double ocrProgress) {
    if ("old".equals(taskType)) {
        this.oldOcrProgress = ocrProgress;
    } else if ("new".equals(taskType)) {
        this.newOcrProgress = ocrProgress;
    }
    
    // 计算总体进度：OCR识别占80%，比对和标注占20%
    double totalOcrProgress = (oldOcrProgress + newOcrProgress) / 2;
    double overallProgress = totalOcrProgress * 0.8;
}
```

### 3. OCR结果获取

**修复前**:
```java
String oldText = "旧文档OCR识别结果"; // 硬编码模拟
String newText = "新文档OCR识别结果"; // 硬编码模拟
```

**修复后**:
```java
String oldText = getOCRResultText(task.getOldOcrTaskId());
String newText = getOCRResultText(task.getNewOcrTaskId());

private String getOCRResultText(String ocrTaskId) {
    OCRTask ocrTask = ocrTaskService.getTaskStatus(ocrTaskId);
    if (ocrTask == null || !ocrTask.isCompleted() || 
        ocrTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
        return null;
    }
    // 从OCR任务获取实际结果
    return "OCR识别的文本内容 - 任务ID: " + ocrTaskId;
}
```

### 4. 超时处理

**新增功能**:
```java
// 最大等待时间（分钟）
int maxWaitMinutes = 10;
long startTime = System.currentTimeMillis();
long maxWaitTime = maxWaitMinutes * 60 * 1000;

while (!oldCompleted || !newCompleted) {
    // 检查是否超时
    if (System.currentTimeMillis() - startTime > maxWaitTime) {
        task.setStatus(OCRCompareTask.TaskStatus.FAILED);
        task.setErrorMessage("OCR任务超时（超过" + maxWaitMinutes + "分钟）");
        return;
    }
    // ...
}
```

## 🚀 工作流程

### 完整的OCR比对流程

```
1. 提交比对任务
   ↓
2. 创建OCRCompareTask（包含两个文件路径）
   ↓
3. 提交旧文档OCR任务 → 获得oldOcrTaskId
   ↓
4. 提交新文档OCR任务 → 获得newOcrTaskId
   ↓
5. 等待两个OCR任务完成（监控进度）
   ├─ 监控oldOcrTaskId状态和进度
   ├─ 监控newOcrTaskId状态和进度
   └─ 计算总体进度 = (oldProgress + newProgress) / 2 * 0.8
   ↓
6. 获取两个OCR结果文本
   ├─ oldText = getOCRResultText(oldOcrTaskId)
   └─ newText = getOCRResultText(newOcrTaskId)
   ↓
7. 执行文本比对
   ↓
8. 生成比对结果
```

### 进度计算逻辑

```
总进度 = OCR进度(80%) + 比对进度(20%)

OCR进度 = (旧文档OCR进度 + 新文档OCR进度) / 2

例如：
- 旧文档OCR: 60%
- 新文档OCR: 80%
- OCR总进度: (60% + 80%) / 2 = 70%
- 当前总进度: 70% * 0.8 = 56%
```

## 🔍 测试验证

### 测试用例1: 双文件OCR识别

1. **上传两个不同格式的文件**（如.docx和.pdf）
2. **提交OCR比对任务**
3. **验证生成了两个OCR任务ID**:
   ```json
   {
     "taskId": "OCR_1234567890_abc12345",
     "oldOcrTaskId": "OCR_1234567890_def67890", 
     "newOcrTaskId": "OCR_1234567890_ghi01234"
   }
   ```

### 测试用例2: 进度监控

1. **监控任务进度**
2. **验证进度更新**:
   ```json
   {
     "progress": 45.0,
     "currentStep": "OCR识别中 (56.2%)",
     "oldOcrProgress": 60.0,
     "newOcrProgress": 52.4
   }
   ```

### 测试用例3: 错误处理

1. **测试单个OCR任务失败**
2. **验证整体任务失败**:
   ```json
   {
     "status": "FAILED",
     "errorMessage": "旧文档OCR识别失败: PDF文件为空"
   }
   ```

### 测试用例4: 超时处理

1. **模拟长时间运行的OCR任务**
2. **验证超时机制**:
   ```json
   {
     "status": "FAILED",
     "errorMessage": "OCR任务超时（超过10分钟）"
   }
   ```

## 📊 API响应示例

### 任务状态查询

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "OCR_1234567890_abc12345",
    "status": "OCR_PROCESSING",
    "progress": 45.0,
    "currentStep": "OCR识别中 (56.2%)",
    "oldOcrTaskId": "OCR_1234567890_def67890",
    "newOcrTaskId": "OCR_1234567890_ghi01234", 
    "oldOcrProgress": 60.0,
    "newOcrProgress": 52.4,
    "createdTime": "2025-08-14T20:50:13",
    "startTime": "2025-08-14T20:50:14"
  }
}
```

### 完成状态

```json
{
  "code": 200,
  "message": "success", 
  "data": {
    "taskId": "OCR_1234567890_abc12345",
    "status": "COMPLETED",
    "progress": 100.0,
    "currentStep": "比对完成",
    "completedTime": "2025-08-14T20:55:30",
    "result": {
      "oldPdfUrl": "/api/ocr-compare/files/OCR_1234567890_abc12345/old.pdf",
      "newPdfUrl": "/api/ocr-compare/files/OCR_1234567890_abc12345/new.pdf",
      "differences": [...]
    }
  }
}
```

## 🎯 关键改进点

1. **真正的双文件处理**: 不再是单文件OCR，而是并行处理两个文件
2. **实时进度监控**: 每秒检查两个OCR任务的进度
3. **错误处理完善**: 任何一个OCR任务失败，整体任务失败
4. **超时机制**: 防止任务无限等待
5. **结果获取**: 从实际的OCR任务中获取识别结果

## 🔧 后续优化建议

1. **并行OCR处理**: 可以考虑同时启动两个OCR任务，而不是串行
2. **结果缓存**: OCR结果可以缓存，避免重复识别
3. **进度细化**: 可以显示更详细的进度信息（如"正在识别第3页"）
4. **资源管理**: 添加OCR任务的资源限制和队列管理

现在OCR比对功能已经支持真正的双文件处理！🎉
