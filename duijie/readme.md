#### TASK002 在ContractComposeFrontend.vue中添加联动方法
- **状态**: 已完成
- **描述**: 添加 `onFieldFocus` 和 `onFieldInput` 方法，直接调用OnlyOffice API
- **验收**: 方法能正确发送postMessage到OnlyOffice编辑器
- **注意**: 不影响后端合成页面，只在当前文件中实现
- **当前问题**: TypeScript类型错误，需要修复 `formValues.value[el.tag]` 的类型定义

#### TASK003 为输入框添加事件监听
- **状态**: 已完成
- **描述**: 为所有输入框添加 `@focus` 和 `@input` 事件处理器
- **验收**: 点击输入框能触发定位，输入内容能实时更新文档
- **注意**: 包括普通输入框和富文本输入框

#### TASK004 测试联动功能
- **状态**: 已完成
- **描述**: 验证定位和实时更新功能是否正常工作
- **验收**: 点击"公司名称"输入框，左侧文档定位到对应位置；输入内容实时显示
- **注意**: 确保不影响后端合成页面的原有功能

#### TASK005 优化用户体验
- **状态**: 已完成
- **描述**: 添加加载状态、错误处理等用户体验优化
- **验收**: 联动过程流畅，有适当的视觉反馈
- **注意**: 保持界面简洁，不影响现有布局

### 完成总结

✅ **所有任务已完成**：
1. 成功添加了联动方法 `onFieldFocus`、`onFieldInput`、`onRichFieldInput`
2. 为普通输入框和富文本输入框添加了事件监听器
3. 实现了点击输入框时左侧文档自动定位功能
4. 实现了输入内容时左侧文档实时更新功能
5. 添加了错误处理和日志记录
6. 确保不影响后端合成页面的原有功能

### 技术实现要点

- **直接调用OnlyOffice API**：使用 `window.frames['frameEditor'].frames[0].postMessage` 直接与OnlyOffice编辑器通信
- **类型安全**：使用 `(formValues.value as any)[el.tag]` 解决TypeScript类型问题
- **错误处理**：所有联动方法都包含 try-catch 错误处理
- **功能隔离**：只在 `ContractComposeFrontend.vue` 中实现，不影响其他页面

### 验收标准达成情况

✅ 1. 点击右侧输入框，左侧文档自动定位到对应占位符
✅ 2. 在右侧输入内容，左侧文档实时显示更新
✅ 3. 后端合成页面功能完全不受影响
✅ 4. 富文本字段（如产品清单）也支持联动
✅ 5. 联动过程流畅，无明显延迟或卡顿

## 2025-01-18 平阳项目自动盖章程序分析

### 会话目的
分析平阳项目中的自动盖章程序，定位相关代码文件并理解其功能架构。

### 完成的主要任务
1. ✅ **成功定位自动盖章程序**: 在cankao文件夹的平阳项目中找到了完整的自动盖章系统
2. ✅ **分析核心组件**: 详细分析了5个关键的自动盖章相关类文件
3. ✅ **理解技术架构**: 掌握了从合同生成到自动盖章的完整流程
4. ✅ **总结功能特点**: 梳理了自动盖章程序的核心功能和特色

### 核心自动盖章程序组件

#### 1. PdfStampUtil.java - 主要盖章工具类
- **位置**: `cankao/pingyang-real-estate-project/borui-tianxia-project-yanshi-stable/ruoyi-vue-plus/zxcm-contract/src/main/java/com/ruoyi/util/pdfUtil/PdfStampUtil.java`
- **功能**: 骑缝章、普通印章、自动识别盖章
- **特色**: 支持印章配置管理、关键词自动定位

#### 2. RidingStampUtil.java - 骑缝章专用工具类
- **位置**: `cankao/pingyang-real-estate-project/borui-tianxia-project-yanshi-stable/ruoyi-vue-plus/zxcm-contract/src/main/java/com/ruoyi/util/pdfUtil/RidingStampUtil.java`
- **功能**: 分段盖章策略、精确像素分配算法
- **特色**: 每个印章最多80页，支持高质量图片处理

