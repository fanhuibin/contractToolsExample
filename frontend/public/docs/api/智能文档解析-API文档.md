# 智能文档解析 API 接口文档

## 📍 接口地址汇总

| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **上传并解析** | POST | `/api/ocr/extract/upload` | 上传文档并进行OCR解析 |
| **查询任务状态** | GET | `/api/ocr/extract/status/{taskId}` | 查询任务执行状态 |
| **获取解析结果** | GET | `/api/ocr/extract/result/{taskId}` | 获取解析的文本结果 |
| **获取页面图片** | GET | `/api/ocr/extract/page-image/{taskId}/{pageNum}` | 获取文档页面图片 |

---

## 📋 基础信息

| 项目 | 内容 |
|------|------|
| **API版本** | v1.0 |
| **基础URL** | `https://your-domain.com` |
| **认证方式** | API Key（X-API-Key请求头） |
| **响应格式** | JSON |
| **支持文件** | PDF格式 |
| **解析技术** | GPU加速OCR + 版面分析 |

---

## 🎯 功能概述

智能文档解析功能基于**GPU加速OCR技术**，将PDF文档快速转换为可编辑的文本内容，支持版面分析和页眉页脚过滤。

### 核心特点

- ✅ **高速解析**：GPU加速，处理速度快
- ✅ **高准确率**：中英文识别准确率>98%
- ✅ **版面分析**：保留段落结构
- ✅ **页眉页脚过滤**：自动过滤页眉页脚
- ✅ **图文对照**：提供页面图片对照查看
- ✅ **批量处理**：支持异步批量解析

### 应用场景

- 📄 **文档数字化**：将纸质文档转为电子文本
- 🔍 **内容检索**：提取文本用于全文检索
- 📊 **文档分析**：提取文档结构和内容
- 📝 **内容提取**：从PDF中提取纯文本

---

## 🎯 接口1: 上传文档并解析

### `POST /api/ocr/extract/upload`

**功能描述**: 上传PDF文档进行OCR文本识别和解析

**请求地址**
```
POST https://your-domain.com/api/ocr/extract/upload
Content-Type: multipart/form-data
```

### 请求参数

#### 表单参数

| 参数名 | 类型 | 必需 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `file` | file | ✅ | - | PDF文档文件 |
| `ignoreHeaderFooter` | boolean | ❌ | true | 是否忽略页眉页脚 |
| `headerHeightPercent` | number | ❌ | 6.0 | 页眉高度百分比（0-50） |
| `footerHeightPercent` | number | ❌ | 6.0 | 页脚高度百分比（0-50） |

**支持的文件格式**:
- PDF格式: `.pdf`

**文件处理说明**:
- **文件大小**: 建议不超过50MB
- **页数限制**: 建议不超过100页
- **处理时间**: 一般5-30秒（取决于页数和文档复杂度）
- **存储位置**: 文档和结果存储7天

### 页眉页脚过滤说明

系统会根据设置的百分比自动过滤页眉页脚区域的文本：

```
页眉区域 = 页面高度 × headerHeightPercent%
页脚区域 = 页面高度 × footerHeightPercent%

示例：A4页面高度 = 842pt
     页眉高度 = 842 × 6% = 51pt
     页脚高度 = 842 × 6% = 51pt
```

### 请求示例

**Java 示例（使用Apache HttpClient）**
```java
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.File;

CloseableHttpClient httpClient = HttpClients.createDefault();
HttpPost uploadFile = new HttpPost("https://your-domain.com/api/ocr/extract/upload");

MultipartEntityBuilder builder = MultipartEntityBuilder.create();
builder.addBinaryBody("file", new File("document.pdf"));
builder.addTextBody("ignoreHeaderFooter", "true");
builder.addTextBody("headerHeightPercent", "6.0");
builder.addTextBody("footerHeightPercent", "6.0");
HttpEntity multipart = builder.build();

uploadFile.setEntity(multipart);
CloseableHttpResponse response = httpClient.execute(uploadFile);
System.out.println(EntityUtils.toString(response.getEntity()));
```

