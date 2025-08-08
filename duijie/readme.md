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