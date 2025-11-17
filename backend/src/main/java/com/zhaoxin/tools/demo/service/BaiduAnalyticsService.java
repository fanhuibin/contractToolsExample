package com.zhaoxin.tools.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 百度统计服务 - 极简版
 * 只需要一个站点ID参数
 * 
 * @author Zhaoxin Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class BaiduAnalyticsService {
    
    @Value("${zhaoxin.baidu-analytics:}")
    private String siteId;
    
    /**
     * 检查是否启用百度统计
     */
    public boolean isEnabled() {
        boolean enabled = StringUtils.hasText(siteId) && siteId.matches("^[a-f0-9]{16}$");
        if (enabled) {
            log.debug("百度统计已启用，站点ID: {}", siteId);
        } else {
            log.debug("百度统计未启用或配置无效");
        }
        return enabled;
    }
    
    /**
     * 获取站点ID
     */
    public String getSiteId() {
        return siteId;
    }
    
    /**
     * 获取百度统计JavaScript代码
     */
    public String getJavaScriptCode() {
        if (!isEnabled()) {
            return "";
        }
        
        String jsCode = String.format(
            "var _hmt = _hmt || [];\n" +
            "(function() {\n" +
            "  var hm = document.createElement(\"script\");\n" +
            "  hm.src = \"https://hm.baidu.com/hm.js?%s\";\n" +
            "  var s = document.getElementsByTagName(\"script\")[0];\n" +
            "  s.parentNode.insertBefore(hm, s);\n" +
            "})();",
            siteId
        );
        
        log.debug("生成百度统计JS代码，站点ID: {}", siteId);
        return jsCode;
    }
    
    /**
     * 获取完整的HTML代码
     */
    public String getTrackingCode() {
        String jsCode = getJavaScriptCode();
        if (jsCode.isEmpty()) {
            return "";
        }
        return "<script>\n" + jsCode + "\n</script>";
    }
}
