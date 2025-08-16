# OCR比对功能修复测试说明

## 🐛 已修复的问题

### 问题描述
前台报错：`OCR比对失败: C:\Users\范慧斌\AppData\Local\Temp\...\work\Tomcat\localhost\ROOT\upload_...tmp (系统找不到指定的文件。)`

### 问题原因
1. **文件路径传递错误**: CompareController直接传递MultipartFile对象给OCR比对服务
2. **文件扩展名处理bug**: 新文件使用了旧文件的扩展名
3. **文件保存时机**: OCR比对服务期望文件路径，但文件还没有正确保存

### 修复内容
1. ✅ 添加了`submitCompareTaskWithPaths`方法，接受文件路径参数
2. ✅ 修复了文件扩展名处理bug
3. ✅ 确保文件先保存到本地，再传递路径给OCR比对服务

## 🚀 测试步骤

### 1. 重新编译和启动服务

#### 后端服务
```bash
cd sdk
mvn clean compile
mvn spring-boot:run
```

#### 前端服务
```bash
cd frontend
npm run dev
```

### 2. 测试OCR比对功能

#### 2.1 使用前端界面测试
1. 访问 `http://localhost:3000/ocr-compare`
2. 选择两个不同格式的文件（如：一个PDF，一个Word文档）
3. 设置比对选项
4. 点击"开始OCR比对"

#### 2.2 使用API测试
```bash
curl -X POST http://localhost:8080/api/compare/upload \
  -F "oldFile=@old_document.pdf" \
  -F "newFile=@new_document.docx" \
  -F "useOCR=true" \
  -F "ignoreSpaces=true" \
  -F "ignoreCase=true"
```

### 3. 验证修复效果

#### 3.1 检查响应
**修复前**（错误）:
```json
{
  "code": 500,
  "message": "OCR比对失败: C:\Users\...\tmp (系统找不到指定的文件。)"
}
```

**修复后**（正常）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "OCR_COMPARE_1234567890_abc12345",
    "message": "OCR比对任务已提交，请等待处理完成",
    "useOCR": true,
    "taskType": "OCR_COMPARE",
    "status": "PROCESSING"
  }
}
```

#### 3.2 检查任务状态
```bash
curl http://localhost:8080/api/compare/ocr-task/{taskId}/status
```

应该能看到任务正在处理中，而不是文件找不到的错误。

## 🔍 测试用例

### 测试用例1: 不同格式文件
- **旧文件**: PDF文档
- **新文件**: Word文档(.docx)
- **预期**: 成功提交OCR比对任务

### 测试用例2: 相同格式文件
- **旧文件**: PDF文档
- **新文件**: PDF文档
- **预期**: 成功提交OCR比对任务

### 测试用例3: Excel文件
- **旧文件**: Excel表格(.xlsx)
- **新文件**: Word文档(.docx)
- **预期**: 成功提交OCR比对任务

### 测试用例4: 大文件
- **文件大小**: >5MB
- **预期**: 正常处理，无文件路径错误

## 🐛 如果仍有问题

### 检查点1: 文件保存
检查CompareController是否正确保存了文件：
```java
// 在CompareController中添加日志
log.info("保存文件: oldSrc={}, newSrc={}", oldSrc.getAbsolutePath(), newSrc.getAbsolutePath());
log.info("文件是否存在: oldSrc.exists()={}, newSrc.exists()={}", oldSrc.exists(), newSrc.exists());
```

### 检查点2: 文件路径传递
检查传递给OCR比对服务的文件路径是否正确：
```java
// 在CompareController中添加日志
log.info("传递给OCR比对服务的文件路径: oldFilePath={}, newFilePath={}", oldFilePath, newFilePath);
```

### 检查点3: 文件权限
确保应用有权限访问保存文件的目录：
```bash
# 检查目录权限
ls -la uploads/
```

### 检查点4: 依赖注入
确保OCRCompareService正确注入：
```java
// 在CompareController中添加日志
log.info("OCR比对服务注入状态: {}", ocrCompareService != null ? "成功" : "失败");
```

## 📊 性能验证

### 文件上传速度
- **小文件** (<1MB): <2秒
- **中等文件** (1-10MB): <5秒
- **大文件** (>10MB): <15秒

### 任务提交响应
- **响应时间**: <3秒
- **错误率**: 0%
- **文件路径正确性**: 100%

## ✅ 测试检查清单

- [ ] 不同格式文件上传成功
- [ ] OCR比对任务提交成功
- [ ] 任务状态查询正常
- [ ] 无文件路径错误
- [ ] 文件扩展名处理正确
- [ ] 大文件处理正常
- [ ] 错误处理友好

## 🎉 修复完成

当所有测试用例都通过后，OCR比对功能的文件路径问题就完全解决了！

### 下一步
1. 测试完整的OCR比对流程
2. 验证比对结果生成
3. 优化性能和用户体验
4. 部署到生产环境
