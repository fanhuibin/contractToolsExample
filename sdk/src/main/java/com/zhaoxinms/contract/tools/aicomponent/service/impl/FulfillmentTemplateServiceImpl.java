package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaoxinms.contract.tools.aicomponent.mapper.FulfillmentTemplateMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.FulfillmentTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 履约任务模板服务实现类
 * 提供模板管理的具体业务逻辑
 */
@Service
public class FulfillmentTemplateServiceImpl 
    extends ServiceImpl<FulfillmentTemplateMapper, FulfillmentTemplate> 
    implements FulfillmentTemplateService {

    @Override
    public List<FulfillmentTemplate> listTemplatesByType(String contractType, String userId) {
        LambdaQueryWrapper<FulfillmentTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FulfillmentTemplate::getContractType, contractType)
                   .and(wrapper -> wrapper
                       .eq(FulfillmentTemplate::getUserId, userId)
                       .or()
                       .eq(FulfillmentTemplate::getType, "system")
                   );
        return list(queryWrapper);
    }

    @Override
    public FulfillmentTemplate getDefaultTemplate(String contractType, String userId) {
        return baseMapper.selectDefaultTemplate(contractType, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentTemplate createTemplate(FulfillmentTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setType("user");  // 用户自定义模板
        save(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentTemplate updateTemplate(FulfillmentTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentTemplate copyTemplate(Long templateId, String newName, String userId) {
        // 获取源模板
        FulfillmentTemplate sourceTemplate = getById(templateId);
        if (sourceTemplate == null) {
            throw new RuntimeException("源模板不存在");
        }

        // 创建新模板
        FulfillmentTemplate newTemplate = BeanUtil.copyProperties(sourceTemplate, FulfillmentTemplate.class);
        newTemplate.setId(null);  // 清除原ID
        newTemplate.setName(newName);
        newTemplate.setUserId(userId);
        newTemplate.setIsDefault(false);
        newTemplate.setCreateTime(LocalDateTime.now());
        newTemplate.setUpdateTime(LocalDateTime.now());

        save(newTemplate);
        return newTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentTemplate setDefaultTemplate(Long templateId, String contractType) {
        // 取消当前合同类型的其他默认模板
        LambdaUpdateWrapper<FulfillmentTemplate> clearDefaultWrapper = new LambdaUpdateWrapper<>();
        clearDefaultWrapper.eq(FulfillmentTemplate::getContractType, contractType)
                           .set(FulfillmentTemplate::getIsDefault, false);
        update(clearDefaultWrapper);

        // 设置新的默认模板
        FulfillmentTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        template.setIsDefault(true);
        updateById(template);
        return template;
    }
}
