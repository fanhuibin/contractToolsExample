# ✅ MinerU 配置完成 - 可以使用

## 🎉 配置状态

### ✅ 已完成
- [x] MinerU设置为默认OCR引擎
- [x] RapidOCR已禁用
- [x] 配置文件已更新
- [x] 代码无错误
- [x] 文档已完善

### 📋 当前配置

```yaml
# application.yml
zxcm:
  compare:
    zxocr:
      render-dpi: 160
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
    rapidocr:
      enabled: false  # ✅ 已禁用
```

```java
// CompareOptions.java
private String ocrServiceType = "mineru";  // ✅ 默认MinerU
```

## 🚀 启动步骤

### 1. 确认MinerU服务运行

```bash
# 检查MinerU容器
docker ps | grep mineru

# 检查vLLM容器
docker ps | grep vllm

# 测试连接
curl http://192.168.0.100:8000/docs
curl http://192.168.0.100:30000/v1/models
```

### 2. 启动后端服务

```bash
cd contract-tools-backend
mvn clean spring-boot:run
```

查看启动日志，确认：
- ✅ MinerU配置加载成功
- ✅ ZxOcrConfig初始化完成
- ✅ MinerUOCRService注入成功

### 3. 启动前端

```bash
cd frontend
npm run dev
```

### 4. 测试功能

1. 打开前端页面
2. 上传两个PDF文件
3. 点击"开始比对"
4. 查看后端日志，应该看到：

```
使用MinerU识别PDF: test.pdf, 任务ID: xxx
并行处理：提交PDF识别和生成图片
MinerU识别完成，解析结果...
MinerU OCR识别完成，共3页，耗时25000ms
```

## 📊 功能说明

### 默认行为

**前端不传参数时**，系统自动使用MinerU：

```javascript
// 请求
{
  "oldFileUrl": "http://example.com/old.pdf",
  "newFileUrl": "http://example.com/new.pdf",
  "ignoreHeaderFooter": true,
  "headerHeightPercent": 12,
  "footerHeightPercent": 12
}

// 系统自动使用 ocrServiceType = "mineru"
```

### 临时切换引擎

```javascript
// 切换到dots.ocr（更快）
{
  "ocrServiceType": "dotsocr",
  ...
}

// 切换到第三方OCR
{
  "ocrServiceType": "thirdparty",
  ...
}
```

## 🎯 OCR引擎特性

| 引擎 | 默认 | 速度 | 精度 | 适用场景 |
|------|------|------|------|---------|
| **MinerU** | ✅ 是 | ⚡ 慢 | ⭐⭐⭐⭐⭐ 最高 | 复杂文档、表格 |
| **dots.ocr** | 可选 | ⚡⚡⚡ 快 | ⭐⭐⭐⭐ 高 | 标准合同 |
| **第三方** | 可选 | ⚡⚡ 中 | ⭐⭐⭐⭐ 高 | 云服务 |
| **RapidOCR** | ❌ 禁用 | - | - | 不使用 |

## 💡 使用建议

### 何时使用MinerU（默认）
- ✅ 复杂表格文档
- ✅ 多列排版文档
- ✅ 需要最高精度
- ✅ 处理时间不敏感

### 何时切换到dots.ocr
- ✅ 标准合同文档
- ✅ 需要快速处理
- ✅ 批量处理场景
- ✅ 简单布局文档

## 🔧 故障排查

### 问题1: MinerU连接失败

```bash
# 检查服务状态
docker ps | grep mineru

# 如果没有运行，启动服务
docker-compose up -d mineru

# 检查日志
docker logs mineru
```

### 问题2: 识别超时

**解决方案**:
1. 增加超时时间（已设置为30分钟）
2. 切换backend为 `pipeline` 模式
3. 或临时使用 dots.ocr

### 问题3: 想切回dots.ocr为默认

修改 `CompareOptions.java`:
```java
private String ocrServiceType = "dotsocr";
```

重启服务即可。

## 📈 性能预期

### 处理时间

| 页数 | dots.ocr | MinerU | 差异 |
|------|----------|--------|------|
| 1页 | 0.5秒 | 2-3秒 | 4-6倍 |
| 10页 | 5秒 | 20-30秒 | 4-6倍 |
| 50页 | 25秒 | 2-3分钟 | 4-6倍 |

### 识别精度

- **标准文本**: MinerU ≈ dots.ocr (都很高)
- **复杂表格**: MinerU >> dots.ocr (明显优势)
- **多列排版**: MinerU >> dots.ocr (明显优势)
- **手写体**: 取决于模型训练

## ✅ 确认清单

使用前请确认：

- [ ] MinerU服务已启动（http://192.168.0.100:8000）
- [ ] vLLM服务已启动（http://192.168.0.100:30000）
- [ ] 后端服务已启动（日志无错误）
- [ ] 前端服务已启动
- [ ] 上传测试PDF，查看日志确认使用MinerU

全部确认后，即可正常使用！

## 📚 相关文档

- **快速参考**: [MINERU_QUICK_START.md](./MINERU_QUICK_START.md)
- **完整文档**: [MINERU_INTEGRATION_FINAL.md](./MINERU_INTEGRATION_FINAL.md)
- **配置说明**: [MINERU_CONFIG_CURRENT.md](./MINERU_CONFIG_CURRENT.md)
- **变更总结**: [CONFIG_CHANGES_SUMMARY.md](./CONFIG_CHANGES_SUMMARY.md)

## 🎊 开始使用

配置已完成，重启服务后即可使用MinerU进行高精度的合同比对！

---

**状态**: ✅ 配置完成，可以使用

**默认引擎**: MinerU

**RapidOCR**: 已禁用

**最后更新**: 2025-10-07

