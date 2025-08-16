# OCR比对集成指南

## 🎯 架构设计

### 统一接口设计
为了简化系统架构，OCR比对和普通比对现在使用同一个上传接口，通过`useOCR`参数来区分：

- **普通比对**: `POST /api/compare/upload` (useOCR=false 或省略)
- **OCR比对**: `POST /api/compare/upload` (useOCR=true)

### 接口路径
```
POST /api/compare/upload
```

### 请求参数
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| oldFile | MultipartFile | 是 | - | 原始文件 |
| newFile | MultipartFile | 是 | - | 新文件 |
| ignoreHeaderFooter | boolean | 否 | true | 是否忽略页眉页脚 |
| headerHeightMm | float | 否 | 20 | 页眉高度(mm) |
| footerHeightMm | float | 否 | 20 | 页脚高度(mm) |
| ignoreCase | boolean | 否 | true | 是否忽略大小写 |
| ignoredSymbols | string | 否 | "_＿" | 忽略的符号集 |
| **useOCR** | **boolean** | **否** | **false** | **是否使用OCR比对** |
| **ignoreSpaces** | **boolean** | **否** | **false** | **是否忽略空格** |

## 🚀 使用方法

### 1. 前端调用

#### OCR比对
```typescript
import { uploadOCRCompare } from '@/api/ocr-compare'

const formData = new FormData()
formData.append('oldFile', oldFile)
formData.append('newFile', newFile)
formData.append('useOCR', 'true')  // 关键参数
formData.append('ignoreSpaces', 'true')
formData.append('ignoreCase', 'true')

const result = await uploadOCRCompare(formData)
```

#### 普通比对
```typescript
import { uploadCompare } from '@/api/compare'

const formData = new FormData()
formData.append('oldFile', oldFile)
formData.append('newFile', newFile)
// 不传useOCR或传false
formData.append('ignoreCase', 'true')

const result = await uploadCompare(formData)
```

### 2. 后端处理

#### CompareController.uploadAndCompare()
```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Result<Map<String, Object>> uploadAndCompare(
    // ... 其他参数
    @RequestParam(value = "useOCR", required = false, defaultValue = "false") boolean useOCR,
    @RequestParam(value = "ignoreSpaces", required = false, defaultValue = "false") boolean ignoreSpaces,
    HttpServletRequest request
) {
    if (useOCR) {
        // OCR比对逻辑
        return handleOCRCompare(oldFile, newFile, options);
    } else {
        // 普通比对逻辑
        return handleNormalCompare(oldFile, newFile, options);
    }
}
```

## 🔧 技术实现

### 1. 文件转换
两种比对模式都支持多种文档格式：
- **Word**: .doc, .docx, .docm
- **Excel**: .xls, .xlsx, .xlsm, .xlsb
- **PDF**: .pdf

通过`ChangeFileToPDFService`统一转换为PDF格式。

### 2. 比对引擎
- **普通比对**: 使用`PDFComparsionHelper`进行PDF文本比对
- **OCR比对**: 使用`RapidOCR`进行文字识别，然后进行文本比对

### 3. 结果处理
- **普通比对**: 直接返回比对结果和标注PDF
- **OCR比对**: 返回任务ID，支持异步查询进度

## 📱 前端组件

### OCRCompare.vue
- 文件上传支持多种格式
- 比对设置（忽略空格、大小写等）
- 进度监控和结果展示

### 关键特性
```vue
<template>
  <!-- 文件上传 -->
  <input
    type="file"
    accept=".pdf,.doc,.docx,.docm,.xls,.xlsx,.xlsm,.xlsb"
    @change="onFileChange"
  />
  
  <!-- 比对设置 -->
  <el-form-item label="忽略空格">
    <el-switch v-model="settings.ignoreSpaces" />
  </el-form-item>
</template>

<script setup>
const doUploadOCRCompare = async () => {
  const formData = new FormData()
  formData.append('oldFile', oldFile.value)
  formData.append('newFile', newFile.value)
  formData.append('useOCR', 'true')  // 关键参数
  formData.append('ignoreSpaces', String(settings.ignoreSpaces))
  
  const res = await uploadOCRCompare(formData)
  // 处理响应
}
</script>
```

## 🔄 工作流程

### OCR比对流程
```
1. 前端上传文件 (useOCR=true)
2. 后端接收文件，设置OCR模式
3. 文档转换为PDF (如果需要)
4. 调用OCR服务识别文字
5. 执行文本比对
6. 生成标注PDF
7. 返回结果
```

### 普通比对流程
```
1. 前端上传文件 (useOCR=false)
2. 后端接收文件，设置普通模式
3. 文档转换为PDF (如果需要)
4. 直接进行PDF文本比对
5. 生成标注PDF
6. 返回结果
```

## 🎉 优势

### 1. 统一接口
- 减少API数量，简化维护
- 前端可以复用大部分逻辑
- 统一的错误处理和参数验证

### 2. 灵活配置
- 通过参数控制比对模式
- 支持渐进式功能开发
- 易于扩展新的比对类型

### 3. 代码复用
- 文件转换逻辑共享
- 比对选项配置统一
- 结果处理流程一致

## 🚧 开发状态

### 已完成
- [x] 统一接口设计
- [x] 前端参数传递
- [x] 后端参数接收
- [x] 基础OCR比对框架

### 进行中
- [ ] OCR比对服务集成
- [ ] 异步任务处理
- [ ] 进度监控实现

### 下一步
1. 完善OCR比对服务
2. 实现任务状态查询
3. 添加结果展示页面
4. 优化用户体验

## 📞 技术支持

如有问题，请检查：
1. `useOCR`参数是否正确传递
2. 文件格式是否支持
3. 后端服务是否正常启动
4. 网络请求是否成功
