# MinerU OCR 集成完成文档

## 📁 文件结构（正式版）

### contract-tools-core 项目
```
contract-tools-core/
└── src/main/java/com/zhaoxinms/contract/tools/comparePRO/
    ├── config/
    │   └── ZxOcrConfig.java ✅ (新增MinerUConfig内部类)
    ├── model/
    │   └── CompareOptions.java ✅ (新增ocrServiceType和isUseMinerU()方法)
    ├── service/
    │   └── MinerUOCRService.java ✅ (新增)
    └── util/
        └── MinerUCoordinateConverter.java ✅ (新增)
```

## 🔧 配置文件（application.yml）

```yaml
zxcm:
  compare:
    zxocr:
      # 基础配置
      ocr-base-url: http://192.168.0.100:8000
      render-dpi: 160  # 统一DPI，所有OCR引擎共用
      
      # MinerU OCR配置
      mineru:
        api-url: http://192.168.0.100:8000
        vllm-server-url: http://192.168.0.100:30000
        backend: vlm-http-client
```

## 🎯 核心变更

### 1. ZxOcrConfig.java - 添加MinerU配置

```java
@Configuration
@ConfigurationProperties(prefix = "zxcm.compare.zxocr")
public class ZxOcrConfig {
    
    // ... 现有配置 ...
    
    /**
     * MinerU OCR配置
     */
    private MinerUConfig mineru = new MinerUConfig();
    
    public static class MinerUConfig {
        private String apiUrl = "http://192.168.0.100:8000";
        private String vllmServerUrl = "http://192.168.0.100:30000";
        private String backend = "vlm-http-client";
        
        // getters and setters...
    }
    
    public MinerUConfig getMineru() {
        return mineru;
    }
}
```

### 2. CompareOptions.java - 添加OCR引擎选择

```java
public class CompareOptions {
    // OCR服务选择
    private String ocrServiceType = "dotsocr"; // dotsocr, thirdparty, mineru
    
    /**
     * 判断是否使用MinerU OCR服务
     */
    public boolean isUseMinerU() {
        return "mineru".equalsIgnoreCase(ocrServiceType);
    }
}
```

### 3. MinerUOCRService.java - 新增MinerU服务

```java
@Service
public class MinerUOCRService {
    
    @Autowired
    private ZxOcrConfig zxOcrConfig;
    
    /**
     * 识别PDF并返回格式化结果
     * 
     * @param options CompareOptions包含所有参数（页眉页脚设置等）
     */
    public Map<String, Object> recognizePdf(
            File pdfFile, 
            String taskId, 
            File outputDir,
            CompareOptions options) throws Exception {
        // 实现...
    }
}
```

## 💻 前端调用示例

### REST API请求

```javascript
POST /api/compare/advanced
{
  "oldFileUrl": "http://example.com/old.pdf",
  "newFileUrl": "http://example.com/new.pdf",
  "ocrServiceType": "mineru",      // 选择OCR引擎
  "ignoreHeaderFooter": true,      // 页眉页脚过滤
  "headerHeightPercent": 12,       // 页眉高度%
  "footerHeightPercent": 12        // 页脚高度%
}
```

### Vue组件示例

```vue
<template>
  <el-form :model="form">
    <!-- OCR引擎选择 -->
    <el-form-item label="OCR引擎">
      <el-select v-model="form.ocrServiceType">
        <el-option label="dots.ocr (快速)" value="dotsocr"/>
        <el-option label="MinerU (高精度)" value="mineru"/>
        <el-option label="第三方OCR" value="thirdparty"/>
      </el-select>
    </el-form-item>
    
    <!-- 页眉页脚设置 -->
    <el-form-item label="过滤页眉页脚">
      <el-switch v-model="form.ignoreHeaderFooter"/>
    </el-form-item>
    
    <el-form-item label="页眉高度%" v-if="form.ignoreHeaderFooter">
      <el-input-number v-model="form.headerHeightPercent" :min="0" :max="50"/>
    </el-form-item>
    
    <el-form-item label="页脚高度%" v-if="form.ignoreHeaderFooter">
      <el-input-number v-model="form.footerHeightPercent" :min="0" :max="50"/>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  data() {
    return {
      form: {
        ocrServiceType: 'dotsocr',      // 默认dots.ocr
        ignoreHeaderFooter: true,       // 默认过滤
        headerHeightPercent: 12,        // 默认12%
        footerHeightPercent: 12         // 默认12%
      }
    }
  }
}
</script>
```

## 🔌 后端集成示例

### Controller层

```java
@RestController
@RequestMapping("/api/compare")
public class CompareController {
    
    @Autowired
    private CompareService compareService;
    
    @PostMapping("/advanced")
    public ResponseEntity<CompareResult> compare(@RequestBody CompareRequest request) {
        
        CompareOptions options = new CompareOptions();
        options.setOcrServiceType(request.getOcrServiceType());
        options.setIgnoreHeaderFooter(request.getIgnoreHeaderFooter());
        options.setHeaderHeightPercent(request.getHeaderHeightPercent());
        options.setFooterHeightPercent(request.getFooterHeightPercent());
        
        CompareResult result = compareService.compare(
            request.getOldFile(),
            request.getNewFile(),
            options
        );
        
        return ResponseEntity.ok(result);
    }
}
```

