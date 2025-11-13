@echo off
chcp 65001 >nul
echo ========================================
echo 肇新工具集 Demo 项目打包脚本
echo ========================================
echo.

:: 设置变量
set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%backend
set FRONTEND_DIR=%PROJECT_ROOT%frontend
set DIST_DIR=%PROJECT_ROOT%dist
set BUILD_TIME=%date:~0,10% %time:~0,8%

echo [INFO] 项目根目录: %PROJECT_ROOT%
echo [INFO] 后端目录: %BACKEND_DIR%
echo [INFO] 前端目录: %FRONTEND_DIR%
echo [INFO] 输出目录: %DIST_DIR%
echo [INFO] 构建时间: %BUILD_TIME%
echo.

:: 检查必要的工具
echo [INFO] 检查构建环境...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven 未找到，请确保 Maven 已安装并添加到 PATH
    pause
    exit /b 1
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js 未找到，请确保 Node.js 已安装并添加到 PATH
    pause
    exit /b 1
)

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm 未找到，请确保 npm 已安装并添加到 PATH
    pause
    exit /b 1
)

echo [SUCCESS] 构建环境检查通过
echo.

:: 清理旧的构建文件
echo [INFO] 清理旧的构建文件...
if exist "%DIST_DIR%" (
    rmdir /s /q "%DIST_DIR%"
    echo [SUCCESS] 已清理旧的 dist 目录
)

if exist "%BACKEND_DIR%\target" (
    rmdir /s /q "%BACKEND_DIR%\target"
    echo [SUCCESS] 已清理后端 target 目录
)

if exist "%FRONTEND_DIR%\dist" (
    rmdir /s /q "%FRONTEND_DIR%\dist"
    echo [SUCCESS] 已清理前端 dist 目录
)

if exist "%FRONTEND_DIR%\node_modules" (
    echo [INFO] 发现 node_modules，跳过清理（如需重新安装依赖，请手动删除）
) else (
    echo [INFO] 未发现 node_modules，稍后将安装依赖
)
echo.

:: 创建输出目录
mkdir "%DIST_DIR%" 2>nul
mkdir "%DIST_DIR%\backend" 2>nul
mkdir "%DIST_DIR%\frontend" 2>nul
mkdir "%DIST_DIR%\scripts" 2>nul
mkdir "%DIST_DIR%\config" 2>nul
mkdir "%DIST_DIR%\nginx" 2>nul

:: 构建后端
echo ========================================
echo 开始构建后端项目...
echo ========================================
cd /d "%BACKEND_DIR%"

echo [INFO] 执行 Maven 清理和打包...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] 后端构建失败
    pause
    exit /b 1
)

echo [SUCCESS] 后端构建完成
echo.

:: 复制后端构建产物
echo [INFO] 复制后端构建产物...
copy "%BACKEND_DIR%\target\*.jar" "%DIST_DIR%\backend\" >nul
if %errorlevel% neq 0 (
    echo [ERROR] 复制后端 JAR 文件失败
    pause
    exit /b 1
)

:: 获取JAR文件名
for %%f in ("%BACKEND_DIR%\target\*.jar") do (
    if not "%%~nf"=="%%~nf.original" (
        set JAR_NAME=%%~nxf
    )
)

echo [SUCCESS] 后端 JAR 文件已复制: %JAR_NAME%
echo.

:: 构建前端
echo ========================================
echo 开始构建前端项目...
echo ========================================
cd /d "%FRONTEND_DIR%"

:: 检查并安装依赖
if not exist "node_modules" (
    echo [INFO] 安装前端依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] 前端依赖安装失败
        pause
        exit /b 1
    )
    echo [SUCCESS] 前端依赖安装完成
) else (
    echo [INFO] 前端依赖已存在，跳过安装
)

echo [INFO] 执行前端构建...
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] 前端构建失败
    pause
    exit /b 1
)

echo [SUCCESS] 前端构建完成
echo.

:: 复制前端构建产物
echo [INFO] 复制前端构建产物...
xcopy "%FRONTEND_DIR%\dist\*" "%DIST_DIR%\frontend\" /s /e /y >nul
if %errorlevel% neq 0 (
    echo [ERROR] 复制前端构建产物失败
    pause
    exit /b 1
)

echo [SUCCESS] 前端构建产物已复制
echo.

:: 创建启动脚本
echo [INFO] 创建启动脚本...

:: Windows 启动脚本
(
echo @echo off
echo chcp 65001 ^>nul
echo echo ========================================
echo echo 肇新工具集 Demo 启动脚本
echo echo ========================================
echo echo.
echo.
echo :: 检查 Java 环境
echo where java ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo [ERROR] Java 未找到，请确保 Java 17+ 已安装并添加到 PATH
echo     pause
echo     exit /b 1
echo ^)
echo.
echo :: 获取 Java 版本
echo for /f "tokens=3" %%%%i in ^('java -version 2^>^&1 ^| findstr "version"'^) do set JAVA_VERSION=%%%%i
echo echo [INFO] Java 版本: %%JAVA_VERSION%%
echo.
echo :: 设置 JVM 参数
echo set JAVA_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai
echo.
echo :: 启动应用
echo echo [INFO] 启动肇新工具集 Demo 后端服务...
echo echo [INFO] 服务端口: 8091
echo echo [INFO] 配置文件: application.yml
echo echo [INFO] 按 Ctrl+C 停止服务
echo echo.
echo.
echo java %%JAVA_OPTS%% -jar backend\%JAR_NAME%
echo.
echo pause
) > "%DIST_DIR%\scripts\start-backend.bat"

