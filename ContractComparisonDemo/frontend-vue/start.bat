@echo off
chcp 65001 >nul
echo ========================================
echo 启动合同比对 Demo 前端 (Vue 3)
echo ========================================
echo.

cd /d "%~dp0"

echo 检查 Node.js...
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)

echo.
echo 检查依赖...
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
) else (
    echo 依赖已安装
)

echo.
echo 启动开发服务器...
echo 访问地址: http://localhost:3000
echo.
call npm run dev

pause