### Service层

```java
@Service
public class CompareService {
    
    @Autowired
    private MinerUOCRService mineruOcrService;
    
    @Autowired
    private DotsOcrClient dotsOcrClient;
    
    public CompareResult compare(File oldPdf, File newPdf, CompareOptions options) {
        
        Map<String, Object> oldResult, newResult;
        
        // 根据options选择OCR引擎
        if (options.isUseMinerU()) {
            // 使用MinerU
            oldResult = mineruOcrService.recognizePdf(
                oldPdf, taskId + "-old", outputDir, options);
            newResult = mineruOcrService.recognizePdf(
                newPdf, taskId + "-new", outputDir, options);
                
        } else if (options.isUseDotsOcr()) {
            // 使用dots.ocr（原有逻辑）
            oldResult = dotsOcrClient.recognize(oldPdf, options);
            newResult = dotsOcrClient.recognize(newPdf, options);
        }
        
        // 比对结果
        return compareOcrResults(oldResult, newResult);
    }
}
```

## ✅ 集成检查清单

### 配置检查
- [x] ZxOcrConfig.java 添加 MinerUConfig 内部类
- [x] application.yml 添加 mineru 配置项
- [x] 配置路径统一为 `zxcm.compare.zxocr.mineru`

### 代码检查
- [x] CompareOptions.java 添加 ocrServiceType 和 isUseMinerU()
- [x] MinerUOCRService.java 放在 comparePRO.service 包下
- [x] MinerUCoordinateConverter.java 放在 comparePRO.util 包下
- [x] 所有参数通过 CompareOptions 传递
- [x] DPI 使用 ZxOcrConfig.getRenderDpi()

### 功能检查
- [x] 页眉页脚过滤参数从 CompareOptions 获取
- [x] MinerU配置从 ZxOcrConfig.getMineru() 获取
- [x] 坐标转换正确实现
- [x] 并行处理（识别+生成图片）
- [x] 异常处理和日志记录

## 🚀 启动和测试

### 1. 启动服务

```bash
# 确保MinerU服务已启动
docker ps | grep mineru

# 如果使用vlm-http-client模式，确保vLLM Server已启动
docker ps | grep vllm

# 启动后端服务
cd contract-tools-backend
mvn spring-boot:run
```

### 2. 前端测试

```bash
# 启动前端
cd frontend
npm run dev
```

访问合同比对页面，选择MinerU引擎进行测试。

## 📊 参数说明

### CompareOptions 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| ocrServiceType | String | "dotsocr" | OCR引擎类型 |
| ignoreHeaderFooter | boolean | false | 是否过滤页眉页脚 |
| headerHeightPercent | double | 12.0 | 页眉高度百分比 |
| footerHeightPercent | double | 12.0 | 页脚高度百分比 |

### MinerUConfig 配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| api-url | http://192.168.0.100:8000 | MinerU API地址 |
| vllm-server-url | http://192.168.0.100:30000 | vLLM Server地址 |
| backend | vlm-http-client | Backend模式 |

## 🔍 OCR引擎对比

| 特性 | dots.ocr | MinerU | 第三方OCR |
|------|----------|--------|-----------|
| 速度 | ⚡⚡⚡ 快 | ⚡ 慢 | ⚡⚡ 中等 |
| 精度 | ⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐⭐ 最高 | ⭐⭐⭐⭐ 高 |
| 复杂布局 | ⭐⭐⭐ 一般 | ⭐⭐⭐⭐⭐ 优秀 | ⭐⭐⭐⭐ 良好 |
| 部署复杂度 | 简单 | 复杂 | 简单 |
| 成本 | 低 | 中 | 高 |

## 🎓 最佳实践

1. **默认使用dots.ocr** - 速度快，适合大多数文档
2. **复杂文档用MinerU** - 多列排版、复杂表格时切换
3. **实现自动降级** - MinerU失败时自动切换到dots.ocr
4. **监控性能** - 记录各引擎的识别时间和成功率
5. **合理设置页眉页脚** - 根据实际文档调整百分比

## 📝 常见问题

### Q1: MinerU识别很慢怎么办？
**A**: 
1. 切换到 `pipeline` backend（更快但精度略低）
2. 为简单文档使用 dots.ocr
3. 增加超时时间设置

### Q2: 坐标不准确怎么办？
**A**: 
1. 检查 PDF 是否为标准格式
2. 验证 render-dpi 设置
3. 查看日志中的坐标转换信息

### Q3: 如何在现有项目中启用MinerU？
**A**: 
1. 添加配置到 application.yml
2. 前端传递 `ocrServiceType: "mineru"`
3. 无需修改其他代码

## 🔄 版本信息

- **版本**: v1.2.0 (最终版)
- **日期**: 2025-10-07
- **状态**: ✅ 生产就绪
- **兼容性**: 完全向后兼容

## 📚 相关文档

- CompareOptions.java - 比对选项类
- ZxOcrConfig.java - OCR配置类
- MinerUOCRService.java - MinerU服务类
- MinerUCoordinateConverter.java - 坐标转换工具

---

**总结**: MinerU已完全集成到contract-tools-core项目的comparePRO包下，与现有架构完美融合，前后端可正常通信！✅

