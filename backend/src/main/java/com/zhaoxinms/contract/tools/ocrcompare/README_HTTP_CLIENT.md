# OCR HTTP客户端使用说明

## 概述

OCR HTTP客户端是一个Java组件，用于通过REST API调用Python OCR服务。它替代了原来的命令行调用方式，提供了更稳定、更灵活的OCR集成方案。

## 主要特性

✅ **HTTP REST接口** - 通过HTTP调用Python OCR服务
✅ **异步任务处理** - 支持长时间运行的OCR任务
✅ **自动重试和超时** - 内置错误处理和超时机制
✅ **进度监控** - 实时查询任务进度
✅ **结果获取** - 自动获取OCR识别结果
✅ **历史管理** - 查询和清理历史任务
✅ **健康检查** - 服务可用性检测

## 架构说明

```
Java应用 ←→ OCRHttpClient ←→ Python OCR服务 (端口9898)
                ↓
         OCRTaskService
                ↓
         JavaOCR (对外接口)
```

## 配置说明

在 `application-ocr.yml` 中添加OCR服务配置：

```yaml
ocr:
  service:
    url: http://localhost:9898     # Python OCR服务地址
    timeout: 30000                 # HTTP请求超时时间（毫秒）
  
  # 通用OCR参数
  settings:
    dpi: 150
    minScore: 0.5
  
  # 任务参数
  task:
    timeout: 20
    resultRetentionDays: 7
```

## 使用方法

### 1. 基本OCR任务

```java
@Autowired
private JavaOCR javaOCR;

// 提交OCR任务
String taskId = javaOCR.submitOCRTask("/path/to/document.pdf");

// 等待任务完成
OCRTask task = javaOCR.waitForTaskCompletion(taskId, 30);

// 读取OCR结果
JsonNode result = javaOCR.readOCRResult(taskId);

// 显示结果
javaOCR.displayTaskResult(taskId, true);
```

### 2. 使用HTTP客户端直接调用

```java
@Autowired
private OCRHttpClient ocrHttpClient;

// 检查服务状态
boolean isHealthy = ocrHttpClient.healthCheck();

// 提交OCR任务
Map<String, Object> options = new HashMap<>();
options.put("dpi", 150);
options.put("min_score", 0.5);

String taskId = ocrHttpClient.submitOCRTask("/path/to/document.pdf", "pdf", options);

// 查询任务状态
JsonNode status = ocrHttpClient.getTaskStatus(taskId);

// 等待任务完成
JsonNode result = ocrHttpClient.waitForTaskCompletion(taskId, 30);

// 获取OCR结果
JsonNode ocrResult = ocrHttpClient.getOCRResult(taskId);
```

### 3. 历史任务管理

```java
// 查询历史任务
JsonNode history = ocrHttpClient.getOCRHistory(1, 20, "completed");

// 清除历史数据
ocrHttpClient.clearOCRHistory(new String[]{"task1", "task2"}, false);
ocrHttpClient.clearOCRHistory(null, true); // 清除所有
```

## API接口说明

### OCRHttpClient 主要方法

| 方法 | 说明 | 参数 |
|------|------|------|
| `healthCheck()` | 健康检查 | 无 |
| `submitOCRTask()` | 提交OCR任务 | filePath, fileType, options |
| `getTaskStatus()` | 查询任务状态 | taskId |
| `getOCRResult()` | 获取OCR结果 | taskId |
| `waitForTaskCompletion()` | 等待任务完成 | taskId, timeoutMinutes |
| `getOCRHistory()` | 查询历史任务 | page, size, status |
| `clearOCRHistory()` | 清除历史数据 | taskIds, clearAll |

### JavaOCR 主要方法

| 方法 | 说明 | 参数 |
|------|------|------|
| `submitOCRTask()` | 提交OCR任务 | pdfPath |
| `getTaskStatus()` | 查询任务状态 | taskId |
| `waitForTaskCompletion()` | 等待任务完成 | taskId, timeoutMinutes |
| `readOCRResult()` | 读取OCR结果 | taskId |
| `displayTaskResult()` | 显示任务结果 | taskId, showDetails |
| `getOCRServiceStatus()` | 获取服务状态 | 无 |
| `getOCRHistory()` | 查询历史任务 | page, size, status |
| `clearOCRHistory()` | 清除历史数据 | taskIds, clearAll |

## 错误处理

### 常见错误及解决方案

1. **OCR服务不可用**
   ```
   错误: OCR服务不可用，请确保Python OCR服务已启动
   解决: 启动Python OCR服务 (python rapid_pdf_ocr_server.py)
   ```

2. **任务提交失败**
   ```
   错误: OCR任务提交失败: HTTP状态码: 500
   解决: 检查Python服务日志，确认文件路径正确
   ```

3. **任务超时**
   ```
   错误: 等待任务完成超时: taskId
   解决: 增加超时时间，检查PDF文件大小和复杂度
   ```

4. **网络连接问题**
   ```
   错误: 连接超时
   解决: 检查网络配置，确认端口9898可访问
   ```

## 测试和调试

### 1. 使用测试类

```java
@Autowired
private OCRHttpClientTest test;

// 快速连接测试
test.runQuickConnectionTest();

// 完整功能测试
test.runFullTest("/path/to/test.pdf");
```

### 2. 手动测试

```bash
# 测试Python OCR服务健康状态
curl http://localhost:9898/health

# 测试任务提交
curl -X POST http://localhost:9898/api/ocr/submit \
  -H "Content-Type: application/json" \
  -d '{"file_source":"local","file_path":"/path/to/test.pdf","file_type":"pdf"}'
```

## 性能优化建议

1. **并发控制**: 避免同时提交过多OCR任务
2. **超时设置**: 根据PDF复杂度调整超时时间
3. **结果缓存**: 对于重复文档，可以实现结果缓存
4. **异步处理**: 使用异步方法避免阻塞主线程

## 监控和日志

### 1. 服务监控

- 定期调用 `healthCheck()` 监控服务状态
- 监控任务执行时间和成功率
- 记录错误日志和异常情况

### 2. 日志配置

在 `logback.xml` 中添加OCR相关日志：

```xml
<logger name="com.zhaoxinms.contract.tools.ocrcompare" level="INFO"/>
```

## 迁移指南

### 从命令行方式迁移

1. **启动Python OCR服务**
   ```bash
   cd compareScript
   python rapid_pdf_ocr_server.py
   ```

2. **更新Java配置**
   - 添加OCR服务配置
   - 注入OCRHttpClient

3. **修改代码调用**
   - 替换命令行调用为HTTP客户端调用
   - 更新错误处理逻辑

4. **测试验证**
   - 运行连接测试
   - 验证OCR功能正常

## 总结

OCR HTTP客户端提供了更稳定、更灵活的OCR集成方案，主要优势包括：

- **解耦**: Java和Python服务完全解耦
- **稳定**: HTTP接口比命令行更稳定
- **灵活**: 支持异步处理、进度监控等
- **可扩展**: 易于添加新功能和优化
- **易维护**: 统一的错误处理和日志记录

通过这个客户端，Java应用可以轻松集成OCR功能，提升文档处理能力。
