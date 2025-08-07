package com.zhaoxinms.contract.tools.api.service;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;

/**
 * 模板设计服务接口
 */
public interface TemplateDesignService {
    
    /**
     * 获取字段信息
     * @return 字段响应
     */
    FieldResponse getFields();
    
    /**
     * 获取文件下载地址
     * @param fileId 文件ID
     * @return 文件下载地址
     */
    String getFileDownloadUrl(String fileId);
    
    /**
     * 发起模板设计
     * @param request 模板设计请求
     * @return 模板设计响应
     */
    TemplateDesignResponse startTemplateDesign(TemplateDesignRequest request);
} 