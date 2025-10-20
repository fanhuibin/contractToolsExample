package com.zhaoxinms.contract.template.sdk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Swagger访问拦截器
 * 
 * 用于实现Swagger文档的密码保护功能
 * 
 * @author zhaoxin
 * @since 2024-10-18
 */
@Component
public class SwaggerInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SwaggerInterceptor.class);

    @Autowired
    private SwaggerProperties swaggerProperties;

    /**
     * Session中标记已认证的属性名
     */
    private static final String SWAGGER_AUTH_FLAG = "SWAGGER_AUTHENTICATED";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 如果未启用Swagger或不需要密码，直接放行
        if (!swaggerProperties.isEnabled() || !swaggerProperties.isRequirePassword()) {
            return true;
        }

        String uri = request.getRequestURI();
        
        // 只拦截Swagger相关路径
        if (!isSwaggerPath(uri)) {
            return true;
        }

        HttpSession session = request.getSession();
        
        // 检查是否已认证
        Boolean authenticated = (Boolean) session.getAttribute(SWAGGER_AUTH_FLAG);
        if (authenticated != null && authenticated) {
            return true;
        }

        // 检查请求参数中的密码
        String password = request.getParameter("password");
        if (password != null && password.equals(swaggerProperties.getPassword())) {
            // 密码正确，标记为已认证
            session.setAttribute(SWAGGER_AUTH_FLAG, true);
            log.info("Swagger文档访问认证成功，来自IP: {}", getClientIp(request));
            return true;
        }

        // 认证失败，返回登录页面或错误提示
        handleAuthenticationFailure(request, response, password != null);
        return false;
    }

    /**
     * 判断是否是Swagger相关路径
     */
    private boolean isSwaggerPath(String uri) {
        return uri.contains("/swagger-ui") 
            || uri.contains("/swagger-resources") 
            || uri.contains("/v2/api-docs")
            || uri.contains("/webjars/springfox-swagger-ui");
    }

    /**
     * 处理认证失败
     */
    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, boolean passwordProvided) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        PrintWriter out = response.getWriter();
        
        if (passwordProvided) {
            // 密码错误
            log.warn("Swagger文档访问认证失败（密码错误），来自IP: {}", getClientIp(request));
            out.println(buildAuthenticationPage("密码错误，请重新输入", true));
        } else {
            // 需要输入密码
            log.info("Swagger文档访问需要密码认证，来自IP: {}", getClientIp(request));
            out.println(buildAuthenticationPage("请输入访问密码", false));
        }
        
        out.flush();
    }

    /**
     * 构建认证页面HTML
     */
    private String buildAuthenticationPage(String message, boolean isError) {
        String errorStyle = isError ? "color: red;" : "color: #666;";
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Swagger文档访问认证</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            height: 100vh;\n" +
                "        }\n" +
                "        .auth-container {\n" +
                "            background: white;\n" +
                "            padding: 40px;\n" +
                "            border-radius: 10px;\n" +
                "            box-shadow: 0 10px 40px rgba(0,0,0,0.2);\n" +
                "            width: 90%;\n" +
                "            max-width: 400px;\n" +
                "        }\n" +
                "        h2 {\n" +
                "            text-align: center;\n" +
                "            color: #333;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .subtitle {\n" +
                "            text-align: center;\n" +
                "            color: #999;\n" +
                "            font-size: 14px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .message {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "            font-size: 14px;\n" +
                "            " + errorStyle + "\n" +
                "        }\n" +
                "        .form-group {\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        label {\n" +
                "            display: block;\n" +
                "            margin-bottom: 8px;\n" +
                "            color: #555;\n" +
                "            font-size: 14px;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "        input[type=\"password\"] {\n" +
                "            width: 100%;\n" +
                "            padding: 12px;\n" +
                "            border: 2px solid #e0e0e0;\n" +
                "            border-radius: 5px;\n" +
                "            font-size: 14px;\n" +
                "            transition: border-color 0.3s;\n" +
                "        }\n" +
                "        input[type=\"password\"]:focus {\n" +
                "            outline: none;\n" +
                "            border-color: #667eea;\n" +
                "        }\n" +
                "        button {\n" +
                "            width: 100%;\n" +
                "            padding: 12px;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            border: none;\n" +
                "            border-radius: 5px;\n" +
                "            font-size: 16px;\n" +
                "            font-weight: 600;\n" +
                "            cursor: pointer;\n" +
                "            transition: transform 0.2s;\n" +
                "        }\n" +
                "        button:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "        }\n" +
                "        .footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            color: #999;\n" +
                "            font-size: 12px;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"auth-container\">\n" +
                "        <h2>🔐 Swagger API文档</h2>\n" +
                "        <div class=\"subtitle\">" + swaggerProperties.getCompany().getName() + "</div>\n" +
                "        <div class=\"message\">" + message + "</div>\n" +
                "        <form method=\"get\" action=\"\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label for=\"password\">访问密码</label>\n" +
                "                <input type=\"password\" id=\"password\" name=\"password\" placeholder=\"请输入访问密码\" required autofocus>\n" +
                "            </div>\n" +
                "            <button type=\"submit\">访问文档</button>\n" +
                "        </form>\n" +
                "        <div class=\"footer\">请联系管理员获取访问密码</div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个IP时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

