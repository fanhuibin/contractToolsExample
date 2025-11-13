#!/bin/bash

echo "========================================"
echo "肇新工具集 Demo 构建入口"
echo "========================================"
echo

echo "请选择构建方式:"
echo "  1. 完整构建 (推荐)"
echo "  2. 查看构建文档"
echo

read -p "请输入选择 (1-2): " choice

case $choice in
    1)
        echo
        echo "[INFO] 执行完整构建..."
        chmod +x build-tools/scripts/build.sh
        ./build-tools/scripts/build.sh
        ;;
    2)
        echo
        echo "[INFO] 查看构建文档..."
        if command -v code &> /dev/null; then
            code build-tools/docs/BUILD_GUIDE.md
        else
            cat build-tools/docs/BUILD_GUIDE.md
        fi
        ;;
    *)
        echo "[ERROR] 无效选择"
        exit 1
        ;;
esac
