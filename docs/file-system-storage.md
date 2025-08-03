# 文件系统存储说明

## 概述

本系统采用纯文件系统存储，不依赖任何数据库。所有文件信息和元数据都存储在文件系统中，确保系统的轻量化和部署简单性。

## 存储结构

```
uploads/
├── file_metadata.json          # 文件元数据存储文件
├── 2024/
│   ├── 01/
│   │   ├── 15/
│   │   │   ├── abc123.docx     # 实际文件
│   │   │   └── def456.pdf
│   │   └── 16/
│   │       └── ghi789.xlsx
│   └── 02/
│       └── 01/
│           └── jkl012.docx
└── ...
```

## 核心组件

### 1. 雪花ID生成器 (SnowflakeIdGenerator)

- 基于Twitter的雪花算法实现
- 生成64位的唯一ID
- 支持分布式环境下的ID生成
- 时间戳精度到毫秒

**ID结构**：
- 1位符号位（始终为0）
- 41位时间戳
- 10位机器ID（5位数据中心ID + 5位工作机器ID）
- 12位序列号

### 2. 文件系统存储管理器 (FileSystemStorageManager)

- 使用JSON文件存储文件元数据
- 实现读写锁保证并发安全
- 内存缓存提高访问性能
- 文件锁确保数据一致性

**并发处理**：
- 使用 `ReentrantReadWriteLock` 实现读写分离
- 使用 `FileChannel` 和 `FileLock` 确保文件操作的原子性
- 使用 `ConcurrentHashMap` 作为内存缓存

### 3. 文件管理工具 (FileManager)

- 处理实际文件的存储、删除、重命名等操作
- 支持文件大小限制
- 自动创建目录结构
- 生成唯一文件名

## 文件信息结构

```json
{
  "id": 1234567890123456789,
  "originalName": "合同文档.docx",
  "fileName": "abc123.docx",
  "filePath": "2024/01/15/abc123.docx",
  "fileSize": 1024000,
  "fileType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "fileExtension": ".docx",
  "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
  "status": 0,
  "uploadTime": "2024-01-15T10:30:00",
  "updateTime": "2024-01-15T10:30:00",
  "remark": "合同文档"
}
```

## 配置说明

### application.yml 配置

```yaml
file:
  upload:
    root-path: ./uploads                    # 文件存储根目录
    max-size: 104857600                     # 最大文件大小（100MB）
  storage:
    metadata-file: file_metadata.json       # 元数据文件名
```

## 并发安全机制

### 1. 读写锁分离

```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

// 读操作
lock.readLock().lock();
try {
    // 读取操作
} finally {
    lock.readLock().unlock();
}

// 写操作
lock.writeLock().lock();
try {
    // 写入操作
} finally {
    lock.writeLock().unlock();
}
```

### 2. 文件锁

```java
try (FileChannel channel = FileChannel.open(metadataPath, 
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
     FileLock lock = channel.lock()) {
    // 文件操作
}
```

### 3. 内存缓存

```java
private final Map<Long, FileInfo> fileInfoCache = new ConcurrentHashMap<>();
```

## 性能优化

### 1. 缓存策略

- 使用内存缓存减少文件I/O操作
- 缓存未命中时自动从文件系统加载
- 写操作时同步更新缓存和文件系统

### 2. 批量操作

- 支持批量文件操作
- 减少文件锁的获取和释放次数

### 3. 异步处理

- 文件上传和删除操作支持异步处理
- 提高系统响应性能

## 数据一致性

### 1. 事务性保证

- 文件操作和元数据更新保持一致性
- 失败时自动回滚操作

### 2. 备份策略

- 定期备份元数据文件
- 支持元数据文件的导入导出

### 3. 错误恢复

- 文件损坏时自动重建元数据
- 支持从文件系统扫描恢复元数据

## 使用示例

### 1. 上传文件

```java
@PostMapping("/upload")
public Result<FileInfo> uploadFile(@RequestParam("file") MultipartFile file) {
    try {
        FileInfo fileInfo = fileInfoService.uploadFile(file);
        return Result.success(fileInfo);
    } catch (IOException e) {
        return Result.error("文件上传失败：" + e.getMessage());
    }
}
```

### 2. 分页查询

```java
@GetMapping("/page")
public Result<Map<String, Object>> getFilePage(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
    Map<String, Object> result = fileInfoService.getFileInfoPage(page, size);
    return Result.success(result);
}
```

### 3. 搜索文件

```java
@GetMapping("/search")
public Result<List<FileInfo>> searchFiles(@RequestParam String originalName) {
    List<FileInfo> files = fileInfoService.searchByOriginalName(originalName);
    return Result.success(files);
}
```

## 注意事项

1. **文件大小限制**：确保上传文件不超过配置的最大大小
2. **存储空间**：定期清理无用文件，避免存储空间不足
3. **备份策略**：定期备份元数据文件，防止数据丢失
4. **并发访问**：大量并发访问时注意性能影响
5. **文件权限**：确保应用有足够的文件系统权限

## 扩展功能

### 1. 文件版本控制

- 支持文件版本管理
- 保留历史版本信息

### 2. 文件分类

- 支持文件分类管理
- 按类型、大小、时间等维度分类

### 3. 文件压缩

- 支持大文件压缩存储
- 减少存储空间占用

### 4. 分布式存储

- 支持多节点分布式存储
- 提高存储容量和性能 