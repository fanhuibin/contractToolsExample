package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.AutoFulfillmentTemplateMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class AutoFulfillmentTemplateServiceImpl
        extends ServiceImpl<AutoFulfillmentTemplateMapper, AutoFulfillmentTemplate>
        implements AutoFulfillmentTemplateService {

    private final ObjectMapper objectMapper;

    private static final Map<String, String> CONTRACT_TYPES = new LinkedHashMap<>();
    static {
        CONTRACT_TYPES.put("lease", "租赁合同");
        CONTRACT_TYPES.put("purchase", "购销合同");
        CONTRACT_TYPES.put("labor", "劳动合同");
        CONTRACT_TYPES.put("construction", "建筑合同");
        CONTRACT_TYPES.put("technical", "技术合同");
        CONTRACT_TYPES.put("intellectual", "知识产权合同");
        CONTRACT_TYPES.put("operation", "运营服务合同");
        CONTRACT_TYPES.put("custom", "自定义类型");
    }

    @Override
    public List<AutoFulfillmentTemplate> getSystemTemplates() {
        return list(new LambdaQueryWrapper<AutoFulfillmentTemplate>().eq(AutoFulfillmentTemplate::getType, "system"));
    }

    @Override
    public List<AutoFulfillmentTemplate> getTemplatesByContractType(String contractType) {
        return list(new LambdaQueryWrapper<AutoFulfillmentTemplate>().eq(AutoFulfillmentTemplate::getContractType, contractType));
    }

    @Override
    public List<AutoFulfillmentTemplate> getTemplatesByContractTypeAndUser(String contractType, String userId) {
        List<AutoFulfillmentTemplate> result = new ArrayList<>();
        result.addAll(list(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                .eq(AutoFulfillmentTemplate::getContractType, contractType)
                .eq(AutoFulfillmentTemplate::getType, "system")));
        if (userId != null && !userId.isEmpty()) {
            result.addAll(list(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                    .eq(AutoFulfillmentTemplate::getContractType, contractType)
                    .eq(AutoFulfillmentTemplate::getCreatorId, userId)));
        }
        return result;
    }

    @Override
    public List<AutoFulfillmentTemplate> findAllSystemAndUserTemplates(String userId) {
        List<AutoFulfillmentTemplate> system = list(new LambdaQueryWrapper<AutoFulfillmentTemplate>().eq(AutoFulfillmentTemplate::getType, "system"));
        List<AutoFulfillmentTemplate> user = list(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                .eq(AutoFulfillmentTemplate::getType, "user")
                .eq(AutoFulfillmentTemplate::getCreatorId, userId));
        List<AutoFulfillmentTemplate> result = new ArrayList<>();
        result.addAll(system);
        result.addAll(user);
        return result;
    }

    @Override
    public Optional<AutoFulfillmentTemplate> getTemplateById(Long id) {
        return Optional.ofNullable(getById(id));
    }

    @Override
    @Transactional
    public AutoFulfillmentTemplate createTemplate(AutoFulfillmentTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            AutoFulfillmentTemplate oldDefault = getOne(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                    .eq(AutoFulfillmentTemplate::getContractType, template.getContractType())
                    .eq(AutoFulfillmentTemplate::getIsDefault, true));
            if (oldDefault != null) {
                oldDefault.setIsDefault(false);
                updateById(oldDefault);
            }
        }
        save(template);
        return template;
    }

    @Override
    @Transactional
    public AutoFulfillmentTemplate updateTemplate(Long id, AutoFulfillmentTemplate template) {
        AutoFulfillmentTemplate existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Template not found with id: " + id);
        }
        existing.setName(template.getName());
        existing.setContractType(template.getContractType());
        existing.setFields(template.getFields());
        existing.setDescription(template.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(template.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            AutoFulfillmentTemplate oldDefault = getOne(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                    .eq(AutoFulfillmentTemplate::getContractType, template.getContractType())
                    .eq(AutoFulfillmentTemplate::getIsDefault, true));
            if (oldDefault != null) {
                oldDefault.setIsDefault(false);
                updateById(oldDefault);
            }
            existing.setIsDefault(true);
        }
        updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        AutoFulfillmentTemplate t = getById(id);
        if (t != null && "system".equals(t.getType())) {
            throw new IllegalArgumentException("System templates cannot be deleted");
        }
        removeById(id);
    }

    @Override
    @Transactional
    public AutoFulfillmentTemplate copyTemplate(Long id, String newName, String userId) {
        AutoFulfillmentTemplate src = getById(id);
        if (src == null) throw new IllegalArgumentException("Template not found with id: " + id);
        AutoFulfillmentTemplate n = new AutoFulfillmentTemplate();
        n.setName(newName);
        n.setType("user");
        n.setCreatorId(userId);
        n.setContractType(src.getContractType());
        n.setFields(src.getFields());
        n.setIsDefault(false);
        n.setDescription((src.getDescription() == null ? "" : src.getDescription()) + " (复制)");
        n.setCreateTime(LocalDateTime.now());
        n.setUpdateTime(LocalDateTime.now());
        save(n);
        return n;
    }

    @Override
    @Transactional
    public AutoFulfillmentTemplate setDefaultTemplate(Long id, String contractType) {
        update(new LambdaUpdateWrapper<AutoFulfillmentTemplate>()
                .eq(AutoFulfillmentTemplate::getContractType, contractType)
                .set(AutoFulfillmentTemplate::getIsDefault, false));
        AutoFulfillmentTemplate n = getById(id);
        if (n == null) throw new IllegalArgumentException("Template not found with id: " + id);
        n.setIsDefault(true);
        updateById(n);
        return n;
    }

    @Override
    public Optional<AutoFulfillmentTemplate> getDefaultTemplate(String contractType) {
        AutoFulfillmentTemplate t = getOne(new LambdaQueryWrapper<AutoFulfillmentTemplate>()
                .eq(AutoFulfillmentTemplate::getContractType, contractType)
                .eq(AutoFulfillmentTemplate::getIsDefault, true));
        return Optional.ofNullable(t);
    }

    @Override
    public Map<String, String> getAllContractTypes() {
        return CONTRACT_TYPES;
    }

    @Override
    @PostConstruct
    @Transactional
    public void initSystemTemplates() {
        log.info("Initializing system auto fulfillment templates...");
        if (count(new LambdaQueryWrapper<AutoFulfillmentTemplate>().eq(AutoFulfillmentTemplate::getType, "system")) > 0) {
            log.info("System auto fulfillment templates already initialized");
            return;
        }
        try {
            List<String> genericFields = Arrays.asList(
                    "合同编号", "合同名称", "甲方名称", "乙方名称",
                    "合同签署日期", "合同有效期", "合同起始时间", "合同终止时间"
            );
            createSystemTemplate("自动履约-通用模板", "operation", genericFields, "自动履约通用字段", true);
        } catch (Exception e) {
            log.error("Failed to initialize auto fulfillment templates", e);
        }
    }

    private void createSystemTemplate(String name, String contractType, List<String> fields,
                                      String description, boolean isDefault) throws JsonProcessingException {
        String fieldsJson = objectMapper.writeValueAsString(fields);
        AutoFulfillmentTemplate t = new AutoFulfillmentTemplate();
        t.setName(name);
        t.setType("system");
        t.setContractType(contractType);
        t.setFields(fieldsJson);
        t.setCreatorId("system");
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        t.setIsDefault(isDefault);
        t.setDescription(description);
        save(t);
    }
}


