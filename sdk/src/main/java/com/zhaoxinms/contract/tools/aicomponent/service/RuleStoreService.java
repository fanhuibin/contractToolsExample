package com.zhaoxinms.contract.tools.aicomponent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.ContractRuleMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.ContractRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleStoreService {

    private final ContractRuleMapper ruleMapper;
    private final ObjectMapper objectMapper;

    public List<Map<String, String>> listModels() {
        List<ContractRule> list = ruleMapper.selectList(null);
        List<Map<String, String>> result = new ArrayList<>();
        for (ContractRule r : list) {
            Map<String, String> m = new LinkedHashMap<>();
            // Keep compatibility fields for legacy UI if ever used
            m.put("contractType", r.getContractType());
            m.put("name", Optional.ofNullable(r.getName()).orElse(r.getContractType()));
            m.put("templateId", String.valueOf(Optional.ofNullable(r.getTemplateId()).orElse(-1L)));
            result.add(m);
        }
        result.sort(Comparator.comparing(m -> m.getOrDefault("contractType", "")));
        return result;
    }

    /**
     * Legacy by contractType (deprecated)
     */
    public Optional<JsonNode> readRule(String contractType) {
        ContractRule r = ruleMapper.selectOne(new LambdaQueryWrapper<ContractRule>().eq(ContractRule::getContractType, contractType));
        if (r == null) return Optional.empty();
        try {
            return Optional.ofNullable(objectMapper.readTree(r.getContentJson()));
        } catch (Exception e) {
            log.warn("failed to parse rule json: {}", r.getContractType(), e);
            return Optional.empty();
        }
    }

    /**
     * Legacy by contractType (deprecated)
     */
    public void saveRule(String contractType, JsonNode content, String name) throws Exception {
        ContractRule r = ruleMapper.selectOne(new LambdaQueryWrapper<ContractRule>().eq(ContractRule::getContractType, contractType));
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        if (r == null) {
            r = new ContractRule();
            r.setContractType(contractType);
            r.setName(name);
            r.setContentJson(json);
            r.setCreateTime(LocalDateTime.now());
            r.setUpdateTime(LocalDateTime.now());
            ruleMapper.insert(r);
        } else {
            r.setName(name);
            r.setContentJson(json);
            r.setUpdateTime(LocalDateTime.now());
            ruleMapper.updateById(r);
        }
    }

    /**
     * Read rule by templateId (new)
     */
    public Optional<JsonNode> readRuleByTemplateId(Long templateId) {
        if (templateId == null) return Optional.empty();
        ContractRule r = ruleMapper.selectOne(new LambdaQueryWrapper<ContractRule>().eq(ContractRule::getTemplateId, templateId));
        if (r == null) return Optional.empty();
        try {
            return Optional.ofNullable(objectMapper.readTree(r.getContentJson()));
        } catch (Exception e) {
            log.warn("failed to parse rule json by templateId: {}", templateId, e);
            return Optional.empty();
        }
    }

    /**
     * Save rule by templateId (new)
     */
    public void saveRuleByTemplateId(Long templateId, JsonNode content, String name, String contractType) throws Exception {
        if (templateId == null) throw new IllegalArgumentException("templateId is required");
        ContractRule r = ruleMapper.selectOne(new LambdaQueryWrapper<ContractRule>().eq(ContractRule::getTemplateId, templateId));
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        if (r == null) {
            r = new ContractRule();
            r.setTemplateId(templateId);
            r.setContractType(contractType);
            r.setName(name);
            r.setContentJson(json);
            r.setCreateTime(LocalDateTime.now());
            r.setUpdateTime(LocalDateTime.now());
            ruleMapper.insert(r);
        } else {
            r.setName(name);
            r.setContentJson(json);
            r.setUpdateTime(LocalDateTime.now());
            ruleMapper.updateById(r);
        }
    }

    /**
     * Upsert fields into existing rule JSON by templateId: only add missing fields, never delete.
     */
    public void upsertFieldsByTemplateId(Long templateId, Collection<String> fieldNames, String name, String contractType) {
        if (templateId == null || fieldNames == null) return;
        try {
            JsonNode existing = readRuleByTemplateId(templateId).orElseGet(() -> objectMapper.createObjectNode());
            var root = existing.isObject() ? existing.deepCopy() : objectMapper.createObjectNode();

            // ensure fields object exists
            var fieldsNode = root.get("fields");
            if (fieldsNode == null || !fieldsNode.isObject()) {
                fieldsNode = objectMapper.createObjectNode();
                ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("fields", fieldsNode);
            }

            // add missing fields with minimal default rule
            for (String f : fieldNames) {
                if (!fieldsNode.has(f)) {
                    var def = objectMapper.createObjectNode();
                    def.put("type", "string");
                    def.put("required", false);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) fieldsNode).set(f, def);
                }
            }

            saveRuleByTemplateId(templateId, root, name, contractType);
        } catch (Exception e) {
            log.warn("upsertFieldsByTemplateId failed: {}", templateId, e);
        }
    }
}


