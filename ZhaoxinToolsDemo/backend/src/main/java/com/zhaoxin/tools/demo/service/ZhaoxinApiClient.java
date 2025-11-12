package com.zhaoxin.tools.demo.service;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 肇新 SDK API 客户端
 * 封装对肇新SDK的所有API调用
 */
@Slf4j
@Service
public class ZhaoxinApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${zhaoxin.api.base-url}")
    private String baseUrl;
    
    public ZhaoxinApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 提交文档抽取任务
     * 
     * @param file PDF文件
     * @param templateId 抽取模板ID
     * @return 任务ID
     */
    public Map<String, Object> submitExtractTask(MultipartFile file, String templateId) {
        String url = baseUrl + "/api/rule-extract/extract/upload";
        
        try {
            // 创建临时文件
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }
            
            // 构建multipart请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));
            body.add("templateId", templateId);
            body.add("ignoreHeaderFooter", "true");
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            log.info("提交文档抽取任务: templateId={}, fileName={}", templateId, file.getOriginalFilename());
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            
            // 删除临时文件
            tempFile.delete();
            
            log.info("任务提交成功: {}", response.getBody());
            return response.getBody();
            
        } catch (IOException e) {
            log.error("提交抽取任务失败", e);
            throw new RuntimeException("提交抽取任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询抽取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public Map<String, Object> getExtractStatus(String taskId) {
        String url = baseUrl + "/api/rule-extract/extract/status/" + taskId;
        
        try {
            log.debug("查询任务状态: taskId={}", taskId);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("查询任务状态失败: taskId={}", taskId, e);
            throw new RuntimeException("查询任务状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取抽取结果
     * 
     * @param taskId 任务ID
     * @return 抽取结果
     */
    public Map<String, Object> getExtractResult(String taskId) {
        String url = baseUrl + "/api/rule-extract/extract/result/" + taskId;
        
        try {
            log.info("获取抽取结果: taskId={}", taskId);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取抽取结果失败: taskId={}", taskId, e);
            throw new RuntimeException("获取抽取结果失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 取消抽取任务
     * 
     * @param taskId 任务ID
     */
    public void cancelExtractTask(String taskId) {
        String url = baseUrl + "/api/rule-extract/extract/cancel/" + taskId;
        
        try {
            log.info("取消任务: taskId={}", taskId);
            restTemplate.postForEntity(url, null, Map.class);
            log.info("任务取消成功: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}", taskId, e);
            throw new RuntimeException("取消任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取任务列表
     * 
     * @return 任务列表
     */
    public Map<String, Object> getExtractTasks() {
        String url = baseUrl + "/api/rule-extract/extract/tasks";
        
        try {
            log.info("获取任务列表");
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            throw new RuntimeException("获取任务列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取页面图片
     * 
     * @param taskId 任务ID
     * @param pageNumber 页码
     * @return 图片字节数组
     */
    public byte[] getPageImage(String taskId, int pageNumber) {
        String url = baseUrl + "/api/rule-extract/extract/page-image/" + taskId + "/" + pageNumber;
        
        try {
            log.debug("获取页面图片: taskId={}, pageNumber={}", taskId, pageNumber);
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取页面图片失败: taskId={}, pageNumber={}", taskId, pageNumber, e);
            throw new RuntimeException("获取页面图片失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取模板列表
     * 
     * @return 模板列表
     */
    public Map<String, Object> getTemplates(String status) {
        String url = baseUrl + "/api/rule-extract/templates";
        
        // 添加status参数
        if (status != null && !status.isEmpty()) {
            url += "?status=" + status;
        }
        
        try {
            log.info("获取模板列表: status={}, url={}", status, url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            throw new RuntimeException("获取模板列表失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 智能文档比对相关 API ====================
    
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
    public ApiResponse submitCompareTask(String oldFileUrl, String newFileUrl, Boolean removeWatermark, 
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
            log.info("提交比对任务: oldFile={}, newFile={}, removeWatermark={}", 
                    oldFileUrl, newFileUrl, removeWatermark);
            
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
            throw new RuntimeException("提交比对任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取比对任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public ApiResponse getCompareTaskStatus(String taskId) {
        String url = baseUrl + "/api/compare-pro/task/" + taskId;
        
        try {
            log.debug("获取比对任务状态: taskId={}", taskId);
            
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                url, 
                ApiResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取比对任务状态失败: taskId={}", taskId, e);
            throw new RuntimeException("获取比对任务状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取比对结果
     * 
     * @param taskId 任务ID
     * @return 比对结果
     */
    public ApiResponse getCompareResult(String taskId) {
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
            throw new RuntimeException("获取比对结果失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除比对任务
     * 
     * @param taskId 任务ID
     */
    public void deleteCompareTask(String taskId) {
        String url = baseUrl + "/api/compare-pro/task/" + taskId;
        
        try {
            log.info("删除比对任务: taskId={}", taskId);
            restTemplate.delete(url);
            log.info("任务删除成功: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("删除比对任务失败: taskId={}", taskId, e);
            throw new RuntimeException("删除比对任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有比对任务历史
     * 
     * @return 任务列表
     */
    public ApiResponse getAllCompareTasks() {
        String url = baseUrl + "/api/compare-pro/tasks";
        
        try {
            log.info("获取比对任务历史列表");
            
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                url, 
                ApiResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取比对任务历史失败", e);
            throw new RuntimeException("获取比对任务历史失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 导出比对报告
     * 
     * @param exportData 导出参数
     * @return 文件二进制数据
     */
    public ResponseEntity<byte[]> exportCompareReport(Map<String, Object> exportData) {
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
            throw new RuntimeException("导出比对报告失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 智能合同合成相关 API ====================
    
    /**
     * 获取合成模板列表
     * 
     * @param status 可选的状态筛选参数：PUBLISHED（已发布）、DRAFT（草稿）、DISABLED（已禁用）、DELETED（已删除）
     * @return 模板列表
     */
    public Map<String, Object> getComposeTemplates(String status) {
        String url = baseUrl + "/api/template/design/list";
        if (status != null && !status.trim().isEmpty()) {
            url += "?status=" + status;
        }
        
        try {
            log.info("获取合成模板列表: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("获取合成模板列表失败", e);
            throw new RuntimeException("获取模板列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取模板详情（包含 elementsJson）
     * 
     * @param templateId 模板ID
     * @return 模板详情
     */
    public Map<String, Object> getTemplateDetail(String templateId) {
        String url = baseUrl + "/api/template/design/detail/" + templateId;
        
        try {
            log.info("获取模板详情: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("获取模板详情失败: templateId={}", templateId, e);
            throw new RuntimeException("获取模板详情失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 合成合同
     */
    public Map<String, Object> generateContract(Map<String, Object> request) {
        String url = baseUrl + "/api/compose/sdt";
        
        try {
            log.info("合成合同: {}", url);
            log.info("请求参数: {}", request);
            
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("合成合同失败", e);
            throw new RuntimeException("合成合同失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载合成的合同文件（通过文件ID）
     */
    public byte[] downloadComposeFile(String fileId) {
        String url = baseUrl + "/api/file/download/" + fileId;
        
        try {
            log.info("下载合同文件: {}", url);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                byte[].class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("下载合同文件失败: fileId={}", fileId, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载文件（通过相对路径）
     * 用于下载 PDF、盖章版、骑缝章版等文件
     */
    public byte[] downloadFileByPath(String path) {
        String url = baseUrl + "/api/file/download-by-path?path=" + path;
        
        try {
            log.info("通过路径下载文件: {}", url);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                byte[].class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("通过路径下载文件失败: path={}", path, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建通用的HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        // 如果需要添加认证token等，可以在这里添加
        // headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}

