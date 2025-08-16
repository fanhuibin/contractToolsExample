# OCR配置加载测试指南

## 🐛 问题描述

OCR比对失败，错误信息：`旧文档OCR识别失败: OCR执行异常: null`

**根本原因**: OCR配置没有正确配置在`sdk`项目中。

## ✅ 已修复的问题

我已经在`sdk/src/main/resources/application.yml`中添加了OCR配置：

```yaml
# OCR配置
ocr:
  # Python脚本根目录 - compareScript目录的完整路径
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
  
  # OCR参数配置
  settings:
    dpi: 150                # 图像DPI设置 (150-300)
    minScore: 0.5           # 文字识别最低置信度 (0.0-1.0)
    fastMode: false         # 快速模式 (牺牲精度换取速度)
    debugMode: true         # 调试模式 (输出详细日志)
    enableLogging: true     # 启用日志记录
  
  # 任务配置
  task:
    timeout: 20                    # 任务超时时间（分钟）
    resultRetentionDays: 7         # 结果保存天数
    statusCheckInterval: 5         # 任务状态检查间隔（秒）
```

## 🚀 测试步骤

### 1. 重新启动sdk项目

```bash
cd sdk
mvn clean compile
mvn spring-boot:run
```

### 2. 检查启动日志

启动时应该能看到OCR配置相关的日志，特别是：

```
com.zhaoxinms.contract.tools: DEBUG
```

### 3. 测试OCR配置加载

启动服务后，尝试提交一个OCR比对任务，查看控制台输出：

**如果配置正确加载，应该看到**:
```
🚀 开始执行OCR任务: OCR_1234567890_abc12345
📁 PDF文件: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\old_1234567890.docx (大小: 12345 bytes)
🐍 Python路径: D:\git\zhaoxin-contract-tool-set\compareScript
⚡ 执行命令: python D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py --pdf ...
```

**如果配置未加载，会看到**:
```
❌ OCR任务执行异常: OCR配置未正确加载，请检查application-ocr.yml配置文件
```

### 4. 验证Python脚本目录

确认以下路径存在：

```bash
# 检查目录是否存在
ls -la D:\git\zhaoxin-contract-tool-set\compareScript

# 检查OCR脚本是否存在
ls -la D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py
```

## 🔧 如果仍有问题

### 问题1: 配置文件路径错误

**症状**: 启动时显示"Could not load config file"
**解决**: 确保OCR配置在`sdk/src/main/resources/application.yml`中

### 问题2: 配置文件语法错误

**症状**: 启动时显示YAML语法错误
**解决**: 检查YAML语法，确保缩进正确

### 问题3: 属性绑定失败

**症状**: 配置加载了但`OCRProperties`仍然为null
**解决**: 检查Java类的注解和属性名是否与YAML匹配

## 📊 验证清单

### 配置加载验证
- [ ] `sdk/application.yml`中包含了OCR配置
- [ ] 启动日志显示OCR配置已加载
- [ ] 无配置相关的错误信息

### 属性绑定验证
- [ ] `OCRProperties`对象不为null
- [ ] `ocrProperties.getPython().getRoot()`返回正确路径
- [ ] `ocrProperties.getSettings().getDpi()`返回150
- [ ] `ocrProperties.getTask().getTimeout()`返回20

### 文件路径验证
- [ ] Python脚本目录存在
- [ ] OCR脚本文件存在
- [ ] 目录有读取权限

## 🎯 测试用例

### 测试用例1: 配置加载测试
1. 启动sdk项目
2. 检查启动日志
3. 确认OCR配置已加载

### 测试用例2: 属性绑定测试
1. 提交OCR比对任务
2. 检查控制台输出
3. 验证配置属性正确

### 测试用例3: 完整流程测试
1. 上传Word文档
2. 提交OCR比对
3. 监控任务进度
4. 查看比对结果

## 🎉 成功标志

当配置正确加载后，您应该看到：

1. **启动日志**: 显示OCR配置已加载
2. **配置验证**: 无配置相关的错误信息
3. **任务执行**: OCR任务正常启动和执行
4. **Python执行**: 控制台显示Python命令执行

## 📞 技术支持

如果仍有问题，请提供：

1. sdk项目启动时的完整日志
2. 提交OCR任务时的控制台输出
3. `sdk/application.yml`文件内容
4. 具体的错误信息

## 🔍 调试技巧

### 1. 添加调试日志

在`OCRTaskService`中添加更多日志：

```java
System.out.println("🔧 OCR配置信息:");
System.out.println("  - Python路径: " + (ocrProperties != null ? ocrProperties.getPython().getRoot() : "NULL"));
System.out.println("  - DPI设置: " + (ocrProperties != null ? ocrProperties.getSettings().getDpi() : "NULL"));
System.out.println("  - 超时设置: " + (ocrProperties != null ? ocrProperties.getTask().getTimeout() : "NULL"));
```

### 2. 检查Spring配置

在启动类上添加调试注解：

```java
@SpringBootApplication
@EnableConfigurationProperties(OCRProperties.class)
public class SdkApplication {
    // ...
}
```

### 3. 验证配置文件位置

确保文件结构如下：

```
sdk/
  src/
    main/
      resources/
        application.yml          ← 主配置文件（包含OCR配置）
        application-ai.yml       ← AI配置文件
```
