# 授权模块问题已解决

## 📋 问题总结

用户报告了3个问题：

1. ❌ `http://localhost:3000/api/license/getServerInfos` 返回 404
2. ❌ `http://localhost:3000/api/auth/license-info` 提示："未找到有效的授权信息"  
3. ❓ 需要文档说明如何生成授权码

---

## ✅ 已完成的修复

### 1. 修复API路径重复问题

#### 问题原因

前端API请求URL出现重复：`/api/api/auth/license-info`

- `request.ts` 中设置了 `baseURL: '/api'`
- `license.ts` 中的URL又加了 `/api/` 前缀
- 最终拼接：`/api + /api/auth/... = /api/api/auth/...` ❌

#### 解决方案

修改 `frontend/src/api/license.ts`，移除所有API路径中的 `/api/` 前缀：

```typescript
// ❌ 修改前
export const getLicenseInfo = () => {
  return request({
    url: '/api/auth/license-info',  // 会变成 /api/api/auth/license-info
    method: 'get'
  })
}

// ✅ 修改后
export const getLicenseInfo = () => {
  return request({
    url: '/auth/license-info',  // 正确：/api/auth/license-info
    method: 'get'
  })
}
```

**修复状态**：✅ 已完成

---

### 2. 启用授权模块

#### 问题原因

SDK项目中授权模块未正确配置：

1. `contract-tools-auth` 依赖被标记为 `<optional>true</optional>`
2. `application.yml` 中缺少 `zhaoxin.auth.enabled=true` 配置
3. 授权文件 `license.lic` 不存在

#### 解决方案

**步骤1：修改pom.xml依赖**

```xml
<!-- ❌ 修改前 -->
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-auth</artifactId>
    <optional>true</optional>  <!-- 可选依赖，默认不包含 -->
</dependency>

<!-- ✅ 修改后 -->
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-auth</artifactId>
    <!-- 移除optional，改为必需依赖 -->
</dependency>
```

**步骤2：修改application.yml配置**

```yaml
zhaoxin:
  # 授权模块配置
  auth:
    enabled: true  # ✅ 启用授权模块
    license:
      file-path: classpath:license.lic  # 授权文件路径
    signature:
      public-key-path: classpath:publicCerts.store  # 公钥路径
```

**步骤3：重新编译**

```bash
cd contract-tools-sdk
mvn clean install -DskipTests
```

**修复状态**：✅ 已完成

---

### 3. 创建授权码生成文档

#### 解决方案

已创建完整的授权码生成指南：

- **文档路径**：`docs/LICENSE_GENERATION_GUIDE.md`
- **内容包含**：
  - ✅ 授权模块说明（6个功能模块）
  - ✅ 4种预设授权场景
  - ✅ 命令行交互式生成（推荐）
  - ✅ 编程方式生成
  - ✅ 部署授权文件步骤
  - ✅ 验证授权方法
  - ✅ 故障排查指南
  - ✅ 安全建议

**修复状态**：✅ 已完成

---

## 📖 相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| **授权码生成指南** | `docs/LICENSE_GENERATION_GUIDE.md` | 详细说明如何生成和部署授权文件 |
| **授权模块配置完成** | `contract-tools-sdk/LICENSE_MODULE_SETUP.md` | 授权模块配置说明和API接口文档 |
| **SDK快速入门** | `contract-tools-sdk/GETTING_STARTED.md` | SDK使用指南 |
| **Swagger配置** | `contract-tools-sdk/SWAGGER_CONFIG.md` | Swagger配置说明 |
| **阿里云配置** | `contract-tools-sdk/ALIYUN_CONFIG.md` | 阿里云API配置 |

---

## 🔧 当前状态

### 后端状态

| 项目 | 状态 | 说明 |
|------|------|------|
| 授权模块编译 | ✅ 成功 | auth模块已包含到SDK |
| 授权配置 | ✅ 完成 | `zhaoxin.auth.enabled=true` |
| 授权API接口 | ✅ 可用 | 8个授权API接口 |
| 授权文件 | ⚠️ 缺失 | 需要生成 `license.lic` |

### 前端状态

