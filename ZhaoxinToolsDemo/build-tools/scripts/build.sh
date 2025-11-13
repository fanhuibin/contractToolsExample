#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 项目打包脚本"
echo "========================================"
echo

# 设置变量
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
DIST_DIR="$PROJECT_ROOT/dist"
BUILD_TIME=$(date '+%Y-%m-%d %H:%M:%S')

echo "[INFO] 项目根目录: $PROJECT_ROOT"
echo "[INFO] 后端目录: $BACKEND_DIR"
echo "[INFO] 前端目录: $FRONTEND_DIR"
echo "[INFO] 输出目录: $DIST_DIR"
echo "[INFO] 构建时间: $BUILD_TIME"
echo

# 检查必要的工具
echo "[INFO] 检查构建环境..."

if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven 未找到，请确保 Maven 已安装并添加到 PATH"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "[ERROR] Node.js 未找到，请确保 Node.js 已安装并添加到 PATH"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "[ERROR] npm 未找到，请确保 npm 已安装并添加到 PATH"
    exit 1
fi

echo "[SUCCESS] 构建环境检查通过"
echo

# 清理旧的构建文件
echo "[INFO] 清理旧的构建文件..."
if [ -d "$DIST_DIR" ]; then
    rm -rf "$DIST_DIR"
    echo "[SUCCESS] 已清理旧的 dist 目录"
fi

if [ -d "$BACKEND_DIR/target" ]; then
    rm -rf "$BACKEND_DIR/target"
    echo "[SUCCESS] 已清理后端 target 目录"
fi

if [ -d "$FRONTEND_DIR/dist" ]; then
    rm -rf "$FRONTEND_DIR/dist"
    echo "[SUCCESS] 已清理前端 dist 目录"
fi

if [ -d "$FRONTEND_DIR/node_modules" ]; then
    echo "[INFO] 发现 node_modules，跳过清理（如需重新安装依赖，请手动删除）"
else
    echo "[INFO] 未发现 node_modules，稍后将安装依赖"
fi
echo

# 创建输出目录
mkdir -p "$DIST_DIR"/{backend,frontend,scripts,config,nginx}

# 构建后端
echo "========================================"
echo "开始构建后端项目..."
echo "========================================"
cd "$BACKEND_DIR"

echo "[INFO] 执行 Maven 清理和打包..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[ERROR] 后端构建失败"
    exit 1
fi

echo "[SUCCESS] 后端构建完成"
echo

