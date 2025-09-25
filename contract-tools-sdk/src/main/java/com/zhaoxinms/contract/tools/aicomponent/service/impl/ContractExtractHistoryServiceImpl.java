package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.ContractExtractHistoryMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
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

    private final ContractExtractHistoryMapper historyMapper;

    @Override
    public ContractExtractHistory saveHistory(String fileName, String extractedContent, String userId) {
        ContractExtractHistory history = new ContractExtractHistory();
        history.setFileName(fileName);
        history.setExtractedContent(extractedContent);
        history.setUserId(userId);
        history.setExtractTime(LocalDateTime.now());
        historyMapper.insert(history);
        return history;
    }

    @Override
    public List<ContractExtractHistory> getHistoryByUserId(String userId) {
        return historyMapper.selectList(new LambdaQueryWrapper<ContractExtractHistory>()
                .eq(ContractExtractHistory::getUserId, userId)
                .orderByDesc(ContractExtractHistory::getExtractTime));
    }
}

