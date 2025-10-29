# 智能文档抽取 API 接口文档

## 📍 接口地址汇总

| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **上传并抽取** | POST | `/api/rule-extract/extract/upload` | 上传文档并开始抽取 |
| **查询任务状态** | GET | `/api/rule-extract/extract/status/{taskId}` | 查询任务执行状态 |
| **查询抽取结果** | GET | `/api/rule-extract/extract/result/{taskId}` | 获取抽取结果数据 |
| **取消任务** | POST | `/api/rule-extract/extract/cancel/{taskId}` | 取消正在执行的任务 |
| **查询任务列表** | GET | `/api/rule-extract/extract/tasks` | 获取任务列表 |
| **获取页面图片** | GET | `/api/rule-extract/extract/page-image/{taskId}/{pageNumber}` | 获取文档页面图片 |

---

## 📋 基础信息

| 项目 | 内容 |
|------|------|
| **API版本** | v1.0 |
| **基础URL** | `https://your-domain.com` |
| **认证方式** | API Key（X-API-Key请求头） |
| **响应格式** | JSON |
| **支持文件** | PDF格式 |
| **抽取技术** | OCR + 规则引擎 + 智能定位 |

---

## 🎯 功能概述

智能文档抽取功能基于**规则引擎**，通过预定义的抽取模板（规则），从PDF文档中自动识别和提取结构化信息。

### 核心特点

- ✅ **规则可配置**：支持自定义抽取规则
- ✅ **智能定位**：自动定位关键字段
- ✅ **高准确率**：基于GPU OCR + 规则引擎
- ✅ **批量处理**：支持异步批量抽取
- ✅ **可视化结果**：提供图文对照
- ✅ **灵活扩展**：可适配各类文档格式

### 应用场景

- 📄 **合同信息提取**：合同编号、当事人、金额、日期等
- 📋 **发票数据录入**：发票号、金额、税额、购销方信息
- 🆔 **证件信息识别**：身份证、营业执照等关键信息
- 📊 **报表数据提取**：财务报表、统计表格等结构化数据

---

## 🎯 接口1: 上传文档并开始抽取

### `POST /api/rule-extract/extract/upload`

**功能描述**: 上传PDF文档，使用指定的抽取模板进行信息提取

**请求地址**
```
POST https://your-domain.com/api/rule-extract/extract/upload
Content-Type: multipart/form-data
```

### 请求参数

#### 表单参数

| 参数名 | 类型 | 必需 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `file` | file | ✅ | - | PDF文档文件 |
| `templateId` | string | ✅ | - | 抽取模板ID |
| `ocrProvider` | string | ❌ | gpu | OCR提供商（gpu/cloud） |
| `ignoreHeaderFooter` | boolean | ❌ | true | 是否忽略页眉页脚 |
| `headerHeightPercent` | number | ❌ | 12.0 | 页眉高度百分比 |
| `footerHeightPercent` | number | ❌ | 12.0 | 页脚高度百分比 |

**支持的文件格式**:
- PDF格式: `.pdf`

**文件处理说明**:
- **文件大小**: 建议不超过50MB
- **处理时间**: 一般10-60秒（取决于文档页数和复杂度）
- **存储位置**: 文档和结果存储在服务器，保留7天
- **并发限制**: 每个用户最多同时执行5个任务

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
HttpPost uploadFile = new HttpPost("https://your-domain.com/api/rule-extract/extract/upload");

MultipartEntityBuilder builder = MultipartEntityBuilder.create();
builder.addBinaryBody("file", new File("contract.pdf"));
builder.addTextBody("templateId", "contract-template-001");
builder.addTextBody("ignoreHeaderFooter", "true");
builder.addTextBody("headerHeightPercent", "12.0");
builder.addTextBody("footerHeightPercent", "12.0");
HttpEntity multipart = builder.build();

uploadFile.setEntity(multipart);
CloseableHttpResponse response = httpClient.execute(uploadFile);
System.out.println(EntityUtils.toString(response.getEntity()));
```

**Python 示例**
```python
import requests

url = 'https://your-domain.com/api/rule-extract/extract/upload'

files = {'file': open('contract.pdf', 'rb')}
data = {
    'templateId': 'contract-template-001',
    'ignoreHeaderFooter': 'true',
    'headerHeightPercent': '12.0',
    'footerHeightPercent': '12.0'
}

