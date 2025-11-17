package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import com.zhaoxin.tools.demo.service.BaiduAnalyticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示资源控制器
 * 提供演示文档列表和下载功能
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoResourceController {
    
    private final BaiduAnalyticsService baiduAnalyticsService;
    
    @Value("${server.port:8091}")
    private String serverPort;
    
    @Value("${zhaoxin.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${zhaoxin.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Value("${zhaoxin.demo.backend-url:http://192.168.0.10:8091}")
    private String fieldsConfigBaseUrl;
    
    /**
     * 获取演示文档列表
     */
    @GetMapping("/documents")
    public ApiResponse<List<DemoDocument>> getDemoDocuments() {
        List<DemoDocument> documents = new ArrayList<>();
        
        // 智能文档抽取 - 演示文档（关联历史抽取任务）
        documents.add(new DemoDocument(
            "extract_demo_1",
            "采购合同（文字版）抽取",
            null,
            "extract",
            null,
            "202511_ec86c4fece1d4a52b37c58f52783f0a4",
            "演示从采购合同中智能抽取合同基本信息、和明细表格等多列数据"
        ));
        
        documents.add(new DemoDocument(
            "extract_demo_2",
            "采购合同（扫描件）抽取",
            null,
            "extract",
            null,
            "202511_fa9d0a3f102047d2838b4ca80e16788e",
            "演示从采购合同中的pdf扫描件中智能抽取合同基本信息、和明细表格等多列数据"
        ));
        
        documents.add(new DemoDocument(
            "extract_demo_3",
            "租赁收费信息抽取",
            null,
            "extract",
            null,
            "202511_40957a29dbf74a3f87db5d2b25e419a0",
            "演示从租赁合同中精准抽取收费明细，包括租金、押金、物业费、水电费等多项费用及计算方式"
        ));
        
        // 智能文档比对 - 演示文档（关联历史比对任务）
        documents.add(new DemoDocument(
            "compare_demo_1",
            "合同与模板的比对",
            null,
            "compare",
            null,
            "202511_01711957-865a-4633-956b-8b2f8b2abf36",
            "演示空白模板与填写后内容的差异比对"
        ));
        
        documents.add(new DemoDocument(
            "compare_demo_2",
            "合同不同版本比对",
            null,
            "compare",
            null,
            "202511_c5c42590-c1ea-44e5-9292-567c870a35ed",
            "演示同一文档修订前后的版本差异比对"
        ));
        
        documents.add(new DemoDocument(
            "compare_demo_3",
            "盖章版与定稿版比对",
            null,
            "compare",
            null,
            "202511_3469037e-e69e-4638-b692-906b20bc6dbe",
            "演示盖章后文档与定稿版本的差异比对"
        ));
        
        // 智能合同合成 - 演示模板（使用模板编号，支持多版本）
        documents.add(new DemoDocument(
            "compose_demo_1",
            "文字带样式合成",
            null,
            "compose",
            null,
            null,
            "caigou",  // 模板编号（多个版本共用同一编号）
            "演示富文本样式合成，支持字体、颜色、对齐方式等格式设置，适用于需要精美排版的合同文档"
        ));
        
        documents.add(new DemoDocument(
            "compose_demo_2",
            "表格数据合成",
            null,
            "compose",
            null,
            null,
            "caigou",  // 模板编号
            "演示表格数据合成，支持动态表格填充，自动处理表头、表体样式，适用于采购清单、费用明细等场景"
        ));
        
        documents.add(new DemoDocument(
            "compose_demo_3",
            "印章条款与附件",
            null,
            "compose",
            null,
            null,
            "caigou",  // 模板编号
            "展示标准公章+骑缝章联动，并支持条款变量、相对方信息及 PDF 附件合并"
        ));
        
        return ApiResponse.success(documents);
    }
    
    /**
     * 获取前端配置
     * 返回前端需要的配置信息，包括API地址、前端地址等
     */
    @GetMapping("/config")
    public ApiResponse<FrontendConfig> getFrontendConfig() {
        FrontendConfig config = new FrontendConfig();
        config.setFrontendUrl(frontendUrl);
        config.setApiBaseUrl(apiBaseUrl);
        config.setCustomFieldsBaseUrl(fieldsConfigBaseUrl);
        return ApiResponse.success(config);
    }
    
    /**
     * 下载演示文档
     */
    @GetMapping("/documents/download")
    public ResponseEntity<byte[]> downloadDemoDocument(@RequestParam String path) {
        try {
            log.info("下载演示文档: {}", path);
            
            // 从 classpath 读取文件
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            Resource resource = new ClassPathResource(normalizedPath);
            
            if (!resource.exists()) {
                log.warn("演示文档不存在: {}", path);
                return ResponseEntity.notFound().build();
            }
            
            byte[] data;
            try (InputStream inputStream = resource.getInputStream()) {
                data = StreamUtils.copyToByteArray(inputStream);
            }
            
            // 提取文件名
            String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
            String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentType(mediaType);
            ContentDisposition contentDisposition = ContentDisposition.inline()
                    .filename(encodedFileName, StandardCharsets.UTF_8)
                    .build();
            headers.setContentDisposition(contentDisposition);
            headers.set("filename*", "UTF-8''" + encodedFileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
                    
        } catch (IOException e) {
            log.error("下载演示文档失败: {}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 前端配置数据模型
     */
    @Data
    public static class FrontendConfig {
        private String frontendUrl;  // 肇新SDK前端地址
        private String apiBaseUrl;   // 肇新SDK后端API地址
        private String customFieldsBaseUrl; // 自定义字段配置访问基址
    }
    
    /**
     * 演示文档数据模型
     */
    @Data
    public static class DemoDocument {
        private String id;
        private String name;
        private String filePath;
        private String category;  // extract, compare, compose
        private String templateId;  // 关联的模板ID（用于抽取和合成，向后兼容）
        private String templateCode;  // 关联的模板编号（用于合成，推荐使用，支持多版本）
        private String taskId;  // 关联的任务ID（用于比对）
        private String description;
        
        public DemoDocument(String id, String name, String filePath, String category, 
                           String templateId, String description) {
            this(id, name, filePath, category, templateId, null, null, description);
        }
        
        public DemoDocument(String id, String name, String filePath, String category, 
                           String templateId, String taskId, String description) {
            this(id, name, filePath, category, templateId, taskId, null, description);
        }
        
        public DemoDocument(String id, String name, String filePath, String category, 
                           String templateId, String taskId, String templateCode, String description) {
            this.id = id;
            this.name = name;
            this.filePath = filePath;
            this.category = category;
            this.templateId = templateId;
            this.taskId = taskId;
            this.templateCode = templateCode;
            this.description = description;
        }
    }
    
    // ==================== 百度统计API ====================
    
    /**
     * 获取百度统计JavaScript代码
     */
    @GetMapping("/baidu-analytics/js-code")
    public ApiResponse<String> getBaiduAnalyticsJsCode() {
        try {
            String jsCode = baiduAnalyticsService.getJavaScriptCode();
            return ApiResponse.success(jsCode);
        } catch (Exception e) {
            log.error("获取百度统计代码失败", e);
            return ApiResponse.error("获取代码失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查百度统计状态
     */
    @GetMapping("/baidu-analytics/status")
    public ApiResponse<AnalyticsInfo> getBaiduAnalyticsStatus() {
        try {
            AnalyticsInfo info = new AnalyticsInfo();
            info.setEnabled(baiduAnalyticsService.isEnabled());
            info.setSiteId(baiduAnalyticsService.getSiteId());
            return ApiResponse.success(info);
        } catch (Exception e) {
            log.error("获取百度统计状态失败", e);
            return ApiResponse.error("获取状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 百度统计信息
     */
    @Data
    public static class AnalyticsInfo {
        private Boolean enabled;
        private String siteId;
    }
}

