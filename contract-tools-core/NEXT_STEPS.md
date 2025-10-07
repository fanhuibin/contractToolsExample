# 🎯 下一步操作指南

## 📌 当前状态

✅ **代码已完成**:
- CompareService已集成MinerU识别逻辑
- MinerUOCRService已创建
- CompareOptions默认使用MinerU
- RapidOCR已禁用
- 初始化日志已添加

⚠️ **需要操作**:
- 重新编译模块
- 重启服务
- 验证MinerU注入成功

## 🚀 立即执行（按顺序）

### 1️⃣ 停止当前服务

如果服务正在运行，按 `Ctrl+C` 停止。

---

### 2️⃣ 重新编译（关键！）

在PowerShell中执行：

```powershell
# 进入项目根目录
cd D:\git\zhaoxin-contract-tool-set

# 编译contract-tools-core（包含MinerU服务）
cd contract-tools-core
mvn clean install -DskipTests
```

**预期输出**:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX.XXX s
```

```powershell
# 编译contract-tools-sdk（使用core的新代码）
cd ..\contract-tools-sdk
mvn clean install -DskipTests
```

**预期输出**:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX.XXX s
```

**如果编译失败**: 
- 查看错误信息
- 检查Java版本 (`java -version`)
- 确保Maven正常 (`mvn -version`)

---

### 3️⃣ 启动服务

```powershell
cd D:\git\zhaoxin-contract-tool-set\contract-tools-sdk
mvn spring-boot:run
```

---

### 4️⃣ 检查启动日志（关键！）

**必须看到以下日志**:

```
GPU OCR比对服务初始化完成，最大并发线程数: 4
✅ MinerU OCR服务已注入并可用          ← 必须有这行！
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
```

**如果看到**:
```
⚠️  MinerU OCR服务未注入（可选）        ← 说明注入失败！
```

**解决方案**: 回到步骤2，重新编译

---

### 5️⃣ 测试功能

1. 打开前端页面
2. 上传两个PDF进行比对
3. **查看后端控制台日志**

**成功的日志**:
```
使用MinerU OCR识别原文档              ← 关键！
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx-old
并行处理：提交PDF识别和生成图片
正在调用MinerU API: http://192.168.0.100:8000/file_parse
MinerU API调用完成，耗时: 25000ms
MinerU识别完成: 6页, 1234个CharBox    ← 关键！
```

**失败的日志（说明还在用dots.ocr）**:
```
❌ POST /v1/chat/completions failed: 404
❌ OCR识别第1页失败
```

---

## ✅ 成功验证清单

启动时:
- [ ] 看到"MinerU OCR服务已注入并可用"
- [ ] 看到MinerU API地址和Backend信息
- [ ] 无编译错误
- [ ] 无启动错误

使用时:
- [ ] 看到"使用MinerU OCR识别原文档"
- [ ] 看到"正在调用MinerU API"
- [ ] 看到"MinerU识别完成: X页, XXXX个CharBox"
- [ ] **没有**404错误
- [ ] **没有**"POST /v1/chat/completions"日志
- [ ] 比对结果正常显示

---

## 🐛 如果出现问题

### 情况1: 编译失败

**检查**:
```powershell
# 查看Java版本
java -version

# 查看Maven版本
mvn -version

# 查看详细错误
mvn clean install -e
```

**常见原因**:
- Java版本不对（需要Java 11+）
- Maven配置问题
- 依赖下载失败

### 情况2: MinerU服务未注入

**症状**: 启动日志显示 "⚠️ MinerU OCR服务未注入"

**原因**: `MinerUOCRService.class` 没有编译到 `contract-tools-core`

**解决**:
```powershell
# 检查class文件是否存在
dir contract-tools-core\target\classes\com\zhaoxinms\contract\tools\comparePRO\service\MinerUOCRService.class

# 如果不存在，重新编译
cd contract-tools-core
mvn clean install -DskipTests -X  # -X显示详细日志
```

### 情况3: 还在使用dots.ocr（404错误）

**症状**: 上传PDF后看到 `POST /v1/chat/completions failed: 404`

**原因**: 代码没有生效，可能是：
1. 没有重新编译
2. 缓存问题

**解决**:
```powershell
# 完全清理
cd D:\git\zhaoxin-contract-tool-set
mvn clean

# 重新编译所有
mvn clean install -DskipTests

# 重启服务
cd contract-tools-sdk
mvn spring-boot:run
```

### 情况4: MinerU服务连接失败

**症状**: `MinerU识别失败: Connection refused`

**原因**: MinerU服务未启动

**解决**:
```bash
# 检查MinerU容器
docker ps | grep mineru

# 如果没运行，启动它
docker start mineru
# 或
docker-compose up -d mineru vllm
```

---

## 📊 快速诊断命令

```powershell
# 1. 检查编译
dir contract-tools-core\target\classes\com\zhaoxinms\contract\tools\comparePRO\service\MinerU*.class

# 2. 检查MinerU服务
curl http://192.168.0.100:8000/docs

# 3. 检查配置
type contract-tools-sdk\src\main\resources\application.yml | findstr "mineru"

# 4. 查看完整启动日志
mvn spring-boot:run > startup.log 2>&1
type startup.log | findstr "MinerU"
```

---

## 📞 需要帮助时提供的信息

如果以上步骤无法解决，请提供：

1. **编译日志**:
   ```powershell
   cd contract-tools-core
   mvn clean install > build-core.log 2>&1
   cd ..\contract-tools-sdk
   mvn clean install > build-sdk.log 2>&1
   ```

2. **启动日志**:
   ```powershell
   mvn spring-boot:run > startup.log 2>&1
   ```

3. **class文件检查**:
   ```powershell
   dir contract-tools-core\target\classes\com\zhaoxinms\contract\tools\comparePRO\service\*.class
   ```

4. **完整错误信息**（截图或复制）

---

## 🎯 现在就开始

**立即执行以下命令**:

```powershell
# 停止当前服务（如果运行中）
# Ctrl+C

# 重新编译
cd D:\git\zhaoxin-contract-tool-set\contract-tools-core
mvn clean install -DskipTests

cd ..\contract-tools-sdk
mvn clean install -DskipTests

# 启动服务
mvn spring-boot:run

# 然后仔细查看启动日志，寻找：
# "✅ MinerU OCR服务已注入并可用"
```

---

**重要**: 一定要等待编译完成后再启动服务！

**预计时间**: 
- 编译: 2-5分钟
- 启动: 10-15秒

**成功标志**: 看到 "✅ MinerU OCR服务已注入并可用"

---

**最后更新**: 2025-10-07

