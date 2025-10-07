# 🎯 MinerU完整集成指南

## 📋 功能总览

✅ MinerU OCR服务完全集成到合同比对系统  
✅ 支持配置文件控制OCR引擎选择  
✅ 支持列表项（listItems）自动展开  
✅ 完整的JSON保存（原始+过滤+未过滤）  
✅ 坐标自动转换和边界修正  
✅ 页眉页脚智能过滤  
✅ 图片保存到正确目录  

---

## 🚀 快速开始

### 1. 配置OCR引擎

**文件**: `contract-tools-sdk/src/main/resources/application.yml`

```yaml
zxcm:
  compare:
    zxocr:
      # 默认OCR引擎（全局配置）
      default-ocr-service: mineru  # mineru | dotsocr | thirdparty
      
      # PDF转图片DPI（所有引擎共享）
      render-dpi: 160
      
      # MinerU配置
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
```

### 2. 启动MinerU服务

```bash
# 使用Docker启动MinerU（包含vLLM）
docker-compose -f dots.ocr-master/docker/docker-compose.yml up -d
```

### 3. 编译并启动应用

```bash
cd D:\git\zhaoxin-contract-tool-set

# 编译core和sdk模块
mvn clean install -DskipTests -pl contract-tools-core,contract-tools-sdk -am

# 启动服务
cd contract-tools-sdk
mvn spring-boot:run
```

### 4. 测试

上传两个PDF文件进行比对，查看日志确认使用MinerU：

```
✅ MinerU OCR服务已注入并可用
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
🔍 OCR服务配置: mineru
✅ 使用MinerU OCR服务
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx, 模式: old
并行处理：提交PDF识别和生成图片
MinerU识别完成: 6页, 753个CharBox
```

---

## 📁 目录结构

```
uploads/compare-pro/tasks/{taskId}/
├── images/
│   ├── old/                           ← MinerU生成的原文档图片
│   │   ├── page-1.png (1322x1870)
│   │   ├── page-2.png
│   │   └── ...
│   └── new/                           ← MinerU生成的新文档图片
│       ├── page-1.png (1322x1870)
│       ├── page-2.png
│       └── ...
├── ocr/                               ← MinerU识别结果JSON
│   ├── mineru_raw_old.json                    ← API原始响应
│   ├── mineru_raw_new.json
│   ├── mineru_processed_old_unfiltered.json   ← 处理后-全部数据
│   ├── mineru_processed_old_filtered.json     ← 处理后-已过滤（实际使用）
│   ├── mineru_processed_new_unfiltered.json
│   └── mineru_processed_new_filtered.json
├── old_xxx.pdf
└── new_xxx.pdf
```

---

## 🔧 核心功能详解

### 1. 配置驱动的OCR选择

**文件**: `ZxOcrConfig.java` + `CompareService.java`

**配置属性**:
```java
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {
    private String defaultOcrService = "mineru";  // 全局默认
    // ...
}
```

**使用逻辑**:
```java
// 强制使用配置文件中的OCR，忽略前端传递的值
String configuredOcrService = gpuOcrConfig.getDefaultOcrService();
options.setOcrServiceType(configuredOcrService);
```

### 2. 列表项（listItems）支持

**MinerU返回的列表格式**:
```json
{
  "listItems": [
    "1、第一项内容...",
    "2、第二项内容..."
  ],
  "bbox": [320, 471, 1322, 1037],
  "type": "list"
}
```

**自动展开处理**:
```java
// CompareService.java
if (listItems != null && !listItems.isEmpty()) {
    // 计算每个列表项的垂直位置（平均分配）
    double itemHeight = totalHeight / listItems.size();
    
    // 为每个列表项创建独立的CharBox
    for (int itemIdx = 0; itemIdx < listItems.size(); itemIdx++) {
        int[] itemBbox = calculateItemBbox(bbox, itemIdx, itemHeight);
        charBoxes.addAll(splitTextToCharBoxes(itemText, itemBbox, pageIdx));
    }
}
```

### 3. 坐标转换和修正

**两级修正策略**:

**第一级：修正MinerU原始坐标**
```java
// MinerU可能返回超出PDF尺寸的坐标
if (mineruBbox[2] > pdfWidth || mineruBbox[3] > pdfHeight) {
    log.warn("⚠️  MinerU返回的坐标超出PDF尺寸！");
    mineruBbox[2] = Math.min(mineruBbox[2], pdfWidth);
    mineruBbox[3] = Math.min(mineruBbox[3], pdfHeight);
}
```

**第二级：转换后再次验证**
```java
// 转换到图片坐标
int[] imageBbox = MinerUCoordinateConverter.convertToImageCoordinates(
    mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);

// 防止浮点数舍入误差
if (!isValidBbox(imageBbox, imageWidth, imageHeight)) {
    imageBbox = clampBbox(imageBbox, imageWidth, imageHeight);
}
```

### 4. JSON数据保存

**三种JSON文件**:

1. **mineru_raw_*.json**: MinerU API完整原始响应
2. **mineru_processed_*_unfiltered.json**: 转换后保留所有数据（150个块）
3. **mineru_processed_*_filtered.json**: 转换后过滤页眉页脚（95个块）← 实际使用

