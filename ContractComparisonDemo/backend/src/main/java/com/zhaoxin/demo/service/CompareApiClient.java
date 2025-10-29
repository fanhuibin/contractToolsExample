package com.zhaoxin.demo.service;

import com.zhaoxin.demo.model.exception.ApiException;
import com.zhaoxin.demo.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 肇新合同比对 API 客户端
 * 
 * 封装所有对肇新 API 的调用
 */
@Slf4j
@Service
public class CompareApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${zhaoxin.api.base-url}")
    private String baseUrl;
    
    public CompareApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 提交比对任务
     * 
     * @param oldFileUrl 原文件URL
     * @param newFileUrl 新文件URL
     * @param removeWatermark 是否去除水印
     * @param oldFileName 原文件名（用于显示）
     * @param newFileName 新文件名（用于显示）
     * @return 任务结果
     */
    public ApiResponse submitCompare(String oldFileUrl, String newFileUrl, Boolean removeWatermark, 
                                     String oldFileName, String newFileName) {
        String url = baseUrl + "/api/compare-pro/submit-url";
        
        Map<String, Object> request = new HashMap<>();
        request.put("oldFileUrl", oldFileUrl);
        request.put("newFileUrl", newFileUrl);
        if (removeWatermark != null) {
            request.put("removeWatermark", removeWatermark);
        }
        // 传递文件名给肇新后端（用于任务列表显示）
        if (oldFileName != null && !oldFileName.isEmpty()) {
            request.put("oldFileName", oldFileName);
        }
        if (newFileName != null && !newFileName.isEmpty()) {
            request.put("newFileName", newFileName);
        }
        
        try {
            log.info("提交比对任务: oldFile={}, newFile={}, oldFileName={}, newFileName={}, removeWatermark={}", 
                    oldFileUrl, newFileUrl, oldFileName, newFileName, removeWatermark);
            
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                url, 
                request, 
                ApiResponse.class
            );
            
            ApiResponse result = response.getBody();
            log.info("任务提交成功: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("提交比对任务失败", e);
            throw new ApiException("提交比对任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public ApiResponse getTaskStatus(String taskId) {
        String url = baseUrl + "/api/compare-pro/task/" + taskId;
        
        try {
            log.debug("获取任务状态: taskId={}", taskId);
            
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                url, 
                ApiResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取任务状态失败: taskId={}", taskId, e);
            throw new ApiException("获取任务状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取比对结果
     * 
     * @param taskId 任务ID
     * @return 比对结果
     */
    public ApiResponse getResult(String taskId) {
        String url = baseUrl + "/api/compare-pro/canvas-result/" + taskId;
        
        try {
            log.info("获取比对结果: taskId={}", taskId);
            
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                url, 
                ApiResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取比对结果失败: taskId={}", taskId, e);
            throw new ApiException("获取比对结果失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除任务
     * 
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        String url = baseUrl + "/api/compare-pro/task/" + taskId;
        
        try {
            log.info("删除任务: taskId={}", taskId);
            restTemplate.delete(url);
            log.info("任务删除成功: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("删除任务失败: taskId={}", taskId, e);
            throw new ApiException("删除任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有任务历史
     * 
     * @return 任务列表
     */
    public ApiResponse getAllTasks() {
        String url = baseUrl + "/api/compare-pro/tasks";
        
        try {
            log.info("获取任务历史列表");
            
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                url, 
                ApiResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取任务历史失败", e);
            throw new ApiException("获取任务历史失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 导出比对报告
     * 
     * @param exportData 导出参数
     * @return 文件二进制数据
     */
    public ResponseEntity<byte[]> exportReport(Map<String, Object> exportData) {
        String url = baseUrl + "/api/compare-pro/export-report";
        
        try {
            log.info("导出比对报告: taskId={}", exportData.get("taskId"));
            
            // 调用肇新后端导出接口
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                url,
                exportData,
                byte[].class
            );
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "比对结果.docx");
            
            log.info("导出成功，文件大小: {} bytes", response.getBody().length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response.getBody());
            
        } catch (Exception e) {
            log.error("导出比对报告失败", e);
            throw new ApiException("导出比对报告失败: " + e.getMessage(), e);
        }
    }
}

