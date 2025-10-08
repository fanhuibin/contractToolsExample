@echo off
echo ========================================
echo 重新打包并测试配置
echo ========================================

echo.
echo [1/4] 清理旧的编译文件...
call mvn clean -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo 清理失败！
    pause
    exit /b 1
)

echo.
echo [2/4] 打包项目...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo.
echo [3/4] 清理缓存图片...
if exist ".\uploads\compare-pro\tasks\" (
    rmdir /s /q ".\uploads\compare-pro\tasks\"
    echo 缓存已清理
) else (
    echo 没有缓存需要清理
)

echo.
echo [4/4] 启动应用...
echo 请查看日志中的 "🎨 渲染DPI: xx" 来验证配置
echo.
echo 按任意键启动应用...
pause > nul

java -jar contract-tools-sdk\target\contract-tools-sdk-1.0.0.jar

pause

