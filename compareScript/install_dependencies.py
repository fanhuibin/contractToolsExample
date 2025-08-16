#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能依赖安装脚本
自动检测环境并安装合适的RapidOCR版本
"""

import subprocess
import sys
import os
import platform

def run_command(command, description):
    """运行命令并显示结果"""
    print(f"正在{description}...")
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        print(f"✓ {description}成功")
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ {description}失败: {e}")
        if e.stdout:
            print(f"输出: {e.stdout}")
        if e.stderr:
            print(f"错误: {e.stderr}")
        return False

def check_python_version():
    """检查Python版本"""
    version = sys.version_info
    if version.major < 3 or (version.major == 3 and version.minor < 7):
        print(f"✗ Python版本过低: {version.major}.{version.minor}")
        print("需要Python 3.7或更高版本")
        return False
    print(f"✓ Python版本: {version.major}.{version.minor}.{version.micro}")
    return True

def check_system_info():
    """检查系统信息"""
    print(f"操作系统: {platform.system()} {platform.release()}")
    print(f"Python架构: {platform.architecture()[0]}")
    print(f"Python路径: {sys.executable}")

def install_basic_dependencies():
    """安装基础依赖"""
    basic_deps = [
        "flask>=2.0.0",
        "flask-cors>=3.0.0", 
        "requests>=2.25.0",
        "PyMuPDF>=1.18.0",
        "opencv-python>=4.5.0",
        "numpy>=1.19.0"
    ]
    
    print("\n=== 安装基础依赖 ===")
    for dep in basic_deps:
        if not run_command(f"{sys.executable} -m pip install {dep}", f"安装 {dep}"):
            return False
    return True

def check_gpu_support():
    """检查GPU支持"""
    print("\n=== 检查GPU支持 ===")
    
    # 检查CUDA
    try:
        result = subprocess.run("nvidia-smi", shell=True, capture_output=True, text=True)
        if result.returncode == 0:
            print("✓ 检测到NVIDIA GPU")
            print("GPU信息:")
            print(result.stdout)
            return True
    except:
        pass
    
    # 检查CUDA环境变量
    cuda_path = os.environ.get('CUDA_PATH') or os.environ.get('CUDA_HOME')
    if cuda_path:
        print(f"✓ 检测到CUDA环境: {cuda_path}")
        return True
    
    print("⚠ 未检测到GPU支持，将使用CPU版本")
    return False

def install_ocr_dependencies(has_gpu):
    """安装OCR依赖"""
    print("\n=== 安装OCR依赖 ===")
    
    if has_gpu:
        print("尝试安装GPU版本...")
        if run_command(f"{sys.executable} -m pip install rapidocr-onnxruntime-gpu", "安装GPU版本"):
            print("✓ GPU版本安装成功")
            return True
        
        print("⚠ GPU版本安装失败，尝试CPU版本...")
    
    # 安装CPU版本
    if run_command(f"{sys.executable} -m pip install rapidocr-onnxruntime", "安装CPU版本"):
        print("✓ CPU版本安装成功")
        return True
    
    print("✗ OCR依赖安装失败")
    return False

def verify_installation():
    """验证安装"""
    print("\n=== 验证安装 ===")
    
    try:
        import flask
        print("✓ Flask")
    except ImportError:
        print("✗ Flask")
        return False
    
    try:
        import fitz
        print("✓ PyMuPDF")
    except ImportError:
        print("✗ PyMuPDF")
        return False
    
    try:
        import cv2
        print("✓ OpenCV")
    except ImportError:
        print("✗ OpenCV")
        return False
    
    try:
        import numpy
        print("✓ NumPy")
    except ImportError:
        print("✗ NumPy")
        return False
    
    try:
        import rapidocr_onnxruntime
        print("✓ RapidOCR")
        return True
    except ImportError:
        print("✗ RapidOCR")
        return False

def main():
    """主函数"""
    print("=" * 60)
    print("RapidOCR HTTP服务 - 智能依赖安装")
    print("=" * 60)
    
    # 检查Python版本
    if not check_python_version():
        sys.exit(1)
    
    # 显示系统信息
    check_system_info()
    
    # 检查GPU支持
    has_gpu = check_gpu_support()
    
    # 安装基础依赖
    if not install_basic_dependencies():
        print("\n✗ 基础依赖安装失败")
        sys.exit(1)
    
    # 安装OCR依赖
    if not install_ocr_dependencies(has_gpu):
        print("\n✗ OCR依赖安装失败")
        print("\n手动安装建议:")
        if has_gpu:
            print("1. 确保CUDA环境正确配置")
            print("2. 运行: pip install rapidocr-onnxruntime-gpu")
        print("3. 或者运行: pip install rapidocr-onnxruntime")
        sys.exit(1)
    
    # 验证安装
    if not verify_installation():
        print("\n✗ 安装验证失败")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("🎉 所有依赖安装成功！")
    print("现在可以启动OCR服务了:")
    print("  Python: python start_ocr_server.py")
    print("  Windows: start_ocr_server.bat")
    print("=" * 60)

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n安装被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n安装过程中发生错误: {e}")
        sys.exit(1)
