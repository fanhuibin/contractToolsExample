# 赵信合同工具集开发总结

## 项目概述
- **项目名称**: 赵信合同工具集 (Zhaoxin Contract Tool Set)
- **参考项目**: JNPF-SpringBoot.V5.2.7-多租户 (旧项目，作为参考)
- **新项目**: zhaoxin-contract-tool-set (当前开发项目)
- **技术栈**: Vue 3 + TypeScript + Spring Boot 2.7.x + OnlyOffice

## AI组件开发任务清单

### TASK011: AI组件开发 - 后端基础配置
- **任务名称**: AI组件开发 - 后端基础配置
- **任务描述**: 创建AI模块后端配置文件和映射类，为整个AI组件提供灵活配置的基础
- **版本**: v1.0.0
- **状态**: 开发中
- **验收标准**:
  - [ ] 在后端项目中创建aicomponent目录
  - [ ] 创建application-ai.yml配置文件
  - [ ] 创建AiProperties.java配置类
  - [ ] 配置文件包含通义千问和PDF抽取的配置项
- **注意事项**:
  - API密钥等敏感信息使用占位符
  - 属性命名采用驼峰式
  - 配置项要全面覆盖AI功能需求
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-admin/src/main/resources/application-dev.yml` (第149-175行AI配置)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/config/AiProperties.java` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/constants/AiConstants.java` (常量定义)

### TASK012: AI组件开发 - 后端服务自动装配
- **任务名称**: AI组件开发 - 后端服务自动装配
- **任务描述**: 实现AI服务的自动配置，利用Spring Boot的自动装配能力初始化通义千问SDK客户端
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建AiAutoConfiguration.java自动配置类
  - [ ] 实现@Configuration和@EnableConfigurationProperties注解
  - [ ] 创建通义千问客户端Bean
  - [ ] 实现条件装配功能
- **注意事项**:
  - 确保服务的创建和依赖注入在自动配置中完成
  - 业务代码不应关心初始化细节
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/config/AiAutoConfiguration.java` (直接复制)

### TASK013: AI组件开发 - PDF内容抽取功能开发(后端)
- **任务名称**: AI组件开发 - PDF内容抽取功能开发(后端)
- **任务描述**: 开发PDF文件文本抽取服务，实现接收前端上传的PDF文件并返回文本内容
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建PdfExtractService.java服务类
  - [ ] 创建PdfExtractController.java控制器类
  - [ ] 实现文件上传和处理接口
  - [ ] 实现PDF文本抽取功能
  - [ ] 实现文件大小限制和异常处理
- **注意事项**:
  - 引入Apache PDFBox等第三方库处理PDF解析
  - 考虑并发请求和临时文件处理
  - 防止内存溢出和文件泄露
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/util/AiLimitUtil.java` (限流工具类)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-core/src/main/java/jnpf/model/ai/AiFormModel.java` (AI模型类)

### TASK014: AI组件开发 - AI智能聊天功能开发(后端)
- **任务名称**: AI组件开发 - AI智能聊天功能开发(后端)
- **任务描述**: 开发AI智能聊天后端服务，接收前端聊天请求，调用通义千问模型接口并返回结果
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建AiChatService.java服务类
  - [ ] 创建AiChatController.java控制器类
  - [ ] 实现聊天消息处理接口
  - [ ] 集成通义千问模型API
  - [ ] 实现对话上下文管理
- **注意事项**:
  - 考虑异步处理，避免长时间阻塞请求线程
  - 设计合理的请求和响应数据结构
  - 实现对话历史记录管理
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-system/jnpf-system-controller/src/main/java/jnpf/base/controller/AiChatController.java` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-system/jnpf-system-biz/src/main/java/jnpf/base/service/impl/AiChatServiceImpl.java` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-system/jnpf-system-entity/src/main/java/jnpf/base/entity/AiChatEntity.java` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/service/OpenAiService.java` (接口定义)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-common/jnpf-boot-common/jnpf-common-ai/src/main/java/jnpf/service/impl/DefaultOpenAiServiceImpl.java` (服务实现)

### TASK015: AI组件开发 - 前端API层封装
- **任务名称**: AI组件开发 - 前端API层封装
- **任务描述**: 创建前端API请求模块，统一管理所有与AI后端服务的API交互
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 在前端项目中创建ai.js文件
  - [ ] 实现extractPdfText函数
  - [ ] 实现sendChatMessage函数
  - [ ] 配置统一的请求处理和错误处理
- **注意事项**:
  - 统一处理API的错误信息
  - 使用Promise进行异步处理
  - 配置合理的超时时间
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-web-vue3/src/api/system/aiChat.ts` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-app/libs/chat.js` (移动端API封装)

### TASK016: AI组件开发 - PDF抽取界面开发(前端)
- **任务名称**: AI组件开发 - PDF抽取界面开发(前端)
- **任务描述**: 开发PDF内容抽取前端界面，允许用户上传PDF文件并展示抽取结果
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建PdfExtractor.vue组件
  - [ ] 实现文件上传区域
  - [ ] 实现加载状态显示
  - [ ] 实现文本内容展示区域
  - [ ] 实现文件类型和大小校验
- **注意事项**:
  - 使用Element Plus组件库
  - 提供友好的用户交互体验
  - 实现文件类型和大小的前端校验
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-web-vue3/src/components/FormGenerator/src/components/AiChatPopover.vue` (文件上传组件)

### TASK017: AI组件开发 - AI聊天界面开发(前端)
- **任务名称**: AI组件开发 - AI聊天界面开发(前端)
- **任务描述**: 开发AI智能聊天前端界面，实现对话、会话管理、历史记录等功能
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建AiChat.vue组件
  - [ ] 实现会话列表区域
  - [ ] 实现聊天记录区域
  - [ ] 实现消息输入区域
  - [ ] 支持会话管理功能
  - [ ] 支持消息复制和重新生成
