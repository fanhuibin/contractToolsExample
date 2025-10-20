package com.zhaoxinms.contract.tools.api.common;

import lombok.Getter;

/**
 * 统一API状态码
 * 
 * 状态码设计规范：
 * - 200: 成功
 * - 400-499: 客户端错误
 * - 500-599: 服务器错误
 * - 业务错误码: 10000+（按模块划分）
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Getter
public enum ApiCode {
    
    // ==================== 通用状态码 (200-599) ====================
    
    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 未认证
     */
    UNAUTHORIZED(401, "未认证，请先登录"),
    
    /**
     * 无权限
     */
    FORBIDDEN(403, "权限不足，无法访问"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "请求的资源不存在"),
    
    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),
    
    /**
     * 资源冲突
     */
    CONFLICT(409, "资源冲突"),
    
    /**
     * 请求频率过高
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    
    /**
     * 服务器内部错误
     */
    SERVER_ERROR(500, "服务器内部错误"),
    
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    
    // ==================== 业务错误码 (10000+) ====================
    
    /**
     * 通用业务错误
     */
    BUSINESS_ERROR(10000, "业务处理失败"),
    
    // ---------- 智能文档抽取 (11000-11999) ----------
    
    /**
     * 模板不存在
     */
    TEMPLATE_NOT_FOUND(11001, "抽取模板不存在"),
    
    /**
     * 模板格式错误
     */
    TEMPLATE_FORMAT_ERROR(11002, "抽取模板格式错误"),
    
    /**
     * 抽取任务不存在
     */
    EXTRACT_TASK_NOT_FOUND(11003, "抽取任务不存在"),
    
    /**
     * 抽取任务执行失败
     */
    EXTRACT_TASK_FAILED(11004, "抽取任务执行失败"),
    
    /**
     * OCR识别失败
     */
    OCR_RECOGNITION_FAILED(11005, "OCR识别失败"),
    
    // ---------- 智能文档比对 (12000-12999) ----------
    
    /**
     * 比对任务不存在
     */
    COMPARE_TASK_NOT_FOUND(12001, "比对任务不存在"),
    
    /**
     * 比对任务执行失败
     */
    COMPARE_TASK_FAILED(12002, "比对任务执行失败"),
    
    /**
     * 文档格式不支持
     */
    DOCUMENT_FORMAT_NOT_SUPPORTED(12003, "文档格式不支持"),
    
    /**
     * GPU服务不可用
     */
    GPU_SERVICE_UNAVAILABLE(12004, "GPU服务不可用"),
    
    // ---------- 智能合同合成 (13000-13999) ----------
    
    /**
     * 合成模板不存在
     */
    COMPOSE_TEMPLATE_NOT_FOUND(13001, "合成模板不存在"),
    
    /**
     * 合成任务失败
     */
    COMPOSE_TASK_FAILED(13002, "合成任务失败"),
    
    /**
     * 合成失败
     */
    COMPOSE_FAILED(13003, "合成失败"),
    
    /**
     * 模板占位符不匹配
     */
    TEMPLATE_PLACEHOLDER_MISMATCH(13004, "模板占位符与数据不匹配"),
    
    // ---------- 智能文档解析 (14000-14999) ----------
    
    /**
     * 文档解析失败
     */
    DOCUMENT_PARSE_FAILED(14001, "文档解析失败"),
    
    /**
     * MinerU服务不可用
     */
    MINERU_SERVICE_UNAVAILABLE(14002, "MinerU服务不可用"),
    
    /**
     * 文档页数超限
     */
    DOCUMENT_PAGE_LIMIT_EXCEEDED(14003, "文档页数超过限制"),
    
    /**
     * 解析任务不存在
     */
    PARSE_TASK_NOT_FOUND(14004, "解析任务不存在"),
    
    /**
     * 解析结果不存在
     */
    PARSE_RESULT_NOT_FOUND(14005, "解析结果不存在"),
    
    // ---------- 文档在线编辑 (15000-15999) ----------
    
    /**
     * OnlyOffice服务不可用
     */
    ONLYOFFICE_SERVICE_UNAVAILABLE(15001, "OnlyOffice服务不可用"),
    
    /**
     * 文档锁定中
     */
    DOCUMENT_LOCKED(15002, "文档正在被其他用户编辑"),
    
    /**
     * 文档保存失败
     */
    DOCUMENT_SAVE_FAILED(15003, "文档保存失败"),
    
    // ---------- 文档格式转换 (16000-16999) ----------
    
    /**
     * 格式转换失败
     */
    FORMAT_CONVERT_FAILED(16001, "文档格式转换失败"),
    
    /**
     * 文档转换失败
     */
    CONVERT_FAILED(16002, "文档转换失败"),
    
    /**
     * 源格式不支持
     */
    SOURCE_FORMAT_NOT_SUPPORTED(16003, "源文档格式不支持"),
    
    /**
     * 目标格式不支持
     */
    TARGET_FORMAT_NOT_SUPPORTED(16004, "目标文档格式不支持"),
    
    // ---------- 文件上传/下载 (17000-17999) ----------
    
    /**
     * 文件为空
     */
    FILE_EMPTY(17001, "上传的文件为空"),
    
    /**
     * 文件大小超限
     */
    FILE_SIZE_EXCEEDED(17002, "文件大小超过限制"),
    
    /**
     * 文件类型不支持
     */
    FILE_TYPE_NOT_SUPPORTED(17003, "文件类型不支持"),
    
    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(17004, "文件上传失败"),
    
    /**
     * 文件上传错误
     */
    FILE_UPLOAD_ERROR(17005, "文件上传错误"),
    
    /**
     * 文件读取错误
     */
    FILE_READ_ERROR(17006, "文件读取错误"),
    
    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(17007, "文件不存在"),
    
    // ---------- 授权相关 (18000-18999) ----------
    
    /**
     * License无效
     */
    LICENSE_INVALID(18001, "License无效"),
    
    /**
     * License已过期
     */
    LICENSE_EXPIRED(18002, "License已过期"),
    
    /**
     * 模块未授权
     */
    MODULE_UNAUTHORIZED(18003, "该模块未授权"),
    
    /**
     * 硬件信息不匹配
     */
    HARDWARE_MISMATCH(18004, "硬件信息不匹配"),
    
    /**
     * 用户数量超限
     */
    USER_LIMIT_EXCEEDED(18005, "授权用户数量超过限制");
    
    private final Integer code;
    private final String message;
    
    ApiCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 根据错误码获取枚举
     */
    public static ApiCode fromCode(Integer code) {
        for (ApiCode apiCode : ApiCode.values()) {
            if (apiCode.getCode().equals(code)) {
                return apiCode;
            }
        }
        return null;
    }
}

