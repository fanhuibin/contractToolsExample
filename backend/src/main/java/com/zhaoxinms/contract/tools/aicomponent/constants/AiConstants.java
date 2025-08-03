package com.zhaoxinms.contract.tools.aicomponent.constants;

/**
 * AI 常量
 *
 * @author zhaoxinms
 */
public class AiConstants {

    public static final String OPENAI_HOST = "https://dashscope.aliyuncs.com/compatible-mode/";

    public static final String CONFIGURATION_PREFIX = "jnpf.ai";

    public static final String DEFAULT_HTTP_CLIENT_BEAN_NAME = "defaultOpenAiHttpClient";

    public static final String GEN_MODEL_COMPNENT = "- input - textarea - inputNumber - switch - radio - checkbox - select - datePicker - timePicker - uploadFile - uploadImg - colorPicker - rate - slider - editor - depSelect - posSelect - userSelect - roleSelect - areaSelect - signature - sign - location";

    public static final String GEN_MODEL_QUETION = "根据当前业务需求，设计相应的表单结构。请仅返回JSON数据，不包含其他任何形式的内容。预期结果是一个JSON数组，因涉及不同表单需求，故可能包含多个表单对象。请确保命名规避数据库与编程保留字。\n" +
            "所需表单应充分利用以下组件列表进行设计： " + GEN_MODEL_COMPNENT + "。\n" +
            "参考给定的JSON格式，属性包含：中文名（tableTitle)、英文名(tableName)、字段列表(fields)；字段列表是一个json数组，包含字段英文名(fieldName)、字段中文名(fieldTitle)等；" +
            "创建表单结构，示例如下： [ { \"tableTitle\": \"商城订单\", \"tableName\": \"online_order_form\", \"fields\": [ {\"fieldTitle\": \"订单编号\", \"fieldName\": \"order_id\", \"fieldDbType\": \"varchar\", \"fieldComponent\": \"input\"}, {\"fieldTitle\": \"订单状态\", \"fieldName\": \"order_status\", \"fieldDbType\": \"int\", \"fieldComponent\": \"radio\", \"fieldOptions\":[{\"id\":\"1\", \"fullName\":\"未付款\"},{\"id\":\"2\", \"fullName\":\"已付款\"}]}] }, { \"tableTitle\": \"订单商品明细\", \"tableName\": \"order_item_details\", \"fields\": [ {\"fieldTitle\": \"订单ID（外键）\", \"fieldName\": \"order_id_fk\", \"fieldDbType\": \"varchar\", \"fieldComponent\": \"input\"}, {\"fieldTitle\": \"商品名称\", \"fieldName\": \"product_name\", \"fieldDbType\": \"varchar\", \"fieldComponent\": \"input\"}, {\"fieldTitle\": \"商品数量\", \"fieldName\": \"quantity\", \"fieldDbType\": \"int\", \"fieldComponent\": \"inputNumber\"}] } ]\n" +
            "请依据实际业务逻辑，合理选择组件与字段类型，确保设计的表单既能满足数据收集需求，又便于用户操作。";

    public static final String CHAT_PRE_QUETION = "赵信合同工具集是一个集成了OnlyOffice、模板设计、文档合成、合同比对等功能的合同管理工具集。";

    /**
     * 模型名称
     * <a href="https://help.aliyun.com/zh/model-studio/getting-started/models">阿里官方稳定模型列表</a>
     */
    public static class Model {

        /**
         * 通义千问系列效果最好的模型，适合复杂、多步骤的任务。
         */
        public static final String QWEN_MAX = "qwen-max";
        /**
         * 通义千问系列速度最快、成本很低的模型，适合简单任务。
         */
        public static final String QWEN_TURBO = "qwen-turbo";
        /**
         * 通义千问开源版, 可部署参数最高的版本, 云版本收费
         */
        public static final String QWEN_25_72 = "qwen2.5-72b-instruct";
        /**
         * 通义千问开源版, 官方提供接口免费版本, 云版本限时免费
         */
        public static final String QWEN_25_3 = "qwen2.5-3b-instruct";

    }
}