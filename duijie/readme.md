### 2025-08-16 智能审核 × OnlyOffice 风险定位联动 v1.0

## 2025-08-16 风险卡片与 OnlyOffice 书签联动完善

### 复用记录
- 复用前端已有的 `OnlyOfficeEditor` 方法通道（`setAnchors`/`gotoAnchor`/`clearAnchors`）
- 复用现有 `RiskCardPanel` 的 `goto` 事件对接编辑器定位

### 变更与实现
- 插件 `public/onlyoffice-plugins/risk-anchors/pluginCode.js`
  - 新增：使用 `window.Asc.plugin.callCommand` + `Api.GetDocument()` 创建/删除书签
  - 规则：为每个锚点生成前缀为 `risk_anchor_` 的书签名，优先依据 `paragraphIndex/startOffset/endOffset` 精确选区，缺失则回退 `GetRangeByText` 按文本搜索后加书签
  - 定位：优先 `GoToBookmark(bookmarkName)`，并保留原文本搜索兜底路径
  - 清理：批量遍历 `GetBookmarks()`，按前缀删除本插件创建的书签

### 验收标准
- 点击“开始审核”后，插件能够为返回的所有风险点创建对应书签；
- 在“风险审核”面板点击任意卡片，文档可跳转至对应位置；
- 再次审核或点击“清理锚点”（内部命令）后，旧书签被清理并重新创建；

### 注意事项
- 为保证精确定位，请尽量提供 `paragraphIndex / startOffset / endOffset`；若仅有 `text`，会按文本首次匹配定位；
- 书签命名使用安全字符集，锚点 ID 会被规范化；
- 插件消息在 `risk.ready` 后发送更稳妥；

### 工具检测结果
| 工具       | 问题数 | 修复情况          |
|------------|--------|-------------------|
| ESLint/TS  | 1      | 已为隐式 any 补类型 |

### 经验沉淀
1. 涉及文档结构修改（如书签/内容控件）时，优先使用 `callCommand + Api.*`，较 `executeMethod` 能力更完整；
2. 设计书签前缀有助于批量清理与幂等重建；

#### 复用记录
- 复用 `frontend/src/components/onlyoffice/OnlyOfficeEditor.vue` 的 `postMessage` 通道并扩展 `setAnchors/gotoAnchor/clearAnchors`
- 复用 `riskApi.executeReview()` 返回结构，前端做轻量规范化（空字段容错）

#### 本次任务清单（按开发顺序）
- TASK001 前端组件：风险卡面板（完成）
  - 名称: RiskCardPanel
  - 描述: 渲染 AI 风险结果，筛选/搜索，点击触发定位事件
  - 版本: v1.0  状态: 完成
  - 验收: 结果列表可筛选搜索；点击项触发 `emit('goto', anchorId)`
  - 注意: 大数据量时建议虚拟滚动；空 `message/evidence` 容错展示

- TASK004 `OnlyOfficeEditor.vue` 扩展方法（完成）
  - 名称: 编辑器对接扩展
  - 描述: 向父组件暴露 `setAnchors/gotoAnchor/clearAnchors`
  - 版本: v1.0  状态: 完成
  - 验收: 可被 `ContractReview.vue` 调用，无报错
  - 注意: 插件就绪判定与 postMessage 失败容错

- TASK002 前端：结果锚点转换与联动（完成）
  - 名称: 锚点转换与联动
  - 描述: 将 `results[].evidence[]` 转 anchors；审核后下发至编辑器；点击卡片定位
  - 版本: v1.0  状态: 完成
  - 验收: 审核完成后右侧展示结果，点击即可定位
  - 注意: 无 evidence 的项仅展示不定位

- TASK003 OnlyOffice 插件：risk-anchors（书签/高亮/定位）（完成 v1 最小可用）
  - 名称: 风险锚点插件
  - 描述: 支持三类消息 `risk.setAnchors`/`risk.gotoAnchor`/`risk.clearAnchors`
  - 版本: v1.0  状态: 完成（基础版）
  - 验收: 可接收 anchors、点击定位（Search）；清空缓存
  - 注意: 下一步可升级为“创建书签/内容控件 + 高亮 + 精确清理”

