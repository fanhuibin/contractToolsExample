@echo off
chcp 65001
cls
echo =====================================
echo   肇新工具集 Demo - 后端启动脚本
echo =====================================
echo.

echo [1/2] 正在编译项目...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b %errorlevel%
)

echo.
echo [2/2] 启动Spring Boot应用...
echo.
java -jar target\zhaoxin-tools-demo-backend-1.0.0.jar

pause

