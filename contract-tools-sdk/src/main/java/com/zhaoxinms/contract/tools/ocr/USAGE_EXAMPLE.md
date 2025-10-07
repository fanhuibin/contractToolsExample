# MinerU OCR使用示例

## 1. 在合同比对中使用MinerU

### 1.1 配置OCR引擎类型

在`CompareOptions`中设置OCR服务类型：

```java
CompareOptions options = new CompareOptions();
options.setOcrServiceType("mineru");  // 使用MinerU
// options.setOcrServiceType("dotsocr");  // 使用dots.ocr（默认）
// options.setOcrServiceType("thirdparty");  // 使用第三方OCR

// 页眉页脚设置（所有OCR引擎通用）
options.setIgnoreHeaderFooter(true);
options.setHeaderHeightPercent(12.0);
options.setFooterHeightPercent(12.0);
```

### 1.2 在比对服务中调用MinerU

```java
@Service
public class CompareService {
    
    @Autowired
    private MinerUOCRService mineruOcrService;
    
    public CompareResult compare(File oldPdf, File newPdf, CompareOptions options) {
        
        // 根据options选择OCR引擎
        if (options.isUseMinerU()) {
            // 使用MinerU识别
            Map<String, Object> oldResult = mineruOcrService.recognizePdf(
                oldPdf,
                taskId + "-old",
                outputDir,
                options.isIgnoreHeaderFooter(),
                options.getHeaderHeightPercent(),
                options.getFooterHeightPercent()
            );
            
            Map<String, Object> newResult = mineruOcrService.recognizePdf(
                newPdf,
                taskId + "-new",
                outputDir,
                options.isIgnoreHeaderFooter(),
                options.getHeaderHeightPercent(),
                options.getFooterHeightPercent()
            );
            
            // 处理识别结果...
            
        } else if (options.isUseDotsOcr()) {
            // 使用dots.ocr识别（原有逻辑）
            // ...
        }
        
        return result;
    }
}
```

## 2. 前端API调用示例

### 2.1 REST API请求

```javascript
// POST /api/compare/advanced
{
  "oldFileUrl": "http://example.com/old.pdf",
  "newFileUrl": "http://example.com/new.pdf",
  "ocrServiceType": "mineru",  // 选择MinerU引擎
  "ignoreHeaderFooter": true,
  "headerHeightPercent": 12,
  "footerHeightPercent": 12
}
```

### 2.2 Controller处理

```java
@RestController
@RequestMapping("/api/compare")
public class CompareController {
    
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

## 3. OCR引擎切换说明

### 3.1 支持的OCR引擎

| 引擎类型 | ocrServiceType值 | 特点 | 适用场景 |
|---------|-----------------|------|---------|
| dots.ocr | `dotsocr` | 快速、准确 | 标准合同文档 |
| MinerU | `mineru` | 高精度、支持复杂布局 | 复杂格式、表格多的文档 |
| 第三方OCR | `thirdparty` | 阿里云通义千问 | 需要云服务的场景 |

### 3.2 引擎选择建议

```java
// 自动选择最佳引擎
public String selectBestOcrEngine(File pdfFile) {
    int pageCount = getPdfPageCount(pdfFile);
    boolean hasComplexLayout = checkComplexLayout(pdfFile);
    
    if (hasComplexLayout) {
        return "mineru";  // 复杂布局使用MinerU
    } else if (pageCount > 50) {
        return "dotsocr";  // 大文档使用dots.ocr（更快）
    } else {
        return "dotsocr";  // 默认使用dots.ocr
    }
}
```

## 4. 配置说明

### 4.1 application.yml配置

```yaml
# MinerU配置
mineru:
  api:
    url: http://192.168.0.100:8000
  vllm:
    server:
      url: http://192.168.0.100:30000
  backend: vlm-http-client

# 通用OCR配置（所有引擎共享）
zxcm:
  compare:
    zxocr:
      render-dpi: 160  # DPI设置（MinerU和dots.ocr都使用此值）
      ocr-base-url: http://192.168.0.100:8000  # dots.ocr地址
```

### 4.2 运行时参数

页眉页脚过滤参数**不从配置文件读取**，而是通过`CompareOptions`传入：

```java
// ✅ 正确：通过CompareOptions传入
options.setIgnoreHeaderFooter(true);
options.setHeaderHeightPercent(12.0);
options.setFooterHeightPercent(12.0);

// ❌ 错误：不要在配置文件中设置（会被忽略）
// mineru.filter.header.footer: true  # 此配置已废弃
```

## 5. 完整示例

### 5.1 比对两个PDF文档

```java
@Service
public class ContractCompareService {
    
    @Autowired
    private MinerUOCRService mineruOcrService;
    
    @Autowired
    private DotsOcrClient dotsOcrClient;
    
