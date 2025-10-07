# 🔍 MinerU启动诊断指南

## 当前问题

从启动日志分析：
```
2025-10-07 12:15:56 [main] INFO  c.z.c.t.o.service.UnifiedOCRService - 初始化统一OCR服务，提供者: dotsocr
GPU OCR比对服务初始化完成，最大并发线程数: 4
```

**问题**:
1. ✅ CompareService已初始化
2. ❌ 没有看到"MinerU OCR服务已注入并可用"的日志
3. ⚠️ 说明MinerU服务可能没有被Spring注入

## 🔧 诊断步骤

### 步骤1: 重新编译所有模块

MinerU服务在 `contract-tools-core` 模块，需要确保编译到位：

```bash
# 在项目根目录
cd D:\git\zhaoxin-contract-tool-set

# 清理并编译所有模块
mvn clean install -DskipTests

# 或者只编译core模块
cd contract-tools-core
mvn clean install -DskipTests
```

**预期输出**:
```
[INFO] Building contract-tools-core
[INFO] BUILD SUCCESS
```

### 步骤2: 检查依赖关系

确认 `contract-tools-sdk` 依赖 `contract-tools-core`：

```bash
cd contract-tools-sdk
grep -A 5 "contract-tools-core" pom.xml
```

**预期**:
```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-core</artifactId>
    <version>xxx</version>
</dependency>
```

### 步骤3: 验证类文件存在

```bash
# 检查编译后的class文件
ls contract-tools-core/target/classes/com/zhaoxinms/contract/tools/comparePRO/service/MinerUOCRService.class

# 检查SDK的依赖中是否包含
ls contract-tools-sdk/target/classes/com/zhaoxinms/contract/tools/comparePRO/service/
```

### 步骤4: 重启并查看完整日志

```bash
cd contract-tools-sdk
mvn spring-boot:run 2>&1 | tee startup.log
```

**关键日志查找**:

搜索以下内容：
```bash
grep "MinerU" startup.log
grep "CompareService" startup.log
```

**应该看到**:
```
GPU OCR比对服务初始化完成，最大并发线程数: 4
✅ MinerU OCR服务已注入并可用
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
```

**如果看到**:
```
⚠️  MinerU OCR服务未注入（可选）
```

说明Spring没有找到或注入MinerUOCRService。

## 🐛 常见问题排查

### 问题1: MinerU服务未注入

**可能原因**:
1. `contract-tools-core` 没有编译
2. `contract-tools-sdk` 的依赖缓存问题
3. Spring组件扫描未覆盖

**解决方案**:

```bash
# 1. 完全清理
cd D:\git\zhaoxin-contract-tool-set
mvn clean

# 2. 按顺序编译
cd contract-tools-core
mvn clean install -DskipTests

cd ../contract-tools-sdk
mvn clean install -DskipTests

# 3. 启动
mvn spring-boot:run
```

### 问题2: 编译错误

**症状**: 编译时出现错误

**检查**:
```bash
cd contract-tools-core
mvn compile 2>&1 | grep ERROR
```

**如果有错误**: 先修复编译错误再继续

### 问题3: 依赖版本不匹配

**检查POM版本**:
```bash
# 检查各模块版本
grep -A 3 "<artifactId>contract-tools-core" pom.xml
```

确保所有模块版本一致。

## ✅ 验证成功的标志

重启后应该看到以下日志：

```
GPU OCR线程池最大线程数已调整为: 4
GPU OCR比对服务初始化完成，最大并发线程数: 4
✅ MinerU OCR服务已注入并可用              ← 关键！
   MinerU API: http://192.168.0.100:8000   ← 关键！
   Backend: vlm-http-client                 ← 关键！
```

然后上传PDF测试，应该看到：

```
使用MinerU OCR识别原文档                    ← 关键！
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx-old
并行处理：提交PDF识别和生成图片
MinerU识别完成: 6页, 1234个CharBox
```

**不应该看到**:
```
❌ POST /v1/chat/completions failed: 404
❌ OCR识别第X页失败
```

## 🚀 快速修复脚本

创建 `fix-mineru.bat` (Windows):

```batch
@echo off
echo 🔧 开始修复MinerU集成...
echo.

echo 1️⃣ 清理所有模块...
cd /d D:\git\zhaoxin-contract-tool-set
call mvn clean

echo.
echo 2️⃣ 编译contract-tools-core...
cd contract-tools-core
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ❌ contract-tools-core编译失败
    pause
    exit /b 1
)

echo.
echo 3️⃣ 编译contract-tools-sdk...
cd ..\contract-tools-sdk
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ❌ contract-tools-sdk编译失败
    pause
    exit /b 1
)

echo.
echo ✅ 编译完成！
echo.
echo 4️⃣ 启动服务...
call mvn spring-boot:run
```

使用方式：
```bash
fix-mineru.bat
```

或者手动执行：
```bash
cd D:\git\zhaoxin-contract-tool-set\contract-tools-core
mvn clean install -DskipTests

cd ..\contract-tools-sdk
mvn clean install -DskipTests
mvn spring-boot:run
```

## 📊 完整检查清单

启动前：
- [ ] contract-tools-core已编译 (`mvn clean install`)
- [ ] contract-tools-sdk已编译 (`mvn clean install`)
- [ ] MinerUOCRService.class文件存在
- [ ] MinerU服务运行中 (8000端口)
- [ ] vLLM服务运行中 (30000端口，如需要)

启动后：
- [ ] 看到"MinerU OCR服务已注入并可用"
- [ ] 看到MinerU API配置信息
- [ ] 上传PDF后看到"使用MinerU OCR识别"
- [ ] 没有404错误
- [ ] 识别结果正常

## 🆘 如果仍然失败

请提供以下信息：

1. **完整启动日志** (`mvn spring-boot:run > startup.log 2>&1`)
2. **编译输出** (`mvn clean install > build.log 2>&1`)
3. **POM文件**: `contract-tools-sdk/pom.xml`
4. **类文件检查**:
   ```bash
   ls -la contract-tools-core/target/classes/com/zhaoxinms/contract/tools/comparePRO/service/MinerUOCRService.class
   ```

---

**最后更新**: 2025-10-07

**状态**: 等待重新编译和启动验证

