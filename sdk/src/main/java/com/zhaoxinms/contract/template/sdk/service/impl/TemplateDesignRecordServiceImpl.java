package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.template.sdk.mapper.TemplateDesignRecordMapper;
import com.zhaoxinms.contract.template.sdk.service.TemplateDesignRecordService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
        // 唯一性校验：template_id 不可重复
        if (templateId != null && !templateId.isEmpty()) {
            TemplateDesignRecord exist = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                    .eq("template_id", templateId)
                    .last("limit 1"));
            if (exist != null && (id == null || id.isEmpty() || !exist.getId().equals(id))) {
                throw new IllegalArgumentException("模板ID已存在，请更换模板ID");
            }
        }
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

    @Override
    public TemplateDesignRecord getByTemplateId(String templateId) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .eq("template_id", templateId)
                .last("limit 1"));
    }

    @Override
    public List<TemplateDesignRecord> listAll() {
        return mapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .orderByDesc("updated_at"));
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isEmpty()) return false;
        return mapper.deleteById(id) > 0;
    }
}


