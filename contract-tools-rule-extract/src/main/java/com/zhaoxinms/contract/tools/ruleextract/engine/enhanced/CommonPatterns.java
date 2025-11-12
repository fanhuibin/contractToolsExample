package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 常用的正则表达式Pattern
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
public class CommonPatterns {

    /**
     * 获取所有常用Pattern
     */
    public static Map<String, PatternTemplate> getAllPatterns() {
        Map<String, PatternTemplate> patterns = new LinkedHashMap<>();

        // 日期相关
        patterns.put("DATE_YYYY_MM_DD", new PatternTemplate(
                "日期(yyyy-MM-dd)",
                "\\d{4}-\\d{1,2}-\\d{1,2}",
                "2024-10-09",
                "date"
        ));

        patterns.put("DATE_CHINESE", new PatternTemplate(
                "日期(中文)",
                "\\d{4}年\\d{1,2}月\\d{1,2}日",
                "2024年10月9日",
                "date"
        ));

        patterns.put("DATE_SLASH", new PatternTemplate(
                "日期(斜线)",
                "\\d{4}/\\d{1,2}/\\d{1,2}",
                "2024/10/09",
                "date"
        ));

        // 金额相关
        patterns.put("AMOUNT_DECIMAL", new PatternTemplate(
                "金额(小数)",
                "\\d+(?:,\\d{3})*(?:\\.\\d{2})?",
                "100,000.00",
                "amount"
        ));

        patterns.put("AMOUNT_CHINESE", new PatternTemplate(
                "金额(中文)",
                "[壹贰叁肆伍陆柒捌玖拾佰仟万亿]+元",
                "壹佰万元",
                "amount"
        ));

        patterns.put("CURRENCY", new PatternTemplate(
                "币种",
                "(?:人民币|美元|欧元|日元|港币|CNY|USD|EUR|JPY|HKD)",
                "人民币",
                "string"
        ));

        // 编号相关
        patterns.put("CONTRACT_NUMBER", new PatternTemplate(
                "合同编号(通用)",
                "[A-Z]{2,4}\\d{6,12}",
                "HT20240001",
                "string"
        ));

        patterns.put("ID_NUMBER", new PatternTemplate(
                "编号(数字字母组合)",
                "[A-Z0-9-]{6,20}",
                "ABC-123-456",
                "string"
        ));

        patterns.put("COMPANY_NAME_GENERAL", new PatternTemplate(
                "公司名称(中英文通用)",
                "[\\u4e00-\\u9fa5A-Za-z0-9&（）()·\\s,.'-]{2,120}(?:公司|企业|集团|有限责任公司|股份有限公司|合作社|事务所|基金会|协会|联合会|委员会|管理局|局|机关|人民政府|政府|人民法院|法院|人民检察院|检察院|党委|党组|办事处|中心|研究院|学院|大学|银行|交易所|证券|保险|Co\\.?\\s*,?\\s*Ltd\\.?|Co\\.?\\s*,?\\s*Limited|Company|Corporation|Inc\\.?|LLC|Limited|Ltd\\.?|Agency|Association|Foundation|Institute|University|Bank|Committee|Government|Authority|Administration|Bureau|Council|Union|Society|Office)",
                "Ministry of Finance of the People's Republic of China",
                "string"
        ));

        // 人名
        patterns.put("CHINESE_NAME", new PatternTemplate(
                "中文姓名",
                "[\\u4e00-\\u9fa5]{2,4}",
                "张三",
                "string"
        ));

        // 电话号码
        patterns.put("MOBILE_PHONE", new PatternTemplate(
                "手机号",
                "\\d{7,12}",
                "13800138000",
                "string"
        ));

        patterns.put("PHONE_NUMBER", new PatternTemplate(
                "固定电话",
                "(?:0\\d{2,3}-)?\\d{7,8}",
                "010-12345678",
                "string"
        ));

        // 邮箱
        patterns.put("EMAIL", new PatternTemplate(
                "电子邮箱",
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                "example@company.com",
                "string"
        ));

        // 地址
        patterns.put("ADDRESS", new PatternTemplate(
                "地址",
                "[\\u4e00-\\u9fa5]{2,}[省市区县][\\u4e00-\\u9fa5\\d号室栋楼单元-]+",
                "北京市海淀区XX街道XX号",
                "string"
        ));

        // 百分比
        patterns.put("PERCENTAGE", new PatternTemplate(
                "百分比",
                "\\d+(?:\\.\\d+)?%",
                "13.5%",
                "number"
        ));

        // 数字
        patterns.put("INTEGER", new PatternTemplate(
                "整数",
                "\\d+",
                "12345",
                "number"
        ));

        patterns.put("DECIMAL", new PatternTemplate(
                "小数",
                "\\d+\\.\\d+",
                "123.45",
                "number"
        ));

        // 通用文本
        patterns.put("TEXT_UNTIL_PUNCT", new PatternTemplate(
                "文本(到标点)",
                "[^，。；！？\\n]+",
                "任意文本直到标点",
                "string"
        ));

        patterns.put("TEXT_UNTIL_NEWLINE", new PatternTemplate(
                "文本(到换行)",
                "[^\\n]+",
                "任意文本直到换行",
                "string"
        ));

        patterns.put("TEXT_CHINESE", new PatternTemplate(
                "中文文本",
                "[\\u4e00-\\u9fa5]+",
                "中文文本",
                "string"
        ));

        patterns.put("TEXT_WITH_SPACE", new PatternTemplate(
                "文本(含空格)",
                "[\\u4e00-\\u9fa5a-zA-Z0-9\\s]+",
                "中文 English 123",
                "string"
        ));

        // 时间段
        patterns.put("TIME_PERIOD", new PatternTemplate(
                "时间段",
                "\\d+(?:年|个月|月|天|日)",
                "3年",
                "string"
        ));

        return patterns;
    }

    /**
     * Pattern模板
     */
    public static class PatternTemplate {
        private String name;
        private String pattern;
        private String example;
        private String type;

        public PatternTemplate(String name, String pattern, String example, String type) {
            this.name = name;
            this.pattern = pattern;
            this.example = example;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getPattern() {
            return pattern;
        }

        public String getExample() {
            return example;
        }

        public String getType() {
            return type;
        }
    }
}

