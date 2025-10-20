# 阿里云API配置说明

## 概述

合同提取功能使用阿里云通义千问大模型进行智能文本分析和信息提取。使用前需要配置阿里云API密钥。

## 获取API密钥

### 步骤1：注册阿里云账号

如果还没有阿里云账号，请先注册：
- 访问：https://www.aliyun.com/
- 点击"免费注册"
- 完成注册流程

### 步骤2：开通DashScope服务

1. 访问阿里云灵积平台（DashScope）：https://dashscope.console.aliyun.com/
2. 登录您的阿里云账号
3. 同意服务协议并开通服务

### 步骤3：获取API Key

1. 访问API密钥管理页面：https://dashscope.console.aliyun.com/apiKey
2. 点击"创建API Key"
3. 复制生成的API Key（请妥善保管，只显示一次）

## 配置方法

### 方法1：修改配置文件（推荐用于开发环境）

编辑 `contract-tools-sdk/src/main/resources/application.yml`：

```yaml
zhaoxin:
  extract:
    aliyun:
      api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # 替换为您的API Key
      model: qwen-plus  # 可选：qwen-turbo, qwen-plus, qwen-max
```

### 方法2：使用环境变量（推荐用于生产环境）

#### Linux/Mac

```bash
export ZHAOXIN_EXTRACT_ALIYUN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export ZHAOXIN_EXTRACT_ALIYUN_MODEL=qwen-plus
```

#### Windows (PowerShell)

```powershell
$env:ZHAOXIN_EXTRACT_ALIYUN_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:ZHAOXIN_EXTRACT_ALIYUN_MODEL="qwen-plus"
```

#### Windows (CMD)

```cmd
set ZHAOXIN_EXTRACT_ALIYUN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
set ZHAOXIN_EXTRACT_ALIYUN_MODEL=qwen-plus
```

### 方法3：启动参数

```bash
java -jar contract-tools-sdk.jar \
  --zhaoxin.extract.aliyun.api-key=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx \
  --zhaoxin.extract.aliyun.model=qwen-plus
```

## 模型说明

阿里云通义千问提供多个模型供选择：

| 模型 | 特点 | 适用场景 |
|-----|------|---------|
| **qwen-turbo** | 响应快，成本低 | 简单文本提取、批量处理 |
| **qwen-plus** | 平衡性能和成本（推荐） | 通用合同提取、信息识别 |
| **qwen-max** | 最强性能，理解能力最佳 | 复杂合同、精确分析 |

默认使用 `qwen-plus`，可根据实际需求调整。

## 费用说明

### 定价（参考）

- **qwen-turbo**: 约 ¥0.003/千tokens
- **qwen-plus**: 约 ¥0.006/千tokens
- **qwen-max**: 约 ¥0.02/千tokens

> 具体价格以阿里云官网为准：https://help.aliyun.com/zh/dashscope/developer-reference/tongyi-qianwen-metering-and-billing

### 新用户福利

阿里云通常为新用户提供免费额度：
- 免费调用次数
- 赠送token额度

详情请查看：https://dashscope.console.aliyun.com/

## 验证配置

配置完成后，启动应用验证：

```bash
cd contract-tools-sdk
mvn spring-boot:run
```

如果配置正确，应用将正常启动，日志中不会出现API Key相关错误。

## 测试API连接

使用合同提取功能测试：

```bash
curl -X POST http://localhost:8080/api/contract/extract/upload \
  -F "file=@test.pdf" \
  -F "ignoreHeaderFooter=true"
```

如果返回任务ID，说明配置成功。

## 常见问题

### Q1: 提示 "Could not resolve placeholder 'zhaoxin.extract.aliyun.api-key'"

**A**: API Key未配置或配置错误，请检查：
1. 配置文件中是否正确填写了API Key
2. 配置文件格式是否正确（注意缩进）
3. 环境变量是否正确设置

### Q2: 提示 "Invalid API Key"

**A**: API Key无效，请检查：
1. API Key是否完整复制（通常以`sk-`开头）
2. API Key是否已过期或被删除
3. 在阿里云控制台重新生成新的API Key

### Q3: 提示 "Insufficient balance" 或 "Quota exceeded"

**A**: 账户余额不足或配额用尽：
1. 登录阿里云控制台检查余额
2. 充值或等待配额刷新
3. 考虑使用更经济的模型（如qwen-turbo）

### Q4: 如何更换模型？

**A**: 修改配置文件中的 `model` 参数：

```yaml
zhaoxin:
  extract:
    aliyun:
      model: qwen-max  # 更换为其他模型
```

### Q5: 生产环境如何保护API Key安全？

**A**: 建议：
1. 使用环境变量，不要将API Key写入配置文件
2. 配置文件中使用占位符
3. 使用密钥管理服务（如阿里云KMS）
4. 定期轮换API Key
5. 限制API Key的访问权限

## 监控和日志

### 查看API调用日志

应用日志会记录API调用情况：

```
2024-10-18 16:30:15 [INFO] - 调用阿里云API，模型: qwen-plus
2024-10-18 16:30:16 [INFO] - API响应成功，耗时: 1.2s
```

### 监控配额使用

访问阿里云控制台查看：
- API调用次数
- Token消耗量
- 费用统计

控制台地址：https://dashscope.console.aliyun.com/

## 技术支持

### 阿里云官方文档

- DashScope文档：https://help.aliyun.com/zh/dashscope/
- API参考：https://help.aliyun.com/zh/dashscope/developer-reference/api-details

### 肇新科技支持

- 企业网址：https://www.zhaoxinms.com
- 技术支持：develop@zhaoxinms.com

---

## 快速配置检查清单

- [ ] 已注册阿里云账号
- [ ] 已开通DashScope服务
- [ ] 已创建API Key
- [ ] 已在配置文件中填写API Key
- [ ] 已选择合适的模型
- [ ] 已测试启动应用
- [ ] 已验证功能正常

完成以上步骤后，您就可以开始使用合同提取功能了！🎉

