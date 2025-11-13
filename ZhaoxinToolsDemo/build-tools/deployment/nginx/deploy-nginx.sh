#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo Nginx 部署脚本"
echo "========================================"
echo

# 设置变量
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
NGINX_ROOT="/usr/share/nginx/html/zhaoxin-demo"
NGINX_CONFIG="/etc/nginx/conf.d/zhaoxin-demo.conf"
BACKEND_SERVICE="zhaoxin-demo-backend"

# 检查是否以 root 权限运行
if [ "$EUID" -ne 0 ]; then
    echo "[ERROR] 请以 root 权限运行此脚本"
    echo "使用: sudo $0"
    exit 1
fi

# 检查构建产物是否存在
if [ ! -d "$DIST_DIR" ]; then
    echo "[ERROR] 构建产物不存在，请先执行构建脚本"
    echo "运行: ./build.sh"
    exit 1
fi

echo "[INFO] 开始部署肇新工具集 Demo..."
echo "[INFO] 构建产物目录: $DIST_DIR"
echo "[INFO] Nginx 根目录: $NGINX_ROOT"
echo

# 1. 部署前端静态文件
echo "[1/5] 部署前端静态文件..."
mkdir -p "$NGINX_ROOT"
cp -r "$DIST_DIR/frontend/"* "$NGINX_ROOT/"
chown -R nginx:nginx "$NGINX_ROOT"
echo "[SUCCESS] 前端文件已部署到 $NGINX_ROOT"

# 2. 配置 Nginx
echo "[2/5] 配置 Nginx..."
cp "$PROJECT_ROOT/nginx.conf" "$NGINX_CONFIG"
echo "[SUCCESS] Nginx 配置已复制到 $NGINX_CONFIG"

# 3. 测试 Nginx 配置
echo "[3/5] 测试 Nginx 配置..."
nginx -t
if [ $? -ne 0 ]; then
    echo "[ERROR] Nginx 配置测试失败"
    exit 1
fi
echo "[SUCCESS] Nginx 配置测试通过"

# 4. 部署后端服务
echo "[4/5] 部署后端服务..."

# 创建后端服务目录
BACKEND_DIR="/opt/zhaoxin-demo"
mkdir -p "$BACKEND_DIR"

# 复制后端文件
cp "$DIST_DIR/backend/"*.jar "$BACKEND_DIR/app.jar"
cp "$DIST_DIR/config/application.yml" "$BACKEND_DIR/"

# 创建后端服务用户
if ! id "zhaoxin" &>/dev/null; then
    useradd -r -s /bin/false zhaoxin
    echo "[INFO] 已创建服务用户: zhaoxin"
fi

# 设置文件权限
chown -R zhaoxin:zhaoxin "$BACKEND_DIR"
chmod +x "$BACKEND_DIR/app.jar"

# 创建 systemd 服务文件
cat > "/etc/systemd/system/$BACKEND_SERVICE.service" << EOF
[Unit]
Description=Zhaoxin Tools Demo Backend Service
After=network.target

[Service]
Type=simple
User=zhaoxin
Group=zhaoxin
WorkingDirectory=$BACKEND_DIR
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# 环境变量
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
EOF

echo "[SUCCESS] 后端服务已配置"

# 5. 启动服务
echo "[5/5] 启动服务..."

# 重新加载 systemd
systemctl daemon-reload

# 启动并启用后端服务
systemctl enable "$BACKEND_SERVICE"
systemctl start "$BACKEND_SERVICE"

# 等待后端服务启动
echo "[INFO] 等待后端服务启动..."
sleep 5

# 检查后端服务状态
if systemctl is-active --quiet "$BACKEND_SERVICE"; then
    echo "[SUCCESS] 后端服务启动成功"
else
    echo "[ERROR] 后端服务启动失败"
    systemctl status "$BACKEND_SERVICE"
    exit 1
fi

# 重新加载 Nginx
systemctl reload nginx
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Nginx 重新加载成功"
else
    echo "[ERROR] Nginx 重新加载失败"
    exit 1
fi

# 启用 Nginx 开机自启
systemctl enable nginx

echo
echo "========================================"
echo "部署完成！"
echo "========================================"
echo
echo "[SUCCESS] 肇新工具集 Demo 部署成功"
echo
echo "服务信息:"
echo "  - 前端地址: http://$(hostname -I | awk '{print $1}')"
echo "  - 后端API: http://$(hostname -I | awk '{print $1}')/api"
echo "  - 健康检查: http://$(hostname -I | awk '{print $1}')/health"
echo
echo "服务管理命令:"
echo "  - 查看后端状态: systemctl status $BACKEND_SERVICE"
echo "  - 重启后端: systemctl restart $BACKEND_SERVICE"
echo "  - 查看后端日志: journalctl -u $BACKEND_SERVICE -f"
echo "  - 重新加载Nginx: systemctl reload nginx"
echo
echo "配置文件位置:"
echo "  - Nginx配置: $NGINX_CONFIG"
echo "  - 前端文件: $NGINX_ROOT"
echo "  - 后端文件: $BACKEND_DIR"
echo
