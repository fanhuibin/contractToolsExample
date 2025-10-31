package com.zhaoxin.tools.demo.service;

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
    public Map<String, Object> getTemplates() {
        String url = baseUrl + "/api/rule-extract/templates";
        
        try {
            log.info("获取模板列表");
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            throw new RuntimeException("获取模板列表失败: " + e.getMessage(), e);
        }
    }
}

