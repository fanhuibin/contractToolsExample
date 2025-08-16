import os
import sys
import argparse
import time
from typing import List
import logging

import fitz  # PyMuPDF
import numpy as np
import cv2
import json
from typing import Optional, Dict, Any

# 确保stdout使用UTF-8编码和无缓冲输出
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')
if sys.stderr.encoding != 'utf-8':
    sys.stderr.reconfigure(encoding='utf-8')

# 强制stdout无缓冲，确保实时输出
sys.stdout.reconfigure(line_buffering=True)
sys.stderr.reconfigure(line_buffering=True)


def setup_logging(debug: bool = False, log_file: Optional[str] = None) -> logging.Logger:
    """
    设置日志配置
    """
    level = logging.DEBUG if debug else logging.INFO
    format_str = '%(asctime)s - %(levelname)s - %(message)s'
    
    logger = logging.getLogger('rapidOCR')
    logger.setLevel(level)
    
    # 清除现有的处理器
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)
    
    # 控制台处理器 - 只记录到日志文件，不输出到控制台避免乱码
    # console_handler = logging.StreamHandler()
    # console_handler.setLevel(level)
    # console_formatter = logging.Formatter(format_str)
    # console_handler.setFormatter(console_formatter)
    # logger.addHandler(console_handler)
    
    # 文件处理器（如果指定）
    if log_file:
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setLevel(level)
        file_formatter = logging.Formatter(format_str)
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)
    
    return logger


def check_gpu_support() -> Dict[str, Any]:
    """
    检查GPU支持情况
    """
    gpu_info = {
        'available': False,
        'provider': 'CPUExecutionProvider',
        'gpu_memory': 0,
        'cuda_version': None,
        'error': None
    }
    
    try:
        import onnxruntime as ort
        
        # 检查可用的执行提供程序
        providers = ort.get_available_providers()
        logger = logging.getLogger('rapidOCR')
        
        if 'CUDAExecutionProvider' in providers:
            gpu_info['available'] = True
            gpu_info['provider'] = 'CUDAExecutionProvider'
            logger.info("检测到CUDA GPU支持")
            
            # 尝试获取GPU信息
            try:
                import pynvml
                pynvml.nvmlInit()
                device_count = pynvml.nvmlDeviceGetCount()
                if device_count > 0:
                    handle = pynvml.nvmlDeviceGetHandleByIndex(0)
                    memory_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
                    gpu_info['gpu_memory'] = memory_info.total // (1024 * 1024 * 1024)  # GB
                    logger.info(f"GPU显存: {gpu_info['gpu_memory']} GB")
                    
                    # 获取CUDA版本
                    cuda_version = pynvml.nvmlSystemGetCudaDriverVersion()
                    gpu_info['cuda_version'] = f"{cuda_version // 1000}.{(cuda_version % 1000) // 10}"
                    logger.info(f"CUDA版本: {gpu_info['cuda_version']}")
                    
            except ImportError:
                logger.warning("未安装pynvml，无法获取详细GPU信息")
            except Exception as e:
                logger.warning(f"获取GPU信息失败: {e}")
                
        elif 'ROCMExecutionProvider' in providers:
            gpu_info['available'] = True
            gpu_info['provider'] = 'ROCMExecutionProvider'
            logger.info("检测到ROCm GPU支持")
            
        else:
            logger.info("未检测到GPU支持，使用CPU执行")
            
    except ImportError:
        gpu_info['error'] = "未安装onnxruntime"
    except Exception as e:
        gpu_info['error'] = str(e)
        
    return gpu_info


def create_rapidocr_instance(use_gpu: bool = True, gpu_memory_limit: Optional[int] = None) -> Any:
    """
    创建RapidOCR实例，支持GPU配置
    """
    try:
        from rapidocr_onnxruntime import RapidOCR
        
        if use_gpu:
            # 检查GPU支持
            gpu_info = check_gpu_support()
            
            if gpu_info['available']:
                # 配置GPU执行提供程序
                providers = [gpu_info['provider']]
                
                # 如果指定了GPU内存限制
                if gpu_memory_limit and gpu_info['provider'] == 'CUDAExecutionProvider':
                    provider_options = [{
                        'device_id': 0,
                        'arena_extend_strategy': 'kNextPowerOfTwo',
                        'gpu_mem_limit': gpu_memory_limit * 1024 * 1024 * 1024,  # 转换为字节
                        'cudnn_conv_use_max_workspace': True,
                        'do_copy_in_default_stream': True,
                    }]
                    ocr = RapidOCR(providers=providers, provider_options=provider_options)
                else:
                    ocr = RapidOCR(providers=providers)
                    
                logger = logging.getLogger('rapidOCR')
                logger.info(f"使用GPU执行提供程序: {gpu_info['provider']}")
                if gpu_memory_limit:
                    logger.info(f"GPU内存限制: {gpu_memory_limit} GB")
                    
            else:
                logger = logging.getLogger('rapidOCR')
                logger.warning("GPU不可用，回退到CPU执行")
                if gpu_info['error']:
                    logger.warning(f"GPU检测错误: {gpu_info['error']}")
                ocr = RapidOCR()
        else:
            # 强制使用CPU
            ocr = RapidOCR(providers=['CPUExecutionProvider'])
            logger = logging.getLogger('rapidOCR')
            logger.info("强制使用CPU执行")
            
        return ocr
        
    except ImportError as e:
        raise ImportError(f"导入RapidOCR失败: {e}\n请安装: pip install rapidocr-onnxruntime-gpu")


