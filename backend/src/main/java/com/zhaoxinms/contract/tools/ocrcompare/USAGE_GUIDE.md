# 🎯 **OCR文档比对系统使用指南**

## 🆕 **新增功能：多格式文档支持**

OCR比对系统现在支持多种文档格式，无需手动转换即可进行比对：

### **支持的文件格式**
- ✅ **PDF文档** (.pdf) - 直接OCR识别
- ✅ **Word文档** (.doc, .docx, .docm) - 自动转换为PDF
- ✅ **Excel表格** (.xls, .xlsx, .xlsm, .xlsb) - 自动转换为PDF

### **文档转换流程**
```
上传文件 → 格式检查 → 转换为PDF → OCR识别 → 文本比对 → 生成结果
    ↓           ↓         ↓         ↓         ↓         ↓
  支持格式   格式验证    OnlyOffice   RapidOCR   差异分析   PDF标注
```

### **技术特点**
- 🔄 **自动转换**: 基于OnlyOffice Document Server
- ⚡ **高效处理**: 转换完成后自动清理临时文件
- 🛡️ **格式验证**: 严格的文件格式检查
- 📊 **进度监控**: 实时显示转换和OCR进度

## ✅ **编译错误修复完成**

所有编译错误已修复完成！系统现在可以正常编译和运行。

### **修复的主要问题**
1. ✅ `MockMultipartFile` 导入问题 → 创建了自定义 `SimpleMultipartFile`
2. ✅ `CompareOptions` 方法参数类型不匹配 → 修复了类型转换
3. ✅ `Position.getPosition()` 方法不存在 → 直接使用 `Position` 的字段
4. ✅ `OCRTaskService.readOCRResult()` 方法不存在 → 创建了文件读取方法
5. ✅ 清理了未使用的方法和导入

## 🚀 **快速启动**

### **1. 启动后端服务**
```bash
# 在项目根目录下运行
mvn spring-boot:run

# 或者使用IDE直接运行Spring Boot应用
```

### **2. 访问前端页面**
```
# OCR比对上传页面
http://localhost:8080/#/ocr-compare

# 现有的文档比对页面（参考）
http://localhost:8080/#/compare
```

### **3. 测试API接口**

#### **提交OCR比对任务**
```bash
curl -X POST http://localhost:8080/api/ocr-compare/upload \
  -F "oldFile=@path/to/old.pdf" \
  -F "newFile=@path/to/new.pdf" \
  -F "ignoreHeaderFooter=true" \
  -F "ignoreCase=true"
```

#### **查询任务状态**
```bash
curl http://localhost:8080/api/ocr-compare/task/{taskId}/status
```

#### **获取比对结果**
```bash
curl http://localhost:8080/api/ocr-compare/task/{taskId}/result
```

## 📋 **系统架构验证**

### **后端组件 ✅**
- `OCRCompareController` - REST API控制器
- `OCRCompareService` - 核心业务逻辑
- `OCRCompareTask` - 任务实体和状态管理
- `OCRTextPositionProcessor` - OCR结果处理
- `OCRPDFAnnotator` - PDF标注功能
- `OCRCompareFileController` - 文件服务

### **前端组件 ✅**
- `OCRCompare.vue` - 文件上传和进度监控页面
- `OCRCompareResult.vue` - 比对结果展示页面
- `ocr-compare.ts` - API接口封装
- 路由配置已集成

### **核心功能 ✅**
- 异步OCR任务处理
- 实时进度监控
- 文档比对算法集成
- PDF标注和高亮
- 在线PDF查看

## 🔧 **环境配置**

### **必需环境**
1. **Java 8+** - Spring Boot后端
2. **Python 3.7+** - OCR识别
3. **RapidOCR** - 文字识别库
4. **Node.js** - 前端构建（如果需要）

### **OCR配置**
确保 `application-ocr.yml` 配置正确：
```yaml
ocr:
  python:
    root: D:\git\zhaoxin-contract-tool-set\compareScript
    executable: python
    script: rapid_pdf_ocr.py
  settings:
    dpi: 150
    min-score: 0.5
```

### **文件目录结构**
```
compareScript/
├── rapid_pdf_ocr.py          # OCR识别脚本
├── test.pdf                  # 测试PDF文件
├── output/                   # OCR结果输出目录
└── logs/                     # Python日志目录

uploads/ocr-compare/
├── uploads/                  # 用户上传文件
└── results/                  # 比对结果文件
    └── {taskId}/
        ├── old_annotated.pdf
        ├── new_annotated.pdf
        └── result.json
```

## 🧪 **测试功能**

### **自动化测试**
```bash
# 启动时运行测试（可选）
java -Docr.test.run=true -jar your-app.jar
```

### **手动测试步骤**

#### **1. 上传文件测试**
1. 访问 `