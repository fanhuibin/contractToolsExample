# MinerU OCR集成文档

## 概述

本模块实现了基于MinerU的OCR识别服务，用于替代dots.ocr作为合同比对的识别引擎。

## 主要特性

1. **PDF直接识别** - 无需预先将PDF拆分为图片，直接提交完整PDF文件
2. **并行处理** - 同时进行MinerU识别和图片生成，提高效率
3. **坐标转换** - 自动将MinerU的PDF坐标系转换为图片坐标系
4. **页眉页脚过滤** - 支持基于类型和位置的智能过滤
5. **多种内容类型** - 支持文本、标题、列表、表格等20+种内容类型

## 核心类

### MinerUOCRService
主服务类，提供PDF识别功能

**主要方法:**
- `recognizePdf(File pdfFile, String taskId, File outputDir, boolean ignoreHeaderFooter, double headerHeightPercent, double footerHeightPercent)` - 识别PDF并返回结果

**返回结果格式:**
```json
{
  "fileName": "test.pdf",
  "totalPages": 3,
  "processingTimeMs": 12345,
  "pageData": {
    "0": [
      {
        "text": "文本内容",
        "bbox": [100, 200, 300, 220],
        "type": "text",
        "textLevel": 1
      }
    ]
  },
  "pageImages": [
    {
      "pageIndex": 0,
      "imagePath": "/path/to/page-1.png",
      "imageWidth": 1654,
      "imageHeight": 2339,
      "pdfWidth": 595.0,
      "pdfHeight": 842.0
    }
  ]
}
```

### MinerUCoordinateConverter
坐标转换工具类

**主要方法:**
- `getPdfPageSize(File pdfFile, int pageIndex)` - 获取PDF页面原始尺寸
- `convertToImageCoordinates(...)` - 转换MinerU坐标到图片坐标
- `isValidBbox(...)` - 验证坐标有效性
- `clampBbox(...)` - 修正超出边界的坐标

## 配置说明

在`application.yml`中配置：

```yaml
# MinerU OCR配置
mineru:
  api:
    url: http://192.168.0.100:8000  # MinerU Web API地址
  vllm:
    server:
      url: http://192.168.0.100:30000  # vLLM Server地址
  backend: vlm-http-client  # 后端模式

# 通用OCR配置（所有引擎共享）
zxcm:
  compare:
    zxocr:
      render-dpi: 160  # PDF转图片的DPI（MinerU和dots.ocr都使用）
```

**重要提示**：
- 页眉页脚过滤参数通过`CompareOptions`传入，不从配置文件读取
- DPI设置统一使用`zxcm.compare.zxocr.render-dpi`配置

### Backend模式说明

MinerU支持多种backend模式：
- `pipeline` - 基础管道模式（不使用VLM）
- `vlm-transformers` - 使用transformers库的VLM模式
- `vlm-vllm-async-engine` - 使用内嵌vLLM的异步引擎模式
- `vlm-http-client` - 使用外部vLLM Server的HTTP客户端模式（推荐）
- `vlm-sglang-engine` - 使用SGLang的引擎模式

## MinerU内容类型

MinerU识别出的内容类型包括：

| 类型 | 说明 |
|------|------|
| text | 普通文本 |
| title | 标题 |
| equation | 行间公式 |
| image | 图片 |
| image_caption | 图片描述 |
| image_footnote | 图片脚注 |
| table | 表格 |
| table_caption | 表格描述 |
| table_footnote | 表格脚注 |
| phonetic | 拼音 |
| code | 代码块 |
| code_caption | 代码描述 |
| ref_text | 参考文献 |
| algorithm | 算法块 |
| list | 列表 |
| header | 页眉 |
| footer | 页脚 |
| page_number | 页码 |
| aside_text | 装订线旁注 |
| page_footnote | 页面脚注 |

## 坐标系统说明

### MinerU坐标系
- MinerU使用与PDF相同的坐标系统
- 单位：点（point），1英寸 = 72点
- 原点：左下角
- 常见PDF尺寸：A4 = 595×842点

### 图片坐标系
- 单位：像素（pixel）
- 原点：左上角
- 尺寸取决于渲染DPI

### 坐标转换公式
```
scaleX = imageWidth / pdfWidth
scaleY = imageHeight / pdfHeight

imageX = mineruX * scaleX
imageY = mineruY * scaleY
```

