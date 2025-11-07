# 智能合同合成 - API版本 文档

## 📍 接口地址汇总

| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **合成合同** | POST | `/api/compose/sdt` | 基于模板和数据合成合同 |
| **获取字段列表** | GET | `/api/template/fields` | 获取可用字段 |
| **保存模板设计** | POST | `/api/template/design/save` | 保存模板设计 |
| **获取模板设计** | GET | `/api/template/design/{id}` | 获取模板设计详情 |
| **上传模板文档** | POST | `/api/template/design/upload` | 上传Word模板文件 |

---

## 📋 基础信息

| 项目 | 内容 |
|------|------|
| **API版本** | v1.0 |
| **基础URL** | `https://your-domain.com` |
| **认证方式** | API Key（X-API-Key请求头） |
| **响应格式** | JSON |
| **模板格式** | Word DOCX（基于ContentControl） |

---

## 🎯 功能概述

**API版本**通过直接调用后端接口的方式实现合同合成，第三方系统需要自行开发前端界面，但可以完全自定义UI和业务流程。

### 核心特点

- ✅ **完全自定义UI**：可根据业务需求定制界面
- ✅ **灵活的业务流程**：可集成到现有系统流程
- ✅ **批量处理**：支持批量合成合同
- ✅ **深度集成**：与业务系统无缝对接

### 适用场景

- 需要完全自定义UI界面
- 需要深度集成到现有系统
- 有专业的前端开发团队
- 需要批量自动化处理

---

## 🎯 接口1: 合成合同

### `POST /api/compose/sdt`

**功能描述**: 使用模板文件和变量数据合成合同文档

**请求地址**
```
POST https://your-domain.com/api/compose/sdt
Content-Type: application/json
```

### 请求参数

#### JSON请求体

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `templateFileId` | string | ✅ | 模板文件ID |
| `values` | object | ✅ | 字段值映射表（tag -> value） |

**values字段说明**：
- key: ContentControl的tag名称
- value: 要填充的内容（支持文本、HTML表格等）

### 请求示例

**简单合同示例**
```json
{
  "templateFileId": "file-123",
  "values": {
    "contract_no": "HT20250118001",
    "party_a_name": "北京某某科技有限公司",
    "party_b_name": "上海某某商贸有限公司",
    "contract_amount": "1000000.00",
    "sign_date": "2025年1月18日"
  }
}
```

**包含表格的合同**
```json
{
  "templateFileId": "file-123",
  "values": {
    "contract_no": "HT20250118001",
    "party_a_name": "北京某某科技有限公司",
    "party_b_name": "上海某某商贸有限公司",
    "product_list": "<table style='width:100%; border-collapse: collapse;'><thead><tr style='background:#409eff; color:#fff;'><th style='border:1px solid #ddd; padding:8px;'>序号</th><th style='border:1px solid #ddd; padding:8px;'>产品名称</th><th style='border:1px solid #ddd; padding:8px;'>数量</th><th style='border:1px solid #ddd; padding:8px;'>单价</th></tr></thead><tbody><tr><td style='border:1px solid #ddd; padding:8px;'>1</td><td style='border:1px solid #ddd; padding:8px;'>笔记本电脑</td><td style='border:1px solid #ddd; padding:8px;'>10</td><td style='border:1px solid #ddd; padding:8px;'>8000</td></tr></tbody></table>"
  }
}
```

