package com.zhaoxinms.contract.tools.aicomponent.config;

import com.zhaoxinms.contract.tools.aicomponent.constants.AiConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * AI 配置
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
     * 代理配置
     */
    private Proxy proxy = new Proxy();

    /**
     * 对话配置
     */
    private ChatOption chat = new ChatOption();

    @Data
    public static class PdfConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 最大文件大小(字节)
         */
        private long maxFileSize = 10 * 1024 * 1024; // 10MB

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
        private int extractTimeout = 60000;

        /**
         * 最大页数限制
         */
        private int maxPages = 500;
    }

    @Data
    public static class ChatOption {

        /**
         * @see AiConstants.Model
         */
        private String mode = AiConstants.Model.QWEN_25_3;

        /**
         * 设置seed参数会使文本生成过程更具有确定性，通常用于使模型每次运行的结果一致。
         * 在每次模型调用时传入相同的seed值（由您指定），并保持其他参数不变，模型将很可能返回相同的结果。
         */
        private Integer seed = 1234;

        /**
         * 允许模型生成的最大Token数。
         */
        private Integer maxTokens = 1500;

        /**
         * 核采样的概率阈值，用于控制模型生成文本的多样性。
         * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         * 由于temperature与top_p均可以控制生成文本的多样性，因此建议您只设置其中一个值。
         */
        private Double topP = 0.8;

        /**
         * 采样温度，用于控制模型生成文本的多样性。
         * temperature越高，生成的文本更多样，反之，生成的文本更确定。
         * 由于temperature与top_p均可以控制生成文本的多样性，因此建议您只设置其中一个值。
         */
        private Double temperature = 0.85;

        private boolean enableSearch = true;

    }

    @Data
    public static class Proxy {
        /**
         * HTTP, SOCKS
         */
        private java.net.Proxy.Type type = java.net.Proxy.Type.HTTP;
        /**
         * 代理域名
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