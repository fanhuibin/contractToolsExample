package com.zhaoxinms.contract.tools.aicomponent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同提取信息模板实体类
 * 
 * @author zhaoxinms
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contract_extract_template")
public class ContractExtractTemplate {
    
    /**
     * 模板ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模板名称
     */
    private String name;
    
    /**
     * 模板类型
     * system: 系统预设模板
     * user: 用户自定义模板
     */
    private String type;
    
    /**
     * 合同类型
     * common: 通用
     * lease: 租赁合同
     * purchase: 购销合同
     * labor: 劳动合同
     * construction: 建筑合同
     * technical: 技术合同
     * intellectual: 知识产权合同
     * operation: 运营服务合同
     * custom: 自定义类型
     */
    private String contractType;
    
    /**
     * 提取字段列表，JSON格式存储
     */
    @Column(columnDefinition = "TEXT")
    private String fields;
    
    /**
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否默认模板
     */
    private Boolean isDefault;
    
    /**
     * 描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}