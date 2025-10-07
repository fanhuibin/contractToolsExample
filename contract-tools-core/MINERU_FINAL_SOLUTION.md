# ✅ MinerU 最终方案

## 📌 配置方案（推荐）

**配置位置**: `ZxOcrConfig.java` 中的 `defaultOcrService` 属性

**配置文件**: `application.yml`

```yaml
zxcm:
  compare:
    zxocr:
      # 默认OCR工具选择（全局配置，优先级高于前端传递）
      # 可选值: mineru, dotsocr, thirdparty
      default-ocr-service: mineru  # ← 在这里配置！
```

---

## ✅ 已完成的修改

### 1. 修改 `ZxOcrConfig.java`

添加了 `defaultOcrService` 属性：

```java
/**
 * 默认OCR服务类型（全局配置）
 * 可选值: mineru, dotsocr, thirdparty
 * 默认: mineru
 */
private String defaultOcrService = "mineru";

public String getDefaultOcrService() {
    return defaultOcrService;
}

public void setDefaultOcrService(String defaultOcrService) {
    this.defaultOcrService = defaultOcrService;
}
```

### 2. 修改 `application.yml`

```yaml
zxcm:
  compare:
    zxocr:
      default-ocr-service: mineru  # 新增配置
      ocr-base-url: http://192.168.0.100:8000
      ocr-model: model
      # ... 其他配置
```

### 3. 修改 `CompareService.java`

```java
// 使用配置文件中的OCR服务，忽略前端传递的值
String configuredOcrService = gpuOcrConfig.getDefaultOcrService();
options.setOcrServiceType(configuredOcrService);

System.out.println("🔍 OCR服务配置: " + configuredOcrService);
progressManager.logStepDetail("使用配置文件指定的OCR服务: {}", configuredOcrService);
```

### 4. 删除 `CompareConfig.java`

不需要单独的配置类，直接使用 `ZxOcrConfig`。

---

## 🚀 编译和启动

```bash
# 1. 编译core模块
cd D:\git\zhaoxin-contract-tool-set\contract-tools-core
mvn clean install -DskipTests

# 2. 编译sdk模块
cd ..\contract-tools-sdk
mvn clean install -DskipTests

# 3. 启动服务
mvn spring-boot:run
```

---

## 🔍 预期日志

### 启动日志

```
✅ MinerU OCR服务已注入并可用
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
```

### 比对日志

```
🔍 OCR服务配置: mineru                      ← 新增
使用配置文件指定的OCR服务: mineru            ← 新增
✅ 使用MinerU OCR服务
使用MinerU OCR识别原文档
并行处理：提交PDF识别和生成图片
MinerU识别完成: 6页, 1234个CharBox
```

---

## 🎯 切换OCR引擎

### 使用 MinerU（默认）

```yaml
default-ocr-service: mineru
```

### 使用 dots.ocr

```yaml
default-ocr-service: dotsocr
```

### 使用第三方OCR

```yaml
default-ocr-service: thirdparty
```

**修改后重启服务即可！**

---

## 📊 配置层级

```
ZxOcrConfig (zxcm.compare.zxocr)
├── defaultOcrService: mineru       ← 全局OCR选择
├── ocrBaseUrl: ...                 ← dots.ocr配置
├── renderDpi: 160                  ← 渲染配置
├── mineru:                         ← MinerU配置
│   ├── apiUrl: ...
│   ├── vllmServerUrl: ...
│   └── backend: ...
└── ... 其他配置
```

---

## ✅ 优势

1. **统一管理** - 所有比对配置都在 `ZxOcrConfig` 中
2. **前端无关** - 前端无需修改，配置文件控制
3. **易于维护** - 不需要额外的配置类
4. **符合现有架构** - 使用已有的配置类

---

**最后更新**: 2025-10-07

