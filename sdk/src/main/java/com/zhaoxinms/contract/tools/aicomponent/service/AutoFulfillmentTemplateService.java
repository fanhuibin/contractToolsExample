package com.zhaoxinms.contract.tools.aicomponent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AutoFulfillmentTemplateService extends IService<AutoFulfillmentTemplate> {
    List<AutoFulfillmentTemplate> getSystemTemplates();
    List<AutoFulfillmentTemplate> getTemplatesByContractType(String contractType);
    List<AutoFulfillmentTemplate> getTemplatesByContractTypeAndUser(String contractType, String userId);
    List<AutoFulfillmentTemplate> findAllSystemAndUserTemplates(String userId);
    Optional<AutoFulfillmentTemplate> getTemplateById(Long id);
    AutoFulfillmentTemplate createTemplate(AutoFulfillmentTemplate template);
    AutoFulfillmentTemplate updateTemplate(Long id, AutoFulfillmentTemplate template);
    void deleteTemplate(Long id);
    AutoFulfillmentTemplate copyTemplate(Long id, String newName, String userId);
    AutoFulfillmentTemplate setDefaultTemplate(Long id, String contractType);
    Optional<AutoFulfillmentTemplate> getDefaultTemplate(String contractType);
    Map<String, String> getAllContractTypes();
    void initSystemTemplates();
}


