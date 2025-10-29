package com.zhaoxinms.contract.tools.common.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhaoxinms.contract.tools.common.model.CleanupRequest;
import com.zhaoxinms.contract.tools.common.model.CleanupResult;
import com.zhaoxinms.contract.tools.common.service.SystemCleanupService;

/**
 * 系统清理控制器
 * 
 * 功能：
 * 1. 预览清理 - 查看会删除哪些数据
 * 2. 执行清理 - 实际删除数据
 * 3. 获取支持的模块列表
 * 
 * @author AI Assistant
 * @since 2025-10-29
 */
@RestController
@RequestMapping("/api/system/cleanup")
public class SystemCleanupController {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemCleanupController.class);
    
    @Autowired
    private SystemCleanupService cleanupService;
    
    /**
     * 获取支持的模块列表
     * 
     * GET /api/system/cleanup/modules
     * 
     * 响应示例：
     * {
     *   "modules": [
     *     "rule-extract",
     *     "compare-pro",
     *     "ocr-extract",
     *     "compose",
     *     "onlyoffice-demo",
     *     "temp-uploads",
     *     "file-info"
     *   ]
     * }
     */
    @GetMapping("/modules")
    public ApiResponse<List<String>> getSupportedModules() {
        try {
            List<String> modules = cleanupService.getSupportedModules();
            return ApiResponse.success(modules);
        } catch (Exception e) {
            logger.error("获取支持的模块列表失败", e);
            return ApiResponse.error("获取支持的模块列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 预览清理
     * 
     * POST /api/system/cleanup/preview
     * 
     * 请求示例：
     * {
     *   "modules": ["rule-extract", "compare-pro"],
     *   "startDate": "2023-01-01",
     *   "endDate": "2023-12-31",
     *   "cleanDatabase": true,
     *   "cleanFileSystem": true
     * }
     * 
     * 响应：返回清理预览结果，不实际删除数据
     */
    @PostMapping("/preview")
    public ApiResponse<CleanupResult> preview(@RequestBody CleanupRequest request) {
        logger.info("========== 清理预览请求 ==========");
        logger.info("时间范围: {} 至 {}", request.getStartDate(), request.getEndDate());
        logger.info("模块: {}", request.getModules());
        
        try {
            // 强制设置为预览模式
            request.setMode("preview");
            
            CleanupResult result = cleanupService.cleanup(request);
            
            logger.info("预览完成: 文件{}个 ({}MB), 数据库{}条", 
                result.getFileSystemStat().getDeletedFiles(),
                String.format("%.2f", result.getFileSystemStat().getDeletedSize() / (1024.0 * 1024.0)),
                result.getDatabaseStat().getDeletedRecords());
            
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            logger.warn("预览请求参数错误: {}", e.getMessage());
            return ApiResponse.error("参数错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("预览失败", e);
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行清理
     * 
     * POST /api/system/cleanup/execute
     * 
     * 请求示例：
     * {
     *   "modules": ["rule-extract", "compare-pro"],
     *   "startDate": "2023-01-01",
     *   "endDate": "2023-12-31",
     *   "cleanDatabase": true,
     *   "cleanFileSystem": true,
     *   "confirmed": true
     * }
     * 
     * 注意：
     * 1. 必须设置 confirmed=true 才能执行
     * 2. 此操作不可逆，请谨慎使用
     * 3. 建议先使用 /preview 接口查看要删除的数据
     */
    @PostMapping("/execute")
    public ApiResponse<CleanupResult> execute(@RequestBody CleanupRequest request) {
        logger.info("========== 清理执行请求 ==========");
        logger.info("时间范围: {} 至 {}", request.getStartDate(), request.getEndDate());
        logger.info("模块: {}", request.getModules());
        logger.info("清理数据库: {}, 清理文件系统: {}", request.getCleanDatabase(), request.getCleanFileSystem());
        logger.info("确认状态: {}", request.getConfirmed());
        
        try {
            // 强制设置为执行模式
            request.setMode("execute");
            
            // 验证确认标识
            if (!Boolean.TRUE.equals(request.getConfirmed())) {
                return ApiResponse.error("执行清理必须确认操作（confirmed=true）");
            }
            
            CleanupResult result = cleanupService.cleanup(request);
            
            if (result.isSuccess()) {
                logger.info("清理完成: 文件{}个 ({}MB), 数据库{}条", 
                    result.getFileSystemStat().getDeletedFiles(),
                    String.format("%.2f", result.getFileSystemStat().getDeletedSize() / (1024.0 * 1024.0)),
                    result.getDatabaseStat().getDeletedRecords());
            } else {
                logger.error("清理失败: {}", result.getMessage());
            }
            
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            logger.warn("清理请求参数错误: {}", e.getMessage());
            return ApiResponse.error("参数错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("清理失败", e);
            return ApiResponse.error("清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 统一API响应格式
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("success");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
        
        // Getters and Setters
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
}

