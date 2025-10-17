# 肇新科技合同比对pro版 API 接口文档

## 📍 接口地址汇总

| 功能 | 方法 | 接口地址 | 描述 |
|------|------|----------|------|
| **提交比对任务** | POST | `/submit-url` | 通过JSON + URL方式提交文档比对（对外接口） |
| **提交比对任务（内部）** | POST | `/submit` | 通过文件上传方式提交文档比对（内部接口） |
| **获取任务状态** | GET | `/task/{taskId}` | 获取任务处理状态和进度 |
| **获取Canvas比对结果** | GET | `/canvas-result/{taskId}` | 获取Canvas版本的比对结果 |
| **获取文档图片信息** | GET | `/images/{taskId}/{mode}` | 获取文档图片信息 |
| **获取任务列表** | GET | `/tasks` | 获取所有任务列表 |
| **删除任务** | DELETE | `/task/{taskId}` | 删除指定任务 |
| **导出比对报告** | POST | `/export-report` | 导出比对结果为Word/HTML格式 |

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

**cURL 示例**
```bash
curl -X POST "https://your-domain.com/api/compare-pro/submit-url" \
  -H "Content-Type: application/json" \
  -d '{
    "oldFileUrl": "https://example.com/docs/contract_v1.pdf",
    "newFileUrl": "https://example.com/docs/contract_v2.pdf",
    "removeWatermark": false
  }'
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
    "message": "合同比对pro版任务提交成功",
    "data": {
        "taskId": "32fa8f1a-b291-4c01-aad1-9da159e6a705"
    }
}
```

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

