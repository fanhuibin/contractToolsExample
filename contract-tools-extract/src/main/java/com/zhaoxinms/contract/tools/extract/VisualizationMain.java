package com.zhaoxinms.contract.tools.extract;

import com.zhaoxinms.contract.tools.extract.config.ConfigManager;
import com.zhaoxinms.contract.tools.extract.config.LLMConfig;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.exceptions.ExtractException;
import com.zhaoxinms.contract.tools.extract.utils.SchemaBuilder;
import com.zhaoxinms.contract.tools.extract.visualization.SimpleVisualization;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可视化主程序
 * 支持阿里云千问Plus和本地Ollama模型（包括DeepSeek-R1:8b）生成文本提取的可视化HTML报告
 * 使用YAML配置文件管理LLM提供者设置
 * 
 * 使用方法：
 * 1. 修改 DEFAULT_TEST_FILE 常量来指定要测试的文件路径
 * 2. 设置 USE_FILE_MODE = true 使用文件模式，false 使用内置测试文本
 * 3. 调整其他配置参数来控制可视化效果
 * 4. 运行程序生成HTML可视化报告
 * 
 * 命令行使用：
 * mvn exec:java -Dexec.mainClass="...VisualizationMain" -Dexec.args="文件路径"
 */
@Slf4j
public class VisualizationMain {
    
    // ================================
    // 配置参数 - 在这里修改测试文件和选项
    // ================================
    
    /**
     * 默认测试文件路径 - 修改这里来测试不同的文件
     */
    private static final String DEFAULT_TEST_FILE = "D:\\git\\zhaoxin-contract-tool-set\\sdk\\uploads\\compare-pro\\tasks\\0f9eff5d-72f7-43dc-9a43-49968ebe43d2\\new_大连二手房手写.pdf.extracted.compare.txt";
    
    /**
     * 多轮提取的轮数
     */
    private static final int EXTRACTION_PASSES = 3;
    
