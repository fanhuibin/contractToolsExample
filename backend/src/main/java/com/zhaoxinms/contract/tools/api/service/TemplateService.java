package com.zhaoxinms.contract.tools.api.service;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;

/**
 * 模板服务接口
 * 此接口需要由SDK项目实现，提供字段信息查询功能
 */
public interface TemplateService {
    
    /**
     * 获取字段信息
     * @return 字段响应，包含基础字段、相对方字段和条款字段
     */
    FieldResponse getFields();
    
    /**
     * 根据模板ID获取字段信息
     * @param templateId 模板ID
     * @return 字段响应
     */
    FieldResponse getFieldsByTemplateId(String templateId);
} 