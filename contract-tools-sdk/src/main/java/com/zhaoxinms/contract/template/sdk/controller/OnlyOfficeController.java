package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.onlyoffice.filemodel.OnlyofficeFileModel;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.User;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.FileConfigurer;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultFileWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Action;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Type;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * OnlyOffice文档编辑器控制器
 * SDK项目中的OnlyOffice控制器，与Frontend配合调用OnlyOffice
 */
@Validated
@RestController
@RequestMapping("/api/onlyoffice")
public class OnlyOfficeController {

    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private FileConfigurer<DefaultFileWrapper> fileConfigurer;

    @Autowired
    private ZxcmConfig zxcmConfig;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    /**
     * 获取文档编辑器配置
     * 
     * @param fileId 文件ID
     * @param canEdit 是否可编辑
     * @param canReview 是否可审阅
     * @param updateOnlyofficeKey 是否更新OnlyOffice密钥
     * @param templateId 模板ID（可选）
     * @param sessionId 会话ID（可选）
     * @param callbackUrl 回调地址（可选）
     * @return 文档编辑器配置
     */
    @RequestMapping(value = "/editor/config", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<OnlyofficeFileModel> getEditorConfig(
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "canEdit", defaultValue = "false") Boolean canEdit,
            @RequestParam(value = "canReview", defaultValue = "false") Boolean canReview,
            @RequestParam(value = "updateOnlyofficeKey", defaultValue = "false") Boolean updateOnlyofficeKey,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "callbackUrl", required = false) String callbackUrl) throws IOException {
        
        // 获取文件信息
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            return ApiResponse.notFound("文件不存在");
        }

        // 生成OnlyOffice key（如果不存在或需要更新）
        String key = fileInfo.getOnlyofficeKey();
        if (key == null || key.isEmpty() || updateOnlyofficeKey) {
            fileInfo = fileInfoService.generateOnlyofficeKey(fileId);
            key = fileInfo.getOnlyofficeKey();
        }

        // 创建用户信息（简化版，实际应该从认证信息获取）
        User user = new User();
        user.setName("Anonymous");
        user.setId("0");

        // 构建回调基础URL（使用应用基础URL）
        String callbackBaseUrl = zxcmConfig.getApplication().getBaseUrl() + "/api/onlyoffice";

        // 构建文件模型
        OnlyofficeFileModel fileModel = fileConfigurer.getFileModel(
            DefaultFileWrapper.builder()
                .fileId(fileId)
                .fileName(fileInfo.getOriginalName())
                .key(key)
                .canEdit(canEdit)
                .canReview(canReview && canEdit) // 只有可编辑时才能审阅
                .callbackUrl(callbackBaseUrl + "/callback/save?fileId=" + fileId + 
                           (templateId != null ? "&templateId=" + templateId : "") +
                           (sessionId != null ? "&sessionId=" + sessionId : "") +
                           (callbackUrl != null ? "&callbackUrl=" + callbackUrl : ""))
                .url(callbackBaseUrl + "/callback/download/" + fileId)  // 修复：加上 /callback 路径
                .type(Type.desktop)
                .lang("zh-CN")
                .action(canEdit ? Action.edit : Action.view)
                .user(user)
                .pluginsData(java.util.Arrays.asList(zxcmConfig.getOnlyOffice().getPlugins()))
                .build()
        );

        return ApiResponse.success(fileModel);
    }

    /**
     * 获取OnlyOffice服务器地址
     * 
     * @return 服务器地址配置
     */
    @RequestMapping(value = "/server/info", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<ServerInfo> getServerInfo() {
        ServerInfo serverInfo = new ServerInfo();
        String domain = zxcmConfig.getOnlyOffice().getDomain();
        String port = zxcmConfig.getOnlyOffice().getPort();
        
        // 如果domain已经包含协议，直接使用
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            serverInfo.setDomain(domain);
            serverInfo.setPort(port);
            serverInfo.setFullUrl(domain + ":" + port);
        } else {
            // 否则添加协议
            serverInfo.setDomain("http://" + domain);
            serverInfo.setPort(port);
            serverInfo.setFullUrl("http://" + domain + ":" + port);
        }
        
        return ApiResponse.success(serverInfo);
    }

    /**
     * 上传文件
     * 
     * @param file 文件
     * @return 上传结果，包含文件ID
     */
    @PostMapping("/upload")
    @ResponseBody
    public ApiResponse<UploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.<UploadResult>paramError("文件不能为空");
            }
            
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return ApiResponse.<UploadResult>paramError("文件名不能为空");
            }
            
            // 使用 FileInfoService 保存文件，指定模块为 onlyoffice-demo
            FileInfo fileInfo = fileInfoService.saveNewFile(file, "onlyoffice-demo");
            
            // 返回结果
            UploadResult result = new UploadResult();
            result.setFileId(String.valueOf(fileInfo.getId()));
            result.setFileName(fileInfo.getFileName());
            result.setOriginalName(fileInfo.getOriginalName());
            result.setFileSize(fileInfo.getFileSize());
            
            return ApiResponse.success(result, "文件上传成功");
        } catch (Exception e) {
            return ApiResponse.<UploadResult>serverError().errorDetail("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 服务器信息DTO
     */
    public static class ServerInfo {
        private String domain;
        private String port;
        private String fullUrl;

        // getters and setters
        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getFullUrl() {
            return fullUrl;
        }

        public void setFullUrl(String fullUrl) {
            this.fullUrl = fullUrl;
        }
    }
    
    /**
     * 上传结果DTO
     */
    public static class UploadResult {
        private String fileId;
        private String fileName;
        private String originalName;
        private Long fileSize;
        
        // getters and setters
        public String getFileId() {
            return fileId;
        }
        
        public void setFileId(String fileId) {
            this.fileId = fileId;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getOriginalName() {
            return originalName;
        }
        
        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
} 