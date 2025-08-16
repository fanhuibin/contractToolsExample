# OCR 快速设置指南

## 🚨 **当前问题诊断**

根据您的错误信息 "Python进程退出码: 2"，问题很可能是以下之一：

### **1. 缺少测试PDF文件**
需要在以下位置放置测试PDF文件：
```
D:\git\zhaoxin-contract-tool-set\compareScript\test.pdf
```

### **2. 缺少Python依赖**
需要安装RapidOCR依赖：
```bash
pip install rapidocr-onnxruntime
```

### **3. Python脚本路径问题**
确保脚本存在：
```
D:\git\zhaoxin-contract-tool-set\compareScript\rapid_pdf_ocr.py
```

## 🔧 **快速修复步骤**

### 步骤1：检查配置文件
确保 `backend/src/main/resources/application-ocr.yml` 中的路径正确：
```yaml
ocr:
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
```

### 步骤2：运行环境诊断
重新运行程序，选择菜单选项 **8. 运行环境诊断**，这会告诉您具体缺少什么。

### 步骤3：准备测试PDF文件
在 `D:\git\zhaoxin-contract-tool-set\compareScript\` 目录下放置一个名为 `test.pdf` 的PDF文件。

### 步骤4：安装Python依赖
打开命令行，运行：
```bash
cd D:\git\zhaoxin-contract-tool-set\compareScript
pip install rapidocr-onnxruntime
```

### 步骤5：测试Python脚本
手动测试脚本是否能正常运行：
```bash
cd D:\git\zhaoxin-contract-tool-set\compareScript
python rapid_pdf_ocr.py --help
```

## 🔍 **详细诊断命令**

如果上述步骤不能解决问题，请运行以下命令进行详细诊断：

### 检查Python环境
```bash
python --version
```

### 检查RapidOCR安装
```bash
python -c "import rapidocr_onnxruntime; print('RapidOCR安装正常')"
```

### 检查脚本语法
```bash
python -m py_compile rapid_pdf_ocr.py
```

### 手动运行OCR测试
```bash
python rapid_pdf_ocr.py --pdf test.pdf --debug --log_file test.log
```

## 📁 **目录结构检查**

确保您的目录结构如下：
```
D:\git\zhaoxin-contract-tool-set\compareScript\
├── rapid_pdf_ocr.py          # OCR主脚本
├── test.pdf                  # 测试PDF文件
├── logs\                     # 日志目录（自动创建）
└── output\                   # 输出目录（自动创建）
```

## 🐛 **常见错误及解决方案**

### 错误：ModuleNotFoundError: No module named 'rapidocr_onnxruntime'
**解决方案**：
```bash
pip install rapidocr-onnxruntime
```

### 错误：FileNotFoundError: [Errno 2] No such file or directory: 'python'
**解决方案**：
1. 确保Python已安装并添加到系统PATH
2. 或者使用完整路径：`C:\Python39\python.exe`

### 错误：Permission denied
**解决方案**：
1. 以管理员身份运行命令提示符
2. 检查文件权限

## 🎯 **测试流程**

1. **运行程序**：启动 `JavaOCRExample`
2. **选择诊断**：菜单选项 8 - 运行环境诊断
3. **修复问题**：根据诊断结果修复问题
4. **重新测试**：菜单选项 1 - 提交OCR任务

## 📞 **需要帮助？**

如果问题仍然存在，请提供以下信息：
1. 环境诊断的完整输出
2. `D:\git\zhaoxin-contract-tool-set\compareScript` 目录的文件列表
3. Python版本信息：`python --version`
4. 是否安装了RapidOCR：`pip list | findstr rapid`

---

**提示**：大多数 "Python进程退出码: 2" 错误都是由于缺少依赖包或文件路径问题导致的。按照上述步骤逐一检查即可解决。