## 页眉页脚过滤

支持两种过滤方式：

1. **基于类型** - 过滤type为`header`、`footer`、`page_number`的内容
2. **基于位置** - 过滤Y坐标在顶部/底部阈值范围内的内容

阈值配置：
```yaml
mineru:
  header:
    footer:
      threshold: 5.0  # 距离顶部/底部的距离（相对于PDF高度的百分比）
```

## 使用示例

### 基本使用

```java
@Autowired
private MinerUOCRService mineruOcrService;

public void recognizePdf() throws Exception {
    File pdfFile = new File("test.pdf");
    String taskId = UUID.randomUUID().toString();
    File outputDir = new File("./uploads/ocr-results");
    
    // 设置页眉页脚过滤参数
    boolean ignoreHeaderFooter = true;
    double headerHeightPercent = 12.0;
    double footerHeightPercent = 12.0;
    
    Map<String, Object> result = mineruOcrService.recognizePdf(
        pdfFile, taskId, outputDir,
        ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);
    
    System.out.println("识别完成，共" + result.get("totalPages") + "页");
}
```

### 在合同比对中使用

```java
// 通过CompareOptions传入参数
CompareOptions options = new CompareOptions();
options.setOcrServiceType("mineru");  // 选择MinerU引擎
options.setIgnoreHeaderFooter(true);
options.setHeaderHeightPercent(12.0);
options.setFooterHeightPercent(12.0);

// 调用识别
Map<String, Object> result = mineruOcrService.recognizePdf(
    pdfFile, taskId, outputDir,
    options.isIgnoreHeaderFooter(),
    options.getHeaderHeightPercent(),
    options.getFooterHeightPercent()
);
```

详细使用示例请参考：[USAGE_EXAMPLE.md](./USAGE_EXAMPLE.md)

### 测试类

使用`MinerUOCRServiceTest`进行测试：

```bash
# 启用测试
export MINERU_TEST_ENABLED=true

# 运行应用
java -jar contract-tools-sdk.jar
```

## 与dots.ocr的区别

| 特性 | dots.ocr | MinerU |
|------|----------|--------|
| 输入方式 | 需要预先拆分图片 | 直接提交PDF |
| 处理速度 | 快 | 较慢（VLM处理） |
| 识别精度 | 高 | 更高（支持复杂布局） |
| 内容类型 | 文本+坐标 | 20+种结构化类型 |
| 坐标系统 | 图片坐标 | PDF坐标（需转换） |

## 性能优化

1. **并行处理** - MinerU识别和图片生成同时进行
2. **连接池** - 复用HTTP连接
3. **超时设置** - 合理设置连接和读取超时
   - 连接超时：60秒
   - 读取超时：30分钟（VLM处理较慢）

## 故障排查

### 连接失败
- 检查MinerU服务是否启动
- 检查配置的URL是否正确
- 检查网络连接

### 识别超时
- 增加`readTimeout`设置
- 检查vLLM Server是否正常运行
- 考虑使用更快的backend模式

### 坐标不准确
- 检查PDF是否为标准格式
- 检查renderDpi设置
- 验证坐标转换逻辑

## 开发者指南

### 添加新的内容类型处理

在`convertMinerUToCharBox`方法中添加：

```java
if (item.has("custom_field")) {
    charBox.put("customField", item.get("custom_field").asText());
}
```

### 自定义坐标转换

修改`MinerUCoordinateConverter`类的转换逻辑：

```java
public static int[] customConversion(double[] mineruBbox, ...) {
    // 自定义转换逻辑
}
```

## 参考资料

- [MinerU官方文档](https://opendatalab.github.io/MinerU/)
- [MinerU Docker部署](https://opendatalab.github.io/MinerU/zh/quick_start/docker_deployment/)
- [vLLM文档](https://docs.vllm.ai/)

## 更新日志

### v1.0.0 (2025-10-07)
- ✅ 实现基础PDF识别功能
- ✅ 实现并行图片生成
- ✅ 实现坐标转换
- ✅ 实现页眉页脚过滤
- ✅ 支持多种内容类型
- ✅ 添加配置管理
- ✅ 添加测试类

## License

Copyright © 2025 ZhaoXin

