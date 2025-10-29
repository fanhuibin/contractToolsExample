package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zhaoxinms.contract.tools.onlyoffice.util.JwtManager;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件管理控制器
 * SDK项目中的文件管理功能
 */
@RestController
@RequestMapping("/api/file")
@Api(tags = "文件管理API")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private JwtManager jwtManager;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;
    
    @Value("${zxcm.onlyoffice.domain:http://localhost}")
    private String onlyofficeDomain;
    
    @Value("${zxcm.onlyoffice.port:80}")
    private String onlyofficePort;

    @GetMapping("/download/{fileId}")
    @ApiOperation("下载文件")
    public void downloadFile(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId,
            HttpServletResponse response) {
        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            } 
            
            // 获取文件路径
            String filePath = fileInfoService.getFileDiskPath(fileId);
            if (filePath == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 设置响应头
            String contentType = getContentType(fileInfo.getFileExtension());
            response.setContentType(contentType);
            
            // 正确处理中文文件名编码
            String originalName = fileInfo.getOriginalName();
            String encodedFileName = java.net.URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Content-Length", String.valueOf(file.length()));
            
            // 读取文件并输出
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败：" + e.getMessage());
            } catch (IOException ex) {
                // 忽略异常
            }
        }
    }
    
    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }
        
        switch (fileExtension.toLowerCase()) {
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }

    @GetMapping("/{fileId}")
    @ApiOperation("获取文件信息")
    public ApiResponse<FileInfo> getFileInfo(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                return ApiResponse.<FileInfo>notFound("文件不存在");
            }
            return ApiResponse.success(fileInfo);
        } catch (Exception e) {
            return ApiResponse.<FileInfo>serverError().errorDetail("获取文件信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取文件列表")
    public ApiResponse<List<FileInfo>> getFileList(
            @ApiParam(value = "模块名称（可选）", required = false) 
            @RequestParam(required = false) String module) {
        try {
            List<FileInfo> files;
            if (module != null && !module.isEmpty()) {
                // 按模块过滤
                files = fileInfoService.getFilesByModule(module);
            } else {
                // 返回所有文件
                files = fileInfoService.getAllFiles();
            }
            return ApiResponse.success(files);
        } catch (Exception e) {
            return ApiResponse.<List<FileInfo>>serverError().errorDetail("获取文件列表失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    @ApiOperation("删除文件")
    public ApiResponse<Void> deleteFile(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                return ApiResponse.<Void>notFound("文件不存在");
            }
            
            // 删除磁盘上的文件
            String filePath = fileInfoService.getFileDiskPath(fileId);
            if (filePath != null) {
                java.io.File file = new java.io.File(filePath);
                if (file.exists()) {
                    if (!file.delete()) {
                        return ApiResponse.<Void>serverError().errorDetail("删除磁盘文件失败");
                    }
                }
            }
            
            // 删除数据库记录
            boolean success = fileInfoService.deleteById(fileId);
            if (success) {
                return ApiResponse.<Void>success("文件删除成功", null);
            } else {
                return ApiResponse.<Void>serverError().errorDetail("删除数据库记录失败");
            }
        } catch (Exception e) {
            return ApiResponse.<Void>serverError().errorDetail("删除文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 复制文件（用于前端合成创建模板副本）
     */
    @PostMapping("/copy/{fileId}")
    @ApiOperation("复制文件")
    public ApiResponse<FileInfo> copyFile(
            @ApiParam(value = "源文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            // 获取源文件信息
            FileInfo sourceFileInfo = fileInfoService.getById(fileId);
            if (sourceFileInfo == null) {
                return ApiResponse.notFound("源文件不存在");
            }
            
            // 获取源文件路径
            String sourceFilePath = fileInfoService.getFileDiskPath(fileId);
            if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                return ApiResponse.<FileInfo>serverError().errorDetail("无法获取源文件路径");
            }
            
            java.io.File sourceFile = new java.io.File(sourceFilePath);
            if (!sourceFile.exists()) {
                return ApiResponse.<FileInfo>serverError().errorDetail("源文件不存在于磁盘");
            }
            
            // 创建副本文件（保存到files子目录，使用年月路径）
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            Path uploadRoot = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            Path uploadDir = uploadRoot.resolve("files").resolve(yearMonthPath);
            Files.createDirectories(uploadDir);
            
            String originalName = sourceFileInfo.getOriginalName();
            String extension = sourceFileInfo.getFileExtension();
            String copyFileName = UUID.randomUUID().toString() + "_copy_" + originalName;
            Path copyFilePath = uploadDir.resolve(copyFileName);
            
            // 复制文件
            Files.copy(sourceFile.toPath(), copyFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 注册新文件
            FileInfo newFileInfo = fileInfoService.registerFile(
                originalName, 
                extension, 
                copyFilePath.toString(), 
                Files.size(copyFilePath)
            );
            
            return ApiResponse.success("文件复制成功", newFileInfo);
        } catch (IOException e) {
            return ApiResponse.<FileInfo>serverError().errorDetail("复制文件失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.<FileInfo>serverError().errorDetail("复制文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 触发 OnlyOffice 强制保存
     * 通过向 Document Server 的 Command Service 发送请求来触发强制保存
     * 
     * @param fileId 文件ID
     * @return 保存结果
     */
    @PostMapping("/forcesave/{fileId}")
    @ApiOperation("触发OnlyOffice强制保存")
    public ApiResponse<Map<String, Object>> forceSave(
            @ApiParam(value = "文件ID", required = true) 
            @PathVariable String fileId) {
        try {
            log.info("===== 触发OnlyOffice强制保存 =====");
            log.info("文件ID: {}", fileId);
            
            // 获取文件信息以获取 onlyofficeKey
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                log.error("文件不存在，文件ID: {}", fileId);
                return ApiResponse.paramError("文件不存在");
            }
            
            String onlyofficeKey = fileInfo.getOnlyofficeKey();
            if (onlyofficeKey == null || onlyofficeKey.isEmpty()) {
                log.error("文件缺少OnlyOffice Key，文件ID: {}", fileId);
                return ApiResponse.paramError("文件缺少OnlyOffice Key");
            }
            
            log.info("OnlyOffice Key: {}", onlyofficeKey);
            
            // 构建 Command Service URL
            String commandServiceUrl = String.format("%s:%s/coauthoring/CommandService.ashx", 
                onlyofficeDomain, onlyofficePort);
            log.info("Command Service URL: {}", commandServiceUrl);
            
            // 构建请求负载（payload）
            Map<String, Object> payload = new HashMap<>();
            payload.put("c", "forcesave");
            payload.put("key", onlyofficeKey);
            payload.put("userdata", "forcesave_triggered_by_user");
            
            log.info("Command Service payload: {}", payload);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            
            // 如果启用了 JWT，生成 token 并添加到请求体
            if (jwtManager.tokenEnabled()) {
                String token = jwtManager.createToken(payload);
                requestBody.put("token", token);
                log.info("✅ JWT 已启用，已生成 token");
                log.info("Token: {}", token.substring(0, Math.min(50, token.length())) + "...");
            } else {
                // 如果未启用 JWT，直接使用 payload
                requestBody = payload;
                log.info("⚠️ JWT 未启用，使用原始 payload");
            }
            
            log.info("发送强制保存请求，请求体: {}", requestBody);
            
            // 发送 POST 请求到 Command Service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = 
                (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(
                    commandServiceUrl, 
                    request, 
                    Map.class
                );
            
            log.info("Command Service 响应状态: {}", response.getStatusCode());
            log.info("Command Service 响应体: {}", response.getBody());
            
            // 检查响应错误码
            Map<String, Object> responseBody = response.getBody();
            int errorCode = -1;
            String errorMsg = "未知错误";
            boolean isSuccess = false;
            
            if (responseBody != null) {
                Object errorObj = responseBody.get("error");
                errorCode = (errorObj instanceof Number) ? ((Number) errorObj).intValue() : -1;
                errorMsg = getErrorMessage(errorCode);
                
                // error=0 或 error=4 都视为成功
                // error=0: 成功保存
                // error=4: 文档未修改，无需保存（也是成功状态）
                isSuccess = (errorCode == 0 || errorCode == 4);
                
                log.info("===== Command Service 响应分析 =====");
                log.info("错误码: {}", errorCode);
                log.info("错误含义: {}", errorMsg);
                log.info("处理结果: {}", isSuccess ? "成功" : "失败");
                
                if (errorCode == 0) {
                    log.info("✅ 强制保存成功，文档已保存");
                } else if (errorCode == 4) {
                    log.info("✅ 文档未修改，无需保存（视为成功）");
                } else if (errorCode == 1) {
                    log.error("❌ error=1: 文档密钥缺失或找不到具有该密钥的文档");
                } else if (errorCode == 2) {
                    log.error("❌ error=2: 回调URL不正确");
                } else if (errorCode == 3) {
                    log.error("❌ error=3: 内部服务器错误");
                } else if (errorCode == 5) {
                    log.error("❌ error=5: 命令不正确");
                } else if (errorCode == 6) {
                    log.error("❌ error=6: 令牌无效，请检查 JWT 配置");
                    log.error("【解决方案】:");
                    log.error("  1. 确保 application.yml 中的 zxcm.onlyoffice.secret 与 OnlyOffice Document Server 的 secret 一致");
                    log.error("  2. 如果 Document Server 未启用 JWT，请在 application.yml 中留空 secret");
                } else {
                    log.warn("❌ Command Service 返回未知错误码: {}", errorCode);
                }
                log.info("===== 响应分析完成 =====");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("onlyofficeKey", onlyofficeKey);
            result.put("commandServiceUrl", commandServiceUrl);
            result.put("commandServiceResponse", response.getBody());
            result.put("errorCode", errorCode);
            result.put("errorMessage", errorMsg);
            result.put("success", isSuccess);
            
            String message = isSuccess ? "保存成功" : "保存失败（" + errorMsg + "）";
            return ApiResponse.success(message, result);
            
        } catch (Exception e) {
            log.error("触发强制保存失败", e);
            return ApiResponse.<Map<String, Object>>serverError()
                .errorDetail("触发强制保存失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取 OnlyOffice Command Service 错误码的含义
     * 参考：https://api.onlyoffice.com/editors/command/
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case 0:
                return "没有错误";
            case 1:
                return "文档密钥缺失或找不到具有该密钥的文档";
            case 2:
                return "回调url不正确";
            case 3:
                return "内部服务器错误";
            case 4:
                return "在收到 forcesave 命令之前，未对文档应用任何更改";
            case 5:
                return "命令不正确";
            case 6:
                return "令牌无效";
            default:
                return "未知错误码: " + errorCode;
        }
    }

} 