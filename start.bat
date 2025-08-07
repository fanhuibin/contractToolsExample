@echo off
echo 启动肇新合同工具集...

echo.
echo 1. 启动后端服务...
cd backend
start "后端服务" cmd /k "mvn spring-boot:run"
cd ..

echo.
echo 2. 启动前端服务...
cd frontend
start "前端服务" cmd /k "npm run dev"
cd ..

echo.
echo 服务启动完成！
echo 前端地址: http://localhost:3000
echo 后端地址: http://localhost:8080/api
echo.
pause 