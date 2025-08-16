# RapidOCR PDF文字识别工具

## 简介
RapidOCR是一个基于ONNX运行时的快速OCR文字识别工具，专门用于PDF文档的文字提取和坐标定位。支持CPU和GPU两种执行模式。

## 功能特性
- 🚀 快速PDF文字识别
- 📍 精确的文字坐标定位
- 🔍 字符级边界框提取
- 📊 置信度评分
- 📄 支持多页PDF处理
- 💾 多种输出格式（文本、JSON坐标）
- 🎯 **GPU加速支持**（CUDA/ROCm）
- ⚙️ **智能执行提供程序选择**

## 安装依赖

### CPU版本
```bash
pip install rapidocr-onnxruntime PyMuPDF opencv-python numpy
```

### GPU版本（推荐）
```bash
# CUDA版本（NVIDIA显卡）
pip install rapidocr-onnxruntime-cuda PyMuPDF opencv-python numpy

# 或者通用GPU版本
pip install rapidocr-onnxruntime-gpu PyMuPDF opencv-python numpy

# 额外GPU工具（可选）
pip install pynvml  # GPU信息监控
```

## 系统要求

### CPU版本
- Python 3.7+
- 内存：4GB+
- 处理器：支持AVX2指令集

### GPU版本
- **CUDA**: 11.0+ (推荐11.8或12.1)
- **cuDNN**: 8.0+
- **GPU内存**: 至少2GB显存
- **显卡**: NVIDIA GTX 1060+ 或 RTX系列
- **驱动**: 最新NVIDIA驱动

## 基本用法

### 1. 简单文字识别
```bash
python rapid_pdf_ocr.py --pdf input.pdf --out output.txt
```

### 2. 提取文字坐标
```bash
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json
```

### 3. 快速模式（推荐用于大批量处理）
```bash
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json --fast
```

### 4. GPU加速模式
```bash
# 自动检测GPU
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json

# 强制使用GPU
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json

# 限制GPU内存使用
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json --gpu_memory_limit 4

# 强制使用CPU（禁用GPU）
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coordinates.json --cpu_only
```

## 参数说明

| 参数 | 说明 | 默认值 | 示例 |
|------|------|--------|------|
| `--pdf` | 输入PDF文件路径 | 必需 | `--pdf document.pdf` |
| `--out` | 输出路径 | `output` | `--out results` |
| `--dpi` | 渲染DPI（影响精度和速度） | `150` | `--dpi 200` |
| `--min_score` | 最小置信度阈值 | `0.5` | `--min_score 0.7` |
| `--fast` | 快速模式（DPI=120） | 关闭 | `--fast` |
| `--export_char_boxes` | 导出坐标JSON文件 | 无 | `--export_char_boxes coords.json` |
| `--debug` | 启用调试模式 | 关闭 | `--debug` |
| `--log_file` | 日志文件路径 | 无 | `--log_file ocr.log` |
| `--cpu_only` | 强制使用CPU | 关闭 | `--cpu_only` |
| `--gpu_memory_limit` | GPU内存限制（GB） | 无 | `--gpu_memory_limit 4` |

## GPU配置详解

### 1. 自动GPU检测
程序会自动检测可用的GPU执行提供程序：
- **CUDAExecutionProvider**: NVIDIA显卡
- **ROCMExecutionProvider**: AMD显卡
- **CPUExecutionProvider**: CPU回退

### 2. GPU内存管理
```bash
# 限制GPU内存使用为4GB
--gpu_memory_limit 4

# 限制GPU内存使用为8GB
--gpu_memory_limit 8
```

### 3. 强制CPU模式
```bash
# 在GPU环境下强制使用CPU
--cpu_only
```

### 4. GPU性能优化
```bash
# 高精度GPU模式
python rapid_pdf_ocr.py --pdf input.pdf --out output --dpi 200 --gpu_memory_limit 6

# 快速GPU模式
python rapid_pdf_ocr.py --pdf input.pdf --out output --fast --gpu_memory_limit 2
```

