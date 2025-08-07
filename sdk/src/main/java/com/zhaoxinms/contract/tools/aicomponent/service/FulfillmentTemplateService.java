package com.zhaoxinms.contract.tools.aicomponent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentTemplate;

import java.util.List;

/**
 * 履约任务模板服务接口
 * 定义模板管理的基本方法
 */
public interface FulfillmentTemplateService extends IService<FulfillmentTemplate> {
    
    /**
     * 根据合同类型获取模板列表
     * @param contractType 合同类型
     * @param userId 用户ID
     * @return 模板列表
     */
    List<FulfillmentTemplate> listTemplatesByType(String contractType, String userId);
    
    /**
     * 获取默认模板
     * @param contractType 合同类型
     * @param userId 用户ID
     * @return 默认模板
     */
    FulfillmentTemplate getDefaultTemplate(String contractType, String userId);
    
    /**
     * 创建新模板
     * @param template 模板信息
     * @return 创建的模板
     */
    FulfillmentTemplate createTemplate(FulfillmentTemplate template);
    
    /**
     * 更新模板
     * @param template 模板信息
     * @return 更新后的模板
     */
    FulfillmentTemplate updateTemplate(FulfillmentTemplate template);
    
    /**
     * 复制模板
     * @param templateId 源模板ID
     * @param newName 新模板名称
     * @param userId 用户ID
     * @return 复制的新模板
     */
    FulfillmentTemplate copyTemplate(Long templateId, String newName, String userId);
    
    /**
     * 设置默认模板
     * @param templateId 模板ID
     * @param contractType 合同类型
     * @return 设置后的模板
     */
    FulfillmentTemplate setDefaultTemplate(Long templateId, String contractType);
}
