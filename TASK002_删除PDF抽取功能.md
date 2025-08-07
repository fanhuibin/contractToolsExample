# TASK002: 删除PDF抽取功能

## 任务状态
- **任务名称**: 删除PDF抽取按钮和弹窗页面
- **任务编号**: TASK002
- **版本**: v1.0.0
- **状态**: 开发完成

## 任务描述
删除PDF抽取功能，包括按钮、弹窗页面和相关API，简化用户界面。

## 删除内容

### 删除的文件
1. `frontend/src/components/ai/PdfExtractor.vue` - PDF抽取组件

### 修改的文件
1. `frontend/src/layout/index.vue` - 删除PDF抽取按钮和弹窗
2. `frontend/src/api/ai/index.ts` - 删除已废弃的aiPdf API

## 具体修改内容

### 1. 删除PDF抽取组件文件
- 完全删除了 `PdfExtractor.vue` 组件文件

### 2. 修改布局文件 (layout/index.vue)
- 删除了PDF抽取弹窗对话框
- 删除了头部导航栏中的"PDF抽取"按钮
- 删除了PdfExtractor组件的导入
- 删除了showPdfExtractor状态变量

### 3. 清理API文件 (api/ai/index.ts)
- 删除了已废弃的aiPdf API模块
- 该API已被标记为deprecated，建议使用aiContract替代

## 删除前后对比

### 布局文件修改
```vue
<!-- 删除前 -->
<el-button type="success" class="ai-button" @click="showPdfExtractor = true">
  <el-icon><Document /></el-icon>
  PDF抽取
</el-button>

<!-- 删除后 -->
<!-- PDF抽取按钮已删除 -->
```

### 弹窗修改
```vue
<!-- 删除前 -->
<el-dialog v-model="showPdfExtractor" title="PDF文本抽取" width="70%" destroy-on-close>
  <PdfExtractor />
</el-dialog>

<!-- 删除后 -->
<!-- PDF抽取弹窗已删除 -->
```

## 验收标准
- [x] 头部导航栏中不再显示"PDF抽取"按钮
- [x] 点击PDF抽取按钮不再弹出对话框
- [x] PdfExtractor.vue组件文件被删除
- [x] 布局文件中清理了相关导入和引用
- [x] 删除了已废弃的aiPdf API
- [x] 没有残留的PDF抽取相关代码

## 影响范围
- **正面影响**：
  - 简化了用户界面
  - 减少了功能冗余（PDF抽取功能已集成到合同提取中）
  - 清理了废弃代码

- **无负面影响**：
  - AI聊天功能保持不变
  - 合同信息提取功能保持不变（支持PDF文件）
  - 其他功能不受影响

## 测试验证
- [x] 页面正常加载
- [x] AI助手功能正常
- [x] 合同信息提取功能正常
- [x] 导航菜单正常
- [x] 没有控制台错误

## 注意事项
1. **功能替代**：PDF抽取功能已集成到合同信息提取中，用户仍可通过合同提取功能处理PDF文件
2. **代码清理**：删除了所有相关的组件、API和引用
3. **向后兼容**：不影响现有功能的使用

## 相关文件
- `frontend/src/layout/index.vue` - 主布局文件
- `frontend/src/api/ai/index.ts` - AI相关API
- `frontend/src/components/ai/ContractExtractor.vue` - 合同信息提取组件（保留）

## 后续建议
1. 考虑在合同信息提取功能中添加更明显的PDF处理提示
2. 可以考虑添加功能说明，告知用户PDF处理已集成到合同提取中
