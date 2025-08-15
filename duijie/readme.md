# 合同履约任务智能识别系统

## 版本历史

### 2023-11-15 审核清单管理页面UI美化

本次更新对审核清单管理页面进行了全面的UI美化，添加了悬停效果和视觉增强，提升了用户体验。详细更新内容请查看 [审核清单管理页面UI美化更新日志](./ui-update-log-risklibrary.md)。

### 2023-07-15 合同智能审核页面UI美化

本次更新对合同智能审核页面进行了全面的UI美化，提升了用户体验和视觉效果。详细更新内容请查看 [UI美化更新日志](./ui-update-log.md)。

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

### 2025-08-12 强制 AI 分支与耗时分析
- 变更记录
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/controller/RiskLibraryController.java`：`/ai/review-lib/review/execute` 仅接受 `multipart/form-data`，`file` 必填；删除“轻量 DB 映射”分支，强制走 AI。
  - `frontend/src/api/ai/risk.ts`：`executeReview` 强制以 `multipart/form-data` 提交并必须携带文件；`pointIds` 以 JSON Blob 传入。
- 性能观测（按后台日志）
  - DB 读取与清单构建：若干 `SELECT ...`，总计 < 1s。
  - DashScope 文件上传：约 1.3s（`upload ok: fileId=... costMs=1322`）。
  - 模型对话生成：约 294s（`chat done ... costMs=293857`）。
  - 结论：耗时主要集中在模型生成阶段（`qwen-long`，提示约 15k 字符，选中点位对应 68 条 prompt，输出约 19k 字符）。
- 优化建议（不引入本地算法，保持单次大提示思路）
  - 在提示中限制输出体量：
    - `message` 控制在 50-80 字，禁止赘述与重复定义；
    - 非命中项仅返回 `decisionType/statusType`，不生成 `message/evidence`；
    - `evidence` 单段 ≤120 字，最多 3 段；
    - `actions` 仅返回命中 `actionId` 与 `actionType`，不重复输出 `action_message` 文案；
  - 若业务可接受，切换更快模型（如 `qwen-plus`），或为 `qwen-long` 裁剪冗余提示说明；
  - 若 API 支持，设置最大生成长度（max_tokens）/严格 JSON 输出以避免过度生成；
  - 增加阶段耗时日志：prompt 构建、上传、聊天开始/结束、解析 JSON 各自耗时，便于持续定位性能瓶颈。

### 2025-08-12 智能审核方案对比（行业 vs 当前）
### 2025-08-12 风险库V12全量种子导入（98条）
- 变更记录
  - 新增 `sdk/src/main/resources/db/migration/V12__seed_risk_review_full.sql`：基于“原创化且扩充为98条”的清单，按 `pointCode` 全量写入 `review_point`/`review_prompt`/`review_action` 与 `review_profile_item`，并保持与 `orderList` 一致顺序；所有编号（如 `3437`）未变。
  - 设计为幂等：
    - 先确保 `review_clause_type` 与各 `review_point` 存在；
    - 删除目标点位下旧 `prompt/action` 再重建，避免重复；
    - `review_profile`/`review_profile_item` 使用 `ON DUPLICATE KEY UPDATE` 维护顺序。
- 使用方式
  1. 启动或重启后端，Flyway 将自动执行 V12；
  2. 前端 `风险库/审核清单` 页面即可看到新顺序与内容；
  3. 若数据库已存在同名点位/提示，将被以新版内容覆盖到最新状态。

- 行业常见实现
  - 规则引擎/正则库：高可控、低误报；难覆盖语义、维护成本高。
  - RAG（分段检索+LLM）：先切分与索引，按风险点检索相关段再让 LLM 判定，速度更快、可解释性更好。
  - 多阶段流水线：结构化解析 → 局部判定 → 汇总与建议，稳定性更高、可灰度回退。
  - 专用分类器/微调模型：对“是否存在某条款/是否合规”做分类，再由 LLM 生成人话提示。
- 本项目当前实现
  - 单次大提示 + Qwen（fileid）+ 数据驱动 Prompt（DB 中 prompts/actions），后端不做算法。
- 差异与权衡
  - 你的方式：上线快、改配置即可扩展，但时延较高、稳定性/一致性依赖提示设计与模型负载。
  - 行业主流：RAG/多阶段更常见，吞吐与可控性更优，但工程复杂度高。
  - 若保持“无后端算法”原则，可引入轻量“输入裁剪”和“输出约束”来接近 RAG 的收益，而不改变核心理念。

### 2025-08-12 功能增强建议（对标当前实现）
- 体验与可观测性
  - 服务端流式转发与进度上报（上传/构造提示/模型推理/解析各阶段耗时与状态）。
  - 失败可重试与“继续上次任务”（基于 `traceId` 与文件 `hash`）。
  - 模型动态选择：短文 `qwen-plus`，长文自动切 `qwen-long`。
- 输出收敛与速度优化（保持“无后端算法”）
  - 严格 JSON schema 校验与自动纠偏（有限状态机式补全，拒绝非 JSON 文本）。
  - 提示中限制非命中项仅返回 `decisionType/statusType`；限制 `message/evidence/actions` 长度与数量。
- 审核历史与审计
  - 新增：`review_job`、`review_result`、`review_evidence`、`review_action_record` 表；落库完整 JSON 与索引字段（pointId、decisionType）。
  - 审计日志：谁在何时用哪个方案审核了哪个文件；版本与回溯。
- 风险库治理
  - 版本/草稿-发布/生效时间窗；启停与灰度；标签与权重；动作库复用与分组。
  - Prompt 参数化（变量占位），批量回滚与快速对比测试（A/B）。
- 权限与租户
  - 角色与数据域隔离（库与模板按组织/项目划分）。
- 文档与证据定位
  - 与 ONLYOFFICE 的锚点联动：段落/字符偏移双向定位与高亮，支持多段证据。
  - 扫描件支持：OCR（docTR/Tesseract）+ 坐标映射存储。
- 运维与成本
  - Token 统计与费用看板；超长文档分片策略白名单（仅在业务允许时）。
  - 速率/并发控制与队列优先级；熔断与降级（只返回命中摘要）。

### 2025-08-12 清单管理页改进（返回主页与树同步）
### 2025-08-12 风险库树结构调整（提示为叶子，联动真实SQL表）
### 2025-08-12 风险库中文改写（V13 迁移）
- 目的：将风险库的中文描述统一为更规范的表达（非英文），保持前后端联动一致；不再在前端进行临时替换。
- 范围：
  - 分类名（`review_clause_type.clause_name`）：除“合同主体”外统一改写为“法律条款引用/价款与支付/履约安排/…”
  - 风险点（`review_point.point_name/algorithm_type`）：如“己方主体 → 内部相对方”、“对方主体 → 外部相对方”；若仅名称调整，`algorithm_type` 同步。
  - 提示（`review_prompt.prompt_key/name/message`）：口径统一为“确认/缺失/不清/不一致”等中性表述；3649/3702 等涉及“内部/外部相对方”已精确改写。
- 不改动：
  - 编码（`point_code`）保留（如 3649/3702），仅作幂等定位；前端已隐藏显示。
  - 状态/排序不变。
- 迁移文件：`sdk/src/main/resources/db/migration/V13__refine_risk_text_cn.sql`
- 回滚建议：如需恢复旧表述，追加 `V14__rollback_risk_text_cn.sql` 还原相同字段。
- 接口 JSON 变更（示例）
  - 旧 `/ai/review-lib/tree`（仅分类→点）
```json
[
  {
    "clauseType": { "id": 1, "clauseName": "合同主体", "enabled": true, "sortOrder": 10 },
    "points": [
      { "id": 101, "clauseTypeId": 1, "pointCode": "3649", "pointName": "己方主体名称规范性审查", "algorithmType": "...", "enabled": true, "sortOrder": 1 }
    ]
  }
]
```
  - 新增 `/ai/review-lib/tree-prompts`（分类→点→提示）
```json
[
  {
    "clauseType": { "id": 1, "clauseName": "合同主体", "enabled": true, "sortOrder": 10 },
    "points": [
      {
        "point": { "id": 101, "clauseTypeId": 1, "pointCode": "3649", "pointName": "己方主体名称规范性审查", "algorithmType": "...", "enabled": true, "sortOrder": 1 },
        "prompts": [
          { "id": 10001, "pointId": 101, "promptKey": "首部己方主体名称缺失", "name": "首部己方主体名称缺失", "statusType": "ERROR", "message": "...", "enabled": true, "sortOrder": 1 }
        ]
      }
    ]
  }
]
```
  - 预览与审核入参未变：仍以 `pointIds`（number[]）为请求体
```json
[101, 102, 103]
```
  - 前端树渲染内部节点（用于 el-tree）由“点为叶”改为“提示为叶”，并增加 `type` 与带前缀的 `id`（用于选择聚合）：
```json
{
  "id": "c-1", "type": "CLAUSE", "label": "合同主体", "children": [
    { "id": "p-101", "type": "POINT", "label": "己方主体名称规范性审查", "raw": {"id":101}, "children": [
      { "id": "r-10001", "type": "PROMPT", "label": "首部己方主体名称缺失（ERROR）", "raw": {"id":10001}, "parentPoint": {"id":101} }
    ]}
  ]
}
```
- 变更记录
  - 后端：新增树接口返回“分类→风险点→提示”三级结构（提示为叶子，来源 `review_prompt` 实表），保持与真实数据库一致。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RiskLibraryService.java`：新增 `TreeNodeV2`、`PointNode` DTO 与 `treeWithPrompts(Boolean enabled)` 方法签名。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/impl/RiskLibraryServiceImpl.java`：实现 `treeWithPrompts`，按 `enabled` 过滤并按 `sort_order,id` 排序，组装三级数据。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/controller/RiskLibraryController.java`：新增 `GET /api/ai/review-lib/tree-prompts`。
  - 前端：`RiskLibrary.vue` 树改为“提示（prompt）”为叶子；勾选提示后映射为所属 `pointId` 去重，再调用原有预览/审核接口；应用方案时按 `pointId` 勾选对应点下的全部提示叶。
    - `frontend/src/api/ai/risk.ts`：新增 `getTreePrompts(enabled?)`；`deleteProfile(id, force?)` 支持强删。
    - `frontend/src/views/contracts/RiskLibrary.vue`：
      - `loadTree()` 切换为 `getTreePrompts()`；`buildTree()` 生成 3 级节点（`CLAUSE/POINT/PROMPT`），叶子为 `PROMPT`。
      - `onPreviewSelection()`：从被勾选 `PROMPT` 节点收集所属 `pointId` 去重预览。
      - `applyProfile()`：将方案里的 `pointId` 映射成该点下的所有 `PROMPT` 节点 key（`r-<id>`）批量勾选。
      - 点击提示叶时同步右侧“提示与动作”列表基于父级风险点加载。
