# DPI 配置统一说明

## ✅ 已统一的配置

所有 DPI 配置已统一为 **300 DPI + JPEG 85%**

### 主要配置位置

#### 1. Java 代码默认值（3个文件）

| 文件 | 默认值 | 状态 |
|------|--------|------|
| `contract-tools-core/.../ZxOcrConfig.java` | 300 DPI | ✅ |
| `backend/.../ZxOcrConfig.java` | 300 DPI | ✅ |
| `backend/.../ThirdPartyOcrConfig.java` | 160 DPI | ⚠️ 不影响 |

**说明**：ThirdPartyOcrConfig 用于第三方 OCR，不影响 MinerU。

#### 2. 配置文件（3个文件）

| 文件 | 配置路径 | 值 | 状态 |
|------|---------|-----|------|
| `sdk/src/main/resources/application.yml` | `zxcm.compare.zxocr.render-dpi` | 300 | ✅ |
| `contract-tools-sdk/src/main/resources/application.yml` | `zxcm.compare.zxocr.render-dpi` | 300 | ✅ |
| `contract-tools-sdk/src/main/resources/application-extract.yml` | `zxcm.compare.zxocr.render-dpi` | 300 | ✅ |

## 🎯 最终生效的配置

### 配置优先级

```
环境变量 > application.yml > Java 代码默认值
```

### 实际使用的配置类

**CompareService** 和 **MinerUOCRService** 都使用：
```java
@Autowired
private ZxOcrConfig zxOcrConfig;  // 或 gpuOcrConfig（同一个类）

int dpi = zxOcrConfig.getRenderDpi();        // 获取 DPI
String format = zxOcrConfig.getImageFormat(); // 获取格式（PNG/JPEG）
float quality = zxOcrConfig.getJpegQuality(); // 获取 JPEG 质量
```

### 配置加载路径

Spring Boot 从以下位置加载配置（按优先级）：
1. 环境变量：`ZXCM_COMPARE_ZXOCR_RENDER_DPI`
2. 命令行参数：`--zxcm.compare.zxocr.render-dpi=300`
3. `application.yml` 文件
4. Java 代码默认值

## 🔍 验证方法

### 1. 查看启动日志

启动应用后查找：
```
开始生成XX个页面图片，DPI: 300
图片格式: JPEG, JPEG质量: 0.85
```

### 2. 检查生成的图片

```powershell
# 查看图片文件
ls .\uploads\compare-pro\tasks\*\images\old\*.jpg

# 查看图片尺寸（A4 @ 300 DPI 应该是 2480x3508）
```

### 3. 查看图片文件大小

```
200 DPI PNG: ~400KB
300 DPI PNG: ~800KB
300 DPI JPEG 85%: ~250KB  ← 当前配置
```

## ⚠️ 常见问题

### Q1: 修改配置后还是旧的 DPI？

**原因**：使用了缓存的图片

**解决**：
```powershell
# 删除缓存图片
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*\images\
```

### Q2: 不同模块的配置不一致？

**检查**：确认启动的是哪个模块

| 模块 | 配置文件 | 代码默认值 |
|------|---------|-----------|
| SDK | `sdk/src/main/resources/application.yml` | `contract-tools-core/.../ZxOcrConfig.java` |
| Backend | 使用 SDK 的配置 | `backend/.../ZxOcrConfig.java` |

**所有配置现已统一为 300 DPI！**

### Q3: 想临时修改 DPI？

**方法 1**：环境变量（推荐）
```bash
export ZXCM_COMPARE_ZXOCR_RENDER_DPI=200
export ZXCM_COMPARE_ZXOCR_IMAGE_FORMAT=PNG
```

**方法 2**：命令行参数
```bash
java -jar app.jar \
  --zxcm.compare.zxocr.render-dpi=200 \
  --zxcm.compare.zxocr.image-format=PNG
```

**方法 3**：修改 application.yml（需重启）

## 📊 配置建议

### 生产环境（推荐）
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 300
      image-format: JPEG
      jpeg-quality: 0.85
```
- 清晰度：⭐⭐⭐⭐⭐
- 文件大小：约 250KB/页
- 推荐用于正式环境

### 高质量环境
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 300
      image-format: JPEG
      jpeg-quality: 0.90
```
- 清晰度：⭐⭐⭐⭐⭐
- 文件大小：约 320KB/页

### 测试环境
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 200
      image-format: JPEG
      jpeg-quality: 0.85
```
- 清晰度：⭐⭐⭐⭐
- 文件大小：约 120KB/页
- 速度更快

## 🔧 故障排查

### 1. 确认当前配置

查看日志输出的实际值：
```
开始生成XX个页面图片，DPI: ???  ← 这里显示实际使用的 DPI
图片格式: ???, JPEG质量: ???    ← 这里显示实际格式和质量
```

### 2. 检查配置加载

在 ZxOcrConfig 添加 `@PostConstruct` 日志：
```java
@PostConstruct
public void init() {
    log.info("ZxOcrConfig loaded: renderDpi={}, imageFormat={}, jpegQuality={}", 
        renderDpi, imageFormat, jpegQuality);
}
```

### 3. 确认图片尺寸

```java
// A4 页面不同 DPI 的尺寸
150 DPI: 1240 x 1754 像素
200 DPI: 1654 x 2339 像素
300 DPI: 2480 x 3508 像素  ← 当前配置
400 DPI: 3307 x 4677 像素
```

## 📝 总结

✅ **所有配置已统一**：300 DPI + JPEG 85%

✅ **生效位置**：
- `ZxOcrConfig` 类（Java 代码默认值）
- `application.yml` 文件
- MinerUOCRService 使用该配置生成图片

✅ **验证方法**：
- 查看日志中的 DPI 值
- 检查图片文件大小（约 250KB）
- 检查图片尺寸（2480 x 3508）

现在重启应用，所有图片将使用统一的 300 DPI JPEG 格式！

