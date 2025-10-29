# API 对接说明

本文档详细说明肇新合同比对服务的 API 接口规范。

## 📋 目录

- [基础信息](#基础信息)
- [接口列表](#接口列表)
- [详细说明](#详细说明)
- [错误码](#错误码)
- [示例代码](#示例代码)

---

## 基础信息

### 服务地址

- **开发环境**: `http://localhost:8080`
- **生产环境**: 根据实际部署配置

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "traceId": "xxx-xxx-xxx",
  "timestamp": "2025-10-25T17:32:15"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码（200成功，其他失败） |
| message | String | 提示信息 |
| data | Object | 业务数据 |
| traceId | String | 追踪ID（可选） |
| timestamp | String | 时间戳 |

---

## 接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| [提交比对任务](#1-提交比对任务) | POST | `/api/compare-pro/submit-url` | 提交文件比对任务 |
| [查询任务状态](#2-查询任务状态) | GET | `/api/compare-pro/task/{taskId}` | 查询任务执行状态 |
| [获取比对结果](#3-获取比对结果) | GET | `/api/compare-pro/canvas-result/{taskId}` | 获取比对详细结果 |
| [获取任务列表](#4-获取任务列表) | GET | `/api/compare-pro/tasks` | 获取所有任务历史 |
| [删除任务](#5-删除任务) | DELETE | `/api/compare-pro/task/{taskId}` | 删除指定任务 |
| [导出报告](#6-导出报告) | POST | `/api/compare-pro/export-report` | 导出比对报告 |

---

## 详细说明

### 1. 提交比对任务

提交两个文档的比对任务。

#### 请求

**URL**: `POST /api/compare-pro/submit-url`

**Content-Type**: `application/json`

**请求体**:

```json
{
  "oldFileUrl": "http://example.com/old-file.pdf",
  "newFileUrl": "http://example.com/new-file.pdf",
  "removeWatermark": false,
  "oldFileName": "原始合同.pdf",
  "newFileName": "修改后合同.pdf"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldFileUrl | String | 是 | 原文件的可访问 URL |
| newFileUrl | String | 是 | 新文件的可访问 URL |
| removeWatermark | Boolean | 否 | 是否去除水印，默认 false |
| oldFileName | String | 否 | 原文件名（用于显示） |
| newFileName | String | 否 | 新文件名（用于显示） |

**注意**:
- URL 必须可公网访问，或服务器可访问
- 支持 PDF、Word 格式
- 文件大小建议不超过 50MB

#### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": "task-uuid-12345678",
  "timestamp": "2025-10-25T17:32:15"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | String | 任务ID（taskId），用于后续查询 |

#### 示例

```javascript
const response = await axios.post('/api/compare-pro/submit-url', {
  oldFileUrl: 'http://localhost:8090/api/files/download/abc123.pdf',
  newFileUrl: 'http://localhost:8090/api/files/download/def456.pdf',
  removeWatermark: false,
  oldFileName: '原始合同.pdf',
  newFileName: '修改后合同.pdf'
})

const taskId = response.data.data
console.log('任务ID:', taskId)
```

---

### 2. 查询任务状态

查询比对任务的执行状态。

#### 请求

**URL**: `GET /api/compare-pro/task/{taskId}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 任务ID |

#### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task-uuid-12345678",
    "status": "COMPLETED",
    "statusMessage": "比对完成",
    "progress": 100,
    "startTime": "2025-10-25T17:32:15",
    "endTime": "2025-10-25T17:32:45",
    "differenceCount": 15,
    "oldFileName": "原始合同.pdf",
    "newFileName": "修改后合同.pdf",
    "resultUrl": "/gpu-ocr-compare/canvas-result/task-uuid-12345678"
  }
}
```

**status 状态值**:

| 状态 | 说明 |
|------|------|
| PENDING | 等待中 |
| PROCESSING | 处理中 |
| COMPLETED | 已完成 |
| FAILED | 失败 |

#### 示例

```javascript
const response = await axios.get(`/api/compare-pro/task/${taskId}`)
const task = response.data.data

if (task.status === 'COMPLETED') {
  console.log('比对完成，差异数:', task.differenceCount)
} else if (task.status === 'FAILED') {
  console.error('任务失败:', task.errorMessage)
}
```

---

### 3. 获取比对结果

获取比对任务的详细结果数据。

#### 请求

**URL**: `GET /api/compare-pro/canvas-result/{taskId}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 任务ID |

#### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task-uuid-12345678",
    "differences": [
      {
        "id": 1,
        "type": "MODIFIED",
        "oldText": "原文本内容",
        "newText": "修改后内容",
        "pageNumber": 1,
        "position": { "x": 100, "y": 200 }
      }
    ],
    "oldDocument": {
      "fileName": "原始合同.pdf",
      "pageCount": 10,
      "fileSize": 1024000
    },
    "newDocument": {
      "fileName": "修改后合同.pdf",
      "pageCount": 10,
      "fileSize": 1048000
    },
    "summary": {
      "totalDifferences": 15,
      "addedCount": 5,
      "deletedCount": 3,
      "modifiedCount": 7
    }
  }
}
```

**difference.type 类型**:

| 类型 | 说明 |
|------|------|
| ADDED | 新增内容 |
| DELETED | 删除内容 |
| MODIFIED | 修改内容 |

#### 示例

```javascript
const response = await axios.get(`/api/compare-pro/canvas-result/${taskId}`)
const result = response.data.data

console.log('差异总数:', result.summary.totalDifferences)
console.log('新增:', result.summary.addedCount)
console.log('删除:', result.summary.deletedCount)
console.log('修改:', result.summary.modifiedCount)
```

---

### 4. 获取任务列表

获取所有比对任务的历史记录。

#### 请求

**URL**: `GET /api/compare-pro/tasks`

#### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "taskId": "task-uuid-12345678",
      "status": "COMPLETED",
      "startTime": "2025-10-25T17:32:15",
      "endTime": "2025-10-25T17:32:45",
      "differenceCount": 15,
      "oldFileName": "原始合同.pdf",
      "newFileName": "修改后合同.pdf",
      "resultUrl": "/gpu-ocr-compare/canvas-result/task-uuid-12345678"
    }
  ]
}
```

#### 示例

```javascript
const response = await axios.get('/api/compare-pro/tasks')
const tasks = response.data.data

tasks.forEach(task => {
  console.log(`任务${task.taskId}: ${task.status}`)
})
```

---

### 5. 删除任务

删除指定的比对任务及其结果。

#### 请求

**URL**: `DELETE /api/compare-pro/task/{taskId}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 任务ID |

#### 响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

#### 示例

```javascript
await axios.delete(`/api/compare-pro/task/${taskId}`)
console.log('任务已删除')
```

---

### 6. 导出报告

导出比对报告（支持 Word、HTML 格式）。

#### 请求

**URL**: `POST /api/compare-pro/export-report`

**Content-Type**: `application/json`

**Response-Type**: `application/octet-stream` 或 `application/zip`

**请求体**:

```json
{
  "taskId": "task-uuid-12345678",
  "formats": ["doc", "html"],
  "includeIgnored": false,
  "includeRemarks": true
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 任务ID |
| formats | Array<String> | 是 | 导出格式：["doc", "html"] |
| includeIgnored | Boolean | 否 | 是否包含已忽略差异，默认 false |
| includeRemarks | Boolean | 否 | 是否包含备注，默认 true |

#### 响应

- **单格式**: 返回文件流（如 `.docx`）
- **多格式**: 返回 ZIP 压缩包

**响应头**:
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="比对报告.zip"
```

#### 示例

```javascript
const response = await axios({
  url: '/api/compare-pro/export-report',
  method: 'post',
  data: {
    taskId,
    formats: ['doc', 'html']
  },
  responseType: 'blob'
})

// 下载文件
const blob = new Blob([response.data], { type: 'application/zip' })
const url = window.URL.createObjectURL(blob)
const link = document.createElement('a')
link.href = url
link.download = `比对报告_${taskId}.zip`
link.click()
window.URL.revokeObjectURL(url)
```

---

## 错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数 |
| 404 | 任务不存在 | 确认 taskId 是否正确 |
| 500 | 服务器内部错误 | 联系技术支持 |
| 10001 | 文件下载失败 | 检查文件 URL 是否可访问 |
| 10002 | 文件格式不支持 | 仅支持 PDF、Word 格式 |
| 10003 | 文件大小超限 | 文件不超过 50MB |
| 10004 | 任务超时 | 稍后重试 |

---

## 示例代码

### 完整流程示例

```javascript
import axios from 'axios'

const client = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60000
})

// 1. 提交比对任务
async function submitCompare(oldUrl, newUrl) {
  const { data } = await client.post('/api/compare-pro/submit-url', {
    oldFileUrl: oldUrl,
    newFileUrl: newUrl,
    removeWatermark: false,
    oldFileName: '原始合同.pdf',
    newFileName: '修改后合同.pdf'
  })
  return data.data // taskId
}

// 2. 轮询任务状态
async function pollTaskStatus(taskId, onProgress) {
  const maxAttempts = 60
  const interval = 2000
  
  for (let i = 0; i < maxAttempts; i++) {
    const { data } = await client.get(`/api/compare-pro/task/${taskId}`)
    const task = data.data
    
    if (onProgress) {
      onProgress(i / maxAttempts, task)
    }
    
    if (task.status === 'COMPLETED') {
      return task
    }
    
    if (task.status === 'FAILED') {
      throw new Error(task.errorMessage || '任务失败')
    }
    
    await new Promise(resolve => setTimeout(resolve, interval))
  }
  
  throw new Error('任务超时')
}

// 3. 获取比对结果
async function getResult(taskId) {
  const { data } = await client.get(`/api/compare-pro/canvas-result/${taskId}`)
  return data.data
}

// 使用示例
async function compare() {
  try {
    // 提交任务
    console.log('提交比对任务...')
    const taskId = await submitCompare(
      'http://localhost:8090/api/files/download/old.pdf',
      'http://localhost:8090/api/files/download/new.pdf'
    )
    console.log('任务ID:', taskId)
    
    // 轮询状态
    console.log('等待比对完成...')
    await pollTaskStatus(taskId, (progress, task) => {
      console.log(`进度: ${Math.round(progress * 100)}%, 状态: ${task.statusMessage}`)
    })
    
    // 获取结果
    console.log('获取比对结果...')
    const result = await getResult(taskId)
    console.log('差异总数:', result.summary.totalDifferences)
    console.log('详细差异:', result.differences)
    
  } catch (error) {
    console.error('比对失败:', error.message)
  }
}

compare()
```

---

## 🔧 最佳实践

### 1. 错误处理

```javascript
try {
  const taskId = await submitCompare(oldUrl, newUrl)
} catch (error) {
  if (error.response) {
    // 服务器返回错误
    console.error('错误码:', error.response.data.code)
    console.error('错误信息:', error.response.data.message)
  } else if (error.request) {
    // 请求发送失败
    console.error('网络错误')
  } else {
    // 其他错误
    console.error('未知错误:', error.message)
  }
}
```

### 2. 超时处理

```javascript
const client = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60000, // 60秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})

client.interceptors.response.use(
  response => response,
  error => {
    if (error.code === 'ECONNABORTED') {
      console.error('请求超时')
    }
    return Promise.reject(error)
  }
)
```

### 3. 请求追踪

```javascript
client.interceptors.request.use(config => {
  const traceId = generateTraceId()
  config.headers['X-Trace-Id'] = traceId
  console.log(`[${traceId}] ${config.method.toUpperCase()} ${config.url}`)
  return config
})
```

---

## 📞 技术支持

如有问题，请参考：
- [前端集成指南](./前端集成指南.md)
- [快速开始](./快速开始.md)
- 或联系肇新技术支持团队

