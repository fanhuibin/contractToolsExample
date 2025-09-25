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
        
        // 基础字段数据（按用户要求补充并规范code前缀）
        List<BaseField> baseFields = new ArrayList<>();

        BaseField base1 = new BaseField();
        base1.setId("1");
        base1.setName("合同名");
        base1.setCode("base_contractName");
        base1.setIsRichText(false);
        base1.setSampleValue("示例-合同名");
        baseFields.add(base1);

        BaseField base2 = new BaseField();
        base2.setId("2");
        base2.setName("合同编号");
        base2.setCode("base_contractCode");
        base2.setIsRichText(false);
        base2.setSampleValue("HT-2025-0001");
        baseFields.add(base2);

        BaseField base3 = new BaseField();
        base3.setId("3");
        base3.setName("收支类型");
        base3.setCode("base_inouttype");
        base3.setIsRichText(false);
        base3.setSampleValue("支出");
        baseFields.add(base3);

        BaseField base4 = new BaseField();
        base4.setId("4");
        base4.setName("合同金额");
        base4.setCode("base_amount");
        base4.setIsRichText(false);
        base4.setSampleValue("100000.00");
        baseFields.add(base4);

        BaseField base5 = new BaseField();
        base5.setId("5");
        base5.setName("合同金额大写");
        base5.setCode("base_amountB");
        base5.setIsRichText(false);
        base5.setSampleValue("壹拾万元整");
        baseFields.add(base5);

        BaseField base6 = new BaseField();
        base6.setId("6");
        base6.setName("所属项目");
        base6.setCode("base_project");
        base6.setIsRichText(false);
        base6.setSampleValue("上海新总部项目");
        baseFields.add(base6);

        BaseField base7 = new BaseField();
        base7.setId("7");
        base7.setName("生效时间");
        base7.setCode("base_effectiveTime");
        base7.setIsRichText(false);
        base7.setSampleValue("2025-01-01");
        baseFields.add(base7);

        BaseField base8 = new BaseField();
        base8.setId("8");
        base8.setName("截止时间");
        base8.setCode("base_deadline");
        base8.setIsRichText(false);
        base8.setSampleValue("2025-12-31");
        baseFields.add(base8);

        // 富文本字段：产品明细（页面需标记）
        BaseField base9 = new BaseField();
        base9.setId("9");
        base9.setName("产品明细");
        base9.setCode("base_productDetail");
        base9.setIsRichText(true);
        base9.setSampleValue("<p>产品A x 10，产品B x 5</p>");
        baseFields.add(base9);
        
        response.setBaseFields(baseFields);
        
        // 相对方（签约方/主体）字段数据（按用户提供清单）
        List<CounterpartyField> counterpartyFields = new ArrayList<>();

        counterpartyFields.add(createCounterpartyField("101", "主体名称", "subject_name", 1, "某某科技有限公司"));
        counterpartyFields.add(createCounterpartyField("110", "法人姓名", "subject_contractLegalPerson", 1, "张三"));
        counterpartyFields.add(createCounterpartyField("111", "法人身份证", "subject_contractLegalPersonId", 1, "110101199001010011"));
        counterpartyFields.add(createCounterpartyField("102", "统一信用代码", "subject_uscc", 1, "91310101MA1KXXXXXX"));
        counterpartyFields.add(createCounterpartyField("103", "主体地址", "subject_address", 1, "上海市浦东新区世纪大道1号"));
        counterpartyFields.add(createCounterpartyField("104", "联系人", "subject_contactName", 1, "李四"));
        counterpartyFields.add(createCounterpartyField("105", "联系电话", "subject_contactTel", 1, "13800138000"));
        counterpartyFields.add(createCounterpartyField("108", "联系人邮箱", "subject_contractEmail", 1, "a@example.com"));
        counterpartyFields.add(createCounterpartyField("109", "联系人地址", "subject_contractAddr", 1, "上海市浦东新区XX路88号"));
        counterpartyFields.add(createCounterpartyField("106", "收款帐号", "subject_cardNo", 1, "622202*********1234"));
        counterpartyFields.add(createCounterpartyField("107", "开户行", "subject_bankName", 1, "中国工商银行上海分行"));
        
        response.setCounterpartyFields(counterpartyFields);
        
        // 条款字段数据
        List<ClauseField> clauseFields = new ArrayList<>();
        ClauseField clauseField1 = new ClauseField();
        clauseField1.setId("clause_001");
        clauseField1.setName("第一条");
        clauseField1.setCode("clause_1");
        clauseField1.setContent("甲方：${party_a_name}，乙方：${party_b_name}，就${base_contractName}达成如下协议：");
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

    private CounterpartyField createCounterpartyField(String id, String name, String code, int index, String sample) {
        CounterpartyField f = new CounterpartyField();
        f.setId(id);
        f.setName(name);
        f.setCode(code);
        f.setCounterpartyIndex(index);
        f.setSampleValue(sample);
        return f;
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