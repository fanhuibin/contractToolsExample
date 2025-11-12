package com.zhaoxinms.contract.tools.compare.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 字符映射配置管理器
 * 用于从配置文件加载和管理字符映射规则
 */
public class CharacterMappingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterMappingConfig.class);
    
    /**
     * 默认配置文件名
     */
    private static final String DEFAULT_CONFIG_FILE = "character-mapping.properties";
    
    /**
     * jar包同级目录下的配置文件路径
     */
    private static final String SAME_LEVEL_CONFIG_PATH = DEFAULT_CONFIG_FILE;
    
    /**
     * config文件夹下的配置文件路径
     */
    private static final String CONFIG_DIR_CONFIG_PATH = "config/" + DEFAULT_CONFIG_FILE;
    
    /**
     * 字符映射表（原始映射）
     */
    private Map<String, String> rawMappingMap = new HashMap<>();
    
    /**
     * 验证后的字符映射表
     */
    private Map<String, String> validatedMappingMap = new HashMap<>();
    
    /**
     * 单例实例
     */
    private static volatile CharacterMappingConfig instance;
    
    /**
     * 私有构造函数
     */
    private CharacterMappingConfig() {
        loadConfig();
    }
    
    /**
     * 获取单例实例
     * 
     * @return 配置实例
     */
    public static CharacterMappingConfig getInstance() {
        if (instance == null) {
            synchronized (CharacterMappingConfig.class) {
                if (instance == null) {
                    instance = new CharacterMappingConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            // 1. 首先尝试加载jar包同级目录下的配置文件
            if (loadSameLevelConfig()) {
                logger.info("成功加载jar包同级目录配置文件: {}", SAME_LEVEL_CONFIG_PATH);
            } else if (loadConfigDirConfig()) {
                // 2. 如果同级目录没有，则尝试config文件夹下的配置文件
                logger.info("成功加载config文件夹配置文件: {}", CONFIG_DIR_CONFIG_PATH);
            } else if (loadInternalConfig()) {
                // 3. 如果外部配置文件都不存在，则加载内部默认配置文件
                logger.info("成功加载内部配置文件: {}", DEFAULT_CONFIG_FILE);
            } else {
                // 4. 如果都加载失败，则使用硬编码的默认配置
                loadDefaultConfig();
                logger.warn("配置文件加载失败，使用默认硬编码配置");
            }
            
            // 验证和处理映射规则
            validateAndProcessMappings();
            
        } catch (Exception e) {
            logger.error("加载字符映射配置时发生错误", e);
            loadDefaultConfig();
            validateAndProcessMappings();
        }
    }
    
    /**
     * 加载jar包同级目录下的配置文件
     * 
     * @return 是否成功加载
     */
    private boolean loadSameLevelConfig() {
        File configFile = new File(SAME_LEVEL_CONFIG_PATH);
        if (!configFile.exists()) {
            return false;
        }
        
        return loadExternalConfigFile(configFile, SAME_LEVEL_CONFIG_PATH);
    }
    
    /**
     * 加载config文件夹下的配置文件
     * 
     * @return 是否成功加载
     */
    private boolean loadConfigDirConfig() {
        File configFile = new File(CONFIG_DIR_CONFIG_PATH);
        if (!configFile.exists()) {
            return false;
        }
        
        return loadExternalConfigFile(configFile, CONFIG_DIR_CONFIG_PATH);
    }
    
    /**
     * 加载外部配置文件的通用方法
     * 
     * @param configFile 配置文件对象
     * @param configPath 配置文件路径（用于日志）
     * @return 是否成功加载
     */
    private boolean loadExternalConfigFile(File configFile, String configPath) {
        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            
            Properties properties = new Properties();
            properties.load(isr);
            
            rawMappingMap.clear();
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                if (value != null && !value.trim().isEmpty()) {
                    rawMappingMap.put(key, value);
                }
            }
            
            return true;
            
        } catch (IOException e) {
            logger.error("读取外部配置文件失败: {}", configPath, e);
            return false;
        }
    }
    
    /**
     * 加载内部配置文件
     * 
     * @return 是否成功加载
     */
    private boolean loadInternalConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is == null) {
                return false;
            }
            
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(isr);
                
                rawMappingMap.clear();
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (value != null && !value.trim().isEmpty()) {
                        rawMappingMap.put(key, value);
                    }
                }
                
                return true;
            }
            
        } catch (IOException e) {
            logger.error("读取内部配置文件失败: {}", DEFAULT_CONFIG_FILE, e);
            return false;
        }
    }
    
    /**
     * 加载默认硬编码配置
     */
    private void loadDefaultConfig() {
        rawMappingMap.clear();
        
        // 括号
        rawMappingMap.put("（", "(");
        rawMappingMap.put("）", ")");
        rawMappingMap.put("【", "[");
        rawMappingMap.put("】", "]");
        rawMappingMap.put("｛", "{");
        rawMappingMap.put("｝", "}");
        
        // 标点符号
        rawMappingMap.put("：", ":");
        rawMappingMap.put("；", ";");
        rawMappingMap.put("，", ",");
        rawMappingMap.put("。", ".");
        rawMappingMap.put("．", ".");  // 全角点
        rawMappingMap.put("？", "?");
        rawMappingMap.put("！", "!");
        
        // 引号
        rawMappingMap.put("\u201c", "\"");  // 左双引号
        rawMappingMap.put("\u201d", "\"");  // 右双引号
        rawMappingMap.put("\u2018", "'");   // 左单引号
        rawMappingMap.put("\u2019", "'");   // 右单引号
        rawMappingMap.put("｀", "`");
        
        // 破折号和连接符
        rawMappingMap.put("——", "--");
        rawMappingMap.put("—", "-");
        rawMappingMap.put("－", "-");
        rawMappingMap.put("～", "~");
        
        // 省略号
        rawMappingMap.put("……", "..");
        rawMappingMap.put("…", ".");
        
        // 其他符号
        rawMappingMap.put("、", ".");   // 顿号转点（中文文档常用于列表分隔）
        // rawMappingMap.put(",", ".");  // ❌ 已删除：半角逗号不应该转点，会破坏金额等数据格式
        rawMappingMap.put("·", ".");    // 间隔号转点（常用于列表项）
        rawMappingMap.put("＊", "*");
        rawMappingMap.put("＃", "#");
        rawMappingMap.put("＆", "&");
        rawMappingMap.put("％", "%");
        rawMappingMap.put("＠", "@");
        rawMappingMap.put("＋", "+");
        rawMappingMap.put("＝", "=");
        rawMappingMap.put("＜", "<");
        rawMappingMap.put("＞", ">");
        rawMappingMap.put("｜", "|");
        rawMappingMap.put("＼", "\\");
        rawMappingMap.put("／", "/");
        
        // 全角空格
        rawMappingMap.put("　", " ");  // 全角空格转半角空格
        
        // 全角数字
        rawMappingMap.put("０", "0");
        rawMappingMap.put("１", "1");
        rawMappingMap.put("２", "2");
        rawMappingMap.put("３", "3");
        rawMappingMap.put("４", "4");
        rawMappingMap.put("５", "5");
        rawMappingMap.put("６", "6");
        rawMappingMap.put("７", "7");
        rawMappingMap.put("８", "8");
        rawMappingMap.put("９", "9");
        
        // 全角字母 - 大写
        rawMappingMap.put("Ａ", "A");
        rawMappingMap.put("Ｂ", "B");
        rawMappingMap.put("Ｃ", "C");
        rawMappingMap.put("Ｄ", "D");
        rawMappingMap.put("Ｅ", "E");
        rawMappingMap.put("Ｆ", "F");
        rawMappingMap.put("Ｇ", "G");
        rawMappingMap.put("Ｈ", "H");
        rawMappingMap.put("Ｉ", "I");
        rawMappingMap.put("Ｊ", "J");
        rawMappingMap.put("Ｋ", "K");
        rawMappingMap.put("Ｌ", "L");
        rawMappingMap.put("Ｍ", "M");
        rawMappingMap.put("Ｎ", "N");
        rawMappingMap.put("Ｏ", "O");
        rawMappingMap.put("Ｐ", "P");
        rawMappingMap.put("Ｑ", "Q");
        rawMappingMap.put("Ｒ", "R");
        rawMappingMap.put("Ｓ", "S");
        rawMappingMap.put("Ｔ", "T");
        rawMappingMap.put("Ｕ", "U");
        rawMappingMap.put("Ｖ", "V");
        rawMappingMap.put("Ｗ", "W");
        rawMappingMap.put("Ｘ", "X");
        rawMappingMap.put("Ｙ", "Y");
        rawMappingMap.put("Ｚ", "Z");
        
        // 全角字母 - 小写
        rawMappingMap.put("ａ", "a");
        rawMappingMap.put("ｂ", "b");
        rawMappingMap.put("ｃ", "c");
        rawMappingMap.put("ｄ", "d");
        rawMappingMap.put("ｅ", "e");
        rawMappingMap.put("ｆ", "f");
        rawMappingMap.put("ｇ", "g");
        rawMappingMap.put("ｈ", "h");
        rawMappingMap.put("ｉ", "i");
        rawMappingMap.put("ｊ", "j");
        rawMappingMap.put("ｋ", "k");
        rawMappingMap.put("ｌ", "l");
        rawMappingMap.put("ｍ", "m");
        rawMappingMap.put("ｎ", "n");
        rawMappingMap.put("ｏ", "o");
        rawMappingMap.put("ｐ", "p");
        rawMappingMap.put("ｑ", "q");
        rawMappingMap.put("ｒ", "r");
        rawMappingMap.put("ｓ", "s");
        rawMappingMap.put("ｔ", "t");
        rawMappingMap.put("ｕ", "u");
        rawMappingMap.put("ｖ", "v");
        rawMappingMap.put("ｗ", "w");
        rawMappingMap.put("ｘ", "x");
        rawMappingMap.put("ｙ", "y");
        rawMappingMap.put("ｚ", "z");
        
        // 其他全角标点符号补充
        rawMappingMap.put("＂", "\"");  // 全角双引号
        rawMappingMap.put("＇", "'");   // 全角单引号
        rawMappingMap.put("﹐", ",");   // 小写全角逗号
        rawMappingMap.put("﹑", ",");   // 顿号（变体）
        rawMappingMap.put("﹒", ".");   // 小写全角点
        rawMappingMap.put("﹔", ";");   // 小写全角分号
        rawMappingMap.put("﹕", ":");   // 小写全角冒号
        rawMappingMap.put("﹖", "?");   // 小写全角问号
        rawMappingMap.put("﹗", "!");   // 小写全角叹号
        
        // 特殊符号统一化（保持长度一致）
        rawMappingMap.put("°", " ");   // 度符号 → 空格
        rawMappingMap.put("′", "'");   // 角分符号 → 单引号
        rawMappingMap.put("″", "\"");  // 角秒符号 → 双引号
        rawMappingMap.put("※", "*");   // 参考标记 → 星号
        rawMappingMap.put("§", " ");   // 节标记 → 空格
        rawMappingMap.put("¶", " ");   // 段落标记 → 空格
        rawMappingMap.put("†", "+");   // 剑标 → 加号
        rawMappingMap.put("‡", "+");   // 双剑标 → 加号
        rawMappingMap.put("•", ".");   // 项目符号 → 点
        rawMappingMap.put("◦", ".");   // 空心项目符号 → 点
        rawMappingMap.put("‣", ".");   // 三角项目符号 → 点
        
        //金额常见识别错误
        rawMappingMap.put("貳", "贰");
        rawMappingMap.put("參", "叁");
        rawMappingMap.put("陸", "陆");
        rawMappingMap.put("陌", "佰");
        rawMappingMap.put("萬", "万");
        rawMappingMap.put("億", "亿");
        
        // 更多常见错误项
        rawMappingMap.put("经营商", "经营者");
        rawMappingMap.put("購", "购");
        rawMappingMap.put("羔", "盖");
        rawMappingMap.put("运营", "经营");
        rawMappingMap.put("買", "买");
        rawMappingMap.put("説", "说");
    }
    
    /**
     * 验证和处理映射规则
     * 根据字符长度规则处理映射关系
     */
    private void validateAndProcessMappings() {
        validatedMappingMap.clear();
        
        int validCount = 0;
        int paddedCount = 0;
        int ignoredCount = 0;
        
        for (Map.Entry<String, String> entry : rawMappingMap.entrySet()) {
            String sourceChar = entry.getKey();
            String targetChar = entry.getValue();
            
            if (sourceChar == null || targetChar == null) {
                continue;
            }
            
            int sourceLength = sourceChar.length();
            int targetLength = targetChar.length();
            
            if (sourceLength == targetLength) {
                // 长度相等，直接使用
                validatedMappingMap.put(sourceChar, targetChar);
                validCount++;
                
            } else if (sourceLength > targetLength) {
                // 替换前长，替换后短，补充空格使长度相等
                String paddedTarget = targetChar + " ".repeat(sourceLength - targetLength);
                validatedMappingMap.put(sourceChar, paddedTarget);
                paddedCount++;
                logger.debug("字符映射 '{}' -> '{}' 长度不匹配，已补充空格: '{}'", 
                    sourceChar, targetChar, paddedTarget);
                
            } else {
                // 替换前短，替换后长，忽略此映射
                ignoredCount++;
                logger.warn("字符映射 '{}' -> '{}' 被忽略，因为目标字符长度({})大于源字符长度({})", 
                    sourceChar, targetChar, targetLength, sourceLength);
            }
        }
        
        logger.info("字符映射验证完成 - 有效: {}, 补充空格: {}, 忽略: {}, 总计: {}", 
            validCount, paddedCount, ignoredCount, rawMappingMap.size());
    }
    
    /**
     * 获取验证后的字符映射表
     * 
     * @return 映射表的副本
     */
    public Map<String, String> getMappingMap() {
        return new HashMap<>(validatedMappingMap);
    }
    
    /**
     * 获取字符映射
     * 
     * @param sourceChar 源字符
     * @return 映射后的字符，如果没有映射则返回原字符
     */
    public String getMapping(String sourceChar) {
        return validatedMappingMap.getOrDefault(sourceChar, sourceChar);
    }
    
    /**
     * 重新加载配置文件
     * 可用于在运行时更新配置
     */
    public void reloadConfig() {
        logger.info("重新加载字符映射配置...");
        loadConfig();
        logger.info("字符映射配置重新加载完成，共加载 {} 个有效映射规则", validatedMappingMap.size());
    }
    
    /**
     * 创建外部配置文件示例
     * 将当前配置导出到外部配置文件
     */
    public void createExternalConfigExample() {
        // 优先在jar包同级目录创建配置文件
        createConfigFile(SAME_LEVEL_CONFIG_PATH);
    }
    
    /**
     * 在config文件夹创建配置文件示例
     * 将当前配置导出到config文件夹下的配置文件
     */
    public void createConfigDirExample() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        createConfigFile(CONFIG_DIR_CONFIG_PATH);
    }
    
    /**
     * 创建配置文件的通用方法
     * 
     * @param configPath 配置文件路径
     */
    private void createConfigFile(String configPath) {
        File configFile = new File(configPath);
        
        try (FileOutputStream fos = new FileOutputStream(configFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            
            Properties properties = new Properties();
            for (Map.Entry<String, String> entry : rawMappingMap.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
            
            // 添加配置文件说明
            osw.write("# 字符映射配置文件\n");
            osw.write("# 格式：源字符=目标字符\n");
            osw.write("# 用于统一处理中英文标点符号、数字等，提高文档比对准确性\n");
            osw.write("# 修改此文件后，重启应用或调用重新加载接口即可生效\n");
            osw.write("#\n");
            osw.write("# 长度处理规则：\n");
            osw.write("# 1. 源字符长度 = 目标字符长度：直接替换\n");
            osw.write("# 2. 源字符长度 > 目标字符长度：目标字符后补充空格至相同长度\n");
            osw.write("# 3. 源字符长度 < 目标字符长度：忽略此映射规则\n");
            osw.write("#\n");
            osw.write("# 示例：\n");
            osw.write("# （=( 表示将中文左括号映射为英文左括号\n");
            osw.write("# ——=-- 表示将中文破折号映射为两个英文短横线\n");
            osw.write("#\n");
            
            properties.store(osw, null);
            
            logger.info("外部配置文件示例已创建: {}", configPath);
            
        } catch (IOException e) {
            logger.error("创建外部配置文件失败", e);
        }
    }
    
    /**
     * 获取配置统计信息
     * 
     * @return 配置信息描述
     */
    public String getConfigInfo() {
        return String.format("字符映射配置 - 原始: %d 个, 有效: %d 个映射规则", 
            rawMappingMap.size(), validatedMappingMap.size());
    }
}