**包含条款变量的合同**
```json
{
  "templateFileId": "file-123",
  "values": {
    "contract_clause": "甲方：${party_a}，乙方：${party_b}，就${service_name}达成如下协议：",
    "party_a": "北京某某公司",
    "party_b": "上海某某公司",
    "service_name": "技术服务"
  }
}
```

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
String json = """
    {
        "templateFileId": "file-123",
        "values": {
            "contract_no": "HT20250118001",
            "party_a_name": "北京某某科技有限公司",
            "party_b_name": "上海某某商贸有限公司",
            "contract_amount": "1000000.00"
        }
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compose/sdt"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compose/sdt"
payload = {
    "templateFileId": "file-123",
    "values": {
        "contract_no": "HT20250118001",
        "party_a_name": "北京某某科技有限公司",
        "party_b_name": "上海某某商贸有限公司",
        "contract_amount": "1000000.00"
    }
}

response = requests.post(url, json=payload)
result = response.json()
print(result)

# 下载生成的合同
docx_url = result['data']['docxDownloadUrl']
pdf_url = result['data']['pdfDownloadUrl']
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compose/sdt";
$data = array(
    "templateFileId" => "file-123",
    "values" => array(
        "contract_no" => "HT20250118001",
        "party_a_name" => "北京某某科技有限公司",
        "party_b_name" => "上海某某商贸有限公司",
        "contract_amount" => "1000000.00"
    )
);

$options = array(
    'http' => array(
        'header'  => "Content-Type: application/json\r\n",
        'method'  => 'POST',
        'content' => json_encode($data)
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);
$response = json_decode($result, true);

// 下载链接
$docx_url = $response['data']['docxDownloadUrl'];
$pdf_url = $response['data']['pdfDownloadUrl'];

print_r($response);
?>
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "合成成功",
  "data": {
    "fileId": "composed-456",
    "docxDownloadUrl": "/api/files/download/composed-456.docx",
    "pdfDownloadUrl": "/api/files/download/composed-456.pdf",
    "stampedDownloadUrl": "/api/files/download/composed-456-stamped.pdf",
    "fileName": "合同-HT20250118001.docx"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

**响应字段说明**:

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `fileId` | string | 生成的文件ID |
| `docxDownloadUrl` | string | Word文档下载URL |
| `pdfDownloadUrl` | string | PDF文档下载URL（如果启用转换） |
| `stampedDownloadUrl` | string | 盖章版PDF下载URL（如果配置了印章） |
| `fileName` | string | 文件名 |

#### 错误响应

**模板文件不存在 (404)**
```json
{
  "code": 400,
  "message": "文件不存在：无法获取模板文件路径，文件ID: file-123",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

**values为空 (400)**
```json
{
  "code": 400,
  "message": "values 不能为空",
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📋 接口2: 获取字段列表

### `GET /api/template/fields`

**功能描述**: 获取系统支持的模板字段列表

**请求地址**
```
GET https://your-domain.com/api/template/fields
```

### 请求示例

```bash
curl -X GET "https://your-domain.com/api/template/fields" \
  -H "X-API-Key: your-api-key-here"
```

### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "baseFields": [
      {
        "tag": "contract_no",
        "label": "合同编号",
        "type": "text",
        "required": true
      },
      {
        "tag": "contract_name",
        "label": "合同名称",
        "type": "text",
        "required": true
      }
    ],
    "clauseFields": [
      {
        "tag": "clause_payment",
        "label": "付款条款",
        "type": "clause",
        "required": false
      }
    ],
    "counterpartyFields": [
      {
        "tag": "party_a_name",
        "label": "甲方名称",
        "type": "text",
        "required": true
      }
    ],
    "sealFields": [
      {
        "tag": "seal_party_a",
        "label": "甲方印章",
        "type": "seal",
        "required": false
      }
    ]
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📋 接口3: 上传模板文档

### `POST /api/template/design/upload`

**功能描述**: 上传Word模板文件（必须是.docx格式）

**请求地址**
```
POST https://your-domain.com/api/template/design/upload
Content-Type: multipart/form-data
```

### 请求参数

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `file` | file | ✅ | Word模板文件（.docx） |
| `templateId` | string | ✅ | 模板ID |

### 请求示例

```bash
curl -X POST "https://your-domain.com/api/template/design/upload" \
  -H "X-API-Key: your-api-key-here" \
  -F "file=@template.docx" \
  -F "templateId=template-001"
```

### 响应示例

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": "design-123",
    "templateId": "template-001",
    "fileId": "file-789",
    "fileName": "template.docx"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📋 接口4: 保存模板设计

### `POST /api/template/design/save`

**功能描述**: 保存模板设计的元素配置

**请求地址**
```
POST https://your-domain.com/api/template/design/save
Content-Type: application/json
```

### 请求参数

```json
{
  "id": "design-123",
  "templateId": "template-001",
  "templateName": "标准买卖合同",
  "templateCode": "TRADE_CONTRACT",
  "version": "1.0",
  "fileId": "file-789",
  "elementsJson": "[{\"tag\":\"contract_no\",\"label\":\"合同编号\",\"type\":\"text\"}]",
  "description": "标准买卖合同模板"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "id": "design-123",
    "templateName": "标准买卖合同",
    "templateCode": "TRADE_CONTRACT",
    "version": "1.0"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 📋 接口5: 获取模板设计

### `GET /api/template/design/{id}`

**功能描述**: 获取模板设计详情

**请求地址**
```
GET https://your-domain.com/api/template/design/{id}
```

### 请求示例

```bash
curl -X GET "https://your-domain.com/api/template/design/design-123" \
  -H "X-API-Key: your-api-key-here"
```

### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "design-123",
    "templateId": "template-001",
    "templateName": "标准买卖合同",
    "templateCode": "TRADE_CONTRACT",
    "version": "1.0",
    "fileId": "file-789",
    "elementsJson": "[...]",
    "description": "标准买卖合同模板",
    "createdBy": "admin",
    "createdAt": "2025-01-18T10:00:00"
  },
  "traceId": "...",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 💡 完整使用流程

### 流程图

```
1. 准备Word模板（插入ContentControl）
   ↓
2. 上传模板文件（POST /api/template/design/upload）
   ↓
3. 保存模板设计（POST /api/template/design/save）
   ↓
4. 准备合同数据
   ↓
5. 调用合成接口（POST /api/compose/sdt）
   ↓
6. 下载生成的文档
```

### 完整代码示例

```javascript
// 完整的合同合成流程
async function createAndComposeContract() {
  try {
    // 步骤1: 上传模板文件
    const formData = new FormData()
    formData.append('file', templateFile)
    formData.append('templateId', 'template-001')
    
    const uploadRes = await axios.post('/api/template/design/upload', formData, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    const fileId = uploadRes.data.data.fileId
    console.log('模板上传成功，文件ID:', fileId)
    
    // 步骤2: 保存模板设计
    const saveRes = await axios.post('/api/template/design/save', {
      templateId: 'template-001',
      templateName: '标准买卖合同',
      templateCode: 'TRADE_CONTRACT',
      version: '1.0',
      fileId: fileId,
      elementsJson: JSON.stringify(elements),
      description: '标准买卖合同模板'
    }, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    console.log('模板保存成功')
    
    // 步骤3: 合成合同
    const composeRes = await axios.post('/api/compose/sdt', {
      templateFileId: fileId,
      values: {
        contract_no: 'HT20250118001',
        party_a_name: '北京某某科技有限公司',
        party_b_name: '上海某某商贸有限公司',
        contract_amount: '1000000.00',
        sign_date: '2025年1月18日',
        product_list: generateProductTable()
      }
    }, {
      headers: { 'X-API-Key': 'your-api-key-here' }
    })
    
    console.log('合同合成成功')
    console.log('Word下载:', composeRes.data.data.docxDownloadUrl)
    console.log('PDF下载:', composeRes.data.data.pdfDownloadUrl)
    
    // 步骤4: 下载文档
    window.open(composeRes.data.data.docxDownloadUrl, '_blank')
    
  } catch (error) {
    console.error('流程失败:', error)
  }
}

// 生成产品表格HTML
function generateProductTable() {
  const products = [
    { no: 1, name: '笔记本电脑', spec: 'ThinkPad X1', qty: 10, price: 8000, amount: 80000 },
    { no: 2, name: '显示器', spec: 'Dell 27寸', qty: 10, price: 2000, amount: 20000 }
  ]
  
  let rows = ''
  products.forEach(p => {
    rows += `<tr>
      <td style='border:1px solid #ddd; padding:8px;'>${p.no}</td>
      <td style='border:1px solid #ddd; padding:8px;'>${p.name}</td>
      <td style='border:1px solid #ddd; padding:8px;'>${p.spec}</td>
      <td style='border:1px solid #ddd; padding:8px;'>${p.qty}</td>
      <td style='border:1px solid #ddd; padding:8px;'>${p.price}</td>
      <td style='border:1px solid #ddd; padding:8px;'>${p.amount}</td>
    </tr>`
  })
  
  return `<table style='width:100%; border-collapse: collapse;'>
    <thead>
      <tr style='background:#409eff; color:#fff;'>
        <th style='border:1px solid #ddd; padding:8px;'>序号</th>
        <th style='border:1px solid #ddd; padding:8px;'>产品名称</th>
        <th style='border:1px solid #ddd; padding:8px;'>规格</th>
        <th style='border:1px solid #ddd; padding:8px;'>数量</th>
        <th style='border:1px solid #ddd; padding:8px;'>单价</th>
        <th style='border:1px solid #ddd; padding:8px;'>金额</th>
      </tr>
    </thead>
    <tbody>${rows}</tbody>
  </table>`
}
```

### 批量合成示例

```javascript
// 批量合成多份合同
async function batchComposeContracts(templateFileId, dataList) {
  const results = []
  
  for (const data of dataList) {
    try {
      const res = await axios.post('/api/compose/sdt', {
        templateFileId: templateFileId,
        values: data
      }, {
        headers: { 'X-API-Key': 'your-api-key-here' }
      })
      
      results.push({
        success: true,
        data: res.data.data
      })
      
      console.log(`合同 ${data.contract_no} 合成成功`)
      
    } catch (error) {
      results.push({
        success: false,
        error: error.message
      })
      
      console.error(`合同 ${data.contract_no} 合成失败:`, error)
    }
  }
  
  return results
}

// 使用
const contracts = [
  { contract_no: 'HT001', party_a_name: '公司A', ... },
  { contract_no: 'HT002', party_a_name: '公司B', ... },
  { contract_no: 'HT003', party_a_name: '公司C', ... }
]

const results = await batchComposeContracts('file-123', contracts)
console.log(`成功: ${results.filter(r => r.success).length}`)
console.log(`失败: ${results.filter(r => !r.success).length}`)
```

---

## 🎨 表格HTML格式说明

详细的表格格式说明请参考：[文档合成功能支持说明.md](./文档合成功能支持说明.md)

### 基本表格示例

```html
<table style="width:100%; border-collapse: collapse;">
  <thead>
    <tr style="background:#409eff; color:#ffffff;">
      <th style="border:1px solid #ddd; padding:8px;">表头1</th>
      <th style="border:1px solid #ddd; padding:8px;">表头2</th>
    </tr>
  </thead>
  <tbody style="color:#606266;">
    <tr>
      <td style="border:1px solid #ddd; padding:8px;">内容1</td>
      <td style="border:1px solid #ddd; padding:8px;">内容2</td>
    </tr>
  </tbody>
</table>
```

---

## 🎯 最佳实践

### 1. 错误处理

```javascript
try {
  const res = await composeContract(templateFileId, values)
  // 处理成功
} catch (error) {
  if (error.response) {
    switch(error.response.data.code) {
      case 400:
        console.error('参数错误:', error.response.data.message)
        break
      case 404:
        console.error('模板文件不存在')
        break
      case 13001:
        console.error('合成失败:', error.response.data.message)
        break
      default:
        console.error('系统错误')
    }
  }
}
```

### 2. 数据验证

```javascript
function validateContractData(values) {
  const required = ['contract_no', 'party_a_name', 'party_b_name']
  
  for (const field of required) {
    if (!values[field]) {
      throw new Error(`缺少必填字段: ${field}`)
    }
  }
  
  return true
}
```

### 3. 性能优化

| 优化项 | 建议 | 说明 |
|-------|------|------|
| 并发控制 | 最多5个 | 避免服务器压力过大 |
| 重试机制 | 最多3次 | 网络错误时自动重试 |
| 超时设置 | 60秒 | 复杂文档可能需要更长时间 |
| 结果缓存 | 本地存储 | 避免重复合成 |

---

## 📊 状态码说明

| 状态码 | 类型 | 说明 |
|-------|------|------|
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 参数缺失或格式错误 |
| 404 | 未找到 | 模板或文件不存在 |
| 500 | 服务器错误 | 处理异常 |

### 业务错误码

| 错误码 | 说明 |
|-------|------|
| 13001 | 合成模板不存在 |
| 13002 | 合成失败 |
| 13003 | 模板文件损坏 |
| 17001 | 文件为空 |
| 17002 | 文件格式不支持 |

---

## 📞 技术支持

如有疑问，请联系：

- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)

---


