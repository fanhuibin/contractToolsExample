# 🔍 MinerU设置检查指南

## 快速诊断步骤

### 1️⃣ 检查MinerU服务

```bash
# 检查MinerU Web API
curl http://192.168.0.100:8000/docs

# 预期: 返回Swagger文档页面（HTML）
```

```bash
# 检查vLLM Server（如果使用vlm-http-client模式）
curl http://192.168.0.100:30000/v1/models

# 预期: 返回模型列表JSON
```

**如果失败**: 启动MinerU服务
```bash
docker ps | grep -E "mineru|vllm"
docker-compose up -d mineru vllm
```

---

### 2️⃣ 检查配置文件

#### application.yml

```bash
cd contract-tools-sdk/src/main/resources
grep -A 5 "mineru:" application.yml
```

**预期输出**:
```yaml
mineru:
  api-url: http://192.168.0.100:8000
  vllm-server-url: http://192.168.0.100:30000
  backend: vlm-http-client
```

```bash
grep "enabled:" application.yml | grep -A 1 rapidocr
```

**预期输出**:
```yaml
rapidocr:
  enabled: false
```

#### CompareOptions.java

```bash
cd contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/model
grep "ocrServiceType =" CompareOptions.java
```

**预期输出**:
```java
private String ocrServiceType = "mineru";
```

---

### 3️⃣ 编译检查

```bash
cd contract-tools-core
mvn clean compile
```

**预期**: 编译成功，无ERROR

---

### 4️⃣ 启动后端服务

```bash
cd contract-tools-backend
mvn spring-boot:run
```

**关键启动日志**:

查找以下日志：
```
✅ 配置加载成功
   ZxOcrConfig initialized
   MinerU API URL: http://192.168.0.100:8000

✅ 服务启动成功
   Started BackendApplication in X.XXX seconds
```

---

### 5️⃣ 功能测试

#### 上传PDF进行比对

**检查后端日志**，应该看到：

✅ **正确的日志**:
```
使用MinerU OCR识别原文档
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx-old
并行处理：提交PDF识别和生成图片
MinerU识别完成，解析结果...
MinerU识别完成: 6页, 1234个CharBox
```

❌ **错误的日志**（说明还在用dots.ocr）:
```
POST /v1/chat/completions failed: 404
OCR识别第X页失败: POST /v1/chat/completions failed: 404
```

---

## 🔴 常见问题诊断

### 问题1: 还在调用dots.ocr（404错误）

**症状**:
```
OCR识别第1页失败: POST /v1/chat/completions failed: 404
```

**诊断**:
```bash
# 检查配置
grep "ocrServiceType" CompareOptions.java
```

**解决**:
1. 确保 `ocrServiceType = "mineru"`
2. 重新编译: `mvn clean compile`
3. 重启服务

---

### 问题2: MinerU服务连接失败

**症状**:
```
MinerU识别失败: Connection refused
```

**诊断**:
```bash
curl -I http://192.168.0.100:8000/docs
```

**解决**:
```bash
# 启动MinerU
docker-compose up -d mineru

# 检查日志
docker logs mineru
```

---

### 问题3: 识别返回空结果

**症状**:
```
MinerU识别完成: 6页, 0个CharBox
OCR完成: 0 字符 vs 0 字符
```

**可能原因**:
1. MinerU返回结果格式不对
2. content_list为空
3. 坐标转换失败

**诊断**:
```bash
# 查看MinerU原始结果
cd contract-tools-sdk/uploads/compare-pro/tasks/{taskId}
cat mineru_content_list.json
```

**检查**:
- 文件是否存在
- JSON格式是否正确
- content_list是否有数据

---

### 问题4: vLLM模型404

**症状**:
```
The model vlm-http-client does not exist
```

**原因**: backend设置错误

**解决**:
```yaml
# application.yml
mineru:
  backend: vlm-http-client  # 不是模型名！
  vllm-server-url: http://192.168.0.100:30000  # 必须配置
```

---

## ✅ 完整检查清单

### 配置检查
- [ ] application.yml中mineru配置正确
- [ ] application.yml中rapidocr.enabled=false
- [ ] CompareOptions.java中ocrServiceType="mineru"
- [ ] ZxOcrConfig.java有MinerUConfig内部类

### 服务检查
- [ ] MinerU服务可访问（8000端口）
- [ ] vLLM服务可访问（30000端口，如需要）
- [ ] 后端服务启动成功

### 代码检查
- [ ] MinerUOCRService类存在于comparePRO/service/
- [ ] MinerUCoordinateConverter类存在于comparePRO/util/
- [ ] CompareService注入了MinerUOCRService
- [ ] CompareService有recognizePdfWithMinerU方法
- [ ] 编译无ERROR

### 功能检查
- [ ] 上传PDF后看到"使用MinerU OCR识别"
- [ ] 没有404错误
- [ ] 识别结果不为空
- [ ] 前端显示比对结果

---

## 🚀 一键检查脚本（可选）

创建 `check-mineru.sh`:

```bash
#!/bin/bash

echo "🔍 MinerU设置检查开始..."
echo ""

# 1. 检查MinerU服务
echo "1️⃣ 检查MinerU服务..."
if curl -s -o /dev/null -w "%{http_code}" http://192.168.0.100:8000/docs | grep -q "200"; then
    echo "✅ MinerU Web API正常"
else
    echo "❌ MinerU Web API无法访问"
fi

# 2. 检查vLLM服务
echo "2️⃣ 检查vLLM服务..."
if curl -s -o /dev/null -w "%{http_code}" http://192.168.0.100:30000/v1/models | grep -q "200"; then
    echo "✅ vLLM Server正常"
else
    echo "⚠️  vLLM Server无法访问（如不使用vlm-http-client可忽略）"
fi

# 3. 检查配置
echo "3️⃣ 检查配置文件..."
if grep -q "ocrServiceType = \"mineru\"" contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/model/CompareOptions.java; then
    echo "✅ CompareOptions配置正确"
else
    echo "❌ CompareOptions未设置为mineru"
fi

if grep -A 1 "rapidocr:" contract-tools-sdk/src/main/resources/application.yml | grep -q "enabled: false"; then
    echo "✅ RapidOCR已禁用"
else
    echo "❌ RapidOCR未禁用"
fi

# 4. 检查文件
echo "4️⃣ 检查关键文件..."
if [ -f "contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/service/MinerUOCRService.java" ]; then
    echo "✅ MinerUOCRService存在"
else
    echo "❌ MinerUOCRService不存在"
fi

if [ -f "contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/util/MinerUCoordinateConverter.java" ]; then
    echo "✅ MinerUCoordinateConverter存在"
else
    echo "❌ MinerUCoordinateConverter不存在"
fi

echo ""
echo "🎯 检查完成！"
```

使用方式:
```bash
chmod +x check-mineru.sh
./check-mineru.sh
```

---

## 📞 需要帮助？

如果以上步骤无法解决问题，请提供以下信息：

1. **完整的后端启动日志**
2. **比对任务的完整日志**
3. **MinerU服务日志**: `docker logs mineru`
4. **配置文件内容**: `application.yml`相关部分
5. **错误截图或完整错误信息**

---

**最后更新**: 2025-10-07

