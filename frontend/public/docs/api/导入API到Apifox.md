# 导入 API 到 Apifox

## 📚 概述

本文档介绍如何将肇新合同工具集的 API 接口导入到 Apifox 进行管理和测试。

## 🎯 为什么选择 Apifox？

Apifox 是一款国产 API 管理工具，具有以下优势：

- ✅ **完全兼容 Postman Collection 格式**
- ✅ **国内访问速度快，无需科学上网**
- ✅ **支持 API 文档、测试、Mock 一体化**
- ✅ **界面友好，中文支持完善**
- ✅ **团队协作功能强大**

## 📦 获取 API Collection 文件

### 方式1：从系统下载

1. 点击左侧菜单"其他文档" → "API Collection 使用指南"
2. 点击页面中的 **"下载 Collection JSON"** 按钮
3. 保存文件：`肇新合同工具集-API-Collection.json`

## 📥 导入到 Apifox

### 步骤1：打开 Apifox

1. 访问 Apifox 官网：https://apifox.com
2. 注册/登录账号
3. 创建新项目或打开现有项目

### 步骤2：导入 Collection

1. 点击左上角 **"导入"** 按钮
2. 选择 **"Postman"** 格式
3. 选择 **"导入文件"**
4. 上传刚才下载的 JSON 文件
5. 点击 **"确认导入"**

### 步骤3：配置环境变量

导入后，需要配置环境变量 `base_url`：

1. 点击右上角 **"环境"** 按钮
2. 选择或创建环境（如"开发环境"）
3. 添加变量：
   - **变量名**：`base_url`
   - **变量值**：`http://your-domain:port`（如 `http://localhost:80`）
4. 保存并切换到该环境

## 📞 技术支持

如需了解更多功能或有定制需求，请联系：

- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)
- 📚 Apifox 官方文档：[https://apifox.com/help](https://apifox.com/help)

## 🔗 相关文档

- [API Collection 使用指南](./Postman-Collection使用指南.md)
- [API 设计规范](./API设计文档.md)
- [文档格式转换 API](./文档格式转换-API文档.md)

