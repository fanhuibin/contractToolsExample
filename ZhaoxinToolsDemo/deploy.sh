#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 部署入口"
echo "========================================"
echo

echo "请选择部署方式:"
echo "  1. Nginx 生产部署 (推荐)"
echo "  2. Docker 容器部署"
echo "  3. 查看部署文档"
echo

read -p "请输入选择 (1-3): " choice

case $choice in
    1)
        echo
        echo "[INFO] 执行 Nginx 部署..."
        if [ ! -d "dist" ]; then
            echo "[ERROR] 请先执行构建: ./build.sh"
            exit 1
        fi
        cd dist/nginx
        chmod +x deploy-nginx.sh
        sudo ./deploy-nginx.sh
        ;;
    2)
        echo
        echo "[INFO] Docker 部署功能开发中..."
        echo "请查看: build-tools/deployment/docker/"
        ;;
    3)
        echo
        echo "[INFO] 查看部署文档..."
        if command -v code &> /dev/null; then
            code build-tools/docs/DEPLOY_SUMMARY.md
        else
            cat build-tools/docs/DEPLOY_SUMMARY.md
        fi
        ;;
    *)
        echo "[ERROR] 无效选择"
        exit 1
        ;;
esac
