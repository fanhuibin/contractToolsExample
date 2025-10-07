# 统一OCR服务说明

## 概述

统一OCR服务基于合同比对中成熟的OCR实现，提供了统一的OCR识别能力，支持多种OCR提供者，避免了重复开发。

## 支持的OCR提供者

1. **DotSOCR** (默认)
   - 基于GPU的高性能OCR服务
   - 适用于高精度文档识别

2. **阿里云通义千问** (qwen/aliyun/third-party)
   - 基于大语言模型的多模态OCR
   - 适用于复杂版面和多语言文档

3. **RapidOCR** (rapidocr)
   - 轻量级OCR解决方案
   - 适用于简单文档和备用场景

## 配置说明

### 基础配置

```yaml
zxcm:
  # OCR提供者选择: dotsocr, qwen/aliyun/third-party, rapidocr
  ocr:
    provider: dotsocr
```

### DotSOCR配置

```yaml
zxcm:
  compare:
    zxocr:
      ocr-base-url: http://192.168.0.100:8000
      ocr-model: model
      render-dpi: 160
      parallel-threads: 4
      save-ocr-images: false
```

### 阿里云通义千问配置

```yaml
zxcm:
  compare:
    third-party-ocr:
      enabled: true
      api-key: your-api-key
      default-model: qwen3-vl-235b-a22b-instruct
      timeout: PT2M
      max-concurrency: 5
```

### RapidOCR配置

```yaml
zxcm:
  compare:
    rapidocr:
      enabled: true
      base-url: http://192.168.0.100:9005
      timeout: PT2M
      max-concurrency: 10
```

## 环境变量支持

可以通过环境变量覆盖配置：

- `OCR_DOTSOCR_ENDPOINT`: DotSOCR服务地址
- `OCR_DOTSOCR_MODEL`: DotSOCR模型名称
- `OCR_DOTSOCR_RENDER_DPI`: PDF转图片DPI
- `OCR_QWEN_ENABLED`: 是否启用通义千问
- `OCR_QWEN_API_KEY`: 通义千问API密钥
- `OCR_RAPID_ENABLED`: 是否启用RapidOCR
- `OCR_RAPID_ENDPOINT`: RapidOCR服务地址

## 使用方式

### 在合同信息抽取中使用

服务会自动注入`UnifiedOCRService`，无需额外配置：

```java
@Autowired
private UnifiedOCRService unifiedOCRService;

// 识别PDF
OCRService.OCRResult result = unifiedOCRService.recognizePdf(pdfFile);

// 识别图片
OCRService.OCRResult result = unifiedOCRService.recognizeImage(imageFile);
```

### 在合同比对中使用

合同比对功能继续使用原有的配置和服务，无需更改。

## 任务数据存储

统一OCR服务会自动将识别结果保存到任务文件夹：

```
uploads/extract-tasks/{taskId}/
├── ocr_result.txt          # OCR识别的原始文本
├── ocr_metadata.json       # OCR元数据（提供者、置信度等）
├── extract_result.json     # 信息提取结果
├── visualization.html      # 可视化结果
└── task_status.json        # 任务状态
```

## 服务切换

通过修改`zxcm.ocr.provider`配置即可切换OCR服务：

1. `dotsocr`: 使用DotSOCR服务
2. `qwen`/`aliyun`/`third-party`: 使用阿里云通义千问
3. `rapidocr`/`rapid`: 使用RapidOCR

## 故障回退

- 阿里云通义千问失败时，会自动回退到DotSOCR
- 图片识别时，如果主要服务不支持，会回退到RapidOCR

## 性能优化

- DotSOCR: 适合批量处理，GPU加速
- 通义千问: 适合复杂文档，但有API调用限制
- RapidOCR: 适合轻量级场景，CPU处理

## 注意事项

1. 确保对应的OCR服务已启动并可访问
2. 阿里云通义千问需要有效的API密钥
3. 不同OCR服务的识别精度和速度可能有差异
4. 建议根据实际场景选择合适的OCR提供者