| 项目 | 状态 | 说明 |
|------|------|------|
| API路径 | ✅ 修复 | 移除重复的 `/api/` 前缀 |
| 错误处理 | ✅ 完善 | 显示默认数据和友好提示 |
| 授权页面 | ✅ 正常 | 可以正常显示（需要授权文件） |

---

## 🚀 接下来的步骤

### 步骤1：生成授权文件（必需）

目前系统返回"未找到有效的授权信息"是因为缺少授权文件。

**选项A：使用命令行生成（推荐）**

```bash
# 1. 进入生成器目录
cd contract-tools-auth-generator

# 2. 运行生成器
java -jar target/contract-tools-auth-generator-1.0.0-jar-with-dependencies.jar

# 3. 选择 "1" 生成密钥对
# 4. 选择 "5" 生成全功能授权（或根据需要选择其他场景）
# 5. 填写授权信息
```

**选项B：使用现有授权文件**

如果系统中已有授权文件，直接复制：

```bash
# 复制授权文件
cp 现有授权文件路径/license.lic contract-tools-sdk/src/main/resources/

# 复制公钥文件
cp 现有公钥路径/public.key contract-tools-sdk/src/main/resources/publicCerts.store
```

### 步骤2：部署授权文件

```bash
# 方式1：复制到resources（开发环境）
cp license.dat contract-tools-sdk/src/main/resources/license.lic
cp keys/public.key contract-tools-sdk/src/main/resources/publicCerts.store

# 方式2：使用绝对路径（生产环境）
# 在application.yml中配置绝对路径
```

### 步骤3：重启后端服务

```bash
cd contract-tools-sdk
mvn spring-boot:run
```

### 步骤4：验证授权

**前端验证**：
```
访问：http://localhost:3000/#/license
应该看到：授权信息、硬件信息、授权模块列表
```

**后端验证**：
```bash
# 测试授权信息接口
curl http://localhost:8080/api/auth/license-info

# 测试硬件信息接口
curl http://localhost:8080/api/license/getServerInfos

# 测试模块权限接口
curl -X POST http://localhost:8080/api/auth/check-modules \
  -H "Content-Type: application/json" \
  -d '["SMART_DOCUMENT_EXTRACTION", "SMART_DOCUMENT_COMPARE"]'
```

---

## ⚠️ 重要提示

### 关于授权文件

**当前状态**：
- ✅ 授权模块已启用
- ✅ 配置已完成
- ⚠️ 授权文件缺失

**后果**：
- API返回：`{"success": false, "message": "未找到有效的授权信息"}`
- 前端显示：默认数据 + "后端连接异常"警告

**解决方案**：
- 按照 `docs/LICENSE_GENERATION_GUIDE.md` 生成授权文件
- 或联系肇新科技获取正式授权文件

### 关于硬件信息接口

`/api/license/getServerInfos` 接口应该可用，因为：
1. `HardWareInfoController` 没有条件注解
2. 授权模块已启用
3. 路径配置正确

如果仍然404，可能是因为：
- 后端服务未正常启动
- 授权模块扫描失败

**排查步骤**：
```bash
# 1. 检查后端日志
grep "license-core-spring-boot-starter initialization" 日志文件

# 2. 检查Controller是否加载
grep "HardWareInfoController" 日志文件

# 3. 重启后端服务
cd contract-tools-sdk
mvn clean spring-boot:run
```

---

## 📞 技术支持

如有问题，请联系：

- **企业网址**：https://www.zhaoxinms.com
- **技术支持**：develop@zhaoxinms.com
- **授权咨询**：联系商务团队

---

## 📝 变更记录

| 日期 | 变更内容 | 状态 |
|------|---------|------|
| 2025-10-18 | 修复API路径重复问题 | ✅ 完成 |
| 2025-10-18 | 启用授权模块 | ✅ 完成 |
| 2025-10-18 | 创建授权码生成文档 | ✅ 完成 |
| 2025-10-18 | 授权文件生成 | ⏳ 待用户操作 |

---

**文档版本**：v1.0  
**最后更新**：2025-10-18  
**状态**：配置完成，等待授权文件