    public CompareResult compareContracts(
            File oldContract, 
            File newContract,
            String ocrEngine,
            boolean ignoreHeaderFooter) {
        
        String taskId = UUID.randomUUID().toString();
        File outputDir = new File("./uploads/compare-results/" + taskId);
        outputDir.mkdirs();
        
        CompareOptions options = new CompareOptions();
        options.setOcrServiceType(ocrEngine);
        options.setIgnoreHeaderFooter(ignoreHeaderFooter);
        options.setHeaderHeightPercent(12.0);
        options.setFooterHeightPercent(12.0);
        
        // 识别旧文档
        Map<String, Object> oldOcrResult;
        if ("mineru".equals(ocrEngine)) {
            oldOcrResult = mineruOcrService.recognizePdf(
                oldContract, taskId + "-old", outputDir,
                options.isIgnoreHeaderFooter(),
                options.getHeaderHeightPercent(),
                options.getFooterHeightPercent()
            );
        } else {
            // 使用dots.ocr或其他引擎
            oldOcrResult = dotsOcrClient.recognize(oldContract, options);
        }
        
        // 识别新文档
        Map<String, Object> newOcrResult;
        if ("mineru".equals(ocrEngine)) {
            newOcrResult = mineruOcrService.recognizePdf(
                newContract, taskId + "-new", outputDir,
                options.isIgnoreHeaderFooter(),
                options.getHeaderHeightPercent(),
                options.getFooterHeightPercent()
            );
        } else {
            newOcrResult = dotsOcrClient.recognize(newContract, options);
        }
        
        // 比对识别结果
        CompareResult result = compareOcrResults(oldOcrResult, newOcrResult);
        
        return result;
    }
    
    private CompareResult compareOcrResults(
            Map<String, Object> oldResult,
            Map<String, Object> newResult) {
        // 比对逻辑...
        return new CompareResult();
    }
}
```

### 5.2 前端Vue组件示例

```vue
<template>
  <div class="compare-panel">
    <el-form>
      <el-form-item label="OCR引擎">
        <el-select v-model="ocrEngine">
          <el-option label="dots.ocr (快速)" value="dotsocr"/>
          <el-option label="MinerU (高精度)" value="mineru"/>
          <el-option label="第三方OCR" value="thirdparty"/>
        </el-select>
      </el-form-item>
      
      <el-form-item label="过滤页眉页脚">
        <el-switch v-model="ignoreHeaderFooter"/>
      </el-form-item>
      
      <el-form-item label="页眉高度%" v-if="ignoreHeaderFooter">
        <el-input-number v-model="headerHeight" :min="0" :max="50"/>
      </el-form-item>
      
      <el-form-item label="页脚高度%" v-if="ignoreHeaderFooter">
        <el-input-number v-model="footerHeight" :min="0" :max="50"/>
      </el-form-item>
      
      <el-button @click="startCompare" type="primary">
        开始比对
      </el-button>
    </el-form>
  </div>
</template>

<script>
export default {
  data() {
    return {
      ocrEngine: 'dotsocr',
      ignoreHeaderFooter: true,
      headerHeight: 12,
      footerHeight: 12
    }
  },
  methods: {
    async startCompare() {
      const response = await this.$api.post('/api/compare/advanced', {
        oldFileUrl: this.oldFile,
        newFileUrl: this.newFile,
        ocrServiceType: this.ocrEngine,
        ignoreHeaderFooter: this.ignoreHeaderFooter,
        headerHeightPercent: this.headerHeight,
        footerHeightPercent: this.footerHeight
      })
      
      // 处理比对结果...
    }
  }
}
</script>
```

## 6. 性能对比

| 维度 | dots.ocr | MinerU |
|------|----------|--------|
| 单页耗时 | 0.5-1秒 | 2-5秒 |
| 10页文档 | 5-10秒 | 20-50秒 |
| 识别精度 | 95%+ | 98%+ |
| 复杂表格 | 一般 | 优秀 |
| 多列排版 | 一般 | 优秀 |

## 7. 故障排查

### 7.1 MinerU识别失败

```java
try {
    result = mineruOcrService.recognizePdf(...);
} catch (Exception e) {
    log.error("MinerU识别失败，降级到dots.ocr", e);
    // 自动降级
    result = dotsOcrClient.recognize(pdfFile, options);
}
```

### 7.2 超时问题

如果遇到MinerU超时，可以调整连接超时设置或切换到更快的backend模式：

```yaml
mineru:
  backend: pipeline  # 使用pipeline模式（更快但精度略低）
```

## 8. 最佳实践

1. **默认使用dots.ocr**：对于大多数标准文档，dots.ocr速度更快
2. **特殊情况使用MinerU**：复杂表格、多列排版、特殊格式时使用MinerU
3. **统一参数管理**：页眉页脚过滤参数通过`CompareOptions`传递，不写死在配置中
4. **实现自动降级**：MinerU失败时自动切换到dots.ocr
5. **监控性能指标**：记录每种引擎的识别耗时和成功率

## 9. 更新日志

- **v1.0.0 (2025-10-07)**
  - ✅ 支持通过CompareOptions切换OCR引擎
  - ✅ 页眉页脚参数从外部传入，不依赖配置文件
  - ✅ DPI设置统一使用zxcm.compare.zxocr.render-dpi
  - ✅ 完整的向后兼容性（dots.ocr/第三方OCR/MinerU）

