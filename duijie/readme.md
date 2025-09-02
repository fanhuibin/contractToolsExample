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
- 点击"开始审核"后，插件能够为返回的所有风险点创建对应书签；
- 在"风险审核"面板点击任意卡片，文档可跳转至对应位置；
- 再次审核或点击"清理锚点"（内部命令）后，旧书签被清理并重新创建；

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
  - 注意: 下一步可升级为"创建书签/内容控件 + 高亮 + 精确清理"

- TASK005 前端 UI 联动（完成）
  - 名称: 页面联动与切换
  - 描述: 打开编辑器后左侧切换为"风险审核"区；工具栏含"开始审核"
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
- `frontend/src/views/contracts/ContractReview.vue`: 编辑视图左侧展示"风险审核"；工具栏"开始审核"联动插件
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
### 2025-09-02 OCR Diff 过滤：从“删除”改为“相等”
- 主要目的：将满足过滤条件（仅空格/下划线/换行，或成对互消的单字符标点`, / 、 / .`）的差异由 INSERT/DELETE 统一转换为 EQUAL，而不是直接删除，从而保留长度与游标一致性，便于后续基于索引的矩形映射与统计。
- 技术栈：Java（diff-match-patch 封装 `DiffUtil`）。
- 修改文件：
  - `backend/src/main/java/com/zhaoxinms/contract/tools/compare/DiffUtil.java`
    - 方法 `diff_cleanupCustomIgnore(LinkedList<Diff> diffs)`：
      - 第一阶段：将仅由空格/下划线/空格+下划线/含换行的差异，统一改写为 `EQUAL`，不再丢弃。
      - 第二阶段：当相邻的单字符目标标点（`,、.`）一删一增时，合并为一个长度为 1 的 `EQUAL`，而非双方删除。
      - 第三阶段：保持原有“相邻 EQUAL 合并”的行为。
- 影响：
  - `DotsOcrCompareDemoTest` 后续以 diff 游标映射 bbox 时，等长保留使索引更稳定；
  - 视觉上，这些“可忽略差异”在结果中体现为相等段，避免误报新增/删除。

### 2025-09-01 Dots.OCR 比对调试能力增强（导出抽取文本）
- 主要目的：为了手动校验 OCR JSON 抽取内容与版面顺序，新增将解析到的文本落盘为 `.extracted.txt`。
- 技术栈：Java（JUnit5）、PDFBox、OkHttp、Jackson。
- 修改文件：
  - `sdk/src/test/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrCompareDemoTest.java`
    - 在 `recognizePdfAsCharSeq(...)` 中并发解析完成后、展开字符前，汇总每页 `LayoutItem.text` 并写入 `${pdf}.extracted.txt`。
- 使用方式：运行 `compareTwoPdfs_Demo` 时，会在传入的 PDF 同目录生成 `*.extracted.txt`，每页以 `=== PAGE n ===` 分隔，便于人工对照与定位。
- 影响：不改变现有比对流程输出，仅新增调试文件输出；异常写盘不会中断测试（打印错误并继续）。

#### 2025-09-01 文本页内顺序优化
- 现象：`.extracted.txt` 中每页文本出现"从底向上"的倒序。
- 原因：原实现使用 DFS 的栈式遍历导致数组与对象字段压栈后出栈顺序反转。
- 调整：
  - `extractLayoutItems` 改为队列（BFS）遍历，保持 JSON 原序。
  - 返回前按版面近似阅读顺序排序：先按 `y1` 升序，再按 `x1` 升序。
- 影响：每页文本输出更符合"由上至下、由左至右"的阅读直觉。

#### 2025-09-01 新增对比用纯文本文件（无页标记）
- 场景：含有 `=== PAGE n ===` 的页分隔会影响文本级别差异对比。
- 调整：在生成 `${pdf}.extracted.txt`（含页标记）的同时，新增 `${pdf}.extracted.compare.txt`，去除所有页标记，仅保留文本行，适用于 `diff`/对照工具。
- 影响：便于直接对比两版文档的抽取文本，不受页头标识干扰。

### 2025-09-01 Dots.OCR 并发控制与渲染参数
- 并发控制：
  - `DotsOcrClient` 新增最大并发限流（默认 10，可在 `builder.maxConcurrency(n)` 配置）。
  - 作用范围：每次 `POST /v1/chat/completions` 调用都会占用一个许可，完成后释放。
- PDF 渲染（由客户端配置控制）：
  - `DotsOcrClient.builder().renderDpi(150)` 设置渲染 DPI（默认 150）。
  - `DotsOcrClient.builder().saveRenderedImages(true)` 决定是否保存 `${pdf}.page-<n>.png` 到同目录（默认 false）。
