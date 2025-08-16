# OCR配置测试指南

## 🐛 问题描述

OCR比对失败，错误信息：`旧文档OCR识别失败: OCR执行异常: null`

## 🔍 问题分析

这个错误通常由以下原因引起：

1. **OCR配置未正确加载** - `OCRProperties`为null
2. **Python路径配置错误** - 找不到Python脚本目录
3. **OCR脚本文件缺失** - `rapid_pdf_ocr.py`不存在
4. **Python环境问题** - Python未安装或版本不兼容

## 🚀 测试步骤

### 1. 检查配置文件

确认`backend/src/main/resources/application-ocr.yml`文件存在且配置正确：

```yaml
ocr:
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
  
  settings:
    dpi: 150
    minScore: 0.5
    fastMode: false
    debugMode: true
    enableLogging: true
  
  task:
    timeout: 20
    resultRetentionDays: 7
    statusCheckInterval: 5
```

### 2. 检查Python脚本目录

确认以下目录和文件存在：

```bash
# 检查目录是否存在
ls -la D:\git\zhaoxin-contract-tool-set\compareScript

# 检查OCR脚本是否存在
ls -la D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py

# 检查Python环境
cd D:\git\zhaoxin-contract-tool-set\compareScript
python --version
```

### 3. 检查Python依赖

在`compareScript`目录下执行：

```bash
pip list | grep rapidocr
pip list | grep onnxruntime
```

如果没有安装，请安装依赖：

```bash
pip install rapidocr-onnxruntime
```

### 4. 测试Python脚本

手动测试OCR脚本：

```bash
cd D:\git\zhaoxin-contract-tool-set\compareScript
python rapid_pdf_ocr.py --help
```

### 5. 检查后端日志

启动后端服务，查看控制台输出：

```bash
cd sdk
mvn spring-boot:run
```

应该能看到以下日志：

```
🚀 开始执行OCR任务: OCR_1234567890_abc12345
📁 PDF文件: D:\git\zhaoxin-contract-tool-set\sdk\uploads\compare\old_1234567890.docx (大小: 12345 bytes)
🐍 Python路径: D:\git\zhaoxin-contract-tool-set\compareScript
⚡ 执行命令: python D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py --pdf ...
```

## 🔧 常见问题修复

### 问题1: OCR配置未加载

**症状**: 控制台显示"OCR配置未正确加载"
**原因**: `application-ocr.yml`没有被正确导入
**解决**: 确保`application.yml`中包含了OCR配置：

```yaml
spring:
  config:
    import:
      - classpath:application-ocr.yml
```

### 问题2: Python路径错误

**症状**: 控制台显示"Python脚本目录不存在"
**原因**: 配置文件中的路径不正确
**解决**: 修改`application-ocr.yml`中的路径，使用绝对路径

### 问题3: OCR脚本缺失

**症状**: 控制台显示"OCR脚本文件不存在"
**原因**: `rapid_pdf_ocr.py`文件不存在
**解决**: 从项目根目录复制脚本文件到`compareScript`目录

### 问题4: Python环境问题

**症状**: 控制台显示"Python进程退出码: 1"
**原因**: Python环境配置错误或依赖缺失
**解决**: 
1. 确认Python已安装且版本>=3.7
2. 安装必要的依赖包
3. 检查Python路径是否正确

## 📊 验证清单

### 配置验证
- [ ] `application-ocr.yml`文件存在
- [ ] 配置文件语法正确
- [ ] Python路径配置正确
- [ ] 所有属性名与Java类匹配

### 文件验证
- [ ] `compareScript`目录存在
- [ ] `rapid_pdf_ocr.py`脚本存在
- [ ] Python可执行文件存在
- [ ] 目录有读取权限

### 环境验证
- [ ] Python版本>=3.7
- [ ] 依赖包已安装
- [ ] 脚本可手动执行
- [ ] 日志目录可写

### 服务验证
- [ ] 后端服务正常启动
- [ ] OCR配置正确加载
- [ ] 任务提交成功
- [ ] 错误信息清晰明确

## 🎯 测试用例

### 测试用例1: 基本配置测试
1. 启动后端服务
2. 提交OCR比对任务
3. 检查控制台日志
4. 验证任务状态

### 测试用例2: 错误处理测试
1. 删除OCR脚本文件
2. 提交OCR比对任务
3. 检查错误信息
4. 验证错误处理

### 测试用例3: 完整流程测试
1. 上传Word文档
2. 提交OCR比对
3. 监控任务进度
4. 查看比对结果

## 🎉 成功标志

当所有测试通过后，您应该看到：

1. **配置加载成功**: 无配置相关错误
2. **Python脚本执行**: 控制台显示Python命令执行
3. **任务状态更新**: 任务从PROCESSING到COMPLETED
4. **结果生成**: 生成OCR比对结果

## 📞 技术支持

如果仍有问题，请提供：

1. 后端控制台的完整日志
2. `application-ocr.yml`文件内容
3. Python环境信息
4. 具体的错误信息
