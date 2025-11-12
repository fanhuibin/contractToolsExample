package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义字段配置控制器
 * 为模板设计器提供与标准字段接口一致的数据结构
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-fields")
public class CustomFieldsController {

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getCustomFieldsConfig(
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String configId) {

        log.info("获取自定义字段配置，模板类型: {}, configId: {}", templateType, configId);

        try {
            Map<String, Object> config = resolveConfig(templateType, configId);
            if (config == null) {
                return ApiResponse.error("未找到对应的字段配置");
            }
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取自定义字段配置失败", e);
            return ApiResponse.error("获取自定义字段配置失败: " + e.getMessage());
        }
    }

    private Map<String, Object> resolveConfig(String templateType, String configId) {
        Map<String, Map<String, Object>> presets = new LinkedHashMap<>();
        presets.put("default", defaultConfig());
        presets.put("purchase_contract", purchaseConfig());
        presets.put("service_agreement", serviceConfig());
        presets.put("insulation_material_purchase_contract", insulationPurchaseConfig());

        if (StringUtils.hasText(configId)) {
            return presets.get(configId.trim());
        }

        if ("purchase".equalsIgnoreCase(templateType)) {
            return presets.get("purchase_contract");
        }
        if ("service".equalsIgnoreCase(templateType)) {
            return presets.get("service_agreement");
        }
        return presets.get("default");
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configName", "通用合同字段");
        data.put("baseFields", defaultBaseFields());
        data.put("counterpartyFields", defaultPartyFields());
        data.put("clauseFields", defaultClauseFields());
        data.put("sealFields", defaultSealFields());
        return data;
    }

