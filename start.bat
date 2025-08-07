@echo off
echo ========================================
echo 肇新合同工具集 - 启动脚本
echo ========================================

echo.
echo 1. 检查MySQL服务...
net start mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo 警告: MySQL服务未启动，请手动启动MySQL服务
    pause
)

echo.
echo 2. 启动后端服务...
cd backend
start "后端服务" cmd /k "mvn spring-boot:run"

echo.
echo 3. 等待后端服务启动...
timeout /t 15 /nobreak >nul

echo.
echo 4. 启动前端服务...
cd ..\frontend
start "前端服务" cmd /k "npm run dev"

echo.
echo 服务启动完成！
echo 后端地址: http://localhost:8080/api
echo 前端地址: http://localhost:5173
echo Druid监控: http://localhost:8080/api/druid (admin/admin)
echo.
echo 按任意键退出...
pause >nul 