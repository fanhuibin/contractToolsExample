package com.zhaoxinms.contract.tools.aicomponent.service;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
import java.util.List;

public interface ContractExtractHistoryService {
    ContractExtractHistory saveHistory(String fileName, String extractedContent, String userId);
    List<ContractExtractHistory> getHistoryByUserId(String userId);
}

