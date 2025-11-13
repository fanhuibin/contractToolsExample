@echo off
chcp 65001 >nul
echo ========================================
echo 肇新工具集 Demo 部署入口
echo ========================================
echo.
echo 请选择部署方式:
echo   1. Nginx 生产部署 (推荐)
echo   2. Docker 容器部署
echo   3. 查看部署文档
echo.
set /p choice="请输入选择 (1-3): "

if "%choice%"=="1" (
    echo.
    echo [INFO] 准备 Nginx 部署...
    if not exist "dist\" (
        echo [ERROR] 请先执行构建: build.bat
        pause
        exit /b 1
    )
    echo [INFO] 请在 Linux 服务器上执行以下命令:
    echo   cd dist/nginx
    echo   sudo ./deploy-nginx.sh
    echo.
    echo [INFO] 查看详细部署文档...
    start "" "build-tools\deployment\nginx\README.md"
) else if "%choice%"=="2" (
    echo.
    echo [INFO] Docker 部署功能开发中...
    start "" "build-tools\deployment\docker\"
) else if "%choice%"=="3" (
    echo.
    echo [INFO] 打开部署文档...
    start "" "build-tools\docs\DEPLOY_SUMMARY.md"
) else (
    echo [ERROR] 无效选择
    pause
    exit /b 1
)

pause
