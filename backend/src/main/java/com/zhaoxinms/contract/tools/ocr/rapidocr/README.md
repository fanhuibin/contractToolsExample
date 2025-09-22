# RapidOCR集成文档

## 概述

本模块实现了与[RapidOCR API](https://rapidai.github.io/RapidOCRDocs/main/install_usage/rapidocr_api/usage/)的集成，提供了完整的OCR识别功能。

## 功能特性

- **多种调用方式**：支持文件上传和Base64数据两种OCR识别方式
- **灵活配置**：可配置是否启用文本检测、方向分类、文本识别
- **Spring Boot集成**：自动配置，开箱即用
- **REST API**：提供HTTP接口供前端调用
- **批量处理**：支持批量图像识别
- **健康检查**：服务可用性检测
- **详细日志**：可配置详细的请求响应日志

## 快速开始

### 1. 启动RapidOCR服务

根据[官方文档](https://rapidai.github.io/RapidOCRDocs/main/install_usage/rapidocr_api/usage/)，在192.168.0.100:9005启动RapidOCR API服务：

```bash
# 安装
pip install rapidocr_api

# 启动服务
rapidocr_api -ip 0.0.0.0 -p 9005 -workers 2
```

### 2. 配置应用

在`application.yml`中配置RapidOCR：

```yaml
rapidocr:
  enabled: true
  base-url: http://192.168.0.100:9005
  timeout: PT2M  # 2分钟超时
  verbose-logging: false
  max-concurrency: 10
  default-use-detection: true
  default-use-classification: true
  default-use-recognition: true
```

### 3. 使用方式

#### 3.1 通过依赖注入使用

```java
@Autowired
private RapidOcrService rapidOcrService;

public void recognizeImage() {
    try {
        // 检查服务状态
        if (!rapidOcrService.isServiceAvailable()) {
            logger.error("RapidOCR服务不可用");
            return;
        }
        
        // 识别文件
        File imageFile = new File("test.jpg");
        List<RapidOcrClient.RapidOcrTextBox> textBoxes = rapidOcrService.recognizeFile(imageFile);
        
        // 输出结果
        for (RapidOcrClient.RapidOcrTextBox box : textBoxes) {
            System.out.println("文本: " + box.text + ", 置信度: " + box.confidence);
        }
        
        // 转换为纯文本
        String text = rapidOcrService.convertToText(textBoxes);
        System.out.println("识别文本: " + text);
        
    } catch (IOException e) {
        logger.error("OCR识别失败", e);
    }
}
```

#### 3.2 直接使用客户端

```java
// 创建客户端
RapidOcrClient client = RapidOcrClient.builder()
        .baseUrl("http://192.168.0.100:9005")
        .verboseLogging(true)
        .build();

// 识别图像
List<RapidOcrClient.RapidOcrTextBox> textBoxes = client.ocrFile(new File("test.jpg"));

// 获取原始JSON结果
JsonNode rawResult = client.ocrByFile(new File("test.jpg"), true, true, true);
```

#### 3.3 通过REST API使用

```bash
# 健康检查
curl http://localhost:8080/api/rapidocr/health

# 文件上传OCR
curl -X POST http://localhost:8080/api/rapidocr/ocr/file \
  -F "file=@test.jpg" \
  -F "use_detection=true" \
  -F "use_classification=true" \
  -F "use_recognition=true"

# Base64数据OCR
curl -X POST http://localhost:8080/api/rapidocr/ocr/data \
  -H "Content-Type: application/json" \
  -d '{
    "image_data": "base64_encoded_image_data",
    "use_detection": true,
    "use_classification": true,
    "use_recognition": true,
    "return_raw": false
  }'
```

## API接口

### 服务健康检查
- **URL**: `GET /api/rapidocr/health`
- **响应**: `{"service": "RapidOCR", "status": "UP", "info": "..."}`

### 文件OCR识别
- **URL**: `POST /api/rapidocr/ocr/file`
- **参数**: 
  - `file`: 图像文件 (必需)
  - `use_detection`: 是否使用文本检测 (默认: true)
  - `use_classification`: 是否使用方向分类 (默认: true)
  - `use_recognition`: 是否使用文本识别 (默认: true)
  - `return_raw`: 是否返回原始JSON (默认: false)

### Base64数据OCR识别
- **URL**: `POST /api/rapidocr/ocr/data`
- **请求体**: 
```json
{
  "image_data": "base64_encoded_image",
  "use_detection": true,
  "use_classification": true,
  "use_recognition": true,
  "return_raw": false
}
```

## 数据结构

### RapidOcrTextBox
```java
public static class RapidOcrTextBox {
    public String id;              // 文本框ID
    public String text;            // 识别的文本内容
    public float confidence;       // 置信度 (0.0-1.0)
    public float[][] boundingBox;  // 边界框坐标 [左上, 右上, 右下, 左下]
}
```

### RapidOCR API原始格式
```json
{
  "0": {
    "rec_txt": "识别的文本",
    "dt_boxes": [[x1, y1], [x2, y2], [x3, y3], [x4, y4]],
    "score": "0.8176"
  }
}
```

## 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `rapidocr.enabled` | boolean | true | 是否启用RapidOCR功能 |
| `rapidocr.base-url` | String | http://192.168.0.100:9005 | RapidOCR API服务地址 |
| `rapidocr.timeout` | Duration | PT2M | 请求超时时间 |
| `rapidocr.verbose-logging` | boolean | false | 是否开启详细日志 |
| `rapidocr.max-concurrency` | int | 10 | 最大并发请求数 |
| `rapidocr.default-use-detection` | boolean | true | 默认是否使用文本检测 |
| `rapidocr.default-use-classification` | boolean | true | 默认是否使用方向分类 |
| `rapidocr.default-use-recognition` | boolean | true | 默认是否使用文本识别 |

## 注意事项

1. **服务依赖**: 确保RapidOCR API服务在指定地址正常运行
2. **网络连接**: 确保应用服务器能够访问RapidOCR服务地址
3. **图像格式**: 支持常见的图像格式 (JPG, PNG, BMP等)
4. **文件大小**: 根据RapidOCR服务的限制调整上传文件大小
5. **并发控制**: 可通过`max-concurrency`参数控制并发请求数，避免服务过载

## 故障排除

### 常见错误及解决方案

#### 1. HTTP 500 Internal Server Error
**问题**: 服务器内部错误，通常是参数格式或服务器配置问题

**解决方案**:
- 确认请求参数格式正确，文件参数名必须使用 `image_file`
- 检查RapidOCR服务的日志，查看具体错误信息
- 确认模型文件路径配置正确
- 尝试重启RapidOCR服务

**示例curl测试**:
```bash
curl -F image_file=@test.jpg http://192.168.0.100:9005/ocr
```

#### 2. 连接被拒绝 (Connection Refused)
**问题**: 无法连接到RapidOCR服务

**解决方案**:
- 检查服务是否在指定端口启动: `netstat -an | grep 9005`
- 确认服务地址和端口配置正确
- 检查防火墙设置
- 在浏览器中访问 `http://192.168.0.100:9005/docs` 确认服务可用

#### 3. 超时错误 (Timeout)
**问题**: 请求超时

**解决方案**:
- 增加`timeout`配置
- 检查图像文件大小，过大的图像可能需要更长处理时间
- 检查服务器性能和负载
- 尝试减小图像分辨率

#### 4. 图像格式不支持
**问题**: 图像格式不被识别

**解决方案**:
- 确保图像格式为 JPG, PNG, BMP, GIF 等常见格式
- 检查图像文件是否损坏
- 尝试转换图像格式

#### 5. 模型文件错误
**问题**: OCR模型加载失败

**解决方案**:
```bash
# 确保模型路径正确配置
export det_model_path=/path/to/ch_PP-OCRv4_det_server_infer.onnx
export rec_model_path=/path/to/ch_PP-OCRv4_rec_server_infer.onnx
rapidocr_api -ip 0.0.0.0 -p 9005 -workers 2
```

### 调试技巧

1. **启用详细日志**: 设置 `verboseLogging=true`
2. **使用测试方法**: 调用 `client.testApiEndpoints()` 检查所有端点
3. **检查服务日志**: 查看RapidOCR服务的运行日志
4. **逐步测试**: 先测试服务健康状态，再测试OCR功能

### 性能监控

使用main方法进行性能测试:
```bash
java -cp your-classpath RapidOcrClient "test.jpg" "http://192.168.0.100:9005"
```

输出包含:
- 健康检查耗时
- OCR识别耗时  
- 处理速度 (KB/s)
- 详细的错误信息和建议

## 性能优化建议

1. **合理设置并发数**: 根据RapidOCR服务性能调整`max-concurrency`
2. **启用连接池**: OkHttp客户端自动管理连接池
3. **批量处理**: 使用`recognizeFiles`方法批量处理多个文件
4. **缓存结果**: 对相同图像的识别结果进行缓存
5. **异步处理**: 对于大量图像，考虑使用异步处理方式

## 扩展功能

如需要额外功能，可以扩展以下类：
- `RapidOcrClient`: 添加新的API调用方法
- `RapidOcrService`: 添加业务逻辑处理
- `RapidOcrController`: 添加新的REST接口
