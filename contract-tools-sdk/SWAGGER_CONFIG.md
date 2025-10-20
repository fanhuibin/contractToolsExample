# Swagger API文档配置说明

## 概述

SDK项目已集成Swagger API文档功能，支持企业信息自定义、启用开关和密码保护等特性。

## 功能特性

✅ **启用开关**：可通过配置文件控制是否启用Swagger文档  
✅ **企业信息**：支持自定义企业名称、网址、联系邮箱  
✅ **密码保护**：可选的密码访问机制，保护API文档安全  
✅ **美观界面**：提供美观的登录认证页面  
✅ **IP日志**：记录访问者IP地址，便于审计  

## 配置说明

### 1. 配置文件位置

`contract-tools-sdk/src/main/resources/application.yml`

### 2. 配置参数详解

```yaml
zxcm:
  swagger:
    # 是否启用Swagger文档（生产环境建议设置为false）
    enabled: true
    
    # 是否需要密码访问Swagger文档
    require-password: true
    
    # Swagger访问密码（当require-password=true时生效）
    password: zxcm
    
    # 企业信息配置
    company:
      name: 肇新科技          # 企业名称
      url: https://www.zhaoxinms.com    # 企业网址
      email: develop@zhaoxinms.com      # 联系邮箱
    
    # API文档信息
    api:
      title: 肇新合同工具集API文档    # API文档标题
      description: 提供合同比对、模板设计、规则提取等功能的RESTful API接口
      version: 1.0.0                    # API版本号
      base-package: com.zhaoxinms.contract  # 扫描的基础包路径
```

## 使用场景

### 场景1：开发环境（完全开放）

适用于开发和测试阶段，无需密码即可访问。

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: false
```

访问地址：
- Swagger UI: http://localhost:8080/swagger-ui.html
- API文档: http://localhost:8080/v2/api-docs

### 场景2：演示环境（密码保护）

适用于给客户演示或内部评审，需要密码访问。

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: true
    password: your-secure-password
```

访问方式：
1. 浏览器访问 Swagger UI 地址
2. 在弹出的认证页面输入配置的密码
3. 认证成功后可正常访问文档

### 场景3：生产环境（完全禁用）

生产环境建议完全禁用Swagger，避免API泄露。

```yaml
zxcm:
  swagger:
    enabled: false
```

此时访问Swagger相关地址将返回404。

## 企业定制

您可以根据自己的企业信息修改配置：

```yaml
zxcm:
  swagger:
    company:
      name: 您的企业名称
      url: http://www.yourcompany.com
      email: support@yourcompany.com
    
    api:
      title: 您的产品API文档
      description: 您的产品功能描述
      version: 2.0.0
      base-package: com.yourcompany.yourproject
```

修改后重启应用即可生效。

## 安全建议

### 1. 密码设置

如果启用密码保护，请使用强密码：

```yaml
# ❌ 不推荐
password: 123456

# ✅ 推荐
password: SwG@2024#Secure!Pass
```

### 2. 生产环境配置

生产环境强烈建议：

```yaml
zxcm:
  swagger:
    enabled: false  # 完全禁用
```

或者如果必须保留：

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: true
    password: 使用极强的密码
```

### 3. IP白名单（可选扩展）

未来可以扩展IP白名单功能，只允许特定IP访问。

## 访问日志

系统会自动记录Swagger访问日志，包括：

- ✅ 认证成功：`Swagger文档访问认证成功，来自IP: xxx.xxx.xxx.xxx`
- ❌ 认证失败：`Swagger文档访问认证失败（密码错误），来自IP: xxx.xxx.xxx.xxx`
- 📋 需要认证：`Swagger文档访问需要密码认证，来自IP: xxx.xxx.xxx.xxx`

日志文件位置根据您的日志配置而定。

## 技术实现

### 核心类说明

| 类名 | 说明 |
|-----|------|
| `SwaggerProperties.java` | 配置属性类，读取yml配置 |
| `SwaggerConfig.java` | Swagger核心配置类 |
| `SwaggerInterceptor.java` | 访问拦截器，实现密码保护 |
| `WebMvcConfig.java` | 注册拦截器 |

### 拦截路径

系统会拦截以下路径：

- `/swagger-ui/**`
- `/swagger-ui.html`
- `/doc.html`
- `/swagger-resources/**`
- `/v2/api-docs`
- `/v3/api-docs`
- `/webjars/**`

## 快速开始

### 1. 修改配置（可选）

编辑 `application.yml`，根据需要修改配置。

### 2. 启动应用

```bash
mvn spring-boot:run
```

### 3. 访问文档

打开浏览器访问：

```
http://localhost:8080/swagger-ui.html
```

如果启用了密码保护，输入配置的密码即可。

## 常见问题

### Q1: 修改配置后未生效？

**A**: 修改配置文件后需要重启应用才能生效。

### Q2: 忘记密码怎么办？

**A**: 修改`application.yml`中的`password`参数，然后重启应用。

### Q3: 如何查看当前配置？

**A**: 查看日志启动信息，或直接查看`application.yml`文件。

### Q4: 密码认证后多久失效？

**A**: 基于Session实现，默认随Session失效而失效（通常是关闭浏览器或30分钟无操作）。

### Q5: 可以针对不同API设置不同权限吗？

**A**: 当前版本是全局控制，未来可以扩展细粒度权限控制。

### Q6: 如何临时关闭Swagger？

**A**: 设置 `enabled: false`，重启应用即可。

## 更新日志

### v1.0.0 (2024-10-18)

- ✅ 实现Swagger启用开关
- ✅ 实现企业信息自定义
- ✅ 实现密码保护功能
- ✅ 实现美观的认证页面
- ✅ 实现访问日志记录
- ✅ 实现IP地址追踪

## 联系支持

如有问题，请联系：

- 企业网址：https://www.zhaoxinms.com
- 技术支持：develop@zhaoxinms.com

