# 肇新工具集 Demo Nginx 部署指南

## 概述

本指南介绍如何使用 Nginx 统一部署肇新工具集 Demo 的前后端服务到 80 端口。

- **前端**: 静态文件服务
- **后端**: 通过 Nginx 反向代理
- **统一访问**: 所有服务通过 80 端口访问

## 架构图

```
用户请求 (80端口)
       ↓
   Nginx 服务器
       ├── / → 前端静态文件
       └── /api/ → 后端服务 (8091端口)
```

## 部署准备

### 1. 环境要求

**服务器环境**:
- Linux 系统 (CentOS 7+, Ubuntu 18.04+)
- Nginx 1.18+
- Java 17+
- 至少 2GB 内存

**权限要求**:
- root 或 sudo 权限
- 防火墙开放 80 端口

### 2. 构建项目

在部署前，先构建项目：

```bash
# 执行完整构建
./build.sh

# 确认构建产物
ls -la dist/
```

## 快速部署

### 自动部署脚本

```bash
# 1. 赋予执行权限
chmod +x deploy-nginx.sh

# 2. 以 root 权限执行部署
sudo ./deploy-nginx.sh
```

部署脚本会自动完成：
- ✅ 部署前端静态文件
- ✅ 配置 Nginx 反向代理
- ✅ 创建后端 systemd 服务
- ✅ 启动所有服务

## 手动部署步骤

### 1. 部署前端静态文件

```bash
# 创建 Nginx 网站目录
sudo mkdir -p /usr/share/nginx/html/zhaoxin-demo

# 复制前端文件
sudo cp -r dist/frontend/* /usr/share/nginx/html/zhaoxin-demo/

# 设置权限
sudo chown -R nginx:nginx /usr/share/nginx/html/zhaoxin-demo
```

### 2. 配置 Nginx

```bash
# 复制 Nginx 配置
sudo cp nginx.conf /etc/nginx/conf.d/zhaoxin-demo.conf

# 测试配置
sudo nginx -t

# 重新加载配置
sudo systemctl reload nginx
```

### 3. 部署后端服务

```bash
# 创建应用目录
sudo mkdir -p /opt/zhaoxin-demo

# 复制后端文件
sudo cp dist/backend/*.jar /opt/zhaoxin-demo/app.jar
sudo cp dist/config/application.yml /opt/zhaoxin-demo/

# 创建服务用户
sudo useradd -r -s /bin/false zhaoxin

# 设置权限
sudo chown -R zhaoxin:zhaoxin /opt/zhaoxin-demo
```

### 4. 创建 systemd 服务

创建服务文件 `/etc/systemd/system/zhaoxin-demo-backend.service`：

```ini
[Unit]
Description=Zhaoxin Tools Demo Backend Service
After=network.target

[Service]
Type=simple
User=zhaoxin
Group=zhaoxin
WorkingDirectory=/opt/zhaoxin-demo
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
```

### 5. 启动服务

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动并启用服务
sudo systemctl enable zhaoxin-demo-backend
sudo systemctl start zhaoxin-demo-backend

