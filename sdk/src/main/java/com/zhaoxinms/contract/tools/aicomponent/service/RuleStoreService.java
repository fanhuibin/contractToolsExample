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
            m.put("contractType", r.getContractType());
            m.put("name", Optional.ofNullable(r.getName()).orElse(r.getContractType()));
            result.add(m);
        }
        result.sort(Comparator.comparing(m -> m.getOrDefault("contractType", "")));
        return result;
    }

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
}


