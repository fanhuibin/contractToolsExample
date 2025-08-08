package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.template.sdk.mapper.TemplateDesignRecordMapper;
import com.zhaoxinms.contract.template.sdk.service.TemplateDesignRecordService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Primary
public class TemplateDesignRecordServiceImpl implements TemplateDesignRecordService {

    private final TemplateDesignRecordMapper mapper;

    public TemplateDesignRecordServiceImpl(TemplateDesignRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateDesignRecord save(String id, String templateId, String fileId, String elementsJson) {
        TemplateDesignRecord record = id == null || id.isEmpty() ? null : mapper.selectById(id);
        if (record == null) {
            record = new TemplateDesignRecord();
            record.setId(id == null || id.isEmpty() ? UUID.randomUUID().toString() : id);
            record.setCreatedAt(LocalDateTime.now());
        }
        record.setUpdatedAt(LocalDateTime.now());
        record.setTemplateId(templateId);
        record.setFileId(fileId);
        record.setElementsJson(elementsJson);
        if (mapper.selectById(record.getId()) == null) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
        return mapper.selectById(record.getId());
    }

    @Override
    public TemplateDesignRecord getById(String id) {
        return mapper.selectById(id);
    }
}


