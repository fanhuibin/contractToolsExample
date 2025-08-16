# OCR比对功能测试指南

## 🎯 测试目标

验证OCR比对功能是否正常工作，包括：
1. 文件上传和OCR比对任务提交
2. 任务状态查询
3. 比对结果获取
4. 任务管理（删除等）

## 🚀 测试步骤

### 1. 启动服务

#### 后端服务
```bash
cd sdk
mvn spring-boot:run
```

#### 前端服务
```bash
cd frontend
npm run dev
```

### 2. 测试OCR比对上传

#### 2.1 使用前端界面
1. 访问 `http://localhost:3000/ocr-compare`
2. 选择两个文件（支持PDF、Word、Excel）
3. 设置比对选项（忽略空格、大小写等）
4. 点击"开始OCR比对"

#### 2.2 使用API测试
```bash
curl -X POST http://localhost:8080/api/compare/upload \
  -F "oldFile=@old_document.pdf" \
  -F "newFile=@new_document.pdf" \
  -F "useOCR=true" \
  -F "ignoreSpaces=true" \
  -F "ignoreCase=true"
```

**预期响应**:
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

### 3. 测试任务状态查询

#### 3.1 查询单个任务状态
```bash
curl http://localhost:8080/api/compare/ocr-task/{taskId}/status
```

**预期响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "OCR_COMPARE_1234567890_abc12345",
    "status": "OCR_PROCESSING",
    "progress": 25.0,
    "currentStep": "OCR识别旧文档",
    "totalSteps": 5,
    "message": "正在处理中..."
  }
}
```

#### 3.2 查询所有任务
```bash
curl http://localhost:8080/api/compare/ocr-task/list
```

**预期响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "taskId": "OCR_COMPARE_1234567890_abc12345",
      "status": "COMPLETED",
      "progress": 100.0,
      "currentStep": "比对完成",
      "totalSteps": 5
    }
  ]
}
```

### 4. 测试比对结果获取

```bash
curl http://localhost:8080/api/compare/ocr-task/{taskId}/result
```

**预期响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "OCR_COMPARE_1234567890_abc12345",
    "oldPdfUrl": "/api/ocr-compare/files/.../old_annotated.pdf",
    "newPdfUrl": "/api/ocr-compare/files/.../new_annotated.pdf",
    "differences": [
      {
        "operation": "DELETE",
        "text": "删除的文本",
        "oldPosition": {"page": 1, "x": 100, "y": 200}
      },
      {
        "operation": "INSERT",
        "text": "新增的文本",
        "newPosition": {"page": 1, "x": 100, "y": 200}
      }
    ]
  }
}
```

### 5. 测试任务删除

```bash
curl -X DELETE http://localhost:8080/api/compare/ocr-task/{taskId}
```

**预期响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

## 🔍 测试用例

### 测试用例1: PDF文档OCR比对
- **文件类型**: PDF
- **内容**: 包含文字的合同文档
- **预期结果**: 成功识别文字并比对差异

### 测试用例2: Word文档OCR比对
- **文件类型**: .docx
- **内容**: 包含文字的合同文档
- **预期结果**: 自动转换为PDF，成功OCR识别和比对

### 测试用例3: Excel表格OCR比对
- **文件类型**: .xlsx
- **内容**: 包含文字的表格
- **预期结果**: 自动转换为PDF，成功OCR识别和比对

### 测试用例4: 大文件处理
- **文件大小**: >10MB
- **预期结果**: 正常处理，显示进度信息

### 测试用例5: 错误处理
- **无效文件**: 损坏的文档
- **预期结果**: 返回友好的错误信息

## 🐛 常见问题

### 问题1: OCR比对服务未配置
**症状**: 返回"OCR比对服务未配置"错误
**原因**: OCRCompareService依赖注入失败
**解决**: 检查OCRCompareService是否正确配置和扫描

### 问题2: 文件格式不支持
**症状**: 返回"不支持的文件格式"错误
**原因**: 上传的文件格式不在支持列表中
**解决**: 确保文件格式为PDF、Word或Excel

### 问题3: 任务状态查询失败
**症状**: 返回"任务不存在"错误
**原因**: 任务ID无效或任务已过期
**解决**: 使用正确的任务ID，检查任务是否还在有效期内

### 问题4: 比对结果为空
**症状**: 比对完成但differences数组为空
**原因**: 两个文档内容完全相同
**解决**: 这是正常情况，表示没有差异

## 📊 性能测试

### 响应时间测试
- **文件上传**: <5秒（10MB以内）
- **任务提交**: <2秒
- **状态查询**: <1秒
- **结果获取**: <3秒

### 并发测试
- **同时提交**: 10个OCR比对任务
- **预期结果**: 所有任务正常处理，无死锁

### 内存使用测试
- **大文件处理**: 100MB文档
- **预期结果**: 内存使用稳定，无内存泄漏

## ✅ 测试检查清单

- [ ] OCR比对任务提交成功
- [ ] 任务状态正确更新
- [ ] 进度信息准确显示
- [ ] 比对结果正确生成
- [ ] 错误处理友好
- [ ] 性能满足要求
- [ ] 并发处理正常
- [ ] 文件清理及时

## 🎉 测试完成

当所有测试用例都通过后，OCR比对功能就可以正式使用了！

### 下一步
1. 集成到生产环境
2. 添加监控和告警
3. 优化性能和用户体验
4. 收集用户反馈
