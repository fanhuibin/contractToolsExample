package com.zhaoxinms.contract.tools.extract.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置管理器
 * 负责加载和管理YAML配置文件
 */
@Slf4j
public class ConfigManager {
    
    private static final String CONFIG_FILE_NAME = "extract-config.yml";
    private static final String DEFAULT_CONFIG_PATH = "config/" + CONFIG_FILE_NAME;
    
    private static LLMConfig cachedConfig;
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * 获取LLM配置
     */
    public static LLMConfig getLLMConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfig();
        }
        return cachedConfig;
    }
    
    /**
     * 加载配置文件
     */
    private static LLMConfig loadConfig() {
        try {
            // 1. 首先尝试从当前目录加载
            Path configPath = Paths.get(CONFIG_FILE_NAME);
            if (Files.exists(configPath)) {
                log.info("从当前目录加载配置文件: {}", configPath.toAbsolutePath());
                return yamlMapper.readValue(configPath.toFile(), LLMConfig.class);
            }
            
            // 2. 尝试从config目录加载
            configPath = Paths.get("config", CONFIG_FILE_NAME);
            if (Files.exists(configPath)) {
                log.info("从config目录加载配置文件: {}", configPath.toAbsolutePath());
                return yamlMapper.readValue(configPath.toFile(), LLMConfig.class);
            }
            
            // 3. 尝试从classpath加载
            InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH);
            if (inputStream != null) {
                log.info("从classpath加载配置文件: {}", DEFAULT_CONFIG_PATH);
                return yamlMapper.readValue(inputStream, LLMConfig.class);
            }
            
            // 4. 如果都找不到，创建默认配置文件
            log.warn("未找到配置文件，创建默认配置: {}", CONFIG_FILE_NAME);
            return createDefaultConfig();
            
        } catch (IOException e) {
            log.error("加载配置文件失败，使用默认配置", e);
            return new LLMConfig();
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private static LLMConfig createDefaultConfig() {
        try {
            LLMConfig defaultConfig = new LLMConfig();
            
            // 创建默认配置文件
            File configFile = new File(CONFIG_FILE_NAME);
            yamlMapper.writeValue(configFile, defaultConfig);
            
            log.info("已创建默认配置文件: {}", configFile.getAbsolutePath());
            return defaultConfig;
            
        } catch (IOException e) {
            log.error("创建默认配置文件失败", e);
            return new LLMConfig();
        }
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig(LLMConfig config) {
        try {
            File configFile = new File(CONFIG_FILE_NAME);
            yamlMapper.writeValue(configFile, config);
            cachedConfig = config; // 更新缓存
            log.info("配置已保存到: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存配置文件失败", e);
        }
    }
    
    /**
     * 重新加载配置
     */
    public static void reloadConfig() {
        cachedConfig = null;
        log.info("配置已重新加载");
    }
}
