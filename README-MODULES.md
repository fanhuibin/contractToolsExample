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
- **用途**：提供完整的License授权验证和模块级权限控制
- **特点**：
  - 可选模块，通过配置启用/禁用
  - 不依赖Spring Security，轻量级实现
  - 支持6个核心业务模块的独立授权控制
  - 数字签名验证，确保License安全性
  - 硬件绑定支持（MAC地址、CPU序列号、主板序列号）
  - 提供注解式授权验证（@RequireFeature）
  - AOP切面自动拦截未授权访问
  - REST API接口查询授权状态
  - 命令行工具生成License文件
- **支持模块**：合同模板设计、合同合成、合同比对PRO、合同信息抽取、合同智能审核、履约任务生成
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
    enabled: true                    # 启用授权功能
    license:
      filePath: "license.dat"        # License文件路径
      hardwareBound: true            # 是否启用硬件绑定
    signature:
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."  # 公钥内容
      # 或者使用文件路径
      publicKeyPath: "public.key"
```

**支持的业务模块**：

| 模块代码 | 模块名称 | 主要功能 |
|---------|---------|---------|
| `contract_template_design` | 合同模板设计模块 | 模板创建、编辑、预览 |
| `contract_synthesis` | 合同合成模块 | 合同生成、合并、数据绑定 |
| `contract_compare_pro` | 合同比对PRO模块 | GPU OCR比对、智能差异分析 |
| `contract_info_extraction` | 合同信息抽取模块 | 实体识别、关键信息提取 |
| `contract_intelligent_review` | 合同智能审核模块 | 风险分析、合规检查 |
| `performance_task_generation` | 履约任务生成模块 | 任务生成、调度、提醒 |

**重要说明**：
- 当 `zhaoxin.auth.enabled=false` 或未配置时，系统会：
  - 跳过所有授权检查
  - 所有API接口无需授权即可访问
- 当 `zhaoxin.auth.enabled=true` 时，系统会：
  - 启用License文件验证
  - 支持模块级别的授权控制
  - 数字签名验证确保License安全性
  - 硬件绑定防止License被复制使用

**使用示例**：

控制器级别授权：
```java
@RestController
@RequestMapping("/api/template")
@RequireFeature(module = ModuleType.CONTRACT_TEMPLATE_DESIGN, message = "合同模板设计模块需要授权")
public class TemplateController {
    // 整个控制器需要模板设计模块权限
}
```

方法级别授权：
```java
@RestController
public class ContractController {
    
    @RequireFeature(module = ModuleType.CONTRACT_COMPARE_PRO, message = "合同比对功能需要授权")
    @PostMapping("/compare")
    public ResponseEntity<?> compareContract() {
        // 需要合同比对PRO模块权限
        return ResponseEntity.ok("比对结果");
    }
}
```

编程式检查：
```java
@Service
public class BusinessService {
    
    @Autowired
    private LicenseService licenseService;
    
    public void processContract() {
        if (!licenseService.hasModulePermission(ModuleType.CONTRACT_TEMPLATE_DESIGN)) {
            throw new RuntimeException("无合同模板设计模块权限");
        }
        // 执行业务逻辑
    }
}
```

**License生成工具**：
```bash
# 使用命令行工具
java -cp contract-tools-auth-1.0.0.jar com.zhaoxinms.contract.tools.auth.generator.LicenseGeneratorCLI
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
