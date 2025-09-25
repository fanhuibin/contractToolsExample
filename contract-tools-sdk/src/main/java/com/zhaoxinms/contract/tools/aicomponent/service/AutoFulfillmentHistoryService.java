package com.zhaoxinms.contract.tools.aicomponent.service;

import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentHistory;
import java.util.List;

public interface AutoFulfillmentHistoryService {
    AutoFulfillmentHistory saveHistory(String fileName, String extractedContent, String userId);
    List<AutoFulfillmentHistory> getHistoryByUserId(String userId);
}


