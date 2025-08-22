# RapidOCR 切换到 PP-OCRv5 模型配置指南（保留 RapidOCR 引擎）

本指南只更换 RapidOCR 所用的检测/识别模型为 PP-OCRv5（v5），不改用 PaddleOCR 运行时。

适用范围：`rapidocr-onnxruntime(-gpu)` 引擎（本仓库默认使用）。

---

## 1. 升级依赖（确保支持 v5）

- GPU 环境（有 CUDA）：
```powershell
pip install -U rapidocr-onnxruntime-gpu onnxruntime-gpu
```

- CPU 环境：
```powershell
pip install -U rapidocr-onnxruntime onnxruntime
```

可选检查：
```powershell
pip show rapidocr-onnxruntime-gpu rapidocr-onnxruntime onnxruntime-gpu onnxruntime
```

说明：较新的 `rapidocr-onnxruntime` 版本内置 v5 模型支持；若默认仍使用老模型，建议显式指定 v5 模型文件路径（见下文）。

---

## 2. 下载 PP-OCRv5 ONNX 模型文件

到官方模型列表下载对应语言的 v5 模型（检测 det / 识别 rec / 方向分类 cls 可选）并准备字典文件：

- 模型列表（含 v5）：[RapidOCR 模型列表](https://rapidai.github.io/RapidOCRDocs/main/model_list/)
- 典型文件（中文示例，文件名以发布为准）：
  - 检测（det）：`ch_PP-OCRv5_det_infer.onnx`
  - 识别（rec）：`ch_PP-OCRv5_rec_infer.onnx`
  - 方向分类（cls，可选）：`ch_ppocr_mobile_v2.0_cls_infer.onnx`
  - 识别字典：`ppocr_keys_v1.txt`（或模型发布页对应字典）

建议放置目录（示例）：
```
compareScript/models/ppocr_v5/
  det/ch_PP-OCRv5_det_infer.onnx
  rec/ch_PP-OCRv5_rec_infer.onnx
  cls/ch_ppocr_mobile_v2.0_cls_infer.onnx  # 可选
  dict/ppocr_keys_v1.txt
```

---

## 3. 在 RapidOCR 初始化处指定 v5 模型路径

本仓库有两个入口会初始化 RapidOCR：

- `compareScript/rapid_pdf_ocr_server.py`（HTTP 服务）
- `compareScript/rapid_pdf_ocr.py`（命令行脚本）

将默认 `RapidOCR(providers=...)` 改为显式传入 v5 模型路径，例如：

```python
from rapidocr_onnxruntime import RapidOCR

self.ocr = RapidOCR(
    det_model_path=r"D:\\git\\zhaoxin-contract-tool-set\\compareScript\\models\\ppocr_v5\\det\\ch_PP-OCRv5_det_infer.onnx",
    rec_model_path=r"D:\\git\\zhaoxin-contract-tool-set\\compareScript\\models\\ppocr_v5\\rec\\ch_PP-OCRv5_rec_infer.onnx",
    cls_model_path=r"D:\\git\\zhaoxin-contract-tool-set\\compareScript\\models\\ppocr_v5\\cls\\ch_ppocr_mobile_v2.0_cls_infer.onnx",  # 可选
    rec_char_dict_path=r"D:\\git\\zhaoxin-contract-tool-set\\compareScript\\models\\ppocr_v5\\dict\\ppocr_keys_v1.txt",
    providers=providers  # 保持你原来的 GPU/CPU 设置
)
```

命令行脚本 `rapid_pdf_ocr.py` 中同理，将内部 `RapidOCR(...)` 初始化替换为带路径的版本（CPU 版本为 `providers=['CPUExecutionProvider']`，GPU 版本为 `providers=['CUDAExecutionProvider', ...]`）。

> 文件名可能随发布更新，请以实际下载的文件名为准；路径可使用相对路径。

---

## 4. 重启与验证

- 若使用 HTTP 服务：重启 `rapid_pdf_ocr_server.py`。
- 触发一次识别，观察日志，确认已加载你指定的 onnx 模型路径。
- 识别结果应保持与之前相同的 JSON 结构（`pages[].items[].text/score/box/chars`）。

---

## 5. 常见问题

- 仍然加载旧模型/效果无变化
  - 显式传入 `det_model_path/rec_model_path/cls_model_path/rec_char_dict_path`，避免使用历史缓存。
  - 确认路径指向 v5 模型；注意文件权限与路径转义。

- GPU 未启用
  - 确认安装了 `onnxruntime-gpu` 与匹配的 CUDA 驱动；并在 `providers` 中包含 `CUDAExecutionProvider`。

- 识别字符异常
  - 检查 `rec_char_dict_path` 是否与所用识别模型匹配（中文通常 `ppocr_keys_v1.txt`）。

- 速度/显存占用较高
  - 优先使用 `mobile` 版模型；或降低 PDF 渲染 `dpi`（如 150）；必要时改用 CPU。

---

## 6. 回滚与锁定

- 回滚：恢复为原先默认初始化（不传模型路径），或改回旧模型路径。
- 锁定：将模型文件随项目存放并固定路径，避免线上自动更新造成不确定性。

---

## 7. 参考

- RapidOCR 模型列表与参数说明：
  - 模型列表：[https://rapidai.github.io/RapidOCRDocs/main/model_list/](https://rapidai.github.io/RapidOCRDocs/main/model_list/)
  - 参数说明（如需）：[https://rapidai.github.io/RapidOCRDocs/main/install_usage/rapidocr/parameters/](https://rapidai.github.io/RapidOCRDocs/main/install_usage/rapidocr/parameters/)

如需我直接帮你把 `rapid_pdf_ocr_server.py` 和 `rapid_pdf_ocr.py` 的初始化改为显式 v5 模型路径，请告诉我模型文件的存放位置。
