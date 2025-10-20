# 合同工具集 - 授权管理模块

## 概述

授权管理模块提供了完整的License授权验证功能，支持模块级别的权限控制、硬件绑定、数字签名验证等特性。

## 功能特性

- ✅ **模块级别授权**: 支持6个核心业务模块的独立授权控制
- ✅ **硬件绑定**: 支持MAC地址、CPU序列号、主板序列号绑定
- ✅ **数字签名**: 使用RSA数字签名确保License文件安全
- ✅ **灵活配置**: 支持配置文件和注解两种授权控制方式
- ✅ **AOP切面**: 自动拦截未授权的方法调用
- ✅ **REST API**: 提供HTTP接口查询授权状态

## 支持的模块

| 模块代码 | 模块名称 | 说明 |
|---------|---------|------|
| `contract_template_design` | 合同模板设计模块 | 合同模板的创建、编辑、预览等功能 |
| `contract_synthesis` | 合同合成模块 | 合同生成、合并、数据绑定等功能 |
| `contract_compare_pro` | 合同比对PRO模块 | GPU OCR比对、智能差异分析等功能 |
| `contract_info_extraction` | 合同信息抽取模块 | 实体识别、关键信息提取等功能 |
| `contract_intelligent_review` | 合同智能审核模块 | 风险分析、合规检查等功能 |
| `performance_task_generation` | 履约任务生成模块 | 任务生成、调度、提醒等功能 |

## 快速开始

### 1. 启用授权模块

在 `application.yml` 中配置：

```yaml
zhaoxin:
  auth:
    enabled: true
    license:
      filePath: "license.lic"
      hardwareBound: true
    signature:
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
      # 或者使用文件路径
      publicKeyPath: "public.key"
```

### 2. 使用注解控制权限

#### 控制器级别授权
```java
// 注意：授权注解应该在backend或sdk模块中使用，不要在core模块中使用
@RestController
@RequestMapping("/api/template")
@RequireFeature(module = ModuleType.CONTRACT_TEMPLATE_DESIGN, message = "合同模板设计模块需要授权")
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AuthorizedTemplateController {
    // 整个控制器的所有方法都需要模板设计模块权限
    // 只有在启用授权时才会注册这个控制器
}
```

#### 方法级别授权
```java
@RestController
@RequestMapping("/api/contract")
public class ContractController {
    
    @PostMapping("/generate")
    @RequireFeature(module = ModuleType.CONTRACT_SYNTHESIS, message = "合同生成功能需要授权")
    public ResponseEntity<?> generateContract() {
        // 只有这个方法需要合同合成模块权限
    }
    
    @PostMapping("/compare")
    @RequireFeature(module = ModuleType.CONTRACT_COMPARE_PRO, message = "合同比对功能需要授权")
    public ResponseEntity<?> compareContract() {
        // 只有这个方法需要合同比对PRO模块权限
    }
}
```

### 3. 编程式权限检查

```java
@Service
public class BusinessService {
    
    @Autowired
    private LicenseService licenseService;
    
    public void doSomething() {
        // 检查特定模块权限
        if (!licenseService.hasModulePermission(ModuleType.CONTRACT_TEMPLATE_DESIGN)) {
            throw new RuntimeException("无合同模板设计模块权限");
        }
        
        // 执行业务逻辑
    }
}
```

## License生成

### 1. 生成密钥对

```bash
# 使用命令行工具生成密钥对
java -cp contract-tools-auth-1.0.0.jar com.zhaoxinms.contract.tools.auth.generator.LicenseGeneratorCLI
```

或者编程方式：
```java
LicenseGenerator generator = new LicenseGenerator();
KeyGenerateResult result = generator.generateKeyPair("./keys");
System.out.println("公钥: " + result.getPublicKeyContent());
System.out.println("私钥: " + result.getPrivateKeyContent());
```

### 2. 生成License文件

```java
LicenseGenerator generator = new LicenseGenerator();
LicenseGenerateRequest request = new LicenseGenerateRequest();

// 基本信息
request.setLicenseCode("LC2024001");
request.setCompanyName("测试公司");
request.setContactPerson("张三");
request.setContactPhone("13800138000");

// 授权模块
Set<ModuleType> modules = Set.of(
    ModuleType.CONTRACT_TEMPLATE_DESIGN,
    ModuleType.CONTRACT_SYNTHESIS,
    ModuleType.CONTRACT_COMPARE_PRO
);
request.setAuthorizedModules(modules);

// 时间和用户限制
request.setStartDate(LocalDateTime.now());
request.setExpireDate(LocalDateTime.now().plusYears(1));
request.setMaxUsers(10);
request.setHardwareBound(true);

// 签名配置
request.setPrivateKeyPath("private.key");
request.setOutputPath("license.lic");

// 生成License
boolean success = generator.generateLicense(request);
```

## REST API

授权模块提供了以下HTTP接口：

### 获取授权信息
```http
GET /api/auth/license-info
```

### 检查模块权限
```http
GET /api/auth/check-module?moduleCode=contract_compare_pro
```

### 验证License
```http
GET /api/auth/validate
```

### 获取所有模块
```http
GET /api/auth/modules
```

### 批量检查模块权限
```http
POST /api/auth/check-modules
Content-Type: application/json

["contract_template_design", "contract_synthesis", "contract_compare_pro"]
```

## 错误处理

当权限验证失败时，系统会抛出 `RuntimeException` 异常，包含具体的错误信息：

```java
@ControllerAdvice
public class AuthExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleAuthException(RuntimeException e) {
        if (e.getMessage().contains("权限不足")) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "code", "AUTH_DENIED"
            ));
        }
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "系统错误"
        ));
    }
}
```

## 部署注意事项

1. **公钥配置**: 确保生产环境中正确配置公钥用于验证License
2. **License文件**: 将License文件放置在应用能够访问的路径
3. **硬件绑定**: 如果启用硬件绑定，确保License是在目标服务器上生成的
4. **时间同步**: 确保服务器时间准确，避免时间偏差导致License验证失败

## 故障排除

### 常见问题

1. **License文件不存在**
   - 检查 `license.filePath` 配置是否正确
   - 确认License文件已正确部署

2. **签名验证失败**
   - 检查公钥配置是否正确
   - 确认License文件未被篡改

3. **硬件信息不匹配**
   - 检查License是否在正确的服务器上生成
   - 考虑禁用硬件绑定进行测试

4. **模块权限检查失败**
   - 确认License中包含所需的模块权限
   - 检查模块代码是否正确

### 调试模式

在开发环境中，可以通过以下配置禁用授权：

```yaml
zhaoxin:
  auth:
    enabled: false  # 禁用授权模块
```

或者设置日志级别查看详细信息：

```yaml
logging:
  level:
    com.zhaoxinms.contract.tools.auth: DEBUG
```
