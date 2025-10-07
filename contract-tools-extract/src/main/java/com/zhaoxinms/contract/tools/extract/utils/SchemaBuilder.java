package com.zhaoxinms.contract.tools.extract.utils;

import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema.FieldDefinition;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema.FieldType;

import java.util.Arrays;

/**
 * 提取模式构建工具类
 * 提供常用的预定义模式和便捷的构建方法
 */
public class SchemaBuilder {
    
    /**
     * 创建合同信息提取模式
     */
    public static ExtractionSchema createContractSchema() {
        ExtractionSchema schema = ExtractionSchema.builder()
            .name("合同信息提取")
            .description("从合同文本中提取关键信息")
            .version("1.0")
            .createdAt(System.currentTimeMillis())
            .build();
        
        schema.addField(FieldDefinition.builder()
            .name("contract_title")
            .description("合同标题")
            .type(FieldType.STRING)
            .required(true)
            .hint("通常在文档开头或标题位置")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("party_a")
            .description("甲方名称")
            .type(FieldType.STRING)
            .required(true)
            .hint("合同中的第一方当事人")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("party_b")
            .description("乙方名称")
            .type(FieldType.STRING)
            .required(true)
            .hint("合同中的第二方当事人")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("contract_amount")
            .description("合同金额")
            .type(FieldType.CURRENCY)
            .required(false)
            .hint("合同总价值，可能包含货币符号")
            .examples(Arrays.asList("100000", "50万元", "$10,000"))
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("signing_date")
            .description("签署日期")
            .type(FieldType.DATE)
            .required(false)
            .hint("合同签署的日期")
            .examples(Arrays.asList("2024-01-15", "2024年1月15日"))
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("contract_duration")
            .description("合同期限")
            .type(FieldType.STRING)
            .required(false)
            .hint("合同有效期或履行期限")
            .examples(Arrays.asList("一年", "6个月", "2024-01-01至2024-12-31"))
            .build());
        
        return schema;
    }
    
    /**
     * 创建发票信息提取模式
     */
    public static ExtractionSchema createInvoiceSchema() {
        ExtractionSchema schema = ExtractionSchema.builder()
            .name("发票信息提取")
            .description("从发票文本中提取关键财务信息")
            .version("1.0")
            .createdAt(System.currentTimeMillis())
            .build();
        
        schema.addField(FieldDefinition.builder()
            .name("invoice_number")
            .description("发票号码")
            .type(FieldType.STRING)
            .required(true)
            .hint("发票的唯一标识号码")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("invoice_date")
            .description("开票日期")
            .type(FieldType.DATE)
            .required(true)
            .hint("发票开具的日期")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("seller_name")
            .description("销售方名称")
            .type(FieldType.STRING)
            .required(true)
            .hint("开票方的公司名称")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("buyer_name")
            .description("购买方名称")
            .type(FieldType.STRING)
            .required(true)
            .hint("收票方的公司名称")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("total_amount")
            .description("发票总金额")
            .type(FieldType.CURRENCY)
            .required(true)
            .hint("价税合计金额")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("tax_amount")
            .description("税额")
            .type(FieldType.CURRENCY)
            .required(false)
            .hint("增值税金额")
            .build());
        
        return schema;
    }
    
    /**
     * 创建人员简历提取模式
     */
    public static ExtractionSchema createResumeSchema() {
        ExtractionSchema schema = ExtractionSchema.builder()
            .name("简历信息提取")
            .description("从简历文本中提取人员基本信息")
            .version("1.0")
            .createdAt(System.currentTimeMillis())
            .build();
        
        schema.addField(FieldDefinition.builder()
            .name("name")
            .description("姓名")
            .type(FieldType.STRING)
            .required(true)
            .hint("求职者的姓名")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("age")
            .description("年龄")
            .type(FieldType.INTEGER)
            .required(false)
            .hint("求职者的年龄")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("gender")
            .description("性别")
            .type(FieldType.STRING)
            .required(false)
            .hint("男或女")
            .examples(Arrays.asList("男", "女"))
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("email")
            .description("邮箱地址")
            .type(FieldType.EMAIL)
            .required(false)
            .hint("联系邮箱")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("phone")
            .description("联系电话")
            .type(FieldType.PHONE)
            .required(false)
            .hint("手机号码")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("education")
            .description("教育背景")
            .type(FieldType.STRING)
            .required(false)
            .hint("最高学历")
            .examples(Arrays.asList("本科", "硕士", "博士"))
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("work_experience")
            .description("工作经验年数")
            .type(FieldType.INTEGER)
            .required(false)
            .hint("从事相关工作的年数")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("skills")
            .description("专业技能")
            .type(FieldType.ARRAY)
            .required(false)
            .hint("掌握的技能列表")
            .examples(Arrays.asList(Arrays.asList("Java", "Python", "Spring")))
            .build());
        
        return schema;
    }
    
    /**
     * 创建新闻文章提取模式
     */
    public static ExtractionSchema createNewsSchema() {
        ExtractionSchema schema = ExtractionSchema.builder()
            .name("新闻文章提取")
            .description("从新闻文章中提取关键信息")
            .version("1.0")
            .createdAt(System.currentTimeMillis())
            .build();
        
        schema.addField(FieldDefinition.builder()
            .name("title")
            .description("新闻标题")
            .type(FieldType.STRING)
            .required(true)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("author")
            .description("作者")
            .type(FieldType.STRING)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("publish_date")
            .description("发布日期")
            .type(FieldType.DATE)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("category")
            .description("新闻分类")
            .type(FieldType.STRING)
            .required(false)
            .examples(Arrays.asList("科技", "财经", "体育", "娱乐"))
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("keywords")
            .description("关键词")
            .type(FieldType.ARRAY)
            .required(false)
            .hint("文章的主要关键词")
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("summary")
            .description("文章摘要")
            .type(FieldType.STRING)
            .required(false)
            .hint("文章的简要概述")
            .build());
        
        return schema;
    }
    
    /**
     * 创建公司信息提取模式
     */
    public static ExtractionSchema createCompanySchema() {
        ExtractionSchema schema = ExtractionSchema.builder()
            .name("公司信息提取")
            .description("从公司介绍文本中提取关键信息")
            .version("1.0")
            .createdAt(System.currentTimeMillis())
            .build();
        
        schema.addField(FieldDefinition.builder()
            .name("company_name")
            .description("公司名称")
            .type(FieldType.STRING)
            .required(true)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("established_date")
            .description("成立时间")
            .type(FieldType.DATE)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("industry")
            .description("所属行业")
            .type(FieldType.STRING)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("headquarters")
            .description("总部地址")
            .type(FieldType.STRING)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("employee_count")
            .description("员工数量")
            .type(FieldType.INTEGER)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("annual_revenue")
            .description("年营收")
            .type(FieldType.CURRENCY)
            .required(false)
            .build());
        
        schema.addField(FieldDefinition.builder()
            .name("business_scope")
            .description("经营范围")
            .type(FieldType.STRING)
            .required(false)
            .build());
        
        return schema;
    }
}
