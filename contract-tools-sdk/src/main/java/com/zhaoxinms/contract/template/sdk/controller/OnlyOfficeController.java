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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
        
        // 获取文件信息（支持内存自动注册的 templateDesign）
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            if ("templateDesign".equals(fileId)) {
                // 构造一个临时配置，下载接口中会做兜底
                String key = "demo_key_" + System.currentTimeMillis();
                User user = new User();
                user.setName("Anonymous");
                user.setId("0");
                OnlyofficeFileModel tempModel = fileConfigurer.getFileModel(
                    DefaultFileWrapper.builder()
                        .fileId(fileId)
                        .fileName("templateDesign.docx")
                        .key(key)
                        .canEdit(canEdit)
                        .canReview(canReview && canEdit)
                        .callbackUrl(zxcmConfig.getOnlyOffice().getCallback().getUrl().replace("/save", "") + "/save?fileId=" + fileId)
                        .url(zxcmConfig.getOnlyOffice().getCallback().getUrl().replace("/save", "") + "/download/" + fileId)
                        .type(Type.desktop)
                        .lang("zh-CN")
                        .action(canEdit ? Action.edit : Action.view)
                        .user(user)
                        .pluginsData(java.util.Arrays.asList(zxcmConfig.getOnlyOffice().getPlugins()))
                        .build()
                );
                return ApiResponse.success(tempModel);
            }
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

        // 构建文件模型
        OnlyofficeFileModel fileModel = fileConfigurer.getFileModel(
            DefaultFileWrapper.builder()
                .fileId(fileId)
                .fileName(fileInfo.getOriginalName())
                .key(key)
                .canEdit(canEdit)
                .canReview(canReview && canEdit) // 只有可编辑时才能审阅
                .callbackUrl(zxcmConfig.getOnlyOffice().getCallback().getUrl().replace("/save", "") + "/save?fileId=" + fileId + 
                           (templateId != null ? "&templateId=" + templateId : "") +
                           (sessionId != null ? "&sessionId=" + sessionId : "") +
                           (callbackUrl != null ? "&callbackUrl=" + callbackUrl : ""))
                .url(zxcmConfig.getOnlyOffice().getCallback().getUrl().replace("/save", "") + "/download/" + fileId)
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
} 