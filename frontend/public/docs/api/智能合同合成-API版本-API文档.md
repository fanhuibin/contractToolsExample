# 智能合同合成 - API版本 文档

## 📚 文档导航

- 本文档：接口定义与请求/响应格式
- [合同合成模板设计指南](./合同合成模板设计指南.md)：在线设计模块、字段体系、自定义字段
- [集成智能合同合成](./集成智能合同合成.md)：合成流程、构建请求、下载结果

---

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
| `extraFiles` | array | ❌ | 需要合并的额外PDF文件URL列表（会在合成后合并，合并后再盖骑缝章） |
| `stampImageUrls` | object | ❌ | 普通章图片URL映射（key为模板字段的tag，value为包含normal章URL的对象） |
| `ridingStampUrl` | string | ❌ | 骑缝章图片URL（如果提供则会在合并后的PDF上盖骑缝章，不绑定任何字段） |

**values字段说明**：
- **key: 必须是ContentControl的tag名称**（从模板查询接口返回的`elementsJson`中获取）
- **重要**：必须使用完整的tag，格式为`tagElement{code}_{timestamp}_{random}`，例如：`tagElementbase_contractCode_1762826537996_2xuxwr`
- **不要使用code作为key**，必须使用tag
- value: 要填充的内容（支持文本、HTML表格等）
- **注意**：对于印章字段（type为"seal"），value由系统自动生成，不需要在values中传递

**stampImageUrls字段说明**：
- key: 模板字段的tag（从模板查询接口返回的`elementsJson`中获取，格式如：`tagElementseal_party_a_1762827325581_rjhjvg`）
- value: 包含`normal`字段的对象，`normal`为普通章图片URL
- **如何识别印章字段**：在模板的`elementsJson`中，`type === "seal"`的字段即为印章字段
- **示例**：
  ```json
  {
    "tagElementseal_party_a_1762827325581_rjhjvg": {
      "normal": "https://example.com/stamps/party_a_seal.png"
    },
    "tagElementseal_party_b_1762827344941_0yfagr": {
      "normal": "https://example.com/stamps/party_b_seal.png"
    }
  }
  ```

**ridingStampUrl字段说明**：
- 骑缝章是独立参数，不绑定任何模板字段
- 只有明确提供此参数才会盖骑缝章
- 骑缝章会在合并后的完整PDF上盖章（包括主合同和所有附件）

### 请求示例

**简单合同示例**
```json
{
  "templateFileId": "file-123",
  "values": {
    "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
    "tagElementbase_projectName_1762826556548_lqpzxi": "绝热材料采购项目",
    "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
    "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司",
    "tagElementbase_signLocation_1762826575800_m9nit8": "北京市"
  }
}
```

> 💡 **说明**：
> - `values` 的key必须是模板字段的tag（从模板查询接口的`elementsJson`中获取）
> - tag格式：`tagElement{code}_{timestamp}_{random}`，例如：`tagElementbase_contractCode_1762826537996_2xuxwr`
> - 不要使用code作为key，必须使用完整的tag

**包含额外PDF文件的合同（合并后再盖骑缝章）**
```json
{
  "templateFileId": "file-123",
  "values": {
    "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
    "tagElementbase_projectName_1762826556548_lqpzxi": "绝热材料采购项目",
    "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
    "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司",
    "tagElementbase_signLocation_1762826575800_m9nit8": "北京市"
  },
  "extraFiles": [
    "https://example.com/attachments/attachment1.pdf",
    "https://example.com/attachments/attachment2.pdf"
  ],
  "stampImageUrls": {
    "tagElementseal_party_a_1762827325581_rjhjvg": {
      "normal": "https://example.com/stamps/party_a_seal.png"
    },
    "tagElementseal_party_b_1762827344941_0yfagr": {
      "normal": "https://example.com/stamps/party_b_seal.png"
    }
  },
  "ridingStampUrl": "https://example.com/stamps/riding_seal.png"
}
```

> 💡 **说明**：
> - `extraFiles` 中的PDF文件会在合同合成后自动下载并合并
> - 合并顺序：主合同PDF + extraFiles[0] + extraFiles[1] + ...
> - 合并完成后再进行盖章操作
> - `stampImageUrls` 的key是模板字段的tag（从模板查询接口的`elementsJson`中获取）
> - 印章字段的识别：在模板的`elementsJson`中，`type === "seal"`的字段即为印章字段
> - 印章字段的value由系统自动生成，不需要在`values`中传递
> - `ridingStampUrl` 用于骑缝章，**只有明确提供此参数才会盖骑缝章**，不绑定任何字段
> - 如果某个PDF下载失败，会跳过该文件继续处理其他文件

**包含表格的合同**
```json
{
  "templateFileId": "file-123",
  "values": {
    "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
    "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
    "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司",
    "tagElementbase_productTable_1762826784646_hl679b": "<table style='width:100%; border-collapse: collapse;'><thead><tr style='background:#409eff; color:#fff;'><th style='border:1px solid #ddd; padding:8px;'>序号</th><th style='border:1px solid #ddd; padding:8px;'>产品名称</th><th style='border:1px solid #ddd; padding:8px;'>数量</th><th style='border:1px solid #ddd; padding:8px;'>单价</th></tr></thead><tbody><tr><td style='border:1px solid #ddd; padding:8px;'>1</td><td style='border:1px solid #ddd; padding:8px;'>笔记本电脑</td><td style='border:1px solid #ddd; padding:8px;'>10</td><td style='border:1px solid #ddd; padding:8px;'>8000</td></tr></tbody></table>"
  }
}
```

