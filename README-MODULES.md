# 肇新合同工具集 - 多模块架构说明

## 项目结构

本项目已重构为多模块Maven项目，支持灵活的部署和使用方式：

```
zhaoxin-contract-tool-set/
├── pom.xml                    # 父级POM，管理依赖版本和子模块
├── contract-tools-core/       # 核心业务库
├── contract-tools-auth/       # 授权管理模块（可选）
├── contract-tools-backend/    # 独立Spring Boot后端应用
├── contract-tools-sdk/        # SDK应用（原sdk项目的重构版）
└── frontend/                  # 前端项目
```

## 模块说明

### 1. contract-tools-core（核心库）
- **用途**：包含所有核心业务逻辑
- **特点**：
  - 纯Java库，不包含Spring Boot启动配置
  - 可以被其他项目作为jar依赖使用
  - 包含：文档处理、OCR比较、OnlyOffice集成、PDF处理等核心功能
- **打包方式**：普通jar包

### 2. contract-tools-auth（授权管理模块）
- **用途**：提供基础的授权码检查和功能授权管理
- **特点**：
  - 可选模块，通过配置启用/禁用
  - 不依赖Spring Security，轻量级实现
  - 支持功能级别的授权检查
  - 提供注解式授权验证（@RequireFeature）
  - 自动配置，零侵入集成
- **打包方式**：普通jar包

### 3. contract-tools-backend（独立后端应用）
- **用途**：独立运行的Spring Boot应用
- **特点**：
  - 整合core和auth模块
  - 可以独立部署和运行
  - 支持两种模式：简单模式（仅核心功能）和完整模式（包含授权）
- **打包方式**：可执行jar包

### 4. contract-tools-sdk（SDK应用）
- **用途**：合同模板设计SDK，包含AI功能和数据库集成
- **特点**：
  - 灵活的依赖配置，可选择依赖core或完整backend
  - 包含AI组件和数据库操作
  - 可以作为独立应用运行

## 使用场景

### 场景1：作为jar库使用（推荐给其他项目集成）

在其他项目的pom.xml中添加：

```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 可选：如果需要授权功能 -->
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-auth</artifactId>
    <version>1.0.0</version>
</dependency>
```

然后在Spring Boot应用中启用组件扫描：

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.yourproject", 
    "com.zhaoxinms.contract.tools"
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 场景2：独立后端应用运行

```bash
# 编译整个项目
mvn clean package

# 运行独立后端应用
cd contract-tools-backend
java -jar target/contract-tools-backend-1.0.0.jar

# 或使用Spring Boot插件运行
mvn spring-boot:run
```

配置授权功能（在application.yml中）：

```yaml
zhaoxin:
  auth:
    enabled: true  # 启用授权功能
    jwt:
      secret: your-secret-key
      expiration: 86400
```

### 场景3：SDK应用运行

```bash
# 运行SDK应用
cd contract-tools-sdk
mvn spring-boot:run

# 或编译后运行
mvn clean package
java -jar target/contract-tools-sdk-1.0.0.jar
```

## 编译和部署

### 编译整个项目

```bash
# 在根目录下执行
mvn clean package
```

### 编译单个模块

```bash
# 编译核心库
cd contract-tools-core
mvn clean package

# 编译后端应用
cd contract-tools-backend
mvn clean package
```

### 安装到本地仓库

```bash
# 安装所有模块到本地Maven仓库
mvn clean install
```

## 配置说明

### 核心库配置
核心库本身不需要特殊配置，但使用时可能需要配置：

- OnlyOffice服务地址
- OCR服务配置
- 文件存储路径

### 授权模块配置

**默认情况下授权模块是禁用的**，系统会跳过所有授权检查。如需启用授权功能：

```yaml
zhaoxin:
  auth:
    enabled: true              # 启用授权功能
    license:
      code: "your-license-code"           # 授权码
      expiration: 1735689600000           # 授权过期时间（毫秒时间戳）
      features: ["*"]                     # 授权的功能列表，"*"表示所有功能
      max-users: -1                       # 最大用户数，-1表示无限制
```

**重要说明**：
- 当 `zhaoxin.auth.enabled=false` 或未配置时，系统会：
  - 跳过所有授权检查
  - 自动配置CORS支持，允许跨域访问
  - 所有API接口无需授权即可访问
- 当 `zhaoxin.auth.enabled=true` 时，系统会：
  - 启用授权码检查
  - 支持功能级别的授权控制
  - 提供 `@RequireFeature` 注解进行方法级授权检查

**使用示例**：
```java
@RestController
public class MyController {
    
    @RequireFeature("advanced-ocr")
    @PostMapping("/advanced-compare")
    public Result advancedCompare() {
        // 需要 "advanced-ocr" 功能授权
        return Result.success();
    }
}
```

### 数据库配置（SDK和后端应用）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/contract_tools
    username: root
    password: password
```

## 迁移指南

### 从原backend项目迁移

1. 原backend项目的代码已迁移到contract-tools-core
2. 如需独立运行，使用contract-tools-backend
3. 原有的Controller可能需要调整包扫描路径

### 从原sdk项目迁移

1. 代码已迁移到contract-tools-sdk
2. 依赖关系已优化，减少重复依赖
3. 启动类已更新，支持模块化架构

## 开发建议

1. **新功能开发**：优先在contract-tools-core中开发核心功能
2. **授权相关**：在contract-tools-auth中开发
3. **应用配置**：在contract-tools-backend或contract-tools-sdk中配置
4. **版本管理**：在父POM中统一管理依赖版本

## 故障排除

### 常见问题

1. **包扫描问题**：确保@ComponentScan包含正确的包路径
2. **依赖冲突**：检查父POM的依赖管理
3. **启动失败**：检查数据库连接和配置文件

### 日志配置

在application.yml中配置日志级别：

```yaml
logging:
  level:
    com.zhaoxinms: DEBUG
    org.springframework: INFO
```
