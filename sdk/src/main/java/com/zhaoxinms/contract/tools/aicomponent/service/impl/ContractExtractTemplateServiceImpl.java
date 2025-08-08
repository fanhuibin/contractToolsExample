package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.ContractExtractTemplateMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.ContractExtractTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 合同提取信息模板服务实现类
 * 
 * @author zhaoxinms
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ContractExtractTemplateServiceImpl implements ContractExtractTemplateService {

    private final ContractExtractTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 合同类型映射
     */
    private static final Map<String, String> CONTRACT_TYPES = new LinkedHashMap<>();
    
    static {
        CONTRACT_TYPES.put("lease", "租赁合同");
        CONTRACT_TYPES.put("purchase", "购销合同");
        CONTRACT_TYPES.put("labor", "劳动合同");
        CONTRACT_TYPES.put("construction", "建筑合同");
        CONTRACT_TYPES.put("technical", "技术合同");
        CONTRACT_TYPES.put("intellectual", "知识产权合同");
        CONTRACT_TYPES.put("operation", "运营服务合同");
        CONTRACT_TYPES.put("custom", "自定义类型");
    }
    
    @Override
    public List<ContractExtractTemplate> getAllTemplates() {
        return templateMapper.selectList(null);
    }
    
    @Override
    public Optional<ContractExtractTemplate> getTemplateById(Long id) {
        return Optional.ofNullable(templateMapper.selectById(id));
    }
    
    @Override
    public List<ContractExtractTemplate> getTemplatesByContractType(String contractType) {
        return templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getContractType, contractType));
    }
    
    @Override
    public List<ContractExtractTemplate> getSystemTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getType, "system"));
    }
    
    @Override
    public List<ContractExtractTemplate> getUserTemplates(String userId) {
        return templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getCreatorId, userId));
    }
    
    @Override
    @Transactional
    public ContractExtractTemplate createTemplate(ContractExtractTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        // 如果是设置为默认模板，需要取消同类型的其他默认模板
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            ContractExtractTemplate oldDefault = templateMapper.selectOne(new LambdaQueryWrapper<ContractExtractTemplate>()
                    .eq(ContractExtractTemplate::getContractType, template.getContractType())
                    .eq(ContractExtractTemplate::getIsDefault, true));
            if (oldDefault != null) {
                oldDefault.setIsDefault(false);
                templateMapper.updateById(oldDefault);
            }
        }
        templateMapper.insert(template);
        return template;
    }
    
    @Override
    @Transactional
    public ContractExtractTemplate updateTemplate(Long id, ContractExtractTemplate template) {
        ContractExtractTemplate existingTemplate = templateMapper.selectById(id);
        if (existingTemplate == null) {
            throw new IllegalArgumentException("Template not found with id: " + id);
        }
        existingTemplate.setName(template.getName());
        existingTemplate.setContractType(template.getContractType());
        existingTemplate.setFields(template.getFields());
        existingTemplate.setDescription(template.getDescription());
        existingTemplate.setUpdateTime(LocalDateTime.now());

        if (Boolean.TRUE.equals(template.getIsDefault()) && !Boolean.TRUE.equals(existingTemplate.getIsDefault())) {
            ContractExtractTemplate oldDefault = templateMapper.selectOne(new LambdaQueryWrapper<ContractExtractTemplate>()
                    .eq(ContractExtractTemplate::getContractType, template.getContractType())
                    .eq(ContractExtractTemplate::getIsDefault, true));
            if (oldDefault != null) {
                oldDefault.setIsDefault(false);
                templateMapper.updateById(oldDefault);
            }
            existingTemplate.setIsDefault(true);
        }
        templateMapper.updateById(existingTemplate);
        return existingTemplate;
    }
    
    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        // 系统模板不允许删除
        ContractExtractTemplate template = templateMapper.selectById(id);
        if (template != null) {
            if ("system".equals(template.getType())) {
                throw new IllegalArgumentException("System templates cannot be deleted");
            }
            templateMapper.deleteById(id);
        }
    }
    
    @Override
    @Transactional
    public ContractExtractTemplate copyTemplate(Long id, String newName, String userId) {
        ContractExtractTemplate templateDb = templateMapper.selectById(id);
        if (templateDb == null) {
            throw new IllegalArgumentException("Template not found with id: " + id);
        }
        ContractExtractTemplate newTemplate = new ContractExtractTemplate();
        newTemplate.setName(newName);
        newTemplate.setContractType(templateDb.getContractType());
        newTemplate.setFields(templateDb.getFields());
        newTemplate.setType("user");
        newTemplate.setCreatorId(userId);
        newTemplate.setCreateTime(LocalDateTime.now());
        newTemplate.setUpdateTime(LocalDateTime.now());
        newTemplate.setIsDefault(false);
        newTemplate.setDescription(templateDb.getDescription() + " (复制)");
        templateMapper.insert(newTemplate);
        return newTemplate;
    }
    
    @Override
    @Transactional
    public ContractExtractTemplate setDefaultTemplate(Long id, String contractType) {
        // 先取消当前默认模板
        ContractExtractTemplate oldDefault = templateMapper.selectOne(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getContractType, contractType)
                .eq(ContractExtractTemplate::getIsDefault, true));
        if (oldDefault != null) {
            oldDefault.setIsDefault(false);
            templateMapper.updateById(oldDefault);
        }
        
        // 设置新的默认模板
        ContractExtractTemplate template2 = templateMapper.selectById(id);
        if (template2 == null) {
            throw new IllegalArgumentException("Template not found with id: " + id);
        }
        template2.setIsDefault(true);
        template2.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template2);
        return template2;
    }
    
    @Override
    public Optional<ContractExtractTemplate> getDefaultTemplate(String contractType) {
        ContractExtractTemplate tpl = templateMapper.selectOne(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getContractType, contractType)
                .eq(ContractExtractTemplate::getIsDefault, true));
        return Optional.ofNullable(tpl);
    }
    
    @Override
    public List<ContractExtractTemplate> findAllSystemAndUserTemplates(String userId) {
        List<ContractExtractTemplate> system = templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getType, "system"));
        List<ContractExtractTemplate> user = templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getType, "user")
                .eq(ContractExtractTemplate::getCreatorId, userId));
        List<ContractExtractTemplate> result = new ArrayList<>();
        result.addAll(system);
        result.addAll(user);
        return result;
    }
    
    @Override
    public List<ContractExtractTemplate> getTemplatesByContractTypeAndUser(String contractType, String userId) {
        List<ContractExtractTemplate> result = new ArrayList<>();
        
        // 添加系统模板
        result.addAll(templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getContractType, contractType)
                .eq(ContractExtractTemplate::getType, "system")));
        
        // 添加用户模板
        if (userId != null && !userId.isEmpty()) {
            result.addAll(templateMapper.selectList(new LambdaQueryWrapper<ContractExtractTemplate>()
                    .eq(ContractExtractTemplate::getContractType, contractType)
                    .eq(ContractExtractTemplate::getCreatorId, userId)));
        }
        
        return result;
    }
    
    @Override
    @PostConstruct
    @Transactional
    public void initSystemTemplates() {
        log.info("Initializing system contract extract templates...");
        
        // 检查是否已经初始化
        if (templateMapper.selectCount(new LambdaQueryWrapper<ContractExtractTemplate>()
                .eq(ContractExtractTemplate::getType, "system")) > 0) {
            log.info("System templates already initialized");
            return;
        }
        
        try {
            // 通用字段集合，不再单独生成“通用合同模板”，而是合并进其他类型
            List<String> genericFields = Arrays.asList(
                    "合同编号", "合同名称", "甲方名称", "乙方名称",
                    "甲方联系地址", "乙方联系地址", "甲方联系人及电话", "乙方联系人及电话",
                    "合同签署日期", "合同有效期", "合同起始时间", "合同终止时间",
                    "签署人信息", "违约责任条款", "争议解决方式", "适用法律"
            );

            // 租赁合同模板
            createSystemTemplate(
                    "租赁合同模板",
                    "lease",
                    mergeUnique(genericFields, Arrays.asList(
                            "承租方", "出租方", "甲方联系地址", "乙方联系人地址",
                            "甲方联系人电话", "乙方联系人电话", "租赁地址", "租赁面积",
                            "合同编号", "甲方签署日期", "乙方签署日期", "合同有效期",
                            "合同起始时间", "租赁期限", "租赁起始时间", "租赁终止时间",
                            "租赁费用", "租赁费用大写", "每期金额", "每期金额（大写)",
                            "履约保证金", "付款方式", "支付周期", "支付节点",
                            "甲方违约金", "乙方违约金", "甲方违约金（大写)", "乙方违约金（大写)",
                            "甲方签署人", "乙方签署人"
                    )),
                    "适用于各类租赁合同",
                    true
            );
            
            // 购销合同模板
            createSystemTemplate(
                    "购销合同模板",
                    "purchase",
                    mergeUnique(genericFields, Arrays.asList(
                            "甲方公司名称", "乙方公司名称", "甲方通信地址", "乙方通信地址",
                            "合同名称", "合同编号", "合同总金额小写", "币种",
                            "合同总金额大写", "付款节点", "付款比例", "付款金额（小写）",
                            "付款金额（大写）", "合同生效日期", "合同终止日期", "合同有效期",
                            "交货单位", "交货地点", "交货时间", "验收时间",
                            "运输方式", "运输费用", "增值率税率", "税额",
                            "发票类型", "服务期限", "保证金金额（小写）", "保证金金额（大写）",
                            "保证金比例", "保证金归还节点", "争议仲裁地", "甲方纳税人识别号",
                            "乙方纳税人识别号", "甲方注册地址", "乙方注册地址", "甲方登记电话",
                            "乙方登记电话", "甲方开户行", "乙方开户行", "甲方银行账号名称",
                            "乙方银行账号名称", "甲方银行账号", "乙方银行账号", "违约内容",
                            "甲方邮政编码", "乙方邮政编码", "甲方法人", "乙方法人",
                            "甲方业务联系人", "乙方业务联系人", "甲方业务联系人电话", "甲方业务联系人邮箱",
                            "乙方业务联系人电话", "乙方业务联系人邮箱", "甲方签署日期", "乙方签署日期",
                            "甲方法人或代表人是否签字", "乙方法人或代表人是否签字"
                    )),
                    "适用于各类购销合同",
                    true
            );
            
            // 劳动合同模板
            createSystemTemplate(
                    "劳动合同模板",
                    "labor",
                    mergeUnique(genericFields, Arrays.asList(
                            "员工姓名", "身份证号", "职位名称", "工作地点",
                            "合同期限类型", "合同起止日期", "试用期期限", "试用期工资",
                            "转正后工资", "工资构成明细", "工资支付时间", "工作时间安排",
                            "休息休假制度", "社会保险缴纳", "福利待遇", "保密条款",
                            "竞业限制条款", "培训服务期", "解除合同条件"
                    )),
                    "适用于劳动合同",
                    true
            );
            
            // 建筑合同模板
            createSystemTemplate(
                    "建筑合同模板",
                    "construction",
                    mergeUnique(genericFields, Arrays.asList(
                            "工程名称", "工程地点", "工程内容", "工程范围",
                            "承包方式", "合同价款", "付款进度安排", "工程期限",
                            "开工日期", "竣工日期", "质量标准", "工程变更条款",
                            "工程验收标准", "质保金比例", "质保期限", "安全责任条款",
                            "工程进度报告要求", "材料供应方式", "违约责任"
                    )),
                    "适用于建筑工程承包合同",
                    true
            );
            
            // 技术合同模板
            createSystemTemplate(
                    "技术合同模板",
                    "technical",
                    mergeUnique(genericFields, Arrays.asList(
                            "技术项目名称", "技术内容", "技术指标", "技术成果形式",
                            "技术开发方式", "研究开发经费", "经费支付方式", "技术资料交付",
                            "技术指导要求", "验收标准", "知识产权归属", "技术保密条款",
                            "后续改进技术归属", "技术风险责任", "违约金计算方式", "技术培训条款"
                    )),
                    "适用于技术开发、技术转让等技术合同",
                    true
            );
            
            // 知识产权合同模板
            createSystemTemplate(
                    "知识产权合同模板",
                    "intellectual",
                    mergeUnique(genericFields, Arrays.asList(
                            "知识产权类型", "知识产权名称", "注册/登记号", "许可/转让范围",
                            "地域限制", "使用期限", "许可/转让费用", "支付方式",
                            "权利保证条款", "侵权责任", "改进技术归属", "再许可权限",
                            "合同备案要求", "权利维持义务", "合同终止条件"
                    )),
                    "适用于知识产权许可、转让合同",
                    true
            );
            
            // 运营服务合同模板
            createSystemTemplate(
                    "运营服务合同模板",
                    "operation",
                    mergeUnique(genericFields, Arrays.asList(
                            "服务内容描述", "服务标准", "服务期限", "服务地点",
                            "服务人员要求", "服务时间", "服务费用", "费用支付周期",
                            "绩效考核标准", "数据保密条款", "服务报告要求", "突发事件处理",
                            "服务变更流程", "服务终止条件", "过渡期安排"
                    )),
                    "适用于运营服务类合同",
                    true
            );
            
            log.info("System contract extract templates initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize system templates", e);
        }
    }
    
    @Override
    public Map<String, String> getAllContractTypes() {
        return CONTRACT_TYPES;
    }
    
    /**
     * 创建系统预设模板
     * 
     * @param name 模板名称
     * @param contractType 合同类型
     * @param fields 字段列表
     * @param description 描述
     * @param isDefault 是否默认
     * @throws JsonProcessingException JSON处理异常
     */
    private void createSystemTemplate(String name, String contractType, List<String> fields, 
                                     String description, boolean isDefault) throws JsonProcessingException {
        // 将字段列表转换为JSON
        String fieldsJson = objectMapper.writeValueAsString(fields);
        
        ContractExtractTemplate template = ContractExtractTemplate.builder()
                .name(name)
                .type("system")
                .contractType(contractType)
                .fields(fieldsJson)
                .creatorId("system")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDefault(isDefault)
                .description(description)
                .build();
        
        templateMapper.insert(template);
    }

    /**
     * 合并两个字段列表，按插入顺序去重
     */
    private List<String> mergeUnique(List<String> base, List<String> extra) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.addAll(base);
        set.addAll(extra);
        return new ArrayList<>(set);
    }
}