**保存逻辑**:
```java
// 保存原始响应
saveRawResponse(apiResult, outputDir, taskId, docMode);

// 保存过滤版本（实际使用）
Map<String, Object> filteredResult = parseMinerUResult(..., options);
saveProcessedResult(filteredResult, outputDir, taskId, docMode, "filtered");

// 保存未过滤版本（调试对比）
CompareOptions noFilterOptions = new CompareOptions();
noFilterOptions.setIgnoreHeaderFooter(false);
Map<String, Object> unfilteredResult = parseMinerUResult(..., noFilterOptions);
saveProcessedResult(unfilteredResult, outputDir, taskId, docMode, "unfiltered");
```

### 5. 页眉页脚过滤

**两种过滤方式**:

**基于类型**:
```java
if ("header".equals(type) || "footer".equals(type) || "page_number".equals(type)) {
    return true;  // 过滤
}
```

**基于位置**:
```java
double headerThreshold = pdfHeight * (headerHeightPercent / 100.0);  // 默认12%
double footerThreshold = pdfHeight * (1 - footerHeightPercent / 100.0);  // 默认88%

if (y1 < headerThreshold || y2 > footerThreshold) {
    return true;  // 过滤
}
```

---

## 📊 对比：dots.ocr vs MinerU

| 特性 | dots.ocr | MinerU |
|-----|----------|--------|
| **处理方式** | 逐页图片识别 | 整个PDF识别 |
| **速度** | 较慢（每页单独请求） | 较快（批量处理） |
| **准确度** | 高 | 极高（VLM模型） |
| **布局识别** | 基础 | 高级（表格、列表等） |
| **坐标系统** | 图片像素 | PDF原生+转换 |
| **列表支持** | 无 | ✅ listItems |
| **并行处理** | 按页并行 | PDF识别+图片生成并行 |

---

## 🔍 调试指南

### 1. 确认MinerU已启用

**查看启动日志**:
```
✅ MinerU OCR服务已注入并可用
   MinerU API: http://192.168.0.100:8000
   Backend: vlm-http-client
```

**查看比对日志**:
```
🔍 OCR服务配置: mineru
✅ 使用MinerU OCR服务
```

### 2. 检查坐标转换

**对比 raw 和 processed JSON**:

**raw** (PDF坐标):
```json
"bbox": [100.5, 200.3, 500.2, 250.8]
```

**processed** (图片坐标):
```json
"bbox": [223, 444, 1110, 556]
```

**验证比例**:
```
DPI: 160
scaleX = 1322 / 595.0 = 2.22
scaleY = 1870 / 842.0 = 2.22

100.5 * 2.22 ≈ 223 ✅
200.3 * 2.22 ≈ 444 ✅
```

### 3. 分析过滤效果

**对比两个文件**:
```bash
# 看看过滤掉了什么
diff mineru_processed_old_unfiltered.json mineru_processed_old_filtered.json
```

**调整过滤参数**:
```yaml
# 关闭过滤
options.setIgnoreHeaderFooter(false)

# 或减小范围
options.setHeaderHeightPercent(5)   # 默认12%
options.setFooterHeightPercent(5)   # 默认12%
```

### 4. 验证列表项展开

**unfiltered.json中查看**:
```json
{
  "listItems": ["1、第一项", "2、第二项"],
  "bbox": [320, 471, 1322, 1037]
}
```

**日志中查看CharBox数量**:
```
MinerU识别完成: 6页, 753个CharBox  ← 包含展开后的列表项字符
```

---

## ⚠️  常见问题

### Q1: 坐标超出边界？

**现象**:
```
坐标超出边界，进行修正: [320, 1279, 1892, 1579] -> [320, 1279, 1322, 1579]
```

**原因**: MinerU可能返回超出PDF实际尺寸的坐标（边界扩展）

**解决**: 已自动修正，无需处理

### Q2: 处理后的数据比原始数据少很多？

**原因**: 应用了页眉页脚过滤

**对比**:
- `mineru_raw_*.json`: 150个块
- `mineru_processed_*_unfiltered.json`: 150个块
- `mineru_processed_*_filtered.json`: 95个块（移除了55个页眉页脚）

**解决**: 查看 `unfiltered` 版本确认未误删正文

### Q3: 图片找不到？

**预期路径**:
```
uploads/compare-pro/tasks/{taskId}/images/old/page-1.png
uploads/compare-pro/tasks/{taskId}/images/new/page-1.png
```

**检查日志**:
```
生成页面图片: page-1.png, 尺寸: 1322x1870
```

### Q4: 还是使用dots.ocr？

**检查配置**:
```yaml
zxcm:
  compare:
    zxocr:
      default-ocr-service: mineru  # ← 确认是mineru
```

**检查日志**:
```
🔍 OCR服务配置: mineru  ← 应该显示mineru
```

---

## 📚 相关文档

- `MINERU_DIRECTORY_FIX.md` - 目录结构和JSON保存说明
- `MINERU_COORDINATE_ISSUE.md` - 坐标超出边界问题分析
- `MINERU_JSON_STRUCTURE.md` - JSON文件结构详解
- `MINERU_LIST_SUPPORT.md` - 列表项支持说明
- `MINERU_FINAL_SOLUTION.md` - 配置方案说明

---

## ✅ 功能清单

- [x] MinerU OCR服务集成
- [x] 配置文件控制OCR选择
- [x] 图片保存到正确目录（images/old, images/new）
- [x] JSON保存（raw, unfiltered, filtered）
- [x] 坐标转换（PDF → 图片像素）
- [x] 坐标边界修正
- [x] 页眉页脚过滤
- [x] 列表项（listItems）展开
- [x] 并行处理（API调用 + 图片生成）
- [x] 与前端完全兼容

---

**最后更新**: 2025-10-07  
**状态**: ✅ 完全就绪，可投入生产使用

