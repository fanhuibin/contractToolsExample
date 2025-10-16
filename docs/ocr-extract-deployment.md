# OCR文本提取功能 - 部署和使用指南

## 快速开始

### 1. 前置条件

确保以下服务已正常运行：
- MinerU OCR服务
- 后端Spring Boot应用
- 前端Vue应用

### 2. 后端部署

#### 2.1 代码已包含

OCR提取功能已集成到SDK项目中，无需额外部署。相关代码位于：

```
contract-tools-sdk/src/main/java/com/zhaoxinms/contract/tools/ocr/
├── controller/OcrExtractController.java
├── service/OcrExtractService.java
└── service/impl/OcrExtractServiceImpl.java
```

#### 2.2 配置检查

确认 `application.yml` 中的配置：

```yaml
file:
  upload:
    root-path: ./uploads
```

#### 2.3 重新编译（如需要）

```bash
cd contract-tools-sdk
mvn clean install
```

### 3. 前端部署

#### 3.1 代码已包含

前端代码已集成，相关文件：

```
frontend/src/
├── api/ocr-extract.ts
├── router/index.ts (已添加路由)
├── views/home/HomePage.vue (已添加入口)
└── views/ocr/
    ├── OcrExtract.vue
    └── components/MarkdownViewer.vue
```

#### 3.2 重新构建（如需要）

```bash
cd frontend
npm run build
```

### 4. 启动服务

#### 4.1 启动后端

```bash
# 使用Maven启动
mvn spring-boot:run

# 或使用编译好的JAR
java -jar target/contract-tools-sdk-*.jar
```

#### 4.2 启动前端（开发模式）

```bash
cd frontend
npm run dev
```

#### 4.3 访问应用

浏览器访问：`http://localhost:5173` (或您配置的端口)

## 功能测试

### 测试步骤

1. **访问功能页面**
   - 在首页点击"OCR文本提取"卡片
   - 或直接访问：`http://localhost:5173/ocr-extract`

2. **上传测试文档**
   - 准备一个PDF文档（建议5-20页）
   - 拖拽或点击上传区域选择文件
   - 配置OCR选项（可选）：
     - 勾选"忽略页眉页脚"
     - 设置页眉高度百分比：12%
     - 设置页脚高度百分比：12%

3. **查看处理进度**
   - 上传后自动开始处理
   - 观察进度条和状态提示
   - 预计处理时间：每页约2-5秒

4. **查看提取结果**
   - 处理完成后自动显示结果
   - 左侧显示PDF图像
   - 右侧显示提取文本
   - 测试功能：
     - 切换显示模式（左右分栏/仅图片/仅文本）
     - 滚动查看不同页面
     - 复制文本
     - 下载文本文件

### 预期结果

✅ **成功标志**：
- 文件上传成功
- 进度正常更新
- 图片正常显示
- 文本正确提取
- 表格格式保留
- 可以复制和下载文本

❌ **失败情况**：
- 上传失败：检查文件格式和大小
- OCR识别失败：检查MinerU服务
- 图片不显示：检查API路径和文件权限
- 文本为空：检查OCR服务配置

## API测试

使用Postman或curl测试API：

### 1. 上传PDF

```bash
curl -X POST http://localhost:8080/api/ocr/extract/upload \
  -F "file=@test.pdf" \
  -F "ignoreHeaderFooter=true" \
  -F "headerHeightPercent=12.0" \
  -F "footerHeightPercent=12.0"
```

