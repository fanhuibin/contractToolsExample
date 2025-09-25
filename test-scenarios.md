# 测试场景验证

## 场景1：测试Backend独立应用

```bash
cd contract-tools-backend
mvn spring-boot:run
```

应该能看到Spring Boot启动日志，并且可以访问：
- http://localhost:8080/api/actuator/health

## 场景2：测试SDK应用

```bash
cd contract-tools-sdk  
mvn spring-boot:run -Dspring.profiles.active=test
```

应该能看到Spring Boot启动日志，包含AI组件和数据库相关的配置。

## 场景3：作为jar库使用

创建一个新的Spring Boot项目，在pom.xml中添加：

```xml
<dependency>
    <groupId>com.zhaoxinms</groupId>
    <artifactId>contract-tools-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

然后就可以使用核心功能：

```java
@Autowired
private TemplateDesignService templateDesignService;

@Autowired  
private CompareService compareService;
```

## 验证打包结果

各模块的jar包大小和类型：

1. **contract-tools-core-1.0.0.jar**: ~519KB - 核心业务库
2. **contract-tools-auth-1.0.0.jar**: ~10KB - 授权管理模块
3. **contract-tools-backend-1.0.0.jar**: ~5KB - 独立应用（thin jar，依赖外部）
4. **contract-tools-sdk-1.0.0.jar**: ~337KB - SDK应用

注意：backend应该生成fat jar（包含所有依赖），让我检查配置。
