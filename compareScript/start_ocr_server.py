#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OCR服务启动脚本
"""

import subprocess
import sys
import os

def main():
    # 检查Python版本
    if sys.version_info < (3, 7):
        print("错误: 需要Python 3.7或更高版本")
        sys.exit(1)
    
    # 检查基础依赖
    try:
        import flask
        import fitz
        import cv2
        import numpy
    except ImportError as e:
        print(f"错误: 缺少基础依赖库 {e}")
        print("正在安装基础依赖...")
        try:
            subprocess.run([sys.executable, "-m", "pip", "install", "flask", "flask-cors", "requests", "PyMuPDF", "opencv-python", "numpy"], check=True)
            print("基础依赖安装完成")
        except subprocess.CalledProcessError:
            print("基础依赖安装失败，请手动安装")
            sys.exit(1)
    
    # 检查OCR依赖
    print("检查OCR依赖...")
    try:
        # 首先尝试GPU版本
        import rapidocr_onnxruntime
        print("✓ RapidOCR已安装")
    except ImportError:
        print("⚠ RapidOCR未安装，正在尝试安装...")
        
        # 尝试安装GPU版本
        try:
            print("尝试安装GPU版本...")
            subprocess.run([sys.executable, "-m", "pip", "install", "rapidocr-onnxruntime-gpu"], check=True)
            print("✓ GPU版本安装成功")
        except subprocess.CalledProcessError:
            print("⚠ GPU版本安装失败，尝试CPU版本...")
            try:
                subprocess.run([sys.executable, "-m", "pip", "install", "rapidocr-onnxruntime"], check=True)
                print("✓ CPU版本安装成功")
            except subprocess.CalledProcessError:
                print("✗ OCR依赖安装失败，请手动安装:")
                print("  GPU版本: pip install rapidocr-onnxruntime-gpu")
                print("  CPU版本: pip install rapidocr-onnxruntime")
                sys.exit(1)
    
    # 启动OCR服务
    script_path = os.path.join(os.path.dirname(__file__), 'rapid_pdf_ocr_server.py')
    
    print("启动RapidOCR HTTP服务...")
    print("默认端口: 9898")
    print("访问地址: http://localhost:9898/health")
    print("按 Ctrl+C 停止服务")
    print("-" * 50)
    
    try:
        subprocess.run([sys.executable, script_path], check=True)
    except KeyboardInterrupt:
        print("\n服务已停止")
    except subprocess.CalledProcessError as e:
        print(f"服务启动失败: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