**cURL 示例**
```bash
curl -X GET "https://your-domain.com/api/compare-pro/task/task_20231215_001"
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

#### 智能进度信息
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `progressPercentage` | number | 智能计算的进度百分比 (0-100) |
| `progressDescription` | string | 进度描述文字 |
| `currentStepDescription` | string | 当前步骤的详细描述 |
| `remainingTime` | string | 预估剩余时间（格式化字符串） |
| `estimatedTotalTime` | string | 预估总耗时（格式化字符串） |

#### 阶段进度信息（新增）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `stageMinProgress` | number | 当前阶段最小进度值 |
| `stageMaxProgress` | number | 当前阶段最大进度值 |
| `stageEstimatedTime` | number | 当前阶段预估时间（毫秒） |
| `stageElapsedTime` | number | 当前阶段已用时间（毫秒） |

#### 页面级别进度信息（新增）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `totalPages` | number | 总页数（两个文档的最大值） |
| `oldDocPages` | number | 旧文档页数 |
| `newDocPages` | number | 新文档页数 |
| `currentPageOld` | number | 当前处理的旧文档页面 |
| `currentPageNew` | number | 当前处理的新文档页面 |
| `completedPagesOld` | number | 已完成的旧文档页面数 |
| `completedPagesNew` | number | 已完成的新文档页面数 |

#### 时间统计信息
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `startTime` | string | 任务开始时间（ISO格式） |
| `endTime` | string | 任务结束时间（ISO格式，仅完成状态） |
| `totalDuration` | number | 总耗时（毫秒，仅完成状态） |
| `stepDurations` | object | 各步骤耗时统计（毫秒） |

#### 错误和失败信息
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `errorMessage` | string | 错误信息（仅失败状态） |
| `failedPages` | array | 识别失败的页面列表 |
| `failedPagesCount` | number | 失败页面数量 |

### 响应示例

#### 处理中状态
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "stageElapsedTime": 10,
        "currentPageOld": 0,
        "newFileName": "1758683907744.pdf",
        "stageMinProgress": 0.0,
        "oldFileName": "1758683907407.pdf",
        "estimatedTotalTime": "8分钟",
        "remainingTime": "约8分钟",
        "currentPageNew": 0,
        "currentStep": 2,
        "statusDescription": "OCR处理中",
        "currentStepDescription": "OCR识别原文档",
        "completedPagesOld": 0,
        "progressDescription": "1.0%",
        "totalPages": 104,
        "currentStepDesc": "OCR识别原文档",
        "stageMaxProgress": 1.0,
        "progressPercentage": 1.0,
        "newDocPages": 104,
        "startTime": "2025-09-24T15:01:54.579711400",
        "oldDocPages": 104,
        "stageEstimatedTime": 10,
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "completedPagesNew": 0,
        "status": "OCR_PROCESSING"
    }
}
```
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "stageElapsedTime": 10,
        "currentPageOld": 104,
        "newFileName": "1758683907744.pdf",
        "stageMinProgress": 0.0,
        "oldFileName": "1758683907407.pdf",
        "estimatedTotalTime": "8分钟",
        "remainingTime": "约3分钟",
        "currentPageNew": 30,
        "currentStep": 3,
        "statusDescription": "OCR处理中",
        "currentStepDescription": "OCR识别新文档",
        "completedPagesOld": 104,
        "progressDescription": "1.0%",
        "totalPages": 104,
        "currentStepDesc": "OCR识别新文档",
        "stageMaxProgress": 1.0,
        "progressPercentage": 1.0,
        "newDocPages": 104,
        "startTime": "2025-09-24T15:01:54.579711400",
        "oldDocPages": 104,
        "stageEstimatedTime": 10,
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "completedPagesNew": 24,
        "status": "OCR_PROCESSING"
    }
}
```
#### 完成状态
```json
{
    "code": 200,
    "message": "获取任务状态成功",
    "data": {
        "stageElapsedTime": 0,
        "currentPageOld": 104,
        "stageMinProgress": 0.0,
        "oldFileName": "1758683907407.pdf",
        "remainingTime": "约7分钟",
        "currentPageNew": 104,
        "failedPages": [
            "old_1758683907407.pdf-第74页: OCR识别失败",
            "old_1758683907407.pdf-第86页: OCR识别失败",
            "new_1758683907744.pdf-第74页: OCR识别失败",
            "new_1758683907744.pdf-第86页: OCR识别失败"
        ],
        "failedPagesCount": 4,
        "completedPagesOld": 104,
        "progressDescription": "已完成",
        "newDocPages": 104,
        "startTime": "2025-09-24T15:01:54.579711400",
        "stepDurations": {
            "OCR_FIRST_DOC": 269626,
            "TASK_COMPLETE": 1,
            "INIT": 3,
            "TEXT_COMPARE": 65,
            "BLOCK_MERGE": 7,
            "OCR_VALIDATION": 7830,
            "RESULT_GENERATION": 6,
            "OCR_COMPLETE": 0,
            "DIFF_ANALYSIS": 764,
            "OCR_SECOND_DOC": 169469
        },
        "completedPagesNew": 104,
        "totalDuration": 447776,
        "newFileName": "1758683907744.pdf",
        "estimatedTotalTime": "",
        "currentStep": 10,
        "statusDescription": "完成",
        "currentStepDescription": "任务完成",
        "totalPages": 104,
        "currentStepDesc": "任务完成",
        "stageMaxProgress": 100.0,
        "progressPercentage": 100.0,
        "oldDocPages": 104,
        "endTime": "2025-09-24T15:09:22.356142800",
        "stageEstimatedTime": 0,
        "taskId": "36f83ce7-9076-4883-99e3-e3f0f3a6502e",
        "status": "COMPLETED"
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
    "progressPercentage": 45,
    "progressDescription": "任务处理失败",
    "currentStepDescription": "文本比对过程中发生错误",
    "remainingTime": "0秒",
    "estimatedTotalTime": "已耗时1分30秒",
    "errorMessage": "OCR识别失败：文档格式不支持或文件损坏",
    "startTime": "2023-12-15T10:30:00",
    "endTime": "2023-12-15T10:31:30",
    "totalDuration": 90000,
    "failedPages": [3, 5],
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
    "progressPercentage": 25,
    "progressDescription": "任务处理超时",
    "currentStepDescription": "OCR识别超时，任务已终止",
    "remainingTime": "0秒",
    "estimatedTotalTime": "已超时终止",
    "errorMessage": "任务处理超时：OCR识别耗时过长",
    "startTime": "2023-12-15T10:30:00",
    "endTime": "2023-12-15T10:45:00",
    "totalDuration": 900000,
    "failedPages": [],
    "failedPagesCount": 0
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

**cURL 示例**
```bash
curl -X GET "https://your-domain.com/api/compare-pro/canvas-result/task_20231215_001"
```

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

**cURL 示例**
```bash
# 获取原始文档图片信息
curl -X GET "https://your-domain.com/api/compare-pro/images/task_20231215_001/old"

# 获取新版本文档图片信息
curl -X GET "https://your-domain.com/api/compare-pro/images/task_20231215_001/new"
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

**cURL 示例**
```bash
curl -X GET "https://your-domain.com/api/compare-pro/tasks"
```



### 响应字段说明

**注意**: 此接口直接返回CompareTask对象列表，包含以下字段：

#### CompareTask对象字段
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `taskId` | string | 任务ID |
| `status` | string | 任务状态枚举值 |
| `progress` | number | 进度百分比 (0-100) |
| `totalSteps` | number | 总步骤数 |
| `currentStep` | number | 当前步骤编号 |
| `currentStepDesc` | string | 当前步骤描述 |
| `createdTime` | string | 创建时间（ISO格式） |
| `updatedTime` | string | 更新时间（ISO格式） |
| `errorMessage` | string | 错误信息 |
| `oldFileName` | string | 原始文档文件名 |
| `newFileName` | string | 新版本文档文件名 |
| `oldPdfUrl` | string | 原始PDF文件URL |
| `newPdfUrl` | string | 新版本PDF文件URL |
| `annotatedOldPdfUrl` | string | 标注后的原始PDF URL |
| `annotatedNewPdfUrl` | string | 标注后的新版本PDF URL |

#### 新增字段（页面级别进度）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `totalPages` | number | 总页数 |
| `oldDocPages` | number | 旧文档页数 |
| `newDocPages` | number | 新文档页数 |
| `currentPageOld` | number | 当前处理的旧文档页面 |
| `currentPageNew` | number | 当前处理的新文档页面 |
| `completedPagesOld` | number | 已完成的旧文档页面数 |
| `completedPagesNew` | number | 已完成的新文档页面数 |

#### 新增字段（时间统计）
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `startTime` | string | 任务开始时间（ISO格式） |
| `endTime` | string | 任务结束时间（ISO格式） |
| `totalDuration` | number | 总耗时（毫秒） |
| `stepDurations` | object | 各步骤耗时统计 |
| `failedPages` | array | 识别失败的页面列表 |
| `statistics` | object | 统计信息 |

### 响应示例
```json
{
    "code": 200,
    "message": "获取任务列表成功",
    "data": [
        {
            "differenceCount": 2,
            "newFileName": "1758683907368.pdf",
            "resultUrl": "/api/compare-pro/canvas-result/bee86e38-2595-4de8-9df0-5d44c5d2e7e5",
            "startTime": "2025-09-25T09:38:05.5295092",
            "endTime": "2025-09-25T09:38:26.5976414",
            "taskId": "bee86e38-2595-4de8-9df0-5d44c5d2e7e5",
            "oldFileName": "1758683907362.pdf"
        },
        {
            "differenceCount": 0,
            "newFileName": "484248753-Employment-Agreement-docx.pdf",
            "resultUrl": "/api/compare-pro/canvas-result/5b39fa5e-fd2d-44e2-b14d-e888c21ed17d",
            "startTime": null,
            "endTime": null,
            "taskId": "5b39fa5e-fd2d-44e2-b14d-e888c21ed17d",
            "oldFileName": "484248753-Employment-Agreement-docx (1).pdf"
        }
    ]
}
```

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

**cURL 示例**
```bash
curl -X DELETE "https://your-domain.com/api/compare-pro/task/task_20231215_001"
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

## 📥 接口7: 导出比对报告

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
| `userModifications` | object | ❌ | 用户修改信息（忽略项和备注） |
| `userModifications.ignoredDifferences` | number[] | ❌ | 已忽略的差异项索引数组 |
| `userModifications.remarks` | object | ❌ | 备注信息，键为差异项索引，值为备注内容 |

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
```json
{
    "taskId": "task_20231215_001",
    "formats": ["doc"],
    "includeIgnored": true,
    "includeRemarks": true,
    "userModifications": {
        "ignoredDifferences": [2, 5, 8],
        "remarks": {
            "0": "此差异已确认，需要保留",
            "3": "与客户沟通后确认为正常修改"
        }
    }
}
```

**cURL 示例**
```bash
curl -X POST "https://your-domain.com/api/compare-pro/export-report" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "task_20231215_001",
    "formats": ["doc"],
    "includeIgnored": false,
    "includeRemarks": true
  }' \
  --output compare_report.doc
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