# 复制后端构建产物
echo "[INFO] 复制后端构建产物..."
JAR_FILE=$(find "$BACKEND_DIR/target" -name "*.jar" ! -name "*.original.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "[ERROR] 未找到后端 JAR 文件"
    exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
cp "$JAR_FILE" "$DIST_DIR/backend/"
if [ $? -ne 0 ]; then
    echo "[ERROR] 复制后端 JAR 文件失败"
    exit 1
fi

echo "[SUCCESS] 后端 JAR 文件已复制: $JAR_NAME"
echo

# 构建前端
echo "========================================"
echo "开始构建前端项目..."
echo "========================================"
cd "$FRONTEND_DIR"

# 检查并安装依赖
if [ ! -d "node_modules" ]; then
    echo "[INFO] 安装前端依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "[ERROR] 前端依赖安装失败"
        exit 1
    fi
    echo "[SUCCESS] 前端依赖安装完成"
else
    echo "[INFO] 前端依赖已存在，跳过安装"
fi

echo "[INFO] 执行前端构建..."
npm run build
if [ $? -ne 0 ]; then
    echo "[ERROR] 前端构建失败"
    exit 1
fi

echo "[SUCCESS] 前端构建完成"
echo

# 复制前端构建产物
echo "[INFO] 复制前端构建产物..."
cp -r "$FRONTEND_DIR/dist/"* "$DIST_DIR/frontend/"
if [ $? -ne 0 ]; then
    echo "[ERROR] 复制前端构建产物失败"
    exit 1
fi

echo "[SUCCESS] 前端构建产物已复制"
echo

# 创建启动脚本
echo "[INFO] 创建启动脚本..."

# Linux 启动脚本
cat > "$DIST_DIR/scripts/start-backend.sh" << EOF
#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 启动脚本"
echo "========================================"
echo

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 未找到，请确保 Java 17+ 已安装"
    exit 1
fi

# 获取 Java 版本
JAVA_VERSION=\$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "[INFO] Java 版本: \$JAVA_VERSION"

# 设置 JVM 参数
JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 启动应用
echo "[INFO] 启动肇新工具集 Demo 后端服务..."
echo "[INFO] 服务端口: 8091"
echo "[INFO] 配置文件: application.yml"
echo "[INFO] 按 Ctrl+C 停止服务"
echo

java \$JAVA_OPTS -jar ../backend/$JAR_NAME
EOF

# 前端服务脚本
cat > "$DIST_DIR/scripts/start-frontend.sh" << EOF
#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 前端服务启动脚本"
echo "========================================"
echo

# 检查 Python 环境（用于简单的 HTTP 服务器）
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo "[ERROR] Python 未找到，请安装 Python 或使用其他 Web 服务器"
    exit 1
fi

echo "[INFO] 使用 \$PYTHON_CMD 启动前端服务..."
echo "[INFO] 前端地址: http://localhost:3004"
echo "[INFO] 按 Ctrl+C 停止服务"
echo

cd ../frontend
\$PYTHON_CMD -m http.server 3004
EOF

# 一键启动脚本
cat > "$DIST_DIR/scripts/start-all.sh" << EOF
#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 一键启动脚本"
echo "========================================"
echo

echo "[INFO] 将同时启动后端服务和前端服务"
echo "[INFO] 后端地址: http://localhost:8091"
echo "[INFO] 前端地址: http://localhost:3004"
echo

# 启动后端服务（后台运行）
echo "[INFO] 启动后端服务..."
nohup ./start-backend.sh > ../logs/backend.log 2>&1 &
BACKEND_PID=\$!
echo "[INFO] 后端服务 PID: \$BACKEND_PID"

# 等待后端启动
sleep 3

# 启动前端服务（后台运行）
echo "[INFO] 启动前端服务..."
nohup ./start-frontend.sh > ../logs/frontend.log 2>&1 &
FRONTEND_PID=\$!
echo "[INFO] 前端服务 PID: \$FRONTEND_PID"

echo
echo "[SUCCESS] 服务启动完成"
echo "[INFO] 请在浏览器中访问: http://localhost:3004"
echo "[INFO] 后端 PID: \$BACKEND_PID"
echo "[INFO] 前端 PID: \$FRONTEND_PID"
echo
echo "停止服务请执行: kill \$BACKEND_PID \$FRONTEND_PID"
EOF

# 停止服务脚本
cat > "$DIST_DIR/scripts/stop-all.sh" << EOF
#!/bin/bash

echo "========================================"
echo "停止肇新工具集 Demo 服务"
echo "========================================"

# 停止后端服务
BACKEND_PID=\$(ps aux | grep '$JAR_NAME' | grep -v grep | awk '{print \$2}')
if [ ! -z "\$BACKEND_PID" ]; then
    kill \$BACKEND_PID
    echo "[INFO] 已停止后端服务 (PID: \$BACKEND_PID)"
else
    echo "[INFO] 后端服务未运行"
fi

# 停止前端服务
FRONTEND_PID=\$(ps aux | grep 'http.server 3004' | grep -v grep | awk '{print \$2}')
if [ ! -z "\$FRONTEND_PID" ]; then
    kill \$FRONTEND_PID
    echo "[INFO] 已停止前端服务 (PID: \$FRONTEND_PID)"
else
    echo "[INFO] 前端服务未运行"
fi

echo "[SUCCESS] 服务停止完成"
EOF

# 设置脚本执行权限
chmod +x "$DIST_DIR/scripts/"*.sh

echo "[SUCCESS] 启动脚本已创建"
echo

# 复制配置文件
echo "[INFO] 复制配置文件..."
cp "$BACKEND_DIR/src/main/resources/application.yml" "$DIST_DIR/config/application.yml"
cp "$FRONTEND_DIR/package.json" "$DIST_DIR/config/frontend-package.json"

echo "[SUCCESS] 配置文件已复制"
echo

# 复制 Nginx 部署文件
echo "[INFO] 复制 Nginx 部署文件..."
cp "$PROJECT_ROOT/build-tools/deployment/nginx/nginx.conf" "$DIST_DIR/nginx/nginx.conf"
cp "$PROJECT_ROOT/build-tools/deployment/nginx/deploy-nginx.sh" "$DIST_DIR/nginx/deploy-nginx.sh"
cp "$PROJECT_ROOT/build-tools/deployment/nginx/README.md" "$DIST_DIR/nginx/README.md"

# 设置脚本执行权限
chmod +x "$DIST_DIR/nginx/deploy-nginx.sh"

echo "[SUCCESS] Nginx 部署文件已复制"
echo

# 创建日志目录
mkdir -p "$DIST_DIR/logs"

# 创建部署说明文档
echo "[INFO] 创建部署说明文档..."
cat > "$DIST_DIR/README.md" << EOF
# 肇新工具集 Demo 部署说明

## 构建信息
- 构建时间: $BUILD_TIME
- 后端 JAR: $JAR_NAME
- Java 版本要求: 17+
- Node.js 版本要求: 16+

## 目录结构
\`\`\`
dist/
├── backend/           # 后端 JAR 文件
├── frontend/          # 前端静态文件
├── scripts/           # 启动脚本
├── config/            # 配置文件
├── nginx/             # Nginx 部署文件
├── logs/              # 日志文件
└── README.md          # 本说明文件
\`\`\`

## 部署步骤

### 1. 环境要求
- Java 17 或更高版本
- Python 3.x (用于前端服务器) 或其他 Web 服务器

### 2. 配置修改
根据实际部署环境，修改 \`config/application.yml\` 中的配置：
\`\`\`yaml
zhaoxin:
  api:
    base-url: http://your-zhaoxin-api-server  # 肇新API服务地址
  frontend:
    url: http://your-zhaoxin-frontend         # 肇新前端地址
  demo:
    backend-url: http://your-demo-backend:8091 # Demo后端地址
\`\`\`

### 3. 启动服务

#### 开发环境
\`\`\`bash
# 一键启动（推荐）
cd scripts
chmod +x *.sh
./start-all.sh

# 分别启动
./start-backend.sh    # 启动后端
./start-frontend.sh   # 启动前端（需要另开终端）

# 停止服务
./stop-all.sh
\`\`\`

#### 生产环境（Nginx 部署）
\`\`\`bash
# 使用 Nginx 统一部署到 80 端口
cd nginx
chmod +x deploy-nginx.sh
sudo ./deploy-nginx.sh
\`\`\`

### 4. 访问地址
- 前端地址: http://localhost:3004
- 后端API: http://localhost:8091

### 5. 生产环境部署

#### 后端部署
1. 将 \`backend/$JAR_NAME\` 上传到服务器
2. 修改 \`config/application.yml\` 配置
3. 使用以下命令启动：
\`\`\`bash
java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -jar $JAR_NAME
\`\`\`

#### 前端部署
1. 将 \`frontend/\` 目录下的所有文件上传到 Web 服务器
2. 配置 Nginx 或 Apache 代理后端 API
3. 配置示例 (Nginx):
\`\`\`nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/frontend;
        try_files \$uri \$uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8091;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
\`\`\`

## 故障排除

### 常见问题
1. **端口被占用**: 修改 \`application.yml\` 中的 \`server.port\`
2. **Java 版本不兼容**: 确保使用 Java 17+
3. **API 连接失败**: 检查 \`zhaoxin.api.base-url\` 配置
4. **前端无法访问**: 检查前端服务是否启动，端口是否正确

### 日志查看
- 后端日志: \`logs/backend.log\`
- 前端日志: \`logs/frontend.log\`

## 技术支持
如有问题，请联系肇新科技技术支持团队。
EOF

echo "[SUCCESS] 部署说明文档已创建"
echo

# 创建版本信息文件
cat > "$DIST_DIR/build-info.json" << EOF
{
  "buildTime": "$BUILD_TIME",
  "backendJar": "$JAR_NAME",
  "version": "1.0.0",
  "description": "肇新工具集 Demo 项目构建产物"
}
EOF

# 回到项目根目录
cd "$PROJECT_ROOT"

# 显示构建结果
echo "========================================"
echo "构建完成！"
echo "========================================"
echo
echo "[SUCCESS] 所有组件构建成功"
echo "[INFO] 构建产物位置: $DIST_DIR"
echo "[INFO] 后端 JAR: $JAR_NAME"
echo "[INFO] 前端文件: 已复制到 dist/frontend/"
echo "[INFO] 启动脚本: 已创建到 dist/scripts/"
echo
echo "快速启动:"
echo "  cd dist/scripts"
echo "  ./start-all.sh"
echo
echo "详细说明请查看: dist/README.md"
echo
