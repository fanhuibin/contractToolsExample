# 肇新合同工具集开发记录

## 当前任务清单

### 已完成任务
- [x] 扫描后端合同合成相关代码与调用链（Merge/ContentControlMerge）
- [x] 定位模板设计记录表template_design_record的实体/DAO/服务与迁移脚本
- [x] 查找后端现有控制器/接口是否已支持合成或文件预览下载
- [x] 扫描前端模板设计与文档预览模块，确认可复用组件与API
- [x] 制定合同合成功能的技术方案与接口设计（后端/前端）
- [x] 实现后端合同合成API（读取模板、按tag填充、输出URL）
- [x] 实现前端合同合成页面（左预览右表单、提交合成、展示结果）
- [x] 联调OnlyOffice/PDF预览并完善文件下载预览链路

### 进行中任务
无

### 待开始任务
无

## 最新更新

### 2025-08-25 Dots.OCR 接入（Java 工具类）
- 主要目的：接入开源 OCR 库 Dots.OCR（OpenAI 兼容接口），提供 Java 工具类统一调用
- 技术栈：Spring Boot（AutoConfiguration）、OkHttp、Jackson
- 修改文件：
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrClient.java`（新增）
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrProperties.java`（新增）
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrAutoConfiguration.java`（新增）
- 使用：
  ```yaml
  zxcm:
    dotsocr:
      enabled: true
      base-url: http://localhost:8000
      model: dots.ocr
      timeout-seconds: 60
      # api-key: xxx
  ```
  ```java
  @Autowired DotsOcrClient client;
  boolean ok = client.health();
  var models = client.listModels();
  String text = client.ocrImageByUrl("data:image/png;base64,...", "Extract all text", null, true);
  ```
  测试用例：
  - `sdk/src/test/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrQuickTest.java`
    - `testHealthAndModels` 默认启用
    - `testOcrLocalImage` 需手动启用（准备本地图片）
  参考：`https://www.dotsocr.net/blog/2`，`https://github.com/rednote-hilab/dots.ocr`

### 2025-01-14 合同合成功能完成
- **主要目的**: 实现合同合成功能，支持基于SDT标签的模板填充
- **关键方案**: 
  - 统一使用SDT的tag作为占位符
  - 后端合成后返回文件ID用于预览
  - 前端支持富文本表格插入（销售产品清单）
- **技术栈**: 
  - 后端：Spring Boot + ContentControlMerge
  - 前端：Vue 3 + Element Plus + PDF.js
- **修改文件**:
  - `backend/src/main/java/com/zhaoxinms/contract/tools/merge/ComposeController.java` - 新增合成控制器
  - `backend/src/main/java/com/zhaoxinms/contract/tools/common/service/FileInfoService.java` - 扩展文件服务
  - `sdk/src/main/java/com/zhaoxinms/contract/template/sdk/service/impl/FileInfoServiceImpl.java` - 实现文件注册
  - `frontend/src/api/contract-compose.ts` - 新增合成API
  - `frontend/src/views/compose/ContractCompose.vue` - 新增合成页面
  - `frontend/src/router/index.ts` - 新增路由
  - `frontend/src/layout/index.vue` - 新增菜单项

## 功能特性

### 合同合成
- 支持基于SDT标签的模板填充
- 富文本字段支持HTML表格（销售产品清单）
- 合成后返回文件ID用于预览
- 集成OnlyOffice/PDF预览功能

### 默认模板
- 使用demo模板（fileId: 9999）
- 包含公司名称、合同日期、金额等字段
- 支持富文本产品清单和条款条件

## 使用说明

### 访问路径
- 前端页面: `/contract-compose`
- 提交接口: `POST /api/compose/sdt`
- 预览: `/pdfviewer/web/viewer.html?file=/api/file/download/{fileId}`

### 合成流程
1. 选择模板（默认使用demo模板）
2. 填写表单字段值
3. 富文本字段可插入HTML表格
4. 点击合成按钮
5. 预览合成结果

## 技术架构

### 后端架构
```
com.zhaoxinms.contract.tools.merge
├── Merge.java (接口)
├── ContentControlMerge.java (实现)
└── ComposeController.java (控制器)
```

### 前端架构
```
views/
├── contracts/ (合同相关)
├── templates/ (模板设计)
├── onlyoffice/ (文档预览)
└── compose/ (合同合成) - 新增
```

## 注意事项
- 合成功能基于SDT标签，确保模板设计时正确设置tag
- 富文本字段支持HTML，可插入表格、样式等
- 合成后文件自动注册到文件服务，返回fileId用于预览