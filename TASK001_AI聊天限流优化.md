# TASK001: AI聊天限流配置优化

## 任务状态
- **任务名称**: AI聊天限流配置优化
- **任务编号**: TASK001
- **版本**: v1.0.0
- **状态**: 开发完成，等待测试验证

## 问题描述
AI助手无法正常对话，日志显示"请求过于频繁，请稍后再试"错误。

## 根本原因
限流配置过于严格：
- 用户限制：3秒内只能发送1条消息
- 时间窗口：3秒
- 导致正常对话被误判为频繁请求

## 解决方案
调整限流参数：
- 用户限流时间：3秒 → 10秒
- 用户限流次数：1次 → 3次
- 全局限流保持不变：1分钟内500次

## 修改内容

### 修改的文件
`backend/src/main/resources/aicomponent/application-ai.yml`

### 修改前后对比
```yaml
# 修改前
user-limit-count: 1
user-limit-time: 3s

# 修改后
user-limit-count: 3
user-limit-time: 10s
```

## 执行步骤

### 1. 重启后端服务
```bash
# 停止当前运行的服务
# 重新启动后端服务
cd backend
mvn spring-boot:run
```

### 2. 验证配置生效
检查启动日志，确认没有配置错误。

### 3. 测试AI聊天功能
1. 打开前端页面
2. 点击AI助手按钮
3. 连续发送3条消息（10秒内）
4. 验证消息发送成功
5. 尝试发送第4条消息，应该被限流
6. 等待10秒后再次发送，应该成功

### 4. 测试场景
- [ ] 正常对话测试
- [ ] 连续快速发送测试
- [ ] 限流触发测试
- [ ] 会话管理测试
- [ ] 错误提示测试

## 验收标准
- [x] 配置修改完成
- [ ] 重启服务后配置生效
- [ ] 连续发送3条消息（10秒内）成功
- [ ] 第4条消息（10秒内）被限流
- [ ] 10秒后发送消息成功
- [ ] 错误提示信息正确
- [ ] 会话管理功能正常

## 注意事项
1. **必须重启服务**：配置修改需要重启后端服务才能生效
2. **监控日志**：观察启动日志和运行日志，确保没有错误
3. **测试覆盖**：确保测试各种使用场景
4. **性能监控**：观察系统资源使用情况

## 回滚方案
如果修改后出现问题，可以恢复原配置：
```yaml
user-limit-count: 1
user-limit-time: 3s
```

## 后续优化建议
1. 添加动态限流配置功能
2. 实现基于用户等级的差异化限流
3. 添加限流统计和监控
4. 优化限流提示信息

## 相关文件
- `backend/src/main/java/com/zhaoxinms/contract/tools/aicomponent/controller/AiChatController.java`
- `backend/src/main/java/com/zhaoxinms/contract/tools/aicomponent/util/AiLimitUtil.java`
- `frontend/src/components/ai/AiChat.vue`
- `frontend/src/api/ai/index.ts`