- 影响
  - 树的叶子与真实 `review_prompt` 表记录一致（而非固定 98 条，V12 仅为初始种子）。
  - 预览与执行审核仍按 `pointId` 粒度，无需后端执行链路改造。
  - `enabled=true` 时仅返回启用的分类/点/提示；“显示停用”开关生效后端联动。
- 变更记录
  - `frontend/src/views/contracts/RiskLibrary.vue`：
    - 在左侧卡片 header 增加“返回主页”按钮（`goHome()` → `router.push('/')`）。
    - 树数据保持与后端一致：继续使用 `riskApi.getTree(enabled?)` 从后端实时加载，`showDisabled` 控制是否仅看启用数据。
- 影响
  - 便于从清单管理页一键返回系统主页；
  - 树形结构严格以数据库为准，新增/启停/删除后通过 `loadTree()` 即时反映。

### 2025-08-12 供应商条款分析调研（思通数科）
- 信息来源
  - Gitee 开源仓库：`free-nlp-api`
  - 在线体验：`nlp.stonedt.com`
- 公开能力（概述）
  - NLP、OCR、图像识别、文本抽取，多格式文档处理（PDF/PPT/CSV/PNG/SVG）。
- 条款分析常见实现路径（推断性归纳，官方未公开内核细节）
  - 文档解析与分块：OCR/版面分析→段落/标题/列表切分。
  - 规则/词典匹配：关键词、正则、条款模板初筛。
  - 向量检索/RAG：条款库嵌入相似度召回相关片段后再判定。
  - 条款归类与证据：输出条款类型、命中文段、位置信息与置信度。
  - 结构化返回：用于搜索、推荐与审核提示。
  - 工程特点：规则+检索+模型组合，支持本地化部署与接口化集成。

