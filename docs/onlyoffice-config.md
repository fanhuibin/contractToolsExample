# OnlyOffice 配置说明

## 概述
本文档说明 OnlyOffice 集成模块的配置项和权限设置。

## 配置文件位置
配置文件位于 `backend/src/main/resources/application.yml`

## 基础配置

```yaml
onlyoffice:
  domain: http://localhost          # OnlyOffice 服务器域名
  port: 80                          # OnlyOffice 服务器端口
  callbackUrl: http://localhost:8080/api/onlyoffice/callback  # 回调地址
  secret: your-secret-key-here      # JWT 签名密钥
```

## 权限配置

### 查看模式权限
```yaml
onlyoffice:
  permissions:
    view:
      print: true  # 是否允许查看模式下打印
```

### 编辑模式权限
```yaml
onlyoffice:
  permissions:
    edit:
      print: true                    # 是否允许编辑模式下打印
      download: true                 # 是否允许下载
      comment: true                  # 是否允许评论
      chat: true                     # 是否允许聊天
      review: true                   # 是否允许审阅
      fillForms: true                # 是否允许填写表单
      modifyContentControl: true     # 是否允许修改内容控件
      modifyFilter: true             # 是否允许修改过滤器
```

## 权限说明

### 查看模式 (View Mode)
- **print**: 控制用户是否可以在查看模式下打印文档
- 其他权限在查看模式下默认关闭

### 编辑模式 (Edit Mode)
- **print**: 控制用户是否可以在编辑模式下打印文档
- **download**: 控制用户是否可以下载文档
- **comment**: 控制用户是否可以添加评论
- **chat**: 控制用户是否可以使用聊天功能
- **review**: 控制用户是否可以进行审阅操作
- **fillForms**: 控制用户是否可以填写表单
- **modifyContentControl**: 控制用户是否可以修改内容控件
- **modifyFilter**: 控制用户是否可以修改过滤器

## 配置示例

### 严格权限配置
```yaml
onlyoffice:
  permissions:
    view:
      print: false  # 禁止查看模式下打印
    edit:
      print: true
      download: false  # 禁止下载
      comment: true
      chat: false      # 禁止聊天
      review: true
      fillForms: true
      modifyContentControl: false  # 禁止修改内容控件
      modifyFilter: false          # 禁止修改过滤器
```

### 宽松权限配置
```yaml
onlyoffice:
  permissions:
    view:
      print: true   # 允许查看模式下打印
    edit:
      print: true
      download: true
      comment: true
      chat: true
      review: true
      fillForms: true
      modifyContentControl: true
      modifyFilter: true
```

## 注意事项

1. **安全性**: 根据实际业务需求设置权限，避免过度开放权限
2. **兼容性**: 某些权限设置可能影响 OnlyOffice 的某些功能
3. **动态配置**: 配置修改后需要重启应用才能生效
4. **默认值**: 所有权限默认值为 `true`，可以通过配置文件覆盖

## 相关代码

权限配置在 `DefaultFileConfigurer` 类中实现，通过 `@Value` 注解注入配置值：

```java
@Value("${onlyoffice.permissions.view.print:true}")
private boolean viewPrintEnabled;

@Value("${onlyoffice.permissions.edit.print:true}")
private boolean editPrintEnabled;
// ... 其他权限配置
```

## 回调处理

OnlyOffice 支持两种保存回调：

### 1. 普通保存回调 (SaveCallback)
- 处理用户主动保存文档的回调
- 文件保存到 `./uploads/` 目录下
- 使用本系统的 `FileInfoService` 管理文件信息

### 2. 强制保存回调 (ForcesaveCallback)
- 处理系统强制保存文档的回调
- 当用户编辑文档但未主动保存时触发
- 同样保存到 `./uploads/` 目录下

### 回调处理流程
1. 接收 OnlyOffice 服务器的保存请求
2. 根据文件ID获取文件信息
3. 从 OnlyOffice 服务器下载最新版本的文件
4. 使用 `FileInfoService.updateFileContent()` 更新文件内容
5. 更新文件信息（修改时间等）
6. 记录操作日志

### 文件处理方式

回调处理使用本系统的文件管理功能：

1. **文件信息管理**：通过 `FileInfoService` 获取和管理文件信息
2. **文件内容更新**：使用 `FileInfoService.updateFileContent()` 方法更新文件内容
3. **统一存储路径**：文件保存在配置的 `file.upload.root-path` 目录下
4. **自动更新元数据**：文件更新后自动更新修改时间和操作人信息

### 相关服务方法

```java
// 更新文件内容
boolean updateFileContent(Long fileId, InputStream inputStream) throws IOException;

// 获取文件信息
FileInfo getById(Long fileId);
``` 