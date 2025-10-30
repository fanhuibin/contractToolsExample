package com.zhaoxinms.contract.tools.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.util.FileUploadValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传拦截器
 * <p>在文件上传前验证文件大小和页数限制</p>
 * 
 * @author 山西肇新科技有限公司
 */
@Slf4j
@Component
public class FileUploadInterceptor implements HandlerInterceptor {

    @Autowired
    private FileUploadValidator fileUploadValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 只处理文件上传请求
        if (!(request instanceof MultipartHttpServletRequest)) {
            return true;
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        
        // 遍历所有上传的文件
        for (Map.Entry<String, MultipartFile> entry : multipartRequest.getFileMap().entrySet()) {
            MultipartFile file = entry.getValue();
            String fileName = file.getOriginalFilename();
            
            // 只验证PDF文件
            if (fileName != null && (fileName.toLowerCase().endsWith(".pdf"))) {
                log.debug("[文件上传拦截器] 验证PDF文件: {}", fileName);
                
                FileUploadValidator.ValidationResult result = fileUploadValidator.validatePdfFile(file);
                
                if (!result.isValid()) {
                    // 验证失败，返回错误信息
                    log.warn("[文件上传拒绝] {}: {}", fileName, result.getErrorMessage());
                    sendErrorResponse(response, result.getErrorMessage());
                    return false;
                }
                
                log.debug("[文件上传验证通过] {}", fileName);
            }
        }

        return true;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 400);
        errorResponse.put("message", errorMessage);
        errorResponse.put("success", false);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

