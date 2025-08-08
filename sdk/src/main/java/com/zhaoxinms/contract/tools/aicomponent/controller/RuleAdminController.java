package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.aicomponent.service.RuleFileService;
import com.zhaoxinms.contract.tools.common.Result;
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
    private final com.zhaoxinms.contract.tools.aicomponent.service.RuleStoreService ruleStoreService;

    @GetMapping("/models")
    public Result<List<Map<String, String>>> listModels() {
        try {
            List<Map<String, String>> db = ruleStoreService.listModels();
            if (db != null && !db.isEmpty()) return Result.success("ok", db);
            return Result.success("ok", ruleFileService.listModels());
        } catch (Exception e) {
            log.error("list models error", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{contractType}")
    public Result<JsonNode> readRule(@PathVariable String contractType) {
        try {
            return ruleStoreService.readRule(contractType)
                    .map(j -> Result.success("ok", j))
                    .orElseGet(() -> {
                        try {
                            return Result.success("ok", ruleFileService.readRule(contractType));
                        } catch (Exception e) {
                            return Result.error(e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("read rule error", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{contractType}")
    public Result<String> saveRule(@PathVariable String contractType, @RequestBody JsonNode content) {
        try {
            String name = content.has("name") ? content.get("name").asText() : contractType;
            // Save to DB first
            ruleStoreService.saveRule(contractType, content, name);
            return Result.success("saved", "OK");
        } catch (Exception e) {
            log.error("save rule error", e);
            return Result.error(e.getMessage());
        }
    }
}


