package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentHistory;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentHistoryService;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/auto-fulfillment/history")
@RequiredArgsConstructor
public class AutoFulfillmentHistoryController {

    private final AutoFulfillmentHistoryService historyService;

    @GetMapping("/list")
    public Result<List<AutoFulfillmentHistory>> getHistory(@RequestParam String userId) {
        return Result.success(historyService.getHistoryByUserId(userId));
    }
}