response = requests.post(url, files=files, data=data)
task_id = response.json()['data']['taskId']
print('任务ID:', task_id)
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/upload";
$file_path = "contract.pdf";

$curl = curl_init();

$file = new CURLFile($file_path);
$post_data = array(
    'file' => $file,
    'templateId' => 'contract-template-001',
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

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "extract-20250118-abc123"
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

**模板不存在 (404)**
```json
{
  "code": 11001,
  "message": "抽取模板不存在: contract-template-001",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

**文件格式错误 (415)**
```json
{
  "code": 17002,
  "message": "不支持的文件格式，仅支持PDF",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📊 接口2: 查询任务状态

### `GET /api/rule-extract/extract/status/{taskId}`

**功能描述**: 查询抽取任务的执行状态和进度

**请求地址**
```
GET https://your-domain.com/api/rule-extract/extract/status/{taskId}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

### 任务状态枚举

| 状态 | 描述 | 进度 | 后续操作 |
|------|------|------|---------|
| `pending` | 等待中 | 0% | 继续轮询 |
| `processing` | 处理中 | 1-99% | 继续轮询 |
| `completed` | 完成 | 100% | 获取结果 |
| `failed` | 失败 | - | 查看错误信息 |
| `cancelled` | 已取消 | - | - |

### 请求示例

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/rule-extract/extract/status/extract-20250118-abc123"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/rule-extract/extract/status/extract-20250118-abc123"
response = requests.get(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/status/extract-20250118-abc123";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```

**JavaScript 轮询示例**
```javascript
async function waitForComplete(taskId) {
  while (true) {
    const res = await axios.get(`/api/rule-extract/extract/status/${taskId}`, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const status = res.data.data.status
    const progress = res.data.data.progress
    
    console.log(`任务状态: ${status}, 进度: ${progress}%`)
    
    if (status === 'completed') {
      return true
    } else if (status === 'failed' || status === 'cancelled') {
      throw new Error(res.data.data.message)
    }
    
    // 等待3秒后继续轮询
    await new Promise(resolve => setTimeout(resolve, 3000))
  }
}

// 使用
try {
  await waitForComplete('extract-20250118-abc123')
  console.log('抽取完成，获取结果...')
} catch (error) {
  console.error('抽取失败:', error.message)
}
```

### 响应示例

#### 处理中状态
```json
{
  "code": 200,
  "data": {
    "taskId": "extract-20250118-abc123",
    "status": "processing",
    "progress": 45,
    "message": "正在进行OCR识别...",
    "fileName": "contract.pdf",
    "createdAt": "2025-01-18T10:30:00"
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
    "taskId": "extract-20250118-abc123",
    "status": "completed",
    "progress": 100,
    "message": "抽取完成",
    "fileName": "contract.pdf",
    "createdAt": "2025-01-18T10:30:00"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:31:25"
}
```

#### 失败状态
```json
{
  "code": 200,
  "data": {
    "taskId": "extract-20250118-abc123",
    "status": "failed",
    "progress": 30,
    "message": "OCR识别失败: 文档质量过低",
    "fileName": "contract.pdf",
    "createdAt": "2025-01-18T10:30:00"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:45"
}
```

---

## 📋 接口3: 查询抽取结果

### `GET /api/rule-extract/extract/result/{taskId}`

**功能描述**: 获取抽取完成后的结构化数据

**请求地址**
```
GET https://your-domain.com/api/rule-extract/extract/result/{taskId}
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
    .uri(URI.create("https://your-domain.com/api/rule-extract/extract/result/extract-20250118-abc123"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/rule-extract/extract/result/extract-20250118-abc123"
response = requests.get(url)
result = response.json()
print(result)
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/result/extract-20250118-abc123";
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
    "taskId": "extract-20250118-abc123",
    "status": "completed",
    "extractedData": {
      "contract_no": {
        "value": "HT20250118001",
        "confidence": 0.98,
        "location": {
          "page": 1,
          "x": 100,
          "y": 150,
          "width": 200,
          "height": 30
        }
      },
      "party_a": {
        "value": "北京某某科技有限公司",
        "confidence": 0.95,
        "location": {
          "page": 1,
          "x": 100,
          "y": 200,
          "width": 300,
          "height": 30
        }
      },
      "party_b": {
        "value": "上海某某商贸有限公司",
        "confidence": 0.96,
        "location": {
          "page": 1,
          "x": 100,
          "y": 250,
          "width": 300,
          "height": 30
        }
      },
      "amount": {
        "value": "1000000.00",
        "confidence": 0.99,
        "location": {
          "page": 2,
          "x": 150,
          "y": 300,
          "width": 150,
          "height": 30
        }
      },
      "sign_date": {
        "value": "2025年1月18日",
        "confidence": 0.97,
        "location": {
          "page": 3,
          "x": 200,
          "y": 800,
          "width": 180,
          "height": 30
        }
      }
    },
    "pageCount": 3,
    "extractedFields": 5,
    "totalFields": 8,
    "completionRate": 0.625,
    "processingTime": 45.6
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:31:30"
}
```

**响应字段说明**:

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `extractedData` | object | 抽取的数据字典 |
| `extractedData[key].value` | string | 字段值 |
| `extractedData[key].confidence` | number | 置信度（0-1） |
| `extractedData[key].location` | object | 在文档中的位置 |
| `pageCount` | number | 文档总页数 |
| `extractedFields` | number | 成功抽取的字段数 |
| `totalFields` | number | 模板定义的总字段数 |
| `completionRate` | number | 完成率（0-1） |
| `processingTime` | number | 处理耗时（秒） |

#### 任务未完成
```json
{
  "code": 400,
  "message": "任务尚未完成，当前状态: processing",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:45"
}
```

#### 任务不存在
```json
{
  "code": 404,
  "message": "任务不存在",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:45"
}
```

---

## ⏹️ 接口4: 取消任务

### `POST /api/rule-extract/extract/cancel/{taskId}`

**功能描述**: 取消正在执行的抽取任务

**请求地址**
```
POST https://your-domain.com/api/rule-extract/extract/cancel/{taskId}
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
    .uri(URI.create("https://your-domain.com/api/rule-extract/extract/cancel/extract-20250118-abc123"))
    .POST(HttpRequest.BodyPublishers.noBody())
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/rule-extract/extract/cancel/extract-20250118-abc123"
response = requests.post(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/cancel/extract-20250118-abc123";

$options = array(
    'http' => array(
        'method'  => 'POST'
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);
$response = json_decode($result, true);
print_r($response);
?>
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "任务已取消",
  "traceId": "...",
  "timestamp": "2025-01-18T10:31:00"
}
```

#### 任务已完成
```json
{
  "code": 400,
  "message": "任务已完成，无法取消",
  "traceId": "...",
  "timestamp": "2025-01-18T10:31:00"
}
```

---

## 📜 接口5: 查询任务列表

### `GET /api/rule-extract/extract/tasks`

**功能描述**: 获取抽取任务列表（最近20条）

**请求地址**
```
GET https://your-domain.com/api/rule-extract/extract/tasks?templateId=&status=
```

### 请求参数

#### 查询参数

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `templateId` | string | ❌ | 按模板ID筛选 |
| `status` | string | ❌ | 按状态筛选（pending/processing/completed/failed） |

### 请求示例

**Java 示例（获取所有任务）**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/rule-extract/extract/tasks"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例（按模板筛选）**
```python
import requests

url = "https://your-domain.com/api/rule-extract/extract/tasks"
params = {'templateId': 'contract-template-001'}

response = requests.get(url, params=params)
print(response.json())
```

**PHP 示例（按状态筛选）**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/tasks?status=completed";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```

### 响应示例

```json
{
  "code": 200,
  "data": [
    {
      "taskId": "extract-20250118-abc123",
      "fileName": "contract.pdf",
      "templateId": "contract-template-001",
      "status": "completed",
      "progress": 100,
      "createdAt": "2025-01-18T10:30:00",
      "completedAt": "2025-01-18T10:31:25"
    },
    {
      "taskId": "extract-20250118-def456",
      "fileName": "invoice.pdf",
      "templateId": "invoice-template-001",
      "status": "processing",
      "progress": 65,
      "createdAt": "2025-01-18T10:25:00",
      "completedAt": null
    }
  ],
  "traceId": "...",
  "timestamp": "2025-01-18T10:32:00"
}
```

**注意**: 仅返回最近20条任务记录，历史任务可通过taskId直接访问。

---

## 🖼️ 接口6: 获取页面图片

### `GET /api/rule-extract/extract/page-image/{taskId}/{pageNumber}`

**功能描述**: 获取文档指定页面的渲染图片

**请求地址**
```
GET https://your-domain.com/api/rule-extract/extract/page-image/{taskId}/{pageNumber}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |
| `pageNumber` | number | 路径 | ✅ | 页码（从1开始） |

### 请求示例

**Java 示例（下载页面图片）**
```java
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/rule-extract/extract/page-image/extract-20250118-abc123/1"))
    .GET()
    .build();

HttpResponse<Path> response = client.send(request,
    HttpResponse.BodyHandlers.ofFile(Paths.get("page-1.png")));
System.out.println("图片已保存: " + response.body());
```

**Python 示例（下载页面图片）**
```python
import requests

url = "https://your-domain.com/api/rule-extract/extract/page-image/extract-20250118-abc123/1"
response = requests.get(url)

with open("page-1.png", "wb") as f:
    f.write(response.content)
print("图片已保存: page-1.png")
```

**PHP 示例（下载页面图片）**
```php
<?php
$url = "https://your-domain.com/api/rule-extract/extract/page-image/extract-20250118-abc123/1";
$image_content = file_get_contents($url);

file_put_contents("page-1.png", $image_content);
echo "图片已保存: page-1.png";
?>
```

### 响应说明

- **响应类型**: `image/png` (二进制图片流)
- **响应头**: `Content-Type: image/png`
- **使用场景**: 在前端显示图文对照的抽取结果

---

## 🔧 抽取模板说明

### 模板结构

抽取模板定义了需要从文档中提取的字段及其规则：

```json
{
  "templateId": "contract-template-001",
  "templateName": "标准合同模板",
  "fields": [
    {
      "fieldName": "contract_no",
      "displayName": "合同编号",
      "rule": {
        "type": "keyword",
        "keyword": "合同编号",
        "direction": "right",
        "maxDistance": 100
      }
    },
    {
      "fieldName": "party_a",
      "displayName": "甲方",
      "rule": {
        "type": "keyword",
        "keyword": "甲方",
        "direction": "right",
        "maxDistance": 200
      }
    },
    {
      "fieldName": "amount",
      "displayName": "合同金额",
      "rule": {
        "type": "regex",
        "pattern": "¥[\\d,]+\\.\\d{2}",
        "context": "合同金额"
      }
    }
  ]
}
```

### 规则类型

| 规则类型 | 说明 | 使用场景 |
|---------|------|---------|
| `keyword` | 关键字定位 | 字段位于固定关键字附近 |
| `regex` | 正则表达式 | 字段格式固定（如日期、金额） |
| `table` | 表格定位 | 字段位于表格中 |
| `position` | 位置定位 | 字段位于固定位置 |

---

## 💡 使用示例

### 完整流程示例

```javascript
// 1. 上传文档并开始抽取
async function extractContract(file, templateId) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('templateId', templateId)
  formData.append('ignoreHeaderFooter', 'true')
  
  const uploadRes = await axios.post('/api/rule-extract/extract/upload', formData, {
    headers: { 'X-API-Key': 'your-api-key-here' }
  })
  
  const taskId = uploadRes.data.data.taskId
  console.log('任务创建成功:', taskId)
  
  // 2. 轮询等待任务完成
  while (true) {
    const statusRes = await axios.get(`/api/rule-extract/extract/status/${taskId}`, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const status = statusRes.data.data.status
    const progress = statusRes.data.data.progress
    
    console.log(`进度: ${progress}%`)
    
    if (status === 'completed') {
      break
    } else if (status === 'failed') {
      throw new Error('抽取失败')
    }
    
    await new Promise(resolve => setTimeout(resolve, 3000))
  }
  
  // 3. 获取抽取结果
  const resultRes = await axios.get(`/api/rule-extract/extract/result/${taskId}`, {
    headers: { 'X-API-Key': 'your-api-key-here' }
  })
  
  return resultRes.data.data.extractedData
}

// 使用
try {
  const data = await extractContract(fileObject, 'contract-template-001')
  console.log('合同编号:', data.contract_no.value)
  console.log('甲方:', data.party_a.value)
  console.log('乙方:', data.party_b.value)
  console.log('金额:', data.amount.value)
} catch (error) {
  console.error('抽取失败:', error)
}
```

### Vue组件示例

```vue
<template>
  <div class="extract-uploader">
    <el-upload
      ref="upload"
      :auto-upload="false"
      :on-change="handleFileChange"
      accept=".pdf"
    >
      <el-button slot="trigger" type="primary">选择PDF文件</el-button>
    </el-upload>
    
    <el-select v-model="templateId" placeholder="选择抽取模板">
      <el-option label="合同模板" value="contract-template-001"></el-option>
      <el-option label="发票模板" value="invoice-template-001"></el-option>
    </el-select>
    
    <el-button 
      @click="startExtract" 
      :loading="extracting"
      :disabled="!file || !templateId"
    >
      开始抽取
    </el-button>
    
    <el-progress 
      v-if="extracting" 
      :percentage="progress"
      :status="progressStatus"
    ></el-progress>
    
    <div v-if="result" class="result-display">
      <h3>抽取结果</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item 
          v-for="(item, key) in result" 
          :key="key"
          :label="key"
        >
          {{ item.value }} 
          <el-tag size="small" type="info">{{ (item.confidence * 100).toFixed(0) }}%</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const file = ref(null)
const templateId = ref('')
const extracting = ref(false)
const progress = ref(0)
const progressStatus = ref('')
const result = ref(null)

const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
}

const startExtract = async () => {
  extracting.value = true
  progress.value = 0
  progressStatus.value = ''
  
  try {
    // 上传文件
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('templateId', templateId.value)
    
    const uploadRes = await axios.post('/api/rule-extract/extract/upload', formData)
    const taskId = uploadRes.data.data.taskId
    
    // 轮询状态
    while (true) {
      const statusRes = await axios.get(`/api/rule-extract/extract/status/${taskId}`)
      const status = statusRes.data.data.status
      progress.value = statusRes.data.data.progress
      
      if (status === 'completed') {
        progressStatus.value = 'success'
        break
      } else if (status === 'failed') {
        progressStatus.value = 'exception'
        throw new Error('抽取失败')
      }
      
      await new Promise(resolve => setTimeout(resolve, 2000))
    }
    
    // 获取结果
    const resultRes = await axios.get(`/api/rule-extract/extract/result/${taskId}`)
    result.value = resultRes.data.data.extractedData
    
    ElMessage.success('抽取完成！')
    
  } catch (error) {
    ElMessage.error(error.message || '抽取失败')
  } finally {
    extracting.value = false
  }
}
</script>
```

---

## 🎯 最佳实践

### 1. 文档质量要求

**✅ 推荐**:
- 清晰的扫描件或原生PDF
- 分辨率≥300 DPI
- 文字清晰可读
- 无严重倾斜或变形

**❌ 不推荐**:
- 模糊不清的照片
- 分辨率过低（<200 DPI）
- 严重倾斜或变形
- 手写文字

### 2. 模板设计建议

- 使用具有代表性的样本文档设计模板
- 关键字应该唯一且明显
- 考虑字段位置的容错范围
- 定期更新和优化模板规则

### 3. 错误处理

```javascript
try {
  const result = await extractDocument(file, templateId)
  
  // 检查完成率
  if (result.completionRate < 0.8) {
    console.warn('抽取完成率较低，建议人工核对')
  }
  
  // 检查置信度
  Object.entries(result.extractedData).forEach(([key, field]) => {
    if (field.confidence < 0.85) {
      console.warn(`字段"${key}"置信度较低，建议人工核对`)
    }
  })
  
} catch (error) {
  // 处理错误
}
```

### 4. 性能优化

| 优化项 | 建议 | 说明 |
|-------|------|------|
| 轮询间隔 | 2-5秒 | 避免频繁请求 |
| 批量处理 | 异步提交 | 不要等待单个完成 |
| 缓存结果 | 本地存储 | 减少重复获取 |
| 超时设置 | 120秒 | 避免长时间等待 |

---

## 📊 状态码说明

| 状态码 | 类型 | 说明 |
|-------|------|------|
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 参数缺失或格式错误 |
| 404 | 未找到 | 任务或模板不存在 |
| 415 | 格式错误 | 不支持的文件格式 |
| 500 | 服务器错误 | 处理异常 |

### 业务错误码

| 错误码 | 说明 |
|-------|------|
| 11001 | 抽取模板不存在 |
| 11002 | 抽取任务不存在 |
| 11003 | OCR识别失败 |
| 11004 | 规则匹配失败 |
| 17001 | 文件为空 |
| 17002 | 文件格式不支持 |

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