- TASK005 前端 UI 联动（完成）
  - 名称: 页面联动与切换
  - 描述: 打开编辑器后左侧切换为“风险审核”区；工具栏含“开始审核”
  - 版本: v1.0  状态: 完成
  - 验收: 视图切换正确；审核与定位链路贯通
  - 注意: 返回后保留审核结果可见

- TASK006 后端（可选）持久化与复用（计划中）
  - 名称: 风险结果与锚点持久化
  - 描述: 保存 `traceId/results/evidence` 至 DB；支持历史回溯与继续任务
  - 版本: v1.0  状态: 计划中
  - 验收: 新增表结构与查询接口；前端历史面板可读取
  - 注意: 索引 pointId/decisionType，控制 JSON 体量

#### 变更文件
- `frontend/src/components/onlyoffice/OnlyOfficeEditor.vue`: 初始化前注入本地插件；暴露 set/goto/clear 方法
- `frontend/src/views/contracts/ContractReview.vue`: 编辑视图左侧展示“风险审核”；工具栏“开始审核”联动插件
- `frontend/src/components/ai/RiskCardPanel.vue`: 统一 `anchorId` 规则为 `${pointId}_0`
- `public/onlyoffice-plugins/risk-anchors/config.json`: 标准化插件配置
- `public/onlyoffice-plugins/risk-anchors/pluginCode.js`: 实现 set/goto/clear 基础逻辑
- `public/onlyoffice-plugins/risk-anchors/index.html`: 插件入口与 SDK 引入

#### 工具检测结果
| 工具 | 问题数 | 修复情况 |
|------|--------|----------|
| Lint | 0      | 已通过   |

#### 经验沉淀
1. 前端在创建编辑器前注入插件 URL，可降低后端配置耦合
2. 先用 `Search` 落地最小可用定位，再演进到书签/控件与高亮
3. `anchorId` 与 evidence 索引对齐，UI 点击默认指向首条证据

# 合同履约任务智能识别系统

## 当前任务清单

### 2023-11-15 审核清单管理页面UI美化

本次更新对审核清单管理页面进行了全面的UI美化，添加了悬停效果和视觉增强，提升了用户体验。详细更新内容请查看 [审核清单管理页面UI美化更新日志](./ui-update-log-risklibrary.md)。

### 2023-07-15 合同智能审核页面UI美化

本次更新对合同智能审核页面进行了全面的UI美化，提升了用户体验和视觉效果。详细更新内容请查看 [UI美化更新日志](./ui-update-log.md)。

### 2025-08-07 合同履约任务功能重构

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

# 会话总结

## 2024-07-26 合同审核页面集成OnlyOffice

### 会话的主要目的
将“合同智能审核”页面中的OnlyOffice占位符替换为功能完善的真实OnlyOffice编辑器。

### 完成的主要任务
1.  **TASK001**: 在合同审核页面成功集成了 `OnlyOfficeEditor` 组件。
    -   将 `ContractReview.vue` 页面中的占位符替换为真实的编辑器组件。
    -   实现了文件上传到后端的逻辑，并能正确获取 `fileId`。
    -   将 `fileId` 传递给编辑器组件以加载相应的文档。
    -   添加了文件上传和编辑器加载过程中的状态提示与错误处理。

### 关键决策和解决方案
- **代码复用**: 决定直接复用项目中已有的 `OnlyOfficeEditor.com`，而不是重新开发。这大大加快了开发速度。
- **真实上传流程**: 重构了 `openInEditor` 方法，将其从一个模拟文件上传的同步函数，改造为调用真实API的异步函数，并添加了完整的 `try...catch` 异常处理流程。

### 使用的技术栈
- Vue 3 (Composition API)
- Element Plus
- TypeScript
- Axios (封装在 `request.ts` 中)

