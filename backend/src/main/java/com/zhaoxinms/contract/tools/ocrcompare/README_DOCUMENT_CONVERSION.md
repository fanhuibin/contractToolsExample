# OCR比对文档转换功能说明

## 功能概述

OCR比对系统现在支持多种文档格式的自动转换，包括：
- **Word文档**: .doc, .docx, .docm
- **Excel表格**: .xls, .xlsx, .xlsm, .xlsb
- **PDF文档**: .pdf (无需转换)

## 技术架构

### 核心组件

1. **OCRDocumentConverterService** - 文档转换服务
   - 负责检查文件格式
   - 调用OnlyOffice服务进行转换
   - 管理临时PDF文件
   - 验证转换结果

2. **OCRCompareService** - OCR比对服务
   - 集成文档转换流程
   - 管理5步比对流程
   - 协调OCR识别和比对

3. **ChangeFileToPDFService** - OnlyOffice转换服务
   - 利用OnlyOffice Document Server
   - 支持多种格式转换
   - 异步转换处理

### 转换流程

```
上传文件 → 格式检查 → 转换为PDF → OCR识别 → 文本比对 → 生成结果
    ↓           ↓         ↓         ↓         ↓         ↓
  支持格式   格式验证    OnlyOffice   RapidOCR   差异分析   PDF标注
```

## 支持的文件格式

### Word文档
- `.doc` - Microsoft Word 97-2003
- `.docx` - Microsoft Word 2007+
- `.docm` - Microsoft Word 2007+ (启用宏)

### Excel表格
- `.xls` - Microsoft Excel 97-2003
- `.xlsx` - Microsoft Excel 2007+
- `.xlsm` - Microsoft Excel 2007+ (启用宏)
- `.xlsb` - Microsoft Excel 2007+ (二进制)

### PDF文档
- `.pdf` - 便携式文档格式 (无需转换)

## 使用方法

### 1. 前端文件上传

```vue
<template>
  <input
    type="file"
    accept=".pdf,.doc,.docx,.docm,.xls,.xlsx,.xlsm,.xlsb,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    @change="onFileChange"
  />
</template>
```

### 2. 后端API调用

```java
// 检查文件格式
if (!documentConverterService.isFileFormatSupported(fileName)) {
    throw new IllegalArgumentException("不支持的文件格式");
}

// 转换为PDF
String pdfPath = documentConverterService.convertToPdfIfNeeded(
    filePath, taskId, uploadDir);
```

### 3. 比对选项配置

```java
OCRCompareOptions options = new OCRCompareOptions();
options.setIgnoreHeaderFooter(true);
options.setIgnoreCase(true);
options.setIgnoreSpaces(false);  // 新增：忽略空格选项
```

## 配置要求

### OnlyOffice Document Server

确保OnlyOffice服务正常运行：

```yaml
# application.yml
onlyoffice:
  server:
    url: http://localhost:80
    callback:
      url: http://localhost:8080/api
```

### 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

## 性能优化

### 1. 转换超时设置

```java
// 最长等待120秒
long deadline = System.currentTimeMillis() + 120_000L;
```

### 2. 临时文件清理

```java
// 自动清理转换后的临时PDF文件
documentConverterService.cleanupTempPdfFiles(oldPdfPath, newPdfPath);
```

### 3. 内存管理

- 转换完成后立即清理临时文件
- 使用流式处理避免大文件内存占用
- 支持大文件分块处理

## 错误处理

### 常见错误类型

1. **格式不支持**
   ```
   错误: 不支持的文件格式，仅支持PDF、Word、Excel格式
   解决: 检查文件扩展名，确保在支持列表中
   ```

2. **转换失败**
   ```
   错误: 文档转换失败
   解决: 检查OnlyOffice服务状态，验证文件完整性
   ```

3. **转换超时**
   ```
   错误: 文档转换超时
   解决: 增加超时时间，检查网络连接
   ```

### 错误恢复策略

- 自动重试机制
- 详细的错误日志
- 用户友好的错误提示
- 支持手动重试

## 监控和日志

### 日志级别

```java
@Slf4j
public class OCRDocumentConverterService {
    
    // 转换开始
    log.info("开始转换文档为PDF: {} -> {}", filePath, tempPdfPath);
    
    // 转换成功
    log.info("文档转换成功: {} -> {} (大小: {} bytes)", 
            filePath, convertedPath, convertedFile.length());
    
    // 转换失败
    log.error("文档转换失败，转换路径为空或文件不存在: {}", convertedPath);
}
```

### 性能指标

- 转换成功率
- 平均转换时间
- 文件大小分布
- 错误率统计

## 扩展性

### 1. 新增格式支持

```java
// 在SUPPORTED_FORMATS中添加新格式
private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
    ".pdf", ".doc", ".docx", ".docm", 
    ".xls", ".xlsx", ".xlsm", ".xlsb",
    ".ppt", ".pptx", ".pptm"  // 新增PowerPoint支持
);
```

### 2. 自定义转换器

```java
// 实现自定义转换接口
public interface DocumentConverter {
    String convert(String sourcePath, String targetPath);
    boolean supports(String format);
}
```

### 3. 转换策略配置

```yaml
document:
  conversion:
    strategy: onlyoffice  # onlyoffice, libreoffice, custom
    timeout: 120
    retry-count: 3
    cleanup-temp: true
```

## 测试用例

### 1. 格式支持测试

```java
@Test
public void testSupportedFormats() {
    assertTrue(converter.isFileFormatSupported("document.docx"));
    assertTrue(converter.isFileFormatSupported("spreadsheet.xlsx"));
    assertFalse(converter.isFileFormatSupported("image.jpg"));
}
```

### 2. 转换功能测试

```java
@Test
public void testDocToPdfConversion() {
    String pdfPath = converter.convertToPdfIfNeeded(
        "test.docx", "task123", "/tmp/");
    assertNotNull(pdfPath);
    assertTrue(new File(pdfPath).exists());
}
```

### 3. 错误处理测试

```java
@Test
public void testInvalidFileHandling() {
    assertThrows(RuntimeException.class, () -> {
        converter.convertToPdfIfNeeded(
            "nonexistent.docx", "task123", "/tmp/");
    });
}
```

## 部署注意事项

### 1. 系统要求

- Java 8+
- OnlyOffice Document Server
- 足够的磁盘空间（临时文件）
- 网络访问权限

### 2. 安全考虑

- 文件上传验证
- 临时文件权限控制
- 网络访问限制
- 日志信息脱敏

### 3. 监控告警

- 转换失败率监控
- 磁盘空间监控
- 服务响应时间监控
- 错误日志告警

## 总结

OCR比对系统的文档转换功能大大提升了系统的实用性，现在用户可以：

1. **直接上传Word/Excel文档**，无需手动转换
2. **享受自动化的转换流程**，提高工作效率
3. **获得一致的比对体验**，无论原始格式如何
4. **减少用户操作步骤**，简化工作流程

该功能基于成熟的OnlyOffice技术，确保了转换的稳定性和准确性，为OCR比对系统提供了强有力的支持。
