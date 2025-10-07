package com.zhaoxinms.contract.tools.extract.debug;

import com.zhaoxinms.contract.tools.extract.LangExtract;
import com.zhaoxinms.contract.tools.extract.config.ConfigManager;
import com.zhaoxinms.contract.tools.extract.config.LLMConfig;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.utils.SchemaBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 提取调试器 - 用于调试字符位置锚定问题
 * 
 * 使用方法：
 * 1. 修改 DEFAULT_TEST_FILE 常量来指定要测试的文件路径
 * 2. 设置 USE_FILE_MODE = true 使用文件模式，false 使用内置测试文本
 * 3. 调整 PREVIEW_LENGTH 来控制预览文本的长度
 * 4. 运行程序查看提取结果和字符位置分析
 * 
 * 命令行使用：
 * mvn exec:java -Dexec.mainClass="...ExtractionDebugger" -Dexec.args="文件路径"
 */
@Slf4j
public class ExtractionDebugger {
    
    // ================================
    // 配置参数 - 在这里修改测试文件
    // ================================
    
    /**
     * 默认测试文件路径 - 修改这里来测试不同的文件
     */
    private static final String DEFAULT_TEST_FILE = "D:\\git\\zhaoxin-contract-tool-set\\sdk\\uploads\\compare-pro\\tasks\\1b658b9a-8f4a-488a-8c06-0339abb3287d\\new_007-建设工程施工合同（2017版）.pdf.extracted.compare.txt";
    
    /**
     * 是否使用文件模式（true）还是内置测试文本（false）
     */
    private static final boolean USE_FILE_MODE = true;
    
    /**
     * 预览文本的字符数
     */
    private static final int PREVIEW_LENGTH = 800;
    
    /**
     * 是否显示详细的关键文本位置分析
     */
    private static final boolean SHOW_DETAILED_TEXT_ANALYSIS = true;
    
    /**
     * 关键文本分析的上下文字符数
     */
    private static final int CONTEXT_LENGTH = 30;
    
    public static void main(String[] args) {
        try {
            // 初始化LLM提供者
            initializeLLMProvider();
            
            // 获取文本内容
            String contractText;
            String sourceDescription;
            
            if (args.length > 0) {
                // 从命令行参数指定的文件读取
                String filePath = args[0];
                contractText = readTextFromFile(filePath);
                sourceDescription = "命令行参数文件: " + filePath;
            } else if (USE_FILE_MODE) {
                // 使用配置的默认测试文件
                if (Files.exists(Paths.get(DEFAULT_TEST_FILE))) {
                    contractText = readTextFromFile(DEFAULT_TEST_FILE);
                    sourceDescription = "配置的默认文件: " + DEFAULT_TEST_FILE;
                } else {
                    log.warn("⚠️  配置的默认文件不存在: {}", DEFAULT_TEST_FILE);
                    log.info("💡 请检查文件路径，或将 USE_FILE_MODE 设为 false 使用内置测试文本");
                    contractText = createTestContract();
                    sourceDescription = "内置测试文本（文件不存在时的备用）";
                }
            } else {
                // 使用内置测试文本
                contractText = createTestContract();
                sourceDescription = "内置测试文本";
            }
            
            log.info("📄 合同文本来源: {}", sourceDescription);
            log.info("=====================================");
            log.info("文本长度: {} 字符", contractText.length());
            log.info("前{}字符预览:", PREVIEW_LENGTH);
            log.info("{}", contractText.length() > PREVIEW_LENGTH ? 
                contractText.substring(0, PREVIEW_LENGTH) + "..." : contractText);
            log.info("=====================================");
            
            // 手动检查关键文本位置
            debugTextPositions(contractText);
            
            // 执行提取
            ExtractionSchema schema = SchemaBuilder.createContractSchema();
            List<Extraction> extractions = LangExtract.extract(contractText, schema);
            
            log.info("🔍 提取结果分析:");
            log.info("=====================================");
            
            for (Extraction extraction : extractions) {
                String fieldName = extraction.getField();
                Object extractedValue = extraction.getValue();
                String sourceTextFromInterval = extraction.getSourceTextFromInterval();
                
                log.info("字段: {}", fieldName);
                log.info("  提取值: '{}'", extractedValue);
                log.info("  源文本: '{}'", sourceTextFromInterval);
                log.info("  置信度: {:.2f}", extraction.getConfidence());
                
                if (extraction.getCharInterval() != null && extraction.getCharInterval().isValid()) {
                    int start = extraction.getCharInterval().getStartPos();
                    int end = extraction.getCharInterval().getEndPos();
                    String sourceText = extraction.getCharInterval().getSourceText();
                    
                    log.info("  字符位置: [{}-{}]", start, end);
                    log.info("  原文片段: '{}'", sourceText);
                    
                    // 验证位置是否正确
                    if (start >= 0 && end <= contractText.length()) {
                        String actualText = contractText.substring(start, end);
                        log.info("  实际文本: '{}'", actualText);
                        
                        if (!actualText.equals(sourceText)) {
                            log.warn("  ⚠️  位置不匹配！原文片段和实际位置文本不一致");
                        }
                        
                        if (!actualText.equals(String.valueOf(extractedValue))) {
                            log.warn("  ⚠️  提取值和位置文本不一致！");
                            log.warn("      提取值: '{}'", extractedValue);
                            log.warn("      位置文本: '{}'", actualText);
                        }
                    } else {
                        log.warn("  ⚠️  位置超出范围！");
                    }
                } else {
                    log.warn("  ⚠️  未找到字符位置");
                }
                
                log.info("  ---");
            }
            
        } catch (Exception e) {
            log.error("调试过程发生错误", e);
        }
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
        
        // 尝试不同的编码
        String content;
        try {
            // 首先尝试UTF-8
            content = new String(bytes, "UTF-8");
        } catch (Exception e1) {
            try {
                // 如果UTF-8失败，尝试GBK
                content = new String(bytes, "GBK");
                log.info("使用GBK编码读取文件");
            } catch (Exception e2) {
                // 最后尝试系统默认编码
                content = new String(bytes);
                log.info("使用系统默认编码读取文件");
            }
        }
        
        log.info("✅ 文件读取完成，内容长度: {} 字符", content.length());
        return content;
    }
    
