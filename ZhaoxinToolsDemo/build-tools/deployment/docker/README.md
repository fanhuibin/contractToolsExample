# Docker 容器化部署方案

## 概述

本方案使用 Docker 容器化部署肇新工具集 Demo，提供完整的容器化解决方案。

## 架构

```
Docker 容器架构
├── zhaoxin-demo-frontend (Nginx)  # 前端容器 (端口 80)
└── zhaoxin-demo-backend (Java)    # 后端容器 (端口 8091)
```

## 文件说明

- `Dockerfile.backend` - 后端容器构建文件
- `Dockerfile.frontend` - 前端容器构建文件  
- `docker-compose.yml` - 容器编排配置
- `README.md` - 本说明文件

## 部署步骤

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 已完成项目构建 (`dist/` 目录存在)

### 2. 构建和启动

```bash
# 1. 确保已构建项目
cd ../../..  # 回到项目根目录
./build.sh   # 或 ./build.bat

# 2. 进入 Docker 部署目录
cd build-tools/deployment/docker

# 3. 构建并启动容器
docker-compose up -d --build

# 4. 查看容器状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f
```

### 3. 访问服务

- **前端地址**: http://localhost/
- **后端API**: http://localhost/api/
- **健康检查**: http://localhost/health

### 4. 服务管理

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f [service_name]

# 进入容器
docker-compose exec zhaoxin-demo-backend bash
docker-compose exec zhaoxin-demo-frontend sh
```

## 配置说明

### 环境变量

可以通过环境变量自定义配置：

```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - JAVA_OPTS=-Xms512m -Xmx1024m
  - ZHAOXIN_API_BASE_URL=http://your-api-server
```

### 数据持久化

```yaml
# docker-compose.yml
volumes:
  - ./logs:/app/logs          # 日志持久化
  - ./config:/app/config      # 配置文件持久化
```

### 网络配置

```yaml
# docker-compose.yml
networks:
  zhaoxin-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## 生产环境优化

### 1. 资源限制

```yaml
# docker-compose.yml
services:
  zhaoxin-demo-backend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 2. 健康检查

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8091/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### 3. 日志管理

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

## 扩展部署

### 1. 多实例部署

```yaml
# docker-compose.yml
services:
  zhaoxin-demo-backend:
    deploy:
      replicas: 3
    ports:
      - "8091-8093:8091"
```

### 2. 负载均衡

```yaml
# nginx.conf
upstream backend {
    server zhaoxin-demo-backend-1:8091;
    server zhaoxin-demo-backend-2:8091;
    server zhaoxin-demo-backend-3:8091;
}
```

### 3. 数据库集成

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: zhaoxin_demo
    volumes:
      - mysql_data:/var/lib/mysql
```

## 监控和运维

### 1. 容器监控

```bash
# 查看资源使用
docker stats

# 查看容器详情
docker inspect zhaoxin-demo-backend
```

### 2. 日志收集

```yaml
# 使用 ELK Stack
services:
  elasticsearch:
    image: elasticsearch:7.14.0
  
  logstash:
    image: logstash:7.14.0
  
  kibana:
    image: kibana:7.14.0
```

### 3. 备份策略

```bash
# 备份容器数据
docker run --rm -v zhaoxin_logs:/data -v $(pwd):/backup alpine tar czf /backup/logs-backup.tar.gz /data

# 备份镜像
docker save zhaoxin-demo-backend:latest | gzip > backend-image.tar.gz
```

## 故障排除

### 常见问题

**1. 容器启动失败**
```bash
# 查看详细日志
docker-compose logs zhaoxin-demo-backend

# 检查容器状态
docker-compose ps
```

**2. 网络连接问题**
```bash
# 检查网络
docker network ls
docker network inspect docker_zhaoxin-network

# 测试连接
docker-compose exec zhaoxin-demo-frontend ping zhaoxin-demo-backend
```

**3. 端口冲突**
```bash
# 检查端口占用
netstat -tlnp | grep :80
netstat -tlnp | grep :8091

# 修改端口映射
ports:
  - "8080:80"    # 改为其他端口
```

### 性能调优

**1. JVM 参数优化**
```yaml
environment:
  - JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**2. Nginx 优化**
```nginx
worker_processes auto;
worker_connections 1024;
keepalive_timeout 65;
```

**3. 容器资源优化**
```yaml
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 2G
```

## 安全配置

### 1. 网络安全

```yaml
# 内部网络隔离
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

### 2. 用户权限

```dockerfile
# 非 root 用户运行
RUN groupadd -r zhaoxin && useradd -r -g zhaoxin zhaoxin
USER zhaoxin
```

### 3. 镜像安全

```bash
# 扫描镜像漏洞
docker scan zhaoxin-demo-backend:latest

# 使用最小化基础镜像
FROM openjdk:17-jre-alpine
```

---

**版本**: 1.0.0  
**状态**: 开发中  
**维护团队**: 肇新科技
