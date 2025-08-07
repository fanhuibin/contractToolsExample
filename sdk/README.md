# 合同模板设计SDK

## 概述

合同模板设计SDK是一个Spring Boot项目，它实现了`TemplateService`接口，为合同模板设计系统提供字段信息查询功能。SDK项目直接引用backend项目的jar包，实现了模板设计的核心业务逻辑。

## 功能特性

- 实现TemplateService接口，提供字段信息查询
- 支持基础字段、相对方字段和条款字段的配置
- 支持根据模板ID返回不同的字段配置
- 作为独立的Spring Boot应用运行
- 与backend项目无缝集成

## 依赖要求

- Java 11+
- Spring Boot 2.7.18
- contract-tools-backend 1.0.0

## 快速开始

### 1. 编译backend项目

首先需要编译backend项目并安装到本地Maven仓库：

```bash
cd backend
mvn clean install
```

### 2. 运行SDK项目

```bash
cd sdk
mvn spring-boot:run
```

### 3. 基本使用

SDK项目实现了`TemplateService`接口，提供以下功能：

```java
@Service
public class TemplateServiceImpl implements TemplateService {
    
    @Override
    public FieldResponse getFields() {
        // 返回所有字段信息
    }
    
    @Override
    public FieldResponse getFieldsByTemplateId(String templateId) {
        // 根据模板ID返回特定字段配置
    }
}
```

## API参考

### TemplateService

核心服务接口，需要由SDK项目实现。

#### 方法

##### getFields()

获取所有字段信息。

```java
public FieldResponse getFields()
```

**返回**: `FieldResponse` - 包含基础字段、相对方字段和条款字段的响应对象

##### getFieldsByTemplateId(String templateId)

根据模板ID获取字段信息。

```java
public FieldResponse getFieldsByTemplateId(String templateId)
```

**参数**:
- `templateId`: 模板ID

**返回**: `FieldResponse` - 特定模板的字段配置

### 字段配置

#### 基础字段 (BaseField)
- 合同名称 (contract_name)
- 合同描述 (contract_description) - 支持富文本
- 合同金额 (contract_amount)

#### 相对方字段 (CounterpartyField)
- 甲方名称 (party_a_name)
- 乙方名称 (party_b_name)
- 甲方地址 (party_a_address)
- 乙方地址 (party_b_address)

#### 条款字段 (ClauseField)
- 通用条款 - 支持表达式语法
- 付款条款 - 支持金额字段引用
- 地址条款 - 支持地址字段引用

## 错误处理

SDK项目使用Spring Boot的异常处理机制。如果TemplateService实现出现问题，会抛出相应的异常。

## 最佳实践

1. **接口实现**: 确保正确实现TemplateService接口的所有方法
2. **字段配置**: 根据业务需求合理配置字段信息
3. **模板管理**: 支持多种模板配置，满足不同场景需求
4. **表达式语法**: 在条款内容中正确使用字段表达式

## 示例项目

完整的示例代码请参考 `src/main/java/com/zhaoxinms/contract/template/sdk/example/TemplateDesignExample.java`。

## 部署说明

1. **开发环境**: 直接运行`SdkApplication`主类
2. **生产环境**: 打包成jar文件部署
3. **端口配置**: 默认端口8081，可通过`application.yml`修改

## 许可证

本项目采用Apache License 2.0许可证。 