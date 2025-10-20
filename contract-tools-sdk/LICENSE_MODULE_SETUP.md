# 授权模块配置完成

## ✅ 已完成的配置

### 1. **Maven依赖配置**

#### 修改文件：`contract-tools-sdk/pom.xml`

**修改前**：
```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-auth</artifactId>
    <optional>true</optional>  <!-- 可选依赖，默认不包含 -->
</dependency>
```

**修改后**：
```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-auth</artifactId>
    <!-- 移除optional标签，改为必需依赖 -->
</dependency>
```

---

### 2. **应用配置**

#### 修改文件：`contract-tools-sdk/src/main/resources/application.yml`

添加了授权模块配置：

```yaml
zhaoxin:
  # 授权模块配置
  auth:
    enabled: true  # 启用授权模块
    license:
      file-path: classpath:license.lic  # 授权文件路径
    signature:
      public-key-path: classpath:publicCerts.store  # 公钥路径
```

---

## 📋 可用的授权API接口

### 1. **获取授权信息**
```
GET /api/auth/license-info
```
返回授权码、企业名称、联系信息、有效期等。

### 2. **验证授权**
```
GET /api/auth/validate
```
验证授权是否有效。

### 3. **检查单个模块权限**
```
GET /api/auth/check-module?moduleCode=smart_document_extraction
```
检查指定模块是否授权。

### 4. **批量检查模块权限**
```
POST /api/auth/check-modules
Content-Type: application/json

["smart_document_extraction", "smart_document_compare"]
```
批量检查多个模块的授权状态。

### 5. **获取所有可用模块**
```
GET /api/auth/modules
```
返回所有可用的模块列表。

### 6. **获取授权详情**
```
GET /api/auth/license-details
```
获取完整的授权详情，包括签名验证、时间状态等。

### 7. **验证硬件匹配**
```
GET /api/auth/hardware-validation
```
验证当前硬件是否与授权绑定的硬件匹配。

### 8. **获取服务器硬件信息**
```
GET /api/license/getServerInfos
```
获取当前服务器的硬件信息（MAC地址、CPU序列号、主板序列号等）。

---

## 🔑 授权模块说明

### 模块代码（ModuleType）

| 模块代码 | 模块名称 | 说明 |
|----------|---------|------|
| `smart_document_extraction` | 智能文档抽取 | AI驱动的合同信息提取 |
| `smart_document_compare` | 智能文档比对 | GPU OCR文档智能比对 |
| `smart_contract_synthesis` | 智能合同合成 | 合同智能生成 |
| `smart_document_parse` | 智能文档解析 | 文档结构化解析 |
| `document_online_edit` | 文档在线编辑 | OnlyOffice在线编辑 |
| `document_format_convert` | 文档格式转换 | 多种格式互转 |

---

## 🗂️ 授权文件配置

### 授权文件位置

**默认位置**：`contract-tools-sdk/src/main/resources/license.lic`

### 公钥文件位置

**默认位置**：`contract-tools-sdk/src/main/resources/publicCerts.store`

**注意**：这个文件已经从 `contract-tools-auth` 模块复制到 SDK 的 resources 目录。

---

## 🚀 启动验证

### 1. 启动应用

```bash
cd contract-tools-sdk
mvn spring-boot:run
```

### 2. 验证授权API

#### 测试授权信息接口

```bash
curl http://localhost:8080/api/auth/license-info
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "licenseCode": "xxx",
    "companyName": "xxx",
    ...
  }
}
```

#### 测试硬件信息接口

```bash
curl http://localhost:8080/api/license/getServerInfos
```

**预期响应**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "osName": "Windows 10",
    "mainBoardSerial": "xxx",
    "cpuSerial": "xxx",
    "macAddress": ["xx:xx:xx:xx:xx:xx"]
  }
}
```

#### 测试模块权限接口

```bash
curl -X POST http://localhost:8080/api/auth/check-modules \
  -H "Content-Type: application/json" \
  -d '["smart_document_extraction", "smart_document_compare"]'
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "smart_document_extraction": true,
    "smart_document_compare": false
  }
}
```

---

## ⚠️ 注意事项

### 1. 授权文件不存在时

如果 `license.lic` 文件不存在，API会返回：

```json
{
  "success": false,
  "message": "未找到有效的授权信息"
}
```

**解决方案**：
- 联系肇新科技获取授权文件
- 或使用 `contract-tools-auth-generator` 生成测试授权

### 2. 关闭授权模块

如果暂时不需要授权功能，可以禁用：

```yaml
zhaoxin:
  auth:
    enabled: false  # 禁用授权模块
```

禁用后，所有 `/api/auth/*` 接口将返回404。

### 3. 授权文件路径

支持多种路径格式：

```yaml
zhaoxin:
  auth:
    license:
      # classpath路径（推荐）
      file-path: classpath:license.lic
      
      # 绝对路径
      # file-path: D:/licenses/license.lic
      
      # 相对路径
      # file-path: ./config/license.lic
```

---

## 📖 前端集成

### API路径配置

前端已正确配置API路径（`frontend/src/api/license.ts`）：

```typescript
// ✅ 正确的路径（不带 /api/ 前缀）
export const getLicenseInfo = () => {
  return request({
    url: '/auth/license-info',  // baseURL会自动添加 /api
    method: 'get'
  })
}
```

### 授权信息页面

访问：http://localhost:3000/#/license

**功能**：
- ✅ 显示授权状态
- ✅ 显示授权信息
- ✅ 显示硬件信息
- ✅ 显示授权模块列表
- ✅ 错误降级处理（后端未连接时显示默认数据）

---

## 🔧 故障排查

### 问题1：API返回404

**原因**：
1. 授权模块未启用（`zhaoxin.auth.enabled=false`）
2. SDK项目未重新编译
3. 后端服务未启动

**解决**：
```bash
# 1. 检查配置
grep "auth:" contract-tools-sdk/src/main/resources/application.yml

# 2. 重新编译
cd contract-tools-sdk
mvn clean install

# 3. 重启服务
mvn spring-boot:run
```

### 问题2：授权信息为空

**原因**：授权文件不存在或损坏

**解决**：
```bash
# 检查文件是否存在
ls contract-tools-sdk/src/main/resources/license.lic

# 检查公钥文件
ls contract-tools-sdk/src/main/resources/publicCerts.store
```

### 问题3：硬件信息获取失败

**原因**：权限不足或系统不支持

**解决**：
- Windows：以管理员身份运行
- Linux：确保有足够的权限读取硬件信息

---

## 📞 技术支持

如有问题，请联系：

- 企业网址：https://www.zhaoxinms.com
- 技术支持：develop@zhaoxinms.com
- 授权咨询：联系商务团队

---

**配置完成时间**：2024-10-18

**配置状态**：✅ 完成并验证