:: Linux 启动脚本
(
echo #!/bin/bash
echo.
echo echo "========================================"
echo echo "肇新工具集 Demo 启动脚本"
echo echo "========================================"
echo echo
echo.
echo # 检查 Java 环境
echo if ! command -v java ^&^> /dev/null; then
echo     echo "[ERROR] Java 未找到，请确保 Java 17+ 已安装"
echo     exit 1
echo fi
echo.
echo # 获取 Java 版本
echo JAVA_VERSION=$^(java -version 2^>^&1 ^| head -n 1 ^| cut -d'"' -f2^)
echo echo "[INFO] Java 版本: $JAVA_VERSION"
echo.
echo # 设置 JVM 参数
echo JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
echo.
echo # 启动应用
echo echo "[INFO] 启动肇新工具集 Demo 后端服务..."
echo echo "[INFO] 服务端口: 8091"
echo echo "[INFO] 配置文件: application.yml"
echo echo "[INFO] 按 Ctrl+C 停止服务"
echo echo
echo.
echo java $JAVA_OPTS -jar backend/%JAR_NAME%
) > "%DIST_DIR%\scripts\start-backend.sh"

:: 前端服务脚本（使用 http-server）
(
echo @echo off
echo chcp 65001 ^>nul
echo echo ========================================
echo echo 肇新工具集 Demo 前端服务启动脚本
echo echo ========================================
echo echo.
echo.
echo :: 检查 Node.js 环境
echo where node ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo [ERROR] Node.js 未找到，请确保 Node.js 已安装并添加到 PATH
echo     pause
echo     exit /b 1
echo ^)
echo.
echo :: 检查 http-server
echo where http-server ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo [INFO] 安装 http-server...
echo     npm install -g http-server
echo     if %%errorlevel%% neq 0 ^(
echo         echo [ERROR] http-server 安装失败
echo         pause
echo         exit /b 1
echo     ^)
echo ^)
echo.
echo :: 启动前端服务
echo echo [INFO] 启动前端服务...
echo echo [INFO] 前端地址: http://localhost:3004
echo echo [INFO] 后端代理: http://localhost:8091
echo echo [INFO] 按 Ctrl+C 停止服务
echo echo.
echo.
echo cd /d "%%~dp0..\frontend"
echo http-server -p 3004 -P http://localhost:8091 --cors
echo.
echo pause
) > "%DIST_DIR%\scripts\start-frontend.bat"

:: 一键启动脚本
(
echo @echo off
echo chcp 65001 ^>nul
echo echo ========================================
echo echo 肇新工具集 Demo 一键启动脚本
echo echo ========================================
echo echo.
echo echo [INFO] 将同时启动后端服务和前端服务
echo echo [INFO] 后端地址: http://localhost:8091
echo echo [INFO] 前端地址: http://localhost:3004
echo echo.
echo pause
echo.
echo :: 启动后端服务
echo start "肇新Demo后端" cmd /k "%%~dp0start-backend.bat"
echo.
echo :: 等待2秒
echo timeout /t 2 /nobreak ^>nul
echo.
echo :: 启动前端服务
echo start "肇新Demo前端" cmd /k "%%~dp0start-frontend.bat"
echo.
echo echo [SUCCESS] 服务启动完成
echo echo [INFO] 请在浏览器中访问: http://localhost:3004
echo echo.
echo pause
) > "%DIST_DIR%\scripts\start-all.bat"

echo [SUCCESS] 启动脚本已创建
echo.

:: 复制配置文件
echo [INFO] 复制配置文件...
copy "%BACKEND_DIR%\src\main\resources\application.yml" "%DIST_DIR%\config\application.yml" >nul
copy "%FRONTEND_DIR%\package.json" "%DIST_DIR%\config\frontend-package.json" >nul

echo [SUCCESS] 配置文件已复制
echo.

:: 复制 Nginx 部署文件
echo [INFO] 复制 Nginx 部署文件...
copy "%PROJECT_ROOT%\build-tools\deployment\nginx\nginx.conf" "%DIST_DIR%\nginx\nginx.conf" >nul
copy "%PROJECT_ROOT%\build-tools\deployment\nginx\deploy-nginx.sh" "%DIST_DIR%\nginx\deploy-nginx.sh" >nul
copy "%PROJECT_ROOT%\build-tools\deployment\nginx\README.md" "%DIST_DIR%\nginx\README.md" >nul

echo [SUCCESS] Nginx 部署文件已复制
echo.

