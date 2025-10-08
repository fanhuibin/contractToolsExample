# SDK 配置问题修复指南

## 🔍 问题根源

之前存在**多个重复的配置类**，导致配置冲突：

### 修复前（❌ 有冲突）
```
backend/
  └── ZxOcrConfig.java         ← renderDpi = 200
  └── GpuOcrConfig.java        ← 没有 renderDpi
  
contract-tools-core/
  └── ZxOcrConfig.java         ← renderDpi = 300
  └── GpuOcrConfig.java        ← 没有 renderDpi
```

**问题**：两个模块都有 `ZxOcrConfig` 且都使用 `@ConfigurationProperties(prefix = "zxcm.compare.zxocr")`，Spring 不知道该加载哪个！

### 修复后（✅ 统一配置）
```
contract-tools-core/
  └── ZxOcrConfig.java         ← renderDpi = 300 (唯一配置)
  └── GpuOcrConfig.java        ← 线程池配置（不包含 DPI）
```

**backend 模块的重复配置类已删除！**

## ✅ 已执行的修复

### 1. 删除重复配置类
- ❌ 删除 `backend/.../ZxOcrConfig.java`
- ❌ 删除 `backend/.../GpuOcrConfig.java`

### 2. 统一配置源
现在所有模块都使用 `contract-tools-core` 中的配置类：
- ✅ `ZxOcrConfig.java` - 唯一的配置源
- ✅ 所有 YML 配置都映射到这个类

### 3. 添加配置加载日志
在 `ZxOcrConfig` 中添加了 `@PostConstruct` 方法，启动时会输出：
```
╔════════════════════════════════════════════════════════════════
║ ZxOcrConfig 配置已加载 (来自: contract-tools-core)
╠════════════════════════════════════════════════════════════════
║ 📍 配置前缀: zxcm.compare.zxocr
║ 🎨 渲染DPI: 300
║ 🖼️  图片格式: JPEG
║ 📊 JPEG质量: 0.85
║ 📁 上传路径: ./uploads
║ 🔧 OCR服务: mineru @ http://192.168.0.100:8000
╚════════════════════════════════════════════════════════════════
```

### 4. 统一 DPI 配置
所有配置文件都已更新为 300 DPI：
- ✅ `sdk/src/main/resources/application.yml`: 300
- ✅ `contract-tools-sdk/src/main/resources/application.yml`: 300
- ✅ `contract-tools-sdk/src/main/resources/application-extract.yml`: 300
- ✅ `contract-tools-core/.../ZxOcrConfig.java`: 300 (代码默认值)

## 🎯 配置优先级（正确的）

现在 Spring Boot 按以下顺序加载配置：

```
1️⃣ 环境变量: ZXCM_COMPARE_ZXOCR_RENDER_DPI=300
          ↓ (如果没有)
2️⃣ 命令行参数: --zxcm.compare.zxocr.render-dpi=300
          ↓ (如果没有)
3️⃣ application.yml: render-dpi: 300
          ↓ (如果没有)
4️⃣ Java 默认值: private int renderDpi = 300;
```

**所有级别现在都是 300 DPI！**

## 🚀 验证步骤

