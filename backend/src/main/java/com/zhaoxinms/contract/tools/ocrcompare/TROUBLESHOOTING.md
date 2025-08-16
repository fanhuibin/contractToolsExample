# OCR 任务故障排除指南

## 🚨 常见错误及解决方案

### 错误1：Python进程退出码: 2

**症状**：
- 任务状态显示"失败 (FAILED)"
- 错误信息：`Python进程退出码: 2`
- 任务执行时间很短（通常不到1秒）

**原因分析**：
退出码2通常表示以下问题之一：
1. Python脚本语法错误
2. 缺少必要的Python包
3. Python环境配置问题
4. 脚本路径或权限问题

**解决步骤**：

#### 步骤1：运行环境诊断
```java
// 在JavaOCRManualTest中调用
diagnoseOCREnvironment();
```

#### 步骤2：检查Python环境和脚本文件
确保以下文件存在：
- `D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py` (OCR主脚本文件)
- 系统已安装Python并可通过命令行访问
- 必要的Python依赖包已安装

#### 步骤3：检查Python依赖包
在命令行中运行：
```bash
cd D:\git\zhaoxin-contract-tool-set\compareScript
python -c "import rapidocr_onnxruntime; print('RapidOCR依赖检查通过')"
```

#### 步骤4：手动测试Python脚本
```bash
# 进入compareScript目录
cd D:\git\zhaoxin-contract-tool-set\compareScript

# 测试rapid_pdf_ocr.py脚本
python rapid_pdf_ocr.py --help

# 测试具体功能（使用实际PDF文件）
python rapid_pdf_ocr.py --pdf test.pdf --log_file logs/test.log --debug
```

#### 步骤5：检查日志文件
OCR任务运行后，会在以下位置生成日志文件：
- **Python日志**：`D:\git\zhaoxin-contract-tool-set\compareScript\logs\[任务ID]_ocr.log`
- **结果文件**：`D:\git\zhaoxin-contract-tool-set\compareScript\output\[任务ID]_result.json`

查看日志文件内容可以帮助诊断具体的错误原因。

### 错误2：PDF文件不存在

**症状**：
- 错误信息：`PDF文件不存在: [路径]`

**解决方案**：
1. 检查 `TEST_PDF_PATH` 常量设置
2. 确保PDF文件确实存在于指定路径
3. 检查文件权限

### 错误3：OCR组件初始化失败

**症状**：
- 错误信息：`OCR组件未正确初始化`

**解决方案**：
1. 检查 `OCRProperties` 配置
2. 确保Python路径正确
3. 检查反射设置是否成功

## 🔧 诊断工具使用

### 1. 环境诊断
```java
// 自动检查所有环境配置
diagnoseOCREnvironment();
```

### 2. 手动检查
```java
// 检查特定任务状态
testQueryTaskStatus("your_task_id");

// 检查OCR组件状态
if (javaOCR == null || ocrTaskService == null) {
    System.err.println("OCR组件未正确初始化");
}
```

## 📋 检查清单

在运行OCR任务前，请确认：

- [ ] Python环境已正确安装
- [ ] PaddleOCR包已安装 (`pip install paddlepaddle paddleocr`)
- [ ] OCR脚本文件存在且无语法错误
- [ ] PDF文件路径正确且文件存在
- [ ] 有足够的磁盘空间和内存
- [ ] Python环境变量设置正确

## 🐛 调试技巧

### 1. 启用详细日志
在OCR脚本中添加：
```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

### 2. 检查Python错误输出
修改Java代码以捕获Python的错误输出：
```java
// 在ProcessBuilder中
processBuilder.redirectErrorStream(true);
```

### 3. 逐步测试
1. 先测试Python环境：`python --version`
2. 再测试依赖包：`python -c "import paddle"`
3. 最后测试完整脚本

## 📞 获取帮助

如果问题仍然存在，请提供：

1. **错误日志**：完整的错误信息和堆栈跟踪
2. **环境信息**：
   - Python版本
   - 操作系统版本
   - PaddleOCR版本
3. **配置文件**：OCR相关的配置文件内容
4. **测试文件**：使用的PDF文件（如果可能）

## 🔄 常见修复

### 修复1：重新安装PaddleOCR
```bash
pip uninstall paddlepaddle paddleocr
pip install paddlepaddle paddleocr
```

### 修复2：检查Python路径
确保Python在系统PATH中，或使用完整路径：
```java
python.setRoot("C:\\Python39"); // 使用实际Python安装路径
```

### 修复3：权限问题
以管理员身份运行Java程序，或检查文件权限设置。

---

**注意**：大多数OCR任务失败问题都与Python环境配置有关。建议先运行 `diagnoseOCREnvironment()` 进行全面诊断。
