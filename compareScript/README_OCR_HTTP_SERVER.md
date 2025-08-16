# RapidOCR HTTP服务使用说明

## 概述

RapidOCR HTTP服务是一个基于Flask的REST API服务，提供OCR文本识别功能。Java等客户端可以通过HTTP接口调用OCR服务，而不需要直接执行Python命令行。

## 功能特性

✅ **完整的REST API接口** - 支持所有OCR操作
✅ **异步任务处理** - 支持长时间运行的OCR任务
✅ **文件源支持** - 支持本地文件和HTTP URL
✅ **进度监控** - 实时查询任务进度
✅ **结果下载** - 支持多种格式的结果文件
✅ **历史管理** - 查询和清理历史任务
✅ **智能GPU检测** - 自动检测GPU支持，自动选择CPU/GPU版本
✅ **配置灵活** - 可配置端口、GPU设置等
✅ **一键安装** - 智能依赖安装脚本，自动处理版本兼容性

## 安装依赖

### 方法1: 智能安装（推荐）

#### Windows用户
```bash
cd compareScript
install_dependencies.bat
```

#### Linux/macOS用户
```bash
cd compareScript
python install_dependencies.py
```

### 方法2: 手动安装

#### 基础依赖
```bash
pip install flask flask-cors requests PyMuPDF opencv-python numpy
```

#### OCR引擎（选择其中一个）
```bash
# GPU版本（推荐，需要CUDA环境）
pip install rapidocr-onnxruntime-gpu

# CPU版本（备用）
pip install rapidocr-onnxruntime
```

### 3. 验证安装

```bash
python -c "import flask, fitz, cv2, numpy, rapidocr_onnxruntime; print('依赖安装成功')"
```

## 启动服务

### 方法1: 使用启动脚本（推荐）

```bash
cd compareScript
python start_ocr_server.py
```

### 方法2: 直接启动

```bash
cd compareScript
python rapid_pdf_ocr_server.py --port 9898 --gpu
```

### 启动参数

- `--port`: 服务端口（默认: 9898）
- `--host`: 服务地址（默认: 0.0.0.0）
- `--gpu`: 启用GPU加速
- `--cpu-only`: 强制使用CPU
- `--gpu-memory-limit`: GPU内存限制(GB)

## API接口说明

### 1. 健康检查

```http
GET /health
```

**响应示例:**
```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00",
  "service": "RapidOCR HTTP Server"
}
```

### 2. 提交OCR任务

```http
POST /api/ocr/submit
Content-Type: application/json
```

**请求参数:**
```json
{
  "file_source": "local",           // "local" 或 "http"
  "file_path": "/path/to/file.pdf", // 本地文件路径或HTTP URL
  "file_type": "pdf",               // "pdf" 或 "image"
  "options": {                      // OCR选项
    "dpi": 150,                     // 图像DPI
    "min_score": 0.5                // 最小置信度
  }
}
```

**响应示例:**
```json
{
  "success": true,
  "task_id": "uuid-task-id",
  "message": "OCR任务已提交"
}
```

### 3. 查询任务状态

```http
GET /api/ocr/status/{task_id}
```

**响应示例:**
```json
{
  "success": true,
  "task": {
    "id": "uuid-task-id",
    "status": "processing",
    "progress": 45.5,
    "current_step": "处理第2页",
    "total_pages": 3,
    "current_page": 2,
    "created_time": "2024-01-15T10:30:00",
    "start_time": "2024-01-15T10:30:05"
  }
}
```

### 4. 获取OCR结果

```http
GET /api/ocr/result/{task_id}
```

**响应示例:**
```json
{
  "success": true,
  "task": { ... },
  "result": {
    "json_data": {
      "pdf": "/path/to/file.pdf",
      "pages": [
        {
          "page_index": 1,
          "image_width": 1654,
          "image_height": 2340,
          "dpi": 150,
          "items": [
            {
              "text": "识别出的文本",
              "score": 0.95,
              "box": [[x1,y1], [x2,y2], [x3,y3], [x4,y4]],
              "chars": [
                {
                  "ch": "字",
                  "box": [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]
                }
              ]
            }
          ]
        }
      ]
    },
    "text_content": "===== Page 1 =====\n识别出的文本内容...",
    "result_path": "/path/to/results/task-id"
  }
}
```

### 5. 查询历史任务

```http
GET /api/ocr/history?page=1&size=20&status=completed
```

**查询参数:**
- `page`: 页码（默认: 1）
- `size`: 每页大小（默认: 20）
- `status`: 状态过滤（可选）

**响应示例:**
```json
{
  "success": true,
  "data": {
    "tasks": [ ... ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 45,
      "pages": 3
    }
  }
}
```

### 6. 清除历史数据

```http
POST /api/ocr/clear
Content-Type: application/json
```

