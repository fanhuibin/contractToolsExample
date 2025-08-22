#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
演示：分别用默认模型（rapidocr-onnxruntime）与显式指定 PP-OCRv5（rapidocr）识别同一张图片。

说明：
- 默认模型：使用 rapidocr_onnxruntime 的 RapidOCR()，走 ONNX Runtime 默认/缓存模型。
- PP-OCRv5：使用 rapidocr 的 RapidOCR(params=...)，指定 EngineType.PADDLE + OCRVersion.PPOCRV5。
  若本机未安装 paddle/rapidocr 依赖，会捕获并提示如何安装。

用法：
  python compareScript/demo_rapidocr_models.py "D:/git/zhaoxin-contract-tool-set/compareScript/results/.../page_002_final.png"
"""

import sys
import time
from pathlib import Path


def pretty_print(title: str, result):
    print("\n== {} ==".format(title))
    try:
        # rapidocr_onnxruntime: result 是 list 或 (list, _)
        if isinstance(result, tuple) and len(result) >= 1:
            items = result[0]
        else:
            items = result
        if not isinstance(items, list):
            print("结果类型: {}".format(type(items)))
            print(items)
            return
        print("识别项数: {}".format(len(items)))
        for i, it in enumerate(items):
            try:
                box, text, score = it[0], str(it[1]), float(it[2])
                print("[{:02d}] {:.2f} {}".format(i + 1, score, text))
            except Exception:
                print("[{:02d}] {}".format(i + 1, it))
    except Exception as e:
        print("打印结果失败: {}".format(e))


def run_default_onnx(image_path: str):
    try:
        from rapidocr_onnxruntime import RapidOCR
    except Exception as e:
        print("[默认(ONNX)] 无法导入 rapidocr_onnxruntime: {}".format(e))
        print("  请安装: pip install rapidocr-onnxruntime 或 rapidocr-onnxruntime-gpu")
        return
    print("[默认(ONNX)] 使用 RapidOCR() 进行识别...")
    ocr = RapidOCR()
    t0 = time.time()
    try:
        result, _ = ocr(image_path)
    except Exception:
        result = ocr(image_path)
    dt = time.time() - t0
    pretty_print("默认(ONNX) 结果 (耗时 {:.3f}s)".format(dt), result)


def run_paddle_ppocrv5(image_path: str):
    try:
        from rapidocr import EngineType, ModelType, OCRVersion, RapidOCR  # type: ignore
    except Exception as e:
        print("[Paddle PP-OCRv5] 无法导入 rapidocr: {}".format(e))
        print("  若要使用该方式，请安装: pip install rapidocr paddlepaddle")
        return
    print("[Paddle PP-OCRv5] 使用 RapidOCR(params=...) 进行识别...")
    params = {
        "Det.engine_type": EngineType.PADDLE,
        "Rec.engine_type": EngineType.PADDLE,
        "Det.ocr_version": OCRVersion.PPOCRV5,
        "Rec.ocr_version": OCRVersion.PPOCRV5,
        "Det.model_type": ModelType.SERVER,
        "Rec.model_type": ModelType.SERVER,
        "Det.lang_type": "ch",
        "Rec.lang_type": "ch",
    }
    engine = RapidOCR(params=params)
    t0 = time.time()
    result = engine(image_path)
    dt = time.time() - t0
    pretty_print("Paddle PP-OCRv5 结果 (耗时 {:.3f}s)".format(dt), result)


def main():
    default_image = r"D:\git\zhaoxin-contract-tool-set\compareScript\results\5d034fea-5b5d-4c76-9ea5-9f3ca752c94a\debug\page_002_final.png"
    image_path = sys.argv[1] if len(sys.argv) > 1 else default_image
    p = Path(image_path)
    if not p.exists():
        print("图片不存在: {}".format(p))
        sys.exit(1)

    print("测试图片: {}".format(str(p)))
    run_default_onnx(str(p))
    run_paddle_ppocrv5(str(p))


if __name__ == "__main__":
    main()


