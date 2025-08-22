# 使用 PaddleOCR PP-OCRv5 模型的配置与切换指南

本指南说明如何在本项目中切换并使用 PaddleOCR 的 PP-OCRv5 模型（简称 v5）。内容包括：环境准备、安装命令、快速验证、与现有 OCR HTTP 服务的适配要点，以及常见问题排查。

> 当前仓库的 `compareScript` 目录默认使用 RapidOCR(ONNXRuntime)。如果你计划在系统内完全切换到 PaddleOCR，需要按下文“服务适配要点”修改/替换 Python 服务实现，接口保持不变即可。

---

## 1. 环境准备（Windows 示例）

- Python 3.8 ~ 3.11（建议 64 位）
- 可选：NVIDIA GPU + 正确版本的 CUDA/CUDNN（如需 GPU 推理）
- 命令行使用国内源可加速下载（可选）

创建并激活虚拟环境（推荐）：

```powershell
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install --upgrade pip
```

---

## 2. 安装 PaddlePaddle 与 PaddleOCR

选择 CPU 或 GPU 版本（两者二选一）：

- CPU 版本（最简单）
```powershell
pip install paddlepaddle -i https://mirror.baidu.com/pypi/simple
```

- GPU 版本（按你的 CUDA 版本选择对应轮子）
  - 请到官方“选择器”页面选择合适命令：[Paddle 安装指南](https://www.paddlepaddle.org.cn/install/quick)
  - 例如（示例，实际以官方页面为准）：
```powershell
pip install paddlepaddle-gpu==2.5.2.post120 -i https://mirror.baidu.com/pypi/simple
```

安装 PaddleOCR：
```powershell
pip install paddleocr -i https://mirror.baidu.com/pypi/simple
```

验证安装：
```powershell
python -c "from paddleocr import PaddleOCR; ocr=PaddleOCR(ocr_version='PP-OCRv5', use_angle_cls=True, lang='ch'); print('OK')"
```

首次运行会自动下载 v5 模型权重，保存在 `~/.paddleocr` 目录。你也可以通过参数手动指定模型路径（见下文）。

---

## 3. 快速运行与验证（CLI）

使用命令行验证 v5 模型是否工作正常：

```powershell
paddleocr --image_dir .\imgs\11.jpg --ocr_version PP-OCRv5 --lang ch --use_angle_cls true --use_gpu false
```

- `--ocr_version PP-OCRv5`：指定使用 v5 模型
- `--lang ch`：中文模型（可改为 `en`、`chinese_cht`、`korean` 等）
- `--use_angle_cls true`：启用方向分类
- `--use_gpu false`：CPU 推理（GPU 环境可设为 true）

---

## 4. 在 Python 中使用 v5（API）

```python
from paddleocr import PaddleOCR

ocr = PaddleOCR(
    ocr_version='PP-OCRv5',   # 切换到 v5
    use_angle_cls=True,       # 方向分类
    use_gpu=False,            # 如有 GPU 且已装 paddlepaddle-gpu 可设 True
    lang='ch',                # 语言
    # 可选：手动指定模型目录，跳过自动下载
    # det_model_dir=r'C:\\models\\ppocrv5\\det',
    # rec_model_dir=r'C:\\models\\ppocrv5\\rec',
    # cls_model_dir=r'C:\\models\\ppocrv5\\cls',
)

# img 为 RGB 或 BGR 图像路径/数组；若是 OpenCV BGR 数组建议转换为 RGB
# result 结构为 list[ [ [box(xyxyxyxy)], text, score ], ... ]
result = ocr.ocr(img)
```

- OpenCV 读取的 `img_bgr` 需转换为 RGB：`img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)`
- `ocr.ocr(...)` 的返回结构与 RapidOCR 类似，包含检测框、文本与置信度。

---

## 5. 与现有 OCR HTTP 服务的适配要点

当前服务文件：`compareScript/rapid_pdf_ocr_server.py`

- 该服务使用 RapidOCR 并暴露 REST 接口（`/api/ocr/submit`、`/api/ocr/status/{id}`、`/api/ocr/result/{id}`）。
- 若要切换为 PaddleOCR，推荐新建一个 `paddle_pdf_ocr_server.py`，保持接口兼容，仅替换引擎实现：
  - 初始化阶段用 `PaddleOCR(ocr_version='PP-OCRv5', ...)` 取代 RapidOCR 初始化。
  - PDF 渲染（PyMuPDF）、去印章逻辑、结果聚合与 JSON 结构保持不变（当前项目消费者依赖 `pages[].items[].text/score/box/chars` 结构）。
  - 注意输入图像使用 RGB（`cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)`）。

参数对齐建议：
- 维持现有接口的请求体 options 字段：`dpi`、`min_score`、`ignore_seals`。
- 置信度过滤在服务端实现，PaddleOCR 输出后统一过滤 `score >= min_score`。

模型缓存与路径：
- 默认缓存：`~/.paddleocr`。
- 如需离线部署，将下载好的 v5 模型放置到指定目录，并在初始化时传入 `det_model_dir/rec_model_dir/cls_model_dir`。

---

## 6. 性能与精度建议

- **GPU**：安装 `paddlepaddle-gpu` 并确保 CUDA/CUDNN 匹配，`use_gpu=True` 可显著提速。
- **DPI**：PDF 渲染时 `dpi=150~200` 一般较平衡，过高会增大耗时与显存占用。
- **语言**：中文用 `lang='ch'`，英文 `lang='en'`；多语混排可参考 PaddleOCR 文档选择合适模型。
- **方向分类**：`use_angle_cls=True` 可提升旋转文本识别的稳定性。

---

## 7. 常见问题排查（FAQ）

- 无法安装 `paddlepaddle-gpu`
  - 请使用官方安装向导，选择与你的 Python/CUDA 对应的版本；或先用 CPU 版本验证流程。

- 首次运行卡在下载模型
  - 网络受限可手动下载模型到本地并在初始化时指定 `det_model_dir/rec_model_dir/cls_model_dir`。

- 结果结构与前端不匹配
  - 请确保返回 JSON 中包含：
    - `pages[].items[].text`、`score`、`box`（四点坐标）
    - `chars`（可选，用于字符级定位，结构为 `{ ch, box }[]`）

- 识别精度不理想
  - 适当提高 `dpi`，开启 `use_angle_cls`，或尝试服务器/轻量模型（`server`/`mobile`）并选择合适语言包。

---

## 8. 参考链接

- Paddle 安装指南（官方选择器）：[https://www.paddlepaddle.org.cn/install/quick](https://www.paddlepaddle.org.cn/install/quick)
- PaddleOCR 文档（v5 模型）：[https://paddlepaddle.github.io/PaddleOCR/latest/version3.x/algorithm/PP-OCRv5/PP-OCRv5.html](https://paddlepaddle.github.io/PaddleOCR/latest/version3.x/algorithm/PP-OCRv5/PP-OCRv5.html)
- PaddleOCR 使用（CLI/API）：[https://paddlepaddle.github.io/PaddleOCR/main/version3.x/pipeline_usage/OCR.html](https://paddlepaddle.github.io/PaddleOCR/main/version3.x/pipeline_usage/OCR.html)

---

## 9. 最小变更清单（落地建议）

1) 新建 `paddle_pdf_ocr_server.py`（可复制 `rapid_pdf_ocr_server.py` 基本框架）：
- 将 RapidOCR 初始化替换为 `PaddleOCR(ocr_version='PP-OCRv5', ...)`；
- 在处理环节将 BGR 转为 RGB；
- 保持响应 JSON 结构不变。

2) 按原有方式启动服务：
```powershell
python paddle_pdf_ocr_server.py --port 9898 --cpu-only  # 或 --gpu
```

3) 后端 Java 无需更改；`OCRCompareService` 仍通过 HTTP 调用获取结果。

如需我直接提供 `paddle_pdf_ocr_server.py` 的实现骨架，请告诉我你的 GPU/CPU 环境和语言模型需求（中文/英文/多语）。
