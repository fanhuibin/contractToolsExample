@echo off
chcp 65001 >nul
title 肇新工具集 Demo 快速打包

echo.
echo ==========================================
echo    肇新工具集 Demo 快速打包工具
echo ==========================================
echo.
echo [INFO] 这是一个简化的打包脚本，适用于快速构建和测试
echo [INFO] 完整的打包请使用 build.bat
echo.

:: 检查环境
where mvn >nul 2>&1 || (echo [ERROR] 未找到 Maven & pause & exit /b 1)
where node >nul 2>&1 || (echo [ERROR] 未找到 Node.js & pause & exit /b 1)

echo [✓] 环境检查通过
echo.

:: 询问用户选择
echo 请选择构建模式:
echo   1. 仅构建后端
echo   2. 仅构建前端  
echo   3. 构建全部 (推荐)
echo   4. 清理构建缓存
echo.
set /p choice="请输入选择 (1-4): "

if "%choice%"=="1" goto build_backend
if "%choice%"=="2" goto build_frontend  
if "%choice%"=="3" goto build_all
if "%choice%"=="4" goto clean_cache
echo [ERROR] 无效选择
pause
exit /b 1

:clean_cache
echo.
echo [INFO] 清理构建缓存...
if exist "backend\target" rmdir /s /q "backend\target"
if exist "frontend\dist" rmdir /s /q "frontend\dist"
if exist "frontend\node_modules" (
    echo [INFO] 发现 node_modules，是否删除? (y/N)
    set /p del_nm="请选择: "
    if /i "%del_nm%"=="y" rmdir /s /q "frontend\node_modules"
)
echo [SUCCESS] 清理完成
pause
exit /b 0

:build_backend
echo.
echo [INFO] 构建后端项目...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] 后端构建失败
    pause
    exit /b 1
)
echo [SUCCESS] 后端构建完成: target\*.jar
cd ..
pause
exit /b 0

:build_frontend
echo.
echo [INFO] 构建前端项目...
cd frontend
if not exist "node_modules" (
    echo [INFO] 安装依赖...
    call npm install --silent
)
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] 前端构建失败
    pause
    exit /b 1
)
echo [SUCCESS] 前端构建完成: dist\
cd ..
pause
exit /b 0

:build_all
echo.
echo [INFO] 构建所有项目...

:: 构建后端
echo [1/2] 构建后端...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] 后端构建失败
    cd ..
    pause
    exit /b 1
)
cd ..
echo [✓] 后端构建完成

:: 构建前端
echo [2/2] 构建前端...
cd frontend
if not exist "node_modules" (
    echo [INFO] 安装依赖...
    call npm install --silent
)
call npm run build --silent
if %errorlevel% neq 0 (
    echo [ERROR] 前端构建失败
    cd ..
    pause
    exit /b 1
)
cd ..
echo [✓] 前端构建完成

echo.
echo [SUCCESS] 全部构建完成！
echo.
echo 构建产物:
echo   - 后端: backend\target\*.jar
echo   - 前端: frontend\dist\
echo.
echo 快速启动测试:
echo   1. 启动后端: java -jar backend\target\*.jar
echo   2. 启动前端: cd frontend\dist ^&^& python -m http.server 3004
echo.
pause