    private Map<String, Object> purchaseConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configName", "采购合同字段");
        data.put("baseFields", purchaseBaseFields());
        data.put("counterpartyFields", purchasePartyFields());
        data.put("clauseFields", purchaseClauseFields());
        data.put("sealFields", purchaseSealFields());
        return data;
    }

    private Map<String, Object> insulationPurchaseConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configName", "绝热材料采购合同字段");
        data.put("baseFields", insulationBaseFields());
        data.put("counterpartyFields", insulationPartyFields());
        data.put("clauseFields", insulationClauseFields());
        data.put("sealFields", insulationSealFields());
        return data;
    }

    private Map<String, Object> serviceConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configName", "服务协议字段");
        data.put("baseFields", serviceBaseFields());
        data.put("counterpartyFields", servicePartyFields());
        data.put("clauseFields", serviceClauseFields());
        data.put("sealFields", serviceSealFields());
        return data;
    }

    private List<Map<String, Object>> defaultBaseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(baseField("1", "合同名", "base_contractName", false, "示例-合同名"));
        list.add(baseField("2", "合同编号", "base_contractCode", false, "HT-2025-0001"));
        list.add(baseField("3", "收支类型", "base_inouttype", false, "支出"));
        list.add(baseField("4", "合同金额", "base_amount", false, "100000.00"));
        list.add(baseField("5", "合同金额大写", "base_amountB", false, "壹拾万元整"));
        list.add(baseField("6", "所属项目", "base_project", false, "上海新总部项目"));
        list.add(baseField("7", "生效时间", "base_effectiveTime", false, "2025-01-01"));
        list.add(baseField("8", "截止时间", "base_deadline", false, "2025-12-31"));
        list.add(baseField("9", "产品明细", "base_productDetail", true, "<p>产品A x 10，产品B x 5</p>"));
        return list;
    }

    private List<Map<String, Object>> defaultPartyFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(partyField("101", "主体名称", "subject_name", 1, "某某科技有限公司"));
        list.add(partyField("110", "法人姓名", "subject_contractLegalPerson", 1, "张三"));
        list.add(partyField("111", "法人身份证", "subject_contractLegalPersonId", 1, "110101199001010011"));
        list.add(partyField("102", "统一信用代码", "subject_uscc", 1, "91310101MA1KXXXXXX"));
        list.add(partyField("103", "主体地址", "subject_address", 1, "上海市浦东新区世纪大道1号"));
        list.add(partyField("104", "联系人", "subject_contactName", 1, "李四"));
        list.add(partyField("105", "联系电话", "subject_contactTel", 1, "13800138000"));
        list.add(partyField("108", "联系人邮箱", "subject_contractEmail", 1, "a@example.com"));
        list.add(partyField("109", "联系人地址", "subject_contractAddr", 1, "上海市浦东新区XX路88号"));
        list.add(partyField("106", "收款帐号", "subject_cardNo", 1, "622202*********1234"));
        list.add(partyField("107", "开户行", "subject_bankName", 1, "中国工商银行上海分行"));
        return list;
    }

    private List<Map<String, Object>> defaultClauseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(clauseField("clause_001", "第一条", "clause_1",
                "甲方：${party_a_name}，乙方：${party_b_name}，就${base_contractName}达成如下协议：",
                "general", "通用条款", "示例条款一（含变量展示）"));
        list.add(clauseField("clause_002", "第二条", "clause_2",
                "合同金额：${contract_amount}元",
                "payment", "付款条款", "示例付款条款"));
        list.add(clauseField("clause_003", "第三条", "clause_3",
                "甲方地址：${party_a_address}，乙方地址：${party_b_address}",
                "address", "地址条款", "示例地址条款"));
        return list;
    }

    private List<Map<String, Object>> defaultSealFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(sealField("seal_001", "公司公章", "company_seal", "company", 1, 100f, 100f));
        list.add(sealField("seal_002", "财务专用章", "finance_seal", "finance", 2, 80f, 80f));
        return list;
    }

    private List<Map<String, Object>> purchaseBaseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(baseField("base_1", "采购项目名称", "base_projectName", false, "绝热材料采购项目"));
        list.add(baseField("base_2", "招标编号", "base_bidNumber", false, "ZB-2025-001"));
        list.add(baseField("base_3", "合同标号", "base_contractCode", false, "HT-2025-001"));
        list.add(baseField("base_4", "签订地点", "base_signLocation", false, "北京市"));
        list.add(baseField("base_5", "签订时间", "base_signTime", false, "2025-01-15"));
        list.add(baseField("base_6", "合同总价", "base_totalAmount", false, "500000.00"));
        list.add(baseField("base_7", "预付款比例", "base_prepayRatio", false, "30%"));
        list.add(baseField("base_8", "预付款金额", "base_prepayAmount", false, "150000.00"));
        list.add(baseField("base_9", "验收付款比例", "base_acceptanceRatio", false, "60%"));
        list.add(baseField("base_10", "验收付款金额", "base_acceptanceAmount", false, "300000.00"));
        list.add(baseField("base_11", "安装调试付款比例", "base_installRatio", false, "7%"));
        list.add(baseField("base_12", "安装调试付款金额", "base_installAmount", false, "35000.00"));
        list.add(baseField("base_13", "质保金比例", "base_warrantyRatio", false, "3%"));
        list.add(baseField("base_14", "质保金金额", "base_warrantyAmount", false, "15000.00"));
        list.add(baseField("base_15", "质保期限", "base_warrantyPeriod", false, "1年"));
        list.add(baseField("base_16", "交货日期", "base_deliveryDate", false, "2025-03-01"));
        list.add(baseField("base_17", "交货地点", "base_deliveryLocation", false, "甲方指定仓库"));
        list.add(baseField("base_18", "产品明细表格", "base_productTable", true,
                "<table><tr><td>序号</td><td>货物名称</td><td>规格型号</td><td>产地</td><td>数量</td><td>单价</td><td>合计</td></tr><tr><td>1</td><td>岩棉板</td><td>100mm</td><td>河北</td><td>1000</td><td>50.00</td><td>50000.00</td></tr></table>"));
        return list;
    }

    private List<Map<String, Object>> purchasePartyFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(partyField("subject_1", "买方公司名称", "party_a_name", 1, "中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部"));
        list.add(partyField("subject_2", "买方地址", "party_a_address", 1, "沈阳市某某区某某路XX号"));
        list.add(partyField("subject_3", "买方联系人", "party_a_contact", 1, "张经理"));
        list.add(partyField("subject_4", "买方电话", "party_a_phone", 1, "13800138000"));
        list.add(partyField("subject_5", "卖方公司名称", "party_b_name", 2, "盘锦瑞普杜欣设备再制造技术开发合作联社"));
        list.add(partyField("subject_6", "卖方地址", "party_b_address", 2, "盘锦市某某区某某街XX号"));
        list.add(partyField("subject_7", "卖方银行账号", "party_b_bankAccount", 2, "5903 1201 0100 2714 35"));
        list.add(partyField("subject_8", "卖方联系人", "party_b_contact", 2, "李经理"));
        list.add(partyField("subject_9", "卖方电话", "party_b_phone", 2, "13900139000"));
        return list;
    }

    private List<Map<String, Object>> purchaseClauseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(clauseField("clause_techStandard", "技术标准条款", "clause_techStandard",
                "本合同下交付的货物应符合中华人民共和国现行国家标准GB/T 相关标准要求。",
                "technical", "技术标准条款", "技术标准条款"));
        list.add(clauseField("clause_acceptance", "验收条款", "clause_acceptance",
                "买方应在货到后15日内组织验收，卖方需在买方在场情况下拆封验货。",
                "acceptance", "验收条款", "验收条款"));
        list.add(clauseField("clause_installation", "安装调试条款", "clause_installation",
                "卖方应在验收后10日内完成设备安装调试，确保正常运行。",
                "installation", "安装调试条款", "安装调试条款"));
        list.add(clauseField("clause_warranty", "质保条款", "clause_warranty",
                "质保期内设备出现故障，卖方应在24小时内响应，72小时内解决。",
                "warranty", "质保条款", "质保条款"));
        list.add(clauseField("clause_penalty", "违约责任条款", "clause_penalty",
                "卖方延迟交货按每周合同总价0.5%支付违约金，最高不超过10%。",
                "penalty", "违约责任条款", "违约责任条款"));
        list.add(clauseField("clause_customVariables", "交付与罚则", "clause_customVariables",
                "双方确认货物应于${deliveryDate}前完成交付；如遇延迟，将按合同总价的${delayPenaltyRate}%支付违约金，且最迟不得晚于${base_acceptanceAmount}到账之日。",
                "delivery", "交付条款（含变量示例）", "演示条款（含变量示例）"));
        return list;
    }

    private List<Map<String, Object>> purchaseSealFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(sealField("seal_1", "买方公章", "seal_party_a", "party_a", 1, 120f, 120f));
        list.add(sealField("seal_2", "卖方公章", "seal_party_b", "party_b", 2, 120f, 120f));
        list.add(sealField("seal_3", "买方代表签字", "signature_party_a", "signature", 3, 150f, 60f));
        list.add(sealField("seal_4", "卖方代表签字", "signature_party_b", "signature", 4, 150f, 60f));
        return list;
    }

    private List<Map<String, Object>> insulationBaseFields() {
        return purchaseBaseFields();
    }

    private List<Map<String, Object>> insulationPartyFields() {
        return purchasePartyFields();
    }

    private List<Map<String, Object>> insulationClauseFields() {
        return purchaseClauseFields();
    }

    private List<Map<String, Object>> insulationSealFields() {
        return purchaseSealFields();
    }

    private List<Map<String, Object>> serviceBaseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(baseField("service_1", "协议编号", "agreement_no", false, "FW-2025-001"));
        list.add(baseField("service_2", "协议名称", "agreement_name", false, "信息系统维护协议"));
        list.add(baseField("service_3", "服务类型", "service_type", false, "系统维护"));
        list.add(baseField("service_4", "服务费用", "service_fee", false, "120000.00"));
        list.add(baseField("service_5", "服务期限", "service_period", false, "2025-01-01 至 2025-12-31"));
        list.add(baseField("service_6", "签订日期", "sign_date", false, "2025-01-05"));
        return list;
    }

    private List<Map<String, Object>> servicePartyFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(partyField("service_party_1", "委托方名称", "client_name", 1, "华夏地产股份有限公司"));
        list.add(partyField("service_party_2", "委托方联系人", "client_contact", 1, "周经理"));
        list.add(partyField("service_party_3", "委托方电话", "client_phone", 1, "010-66668888"));
        list.add(partyField("service_party_4", "服务方名称", "provider_name", 2, "肇新科技有限公司"));
        list.add(partyField("service_party_5", "服务方联系人", "provider_contact", 2, "刘工"));
        list.add(partyField("service_party_6", "服务方电话", "provider_phone", 2, "021-55556666"));
        return list;
    }

    private List<Map<String, Object>> serviceClauseFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(clauseField("service_clause_1", "服务内容", "service_content",
                "乙方提供7x24小时系统运维支持，并按月提交报告。",
                "service", "服务内容", "乙方提供7x24小时系统运维支持，并按月提交报告。"));
        list.add(clauseField("service_clause_2", "服务标准", "service_standard",
                "重要故障2小时响应，4小时内恢复；一般故障8小时内解决。",
                "service", "服务标准", "重要故障2小时响应，4小时内恢复；一般故障8小时内解决。"));
        list.add(clauseField("service_clause_3", "付费方式", "payment_schedule",
                "按季度结算，每季度末支付当季费用。",
                "payment", "付费方式", "按季度结算，每季度末支付当季费用。"));
        list.add(clauseField("service_clause_4", "双方义务", "obligations",
                "甲方应提供必要的系统账户；乙方应保障服务质量。",
                "obligations", "双方义务", "甲方应提供必要的系统账户；乙方应保障服务质量。"));
        list.add(clauseField("service_clause_5", "终止条款", "termination_clause",
                "任一方提前30日书面通知，可解除合同。",
                "termination", "终止条款", "任一方提前30日书面通知，可解除合同。"));
        list.add(clauseField("service_clause_6", "保密条款", "confidentiality",
                "双方对对方商业信息负有保密义务。",
                "confidentiality", "保密条款", "双方对对方商业信息负有保密义务。"));
        return list;
    }

    private List<Map<String, Object>> serviceSealFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(sealField("service_seal_1", "委托方盖章", "client_seal", "client", 1, 110f, 110f));
        list.add(sealField("service_seal_2", "服务方盖章", "provider_seal", "provider", 2, 110f, 110f));
        return list;
    }

    private Map<String, Object> baseField(String id, String name, String code, boolean isRichText, String sampleValue) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("name", name);
        field.put("code", code);
        field.put("tag", code);
        field.put("type", isRichText ? "richText" : "text");
        field.put("isRichText", isRichText);
        field.put("sampleValue", sampleValue);
        return field;
    }

    private Map<String, Object> partyField(String id, String name, String code, int counterpartyIndex, String sampleValue) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("name", name);
        field.put("code", code);
        field.put("tag", code);
        field.put("type", "text");
        field.put("counterpartyIndex", counterpartyIndex);
        field.put("sampleValue", sampleValue);
        return field;
    }

    private Map<String, Object> clauseField(String id, String name, String code, String content,
                                            String type, String typeName, String sampleValue) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("name", name);
        field.put("code", code);
        field.put("tag", code);
        field.put("content", content);
        field.put("type", type);
        field.put("typeName", typeName);
        field.put("sampleValue", sampleValue);
        return field;
    }

    private Map<String, Object> sealField(String id, String name, String code, String type, int orderIndex) {
        return sealField(id, name, code, type, orderIndex, null, null);
    }

    private Map<String, Object> sealField(String id, String name, String code, String type, int orderIndex, Float width, Float height) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("name", name);
        field.put("code", code);
        field.put("tag", code);
        field.put("type", type);
        field.put("orderIndex", orderIndex);
        if (width != null) {
            field.put("width", width);
        }
        if (height != null) {
            field.put("height", height);
        }
        return field;
    }

}

