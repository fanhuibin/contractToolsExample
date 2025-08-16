# 🔍 **进度调试指南**

## 🐛 **当前问题**
进度和页数信息仍然显示为 0.0% 和 0/0，需要调试Python输出是否被Java正确读取。

## 🛠️ **新增的调试功能**

### 1. **Python输出监控**
现在Java会显示所有Python脚本的输出：
```
Python输出[1]: 2025-08-14 16:25:24,123 - INFO - 开始OCR识别 (RapidOCR)，PDF: D:\git\zhaoxin-contract-tool-set\compareScript\test.pdf
Python输出[2]: 2025-08-14 16:25:24,124 - INFO - PDF总页数: 5
Python输出[3]: 2025-08-14 16:25:24,125 - INFO - 第 1/5 页渲染完成
...
总共读取了 XX 行Python输出
```

### 2. **进度解析监控**
当解析到进度信息时会显示：
```
解析到总页数: 5
解析到进度: 1/5 = 20.0%
解析到进度: 2/5 = 40.0%
```

### 3. **错误流监控**
如果Python有错误输出，也会被显示：
```
Python错误[1]: 某些错误信息
总共读取了 XX 行Python错误输出
```

## 🧪 **测试步骤**

### 步骤1：运行测试
```bash
cd D:\git\zhaoxin-contract-tool-set\backend
java JavaOCRExample
```

### 步骤2：提交任务并监控
1. **选择选项1** - 提交OCR任务
2. **选择选项3** - 监控任务进度

### 步骤3：观察调试输出

#### 🟢 **正常情况应该看到**：
```
Python输出[1]: 2025-08-14 16:25:24,123 - INFO - 开始OCR识别 (RapidOCR)，PDF: D:\git\zhaoxin-contract-tool-set\compareScript\test.pdf
Python输出[2]: 2025-08-14 16:25:24,124 - INFO - 设置: DPI=150, 最小置信度=0.5, 调试模式=开启
Python输出[3]: 2025-08-14 16:25:24,125 - INFO - PDF总页数: 5
解析到总页数: 5
Python输出[4]: 2025-08-14 16:25:24,126 - INFO - 第 1/5 页渲染完成
解析到进度: 1/5 = 20.0%
Python输出[5]: 2025-08-14 16:25:24,127 - INFO - 第 2/5 页渲染完成
解析到进度: 2/5 = 40.0%
...
总共读取了 25 行Python输出
```

#### 🔴 **可能的问题情况**：

**情况1：没有Python输出**
```
总共读取了 0 行Python输出
```
**原因**：Python脚本没有运行或没有输出到stdout
**解决**：检查Python环境和脚本路径

**情况2：有输出但没有解析到进度**
```
Python输出[1]: 一些输出...
Python输出[2]: 一些输出...
总共读取了 10 行Python输出
```
但没有看到"解析到总页数"或"解析到进度"
**原因**：输出格式与解析逻辑不匹配
**解决**：检查Python输出的具体格式

**情况3：有错误输出**
```
Python错误[1]: ImportError: No module named 'rapidocr_onnxruntime'
```
**原因**：Python依赖缺失
**解决**：安装缺失的依赖

## 🔧 **可能的修复方案**

### 方案1：Python依赖问题
```bash
cd D:\git\zhaoxin-contract-tool-set\compareScript
pip install rapidocr-onnxruntime
```

### 方案2：Python路径问题
检查配置文件中的Python路径是否正确：
```yaml
ocr:
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
```

### 方案3：输出格式问题
如果Python输出的格式与预期不同，我们需要调整解析逻辑。

## 📋 **请提供的调试信息**

运行测试后，请提供以下信息：
1. **Python输出的所有内容**（从"Python输出[1]"开始的所有行）
2. **是否看到"解析到总页数"和"解析到进度"**
3. **是否有Python错误输出**
4. **最终的任务状态和进度显示**

这些信息将帮助我们精确定位问题所在！
