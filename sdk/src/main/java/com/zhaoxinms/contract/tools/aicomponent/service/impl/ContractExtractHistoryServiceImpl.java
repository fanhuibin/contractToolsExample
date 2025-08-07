package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
import com.zhaoxinms.contract.tools.aicomponent.repository.ContractExtractHistoryRepository;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractHistoryService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Primary
@AllArgsConstructor
public class ContractExtractHistoryServiceImpl implements ContractExtractHistoryService {

    private final ContractExtractHistoryRepository historyRepository;

    @Override
    public ContractExtractHistory saveHistory(String fileName, String extractedContent, String userId) {
        ContractExtractHistory history = new ContractExtractHistory();
        history.setFileName(fileName);
        history.setExtractedContent(extractedContent);
        history.setUserId(userId);
        history.setExtractTime(LocalDateTime.now());
        return historyRepository.save(history);
    }

    @Override
    public List<ContractExtractHistory> getHistoryByUserId(String userId) {
        return historyRepository.findByUserIdOrderByExtractTimeDesc(userId);
    }
}

