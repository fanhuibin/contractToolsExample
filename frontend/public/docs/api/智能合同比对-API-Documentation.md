# 智能文档比对 API 接口文档

## 📍 接口地址汇总

### 核心接口
| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **提交比对任务** | POST | `/submit-url` | 通过JSON + URL方式提交文档比对（对外接口） |
| **提交比对任务（内部）** | POST | `/submit` | 通过文件上传方式提交文档比对（内部接口） |
| **获取任务状态** | GET | `/task/{taskId}` | 获取任务处理状态和进度 |
| **获取Canvas比对结果** | GET | `/canvas-result/{taskId}` | 获取Canvas版本的比对结果 |
| **获取文档图片信息** | GET | `/images/{taskId}/{mode}` | 获取文档图片信息 |
| **获取任务列表** | GET | `/tasks` | 获取所有任务列表 |
| **删除任务** | DELETE | `/task/{taskId}` | 删除指定任务 |
| **保存用户修改** | POST | `/save-user-modifications/{taskId}` | 保存用户对差异项的忽略和备注 |
| **获取用户修改** | GET | `/get-user-modifications/{taskId}` | 获取用户修改的忽略和备注信息 |
| **导出比对报告** | POST | `/export-report` | 导出比对结果为Word/HTML格式 |

### 用户修改管理接口
| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **保存用户修改** | POST | `/save-user-modifications/{taskId}` | 保存用户的差异忽略和备注 |
| **获取用户修改** | GET | `/get-user-modifications/{taskId}` | 获取用户的修改信息 |

### 系统管理接口
| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **获取队列状态** | GET | `/queue/stats` | 获取任务队列统计信息 |
| **检查队列繁忙** | GET | `/queue/busy` | 检查队列是否繁忙 |
| **调整并发数** | POST | `/queue/adjust-concurrency` | 调整最大并发线程数（1-20） |
| **获取原始坐标** | GET | `/debug/raw-coords/{taskId}` | 获取原始坐标数据（调试用） |

## 📋 基础信息

| 项目 | 内容 |
|------|------|
| **API版本** | v1.0 |
| **基础URL** | `https://your-domain.com/api/compare-pro` |
| **认证方式** | 暂无 |
| **响应格式** | JSON |
| **支持文件** | 仅支持PDF格式 |

---

## 🎯 接口1: 提交比对任务

### `POST /submit`

**功能描述**: 提交合同比对pro版文档比对任务，上传两个PDF文档进行智能比对分析

**开发状态**: ✅ 已实现JSON + URL参数方式的新接口 `/submit-url`（原有multipart/form-data接口 `/submit` 保持不变）

**请求地址**
```
POST https://your-domain.com/api/compare-pro/submit-url
Content-Type: application/json
```

### 请求参数

#### 文件参数（必需）

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `oldFileUrl` | string | ✅ | 原始PDF文档的HTTP/HTTPS链接 |
| `newFileUrl` | string | ✅ | 新版本PDF文档的HTTP/HTTPS链接 |

**支持的文件格式**:
- 仅支持PDF格式: `.pdf`

**文件处理说明**:
- **URL方式**: 系统会自动下载URL指向的文件进行处理
- **文件大小**: 单个PDF文件最大50MB
- **支持协议**: 支持HTTP和HTTPS协议
- **下载超时**: 文件下载超时时间为60秒
- **存储位置**: 下载的文件临时存储在服务器，任务完成后自动清理
- **安全保障**: 所有文件传输采用HTTPS加密

#### 比对选项（可选）
| 参数名 | 类型 | 默认值 | 状态 | 描述 |
|--------|------|--------|------|------|
| `removeWatermark` | boolean | `false` | ✅ 已实现 | 是否去除水印（默认使用默认强度） |

#### 比对选项（开发中）
⚠️ **以下功能正在开发中，暂不可用**

| 参数名 | 类型 | 默认值 | 计划用途 |
|--------|------|--------|----------|
| `ignoreCase` | boolean | `true` | 是否忽略大小写差异 |
| `ignoredSymbols` | string | `"_＿"` | 忽略指定符号集（如下划线、破折号等） |
| `ignoreSpaces` | boolean | `false` | 是否忽略空格差异 |
| `ignoreSeals` | boolean | `true` | 是否忽略印章区域 |

#### 系统固定参数（无需传递）
| 参数名 | 固定值 | 说明 |
|--------|--------|------|
| `ignoreHeaderFooter` | `true` | 系统自动忽略页眉页脚 |
| `headerHeightPercent` | `12.0` | 页眉高度固定为12% |
| `footerHeightPercent` | `12.0` | 页脚高度固定为12% |
| `watermarkRemovalStrength` | `"default"` | 水印去除强度固定为默认强度 |

### 请求示例

**JSON请求示例**

**示例1: 公开文档URL（使用默认设置）**
```json
{
    "oldFileUrl": "https://example.com/docs/contract_original.pdf",
    "newFileUrl": "https://example.com/docs/contract_updated.pdf"
}
```

**示例2: 云存储文档URL（启用水印去除）**
```json
{
    "oldFileUrl": "https://storage.googleapis.com/bucket/documents/version1.pdf",
    "newFileUrl": "https://storage.googleapis.com/bucket/documents/version2.pdf",
    "removeWatermark": true
}
```