def render_page_to_bgr_image(page: fitz.Page, dpi: int) -> np.ndarray:
    """
    Render a PDF page to a BGR numpy image for RapidOCR.
    """
    zoom = dpi / 72.0
    matrix = fitz.Matrix(zoom, zoom)
    pix = page.get_pixmap(matrix=matrix, alpha=False, colorspace=fitz.csRGB)

    img = np.frombuffer(pix.samples, dtype=np.uint8).reshape((pix.h, pix.w, 3))
    img_bgr = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
    return img_bgr


def write_text_outputs(all_pages_text: List[List[str]], output_path: str, as_directory: bool) -> None:
    if as_directory:
        os.makedirs(output_path, exist_ok=True)
        for page_index, lines in enumerate(all_pages_text, start=1):
            page_txt = "\n".join(lines)
            page_file = os.path.join(output_path, f"page_{page_index:03d}.txt")
            with open(page_file, "w", encoding="utf-8", newline="\n") as f:
                f.write(page_txt)
        combined_path = os.path.join(output_path, "combined.txt")
        with open(combined_path, "w", encoding="utf-8", newline="\n") as f:
            for page_index, lines in enumerate(all_pages_text, start=1):
                f.write(f"===== Page {page_index} =====\n")
                f.write("\n".join(lines))
                if page_index < len(all_pages_text):
                    f.write("\n\n")
    else:
        combined_txt = []
        for page_index, lines in enumerate(all_pages_text, start=1):
            combined_txt.append(f"===== Page {page_index} =====")
            combined_txt.extend(lines)
            if page_index < len(all_pages_text):
                combined_txt.append("")
        with open(output_path, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(combined_txt))