### 修改了哪些文件
- `frontend/src/views/contracts/ContractReview.vue`:
  - 引入并使用了 `OnlyOfficeEditor` 组件。
  - 修改了 `openInEditor` 方法以处理真实的文件上传。
  - 添加了 `onEditorReady` 和 `onEditorError` 等事件处理函数。

- `frontend/src/api/onlyoffice.ts`:
  - 新增了 `uploadFileForOnlyOffice` 函数，用于将文件上传到后端服务器。

## 2024-07-27 cankao文件夹中OnlyOffice实现分析

### 会话的主要目的
本次会话旨在分析 `cankao` 文件夹中 OnlyOffice 的实现方式，并总结如何在当前项目的合同智能审核组件中进行集成。

### 完成的主要任务
1.  浏览了 `cankao` 文件夹的整体结构。
2.  通过关键词搜索定位了与 OnlyOffice 集成相关的核心文件。
3.  详细分析了前端 Vue 组件 `onlyoffice.vue`，了解了其加载、初始化和与编辑器插件交互的机制。
4.  详细分析了后端服务 `ChangeFileToPDFService.java`，了解了其文件转换和与 OnlyOffice 服务器交互的逻辑。
5.  基于分析结果，制定了在合同智能审核组件中集成 OnlyOffice 的详细开发任务（TASK001），包括任务描述、验收标准、技术要点和接口设计等。

### 关键决策和解决方案
*   **技术选型**: 决定采用前后端分离的方式集成 OnlyOffice。前端负责展示和交互，后端负责生成配置、处理回调和文件转换。
*   **集成方案**: 前端通过动态加载 `api.js` 和实例化 `DocEditor` 对象来集成；后端通过提供 API 返回 `fileModel` 配置，并利用回调机制处理文件保存。
*   **安全性**: 强调了使用 JWT 对所有与 OnlyOffice 的通信进行安全验证的重要性。

### 使用的技术栈
*   前端: Vue.js
*   后端: Java (Spring Boot)
*   文档服务: OnlyOffice Document Server
*   数据库: MySQL (推断)
*   PDF 处理: iTextPDF

### 修改了哪些文件
本次会话没有修改任何文件，主要进行的是代码分析和方案设计。

## 2024-07-28 基于现有代码的OnlyOffice集成方案

### 会话的主要目的
结合对 `cankao`、`sdk` 和 `frontend` 文件夹的分析，制定一个在合同智能审核组件中集成 OnlyOffice 的详细、可执行的方案。

### 完成的主要任务
1.  分析了 `sdk` 文件夹，确认了 `OnlyOfficeController` 和 `OnlyofficeCallbackController` 的存在和功能。
2.  分析了 `frontend` 文件夹，确认了 `OnlyOfficeEditor.vue` 组件的存在和功能。
3.  详细阅读了 `OnlyOfficeController.java` 和 `OnlyOfficeEditor.vue` 的源代码，深入理解了它们的设计和实现。
4.  基于全面的代码分析，制定了最终的实施方案（TASK001 增强版），明确了开发步骤和验收标准。

### 关键决策和解决方案
*   **复用优先**: 最终方案的核心是复用项目中已有的、功能完善的 `OnlyOfficeController` 和 `OnlyOfficeEditor.vue` 组件，避免了重复开发，大大提高了效率。
*   **状态驱动**: 方案采用状态驱动的方式（通过 `fileId` 和 `canEdit` 两个 `ref`），当状态改变时，Vue 的响应式系统会自动触发 `OnlyOfficeEditor` 组件的重新加载和模式切换，代码逻辑清晰简洁。

### 使用的技术栈
*   前端: Vue 3 (Composition API), Element Plus, TypeScript
*   后端: Java (Spring Boot)
*   文档服务: OnlyOffice Document Server

### 修改了哪些文件
本次会话没有修改任何文件，主要进行的是代码分析和方案设计。

## 2024-07-28 实现合同审核页面内联OnlyOffice编辑器

