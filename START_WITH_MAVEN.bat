@echo off
echo ========================================
echo 使用 Maven 直接运行（开发模式）
echo ========================================

echo.
echo 此方法会直接使用源码中的 application.yml
echo 修改 contract-tools-sdk/src/main/resources/application.yml 即可
echo 无需重新打包，但启动较慢
echo.

echo [1/2] 清理缓存图片...
if exist ".\uploads\compare-pro\tasks\" (
    rmdir /s /q ".\uploads\compare-pro\tasks\"
    echo 缓存已清理
)

echo.
echo [2/2] 启动应用...
echo 请查看日志中的 "🎨 渲染DPI: xx" 来验证配置
echo.
pause

cd contract-tools-sdk
mvn spring-boot:run

pause

