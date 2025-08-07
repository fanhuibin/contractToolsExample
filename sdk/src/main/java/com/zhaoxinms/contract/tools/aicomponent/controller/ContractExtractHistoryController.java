package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/contract/history")
public class ContractExtractHistoryController {

    private final ContractExtractHistoryService historyService;

    @Autowired
    public ContractExtractHistoryController(@Qualifier("contractExtractHistoryServiceImpl") ContractExtractHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam String userId) {
        List<ContractExtractHistory> historyList = historyService.getHistoryByUserId(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", historyList));
    }
}