### 会话的主要目的
根据用户需求，在“合同智能审核”页面中集成 `OnlyOfficeEditor` 组件，实现点击按钮后在页面内联打开文件预览的功能。

### 完成的主要任务
1.  **定位文件**: 准确找到了合同智能审核功能对应的 `frontend/src/views/contracts/ContractReview.vue` 文件。
2.  **代码分析**: 阅读了 `ContractReview.vue` 的源代码，理解了其现有的文件上传、状态管理和 UI 布局。
3.  **代码修改**:
    *   将原有的浮层式编辑器布局 (`.editor-overlay`) 移除。
    *   在主内容区域添加了新的编辑器视图 (`.editor-view`)，并使用 `v-if="showEditor"` 来控制其显示。
    *   将原有的上传和结果区域也包裹在一个 `div` 中，并使用 `v-else` 与编辑器视图互斥显示。
    *   修改了 `openInEditor` 方法，在文件上传成功后，设置 `showEditor.value = true` 来显示编辑器。
    *   修改了 `backToUpload` 方法，通过设置 `showEditor.value = false` 来返回上传视图。
    *   添加了新的 CSS 样式以支持内联编辑器的布局。

### 关键决策和解决方案
*   **内联替换浮层**: 放弃了原有的浮层实现，改为在主内容区域内进行视图切换。这种方式用户体验更流畅，且代码结构更易于管理。
*   **视图状态驱动**: 通过单一的状态变量 `showEditor` 来驱动整个主区域的 UI 变化（显示上传区域 vs 显示编辑器），这是 Vue.js 中管理视图状态的最佳实践，使得逻辑清晰且易于维护。
*   **复用现有逻辑**: 最大限度地复用了 `openInEditor` 方法中已有的文件上传逻辑，只在其基础上增加了视图切换的功能，减少了代码改动量。

### 使用的技术栈
*   Vue 3 (Composition API, `<script setup>`)
*   Element Plus
*   TypeScript

### 修改了哪些文件
*   `frontend/src/views/contracts/ContractReview.vue`: 进行了大量的模板、脚本和样式修改，以实现新的内联编辑器功能。

## 2024-07-28 修复OnlyOffice集成404错误并完成实现

### 会话的主要目的
解决用户在“合同智能审核”页面点击“在编辑器中打开”时遇到的 404 错误，并根据用户反馈调整和实现 OnlyOffice 编辑器的集成方式。

### 完成的主要任务
1.  **问题诊断**:
    *   通过分析前端 `frontend/src/api/ai/risk.ts` 文件，确认了文件上传接口指向 `/api/review/upload`。
    *   通过分析后端 `sdk` 项目的所有控制器，发现不存在 `/api/review/upload` 接口，从而定位了 404 错误的根源。
    *   进一步分析发现，已有的 `/api/compare/upload` 接口需要接收两个文件，与当前场景不匹配。
2.  **后端开发**:
    *   创建了一个新的 `ReviewController.java` 文件。
    *   在新控制器中实现了一个 `POST /api/review/upload` 接口，专门用于接收单个文件上传，并返回 `fileId`。
3.  **前端实现**:
    *   确认了前端 API (`riskApi.uploadFileForReview`) 的调用地址与新建的后端接口一致。
    *   恢复了之前被用户拒绝的前端 UI 修改，将 `ContractReview.vue` 中的 OnlyOffice 编辑器从浮层改为内联显示，并通过 `showEditor` 状态进行视图切换。

### 关键决策和解决方案
*   **创建专用接口**: 没有复用功能不匹配的 `/api/compare/upload` 接口，而是为智能审核场景创建了一个新的、职责单一的 `/api/review/upload` 接口。这是更清晰、更健壮的设计，避免了未来因接口功能混杂导致的维护困难。
*   **根本原因分析**: 通过深入分析前后端代码，准确地定位了 404 错误是由于后端接口缺失造成的，而不是前端组件的集成问题，从而制定了正确的解决方案。
*   **坚持最佳实践**: 保持了 `OnlyOfficeEditor` 作为独立子组件的结构，同时通过视图状态切换实现了用户期望的内联体验，兼顾了代码质量和用户需求。