- **注意事项**:
  - 设计合理的数据结构管理会话状态和聊天记录
  - 支持Markdown格式渲染
  - 提供良好的用户体验
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-web-vue3/src/layouts/default/header/components/AIChatModal.vue` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-app/store/modules/chat.js` (状态管理)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-app/libs/chat.js` (聊天功能实现)

### TASK018: AI组件开发 - AI表单生成功能(后端)
- **任务名称**: AI组件开发 - AI表单生成功能(后端)
- **任务描述**: 开发AI表单生成后端服务，根据业务需求描述自动生成表单结构
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建VisualAiController.java控制器
  - [ ] 创建VisualAiService.java服务类
  - [ ] 实现表单结构生成接口
  - [ ] 实现字段智能推荐功能
  - [ ] 实现多表关联设计
- **注意事项**:
  - 集成AI模型进行智能分析
  - 支持多种表单组件类型
  - 实现表单验证规则生成
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-visualdev/jnpf-visualdev-base/jnpf-visualdev-base-controller/src/main/java/jnpf/base/controller/VisualAiController.java` (直接复制)
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-java-boot/jnpf-visualdev/jnpf-visualdev-base/jnpf-visualdev-base-biz/src/main/java/jnpf/base/service/impl/VisualAiServiceImpl.java` (直接复制)

### TASK019: AI组件开发 - AI表单生成界面(前端)
- **任务名称**: AI组件开发 - AI表单生成界面(前端)
- **任务描述**: 开发AI表单生成前端界面，提供需求描述输入和表单结构展示
- **版本**: v1.0.0
- **状态**: 计划中
- **验收标准**:
  - [ ] 创建AiFormGenerator.vue组件
  - [ ] 实现需求描述输入区域
  - [ ] 实现表单结构预览区域
  - [ ] 实现表单组件配置界面
  - [ ] 支持表单导出功能
- **注意事项**:
  - 提供直观的表单设计界面
  - 支持实时预览和编辑
  - 实现表单组件的拖拽配置
- **参考文件**:
  - `JNPF-SpringBoot.V5.2.7-多租户/jnpf-web-vue3/src/components/FormGenerator/src/components/AiChatPopover.vue` (直接复制)

## 需要开发的接口清单

### AI聊天接口
- POST /api/ai/chat/send - 发送聊天消息
- GET /api/ai/chat/sessions - 获取会话列表
- POST /api/ai/chat/session - 创建新会话
- DELETE /api/ai/chat/session/{id} - 删除会话
- PUT /api/ai/chat/session/{id}/title - 更新会话标题

### PDF抽取接口
- POST /api/ai/pdf/extract - PDF文本抽取
- GET /api/ai/pdf/status/{id} - 获取抽取状态

### AI表单生成接口
- POST /api/ai/form/generate - 生成表单结构
- POST /api/ai/form/optimize - 优化表单组件
- GET /api/ai/form/templates - 获取表单模板

## 需要开发的组件清单

### 后端组件
1. **配置类**
   - AiProperties.java (参考JNPF)
   - AiAutoConfiguration.java (参考JNPF)
   - AiConstants.java (参考JNPF)

2. **服务类**
   - AiChatService.java (参考JNPF)
   - PdfExtractService.java (新增)
   - VisualAiService.java (参考JNPF)
   - OpenAiService.java (参考JNPF)

3. **控制器类**
   - AiChatController.java (参考JNPF)
   - PdfExtractController.java (新增)
   - VisualAiController.java (参考JNPF)

4. **实体类**
   - AiChatEntity.java (参考JNPF)
   - ChatMessage.java (新增)
   - ChatSession.java (新增)

5. **工具类**
   - AiLimitUtil.java (参考JNPF)

### 前端组件
1. **API层**
   - aiChat.ts (参考JNPF)
   - aiForm.ts (新增)

2. **页面组件**
   - AIChatModal.vue (参考JNPF)
   - AiChatPopover.vue (参考JNPF)
   - PdfExtractor.vue (新增)
   - AiFormGenerator.vue (新增)

3. **状态管理**
   - chat.js (参考JNPF)

## 需要修改的界面清单
1. **主界面**
   - 添加AI聊天入口
   - 集成AI表单生成功能
   - 添加PDF抽取功能入口

2. **导航菜单**
   - AI助手菜单项
   - PDF工具菜单项
   - 表单生成菜单项

## 需要隐藏的功能清单
1. **开发调试功能**
   - AI模型调试面板
   - 请求日志查看
   - 性能监控面板

2. **管理员功能**
   - AI配置管理
   - 使用量统计
   - 模型切换管理

## 开发总结记录

### 2024-01-XX 项目初始化
- **主要任务**: 项目基础架构搭建
- **完成内容**: 
  - 创建项目基础结构
  - 配置开发环境
  - 建立任务清单
- **技术栈**: Vue 3 + TypeScript + Spring Boot 2.7.x
- **修改文件**: 
  - 创建duijie/readme.md
  - 分析项目结构
- **经验沉淀**:
  - 参考JNPF框架进行架构设计
  - 采用前后端分离架构
  - 使用OnlyOffice进行文档处理

### 2024-01-XX AI组件开发规划
- **主要任务**: AI组件开发任务规划
- **完成内容**: 
  - 分析JNPF系统AI功能
  - 建立详细的参考文件清单
  - 制定开发任务计划
- **技术栈**: 基于JNPF AI模块
- **修改文件**: 
  - 更新duijie/readme.md
  - 添加AI组件开发任务
- **经验沉淀**:
  - 充分利用JNPF现有AI功能代码
  - 采用模块化开发方式
  - 保持代码的可复用性和可维护性 