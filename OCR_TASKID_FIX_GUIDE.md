# OCR比对任务ID修复测试指南

## 🐛 已修复的问题

### 问题描述
前端调用进度查询接口时出现`undefined`：
```
http://localhost:3000/api/compare/ocr-task/undefined/status
```

### 问题原因
1. **响应结构不匹配**: 前端期望`res.data.taskId`，但后端返回`res.data.id`
2. **参数验证缺失**: 没有验证taskId参数的有效性
3. **错误处理不完善**: 当获取不到任务ID时，仍然尝试监控进度

### 修复内容
1. ✅ 修复了任务ID获取逻辑，支持多种响应结构
2. ✅ 添加了taskId参数验证，防止undefined传递
3. ✅ 增加了调试日志，便于问题排查
4. ✅ 完善了错误处理，提供友好的用户提示

## 🚀 测试步骤

### 1. 重新启动前端服务

```bash
cd frontend
npm run dev
```

### 2. 测试OCR比对功能

#### 2.1 使用前端界面测试
1. 访问 `http://localhost:3000/ocr-compare`
2. 选择两个文件（支持PDF、Word、Excel）
3. 设置比对选项
4. 点击"开始OCR比对"

#### 2.2 检查浏览器控制台
打开浏览器开发者工具，查看Console标签页，应该能看到以下日志：

**成功提交任务时**:
```
OCR比对响应: {code: 200, message: "操作成功", data: {...}}
获取到的任务ID: OCR_COMPARE_1234567890_abc12345
开始监控任务: OCR_COMPARE_1234567890_abc12345
查询任务状态: OCR_COMPARE_1234567890_abc12345
```

**如果仍有问题**:
```
OCR比对失败: Error: 无法获取任务ID，响应格式异常
```

### 3. 验证API调用

#### 3.1 使用curl测试上传
```bash
curl -X POST http://localhost:3000/api/compare/upload \
  -F "oldFile=@old_document.pdf" \
  -F "newFile=@new_document.docx" \
  -F "useOCR=true" \
  -F "ignoreSpaces=true" \
  -F "ignoreCase=true"
```

#### 3.2 检查响应结构
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

**注意**: 任务ID在`data.id`字段中，不是`data.taskId`

#### 3.3 测试进度查询
使用返回的任务ID查询进度：
```bash
curl http://localhost:3000/api/compare/ocr-task/OCR_COMPARE_1234567890_abc12345/status
```

## 🔍 问题排查

### 如果仍有undefined问题

#### 检查点1: 响应结构
在浏览器控制台中检查OCR比对响应：
```javascript
// 在doUploadOCRCompare方法中添加
console.log('完整响应:', JSON.stringify(res, null, 2))
```

#### 检查点2: 任务ID获取
检查任务ID获取逻辑：
```javascript
console.log('res.data:', res.data)
console.log('res.data.id:', res.data?.id)
console.log('res.data.taskId:', res.data?.taskId)
```

#### 检查点3: 参数传递
检查传递给monitorTask的参数：
```javascript
console.log('传递给monitorTask的taskId:', taskId)
console.log('taskId类型:', typeof taskId)
console.log('taskId长度:', taskId?.length)
```

### 常见问题

#### 问题1: 响应结构不匹配
**症状**: 控制台显示"无法获取任务ID，响应格式异常"
**原因**: 后端返回的字段名与前端期望的不一致
**解决**: 检查后端返回的字段名，确保前端正确获取

#### 问题2: 任务ID为空
**症状**: 控制台显示"任务ID为空"
**原因**: 后端没有返回任务ID或返回了空值
**解决**: 检查后端OCR比对服务的实现

#### 问题3: 网络请求失败
**症状**: 控制台显示网络错误
**原因**: 前端代理配置问题或后端服务未启动
**解决**: 检查vite.config.ts中的代理配置和后端服务状态

## 📊 验证清单

### 前端验证
- [ ] OCR比对任务提交成功
- [ ] 控制台显示正确的任务ID
- [ ] 进度监控正常启动
- [ ] 无undefined错误
- [ ] 任务状态查询正常

### 后端验证
- [ ] 文件上传成功
- [ ] OCR比对任务创建成功
- [ ] 返回正确的任务ID
- [ ] 任务状态更新正常

### API验证
- [ ] 上传接口返回200状态
- [ ] 响应包含正确的任务ID
- [ ] 进度查询接口正常
- [ ] 无404或500错误

## 🎉 修复完成

当所有验证项都通过后，OCR比对的任务ID问题就完全解决了！

### 下一步
1. 测试完整的OCR比对流程
2. 验证比对结果生成
3. 测试任务管理功能
4. 优化用户体验

## 📞 技术支持

如果仍有问题，请提供：
1. 浏览器控制台的完整日志
2. 网络请求的详细信息
3. 后端服务的错误日志
4. 具体的错误信息
