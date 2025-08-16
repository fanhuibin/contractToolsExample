# OCR 日志功能使用指南

## 📋 **功能概述**

OCR系统现在支持完整的日志记录功能，包括：
- **Python脚本日志**：详细记录OCR识别过程
- **Java应用日志**：记录任务管理和系统状态
- **错误和异常日志**：完整的错误堆栈和调试信息

## 🗂️ **日志文件结构**

```
D:\git\zhaoxin-contract-tool-set\compareScript\
├── logs\                          # 日志目录
│   ├── OCR_1234567890_ocr.log     # Python OCR处理日志
│   ├── OCR_1234567891_ocr.log     # 另一个任务的日志
│   └── ...
├── output\                        # 结果目录
│   ├── OCR_1234567890_result.json # OCR识别结果
│   ├── OCR_1234567891_result.json
│   └── ...
├── rapid_pdf_ocr.py               # OCR主脚本
└── test.pdf                       # 测试PDF文件
```

## ⚙️ **配置说明**

### YAML配置文件
在 `application-ocr.yml` 中配置日志参数：

```yaml
ocr:
  # Python脚本根目录
  python:
    root: D:\\git\\zhaoxin-contract-tool-set\\compareScript
  
  # OCR参数配置
  settings:
    dpi: 150
    min-score: 0.5
    fast-mode: false
    # 启用调试模式以获得详细的Python日志
    debug-mode: true
    enable-logging: true
  
  # 任务配置
  task:
    timeout: 20
    result-retention-days: 7
    status-check-interval: 5
```

### Java代码配置
如果使用手动测试类：

```java
// 设置Python环境
OCRProperties.Python python = new OCRProperties.Python();
python.setRoot("D:\\git\\zhaoxin-contract-tool-set\\compareScript");
ocrProperties.setPython(python);

// 启用调试模式以获得详细的Python日志
OCRProperties.Settings settings = new OCRProperties.Settings();
settings.setDebugMode(true); // Python端输出详细日志
settings.setEnableLogging(true);
ocrProperties.setSettings(settings);
```

## 📊 **日志级别说明**

| 级别 | 描述 | 包含内容 |
|------|------|----------|
| **DEBUG** | 最详细 | 所有操作细节、变量值、执行步骤 |
| **INFO** | 常规信息 | 任务进度、重要状态变化 |
| **WARNING** | 警告 | 非致命错误、性能问题 |
| **ERROR** | 错误 | 严重错误、异常情况 |

## 📝 **日志内容示例**

### Python OCR日志示例
```
2024-01-15 10:30:15 - INFO - 开始OCR识别 (RapidOCR)，PDF: D:\test.pdf
2024-01-15 10:30:15 - INFO - 设置: DPI=150, 最小置信度=0.5, 调试模式=开启
2024-01-15 10:30:15 - INFO - GPU模式: 开启
2024-01-15 10:30:15 - INFO - 日志文件: D:\paddleOCR\paddleOCR\logs\OCR_1234567890_ocr.log
2024-01-15 10:30:16 - INFO - 使用GPU执行提供程序: CUDAExecutionProvider
2024-01-15 10:30:16 - INFO - 模型初始化完成，耗时: 1.23秒
2024-01-15 10:30:16 - INFO - PDF总页数: 3
2024-01-15 10:30:17 - INFO - 第 1/3 页渲染完成
2024-01-15 10:30:17 - INFO - 图片尺寸: 1240x1754 像素
2024-01-15 10:30:17 - DEBUG - 识别文字: '合同编号：2024001' (置信度: 0.987)
2024-01-15 10:30:17 - DEBUG - 识别文字: '签约日期：2024年1月15日' (置信度: 0.945)
2024-01-15 10:30:18 - INFO - 第 1/3 页OCR完成，识别行数: 25
2024-01-15 10:30:20 - INFO - 识别完成！总文字行数: 78
```

### 错误日志示例
```
2024-01-15 10:35:22 - ERROR - 第 2 页OCR识别失败: CUDA out of memory
2024-01-15 10:35:22 - WARNING - GPU内存不足，回退到CPU执行
2024-01-15 10:35:22 - INFO - 强制使用CPU执行
```

## 🔍 **日志查看和分析**

### 1. 实时查看日志
```bash
# Windows
tail -f D:\paddleOCR\paddleOCR\logs\OCR_1234567890_ocr.log

# 或使用PowerShell
Get-Content D:\paddleOCR\paddleOCR\logs\OCR_1234567890_ocr.log -Wait
```

### 2. 搜索特定错误
```bash
# 查找错误信息
findstr /i "error\|failed\|exception" D:\paddleOCR\paddleOCR\logs\*.log

# 查找特定任务的日志
findstr "OCR_1234567890" D:\paddleOCR\paddleOCR\logs\*.log
```

### 3. 分析性能
```bash
# 查找耗时信息
findstr /i "耗时\|完成" D:\paddleOCR\paddleOCR\logs\OCR_1234567890_ocr.log
```

## 🛠️ **故障排除**

### 常见问题

#### 1. 日志文件未生成
**原因**：
- 日志目录权限不足
- 磁盘空间不足
- Python脚本启动失败

**解决方案**：
```java
// 检查日志目录是否存在
File logsDir = new File("D:\\paddleOCR\\paddleOCR\\logs");
if (!logsDir.exists()) {
    boolean created = logsDir.mkdirs();
    System.out.println("日志目录创建: " + created);
}
```

#### 2. 日志文件过大
**解决方案**：
- 调整 `max-log-file-size` 配置
- 定期清理旧日志文件
- 降低日志级别（INFO或WARNING）

#### 3. 日志内容不完整
**原因**：
- 任务异常终止
- Python进程被强制结束

**解决方案**：
- 检查任务状态
- 查看系统资源使用情况
- 增加任务超时时间

## 📈 **最佳实践**

### 1. 日志级别选择
- **开发环境**：使用 `DEBUG` 级别
- **测试环境**：使用 `INFO` 级别  
- **生产环境**：使用 `WARNING` 或 `ERROR` 级别

### 2. 日志文件管理
```bash
# 定期清理过期日志（保留最近7天）
forfiles /p "D:\paddleOCR\paddleOCR\logs" /s /m *.log /d -7 /c "cmd /c del @path"
```

### 3. 监控关键指标
- OCR识别准确率（置信度分布）
- 处理时间（每页耗时）
- 错误率（失败任务比例）
- 资源使用（内存、GPU使用率）

## 🔗 **相关工具**

### 日志分析工具推荐
1. **Notepad++**：基础日志查看
2. **LogExpert**：专业日志分析工具
3. **PowerShell**：命令行日志处理
4. **Python脚本**：自定义日志分析

### 示例分析脚本
```python
# 分析OCR日志的简单脚本
import re
from datetime import datetime

def analyze_ocr_log(log_file):
    with open(log_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 提取识别结果统计
    confidence_scores = re.findall(r'置信度: ([\d.]+)', content)
    avg_confidence = sum(float(s) for s in confidence_scores) / len(confidence_scores)
    
    print(f"平均置信度: {avg_confidence:.3f}")
    print(f"识别项目数: {len(confidence_scores)}")
```

---

**注意**：日志文件包含敏感信息，请妥善保管，避免泄露隐私数据。
