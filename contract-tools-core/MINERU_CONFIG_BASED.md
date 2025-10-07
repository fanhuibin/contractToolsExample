# ✅ MinerU 配置文件方案完成

## 📋 最终解决方案

**问题根源**: 前端传递的 `ocrServiceType` 是 "dotsocr"，覆盖了后端的默认配置。

**解决方案**: **使用配置文件全局控制OCR工具选择**，不再依赖前端传递。

---

## 🔧 实现细节

### 1. 新增配置类 `CompareConfig.java`

**位置**: `contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/config/CompareConfig.java`

```java
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare")
public class CompareConfig {
    private String defaultOcrService = "mineru";  // 默认值
    
    // getters and setters
}
```

### 2. 修改 `application.yml`

**位置**: `contract-tools-sdk/src/main/resources/application.yml`

```yaml
zxcm:
  compare:
    # 默认OCR工具选择（全局配置，优先级高于前端传递）
    # 可选值: mineru, dotsocr, thirdparty
    default-ocr-service: mineru  # ← 在这里配置！
    
    zxocr:
      # ... 其他配置
```

### 3. 修改 `CompareService.java`

**核心逻辑**:

```java
@Autowired
private CompareConfig compareConfig;

// 在executeCompareTaskWithPaths方法中：

// 如果options为null，使用默认配置
if (options == null) {
    options = CompareOptions.createDefault();
}

// 【关键】使用配置文件中的OCR服务，忽略前端传递的值
String configuredOcrService = compareConfig.getDefaultOcrService();
options.setOcrServiceType(configuredOcrService);

System.out.println("🔍 OCR服务配置: " + configuredOcrService);
progressManager.logStepDetail("使用配置文件指定的OCR服务: {}", configuredOcrService);
```

---

## 🚀 使用方法

### 方式1: 使用MinerU（默认）

**application.yml**:
```yaml
zxcm:
  compare:
    default-ocr-service: mineru  # 默认已配置
```

### 方式2: 切换到dots.ocr

**application.yml**:
```yaml
zxcm:
  compare:
    default-ocr-service: dotsocr  # 改为dotsocr
```

### 方式3: 使用第三方OCR

**application.yml**:
```yaml
zxcm:
  compare:
    default-ocr-service: thirdparty  # 改为thirdparty
```

**重启服务后生效！**

---

## ✅ 优势

1. **✅ 集中管理**: 所有OCR配置在application.yml中
2. **✅ 前端无关**: 前端不需要修改，不需要传递ocrServiceType
3. **✅ 易于切换**: 修改配置文件即可切换OCR引擎
4. **✅ 环境隔离**: 开发/测试/生产环境可使用不同配置

---

## 📝 编译步骤

```bash
# 1. 编译core模块
cd contract-tools-core
mvn clean install -DskipTests

# 2. 编译sdk模块
cd ..\contract-tools-sdk
mvn clean install -DskipTests

# 3. 启动服务
mvn spring-boot:run
```

---

## 🔍 验证步骤

### 1. 检查启动日志

**应该看到**:
```
✅ MinerU OCR服务已注入并可用
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
```

### 2. 上传PDF测试

**应该看到**:
```
🔍 OCR服务配置: mineru                        ← 新增日志
使用配置文件指定的OCR服务: mineru              ← 新增日志
✅ 使用MinerU OCR服务
使用MinerU OCR识别原文档
并行处理：提交PDF识别和生成图片
MinerU识别完成: 6页, 1234个CharBox
```

**不应该看到**:
```
❌ 📄 PDF转图片流程开始
❌ POST /v1/chat/completions failed: 404
```

---

## 📊 配置优先级

```
配置文件 > 前端传递 > 代码默认值
```

**现在的逻辑**:
1. 读取配置文件中的 `default-ocr-service`
2. 强制设置到 `options.ocrServiceType`
3. 忽略前端传递的值

---

## 🎯 测试场景

### 场景1: 配置文件使用mineru

```yaml
default-ocr-service: mineru
```

**结果**: 使用MinerU，整体PDF识别

### 场景2: 配置文件使用dotsocr

```yaml
default-ocr-service: dotsocr
```

**结果**: 使用dots.ocr，逐页图片识别

### 场景3: 配置文件使用thirdparty

```yaml
default-ocr-service: thirdparty
```

**结果**: 使用阿里云通义千问OCR

---

## 🔧 故障排查

### 问题1: 还在使用dotsocr

**检查**: `application.yml` 中的配置
```bash
grep "default-ocr-service" application.yml
```

**应该显示**:
```yaml
default-ocr-service: mineru
```

### 问题2: 配置未生效

**原因**: 可能是YAML格式问题

**解决**: 确保缩进正确
```yaml
zxcm:
  compare:
    default-ocr-service: mineru  # 注意缩进
```

### 问题3: CompareConfig未注入

**检查启动日志**: 搜索 "CompareConfig"

**解决**: 确保 `@Configuration` 和 `@ConfigurationProperties` 注解存在

---

## 📚 相关文件

| 文件 | 作用 | 修改内容 |
|------|------|---------|
| `application.yml` | 配置文件 | 添加 `default-ocr-service: mineru` |
| `CompareConfig.java` | 配置类 | 新建，读取配置 |
| `CompareService.java` | 比对服务 | 使用配置文件中的OCR服务 |

---

## 🎉 完成

**现在OCR工具完全由配置文件控制！**

修改 `application.yml` → 重启服务 → 立即生效

---

**最后更新**: 2025-10-07

