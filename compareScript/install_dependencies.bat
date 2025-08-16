@echo off
chcp 65001 >nul
echo ========================================
echo RapidOCR HTTP服务 - 依赖安装脚本
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

REM 运行智能安装脚本
echo 正在运行智能依赖安装脚本...
python install_dependencies.py

if errorlevel 1 (
    echo.
    echo 安装失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 安装完成！
pause