响应示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "文件上传成功，开始OCR提取..."
  }
}
```

### 2. 查询状态

```bash
curl http://localhost:8080/api/ocr/extract/status/{taskId}
```

### 3. 获取结果

```bash
curl http://localhost:8080/api/ocr/extract/result/{taskId}
```

### 4. 获取页面图片

```bash
curl http://localhost:8080/api/ocr/extract/page-image/{taskId}/1 --output page-1.png
```

## 性能测试

### 测试场景

| 文档类型 | 页数 | 预期时间 | 内存占用 |
|---------|-----|---------|---------|
| 简单文本 | 10页 | 20-50秒 | ~200MB |
| 复杂文档 | 10页 | 30-60秒 | ~300MB |
| 大型文档 | 100页 | 3-8分钟 | ~500MB |
| 表格文档 | 10页 | 30-70秒 | ~300MB |

### 性能指标

- **并发支持**：建议同时处理任务数 ≤ 3
- **文件大小限制**：≤ 100MB
- **页数限制**：建议 ≤ 200页
- **响应时间**：
  - 上传响应：< 1秒
  - 状态查询：< 100ms
  - 结果获取：< 500ms
  - 图片加载：< 500ms/张

## 故障排查

### 问题1：上传失败

**症状**：点击上传后提示"上传失败"

**可能原因**：
- 文件格式不正确（非PDF）
- 文件大小超过限制
- 后端服务未启动
- 网络连接问题

**解决方法**：
1. 确认文件格式为PDF
2. 检查文件大小 < 100MB
3. 检查后端服务状态
4. 查看浏览器控制台和网络请求

### 问题2：OCR识别失败

**症状**：进度停在"OCR识别"阶段，最终显示失败

**可能原因**：
- MinerU OCR服务未启动
- OCR服务配置错误
- PDF文件损坏

**解决方法**：
1. 检查MinerU服务状态
2. 查看后端日志：`logs/spring.log`
3. 尝试其他PDF文件
4. 检查OCR服务配置

### 问题3：图片不显示

**症状**：右侧文本正常，左侧图片区域空白

**可能原因**：
- 图片文件未生成
- 图片路径配置错误
- 前端API路径错误
- 文件权限问题

**解决方法**：
1. 检查任务目录下是否有images文件夹
2. 确认图片文件存在
3. 检查前端apiPrefix配置
4. 查看浏览器控制台错误
5. 检查文件权限：`chmod -R 755 uploads`

### 问题4：文本显示不完整

**症状**：只显示部分文本，或文本格式混乱

**可能原因**：
- OCR识别质量问题
- 表格解析错误
- Markdown渲染问题

**解决方法**：
1. 检查原始PDF质量
2. 尝试调整页眉页脚设置
3. 查看ocr_text.txt原始文件
4. 检查MarkdownViewer组件

### 问题5：任务卡住不更新

**症状**：进度条长时间不动，状态不更新

**可能原因**：
- 后端异步任务异常
- OCR服务超时
- 服务器资源不足

**解决方法**：
1. 查看后端日志
2. 重启后端服务
3. 检查服务器CPU和内存使用
4. 减小并发任务数

## 日志查看

### 后端日志

查看Spring Boot日志：
```bash
tail -f logs/spring.log
```

关键日志：
- `接收到OCR提取请求`：上传成功
- `开始OCR提取任务`：任务开始
- `MinerU PDF识别完成`：OCR完成
- `OCR提取任务完成`：整体完成
- `OCR提取任务失败`：任务失败

### 前端日志

打开浏览器开发者工具（F12）：
- Console：查看JavaScript错误
- Network：查看API请求状态
- Application：查看本地存储

## 数据清理

### 清理任务数据

删除旧的任务数据以释放空间：

```bash
# 删除所有任务
rm -rf uploads/ocr-extract-tasks/*

# 删除7天前的任务
find uploads/ocr-extract-tasks/ -type d -mtime +7 -exec rm -rf {} \;

# 删除指定任务
rm -rf uploads/ocr-extract-tasks/{taskId}
```

### 定期清理脚本

创建定时任务自动清理（Linux）：

```bash
# 编辑crontab
crontab -e

# 添加清理任务（每天凌晨2点清理7天前的数据）
0 2 * * * find /path/to/uploads/ocr-extract-tasks/ -type d -mtime +7 -exec rm -rf {} \;
```

## 生产环境配置

### 1. 性能优化

```yaml
# application-prod.yml
file:
  upload:
    root-path: /data/uploads
    max-file-size: 100MB
    
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

### 2. 安全配置

- 启用HTTPS
- 配置CORS策略
- 添加文件类型验证
- 限制上传频率

### 3. 监控告警

- 监控OCR服务可用性
- 监控磁盘空间使用
- 监控任务处理时间
- 设置异常告警

### 4. 备份策略

- 定期备份任务结果
- 保留重要文档的OCR结果
- 配置数据归档策略

## 常见问题FAQ

**Q: 支持哪些文件格式？**
A: 目前仅支持PDF格式。

**Q: 单个文件大小限制是多少？**
A: 默认限制100MB，可在配置中调整。

**Q: OCR识别支持哪些语言？**
A: 依赖MinerU OCR引擎，通常支持中英文及常见语言。

**Q: 可以同时处理多个文件吗？**
A: 可以，但建议控制并发数量（≤3）以保证性能。

**Q: 提取的文本可以编辑吗？**
A: 当前版本不支持在线编辑，可复制或下载后编辑。

**Q: 历史提取记录保存多久？**
A: 默认永久保存，建议定期清理或配置自动清理策略。

**Q: 如何提高OCR识别准确率？**
A: 
1. 使用高质量的PDF源文件
2. 启用页眉页脚过滤
3. 确保PDF文件清晰可读
4. 避免扫描质量差的文档

## 技术支持

如遇到其他问题，请：
1. 查看功能文档：`docs/ocr-extract-feature.md`
2. 查看系统日志获取详细错误信息
3. 联系开发团队获取支持
4. 提交Issue到项目仓库