### 2025-08-12 完全开源文档审核工具清单（可组合）
- RAG/工作流
  - Haystack（Apache-2.0）、LlamaIndex（MIT）、LangChain4j（Apache-2.0）、Spring AI（Apache-2.0）
- 文档解析/分块
  - Unstructured（Apache-2.0）、Apache Tika（Apache-2.0）、pdfminer.six（MIT）、PDFPlumber（MIT）
- 文本位置/坐标
  - PyMuPDF（AGPL-3.0，注意许可限制）、PDFBox（Apache-2.0）、pdfplumber 字符级框有限支持
- OCR
  - Tesseract（Apache-2.0）、PaddleOCR（Apache-2.0）、docTR（Apache-2.0）
- 向量库
  - Milvus、Qdrant、Weaviate、OpenSearch k-NN（均开源许可）
- 规则引擎
  - Drools（Apache-2.0）、Easy Rules（Apache-2.0）
- PII/敏感信息
  - Microsoft Presidio（MIT）
- 法律NLP/数据集
  - Blackstone（MIT）、LexNLP（GPL-3.0，注意许可）、CUAD 数据集/基线模型

### 2025-08-12 模型建议子系统：快速落地选型与路径
- 目标：用时短、正确率高、提示词可自编辑
- 选型（成熟度优先）
  - Dify（Apache-2.0）：可视化 Prompt 编辑、应用接口直调；支持 DashScope/Qwen；易于搭建与运维。
  - Flowise（Apache-2.0）：节点流式编排，Prompt 可视化；REST 推理接口；插件生态丰富。
  - LangChain4j / Spring AI（Apache-2.0）：在现有 Java 项目内直接集成，支持结构化输出（函数调用/JSON Schema）。