:: 创建部署说明文档
echo [INFO] 创建部署说明文档...
(
echo # 肇新工具集 Demo 部署说明
echo.
echo ## 构建信息
echo - 构建时间: %BUILD_TIME%
echo - 后端 JAR: %JAR_NAME%
echo - Java 版本要求: 17+
echo - Node.js 版本要求: 16+
echo.
echo ## 目录结构
echo ```
echo dist/
echo ├── backend/           # 后端 JAR 文件
echo ├── frontend/          # 前端静态文件
echo ├── scripts/           # 启动脚本
echo ├── config/            # 配置文件
echo ├── nginx/             # Nginx 部署文件
echo └── README.md          # 本说明文件
echo ```
echo.
echo ## 部署步骤
echo.
echo ### 1. 环境要求
echo - Java 17 或更高版本
echo - Node.js 16 或更高版本 ^(仅前端服务需要^)
echo.
echo ### 2. 配置修改
echo 根据实际部署环境，修改 `config/application.yml` 中的配置：
echo ```yaml
echo zhaoxin:
echo   api:
echo     base-url: http://your-zhaoxin-api-server  # 肇新API服务地址
echo   frontend:
echo     url: http://your-zhaoxin-frontend         # 肇新前端地址
echo   demo:
echo     backend-url: http://your-demo-backend:8091 # Demo后端地址
echo ```
echo.
echo ### 3. 启动服务
echo.
echo #### 开发环境（Windows）
echo ```bash
echo # 一键启动（推荐）
echo cd scripts
echo start-all.bat
echo.
echo # 或分别启动
echo start-backend.bat    # 启动后端服务
echo start-frontend.bat   # 启动前端服务
echo ```
echo.
echo #### 生产环境（Nginx 部署）
echo ```bash
echo # 使用 Nginx 统一部署到 80 端口
echo cd nginx
echo chmod +x deploy-nginx.sh
echo sudo ./deploy-nginx.sh
echo ```
echo.
echo #### Linux 开发环境
echo ```bash
echo # 启动后端
echo chmod +x scripts/start-backend.sh
echo ./scripts/start-backend.sh
echo.
echo # 启动前端（需要另开终端）
echo cd frontend
echo python -m http.server 3004  # 或使用其他静态文件服务器
echo ```
echo.
echo ### 4. 访问地址
echo - 前端地址: http://localhost:3004
echo - 后端API: http://localhost:8091
echo.
echo ### 5. 生产环境部署
echo.
echo #### 后端部署
echo 1. 将 `backend/%JAR_NAME%` 上传到服务器
echo 2. 修改 `config/application.yml` 配置
echo 3. 使用以下命令启动：
echo ```bash
echo java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -jar %JAR_NAME%
echo ```
echo.
echo #### 前端部署
echo 1. 将 `frontend/` 目录下的所有文件上传到 Web 服务器
echo 2. 配置 Nginx 或 Apache 代理后端 API
echo 3. 配置示例 ^(Nginx^):
echo ```nginx
echo server {
echo     listen 80;
echo     server_name your-domain.com;
echo     
echo     location / {
echo         root /path/to/frontend;
echo         try_files $uri $uri/ /index.html;
echo     }
echo     
echo     location /api/ {
echo         proxy_pass http://localhost:8091;
echo         proxy_set_header Host $host;
echo         proxy_set_header X-Real-IP $remote_addr;
echo     }
echo }
echo ```
echo.
echo ## 故障排除
echo.
echo ### 常见问题
echo 1. **端口被占用**: 修改 `application.yml` 中的 `server.port`
echo 2. **Java 版本不兼容**: 确保使用 Java 17+
echo 3. **API 连接失败**: 检查 `zhaoxin.api.base-url` 配置
echo 4. **前端无法访问**: 检查前端服务是否启动，端口是否正确
echo.
echo ### 日志查看
echo - 后端日志: 控制台输出
echo - 前端日志: 浏览器开发者工具
echo.
echo ## 技术支持
echo 如有问题，请联系肇新科技技术支持团队。
) > "%DIST_DIR%\README.md"

echo [SUCCESS] 部署说明文档已创建
echo.

:: 创建版本信息文件
(
echo {
echo   "buildTime": "%BUILD_TIME%",
echo   "backendJar": "%JAR_NAME%",
echo   "version": "1.0.0",
echo   "description": "肇新工具集 Demo 项目构建产物"
echo }
) > "%DIST_DIR%\build-info.json"

:: 回到项目根目录
cd /d "%PROJECT_ROOT%"

:: 显示构建结果
echo ========================================
echo 构建完成！
echo ========================================
echo.
echo [SUCCESS] 所有组件构建成功
echo [INFO] 构建产物位置: %DIST_DIR%
echo [INFO] 后端 JAR: %JAR_NAME%
echo [INFO] 前端文件: 已复制到 dist/frontend/
echo [INFO] 启动脚本: 已创建到 dist/scripts/
echo.
echo 快速启动:
echo   cd dist\scripts
echo   start-all.bat
echo.
echo 详细说明请查看: dist\README.md
echo.
pause
