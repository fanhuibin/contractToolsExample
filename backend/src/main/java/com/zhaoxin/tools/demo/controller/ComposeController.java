package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import com.zhaoxin.tools.demo.service.ZhaoxinApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 合同合成控制器
 * 
 * 功能：
 * 1. 获取模板列表
 * 2. 创建合成任务
 * 3. 下载合成的合同
 */
@Slf4j
@RestController
@RequestMapping("/api/compose")
public class ComposeController {
    
    @Autowired
    private ZhaoxinApiClient apiClient;
    
    /**
     * 获取合成模板列表
     * 
     * @param status 可选的状态筛选参数：PUBLISHED（已发布）、DRAFT（草稿）、DISABLED（已禁用）、DELETED（已删除）
     * @return 模板列表
     */
    @GetMapping("/templates")
    public ApiResponse<Map<String, Object>> getTemplates(
            @RequestParam(required = false) String status) {
        try {
            log.info("获取合成模板列表: status={}", status);
            Map<String, Object> result = apiClient.getComposeTemplates(status);
            return ApiResponse.success(result);
        } catch (Exception e) { 
            log.error("获取合成模板列表失败", e);
            return ApiResponse.error("获取模板列表失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 合成合同
     * 
     * @param request 合成请求（包含模板编号templateCode或模板文件ID templateFileId和填充数据values）
     * @return 合成任务结果（包含fileId）
     */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generateContract(@RequestBody Map<String, Object> request) {
        try {
            // 优先使用模板编号（推荐）
            String templateCode = (String) request.get("templateCode");
            String templateFileId = (String) request.get("templateFileId");
            
            if (templateCode != null && !templateCode.trim().isEmpty()) {
                log.info("合成合同: templateCode={}", templateCode);
            } else if (templateFileId != null && !templateFileId.trim().isEmpty()) {
                log.info("合成合同: templateFileId={}", templateFileId);
            } else {
                log.warn("合成合同: 未提供templateCode或templateFileId");
            }
            
            Map<String, Object> result = apiClient.generateContract(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("合成合同失败", e);
            return ApiResponse.error("合成失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载合成的合同（通过文件ID）
     * 
     * @param fileId 文件ID
     * @param fileName 文件名（可选）
     * @return 文件流
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadContract(
            @PathVariable String fileId,
            @RequestParam(required = false) String fileName) {
        try {
            log.info("下载合同: fileId={}, fileName={}", fileId, fileName);
            
            // 调用主系统下载文件
            byte[] fileData = apiClient.downloadComposeFile(fileId);
            
            // 如果没有提供文件名，使用默认名
            if (fileName == null || fileName.isEmpty()) {
                fileName = "contract_" + fileId + ".docx";
            }
            
            // URL编码文件名以支持中文
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentLength(fileData.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("下载合同失败: fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 下载合成的合同（通过相对路径）
     * 用于下载 PDF、盖章版、骑缝章版等文件
     * 
     * @param path 文件相对路径
     * @return 文件流
     */
    @GetMapping("/download-by-path")
    public ResponseEntity<ByteArrayResource> downloadContractByPath(
            @RequestParam String path) {
        try {
            log.info("通过路径下载文件: path={}", path);
            
            // 调用主系统下载文件
            byte[] fileData = apiClient.downloadFileByPath(path);
            
            // 从路径中提取文件名
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            
            // URL编码文件名以支持中文
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            // 根据文件扩展名确定 Content-Type
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (fileName.toLowerCase().endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF;
            } else if (fileName.toLowerCase().endsWith(".docx")) {
                contentType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentLength(fileData.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("通过路径下载文件失败: path={}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

