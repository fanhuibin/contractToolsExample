@echo off
chcp 65001 >nul
echo ========================================
echo 启动合同比对 Demo 后端
echo ========================================
echo.

cd /d "%~dp0"

echo 检查 Maven...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven，请先安装 Maven
    pause
    exit /b 1
)

echo.
echo 启动 Spring Boot 应用...
echo.
mvn spring-boot:run

pause