## 输出说明

### 文本输出
- 如果输出路径以`.txt`结尾：生成单个合并文件
- 如果输出路径不以`.txt`结尾：生成目录，包含按页分割的文件和`combined.txt`

### 坐标JSON输出
JSON文件包含以下信息：
```json
{
  "pdf": "PDF文件绝对路径",
  "pages": [
    {
      "page_index": 页码,
      "image_width": 图片宽度,
      "image_height": 图片高度,
      "dpi": 渲染DPI,
      "items": [
        {
          "text": "识别出的文字",
          "score": 置信度分数,
          "box": [[x1,y1], [x2,y2], [x3,y3], [x4,y4]],
          "chars": [
            {
              "ch": "字符",
              "box": [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]
            }
          ]
        }
      ]
    }
  ]
}
```

## 性能优化建议

### 1. DPI设置
- **高精度**：DPI 200-300（适合高质量文档）
- **平衡模式**：DPI 150（推荐，精度和速度平衡）
- **快速模式**：DPI 120（适合大批量处理）

### 2. 置信度阈值
- **严格模式**：min_score = 0.8（只保留高置信度结果）
- **标准模式**：min_score = 0.5（推荐）
- **宽松模式**：min_score = 0.3（保留更多可能的结果）

### 3. GPU配置优化
- **小文档**：GPU内存限制2-4GB
- **中等文档**：GPU内存限制4-6GB
- **大文档**：GPU内存限制6-8GB
- **批量处理**：使用快速模式 + 适当GPU内存限制

### 4. 批量处理
```bash
# 处理多个PDF文件（GPU模式）
for pdf in *.pdf; do
    python rapid_pdf_ocr.py --pdf "$pdf" --out "output_${pdf%.pdf}" --fast --gpu_memory_limit 4
done
```

## 调试模式

### 启用详细日志
```bash
python rapid_pdf_ocr.py --pdf input.pdf --out output --debug
```

### 查看处理进度
程序会显示：
- 每页的渲染时间
- OCR识别时间
- 识别出的文字行数
- 总体处理进度
- **GPU使用情况**
- **执行提供程序信息**

### GPU调试信息
启用调试模式后，会显示：
- GPU检测结果
- 执行提供程序选择
- GPU内存使用情况
- CUDA版本信息

## 常见问题

### Q: 识别速度慢怎么办？
A: 使用`--fast`参数、降低DPI值，或启用GPU加速

### Q: 识别精度不高怎么办？
A: 提高DPI值，调整置信度阈值

### Q: 内存占用过高怎么办？
A: 降低DPI值，使用快速模式，或限制GPU内存使用

### Q: 支持哪些PDF格式？
A: 支持标准PDF格式，包括扫描版和文本版

### Q: GPU版本安装失败怎么办？
A: 检查CUDA版本兼容性，确保显卡驱动最新

### Q: 如何选择CPU还是GPU？
A: 小文档用CPU，大文档或批量处理用GPU

### Q: GPU内存不足怎么办？
A: 使用`--gpu_memory_limit`限制内存，或使用`--cpu_only`强制CPU模式

## 技术架构

- **PDF处理**：PyMuPDF (fitz)
- **图像处理**：OpenCV + NumPy
- **OCR引擎**：RapidOCR (ONNX运行时)
- **坐标计算**：自定义字符级边界框算法
- **GPU加速**：CUDA/ROCm执行提供程序
- **智能选择**：自动执行提供程序检测

## 版本信息
- Python版本：3.7+
- 主要依赖：rapidocr-onnxruntime-gpu, PyMuPDF, OpenCV
- GPU支持：CUDA 11.0+, ROCm
- 开发环境：Windows/Linux/macOS

## 许可证
本项目基于开源许可证发布，具体请查看LICENSE文件。
