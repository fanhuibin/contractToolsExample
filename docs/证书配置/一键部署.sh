#!/bin/bash
# SSL证书一键部署脚本
# 使用方法：bash 一键部署.sh 192.168.0.100

set -e

SERVER_IP=${1:-192.168.0.100}
CERT_DIR="D:/git/zhaoxin-contract-tool-set/docs/证书配置"

echo "=========================================="
echo "SSL证书一键部署脚本"
echo "目标服务器: $SERVER_IP"
echo "=========================================="
echo ""

# 第1步：上传证书文件
echo "第1步：上传证书文件..."
scp ${CERT_DIR}/onlyoffice.crt root@${SERVER_IP}:/docker/nginx/cert/
scp ${CERT_DIR}/onlyoffice.key root@${SERVER_IP}:/docker/nginx/cert/
scp ${CERT_DIR}/dhparam.pem root@${SERVER_IP}:/docker/nginx/cert/

scp ${CERT_DIR}/onlyoffice.crt root@${SERVER_IP}:/app/onlyoffice/DocumentServer/certs/
scp ${CERT_DIR}/onlyoffice.key root@${SERVER_IP}:/app/onlyoffice/DocumentServer/certs/
scp ${CERT_DIR}/onlyoffice.csr root@${SERVER_IP}:/app/onlyoffice/DocumentServer/certs/
scp ${CERT_DIR}/dhparam.pem root@${SERVER_IP}:/app/onlyoffice/DocumentServer/certs/
echo "✅ 证书文件上传完成"
echo ""

# 第2步：设置权限
echo "第2步：设置文件权限..."
ssh root@${SERVER_IP} << 'EOF'
chmod 644 /docker/nginx/cert/onlyoffice.crt
chmod 600 /docker/nginx/cert/onlyoffice.key
chmod 644 /docker/nginx/cert/dhparam.pem

chmod 644 /app/onlyoffice/DocumentServer/certs/onlyoffice.crt
chmod 600 /app/onlyoffice/DocumentServer/certs/onlyoffice.key
chmod 644 /app/onlyoffice/DocumentServer/certs/onlyoffice.csr
chmod 644 /app/onlyoffice/DocumentServer/certs/dhparam.pem
EOF
echo "✅ 权限设置完成"
echo ""

# 第3步：备份并上传Nginx配置
echo "第3步：配置Nginx..."
ssh root@${SERVER_IP} "cp /docker/nginx/conf/nginx.conf /docker/nginx/conf/nginx.conf.bak"
scp ${CERT_DIR}/nginx.conf root@${SERVER_IP}:/docker/nginx/conf/nginx.conf
echo "✅ Nginx配置完成"
echo ""

# 第4步：测试Nginx配置
echo "第4步：测试Nginx配置..."
ssh root@${SERVER_IP} "docker exec nginx-web nginx -t"
echo "✅ Nginx配置测试通过"
echo ""

# 第5步：重启服务
echo "第5步：重启服务..."
echo "  - 重启OnlyOffice..."
ssh root@${SERVER_IP} << 'EOF'
cd /docker/server1
docker-compose stop docServer
docker-compose rm -f docServer
docker-compose up -d docServer
EOF

echo "  - 重载Nginx..."
ssh root@${SERVER_IP} "docker exec nginx-web nginx -s reload"

echo "  - 重启应用..."
ssh root@${SERVER_IP} "docker restart zxcm-tool"
echo "✅ 服务重启完成"
echo ""

# 第6步：验证部署
echo "第6步：验证部署..."
sleep 5
echo "  - 检查端口监听..."
ssh root@${SERVER_IP} "netstat -tlnp | grep -E ':443|:8082'" || echo "    端口检查完成"

echo "  - 测试HTTPS访问..."
curl -k -s -o /dev/null -w "    主站 (443): HTTP %{http_code}\n" https://${SERVER_IP}/
curl -k -s -o /dev/null -w "    OnlyOffice (8082): HTTP %{http_code}\n" https://${SERVER_IP}:8082/

echo ""
echo "=========================================="
echo "✅ SSL证书部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  主站: https://${SERVER_IP}"
echo "  OnlyOffice: https://${SERVER_IP}:8082"
echo ""
echo "⚠️  注意事项："
echo "  1. 浏览器会显示'不安全'警告（自签名证书正常现象）"
echo "  2. 点击'高级' → '继续访问'即可"
echo "  3. 请检查 application.yml 中 OnlyOffice URL 是否已改为 https"
echo ""