**示例3: 本地服务器URL**
```json
{
    "oldFileUrl": "http://192.168.1.100:8080/files/agreement_v1.pdf",
    "newFileUrl": "http://192.168.1.100:8080/files/agreement_v2.pdf"
}
```

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
String json = """
    {
        "oldFileUrl": "https://example.com/docs/contract_v1.pdf",
        "newFileUrl": "https://example.com/docs/contract_v2.pdf",
        "removeWatermark": false
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/submit-url"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compare-pro/submit-url"
payload = {
    "oldFileUrl": "https://example.com/docs/contract_v1.pdf",
    "newFileUrl": "https://example.com/docs/contract_v2.pdf",
    "removeWatermark": False
}

response = requests.post(url, json=payload)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/submit-url";
$data = array(
    "oldFileUrl" => "https://example.com/docs/contract_v1.pdf",
    "newFileUrl" => "https://example.com/docs/contract_v2.pdf",
    "removeWatermark" => false
);

$options = array(
    'http' => array(
        'header'  => "Content-Type: application/json\r\n",
        'method'  => 'POST',
        'content' => json_encode($data)
    )
);

$context  = stream_context_create($options);
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
  "message": "合同比对pro版任务提交成功",
  "data": "32fa8f1a-b291-4c01-aad1-9da159e6a705"
}
```

**注意**：`data` 字段直接返回任务ID字符串，不是对象格式。

#### 错误响应示例

**参数错误 (400)**
```json
{
  "code": 400,
  "message": "缺少必需参数: oldFileUrl",
  "data": null
}
```

**网络连接失败 (404)**
```json
{
  "code": 404,
  "message": "无法访问原文档URL: https://example.com/file.pdf",
  "data": null
}
```

**请求超时 (408)**
```json
{
  "code": 408,
  "message": "原文档下载超时",
  "data": null
}
```

**文件过大 (413)**
```json
{
  "code": 413,
  "message": "原文档文件过大，最大支持50MB",
  "data": null
}
```

**文件格式错误 (415)**
```json
{
  "code": 415,
  "message": "原文档格式不支持，仅支持PDF格式",
  "data": null
}
```

**文件下载IO错误 (422)**
```json
{
  "code": 422,
  "message": "原文档下载失败",
  "data": null
}
```

**服务器错误 (500)**
```json
{
  "code": 500,
  "message": "系统内部错误",
  "data": null
}
```

#### 错误码说明

| 错误码 | 错误类型 | 触发场景 | 说明 |
|--------|----------|----------|------|
| 400 | 参数错误 | 缺少必需参数或参数格式错误 | 请检查请求参数 |
| 404 | 网络连接失败 | URL无法访问、网络不通 | 文件URL不存在或网络连接问题 |
| 408 | 请求超时 | 文件下载超时（60秒） | 文件服务器响应慢或网络不稳定 |
| 413 | 文件过大 | 文件大小超过50MB限制 | 请使用较小的文件 |
| 415 | 格式不支持 | 文件不是PDF格式 | 仅支持PDF格式文档 |
| 422 | 下载IO错误 | 文件下载过程中的其他IO错误 | 文件服务器错误或文件损坏 |
| 500 | 服务器错误 | 系统内部错误 | 请联系技术支持 |

---

## 📊 接口2: 获取任务状态

### `GET /task/{taskId}`

**功能描述**: 获取任务处理状态和进度信息，用于轮询任务进度

**请求地址**
```
GET https://your-domain.com/api/compare-pro/task/{taskId}
```

### 请求参数

#### 路径参数
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |


### 任务状态枚举
| 状态 | 描述 | 进度范围 | 详细说明 |
|------|------|----------|----------|
| `PENDING` | 等待中 | 0% | 任务已提交，等待处理 |
| `OCR_PROCESSING` | OCR处理中 | 1-100% | 正在进行OCR识别和文本比对处理 |
| `COMPARING` | 比对中 | - | 文本比对阶段（状态已定义但当前版本未使用） |
| `ANNOTATING` | 标注中 | - | 差异标注阶段（状态已定义但当前版本未使用） |
| `COMPLETED` | 完成 | 100% | 任务处理完成，结果可用 |
| `FAILED` | 失败 | - | 任务处理失败 |
| `TIMEOUT` | 超时 | - | 任务处理超时 |

### 状态流转
任务状态流转: `PENDING` → `OCR_PROCESSING` → `COMPLETED/FAILED/TIMEOUT`

### 请求示例

**说明**: 该接口为GET请求，无需请求体，直接调用即可获取任务状态

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/task/task_20231215_001"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compare-pro/task/task_20231215_001"
response = requests.get(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/task/task_20231215_001";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```



### 响应字段说明

#### 基本任务信息
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `taskId` | string | 任务ID |
| `status` | string | 任务状态枚举值 |
| `statusDescription` | string | 任务状态描述 |
| `oldFileName` | string | 原始文档文件名 |
| `newFileName` | string | 新版本文档文件名 |
| `currentStep` | number | 当前步骤编号 |
| `currentStepDesc` | string | 当前步骤描述 |
| `progress` | number | 进度百分比 (0-100) |

#### 页面级别进度信息
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `totalPages` | number | 总页数（两个文档的最大值） |
| `oldDocPages` | number | 旧文档页数 |
| `newDocPages` | number | 新文档页数 |
| `currentPageOld` | number | 当前处理的旧文档页面 |
| `currentPageNew` | number | 当前处理的新文档页面 |
| `completedPagesOld` | number | 已完成的旧文档页面数 |
| `completedPagesNew` | number | 已完成的新文档页面数 |

#### OCR预估时间信息（可选）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `estimatedOcrTimeOld` | number | 原文档OCR预估时间（毫秒） |
| `estimatedOcrTimeNew` | number | 新文档OCR预估时间（毫秒） |

#### 时间统计信息（完成后才有）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `startTime` | string | 任务开始时间（ISO格式） |
| `endTime` | string | 任务结束时间（ISO格式，仅完成状态） |
| `totalDuration` | number | 总耗时（毫秒，仅完成状态） |
| `stepDurations` | object | 各步骤耗时统计（毫秒） |

#### 错误和失败信息（失败时才有）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `errorMessage` | string | 错误信息（仅失败状态） |
| `failedPages` | array | 识别失败的页面列表 |
| `failedPagesCount` | number | 失败页面数量 |

### 响应示例

#### 处理中状态（OCR识别原文档阶段）
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "status": "OCR_PROCESSING",
        "statusDescription": "OCR处理中",
        "oldFileName": "contract_v1.pdf",
        "newFileName": "contract_v2.pdf",
        "currentStep": 2,
        "currentStepDesc": "OCR识别原文档",
        "progress": 25,
        "totalPages": 104,
        "oldDocPages": 104,
        "newDocPages": 104,
        "currentPageOld": 30,
        "currentPageNew": 0,
        "completedPagesOld": 30,
        "completedPagesNew": 0,
        "estimatedOcrTimeOld": 500000,
        "estimatedOcrTimeNew": 480000,
        "startTime": "2025-09-24T15:01:54.579711400"
    }
}
```

#### 处理中状态（OCR识别新文档阶段）
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "status": "OCR_PROCESSING",
        "statusDescription": "OCR处理中",
        "oldFileName": "contract_v1.pdf",
        "newFileName": "contract_v2.pdf",
        "currentStep": 3,
        "currentStepDesc": "OCR识别新文档",
        "progress": 60,
        "totalPages": 104,
        "oldDocPages": 104,
        "newDocPages": 104,
        "currentPageOld": 104,
        "currentPageNew": 50,
        "completedPagesOld": 104,
        "completedPagesNew": 50,
        "estimatedOcrTimeOld": 500000,
        "estimatedOcrTimeNew": 480000,
        "startTime": "2025-09-24T15:01:54.579711400"
    }
}
```
#### 完成状态
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "status": "COMPLETED",
        "statusDescription": "完成",
        "oldFileName": "contract_v1.pdf",
        "newFileName": "contract_v2.pdf",
        "currentStep": 10,
        "currentStepDesc": "任务完成",
        "progress": 100,
        "totalPages": 104,
        "oldDocPages": 104,
        "newDocPages": 104,
        "currentPageOld": 104,
        "currentPageNew": 104,
        "completedPagesOld": 104,
        "completedPagesNew": 104,
        "startTime": "2025-09-24T15:01:54.579711400",
        "endTime": "2025-09-24T15:09:22.356142800",
        "totalDuration": 447776,
        "stepDurations": {
            "INIT": 3,
            "OCR_FIRST_DOC": 269626,
            "OCR_SECOND_DOC": 169469,
            "OCR_VALIDATION": 7830,
            "TEXT_COMPARE": 65,
            "DIFF_ANALYSIS": 764,
            "BLOCK_MERGE": 7,
            "RESULT_GENERATION": 6,
            "OCR_COMPLETE": 0,
            "TASK_COMPLETE": 1
        },
        "failedPages": [
            "old_contract_v1.pdf-第74页: OCR识别失败",
            "old_contract_v1.pdf-第86页: OCR识别失败",
            "new_contract_v2.pdf-第74页: OCR识别失败",
            "new_contract_v2.pdf-第86页: OCR识别失败"
        ],
        "failedPagesCount": 4
    }
}
```

#### 失败响应示例

**任务不存在 (404)**
```json
{
  "code": 404,
  "message": "任务不存在",
  "data": null
}
```

**任务处理失败**
```json
{
  "code": 200,
  "message": "获取任务状态成功",
  "data": {
    "taskId": "task_20231215_001",
    "status": "FAILED",
    "statusDescription": "失败",
    "oldFileName": "contract_v1.pdf",
    "newFileName": "contract_v2.pdf",
    "currentStep": 5,
    "currentStepDesc": "文本比对",
    "progress": 45,
    "totalPages": 50,
    "oldDocPages": 50,
    "newDocPages": 50,
    "currentPageOld": 25,
    "currentPageNew": 20,
    "completedPagesOld": 25,
    "completedPagesNew": 20,
    "errorMessage": "OCR识别失败：文档格式不支持或文件损坏",
    "startTime": "2023-12-15T10:30:00",
    "endTime": "2023-12-15T10:31:30",
    "totalDuration": 90000,
    "failedPages": [
        "old_contract_v1.pdf-第3页: OCR识别失败",
        "new_contract_v2.pdf-第5页: OCR识别失败"
    ],
    "failedPagesCount": 2
  }
}
```

**任务处理超时**
```json
{
  "code": 200,
  "message": "获取任务状态成功",
  "data": {
    "taskId": "task_20231215_001",
    "status": "TIMEOUT",
    "statusDescription": "超时",
    "oldFileName": "contract_v1.pdf",
    "newFileName": "contract_v2.pdf",
    "currentStep": 3,
    "currentStepDesc": "OCR识别新文档",
    "progress": 25,
    "totalPages": 200,
    "oldDocPages": 200,
    "newDocPages": 200,
    "currentPageOld": 200,
    "currentPageNew": 50,
    "completedPagesOld": 200,
    "completedPagesNew": 50,
    "errorMessage": "任务处理超时：OCR识别耗时过长",
    "startTime": "2023-12-15T10:30:00",
    "endTime": "2023-12-15T10:45:00",
    "totalDuration": 900000
  }
}
```

**服务器错误 (500)**
```json
{
  "code": 500,
  "message": "获取任务状态失败: 数据库连接异常",
  "data": null
}
```

---

## 📊 接口3: 获取Canvas比对结果

### `GET /canvas-result/{taskId}`

**功能描述**: 获取Canvas版本的比对结果，包含差异信息和图片坐标

**注意**: 如果任务未完成会返回 `success: false`，任务完成时会返回包含结果数据的 `success: true` 响应。

**请求地址**
```
GET https://your-domain.com/api/compare-pro/canvas-result/{taskId}
```

### 请求参数

#### 路径参数
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

### 请求示例

**说明**: 该接口为GET请求，无需请求体，任务完成后调用获取比对结果

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/canvas-result/task_20231215_001"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compare-pro/canvas-result/task_20231215_001"
response = requests.get(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/canvas-result/task_20231215_001";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```

### 响应字段说明

#### 差异项（differences）字段说明

每个差异项包含以下字段：

**基本信息**
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `operation` | string | 操作类型：`DELETE`(删除) 或 `INSERT`(新增) |
| `page` | number | 页码 |
| `pageA` | number | 原文档页码 |
| `pageB` | number | 新文档页码 |

**位置信息**
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `oldBboxes` | number[][] | 原文档所有差异框坐标数组，每个元素为 [x1, y1, x2, y2] |
| `newBboxes` | number[][] | 新文档所有差异框坐标数组，每个元素为 [x1, y1, x2, y2] |
| `oldBbox` | number[] | （兼容字段）第一个oldBboxes元素 [x1, y1, x2, y2] |
| `newBbox` | number[] | （兼容字段）第一个newBboxes元素 [x1, y1, x2, y2] |
| `prevOldBboxes` | number[][] | INSERT操作时，原文档的不同处坐标数组 |
| `prevNewBboxes` | number[][] | DELETE操作时，新文档的不同处坐标数组 |
| `prevOldBbox` | number[] | （兼容字段）第一个prevOldBboxes元素 |
| `prevNewBbox` | number[] | （兼容字段）第一个prevNewBboxes元素 |

**文本内容**
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `oldText` | string | 原文档文本内容 |
| `newText` | string | 新文档文本内容 |
| `allTextA` | string[] | 原文档所有文本段落 |
| `allTextB` | string[] | 新文档所有文本段落 |
| `diffRangesA` | object[] | 原文档差异范围标记 |
| `diffRangesB` | object[] | 新文档差异范围标记 |

**用户修改信息（新增）**
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `ignored` | boolean | 是否被用户忽略（仅在调用保存接口后出现，值为true时表示已忽略） |
| `remark` | string | 用户添加的备注信息（仅在调用保存接口且有备注时出现） |

> **注意**：`ignored` 和 `remark` 字段不是默认返回的，只有在用户通过 `/save-user-modifications/{taskId}` 接口保存了修改后，才会在差异项中出现这些字段。

**索引信息**
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `indexA` | number | 差异在原文档字符序列中的索引，-1表示不存在 |
| `indexB` | number | 差异在新文档字符序列中的索引，-1表示不存在 |
| `textStartIndexA` | number | 差异在原文档文本中的起始字符索引 |
| `textStartIndexB` | number | 差异在新文档文本中的起始字符索引 |
| `pageAList` | number[] | 原文档相关页码列表（与oldBboxes对应） |
| `pageBList` | number[] | 新文档相关页码列表（与newBboxes对应） |
| `category` | string | 差异分类标识（可选） |

### 响应示例

#### 成功响应
```json
{
    "code": 200,
    "message": "获取Canvas比对结果成功",
    "data": {
        "totalDuration": 18790,
        "newImageInfo": {
            "totalPages": 6,
            "pages": [
                {
                    "pageNumber": 1,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/new/page-1.png",
                    "width": 1322,
                    "height": 1870
                },
                {
                    "pageNumber": 2,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/new/page-2.png",
                    "width": 1322,
                    "height": 1870
                },
               ......
        },
        "oldImageInfo": {
            "totalPages": 5,
            "pages": [
                {
                    "pageNumber": 1,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-1.png",
                    "width": 1322,
                    "height": 1870
                },
                {
                    "pageNumber": 2,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-2.png",
                    "width": 1322,
                    "height": 1870
                },
                {
                    "pageNumber": 3,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-3.png",
                    "width": 1322,
                    "height": 1870
                },
                {
                    "pageNumber": 4,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-4.png",
                    "width": 1322,
                    "height": 1870
                },
                {
                    "pageNumber": 5,
                    "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-5.png",
                    "width": 1322,
                    "height": 1870
                }
            ]
        },
        "differences": [
            {
                "pageB": 2,
                "pageA": 2,
                "pageAList": [
                    2
                ],
                "oldBboxes": [
                    [
                        197.0,
                        867.0,
                        1117.0,
                        1007.0
                    ]
                ],
                "prevNewBbox": [
                    197.0,
                    941.0,
                    1117.0,
                    1050.0
                ],
                "oldBbox": [
                    197.0,
                    867.0,
                    1117.0,
                    1007.0
                ],
                "diffRangesA": [
                    {
                        "start": 0,
                        "end": 17,
                        "type": "DIFF"
                    }
                ],
                "oldText": "23 WX6 2025 03 06",
                "diffRangesB": [],
                "pageBList": [
                    2
                ],
                "newText": "",
                "prevOldBbox": [
                    197.0,
                    942.0,
                    1117.0,
                    1050.0
                ],
                "allTextB": [],
                "page": 2,
                "textStartIndexB": 0,
                "operation": "DELETE",
                "textStartIndexA": 464,
                "allTextA": [
                    "23 WX6 2025 03 06"
                ]
            },
            {
                "pageB": 2,
                "pageA": 2,
                "pageAList": [
                    2
                ],
                "newBboxes": [
                    [
                        197.0,
                        868.0,
                        1117.0,
                        1007.0
                    ]
                ],
                "prevNewBbox": [
                    267.0,
                    1657.0,
                    1117.0,
                    1696.0
                ],
                "diffRangesA": [],
                "oldText": "",
                "diffRangesB": [
                    {
                        "start": 0,
                        "end": 17,
                        "type": "DIFF"
                    }
                ],
                "pageBList": [
                    2
                ],
                "newText": "23 WX6 2025 03 06",
                "prevOldBbox": [
                    268.0,
                    1657.0,
                    1117.0,
                    1696.0
                ],
                "allTextB": [
                    "23 WX6 2025 03 06"
                ],
                "newBbox": [
                    197.0,
                    868.0,
                    1117.0,
                    1007.0
                ],
                "page": 2,
                "textStartIndexB": 598,
                "operation": "INSERT",
                "textStartIndexA": 0,
                "allTextA": []
            }
        ],
        "newFileName": "1758683907368.pdf",
        "oldImageBaseUrl": "/api/compare-pro/files/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old",
        "oldFileName": "1758683907362.pdf",
        "totalDiffCount": 2,
        "newImageBaseUrl": "/api/compare-pro/files/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/new",
        "failedPages": [],
        "failedPagesCount": 0,
        "startTime": "2025-09-24T15:24:20.046417",
        "endTime": "2025-09-24T15:24:38.836835500",
        "stepDurations": {
            "OCR_FIRST_DOC": 7075,
            "TASK_COMPLETE": 0,
            "INIT": 1,
            "TEXT_COMPARE": 9,
            "BLOCK_MERGE": 0,
            "OCR_VALIDATION": 3949,
            "RESULT_GENERATION": 1,
            "OCR_COMPLETE": 0,
            "DIFF_ANALYSIS": 13,
            "OCR_SECOND_DOC": 7741
        },
        "taskId": "c6a24c49-4ee3-43ae-b426-a70a67a6025d",
        "statistics": {
            "totalDurationMs": 18790,
            "totalSteps": 10,
            "totalDurationHuman": "18秒",
            "taskStartTime": 1758698660046
        }
    }
}
```

---

## 📊 接口4: 获取文档图片信息

### `GET /images/{taskId}/{mode}`

**功能描述**: 获取指定任务的文档图片信息

**请求地址**
```
GET https://your-domain.com/api/compare-pro/images/{taskId}/{mode}
```

### 请求参数

#### 路径参数
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |
| `mode` | string | 路径 | ✅ | 图片模式: `old`(原始文档) 或 `new`(新版本文档) |

### 请求示例

**说明**: 该接口为GET请求，任务完成后调用获取文档图片信息

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();

// 获取原始文档图片信息
HttpRequest request1 = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/images/task_20231215_001/old"))
    .GET()
    .build();
HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());

// 获取新版本文档图片信息
HttpRequest request2 = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/images/task_20231215_001/new"))
    .GET()
    .build();
HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
```

**Python 示例**
```python
import requests

# 获取原始文档图片信息
url_old = "https://your-domain.com/api/compare-pro/images/task_20231215_001/old"
response_old = requests.get(url_old)
print(response_old.json())

# 获取新版本文档图片信息
url_new = "https://your-domain.com/api/compare-pro/images/task_20231215_001/new"
response_new = requests.get(url_new)
print(response_new.json())
```

**PHP 示例**
```php
<?php
// 获取原始文档图片信息
$url_old = "https://your-domain.com/api/compare-pro/images/task_20231215_001/old";
$result_old = file_get_contents($url_old);
$response_old = json_decode($result_old, true);

// 获取新版本文档图片信息
$url_new = "https://your-domain.com/api/compare-pro/images/task_20231215_001/new";
$result_new = file_get_contents($url_new);
$response_new = json_decode($result_new, true);

print_r($response_old);
print_r($response_new);
?>
```

### 响应示例

#### 获取原始文档图片信息
```json
{
    "code": 200,
    "message": "获取文档图片信息成功",
    "data": {
        "totalPages": 5,
        "pages": [
            {
                "pageNumber": 1,
                "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-1.png",
                "width": 1322,
                "height": 1870
            },
            {
                "pageNumber": 2,
                "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-2.png",
                "width": 1322,
                "height": 1870
            },
            {
                "pageNumber": 3,
                "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-3.png",
                "width": 1322,
                "height": 1870
            },
            {
                "pageNumber": 4,
                "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-4.png",
                "width": 1322,
                "height": 1870
            },
            {
                "pageNumber": 5,
                "imageUrl": "/api/compare-pro/files/compare-pro/tasks/c6a24c49-4ee3-43ae-b426-a70a67a6025d/images/old/page-5.png",
                "width": 1322,
                "height": 1870
            }
        ]
    }
}
```

---

## 🗂️ 接口5: 获取任务列表

### `GET /tasks`

**功能描述**: 获取用户的所有比对任务列表

**请求地址**
```
GET https://your-domain.com/api/compare-pro/tasks
```

### 请求示例

**说明**: 该接口为GET请求，无需请求体，直接调用即可获取所有任务列表

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/tasks"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compare-pro/tasks"
response = requests.get(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/tasks";
$result = file_get_contents($url);
$response = json_decode($result, true);
print_r($response);
?>
```



### 响应字段说明

**注意**: 此接口返回简化的任务信息列表，仅包含核心字段。

#### 任务列表对象字段
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `taskId` | string | 任务ID |
| `oldFileName` | string | 原始文档文件名 |
| `newFileName` | string | 新版本文档文件名 |
| `startTime` | string | 任务开始时间（ISO格式，可能为null） |
| `endTime` | string | 任务结束时间（ISO格式，可能为null） |
| `differenceCount` | number | 差异总数（仅完成的任务，否则为null） |
| `resultUrl` | string | 结果页面URL（仅完成的任务，否则为null） |

### 响应示例
```json
{
    "code": 200,
    "message": "获取任务列表成功",
    "data": [
        {
            "taskId": "bee86e38-2595-4de8-9df0-5d44c5d2e7e5",
            "oldFileName": "contract_original.pdf",
            "newFileName": "contract_updated.pdf",
            "startTime": "2025-09-25T09:38:05.5295092",
            "endTime": "2025-09-25T09:38:26.5976414",
            "differenceCount": 2,
            "resultUrl": "/api/compare-pro/canvas-result/bee86e38-2595-4de8-9df0-5d44c5d2e7e5"
        },
        {
            "taskId": "5b39fa5e-fd2d-44e2-b14d-e888c21ed17d",
            "oldFileName": "agreement_v1.pdf",
            "newFileName": "agreement_v2.pdf",
            "startTime": "2025-09-25T09:40:15.123456",
            "endTime": null,
            "differenceCount": null,
            "resultUrl": null
        },
        {
            "taskId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "oldFileName": "contract_2024.pdf",
            "newFileName": "contract_2025.pdf",
            "startTime": null,
            "endTime": null,
            "differenceCount": null,
            "resultUrl": null
        }
    ]
}
```

**说明：**
- 列表按任务开始时间倒序排列（最新的在前面）
- 已完成的任务包含 `differenceCount` 和 `resultUrl`
- 处理中或未完成的任务这两个字段为 `null`

---

## 🗂️ 接口6: 删除任务

### `DELETE /task/{taskId}` - 删除单个任务

**功能描述**: 删除指定的比对任务及其相关数据

**请求地址**
```
DELETE https://your-domain.com/api/compare-pro/task/{taskId}
```

#### 请求参数

**路径参数**
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 要删除的任务ID |

#### 请求示例

**Java 示例**
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/task/task_20231215_001"))
    .DELETE()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

**Python 示例**
```python
import requests

url = "https://your-domain.com/api/compare-pro/task/task_20231215_001"
response = requests.delete(url)
print(response.json())
```

**PHP 示例**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/task/task_20231215_001";

$options = array(
    'http' => array(
        'method'  => 'DELETE'
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);
$response = json_decode($result, true);
print_r($response);
?>
```

#### 响应示例

**删除成功**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

**任务不存在**
```json
{
  "code": 404,
  "message": "任务不存在或已删除",
  "data": null
}
```

---

## 💾 接口7: 保存用户修改

### `POST /save-user-modifications/{taskId}`

**功能描述**: 保存用户对差异项的忽略和备注信息，用于个性化的差异审查流程

**请求地址**
```
POST https://your-domain.com/api/compare-pro/save-user-modifications/{taskId}
Content-Type: application/json
```

### 请求参数

#### 路径参数
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

#### 请求体参数（JSON）
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `ignoredDifferences` | number[] | ✅ | 已忽略的差异项索引数组 |
| `remarks` | object | ✅ | 备注信息，键为差异项索引（字符串），值为备注内容 |

### 请求示例

**示例1: 忽略部分差异并添加备注**
```json
{
    "ignoredDifferences": [2, 5, 8],
    "remarks": {
        "0": "此差异已确认，需要保留",
        "3": "与客户沟通后确认为正常修改",
        "6": "格式调整，无实质性变化"
    }
}
```

**示例2: 仅添加备注**
```json
{
    "ignoredDifferences": [],
    "remarks": {
        "1": "重要条款变更，需法务审核",
        "4": "金额修改，已与财务确认"
    }
}
```

**cURL 示例**
```bash
curl -X POST "https://your-domain.com/api/compare-pro/save-user-modifications/task_20231215_001" \
  -H "Content-Type: application/json" \
  -d '{
    "ignoredDifferences": [2, 5],
    "remarks": {
      "0": "此差异已确认",
      "3": "正常修改"
    }
  }'
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "用户修改已保存",
  "data": null
}
```

#### 错误响应示例

**任务不存在 (404)**
```json
{
  "code": 404,
  "message": "任务不存在",
  "data": null
}
```

**参数错误 (400)**
```json
{
  "code": 400,
  "message": "参数错误: ignoredDifferences必须是数组",
  "data": null
}
```

**服务器错误 (500)**
```json
{
  "code": 500,
  "message": "保存用户修改失败: 数据库写入异常",
  "data": null
}
```

---

## 📖 接口8: 获取用户修改

### `GET /get-user-modifications/{taskId}`

**功能描述**: 获取用户之前保存的忽略和备注信息，用于页面刷新后恢复状态

**请求地址**
```
GET https://your-domain.com/api/compare-pro/get-user-modifications/{taskId}
```

### 请求参数

#### 路径参数
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| `taskId` | string | 路径 | ✅ | 任务ID |

### 请求示例

**cURL 示例**
```bash
curl -X GET "https://your-domain.com/api/compare-pro/get-user-modifications/task_20231215_001"
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "获取用户修改成功",
  "data": {
    "ignoredDifferences": [2, 5, 8],
    "remarks": {
      "0": "此差异已确认，需要保留",
      "3": "与客户沟通后确认为正常修改",
      "6": "格式调整，无实质性变化"
    }
  }
}
```

#### 无用户修改
```json
{
  "code": 200,
  "message": "获取用户修改成功",
  "data": {
    "ignoredDifferences": [],
    "remarks": {}
  }
}
```

#### 错误响应示例

**任务不存在 (404)**
```json
{
  "code": 404,
  "message": "任务不存在",
  "data": null
}
```

**服务器错误 (500)**
```json
{
  "code": 500,
  "message": "获取用户修改失败: 数据库读取异常",
  "data": null
}
```

---

## 📥 接口9: 导出比对报告

### `POST /export-report`

**功能描述**: 导出比对结果为Word或HTML格式，支持自定义导出内容

**请求地址**
```
POST https://your-domain.com/api/compare-pro/export-report
Content-Type: application/json
```

### 请求参数

#### 请求体参数（JSON）

| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `taskId` | string | ✅ | 任务ID |
| `formats` | string[] | ✅ | 导出格式数组，可选值: `["doc"]`, `["html"]`, `["doc", "html"]` |
| `includeIgnored` | boolean | ❌ | 是否包含已忽略的差异，默认false |
| `includeRemarks` | boolean | ❌ | 是否包含备注信息，默认true |

**注意：** 用户修改（忽略差异、备注）通过独立的接口管理，不在此请求体中传递。详见 [用户修改管理接口](#用户修改管理接口)。

### 请求示例

**示例1: 导出Word格式**
```json
{
    "taskId": "task_20231215_001",
    "formats": ["doc"],
    "includeIgnored": false,
    "includeRemarks": true
}
```

**示例2: 同时导出Word和HTML格式**
```json
{
    "taskId": "task_20231215_001",
    "formats": ["doc", "html"],
    "includeIgnored": false,
    "includeRemarks": true
}
```

**示例3: 导出包含用户修改信息**

如需包含用户修改，应先使用用户修改管理接口保存修改：
```bash
# 1. 先保存用户修改
POST /api/compare-pro/save-user-modifications/task_20231215_001
{
    "ignoredDifferences": [2, 5, 8],
    "remarks": {
        "0": "此差异已确认，需要保留",
        "3": "与客户沟通后确认为正常修改"
    }
}

# 2. 再导出（会自动包含已保存的用户修改）
POST /api/compare-pro/export-report
{
    "taskId": "task_20231215_001",
    "formats": ["doc"],
    "includeIgnored": false,
    "includeRemarks": true
}
```

**Java 示例（导出为文件）**
```java
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;

HttpClient client = HttpClient.newHttpClient();
String json = """
    {
        "taskId": "task_20231215_001",
        "formats": ["doc"],
        "includeIgnored": false,
        "includeRemarks": true
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://your-domain.com/api/compare-pro/export-report"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<Path> response = client.send(request, 
    HttpResponse.BodyHandlers.ofFile(Paths.get("compare_report.doc")));
