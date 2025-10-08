# YML 配置不生效的解决方案

## 🔍 问题分析

您遇到的问题：修改 `application.yml` 中的 `render-dpi: 20`，但应用还是使用 300 DPI。

### 根本原因

**使用 `java -jar xxx.jar` 启动时，JAR 包内的 `application.yml` 是打包时的版本！**

```
源码中的 YML 修改
    ↓
❌ JAR 包内的 YML 没有更新（还是旧版本）
    ↓
❌ 应用读取的是 JAR 内的旧配置
    ↓
❌ 修改不生效
```

## ✅ 3 种解决方案

### 方案 1：重新打包（推荐用于生产）

**优点**：部署简单，配置固化在 JAR 内
**缺点**：每次改配置都要重新打包

#### 步骤 1：修改源码中的 YML

编辑：`contract-tools-sdk/src/main/resources/application.yml`
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 20  # 修改这里
```

#### 步骤 2：重新打包

**方法 A - 使用脚本**（推荐）：
```bash
.\REBUILD_AND_TEST.bat
```

**方法 B - 手动执行**：
```bash
# 1. 清理旧编译文件
mvn clean -DskipTests

# 2. 重新打包
mvn package -DskipTests

# 3. 清理缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 4. 启动
java -jar contract-tools-sdk\target\contract-tools-sdk-1.0.0.jar
```

#### 步骤 3：验证

查看启动日志：
```
╔════════════════════════════════════════════════════════════════
║ ZxOcrConfig 配置已加载
║ 🎨 渲染DPI: 20  ← 应该显示 20
╚════════════════════════════════════════════════════════════════
```

---

### 方案 2：使用外部配置文件（推荐用于测试）⭐

**优点**：无需重新打包，改配置立即生效
**缺点**：需要额外的配置文件

#### 步骤 1：使用外部配置文件

我已为您创建了 `application-external.yml`

#### 步骤 2：修改外部配置

编辑：`application-external.yml`（项目根目录）
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 20  # 直接改这里，无需重新打包！
```

#### 步骤 3：使用外部配置启动

**方法 A - 使用脚本**（推荐）：
```bash
.\START_WITH_EXTERNAL_CONFIG.bat
```

**方法 B - 手动执行**：
```bash
java -jar contract-tools-sdk\target\contract-tools-sdk-1.0.0.jar ^
  --spring.config.location=file:./application-external.yml
```

#### 优势
✅ **修改 `application-external.yml`，重启即生效**
✅ **无需重新打包 Maven 项目**
✅ **适合频繁调整配置**

---

### 方案 3：使用 Maven 直接运行（开发模式）

**优点**：自动使用源码中的配置，无需打包
**缺点**：启动较慢

#### 步骤 1：修改源码中的 YML

编辑：`contract-tools-sdk/src/main/resources/application.yml`
```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 20
```

#### 步骤 2：使用 Maven 启动

**方法 A - 使用脚本**：
```bash
.\START_WITH_MAVEN.bat
```

**方法 B - 手动执行**：
```bash
cd contract-tools-sdk
mvn spring-boot:run
```

#### 优势
✅ **直接使用源码中的 YML**
✅ **修改 YML 后直接重启即可**
✅ **适合开发阶段频繁调整**

---

## 🎯 我的推荐

### 开发/测试阶段：方案 2（外部配置文件）

```bash
# 1. 编辑外部配置
notepad application-external.yml  # 改 render-dpi

# 2. 清理缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 3. 启动（使用外部配置）
.\START_WITH_EXTERNAL_CONFIG.bat
```

**优点**：
- ✅ 最快速，无需重新打包
- ✅ 配置独立，不影响源码
- ✅ 可以快速切换不同配置

### 生产部署：方案 1（重新打包）

```bash
# 1. 修改源码中的 YML（设置最终值）
# 2. 打包部署
.\REBUILD_AND_TEST.bat
```

---

## 🔍 配置优先级（重要！）

Spring Boot 配置优先级（从高到低）：

```
1. 命令行参数
   java -jar app.jar --zxcm.compare.zxocr.render-dpi=20
   
2. 外部配置文件
   --spring.config.location=file:./application-external.yml
   
3. JAR 包内的 application.yml  ← 您修改源码但未重新打包时，这个是旧的！
   
4. Java 代码默认值
   private int renderDpi = 160;
```

---

## 📝 快速验证测试

### 测试配置是否生效

1. **修改配置为测试值**：
```yaml
render-dpi: 20  # 使用一个明显的测试值
```

2. **使用方案 2 启动**：
```bash
.\START_WITH_EXTERNAL_CONFIG.bat
```

3. **查看启动日志**：
```
🎨 渲染DPI: 20  ← 如果显示 20，说明配置生效了！
```

4. **执行比对任务**，日志应该显示：
```
开始生成XX个页面图片，DPI: 20
```

5. **如果看到 DPI: 20**：✅ 配置生效！可以改为实际需要的值（160/200/300）

---

## ⚠️ 常见错误

### 错误 1：修改源码 YML，但用 JAR 启动

```
❌ 修改：contract-tools-sdk/src/main/resources/application.yml
❌ 启动：java -jar contract-tools-sdk/target/xxx.jar
❌ 结果：配置不生效（JAR 内的配置是旧的）
```

**解决**：
- 方案 1：重新打包 `mvn clean package`
- 方案 2：使用外部配置文件

### 错误 2：修改外部配置，但未指定加载

```
❌ 修改：application-external.yml
❌ 启动：java -jar xxx.jar  （未指定外部配置）
❌ 结果：配置不生效
```

**解决**：
启动时指定外部配置：
```bash
java -jar xxx.jar --spring.config.location=file:./application-external.yml
```

### 错误 3：使用了缓存的图片

```
❌ 修改配置，重启应用
❌ 但使用了旧任务的图片（已经生成了）
❌ 结果：看起来配置没生效
```

**解决**：
每次修改 DPI 后，务必清理缓存：
```bash
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*
```

---

## 🚀 现在就试试（推荐流程）

### 验证配置机制

```powershell
# 1. 修改外部配置为测试值
# 编辑 application-external.yml，设置 render-dpi: 20

# 2. 清理缓存
Remove-Item -Recurse -Force .\uploads\compare-pro\tasks\*

# 3. 启动应用（使用外部配置）
.\START_WITH_EXTERNAL_CONFIG.bat

# 4. 查看启动日志，确认显示 "🎨 渲染DPI: 20"

# 5. 执行比对任务，查看日志确认使用 20 DPI

# 6. 确认生效后，改为实际需要的值（如 160）
```

---

## 📊 方案对比

| 方案 | 修改配置 | 需要打包 | 启动速度 | 适用场景 |
|------|---------|---------|---------|---------|
| **方案 1 - 重新打包** | 源码 YML | ✅ 需要 | 快 | 生产部署 |
| **方案 2 - 外部配置** | 外部 YML | ❌ 不需要 | 快 | 开发测试 ⭐ |
| **方案 3 - Maven 运行** | 源码 YML | ❌ 不需要 | 慢 | 开发调试 |

---

## 🎉 总结

您的问题：
- ❌ 修改了源码 YML，但用 JAR 启动
- ❌ JAR 内的配置是旧的，所以修改不生效

解决方案：
- ✅ **立即可用**：使用外部配置文件（方案 2）
- ✅ **长期使用**：修改源码后重新打包（方案 1）
- ✅ **开发阶段**：使用 Maven 直接运行（方案 3）

**现在运行 `.\START_WITH_EXTERNAL_CONFIG.bat` 试试，应该能看到 DPI: 20 了！** 🚀

