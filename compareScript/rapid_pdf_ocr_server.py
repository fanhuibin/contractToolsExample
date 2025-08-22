#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RapidOCR HTTP服务
提供OCR识别的REST API接口，支持Java等客户端调用
"""

import os
import sys
import time
import json
import uuid
import logging
import threading
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from pathlib import Path
try:
    import yaml  # 用于读取 RapidOCR YAML 配置，若缺失仅影响模型信息日志
except Exception:
    yaml = None
import argparse

# 第三方库导入
try:
    from flask import Flask, request, jsonify, send_file
    from flask_cors import CORS
    import requests
    import fitz  # PyMuPDF
    import numpy as np
    import cv2
except ImportError as e:
    print(f"缺少必要的依赖库: {e}")
    print("请安装: pip install flask flask-cors requests PyMuPDF opencv-python numpy")
    sys.exit(1)

# OCR相关导入（与GPU/CPU无关，由 onnxruntime providers 决定真正使用的执行提供者）
def import_rapidocr():
    try:
        from rapidocr_onnxruntime import RapidOCR
        print("✓ 成功导入 RapidOCR (onnxruntime)")
        return RapidOCR
    except ImportError:
        print("✗ 无法导入RapidOCR，请安装依赖:")
        print("  GPU版本: pip install rapidocr-onnxruntime-gpu")
        print("  CPU版本: pip install rapidocr-onnxruntime")
        return None

BASE_DIR = Path(__file__).resolve().parent
YAML_CONFIG_PATH = BASE_DIR / 'config' / 'rapidocr_v5.yml'
LOG_DIR = BASE_DIR / 'logs'
SERVER_CONFIG_PATH = BASE_DIR / 'config' / 'ocr_server_config.yml'

# 调试模式（默认关闭，可由配置文件或请求参数覆盖）
SERVER_DEBUG = False
SERVER_CFG: Dict[str, Any] = {}
try:
    if yaml and os.path.exists(str(SERVER_CONFIG_PATH)):
        with open(SERVER_CONFIG_PATH, 'r', encoding='utf-8') as f:
            SERVER_CFG = yaml.safe_load(f) or {}
        SERVER_DEBUG = bool((SERVER_CFG.get('server') or {}).get('debug', False))
except Exception:
    # 保持默认值
    pass

# 导入OCR引擎
RapidOCR = import_rapidocr()
if RapidOCR is None:
    sys.exit(1)

# 全局配置
class Config:
    DEFAULT_PORT = 9898
    DEFAULT_HOST = "0.0.0.0"
    UPLOAD_FOLDER = "uploads"
    RESULTS_FOLDER = "results"
    TASK_TIMEOUT = 3600  # 1小时超时
    MAX_FILE_SIZE = 100 * 1024 * 1024  # 100MB
    SUPPORTED_FORMATS = ['.pdf', '.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.tif']

def _resolve_path(p: Optional[str]) -> Optional[str]:
    if not p:
        return None
    q = Path(p)
    if not q.is_absolute():
        q = BASE_DIR / q
    return str(q)

# 任务管理器
class TaskManager:
    def __init__(self):
        self.tasks: Dict[str, Dict] = {}
        self.lock = threading.Lock()
        
    def create_task(self, file_path: str, file_type: str, options: Dict) -> str:
        """创建新的OCR任务"""
        task_id = str(uuid.uuid4())
        
        with self.lock:
            self.tasks[task_id] = {
                'id': task_id,
                'file_path': file_path,
                'file_type': file_type,
                'options': options,
                'status': 'pending',
                'progress': 0.0,
                'current_step': '等待处理',
                'created_time': datetime.now().isoformat(),
                'start_time': None,
                'completed_time': None,
                'result_path': None,
                'error_message': None,
                'total_pages': 0,
                'current_page': 0
            }
        
        return task_id
    
    def get_task(self, task_id: str) -> Optional[Dict]:
        """获取任务信息"""
        with self.lock:
            return self.tasks.get(task_id)
    
    def update_task(self, task_id: str, **kwargs):
        """更新任务信息"""
        with self.lock:
            if task_id in self.tasks:
                self.tasks[task_id].update(kwargs)
    
    def get_all_tasks(self) -> List[Dict]:
        """获取所有任务"""
        with self.lock:
            return list(self.tasks.values())
    
    def delete_task(self, task_id: str) -> bool:
        """删除任务"""
        with self.lock:
            if task_id in self.tasks:
                del self.tasks[task_id]
                return True
            return False
    
    def cleanup_expired_tasks(self, max_age_hours: int = 24):
        """清理过期的任务"""
        cutoff_time = datetime.now() - timedelta(hours=max_age_hours)
        
        with self.lock:
            expired_tasks = [
                task_id for task_id, task in self.tasks.items()
                if datetime.fromisoformat(task['created_time']) < cutoff_time
            ]
            
            for task_id in expired_tasks:
                del self.tasks[task_id]
        
        if expired_tasks:
            logging.info(f"清理了 {len(expired_tasks)} 个过期任务")

# OCR处理器
class OCRProcessor:
    def __init__(self, task_manager: TaskManager):
        self.task_manager = task_manager
        self.ocr = None
        self.initialized = False
    
    def _build_rapidocr_params(self) -> Dict[str, Any]:
        """构造 RapidOCR 的 params（不再依赖 YAML）。
        优先从 `SERVER_CFG['rapidocr']` 读取；若缺失，使用合理默认：
        - 引擎: onnxruntime
        - 版本: PP-OCRv5
        - 模型类型: server
        - 语言: ch
        """
        rcfg = (SERVER_CFG.get('rapidocr') or {}) if isinstance(SERVER_CFG, dict) else {}
        det_engine = rcfg.get('det_engine', 'onnxruntime')
        rec_engine = rcfg.get('rec_engine', 'onnxruntime')
        det_version = rcfg.get('det_version', 'PP-OCRv5')
        rec_version = rcfg.get('rec_version', 'PP-OCRv5')
        det_model_type = rcfg.get('det_model_type', 'server')
        rec_model_type = rcfg.get('rec_model_type', 'server')
        lang = rcfg.get('lang', 'ch')

        # 尝试导入枚举，若失败则直接用字符串
        EngineType = OCRVersion = ModelType = None
        try:
            from rapidocr_onnxruntime import EngineType as _E, OCRVersion as _V, ModelType as _M  # type: ignore
            EngineType, OCRVersion, ModelType = _E, _V, _M
        except Exception:
            try:
                from rapidocr import EngineType as _E, OCRVersion as _V, ModelType as _M  # type: ignore
                EngineType, OCRVersion, ModelType = _E, _V, _M
            except Exception:
                EngineType = OCRVersion = ModelType = None

        def _enum_or_str(enum_cls, name: str):
            if enum_cls is None:
                return name
            try:
                # 允许大小写/短横线差异
                cleaned = name.replace('-', '').replace('_', '').upper()
                for member in enum_cls:
                    if member.name.replace('-', '').replace('_', '') == cleaned:
                        return member
                return enum_cls[name]  # 可能直接匹配
            except Exception:
                return name

        params: Dict[str, Any] = {
            'Det.engine_type': _enum_or_str(EngineType, det_engine),
            'Rec.engine_type': _enum_or_str(EngineType, rec_engine),
            'Det.ocr_version': _enum_or_str(OCRVersion, det_version),
            'Rec.ocr_version': _enum_or_str(OCRVersion, rec_version),
            'Det.model_type': _enum_or_str(ModelType, det_model_type),
            'Rec.model_type': _enum_or_str(ModelType, rec_model_type),
            'Det.lang_type': lang,
            'Rec.lang_type': lang,
        }
        return params
        
    def initialize_ocr(self, use_gpu: bool = True, gpu_memory_limit: Optional[int] = None):
        """初始化OCR模型"""
        try:
            # 基于 onnxruntime 的可用 providers 决定
            try:
                import onnxruntime as ort
                available = ort.get_available_providers()
            except Exception:
                available = []

            selected_providers: List[str]
            if use_gpu and ('CUDAExecutionProvider' in available or 'ROCMExecutionProvider' in available):
                # 优先 CUDA，其次 ROCm，始终包含 CPU 兜底
                selected_providers = []
                if 'CUDAExecutionProvider' in available:
                    selected_providers.append('CUDAExecutionProvider')
                if 'ROCMExecutionProvider' in available:
                    selected_providers.append('ROCMExecutionProvider')
                selected_providers.append('CPUExecutionProvider')
                logging.info("GPU 请求: True, 可用 providers: %s", available)
            else:
                selected_providers = ['CPUExecutionProvider']
                logging.info("GPU 请求: %s, 未检测到可用 GPU providers(%s)，使用CPU", use_gpu, available)

            # 优先：通过 params 显式指定；若不支持则回退 YAML/默认
            ocr_inited = False
            params = self._build_rapidocr_params()
            try:
                try:
                    self.ocr = RapidOCR(params=params, providers=selected_providers)
                except TypeError:
                    self.ocr = RapidOCR(params=params)
                ocr_inited = True
            except Exception:
                ocr_inited = False

            if not ocr_inited:
                # 回退：使用 YAML（如果存在），否则默认
                yaml_path = str(YAML_CONFIG_PATH)
                if os.path.exists(yaml_path):
                    try:
                        self.ocr = RapidOCR(config_path=yaml_path, providers=selected_providers)
                        ocr_inited = True
                    except TypeError:
                        try:
                            self.ocr = RapidOCR(config_path=yaml_path)
                            ocr_inited = True
                        except Exception:
                            ocr_inited = False
                if not ocr_inited:
                    self.ocr = RapidOCR(providers=selected_providers)

            # 启动时打印使用的模型/配置摘要（基于 params 或 YAML）
            try:
                logging.info(
                    "使用 RapidOCR 参数: Det[engine=%s, lang=%s, type=%s, ver=%s], Rec[engine=%s, lang=%s, type=%s, ver=%s]; providers=%s",
                    params.get('Det.engine_type'), params.get('Det.lang_type'), params.get('Det.model_type'), params.get('Det.ocr_version'),
                    params.get('Rec.engine_type'), params.get('Rec.lang_type'), params.get('Rec.model_type'), params.get('Rec.ocr_version'),
                    selected_providers
                )
            except Exception:
                pass
            self.initialized = True
            logging.info("OCR模型初始化成功")
            
        except Exception as e:
            logging.error(f"OCR模型初始化失败: {e}")
            self.initialized = False
    
    def process_task(self, task_id: str):
        """处理OCR任务"""
        task = self.task_manager.get_task(task_id)
        if not task:
            logging.error(f"任务不存在: {task_id}")
            return
        
        try:
            self.task_manager.update_task(task_id, 
                                        status='processing',
                                        start_time=datetime.now().isoformat(),
                                        current_step='开始OCR识别')
            
            file_path = task['file_path']
            file_type = task['file_type']
            options = task['options']
            
            if file_type.lower() == 'pdf':
                result = self._process_pdf(file_path, task_id, options)
            else:
                result = self._process_image(file_path, task_id, options)
            
            if result:
                self.task_manager.update_task(task_id,
                                            status='completed',
                                            completed_time=datetime.now().isoformat(),
                                            progress=100.0,
                                            current_step='OCR识别完成',
                                            result_path=result)
                logging.info(f"任务完成: {task_id}")
            else:
                self.task_manager.update_task(task_id,
                                            status='failed',
                                            completed_time=datetime.now().isoformat(),
                                            current_step='OCR识别失败')
                logging.error(f"任务失败: {task_id}")
                
        except Exception as e:
            logging.error(f"处理任务异常: {task_id}, 错误: {e}")
            self.task_manager.update_task(task_id,
                                        status='failed',
                                        completed_time=datetime.now().isoformat(),
                                        current_step='处理异常',
                                        error_message=str(e))
    
    def _process_pdf(self, pdf_path: str, task_id: str, options: Dict) -> Optional[str]:
        """处理PDF文件"""
        try:
            result_dir = os.path.join(_resolve_path(Config.RESULTS_FOLDER), task_id)
            os.makedirs(result_dir, exist_ok=True)
            
            doc = fitz.open(pdf_path)
            total_pages = doc.page_count
            
            self.task_manager.update_task(task_id, total_pages=total_pages)
            
            all_pages_text = []
            all_pages_data = []
            
            dpi = options.get('dpi', 300)
            min_score = options.get('min_score', 0.5)
            ignore_seals = options.get('ignore_seals', True)
            
            for page_number in range(total_pages):
                current_page = page_number + 1
                
                progress = (current_page / total_pages) * 100
                self.task_manager.update_task(task_id,
                                            current_page=current_page,
                                            progress=progress,
                                            current_step=f'处理第{current_page}页')
                
                page = doc.load_page(page_number)
                img_bgr = self._render_page_to_bgr_image(page, dpi)
                
                # 去印章
                if ignore_seals:
                    img_bgr = self._remove_red_seals(img_bgr)
                # 预处理（默认关闭，HTTP 显式开启才执行）
                if options.get('preprocess', False):
                    img_bgr = self._preprocess_image(img_bgr, options)
                
                # 如启用调试，则保存送入OCR的最终图片（以及后续绘制框的底图）
                if options.get('debug', False):
                    try:
                        debug_dir = os.path.join(result_dir, 'debug')
                        os.makedirs(debug_dir, exist_ok=True)
                        debug_img_path = os.path.join(debug_dir, f"page_{current_page:03d}_final.png")
                        cv2.imwrite(debug_img_path, img_bgr)
                    except Exception as e:
                        logging.warning(f"保存调试图片失败(第{current_page}页): {e}")
                
                try:
                    result, _ = self.ocr(img_bgr)
                except Exception as e:
                    logging.error(f"第{current_page}页OCR失败: {e}")
                    result = []
                
                page_lines = []
                page_items = []
                
                if isinstance(result, list):
                    for item in result:
                        if not isinstance(item, list) or len(item) < 3:
                            continue
                        
                        box = item[0]
                        text = str(item[1])
                        score = float(item[2]) if len(item) > 2 else 0.0
                        
                        if text.strip() and score >= min_score:
                            page_lines.append(text.strip())
                            page_items.append({
                                'text': text.strip(),
                                'score': score,
                                'box': box,
                                'chars': self._split_quad_by_equal_chars(box, text)
                            })
                
                # 如启用调试，可视化检测框
                if options.get('debug', False):
                    try:
                        debug_dir = os.path.join(result_dir, 'debug')
                        os.makedirs(debug_dir, exist_ok=True)
                        boxed = self._annotate_with_boxes(img_bgr, result)
                        boxed_path = os.path.join(debug_dir, f"page_{current_page:03d}_boxed.png")
                        cv2.imwrite(boxed_path, boxed)
                    except Exception as e:
                        logging.warning(f"保存检测框图片失败(第{current_page}页): {e}")

                all_pages_text.append(page_lines)
                all_pages_data.append({
                    'page_index': current_page,
                    'image_width': int(img_bgr.shape[1]),
                    'image_height': int(img_bgr.shape[0]),
                    'dpi': dpi,
                    'items': page_items
                })
            
            doc.close()
            
            self._save_text_results(all_pages_text, result_dir)
            
            json_result = {
                'pdf': os.path.abspath(pdf_path),
                'pages': all_pages_data,
                'options': options,
                'task_id': task_id,
                'created_time': datetime.now().isoformat()
            }
            
            json_path = os.path.join(result_dir, 'result.json')
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(json_result, f, ensure_ascii=False, indent=2)
            
            return result_dir
            
        except Exception as e:
            logging.error(f"处理PDF失败: {e}")
            return None
    
    def _process_image(self, image_path: str, task_id: str, options: Dict) -> Optional[str]:
        """处理图片文件"""
        try:
            result_dir = os.path.join(Config.RESULTS_FOLDER, task_id)
            os.makedirs(result_dir, exist_ok=True)
            
            img_bgr = cv2.imread(image_path)
            if img_bgr is None:
                raise ValueError(f"无法读取图片: {image_path}")
            
            self.task_manager.update_task(task_id, total_pages=1, current_page=1)
            
            dpi = options.get('dpi', 150)
            min_score = options.get('min_score', 0.5)
            ignore_seals = options.get('ignore_seals', True)
            
            # 去印章
            if ignore_seals:
                img_bgr = self._remove_red_seals(img_bgr)
            # 预处理（默认关闭，HTTP 显式开启才执行）
            if options.get('preprocess', False):
                img_bgr = self._preprocess_image(img_bgr, options)
            
            # 如启用调试，则保存送入OCR的最终图片及检测框可视化
            if options.get('debug', False):
                try:
                    debug_dir = os.path.join(result_dir, 'debug')
                    os.makedirs(debug_dir, exist_ok=True)
                    debug_img_path = os.path.join(debug_dir, 'final.png')
                    cv2.imwrite(debug_img_path, img_bgr)
                except Exception as e:
                    logging.warning(f"保存调试图片失败(图片): {e}")
            
            try:
                result, _ = self.ocr(img_bgr)
            except Exception as e:
                logging.error(f"图片OCR失败: {e}")
                result = []
            
            page_lines = []
            page_items = []
            
            if isinstance(result, list):
                for item in result:
                    if not isinstance(item, list) or len(item) < 3:
                        continue
                    
                    box = item[0]
                    text = str(item[1])
                    score = float(item[2]) if len(item) > 2 else 0.0
                    
                    if text.strip() and score >= min_score:
                        page_lines.append(text.strip())
                        page_items.append({
                            'text': text.strip(),
                            'score': score,
                            'box': box,
                            'chars': self._split_quad_by_equal_chars(box, text)
                        })
            
            # 可视化检测框（debug）
            if options.get('debug', False):
                try:
                    debug_dir = os.path.join(result_dir, 'debug')
                    os.makedirs(debug_dir, exist_ok=True)
                    boxed = self._annotate_with_boxes(img_bgr, result)
                    boxed_path = os.path.join(debug_dir, 'boxed.png')
                    cv2.imwrite(boxed_path, boxed)
                except Exception as e:
                    logging.warning(f"保存检测框图片失败(图片): {e}")

            self._save_text_results([page_lines], result_dir)
            
            json_result = {
                'image': os.path.abspath(image_path),
                'pages': [{
                    'page_index': 1,
                    'image_width': int(img_bgr.shape[1]),
                    'image_height': int(img_bgr.shape[0]),
                    'dpi': dpi,
                    'items': page_items
                }],
                'options': options,
                'task_id': task_id,
                'created_time': datetime.now().isoformat()
            }
            
            json_path = os.path.join(result_dir, 'result.json')
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(json_result, f, ensure_ascii=False, indent=2)
            
            self.task_manager.update_task(task_id, progress=100.0)
            
            return result_dir
            
        except Exception as e:
            logging.error(f"处理图片失败: {e}")
            return None
    
    def _render_page_to_bgr_image(self, page, dpi: int) -> np.ndarray:
        """渲染PDF页面为BGR图像"""
        zoom = dpi / 72.0
        matrix = fitz.Matrix(zoom, zoom)
        pix = page.get_pixmap(matrix=matrix, alpha=False, colorspace=fitz.csRGB)
        
        img = np.frombuffer(pix.samples, dtype=np.uint8).reshape((pix.h, pix.w, 3))
        img_bgr = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
        return img_bgr
    
    def _remove_red_seals(self, img_bgr: np.ndarray) -> np.ndarray:
        """去除图像中的红色印章"""
        try:
            # 转换为HSV颜色空间，更容易处理红色
            img_hsv = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2HSV)
            
            # 定义红色的HSV范围
            # 红色在HSV中有两个范围：0-10 和 170-180
            lower_red1 = np.array([0, 50, 50])
            upper_red1 = np.array([10, 255, 255])
            lower_red2 = np.array([170, 50, 50])
            upper_red2 = np.array([180, 255, 255])
            
            # 创建红色掩码
            mask1 = cv2.inRange(img_hsv, lower_red1, upper_red1)
            mask2 = cv2.inRange(img_hsv, lower_red2, upper_red2)
            red_mask = cv2.bitwise_or(mask1, mask2)
            
            # 形态学操作，去除噪点并连接断开的区域
            kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
            red_mask = cv2.morphologyEx(red_mask, cv2.MORPH_CLOSE, kernel)
            red_mask = cv2.morphologyEx(red_mask, cv2.MORPH_OPEN, kernel)
            
            # 将红色区域替换为白色背景
            img_result = img_bgr.copy()
            img_result[red_mask > 0] = [255, 255, 255]  # 白色背景
            
            return img_result
            
        except Exception as e:
            logging.warning(f"去除红色印章失败: {e}，使用原图像")
            return img_bgr

    def _preprocess_image(self, img_bgr: np.ndarray, options: Dict) -> np.ndarray:
        """通用预处理：灰度 -> 可选CLAHE -> 二值化 -> 形态学去噪 -> 平滑 -> 白底黑字 -> 返回BGR"""
        try:
            gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)

            # 对比度受限自适应直方图均衡（可选）
            if options.get('clahe', False):
                try:
                    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
                    gray = clahe.apply(gray)
                except Exception:
                    pass

            # 二值化
            thr_img = gray
            if options.get('binarize', True):
                method = str(options.get('binarize_method', 'adaptive')).lower()
                if method == 'adaptive':
                    # 自适应阈值适用于光照不均
                    block = 25
                    if block % 2 == 0:
                        block += 1
                    thr_img = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                                    cv2.THRESH_BINARY, blockSize=block, C=15)
                else:
                    # Otsu
                    blur = cv2.GaussianBlur(gray, (3, 3), 0)
                    _, thr_img = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

            # 形态学去噪
            if options.get('denoise_morph', True):
                k = int(options.get('denoise_ksize', 3) or 3)
                k = max(1, k)
                if k % 2 == 0:
                    k += 1
                kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (k, k))
                thr_img = cv2.morphologyEx(thr_img, cv2.MORPH_OPEN, kernel, iterations=1)

            # 平滑
            smooth = str(options.get('smooth', 'bilateral')).lower()
            if smooth == 'bilateral':
                thr_img = cv2.bilateralFilter(thr_img, d=5, sigmaColor=75, sigmaSpace=75)
            elif smooth == 'gaussian':
                thr_img = cv2.GaussianBlur(thr_img, (3, 3), 0)

            # 确保白底黑字
            try:
                white = int((thr_img == 255).sum())
                black = int((thr_img == 0).sum())
                if white < black:
                    thr_img = cv2.bitwise_not(thr_img)
            except Exception:
                pass

            return cv2.cvtColor(thr_img, cv2.COLOR_GRAY2BGR)
        except Exception as e:
            logging.warning(f"预处理失败，使用原图像: {e}")
            return img_bgr
    
    def _split_quad_by_equal_chars(self, box, text: str, skip_space: bool = True):
        """分割四边形为字符级坐标"""
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
    
    def _annotate_with_boxes(self, img_bgr: np.ndarray, result_list: Any) -> np.ndarray:
        """在图像上绘制检测框，并标注文本与置信度。
        优先用 PIL（可更好地显示中文）；若无 PIL，则退化为仅绘制框与分数（cv2）。
        """
        boxed = img_bgr.copy()
        if not isinstance(result_list, list):
            return boxed
        # 预处理结构
        boxes: List[Dict[str, Any]] = []
        for item in result_list:
            if not isinstance(item, list) or len(item) < 3:
                continue
            box = item[0]
            text = str(item[1])
            try:
                score = float(item[2])
            except Exception:
                score = 0.0
            boxes.append({'box': box, 'text': text, 'score': score})

        if not boxes:
            return boxed

        # 尝试 PIL
        use_pil = False
        try:
            from PIL import Image, ImageDraw, ImageFont  # type: ignore
            use_pil = True
        except Exception:
            use_pil = False

        if use_pil:
            try:
                img_rgb = cv2.cvtColor(boxed, cv2.COLOR_BGR2RGB)
                pil_img = Image.fromarray(img_rgb)
                draw = ImageDraw.Draw(pil_img)
                try:
                    font = ImageFont.truetype("arial.ttf", 16)
                except Exception:
                    try:
                        font = ImageFont.truetype("simhei.ttf", 16)
                    except Exception:
                        font = ImageFont.load_default()

                for b in boxes:
                    pts = np.array(b['box'][:4], dtype=np.int32)
                    # 多边形框
                    draw.line([tuple(pts[0]), tuple(pts[1])], fill=(0, 255, 0), width=2)
                    draw.line([tuple(pts[1]), tuple(pts[2])], fill=(0, 255, 0), width=2)
                    draw.line([tuple(pts[2]), tuple(pts[3])], fill=(0, 255, 0), width=2)
                    draw.line([tuple(pts[3]), tuple(pts[0])], fill=(0, 255, 0), width=2)
                    # 标签
                    label = f"{b['score']:.2f} {b['text'][:30]}"
                    x = int(min(p[0] for p in pts))
                    y = int(min(p[1] for p in pts))
                    # 背景框
                    try:
                        bbox = draw.textbbox((x, y), label, font=font)
                        draw.rectangle(bbox, fill=(0, 0, 0, 127))
                    except Exception:
                        pass
                    draw.text((x, y), label, fill=(255, 255, 0), font=font)

                boxed = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
                return boxed
            except Exception:
                # 回退到 cv2
                pass

        # OpenCV 回退：绘制框 + 分数
        for b in boxes:
            try:
                pts = np.array(b['box'][:4], dtype=np.int32)
                cv2.polylines(boxed, [pts], isClosed=True, color=(0, 255, 0), thickness=2)
                x = int(np.min(pts[:, 0]))
                y = int(np.min(pts[:, 1]))
                label = f"{b['score']:.2f}"
                cv2.putText(boxed, label, (x, max(y - 3, 0)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 255), 1, cv2.LINE_AA)
            except Exception:
                continue
        return boxed
    
    def _save_text_results(self, all_pages_text: List[List[str]], result_dir: str):
        """保存文本结果"""
        for page_index, lines in enumerate(all_pages_text, start=1):
            page_txt = "\n".join(lines)
            page_file = os.path.join(result_dir, f"page_{page_index:03d}.txt")
            with open(page_file, "w", encoding="utf-8", newline="\n") as f:
                f.write(page_txt)
        
        combined_path = os.path.join(result_dir, "combined.txt")
        with open(combined_path, "w", encoding="utf-8", newline="\n") as f:
            for page_index, lines in enumerate(all_pages_text, start=1):
                f.write("\n".join(lines))
                if page_index < len(all_pages_text):
                    f.write("\n\n")

# 文件下载器
class FileDownloader:
    @staticmethod
    def download_file(url: str, local_path: str) -> bool:
        """从HTTP URL下载文件到本地"""
        try:
            response = requests.get(url, stream=True, timeout=30)
            response.raise_for_status()
            
            with open(local_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            
            return True
        except Exception as e:
            logging.error(f"下载文件失败: {url}, 错误: {e}")
            return False

# Flask应用
app = Flask(__name__)
CORS(app)
# 允许 /api/xxx 与 /api/xxx/ 都能命中路由，避免因反代或客户端多/少斜杠导致的 404
try:
    app.url_map.strict_slashes = False
except Exception:
    pass

# 全局对象
task_manager = TaskManager()
ocr_processor = OCRProcessor(task_manager)
file_downloader = FileDownloader()

# 创建必要的目录
os.makedirs(Config.UPLOAD_FOLDER, exist_ok=True)
os.makedirs(Config.RESULTS_FOLDER, exist_ok=True)
os.makedirs(LOG_DIR, exist_ok=True)

# 配置日志（固定到文件 + 控制台，INFO 级别，可按需调整）
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(str(LOG_DIR / 'ocr_server.log'), encoding='utf-8'),
        logging.StreamHandler()
    ]
)

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'service': 'RapidOCR HTTP Server'
    })

@app.route('/api/ocr/submit', methods=['POST'])
def submit_ocr_task():
    """1. 发起识别的接口"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求数据为空'}), 400
        
        file_source = data.get('file_source')
        file_path = data.get('file_path')
        file_type = data.get('file_type', 'pdf')
        # 请求 options（如未提供，采用默认值）
        req_options = data.get('options', {}) or {}

        # 以请求参数优先，其次配置文件，最后内置默认值
        cfg_ocr = (SERVER_CFG.get('ocr') or {}) if isinstance(SERVER_CFG, dict) else {}
        def _opt(key, default_val):
            v_req = req_options.get(key)
            if v_req is not None:
                return v_req
            v_cfg = cfg_ocr.get(key)
            return v_cfg if v_cfg is not None else default_val

        options = {
            'dpi': int(_opt('dpi', 300)),
            'min_score': float(_opt('min_score', 0.35)),
            'ignore_seals': bool(_opt('ignore_seals', False)),
            # 是否启用预处理总开关（默认关闭，避免误伤识别）
            'preprocess': bool(_opt('preprocess', False)),
            # 预处理增强（如未传入，采用较保守的默认）
            'binarize': bool(_opt('binarize', False)),
            'binarize_method': str(_opt('binarize_method', 'adaptive')),  # 'adaptive' | 'otsu'
            'denoise_morph': bool(_opt('denoise_morph', False)),
            'denoise_ksize': int(_opt('denoise_ksize', 3)),
            'smooth': str(_opt('smooth', 'gaussian')),  # 'none' | 'bilateral' | 'gaussian'
            'clahe': bool(_opt('clahe', False)),
            # 调试：仅在调试模式下保存送入OCR的最终图片
            'debug': bool(req_options.get('debug', SERVER_DEBUG))
        }
        
        if not file_source or not file_path:
            return jsonify({'error': '缺少必要参数: file_source, file_path'}), 400
        
        if file_type.lower() not in ['pdf', 'image']:
            return jsonify({'error': '不支持的文件类型'}), 400
        
        local_file_path = None
        
        if file_source == 'local':
            if not os.path.exists(file_path):
                return jsonify({'error': f'本地文件不存在: {file_path}'}), 404
            local_file_path = file_path
            
        elif file_source == 'http':
            if not file_path.startswith(('http://', 'https://')):
                return jsonify({'error': '无效的HTTP URL'}), 400
            
            file_name = os.path.basename(file_path) or f"download_{int(time.time())}.pdf"
            local_file_path = os.path.join(_resolve_path(Config.UPLOAD_FOLDER), file_name)
            
            if not file_downloader.download_file(file_path, local_file_path):
                return jsonify({'error': '文件下载失败'}), 500
        else:
            return jsonify({'error': '不支持的文件源类型'}), 400
        
        task_id = task_manager.create_task(local_file_path, file_type, options)
        
        def process_task():
            ocr_processor.process_task(task_id)
        
        thread = threading.Thread(target=process_task)
        thread.daemon = True
        thread.start()
        
        return jsonify({
            'success': True,
            'task_id': task_id,
            'message': 'OCR任务已提交'
        })
        
    except Exception as e:
        logging.error(f"提交OCR任务失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

@app.route('/api/ocr/status/<task_id>', methods=['GET'])
def get_task_status(task_id):
    """2. 查询识别进度的接口"""
    try:
        task = task_manager.get_task(task_id)
        if not task:
            return jsonify({'error': '任务不存在'}), 404
        
        return jsonify({
            'success': True,
            'task': task
        })
        
    except Exception as e:
        logging.error(f"查询任务状态失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

@app.route('/api/ocr/result/<task_id>', methods=['GET'])
def get_ocr_result(task_id):
    """3. 获取识别结果的接口"""
    try:
        task = task_manager.get_task(task_id)
        if not task:
            return jsonify({'error': '任务不存在'}), 404
        
        if task['status'] != 'completed':
            return jsonify({'error': '任务尚未完成'}), 400
        
        result_path = task['result_path']
        if not result_path or not os.path.exists(result_path):
            return jsonify({'error': '结果文件不存在'}), 404
        
        json_path = os.path.join(result_path, 'result.json')
        if os.path.exists(json_path):
            with open(json_path, 'r', encoding='utf-8') as f:
                result_data = json.load(f)
        else:
            result_data = None
        
        combined_path = os.path.join(result_path, 'combined.txt')
        if os.path.exists(combined_path):
            with open(combined_path, 'r', encoding='utf-8') as f:
                text_content = f.read()
        else:
            text_content = ""
        
        return jsonify({
            'success': True,
            'task': task,
            'result': {
                'json_data': result_data,
                'text_content': text_content,
                'result_path': result_path
            }
        })
        
    except Exception as e:
        logging.error(f"获取OCR结果失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

@app.route('/api/ocr/history', methods=['GET'])
def get_ocr_history():
    """4. 查询历史识别数据接口"""
    try:
        page = int(request.args.get('page', 1))
        size = int(request.args.get('size', 20))
        status = request.args.get('status')
        
        all_tasks = task_manager.get_all_tasks()
        
        if status:
            all_tasks = [task for task in all_tasks if task['status'] == status]
        
        total = len(all_tasks)
        start_idx = (page - 1) * size
        end_idx = start_idx + size
        tasks_page = all_tasks[start_idx:end_idx]
        
        return jsonify({
            'success': True,
            'data': {
                'tasks': tasks_page,
                'pagination': {
                    'page': page,
                    'size': size,
                    'total': total,
                    'pages': (total + size - 1) // size
                }
            }
        })
        
    except Exception as e:
        logging.error(f"查询历史数据失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

@app.route('/api/ocr/clear', methods=['POST'])
def clear_ocr_history():
    """5. 清除历史数据的接口"""
    try:
        data = request.get_json() or {}
        task_ids = data.get('task_ids', [])
        clear_all = data.get('clear_all', False)
        
        if clear_all:
            all_tasks = task_manager.get_all_tasks()
            deleted_count = 0
            
            for task in all_tasks:
                task_id = task['id']
                if task_manager.delete_task(task_id):
                    result_path = task.get('result_path')
                    if result_path and os.path.exists(result_path):
                        try:
                            import shutil
                            shutil.rmtree(result_path)
                        except Exception as e:
                            logging.warning(f"删除结果目录失败: {result_path}, 错误: {e}")
                    deleted_count += 1
            
            return jsonify({
                'success': True,
                'message': f'已清除所有任务，共删除 {deleted_count} 个任务'
            })
        
        elif task_ids:
            deleted_count = 0
            not_found_count = 0
            
            for task_id in task_ids:
                task = task_manager.get_task(task_id)
                if task:
                    if task_manager.delete_task(task_id):
                        result_path = task.get('result_path')
                        if result_path and os.path.exists(result_path):
                            try:
                                import shutil
                                shutil.rmtree(result_path)
                            except Exception as e:
                                logging.warning(f"删除结果目录失败: {result_path}, 错误: {e}")
                        deleted_count += 1
                else:
                    not_found_count += 1
            
            return jsonify({
                'success': True,
                'message': f'删除完成，成功删除 {deleted_count} 个任务，未找到 {not_found_count} 个任务'
            })
        
        else:
            return jsonify({'error': '缺少必要参数: task_ids 或 clear_all'}), 400
        
    except Exception as e:
        logging.error(f"清除历史数据失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

@app.route('/api/ocr/download/<task_id>/<file_type>', methods=['GET'])
def download_result_file(task_id, file_type):
    """下载结果文件"""
    try:
        task = task_manager.get_task(task_id)
        if not task:
            return jsonify({'error': '任务不存在'}), 404
        
        if task['status'] != 'completed':
            return jsonify({'error': '任务尚未完成'}), 400
        
        result_path = task['result_path']
        if not result_path or not os.path.exists(result_path):
            return jsonify({'error': '结果文件不存在'}), 404
        
        file_path = None
        
        if file_type == 'combined':
            file_path = os.path.join(result_path, 'combined.txt')
        elif file_type == 'json':
            file_path = os.path.join(result_path, 'result.json')
        elif file_type.startswith('page_'):
            file_path = os.path.join(result_path, f'{file_type}.txt')
        else:
            return jsonify({'error': '不支持的文件类型'}), 400
        
        if not os.path.exists(file_path):
            return jsonify({'error': '文件不存在'}), 404
        
        return send_file(file_path, as_attachment=True)
        
    except Exception as e:
        logging.error(f"下载结果文件失败: {e}")
        return jsonify({'error': f'服务器内部错误: {str(e)}'}), 500

# 定期清理过期任务
def cleanup_tasks():
    """定期清理过期任务"""
    while True:
        try:
            time.sleep(3600)  # 每小时清理一次
            task_manager.cleanup_expired_tasks(max_age_hours=24)
        except Exception as e:
            logging.error(f"清理任务失败: {e}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='RapidOCR HTTP服务')
    parser.add_argument('--port', type=int, default=Config.DEFAULT_PORT, help=f'服务端口 (默认: {Config.DEFAULT_PORT})')
    parser.add_argument('--host', default=Config.DEFAULT_HOST, help=f'服务地址 (默认: {Config.DEFAULT_HOST})')
    parser.add_argument('--gpu', action='store_true', help='启用GPU加速')
    parser.add_argument('--cpu-only', action='store_true', help='强制使用CPU')
    parser.add_argument('--gpu-memory-limit', type=int, help='GPU内存限制(GB)')
    
    args = parser.parse_args()
    
    # 初始化OCR模型
    logging.info("正在初始化OCR模型...")
    use_gpu = args.gpu and not args.cpu_only
    ocr_processor.initialize_ocr(use_gpu=use_gpu, gpu_memory_limit=args.gpu_memory_limit)
    
    if ocr_processor.initialized:
        logging.info("OCR模型初始化成功")
    else:
        logging.warning("OCR模型初始化失败，将使用CPU模式")
        ocr_processor.initialize_ocr(use_gpu=False)
    
    # 启动清理任务线程
    cleanup_thread = threading.Thread(target=cleanup_tasks, daemon=True)
    cleanup_thread.start()
    
    # 启动Flask服务
    logging.info(f"启动RapidOCR HTTP服务，地址: {args.host}:{args.port}")
    # 启动时打印所有已注册路由，便于排查 404
    try:
        routes = []
        for rule in app.url_map.iter_rules():
            routes.append(f"{','.join(sorted(rule.methods))} {rule.rule}")
        logging.info("已注册路由: %s", "; ".join(sorted(routes)))
    except Exception:
        pass
    app.run(host=args.host, port=args.port, debug=False, threaded=True)
