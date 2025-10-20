package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleFileService;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleStoreService;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/rules")
@RequiredArgsConstructor
@Slf4j
public class RuleAdminController {

    private final RuleFileService ruleFileService; // legacy file loader (backward compatible)
    private final RuleStoreService ruleStoreService;

    @GetMapping("/models")
    public ApiResponse<List<Map<String, String>>> listModels() {
        try {
            List<Map<String, String>> db = ruleStoreService.listModels();
            if (db != null && !db.isEmpty()) return ApiResponse.success("ok", db);
            return ApiResponse.success("ok", ruleFileService.listModels());
        } catch (Exception e) {
            log.error("list models error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * Legacy by contractType (deprecated)
     */
    @GetMapping("/{contractType}")
    public ApiResponse<JsonNode> readRule(@PathVariable String contractType) {
        try {
            return ruleStoreService.readRule(contractType)
                    .map(j -> ApiResponse.success("ok", j))
                    .orElseGet(() -> {
                        try {
                            return ApiResponse.success("ok", ruleFileService.readRule(contractType));
                        } catch (Exception e) {
                            return ApiResponse.businessError(e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("read rule error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * Legacy by contractType (deprecated)
     */
    @PutMapping("/{contractType}")
    public ApiResponse<String> saveRule(@PathVariable String contractType, @RequestBody JsonNode content) {
        try {
            String name = content.has("name") ? content.get("name").asText() : contractType;
            // Save to DB first
            ruleStoreService.saveRule(contractType, content, name);
            return ApiResponse.success("saved", "OK");
        } catch (Exception e) {
            log.error("save rule error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * New API: read rule by templateId
     */
    @GetMapping("/template/{templateId}")
    public ApiResponse<JsonNode> readRuleByTemplate(@PathVariable Long templateId) {
        try {
            return ruleStoreService.readRuleByTemplateId(templateId)
                    .map(j -> ApiResponse.success("ok", j))
                    .orElseGet(() -> ApiResponse.success("ok", null));
        } catch (Exception e) {
            log.error("read rule by template error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * New API (alt): read rule by templateId via query param to avoid path conflicts
     */
    @GetMapping("/by-template")
    public ApiResponse<JsonNode> readRuleByTemplateQuery(@RequestParam("templateId") Long templateId) {
        try {
            return ruleStoreService.readRuleByTemplateId(templateId)
                    .map(j -> ApiResponse.success("ok", j))
                    .orElseGet(() -> ApiResponse.success("ok", null));
        } catch (Exception e) {
            log.error("read rule by template (query) error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * New API: save rule by templateId
     * Body should include at least name/prompt/fields; contractType used for compatibility display
     */
    @PutMapping("/template/{templateId}")
    public ApiResponse<String> saveRuleByTemplate(@PathVariable Long templateId, @RequestBody JsonNode content) {
        try {
            String name = content.has("name") ? content.get("name").asText() : ("template-" + templateId);
            String contractType = content.has("contractType") ? content.get("contractType").asText() : null;
            ruleStoreService.saveRuleByTemplateId(templateId, content, name, contractType);
            return ApiResponse.success("saved", "OK");
        } catch (Exception e) {
            log.error("save rule by template error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }

    /**
     * New API (alt): save rule by templateId via query param to avoid path conflicts
     */
    @PutMapping("/by-template")
    public ApiResponse<String> saveRuleByTemplateQuery(@RequestParam("templateId") Long templateId, @RequestBody JsonNode content) {
        try {
            String name = content.has("name") ? content.get("name").asText() : ("template-" + templateId);
            String contractType = content.has("contractType") ? content.get("contractType").asText() : null;
            ruleStoreService.saveRuleByTemplateId(templateId, content, name, contractType);
            return ApiResponse.success("saved", "OK");
        } catch (Exception e) {
            log.error("save rule by template (query) error", e);
            return ApiResponse.businessError(e.getMessage());
        }
    }
}


