# 当前MinerU配置说明

## 📋 当前配置状态

### ✅ 已启用
- **MinerU OCR** - 默认OCR引擎
- **页眉页脚过滤** - 可通过前端控制

### ❌ 已禁用
- **RapidOCR** - 幻觉校验功能（不需要）

## 🔧 配置详情

### application.yml

```yaml
zxcm:
  compare:
    zxocr:
      # 基础配置
      render-dpi: 160
      
      # MinerU配置（已启用）
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
        
    # RapidOCR配置（已禁用）
    rapidocr:
      enabled: false  # ✅ 已禁用
```

### CompareOptions.java

```java
public class CompareOptions {
    // 默认使用MinerU
    private String ocrServiceType = "mineru";  // ✅ 默认值已改为mineru
    
    // 页眉页脚设置（可由前端控制）
    private boolean ignoreHeaderFooter = false;
    private double headerHeightPercent = 12;
    private double footerHeightPercent = 12;
}
```

## 🚀 使用说明

### 前端默认行为

如果前端不传递 `ocrServiceType` 参数，系统会自动使用 **MinerU**：

```javascript
// 方式1: 明确指定MinerU（推荐）
{
  "ocrServiceType": "mineru",
  "ignoreHeaderFooter": true,
  "headerHeightPercent": 12,
  "footerHeightPercent": 12
}

// 方式2: 不传ocrServiceType，使用默认值（MinerU）
{
  "ignoreHeaderFooter": true,
  "headerHeightPercent": 12,
  "footerHeightPercent": 12
}
```

### 如需切换回dots.ocr

前端只需传递：
```javascript
{
  "ocrServiceType": "dotsocr"
}
```

## 📊 OCR引擎对比

| 引擎 | 状态 | 速度 | 精度 | 使用场景 |
|------|------|------|------|---------|
| **MinerU** | ✅ 默认 | 慢 | 最高 | 复杂文档、表格 |
| **dots.ocr** | 可选 | 快 | 高 | 标准合同 |
| **RapidOCR** | ❌ 禁用 | - | - | 不使用 |

## 🔍 验证配置

### 1. 检查配置文件

```bash
# 查看application.yml
grep -A 10 "mineru:" application.yml
grep "enabled:" application.yml | grep rapidocr
```

应该看到：
- `mineru.api-url: http://192.168.0.100:8000`
- `rapidocr.enabled: false`

### 2. 检查Java代码

```bash
# 查看CompareOptions默认值
grep "ocrServiceType = " CompareOptions.java
```

应该看到：
- `private String ocrServiceType = "mineru";`

### 3. 测试识别

启动服务后，上传PDF进行比对，查看日志：

```
使用MinerU识别PDF: test.pdf, 任务ID: xxx
```

## ⚙️ MinerU服务检查

### 检查MinerU服务状态

```bash
# 1. 检查MinerU容器
docker ps | grep mineru

# 2. 检查vLLM容器（如果使用vlm-http-client模式）
docker ps | grep vllm

# 3. 测试API连接
curl http://192.168.0.100:8000/docs

# 4. 测试vLLM连接
curl http://192.168.0.100:30000/v1/models
```

### 如果MinerU服务未启动

```bash
# 启动MinerU（示例）
docker run -d --name mineru \
  --gpus all \
  -p 8000:8000 \
  mineru:latest

# 启动vLLM（如果需要）
docker run -d --name vllm \
  --gpus all \
  -p 30000:8000 \
  vllm/vllm-openai:latest \
  --model /path/to/model
```

## 📝 常见问题

### Q1: 系统是否还能使用dots.ocr？
**A**: 可以！前端传递 `ocrServiceType: "dotsocr"` 即可切换。

### Q2: RapidOCR禁用后有什么影响？
**A**: 无影响。RapidOCR只用于幻觉校验，MinerU自身精度高，不需要额外校验。

### Q3: 如何临时切换回dots.ocr作为默认？
**A**: 修改 `CompareOptions.java` 中的默认值：
```java
private String ocrServiceType = "dotsocr";
```

### Q4: MinerU识别很慢怎么办？
**A**: 
1. 切换backend为 `pipeline` 模式（更快但精度略低）
2. 或者为简单文档切换回 `dotsocr`

## 🎯 最佳实践

1. **默认使用MinerU** - 获得最高识别精度
2. **复杂文档优先** - MinerU特别适合多列、表格多的文档
3. **监控性能** - 注意识别耗时，必要时切换引擎
4. **灵活切换** - 根据文档类型在前端选择合适的引擎

## 📚 相关文档

- [MinerU集成完整文档](./MINERU_INTEGRATION_FINAL.md)
- [MinerU快速启动](./MINERU_QUICK_START.md)
- [CompareOptions API](./src/main/java/com/zhaoxinms/contract/tools/comparePRO/model/CompareOptions.java)

---

**当前状态**: ✅ MinerU已作为默认OCR引擎，RapidOCR已禁用

**最后更新**: 2025-10-07

