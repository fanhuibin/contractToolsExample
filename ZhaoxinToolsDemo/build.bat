@echo off
chcp 65001 >nul
echo ========================================
echo 肇新工具集 Demo 构建入口
echo ========================================
echo.
echo 请选择构建方式:
echo   1. 完整构建 (推荐)
echo   2. 快速构建
echo   3. 查看构建文档
echo.
set /p choice="请输入选择 (1-3): "

if "%choice%"=="1" (
    echo.
    echo [INFO] 执行完整构建...
    call "build-tools\scripts\build.bat"
) else if "%choice%"=="2" (
    echo.
    echo [INFO] 执行快速构建...
    call "build-tools\scripts\quick-build.bat"
) else if "%choice%"=="3" (
    echo.
    echo [INFO] 打开构建文档...
    start "" "build-tools\docs\BUILD_GUIDE.md"
) else (
    echo [ERROR] 无效选择
    pause
    exit /b 1
)