System.out.println("文件已保存: " + response.body());
```

**Python 示例（导出为文件）**
```python
import requests

url = "https://your-domain.com/api/compare-pro/export-report"
payload = {
    "taskId": "task_20231215_001",
    "formats": ["doc"],
    "includeIgnored": False,
    "includeRemarks": True
}

response = requests.post(url, json=payload)

# 保存为文件
with open("compare_report.doc", "wb") as f:
    f.write(response.content)
print("文件已保存: compare_report.doc")
```

**PHP 示例（导出为文件）**
```php
<?php
$url = "https://your-domain.com/api/compare-pro/export-report";
$data = array(
    "taskId" => "task_20231215_001",
    "formats" => array("doc"),
    "includeIgnored" => false,
    "includeRemarks" => true
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

// 保存为文件
file_put_contents("compare_report.doc", $result);
echo "文件已保存: compare_report.doc";
?>
```

### 响应说明

#### 成功响应
- **响应类型**: `application/octet-stream` (二进制文件流)
- **响应头**:
  - `Content-Type`: `application/msword` (doc格式) 或 `text/html` (html格式)
  - `Content-Disposition`: `attachment; filename="compare_report_[taskId].[format]"`

#### 导出文件内容说明

**Word文档 (.doc) 包含**:
- 比对任务基本信息（任务ID、文档名称、时间等）
- 差异统计概览
- 详细差异列表（包含页码、操作类型、原文、新文、备注等）
- 用户备注信息（如果启用）
- 处理时长统计

**HTML文档 (.html) 包含**:
- 响应式网页设计
- 差异高亮显示
- 可交互的差异列表
- 用户备注展示
- 可打印优化样式

#### 错误响应示例

**任务不存在 (404)**
```json
{
  "code": 404,
  "message": "任务不存在或结果未生成",
  "data": null
}
```

**参数错误 (400)**
```json
{
  "code": 400,
  "message": "导出格式不支持，仅支持doc和html",
  "data": null
}
```

**服务器错误 (500)**
```json
{
  "code": 500,
  "message": "导出失败: 文件生成异常",
  "data": null
}
```

### 导出格式对比

| 特性 | Word格式 (.doc) | HTML格式 (.html) |
|------|----------------|------------------|
| **文件大小** | 较大 | 较小 |
| **可编辑性** | 可用Word编辑 | 可用编辑器编辑 |
| **兼容性** | 需要Word或WPS | 任何浏览器可打开 |
| **样式保留** | 完整保留 | 完整保留 |
| **打印效果** | 优秀 | 良好 |
| **分享便利性** | 中等 | 高（可直接浏览） |
| **推荐场景** | 需要进一步编辑的正式报告 | 快速查看和分享 |

---

## 📝 接口8: 用户修改管理接口

### 8.1 保存用户修改

#### `POST /save-user-modifications/{taskId}`

**功能描述**: 保存用户对比对结果的修改（忽略差异、添加备注）

**请求地址**
```
POST https://your-domain.com/api/compare-pro/save-user-modifications/{taskId}
Content-Type: application/json
```

#### 路径参数
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `taskId` | string | ✅ | 任务ID |

#### 请求体参数
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `ignoredDifferences` | number[] | ❌ | 已忽略的差异项索引数组 |
| `remarks` | object | ❌ | 备注信息，键为差异项索引(字符串)，值为备注内容 |

#### 请求示例
```json
{
    "ignoredDifferences": [2, 5, 8],
    "remarks": {
        "0": "此差异已确认，需要保留",
        "3": "与客户沟通后确认为正常修改",
        "7": "格式调整，无实质影响"
    }
}
```

#### 响应示例

**成功响应**
```json
{
    "code": 200,
    "message": "用户修改已保存",
    "data": null
}
```

**任务不存在**
```json
{
    "code": 404,
    "message": "任务不存在",
    "data": null
}
```

---

### 8.2 获取用户修改

#### `GET /get-user-modifications/{taskId}`

**功能描述**: 获取用户对比对结果的修改（页面刷新后恢复状态使用）

**请求地址**
```
GET https://your-domain.com/api/compare-pro/get-user-modifications/{taskId}
```

#### 路径参数
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| `taskId` | string | ✅ | 任务ID |

#### 响应示例

**成功响应**
```json
{
    "code": 200,
    "message": "获取用户修改成功",
    "data": {
        "ignoredDifferences": [2, 5, 8],
        "remarks": {
            "0": "此差异已确认，需要保留",
            "3": "与客户沟通后确认为正常修改",
            "7": "格式调整，无实质影响"
        }
    }
}
```

**无用户修改**
```json
{
    "code": 200,
    "message": "获取用户修改成功",
    "data": {
        "ignoredDifferences": [],
        "remarks": {}
    }
}
```

---

