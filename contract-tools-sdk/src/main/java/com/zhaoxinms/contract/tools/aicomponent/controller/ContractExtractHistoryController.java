package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractHistoryService;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/contract/history")
@RequiredArgsConstructor
public class ContractExtractHistoryController {

    private final ContractExtractHistoryService historyService;

    @GetMapping("/list")
    public ApiResponse<List<ContractExtractHistory>> getHistory(@RequestParam String userId) {
        List<ContractExtractHistory> historyList = historyService.getHistoryByUserId(userId);
        return ApiResponse.success(historyList);
    }
}