**完整示例：合同 + 附件PDF + 普通章 + 骑缝章**
```json
{
  "templateFileId": "file-123",
  "values": {
    "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
    "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
    "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司"
  },
  "extraFiles": [
    "https://example.com/contracts/attachment1.pdf",
    "https://example.com/contracts/attachment2.pdf"
  ],
  "stampImageUrls": {
    "tagElementseal_party_a_1762827325581_rjhjvg": {
      "normal": "https://example.com/stamps/party_a_seal.png"
    },
    "tagElementseal_party_b_1762827344941_0yfagr": {
      "normal": "https://example.com/stamps/party_b_seal.png"
    }
  },
  "ridingStampUrl": "https://example.com/stamps/riding_seal.png"
}
```

> 📋 **处理流程**：
> 1. 合成合同DOCX并转换为PDF
> 2. 下载并合并 `extraFiles` 中的所有PDF文件
> 3. 在合并后的PDF上盖普通章（normal，如果有 `stampImageUrls`）
> 4. 在合并后的PDF上盖骑缝章（**只有提供了 `ridingStampUrl` 才会执行**）
> 
> 💡 **印章字段说明**：
> - 印章字段的tag从模板查询接口的`elementsJson`中获取
> - 印章字段的value由系统自动生成，不需要在`values`中传递
> - 骑缝章是独立参数，不绑定任何字段

**包含条款变量的合同**
```json
{
  "templateFileId": "file-123",
  "values": {
    "tagElementclause_1_1762826900000_abc123": "甲方：${party_a}，乙方：${party_b}，就${service_name}达成如下协议：",
    "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某公司",
    "tagElementparty_b_name_1762826824516_08dxc4": "上海某某公司",
    "tagElementbase_serviceName_1762826920000_def456": "技术服务"
  }
}
```

> 💡 **说明**：
> - 条款字段中可以包含变量引用（如`${party_a}`），系统会自动替换为对应字段的值
> - 变量引用的字段名也是tag，不是code

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
String json = """
    {
        "templateFileId": "file-123",
        "values": {
            "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
            "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
            "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司"
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
        "tagElementbase_contractCode_1762826537996_2xuxwr": "HT20250118001",
        "tagElementparty_a_name_1762826807666_4dgdl6": "北京某某科技有限公司",
        "tagElementparty_b_name_1762826824516_08dxc4": "上海某某商贸有限公司"
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
        "tagElementbase_contractCode_1762826537996_2xuxwr" => "HT20250118001",
        "tagElementparty_a_name_1762826807666_4dgdl6" => "北京某某科技有限公司",
        "tagElementparty_b_name_1762826824516_08dxc4" => "上海某某商贸有限公司"
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
function validateContractData(values, templateElements) {
  // 从模板元素中获取必填字段的tag
  const requiredTags = templateElements
    .filter(el => el.required) // 假设有required字段标识
    .map(el => el.tag)
  
  for (const tag of requiredTags) {
    if (!values[tag]) {
      throw new Error(`缺少必填字段: ${tag}`)
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

## ❓ 常见问题

### Q1: 如何使用印章功能？

**A**: 印章功能分为普通章和骑缝章两种：

**普通章（公章）**：
1. 在模板设计阶段插入印章字段（type为"seal"）
2. 获取模板信息，从`elementsJson`中找出所有`type === "seal"`的字段
3. 使用字段的`tag`作为`stampImageUrls`的key，传递印章图片URL

```javascript
// 1. 获取模板信息
const templateInfo = await getTemplateInfo(templateCode);
const elements = JSON.parse(templateInfo.elementsJson).elements;

// 2. 找出所有印章字段
const sealElements = elements.filter(el => el.type === 'seal');

// 3. 构建stampImageUrls（使用tag作为key）
const stampImageUrls = {};
sealElements.forEach(seal => {
  stampImageUrls[seal.tag] = {
    normal: `https://example.com/stamps/${seal.meta.code}.png`
  };
});

// 4. 构建values（使用tag作为key，不是code）
const values = {};
elements.forEach(el => {
  if (el.type !== 'seal') { // 印章字段的value由系统生成，不需要传递
    values[el.tag] = getFieldValue(el); // 根据字段类型获取值
  }
});

// 5. 调用合成接口
const request = {
  templateCode: templateCode,
  values: values, // 所有字段都使用tag作为key
  stampImageUrls: stampImageUrls // 印章也使用tag作为key
};
```

**骑缝章**：
- 骑缝章是独立参数，不绑定任何模板字段
- 直接传递`ridingStampUrl`参数即可

```javascript
const request = {
  templateCode: templateCode,
  values: { /* 合同数据 */ },
  ridingStampUrl: 'https://example.com/stamps/riding_seal.png'
};
```

**注意事项**：
- 印章字段的value由系统自动生成，不需要在`values`中传递
- 只有明确提供了`ridingStampUrl`才会盖骑缝章
- 普通章会在PDF中对应字段的位置盖章
- 骑缝章会在整个PDF文档上盖章（包括所有页面）

### Q2: 如何合并额外的PDF文件？

**A**: 使用`extraFiles`字段传递PDF文件URL列表：

```javascript
const request = {
  templateCode: 'purchase_contract',
  values: { /* 合同数据 */ },
  extraFiles: [
    'https://example.com/attachments/attachment1.pdf',
    'https://example.com/attachments/attachment2.pdf'
  ],
  stampImageUrls: { /* 普通章 */ },
  ridingStampUrl: 'https://example.com/stamps/riding_seal.png'
};
```

**处理顺序**：
1. 合成合同DOCX并转换为PDF
2. 下载并合并`extraFiles`中的所有PDF文件
3. 在合并后的PDF上盖普通章（如果有）
4. 在合并后的PDF上盖骑缝章（如果提供了`ridingStampUrl`）

---

## 📞 技术支持

如有疑问，请联系：

- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)

---