    public static void main(String[] args) {
        try {
            log.info("🚀 开始生成LangExtract可视化报告...");
            
            // 1. 从配置文件初始化LangExtract
            initializeLLMProvider();
            
            // 2. 准备测试文档 - 直接使用默认文件
            Document testDocument = createDocumentFromDefaultFile();
            log.info("📄 测试文档已准备，长度: {} 字符", testDocument.getContent().length());
            
            // 3. 创建提取模式
            ExtractionSchema schema = SchemaBuilder.createContractSchema();
            log.info("📋 使用合同信息提取模式，包含 {} 个字段", schema.getFields().size());
            
            // 4. 执行提取（多轮提取提高准确性）
            log.info("🔍 开始执行{}轮提取...", EXTRACTION_PASSES);
            List<Extraction> extractions = LangExtract.extractWithMultiplePasses(
                testDocument.getContent(), 
                schema, 
                EXTRACTION_PASSES
            );
            log.info("✅ 提取完成，获得 {} 个结果", extractions.size());
            
            // 5. 生成可视化HTML
            log.info("🎨 生成可视化HTML报告...");
            String htmlContent = SimpleVisualization.generateSimpleHTML(
                testDocument, 
                schema, 
                extractions
            );
            
            // 6. 保存HTML文件
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("langextract_visualization_%s.html", timestamp);
            String filePath = System.getProperty("user.dir") + "/" + fileName;
            
            SimpleVisualization.saveToFile(htmlContent, filePath);
            
            // 7. 输出统计信息
            printStatistics(extractions);
            
            log.info("🎉 可视化报告生成完成！");
            log.info("📁 文件位置: {}", filePath);
            log.info("🌐 请在浏览器中打开查看可视化结果");
            
        } catch (Exception e) {
            log.error("❌ 生成可视化报告失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * 从默认文件创建文档对象
     */
    private static Document createDocumentFromDefaultFile() throws IOException {
        // 直接使用配置的默认测试文件
        if (!Files.exists(Paths.get(DEFAULT_TEST_FILE))) {
            throw new IOException("配置的默认文件不存在: " + DEFAULT_TEST_FILE + 
                "。请检查文件路径是否正确。");
        }
        
        String documentContent = readTextFromFile(DEFAULT_TEST_FILE);
        String sourceDescription = "默认测试文件: " + DEFAULT_TEST_FILE;
        String documentId = "file-" + Paths.get(DEFAULT_TEST_FILE).getFileName().toString();
        
        log.info("📄 文档来源: {}", sourceDescription);
        log.info("📊 文档长度: {} 字符", documentContent.length());
        
        // 创建元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", sourceDescription);
        metadata.put("length", documentContent.length());
        metadata.put("timestamp", System.currentTimeMillis());
        
        return Document.builder()
            .id(documentId)
            .content(documentContent)
            .type("contract")
            .metadata(metadata)
            .build();
    }
    
    /**
     * 从文件读取文本内容
     */
    private static String readTextFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        log.info("📖 正在读取文件: {}", filePath);
        byte[] bytes = Files.readAllBytes(path);
        
        String content;
        try {
            content = new String(bytes, "UTF-8");
        } catch (Exception e1) {
            try {
                content = new String(bytes, "GBK");
                log.info("使用GBK编码读取文件");
            } catch (Exception e2) {
                content = new String(bytes);
                log.info("使用系统默认编码读取文件");
            }
        }
        
        log.info("✅ 文件读取完成，内容长度: {} 字符", content.length());
        return content;
    }
    
      /**
       * 打印提取统计信息
       */
    private static void printStatistics(List<Extraction> extractions) {
        log.info("\n" + "=".repeat(60));
        log.info("📊 提取结果统计");
        log.info("=".repeat(60));
        log.info("总提取字段数: {}", extractions.size());
        
        long withPositionCount = extractions.stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .count();
        double positionPercentage = extractions.isEmpty() ? 0 : (double) withPositionCount / extractions.size() * 100;
        log.info("成功定位字段数: {} (%.1f%%)", withPositionCount, positionPercentage);
        
        long highConfidenceCount = extractions.stream()
            .filter(e -> e.isConfidentEnough(0.8))
            .count();
        double highConfPercentage = extractions.isEmpty() ? 0 : (double) highConfidenceCount / extractions.size() * 100;
        log.info("高置信度字段数: {} (%.1f%%)", highConfidenceCount, highConfPercentage);
        
        double avgConfidence = extractions.stream()
            .mapToDouble(e -> e.getConfidence() != null ? e.getConfidence() : 0.0)
            .average().orElse(0.0);
        log.info("平均置信度: %.1f%%", avgConfidence * 100);
        
        double avgAlignmentConf = extractions.stream()
            .filter(e -> e.getAlignmentConfidence() != null)
            .mapToDouble(Extraction::getAlignmentConfidence)
            .average().orElse(0.0);
        log.info("平均对齐置信度: %.1f%%", avgAlignmentConf * 100);
        
        log.info("\n📋 提取字段详情:");
        for (Extraction extraction : extractions) {
            String positionInfo = "未定位";
            if (extraction.getCharInterval() != null && extraction.getCharInterval().isValid()) {
                CharInterval interval = extraction.getCharInterval();
                positionInfo = String.format("%d-%d", interval.getStartPos(), interval.getEndPos());
            }
            
            double confidence = extraction.getConfidence() != null ? extraction.getConfidence() * 100 : 0.0;
            log.info("  • {}: {} [{}] (置信度: %.1f%%)", 
                extraction.getField(),
                extraction.getValue(),
                positionInfo,
                confidence
            );
        }
        
        log.info("=".repeat(60));
    }
    
    /**
     * 从配置文件初始化LLM提供者
     */
    private static void initializeLLMProvider() throws ExtractException {
        LLMConfig config = ConfigManager.getLLMConfig();
        String provider = config.getProvider();
        
        log.info("📋 读取配置文件，LLM提供者: {}", provider);
        
        if ("ollama".equals(provider)) {
            initializeOllamaProvider(config.getOllama());
        } else if ("aliyun".equals(provider)) {
            initializeAliyunProvider(config.getAliyun());
        } else {
            log.warn("未知的LLM提供者: {}, 使用默认Ollama提供者", provider);
            initializeOllamaProvider(config.getOllama());
        }
    }
    
    /**
     * 初始化Ollama LLM提供者
     */
    private static void initializeOllamaProvider(LLMConfig.OllamaConfig config) throws ExtractException {
        try {
            log.info("🤖 正在初始化Ollama提供者，模型: {}, 地址: {}", config.getModel(), config.getBaseUrl());
            LangExtract.setOllamaProvider(config.getBaseUrl(), config.getModel());
            log.info("✅ 已成功连接DeepSeek-R1模型: {} at {}", config.getModel(), config.getBaseUrl());
            log.info("💡 支持DeepSeek-R1思考内容自动过滤，确保JSON解析正常");
        } catch (Exception e) {
            log.error("❌ Ollama连接失败，尝试切换到阿里云模型", e);
            // 如果Ollama失败，尝试阿里云
            LLMConfig fallbackConfig = ConfigManager.getLLMConfig();
            initializeAliyunProvider(fallbackConfig.getAliyun());
        }
    }
    
    /**
     * 初始化阿里云LLM提供者
     */
    private static void initializeAliyunProvider(LLMConfig.AliyunConfig config) {
        LangExtract.setAliyunProvider(config.getApiKey(), config.getModel());
        log.info("✅ 已连接阿里云千问Plus模型: {}", config.getModel());
    }
}
