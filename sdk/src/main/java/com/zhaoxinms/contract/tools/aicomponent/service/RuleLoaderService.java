package com.zhaoxinms.contract.tools.aicomponent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.rules.ContractRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleLoaderService {

    private final ObjectMapper objectMapper;
    private final Map<String, ContractRules> cache = new ConcurrentHashMap<>();

    public Optional<ContractRules> loadRules(String contractType) {
        try {
            if (contractType == null || contractType.trim().isEmpty()) {
                return Optional.empty();
            }
            ContractRules cached = cache.get(contractType);
            if (cached != null) {
                return Optional.of(cached);
            }
            String path = "contract-extract-rules/" + contractType + ".json";
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                log.warn("Rules file not found for contractType={}, path={}", contractType, path);
                return Optional.empty();
            }
            try (InputStream is = resource.getInputStream()) {
                ContractRules rules = objectMapper.readValue(is, ContractRules.class);
                cache.put(contractType, rules);
                return Optional.of(rules);
            }
        } catch (Exception e) {
            log.warn("Failed to load rules for contractType={}", contractType, e);
            return Optional.empty();
        }
    }
}


