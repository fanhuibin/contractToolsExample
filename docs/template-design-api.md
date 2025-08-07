# 合同模板设计API文档

## 概述

本文档描述了合同模板设计系统的API接口，包括字段获取、文件下载和模板设计发起等功能。

## 基础信息

- **基础URL**: `http://localhost:8080`
- **API前缀**: `/api/template`
- **内容类型**: `application/json`

## 接口列表

### 1. 获取字段信息

获取模板设计所需的基础字段、相对方字段和条款字段信息。

**接口地址**: `GET /api/template/fields`

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "baseFields": [
      {
        "id": "base_001",
        "name": "合同名称",
        "code": "contract_name",
        "isRichText": false
      },
      {
        "id": "base_002",
        "name": "合同描述",
        "code": "contract_description",
        "isRichText": true
      }
    ],
    "counterpartyFields": [
      {
        "id": "cp_001",
        "name": "甲方名称",
        "code": "party_a_name",
        "counterpartyIndex": 1
      },
      {
        "id": "cp_002",
        "name": "乙方名称",
        "code": "party_b_name",
        "counterpartyIndex": 2
      }
    ],
    "clauseFields": [
      {
        "id": "clause_001",
        "name": "第一条",
        "code": "clause_1",
        "content": "甲方：${party_a_name}，乙方：${party_b_name}，就${contract_name}达成如下协议：",
        "type": "general",
        "typeName": "通用条款"
      }
    ]
  }
}
```

### 2. 获取文件下载地址

根据文件ID获取文件的下载地址。

**接口地址**: `GET /api/template/file/download/{fileId}`

**路径参数**:
- `fileId` (string, 必填): 文件ID

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "http://localhost:8080/api/template/file/download/file_001"
}
```

### 3. 发起模板设计

发起一个模板设计会话，返回编辑页面的URL。

**接口地址**: `POST /api/template/design/start`

**请求参数**:
```json
{
  "templateId": "template_001",
  "callbackUrl": "http://localhost:3000/callback",
  "backendUrl": "http://localhost:8080"
}
```

**参数说明**:
- `templateId` (string, 必填): 模板ID
- `callbackUrl` (string, 必填): 回调地址，用于接收设计完成的通知
- `backendUrl` (string, 必填): 后端服务地址

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "editUrl": "http://localhost:8080/onlyoffice?templateId=template_001&sessionId=550e8400-e29b-41d4-a716-446655440000&callbackUrl=http://localhost:3000/callback",
    "status": "SUCCESS",
    "message": "模板设计会话创建成功"
  }
}
```

## 字段说明

### 基础字段 (BaseField)
- `id`: 字段唯一标识
- `name`: 字段显示名称
- `code`: 字段代码，用于表达式引用
- `isRichText`: 是否支持富文本编辑（包括表格）

### 相对方字段 (CounterpartyField)
- `id`: 字段唯一标识
- `name`: 字段显示名称
- `code`: 字段代码，用于表达式引用
- `counterpartyIndex`: 相对方序号（第几个相对方）

### 条款字段 (ClauseField)
- `id`: 条款唯一标识
- `name`: 条款显示名称
- `code`: 条款代码，用于表达式引用
- `content`: 条款内容，支持表达式语法
- `type`: 条款类型
- `typeName`: 条款类型名称

## 表达式语法

在条款内容中，可以使用以下表达式语法插入字段值：

- 基础字段: `${field_code}`
- 相对方字段: `${field_code}`

**示例**:
```
甲方：${party_a_name}，乙方：${party_b_name}，就${contract_name}达成如下协议：
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## SDK使用示例

### Java SDK

```java
// 创建客户端
TemplateDesignClient client = new TemplateDesignClient("http://localhost:8080");

// 获取字段信息
FieldResponse fieldResponse = client.getFields();

// 获取文件下载地址
String downloadUrl = client.getFileDownloadUrl("file_001");

// 发起模板设计
TemplateDesignRequest request = new TemplateDesignRequest();
request.setTemplateId("template_001");
request.setCallbackUrl("http://localhost:3000/callback");
request.setBackendUrl("http://localhost:8080");

TemplateDesignResponse response = client.startTemplateDesign(request);
System.out.println("编辑页面URL: " + response.getEditUrl());
```

## 集成流程

1. **获取字段信息**: 调用 `/api/template/fields` 获取可用的字段列表
2. **发起设计**: 调用 `/api/template/design/start` 创建设计会话
3. **打开编辑器**: 使用返回的 `editUrl` 打开OnlyOffice编辑器
4. **接收回调**: 在指定的 `callbackUrl` 接收设计完成的通知

## 注意事项

1. 所有API调用都需要确保网络连接正常
2. 回调地址必须是可访问的URL
3. 文件ID必须是有效的文件标识
4. 模板设计会话有有效期限制，请及时使用 