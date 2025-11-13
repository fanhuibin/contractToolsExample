package com.zhaoxinms.contract.template.sdk.config;

import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Backend配置类
 * 从SDK配置中读取zxcm前缀的配置，传递给Backend项目
 */
@Configuration
public class BackendConfig {

    @Value("${zxcm.http-client.trust-all-certificates:false}")
    private boolean trustAllCertificates;

    @Value("${zxcm.http-client.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${zxcm.http-client.read-timeout:60000}")
    private int readTimeout;

    /**
     * 创建ZxcmConfig实例，从配置文件读取zxcm前缀的配置
     */
    @Bean
    @ConfigurationProperties(prefix = "zxcm")
    public ZxcmConfig zxcmConfig() {
        return new ZxcmConfig();
    }
    
    /**
     * 创建RestTemplate实例，支持HTTPS和自签名证书
     */
    @Bean
    public RestTemplate restTemplate() throws Exception {
        if (trustAllCertificates) {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // 信任所有客户端证书
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // 信任所有服务器证书
                    }
                }
            };

            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建忽略主机名验证的HostnameVerifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // 设置默认的SSL上下文和主机名验证器
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            // 创建请求工厂并设置超时
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);

            return new RestTemplate(requestFactory);
        } else {
            // 使用默认的RestTemplate
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            return new RestTemplate(requestFactory);
        }
    }
} 