**Python 示例**
```python
import requests

url = 'https://your-domain.com/api/ocr/extract/upload'

files = {'file': open('document.pdf', 'rb')}
data = {
    'ignoreHeaderFooter': 'true',
    'headerHeightPercent': '6.0',
    'footerHeightPercent': '6.0'
}

response = requests.post(url, files=files, data=data)
task_id = response.json()['data']['taskId']
print('任务ID:', task_id)
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/ocr/extract/upload";
$file_path = "document.pdf";

$curl = curl_init();

$file = new CURLFile($file_path);
$post_data = array(
    'file' => $file,
    'ignoreHeaderFooter' => 'true',
    'headerHeightPercent' => '12.0',
    'footerHeightPercent' => '12.0'
);

curl_setopt_array($curl, array(
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => $post_data,
    CURLOPT_RETURNTRANSFER => true
));

$response = curl_exec($curl);
curl_close($curl);

$result = json_decode($response, true);
$task_id = $result['data']['taskId'];
echo "任务ID: " . $task_id;
?>
```

**JavaScript/Axios 示例**
```javascript
const formData = new FormData()
formData.append('file', fileObject)
formData.append('ignoreHeaderFooter', 'true')
formData.append('headerHeightPercent', '6.0')
formData.append('footerHeightPercent', '6.0')

const response = await axios.post('/api/ocr/extract/upload', formData, {
  headers: {
    'X-API-Key': 'your-api-key-here',
    'Content-Type': 'multipart/form-data'
  }
})

console.log('任务ID:', response.data.data.taskId)
```

