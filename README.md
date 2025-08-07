# 肇新合同工具集

## 项目简介

肇新合同工具集是一个基于 AI 的智能合同管理解决方案，提供合同信息提取、履约任务智能识别等创新功能。

## 主要功能

### 1. 合同信息提取
- 支持多种文件格式（PDF、Word、Excel、图片）
- 基于通义千问 Long 模型的智能信息提取
- 可自定义提取提示词
- 结构化输出合同关键信息

### 2. 合同履约任务智能识别 (NEW!)
- AI 驱动的履约任务自动识别
- 可配置的模板管理系统
- 支持系统预置和用户自定义模板
- 动态关键词过滤
- 结果表格导出
- 历史记录管理

## 技术栈

### 后端
- Spring Boot
- MyBatis-Plus
- 通义千问 Long AI 模型
- Hutool 工具库

### 前端
- Vue 3
- TypeScript
- Element Plus
- ExcelJS

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Maven 3.8+
- MySQL 8.0+

### 后端配置
1. 克隆仓库
2. 配置 `application.yml`
3. 执行数据库初始化脚本
4. 启动 Spring Boot 应用

### 前端配置
1. 进入 `frontend` 目录
2. 运行 `npm install`
3. 运行 `npm run dev`

## 功能演示

### 合同信息提取
![合同信息提取](docs/contract-extract-demo.gif)

### 履约任务智能识别
![履约任务识别](docs/fulfillment-task-demo.gif)

## 贡献指南

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交代码变更 (`git commit -m '添加了令人惊叹的功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 `LICENSE` 文件。

## 联系方式

项目负责人：[您的名字]
电子邮件：[您的邮箱]

## 更新日志

### 2025-08-07
- 新增合同履约任务智能识别功能
- 优化合同信息提取系统
- 改进 AI 模型集成

## 致谢

感谢以下开源项目和工具的支持：
- Spring Boot
- Vue.js
- Element Plus
- 通义千问 AI 模型 