- 修改文件：
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrClient.java`：并发限流（`Semaphore`）、新增渲染配置（renderDpi/saveRenderedImages）。
  - `sdk/src/test/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrCompareDemoTest.java`：渲染函数改为从客户端读取配置。

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

### 2025-09-01 Dots.OCR 比对：按 bbox 换行与效率清理
- 主要目的：
  - 生成用于比对的文本时按布局项 bbox 换行，保证“行”粒度与版面一致；
  - 使用 Efficiency Cleanup 清理 diff 结果，并将编辑成本 `EditCost` 设为 10。
- 技术栈：Java（JUnit5）、diff-match-patch 封装（`DiffUtil`）。
- 修改文件：
  - `sdk/src/test/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrCompareDemoTest.java`
    - `expandToChars(...)`：同一布局项内字符共享同一 `bbox`；
    - 新增 `joinWithLineBreaks(...)`：当 `page+bbox` 切换时追加换行，并在最后一个 bbox 收尾换行；
    - 比对阶段设置 `dmp.Diff_EditCost = 10`，并调用 `dmp.diff_cleanupEfficiency(diffs)`；
    - 保留标点归一与 Markdown 符号等长空格替换，不影响行结构。
- 影响：
  - `normA`/`normB` 进入 diff 前按 bbox 为行分隔，结果更贴近视觉行；
  - 效率清理减少琐碎变更噪声，`EditCost=10` 提高合并小跨度修改的阈值。

### 2025-08-12 强制 AI 分支与耗时分析
- 变更记录
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/controller/RiskLibraryController.java`：`/ai/review-lib/review/execute` 仅接受 `multipart/form-data`，`file` 必填；删除"轻量 DB 映射"分支，强制走 AI。
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
  - 新增 `sdk/src/main/resources/db/migration/V12__seed_risk_review_full.sql`：基于"原创化且扩充为98条"的清单，按 `pointCode` 全量写入 `review_point`/`review_prompt`/`review_action` 与 `review_profile_item`，并保持与 `orderList` 一致顺序；所有编号（如 `3437`）未变。
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
  - 专用分类器/微调模型：对"是否存在某条款/是否合规"做分类，再由 LLM 生成人话提示。
- 本项目当前实现
  - 单次大提示 + Qwen（fileid）+ 数据驱动 Prompt（DB 中 prompts/actions），后端不做算法。
- 差异与权衡
  - 你的方式：上线快、改配置即可扩展，但时延较高、稳定性/一致性依赖提示设计与模型负载。
  - 行业主流：RAG/多阶段更常见，吞吐与可控性更优，但工程复杂度高。
  - 若保持"无后端算法"原则，可引入轻量"输入裁剪"和"输出约束"来接近 RAG 的收益，而不改变核心理念。

### 2025-08-12 功能增强建议（对标当前实现）
- 体验与可观测性
  - 服务端流式转发与进度上报（上传/构造提示/模型推理/解析各阶段耗时与状态）。
  - 失败可重试与"继续上次任务"（基于 `traceId` 与文件 `hash`）。
  - 模型动态选择：短文 `qwen-plus`，长文自动切 `qwen-long`。