    private static void initializeLLMProvider() {
        LLMConfig config = ConfigManager.getLLMConfig();
        String provider = config.getProvider();
        
        if ("ollama".equals(provider)) {
            LangExtract.setOllamaProvider(config.getOllama().getBaseUrl(), config.getOllama().getModel());
            log.info("✅ 已连接Ollama模型: {}", config.getOllama().getModel());
        } else if ("aliyun".equals(provider)) {
            LangExtract.setAliyunProvider(config.getAliyun().getApiKey(), config.getAliyun().getModel());
            log.info("✅ 已连接阿里云模型: {}", config.getAliyun().getModel());
        }
    }
    
    private static String createTestContract() {
        return "销售合同\n" +
            "\n" +
            "甲方（卖方）：北京科技发展有限公司\n" +
            "法定代表人：李明\n" +
            "地址：北京市海淀区中关村大街1号\n" +
            "联系电话：010-12345678\n" +
            "\n" +
            "乙方（买方）：上海国际贸易有限公司\n" +
            "法定代表人：王芳\n" +
            "地址：上海市浦东新区陆家嘴环路1000号\n" +
            "联系电话：021-87654321\n" +
            "\n" +
            "根据《中华人民共和国合同法》及相关法律法规，甲乙双方就以下商品买卖事宜达成一致协议：\n" +
            "\n" +
            "第一条 产品信息\n" +
            "产品名称：智能数据分析系统\n" +
            "产品型号：IDA-2024-Pro\n" +
            "数量：10套\n" +
            "单价：人民币50万元/套\n" +
            "总金额：人民币500万元整\n" +
            "\n" +
            "第二条 交付条款\n" +
            "交付地点：乙方指定地址\n" +
            "交付时间：2024年3月15日前\n" +
            "运输方式：甲方负责运输和安装\n" +
            "\n" +
            "第三条 付款方式\n" +
            "1. 合同签署后7日内，乙方支付合同总金额的30%作为预付款；\n" +
            "2. 货物交付验收合格后7日内，乙方支付合同总金额的60%；\n" +
            "3. 质保期满后7日内，乙方支付剩余10%的尾款。\n" +
            "\n" +
            "第四条 质量保证\n" +
            "甲方保证所交付的产品符合国家相关标准和技术要求。\n" +
            "产品质保期为交付验收合格后12个月。\n" +
            "\n" +
            "第五条 违约责任\n" +
            "任何一方违反本合同约定，应承担相应的违约责任。\n" +
            "\n" +
            "第六条 争议解决\n" +
            "因本合同引起的争议，双方应友好协商解决；协商不成的，可向甲方所在地人民法院起诉。\n" +
            "\n" +
            "第七条 其他\n" +
            "本合同一式两份，甲乙双方各执一份，具有同等法律效力。\n" +
            "本合同自双方签字盖章之日起生效。\n" +
            "\n" +
            "甲方（盖章）：________________    乙方（盖章）：________________\n" +
            "\n" +
            "法定代表人签字：______________    法定代表人签字：______________\n" +
            "\n" +
            "签署日期：2024年1月15日        签署日期：2024年1月15日";
    }
    
    private static void debugTextPositions(String text) {
        if (!SHOW_DETAILED_TEXT_ANALYSIS) {
            log.info("🔍 关键文本位置分析已禁用");
            return;
        }
        
        log.info("🔍 关键文本位置分析:");
        
        // 查找常见的合同关键短语
        String[] keyPhrases = {
            // 金额相关
            "万元", "元整", "人民币", "总价", "合同价", "工程造价", "总金额",
            // 日期相关  
            "年", "月", "日", "签订", "签署", "生效",
            // 当事人相关
            "甲方", "乙方", "发包人", "承包人", "委托人", "受托人",
            // 数字模式
            "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
        };
        
        int foundCount = 0;
        for (String phrase : keyPhrases) {
            int index = text.indexOf(phrase);
            if (index != -1) {
                foundCount++;
                // 获取上下文
                int contextStart = Math.max(0, index - CONTEXT_LENGTH);
                int contextEnd = Math.min(text.length(), index + phrase.length() + CONTEXT_LENGTH);
                String context = text.substring(contextStart, contextEnd).replace("\n", "\\n");
                
                log.info("  '{}' 位置: [{}--{}] 上下文: '{}'", 
                    phrase, index, index + phrase.length(), context);
            }
        }
        
        log.info("找到 {} 个关键短语", foundCount);
        log.info("=====================================");
    }
}
