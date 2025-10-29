package com.zhaoxinms.contract.template.sdk.service;

import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import java.util.List;

public interface TemplateDesignRecordService {
    // 旧版保存方法，保留兼容性
    TemplateDesignRecord save(String id, String templateId, String fileId, String elementsJson);
    
    // 新版保存方法
    TemplateDesignRecord saveTemplate(TemplateDesignRecord record);
    
    // 创建新版本（基于现有版本复制）
    TemplateDesignRecord createNewVersion(String sourceId, String newVersion);
    
    // 发布版本（将旧版本设为非发布状态）
    TemplateDesignRecord publishVersion(String id);
    
    // 更新状态
    TemplateDesignRecord updateStatus(String id, String status);
    
    TemplateDesignRecord getById(String id);
    TemplateDesignRecord getByTemplateId(String templateId);
    
    // 根据编码获取最新版本
    TemplateDesignRecord getLatestByCode(String templateCode);
    
    // 根据编码获取已发布版本
    TemplateDesignRecord getPublishedByCode(String templateCode);
    
    // 根据编码获取所有版本
    List<TemplateDesignRecord> getVersionsByCode(String templateCode);
    
    List<TemplateDesignRecord> listAll();
    
    // 软删除
    boolean deleteById(String id);
    
    // 硬删除
    boolean hardDeleteById(String id);
}