def run_pdf_ocr(pdf_path: str, output_path: str, dpi: int = 150, min_score: float = 0.5, 
                debug: bool = False, log_file: Optional[str] = None, use_gpu: bool = True,
                gpu_memory_limit: Optional[int] = None) -> None:
    if not os.path.isfile(pdf_path):
        raise FileNotFoundError(f"PDF 文件不存在: {pdf_path}")

    # 设置日志
    logger = setup_logging(debug, log_file)
    
    output_path = os.path.abspath(output_path)
    as_directory = not output_path.lower().endswith(".txt")

    logger.info(f"开始OCR识别 (RapidOCR)，PDF: {pdf_path}")
    logger.info(f"设置: DPI={dpi}, 最小置信度={min_score}, 调试模式={'开启' if debug else '关闭'}")
    logger.info(f"GPU模式: {'开启' if use_gpu else '关闭'}")
    if gpu_memory_limit:
        logger.info(f"GPU内存限制: {gpu_memory_limit} GB")
    logger.info(f"输出: {output_path}")
    if log_file:
        logger.info(f"日志文件: {log_file}")
    logger.info("-" * 50)

    # 检查GPU支持
    gpu_info = check_gpu_support()
    if gpu_info['available']:
        logger.info(f"GPU支持: {gpu_info['provider']}")
        if gpu_info['gpu_memory'] > 0:
            logger.info(f"GPU显存: {gpu_info['gpu_memory']} GB")
        if gpu_info['cuda_version']:
            logger.info(f"CUDA版本: {gpu_info['cuda_version']}")
    else:
        logger.warning("GPU不可用，将使用CPU执行")
        if gpu_info['error']:
            logger.warning(f"GPU检测错误: {gpu_info['error']}")

    # 初始化RapidOCR
    logger.info("正在初始化RapidOCR模型...")
    init_start = time.time()
    try:
        ocr = create_rapidocr_instance(use_gpu, gpu_memory_limit)
        init_time = time.time() - init_start
        logger.info(f"模型初始化完成，耗时: {init_time:.2f}秒")
        if debug:
            logger.debug(f"RapidOCR模型类型: {type(ocr)}")
    except ImportError as e:
        logger.error(f"导入RapidOCR失败: {e}")
        logger.error("请安装: pip install rapidocr-onnxruntime-gpu")
        raise

    doc = fitz.open(pdf_path)
    total_pages = doc.page_count
    all_pages_text: List[List[str]] = []

    total_render_time = 0.0
    total_ocr_time = 0.0
    total_text_count = 0

    logger.info(f"PDF总页数: {total_pages}")
    print(f"TOTAL_PAGES:{total_pages}", flush=True)  # 强制立即输出
    logger.info("开始处理页面...")
    print("PROCESSING_STARTED", flush=True)  # 强制立即输出

    for page_number in range(total_pages):
        page_begin = time.time()
        current_page = page_number + 1

        # 页面渲染
        render_begin = time.time()
        page = doc.load_page(page_number)
        img_bgr = render_page_to_bgr_image(page, dpi=dpi)
        render_time = time.time() - render_begin
        total_render_time += render_time
        
        logger.info(f"第 {current_page}/{total_pages} 页渲染完成")
        print(f"PAGE_RENDERED:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        logger.info(f"  图片尺寸: {img_bgr.shape[1]}x{img_bgr.shape[0]} 像素")
        logger.info(f"  渲染耗时: {render_time:.2f}秒")
        if debug:
            logger.debug(f"  图片数据类型: {img_bgr.dtype}")
            logger.debug(f"  图片内存占用: {img_bgr.nbytes / 1024 / 1024:.2f} MB")

        # RapidOCR 推理
        ocr_begin = time.time()
        try:
            result, _ = ocr(img_bgr)
            ocr_time = time.time() - ocr_begin
            total_ocr_time += ocr_time
        except Exception as e:
            logger.error(f"第 {current_page} 页OCR识别失败: {e}")
            result = []
            ocr_time = time.time() - ocr_begin

        # 处理识别结果
        page_lines: List[str] = []
        if isinstance(result, list):
            for item in result:
                # item: [box_points, text, score]
                if not isinstance(item, list) or len(item) < 3:
                    if debug:
                        logger.debug(f"跳过无效结果项: {item}")
                    continue
                text = str(item[1])
                try:
                    score = float(item[2])
                except Exception:
                    score = 0.0
                if text.strip() and score >= min_score:
                    page_lines.append(text.strip())
                    if debug:
                        logger.debug(f"  识别文字: '{text.strip()}' (置信度: {score:.3f})")
                elif debug and text.strip():
                    logger.debug(f"  跳过低置信度文字: '{text.strip()}' (置信度: {score:.3f})")

        total_text_count += len(page_lines)
        all_pages_text.append(page_lines)
        
        logger.info(f"第 {current_page}/{total_pages} 页OCR完成")
        print(f"PAGE_COMPLETED:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        logger.info(f"  识别行数: {len(page_lines)}")
        logger.info(f"  OCR耗时: {ocr_time:.2f}秒")
        logger.info(f"  页面总耗时: {time.time() - page_begin:.2f}秒")
        logger.info(f"  累计识别文字: {total_text_count} 行")
        
        # 进度显示
        progress = (current_page / total_pages) * 100
        logger.info(f"  总体进度: {progress:.1f}% ({current_page}/{total_pages})")
        print(f"PROGRESS:{progress:.1f}%:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        logger.info("-" * 30)

    # 写入输出文件
    logger.info("正在写入输出文件...")
    write_text_outputs(all_pages_text, output_path, as_directory)

    # 统计信息
    logger.info("=" * 50)
    logger.info("识别完成！(RapidOCR)")
    logger.info(f"总页数: {total_pages}")
    logger.info(f"总文字行数: {total_text_count}")
    logger.info(f"总渲染时间: {total_render_time:.2f}秒")
    logger.info(f"总OCR时间: {total_ocr_time:.2f}秒")
    logger.info(f"平均每页渲染时间: {total_render_time / max(total_pages, 1):.2f}秒")
    logger.info(f"平均每页OCR时间: {total_ocr_time / max(total_pages, 1):.2f}秒")
    logger.info(f"平均每页总时间: {(total_render_time + total_ocr_time) / max(total_pages, 1):.2f}秒")
    logger.info(f"输出位置: {output_path if as_directory else os.path.abspath(output_path)}")
    if log_file:
        logger.info(f"日志文件: {log_file}")
    logger.info("=" * 50)


def split_quad_by_equal_chars(box, text: str, skip_space: bool = True):
    if not isinstance(box, (list, tuple)) or len(box) < 4 or not text:
        return []
    pts = box[:4]
    if not all(isinstance(p, (list, tuple)) and len(p) >= 2 for p in pts):
        return []
    pts_sorted = sorted(pts, key=lambda p: (p[1], p[0]))
    top2 = sorted(pts_sorted[:2], key=lambda p: p[0])
    bot2 = sorted(pts_sorted[2:], key=lambda p: p[0])
    p0, p1, p2, p3 = top2[0], top2[1], bot2[1], bot2[0]

    def lerp(a, b, t):
        return [a[0] + (b[0]-a[0])*t, a[1] + (b[1]-a[1])*t]

    chars = list(text)
    idx_map = [i for i, ch in enumerate(chars) if ch.strip() != ""] if skip_space else list(range(len(chars)))
    if len(idx_map) == 0:
        return []
    out = []
    for k, real_idx in enumerate(idx_map):
        t0 = k / len(idx_map)
        t1 = (k + 1) / len(idx_map)
        lt = lerp(p0, p1, t0)
        rt = lerp(p0, p1, t1)
        lb = lerp(p3, p2, t0)
        rb = lerp(p3, p2, t1)
        out.append({
            'ch': chars[real_idx],
            'box': [lt, rt, rb, lb]
        })
    return out


def run_pdf_ocr_with_boxes(pdf_path: str, dpi: int = 150, min_score: float = 0.5,
                           debug: bool = False, log_file: Optional[str] = None,
                           use_gpu: bool = True, gpu_memory_limit: Optional[int] = None) -> Dict[str, Any]:
    if not os.path.isfile(pdf_path):
        raise FileNotFoundError(f"PDF 文件不存在: {pdf_path}")

    # 设置日志
    logger = setup_logging(debug, log_file)
    
    logger.info(f"开始坐标提取模式，PDF: {pdf_path}")
    logger.info(f"设置: DPI={dpi}, 最小置信度={min_score}, 调试模式={'开启' if debug else '关闭'}")
    logger.info(f"GPU模式: {'开启' if use_gpu else '关闭'}")

    try:
        ocr = create_rapidocr_instance(use_gpu, gpu_memory_limit)
        logger.info("RapidOCR模型初始化完成")
    except ImportError as e:
        logger.error(f"导入RapidOCR失败: {e}")
        raise

    doc = fitz.open(pdf_path)
    total_pages = doc.page_count
    pages = []
    
    logger.info(f"PDF总页数: {total_pages}")
    print(f"TOTAL_PAGES:{total_pages}", flush=True)  # 强制立即输出
    logger.info("开始处理页面...")
    print("PROCESSING_STARTED", flush=True)  # 强制立即输出

    for page_number in range(total_pages):
        page_begin = time.time()
        current_page = page_number + 1
        
        logger.info(f"处理第 {current_page}/{total_pages} 页...")
        print(f"PROCESSING_PAGE:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        
        # 页面渲染
        render_begin = time.time()
        page = doc.load_page(page_number)
        img_bgr = render_page_to_bgr_image(page, dpi=dpi)
        render_time = time.time() - render_begin
        
        logger.info(f"  页面渲染完成，尺寸: {img_bgr.shape[1]}x{img_bgr.shape[0]}，耗时: {render_time:.2f}秒")

        # OCR识别
        ocr_begin = time.time()
        try:
            result, _ = ocr(img_bgr)
            ocr_time = time.time() - ocr_begin
        except Exception as e:
            logger.error(f"第 {current_page} 页OCR识别失败: {e}")
            result = []
            ocr_time = time.time() - ocr_begin

        # 处理识别结果
        export_items = []
        if isinstance(result, list):
            for item in result:
                # item: [box_points, text, score]
                if not isinstance(item, list) or len(item) < 3:
                    if debug:
                        logger.debug(f"跳过无效结果项: {item}")
                    continue
                box = item[0]
                text = str(item[1])
                try:
                    score = float(item[2])
                except Exception:
                    score = 0.0
                if text.strip() and score >= min_score:
                    char_boxes = split_quad_by_equal_chars(box, text, skip_space=True)
                    export_items.append({
                        'text': text.strip(),
                        'score': score,
                        'box': box,
                        'chars': char_boxes
                    })
                    if debug:
                        logger.debug(f"  识别文字: '{text.strip()}' (置信度: {score:.3f})")
                        logger.debug(f"  边界框: {box}")
                        logger.debug(f"  字符数量: {len(char_boxes)}")

        pages.append({
            'page_index': page_number + 1,
            'image_width': int(img_bgr.shape[1]),
            'image_height': int(img_bgr.shape[0]),
            'dpi': dpi,
            'items': export_items
        })
        
        logger.info(f"第 {current_page}/{total_pages} 页处理完成")
        print(f"PAGE_COMPLETED:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        logger.info(f"  识别项目数: {len(export_items)}")
        logger.info(f"  OCR耗时: {ocr_time:.2f}秒")
        logger.info(f"  页面总耗时: {time.time() - page_begin:.2f}秒")
        
        # 进度显示
        progress = (current_page / total_pages) * 100
        logger.info(f"  总体进度: {progress:.1f}% ({current_page}/{total_pages})")
        print(f"PROGRESS:{progress:.1f}%:{current_page}/{total_pages}", flush=True)  # 强制立即输出
        logger.info("-" * 30)

    result_data = {
        'pdf': os.path.abspath(pdf_path),
        'pages': pages
    }
    
    logger.info("坐标提取完成！")
    logger.info(f"总页数: {total_pages}")
    logger.info(f"总识别项目数: {sum(len(p['items']) for p in pages)}")
    
    return result_data


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="使用 RapidOCR 识别 PDF 文本")
    parser.add_argument("--pdf", required=True, help="输入 PDF 文件路径")
    parser.add_argument(
        "--out",
        default="output",
        help="输出路径：.txt 文件（合并所有页）或目录（按页输出并生成 combined.txt）",
    )
    parser.add_argument(
        "--dpi",
        type=int,
        default=150,
        help="渲染 DPI（建议 120-200，默认150）",
    )
    parser.add_argument(
        "--min_score",
        type=float,
        default=0.5,
        help="最小置信度阈值（0-1）",
    )
    parser.add_argument(
        "--fast",
        action="store_true",
        help="快速模式：DPI=120",
    )
    parser.add_argument(
        "--export_char_boxes",
        type=str,
        default=None,
        help="导出字符级近似位置到JSON（指定文件名，如 rapid_char_boxes.json）"
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="启用调试模式，输出详细日志"
    )
    parser.add_argument(
        "--log_file",
        type=str,
        default=None,
        help="日志文件路径（可选）"
    )
    parser.add_argument(
        "--cpu_only",
        action="store_true",
        help="强制使用CPU执行（禁用GPU）"
    )
    parser.add_argument(
        "--gpu_memory_limit",
        type=int,
        default=None,
        help="GPU内存限制（GB，仅CUDA有效）"
    )
    return parser.parse_args(argv)


def main(argv: List[str]) -> None:
    args = parse_args(argv)
    
    if args.fast:
        print("启用快速模式：DPI=120")
        dpi = 120
    else:
        dpi = args.dpi

    # GPU配置
    use_gpu = not args.cpu_only
    gpu_memory_limit = args.gpu_memory_limit

    # 如需JSON导出，使用带坐标版本
    if args.export_char_boxes:
        print("启用坐标导出：生成JSON")
        data = run_pdf_ocr_with_boxes(
            pdf_path=args.pdf, 
            dpi=dpi, 
            min_score=args.min_score,
            debug=args.debug,
            log_file=args.log_file,
            use_gpu=use_gpu,
            gpu_memory_limit=gpu_memory_limit
        )
        out_path = os.path.abspath(args.export_char_boxes)
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"坐标JSON已生成: {out_path}")
        
        # 同时输出文本
        lines_all = []
        for p in data['pages']:
            lines_all.append(f"===== Page {p['page_index']} =====")
            for it in p['items']:
                lines_all.append(it['text'])
            lines_all.append("")
        out_txt = os.path.abspath(args.out)
        if os.path.isdir(out_txt):
            os.makedirs(out_txt, exist_ok=True)
            with open(os.path.join(out_txt, 'combined.txt'), 'w', encoding='utf-8', newline='\n') as f:
                f.write('\n'.join(lines_all))
        else:
            with open(out_txt, 'w', encoding='utf-8', newline='\n') as f:
                f.write('\n'.join(lines_all))
        print("文本输出完成")
    else:
        run_pdf_ocr(
            pdf_path=args.pdf, 
            output_path=args.out, 
            dpi=dpi, 
            min_score=args.min_score,
            debug=args.debug,
            log_file=args.log_file,
            use_gpu=use_gpu,
            gpu_memory_limit=gpu_memory_limit
        )
if __name__ == "__main__":
    main(sys.argv[1:])


