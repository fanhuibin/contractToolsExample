# 合同履约任务智能识别系统

## 版本历史

### 2025-08-07 合同履约任务功能重构

#### 功能概述
本次重构旨在开发一个基于AI的合同履约任务智能识别系统，帮助用户从合同文本中自动提取和管理履约任务。

#### 关键特性
1. **模板化管理**
   - 支持系统预置和用户自定义模板
   - 可配置任务类型、关键词和时间规则
   - 支持模板复制、编辑和设置默认模板

2. **AI智能识别**
   - 使用通义千问Long模型进行合同文本分析
   - 动态生成结构化的履约任务
   - 支持多种合同类型和任务类型识别

3. **前端交互优化**
   - 层级化任务类型选择
   - 动态关键词过滤
   - 结果表格导出
   - 历史记录管理

#### 技术实现

##### 前端技术栈
- Vue 3
- TypeScript
- Element Plus
- ExcelJS（表格导出）

##### 后端技术栈
- Spring Boot
- MyBatis-Plus
- 通义千问Long AI模型
- Hutool工具库

#### 数据库设计
```sql
CREATE TABLE `ai_fulfillment_template` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `contract_type` VARCHAR(100) NOT NULL,
  `task_types` JSON,
  `keywords` JSON,
  `time_rules` JSON,
  `type` ENUM('system', 'user') DEFAULT 'user',
  `is_default` BOOLEAN DEFAULT FALSE,
  `user_id` VARCHAR(50),
  `create_time` DATETIME,
  `update_time` DATETIME
);
```

#### 重构详细记录

##### 前端重构
1. 创建 `FulfillmentTask.vue` 页面
2. 实现模板选择和自定义逻辑
3. 添加动态关键词过滤
4. 集成 ExcelJS 实现表格导出
5. 优化 API 调用和类型定义

##### 后端重构
1. 创建 `FulfillmentTemplate` 实体类
2. 实现模板管理服务和控制器
3. 开发 `FulfillmentAiService` 对接通义千问模型
4. 完善错误处理和日志记录

#### 配置说明
```yaml
# application.yml
ai:
  fulfillment:
    prompt:
      template: # 自定义AI提示词模板
    model: qwen-long
```

#### 未来优化方向
1. 支持更多AI模型
2. 优化AI识别准确率
3. 增加更多模板管理功能
4. 支持多语言识别

#### 已知问题和限制
- AI识别准确率受合同文本复杂度影响
- 目前仅支持中文合同识别
- 大文件可能导致识别性能下降