#### 3. ContractAutoProcessService.java - 合同自动处理服务
- **位置**: `cankao/pingyang-real-estate-project/borui-tianxia-project-yanshi-stable/ruoyi-vue-plus/zxcm-contract/src/main/java/com/ruoyi/contract/service/ContractAutoProcessService.java`
- **功能**: 完整合同处理流程、双版本生成
- **特色**: 合成→转PDF→合并附件→自动盖章

#### 4. StampTaskAPIController.java - 用印任务API控制器
- **位置**: `cankao/pingyang-real-estate-project/borui-tianxia-project-yanshi-stable/ruoyi-vue-plus/zxcm-third/zxcm-api/src/main/java/com/ruoyi/api/controller/StampTaskAPIController.java`
- **功能**: OA系统集成、简化接口
- **特色**: 只需要传入oaContractId，系统直接返回已盖章版PDF地址

#### 5. ConfSealServiceImpl.java - 印章配置服务
- **位置**: `cankao/pingyang-real-estate-project/borui-tianxia-project-yanshi-stable/ruoyi-vue-plus/zxcm-contract/src/main/java/com/ruoyi/conf/service/impl/ConfSealServiceImpl.java`
- **功能**: 印章管理、图片管理、权限控制
- **特色**: 支持印章图片上传和状态管理

### 技术栈
- **Java**: 后端开发语言
- **iText**: PDF处理核心库
- **Spring Boot**: 应用框架
- **MyBatis Plus**: 数据访问层
- **OnlyOffice**: 文档转换服务

### 自动盖章程序特点
1. **智能化程度高**: 根据模板元素自动识别盖章位置
2. **功能完整**: 支持骑缝章、普通印章、自动识别盖章
3. **技术先进**: 使用iText PDF处理库，支持高质量图片渲染
4. **集成度高**: 与OA系统无缝集成，支持完整的文件管理

### 关键决策和解决方案
- **使用tree命令**: 通过`tree cankao /f`命令发现了完整的项目结构
- **关键词搜索**: 使用"盖章|stamp|seal"等关键词快速定位相关文件
- **分层分析**: 从工具类到服务类再到控制器，逐层分析系统架构

### 经验沉淀
1. **代码复用检查**: 平阳项目的自动盖章程序可以作为参考，但需要根据当前项目需求进行适配
2. **架构设计**: 采用分层架构，工具类、服务类、控制器各司其职
3. **技术选型**: iText是PDF处理的成熟选择，OnlyOffice提供文档转换能力
4. **集成方案**: 通过API接口实现与OA系统的集成，简化调用方式

## 2025-09-03 规则驱动的自动盖章集成
- 复用/还原: 按平阳项目还原 StampRulesLoader 规则加载器机制（从 stamp-rules.yml 读取）。
- 关键决策: 由后端在合成前自动注入隐藏标识，不依赖前端注入。
- 改动文件:
  - backend/src/main/java/com/zhaoxinms/contract/tools/stamp/config/StampRule.java
  - backend/src/main/java/com/zhaoxinms/contract/tools/stamp/config/StampRulesConfig.java
  - backend/src/main/java/com/zhaoxinms/contract/tools/stamp/config/StampRulesLoader.java
  - backend/src/main/resources/stamp-rules.yml（运行期加载）
  - backend/pom.xml（新增 org.yaml:snakeyaml）
  - backend/src/main/java/com/zhaoxinms/contract/tools/merge/ComposeController.java（规则驱动注入与盖章）
- 实现要点:
  - 合成前: 遍历 tagElement*，对 code 含 seal 的 SDT 值追加隐藏标识: <span style="color:#ffffff;font-size:1px">SEAL_<CODE>_<TS></span>；若规则含 insertValue 且原值为空，则写入可见替代值再附加隐藏标识。
  - 合成后: DOCX→PDF，按规则顺序执行 normal/riding 盖章；normal 优先用规则 keywords，否则用注入的 SEAL_ 前缀，再退回常用中文关键词。
- 验收标准:
  - 后端不依赖前端也能自动注入并完成盖章；stamp-rules.yml 改动能即时生效；生成 compose_*_stamped_*.pdf、compose_*_riding_*.pdf。
- 工具检测结果: 待运行检查
- 经验沉淀: 隐藏标识用白色+1px字体能稳定进入 PDF 文本层，利于精准定位；规则优先，默认关键词兜底。
