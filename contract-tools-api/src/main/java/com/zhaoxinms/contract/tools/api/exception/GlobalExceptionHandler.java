package com.zhaoxinms.contract.tools.api.exception;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * 统一处理所有异常，转换为标准API响应格式
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Value("${spring.profiles.active:prod}")
    private String activeProfile;
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.warn("业务异常: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.fail(ex.getCode(), ex.getMessage());
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 参数校验异常 - @Valid注解
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("参数校验失败: {} - {}", request.getRequestURI(), message);
        
        ApiResponse<Void> response = ApiResponse.paramError(message);
        response.setTraceId(getTraceId(request));
        
        if (isDev()) {
            response.setErrorDetail(ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("参数绑定失败: {} - {}", request.getRequestURI(), message);
        
        ApiResponse<Void> response = ApiResponse.paramError(message);
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 约束违反异常 - @Validated注解
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("约束违反: {} - {}", request.getRequestURI(), message);
        
        ApiResponse<Void> response = ApiResponse.paramError(message);
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String message = String.format("缺少必需参数: %s", ex.getParameterName());
        log.warn("缺少参数: {} - {}", request.getRequestURI(), message);
        
        ApiResponse<Void> response = ApiResponse.paramError(message);
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String message = String.format("参数类型错误: %s 应为 %s 类型", 
                ex.getName(), ex.getRequiredType().getSimpleName());
        log.warn("参数类型错误: {} - {}", request.getRequestURI(), message);
        
        ApiResponse<Void> response = ApiResponse.paramError(message);
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        log.warn("文件大小超限: {}", request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.fail(ApiCode.FILE_SIZE_EXCEEDED);
        response.setTraceId(getTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 404 资源不存在异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        log.warn("资源不存在: {}", request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.fail(ApiCode.NOT_FOUND);
        response.setTraceId(getTraceId(request));
        return response;
    }
    
    /**
     * 运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        // 检查是否为授权/权限相关异常
        String message = ex.getMessage();
        if (message != null && (message.contains("权限") || message.contains("授权"))) {
            log.warn("授权异常: {} - {}", request.getRequestURI(), message);
            
            // 返回业务错误（403），包含具体的错误消息
            ApiResponse<Void> response = ApiResponse.<Void>businessError(message);
            response.setTraceId(getTraceId(request));
            
            // 使用200状态码，让前端能正常处理业务错误
            return ResponseEntity.ok(response);
        }
        
        log.error("运行时异常: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.serverError();
        response.setTraceId(getTraceId(request));
        
        if (isDev()) {
            response.setErrorDetail(ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 通用异常（兜底处理）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) {
        
        log.error("系统异常: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.serverError();
        response.setTraceId(getTraceId(request));
        
        if (isDev()) {
            response.setErrorDetail(ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 获取请求追踪ID
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = (String) request.getAttribute("traceId");
        }
        return traceId;
    }
    
    /**
     * 判断是否为开发环境
     */
    private boolean isDev() {
        return "dev".equals(activeProfile) || "test".equals(activeProfile);
    }
}

