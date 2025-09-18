# PDFBox字体警告问题解决方案

## 问题描述

在保存图片时出现以下警告信息：
```
2025-09-15 18:41:36 [pool-1-thread-1] WARN  org.apache.fontbox.ttf.CmapSubtable - Format 14 cmap table is not supported and will be ignored
```

## 问题分析

### 根本原因
这个警告出现在PDF渲染为图片的过程中，具体是在使用Apache PDFBox库处理PDF文档的字体时产生的。虽然用户认为是在"保存图片"，但实际上系统在进行OCR比对时，需要先将PDF文档渲染成图片，然后进行OCR识别。

### 技术细节
1. **触发位置**：`OcrImageSaver.java` 和 `GPUOCRCompareService.java` 中的PDF渲染过程
2. **具体代码**：`renderer.renderImageWithDPI(i, dpi)` 方法调用时
3. **技术原因**：PDFBox在处理某些PDF文档的字体时，遇到了Format 14格式的cmap表，这是Unicode变体选择器（Variation Selectors）的字符映射表格式，PDFBox 2.0.29版本不完全支持这种格式

### 为什么会出现
- PDF文档中包含了复杂的中文字体或特殊字符
- 字体使用了较新的Unicode标准（如emoji、变体字符等）
- PDFBox版本相对较旧，对新格式支持有限

## 解决方案

### 方案1：日志配置抑制（推荐方案）

**修改文件**：`backend/src/main/resources/application.yml`

```yaml
logging:
  level:
    com.zhaoxinms.contract.tools: debug
    org.springframework.web: debug
    # 抑制PDFBox字体警告
    org.apache.fontbox.ttf.CmapSubtable: ERROR
```

**优势**：
- 快速解决日志噪音问题
- 不影响现有功能
- 无需代码修改
- 保持版本稳定性

### 方案2：升级PDFBox版本（不推荐）

**原因**：PDFBox 3.x版本有大量API变更，会导致以下问题：
- `PDDocument.load(File)` 方法签名变更
- `SUB_TYPE_HIGHLIGHT` 常量位置变更  
- `setSubtype()` 方法可见性变更
- 需要大量代码修改和测试

**如果必须升级，需要修改的API包括**：
- `PDDocument.load(File)` → `PDDocument.load(File, password)`
- `PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT` → `PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT`
- 其他API调用方式

**建议**：除非有特殊需求，否则不建议升级到3.x版本。

## 最终推荐方案

**使用方案1：日志配置抑制**

这是最安全、最快速的解决方案，不会影响现有功能，只需要添加一行日志配置即可。

**实施步骤**：
1. 保持PDFBox版本为2.0.29
2. 在application.yml中添加日志配置
3. 重启应用即可生效

**效果**：
- 完全消除字体警告信息
- 保持系统稳定性
- 不影响PDF处理和OCR功能

## 实施建议

1. **推荐使用日志配置抑制**：快速、安全地解决警告问题
2. **避免升级PDFBox**：除非有特殊需求，否则不建议升级到3.x版本
3. **测试重点**：确保PDF渲染和OCR功能正常工作

## 影响范围

- **功能影响**：无，警告不影响实际功能
- **性能影响**：升级PDFBox可能带来性能提升
- **兼容性**：需要测试现有PDF文档的处理效果

## 相关文件

- `backend/src/main/java/com/zhaoxinms/contract/tools/ocrcompare/util/OcrImageSaver.java`
- `backend/src/main/java/com/zhaoxinms/contract/tools/ocrcompare/compare/GPUOCRCompareService.java`
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`

## 总结

这个警告是PDFBox库在处理复杂字体时的正常现象，不影响系统功能。通过升级PDFBox版本可以根本解决，通过日志配置可以快速抑制警告显示。建议优先考虑升级方案以获得更好的长期支持。
