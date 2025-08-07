# 肇新合同工具集 (Zhaoxin Contract Tool Set)

## 项目简介
这是一个集成了OnlyOffice、模板设计、文档合成、合同比对等功能的合同管理工具集。

## 技术栈
- **前端**: Vue 3 + TypeScript + Vite
- **后端**: Spring Boot 2.7.x + Java 11
- **存储**: 文件系统存储
- **文档处理**: OnlyOffice

## 项目结构
```
zhaoxin-contract-tool-set/
├── frontend/                 # Vue3前端项目
├── backend/                  # Spring Boot后端项目
├── docs/                     # 项目文档
└── README.md                 # 项目说明
```

## 后端包结构
```
com.zhaoxinms.contract.tools/
├── onlyoffice/              # OnlyOffice集成包
├── template/                # 模板设计包
├── document/                # 文档合成包
├── comparison/              # 合同比对包
└── common/                  # 通用类包
```

## 快速开始

### 后端启动
```bash
cd backend
./mvnw spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 功能特性
- [x] 文件管理功能（上传、下载、删除）
- [x] 文件系统存储（无数据库依赖）
- [x] 雪花ID生成
- [x] 并发安全处理
- [x] OnlyOffice集成包（包结构修正）
- [x] JWT依赖问题修复
- [x] OnlyOffice权限配置化
- [x] OnlyOffice回调处理优化
- [ ] OnlyOffice文档编辑
- [ ] 模板设计功能
- [ ] 文档合成功能
- [ ] 合同比对功能

## 开发环境要求
- JDK 11+
- Node.js 16+ 