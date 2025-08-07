package com.zhaoxinms.contract.tools.aicomponent.repository;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 合同提取信息模板仓库接口
 * 
 * @author zhaoxinms
 */
@Repository
public interface ContractExtractTemplateRepository extends JpaRepository<ContractExtractTemplate, Long> {
    
    /**
     * 根据合同类型查找模板列表
     * 
     * @param contractType 合同类型
     * @return 模板列表
     */
    List<ContractExtractTemplate> findByContractType(String contractType);
    
    /**
     * 根据类型查找模板列表
     * 
     * @param type 模板类型（system/user）
     * @return 模板列表
     */
    List<ContractExtractTemplate> findByType(String type);
    
    /**
     * 根据创建者ID查找用户自定义模板
     * 
     * @param creatorId 创建者ID
     * @return 模板列表
     */
    List<ContractExtractTemplate> findByCreatorId(String creatorId);
    
    /**
     * 查找指定合同类型的默认模板
     * 
     * @param contractType 合同类型
     * @return 默认模板
     */
    Optional<ContractExtractTemplate> findByContractTypeAndIsDefaultTrue(String contractType);
    
    /**
     * 根据合同类型和创建者ID查找模板列表
     * 
     * @param contractType 合同类型
     * @param creatorId 创建者ID
     * @return 模板列表
     */
    List<ContractExtractTemplate> findByContractTypeAndCreatorId(String contractType, String creatorId);
    
    /**
     * 根据合同类型和模板类型查找模板列表
     * 
     * @param contractType 合同类型
     * @param type 模板类型
     * @return 模板列表
     */
    List<ContractExtractTemplate> findByContractTypeAndType(String contractType, String type);
    
    /**
     * 查询所有系统预设模板和指定用户的自定义模板
     * 
     * @param creatorId 创建者ID
     * @return 模板列表
     */
    @Query("SELECT t FROM ContractExtractTemplate t WHERE t.type = 'system' OR (t.type = 'user' AND t.creatorId = ?1)")
    List<ContractExtractTemplate> findAllSystemAndUserTemplates(String creatorId);
}