# SSL证书部署步骤（实施指南）

## 📋 前提条件

假设证书文件位置：`D:\git\zhaoxin-contract-tool-set\docs\证书配置\`，假设服务器ip:192.168.0.100
- `onlyoffice.crt` - 证书文件
- `onlyoffice.key` - 私钥文件
- `onlyoffice.csr` - 证书签名请求
- `dhparam.pem` - DH参数文件

---

## 🚀 部署步骤

### 第1步：上传证书文件

```bash
# 上传到服务器 Nginx 证书目录
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/onlyoffice.crt root@192.168.0.100:/docker/nginx/cert/
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/onlyoffice.key root@192.168.0.100:/docker/nginx/cert/
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/dhparam.pem root@192.168.0.100:/docker/nginx/cert/

# 上传到 OnlyOffice 证书目录
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/onlyoffice.crt root@192.168.0.100:/app/onlyoffice/DocumentServer/certs/
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/onlyoffice.key root@192.168.0.100:/app/onlyoffice/DocumentServer/certs/
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/onlyoffice.csr root@192.168.0.100:/app/onlyoffice/DocumentServer/certs/
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/dhparam.pem root@192.168.0.100:/app/onlyoffice/DocumentServer/certs/
```

### 第2步：设置文件权限

```bash
ssh root@192.168.0.100

# Nginx 证书权限
chmod 644 /docker/nginx/cert/onlyoffice.crt
chmod 600 /docker/nginx/cert/onlyoffice.key
chmod 644 /docker/nginx/cert/dhparam.pem

# OnlyOffice 证书权限
chmod 644 /app/onlyoffice/DocumentServer/certs/onlyoffice.crt
chmod 600 /app/onlyoffice/DocumentServer/certs/onlyoffice.key
chmod 644 /app/onlyoffice/DocumentServer/certs/onlyoffice.csr
chmod 644 /app/onlyoffice/DocumentServer/certs/dhparam.pem
```

### 第3步：配置 docker-compose.yml

编辑 `/docker/server1/docker-compose.yml`，在 `docServer` 服务的 `volumes` 下添加：

```yaml
docServer:
  image: onlyoffice/documentserver:8.3.3
  container_name: docServer
  volumes:
    - /app/onlyoffice/DocumentServer/certs:/var/www/onlyoffice/Data/certs  # 添加这一行
    - /app/onlyoffice/DocumentServer/logs:/var/log/onlyoffice
    - /app/onlyoffice/DocumentServer/data:/var/www/onlyoffice/Data
    # ... 其他配置保持不变
```

### 第4步：配置 Nginx

备份并替换 Nginx 配置：

```bash
# 备份原配置
cp /docker/nginx/conf/nginx.conf /docker/nginx/conf/nginx.conf.bak

# 使用新配置（从项目获取）
scp D:/git/zhaoxin-contract-tool-set/docs/证书配置/nginx.conf root@192.168.0.100:/docker/nginx/conf/nginx.conf
```

**nginx.conf 关键配置说明**：
- HTTP (80) 自动重定向到 HTTPS (443)
- 主站 HTTPS (443) 使用 `onlyoffice.crt/key`
- OnlyOffice HTTPS (8082) 使用 `onlyoffice.crt/key`

### 第5步：修改 application.yml

编辑 `/docker/server1/application.yml`，修改 OnlyOffice 配置：

**修改前（HTTP）**：
```yaml
zxcm:
  application:
    base-url: http://192.168.0.100:80
  
  onlyoffice:
    domain: http://192.168.0.100
    port: 8082
    plugins:
      - http://192.168.0.100:8082/plugin/html/config.json
      - http://192.168.0.100:8082/plugin/comment/config.json
```

**修改后（HTTPS）**：
```yaml
zxcm:
  application:
    base-url: https://192.168.0.100:443
  
  onlyoffice:
    domain: https://192.168.0.100
    port: 8082
    plugins:
      - https://192.168.0.100:8082/plugin/html/config.json
      - https://192.168.0.100:8082/plugin/comment/config.json
```

### 第6步：重启服务

```bash
# 测试 Nginx 配置
docker exec nginx-web nginx -t

# 重启 OnlyOffice（应用证书配置）
docker-compose stop docServer
docker-compose rm -f docServer
docker-compose up -d docServer

# 重启 Nginx
docker exec nginx-web nginx -s reload

# 重启应用（应用 application.yml 变更）
docker restart zxcm-tool
```

### 第7步：验证部署

```bash
# 1. 检查 Nginx 端口
netstat -tlnp | grep nginx
# 应该看到：443 和 8082 端口监听

# 2. 测试 HTTPS 访问
curl -k https://192.168.0.100
curl -k https://192.168.0.100:8082

# 3. 检查服务状态
docker ps | grep -E "nginx-web|docServer|zxcm-tool"
```

### 第8步：浏览器测试

访问以下地址：
- 主站：`https://192.168.0.100`
- OnlyOffice：`https://192.168.0.100:8082`

**预期结果**：
- ⚠️ 浏览器显示"不安全"警告（自签名证书正常现象）
- 点击"高级" → "继续访问"
- 系统功能正常使用

---

