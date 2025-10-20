package com.zhaoxinms.contract.tools.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一API响应格式
 * 
 * 符合RESTful规范和大厂API设计标准
 * 
 * @author zhaoxin
 * @since 2025-01-18
 * @param <T> 响应数据类型
 */
@Data
@ApiModel("统一API响应")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     * - 200: 成功
     * - 400: 客户端错误（参数错误、业务错误等）
     * - 401: 未认证
     * - 403: 无权限
     * - 404: 资源不存在
     * - 500: 服务器内部错误
     */
    @ApiModelProperty(value = "状态码", required = true, example = "200")
    private Integer code;
    
    /**
     * 响应消息
     */
    @ApiModelProperty(value = "响应消息", required = true, example = "操作成功")
    private String message;
    
    /**
     * 业务数据
     */
    @ApiModelProperty(value = "业务数据")
    private T data;
    
    /**
     * 请求追踪ID（用于日志追踪和问题排查）
     */
    @ApiModelProperty(value = "请求追踪ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String traceId;
    
    /**
     * 响应时间戳
     */
    @ApiModelProperty(value = "响应时间戳", example = "2025-01-18T10:30:00")
    private LocalDateTime timestamp;
    
    /**
     * 详细错误信息（仅在开发/测试环境返回，生产环境不返回）
     */
    @ApiModelProperty(value = "详细错误信息", hidden = true)
    private String errorDetail;
    
    /**
     * 额外的元数据（如分页信息、统计信息等）
     */
    @ApiModelProperty(value = "元数据")
    private Object metadata;
    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApiResponse(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public ApiResponse(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    // ==================== 成功响应 ====================
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage());
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage(), data);
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), message, data);
    }
    
    /**
     * 成功响应（带元数据，如分页信息）
     */
    public static <T> ApiResponse<T> success(T data, Object metadata) {
        ApiResponse<T> response = new ApiResponse<>(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage(), data);
        response.setMetadata(metadata);
        return response;
    }
    
    // ==================== 失败响应 ====================
    
    /**
     * 失败响应（使用错误码枚举）
     */
    public static <T> ApiResponse<T> fail(ApiCode apiCode) {
        return new ApiResponse<>(apiCode.getCode(), apiCode.getMessage());
    }
    
    /**
     * 失败响应（自定义消息）
     */
    public static <T> ApiResponse<T> fail(ApiCode apiCode, String message) {
        return new ApiResponse<>(apiCode.getCode(), message);
    }
    
    /**
     * 失败响应（完全自定义）
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }
    
    // ==================== 特定场景响应 ====================
    
    /**
     * 参数错误
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(ApiCode.PARAM_ERROR.getCode(), message);
    }
    
    /**
     * 业务错误
     */
    public static <T> ApiResponse<T> businessError(String message) {
        return new ApiResponse<>(ApiCode.BUSINESS_ERROR.getCode(), message);
    }
    
    /**
     * 未授权
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(ApiCode.UNAUTHORIZED.getCode(), ApiCode.UNAUTHORIZED.getMessage());
    }
    
    /**
     * 无权限
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(ApiCode.FORBIDDEN.getCode(), ApiCode.FORBIDDEN.getMessage());
    }
    
    /**
     * 资源不存在
     */
    public static <T> ApiResponse<T> notFound(String resource) {
        return new ApiResponse<>(ApiCode.NOT_FOUND.getCode(), resource + "不存在");
    }
    
    /**
     * 服务器错误
     */
    public static <T> ApiResponse<T> serverError() {
        return new ApiResponse<>(ApiCode.SERVER_ERROR.getCode(), ApiCode.SERVER_ERROR.getMessage());
    }
    
    // ==================== 链式调用支持 ====================
    
    public ApiResponse<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
    
    public ApiResponse<T> errorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
        return this;
    }
    
    public ApiResponse<T> metadata(Object metadata) {
        this.metadata = metadata;
        return this;
    }
    
    // ==================== 便捷判断方法 ====================
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ApiCode.SUCCESS.getCode().equals(this.code);
    }
    
    /**
     * 判断是否失败
     */
    public boolean isFail() {
        return !isSuccess();
    }
}

