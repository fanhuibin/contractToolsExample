package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.AutoFulfillmentHistoryMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentHistory;
import com.zhaoxinms.contract.tools.aicomponent.service.AutoFulfillmentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class AutoFulfillmentHistoryServiceImpl implements AutoFulfillmentHistoryService {

    private final AutoFulfillmentHistoryMapper historyMapper;

    @Override
    public AutoFulfillmentHistory saveHistory(String fileName, String extractedContent, String userId) {
        AutoFulfillmentHistory h = new AutoFulfillmentHistory();
        h.setFileName(fileName);
        h.setExtractedContent(extractedContent);
        h.setUserId(userId);
        h.setExtractTime(LocalDateTime.now());
        historyMapper.insert(h);
        return h;
    }

    @Override
    public List<AutoFulfillmentHistory> getHistoryByUserId(String userId) {
        return historyMapper.selectList(new LambdaQueryWrapper<AutoFulfillmentHistory>()
                .eq(AutoFulfillmentHistory::getUserId, userId)
                .orderByDesc(AutoFulfillmentHistory::getExtractTime));
    }
}


