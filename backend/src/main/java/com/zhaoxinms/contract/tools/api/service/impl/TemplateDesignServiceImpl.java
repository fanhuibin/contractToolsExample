package com.zhaoxinms.contract.tools.api.service.impl;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import com.zhaoxinms.contract.tools.api.service.TemplateService;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

/**
 * 模板设计服务实现类
 */
@Service
public class TemplateDesignServiceImpl implements TemplateDesignService {

    @Autowired(required = false)
    private TemplateService templateService;
    
    @Autowired
    private ZxcmConfig zxcmConfig;

    @Override
    public FieldResponse getFields() {
        // 如果SDK项目实现了TemplateService，则使用SDK的实现
        if (templateService != null) {
            return templateService.getFields();
        }
        
        // 否则返回默认的空响应（含印章列表）
        FieldResponse response = new FieldResponse();
        response.setBaseFields(new ArrayList<>());
        response.setCounterpartyFields(new ArrayList<>());
        response.setClauseFields(new ArrayList<>());
        response.setSealFields(new ArrayList<>());
        return response;
    }

    @Override
    public String getFileDownloadUrl(String fileId) {
        // 构建文件下载地址
        String baseUrl = "http://localhost:" + zxcmConfig.getServer().getPort() + zxcmConfig.getServer().getServlet().getContextPath();
        return baseUrl + "/api/template/file/download/" + fileId;
    }

    @Override
    public TemplateDesignResponse startTemplateDesign(TemplateDesignRequest request) {
        TemplateDesignResponse response = new TemplateDesignResponse();
        
        try {
            // 生成会话ID
            String sessionId = UUID.randomUUID().toString();
            response.setSessionId(sessionId);
            
            // 构建编辑页面URL
            String editUrl = request.getBackendUrl() + 
                    "/template-design?id=" + request.getTemplateId() +
                    "&sessionId=" + sessionId +
                    "&callbackUrl=" + request.getCallbackUrl();
            
            response.setEditUrl(editUrl);
            response.setStatus("SUCCESS");
            response.setMessage("模板设计会话创建成功");
            
        } catch (Exception e) {
            response.setStatus("ERROR");
            response.setMessage("创建模板设计会话失败：" + e.getMessage());
        }
        
        return response;
    }
} 