# 快速开始指南

## 🚀 首次启动前的准备

### 必需配置项

在启动应用前，请确保已配置以下内容：

#### 1. ✅ 阿里云API密钥（必需）

合同提取功能需要阿里云通义千问API。

**配置文件位置**：`contract-tools-sdk/src/main/resources/application.yml`

```yaml
zhaoxin:
  extract:
    aliyun:
      api-key: your-aliyun-api-key-here  # ⚠️ 请替换为真实的API Key
      model: qwen-plus
```

**获取API Key**：
1. 访问 https://dashscope.console.aliyun.com/apiKey
2. 登录阿里云账号
3. 创建并复制API Key
4. 替换配置文件中的 `your-aliyun-api-key-here`

📖 **详细配置说明**：请查看 [ALIYUN_CONFIG.md](ALIYUN_CONFIG.md)

#### 2. ✅ Swagger访问密码（可选）

Swagger API文档已启用密码保护。

**当前密码**：`zxcm`

**访问地址**：http://localhost:8080/swagger-ui.html

**修改密码**：编辑 `application.yml`

```yaml
zxcm:
  swagger:
    require-password: true
    password: zxcm  # 修改此处
```

📖 **详细配置说明**：请查看 [SWAGGER_CONFIG.md](SWAGGER_CONFIG.md)

---

## 📋 启动步骤

### 步骤1：配置API密钥

```bash
# 编辑配置文件
vim contract-tools-sdk/src/main/resources/application.yml

# 或者使用环境变量（推荐）
export ZHAOXIN_EXTRACT_ALIYUN_API_KEY="your-real-api-key"
```

### 步骤2：编译项目

```bash
mvn clean install -DskipTests
```

### 步骤3：启动应用

```bash
cd contract-tools-sdk
mvn spring-boot:run
```

或者：

```bash
java -jar contract-tools-sdk/target/contract-tools-sdk-1.0.0.jar
```

### 步骤4：验证启动

如果看到以下日志，说明启动成功：

```
2024-10-18 16:30:00 [INFO] - Started SdkApplication in 8.5 seconds
```

访问：http://localhost:8080/swagger-ui.html

---

## 🔍 常见启动错误

### ❌ 错误1：API Key未配置

```
Could not resolve placeholder 'zhaoxin.extract.aliyun.api-key'
```

**解决方案**：
1. 检查 `application.yml` 中是否配置了 `zhaoxin.extract.aliyun.api-key`
2. 确认配置值不是 `your-aliyun-api-key-here`
3. 或使用环境变量 `ZHAOXIN_EXTRACT_ALIYUN_API_KEY`

### ❌ 错误2：Bean名称冲突

```
ConflictingBeanDefinitionException: webMvcConfig
```

**解决方案**：
- 此问题已修复，确保使用最新代码
- SDK模块使用 `SwaggerWebMvcConfig`
- API模块使用 `WebMvcConfig`

### ❌ 错误3：端口被占用

```
Port 8080 was already in use
```

**解决方案**：
1. 修改端口：在 `application.yml` 中设置 `server.port: 8081`
2. 或关闭占用8080端口的程序

### ❌ 错误4：数据库连接失败

```
Cannot create PoolableConnectionFactory
```

**解决方案**：
1. 检查MySQL是否启动
2. 验证数据库配置：`application.yml` 中的 `spring.datasource`
3. 确认数据库 `contract_tools` 已创建

---

## 🎯 功能验证

### 1. 验证Swagger

访问：http://localhost:8080/swagger-ui.html

输入密码：`zxcm`

应该看到完整的API文档。

### 2. 测试合同提取

```bash
curl -X POST http://localhost:8080/api/contract/extract/upload \
  -F "file=@test.pdf" \
  -F "ignoreHeaderFooter=true" \
  -H "Content-Type: multipart/form-data"
```

成功返回：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "taskId": "xxx-xxx-xxx"
  }
}
```

### 3. 测试GPU OCR比对

```bash
curl -X POST http://localhost:8080/api/compare-pro/submit \
  -F "oldFile=@old.pdf" \
  -F "newFile=@new.pdf"
```

---

## 📊 项目结构

```
contract-tools-sdk/
├── src/main/
│   ├── java/
│   │   └── com/zhaoxinms/contract/
│   │       ├── template/sdk/        # SDK核心
│   │       │   └── config/          # 配置类
│   │       │       ├── SwaggerConfig.java
│   │       │       ├── SwaggerInterceptor.java
│   │       │       ├── SwaggerProperties.java
│   │       │       └── SwaggerWebMvcConfig.java
│   │       └── tools/               # 工具模块
│   │           ├── api/             # API基础
│   │           ├── comparePRO/      # GPU OCR比对
│   │           ├── extract/         # 合同提取
│   │           └── ocr/             # OCR功能
│   └── resources/
│       └── application.yml          # 主配置文件
├── ALIYUN_CONFIG.md                 # 阿里云配置说明
├── SWAGGER_CONFIG.md                # Swagger配置说明
├── SWAGGER_QUICKSTART.md            # Swagger快速开始
└── GETTING_STARTED.md               # 本文档
```

---

## 🔧 开发建议

### 开发环境配置

```yaml
# application-dev.yml
zhaoxin:
  extract:
    aliyun:
      api-key: ${ALIYUN_API_KEY}  # 使用环境变量
      model: qwen-turbo  # 开发环境使用更经济的模型

zxcm:
  swagger:
    enabled: true
    require-password: false  # 开发环境不需要密码

logging:
  level:
    com.zhaoxinms: DEBUG  # 详细日志
```

启动时指定profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产环境配置

```yaml
# application-prod.yml
zhaoxin:
  extract:
    aliyun:
      api-key: ${ALIYUN_API_KEY}  # 必须使用环境变量
      model: qwen-plus

zxcm:
  swagger:
    enabled: false  # 生产环境禁用Swagger

logging:
  level:
    com.zhaoxinms: INFO  # 正常日志级别
```

---

## 📞 获取帮助

### 配置问题

- [Swagger配置](SWAGGER_CONFIG.md)
- [阿里云API配置](ALIYUN_CONFIG.md)

### 技术支持

- 企业网址：https://www.zhaoxinms.com
- 技术支持：develop@zhaoxinms.com
- 问题反馈：提交Issue到项目仓库

---

## ✅ 启动检查清单

完成以下检查后再启动：

- [ ] 已安装JDK 8或更高版本
- [ ] 已安装Maven 3.6+
- [ ] 已安装MySQL并创建数据库
- [ ] **已配置阿里云API密钥**
- [ ] 已确认8080端口未被占用
- [ ] 已执行 `mvn clean install`
- [ ] 配置文件格式正确（YAML缩进）

全部完成后，执行：

```bash
mvn spring-boot:run
```

🎉 **祝您使用愉快！**

