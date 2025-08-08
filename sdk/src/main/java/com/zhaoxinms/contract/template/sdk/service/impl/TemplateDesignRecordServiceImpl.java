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
        TemplateDesignRecord record = new TemplateDesignRecord();
        if (id == null || id.isEmpty()) {
            record.setId(UUID.randomUUID().toString());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setTemplateId(templateId);
            record.setFileId(fileId);
            record.setElementsJson(elementsJson);
            mapper.insert(record);
            return record;
        } else {
            record.setId(id);
            record.setTemplateId(templateId);
            record.setFileId(fileId);
            record.setElementsJson(elementsJson);
            record.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(record);
            return mapper.selectById(id);
        }
    }

    @Override
    public TemplateDesignRecord getById(String id) {
        return mapper.selectById(id);
    }
}