**请求参数:**
```json
{
  "clear_all": true              // 清除所有任务
}
```

或者：

```json
{
  "task_ids": ["id1", "id2"]    // 清除指定任务
}
```

### 7. 下载结果文件

```http
GET /api/ocr/download/{task_id}/{file_type}
```

**文件类型:**
- `combined`: 合并文本文件
- `json`: JSON结果文件
- `page_001`: 第1页文本文件
- `page_002`: 第2页文本文件

## Java客户端调用示例

### 1. 提交OCR任务

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class OCRClient {
    private static final String BASE_URL = "http://localhost:9898";
    
    public String submitOCRTask(String filePath, String fileType) throws Exception {
        String jsonBody = String.format("""
            {
                "file_source": "local",
                "file_path": "%s",
                "file_type": "%s",
                "options": {
                    "dpi": 150,
                    "min_score": 0.5
                }
            }
            """, filePath, fileType);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/ocr/submit"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        // 解析响应获取task_id
        // 这里需要解析JSON响应
        return "task_id_from_response";
    }
    
    public String getTaskStatus(String taskId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/ocr/status/" + taskId))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        return response.body();
    }
    
    public String getOCRResult(String taskId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/ocr/result/" + taskId))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        return response.body();
    }
}
```

### 2. 完整的工作流程

```java
public class OCRWorkflow {
    public void processDocument(String filePath) {
        try {
            // 1. 提交OCR任务
            String taskId = ocrClient.submitOCRTask(filePath, "pdf");
            System.out.println("OCR任务已提交: " + taskId);
            
            // 2. 轮询任务状态
            while (true) {
                String statusResponse = ocrClient.getTaskStatus(taskId);
                // 解析状态响应
                if (isTaskCompleted(statusResponse)) {
                    break;
                }
                
                Thread.sleep(2000); // 等待2秒
            }
            
            // 3. 获取OCR结果
            String resultResponse = ocrClient.getOCRResult(taskId);
            System.out.println("OCR结果: " + resultResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 配置说明

### 1. 服务配置

编辑 `ocr_server_config.yml` 文件：

```yaml
server:
  host: "0.0.0.0"  # 服务地址
  port: 9898        # 服务端口

ocr:
  use_gpu: true     # 启用GPU加速
  dpi: 150          # 默认DPI
```

### 2. 环境变量

```bash
export OCR_SERVER_PORT=9898
export OCR_USE_GPU=true
export OCR_GPU_MEMORY_LIMIT=4
```

## 故障排除

### 1. 服务启动失败

**问题**: 端口被占用
**解决**: 修改端口号或停止占用端口的服务

```bash
# 查看端口占用
netstat -an | grep 9898

# 修改端口
python rapid_pdf_ocr_server.py --port 9899
```

### 2. OCR识别失败

**问题**: 依赖库缺失
**解决**: 安装必要的依赖

```bash
pip install rapidocr-onnxruntime-gpu
pip install PyMuPDF opencv-python
```

### 3. GPU不可用

**问题**: CUDA环境配置问题
**解决**: 检查CUDA安装和GPU驱动

```bash
# 强制使用CPU
python rapid_pdf_ocr_server.py --cpu-only
```

### 4. 文件下载失败

**问题**: HTTP URL无效或网络问题
**解决**: 检查URL和网络连接

```bash
# 测试URL可访问性
curl -I "http://example.com/file.pdf"
```

## 性能优化

### 1. GPU配置

```bash
# 限制GPU内存使用
python rapid_pdf_ocr_server.py --gpu --gpu-memory-limit 4
```

### 2. 并发控制

修改配置文件中的并发设置：

```yaml
tasks:
  max_concurrent: 3  # 限制并发任务数
```

### 3. 文件缓存

对于重复的HTTP文件，可以实现本地缓存机制。

## 监控和日志

### 1. 日志文件

- 服务日志: `ocr_server.log`
- 任务日志: 在结果目录中

### 2. 健康检查

定期调用 `/health` 接口监控服务状态。

### 3. 任务监控

通过 `/api/ocr/history` 接口监控任务执行情况。

## 安全考虑

### 1. 访问控制

在生产环境中，建议添加身份验证和授权机制。

### 2. 文件验证

服务会验证文件类型和大小，防止恶意文件上传。

### 3. 网络隔离

建议在内部网络中运行OCR服务，避免直接暴露到公网。

## 总结

RapidOCR HTTP服务提供了完整的OCR功能，支持Java等客户端通过HTTP接口调用。主要优势包括：

- **易于集成**: 标准的REST API接口
- **异步处理**: 支持长时间运行的OCR任务
- **灵活配置**: 可配置GPU、DPI等参数
- **完整功能**: 包含任务管理、进度监控、结果下载等
- **高性能**: 支持GPU加速和并发处理

通过这个服务，Java应用可以轻松集成OCR功能，提升文档处理能力。