### 使用的技术栈
*   后端: Java (Spring Boot), Swagger
*   前端: Vue 3 (Composition API), TypeScript

### 修改/创建了哪些文件
*   `sdk/src/main/java/com/zhaoxinms/contract/template/sdk/controller/ReviewController.java` (新建): 创建了新的后端控制器和上传接口。
*   `frontend/src/views/contracts/ContractReview.vue` (修改): 恢复并最终应用了 UI 改造，实现了内联编辑器功能。

## 2024-07-28 修复OnlyOffice预览内容不正确的问题

### 会话的主要目的
解决用户上传文件后，OnlyOffice 预览显示的不是所上传文件的问题。

### 完成的主要任务
1.  **问题诊断**: 快速定位到问题根源在于后端上传逻辑。无论用户上传什么文件，后端都使用固定的 `fileId ("templateDesign")` 来覆盖保存，导致预览内容不正确。
2.  **后端服务层增强**:
    *   在 `FileInfoServiceImpl.java` 中新增了一个 `saveNewFile(MultipartFile file)` 方法。
    *   该方法负责生成唯一的 UUID 作为新文件的 `fileId`，将文件保存到磁盘，并在内存中注册一个全新的 `FileInfo` 记录。
3.  **后端控制器重构**:
    *   修改了 `ReviewController.java` 中的 `/api/review/upload` 接口。
    *   使其调用新建的 `FileInfoServiceImpl.saveNewFile` 方法，从而确保每次上传都创建新文件记录。
    *   返回给前端的是新生成的、唯一的 `fileId`。
4.  **实体类调整推断**:
    *   根据 `saveNewFile` 方法中使用 UUID 作为 ID 的实现，推断并建议将 `FileInfo` 实体类中的 `id` 字段类型从 `Long` 修改为 `String`。

### 关键决策和解决方案
*   **创建新文件记录而非覆盖**: 放弃了之前覆盖固定文件记录的临时修复方案，采用了更健壮的方案，即为每个上传的文件创建独立的、唯一的记录。这从根本上解决了文件内容混淆的问题。
*   **服务层封装**: 将创建新文件的复杂逻辑（生成ID、保存文件、创建记录）封装在 `FileInfoServiceImpl` 中，保持了 `ReviewController` 的简洁和职责单一。

### 使用的技术栈
*   后端: Java (Spring Boot)

### 修改/创建了哪些文件
*   `sdk/src/main/java/com/zhaoxinms/contract/template/sdk/service/impl/FileInfoServiceImpl.java` (修改): 新增 `saveNewFile` 方法。
*   `sdk/src/main/java/com/zhaoxinms/contract/template/sdk/controller/ReviewController.java` (修改): 调用新的服务方法，并返回正确的 `fileId`。

### 2025-08-18 合同智能审核页：自动加书签与定位（OnlyOffice 插件联动）

#### 现状核验
- 前端 `ContractReview.vue` 与 `OnlyOfficeEditor.vue` 在同一页面联动：已实现。
  - `ContractReview.vue`：在“开始审核（模拟）/startReview”后组装 `anchors`，调用 `onlyofficeEditorRef.setAnchors(anchors)`；点击风险卡片触发 `goto`，调用 `onlyofficeEditorRef.gotoAnchor(anchorId)`。
  - `OnlyOfficeEditor.vue`：在初始化时注入本地插件 `public/onlyoffice-plugins/risk-anchors/config.json`；向外暴露 `setAnchors/gotoAnchor/clearAnchors`。
  - 插件 `risk-anchors/pluginCode.js`：接收 `risk.setAnchors` 创建书签（前缀 `risk_anchor_`），`risk.gotoAnchor` 跳书签，`risk.clearAnchors` 清理。已具备幂等重建与兜底逻辑。

