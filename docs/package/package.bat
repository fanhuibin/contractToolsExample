@echo off
chcp 65001 >nul
REM =================================================
REM 肇新合同工具集 - 代码混淆打包脚本
REM =================================================
REM 功能：使用 Allatori 对 Java 代码进行混淆加密
REM 版本：1.0.0
REM 最后更新：2025-10-28
REM =================================================

echo ========================================
echo 肇新合同工具集 - 代码混淆打包
echo ========================================
echo.

REM 进入脚本所在目录
cd %~dp0
echo [当前目录] %cd%
echo.

REM 检查必要的文件是否存在
echo [步骤 1/8] 检查必要文件...
if not exist "allatori_crack.jar" (
    echo [错误] 找不到 allatori_crack.jar
    echo 请确保 allatori_crack.jar 文件在 docs/package 目录下
    pause
    exit /b 1
)

if not exist "config.xml" (
    echo [错误] 找不到 config.xml
    echo 请确保 config.xml 文件在 docs/package 目录下
    pause
    exit /b 1
)
echo [成功] 必要文件检查完成
echo.

REM 执行 Maven 打包
echo [步骤 2/9] 执行 Maven 打包...
echo [提示] 切换到项目根目录执行 Maven 打包...
echo.

cd ..\..
echo [当前目录] %cd%
echo.

echo [开始] mvn clean package -DskipTests
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] Maven 打包失败！错误代码: %ERRORLEVEL%
    echo 请检查 Maven 配置和项目代码
    pause
    exit /b 1
)

echo.
echo [成功] Maven 打包完成
echo.

REM 返回脚本目录
cd docs\package
echo [返回目录] %cd%
echo.

REM 检查 Maven 打包文件是否存在
echo [步骤 3/9] 检查 Maven 打包文件...
set SDK_JAR=..\..\contract-tools-sdk\target\contract-tools-sdk-1.0.0.jar
if not exist "%SDK_JAR%" (
    echo [警告] 找不到 SDK 主 JAR 文件
    echo 文件路径: %SDK_JAR%
    echo.
    echo 请先执行 Maven 打包命令：
    echo   cd 到项目根目录
    echo   mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)
echo [成功] Maven 打包文件检查完成
echo.

REM 清理旧的输出目录（保留 runner 相关文件）
echo [步骤 4/9] 清理旧的输出目录...
if exist "output" (
    REM 备份 runner 相关文件
    if exist "output\runner-1.0.0.jar" move /Y "output\runner-1.0.0.jar" "runner-1.0.0.jar.bak" >nul 2>&1
    if exist "output\runner.go" move /Y "output\runner.go" "runner.go.bak" >nul 2>&1
    
    REM 清理目录
    rd /s /q output
    echo [成功] 已清理旧的输出目录
) else (
    echo [提示] 输出目录不存在，跳过清理
)

REM 创建输出目录
mkdir output
mkdir output\BOOT-INF
mkdir output\BOOT-INF\lib

REM 恢复 runner 相关文件
if exist "runner-1.0.0.jar.bak" (
    move /Y "runner-1.0.0.jar.bak" "output\runner-1.0.0.jar" >nul 2>&1
    echo [成功] 已恢复 runner-1.0.0.jar
)
if exist "runner.go.bak" (
    move /Y "runner.go.bak" "output\runner.go" >nul 2>&1
    echo [成功] 已恢复 runner.go
)

echo [成功] 输出目录准备完成
echo.

REM 执行 Allatori 代码混淆
echo [步骤 5/9] 开始执行代码混淆（第一层保护）...
echo [提示] 这个过程可能需要几分钟，请耐心等待...
echo.

java -jar allatori_crack.jar config.xml

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 代码混淆失败！错误代码: %ERRORLEVEL%
    echo 请检查配置文件和日志文件 output/allatori-obfuscation-log.xml
    pause
    exit /b 1
)

echo.
echo [成功] 代码混淆完成
echo.

REM 合并混淆后的模块到主 JAR
echo [步骤 6/9] 合并混淆后的模块...
cd output
jar -uvf0 contract-tools-sdk-obfuscated.jar BOOT-INF\lib

if %ERRORLEVEL% NEQ 0 (
    echo [错误] JAR 文件合并失败！
    cd ..
    pause
    exit /b 1
)

cd ..
echo [成功] 模块合并完成
echo.

REM 执行 JAR 加密（第二层保护）
echo.
echo [步骤 7/9] 开始执行 JAR 加密...
echo [提示] 使用 runner 工具对混淆后的 JAR 进行加密保护...
echo [提示] 加密工具会自动更新 runner.go 中的 MD5/SHA1 值
echo.

cd output
call java -jar runner-1.0.0.jar contract-tools-sdk-obfuscated.jar contract-tools-sdk-encrypted.jar zhaoxinmsPsd

if %ERRORLEVEL% NEQ 0 (
    echo [错误] JAR 加密失败！
    cd ..
    pause
    exit /b 1
)

echo.
echo [成功] JAR 加密完成
echo [提示] runner.go 已自动更新 MD5/SHA1 值
echo.

REM 编译 Go 启动器（Linux 版本）
echo.
echo [步骤 8/9] 编译 Go 启动器 - Linux...
echo [提示] 使用更新后的 runner.go 编译启动器
set GOOS=linux
set GOARCH=amd64
call go build runner.go

if %ERRORLEVEL% NEQ 0 (
    echo [警告] Linux 启动器编译失败，请确保已安装 Go 环境
    echo [提示] 如不需要 Linux 版本可忽略此警告
) else (
    echo [成功] Linux 启动器编译完成 - runner-linux
)
echo.

REM 编译 Go 启动器（Windows 版本）
echo.
echo [步骤 9/9] 编译 Go 启动器 - Windows...
set GOOS=windows
set GOARCH=amd64
call go build runner.go

if %ERRORLEVEL% NEQ 0 (
    echo [警告] Windows 启动器编译失败，请确保已安装 Go 环境
    echo [提示] 如不需要 Windows 版本可忽略此警告
) else (
    echo [成功] Windows 启动器编译完成 - runner.exe
)

cd ..
echo.

REM 显示输出文件信息
echo.
echo ========================================
echo 打包完成
echo ========================================
echo.
echo 输出目录: output
echo.
echo 混淆版JAR: contract-tools-sdk-obfuscated.jar
echo 加密版JAR: contract-tools-sdk-encrypted.jar
echo Windows启动器: runner.exe
echo Linux启动器: runner
echo.
echo 混淆日志: output\allatori-obfuscation-log.xml
echo.
echo ========================================

pause