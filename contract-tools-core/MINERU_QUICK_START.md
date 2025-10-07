# MinerU OCR 快速启动指南

## 🚀 5分钟快速上手

### 步骤1: 配置 application.yml

```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 160
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
```

### 步骤2: 前端调用

```javascript
// 在前端请求中添加 ocrServiceType 参数
const requestData = {
  oldFileUrl: oldPdfUrl,
  newFileUrl: newPdfUrl,
  ocrServiceType: 'mineru',  // 切换到MinerU
  ignoreHeaderFooter: true,
  headerHeightPercent: 12,
  footerHeightPercent: 12
};

// 发送请求
axios.post('/api/compare/advanced', requestData);
```

### 步骤3: 测试

访问前端页面 → 选择OCR引擎为"MinerU" → 上传PDF → 开始比对

## ✅ 完成！

现在您的系统已支持3种OCR引擎：
- **dots.ocr** - 默认，速度快
- **MinerU** - 高精度，适合复杂文档  
- **第三方OCR** - 阿里云等

## 🔄 切换引擎

```java
// Java后端
CompareOptions options = new CompareOptions();
options.setOcrServiceType("mineru");  // 或 "dotsocr" 或 "thirdparty"

// 前端
form.ocrServiceType = 'mineru';
```

## 📊 性能对比

| 引擎 | 10页PDF | 复杂表格 | 推荐场景 |
|------|---------|----------|---------|
| dots.ocr | 5-10秒 | 一般 | 标准合同 |
| MinerU | 20-50秒 | 优秀 | 复杂文档 |

## 🎯 选择建议

- ✅ 标准合同 → dots.ocr
- ✅ 多列排版 → MinerU
- ✅ 复杂表格 → MinerU
- ✅ 大批量处理 → dots.ocr

## 📝 配置位置

所有参数统一在 `CompareOptions` 中：
- ocrServiceType - OCR引擎选择
- ignoreHeaderFooter - 是否过滤页眉页脚
- headerHeightPercent - 页眉高度%
- footerHeightPercent - 页脚高度%

## 🔧 故障排查

### MinerU连接失败
```bash
# 检查服务
docker ps | grep mineru
curl http://192.168.0.100:8000/docs
```

### 识别超时
```yaml
# 切换更快的backend
mineru:
  backend: pipeline  # 不使用VLM，更快
```

## 📚 详细文档

- [完整集成文档](./MINERU_INTEGRATION_FINAL.md)
- [CompareOptions API](../src/main/java/com/zhaoxinms/contract/tools/comparePRO/model/CompareOptions.java)
- [ZxOcrConfig配置](../src/main/java/com/zhaoxinms/contract/tools/comparePRO/config/ZxOcrConfig.java)

---

**提示**: 默认使用dots.ocr，需要时切换到MinerU即可！

