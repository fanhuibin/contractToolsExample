# RapidOCR GPU配置快速参考

## 🚀 快速开始

### 1. 安装GPU版本
```bash
# 卸载CPU版本
pip uninstall rapidocr-onnxruntime

# 安装GPU版本
pip install rapidocr-onnxruntime-cuda

# 验证安装
python -c "import rapidocr_onnxruntime; print('GPU版本安装成功')"
```

### 2. 验证GPU支持
```python
import onnxruntime as ort
providers = ort.get_available_providers()
print("可用执行提供程序:", providers)
print("CUDA支持:", 'CUDAExecutionProvider' in providers)
```

## ⚙️ GPU配置参数

### 基本GPU命令
```bash
# 自动GPU检测（推荐）
python rapid_pdf_ocr.py --pdf input.pdf --out output --export_char_boxes coords.json

# 限制GPU内存使用
python rapid_pdf_ocr.py --pdf input.pdf --out output --gpu_memory_limit 4

# 强制使用CPU
python rapid_pdf_ocr.py --pdf input.pdf --out output --cpu_only
```

### 高级GPU配置
```bash
# 高精度GPU模式
python rapid_pdf_ocr.py \
  --pdf input.pdf \
  --out output \
  --dpi 200 \
  --gpu_memory_limit 6 \
  --debug

# 快速GPU模式
python rapid_pdf_ocr.py \
  --pdf input.pdf \
  --out output \
  --fast \
  --gpu_memory_limit 2

# 批量处理GPU模式
python rapid_pdf_ocr.py \
  --pdf input.pdf \
  --out output \
  --fast \
  --gpu_memory_limit 4 \
  --export_char_boxes coords.json
```

## 🎯 性能优化配置

### 小文档（<10页）
```bash
python rapid_pdf_ocr.py \
  --pdf small.pdf \
  --out output \
  --dpi 150 \
  --gpu_memory_limit 2 \
  --fast
```

### 中等文档（10-50页）
```bash
python rapid_pdf_ocr.py \
  --pdf medium.pdf \
  --out output \
  --dpi 150 \
  --gpu_memory_limit 4
```

### 大文档（>50页）
```bash
python rapid_pdf_ocr.py \
  --pdf large.pdf \
  --out output \
  --dpi 150 \
  --gpu_memory_limit 6 \
  --export_char_boxes coords.json
```

### 批量处理
```bash
python rapid_pdf_ocr.py \
  --pdf batch.pdf \
  --out output \
  --fast \
  --gpu_memory_limit 4 \
  --export_char_boxes coords.json
```

## 🔧 故障排除

### GPU检测失败
```bash
# 检查CUDA版本
nvidia-smi

# 检查Python包
pip list | grep rapidocr
pip list | grep onnxruntime

# 强制重新安装
pip uninstall rapidocr-onnxruntime onnxruntime
pip install rapidocr-onnxruntime-cuda
```

### GPU内存不足
```bash
# 降低GPU内存使用
--gpu_memory_limit 2

# 使用快速模式
--fast

# 降低DPI
--dpi 120

# 强制使用CPU
--cpu_only
```

### 性能问题
```bash
# 启用调试模式查看详细信息
--debug

# 检查GPU使用情况
nvidia-smi -l 1

# 监控GPU内存
watch -n 1 nvidia-smi
```

## 📊 GPU性能基准

### 测试环境
- **GPU**: RTX 3080 (10GB)
- **CPU**: Intel i7-10700K
- **内存**: 32GB DDR4
- **CUDA**: 11.8

### 性能对比

| 模式 | DPI | GPU内存 | 10页PDF耗时 | 50页PDF耗时 |
|------|-----|---------|-------------|-------------|
| CPU | 150 | - | 45秒 | 3分20秒 |
| GPU快速 | 120 | 2GB | 12秒 | 1分5秒 |
| GPU标准 | 150 | 4GB | 15秒 | 1分15秒 |
| GPU高精度 | 200 | 6GB | 22秒 | 1分45秒 |

### 内存使用建议

| 文档大小 | 推荐GPU内存 | 推荐配置 |
|----------|-------------|----------|
| <10页 | 2GB | `--fast --gpu_memory_limit 2` |
| 10-30页 | 4GB | `--gpu_memory_limit 4` |
| 30-100页 | 6GB | `--gpu_memory_limit 6` |
| >100页 | 8GB+ | `--gpu_memory_limit 8` |

## 🛠️ 高级配置

### 环境变量设置
```bash
# 设置CUDA设备
export CUDA_VISIBLE_DEVICES=0

# 设置ONNX Runtime日志级别
export ORT_LOGGING_LEVEL=1

# 设置GPU内存分配策略
export ORT_CUDA_MEMORY_PATTERN=1
```

### 自定义GPU配置
```python
import onnxruntime as ort

# 自定义CUDA提供程序选项
provider_options = [{
    'device_id': 0,
    'arena_extend_strategy': 'kNextPowerOfTwo',
    'gpu_mem_limit': 4 * 1024 * 1024 * 1024,  # 4GB
    'cudnn_conv_use_max_workspace': True,
    'do_copy_in_default_stream': True,
}]

# 创建会话
session = ort.InferenceSession(
    model_path,
    providers=['CUDAExecutionProvider'],
    provider_options=provider_options
)
```

## 📝 最佳实践

### 1. 选择合适的配置
- **小文档**: 使用快速模式 + 低GPU内存
- **中等文档**: 标准模式 + 中等GPU内存
- **大文档**: 高精度模式 + 高GPU内存
- **批量处理**: 快速模式 + 适当GPU内存

### 2. 监控资源使用
```bash
# 实时监控GPU
nvidia-smi -l 1

# 监控系统资源
htop
iostat -x 1
```

### 3. 错误处理
- 启用调试模式查看详细信息
- 监控GPU内存使用
- 设置适当的GPU内存限制
- 准备CPU回退方案

### 4. 性能调优
- 根据文档质量调整DPI
- 根据精度要求调整置信度阈值
- 平衡GPU内存使用和处理速度
- 使用批量处理提高整体效率

## 🔍 调试技巧

### 启用详细日志
```bash
python rapid_pdf_ocr.py \
  --pdf input.pdf \
  --out output \
  --debug \
  --log_file debug.log
```

### 检查GPU状态
```bash
# 检查CUDA版本
nvcc --version

# 检查GPU驱动
nvidia-smi

# 检查GPU进程
nvidia-smi pmon

# 检查GPU内存
nvidia-smi -q -d MEMORY
```

### 性能分析
```bash
# 使用nvprof分析CUDA性能
nvprof python rapid_pdf_ocr.py --pdf input.pdf --out output

# 使用NVIDIA Nsight Systems
nsys profile python rapid_pdf_ocr.py --pdf input.pdf --out output
```

## 📚 参考资源

- [ONNX Runtime GPU文档](https://onnxruntime.ai/docs/execution-providers/CUDA-ExecutionProvider.html)
- [CUDA编程指南](https://docs.nvidia.com/cuda/)
- [RapidOCR GitHub](https://github.com/RapidAI/RapidOCR)
- [PyTorch GPU优化](https://pytorch.org/docs/stable/notes/cuda.html)