- 不引入平台的轻改路径（保持现有 Qwen 链路）
  - 启用严格 JSON 约束（response_format 或 tools/function 调用），减少解析失败与跑偏。
  - 提示词可视化与版本化：在 DB 增加 `review_prompt_example`（few-shot），前端提供编辑与回归测试入口。
  - 调参与模型：`temperature/top_p` 降低；短文用 `qwen-plus`；限制 message/evidence 长度与数量以控时。
- 预估工期
  - Dify/Flowise 对接：0.5–1 天（含容器部署与后端对接）。
  - LangChain4j/Spring AI：1–2 天（结构化输出改造 + DB 驱动 Prompt）。
  - 现有代码轻改：0.5 天（约束输出 + 参数化 + 少样本支持）。

### 2025-08-11 风险库页面树数据修复与种子脚本字段名修正（最小版功能说明）
- 变更记录
  - `frontend/src/views/contracts/RiskLibrary.vue`：将树接口调用从 `riskApi.tree(true)` 更正为 `riskApi.getTree(true)`，修复因方法名不匹配导致左侧树始终为空的问题。
  - `sdk/src/main/resources/db/migration/V10__seed_risk_review.sql`：将一处列名 `sortOrder` 更正为 `sort_order`（第315行），修复 Flyway 执行报错 `Unknown column 'sortOrder' in 'field list'`。
- 现状说明（MVP）
  - 风险库页面当前仅提供：搜索+可勾选树、点击节点查看详情、生成审核清单预览；未实现对条款分类/风险点/提示/动作的增删改与排序。
- 后续建议
  - 新增后台 CRUD 接口（分类/风险点/提示/动作）与前端增删改查表单、启停与拖拽排序、保存为方案等功能。

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

### 2025-08-10 自动履约模板分组与任务类型绑定改造
- 数据库（V7、V8）
  - `auto_fulfillment_template` 新增 `category_code`（从旧 `contract_type` 回填）、`task_type_id`；索引：`idx_template_category_code`、`idx_template_task_type_id`
  - `auto_fulfillment_task_type` 新增唯一编码 `code`
  - 新建映射表：`auto_fulfillment_task_type_keyword(task_type_id, keyword_id)` 主键二元组
  - 初始化五大父类与子项（含 code）：开票/付款/收款/到期提醒/事件触发；插入关键词并建立映射；为每个子项初始化系统模板
- 后端
  - 模型：`AutoFulfillmentTemplate` 用 `categoryCode` 替代 `contractType`，新增 `taskTypeId`；`AutoFulfillmentTaskType` 新增 `code`
  - 服务：`AutoFulfillmentTemplateService`/Impl 新增 `getTemplatesByCategory*`、`getAllCategories`；默认模板设置按 `categoryCode`
  - 控制器：`/api/ai/auto-fulfillment/template/contract-types` 返回五大分类；`/type/{categoryCode}` 按分类列出模板；识别接口解析 `taskTypes/keywords` 并参与提示
- 前端
  - `AutoFulfillment.vue` 分组过滤改为基于 `categoryCode`（向后兼容旧字段）
- 行为变化
  - “选择识别模板”分组切换为：开票履约/付款履约/收款履约/到期提醒/事件触发；每个子项拥有独立模板集合（模板与叶子任务类型一对一）
  - 识别时，所选任务类型与关键词将纳入提示构造，影响识别结果

### 2025-08-10 Flyway V4 迁移修复（先新增列再更新）
- 修改：`backend/src/main/resources/db/migration/V4__alter_contract_rule_to_template.sql`
  - 在执行任何基于 `template_id` 的 `UPDATE` 之前，新增列存在性检查；若缺失则执行：`ALTER TABLE contract_rule ADD COLUMN template_id BIGINT NULL`
  - 保持后续默认模板映射、兜底映射、删除 `contract_type` 唯一约束、添加 `UNIQUE(template_id)` 的逻辑不变
- 影响：修复启动时 Flyway 报错 “Unknown column 'cr.template_id' in 'where clause'”，确保迁移可重复执行
- 操作建议：重启应用触发 Flyway 自动迁移；或在数据库中单独执行该脚本后再启动