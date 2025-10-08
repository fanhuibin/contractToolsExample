# YML 配置优先级验证指南

## ✅ 配置机制说明

您的项目**已经正确配置**了 Spring Boot 的配置绑定机制：

```java
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {
    private int renderDpi = 160;  // ← 这只是默认值，YML 会覆盖它
    // ...
}
```

### 配置优先级（从高到低）

```
1. 环境变量          ZXCM_COMPARE_ZXOCR_RENDER_DPI=200
   ↓ 覆盖
2. 命令行参数        --zxcm.compare.zxocr.render-dpi=200
   ↓ 覆盖
3. application.yml   render-dpi: 200              ← **您应该改这里**
   ↓ 覆盖
4. Java 默认值       private int renderDpi = 160  ← **仅当 YML 没配置时使用**
```

**结论**：✅ YML 配置**会自动覆盖** Java 默认值！

---

## 🎯 如何修改配置

### 方法：修改 YML 文件（推荐）

编辑以下任一文件（根据您启动的项目）：

**Option 1**: `sdk/src/main/resources/application.yml`
**Option 2**: `contract-tools-sdk/src/main/resources/application.yml`

```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 160  # ← 改这里，重启生效
      image-format: PNG
      jpeg-quality: 0.85
```

### 步骤

1. **修改 YML** - 改 `render-dpi` 的值
2. **清理缓存** - `Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*`
3. **重启应用** - 无需重新编译！
4. **验证生效** - 查看启动日志

---

## 🔍 如何验证配置已生效

### 1. 查看启动日志（最直接）

启动应用后，查找这段日志：

```
╔════════════════════════════════════════════════════════════════
║ ZxOcrConfig 配置已加载 (来自: contract-tools-core)
╠════════════════════════════════════════════════════════════════
║ 📍 配置前缀: zxcm.compare.zxocr
║ 🎨 渲染DPI: 160          ← 检查这里的值！
║ 🖼️  图片格式: PNG
║ 📊 JPEG质量: 0.85
║ 📁 上传路径: ./uploads
║ 🔧 OCR服务: mineru @ http://192.168.0.100:8000
╚════════════════════════════════════════════════════════════════
```

**如果显示的 DPI 值与 YML 一致**，说明配置已生效！✅

### 2. 查看图片生成日志

执行比对任务后，查找：

```
开始生成10个页面图片，DPI: 160  ← 这里显示实际使用的 DPI
📸 图片格式: PNG, JPEG质量: 0.85
✅ 生成页面图片: page-1.png, 尺寸: 1323x1871, 大小: 250KB
```

### 3. 验证图片尺寸

不同 DPI 对应的 A4 尺寸：

| DPI | 图片尺寸（像素） | PNG 文件大小 |
|-----|-----------------|-------------|
| 150 | 1240 x 1754 | ~200KB |
| 160 | 1323 x 1871 | ~250KB |
| 200 | 1654 x 2339 | ~400KB |
| 300 | 2480 x 3508 | ~800KB |

查看实际生成的图片：
```powershell
ls .\uploads\compare-pro\tasks\*\images\old\page-1.png

# 然后查看图片属性，确认尺寸
```

---

## ⚠️ 配置不生效的常见原因

### 1. ❌ 修改了错误的 YML 文件

**问题**：项目可能有多个 application.yml 文件
- `sdk/src/main/resources/application.yml`
- `contract-tools-sdk/src/main/resources/application.yml`
- `contract-tools-extract/src/main/resources/application-extract.yml`

**解决**：确认您启动的是哪个项目，修改对应的 YML 文件。

**验证方法**：
```bash
# 查看启动命令中使用的是哪个 jar
java -jar sdk/target/sdk-xxx.jar                          ← 使用 sdk 的配置
java -jar contract-tools-sdk/target/contract-tools-sdk.jar  ← 使用 contract-tools-sdk 的配置
```

### 2. ❌ 使用了缓存的旧图片

**问题**：旧任务的图片已经生成，不会重新生成。

