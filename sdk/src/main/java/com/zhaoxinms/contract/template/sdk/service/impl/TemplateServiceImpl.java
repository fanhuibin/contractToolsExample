package com.zhaoxinms.contract.template.sdk.service.impl;

import com.zhaoxinms.contract.tools.api.dto.*;
import com.zhaoxinms.contract.tools.api.service.TemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板服务实现类
 * 此实现类提供具体的字段信息查询逻辑
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    @Override
    public FieldResponse getFields() {
        FieldResponse response = new FieldResponse();
        
        // 基础字段数据
        List<BaseField> baseFields = new ArrayList<>();
        BaseField baseField1 = new BaseField();
        baseField1.setId("base_001");
        baseField1.setName("合同名称");
        baseField1.setCode("contract_name");
        baseField1.setIsRichText(false);
        baseField1.setSampleValue("测试合同名称");
        baseFields.add(baseField1);
        
        BaseField baseField2 = new BaseField();
        baseField2.setId("base_002");
        baseField2.setName("合同描述");
        baseField2.setCode("contract_description");
        baseField2.setIsRichText(true);
        baseField2.setSampleValue("这是一个富文本的描述，包含换行\n与更多示例内容。");
        baseFields.add(baseField2);
        
        BaseField baseField3 = new BaseField();
        baseField3.setId("base_003");
        baseField3.setName("合同金额");
        baseField3.setCode("contract_amount");
        baseField3.setIsRichText(false);
        baseField3.setSampleValue("100000.00");
        baseFields.add(baseField3);
        
        response.setBaseFields(baseFields);
        
        // 相对方字段数据
        List<CounterpartyField> counterpartyFields = new ArrayList<>();
        CounterpartyField counterpartyField1 = new CounterpartyField();
        counterpartyField1.setId("cp_001");
        counterpartyField1.setName("甲方名称");
        counterpartyField1.setCode("party_a_name");
        counterpartyField1.setCounterpartyIndex(1);
        counterpartyField1.setSampleValue("上海甲方有限公司");
        counterpartyFields.add(counterpartyField1);
        
        CounterpartyField counterpartyField2 = new CounterpartyField();
        counterpartyField2.setId("cp_002");
        counterpartyField2.setName("乙方名称");
        counterpartyField2.setCode("party_b_name");
        counterpartyField2.setCounterpartyIndex(2);
        counterpartyField2.setSampleValue("深圳乙方科技有限公司");
        counterpartyFields.add(counterpartyField2);
        
        CounterpartyField counterpartyField3 = new CounterpartyField();
        counterpartyField3.setId("cp_003");
        counterpartyField3.setName("甲方地址");
        counterpartyField3.setCode("party_a_address");
        counterpartyField3.setCounterpartyIndex(1);
        counterpartyField3.setSampleValue("上海市浦东新区世纪大道1号");
        counterpartyFields.add(counterpartyField3);
        
        CounterpartyField counterpartyField4 = new CounterpartyField();
        counterpartyField4.setId("cp_004");
        counterpartyField4.setName("乙方地址");
        counterpartyField4.setCode("party_b_address");
        counterpartyField4.setCounterpartyIndex(2);
        counterpartyField4.setSampleValue("深圳市南山区科技园路88号");
        counterpartyFields.add(counterpartyField4);
        
        response.setCounterpartyFields(counterpartyFields);
        
        // 条款字段数据
        List<ClauseField> clauseFields = new ArrayList<>();
        ClauseField clauseField1 = new ClauseField();
        clauseField1.setId("clause_001");
        clauseField1.setName("第一条");
        clauseField1.setCode("clause_1");
        clauseField1.setContent("甲方：${party_a_name}，乙方：${party_b_name}，就${contract_name}达成如下协议：");
        clauseField1.setType("general");
        clauseField1.setTypeName("通用条款");
        clauseField1.setSampleValue("示例条款一（含变量展示）");
        clauseFields.add(clauseField1);
        
        ClauseField clauseField2 = new ClauseField();
        clauseField2.setId("clause_002");
        clauseField2.setName("第二条");
        clauseField2.setCode("clause_2");
        clauseField2.setContent("合同金额：${contract_amount}元");
        clauseField2.setType("payment");
        clauseField2.setTypeName("付款条款");
        clauseField2.setSampleValue("示例付款条款");
        clauseFields.add(clauseField2);
        
        ClauseField clauseField3 = new ClauseField();
        clauseField3.setId("clause_003");
        clauseField3.setName("第三条");
        clauseField3.setCode("clause_3");
        clauseField3.setContent("甲方地址：${party_a_address}，乙方地址：${party_b_address}");
        clauseField3.setType("address");
        clauseField3.setTypeName("地址条款");
        clauseField3.setSampleValue("示例地址条款");
        clauseFields.add(clauseField3);

        // 印章字段
        // 由于DTO新增SealField，这里演示返回两枚示例印章
        List<SealField> sealFields = new ArrayList<>();
        SealField companySeal = new SealField();
        companySeal.setId("seal_001");
        companySeal.setName("公司公章");
        companySeal.setCode("company_seal");
        companySeal.setType("company");
        companySeal.setOrderIndex(1);
        sealFields.add(companySeal);

        SealField financeSeal = new SealField();
        financeSeal.setId("seal_002");
        financeSeal.setName("财务专用章");
        financeSeal.setCode("finance_seal");
        financeSeal.setType("finance");
        financeSeal.setOrderIndex(2);
        sealFields.add(financeSeal);

        response.setSealFields(sealFields);
        
        response.setClauseFields(clauseFields);
        
        return response;
    }

    @Override
    public FieldResponse getFieldsByTemplateId(String templateId) {
        // 根据模板ID返回不同的字段配置
        FieldResponse response = getFields(); // 默认返回所有字段
        
        // 这里可以根据templateId进行不同的字段配置
        if ("template_simple".equals(templateId)) {
            // 简单模板，只返回基础字段
            response.setCounterpartyFields(new ArrayList<>());
            response.setClauseFields(new ArrayList<>());
        } else if ("template_complex".equals(templateId)) {
            // 复杂模板，返回所有字段
            // 保持默认配置
        }
        
        return response;
    }
} 