#### 代码复用检查（增强版）
- 直接复用：
  - `OnlyOfficeEditor.vue` 的插件注入与方法通道 → 直接调用
  - `RiskCardPanel.vue` → 复用 `goto` 事件实现卡片点击定位
  - 插件 `risk-anchors` → 已支持书签创建/定位/清理
- 扩展复用：
  - `ContractReview.vue` 替换模拟数据为真实接口 → 仅调整数据来源与锚点映射
- 重构复用：
  - 无需，现有结构清晰；后续可抽出 anchors 构建工具函数
- 全新开发：
  - 非必需；可新增可视化“书签状态/数量”提示与调试开关

#### 任务清单（按优先顺序）
- TASK001 现状核验与联通性自检（完成）
  - 类型：重构/确认  版本：v1.0  状态：完成
  - 描述：确认编辑器与插件同页、方法通道有效、卡片定位事件连通
  - 验收：页面内可调用 `setAnchors/gotoAnchor/clearAnchors`，控制台无报错
  - 注意：OnlyOffice 跨 frame 通讯偶发受限，已内置 localStorage 轮询兜底

- TASK002 审核后自动落书签（完成）
  - 类型：功能新增  版本：v1.0  状态：完成
  - 描述：将审核结果 `evidence` 转为 anchors 并下发插件创建书签
  - 验收：anchors 数量与 evidence 数量一致；再次审核可幂等重建
  - 注意：优先使用 `paragraphIndex/startOffset/endOffset`，缺失再用 `text` 搜索

- TASK003 风险卡片点击定位（完成）
  - 类型：功能新增  版本：v1.0  状态：完成
  - 描述：点击卡片按规则 `anchorId=pointId_0` 跳转对应书签
  - 验收：点击卡片时光标跳至对应位置
  - 注意：无 evidence 的项仅展示，不触发定位

- TASK004 接入真实后端审核接口（开发中）
  - 类型：功能新增  版本：v1.1  状态：开发中
  - 描述：将 `startReview` 的模拟数据替换为 `riskApi.executeReview(profileId?, pointIds?, file)` 真实返回
  - 验收：完成后 anchors 仍正确创建；显示 `traceId/elapsedMs/usage`
  - 注意：后端需保证返回 `evidence` 含精确定位字段；若缺失则仅文本搜索

- TASK005 文件切换与返回时的书签清理（计划中）
  - 类型：功能新增  版本：v1.1  状态：计划中
  - 描述：切换文件/返回上传视图时调用 `clearAnchors`
  - 验收：文件切换后文档不残留旧书签
  - 注意：考虑未就绪/插件异常的兜底日志与静默失败

- TASK006 插件就绪状态与错误提示（计划中）
  - 类型：功能新增  版本：v1.1  状态：计划中
  - 描述：在编辑器 ready 后显示“插件已就绪/锚点已创建(N)”等提示
  - 验收：`risk.anchorsSet`/`risk.cleared` 回传后有 UI 提示
  - 注意：跨域受限时退化为轮询状态提示

- TASK007 性能与稳健性（计划中）
  - 类型：重构  版本：v1.1  状态：计划中
  - 描述：大文档/大量 anchors 的分批创建与节流；异常书签名过滤
  - 验收： anchors>200 时仍可在 2s 内完成创建（本地文档）
  - 注意：限定书签前缀与安全字符，避免冲突

- TASK008 自动化测试（计划中）
  - 类型：测试  版本：v1.1  状态：计划中
  - 描述：前端 e2e（Cypress）覆盖“审核→创建书签→定位→清理”主链路
  - 验收：CI 通过；核心用例稳定

#### 需要的接口/组件/界面变更
- 接口（已有/复用）：
  - `/onlyoffice/server/info`、`/onlyoffice/editor/config`（已用）
  - `/review/upload`（已用）
  - `/ai/review-lib/review/execute`（接入真实审核）
- 组件（复用/轻改）：
  - `OnlyOfficeEditor.vue`（已暴露 set/goto/clear，无需改结构）
  - `RiskCardPanel.vue`（已发出 goto 事件）
  - `ContractReview.vue`（替换模拟数据、添加清理与提示）
