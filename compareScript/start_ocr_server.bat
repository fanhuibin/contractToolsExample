@echo off
chcp 65001 >nul
echo ========================================
echo RapidOCR HTTP服务启动脚本
echo ========================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Python，请先安装Python 3.7+
    pause
    exit /b 1
)

echo Python版本检查通过
echo.

REM 检查基础依赖
echo 检查Python基础依赖...
python -c "import flask, fitz, cv2, numpy" >nul 2>&1
if errorlevel 1 (
    echo 警告: 缺少基础依赖库
    echo 正在安装基础依赖...
    pip install flask flask-cors requests PyMuPDF opencv-python numpy
    if errorlevel 1 (
        echo 错误: 基础依赖安装失败
        pause
        exit /b 1
    )
    echo 基础依赖安装完成
) else (
    echo 基础依赖检查通过
)

REM 检查OCR依赖
echo 检查OCR依赖...
python -c "import rapidocr_onnxruntime" >nul 2>&1
if errorlevel 1 (
    echo 警告: 缺少OCR依赖库
    echo 正在尝试安装OCR依赖...
    
    REM 首先尝试GPU版本
    echo 尝试安装GPU版本...
    pip install rapidocr-onnxruntime-gpu
    if errorlevel 1 (
        echo GPU版本安装失败，尝试CPU版本...
        pip install rapidocr-onnxruntime
        if errorlevel 1 (
            echo 错误: OCR依赖安装失败
            echo 请手动安装:
            echo   GPU版本: pip install rapidocr-onnxruntime-gpu
            echo   CPU版本: pip install rapidocr-onnxruntime
            pause
            exit /b 1
        ) else (
            echo CPU版本安装成功
        )
    ) else (
        echo GPU版本安装成功
    )
) else (
    echo OCR依赖检查通过
)

echo.
echo 启动RapidOCR HTTP服务...
echo 默认端口: 9898
echo 访问地址: http://localhost:9898/health
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

REM 启动服务
python rapid_pdf_ocr_server.py --port 9898 --gpu

echo.
echo 服务已停止
pause