# 启用 Nginx 开机自启
sudo systemctl enable nginx
```

## Nginx 配置说明

### 核心配置项

**上游服务配置**:
```nginx
upstream zhaoxin_demo_backend {
    server 127.0.0.1:8091;
    keepalive 32;
}
```

**前端静态文件**:
```nginx
location / {
    root /usr/share/nginx/html/zhaoxin-demo;
    index index.html;
    try_files $uri $uri/ /index.html;  # SPA 路由支持
}
```

**后端 API 代理**:
```nginx
location /api/ {
    proxy_pass http://zhaoxin_demo_backend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

### 性能优化配置

**静态资源缓存**:
```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

**Gzip 压缩**:
```nginx
gzip on;
gzip_comp_level 6;
gzip_types text/plain text/css application/json application/javascript;
```

**文件上传限制**:
```nginx
client_max_body_size 50M;  # 全局限制
location /api/upload {
    client_max_body_size 100M;  # 上传接口特殊限制
}
```

## 服务管理

### 后端服务管理

```bash
# 查看服务状态
sudo systemctl status zhaoxin-demo-backend

# 启动/停止/重启服务
sudo systemctl start zhaoxin-demo-backend
sudo systemctl stop zhaoxin-demo-backend
sudo systemctl restart zhaoxin-demo-backend

# 查看实时日志
sudo journalctl -u zhaoxin-demo-backend -f

# 查看最近日志
sudo journalctl -u zhaoxin-demo-backend --since "1 hour ago"
```

### Nginx 管理

```bash
# 测试配置
sudo nginx -t

# 重新加载配置（不中断服务）
sudo systemctl reload nginx

# 重启 Nginx
sudo systemctl restart nginx

# 查看 Nginx 状态
sudo systemctl status nginx

# 查看访问日志
sudo tail -f /var/log/nginx/zhaoxin-demo-access.log

# 查看错误日志
sudo tail -f /var/log/nginx/zhaoxin-demo-error.log
```

## 访问验证

部署完成后，验证服务是否正常：

### 1. 前端访问测试

```bash
# 访问首页
curl -I http://localhost/

# 应该返回 200 状态码
```

### 2. 后端 API 测试

```bash
# 健康检查
curl http://localhost/health

# API 接口测试
curl http://localhost/api/system/version
```

### 3. 浏览器访问

打开浏览器访问：
- 前端地址: `http://your-server-ip/`
- API 文档: `http://your-server-ip/api/`

## 域名配置

### 1. 修改 Nginx 配置

编辑 `/etc/nginx/conf.d/zhaoxin-demo.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    # ... 其他配置
}
```

### 2. 配置 DNS

将域名 A 记录指向服务器 IP 地址。

### 3. SSL 证书配置（可选）

```bash
# 安装 Certbot
sudo yum install certbot python3-certbot-nginx

# 获取免费 SSL 证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo crontab -e
# 添加: 0 12 * * * /usr/bin/certbot renew --quiet
```

## 故障排除

### 常见问题

**1. 502 Bad Gateway**
```bash
# 检查后端服务是否运行
sudo systemctl status zhaoxin-demo-backend

# 检查端口是否监听
sudo netstat -tlnp | grep 8091

# 查看后端日志
sudo journalctl -u zhaoxin-demo-backend --since "10 minutes ago"
```

**2. 404 Not Found**
```bash
# 检查前端文件是否存在
ls -la /usr/share/nginx/html/zhaoxin-demo/

# 检查 Nginx 配置
sudo nginx -t

# 查看 Nginx 错误日志
sudo tail -f /var/log/nginx/error.log
```

**3. 文件上传失败**
```bash
# 检查文件大小限制
grep client_max_body_size /etc/nginx/conf.d/zhaoxin-demo.conf

# 检查磁盘空间
df -h
```

### 性能监控

**系统资源监控**:
```bash
# CPU 和内存使用
top -p $(pgrep -f "zhaoxin-demo")

# 磁盘 I/O
iotop

# 网络连接
ss -tulnp | grep :80
```

**应用监控**:
```bash
# Nginx 连接数
curl http://localhost/nginx_status

# 后端健康检查
curl http://localhost/health
```

## 备份与恢复

### 备份重要文件

```bash
# 创建备份目录
sudo mkdir -p /backup/zhaoxin-demo

# 备份配置文件
sudo cp /etc/nginx/conf.d/zhaoxin-demo.conf /backup/zhaoxin-demo/
sudo cp /etc/systemd/system/zhaoxin-demo-backend.service /backup/zhaoxin-demo/

# 备份应用文件
sudo tar -czf /backup/zhaoxin-demo/app-$(date +%Y%m%d).tar.gz /opt/zhaoxin-demo/

# 备份前端文件
sudo tar -czf /backup/zhaoxin-demo/frontend-$(date +%Y%m%d).tar.gz /usr/share/nginx/html/zhaoxin-demo/
```

### 自动备份脚本

```bash
#!/bin/bash
# /etc/cron.daily/zhaoxin-demo-backup

BACKUP_DIR="/backup/zhaoxin-demo"
DATE=$(date +%Y%m%d)

mkdir -p "$BACKUP_DIR"

# 备份应用
tar -czf "$BACKUP_DIR/app-$DATE.tar.gz" /opt/zhaoxin-demo/

# 清理 7 天前的备份
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +7 -delete
```

## 安全建议

### 1. 防火墙配置

```bash
# 只开放必要端口
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 2. 访问控制

```nginx
# 限制管理接口访问
location /api/admin/ {
    allow 192.168.1.0/24;  # 只允许内网访问
    deny all;
    proxy_pass http://zhaoxin_demo_backend;
}
```

### 3. 速率限制

```nginx
# 限制请求频率
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

location /api/ {
    limit_req zone=api burst=20 nodelay;
    proxy_pass http://zhaoxin_demo_backend;
}
```

---

**版本**: 1.0.0  
**更新时间**: 2025-01-13  
**维护团队**: 肇新科技