## 贡献指南
1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交代码变更 (`git commit -m '添加了令人惊叹的功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 许可证
本项目采用 MIT 许可证。详见 `LICENSE` 文件。

## 联系方式
项目负责人：[您的名字]
电子邮件：[您的邮箱]

## 变更记录

### 2025-08-15 OCR 比对页码与坐标修复与联动
#### 变更记录
- `backend/src/main/java/com/zhaoxinms/contract/tools/compare/result/Position.java`：
  + 新增构造函数 `Position(float x, float y, float pageWidth, float pageHeight, int page)`，支持直接注入坐标与页尺寸（用于OCR坐标换算）。
- `backend/src/main/java/com/zhaoxinms/contract/tools/ocrcompare/compare/OCRCompareService.java`：
  + 解析 OCR JSON 的 `pages[].items[].box`（四点）为外接矩形；携带 `image_width/image_height`。
  + 新增从 OCR 像素坐标到 PDF 坐标（pt）的换算，结合 PDF 实际 `MediaBox` 宽高，统一使用 TextPosition 风格的 `yDirAdj`（自上而下）。
  + 为差异生成 `oldPosition/newPosition`（含 `page/x/y/pageWidth/pageHeight`），用于前端左右联动。
  + PDF 标注改为依据 `Position` 的页码与坐标定位，添加 `QuadPoints`，分别以不同颜色高亮新增/删除。
- 前端无需改动 API，仅消费 `differences[].oldPosition/newPosition` 实现页内滚动对齐与左右联动。

#### 影响与收益
- 修复 OCR 比对结果中页码与定位错误问题；PDF 高亮位置与前端预览联动一致。
- 非 OCR 比对保持原有行为不变。

#### 技术栈
- 后端：Spring Boot、PDFBox、Jackson、Hutool
- 前端：Vue3、TypeScript、Element Plus、mozilla/pdf.js viewer

#### 相关文件
- 后端：`Position.java`、`OCRCompareService.java`
- 前端：`frontend/src/views/documents/OCRCompareResult.vue`（消费新字段）

### 2025-08-08 合同抽取字段规则库（新增扩展文件，未改代码）
- 新增外置规则文件目录：`sdk/src/main/resources/contract-extract-rules/`
  - `lease.json`、`purchase.json`、`labor.json`、`construction.json`、`technical.json`、`intellectual.json`、`operation.json`
- 作用：为各合同类型提供字段级约束与规范化策略（类型、必填、正则清洗、日期/金额/百分比等），示例：
  - “合同名称”自动去除“合同/合同书”后缀，并禁止包含这些词；
  - 金额/比例字段仅保留数字与小数点；
  - 日期字段解析多种格式并统一输出为 `yyyy-MM-dd`。
- 说明：当前仅新增扩展文件，不影响现有流程；后续可在 SDK 的提示构建与结果后处理阶段按需加载应用。

### 2025-08-08 合同抽取规则执行集成与“合同名称”策略调整
- 修改：在 SDK 抽取流程中集成外置规则库执行（normalize + validate），路径：`sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/impl/ContractExtractServiceImpl.java`（抽取完成后按模板 `contractType` 加载并执行规则）
- 新增：规则模型与服务
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/ContractRules.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/FieldRule.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/RuleViolation.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RuleLoaderService.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RuleEngineService.java`
- 配置：`sdk/src/main/resources/application-ai.yml` 新增 `zxcm.ai.contract.enforce-rules: true`
- 规则：移除“合同名称”的 `denyContains`，仅保留后缀清洗 `(合同|合同书)$`
  - 影响文件：`lease.json`、`purchase.json`、`labor.json`、`construction.json`、`technical.json`、`intellectual.json`、`operation.json`
- 效果：
  - “合同名称”中部的“合同”保留，末尾“合同/合同书”被去除。
  - 示例：输入“肇新合同管理系统源码销售合同”→ 输出“肇新合同管理系统源码销售”。

### 2025-08-10 规则与模板绑定（templateId 唯一）与前后端改造
- 数据库
  - 新增迁移：`backend/src/main/resources/db/migration/V4__alter_contract_rule_to_template.sql`
    - 为每个 `contract_rule` 赋值 `template_id`（优先默认模板 → 最小ID模板 → 无则自动创建系统迁移模板）
    - 删除 `contract_rule.contract_type` 唯一约束，新增 `UNIQUE(template_id)`
- 后端
  - 模型：`ContractRule` 新增 `templateId` 字段（唯一），保留 `contractType` 兼容
  - 服务：`RuleStoreService` 新增 `readRuleByTemplateId`、`saveRuleByTemplateId`、`upsertFieldsByTemplateId`
  - 控制器：`RuleAdminController` 新增
    - `GET /api/ai/rules/template/{templateId}`
    - `PUT /api/ai/rules/template/{templateId}`
  - 模板服务：`ContractExtractTemplateServiceImpl`
    - create/copy/update 时初始化/复制/补新增规则（不强删旧）
  - 抽取服务：`ContractExtractServiceImpl` 构建 prompt 时优先按 `templateId` 加载规则并合并
- 前端
  - API：`frontend/src/api/ai/rules.ts` 新增 `readRuleByTemplateId`、`saveRuleByTemplateId`
  - 规则设置页：`RuleSettings.vue` 改为按路由 `templateId` 加载模板与规则并保存
  - 模板选择：`ContractExtractor.vue` 新增“编辑该模板规则”跳转；`ContractExtract.vue` 的“提取规则设置”按钮带上 `templateId`

### 2025-08-08 规则与模板调整（去除通用类型并合并）
- 删除：`sdk/src/main/resources/contract-extract-rules/common.json`
- 合并：将原“通用”字段（编号、名称、甲乙方、地址、联系人、签署与起止日期、违约/争议/法律等）并入以下规则文件的 `fields`：
  - `labor.json`、`construction.json`、`technical.json`、`intellectual.json`、`operation.json`
- SDK 系统模板初始化调整：移除“通用合同模板”，并在 `ContractExtractTemplateServiceImpl.initSystemTemplates()` 中将通用字段合并进上述五类模板字段集合；新增 `mergeUnique()` 保证字段去重。

### 2025-08-08 SDK 上传大小限制调整（配置变更）
- `sdk/src/main/resources/application.yml`：新增 `spring.servlet.multipart` 配置，将上传上限设置为 `100MB`：
  - `spring.servlet.multipart.max-file-size: 100MB`
  - `spring.servlet.multipart.max-request-size: 100MB`
  - `spring.servlet.multipart.enabled: true`
  解决由于默认 1MB 导致的 `MaxUploadSizeExceededException`。

### 2025-08-10 移除“合同履约任务”全部代码（前后端）
- 追加：新增“自动履约任务”（完全按“合同抽取”供能实现）
  - 数据库：`backend/src/main/resources/db/migration/V5__create_auto_fulfillment_tables.sql`
    - `auto_fulfillment_template`（字段对齐 `contract_extract_template`）
    - `auto_fulfillment_history`（字段对齐 `contract_extract_history`）
  - 后端（SDK）：
    - 控制器：`AutoFulfillmentController`（/api/ai/auto-fulfillment）、`AutoFulfillmentTemplateController`、`AutoFulfillmentHistoryController`
    - 服务：`AutoFulfillmentServiceImpl`、`AutoFulfillmentTemplateServiceImpl`、`AutoFulfillmentHistoryServiceImpl`
    - 实体/Mapper：`AutoFulfillmentTemplate`、`AutoFulfillmentHistory`、`AutoFulfillmentTemplateMapper`、`AutoFulfillmentHistoryMapper`
  - 前端：
    - API：`frontend/src/api/ai/auto-fulfillment.ts` 并在 `api/ai/index.ts` 下通过 `aiAutoFulfillment` 暴露
    - 页面：`frontend/src/views/contracts/AutoFulfillment.vue`
    - 路由：`/auto-fulfillment`
    - 菜单：`frontend/src/layout/index.vue` 新增“自动履约任务”

### 2025-08-10 SDK 编译错误修复（字典与 Mapper 拆分）
- 修复：`The public type AutoFulfillmentTaskTypeMapper must be defined in its own file`
  - 新增：`sdk/src/main/java/.../mapper/AutoFulfillmentTaskTypeMapper.java`
  - 删除：`AutoFulfillmentDictMapper.java`（重复定义）
  - 拆分模型：`AutoFulfillmentTaskType.java`、`AutoFulfillmentKeyword.java`
- 新增字典迁移与接口（去硬编码，改为ID驱动）
  - 迁移：`V6__auto_fulfillment_dicts.sql`（任务类型/关键词/关联表）
  - 控制器：`AutoFulfillmentDictController` 提供 `/dicts/task-types` 与 `/dicts/keywords`
- 前端
  - 删除：`frontend/src/views/contracts/FulfillmentTask.vue`
  - 删除：`frontend/src/api/ai/fulfillment.ts`
  - 调整：`frontend/src/layout/index.vue` 移除侧边栏“合同履约任务”菜单
  - 调整：`frontend/src/router/index.ts` 移除 `/fulfillment` 路由
- 后端（SDK）
  - 删除控制器：`FulfillmentTaskController`、`FulfillmentTemplateController`
  - 删除服务/实现：`FulfillmentAiService`、`FulfillmentTaskService`、`FulfillmentTemplateService`、`FulfillmentTemplateServiceImpl`
  - 删除模型：`FulfillmentTemplate`、`FulfillmentExtractResult`、`FulfillmentConfig`
  - 删除持久层：`FulfillmentTemplateMapper` 及其 XML：`backend/src/main/java/.../mapper/xml/FulfillmentTemplateMapper.xml`
- 影响
  - 所有以 `/api/fulfillment/**`、`/api/fulfillment/template/**` 为前缀的接口已不可用
  - 前端不再展示“合同履约任务”入口

### 2025-08-10 Flyway V4 迁移修复（先新增列再更新）
- 修改：`backend/src/main/resources/db/migration/V4__alter_contract_rule_to_template.sql`
  - 在执行任何基于 `template_id` 的 `UPDATE` 之前，新增列存在性检查；若缺失则执行：`ALTER TABLE contract_rule ADD COLUMN template_id BIGINT NULL`
  - 保持后续默认模板映射、兜底映射、删除 `contract_type` 唯一约束、添加 `UNIQUE(template_id)` 的逻辑不变
- 影响：修复启动时 Flyway 报错 “Unknown column 'cr.template_id' in 'where clause'”，确保迁移可重复执行
- 操作建议：重启应用触发 Flyway 自动迁移；或在数据库中单独执行该脚本后再启动