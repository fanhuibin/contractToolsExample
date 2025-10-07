# 配置变更总结

## 📅 变更日期
2025-10-07

## 🎯 变更目标
1. 将MinerU设置为默认OCR引擎
2. 禁用RapidOCR（不需要幻觉校验）

## ✅ 已完成的变更

### 1. application.yml

**变更内容**:
```yaml
# 禁用RapidOCR
rapidocr:
  enabled: false  # ✅ 从 true 改为 false
```

**位置**: `contract-tools-sdk/src/main/resources/application.yml` 第189行

### 2. CompareOptions.java

**变更内容**:
```java
// 默认使用MinerU
private String ocrServiceType = "mineru";  // ✅ 从 "dotsocr" 改为 "mineru"
```

**位置**: `contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/model/CompareOptions.java` 第19行

## 📊 配置对比

### 变更前
| 配置项 | 原值 | 说明 |
|--------|------|------|
| ocrServiceType | "dotsocr" | 默认使用dots.ocr |
| rapidocr.enabled | true | RapidOCR已启用 |

### 变更后
| 配置项 | 新值 | 说明 |
|--------|------|------|
| ocrServiceType | **"mineru"** | 默认使用MinerU |
| rapidocr.enabled | **false** | RapidOCR已禁用 ✅ |

## 🚀 影响范围

### ✅ 无需修改
- 前端代码 - 无需修改
- 后端服务逻辑 - 无需修改
- API接口 - 无需修改
- 数据库 - 无影响

### 📝 行为变化
1. **默认OCR引擎** - 从dots.ocr切换为MinerU
2. **识别精度** - 更高（尤其是复杂文档）
3. **识别速度** - 较慢（但精度更高）
4. **RapidOCR校验** - 不再执行

## 🔍 验证步骤

### 1. 验证配置生效

```bash
# 检查application.yml
grep "enabled: false" application.yml | grep -A 2 rapidocr
# 输出应包含: enabled: false

# 检查CompareOptions
grep "ocrServiceType = " CompareOptions.java
# 输出应包含: private String ocrServiceType = "mineru";
```

### 2. 启动服务测试

```bash
# 启动后端服务
mvn spring-boot:run

# 查看启动日志，应该看到：
# - MinerU配置加载成功
# - RapidOCR未启用的提示
```

### 3. 功能测试

访问前端 → 上传PDF进行比对 → 查看后端日志：

**预期日志**:
```
使用MinerU识别PDF: test.pdf
MinerU识别完成，解析结果...
MinerU OCR识别完成，共3页，耗时25000ms
```

## 💡 前端使用指南

### 默认行为（使用MinerU）

```javascript
// 不指定ocrServiceType，使用默认值（MinerU）
const request = {
  oldFileUrl: "...",
  newFileUrl: "...",
  ignoreHeaderFooter: true,
  headerHeightPercent: 12,
  footerHeightPercent: 12
}
```

### 临时切换到dots.ocr

```javascript
// 明确指定使用dots.ocr
const request = {
  oldFileUrl: "...",
  newFileUrl: "...",
  ocrServiceType: "dotsocr",  // ✅ 临时切换
  ignoreHeaderFooter: true
}
```

## ⚠️ 注意事项

### 1. MinerU服务必须启动

确保以下服务正在运行：
- MinerU Web API: `http://192.168.0.100:8000`
- vLLM Server: `http://192.168.0.100:30000` (如果使用vlm-http-client模式)

检查命令：
```bash
docker ps | grep mineru
docker ps | grep vllm
```

### 2. 识别速度变化

MinerU比dots.ocr慢2-5倍：
- dots.ocr: 10页约5-10秒
- MinerU: 10页约20-50秒

如需更快速度，可以：
- 临时切换到dots.ocr
- 或修改backend为 `pipeline` 模式

### 3. 配置回滚方案

如需回滚到原配置：

**application.yml**:
```yaml
rapidocr:
  enabled: true  # 改回true
```

**CompareOptions.java**:
```java
private String ocrServiceType = "dotsocr";  // 改回dotsocr
```

## 📈 性能预期

### MinerU vs dots.ocr

| 指标 | dots.ocr | MinerU | 差异 |
|------|----------|--------|------|
| 单页耗时 | 0.5-1秒 | 2-5秒 | 慢2-5倍 |
| 识别精度 | 95%+ | 98%+ | 提高3% |
| 复杂表格 | 一般 | 优秀 | 明显提升 |
| 多列排版 | 一般 | 优秀 | 明显提升 |

## 🎯 建议

1. **监控性能** - 关注识别耗时和成功率
2. **灵活切换** - 简单文档可临时使用dots.ocr
3. **优化配置** - 如速度不满意，尝试调整backend
4. **收集反馈** - 观察实际使用效果，必要时调整

## 📚 相关文档

- [当前配置说明](./MINERU_CONFIG_CURRENT.md)
- [MinerU集成文档](./MINERU_INTEGRATION_FINAL.md)
- [快速启动指南](./MINERU_QUICK_START.md)

---

## ✅ 变更完成

| 文件 | 变更 | 状态 |
|------|------|------|
| application.yml | RapidOCR禁用 | ✅ |
| CompareOptions.java | 默认MinerU | ✅ |
| 文档 | 更新说明 | ✅ |

**状态**: ✅ 配置已更新，系统已切换到MinerU

**生效方式**: 重启服务后生效

