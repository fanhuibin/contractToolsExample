package com.zhaoxinms.contract.tools.onlyoffice.controller;

import com.zhaoxinms.contract.tools.onlyoffice.filemodel.OnlyofficeFileModel;
import com.zhaoxinms.contract.tools.onlyoffice.filemodel.User;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.FileConfigurer;
import com.zhaoxinms.contract.tools.onlyoffice.services.configurers.wrappers.DefaultFileWrapper;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Action;
import com.zhaoxinms.contract.tools.onlyoffice.enums.Type;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * OnlyOffice文档编辑器控制器
 * 提供文档预览和编辑功能
 */
@Validated
@RestController
@RequestMapping("/onlyoffice")
public class OnlyOfficeController {

    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private FileConfigurer<DefaultFileWrapper> fileConfigurer;

    @Value("${onlyoffice.domain}")
    private String onlyofficeDomain;
    
    @Value("${onlyoffice.port}")
    private String onlyofficePort;
    
    @Value("${onlyoffice.callback.url}")
    private String onlyofficeCallbackUrl;
    
    @Value("${onlyoffice.plugins}")
    private List<String> onlyofficePlugins;

    /**
     * 获取文档编辑器配置
     * 
     * @param fileId 文件ID
     * @param canEdit 是否可编辑
     * @param canReview 是否可审阅
     * @return 文档编辑器配置
     */
    @RequestMapping(value = "/editor/config", method = RequestMethod.GET)
    @ResponseBody
    public Result<OnlyofficeFileModel> getEditorConfig(
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "canEdit", defaultValue = "false") Boolean canEdit,
            @RequestParam(value = "canReview", defaultValue = "false") Boolean canReview) throws IOException {
        
        // 获取文件信息
        FileInfo fileInfo = fileInfoService.getById(Long.valueOf(fileId));
        if (fileInfo == null) {
            return Result.error("文件不存在");
        }

        // 生成OnlyOffice key（如果不存在）
        String key = fileInfo.getOnlyofficeKey();
        if (key == null || key.isEmpty()) {
            fileInfo = fileInfoService.generateOnlyofficeKey(Long.valueOf(fileId));
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
                .callbackUrl(onlyofficeCallbackUrl + "/save?fileId=" + fileId)
                .url(onlyofficeCallbackUrl + "/download/" + fileId)
                .type(Type.desktop)
                .lang("zh-CN")
                .action(canEdit ? Action.edit : Action.view)
                .user(user)
                .pluginsData(onlyofficePlugins)
                .build()
        );

        return Result.success(fileModel);
    }

    /**
     * 获取OnlyOffice服务器地址
     * 
     * @return 服务器地址配置
     */
    @RequestMapping(value = "/server/info", method = RequestMethod.GET)
    @ResponseBody
    public Result<ServerInfo> getServerInfo() {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setDomain(onlyofficeDomain);
        serverInfo.setPort(onlyofficePort);
        serverInfo.setFullUrl(onlyofficeDomain + ":" + onlyofficePort);
        return Result.success(serverInfo);
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