# ✅ MinerU完整集成完成

## 🎉 集成状态

### ✅ 已完成
- [x] MinerU服务类创建并放在comparePRO包下
- [x] 配置类ZxOcrConfig添加MinerU配置
- [x] CompareOptions默认使用MinerU
- [x] CompareService集成MinerU识别逻辑
- [x] CharBox转换逻辑实现
- [x] RapidOCR已禁用
- [x] 代码无错误（仅有警告）

## 📁 完整文件列表

### 新增文件
1. **MinerUOCRService.java** - MinerU识别服务
   - 位置: `contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/service/`
   - 功能: PDF整个提交给MinerU识别

2. **MinerUCoordinateConverter.java** - 坐标转换工具
   - 位置: `contract-tools-core/src/main/java/com/zhaoxinms/contract/tools/comparePRO/util/`
   - 功能: PDF坐标转图片坐标

### 修改文件
3. **ZxOcrConfig.java** - 添加MinerU配置
   ```java
   private MinerUConfig mineru = new MinerUConfig();
   ```

4. **CompareOptions.java** - 默认使用MinerU
   ```java
   private String ocrServiceType = "mineru";
   ```

5. **CompareService.java** - 集成MinerU识别
   ```java
   if (options.isUseMinerU()) {
       resultA = recognizePdfWithMinerU(...);
   }
   ```

6. **application.yml** - RapidOCR禁用
   ```yaml
   rapidocr:
     enabled: false
   ```

## 🔄 识别流程

### 使用MinerU时的流程

```
1. CompareService接收比对请求
   ↓
2. 检查options.isUseMinerU() == true
   ↓
3. 调用recognizePdfWithMinerU()
   ↓
4. MinerUOCRService.recognizePdf()
   ├─→ [并行1] callMinerUAPI() - 提交整个PDF到MinerU
   │           ↓
   │       解析content_list JSON
   └─→ [并行2] generatePageImages() - 生成页面图片
               ↓
           等待两个任务完成
   ↓
5. 解析MinerU结果（pageData, pageImages）
   ↓
6. convertToCharBoxList() - 转换为CharBox
   ├─→ 文本块拆分为单个字符
   └─→ 平均分配字符位置
   ↓
7. 返回RecognitionResult(charBoxes, failedPages, totalPages)
   ↓
8. 继续比对流程...
```

### vs dots.ocr流程对比

| 步骤 | dots.ocr | MinerU |
|------|----------|--------|
| 输入 | 逐页图片 | 整个PDF |
| 识别 | 每页单独POST | 一次性POST |
| API | /v1/chat/completions | /file_parse |
| 并行 | 多页并行识别 | PDF识别+图片生成并行 |
| 结果 | 每页JSON | 统一content_list |

## 🚀 使用说明

### 前端无需修改

系统现在默认使用MinerU，前端不需要做任何修改！

```javascript
// 默认行为（使用MinerU）
{
  "oldFileUrl": "...",
  "newFileUrl": "...",
  "ignoreHeaderFooter": true,
  "headerHeightPercent": 12,
  "footerHeightPercent": 12
}
```

### 启动前检查

**1. MinerU服务必须运行**
```bash
# 检查MinerU
curl http://192.168.0.100:8000/docs

# 检查vLLM（如果使用vlm-http-client）
curl http://192.168.0.100:30000/v1/models
```

**2. 重启后端服务**
```bash
cd contract-tools-backend
mvn clean spring-boot:run
```

### 预期日志

启动后上传PDF进行比对，应该看到：

```
使用MinerU OCR识别原文档
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx-old
并行处理：提交PDF识别和生成图片
MinerU识别完成，解析结果...
MinerU识别完成: 6页, 1234个CharBox

使用MinerU OCR识别新文档
使用MinerU识别PDF: new_xxx.pdf, 任务ID: xxx-new
...
```

**不应该看到**:
- ❌ `POST /v1/chat/completions` (这是dots.ocr)
- ❌ `404错误`

## ⚙️ 配置文件

### application.yml

```yaml
zxcm:
  compare:
    zxocr:
      render-dpi: 160
      
      # MinerU配置
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
        
    # RapidOCR禁用
    rapidocr:
      enabled: false
```

## 📊 关键技术点

### 1. CharBox转换

MinerU返回文本块，需要转换为单个字符的CharBox：

```java
// MinerU结果: {text: "合同", bbox: [100, 200, 150, 220]}

// 转换为CharBox:
CharBox('合', [100, 200, 125, 220])  // 第1个字符
CharBox('同', [125, 200, 150, 220])  // 第2个字符
```

### 2. 坐标转换

```java
// PDF坐标 → 图片坐标
scaleX = imageWidth / pdfWidth
scaleY = imageHeight / pdfHeight

imageX = pdfX * scaleX
imageY = pdfY * scaleY
```

### 3. 并行处理

```java
// 同时进行：
CompletableFuture<String> recognition = // MinerU识别
CompletableFuture<List<...>> images = // 图片生成

// 等待完成
String result = recognition.get();
List<...> imgs = images.get();
```

## 🔍 故障排查

### 问题1: 还在调用dots.ocr（404错误）

**原因**: 后端服务未重启

**解决**:
```bash
# 停止服务
Ctrl+C

# 重新启动
mvn clean spring-boot:run
```

### 问题2: MinerU服务未初始化

**日志**: `MinerU服务未初始化`

**原因**: MinerU服务类未被Spring注入

**解决**: 检查MinerUOCRService类是否有`@Service`注解

### 问题3: 识别失败

**日志**: `MinerU识别失败: Connection refused`

**原因**: MinerU服务未启动

**解决**:
```bash
# 启动MinerU
docker-compose up -d mineru vllm
```

## 📝 配置调优

### 如果识别太慢

**方案1**: 切换backend为pipeline
```yaml
mineru:
  backend: pipeline  # 不使用VLM，更快
```

**方案2**: 临时使用dots.ocr
```javascript
// 前端传递
{
  "ocrServiceType": "dotsocr"
}
```

### 如果想恢复dots.ocr为默认

修改`CompareOptions.java`:
```java
private String ocrServiceType = "dotsocr";
```

## ✅ 验证清单

使用前请确认：

- [ ] MinerU服务运行正常（`curl http://192.168.0.100:8000/docs`）
- [ ] vLLM服务运行正常（如果使用vlm-http-client）
- [ ] 后端服务已重启
- [ ] application.yml配置正确
- [ ] 日志显示"使用MinerU OCR识别"
- [ ] 无404错误
- [ ] 识别结果正常

## 📚 相关文档

- [快速启动](./MINERU_QUICK_START.md)
- [完整文档](./MINERU_INTEGRATION_FINAL.md)
- [当前配置](./MINERU_CONFIG_CURRENT.md)
- [配置变更](./CONFIG_CHANGES_SUMMARY.md)

---

**状态**: ✅ 完整集成完成，重启服务后生效

**时间**: 2025-10-07

**版本**: v1.3.0 (最终完整版)

