# Swagger配置快速开始

## 🚀 5分钟快速配置指南

### 步骤1：查看配置文件

配置文件位置：`contract-tools-sdk/src/main/resources/application.yml`

当前配置（已配置好）：

```yaml
zxcm:
  swagger:
    enabled: true                      # ✅ 已启用Swagger
    require-password: true             # 🔐 需要密码访问
    password: zxcm                     # 当前密码：zxcm
    
    company:
      name: 肇新科技
      url: https://www.zhaoxinms.com
      email: develop@zhaoxinms.com
```

### 步骤2：启动应用

```bash
# Maven
mvn spring-boot:run

# 或者直接运行
java -jar contract-tools-sdk.jar
```

### 步骤3：访问Swagger文档

打开浏览器访问：

```
http://localhost:8080/swagger-ui.html
```

### 步骤4：输入密码

在弹出的精美登录页面输入密码：

```
密码：zxcm
```

点击"访问文档"即可进入！🎉

---

## 📋 不同环境的配置模板

### 开发环境（无密码）

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: false
```

### 测试/演示环境（有密码）

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: true
    password: Test@2024!Demo
```

### 生产环境（禁用）

```yaml
zxcm:
  swagger:
    enabled: false  # 建议完全禁用
```

---

## 🎨 自定义企业信息

只需修改配置文件中的企业信息部分：

```yaml
zxcm:
  swagger:
    company:
      name: ABC科技有限公司
      url: http://www.abc-tech.com
      email: api-support@abc-tech.com
    
    api:
      title: ABC产品API接口文档
      description: 这里写您的产品描述
      version: 2.0.0
      base-package: com.abc.yourproject
```

保存后重启应用即可看到效果！

---

## 🔒 修改访问密码

### 方法1：直接修改配置文件

```yaml
zxcm:
  swagger:
    password: YourNewPassword123!
```

### 方法2：环境变量（推荐）

```bash
# Linux/Mac
export ZXCM_SWAGGER_PASSWORD=YourNewPassword

# Windows
set ZXCM_SWAGGER_PASSWORD=YourNewPassword
```

---

## 💡 常用场景

### 场景1：开发团队内部使用（无密码）

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: false
```

开发团队可以直接访问，无需密码，提高开发效率。

### 场景2：给客户演示（需要密码）

```yaml
zxcm:
  swagger:
    enabled: true
    require-password: true
    password: Demo2024Client
```

然后告诉客户：
- 🌐 地址：http://your-server:8080/swagger-ui.html
- 🔑 密码：Demo2024Client

### 场景3：生产上线（完全禁用）

```yaml
zxcm:
  swagger:
    enabled: false
```

生产环境不暴露API文档，保护系统安全。

---

## ❓ 常见问题快速解答

### Q: 配置修改后未生效？

**A**: 重启应用即可。

```bash
# 停止应用（Ctrl+C）
# 重新启动
mvn spring-boot:run
```

### Q: 忘记密码了？

**A**: 打开 `application.yml`，查看 `password` 的值即可。

或者修改为新密码后重启。

### Q: 如何临时关闭Swagger？

**A**: 
```yaml
zxcm:
  swagger:
    enabled: false
```
重启应用。

### Q: 密码认证会过期吗？

**A**: 会的，关闭浏览器或30分钟无操作后需重新认证。

### Q: 可以在多个浏览器同时登录吗？

**A**: 可以，每个浏览器session独立。

---

## 🔧 开发建议

### ✅ 推荐做法

| 环境 | enabled | require-password | 说明 |
|------|---------|------------------|------|
| 开发环境 | `true` | `false` | 方便开发调试 |
| 测试环境 | `true` | `true` | 保护测试数据 |
| 预发布环境 | `true` | `true` | 演示给客户 |
| 生产环境 | `false` | - | 完全禁用 |

### ❌ 不推荐做法

- ❌ 生产环境开启且不设密码
- ❌ 使用弱密码（如：123456）
- ❌ 所有环境使用相同密码
- ❌ 将密码提交到Git仓库

---

## 📊 访问日志查看

启动应用后，查看控制台日志：

```
✅ 2024-10-18 10:30:15 INFO  - Swagger文档访问认证成功，来自IP: 192.168.1.100
❌ 2024-10-18 10:31:20 WARN  - Swagger文档访问认证失败（密码错误），来自IP: 192.168.1.101
```

通过日志可以：
- 追踪谁访问了文档
- 发现异常访问尝试
- 进行安全审计

---

## 🎯 下一步

配置完成后，您可以：

1. ✅ 浏览所有API接口
2. ✅ 在线测试API
3. ✅ 查看请求/响应示例
4. ✅ 导出API文档

---

## 📞 需要帮助？

如果遇到问题：

1. 查看 [SWAGGER_CONFIG.md](SWAGGER_CONFIG.md) 完整配置文档
2. 检查配置文件格式是否正确
3. 确认端口是否被占用
4. 查看应用启动日志
5. 联系技术支持：develop@zhaoxinms.com

---

## 🌟 提示

### 密码保护的登录页面非常精美！

特点：
- 🎨 渐变背景
- 💫 现代化设计
- 📱 响应式布局
- 🔒 安全提示

快去试试吧！🚀

---

祝使用愉快！🎉

**肇新科技** | https://www.zhaoxinms.com

