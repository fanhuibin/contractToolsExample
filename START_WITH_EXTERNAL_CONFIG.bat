@echo off
echo ========================================
echo 使用外部配置文件启动应用
echo ========================================

echo.
echo 配置文件: application-external.yml
echo 当前 DPI 设置: 请查看 application-external.yml
echo.
echo 修改 application-external.yml 中的 render-dpi 值
echo 然后重新运行此脚本即可，无需重新打包！
echo.

if not exist "application-external.yml" (
    echo 错误: application-external.yml 不存在！
    pause
    exit /b 1
)

echo [1/2] 清理缓存图片...
if exist ".\uploads\compare-pro\tasks\" (
    rmdir /s /q ".\uploads\compare-pro\tasks\"
    echo 缓存已清理
)

echo.
echo [2/2] 启动应用（使用外部配置）...
echo 请查看日志中的 "🎨 渲染DPI: xx" 来验证配置
echo.
pause

java -jar contract-tools-sdk\target\contract-tools-sdk-1.0.0.jar --spring.config.location=file:./application-external.yml

pause

