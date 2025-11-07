# 文档格式转换 API 接口文档

## 📍 接口地址汇总

| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **上传并转换** | POST | `/api/convert/upload` | 上传文档并转换为PDF |
| **下载PDF** | GET | `/api/convert/download/{fileName}` | 下载转换后的PDF文件 |

---

## 📋 基础信息

| 项目 | 内容 |
|------|------|
| **API版本** | v1.0 |
| **基础URL** | `https://your-domain.com` |
| **认证方式** | 需要授权（文档格式转换模块） |
| **响应格式** | JSON |
| **支持格式** | Word(.doc, .docx), Excel(.xls, .xlsx), PowerPoint(.ppt, .pptx) |
| **转换引擎** | OnlyOffice Document Server |

---

## 🎯 接口1: 上传并转换文档

### `POST /api/convert/upload`

**功能描述**: 上传Office文档（Word/Excel/PPT），自动转换为PDF格式

**开发状态**: ✅ 已实现

**请求地址**
```
POST https://your-domain.com/api/convert/upload
Content-Type: multipart/form-data
```

### 请求参数

#### 表单参数（必需）

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `file` | file | ✅ | Office文档文件 |

**支持的文件格式**:
- Word文档: `.doc`, `.docx`
- Excel表格: `.xls`, `.xlsx`
- PowerPoint演示: `.ppt`, `.pptx`

**文件处理说明**:
- **文件大小**: 建议不超过50MB
- **转换时间**: 一般5-30秒（取决于文件大小和复杂度）
- **存储位置**: 转换后的PDF存储在服务器，保留3天
- **自动清理**: 临时文件在转换完成后自动删除

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
HttpPost uploadFile = new HttpPost("https://your-domain.com/api/convert/upload");

MultipartEntityBuilder builder = MultipartEntityBuilder.create();
builder.addBinaryBody("file", new File("/path/to/document.docx"));
HttpEntity multipart = builder.build();

uploadFile.setEntity(multipart);
CloseableHttpResponse response = httpClient.execute(uploadFile);
System.out.println(EntityUtils.toString(response.getEntity()));
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/convert/upload"
files = {'file': open('/path/to/document.docx', 'rb')}

response = requests.post(url, files=files)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/convert/upload";
$file_path = "/path/to/document.docx";

$curl = curl_init();

$file = new CURLFile($file_path);
$post_data = array('file' => $file);

curl_setopt_array($curl, array(
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => $post_data,
    CURLOPT_RETURNTRANSFER => true
));

$response = curl_exec($curl);
curl_close($curl);

$result = json_decode($response, true);
print_r($result);
?>
```

**JavaScript/Axios 示例**
```javascript
const formData = new FormData();
formData.append('file', fileObject);

const response = await axios.post('/api/convert/upload', formData, {
  headers: {
    'Content-Type': 'multipart/form-data'
  }
});
```

**HTML表单示例**
```html
<form action="/api/convert/upload" method="post" enctype="multipart/form-data">
  <input type="file" name="file" accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx" required />
  <button type="submit">转换为PDF</button>
