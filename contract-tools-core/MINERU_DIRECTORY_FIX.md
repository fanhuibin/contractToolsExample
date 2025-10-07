# ✅ MinerU目录结构和JSON保存修复

## 📋 问题

1. ❌ 图片保存路径错误：保存到 `images/` 而不是 `images/old` 或 `images/new`
2. ❌ 缺少JSON文件保存：原始响应和处理后的结果都没有保存

## ✅ 修复方案

### 1. 统一目录结构

参考dots.ocr的目录结构：

```
uploads/compare-pro/tasks/{taskId}/
├── images/
│   ├── old/                  ← MinerU图片保存到这里
│   │   ├── page-1.png
│   │   ├── page-2.png
│   │   └── ...
│   └── new/                  ← MinerU图片保存到这里
│       ├── page-1.png
│       ├── page-2.png
│       └── ...
├── ocr/                      ← 新增：保存OCR结果
│   ├── mineru_raw_old.json          ← MinerU原始响应
│   ├── mineru_raw_new.json
│   ├── mineru_processed_old.json    ← 处理后的结果
│   └── mineru_processed_new.json
├── old_xxx.pdf
└── new_xxx.pdf
```

### 2. 修改内容

#### 修改 `MinerUOCRService.java`

**1) 修改方法签名，添加docMode参数**:

```java
public Map<String, Object> recognizePdf(
        File pdfFile, 
        String taskId, 
        File outputDir,
        String docMode,     // ← 新增参数
        CompareOptions options) throws Exception
```

**2) 修改图片保存路径**:

```java
private List<Map<String, Object>> generatePageImages(
        File pdfFile, File outputDir, String taskId, String docMode) {  // ← 添加docMode
    
    // 图片保存到 images/old 或 images/new 目录
    File imagesDir = new File(outputDir, "images/" + docMode);  // ← 修改路径
```

**3) 添加JSON保存方法**:

```java
// 保存MinerU原始响应JSON
saveRawResponse(apiResult, outputDir, taskId, docMode);

// 保存处理后的结果JSON
saveProcessedResult(result, outputDir, taskId, docMode);
```

#### 修改 `CompareService.java`

**调用时传递docMode参数**:

```java
Map<String, Object> result = mineruOcrService.recognizePdf(
    pdfPath.toFile(),
    taskId,
    outputDir,
    docMode,    // ← 传递old或new
    options
);
```

## 📝 保存的JSON内容

### 1. mineru_raw_old.json（原始响应）

MinerU API的完整原始响应，包含：
- content_list：所有识别的内容块
- model_output：模型原始输出（如果启用）
- middle_json：中间处理数据（如果启用）

### 2. mineru_processed_old.json（处理后结果）

处理后的结构化数据，包含：
- totalPages：总页数
- pageData：按页组织的数据
- pageImages：图片信息
- processingTimeMs：处理耗时

## 🔍 日志示例

### 成功的日志

```
使用MinerU识别PDF: old_xxx.pdf, 任务ID: xxx, 模式: old
并行处理：提交PDF识别和生成图片
生成页面图片: page-1.png, 尺寸: 1322x1870
保存MinerU原始响应: D:\...\ocr\mineru_raw_old.json
保存MinerU处理后结果: D:\...\ocr\mineru_processed_old.json
MinerU OCR识别完成，共6页，耗时2497ms
```

### 图片路径验证

```
images/old/
  - page-1.png  ✅
  - page-2.png  ✅
  - page-3.png  ✅

images/new/
  - page-1.png  ✅
  - page-2.png  ✅
  - page-3.png  ✅
```

## 🚀 编译和测试

```bash
# 1. 编译core模块
cd D:\git\zhaoxin-contract-tool-set\contract-tools-core
mvn clean install -DskipTests

# 2. 编译sdk模块
cd ..\contract-tools-sdk
mvn clean install -DskipTests

# 3. 启动服务
mvn spring-boot:run
```

## ✅ 验证清单

### 启动后测试

- [ ] 上传两个PDF进行比对
- [ ] 查看日志，确认"使用MinerU识别PDF"
- [ ] 检查目录结构：
  - [ ] `uploads/compare-pro/tasks/{taskId}/images/old/` 存在
  - [ ] `uploads/compare-pro/tasks/{taskId}/images/new/` 存在
  - [ ] `uploads/compare-pro/tasks/{taskId}/ocr/` 存在
- [ ] 检查JSON文件：
  - [ ] `mineru_raw_old.json` 存在且有内容
  - [ ] `mineru_raw_new.json` 存在且有内容
  - [ ] `mineru_processed_old.json` 存在且有内容
  - [ ] `mineru_processed_new.json` 存在且有内容
- [ ] 前端能正常显示比对结果
- [ ] 前端能正常显示图片

---

**最后更新**: 2025-10-07

