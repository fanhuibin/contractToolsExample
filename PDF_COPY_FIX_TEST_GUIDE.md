# PDF文件复制修复测试指南

## 🐛 问题描述

用户发现当上传PDF文件时，PDF文档为空的问题出现在`ensurePdf`方法中：
- 在调用`ensurePdf`之前，`newSrc`文件能正常访问
- 调用`ensurePdf`之后，`newSrc`和`newPdf`文件都是空的

## 🔍 问题分析

**根本原因**: `ensurePdf`方法中的文件复制逻辑有问题

**问题代码**:
```java
if ("pdf".equalsIgnoreCase(ext)) {
    // 直接复制/重命名为目标PDF
    try (java.io.InputStream in = new java.io.FileInputStream(src);
         java.io.FileOutputStream out = new java.io.FileOutputStream(destPdf)) {
        StreamUtils.copy(in, out);  // ← 这里可能有问题
    }
    return;
}
```

**可能的原因**:
1. `StreamUtils.copy`没有正确复制文件内容
2. 文件流没有正确关闭
3. 缓冲区大小不合适
4. 文件权限问题

## ✅ 已修复的问题

### 1. 替换StreamUtils.copy

**修复前**:
```java
StreamUtils.copy(in, out);
```

**修复后**:
```java
// 使用缓冲区复制，确保数据正确传输
byte[] buffer = new byte[8192];
int bytesRead;
while ((bytesRead = in.read(buffer)) != -1) {
    out.write(buffer, 0, bytesRead);
}
out.flush();
```

### 2. 添加文件大小验证

**新增验证**:
```java
// 验证复制后的文件大小
if (destPdf.length() == 0) {
    throw new IllegalStateException("PDF文件复制失败，目标文件为空: " + destPdf.getAbsolutePath());
}
```

### 3. 添加详细调试日志

**文件保存验证**:
```java
System.out.println("文件保存验证:");
System.out.println("  旧文件: " + oldSrc.getAbsolutePath() + " (大小: " + oldSrc.length() + " bytes)");
System.out.println("  新文件: " + newSrc.getAbsolutePath() + " (大小: " + newSrc.length() + " bytes)");
```

**PDF转换日志**:
```java
System.out.println("开始PDF转换:");
System.out.println("  旧文件 -> PDF: " + oldSrc.getName() + " -> " + oldPdf.getName());
ensurePdf(request, oldSrc, oldPdf);
System.out.println("  新文件 -> PDF: " + newSrc.getName() + " -> " + newPdf.getName());
ensurePdf(request, newSrc, newPdf);
```

**转换结果验证**:
```java
System.out.println("PDF转换结果验证:");
System.out.println("  旧PDF: " + oldPdf.getAbsolutePath() + " (大小: " + oldPdf.length() + " bytes)");
System.out.println("  新PDF: " + newPdf.getAbsolutePath() + " (大小: " + newPdf.length() + " bytes)");
```

## 🚀 测试步骤

### 1. 重新编译和启动服务

```bash
cd sdk
mvn clean compile
mvn spring-boot:run
```

### 2. 测试PDF文件上传

1. **准备测试文件**: 准备一个正常的PDF文件（确保文件大小 > 0）
2. **上传文件**: 使用前端界面上传两个PDF文件
3. **观察控制台输出**: 应该能看到详细的调试日志

### 3. 预期输出

**如果修复成功，应该看到**:
```
文件保存验证:
  旧文件: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\old_20250814211412481.pdf (大小: 12345 bytes)
  新文件: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\new_20250814211412481.pdf (大小: 67890 bytes)

开始PDF转换:
  旧文件 -> PDF: old_20250814211412481.pdf -> old_20250814211412481.pdf
PDF文件复制成功: old_20250814211412481.pdf -> old_20250814211412481.pdf (大小: 12345 -> 12345 bytes)
  新文件 -> PDF: new_20250814211412481.pdf -> new_20250814211412481.pdf
PDF文件复制成功: new_20250814212481.pdf -> new_20250814212481.pdf (大小: 67890 -> 67890 bytes)

PDF转换结果验证:
  旧PDF: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\old_20250814211412481.pdf (大小: 12345 bytes)
  新PDF: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\new_20250814211412481.pdf (大小: 67890 bytes)
```

**如果仍有问题，会看到**:
```
PDF文件复制失败，目标文件为空: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\new_20250814211412481.pdf
```

或者

```
PDF转换失败，生成的PDF文件为空
```

## 🔧 故障排除

### 问题1: 文件保存失败

**症状**: 文件保存验证显示大小为0
**可能原因**: 
- 磁盘空间不足
- 文件权限问题
- `MultipartFile.transferTo()`失败

**解决**: 检查磁盘空间和文件权限

### 问题2: PDF复制失败

**症状**: PDF文件复制失败，目标文件为空
**可能原因**:
- 源文件被锁定
- 目标目录权限问题
- 文件系统问题

**解决**: 检查文件锁定状态和目录权限

### 问题3: 非PDF文件转换失败

**症状**: 非PDF文件（如.docx）转换后为空
**可能原因**:
- OnlyOffice服务不可用
- 网络配置问题
- 转换服务配置错误

**解决**: 检查OnlyOffice服务状态和网络配置

## 📊 验证清单

### 文件保存验证
- [ ] 旧文件成功保存，大小 > 0
- [ ] 新文件成功保存，大小 > 0
- [ ] 控制台显示正确的文件大小信息

### PDF转换验证
- [ ] PDF文件直接复制成功
- [ ] 复制后的文件大小与源文件一致
- [ ] 控制台显示"PDF文件复制成功"消息

### 错误处理验证
- [ ] 如果文件为空，抛出明确的错误信息
- [ ] 错误信息包含文件路径和具体问题描述

## 🎯 成功标志

当修复成功后，您应该看到：

1. **文件上传成功**: 两个PDF文件都能正常上传和保存
2. **PDF转换成功**: 生成的PDF文件大小与源文件一致
3. **详细日志输出**: 控制台显示完整的文件处理过程
4. **OCR比对正常**: 不再出现"PDF文件为空"的错误

## 🔍 调试技巧

### 1. 检查文件系统

```bash
# 检查文件是否存在和大小
dir D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare

# 检查文件权限
icacls D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare
```

### 2. 监控文件变化

在文件操作前后，手动检查文件大小：
```bash
# 上传前
dir test.pdf

# 上传后
dir D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\new_*.pdf
```

### 3. 查看详细日志

确保日志级别设置为DEBUG：
```yaml
logging:
  level:
    com.zhaoxinms.contract.template.sdk: DEBUG
```

现在PDF文件复制问题应该已经修复！🎉