</form>
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "转换成功",
  "data": {
    "success": true,
    "fileId": 123,
    "downloadUrl": "/api/convert/download/123",
    "fileName": "abc123def456.pdf",
    "originalName": "合同文档.pdf"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

**响应字段说明**:

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `success` | boolean | 转换是否成功 |
| `fileId` | number | 文件ID（用于下载） |
| `downloadUrl` | string | PDF下载URL（相对路径，使用文件ID） |
| `fileName` | string | 服务器上的文件名（UUID格式） |
| `originalName` | string | 原始文件名（转换为.pdf扩展名） |

#### 错误响应示例

**文件为空 (400)**
```json
{
  "code": 17001,
  "message": "请选择要转换的文件",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

**文件格式不支持 (415)**
```json
{
  "code": 17002,
  "message": "不支持的文件格式。支持的格式：doc, docx, xls, xlsx, ppt, pptx",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

**转换失败 (500)**
```json
{
  "code": 16001,
  "message": "文档转换失败，请检查OnlyOffice服务状态",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

**模块未授权 (403)**
```json
{
  "code": 18001,
  "message": "文档格式转换功能需要授权",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📥 接口2: 下载转换后的PDF

### `GET /api/convert/download/{fileId}`

**功能描述**: 下载已转换完成的PDF文件

**请求地址**
```
GET https://your-domain.com/api/convert/download/{fileId}
```

### 请求参数

#### 路径参数

| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `fileId` | number | 路径 | ✅ | 文件ID（从转换接口返回的fileId） |

### 请求示例

**直接访问URL**
```
GET https://your-domain.com/api/convert/download/123
```

**Java 示例（下载文件）**
```java
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;

// 使用转换接口返回的 fileId
Long fileId = 123L;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/convert/download/" + fileId))
    .GET()
    .build();

HttpResponse<Path> response = client.send(request,
    HttpResponse.BodyHandlers.ofFile(Paths.get("downloaded.pdf")));
System.out.println("文件已保存: " + response.body());
```

**Python 示例（下载文件）**
```python
import requests

# 使用转换接口返回的 fileId
file_id = 123

url = f"https://your-domain.com/api/convert/download/{file_id}"
response = requests.get(url)

with open("downloaded.pdf", "wb") as f:
    f.write(response.content)
print("文件已保存: downloaded.pdf")
```

**PHP 示例（下载文件）**
```php
<?php
// 使用转换接口返回的 fileId
$file_id = 123;

$url = "https://your-domain.com/api/convert/download/" . $file_id;
$file_content = file_get_contents($url);

file_put_contents("downloaded.pdf", $file_content);
echo "文件已保存: downloaded.pdf";
?>
```

**JavaScript 示例**
```javascript
// 使用转换接口返回的 fileId
const fileId = 123;

// 方式1: 直接下载
window.open('/api/convert/download/' + fileId);

// 方式2: Fetch API
const response = await fetch('/api/convert/download/' + fileId);
const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'converted.pdf';
a.click();
```

### 响应说明

#### 成功响应
- **响应类型**: `application/pdf` (二进制文件流)
- **响应头**:
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `attachment; filename*=UTF-8''abc123def456.pdf`
  - `Content-Length`: 文件大小（字节）

浏览器会自动触发下载对话框，或在新标签页中打开PDF。

#### 错误响应

**文件不存在 (404)**
- HTTP状态码: 404
- 无响应体

**参数错误 (400)**
- HTTP状态码: 400
- 场景: 文件名包含非法字符（如`..`, `/`, `\`）

---

## 🔧 转换流程说明

### 完整转换流程

```
1. 客户端上传文件
   ↓
2. 服务器验证文件格式
   ↓
3. 保存临时文件
   ↓
4. 调用OnlyOffice转换服务
   ↓
5. 生成PDF文件
   ↓
6. 删除临时文件
   ↓
7. 返回下载链接
   ↓
8. 客户端使用链接下载PDF
```

### 文件存储策略

| 类型 | 存储位置 | 保留时间 | 清理策略 |
|------|---------|---------|---------|
| 临时文件 | `uploads/temp/{date}/` | 转换完成即删除 | 自动清理 |
| 转换后PDF | `uploads/converted/{date}/` | 3天 | 定期清理 |

**说明**:
- `{date}` 格式为 `yyyy/MM/dd`
- 文件名使用UUID确保唯一性
- 下载接口会在最近3天的目录中搜索文件

---

## 📊 状态码说明

| 状态码 | 类型 | 说明 |
|-------|------|------|
| 200 | 成功 | 转换成功 |
| 400 | 参数错误 | 文件为空或参数无效 |
| 403 | 权限错误 | 未授权使用该功能 |
| 404 | 未找到 | PDF文件不存在或已过期 |
| 415 | 格式错误 | 不支持的文件格式 |
| 500 | 服务器错误 | 转换失败或服务异常 |

### 业务错误码

| 错误码 | 说明 |
|-------|------|
| 16001 | 文档转换失败 |
| 17001 | 文件为空 |
| 17002 | 文件格式不支持 |
| 18001 | 模块未授权 |

---

## 💡 使用示例

### 示例1: 完整的前端上传流程

```vue
<template>
  <div class="convert-uploader">
    <input 
      type="file" 
      @change="handleFileChange"
      accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx"
    />
    <el-button 
      @click="convertFile" 
      :loading="converting"
      :disabled="!file"
    >
      {{ converting ? '转换中...' : '转换为PDF' }}
    </el-button>
    
    <div v-if="downloadUrl">
      <el-button type="success" @click="downloadPDF">
        下载PDF
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const file = ref(null)
const converting = ref(false)
const downloadUrl = ref('')

const handleFileChange = (event) => {
  file.value = event.target.files[0]
}

const convertFile = async () => {
  if (!file.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  converting.value = true
  
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    
    const res = await axios.post('/api/convert/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    downloadUrl.value = res.data.data.downloadUrl
    ElMessage.success('转换成功！')
    
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '转换失败')
  } finally {
    converting.value = false
  }
}

const downloadPDF = () => {
  window.open(downloadUrl.value)
}
</script>
```

### 示例2: Java客户端调用

```java
import okhttp3.*;
import java.io.*;

public class DocumentConverter {
    
    private static final String BASE_URL = "https://your-domain.com";
    private final OkHttpClient client = new OkHttpClient();
    
    /**
     * 转换文档为PDF
     */
    public String convertToPdf(File file) throws IOException {
        RequestBody fileBody = RequestBody.create(
            MediaType.parse("application/octet-stream"), 
            file
        );
        
        MultipartBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(), fileBody)
            .build();
        
        Request request = new Request.Builder()
            .url(BASE_URL + "/api/convert/upload")
            .post(requestBody)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("转换失败: " + response);
            }
            
            // 解析响应获取下载URL
            String json = response.body().string();
            JSONObject data = JSON.parseObject(json);
            return data.getJSONObject("data").getString("downloadUrl");
        }
    }
    
    /**
     * 下载PDF文件
     */
    public void downloadPdf(String downloadUrl, File destFile) throws IOException {
        Request request = new Request.Builder()
            .url(BASE_URL + downloadUrl)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败: " + response);
            }
            
            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
```

### 示例3: Python客户端调用

```python
import requests

class DocumentConverter:
    def __init__(self, base_url="https://your-domain.com"):
        self.base_url = base_url
    
    def convert_to_pdf(self, file_path):
        """
        转换文档为PDF
        
        Args:
            file_path: 文档文件路径
            
        Returns:
            dict: 响应数据，包含downloadUrl
        """
        url = f"{self.base_url}/api/convert/upload"
        
        with open(file_path, 'rb') as f:
            files = {'file': f}
            response = requests.post(url, files=files)
        
        response.raise_for_status()
        return response.json()['data']
    
    def download_pdf(self, download_url, dest_path):
        """
        下载PDF文件
        
        Args:
            download_url: 下载URL
            dest_path: 目标保存路径
        """
        url = f"{self.base_url}{download_url}"
        response = requests.get(url, stream=True)
        response.raise_for_status()
        
        with open(dest_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)

# 使用示例
converter = DocumentConverter()

# 转换文档
result = converter.convert_to_pdf('contract.docx')
print(f"转换成功: {result['originalName']}")

# 下载PDF
converter.download_pdf(result['downloadUrl'], 'contract.pdf')
print("下载完成")
```

---

## 🔒 安全说明

### 文件安全

1. **路径遍历防护**: 文件名自动检查，拒绝包含 `..`, `/`, `\` 的请求
2. **格式验证**: 严格验证文件扩展名
3. **大小限制**: 建议设置上传文件大小限制（如50MB）
4. **自动清理**: 临时文件和过期文件自动删除

### 访问控制

- **模块授权**: 需要"文档格式转换"模块的使用权限
- **文件隔离**: 每个转换任务的文件独立存储
- **URL安全**: 下载URL使用UUID，难以被猜测

---

## ⚙️ OnlyOffice配置要求

### 必需配置

```yaml
# application.yml
zxcm:
  onlyoffice:
    server: http://onlyoffice-server:80
    document-server-domain: http://onlyoffice-server
    document-server-port: 80
    secret: your-secret-key
  
  application:
    base-url: https://your-domain.com
  
  file-upload:
    root-path: ./uploads
```

### OnlyOffice Document Server

- **版本要求**: OnlyOffice Document Server 7.0+
- **服务端口**: 默认80端口
- **转换API**: 使用Document Server的转换服务
- **部署方式**: 可使用Docker快速部署

---

## 🎉 最佳实践

### 1. 错误处理

```javascript
try {
  const res = await convertToPdf(file)
  // 处理成功
} catch (error) {
  if (error.response) {
    // 服务器返回错误
    switch(error.response.data.code) {
      case 17001:
        alert('请选择文件')
        break
      case 17002:
        alert('不支持的文件格式')
        break
      case 16001:
        alert('转换失败，请稍后重试')
        break
      case 18001:
        alert('您没有使用此功能的权限')
        break
      default:
        alert('转换失败')
    }
  } else {
    // 网络错误
    alert('网络连接失败')
  }
}
```

### 2. 进度提示

由于转换可能需要一定时间，建议：
- 显示加载动画
- 禁用提交按钮防止重复提交
- 显示"转换中..."提示
- 转换完成后提供下载按钮

### 3. 文件验证

在上传前进行客户端验证：
- 检查文件大小
- 检查文件格式
- 显示友好的错误提示

---

## 📞 联系方式

如有疑问，请联系：
- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)

---