### 1. 清理旧的图片缓存（必须！）
```powershell
# 删除所有任务的缓存图片
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

### 2. 重启 SDK 应用
```bash
mvn clean package -DskipTests
java -jar contract-tools-sdk/target/contract-tools-sdk-1.0.0.jar
```

### 3. 查看启动日志
启动后应该看到：
```
╔════════════════════════════════════════════════════════════════
║ ZxOcrConfig 配置已加载 (来自: contract-tools-core)
╠════════════════════════════════════════════════════════════════
║ 📍 配置前缀: zxcm.compare.zxocr
║ 🎨 渲染DPI: 300          ← 检查这里！
║ 🖼️  图片格式: JPEG
║ 📊 JPEG质量: 0.85
║ 📁 上传路径: ./uploads
║ 🔧 OCR服务: mineru @ http://192.168.0.100:8000
╚════════════════════════════════════════════════════════════════
```

**如果这里显示 300，那么配置就是正确的！**

### 4. 执行比对任务
提交新的比对任务，查看日志：
```
开始生成10个页面图片，DPI: 300     ← 应该是 300
📸 图片格式: JPEG, JPEG质量: 0.85
✅ 生成页面图片: page-1.jpg, 尺寸: 2480x3508, 大小: 250KB
```

### 5. 验证图片尺寸
A4 页面在不同 DPI 下的尺寸：
- 150 DPI: 1240 x 1754 像素
- 200 DPI: 1654 x 2339 像素
- **300 DPI: 2480 x 3508 像素** ← 应该是这个
- 400 DPI: 3307 x 4677 像素

使用图片工具或代码检查：
```powershell
# 使用 PowerShell 查看图片信息
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile(".\uploads\compare-pro\tasks\xxx\images\old\page-1.jpg")
Write-Host "尺寸: $($img.Width) x $($img.Height)"
$img.Dispose()
```

## 📊 模块依赖关系

### SDK 项目依赖
```
contract-tools-sdk
  ├── contract-tools-core     ← 使用这个模块的配置类
  ├── contract-tools-extract
  └── contract-tools-auth (optional)
```

### Backend 项目依赖
```
contract-tools-backend
  ├── contract-tools-core     ← 使用这个模块的配置类
  └── backend (已清理重复配置)
```

**所有项目现在都使用 `contract-tools-core` 中的唯一配置类！**

## ⚠️ 常见问题

### Q1: 启动日志显示的 DPI 不是 300？

**检查**：
1. 是否有环境变量覆盖？
   ```bash
   echo $ZXCM_COMPARE_ZXOCR_RENDER_DPI
   ```

2. 启动命令是否有参数？
   ```bash
   --zxcm.compare.zxocr.render-dpi=200  ← 删除这个
   ```

3. 是否使用了错误的配置文件？
   ```bash
   --spring.config.location=xxx.yml  ← 检查这个
   ```

### Q2: 图片还是 200 DPI 的尺寸？

**原因**：使用了缓存的旧图片

**解决**：
```powershell
# 删除所有缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 重新提交任务
```

### Q3: 配置加载日志没有显示？

**原因**：日志级别太高

**解决**：在 `application.yml` 中添加：
```yaml
logging:
  level:
    com.zhaoxinms.contract.tools.comparePRO.config: INFO
```

### Q4: 想临时使用不同的 DPI？

**方法 1**：环境变量（推荐，不需要重新编译）
```bash
export ZXCM_COMPARE_ZXOCR_RENDER_DPI=200
java -jar contract-tools-sdk.jar
```

**方法 2**：命令行参数（推荐）
```bash
java -jar contract-tools-sdk.jar --zxcm.compare.zxocr.render-dpi=200
```

**方法 3**：修改 YML（需要重启）
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 200
```

## 📝 文件清单

### 已修改的文件
- ✅ `contract-tools-core/.../ZxOcrConfig.java` - 添加配置日志
- ✅ `contract-tools-core/.../CompareService.java` - 修复图片路径
- ✅ `contract-tools-sdk/src/main/resources/application.yml` - DPI 300
- ✅ `sdk/src/main/resources/application.yml` - DPI 300
- ✅ `contract-tools-sdk/src/main/resources/application-extract.yml` - DPI 300

### 已删除的文件
- ❌ `backend/.../ZxOcrConfig.java` - 重复配置类
- ❌ `backend/.../GpuOcrConfig.java` - 重复配置类

### 新增的文档
- 📄 `SDK_CONFIG_FIX_GUIDE.md` - 本文档
- 📄 `DPI_CONFIG_SUMMARY.md` - DPI 配置总结
- 📄 `IMAGE_OPTIMIZATION_GUIDE.md` - 图片优化指南

## 🎉 总结

✅ **删除了 backend 中的重复配置类**
✅ **统一使用 contract-tools-core 的配置**
✅ **所有配置文件都是 300 DPI**
✅ **添加了配置加载日志验证**
✅ **修复了图片路径读取问题**

现在：
- SDK 项目的 YML 配置是最终配置
- Java 代码的默认值只在 YML 未配置时使用
- 不会再有多个配置类冲突
- 启动日志会清楚显示实际使用的配置

**重启应用即可生效！**