**解决**：删除所有缓存图片
```powershell
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

### 3. ❌ YML 格式错误

**问题**：缩进错误、冒号后没有空格等

**错误示例**：
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi:160  # ❌ 冒号后必须有空格
```

**正确示例**：
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 160  # ✅ 冒号后有空格
```

### 4. ❌ 有环境变量覆盖了 YML

**问题**：设置了环境变量 `ZXCM_COMPARE_ZXOCR_RENDER_DPI`

**解决**：检查并删除环境变量
```powershell
# 检查
$env:ZXCM_COMPARE_ZXOCR_RENDER_DPI

# 如果有值，删除它
Remove-Item Env:ZXCM_COMPARE_ZXOCR_RENDER_DPI
```

### 5. ❌ 使用了命令行参数

**问题**：启动时指定了参数
```bash
java -jar app.jar --zxcm.compare.zxocr.render-dpi=300  # ← 这会覆盖 YML
```

**解决**：移除命令行参数

---

## 🔧 快速验证测试

### 测试 1：验证配置加载

1. 修改 YML，设置一个特殊值（如 `render-dpi: 123`）
2. 重启应用
3. 查看启动日志中 `🎨 渲染DPI:` 是否显示 `123`

**如果显示 123**：✅ 配置机制正常，YML 会覆盖 Java 默认值
**如果显示其他值**：❌ 有问题，按照上面的排查步骤检查

### 测试 2：验证实际生效

1. 确认启动日志显示的 DPI 正确
2. 清理缓存：`Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*`
3. 执行比对任务
4. 查看生成图片的尺寸是否符合预期

---

## 📝 推荐的配置流程

### 日常修改配置

```bash
# 1. 修改 YML 文件
vi sdk/src/main/resources/application.yml  # 或用 IDE 打开

# 2. 清理缓存（可选，但推荐）
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 3. 重启应用（无需重新编译）
# Ctrl+C 停止当前应用
java -jar sdk/target/sdk-xxx.jar

# 4. 验证配置
# 查看启动日志中的 "🎨 渲染DPI: xxx"
```

**注意**：
- ✅ **无需重新编译** Maven 项目
- ✅ **只需重启**应用
- ✅ **YML 修改立即生效**

### 不同场景的推荐值

```yaml
# 快速预览（文件小，速度快）
render-dpi: 150
image-format: JPEG
jpeg-quality: 0.85

# 标准使用（平衡质量和大小）
render-dpi: 160
image-format: PNG

# 高质量显示（推荐）
render-dpi: 200
image-format: PNG

# 超高清（文件大，慢）
render-dpi: 300
image-format: PNG
```

---

## 🎯 总结

### 配置机制
✅ **已经正确配置**，YML 会自动覆盖 Java 默认值

### 修改配置的方法
1. ✅ 修改 YML 文件（**推荐**）
2. ⚠️ 环境变量（适合临时测试）
3. ⚠️ 命令行参数（适合临时测试）

### 验证方法
1. ✅ 查看启动日志：`🎨 渲染DPI: xxx`
2. ✅ 查看图片生成日志：`开始生成xx个页面图片，DPI: xxx`
3. ✅ 检查图片尺寸

### 常见问题
- ❌ 修改错误的 YML 文件 → 确认启动的是哪个项目
- ❌ 使用缓存的旧图片 → 清理 uploads 目录
- ❌ YML 格式错误 → 检查缩进和冒号后的空格
- ❌ 环境变量覆盖 → 检查并删除环境变量

### 最佳实践
1. **统一修改**：所有项目的 YML 文件保持一致的 DPI 配置
2. **验证优先**：修改后先查看启动日志验证
3. **清理缓存**：修改 DPI 后务必清理缓存
4. **记录配置**：在 YML 注释中说明为什么选择这个 DPI

---

## 🚀 现在就试试

修改 YML 为任意值（如 160、200、300），然后：

```powershell
# 1. 清理缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 2. 重启应用（无需重新编译！）

# 3. 查看启动日志
# 应该看到：🎨 渲染DPI: <您设置的值>

# 4. 执行比对任务验证
```

**配置机制已经是正确的，您只需修改 YML，重启即可！** ✅

