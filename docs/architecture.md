# 合同模板设计系统架构说明

## 项目概述

合同模板设计系统采用模块化架构，包含三个主要项目：

1. **backend** - 后台基础jar包
2. **sdk** - 主要Spring Boot应用，与Frontend配合
3. **frontend** - 前端界面，与SDK配合

## 架构设计

### 1. Backend项目 (contract-tools-backend)

**定位**: 后台基础jar包，提供核心服务和工具类

**功能**:
- 文件管理服务
- OnlyOffice集成工具类
- 回调处理机制
- 通用DTO和工具类
- 统一配置管理（zxcm前缀）

**技术栈**:
- Java 11
- Maven
- 纯jar包，无Spring Boot依赖

**部署方式**:
- 打包成jar包作为依赖被SDK项目引用
- 不独立运行，只提供基础服务
- 配置通过SDK项目传递

**关键组件**:
- `ZxcmConfig` - 统一配置类（zxcm前缀）
- `TemplateService` - 模板服务接口
- `TemplateDesignService` - 模板设计服务
- `FileInfoService` - 文件信息服务（简化版，只包含核心功能）
- OnlyOffice相关工具类和回调处理

### 2. SDK项目 (contract-template-sdk)

**定位**: 主要Spring Boot应用，与Frontend配合实现功能

**功能**:
- 实现`TemplateService`接口
- 提供REST API接口
- 调用OnlyOffice服务
- 处理OnlyOffice回调
- 与Frontend配合实现完整功能

**技术栈**:
- Spring Boot 2.7.18
- Java 11
- Maven

**依赖关系**:
- 直接引用backend项目的jar包
- 实现backend定义的接口
- 与Frontend配合

**核心实现**:
- `TemplateServiceImpl` - 模板服务实现类
- `TemplateDesignController` - 模板设计API控制器
- `OnlyOfficeController` - OnlyOffice控制器
- `OnlyofficeCallbackController` - OnlyOffice回调控制器

### 3. Frontend项目

**定位**: 用户界面，与SDK项目配合使用

**功能**:
- OnlyOffice编辑器界面
- 模板设计操作界面
- 文件管理界面
- 调用SDK项目的API接口

**技术栈**:
- Vue 3
- Element Plus
- TypeScript

## 模块关系

```
┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │      SDK        │
│   (Vue 3)       │◄──►│ (Spring Boot)   │
└─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  OnlyOffice     │
                       │  Document       │
                       │  Server         │
                       └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │    Backend      │
                       │   (JAR包)       │
                       └─────────────────┘
```

## 数据流

### 1. 模板设计流程

1. **Frontend** 发起模板设计请求
2. **SDK** 接收请求，调用TemplateService
3. **SDK** 返回字段配置信息
4. **SDK** 构建OnlyOffice编辑器配置
5. **Frontend** 打开OnlyOffice编辑器
6. **OnlyOffice** 处理文档编辑
7. **SDK** 接收OnlyOffice回调，保存文档

### 2. 字段查询流程

1. **Frontend** 请求字段信息
2. **SDK** 调用TemplateService.getFields()
3. **SDK** 返回字段配置给Frontend

### 3. OnlyOffice调用流程

1. **Frontend** 请求OnlyOffice编辑器配置
2. **SDK** 调用Backend的工具类生成配置
3. **SDK** 返回配置给Frontend
4. **Frontend** 使用配置打开OnlyOffice编辑器

## 部署模式

### 当前实现模式

```
Backend项目
├── 打包成jar包
├── 提供基础服务和工具类
└── 不独立运行

SDK项目
├── 依赖backend.jar
├── 实现TemplateService接口
├── 提供REST API
├── 调用OnlyOffice
└── 与Frontend配合

Frontend项目
├── 调用SDK的API
├── 集成OnlyOffice编辑器
└── 提供用户界面
```

## 配置说明

### SDK配置（主要配置）

```yaml
server:
  port: 8081

spring:
  application:
    name: contract-template-sdk

# 肇新合同工具集配置（zxcm前缀）
zxcm:
  onlyoffice:
    domain: localhost
    port: 80
    callback:
      url: http://localhost:8081/onlyoffice/callback
    plugins: []
  file:
    upload:
      root-path: ./uploads
```

## 开发指南

### 1. 开发环境搭建

1. 编译backend项目：
   ```bash
   cd backend
   mvn clean install
   ```

2. 运行SDK项目（主要应用）：
   ```bash
   cd sdk
   mvn spring-boot:run
   ```

3. 运行Frontend项目：
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### 2. 扩展开发

#### 添加新字段类型

1. 在backend项目中定义DTO
2. 在SDK项目中实现字段逻辑
3. 在frontend项目中添加界面支持

#### 添加新模板类型

1. 在SDK项目的`TemplateServiceImpl`中添加模板配置
2. 实现`getFieldsByTemplateId`方法的模板逻辑
3. 在frontend项目中添加模板选择界面

### 3. 测试

- Backend API测试：使用Swagger UI
- SDK功能测试：运行示例程序
- 集成测试：启动完整系统进行端到端测试

## 注意事项

1. **依赖管理**: SDK项目必须正确引用backend的jar包
2. **接口实现**: SDK必须完整实现TemplateService接口
3. **配置同步**: 确保各项目的配置参数一致
4. **版本兼容**: 注意backend和SDK的版本兼容性
5. **错误处理**: 实现完善的异常处理机制

## 未来规划

1. **独立Spring Boot模式**: 支持backend和SDK完全独立运行
2. **微服务架构**: 考虑拆分为多个微服务
3. **数据库集成**: 添加持久化存储
4. **权限管理**: 实现用户认证和授权
5. **监控告警**: 添加系统监控和日志 