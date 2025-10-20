package com.zhaoxinms.contract.tools.api.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 请求追踪ID拦截器
 * 
 * 为每个请求生成唯一的TraceID，用于日志追踪和问题排查
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 尝试从请求头获取TraceID（支持客户端传递）
        String traceId = request.getHeader(TRACE_ID_HEADER);
        
        // 如果没有则生成新的TraceID
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        
        // 设置到MDC，用于日志打印
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        
        // 设置到Request属性，供全局异常处理器使用
        request.setAttribute("traceId", traceId);
        
        // 设置到响应头，返回给客户端
        response.setHeader(TRACE_ID_HEADER, traceId);
        
        // 记录请求日志
        log.info("请求开始 - Method: {}, URI: {}, TraceID: {}", 
                request.getMethod(), request.getRequestURI(), traceId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 记录响应日志
        String traceId = (String) request.getAttribute("traceId");
        log.info("请求完成 - Status: {}, TraceID: {}", response.getStatus(), traceId);
        
        // 清理MDC，防止内存泄漏
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}