- 界面：
  - 编辑器工具条增加“清理书签”“书签数”提示（可选隐藏调试开关）

#### 注意事项（落地细节）
- 插件消息桥：优先 `postMessage`，失败使用 localStorage 轮询
- 书签命名：统一前缀 `risk_anchor_` + 安全化 ID，避免重复与清理困难
- 幂等：创建前清理已有同前缀书签；文件切换务必清理
- 兼容：`paragraphIndex` 可能与实际段落存在偏移（被 OnlyOffice 重排时），需保留文本兜底

### 2025-08-18 审核后“克隆并落盘书签”联动（启用编辑模式）

#### 变更
- 前端
  - `frontend/src/views/contracts/ContractReview.vue`
    - 将 `OnlyOfficeEditor` 的 `:can-edit` 改为 `true`
    - 在 `startAuditInEditor()` 内调用 `/api/review/persist-anchors`，持久化书签到新文件，并将 `uploadedFileId` 切换为新文件
- 后端（SDK）
  - `sdk/pom.xml`：新增 `poi-ooxml`
  - `sdk/src/main/java/.../service/impl/FileInfoServiceImpl.java`：新增 `registerClonedFile(Path, originalName)`
  - `sdk/src/main/java/.../controller/ReviewController.java`：新增 `POST /api/review/persist-anchors`，克隆源文件并写入 `risk_anchor_*` 书签，返回新 `fileId`

#### 验收
- 在编辑器视图点击“开始审核”后：
  - 返回新的 `fileId` 并自动切换
  - 下载新文件，可在 Word“插入→书签”看到 `risk_anchor_*`

#### 注意
- 仍保留插件会话级书签到位作为回退（当后端持久化失败时）
- 后续可将段内字符偏移写入书签范围（当前按段落头尾添加）


### 2025-08-18 参考示例：DOCX 自动添加书签小工具

#### 复用记录
- 采用 Apache POI `poi-ooxml` 处理 DOCX，独立于主后端模块（仅放于 `cankao` 目录）。

#### 变更与实现
- 新增参考示例项目：`cankao/docx-bookmark-adder`
  - `pom.xml`：引入 `poi-ooxml`、`commons-lang3`、`slf4j-simple`；配置 `maven-jar-plugin` 与 `maven-dependency-plugin`
  - `src/main/java/com/zhaoxinms/cankao/BookmarkAdder.java`：遍历段落，按正则识别“第X章/第X条/第X节/附件/附录”等标题并插入 `BookmarkStart/End`，输出 `*-bookmarked.docx`

#### 验收标准
- 运行后目标 DOCX 中可在书签面板看到新增书签；输出文件命名为 `${原名}-bookmarked.docx` 且可打开。

#### 注意事项
- 仅支持 DOCX；若为 DOC 请先转换
- 规则基于段落文本正则，若模板标题风格差异较大需调整正则

#### 使用指南（PowerShell）
```powershell
cd D:\git\zhaoxin-contract-tool-set\cankao\docx-bookmark-adder
mvn -DskipTests package
# 默认处理内置路径：
java -jar .\target\docx-bookmark-adder-1.0.0.jar
# 或指定自定义 DOCX：
java -jar .\target\docx-bookmark-adder-1.0.0.jar "C:\\Users\\91088\\Desktop\\1.0.肇新合同系统源码销售合同（二次）.docx"
```

#### 任务清单
- TASK001 示例项目初始化（完成）
  - 名称：创建 `cankao/docx-bookmark-adder` 与 Maven 配置
  - 版本：v1.0  状态：完成
- TASK002 书签添加实现（完成）
  - 名称：按合同章节/条款标题自动加书签
  - 版本：v1.0  状态：完成
- TASK003 使用说明与沉淀（完成）
  - 名称：补充 README 与使用指南
  - 版本：v1.0  状态：完成
- TASK004 精准范围书签（计划中）
  - 名称：支持按字符偏移/文本索引范围创建书签
  - 版本：v1.1  状态：计划中
