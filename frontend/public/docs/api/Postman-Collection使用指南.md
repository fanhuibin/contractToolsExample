# API Collection 使用指南

## 📦 关于 API Collection

本文档提供的是标准 Postman Collection 格式的 API 接口集合文件，**兼容 Apifox、Postman 等主流 API 工具**。

> 💡 **国内用户推荐使用 [Apifox](https://apifox.com)**  
> Apifox 是国内优秀的 API 开发工具，完全兼容 Postman Collection 格式，且在国内访问稳定、功能强大。

---

## 🎯 包含的接口

本 Collection 包含以下模块的所有接口：

- ✅ **文档格式转换**（2个接口）
- ✅ **智能文档比对**（12个接口）
- ✅ **智能文档抽取**（6个接口）
- ✅ **智能文档解析**（4个接口）
- ✅ **智能合同合成**（6个接口）

**共计 30+ 个 API 接口**

---

## 📥 下载文件

点击下方按钮下载 Collection 文件：

<div style="text-align: center; margin: 30px 0;">
  <a href="/docs/api/肇新合同组件库-Postman-Collection.json" download="肇新合同组件库-Postman-Collection.json" style="display: inline-block; padding: 12px 24px; background: #409eff; color: white; text-decoration: none; border-radius: 4px; font-weight: 600;">
    ⬇️ 下载 API Collection 文件
  </a>
</div>

---

## 🚀 使用方式（推荐 Apifox）

### 方式一：使用 Apifox（推荐）

#### 1. 下载并安装 Apifox

访问 [Apifox 官网](https://apifox.com) 下载客户端，或使用 [Web 版](https://app.apifox.com)。

#### 2. 导入 Collection

1. 打开 Apifox，进入项目
2. 点击 **项目设置** → **导入数据**
3. 选择 **Postman** → 上传下载的 JSON 文件
4. 在导入预览页查看接口列表
5. 勾选需要导入的环境（如有）
6. 点击 **确定导入**

![Apifox 导入示例](https://docs.apifox.com/assets/import-postman-preview.png)

> 📖 详细教程参考：[Apifox 导入 Postman 文档](https://docs.apifox.com/import-postman)

#### 3. 配置环境

导入后，Apifox 会自动识别 Collection 中的变量：

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `base_url` | API 基础地址 | `http://localhost:8080` 或 `https://your-domain.com` |

**配置方法**：
1. 打开 **环境管理**
2. 选择或新建环境（如 "开发环境"）
3. 设置 `base_url` 变量值
4. 保存并切换到该环境

#### 4. 开始测试

1. 在左侧接口列表选择要测试的接口
2. 选择对应的环境
3. 点击 **发送** 按钮
4. 查看响应结果

#### 5. Apifox 独有优势

✨ **API 设计 + 调试 + 文档 + Mock + 测试**，一体化解决方案  
✨ **自动生成 API 文档**，支持在线分享  
✨ **数据模型复用**，提高开发效率  
✨ **自动校验响应**，确保数据格式正确  
✨ **国内访问稳定**，无需科学上网

---

### 方式二：使用 Postman（备选）

> ⚠️ **注意**：Postman 在国内访问可能不稳定，推荐使用 Apifox。

#### 1. 导入 Postman

1. 打开 Postman 应用
2. 点击左上角的 **Import** 按钮
3. 选择 **File** 标签
4. 选择下载的 JSON 文件
5. 点击 **Import** 完成导入

#### 2. 配置环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `base_url` | API 基础地址 | `http://localhost:8080` |

**配置方法**：
1. 点击右上角的 **Environments**
2. 创建新环境（如 "开发环境"）
3. 添加 `base_url` 变量
4. 填写对应的值
5. 保存并选择该环境

#### 3. 开始测试

1. 展开左侧的 Collection
2. 选择要测试的接口
3. 点击 **Send** 发送请求
4. 查看响应结果

---

## 💡 使用技巧

### 1. 批量测试

**Apifox**：
1. 新建 **测试场景**
2. 添加多个接口到场景
3. 配置测试步骤
4. 点击 **运行** 批量测试

**Postman**：
1. 点击 Collection 右侧的 **...** 按钮
2. 选择 **Run collection**
3. 点击 **Run** 开始批量测试

### 2. 环境切换

可以创建多个环境应对不同部署场景：

| 环境名称 | base_url 示例 | 用途 |
|---------|--------------|------|
| 开发环境 | `http://localhost:8080` | 本地开发调试 |
| 测试环境 | `http://test.your-domain.com` | 测试环境验证 |
| 生产环境 | `https://your-domain.com` | 生产环境测试 |

### 3. 保存示例响应

成功调用接口后，建议保存响应作为示例：

**Apifox**：自动保存响应到接口文档  
**Postman**：点击 **Save Response** → **Save as example**

### 4. 共享给团队

**Apifox**：
1. 在线分享 API 文档（无需导出）
2. 邀请团队成员加入项目
3. 实时协作，自动同步

**Postman**：
1. 右键点击 Collection → **Export**
2. 选择格式（推荐 v2.1）
3. 分享导出的 JSON 文件

---

## 📚 相关资源

- [Apifox 官网](https://apifox.com)
- [Apifox 导入 Postman 教程](https://docs.apifox.com/import-postman)
- [Apifox 帮助文档](https://docs.apifox.com)
- [API 设计规范](./API设计文档.md)

---

## ❓ 常见问题

**Q: Apifox 和 Postman 有什么区别？**  
A: Apifox 是一体化 API 工具，集成了设计、调试、文档、Mock、测试等功能；Postman 主要专注于 API 调试。Apifox 更适合国内团队使用。

**Q: Collection 文件可以同时导入到 Apifox 和 Postman 吗？**  
A: 可以。本 Collection 采用标准 Postman v2.1 格式，兼容两个工具。

**Q: 导入后找不到接口？**  
A: 请检查导入预览页是否正确显示接口列表，确认勾选了对应的 Collection。

**Q: base_url 变量如何配置？**  
A: 在环境管理中新建或编辑环境，添加 `base_url` 变量并设置为你的 API 服务地址（如 `http://localhost:8080`）。