- 输出收敛与速度优化（保持"无后端算法"）
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
  - 分类名（`review_clause_type.clause_name`）：除"合同主体"外统一改写为"法律条款引用/价款与支付/履约安排/…"
  - 风险点（`review_point.point_name/algorithm_type`）：如"己方主体 → 内部相对方"、"对方主体 → 外部相对方"；若仅名称调整，`algorithm_type` 同步。
  - 提示（`review_prompt.prompt_key/name/message`）：口径统一为"确认/缺失/不清/不一致"等中性表述；3649/3702 等涉及"内部/外部相对方"已精确改写。
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
  - 前端树渲染内部节点（用于 el-tree）由"点为叶"改为"提示为叶"，并增加 `type` 与带前缀的 `id`（用于选择聚合）：
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
  - 后端：新增树接口返回"分类→风险点→提示"三级结构（提示为叶子，来源 `review_prompt` 实表），保持与真实数据库一致。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RiskLibraryService.java`：新增 `TreeNodeV2`、`PointNode` DTO 与 `treeWithPrompts(Boolean enabled)` 方法签名。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/impl/RiskLibraryServiceImpl.java`：实现 `treeWithPrompts`，按 `enabled` 过滤并按 `sort_order,id` 排序，组装三级数据。
    - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/controller/RiskLibraryController.java`：新增 `GET /api/ai/review-lib/tree-prompts`。
  - 前端：`RiskLibrary.vue` 树改为"提示（prompt）"为叶子；勾选提示后映射为所属 `pointId` 去重，再调用原有预览/审核接口；应用方案时按 `pointId` 勾选对应点下的全部提示叶。
    - `frontend/src/api/ai/risk.ts`：新增 `getTreePrompts(enabled?)`；`deleteProfile(id, force?)` 支持强删。
    - `frontend/src/views/contracts/RiskLibrary.vue`：
      - `loadTree()` 切换为 `getTreePrompts()`；`buildTree()` 生成 3 级节点（`CLAUSE/POINT/PROMPT`），叶子为 `PROMPT`。
      - `onPreviewSelection()`：从被勾选 `PROMPT` 节点收集所属 `pointId` 去重预览。
      - `applyProfile()`：将方案里的 `pointId` 映射成该点下的所有 `PROMPT` 节点 key（`r-<id>`）批量勾选。
      - 点击提示叶时同步右侧"提示与动作"列表基于父级风险点加载。
- 影响
  - 树的叶子与真实 `review_prompt` 表记录一致（而非固定 98 条，V12 仅为初始种子）。
  - 预览与执行审核仍按 `pointId` 粒度，无需后端执行链路改造。
  - `enabled=true` 时仅返回启用的分类/点/提示；"显示停用"开关生效后端联动。
- 变更记录
  - `frontend/src/views/contracts/RiskLibrary.vue`：
    - 在左侧卡片 header 增加"返回主页"按钮（`goHome()` → `router.push('/')`）。
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
  - "合同名称"自动去除"合同/合同书"后缀，并禁止包含这些词；
  - 金额/比例字段仅保留数字与小数点；
  - 日期字段解析多种格式并统一输出为 `yyyy-MM-dd`。
- 说明：当前仅新增扩展文件，不影响现有流程；后续可在 SDK 的提示构建与结果后处理阶段按需加载应用。

### 2025-08-08 合同抽取规则执行集成与"合同名称"策略调整
- 修改：在 SDK 抽取流程中集成外置规则库执行（normalize + validate），路径：`sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/impl/ContractExtractServiceImpl.java`（抽取完成后按模板 `contractType` 加载并执行规则）
- 新增：规则模型与服务
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/ContractRules.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/FieldRule.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/rules/RuleViolation.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RuleLoaderService.java`
  - `sdk/src/main/java/com/zhaoxinms/contract/tools/aicomponent/service/RuleEngineService.java`
- 配置：`sdk/src/main/resources/application-ai.yml` 新增 `zxcm.ai.contract.enforce-rules: true`
- 规则：移除"合同名称"的 `denyContains`，仅保留后缀清洗 `(合同|合同书)$`
  - 影响文件：`lease.json`、`purchase.json`、`labor.json`、`construction.json`、`technical.json`、`intellectual.json`、`operation.json`
- 效果：
  - "合同名称"中部的"合同"保留，末尾"合同/合同书"被去除。
  - 示例：输入"肇新合同管理系统源码销售合同"→ 输出"肇新合同管理系统源码销售"。

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
  - 抽取服务：`ContractExtractServiceImpl`

### 2025-09-01 Dots.OCR 五步流水线与可恢复调试
- 流程拆分：
  1) PDF 拆分渲染为图片
  2) 调用识别服务器逐页识别
  3) 保存识别产物：每页 JSON（`*.page-<n>.ocr.json`）与全文 TXT（含/不含页标记）
  4) 执行比对算法（基于字符序列与 bbox 聚合）
  5) 返回比对结果（`*.compare.json`）
- 可恢复调试：
  - 通过修改 `resumeFromStep4 = true`，可跳过渲染与识别，直接从已保存的 `*.page-<n>.ocr.json` 加载并进入步骤 4。
- 修改文件：
  - `sdk/src/test/java/com/zhaoxinms/contract/tools/ocr/dotsocr/DotsOcrCompareDemoTest.java`
    - 识别阶段保存每页 JSON：`${pdf}.page-<n>.ocr.json`
    - 支持从本地 JSON 恢复（resumeFromStep=4）
    - 仍会生成 `${pdf}.extracted.txt` 与 `${pdf}.extracted.compare.txt`

#### 2025-09-01 测试固定参数
- 为了便于本地快速验证：`compareTwoPdfs_Demo` 中 `baseUrl/fileA/fileB` 改为硬编码常量：
  - `baseUrl = http://192.168.0.100:8000`
  - `fileA = C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test1.pdf`
  - `fileB = C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test2.pdf`
（如需切换文件/服务地址，可直接修改测试代码。）