**Python 示例**
```python
import requests

url = 'https://your-domain.com/api/ocr/extract/upload'
headers = {'X-API-Key': 'your-api-key-here'}

files = {'file': open('document.pdf', 'rb')}
data = {
    'ignoreHeaderFooter': 'true',
    'headerHeightPercent': '6.0',
    'footerHeightPercent': '6.0'
}

response = requests.post(url, headers=headers, files=files, data=data)
task_id = response.json()['data']['taskId']
print('任务ID:', task_id)
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "文件上传成功，开始智能解析",
  "data": {
    "taskId": "ocr-parse-20250118-abc123",
    "message": "文件上传成功，开始智能解析..."
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 错误响应示例

**文件为空 (400)**
```json
{
  "code": 17001,
  "message": "文件为空",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

**文件格式错误 (415)**
```json
{
  "code": 17002,
  "message": "只支持PDF格式文件",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📊 接口2: 查询任务状态

### `GET /api/ocr/extract/status/{taskId}`

**功能描述**: 查询OCR解析任务的执行状态和进度

**请求地址**
```
GET https://your-domain.com/api/ocr/extract/status/{taskId}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

### 任务状态枚举

| 状态 | 描述 | 进度 | 说明 |
|------|------|------|------|
| `pending` | 等待中 | 0% | 任务已提交，等待处理 |
| `processing` | 处理中 | 1-99% | 正在进行OCR识别 |
| `completed` | 完成 | 100% | 解析完成 |
| `failed` | 失败 | - | 解析失败 |

### 请求示例

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/ocr/extract/status/ocr-parse-20250118-abc123"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/ocr/extract/status/ocr-parse-20250118-abc123"
response = requests.get(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/ocr/extract/status/ocr-parse-20250118-abc123";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```

**JavaScript 轮询示例**
```javascript
async function waitForOcrComplete(taskId) {
  while (true) {
    const res = await axios.get(`/api/ocr/extract/status/${taskId}`, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const status = res.data.data.status
    const progress = res.data.data.progress
    
    console.log(`解析进度: ${progress}%`)
    
    if (status === 'completed') {
      return true
    } else if (status === 'failed') {
      throw new Error(res.data.data.message)
    }
    
    // 等待2秒后继续轮询
    await new Promise(resolve => setTimeout(resolve, 2000))
  }
}
```

### 响应示例

#### 处理中状态
```json
{
  "code": 200,
  "data": {
    "taskId": "ocr-parse-20250118-abc123",
    "status": "processing",
    "progress": 45,
    "message": "正在识别第3页/共5页",
    "currentPage": 3,
    "totalPages": 5
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:15"
}
```

#### 完成状态
```json
{
  "code": 200,
  "data": {
    "taskId": "ocr-parse-20250118-abc123",
    "status": "completed",
    "progress": 100,
    "message": "解析完成",
    "currentPage": 5,
    "totalPages": 5
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:45"
}
```

#### 失败状态
```json
{
  "code": 200,
  "data": {
    "taskId": "ocr-parse-20250118-abc123",
    "status": "failed",
    "progress": 20,
    "message": "OCR服务异常",
    "currentPage": 1,
    "totalPages": 5
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:30"
}
```

---

## 📋 接口3: 获取解析结果

### `GET /api/ocr/extract/result/{taskId}`

**功能描述**: 获取OCR解析完成后的文本内容

**请求地址**
```
GET https://your-domain.com/api/ocr/extract/result/{taskId}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

### 请求示例

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/ocr/extract/result/ocr-parse-20250118-abc123"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/ocr/extract/result/ocr-parse-20250118-abc123"
response = requests.get(url)
result = response.json()
print(result)
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/ocr/extract/result/ocr-parse-20250118-abc123";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "data": {
    "taskId": "ocr-parse-20250118-abc123",
    "status": "completed",
    "fileName": "document.pdf",
    "totalPages": 5,
    "pages": [
      {
        "pageNumber": 1,
        "text": "第一页的文本内容...\n这是第二行...\n这是第三行...",
        "imageUrl": "/api/ocr/extract/page-image/ocr-parse-20250118-abc123/1",
        "width": 1240,
        "height": 1754,
        "confidence": 0.98
      },
      {
        "pageNumber": 2,
        "text": "第二页的文本内容...",
        "imageUrl": "/api/ocr/extract/page-image/ocr-parse-20250118-abc123/2",
        "width": 1240,
        "height": 1754,
        "confidence": 0.97
      }
    ],
    "fullText": "第一页的文本内容...\n这是第二行...\n这是第三行...\n第二页的文本内容...",
    "totalCharacters": 15680,
    "processingTime": 28.5,
    "averageConfidence": 0.975
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:31:00"
}
```

**响应字段说明**:

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `totalPages` | number | 文档总页数 |
| `pages` | array | 分页文本数组 |
| `pages[].pageNumber` | number | 页码 |
| `pages[].text` | string | 该页的文本内容 |
| `pages[].imageUrl` | string | 该页图片URL |
| `pages[].width` | number | 图片宽度 |
| `pages[].height` | number | 图片高度 |
| `pages[].confidence` | number | 识别置信度（0-1） |
| `fullText` | string | 全文文本（所有页合并） |
| `totalCharacters` | number | 总字符数 |
| `processingTime` | number | 处理耗时（秒） |
| `averageConfidence` | number | 平均置信度 |

#### 任务未完成
```json
{
  "code": 200,
  "data": {
    "status": "processing",
    "message": "任务尚未完成，当前进度: 45%",
    "progress": 45
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:30"
}
```

#### 任务不存在
```json
{
  "code": 14001,
  "message": "解析任务不存在: ocr-parse-20250118-abc123",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:30"
}
```

---

## 🖼️ 接口4: 获取页面图片

### `GET /api/ocr/extract/page-image/{taskId}/{pageNum}`

**功能描述**: 获取文档指定页面的渲染图片

**请求地址**
```
GET https://your-domain.com/api/ocr/extract/page-image/{taskId}/{pageNum}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |
| `pageNum` | number | 路径 | ✅ | 页码（从1开始） |

### 请求示例

**直接访问URL**
```html
<img src="/api/ocr/extract/page-image/ocr-parse-20250118-abc123/1" />
```

**cURL下载**
```bash
curl -X GET "https://your-domain.com/api/ocr/extract/page-image/ocr-parse-20250118-abc123/1" \
  -H "X-API-Key: your-api-key-here" \
  --output page-1.png
```

### 响应说明

- **响应类型**: `image/png` (二进制图片流)
- **响应头**: 
  - `Content-Type: image/png`
  - `Content-Disposition: inline; filename="page-1.png"`

---

## 💡 使用示例

### 完整流程示例

```javascript
// 完整的文档解析流程
async function parseDocument(file) {
  try {
    // 1. 上传文档
    const formData = new FormData()
    formData.append('file', file)
    formData.append('ignoreHeaderFooter', 'true')
    
    const uploadRes = await axios.post('/api/ocr/extract/upload', formData, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const taskId = uploadRes.data.data.taskId
    console.log('任务创建:', taskId)
    
    // 2. 轮询等待完成
    let completed = false
    while (!completed) {
      const statusRes = await axios.get(`/api/ocr/extract/status/${taskId}`, {
        headers: { 'X-API-Key': 'your-api-key-here' }
      })
      
      const status = statusRes.data.data.status
      const progress = statusRes.data.data.progress
      
      console.log(`进度: ${progress}%`)
      
      if (status === 'completed') {
        completed = true
      } else if (status === 'failed') {
        throw new Error('解析失败')
      } else {
        await new Promise(resolve => setTimeout(resolve, 2000))
      }
    }
    
    // 3. 获取结果
    const resultRes = await axios.get(`/api/ocr/extract/result/${taskId}`, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const result = resultRes.data.data
    
    console.log('解析完成!')
    console.log('总页数:', result.totalPages)
    console.log('总字符数:', result.totalCharacters)
    console.log('平均置信度:', (result.averageConfidence * 100).toFixed(2) + '%')
    console.log('全文:\n', result.fullText)
    
    return result
    
  } catch (error) {
    console.error('解析失败:', error.message)
    throw error
  }
}

// 使用
parseDocument(fileObject).then(result => {
  // 处理结果
  console.log('解析结果:', result)
})
```

### Vue组件示例

```vue
<template>
  <div class="ocr-parser">
    <el-upload
      :auto-upload="false"
      :on-change="handleFileChange"
      accept=".pdf"
      :limit="1"
    >
      <el-button type="primary">选择PDF文件</el-button>
    </el-upload>
    
    <el-form :inline="true" style="margin-top: 20px;">
      <el-form-item label="忽略页眉页脚">
        <el-switch v-model="ignoreHeaderFooter"></el-switch>
      </el-form-item>
      <el-form-item label="页眉高度" v-if="ignoreHeaderFooter">
        <el-input-number 
          v-model="headerHeightPercent" 
          :min="0" 
          :max="50"
          :step="1"
        ></el-input-number>
        <span style="margin-left: 5px;">%</span>
      </el-form-item>
      <el-form-item label="页脚高度" v-if="ignoreHeaderFooter">
        <el-input-number 
          v-model="footerHeightPercent" 
          :min="0" 
          :max="50"
          :step="1"
        ></el-input-number>
        <span style="margin-left: 5px;">%</span>
      </el-form-item>
    </el-form>
    
    <el-button 
      @click="startParse" 
      :loading="parsing"
      :disabled="!file"
      type="success"
    >
      开始解析
    </el-button>
    
    <el-progress 
      v-if="parsing" 
      :percentage="progress"
      :status="progressStatus"
      style="margin-top: 20px;"
    ></el-progress>
    
    <div v-if="result" class="result-display">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="全文文本" name="fullText">
          <el-card>
            <div class="stats">
              <el-statistic title="总页数" :value="result.totalPages"></el-statistic>
              <el-statistic title="总字符数" :value="result.totalCharacters"></el-statistic>
              <el-statistic 
                title="平均置信度" 
                :value="(result.averageConfidence * 100).toFixed(2)" 
                suffix="%"
              ></el-statistic>
            </div>
            <el-divider></el-divider>
            <pre class="text-content">{{ result.fullText }}</pre>
          </el-card>
        </el-tab-pane>
        
        <el-tab-pane label="分页查看" name="pages">
          <el-card v-for="page in result.pages" :key="page.pageNumber" class="page-card">
            <template #header>
              <span>第 {{ page.pageNumber }} 页</span>
              <el-tag size="small" type="info" style="margin-left: 10px;">
                置信度: {{ (page.confidence * 100).toFixed(2) }}%
              </el-tag>
            </template>
            <el-row :gutter="20">
              <el-col :span="12">
                <h4>原始图片</h4>
                <img :src="page.imageUrl" style="width: 100%;" />
              </el-col>
              <el-col :span="12">
                <h4>识别文本</h4>
                <pre class="page-text">{{ page.text }}</pre>
              </el-col>
            </el-row>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const file = ref(null)
const ignoreHeaderFooter = ref(true)
const headerHeightPercent = ref(12)
const footerHeightPercent = ref(12)
const parsing = ref(false)
const progress = ref(0)
const progressStatus = ref('')
const result = ref(null)
const activeTab = ref('fullText')

const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
  result.value = null
}

const startParse = async () => {
  parsing.value = true
  progress.value = 0
  progressStatus.value = ''
  
  try {
    // 上传文件
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('ignoreHeaderFooter', ignoreHeaderFooter.value)
    formData.append('headerHeightPercent', headerHeightPercent.value)
    formData.append('footerHeightPercent', footerHeightPercent.value)
    
    const uploadRes = await axios.post('/api/ocr/extract/upload', formData)
    const taskId = uploadRes.data.data.taskId
    
    // 轮询状态
    while (true) {
      const statusRes = await axios.get(`/api/ocr/extract/status/${taskId}`)
      const status = statusRes.data.data.status
      progress.value = statusRes.data.data.progress || 0
      
      if (status === 'completed') {
        progressStatus.value = 'success'
        break
      } else if (status === 'failed') {
        progressStatus.value = 'exception'
        throw new Error('解析失败')
      }
      
      await new Promise(resolve => setTimeout(resolve, 2000))
    }
    
    // 获取结果
    const resultRes = await axios.get(`/api/ocr/extract/result/${taskId}`)
    result.value = resultRes.data.data
    
    ElMessage.success('解析完成！')
    
  } catch (error) {
    ElMessage.error(error.message || '解析失败')
  } finally {
    parsing.value = false
  }
}
</script>

<style scoped>
.stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.text-content {
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 500px;
  overflow-y: auto;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
}

.page-card {
  margin-bottom: 20px;
}

.page-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
  max-height: 400px;
  overflow-y: auto;
}
</style>
```

---

## 🎯 最佳实践

### 1. 文档质量建议

**✅ 推荐**:
- 清晰的PDF文档（原生PDF或高质量扫描件）
- 分辨率 ≥ 300 DPI
- 文字清晰可读
- 无严重倾斜或变形

**❌ 避免**:
- 模糊不清的图片
- 分辨率过低（< 200 DPI）
- 严重倾斜的文档
- 手写文字（识别率较低）

### 2. 页眉页脚设置

| 文档类型 | 页眉高度 | 页脚高度 | 说明 |
|---------|---------|---------|------|
| 标准文档 | 6% | 6% | 默认推荐值 |
| 简单文档 | 10% | 10% | 页眉页脚较少 |
| 复杂文档 | 15% | 15% | 页眉页脚较多 |
| 无页眉页脚 | 0% | 0% | 关闭过滤 |

### 3. 性能优化

```javascript
// 批量解析多个文档
async function batchParse(files) {
  // 并发提交任务
  const tasks = await Promise.all(
    files.map(file => uploadForParse(file))
  )
  
  // 并发轮询状态
  const results = await Promise.all(
    tasks.map(task => waitAndGetResult(task.taskId))
  )
  
  return results
}
```

### 4. 错误处理

```javascript
try {
  const result = await parseDocument(file)
  
  // 检查置信度
  if (result.averageConfidence < 0.9) {
    console.warn('识别置信度较低，建议人工核对')
  }
  
  // 检查字符数
  if (result.totalCharacters === 0) {
    console.warn('未识别到文本内容')
  }
  
} catch (error) {
  console.error('解析失败:', error)
}
```

---

## 📊 状态码说明

| 状态码 | 类型 | 说明 |
|-------|------|------|
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 参数缺失或格式错误 |
| 404 | 未找到 | 任务不存在 |
| 415 | 格式错误 | 不支持的文件格式 |
| 500 | 服务器错误 | 处理异常 |

### 业务错误码

| 错误码 | 说明 |
|-------|------|
| 14001 | 解析任务不存在 |
| 14002 | OCR服务不可用 |
| 14003 | 解析失败 |
| 17001 | 文件为空 |
| 17002 | 文件格式不支持 |

---

## 🔧 技术细节

### OCR处理流程

```
1. PDF上传
   ↓
2. PDF转图片（每页一张）
   ↓
3. GPU OCR识别
   ↓
4. 版面分析
   ↓
5. 页眉页脚过滤
   ↓
6. 文本提取
   ↓
7. 结果组装
```

### 置信度说明

| 置信度范围 | 质量评价 | 建议 |
|-----------|---------|------|
| 0.95 - 1.0 | 优秀 | 可直接使用 |
| 0.90 - 0.95 | 良好 | 建议抽查 |
| 0.85 - 0.90 | 一般 | 需要核对 |
| < 0.85 | 较差 | 需要人工审核 |

---

## 📞 技术支持

如有疑问，请联系：

- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)
- ☎️ 技术支持：18306806281

---

**文档版本**: 1.0  
**最后更新**: 2025-01-18  
**维护者**: 山西肇新科技有限公司

