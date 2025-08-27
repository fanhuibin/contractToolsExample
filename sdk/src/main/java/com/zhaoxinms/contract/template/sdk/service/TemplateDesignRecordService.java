package com.zhaoxinms.contract.template.sdk.service;

import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import java.util.List;

public interface TemplateDesignRecordService {
    TemplateDesignRecord save(String id, String templateId, String fileId, String elementsJson);
    TemplateDesignRecord getById(String id);
    TemplateDesignRecord getByTemplateId(String templateId);
    List<TemplateDesignRecord> listAll();
    boolean deleteById(String id);
}


