# OCR 功能测试说明

## 问题分析

原始的 `JavaOCRExample.java` 类存在以下问题：

1. **Spring 依赖注入问题**：直接使用 `new` 关键字创建对象，而不是通过 Spring 容器，导致 `@Autowired` 注解无法生效
2. **配置加载问题**：`OCRProperties` 需要通过 Spring 的 `@ConfigurationProperties` 来加载配置文件
3. **服务初始化问题**：`OCRTaskService` 中的 `ocrProperties` 为 `null`，导致 `NullPointerException`

## 解决方案

### 方案1：使用 JUnit 测试（推荐）

如果您想使用 JUnit 测试，需要：

1. 确保项目中有 `spring-boot-starter-test` 依赖（已存在）
2. 创建测试目录：`backend/src/test/java/`
3. 在测试类中使用 `@SpringBootTest` 和 `@Autowired` 注解

### 方案2：手动依赖注入（当前实现）

我创建了 `JavaOCRManualTest.java` 类，它：

1. 不依赖 Spring 容器
2. 手动创建所有必要的对象
3. 使用反射设置私有字段，模拟依赖注入
4. 可以直接运行 `main` 方法进行测试

## 使用方法

### 运行手动测试类

```bash
# 编译项目
mvn compile

# 运行测试类
java -cp "target/classes;target/dependency/*" com.zhaoxinms.contract.tools.ocrcompare.JavaOCRManualTest
```

### 新功能特性

**自动轮询监控**：
- 任务提交后自动开始监控
- **每秒轮询一次**任务状态
- 持续监控直到任务完成
- 自动获取并显示OCR识别结果
- 无需手动干预，全自动完成

**智能结果展示**：
- 显示任务进度和状态
- 任务完成后自动读取结果
- 展示识别文字和置信度
- 分页显示识别内容

### 测试方法说明

1. **testAutoOCRTask()** - **自动OCR任务测试（推荐）**：提交任务并自动轮询直到完成，获取识别结果
2. **testSubmitOCRTask()** - 测试提交OCR任务
3. **testQueryTaskStatus(taskId)** - 测试查询任务状态
4. **testMonitorTaskProgress(taskId)** - 测试监控任务进度（1秒轮询一次，直到完成）
5. **testWaitForTaskCompletion(taskId)** - 测试等待任务完成
6. **testOCRTaskServiceDirect()** - 测试OCR任务服务直接调用

### 配置说明

在 `JavaOCRManualTest.java` 中修改以下配置：

```java
// 测试用的PDF文件路径
private static final String TEST_PDF_PATH = "D:\\paddleOCR\\paddleOCR\\test.pdf";

// Python环境根目录
python.setRoot("D:\\paddleOCR\\paddleOCR");
```

## 注意事项

1. **PDF文件路径**：确保 `TEST_PDF_PATH` 指向一个真实存在的PDF文件
2. **Python环境**：确保 `python.setRoot()` 指向正确的PaddleOCR安装目录
3. **依赖关系**：测试类会自动处理所有依赖关系，无需手动配置

## 故障排除

### 如果仍然出现 NullPointerException

1. 检查控制台输出，确认OCR组件初始化是否成功
2. 检查PDF文件路径是否正确
3. 检查Python环境路径是否正确
4. 查看详细的错误堆栈信息

### 如果OCR任务执行失败

1. 检查Python脚本是否存在
2. 检查Python环境是否正确配置
3. 检查PaddleOCR是否正确安装
4. 查看任务错误信息

## 下一步

1. 测试基本功能是否正常
2. 根据实际需求调整配置参数
3. 集成到您的应用程序中
4. 考虑使用Spring Boot的测试框架进行更完整的测试

## 联系支持

如果遇到问题，请：

1. 查看控制台错误信息
2. 检查配置文件设置
3. 确认环境依赖是否正确安装
4. 提供详细的错误日志
