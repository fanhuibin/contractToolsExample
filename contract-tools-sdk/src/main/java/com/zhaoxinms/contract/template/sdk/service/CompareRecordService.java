package com.zhaoxinms.contract.template.sdk.service;

import com.zhaoxinms.contract.template.sdk.entity.CompareRecord;

public interface CompareRecordService {
    void save(CompareRecord record);
    CompareRecord getByBizId(String bizId);
}


