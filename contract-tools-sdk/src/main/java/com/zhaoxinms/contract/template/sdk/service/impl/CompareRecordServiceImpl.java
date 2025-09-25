package com.zhaoxinms.contract.template.sdk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.template.sdk.entity.CompareRecord;
import com.zhaoxinms.contract.template.sdk.mapper.CompareRecordMapper;
import com.zhaoxinms.contract.template.sdk.service.CompareRecordService;
import org.springframework.stereotype.Service;

@Service
public class CompareRecordServiceImpl implements CompareRecordService {
    private final CompareRecordMapper mapper;
    private final ObjectMapper objectMapper;

    public CompareRecordServiceImpl(CompareRecordMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(CompareRecord record) {
        mapper.insert(record);
    }

    @Override
    public CompareRecord getByBizId(String bizId) {
        return mapper.findByBizId(bizId);
    }
}


