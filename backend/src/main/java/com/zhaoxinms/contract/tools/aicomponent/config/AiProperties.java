package com.zhaoxinms.contract.tools.aicomponent.config;

import com.zhaoxinms.contract.tools.aicomponent.constants.AiConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * AI配置属性
 *
 * @author zhaoxinms
 */
@Data
@Component
@ConfigurationProperties(prefix = AiConstants.CONFIGURATION_PREFIX)
public class AiProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * openai服务器
     */
    private String apiHost = AiConstants.OPENAI_HOST;

    /**
     * openai key
     */
    private List<String> apiKey;

    /**
     * 默认模型
     */
    private String model = "qwen-turbo";

    /**
     * 超时时间
     */
    private Long timeout = 30L;

    /**
     * 每个用户限制时间内的请求次数
     */
    private Integer userLimitCount = 1;

    /**
     * 每个用户限制时间频率
     */
    private Duration userLimitTime = Duration.ofSeconds(3);

    /**
     * 全部请求限制时间内的请求次数
     */
    private Integer totalLimitCount = 500;

    /**
     * 全部请求限制时间频率
     */
    private Duration totalLimitTime = Duration.ofMinutes(1L);

    /**
     * PDF抽取配置
     */
    private PdfConfig pdf = new PdfConfig();

    /**
     * 聊天配置
     */
    private ChatConfig chat = new ChatConfig();

    /**
     * 代理配置
     */
    private ProxyConfig proxy = new ProxyConfig();

    /**
     * PDF配置
     */
    @Data
    public static class PdfConfig {
        /**
         * 是否启用PDF抽取功能
         */
        private boolean enabled = true;

        /**
         * 最大文件大小(字节)
         */
        private Long maxFileSize = 10485760L; // 10MB

        /**
         * 临时文件存储路径
         */
        private String tempDir = System.getProperty("java.io.tmpdir") + "/pdf-extract";

        /**
         * 是否保留临时文件
         */
        private boolean keepTempFiles = false;

        /**
         * 抽取超时时间(毫秒)
         */
        private Long extractTimeout = 60000L;

        /**
         * 最大页数限制
         */
        private Integer maxPages = 500;
    }

    /**
     * 聊天配置
     */
    @Data
    public static class ChatConfig {
        /**
         * 模型名称
         */
        private String mode = "qwen-turbo";

        /**
         * 随机种子
         */
        private Integer seed = 1234;

        /**
         * 最大令牌数
         */
        private Integer maxTokens = 1500;

        /**
         * 核采样概率阈值
         */
        private Double topP = 0.8;

        /**
         * 温度参数(0.0-1.0)
         */
        private Double temperature = 0.85;

        /**
         * 是否启用搜索
         */
        private boolean enableSearch = true;
    }

    /**
     * 代理配置
     */
    @Data
    public static class ProxyConfig {
        /**
         * 代理类型
         */
        private String type = "HTTP";

        /**
         * 代理主机
         */
        private String host;

        /**
         * 代理端口
         */
        private Integer port;

        /**
         * 代理用户名
         */
        private String username;

        /**
         * 代理密码
         */
        private String password;
    }
}