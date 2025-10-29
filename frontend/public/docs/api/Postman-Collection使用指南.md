# Postman Collection 使用指南

## 📦 关于 Postman Collection

Postman Collection 是一个包含所有 API 接口的测试集合文件，可以直接导入到 Postman 中使用。

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

点击下方按钮下载 Postman Collection 文件：

<div style="text-align: center; margin: 30px 0;">
  <a href="/docs/api/肇新合同组件库-Postman-Collection.json" download="肇新合同组件库-Postman-Collection.json" style="display: inline-block; padding: 12px 24px; background: #409eff; color: white; text-decoration: none; border-radius: 4px; font-weight: 600;">
    ⬇️ 下载 Postman Collection
  </a>
</div>

---

## 🚀 使用步骤

### 1. 导入 Postman

1. 打开 Postman 应用
2. 点击左上角的 **Import** 按钮
3. 选择 **File** 标签
4. 选择下载的 JSON 文件
5. 点击 **Import** 完成导入

### 2. 配置环境变量

导入后需要配置环境变量：

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `base_url` | API 基础地址 | `http://localhost:8080` 或 `https://your-domain.com` |

**配置方法**：
1. 点击右上角的 **Environments**
2. 创建新环境（如 "开发环境"）
3. 添加 `base_url` 变量
4. 填写对应的值
5. 保存并选择该环境

### 3. 开始测试

1. 展开左侧的 Collection
2. 选择要测试的接口
3. 点击 **Send** 发送请求
4. 查看响应结果

---

## 💡 使用技巧

### 1. 批量测试

使用 **Collection Runner** 批量运行所有接口：
1. 点击 Collection 右侧的 **...** 按钮
2. 选择 **Run collection**
3. 配置运行参数
4. 点击 **Run** 开始批量测试

### 2. 环境切换

可以创建多个环境（开发、测试、生产）：
- 开发环境：`http://localhost:8080`
- 测试环境：`http://test.your-domain.com`
- 生产环境：`https://your-domain.com`

### 3. 保存示例响应

成功调用接口后，可以保存响应作为示例：
1. 点击 **Save Response**
2. 选择 **Save as example**
3. 为示例命名

### 4. 共享 Collection

可以导出 Collection 分享给团队成员：
1. 右键点击 Collection
2. 选择 **Export**
3. 选择格式（推荐 v2.1）
4. 保存并分享
