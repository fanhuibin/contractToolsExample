# 肇新合同管理系统 - API设计规范

## 📋 目录

- [1. 概述](#1-概述)
- [2. RESTful规范](#2-restful规范)
- [3. 统一响应格式](#3-统一响应格式)
- [4. 状态码规范](#4-状态码规范)
- [5. 请求规范](#5-请求规范)
- [6. 分页规范](#6-分页规范)
- [7. 错误处理](#7-错误处理)
- [8. 最佳实践](#8-最佳实践)

---

## 1. 概述

本文档定义了肇新合同管理系统的API设计规范，旨在：
- 统一前后端交互格式
- 提高API可维护性和可扩展性
- 符合大厂API设计标准
- 提升开发效率和用户体验

### 1.1 设计原则

- **一致性**：所有API遵循统一的格式和规范
- **RESTful**：遵循REST架构风格
- **语义化**：URL和参数命名清晰、易理解
- **版本化**：支持API版本控制
- **文档化**：所有API都有完整的文档

---

## 2. RESTful规范

### 2.1 HTTP方法

| HTTP方法 | 语义 | 示例 |
|---------|------|------|
| `GET` | 获取资源 | `GET /api/templates/{id}` |
| `POST` | 创建资源 | `POST /api/templates` |
| `PUT` | 完整更新资源 | `PUT /api/templates/{id}` |
| `PATCH` | 部分更新资源 | `PATCH /api/templates/{id}` |
| `DELETE` | 删除资源 | `DELETE /api/templates/{id}` |

### 2.2 URL设计

#### 基本规则
- 使用名词复数形式：`/api/templates` 而不是 `/api/template`
- 使用小写字母和连字符：`/api/rule-extract` 而不是 `/api/RuleExtract`
- 避免URL过深：建议不超过3层

#### 资源命名示例

```
# 智能文档抽取
GET    /api/rule-extract/templates          # 获取模板列表
POST   /api/rule-extract/templates          # 创建模板
GET    /api/rule-extract/templates/{id}     # 获取模板详情
PUT    /api/rule-extract/templates/{id}     # 更新模板
DELETE /api/rule-extract/templates/{id}     # 删除模板

# 智能文档比对
POST   /api/compare-pro/submit-url           # 创建比对任务（URL方式）
GET    /api/compare-pro/task/{taskId}        # 获取任务状态
GET    /api/compare-pro/canvas-result/{taskId}  # 获取比对结果
POST   /api/compare-pro/export-report        # 导出比对报告
```

### 2.3 主要模块API路径

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 智能文档抽取 | `/api/rule-extract` | 规则提取功能 |
| 智能文档比对 | `/api/compare-pro` | GPU OCR比对 |
| 智能合同合成 | `/api/compose` | 合同合成功能 |
| 智能文档解析 | `/api/ocr` | OCR文本提取 |
| AI合同抽取 | `/api/ai/contract` | AI智能抽取 |
| 文档在线编辑 | `/api/onlyoffice` | OnlyOffice集成 |
| 文档格式转换 | `/api/convert` | 格式转换 |

---

## 3. 统一响应格式

### 3.1 响应结构

所有API返回统一格式的JSON对象：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | Integer | ✅ | 业务状态码（200表示成功） |
| `message` | String | ✅ | 响应消息 |
| `data` | Object | ❌ | 业务数据（成功时返回） |
| `timestamp` | String | ✅ | 响应时间戳 |
| `metadata` | Object | ❌ | 额外元数据（如分页信息） |

### 3.2 成功响应示例

#### 简单数据
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "123",
    "name": "合同模板A"
  },
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 列表数据
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {"id": "1", "name": "模板A"},
    {"id": "2", "name": "模板B"}
  ],
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 分页数据
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {"id": "1", "name": "模板A"},
      {"id": "2", "name": "模板B"}
    ],
    "current": 1,
    "size": 10,
    "total": 100,
    "pages": 10,
    "hasPrevious": false,
    "hasNext": true
  },
  "timestamp": "2025-01-18T10:30:00"
}
```

### 3.3 失败响应示例

#### 参数错误
```json
{
  "code": 400,
  "message": "参数错误: 模板ID不能为空",
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 业务错误
```json
{
  "code": 11001,
  "message": "抽取模板不存在",
  "timestamp": "2025-01-18T10:30:00"
}
```

#### 服务器错误
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "timestamp": "2025-01-18T10:30:00"
}
```

---

## 4. 状态码规范

### 4.1 HTTP状态码

使用标准HTTP状态码：

| 状态码 | 说明 | 使用场景 |
|-------|------|---------|
| `200` | 成功 | 请求成功处理 |
| `201` | 已创建 | 资源创建成功 |
| `204` | 无内容 | 删除成功（无返回内容） |
| `400` | 参数错误 | 请求参数错误 |
| `401` | 未认证 | 未登录或Token无效 |
| `403` | 禁止访问 | 无权限或未授权 |
| `404` | 未找到 | 资源不存在 |
| `429` | 请求过多 | 请求频率超限 |
| `500` | 服务器错误 | 服务器内部错误 |
| `503` | 服务不可用 | 服务暂时不可用 |

### 4.2 业务状态码

业务状态码（10000+）用于细分业务错误：

| 范围 | 模块 | 示例 |
|------|------|------|
| `11000-11999` | 智能文档抽取 | `11001`: 模板不存在 |
| `12000-12999` | 智能文档比对 | `12001`: 比对任务不存在 |
| `13000-13999` | 智能合同合成 | `13001`: 合成模板不存在 |
| `14000-14999` | 智能文档解析 | `14001`: 文档解析失败 |
| `15000-15999` | 文档在线编辑 | `15001`: OnlyOffice服务不可用 |
| `16000-16999` | 文档格式转换 | `16001`: 格式转换失败 |
| `17000-17999` | 文件上传/下载 | `17001`: 文件为空 |
| `18000-18999` | 授权相关 | `18001`: License无效 |

完整错误码请参考：`ApiCode.java`

---

## 5. 请求规范

### 5.1 请求头

#### 必需请求头
- `Content-Type: application/json` - JSON请求
- `Content-Type: multipart/form-data` - 文件上传

### 5.2 请求体

#### JSON格式
```json
POST /api/rule-extract/extract

{
  "templateId": "template-001",
  "fileId": "file-123",
  "options": {
    "ignoreHeaderFooter": true,
    "headerHeightPercent": 6.0
  }
}
```

#### 文件上传（multipart/form-data）
```
POST /api/ocr/extract/upload

Content-Type: multipart/form-data

file=@document.pdf
ignoreHeaderFooter=true
headerHeightPercent=6.0
```

### 5.3 查询参数

#### 单一资源
```
GET /api/templates/123
```

#### 列表查询
```
GET /api/templates?keyword=合同&status=active
```

#### 分页查询
```
GET /api/templates?current=1&size=10&sortField=createTime&sortOrder=DESC
```

---

## 6. 分页规范

### 6.1 请求参数

使用统一的分页参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `current` | Long | ✅ | 1 | 当前页码（从1开始） |
| `size` | Long | ✅ | 10 | 每页大小（最大100） |
| `sortField` | String | ❌ | - | 排序字段 |
| `sortOrder` | String | ❌ | DESC | 排序方向（ASC/DESC） |
| `keyword` | String | ❌ | - | 搜索关键词 |

### 6.2 响应数据

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [ ],        // 数据列表
    "current": 1,          // 当前页码
    "size": 10,            // 每页大小
    "total": 100,          // 总记录数
    "pages": 10,           // 总页数
    "hasPrevious": false,  // 是否有上一页
    "hasNext": true        // 是否有下一页
  }
}
```

### 6.3 后端实现示例

```java
@GetMapping("/templates")
public ApiResponse<PageData<Template>> listTemplates(PageQuery query) {
    Page<Template> page = templateService.page(query.toPage());
    return ApiResponse.success(PageData.from(page));
}
```

---

## 7. 错误处理

### 7.1 异常分类

| 异常类型 | HTTP状态码 | 业务状态码 | 处理方式 |
|---------|-----------|-----------|---------|
| 参数校验失败 | 400 | 400 | 返回具体校验错误 |
| 业务逻辑错误 | 400 | 10000+ | 返回业务错误信息 |
| 未认证 | 401 | 401 | 跳转登录页 |
| 无权限 | 403 | 403 | 提示权限不足 |
| 资源不存在 | 404 | 404 | 提示资源不存在 |
| 服务器错误 | 500 | 500 | 记录日志+通用提示 |

### 7.2 业务异常抛出

```java
// 方式1：使用预定义的错误码
throw BusinessException.of(ApiCode.TEMPLATE_NOT_FOUND);

// 方式2：自定义错误消息
throw BusinessException.of(ApiCode.TEMPLATE_NOT_FOUND, "模板ID: " + templateId);

// 方式3：快捷方法
throw BusinessException.templateNotFound(templateId);
```

### 7.3 前端错误处理

前端会根据错误码自动显示相应图标和提示：

- `18000-18999`：🔐 授权相关错误
- `17000-17999`：📁 文件相关错误
- `10000-16999`：❌ 其他业务错误
- `500+`：❌ 服务器错误

---

## 8. 最佳实践

### 8.1 Controller示例

```java
@RestController
@RequestMapping("/api/templates")
@RequireFeature(module = ModuleType.SMART_DOCUMENT_EXTRACTION)
@Api(tags = "模板管理")
public class TemplateController {
    
    /**
     * 分页查询模板列表
     */
    @GetMapping
    @ApiOperation("查询模板列表")
    public ApiResponse<PageData<Template>> listTemplates(
            @Valid PageQuery query,
            @RequestParam(required = false) String keyword) {
        
        Page<Template> page = templateService.page(query.toPage());
        return ApiResponse.success(PageData.from(page));
    }
    
    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @ApiOperation("获取模板详情")
    public ApiResponse<Template> getTemplate(@PathVariable String id) {
        Template template = templateService.getById(id);
        if (template == null) {
            throw BusinessException.templateNotFound(id);
        }
        return ApiResponse.success(template);
    }
    
    /**
     * 创建模板
     */
    @PostMapping
    @ApiOperation("创建模板")
    public ApiResponse<Template> createTemplate(@Valid @RequestBody TemplateDTO dto) {
        Template template = templateService.create(dto);
        return ApiResponse.success("创建成功", template);
    }
    
    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    @ApiOperation("更新模板")
    public ApiResponse<Template> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody TemplateDTO dto) {
        
        Template template = templateService.update(id, dto);
        return ApiResponse.success("更新成功", template);
    }
    
    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除模板")
    public ApiResponse<Void> deleteTemplate(@PathVariable String id) {
        templateService.removeById(id);
        return ApiResponse.success("删除成功");
    }
}
```

### 8.2 前端调用示例

```typescript
// API定义
export const getTemplateList = (params: PageQuery) => {
  return request({
    url: '/templates',
    method: 'get',
    params
  })
}

// 组件中使用
const { data } = await getTemplateList({
  current: 1,
  size: 10,
  sortField: 'createTime',
  sortOrder: 'DESC'
})

// data结构
{
  records: [ ],      // 数据列表
  current: 1,        // 当前页
  size: 10,          // 每页大小
  total: 100,        // 总数
  pages: 10,         // 总页数
  hasPrevious: false,
  hasNext: true
}
```
