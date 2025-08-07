package com.zhaoxinms.contract.tools.aicomponent.service;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 合同提取信息模板服务接口
 * 
 * @author zhaoxinms
 */
public interface ContractExtractTemplateService {
    
    /**
     * 获取所有模板列表
     * 
     * @return 模板列表
     */
    List<ContractExtractTemplate> getAllTemplates();
    
    /**
     * 获取指定ID的模板
     * 
     * @param id 模板ID
     * @return 模板信息
     */
    Optional<ContractExtractTemplate> getTemplateById(Long id);
    
    /**
     * 根据合同类型获取模板列表
     * 
     * @param contractType 合同类型
     * @return 模板列表
     */
    List<ContractExtractTemplate> getTemplatesByContractType(String contractType);
    
    /**
     * 获取系统预设模板列表
     * 
     * @return 系统模板列表
     */
    List<ContractExtractTemplate> getSystemTemplates();
    
    /**
     * 获取用户自定义模板列表
     * 
     * @param userId 用户ID
     * @return 用户模板列表
     */
    List<ContractExtractTemplate> getUserTemplates(String userId);
    
    /**
     * 创建新模板
     * 
     * @param template 模板信息
     * @return 创建的模板
     */
    ContractExtractTemplate createTemplate(ContractExtractTemplate template);
    
    /**
     * 更新模板信息
     * 
     * @param id 模板ID
     * @param template 更新的模板信息
     * @return 更新后的模板
     */
    ContractExtractTemplate updateTemplate(Long id, ContractExtractTemplate template);
    
    /**
     * 删除模板
     * 
     * @param id 模板ID
     */
    void deleteTemplate(Long id);
    
    /**
     * 复制模板
     * 
     * @param id 源模板ID
     * @param newName 新模板名称
     * @param userId 用户ID
     * @return 复制的新模板
     */
    ContractExtractTemplate copyTemplate(Long id, String newName, String userId);
    
    /**
     * 设置默认模板
     * 
     * @param id 模板ID
     * @param contractType 合同类型
     * @return 设置为默认的模板
     */
    ContractExtractTemplate setDefaultTemplate(Long id, String contractType);
    
    /**
     * 获取指定合同类型的默认模板
     * 
     * @param contractType 合同类型
     * @return 默认模板
     */
    Optional<ContractExtractTemplate> getDefaultTemplate(String contractType);
    
    /**
     * 获取系统模板和指定用户的自定义模板
     * 
     * @param userId 用户ID
     * @return 模板列表
     */
    List<ContractExtractTemplate> findAllSystemAndUserTemplates(String userId);
    
    /**
     * 获取指定合同类型的系统模板和用户模板
     * 
     * @param contractType 合同类型
     * @param userId 用户ID
     * @return 模板列表
     */
    List<ContractExtractTemplate> getTemplatesByContractTypeAndUser(String contractType, String userId);
    
    /**
     * 初始化系统预设模板
     */
    void initSystemTemplates();
    
    /**
     * 获取所有合同类型
     * 
     * @return 合同类型映射
     */
    Map<String, String> getAllContractTypes();
}