package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.template.sdk.mapper.TemplateDesignRecordMapper;
import com.zhaoxinms.contract.template.sdk.service.TemplateDesignRecordService;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Primary
public class TemplateDesignRecordServiceImpl implements TemplateDesignRecordService {

    private final TemplateDesignRecordMapper mapper;
    
    @Autowired(required = false)
    private FileInfoService fileInfoService;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    public TemplateDesignRecordServiceImpl(TemplateDesignRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateDesignRecord save(String id, String templateId, String fileId, String elementsJson) {
        // 解析 ID（兼容 String 参数）
        Long longId = null;
        if (id != null && !id.trim().isEmpty()) {
            try {
                longId = Long.parseLong(id.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的ID格式: " + id);
            }
        }
        
        // 解析 fileId
        Long longFileId = null;
        if (fileId != null && !fileId.trim().isEmpty()) {
            try {
                longFileId = Long.parseLong(fileId.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的文件ID格式: " + fileId);
            }
        }
        
        // 唯一性校验：template_id 不可重复
        if (templateId != null && !templateId.isEmpty()) {
            TemplateDesignRecord exist = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                    .eq("template_id", templateId)
                    .last("limit 1"));
            if (exist != null && (longId == null || !exist.getId().equals(longId))) {
                throw new IllegalArgumentException("模板ID已存在，请更换模板ID");
            }
        }
        
        TemplateDesignRecord record = longId == null ? null : mapper.selectById(longId);
        if (record == null) {
            record = new TemplateDesignRecord();
            // 使用雪花算法生成 ID
            record.setId(SnowflakeIdGenerator.getInstance().nextId());
            record.setCreatedAt(LocalDateTime.now());
            record.setStatus("DRAFT"); // 默认草稿状态
        }
        record.setUpdatedAt(LocalDateTime.now());
        record.setTemplateId(templateId);
        record.setFileId(longFileId);
        record.setElementsJson(elementsJson);
        if (mapper.selectById(record.getId()) == null) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
        return mapper.selectById(record.getId());
    }

    @Override
    public TemplateDesignRecord saveTemplate(TemplateDesignRecord record) {
        if (record.getId() == null) {
            // 使用雪花算法生成 ID
            record.setId(SnowflakeIdGenerator.getInstance().nextId());
            record.setCreatedAt(LocalDateTime.now());
            if (record.getStatus() == null || record.getStatus().isEmpty()) {
                record.setStatus("DRAFT");
            }
        }
        record.setUpdatedAt(LocalDateTime.now());
        
        if (mapper.selectById(record.getId()) == null) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
        return mapper.selectById(record.getId());
    }

    @Override
    public TemplateDesignRecord createNewVersion(String sourceId, String newVersion) {
        if (sourceId == null || sourceId.isEmpty()) {
            throw new IllegalArgumentException("源版本ID不能为空");
        }
        if (newVersion == null || newVersion.isEmpty()) {
            throw new IllegalArgumentException("新版本号不能为空");
        }
        
        // 获取源版本
        TemplateDesignRecord source = mapper.selectById(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("源版本不存在: " + sourceId);
        }
        
        String templateCode = source.getTemplateCode();
        if (templateCode == null || templateCode.isEmpty()) {
            throw new IllegalArgumentException("源版本缺少模板编码");
        }
        
        // 验证新版本号是否已存在
        TemplateDesignRecord existingVersion = mapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .eq("template_code", templateCode)
                .eq("version", newVersion)
                .last("limit 1")
        );
        if (existingVersion != null) {
            throw new IllegalArgumentException("版本号已存在: " + newVersion);
        }
        
        // 复制文件
        Long newFileId = null;
        if (source.getFileId() != null && fileInfoService != null) {
            try {
                FileInfo sourceFile = fileInfoService.getById(String.valueOf(source.getFileId()));
                if (sourceFile != null && sourceFile.getFilePath() != null) {
                    File srcFile = new File(sourceFile.getFilePath());
                    if (srcFile.exists()) {
                        // 生成新文件路径（文件名使用 UUID 避免冲突）
                        String newFileName = "v" + newVersion + "_" + sourceFile.getOriginalName();
                        File destFile = new File(srcFile.getParent(), UUID.randomUUID().toString() + "_" + newFileName);
                        
                        // 复制文件
                        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        
                        // 注册新文件
                        FileInfo newFile = fileInfoService.registerFile(
                            newFileName,
                            sourceFile.getFileExtension(),
                            destFile.getAbsolutePath(),
                            destFile.length()
                        );
                        newFileId = newFile.getId();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("复制文件失败: " + e.getMessage(), e);
            } catch (Exception e) {
                // 如果文件复制失败，记录日志但继续创建版本
                System.err.println("复制文件失败，将创建不带文件的版本: " + e.getMessage());
            }
        }
        
        // 创建新版本记录（复制所有字段）
        TemplateDesignRecord newRecord = new TemplateDesignRecord();
        // 使用雪花算法生成 ID
        newRecord.setId(SnowflakeIdGenerator.getInstance().nextId());
        newRecord.setTemplateCode(source.getTemplateCode());
        newRecord.setTemplateName(source.getTemplateName());
        newRecord.setVersion(newVersion);
        newRecord.setFileId(newFileId != null ? newFileId : source.getFileId()); // 如果复制失败，使用原文件ID
        newRecord.setElementsJson(source.getElementsJson()); // 复制设计元素
        newRecord.setStatus("DRAFT");
        newRecord.setDescription("基于v" + source.getVersion() + "创建的新版本");
        newRecord.setCreatedAt(LocalDateTime.now());
        newRecord.setUpdatedAt(LocalDateTime.now());
        
        mapper.insert(newRecord);
        return mapper.selectById(newRecord.getId());
    }

    @Override
    public TemplateDesignRecord publishVersion(String id) {
        TemplateDesignRecord record = mapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        
        String templateCode = record.getTemplateCode();
        if (templateCode != null && !templateCode.isEmpty()) {
            // 将同一编码下的其他已发布版本改为草稿状态
            mapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                    .eq("template_code", templateCode)
                    .eq("status", "PUBLISHED"))
                .forEach(r -> {
                    if (!r.getId().equals(id)) {
                        r.setStatus("DRAFT");
                        r.setUpdatedAt(LocalDateTime.now());
                        mapper.updateById(r);
                    }
                });
        }
        
        // 发布当前版本
        record.setStatus("PUBLISHED");
        record.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(record);
        
        return mapper.selectById(id);
    }

    @Override
    public TemplateDesignRecord updateStatus(String id, String status) {
        TemplateDesignRecord record = mapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        
        record.setStatus(status);
        record.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(record);
        
        return mapper.selectById(id);
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
    public TemplateDesignRecord getLatestByCode(String templateCode) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .eq("template_code", templateCode)
                .orderByDesc("version")
                .last("limit 1"));
    }

    @Override
    public TemplateDesignRecord getPublishedByCode(String templateCode) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .eq("template_code", templateCode)
                .eq("status", "PUBLISHED")
                .orderByDesc("version")
                .last("limit 1"));
    }

    @Override
    public List<TemplateDesignRecord> getVersionsByCode(String templateCode) {
        return mapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .eq("template_code", templateCode)
                .orderByDesc("version"));
    }

    @Override
    public List<TemplateDesignRecord> listAll() {
        return mapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TemplateDesignRecord>()
                .ne("status", "DELETED")
                .orderByDesc("updated_at"));
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isEmpty()) return false;
        TemplateDesignRecord record = mapper.selectById(id);
        if (record == null) return false;
        
        // 软删除：更新状态为DELETED
        record.setStatus("DELETED");
        record.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(record);
        return true;
    }

    @Override
    public boolean hardDeleteById(String id) {
        if (id == null || id.isEmpty()) return false;
        return mapper.deleteById(id) > 0;
    }
}


