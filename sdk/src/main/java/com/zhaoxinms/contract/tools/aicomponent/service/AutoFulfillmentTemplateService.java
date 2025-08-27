package com.zhaoxinms.contract.tools.aicomponent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AutoFulfillmentTemplateService extends IService<AutoFulfillmentTemplate> {
    List<AutoFulfillmentTemplate> getSystemTemplates();
    List<AutoFulfillmentTemplate> getTemplatesByCategory(String categoryCode);
    List<AutoFulfillmentTemplate> getTemplatesByCategoryAndUser(String categoryCode, String userId);
    List<AutoFulfillmentTemplate> findAllSystemAndUserTemplates(String userId);
    Optional<AutoFulfillmentTemplate> getTemplateById(Long id);
    AutoFulfillmentTemplate createTemplate(AutoFulfillmentTemplate template);
    AutoFulfillmentTemplate updateTemplate(Long id, AutoFulfillmentTemplate template);
    void deleteTemplate(Long id);
    AutoFulfillmentTemplate copyTemplate(Long id, String newName, String userId);
    AutoFulfillmentTemplate setDefaultTemplate(Long id, String categoryCode);
    Optional<AutoFulfillmentTemplate> getDefaultTemplate(String categoryCode);
    Map<String, String> getAllCategories();
    void initSystemTemplates();
}


