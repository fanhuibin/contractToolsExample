/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80040
 Source Host           : localhost:3306
 Source Schema         : contract_tools

 Target Server Type    : MySQL
 Target Server Version : 80040
 File Encoding         : 65001

 Date: 14/08/2025 14:25:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auto_fulfillment_history
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_history`;
CREATE TABLE `auto_fulfillment_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `extracted_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '识别的JSON结果',
  `extract_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '识别时间',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auto_ful_hist_user_time`(`user_id`, `extract_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '自动履约任务识别历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_history
-- ----------------------------
INSERT INTO `auto_fulfillment_history` VALUES (1, '1.0.肇新合同系统源码销售合同（二次）.docx', '[ \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"源码销售合同\\\",\\r\\n  \\\"甲方名称\\\" : \\\"\\\",\\r\\n  \\\"乙方名称\\\" : \\\"山西肇新科技有限公司\\\",\\r\\n  \\\"开票金额\\\" : 66000,\\r\\n  \\\"开票时间\\\" : \\\"\\\",\\r\\n  \\\"触发条件\\\" : [ \\\"合同生效后\\\" ],\\r\\n  \\\"基准日\\\" : \\\"合同签署日\\\",\\r\\n  \\\"偏移工作日\\\" : 7,\\r\\n  \\\"任务类型\\\" : \\\"支付\\\",\\r\\n  \\\"关联关键词\\\" : \\\"第一期款项\\\",\\r\\n  \\\"开票比例\\\" : 80\\r\\n}\", \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"源码销售合同\\\",\\r\\n  \\\"甲方名称\\\" : \\\"\\\",\\r\\n  \\\"乙方名称\\\" : \\\"山西肇新科技有限公司\\\",\\r\\n  \\\"开票金额\\\" : \\\"66000.00\\\",\\r\\n  \\\"开票时间\\\" : \\\"\\\",\\r\\n  \\\"基准日\\\" : \\\"\\\",\\r\\n  \\\"偏移天数\\\" : \\\"\\\",\\r\\n  \\\"任务类型\\\" : \\\"合同履约\\\",\\r\\n  \\\"关联关键词\\\" : [ \\\"肇新合同管理系统V1.4\\\", \\\"源码授权服务\\\", \\\"永久授权\\\", \\\"技术培训\\\", \\\"软件维护\\\" ]\\r\\n}\", \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"源码销售合同\\\",\\r\\n  \\\"甲方名称\\\" : \\\"甲方信息未完全提供\\\",\\r\\n  \\\"乙方名称\\\" : \\\"山西肇新科技有限公司\\\",\\r\\n  \\\"开票金额\\\" : \\\"66000.00元\\\",\\r\\n  \\\"验收时间\\\" : \\\"具体时间未明确提供，应在合同签订并交付后由甲方确认验收合格的时间\\\",\\r\\n  \\\"任务类型\\\" : \\\"软件源码授权及服务\\\",\\r\\n  \\\"关联关键词\\\" : [ \\\"肇新合同管理系统\\\", \\\"源码授权\\\", \\\"永久授权\\\", \\\"知识产权\\\", \\\"技术培训\\\", \\\"维护支持\\\" ]\\r\\n}\" ]', '2025-08-11 11:23:07', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (2, '1.0.肇新合同系统源码销售合同（二次）.docx', '[ \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"源码销售合同\\\",\\r\\n  \\\"甲方名称\\\" : \\\"\\\",\\r\\n  \\\"乙方名称\\\" : \\\"山西肇新科技有限公司\\\",\\r\\n  \\\"开票金额\\\" : \\\"66000.00\\\",\\r\\n  \\\"开票时间\\\" : \\\"\\\",\\r\\n  \\\"基准日\\\" : \\\"\\\",\\r\\n  \\\"偏移天数\\\" : \\\"\\\",\\r\\n  \\\"任务类型\\\" : \\\"付款\\\",\\r\\n  \\\"关联关键词\\\" : \\\"肇新合同管理系统V1.4, 源码授权服务\\\"\\r\\n}\" ]', '2025-08-11 11:24:08', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (3, '1.0.肇新合同系统源码销售合同（二次）.docx', '[ \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"肇新合同管理系统源码销售合同\\\",\\r\\n  \\\"付款比例\\\" : {\\r\\n    \\\"第一期款项\\\" : \\\"80%\\\",\\r\\n    \\\"第二期款项\\\" : \\\"20%\\\"\\r\\n  },\\r\\n  \\\"付款节点\\\" : {\\r\\n    \\\"第一期款项\\\" : \\\"甲乙双方签订合同后，合同生效日起7个工作日内\\\",\\r\\n    \\\"第二期款项\\\" : \\\"甲方确认验收合格起7个工作日内\\\"\\r\\n  },\\r\\n  \\\"预计付款时间\\\" : {\\r\\n    \\\"第一期款项\\\" : \\\"合同生效日起第7个工作日\\\",\\r\\n    \\\"第二期款项\\\" : \\\"确认验收合格起第7个工作日\\\"\\r\\n  },\\r\\n  \\\"基准日\\\" : {\\r\\n    \\\"第一期款项\\\" : \\\"合同生效日\\\",\\r\\n    \\\"第二期款项\\\" : \\\"确认验收合格日\\\"\\r\\n  },\\r\\n  \\\"到期日\\\" : {\\r\\n    \\\"第一期款项\\\" : \\\"合同生效日起第10个工作日\\\",\\r\\n    \\\"第二期款项\\\" : \\\"确认验收合格起第10个工作日\\\"\\r\\n  },\\r\\n  \\\"任务类型\\\" : \\\"软件源码销售及授权服务\\\",\\r\\n  \\\"关联关键词\\\" : [ \\\"肇新合同管理系统V1.4\\\", \\\"源码授权\\\", \\\"永久授权\\\", \\\"软件交付和验收标准\\\", \\\"技术培训\\\", \\\"软件维护与支持\\\" ]\\r\\n}\" ]', '2025-08-11 11:24:38', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (4, '1.0.肇新合同系统源码销售合同（二次）.docx', '[ \"{\\r\\n  \\\"合同编号\\\" : \\\"\\\",\\r\\n  \\\"合同名称\\\" : \\\"肇新合同管理系统源码销售合同\\\",\\r\\n  \\\"应收金额\\\" : \\\"¥66000元\\\",\\r\\n  \\\"收款条件\\\" : [ \\\"甲乙双方签订合同后，甲方需于合同生效日起7个工作日内支付第一期款项：金额为【52800】元。\\\", \\\"甲方验收后，甲方需于确认验收合格起7个工作日内支付第二期款项：金额为【13200】元。\\\" ],\\r\\n  \\\"预计收款时间\\\" : [ \\\"合同生效日起第7个工作日\\\", \\\"确认验收合格起第7个工作日\\\" ],\\r\\n  \\\"应收提醒日\\\" : [ \\\"合同生效日起第8个工作日\\\", \\\"确认验收合格起第8个工作日\\\" ],\\r\\n  \\\"任务类型\\\" : \\\"合同履约\\\",\\r\\n  \\\"关联关键词\\\" : [ \\\"肇新合同管理系统\\\", \\\"源码授权服务\\\", \\\"软件产品\\\", \\\"山西肇新科技有限公司\\\", \\\"付款\\\", \\\"验收\\\" ]\\r\\n}\" ]', '2025-08-11 11:25:08', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (5, '1.0.肇新合同系统源码销售合同（二次）.docx', '{\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"应收金额\" : \"66000元\",\r\n  \"收款条件\" : \"分两期支付：\\n（1）合同生效日起7个工作日内支付第一期款项：金额为52800元；\\n（2）确认验收合格起7个工作日内支付第二期款项：金额为13200元。\",\r\n  \"预计收款时间\" : {\r\n    \"第一期\" : \"合同生效日起第7个工作日\",\r\n    \"第二期\" : \"确认验收合格起第7个工作日\"\r\n  },\r\n  \"应收提醒日\" : {\r\n    \"第一期\" : \"合同生效日起第8个工作日\",\r\n    \"第二期\" : \"确认验收合格起第8个工作日\"\r\n  },\r\n  \"任务类型\" : \"收款\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统V1.4\", \"源码授权服务\", \"山西肇新科技有限公司\", \"永久授权\", \"分期付款\", \"逾期违约金\" ]\r\n}', '2025-08-11 11:25:45', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (6, '1.0.肇新合同系统源码销售合同（二次）.docx', '[ \"```json\\n[\\n    {\\n        \\\"合同编号\\\": \\\"\\\",\\n        \\\"合同名称\\\": \\\"源码销售合同\\\",\\n        \\\"甲方名称\\\": \\\"甲方\\\",\\n        \\\"乙方名称\\\": \\\"山西肇新科技有限公司\\\",\\n        \\\"开票金额\\\": 66000,\\n        \\\"开票时间\\\": \\\"\\\",\\n        \\\"触发条件\\\": [\\n            \\\"合同生效后\\\"\\n        ],\\n        \\\"基准日\\\": \\\"合同签署日\\\",\\n        \\\"偏移工作日\\\": 7,\\n        \\\"任务类型\\\": \\\"支付\\\",\\n        \\\"关联关键词\\\": \\\"第一期款项\\\",\\n        \\\"开票比例\\\": \\\"80%\\\"\\n    },\\n    {\\n        \\\"合同编号\\\": \\\"\\\",\\n        \\\"合同名称\\\": \\\"源码销售合同\\\",\\n        \\\"甲方名称\\\": \\\"甲方\\\",\\n        \\\"乙方名称\\\": \\\"山西肇新科技有限公司\\\",\\n        \\\"开票金额\\\": 66000,\\n        \\\"开票时间\\\": \\\"\\\",\\n        \\\"触发条件\\\": [\\n            \\\"验收合格后\\\"\\n        ],\\n        \\\"基准日\\\": \\\"验收合格日\\\",\\n        \\\"偏移工作日\\\": 7,\\n        \\\"任务类型\\\": \\\"支付\\\",\\n        \\\"关联关键词\\\": \\\"第二期款项\\\",\\n        \\\"开票比例\\\": \\\"20%\\\"\\n    }\\n]\\n``` \\n\\n请注意，由于文档中未提供具体的合同编号和开票时间，这两项留空。如果需要进一步完善信息，请提供更多细节。\", {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"甲方名称\" : \"\",\r\n  \"乙方名称\" : \"山西肇新科技有限公司\",\r\n  \"开票金额\" : \"66000.00\",\r\n  \"开票时间\" : \"\",\r\n  \"基准日\" : \"\",\r\n  \"偏移天数\" : \"\",\r\n  \"任务类型\" : \"付款\",\r\n  \"关联关键词\" : \"逾期付款\"\r\n}, {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"甲方名称\" : \"\",\r\n  \"乙方名称\" : \"山西肇新科技有限公司\",\r\n  \"开票金额\" : \"65000.00\",\r\n  \"验收时间\" : \"\",\r\n  \"任务类型\" : \"软件源码授权及服务\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统\", \"源码授权\", \"永久授权\", \"技术服务\", \"培训\", \"维护\" ]\r\n}, {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"付款金额\" : [ {\r\n    \"阶段\" : \"第一期款项\",\r\n    \"金额\" : 52800,\r\n    \"备注\" : \"人民币大写【伍万贰仟捌佰圆整】\"\r\n  }, {\r\n    \"阶段\" : \"第二期款项\",\r\n    \"金额\" : 13200,\r\n    \"备注\" : \"人民币大写【壹万叁仟贰佰圆整】\"\r\n  } ],\r\n  \"付款条件\" : [ \"甲乙双方签订合同后，甲方需于合同生效日起7个工作日内支付第一期款项。\", \"甲方验收后，甲方需于确认验收合格起7个工作日内支付第二期款项。\" ],\r\n  \"预计付款时间\" : [ {\r\n    \"阶段\" : \"第一期款项\",\r\n    \"时间\" : \"合同生效日起7个工作日内\"\r\n  }, {\r\n    \"阶段\" : \"第二期款项\",\r\n    \"时间\" : \"确认验收合格起7个工作日内\"\r\n  } ],\r\n  \"基准日\" : \"签约日期：       年    月    日\",\r\n  \"到期日\" : \"签约日期后的7个工作日（第一期款项到期日）\",\r\n  \"任务类型\" : \"付款\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统V1.4\", \"源码授权服务\", \"付款金额\", \"付款条件\", \"付款时间\", \"合同生效日\", \"验收合格\" ]\r\n}, {\r\n  \"templateId\" : 6,\r\n  \"error\" : \"自动履约任务识别失败: 429: OpenAIError{additionalProperties={error={message=Too many requests in route. Please try again later., type=invalid_request_error, param=null, code=rate_limit_error}}}\"\r\n}, {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"尾款金额\" : \"13200元（人民币大写【】圆整）\",\r\n  \"结算条件\" : \"甲方验收后，甲方需于确认验收合格起7个工作日内支付第二期款项\",\r\n  \"预计付款时间\" : \"验收合格起7个工作日内\",\r\n  \"基准日\" : \"验收日期\",\r\n  \"到期日\" : \"验收合格起7个工作日内\",\r\n  \"任务类型\" : \"合同履约\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统V1.4\", \"源码授权服务\", \"软件交付和验收标准\", \"技术培训\", \"软件维护与支持\" ]\r\n}, {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"肇新合同管理系统源码销售合同\",\r\n  \"应收金额\" : \"¥66000元\",\r\n  \"收款条件\" : \"（1）甲乙双方签订合同后，甲方需于合同生效日起7个工作日内支付第一期款项：金额为【52800】元；（2）甲方验收后，甲方需于确认验收合格起7个工作日内支付第二期款项：金额为【13200】元。\",\r\n  \"预计收款时间\" : \"第一期：合同生效日起第7个工作日；第二期：验收合格起第7个工作日\",\r\n  \"应收提醒日\" : \"第一期：合同生效日起第8个工作日；第二期：验收合格起第8个工作日\",\r\n  \"任务类型\" : \"合同履约\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统\", \"源码授权\", \"软件产品\", \"永久授权\", \"技术服务\", \"培训\", \"维护\" ]\r\n}, {\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"源码销售合同\",\r\n  \"应收尾款\" : 13200,\r\n  \"结算条件\" : \"（2）甲方验收后，甲方需于确认验收合格起7个工作日内支付第二期款项：金额为【13200】元（人民币大写【】圆整）\",\r\n  \"预计收款时间\" : \"确认验收合格起7个工作日内\",\r\n  \"应收提醒日\" : \"确认验收合格起第8个工作日\",\r\n  \"任务类型\" : \"收款提醒\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统\", \"源码授权服务\", \"验收合格\", \"付款\", \"逾期\" ]\r\n} ]', '2025-08-11 11:31:59', 'default-user');
INSERT INTO `auto_fulfillment_history` VALUES (7, '1.0.肇新合同系统源码销售合同（二次）.docx', '{\r\n  \"合同编号\" : \"\",\r\n  \"合同名称\" : \"肇新合同管理系统源码销售合同\",\r\n  \"甲方名称\" : \"\",\r\n  \"乙方名称\" : \"山西肇新科技有限公司\",\r\n  \"开票金额\" : [ 65000 ],\r\n  \"开票时间\" : [ \"yyyy-MM-dd\" ],\r\n  \"触发条件\" : [ \"合同生效后\", \"验收合格后\" ],\r\n  \"基准日\" : [ \"签署日期\", \"验收日期\" ],\r\n  \"偏移工作日\" : [ 7, 7 ],\r\n  \"偏移天数\" : [ ],\r\n  \"验收时间\" : \"yyyy-MM-dd\",\r\n  \"付款金额\" : [ 52800, 13200 ],\r\n  \"付款条件\" : [ \"合同生效后7个工作日内支付第一期款项\", \"验收合格后7个工作日内支付第二期款项\" ],\r\n  \"预计付款时间\" : [ \"yyyy-MM-dd\", \"yyyy-MM-dd\" ],\r\n  \"到期日\" : [ \"yyyy-MM-dd\", \"yyyy-MM-dd\" ],\r\n  \"付款比例\" : [ 80, 20 ],\r\n  \"付款节点\" : [ \"第一期款项\", \"第二期款项\" ],\r\n  \"尾款金额\" : 13200,\r\n  \"结算条件\" : [ \"验收合格\" ],\r\n  \"应收金额\" : 66000,\r\n  \"收款条件\" : [ \"合同生效后7个工作日内支付第一期款项\", \"验收合格后7个工作日内支付第二期款项\" ],\r\n  \"预计收款时间\" : [ \"yyyy-MM-dd\", \"yyyy-MM-dd\" ],\r\n  \"应收提醒日\" : [ \"yyyy-MM-dd\", \"yyyy-MM-dd\" ],\r\n  \"应收尾款\" : 13200,\r\n  \"任务类型\" : \"付款\",\r\n  \"关联关键词\" : [ \"肇新合同管理系统\", \"源码授权服务\", \"软件产品\", \"付款\", \"验收\" ],\r\n  \"_meta\" : {\r\n    \"selectedTemplateIds\" : [ 2, 3, 4, 5, 6, 7, 8, 9 ],\r\n    \"merge\" : true,\r\n    \"time\" : \"2025-08-11T11:57:11.305+08:00\"\r\n  }\r\n}', '2025-08-11 11:57:11', 'default-user');

-- ----------------------------
-- Table structure for auto_fulfillment_keyword
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_keyword`;
CREATE TABLE `auto_fulfillment_keyword`  (
  `id` bigint NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_keyword
-- ----------------------------
INSERT INTO `auto_fulfillment_keyword` VALUES (1001, '首笔款');
INSERT INTO `auto_fulfillment_keyword` VALUES (1002, '合同生效');
INSERT INTO `auto_fulfillment_keyword` VALUES (1003, '预付款');
INSERT INTO `auto_fulfillment_keyword` VALUES (1004, '订金');
INSERT INTO `auto_fulfillment_keyword` VALUES (1101, '终验');
INSERT INTO `auto_fulfillment_keyword` VALUES (1102, '验收报告');
INSERT INTO `auto_fulfillment_keyword` VALUES (1103, '交付完成');
INSERT INTO `auto_fulfillment_keyword` VALUES (1104, '质检合格');
INSERT INTO `auto_fulfillment_keyword` VALUES (1201, '支付');
INSERT INTO `auto_fulfillment_keyword` VALUES (1202, '付款');
INSERT INTO `auto_fulfillment_keyword` VALUES (1203, '转账');
INSERT INTO `auto_fulfillment_keyword` VALUES (1204, '汇出');
INSERT INTO `auto_fulfillment_keyword` VALUES (1301, '期限届满');
INSERT INTO `auto_fulfillment_keyword` VALUES (1302, '到期日');
INSERT INTO `auto_fulfillment_keyword` VALUES (1303, '终止日');
INSERT INTO `auto_fulfillment_keyword` VALUES (1304, '续约');
INSERT INTO `auto_fulfillment_keyword` VALUES (1401, '当...时');
INSERT INTO `auto_fulfillment_keyword` VALUES (1402, '经确认后');
INSERT INTO `auto_fulfillment_keyword` VALUES (1403, '达到...条件');
INSERT INTO `auto_fulfillment_keyword` VALUES (90001, '首笔款');
INSERT INTO `auto_fulfillment_keyword` VALUES (90002, '合同生效');
INSERT INTO `auto_fulfillment_keyword` VALUES (90003, '预付款');
INSERT INTO `auto_fulfillment_keyword` VALUES (90004, '订金');
INSERT INTO `auto_fulfillment_keyword` VALUES (90011, '终验');
INSERT INTO `auto_fulfillment_keyword` VALUES (90012, '验收报告');
INSERT INTO `auto_fulfillment_keyword` VALUES (90013, '交付完成');
INSERT INTO `auto_fulfillment_keyword` VALUES (90014, '质检合格');
INSERT INTO `auto_fulfillment_keyword` VALUES (90021, '支付');
INSERT INTO `auto_fulfillment_keyword` VALUES (90022, '付款');
INSERT INTO `auto_fulfillment_keyword` VALUES (90023, '转账');
INSERT INTO `auto_fulfillment_keyword` VALUES (90024, '汇出');
INSERT INTO `auto_fulfillment_keyword` VALUES (90031, '期限届满');
INSERT INTO `auto_fulfillment_keyword` VALUES (90032, '到期日');
INSERT INTO `auto_fulfillment_keyword` VALUES (90033, '终止日');
INSERT INTO `auto_fulfillment_keyword` VALUES (90034, '续约');
INSERT INTO `auto_fulfillment_keyword` VALUES (90041, '当...时');
INSERT INTO `auto_fulfillment_keyword` VALUES (90042, '经确认后');
INSERT INTO `auto_fulfillment_keyword` VALUES (90043, '达到...条件');

-- ----------------------------
-- Table structure for auto_fulfillment_task_keyword
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_task_keyword`;
CREATE TABLE `auto_fulfillment_task_keyword`  (
  `task_type_id` bigint NOT NULL,
  `keyword_id` bigint NOT NULL,
  PRIMARY KEY (`task_type_id`, `keyword_id`) USING BTREE,
  INDEX `idx_kw`(`keyword_id`) USING BTREE,
  CONSTRAINT `fk_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `auto_fulfillment_keyword` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_task_type` FOREIGN KEY (`task_type_id`) REFERENCES `auto_fulfillment_task_type` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_task_keyword
-- ----------------------------
INSERT INTO `auto_fulfillment_task_keyword` VALUES (101, 1001);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (101, 1002);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (101, 1003);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (101, 1004);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (103, 1101);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (103, 1102);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (103, 1103);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (103, 1104);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (201, 1201);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (202, 1201);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (203, 1201);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (201, 1202);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (202, 1202);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (203, 1202);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (201, 1203);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (202, 1203);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (203, 1203);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (201, 1204);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (202, 1204);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (203, 1204);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (401, 1301);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (401, 1302);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (401, 1303);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (401, 1304);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (501, 1401);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (501, 1402);
INSERT INTO `auto_fulfillment_task_keyword` VALUES (501, 1403);

-- ----------------------------
-- Table structure for auto_fulfillment_task_type
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_task_type`;
CREATE TABLE `auto_fulfillment_task_type`  (
  `id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_type_code`(`code`) USING BTREE,
  INDEX `idx_parent`(`parent_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_task_type
-- ----------------------------
INSERT INTO `auto_fulfillment_task_type` VALUES (1, NULL, '开票履约', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (2, NULL, '付款履约', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (3, NULL, '收款履约', 3, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (4, NULL, '到期提醒', 4, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (5, NULL, '事件触发', 5, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (101, 1, '预付款开票', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (102, 1, '进度款开票', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (103, 1, '验收款开票', 3, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (201, 2, '应付任务-预付款', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (202, 2, '应付任务-进度款', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (203, 2, '应付任务-尾款', 3, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (301, 3, '应收任务-预收款', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (302, 3, '应收任务-尾款', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (401, 4, '合同到期提醒', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (402, 4, '服务到期提醒', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (501, 5, '验收合格提醒', 1, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (502, 5, '货物送达提醒', 2, NULL);
INSERT INTO `auto_fulfillment_task_type` VALUES (1000, NULL, '开票履约', 1, 'invoice_fulfillment');
INSERT INTO `auto_fulfillment_task_type` VALUES (1101, 1000, '预付款开票', 1, 'prepayment_invoice');
INSERT INTO `auto_fulfillment_task_type` VALUES (1102, 1000, '进度款开票', 2, 'progress_invoice');
INSERT INTO `auto_fulfillment_task_type` VALUES (1103, 1000, '验收款开票', 3, 'acceptance_invoice');
INSERT INTO `auto_fulfillment_task_type` VALUES (2000, NULL, '付款履约', 2, 'payment_fulfillment');
INSERT INTO `auto_fulfillment_task_type` VALUES (2101, 2000, '应付任务-预付款', 1, 'payable_prepayment');
INSERT INTO `auto_fulfillment_task_type` VALUES (2102, 2000, '应付任务-进度款', 2, 'payable_progress_payment');
INSERT INTO `auto_fulfillment_task_type` VALUES (2103, 2000, '应付任务-尾款', 3, 'payable_final_payment');
INSERT INTO `auto_fulfillment_task_type` VALUES (3000, NULL, '收款履约', 3, 'collection_fulfillment');
INSERT INTO `auto_fulfillment_task_type` VALUES (3101, 3000, '应收任务-预收款', 1, 'receivable_prepayment');
INSERT INTO `auto_fulfillment_task_type` VALUES (3102, 3000, '应收任务-尾款', 2, 'receivable_final_payment');
INSERT INTO `auto_fulfillment_task_type` VALUES (4000, NULL, '到期提醒', 4, 'expiry_reminder');
INSERT INTO `auto_fulfillment_task_type` VALUES (4101, 4000, '合同到期提醒', 1, 'contract_expiry_reminder');
INSERT INTO `auto_fulfillment_task_type` VALUES (4102, 4000, '服务到期提醒', 2, 'service_expiry_reminder');
INSERT INTO `auto_fulfillment_task_type` VALUES (5000, NULL, '事件触发', 5, 'event_trigger');
INSERT INTO `auto_fulfillment_task_type` VALUES (5101, 5000, '验收合格提醒', 1, 'acceptance_passed_reminder');
INSERT INTO `auto_fulfillment_task_type` VALUES (5102, 5000, '货物送达提醒', 2, 'goods_delivered_reminder');

-- ----------------------------
-- Table structure for auto_fulfillment_task_type_keyword
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_task_type_keyword`;
CREATE TABLE `auto_fulfillment_task_type_keyword`  (
  `task_type_id` bigint NOT NULL,
  `keyword_id` bigint NOT NULL,
  PRIMARY KEY (`task_type_id`, `keyword_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_task_type_keyword
-- ----------------------------
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1101, 90001);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1101, 90002);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1101, 90003);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1101, 90004);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1103, 90011);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1103, 90012);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1103, 90013);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (1103, 90014);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2101, 90021);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2101, 90022);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2101, 90023);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2101, 90024);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2102, 90021);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2102, 90022);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2102, 90023);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2102, 90024);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2103, 90021);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2103, 90022);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2103, 90023);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (2103, 90024);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (4101, 90031);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (4101, 90032);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (4101, 90033);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (4101, 90034);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5101, 90041);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5101, 90042);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5101, 90043);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5102, 90041);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5102, 90042);
INSERT INTO `auto_fulfillment_task_type_keyword` VALUES (5102, 90043);

-- ----------------------------
-- Table structure for auto_fulfillment_template
-- ----------------------------
DROP TABLE IF EXISTS `auto_fulfillment_template`;
CREATE TABLE `auto_fulfillment_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板类型：system-系统模板，user-用户模板',
  `contract_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同类型',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提取字段列表，JSON格式',
  `creator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL COMMENT '更新时间',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述',
  `category_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `task_type_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auto_ful_template_type`(`type`) USING BTREE,
  INDEX `idx_auto_ful_template_contract_type`(`contract_type`) USING BTREE,
  INDEX `idx_auto_ful_template_creator`(`creator_id`) USING BTREE,
  INDEX `idx_auto_ful_template_default`(`contract_type`, `is_default`) USING BTREE,
  INDEX `idx_template_category_code`(`category_code`) USING BTREE,
  INDEX `idx_template_task_type_id`(`task_type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '自动履约任务模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_fulfillment_template
-- ----------------------------
INSERT INTO `auto_fulfillment_template` VALUES (1, '自动履约-通用模板', 'system', 'operation', '[\"合同编号\",\"合同名称\",\"甲方名称\",\"乙方名称\",\"合同签署日期\",\"合同有效期\",\"合同起始时间\",\"合同终止时间\"]', 'system', '2025-08-10 16:34:33', '2025-08-10 16:34:33', 1, '自动履约通用字段', 'operation', NULL);
INSERT INTO `auto_fulfillment_template` VALUES (2, '预付款开票-系统模板', 'system', 'invoice_fulfillment', '[\"合同编号\",\"合同名称\",\"甲方名称\",\"乙方名称\",\"开票金额\",\"开票时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'invoice_fulfillment', 1101);
INSERT INTO `auto_fulfillment_template` VALUES (3, '进度款开票-系统模板', 'system', 'invoice_fulfillment', '[\"合同编号\",\"合同名称\",\"甲方名称\",\"乙方名称\",\"开票金额\",\"开票时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'invoice_fulfillment', 1102);
INSERT INTO `auto_fulfillment_template` VALUES (4, '验收款开票-系统模板', 'system', 'invoice_fulfillment', '[\"合同编号\",\"合同名称\",\"甲方名称\",\"乙方名称\",\"开票金额\",\"验收时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'invoice_fulfillment', 1103);
INSERT INTO `auto_fulfillment_template` VALUES (5, '应付-预付款-系统模板', 'system', 'payment_fulfillment', '[\"合同编号\",\"合同名称\",\"付款金额\",\"付款条件\",\"预计付款时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'payment_fulfillment', 2101);
INSERT INTO `auto_fulfillment_template` VALUES (6, '应付-进度款-系统模板', 'system', 'payment_fulfillment', '[\"合同编号\",\"合同名称\",\"付款比例\",\"付款节点\",\"预计付款时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'payment_fulfillment', 2102);
INSERT INTO `auto_fulfillment_template` VALUES (7, '应付-尾款-系统模板', 'system', 'payment_fulfillment', '[\"合同编号\",\"合同名称\",\"尾款金额\",\"结算条件\",\"预计付款时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'payment_fulfillment', 2103);
INSERT INTO `auto_fulfillment_template` VALUES (8, '应收-预收款-系统模板', 'system', 'collection_fulfillment', '[\"合同编号\",\"合同名称\",\"应收金额\",\"收款条件\",\"预计收款时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'collection_fulfillment', 3101);
INSERT INTO `auto_fulfillment_template` VALUES (9, '应收-尾款-系统模板', 'system', 'collection_fulfillment', '[\"合同编号\",\"合同名称\",\"应收尾款\",\"结算条件\",\"预计收款时间\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'collection_fulfillment', 3102);
INSERT INTO `auto_fulfillment_template` VALUES (10, '合同到期提醒-系统模板', 'system', 'expiry_reminder', '[\"合同编号\",\"合同名称\",\"到期日\",\"提醒提前天数\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'expiry_reminder', 4101);
INSERT INTO `auto_fulfillment_template` VALUES (11, '服务到期提醒-系统模板', 'system', 'expiry_reminder', '[\"服务名称\",\"到期日\",\"提醒提前天数\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'expiry_reminder', 4102);
INSERT INTO `auto_fulfillment_template` VALUES (12, '验收合格提醒-系统模板', 'system', 'event_trigger', '[\"合同编号\",\"合同名称\",\"事件\",\"提醒对象\",\"提醒方式\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'event_trigger', 5101);
INSERT INTO `auto_fulfillment_template` VALUES (13, '货物送达提醒-系统模板', 'system', 'event_trigger', '[\"合同编号\",\"合同名称\",\"事件\",\"提醒对象\",\"提醒方式\"]', 'system', '2025-08-10 17:58:43', '2025-08-10 17:58:43', 0, '系统初始化模板', 'event_trigger', 5102);

-- ----------------------------
-- Table structure for contract_extract_history
-- ----------------------------
DROP TABLE IF EXISTS `contract_extract_history`;
CREATE TABLE `contract_extract_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提取的文件名',
  `extracted_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提取的JSON内容',
  `extract_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '提取时间',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id_extract_time`(`user_id`, `extract_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '合同信息提取历史记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of contract_extract_history
-- ----------------------------
INSERT INTO `contract_extract_history` VALUES (1, '四川莞蓉科技有限公司.pdf', '```json\n{\n    \"甲方公司名称\": \"四川莞蓉科技有限公司\",\n    \"乙方公司名称\": \"山西肇新科技有限公司\",\n    \"甲方通信地址\": \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\n    \"乙方通信地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"合同名称\": \"肇新合同管理系统源码销售合同\",\n    \"合同编号\": null,\n    \"合同总金额小写\": 58000,\n    \"币种\": \"人民币\",\n    \"合同总金额大写\": \"伍万捌仟元圆整\",\n    \"付款节点\": \"合同生效日起7个工作日内\",\n    \"付款比例\": \"100%\",\n    \"付款金额（小写）\": 58000,\n    \"付款金额（大写）\": \"伍万捌仟元圆整\",\n    \"合同生效日期\": null,\n    \"合同终止日期\": null,\n    \"合同有效期\": \"永久授权，自合同签订之日起生效\",\n    \"交货单位\": \"乙方（山西肇新科技有限公司）\",\n    \"交货地点\": null,\n    \"交货时间\": null,\n    \"验收时间\": null,\n    \"运输方式\": null,\n    \"运输费用\": null,\n    \"增值率税率\": \"1%\",\n    \"税额\": 574.26,\n    \"发票类型\": null,\n    \"服务期限\": \"自源码正式交付之日起一个月\",\n    \"保证金金额（小写）\": null,\n    \"保证金金额（大写）\": null,\n    \"保证金比例\": null,\n    \"保证金归还节点\": null,\n    \"争议仲裁地\": \"甲方或乙方所在地的法院\",\n    \"甲方纳税人识别号\": \"91510100MA61WHB260\",\n    \"乙方纳税人识别号\": \"91140105MAOLLW8Q1D\",\n    \"甲方注册地址\": \"成都高新区天府大道中段1号1栋209单元1层8号\",\n    \"乙方注册地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"甲方登记电话\": \"18980746257\",\n    \"乙方登记电话\": \"13671354640\",\n    \"甲方开户行\": \"中国民生银行股份有限公司成都锦江支行\",\n    \"乙方开户行\": \"兴业银行股份有限公司太原大营盘支行\",\n    \"甲方银行账号名称\": \"四川莞蓉科技有限公司\",\n    \"乙方银行账号名称\": \"山西肇新科技有限公司\",\n    \"甲方银行账号\": \"150645726\",\n    \"乙方银行账号\": \"485060100100305592\",\n    \"违约内容\": \"逾期支付费用超过30天的；逾期交付超过10日的；提供的软件存在知识产权争议的；其他违约行为。\",\n    \"甲方邮政编码\": null,\n    \"乙方邮政编码\": null,\n    \"甲方法人\": \"刘晓兰\",\n    \"乙方法人\": \"范忠\",\n    \"甲方业务联系人\": \"吴世杰\",\n    \"乙方业务联系人\": \"范慧斌\",\n    \"甲方业务联系人电话\": \"18980746257\",\n    \"甲方业务联系人邮箱\": \"wushijie@scwanrong.com\",\n    \"乙方业务联系人电话\": \"13671354640\",\n    \"乙方业务联系人邮箱\": \"fanhuibin@zhaoxinms.com\",\n    \"甲方签署日期\": null,\n    \"乙方签署日期\": null,\n    \"甲方法人或代表人是否签字\": true,\n    \"乙方法人或代表人是否签字\": true\n}\n```', '2025-08-07 12:02:58', 'default-user');
INSERT INTO `contract_extract_history` VALUES (2, '四川莞蓉科技有限公司.pdf', '```json\n{\n    \"甲方公司名称\": \"四川莞蓉科技有限公司\",\n    \"乙方公司名称\": \"山西肇新科技有限公司\",\n    \"甲方通信地址\": \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\n    \"乙方通信地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"合同名称\": \"肇新合同管理系统源码销售合同\",\n    \"合同编号\": null,\n    \"合同总金额小写\": 58000.00,\n    \"币种\": \"人民币\",\n    \"合同总金额大写\": \"伍万捌仟元圆整\",\n    \"付款节点\": \"合同生效日起7个工作日内\",\n    \"付款比例\": 100,\n    \"付款金额（小写）\": 58000,\n    \"付款金额（大写）\": \"伍万捌仟元圆整\",\n    \"合同生效日期\": null,\n    \"合同终止日期\": null,\n    \"合同有效期\": \"永久授权，自合同签订之日起生效\",\n    \"交货单位\": \"乙方（山西肇新科技有限公司）\",\n    \"交货地点\": \"甲方指定的电子邮件地址\",\n    \"交货时间\": \"合同生效日起7个工作日内\",\n    \"验收时间\": null,\n    \"运输方式\": \"电子邮件\",\n    \"运输费用\": null,\n    \"增值率税率\": 1,\n    \"税额\": 574.26,\n    \"发票类型\": null,\n    \"服务期限\": \"一个月的系统部署专项技术支撑服务\",\n    \"保证金金额（小写）\": null,\n    \"保证金金额（大写）\": null,\n    \"保证金比例\": null,\n    \"保证金归还节点\": null,\n    \"争议仲裁地\": \"甲方或乙方所在地的法院\",\n    \"甲方纳税人识别号\": \"91510100MA61WHB260\",\n    \"乙方纳税人识别号\": \"91140105MAOLLW8Q1D\",\n    \"甲方注册地址\": \"成都高新区天府大道中段1号1栋209单元1层8号\",\n    \"乙方注册地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"甲方登记电话\": \"18980746257\",\n    \"乙方登记电话\": \"13671354640\",\n    \"甲方开户行\": \"中国民生银行股份有限公司成都锦江支行\",\n    \"乙方开户行\": \"兴业银行股份有限公司太原大营盘支行\",\n    \"甲方银行账号名称\": \"四川莞蓉科技有限公司\",\n    \"乙方银行账号名称\": \"山西肇新科技有限公司\",\n    \"甲方银行账号\": \"150645726\",\n    \"乙方银行账号\": \"485060100100305592\",\n    \"违约内容\": [\n        \"逾期支付费用超过30天的\",\n        \"逾期交付超过10日的\",\n        \"乙方提供的软件存在知识产权争议的\",\n        \"其他违约行为\"\n    ],\n    \"甲方邮政编码\": null,\n    \"乙方邮政编码\": null,\n    \"甲方法人\": \"刘晓兰\",\n    \"乙方法人\": \"范忠\",\n    \"甲方业务联系人\": \"吴世杰\",\n    \"乙方业务联系人\": \"范慧斌\",\n    \"甲方业务联系人电话\": \"18980746257\",\n    \"甲方业务联系人邮箱\": \"wushijie@scwanrong.com\",\n    \"乙方业务联系人电话\": \"13671354640\",\n    \"乙方业务联系人邮箱\": \"fanhuibin@zhaoxinms.com\",\n    \"甲方签署日期\": null,\n    \"乙方签署日期\": null,\n    \"甲方法人或代表人是否签字\": null,\n    \"乙方法人或代表人是否签字\": null\n}\n```', '2025-08-08 11:14:49', 'default-user');
INSERT INTO `contract_extract_history` VALUES (3, '四川莞蓉科技有限公司.pdf', '```json\n{\n    \"甲方公司名称\": \"四川莞蓉科技有限公司\",\n    \"乙方公司名称\": \"山西肇新科技有限公司\",\n    \"甲方通信地址\": \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\n    \"乙方通信地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"合同名称\": \"肇新合同管理系统源码销售合同\",\n    \"合同编号\": null,\n    \"合同总金额小写\": 58000.00,\n    \"币种\": \"人民币\",\n    \"合同总金额大写\": \"伍万捌仟元圆整\",\n    \"付款节点\": [\"合同生效日起7个工作日内\"],\n    \"付款比例\": [100],\n    \"付款金额（小写）\": 58000.00,\n    \"付款金额（大写）\": \"伍万捌仟元圆整\",\n    \"合同生效日期\": null,\n    \"合同终止日期\": null,\n    \"合同有效期\": \"永久授权，自合同签订之日起生效\",\n    \"交货单位\": \"乙方\",\n    \"交货地点\": \"电子邮件方式传递\",\n    \"交货时间\": null,\n    \"验收时间\": null,\n    \"运输方式\": null,\n    \"运输费用\": null,\n    \"增值率税率\": 1,\n    \"税额\": 574.26,\n    \"发票类型\": null,\n    \"服务期限\": \"3个月内提供技术培训，1个月的系统部署专项技术支撑服务\",\n    \"保证金金额（小写）\": null,\n    \"保证金金额（大写）\": null,\n    \"保证金比例\": null,\n    \"保证金归还节点\": null,\n    \"争议仲裁地\": \"甲方或乙方所在地的法院\",\n    \"甲方纳税人识别号\": \"91510100MA61WHB260\",\n    \"乙方纳税人识别号\": \"91140105MAOLLW8Q1D\",\n    \"甲方注册地址\": \"成都高新区天府大道中段1号1栋209单元1层8号\",\n    \"乙方注册地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"甲方登记电话\": \"18980746257\",\n    \"乙方登记电话\": \"13671354640\",\n    \"甲方开户行\": \"中国民生银行股份有限公司成都锦江支行\",\n    \"乙方开户行\": \"兴业银行股份有限公司太原大营盘支行\",\n    \"甲方银行账号名称\": \"四川莞蓉科技有限公司\",\n    \"乙方银行账号名称\": \"山西肇新科技有限公司\",\n    \"甲方银行账号\": \"150645726\",\n    \"乙方银行账号\": \"485060100100305592\",\n    \"违约内容\": [\n        \"甲方逾期支付费用超过30天，乙方有权解除合同。\",\n        \"乙方逾期交付超过10日或软件存在知识产权争议，甲方有权解除合同。\",\n        \"任何一方违约需赔偿守约方全部损失，包括直接和间接经济损失及实现权益的合理支出。\",\n        \"乙方每逾期一日按照合同总金额的0.5%向甲方支付逾期违约金。\",\n        \"甲方每逾期一日按照合同总金额的0.5%向乙方支付逾期违约金。\"\n    ],\n    \"甲方邮政编码\": null,\n    \"乙方邮政编码\": null,\n    \"甲方法人\": \"刘晓兰\",\n    \"乙方法人\": \"范忠\",\n    \"甲方业务联系人\": \"吴世杰\",\n    \"乙方业务联系人\": \"范慧斌\",\n    \"甲方业务联系人电话\": \"18980746257\",\n    \"甲方业务联系人邮箱\": \"wushijie@scwanrong.com\",\n    \"乙方业务联系人电话\": \"13671354640\",\n    \"乙方业务联系人邮箱\": \"fanhuibin@zhaoxinms.com\",\n    \"甲方签署日期\": null,\n    \"乙方签署日期\": null,\n    \"甲方法人或代表人是否签字\": true,\n    \"乙方法人或代表人是否签字\": true\n}\n```', '2025-08-08 12:00:58', 'default-user');
INSERT INTO `contract_extract_history` VALUES (4, '四川莞蓉科技有限公司.pdf', '```json\n{\n    \"甲方公司名称\": \"四川莞蓉科技有限公司\",\n    \"乙方公司名称\": \"山西肇新科技有限公司\",\n    \"甲方通信地址\": \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\n    \"乙方通信地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\n    \"合同名称\": \"肇新合同管理系统源码销售合同\",\n    \"合同编号\": null,\n    \"合同总金额小写\": 58000,\n    \"币种\": \"人民币\",\n    \"合同总金额大写\": \"伍万捌仟元\",\n    \"付款节点\": \"合同生效日起7个工作日内\",\n    \"付款比例\": 100,\n    \"付款金额（小写）\": 58000,\n    \"付款金额（大写）\": \"伍万捌仟元\",\n    \"合同生效日期\": null,\n    \"合同终止日期\": null,\n    \"合同有效期\": \"永久授权\",\n    \"交货单位\": \"乙方（山西肇新科技有限公司）\",\n    \"交货地点\": null,\n    \"交货时间\": null,\n    \"验收时间\": null,\n    \"运输方式\": null,\n    \"运输费用\": null,\n    \"增值率税率\": 1,\n    \"税额\": 574.26,\n    \"发票类型\": null,\n    \"服务期限\": \"自源码正式交付之日起为期一个月\",\n    \"保证金金额（小写）\": null,\n    \"保证金金额（大写）\": null,\n    \"保证金比例\": null,\n    \"保证金归还节点\": null,\n    \"争议仲裁地\": \"甲方或乙方所在地的法院\",\n    \"甲方纳税人识别号\": \"91510100MA61WHB260\",\n    \"乙方纳税人识别号\": \"91140105MAOLLW8Q1D\",\n    \"甲方注册地址\": \"成都高新区天府大道中段1号1栋209单元1层8号\",\n    \"乙方注册地址\": \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区(入驻山西亿企邦孵化园有限公司E区-087)\",\n    \"甲方登记电话\": \"18980746257\",\n    \"乙方登记电话\": \"13671354640\",\n    \"甲方开户行\": \"中国民生银行股份有限公司成都锦江支行\",\n    \"乙方开户行\": \"兴业银行股份有限公司太原大营盘支行\",\n    \"甲方银行账号名称\": \"四川莞蓉科技有限公司\",\n    \"乙方银行账号名称\": \"山西肇新科技有限公司\",\n    \"甲方银行账号\": \"150645726\",\n    \"乙方银行账号\": \"485060100100305592\",\n    \"违约内容\": \"详见合同第八条违约责任\",\n    \"甲方邮政编码\": null,\n    \"乙方邮政编码\": null,\n    \"甲方法人\": \"刘晓兰\",\n    \"乙方法人\": \"范忠\",\n    \"甲方业务联系人\": \"吴世杰\",\n    \"乙方业务联系人\": \"范慧斌\",\n    \"甲方业务联系人电话\": \"18980746257\",\n    \"甲方业务联系人邮箱\": \"wushijie@scwanrong.com\",\n    \"乙方业务联系人电话\": \"13671354640\",\n    \"乙方业务联系人邮箱\": \"fanhuibin@zhaoxinms.com\",\n    \"甲方签署日期\": null,\n    \"乙方签署日期\": null,\n    \"甲方法人或代表人是否签字\": true,\n    \"乙方法人或代表人是否签字\": true\n}\n```', '2025-08-08 15:21:20', 'default-user');
INSERT INTO `contract_extract_history` VALUES (5, '四川莞蓉科技有限公司.pdf', '{\r\n  \"甲方公司名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方公司名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方通信地址\" : \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\r\n  \"乙方通信地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"合同名称\" : \"肇新合同管理系统源码销售\",\r\n  \"合同编号\" : null,\r\n  \"合同总金额小写\" : \"58000\",\r\n  \"币种\" : \"人民币\",\r\n  \"合同总金额大写\" : \"伍万捌仟元圆整\",\r\n  \"付款节点\" : \"合同生效日起7个工作日内\",\r\n  \"付款比例\" : \"100%\",\r\n  \"付款金额（小写）\" : \"58000\",\r\n  \"付款金额（大写）\" : \"伍万捌仟元圆整\",\r\n  \"合同生效日期\" : null,\r\n  \"合同终止日期\" : null,\r\n  \"合同有效期\" : \"永久授权，自合同签订之日起生效\",\r\n  \"交货单位\" : \"乙方\",\r\n  \"交货地点\" : null,\r\n  \"交货时间\" : null,\r\n  \"验收时间\" : null,\r\n  \"运输方式\" : null,\r\n  \"运输费用\" : null,\r\n  \"增值率税率\" : \"1%\",\r\n  \"税额\" : \"574.26\",\r\n  \"发票类型\" : null,\r\n  \"服务期限\" : \"3个月的技术培训期 + 1个月的系统部署专项技术支撑服务\",\r\n  \"保证金金额（小写）\" : null,\r\n  \"保证金金额（大写）\" : null,\r\n  \"保证金比例\" : null,\r\n  \"保证金归还节点\" : null,\r\n  \"争议仲裁地\" : \"甲方或乙方所在地的法院\",\r\n  \"甲方纳税人识别号\" : \"91510100MA61WHB260\",\r\n  \"乙方纳税人识别号\" : \"91140105MAOLLW8Q1D\",\r\n  \"甲方注册地址\" : \"成都高新区天府大道中段1号1栋209单元1层8号\",\r\n  \"乙方注册地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"甲方登记电话\" : \"18980746257\",\r\n  \"乙方登记电话\" : \"13671354640\",\r\n  \"甲方开户行\" : \"中国民生银行股份有限公司成都锦江支行\",\r\n  \"乙方开户行\" : \"兴业银行股份有限公司太原大营盘支行\",\r\n  \"甲方银行账号名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方银行账号名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方银行账号\" : \"150645726\",\r\n  \"乙方银行账号\" : \"485060100100305592\",\r\n  \"违约内容\" : \"逾期支付费用超过30天；逾期交付超过10日；知识产权争议等\",\r\n  \"甲方邮政编码\" : null,\r\n  \"乙方邮政编码\" : null,\r\n  \"甲方法人\" : \"刘晓兰\",\r\n  \"乙方法人\" : \"范忠\",\r\n  \"甲方业务联系人\" : \"吴世杰\",\r\n  \"乙方业务联系人\" : \"范慧斌\",\r\n  \"甲方业务联系人电话\" : \"18980746257\",\r\n  \"甲方业务联系人邮箱\" : \"wushijie@scwanrong.com\",\r\n  \"乙方业务联系人电话\" : \"13671354640\",\r\n  \"乙方业务联系人邮箱\" : \"fanhuibin@zhaoxinms.com\",\r\n  \"甲方签署日期\" : null,\r\n  \"乙方签署日期\" : null,\r\n  \"甲方法人或代表人是否签字\" : null,\r\n  \"乙方法人或代表人是否签字\" : null\r\n}', '2025-08-08 15:48:33', 'default-user');
INSERT INTO `contract_extract_history` VALUES (6, '四川莞蓉科技有限公司.pdf', '{\r\n  \"甲方公司名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方公司名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方通信地址\" : \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\r\n  \"乙方通信地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"合同名称\" : \"肇新合同管理系统源码销售合同\",\r\n  \"合同编号\" : null,\r\n  \"合同总金额小写\" : \"58000\",\r\n  \"币种\" : \"人民币\",\r\n  \"合同总金额大写\" : \"伍万捌仟元圆整\",\r\n  \"付款节点\" : \"合同生效日起7个工作日内\",\r\n  \"付款比例\" : \"100%\",\r\n  \"付款金额（小写）\" : \"58000\",\r\n  \"付款金额（大写）\" : \"伍万捌仟元圆整\",\r\n  \"合同生效日期\" : null,\r\n  \"合同终止日期\" : null,\r\n  \"合同有效期\" : \"永久授权，自合同签订之日起生效\",\r\n  \"交货单位\" : \"乙方\",\r\n  \"交货地点\" : null,\r\n  \"交货时间\" : null,\r\n  \"验收时间\" : null,\r\n  \"运输方式\" : null,\r\n  \"运输费用\" : null,\r\n  \"增值率税率\" : \"1%\",\r\n  \"税额\" : \"574.26\",\r\n  \"发票类型\" : null,\r\n  \"服务期限\" : \"自源码正式交付之日起一个月\",\r\n  \"保证金金额（小写）\" : null,\r\n  \"保证金金额（大写）\" : null,\r\n  \"保证金比例\" : null,\r\n  \"保证金归还节点\" : null,\r\n  \"争议仲裁地\" : \"甲方或乙方所在地的法院\",\r\n  \"甲方纳税人识别号\" : \"91510100MA61WHB260\",\r\n  \"乙方纳税人识别号\" : \"91140105MAOLLW8Q1D\",\r\n  \"甲方注册地址\" : \"成都高新区天府大道中段1号1栋209单元1层8号\",\r\n  \"乙方注册地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"甲方登记电话\" : \"18980746257\",\r\n  \"乙方登记电话\" : \"13671354640\",\r\n  \"甲方开户行\" : \"中国民生银行股份有限公司成都锦江支行\",\r\n  \"乙方开户行\" : \"兴业银行股份有限公司太原大营盘支行\",\r\n  \"甲方银行账号名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方银行账号名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方银行账号\" : \"150645726\",\r\n  \"乙方银行账号\" : \"485060100100305592\",\r\n  \"违约内容\" : \"逾期支付费用超过30天；逾期交付超过10日；知识产权争议等\",\r\n  \"甲方邮政编码\" : null,\r\n  \"乙方邮政编码\" : null,\r\n  \"甲方法人\" : \"刘晓兰\",\r\n  \"乙方法人\" : \"范忠\",\r\n  \"甲方业务联系人\" : \"吴世杰\",\r\n  \"乙方业务联系人\" : \"范慧斌\",\r\n  \"甲方业务联系人电话\" : \"18980746257\",\r\n  \"甲方业务联系人邮箱\" : \"wushijie@scwanrong.com\",\r\n  \"乙方业务联系人电话\" : \"13671354640\",\r\n  \"乙方业务联系人邮箱\" : \"fanhuibin@zhaoxinms.com\",\r\n  \"甲方签署日期\" : null,\r\n  \"乙方签署日期\" : null,\r\n  \"甲方法人或代表人是否签字\" : null,\r\n  \"乙方法人或代表人是否签字\" : null\r\n}', '2025-08-08 16:30:54', 'default-user');
INSERT INTO `contract_extract_history` VALUES (7, '四川莞蓉科技有限公司.pdf', '{\r\n  \"甲方公司名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方公司名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方通信地址\" : \"成都市武侯区佳灵路7号红牌楼广场3号写字楼1315\",\r\n  \"乙方通信地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"合同名称\" : \"肇新合同管理系统源码销售\",\r\n  \"合同编号\" : null,\r\n  \"合同总金额小写\" : 58000.0,\r\n  \"币种\" : \"人民币\",\r\n  \"合同总金额大写\" : \"伍万捌仟元圆整\",\r\n  \"付款节点\" : \"合同生效日起7个工作日内\",\r\n  \"付款比例\" : null,\r\n  \"付款金额（小写）\" : 58000.0,\r\n  \"付款金额（大写）\" : \"伍万捌仟元圆整\",\r\n  \"合同生效日期\" : null,\r\n  \"合同终止日期\" : null,\r\n  \"合同有效期\" : null,\r\n  \"交货单位\" : null,\r\n  \"交货地点\" : null,\r\n  \"交货时间\" : null,\r\n  \"验收时间\" : null,\r\n  \"运输方式\" : null,\r\n  \"运输费用\" : null,\r\n  \"增值率税率\" : \"1%\",\r\n  \"税额\" : 574.26,\r\n  \"发票类型\" : null,\r\n  \"服务期限\" : \"自源码正式交付之日起，为期一个月\",\r\n  \"保证金金额（小写）\" : null,\r\n  \"保证金金额（大写）\" : null,\r\n  \"保证金比例\" : null,\r\n  \"保证金归还节点\" : null,\r\n  \"争议仲裁地\" : \"甲方或乙方所在地的法院\",\r\n  \"甲方纳税人识别号\" : \"91510100MA61WHB260\",\r\n  \"乙方纳税人识别号\" : \"91140105MAOLLW8Q1D\",\r\n  \"甲方注册地址\" : \"成都高新区天府大道中段1号1栋209单元1层8号\",\r\n  \"乙方注册地址\" : \"太原市小店区平阳路1号金茂大厦B座26层A2-B-C-D-E-F区\",\r\n  \"甲方登记电话\" : \"18980746257\",\r\n  \"乙方登记电话\" : \"13671354640\",\r\n  \"甲方开户行\" : \"中国民生银行股份有限公司成都锦江支行\",\r\n  \"乙方开户行\" : \"兴业银行股份有限公司太原大营盘支行\",\r\n  \"甲方银行账号名称\" : \"四川莞蓉科技有限公司\",\r\n  \"乙方银行账号名称\" : \"山西肇新科技有限公司\",\r\n  \"甲方银行账号\" : \"150645726\",\r\n  \"乙方银行账号\" : \"485060100100305592\",\r\n  \"违约内容\" : \"逾期支付费用超过30天；逾期交付超过10日；知识产权争议；其他违约行为\",\r\n  \"甲方邮政编码\" : null,\r\n  \"乙方邮政编码\" : null,\r\n  \"甲方法人\" : \"刘晓兰\",\r\n  \"乙方法人\" : \"范忠\",\r\n  \"甲方业务联系人\" : \"吴世杰\",\r\n  \"乙方业务联系人\" : \"范慧斌\",\r\n  \"甲方业务联系人电话\" : \"18980746257\",\r\n  \"甲方业务联系人邮箱\" : \"wushijie@scwanrong.com\",\r\n  \"乙方业务联系人电话\" : \"13671354640\",\r\n  \"乙方业务联系人邮箱\" : \"fanhuibin@zhaoxinms.com\",\r\n  \"甲方签署日期\" : null,\r\n  \"乙方签署日期\" : null,\r\n  \"甲方法人或代表人是否签字\" : null,\r\n  \"乙方法人或代表人是否签字\" : null,\r\n  \"合同期限(天)\" : 0\r\n}', '2025-08-08 16:40:50', 'default-user');

-- ----------------------------
-- Table structure for contract_extract_template
-- ----------------------------
DROP TABLE IF EXISTS `contract_extract_template`;
CREATE TABLE `contract_extract_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板类型：system-系统模板，user-用户模板',
  `contract_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同类型',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提取字段列表，JSON格式',
  `creator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL COMMENT '更新时间',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_contract_extract_template_type`(`type`) USING BTREE,
  INDEX `idx_contract_extract_template_contract_type`(`contract_type`) USING BTREE,
  INDEX `idx_contract_extract_template_creator`(`creator_id`) USING BTREE,
  INDEX `idx_contract_extract_template_default`(`contract_type`, `is_default`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '合同提取信息模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of contract_extract_template
-- ----------------------------
INSERT INTO `contract_extract_template` VALUES (1, '通用合同模板', 'system', 'common', '[\"合同编号\",\"合同名称\",\"甲方名称\",\"乙方名称\",\"甲方联系地址\",\"乙方联系地址\",\"甲方联系人及电话\",\"乙方联系人及电话\",\"合同签署日期\",\"合同有效期\",\"合同起始时间\",\"合同终止时间\",\"签署人信息\",\"违约责任条款\",\"争议解决方式\",\"适用法律\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于大多数合同类型的通用字段');
INSERT INTO `contract_extract_template` VALUES (2, '租赁合同模板', 'system', 'lease', '[\"承租方\",\"出租方\",\"甲方联系地址\",\"乙方联系人地址\",\"甲方联系人电话\",\"乙方联系人电话\",\"租赁地址\",\"租赁面积\",\"合同编号\",\"甲方签署日期\",\"乙方签署日期\",\"合同有效期\",\"合同起始时间\",\"租赁期限\",\"租赁起始时间\",\"租赁终止时间\",\"租赁费用\",\"租赁费用大写\",\"每期金额\",\"每期金额（大写)\",\"履约保证金\",\"付款方式\",\"支付周期\",\"支付节点\",\"甲方违约金\",\"乙方违约金\",\"甲方违约金（大写)\",\"乙方违约金（大写)\",\"甲方签署人\",\"乙方签署人\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于各类租赁合同');
INSERT INTO `contract_extract_template` VALUES (3, '购销合同模板', 'system', 'purchase', '[\"甲方公司名称\",\"乙方公司名称\",\"甲方通信地址\",\"乙方通信地址\",\"合同名称\",\"合同编号\",\"合同总金额小写\",\"币种\",\"合同总金额大写\",\"付款节点\",\"付款比例\",\"付款金额（小写）\",\"付款金额（大写）\",\"合同生效日期\",\"合同终止日期\",\"合同有效期\",\"交货单位\",\"交货地点\",\"交货时间\",\"验收时间\",\"运输方式\",\"运输费用\",\"增值率税率\",\"税额\",\"发票类型\",\"服务期限\",\"保证金金额（小写）\",\"保证金金额（大写）\",\"保证金比例\",\"保证金归还节点\",\"争议仲裁地\",\"甲方纳税人识别号\",\"乙方纳税人识别号\",\"甲方注册地址\",\"乙方注册地址\",\"甲方登记电话\",\"乙方登记电话\",\"甲方开户行\",\"乙方开户行\",\"甲方银行账号名称\",\"乙方银行账号名称\",\"甲方银行账号\",\"乙方银行账号\",\"违约内容\",\"甲方邮政编码\",\"乙方邮政编码\",\"甲方法人\",\"乙方法人\",\"甲方业务联系人\",\"乙方业务联系人\",\"甲方业务联系人电话\",\"甲方业务联系人邮箱\",\"乙方业务联系人电话\",\"乙方业务联系人邮箱\",\"甲方签署日期\",\"乙方签署日期\",\"甲方法人或代表人是否签字\",\"乙方法人或代表人是否签字\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于各类购销合同');
INSERT INTO `contract_extract_template` VALUES (4, '劳动合同模板', 'system', 'labor', '[\"员工姓名\",\"身份证号\",\"职位名称\",\"工作地点\",\"合同期限类型\",\"合同起止日期\",\"试用期期限\",\"试用期工资\",\"转正后工资\",\"工资构成明细\",\"工资支付时间\",\"工作时间安排\",\"休息休假制度\",\"社会保险缴纳\",\"福利待遇\",\"保密条款\",\"竞业限制条款\",\"培训服务期\",\"解除合同条件\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于劳动合同');
INSERT INTO `contract_extract_template` VALUES (5, '建筑合同模板', 'system', 'construction', '[\"工程名称\",\"工程地点\",\"工程内容\",\"工程范围\",\"承包方式\",\"合同价款\",\"付款进度安排\",\"工程期限\",\"开工日期\",\"竣工日期\",\"质量标准\",\"工程变更条款\",\"工程验收标准\",\"质保金比例\",\"质保期限\",\"安全责任条款\",\"工程进度报告要求\",\"材料供应方式\",\"违约责任\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于建筑工程承包合同');
INSERT INTO `contract_extract_template` VALUES (6, '技术合同模板', 'system', 'technical', '[\"技术项目名称\",\"技术内容\",\"技术指标\",\"技术成果形式\",\"技术开发方式\",\"研究开发经费\",\"经费支付方式\",\"技术资料交付\",\"技术指导要求\",\"验收标准\",\"知识产权归属\",\"技术保密条款\",\"后续改进技术归属\",\"技术风险责任\",\"违约金计算方式\",\"技术培训条款\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于技术开发、技术转让等技术合同');
INSERT INTO `contract_extract_template` VALUES (7, '知识产权合同模板', 'system', 'intellectual', '[\"知识产权类型\",\"知识产权名称\",\"注册/登记号\",\"许可/转让范围\",\"地域限制\",\"使用期限\",\"许可/转让费用\",\"支付方式\",\"权利保证条款\",\"侵权责任\",\"改进技术归属\",\"再许可权限\",\"合同备案要求\",\"权利维持义务\",\"合同终止条件\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于知识产权许可、转让合同');
INSERT INTO `contract_extract_template` VALUES (8, '运营服务合同模板', 'system', 'operation', '[\"服务内容描述\",\"服务标准\",\"服务期限\",\"服务地点\",\"服务人员要求\",\"服务时间\",\"服务费用\",\"费用支付周期\",\"绩效考核标准\",\"数据保密条款\",\"服务报告要求\",\"突发事件处理\",\"服务变更流程\",\"服务终止条件\",\"过渡期安排\"]', 'system', '2025-08-07 11:03:10', '2025-08-07 11:03:10', 1, '适用于运营服务类合同');
INSERT INTO `contract_extract_template` VALUES (9, '购销合同模板 - 副本', 'user', 'purchase', '[\"甲方公司名称\",\"乙方公司名称\",\"甲方通信地址\",\"乙方通信地址\",\"合同名称\",\"合同编号\",\"合同总金额小写\",\"币种\",\"合同总金额大写\",\"付款节点\",\"付款比例\",\"付款金额（小写）\",\"付款金额（大写）\",\"合同生效日期\",\"合同终止日期\",\"合同有效期\",\"交货单位\",\"交货地点\",\"交货时间\",\"验收时间\",\"运输方式\",\"运输费用\",\"增值率税率\",\"税额\",\"发票类型\",\"服务期限\",\"保证金金额（小写）\",\"保证金金额（大写）\",\"保证金比例\",\"保证金归还节点\",\"争议仲裁地\",\"甲方纳税人识别号\",\"乙方纳税人识别号\",\"甲方注册地址\",\"乙方注册地址\",\"甲方登记电话\",\"乙方登记电话\",\"甲方开户行\",\"乙方开户行\",\"甲方银行账号名称\",\"乙方银行账号名称\",\"甲方银行账号\",\"乙方银行账号\",\"违约内容\",\"甲方邮政编码\",\"乙方邮政编码\",\"甲方法人\",\"乙方法人\",\"甲方业务联系人\",\"乙方业务联系人\",\"甲方业务联系人电话\",\"甲方业务联系人邮箱\",\"乙方业务联系人电话\",\"乙方业务联系人邮箱\",\"甲方签署日期\",\"乙方签署日期\",\"甲方法人或代表人是否签字\",\"乙方法人或代表人是否签字\",\"是否有保密条款\"]', 'user123', '2025-08-07 11:31:24', '2025-08-07 11:31:24', 0, '用户自定义模板');

-- ----------------------------
-- Table structure for contract_rule
-- ----------------------------
DROP TABLE IF EXISTS `contract_rule`;
CREATE TABLE `contract_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime(0) NOT NULL,
  `update_time` datetime(0) NOT NULL,
  `templateId` bigint NOT NULL,
  `template_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_contract_rule_template_id`(`template_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of contract_rule
-- ----------------------------
INSERT INTO `contract_rule` VALUES (1, 'purchase', '进度款开票-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"开票履约\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"进度款开票\"], \"required\": true },\r\n    \"进度节点\": { \"type\": \"string\" },\r\n    \"阶段性金额\": { \"type\": \"number\" },\r\n    \"进度证明编号\": { \"type\": \"string\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"偏移天数\": { \"type\": \"integer\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"基准日\": [\"取‘进度确认日期/当期结算确认日’。\"],\r\n      \"偏移天数\": [\"‘N个自然日内’写入本字段并计算到期日。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 3);
INSERT INTO `contract_rule` VALUES (2, 'construction', '应付-预付款-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"应付任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"预付款\"], \"required\": true },\r\n    \"金额依据\": { \"type\": \"string\" },\r\n    \"账期\": { \"type\": \"string\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"偏移天数\": { \"type\": \"integer\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"基准日\": [\"签署日期\"],\r\n      \"到期日\": [\"签署日 + 预付款期限。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 5);
INSERT INTO `contract_rule` VALUES (3, 'lease', '预付款开票-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"开票履约\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"预付款开票\"], \"required\": true },\r\n    \"触发条件\": { \"type\": \"array\", \"itemType\": \"string\", \"itemEnum\": [\"预付款\",\"首笔款\",\"合同生效后\"] },\r\n    \"开票金额\": { \"type\": \"number\" },\r\n    \"开票金额大写\": { \"type\": \"string\", \"pattern\": \"[零壹贰叁肆伍陆柒捌玖拾佰仟万亿角分整]+\" },\r\n    \"纳税人识别号\": { \"type\": \"string\", \"pattern\": \"^[A-Z0-9]{15,20}$\" },\r\n    \"发票类型\": { \"type\": \"string\", \"enum\": [\"增值税专用发票\",\"增值税普通发票\"] },\r\n    \"开票时限\": { \"type\": \"string\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"偏移天数\": { \"type\": \"integer\" },\r\n    \"偏移工作日\": { \"type\": \"integer\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"global\": [\r\n      \"仅输出列出的键；日期格式 yyyy-MM-dd；金额用阿拉伯数字。\",\r\n      \"若出现百分比，请额外输出开票比例字段（若存在）。\"\r\n    ],\r\n    \"fields\": {\r\n      \"触发条件\": [\"从条款中抽取如‘预付款/首笔款/合同生效后’等触发词，去重输出。\"],\r\n      \"基准日\": [\"若出现‘签署后X日’取合同签署日；出现‘收到预付款后Y日’取付款到账日。\"],\r\n      \"偏移工作日\": [\"若条款为工作日写本字段；自然日写偏移天数。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 2);
INSERT INTO `contract_rule` VALUES (4, 'technical', '应付-进度款-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"应付任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"进度款\"], \"required\": true },\r\n    \"金额依据\": { \"type\": \"string\" },\r\n    \"账期\": { \"type\": \"string\" },\r\n    \"最近进度批准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"偏移天数\": { \"type\": \"integer\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"基准日\": [\"最近进度批准日\"],\r\n      \"到期日\": [\"进度批准日 + 付款缓冲期。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 6);
INSERT INTO `contract_rule` VALUES (5, 'labor', '验收款开票-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"开票履约\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"验收款开票\"], \"required\": true },\r\n    \"触发事件\": { \"type\": \"array\", \"itemType\": \"string\", \"itemEnum\": [\"验收合格\",\"终验完成\",\"项目交付\"] },\r\n    \"开票比例\": { \"type\": \"string\", \"pattern\": \"^\\d{1,3}%$\" },\r\n    \"验收证明编号\": { \"type\": \"string\" },\r\n    \"税点说明\": { \"type\": \"string\", \"pattern\": \"^(6%|9%|13%)$\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"偏移工作日\": { \"type\": \"integer\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"global\": [\"仅输出列出的键；日期yyyy-MM-dd；比例保留%符号。\"],\r\n    \"fields\": {\r\n      \"基准日\": [\"取‘验收确认日期/终验完成日’。\"],\r\n      \"偏移工作日\": [\"‘N个工作日内’填入本字段并计算到期日。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 4);
INSERT INTO `contract_rule` VALUES (6, 'intellectual', '应付-尾款-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"应付任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"尾款\"], \"required\": true },\r\n    \"金额依据\": { \"type\": \"string\" },\r\n    \"账期\": { \"type\": \"string\" },\r\n    \"验收日期\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"合同行最晚付款日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"规则原文\": { \"type\": \"string\" },\r\n    \"基准日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"基准日\": [\"验收日期\"],\r\n      \"到期日\": [\"max(验收日+账期, 合同最晚付款日)。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 7);
INSERT INTO `contract_rule` VALUES (7, 'operation', '应收-预收款-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"应收任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"预收款\"], \"required\": true },\r\n    \"收款账户\": { \"type\": \"string\" },\r\n    \"开户行\": { \"type\": \"string\" },\r\n    \"逾期条款\": { \"type\": \"string\" },\r\n    \"应付款到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"应收提醒日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"应收提醒日\": [\"应收提醒日 = 应付款到期日 + 1个工作日（逾期即触发）。\"]\r\n    }\r\n  }\r\n}', '2025-08-08 19:15:09', '2025-08-11 09:50:41', 0, 8);
INSERT INTO `contract_rule` VALUES (9, 'common', '通用合同模板', '{\r\n  \"version\" : \"1.0\",\r\n  \"contractType\" : \"common\",\r\n  \"defaults\" : {\r\n    \"type\" : \"string\",\r\n    \"required\" : false,\r\n    \"normalize\" : [ \"trim\" ]\r\n  },\r\n  \"fields\" : {\r\n    \"合同编号\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"合同名称\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"甲方名称\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"乙方名称\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"甲方联系地址\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"乙方联系地址\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"甲方联系人及电话\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"乙方联系人及电话\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"合同签署日期\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"合同有效期\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"合同起始时间\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"合同终止时间\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"签署人信息\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"违约责任条款\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"争议解决方式\" : {\r\n      \"type\" : \"string\"\r\n    },\r\n    \"适用法律\" : {\r\n      \"type\" : \"string\"\r\n    }\r\n  },\r\n  \"prompt\" : {\r\n    \"global\" : [ ],\r\n    \"negative\" : [ ],\r\n    \"format\" : [ ],\r\n    \"fields\" : { }\r\n  }\r\n}', '2025-08-10 10:09:04', '2025-08-10 10:09:04', 0, 1);
INSERT INTO `contract_rule` VALUES (13, 'expiry_reminder', '合同到期提醒-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"到期提醒\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"合同到期提醒\"], \"required\": true },\r\n    \"合同起始日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"合同到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"续约条款\": { \"type\": \"string\" },\r\n    \"终止条件\": { \"type\": \"string\" },\r\n    \"提醒日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"提醒日\": [\"若无特别约定，提醒日 = 合同到期日 - 30天。\"]\r\n    }\r\n  }\r\n}', '2025-08-11 09:50:41', '2025-08-11 09:50:41', 10, 10);
INSERT INTO `contract_rule` VALUES (18, 'collection_fulfillment', '应收-尾款-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"应收任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"尾款\"], \"required\": true },\r\n    \"收款账户\": { \"type\": \"string\" },\r\n    \"开户行\": { \"type\": \"string\" },\r\n    \"逾期条款\": { \"type\": \"string\" },\r\n    \"应付款到期日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"应收提醒日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"应收提醒日\": [\"应收提醒日 = 应付款到期日 + 1个工作日（逾期即触发）。\"]\r\n    }\r\n  }\r\n}', '2025-08-11 09:50:41', '2025-08-11 09:50:41', 9, 9);
INSERT INTO `contract_rule` VALUES (19, 'expiry_reminder', '服务到期提醒-规则', '{\r\n  \"fields\": {\r\n    \"任务类型\": { \"type\": \"string\", \"enum\": [\"到期任务\"], \"required\": true },\r\n    \"子类型\": { \"type\": \"string\", \"enum\": [\"服务到期\"], \"required\": true },\r\n    \"服务截止日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" },\r\n    \"提醒日\": { \"type\": \"date\", \"outputFormat\": \"yyyy-MM-dd\" }\r\n  },\r\n  \"prompt\": {\r\n    \"fields\": {\r\n      \"提醒日\": [\"如未约定，默认提醒日 = 服务截止日 - 30天。\"]\r\n    }\r\n  }\r\n}', '2025-08-11 09:50:41', '2025-08-11 09:50:41', 11, 11);

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installed_on` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flyway_schema_history
-- ----------------------------
INSERT INTO `flyway_schema_history` VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'root', '2025-08-07 11:03:08', 0, 1);
INSERT INTO `flyway_schema_history` VALUES (2, '2', 'create contract extract history table', 'SQL', 'V2__create_contract_extract_history_table.sql', 1725749092, 'root', '2025-08-07 12:00:26', 126, 1);
INSERT INTO `flyway_schema_history` VALUES (3, '3', 'create contract rule table', 'SQL', 'V3__create_contract_rule_table.sql', -1775363827, 'root', '2025-08-08 18:27:44', 99, 1);
INSERT INTO `flyway_schema_history` VALUES (4, '3', 'create contract rule table', 'DELETE', 'V3__create_contract_rule_table.sql', -1775363827, 'root', '2025-08-10 11:42:15', 0, 1);
INSERT INTO `flyway_schema_history` VALUES (5, '3', 'create contract rule table', 'SQL', 'V3__create_contract_rule_table.sql', -1775363827, 'root', '2025-08-10 15:43:23', 23, 1);
INSERT INTO `flyway_schema_history` VALUES (6, '4', 'alter contract rule to template', 'SQL', 'V4__alter_contract_rule_to_template.sql', -1756599790, 'root', '2025-08-10 15:51:04', 147, 1);
INSERT INTO `flyway_schema_history` VALUES (7, '5', 'create auto fulfillment tables', 'SQL', 'V5__create_auto_fulfillment_tables.sql', 1274718327, 'root', '2025-08-10 16:34:32', 208, 1);
INSERT INTO `flyway_schema_history` VALUES (8, '6', 'auto fulfillment dicts', 'SQL', 'V6__auto_fulfillment_dicts.sql', 1460385375, 'root', '2025-08-10 17:13:37', 252, 1);
INSERT INTO `flyway_schema_history` VALUES (9, '7', 'auto fulfillment category and binding', 'SQL', 'V7__auto_fulfillment_category_and_binding.sql', 726914008, 'root', '2025-08-10 17:56:13', 432, 1);
INSERT INTO `flyway_schema_history` VALUES (10, '8', 'auto fulfillment seed types keywords templates', 'SQL', 'V8__auto_fulfillment_seed_types_keywords_templates.sql', 386697706, 'root', '2025-08-10 17:58:43', 31, 1);
INSERT INTO `flyway_schema_history` VALUES (11, '9', 'create contract risk review tables', 'SQL', 'V9__seed_auto_fulfillment_rules.sql', 132941065, 'root', '2025-08-11 09:50:41', 59, 1);
INSERT INTO `flyway_schema_history` VALUES (12, '9', 'seed auto fulfillment rules', 'DELETE', 'V9__seed_auto_fulfillment_rules.sql', -1500786437, 'root', '2025-08-11 10:11:04', 0, 1);
INSERT INTO `flyway_schema_history` VALUES (13, '9', 'create contract risk review tables', 'SQL', 'V9__create_contract_risk_review_tables.sql', 132941065, 'root', '2025-08-11 16:53:39', 403, 1);
INSERT INTO `flyway_schema_history` VALUES (14, '10', 'seed risk review', 'SQL', 'V10__seed_risk_review.sql', -1592270153, 'root', '2025-08-11 16:56:11', 299, 1);
INSERT INTO `flyway_schema_history` VALUES (15, '11', 'review prompt versioning', 'SQL', 'V11__review_prompt_versioning.sql', -1540634151, 'root', '2025-08-12 10:26:53', 229, 1);
INSERT INTO `flyway_schema_history` VALUES (16, '12', 'seed risk review full', 'SQL', 'V12__seed_risk_review_full.sql', 934784902, 'root', '2025-08-12 15:30:58', 231, 1);
INSERT INTO `flyway_schema_history` VALUES (17, '13', 'refine risk text cn', 'SQL', 'V13__refine_risk_text_cn.sql', -1141215407, 'root', '2025-08-12 17:03:06', 94, 1);

-- ----------------------------
-- Table structure for profile_prompt_version
-- ----------------------------
DROP TABLE IF EXISTS `profile_prompt_version`;
CREATE TABLE `profile_prompt_version`  (
  `profile_id` bigint NOT NULL,
  `prompt_id` bigint NOT NULL,
  `version_id` bigint NOT NULL,
  PRIMARY KEY (`profile_id`, `prompt_id`) USING BTREE,
  INDEX `idx_version`(`version_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of profile_prompt_version
-- ----------------------------

-- ----------------------------
-- Table structure for review_action
-- ----------------------------
DROP TABLE IF EXISTS `review_action`;
CREATE TABLE `review_action`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_id` bigint NOT NULL,
  `action_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_type` enum('COPY','REPLACE','LINK') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_action_prompt`(`prompt_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 80 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_action
-- ----------------------------

-- ----------------------------
-- Table structure for review_clause_type
-- ----------------------------
DROP TABLE IF EXISTS `review_clause_type`;
CREATE TABLE `review_clause_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clause_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `clause_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `clause_code`(`clause_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 92 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_clause_type
-- ----------------------------
INSERT INTO `review_clause_type` VALUES (14, '主体', '合同主体', 10, 1, NULL, '2025-08-11 16:56:10', '2025-08-11 16:56:10');
INSERT INTO `review_clause_type` VALUES (15, '法律引用', '法律条款引用', 20, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (16, '财务条款', '价款与支付', 30, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (17, '履行', '履约安排', 40, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (18, '验收', '验收与质量', 50, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (19, '知识产权', '知识产权约定', 60, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (20, '违约责任', '违约与赔偿', 70, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (21, '保密', '保密约定', 80, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (22, '不可抗力', '不可抗力约定', 90, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (23, '争议解决', '争议处理', 100, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (24, '合同解除', '解除条款', 110, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (25, '合同形式与生效', '生效与形式', 120, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_clause_type` VALUES (26, '其他', '其他事项', 130, 1, NULL, '2025-08-11 16:56:10', '2025-08-12 17:03:06');

-- ----------------------------
-- Table structure for review_point
-- ----------------------------
DROP TABLE IF EXISTS `review_point`;
CREATE TABLE `review_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clause_type_id` bigint NOT NULL,
  `point_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `point_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `algorithm_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `point_code`(`point_code`) USING BTREE,
  INDEX `idx_point_clause`(`clause_type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 266 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_point
-- ----------------------------
INSERT INTO `review_point` VALUES (18, 23, '3566', '诉讼/仲裁择一约定核对', '诉讼/仲裁择一约定核对', NULL, 1, 1, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (19, 23, '3570', '仲裁机构约定核对', '仲裁机构约定核对', NULL, 2, 1, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (20, 22, '653', '不可抗力条款核对', '不可抗力条款核对', NULL, 1, 1, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (21, 14, '3649', '内部相对方名称规范核对', '内部相对方名称规范核对', NULL, 1, 1, '2025-08-11 16:56:10', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (22, 14, '3702', '外部相对方名称规范核对', '外部相对方名称规范核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (23, 15, '605', '法规引用准确性核对', '法规引用准确性核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (24, 21, '652', '保密义务与范围核对', '保密义务与范围核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (25, 19, '3423', '知识产权归属约定核对', '知识产权归属约定核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (26, 19, '674', '知产无瑕疵保证与侵权责任核对', '知产无瑕疵保证与侵权责任核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (27, 17, '578', '履约期限核对', '履约期限核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (28, 17, '689', '履约/交付地点核对', '履约/交付地点核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (29, 18, '3388', '验收时限核对', '验收时限核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (30, 18, '369', '验收标准与质量核对', '验收标准与质量核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (31, 18, '370', '不合格处理措施核对', '不合格处理措施核对', NULL, 3, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (32, 25, '3424', '签署地点核对', '签署地点核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (33, 25, '3518', '签署日期核对', '签署日期核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (34, 25, '3520', '合同编号填写核对', '合同编号填写核对', NULL, 3, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (35, 25, '3568', '生效条件约定核对', '生效条件约定核对', NULL, 4, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (36, 20, '651', '逾期履行责任核对', '逾期履行责任核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (37, 20, '356', '逾期付款责任核对', '逾期付款责任核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (38, 20, '478', '违约金标准合理性核对', '违约金标准合理性核对', NULL, 3, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (39, 20, '567', '赔偿上限约定核对', '赔偿上限约定核对', NULL, 4, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (40, 20, '374', '损失赔偿范围核对', '损失赔偿范围核对', NULL, 5, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (41, 20, '3430', '直接损失赔偿约定核对', '直接损失赔偿约定核对', NULL, 6, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (42, 20, '3446', '连带责任范围核对', '连带责任范围核对', NULL, 7, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (43, 16, '517', '金额大小写一致性核验', '金额大小写一致性核验', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (44, 16, '3521', '含税/不含税约定核对', '含税/不含税约定核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (45, 16, '3549', '税率约定核验', '税率约定核验', NULL, 3, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (46, 16, '375', '价款组成范围核对', '价款组成范围核对', NULL, 4, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (47, 16, '704', '支付节点与期限核对', '支付节点与期限核对', NULL, 5, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (48, 16, '3524', '支付方式与账户核对', '支付方式与账户核对', NULL, 6, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (49, 16, '378', '先款后票条款核对', '先款后票条款核对', NULL, 7, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (50, 16, '3523', '发票类型约定核对', '发票类型约定核对', NULL, 8, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (51, 16, '3445', '税率调整处理核对', '税率调整处理核对', NULL, 9, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (52, 24, '3442', '单方通知解除权约定核对', '单方通知解除权约定核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (53, 26, '3422', '合同份数与留存核对', '合同份数与留存核对', NULL, 1, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (54, 26, '3437', '主合同与附件优先顺序核对', '主合同与附件优先顺序核对', NULL, 2, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (55, 23, '3578', '管辖法院约定核对', '管辖法院约定核对', NULL, 3, 1, '2025-08-11 16:56:11', '2025-08-12 17:03:06');
INSERT INTO `review_point` VALUES (242, 14, '000001', '结算条款', '结算条款', NULL, 1, 1, '2025-08-12 18:56:42', '2025-08-12 18:56:42');
INSERT INTO `review_point` VALUES (246, 16, 'ZX-0001', '收付款时间', '收付款时间', NULL, 1, 1, '2025-08-13 10:28:40', '2025-08-13 10:28:40');
INSERT INTO `review_point` VALUES (247, 26, 'ZX-0002', '合同失效日期', '合同失效日期', NULL, 1, 1, '2025-08-13 11:04:02', '2025-08-13 11:04:02');
INSERT INTO `review_point` VALUES (248, 26, 'ZX-0003', '合同有效期', '合同有效期', NULL, 1, 1, '2025-08-13 11:10:31', '2025-08-13 11:10:31');
INSERT INTO `review_point` VALUES (256, 26, 'ZX-0004', '附件引用', '附件引用', NULL, 1, 1, '2025-08-13 15:13:04', '2025-08-13 15:13:04');
INSERT INTO `review_point` VALUES (257, 26, 'ZX-0005', '序号', '序号', NULL, 1, 1, '2025-08-13 15:41:14', '2025-08-13 15:41:14');
INSERT INTO `review_point` VALUES (258, 26, 'ZX-0006', '对方企业', '对方企业', NULL, 1, 1, '2025-08-13 15:51:44', '2025-08-13 15:51:44');
INSERT INTO `review_point` VALUES (259, 20, 'ZX-0007', '违约责任', '违约责任', NULL, 1, 1, '2025-08-13 16:03:23', '2025-08-13 16:03:23');
INSERT INTO `review_point` VALUES (260, 23, 'ZX-0008', '诉讼管辖', '诉讼管辖', NULL, 1, 1, '2025-08-13 16:45:14', '2025-08-13 16:45:14');
INSERT INTO `review_point` VALUES (261, 16, 'ZX-0009', '结算条款', '结算条款', NULL, 1, 1, '2025-08-13 16:47:51', '2025-08-13 16:47:51');
INSERT INTO `review_point` VALUES (262, 16, 'ZX-0010', '付款渠道', '付款渠道', NULL, 1, 1, '2025-08-13 16:51:20', '2025-08-13 16:51:20');
INSERT INTO `review_point` VALUES (264, 26, 'ZX-0011', '关联主体', '关联主体', NULL, 1, 1, '2025-08-14 11:41:11', '2025-08-14 11:41:11');
INSERT INTO `review_point` VALUES (265, 18, 'ZX-0012', '质保金', '质保金', NULL, 1, 1, '2025-08-14 12:08:42', '2025-08-14 12:08:42');

-- ----------------------------
-- Table structure for review_profile
-- ----------------------------
DROP TABLE IF EXISTS `review_profile`;
CREATE TABLE `review_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profile_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `profile_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `profile_code`(`profile_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_profile
-- ----------------------------
INSERT INTO `review_profile` VALUES (4, 'p_1754967482103', '全局审核', 0, '从清单管理保存', '2025-08-12 10:58:02', '2025-08-12 10:58:02');
INSERT INTO `review_profile` VALUES (5, 'p_1754968113573', '财务审核', 0, '从清单管理保存', '2025-08-12 11:08:33', '2025-08-12 11:08:33');

-- ----------------------------
-- Table structure for review_profile_item
-- ----------------------------
DROP TABLE IF EXISTS `review_profile_item`;
CREATE TABLE `review_profile_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profile_id` bigint NOT NULL,
  `clause_type_id` bigint NOT NULL,
  `point_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_profile_point`(`profile_id`, `point_id`) USING BTREE,
  INDEX `idx_profile_item`(`profile_id`, `clause_type_id`, `sort_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 165 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_profile_item
-- ----------------------------
INSERT INTO `review_profile_item` VALUES (95, 5, 0, 21, 1);
INSERT INTO `review_profile_item` VALUES (96, 5, 0, 43, 2);
INSERT INTO `review_profile_item` VALUES (97, 5, 0, 44, 3);
INSERT INTO `review_profile_item` VALUES (98, 5, 0, 45, 4);
INSERT INTO `review_profile_item` VALUES (99, 5, 0, 46, 5);
INSERT INTO `review_profile_item` VALUES (100, 5, 0, 47, 6);
INSERT INTO `review_profile_item` VALUES (101, 4, 0, 21, 1);
INSERT INTO `review_profile_item` VALUES (102, 4, 0, 22, 2);
INSERT INTO `review_profile_item` VALUES (103, 4, 0, 23, 3);
INSERT INTO `review_profile_item` VALUES (104, 4, 0, 43, 4);
INSERT INTO `review_profile_item` VALUES (105, 4, 0, 44, 5);
INSERT INTO `review_profile_item` VALUES (106, 4, 0, 45, 6);
INSERT INTO `review_profile_item` VALUES (107, 4, 0, 46, 7);
INSERT INTO `review_profile_item` VALUES (108, 4, 0, 47, 8);
INSERT INTO `review_profile_item` VALUES (109, 4, 0, 48, 9);
INSERT INTO `review_profile_item` VALUES (110, 4, 0, 49, 10);
INSERT INTO `review_profile_item` VALUES (111, 4, 0, 50, 11);
INSERT INTO `review_profile_item` VALUES (112, 4, 0, 51, 12);
INSERT INTO `review_profile_item` VALUES (113, 4, 0, 27, 13);
INSERT INTO `review_profile_item` VALUES (114, 4, 0, 28, 14);
INSERT INTO `review_profile_item` VALUES (115, 4, 0, 29, 15);
INSERT INTO `review_profile_item` VALUES (116, 4, 0, 30, 16);
INSERT INTO `review_profile_item` VALUES (117, 4, 0, 31, 17);
INSERT INTO `review_profile_item` VALUES (118, 4, 0, 25, 18);
INSERT INTO `review_profile_item` VALUES (119, 4, 0, 26, 19);
INSERT INTO `review_profile_item` VALUES (120, 4, 0, 36, 20);
INSERT INTO `review_profile_item` VALUES (121, 4, 0, 37, 21);
INSERT INTO `review_profile_item` VALUES (122, 4, 0, 38, 22);
INSERT INTO `review_profile_item` VALUES (123, 4, 0, 39, 23);
INSERT INTO `review_profile_item` VALUES (124, 4, 0, 40, 24);
INSERT INTO `review_profile_item` VALUES (125, 4, 0, 41, 25);
INSERT INTO `review_profile_item` VALUES (126, 4, 0, 42, 26);

-- ----------------------------
-- Table structure for review_prompt
-- ----------------------------
DROP TABLE IF EXISTS `review_prompt`;
CREATE TABLE `review_prompt`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `point_id` bigint NOT NULL,
  `prompt_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status_type` enum('INFO','WARNING','ERROR') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `prompt_key`(`prompt_key`) USING BTREE,
  INDEX `idx_prompt_point`(`point_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 427 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_prompt
-- ----------------------------
INSERT INTO `review_prompt` VALUES (282, 20, '不可抗力条款确认', '不可抗力条款确认', '已识别不可抗力条款，请确认情形列举、影响范围与恢复义务明确。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (283, 20, '不可抗力条款缺失', '不可抗力条款缺失', '未见不可抗力安排，建议补充情形示例、通知与证明义务及后续处理。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (284, 20, '不可抗力通知与证明义务不明', '不可抗力通知与证明义务不明', '触发后的通知时限与证明材料未明确，建议补足流程性约定。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (285, 18, '或诉或裁风险', '或诉或裁风险', '同时允许诉讼或仲裁可能致仲裁协议无效，建议仅选择一种方式并明确。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (286, 18, '选择式文本未进行选择', '选择式文本未进行选择', '争议解决以选择题呈现但未勾选，建议明确仅选诉讼或仲裁之一。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (287, 18, '争议解决条款内部矛盾', '争议解决条款内部矛盾', '同一合同内存在冲突路径，建议统一为单一争议解决方式。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (288, 55, '争议解决条款缺失', '争议解决条款缺失', '未识别到争议解决方式，建议补充并避免与仲裁条款冲突。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (289, 55, '诉讼管辖法院确认', '诉讼管辖法院确认', '已约定诉讼方式，请核对管辖法院是否对己方有利且合法有效。', 'INFO', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (290, 55, '诉讼管辖法院缺失', '诉讼管辖法院缺失', '选择诉讼但未指定管辖，建议补充并与专属管辖情形避冲突。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (291, 19, '仲裁机构确认', '仲裁机构确认', '已识别仲裁机构，请确认名称准确且唯一，避免无效风险。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (292, 19, '仲裁机构缺失', '仲裁机构缺失', '未指定仲裁机构，建议补充唯一机构并核对最新全称。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (293, 21, '首部未见内部相对方名称', '首部未见内部相对方名称', '未在合同首部识别到内部相对方全称，建议补充营业执照一致的标准名称。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (294, 21, '尾部内部相对方名称不规范', '尾部内部相对方名称不规范', '尾部名称与登记信息不一致或缺少组织标识，建议统一至工商登记全称。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (295, 21, '内部相对方名称前后不一致', '内部相对方名称前后不一致', '首尾出现不同版本的己方名称，需核对并保持一致，以免影响效力与履行。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (296, 22, '未见外部相对方名称', '未见外部相对方名称', '未识别到外部相对方标准全称，建议补充并与登记信息核验一致。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (297, 22, '外部相对方名称前后不一致', '外部相对方名称前后不一致', '首尾或条款内对方名称存在差异，建议统一到工商登记全称。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (298, 22, '外部相对方名称已识别请核对', '外部相对方名称已识别请核对', '已识别到外部相对方名称，请确认与最新登记信息一致且无简写。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (299, 23, '法律全称引用核对', '法律全称引用核对', '发现对失效法条的全称引用，建议统一为现行有效规范性文件的标准全称。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (300, 23, '法律简称引用核对', '法律简称引用核对', '出现简写或旧简称，可能导致歧义，建议采用现行法统一简称。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (301, 23, '仍引用已失效条文', '仍引用已失效条文', '文本仍引用已被新法替代的内容，建议更新为对应现行条款。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 17:03:06');
INSERT INTO `review_prompt` VALUES (302, 43, '大小写金额不一致', '大小写金额不一致', '同一金额的中文大写与数字小写不一致，建议以书面约定的优先规则修正。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (303, 43, '大小写金额一致性确认', '大小写金额一致性确认', '已识别金额表述，请复核大写与小写一致性，避免结算争议。', 'INFO', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (304, 44, '含税价确认', '含税价确认', '文本出现含税价表述，请确认税费口径、税率变化时的结算原则。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (305, 44, '是否含税未约定', '是否含税未约定', '未明确价款是否含税，建议同时列示含税价与不含税价并注明口径。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (306, 44, '含税与不含税表述不一致', '含税与不含税表述不一致', '不同条款对含税口径描述冲突，建议统一并明确税负承担规则。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (307, 45, '税率确认', '税率确认', '已识别增值税税率，请核对是否匹配纳税人身份及当前政策。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (308, 45, '税率缺失', '税率缺失', '未明确适用税率，建议补充并注明随政策调整的处理方式。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (309, 45, '税率与纳税人身份不匹配', '税率与纳税人身份不匹配', '税率与纳税人类型或业务实情不符，建议调整为合规口径。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (310, 46, '价款构成范围确认', '价款构成范围确认', '已列示价款包含的费用，请确认是否覆盖运输、安装、服务等必要成本。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (311, 46, '价款构成范围缺失', '价款构成范围缺失', '未明确价款包含范围，建议结合业务列明税费与各项支出归属。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (312, 46, '价款构成范围表述不清', '价款构成范围表述不清', '费用口径与边界描述模糊，建议约定“不含/包含”清单与示例。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (313, 47, '支付时间确认', '支付时间确认', '已识别支付节点，请确认期限、条件与逾期处理一致且可执行。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (314, 47, '支付时间缺失', '支付时间缺失', '未约定支付时间或节点，建议明确具体日期或计算规则。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (315, 47, '支付期限不明确', '支付期限不明确', '存在“及时/尽快”等模糊用语，建议换成可衡量的期限表述。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (316, 48, '支付途径确认', '支付途径确认', '已识别支付方式与账户，请确认开户名、账号、银行等要素准确。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (317, 48, '支付途径缺失', '支付途径缺失', '未明确转账/票据/现金等支付方式，建议补充并约定变更流程。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (318, 49, '先款后票确认', '先款后票确认', '约定先付款后开票，建议评估现金流与抵扣影响并设置保障措施。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (319, 49, '先票后款确认', '先票后款确认', '若改为先票后款，需同步调整结算与验收流程，确保凭证闭环。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (320, 50, '专用发票确认', '专用发票确认', '识别到专票约定，请确认开票主体资质与抵扣需求匹配。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (321, 50, '普通发票确认', '普通发票确认', '识别到普票约定，如需抵扣请评估是否需调整为专票。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (322, 50, '发票类型缺失', '发票类型缺失', '未明确发票类型，建议明确为专票或普票并注明开具条件。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (323, 51, '存在税收调整政策', '存在税收调整政策', '文本包含税率调整条款，建议核对调价机制及税负分担。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (324, 51, '税收调整政策缺失', '税收调整政策缺失', '未约定税率变化的处理原则，建议补充“价税分离/不变含税总价”等方案。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (325, 51, '税率调整责任约定不清', '税率调整责任约定不清', '调整触发条件与结算口径不清晰，建议明确基准日与生效逻辑。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (326, 27, '履行时间确认', '履行时间确认', '已识别履行时间，请确认与工期/服务周期一致并可落地执行。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (327, 27, '履行时间缺失', '履行时间缺失', '未约定履行完成或交付的具体时间，建议明确日期或计算口径。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (328, 27, '履行时间不完整', '履行时间不完整', '仅出现阶段性描述，缺少起止/验收节点，建议补全关键里程碑。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (329, 28, '履行地点确认', '履行地点确认', '已识别履行/交付地点，请确认与物流、施工或服务安排一致。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (330, 28, '履行地点缺失', '履行地点缺失', '未明确履行地点，建议补充并约定变更流程与费用承担。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (331, 28, '交付或服务地点不一致', '交付或服务地点不一致', '不同条款对交付地与服务地描述不一致，建议统一口径。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (332, 30, '验收与质量标准确认', '验收与质量标准确认', '已识别验收/质量标准，请确认与行业标准或技术协议一致。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (333, 30, '验收与质量标准缺失', '验收与质量标准缺失', '未约定验收口径或质量标准，建议引用国家/行业或双方约定的更高标准。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (334, 30, '验收标准表述不清', '验收标准表述不清', '存在“合格/满足需求”等模糊表述，建议转为可检验的明确条款。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (335, 29, '验收时间条款', '验收时间条款', '已识别验收时限，请确认与交付计划匹配并预留整改周期。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (336, 29, '未约定验收时间', '未约定验收时间', '未见验收期限或触发条件，建议加入具体日期或触发事件。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (337, 29, '验收时间与履行安排冲突', '验收时间与履行安排冲突', '验收节点早于交付完成或与工期冲突，建议统一计划。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (338, 31, '验收不通过的处理措施确认', '验收不通过的处理措施确认', '已设置不通过后的整改/重验措施，请确认时限与费用承担明确。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (339, 31, '验收不通过的处理措施缺失', '验收不通过的处理措施缺失', '未约定不合格处理流程，建议补充整改、重验与解除等路径。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (340, 25, '知识产权归属确认', '知识产权归属确认', '已识别成果归属安排，请确认权利范围、地域、期限及费用口径。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (341, 25, '知识产权归属表述不清', '知识产权归属表述不清', '归属、许可与使用权描述模糊，建议以清单化方式明确。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (342, 26, '知产无瑕疵保证确认', '知产无瑕疵保证确认', '已约定不存在侵犯第三方权利的保证，请确认补救措施完备。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (343, 26, '知产无瑕疵保证缺失', '知产无瑕疵保证缺失', '未约定不侵权保证与违约处理，建议补充替换、许可或退费等方案。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (344, 26, '侵权责任未约定', '侵权责任未约定', '未明确被主张侵权时的责任承担与协助义务，建议完善条款。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (345, 24, '保密条款确认', '保密条款确认', '已识别保密条款，请核对保密信息范围、义务主体、期限与例外。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (346, 24, '保密条款缺失', '保密条款缺失', '未见保密约定，建议补充范围、期限、例外及违约责任。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (347, 24, '保密期限或范围不明', '保密期限或范围不明', '仅泛化描述“商业秘密”，建议补充可识别的范围与期限。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (348, 36, '迟延履行违约责任确认', '迟延履行违约责任确认', '已设置逾期责任，请核对计算口径、上限与继续履行安排。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (349, 36, '迟延履行违约责任缺失', '迟延履行违约责任缺失', '未约定逾期履行的处理方式，建议补充违约金与补救措施。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (350, 36, '迟延违约金标准不合理', '迟延违约金标准不合理', '违约金比例明显偏高或缺乏上限，建议结合业务下调并设封顶。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (351, 37, '迟延支付违约责任确认', '迟延支付违约责任确认', '已设置逾期付款的违约条款，请确认比例、宽限与解除权一致。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (352, 37, '迟延支付违约责任缺失', '迟延支付违约责任缺失', '未明确逾期付款的处理，建议补充违约金、停供与解除等路径。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (353, 37, '逾期后处理措施缺失', '逾期后处理措施缺失', '缺少逾期后的通知、催告与分期整改安排，建议完善流程。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (354, 38, '违约金标准确认', '违约金标准确认', '已识别违约金比例，请评估与交易风险匹配并避免显失公平。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (355, 38, '违约金金额未填写完整', '违约金金额未填写完整', '计算基数或比例缺失，建议补充清晰的计算方式与封顶规则。', 'ERROR', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (356, 38, '违约金标准过高', '违约金标准过高', '违约金设置偏高，建议按实际风险与行业惯例调整为合理区间。', 'ERROR', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (357, 39, '赔偿限额确认', '赔偿限额确认', '存在赔偿上限条款，请评估是否覆盖可预见损失及例外情形。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (358, 39, '赔偿限额未约定', '赔偿限额未约定', '未设置赔偿上限，建议结合业务规模与保险安排确定限额。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (359, 40, '损失赔偿包含间接损失', '损失赔偿包含间接损失', '条款包含“全部损失/预期利益”等表述，建议评估范围是否过宽。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (360, 40, '损失赔偿未明确包含间接损失', '损失赔偿未明确包含间接损失', '仅出现“直接损失/实际损失”等表述，如为相对方请确认是否足够。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (361, 40, '律师费/差旅费承担不明', '律师费/差旅费承担不明', '费用承担未细化，建议明确举证标准、范围与计取方式。', 'WARNING', 3, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (362, 41, '直接损失赔偿确认', '直接损失赔偿确认', '仅对直接损失赔偿的表述已识别，建议结合业务评估可接受性。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (363, 41, '仅赔直接损失的风险', '仅赔直接损失的风险', '如排除了间接损失，可能无法覆盖必要费用，建议审慎评估。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (364, 42, '连带责任风险', '连带责任风险', '存在连带责任约定，未限定范围或顺序可能加重责任负担。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (365, 42, '连带责任范围不清', '连带责任范围不清', '未明确连带范围、分担与追偿机制，建议细化以降低争议。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (366, 52, '无理由通知解除权确认', '无理由通知解除权确认', '存在单方通知解除条款，建议评估冷静期、补偿与未履行费用分配。', 'ERROR', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (367, 52, '解除流程与期限未约定', '解除流程与期限未约定', '解除条件、通知路径与生效时间未细化，建议补充流程规范。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (368, 35, '合同生效条件缺失', '合同生效条件缺失', '未明确生效触发点，建议约定签署/盖章/审批或特定日期生效。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (369, 35, '生效条件表述不清', '生效条件表述不清', '生效条件存在多义或冲突，建议统一并避免前后矛盾。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (370, 34, '合同编号缺失', '合同编号缺失', '未设置合同编号，建议补充并与内控系统一致便于归档检索。', 'INFO', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (371, 34, '合同编号未填写完整', '合同编号未填写完整', '编号位数或规则不完整，建议按公司编号体系补齐。', 'INFO', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (372, 33, '签订日期确认', '签订日期确认', '已识别签订日期，请确认不早于实际签署且与审批流一致。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (373, 33, '签订日期缺失', '签订日期缺失', '未见签订日期，建议补充明确并避免倒签风险。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (374, 32, '签订地点缺失', '签订地点缺失', '未约定签订地点，建议补充并与印章/签署主体所在地匹配。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (375, 32, '签订地点确认', '签订地点确认', '已识别签订地点，请确认与双方签署安排相符。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (376, 54, '主合同与附件优先级确认', '主合同与附件优先级确认', '存在主合同与附件并行的情形，建议明确冲突时的适用顺序。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (377, 54, '主合同与附件优先级未约定', '主合同与附件优先级未约定', '未约定优先级，建议补充“以本合同/以补充协议为准”等表述。', 'WARNING', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (378, 53, '未约定合同份数', '未约定合同份数', '未明确合同份数与流向，建议补充并与归档制度一致。', 'WARNING', 1, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (379, 53, '合同份数确认', '合同份数确认', '已识别份数约定，请确认各方留存数量一致且具同等效力。', 'INFO', 2, 1, '2025-08-12 15:30:57', '2025-08-12 15:30:57');
INSERT INTO `review_prompt` VALUES (380, 242, '合同约定的预付款比例为20%，超过公司的10%', '合同约定的预付款比例为20%，超过公司的10%', '您的预付款比例超出公司规定', 'WARNING', 1, 1, '2025-08-12 18:58:13', '2025-08-12 18:58:13');
INSERT INTO `review_prompt` VALUES (381, 247, '公司规定的合同失效日期不得超过寻源单据失效日期180天', '公司规定的合同失效日期不得超过寻源单据失效日期180天', '识别到该合同失效日期超出对应寻源单据失效日期180天以上，存在合规风险，请核查填写情况。', 'WARNING', 1, 1, '2025-08-13 11:06:30', '2025-08-13 11:06:30');
INSERT INTO `review_prompt` VALUES (382, 248, '公司规定的合同有效期不得超过寻源单据生效时长', '超出寻源单据生效时长', '识别到该合同有效期超出对应寻源单据生效时长，存在合规风险，请减少合同生效时长，或者增加寻源单据生效时长', 'WARNING', 1, 1, '2025-08-13 11:11:55', '2025-08-13 11:11:55');
INSERT INTO `review_prompt` VALUES (392, 256, '公司规定的附件引用内容必须存在', '引用内容不存在', '引用内容在合同中找不到，请确认引用内容在合同中是否存在', 'WARNING', 1, 1, '2025-08-13 15:13:35', '2025-08-13 15:13:35');
INSERT INTO `review_prompt` VALUES (393, 256, '公司规定的附件引用序号格式应一致', '引用序号格式不一致', '引用内容的序号与被引用内容的序号大小写不一致，请确认是否要修改引用序号', 'WARNING', 1, 1, '2025-08-13 15:40:07', '2025-08-13 15:40:07');
INSERT INTO `review_prompt` VALUES (394, 257, '公司规定的附件序号应与上级或同级编号规则一致', '编号规则不一致', '识别到当前附件的有序列表与上级或者同级编号规则不一致，请确认是否需要修改序号', 'WARNING', 1, 1, '2025-08-13 15:42:43', '2025-08-13 15:42:43');
INSERT INTO `review_prompt` VALUES (395, 257, '公司规定的附件序号格式应统一', '格式不一致', '识别到附件编号存在大小写或分隔符不一致问题，请确认是否需要修改序号格式。', 'WARNING', 1, 1, '2025-08-13 15:44:57', '2025-08-13 15:44:57');
INSERT INTO `review_prompt` VALUES (396, 257, '公司规定的附件序号不得重复', '重复', '识别到附件编号存在序号重复，请确认是否需要修改序号', 'INFO', 1, 1, '2025-08-13 15:46:19', '2025-08-13 15:46:19');
INSERT INTO `review_prompt` VALUES (397, 257, '公司规定的附件序号不得遗漏', '遗漏', '识别到附件编号存在序号遗漏，请确认是否需要修改序号。', 'INFO', 1, 1, '2025-08-13 15:50:26', '2025-08-13 15:50:26');
INSERT INTO `review_prompt` VALUES (398, 258, '公司规定的合同交易方应无风险', '天眼查风险', '根据天眼查信息，合同交易方存在风险，请确认风险。', 'INFO', 1, 1, '2025-08-13 15:52:19', '2025-08-13 15:52:19');
INSERT INTO `review_prompt` VALUES (399, 258, '公司规定的合同交易方应在天眼查可查询到', '天眼查未找到', '天眼查未查询到合同交易方，建议确认交易方主体名称是否正确。', 'INFO', 1, 1, '2025-08-13 15:53:13', '2025-08-13 15:53:13');
INSERT INTO `review_prompt` VALUES (400, 258, '公司规定的合同应包含对方知识产权违约责任条款', '缺失对方知识产权违约责任', '建议补充对方知识产权违约责任条款，如版权瑕疵、内容瑕疵、滥用品牌或侵犯他人权益等。条款示例：如因权利归属、瑕疵及违法等问题而引起的任何纠纷，由乙方负责解决纠纷，并承担包括侵权赔偿在内的一切责任。', 'INFO', 1, 1, '2025-08-13 15:57:47', '2025-08-13 15:57:47');
INSERT INTO `review_prompt` VALUES (401, 258, '公司规定的合同应包含对方违约金外赔偿条款', '缺失对方违约金外赔偿条款', '建议补充对方违约金外继续赔偿损失条款，条款示例：\"若支付的违约金不足弥补甲方损失的，乙方还应负责赔偿直至达到弥补全部损失为止。', 'INFO', 1, 1, '2025-08-13 16:00:14', '2025-08-13 16:00:14');
INSERT INTO `review_prompt` VALUES (402, 259, '公司规定的合同不应包含对方违约免责条款', '对方违约免责', '识别到对方可不承担违约责任，建议与法务确认是否符合公司规定。', 'INFO', 1, 1, '2025-08-13 16:03:52', '2025-08-13 16:03:52');
INSERT INTO `review_prompt` VALUES (403, 259, '公司规定的合同应使用\"违约金\"而非\"滞纳金\"表述', '滞纳金错误表述', '合同内一般不使用\"滞纳金\"的表述，建议更换成\"违约金\"。', 'INFO', 1, 1, '2025-08-13 16:18:20', '2025-08-13 16:18:20');
INSERT INTO `review_prompt` VALUES (404, 259, '公司规定的合同不应限制对方违约赔偿的最高限额。', '对方违约责任限额', '识别到对方违约赔偿的最高限额，建议与法务确认是否符合公司规定。', 'INFO', 1, 1, '2025-08-13 16:25:49', '2025-08-13 16:25:49');
INSERT INTO `review_prompt` VALUES (405, 259, '公司规定的违约金不应超过合同总额的20%。', '违约金过高', '识别到我方需承担违约金超过合同总额的20%，建议将违约金比例降低至20%以下。', 'INFO', 1, 1, '2025-08-13 16:36:37', '2025-08-13 16:36:37');
INSERT INTO `review_prompt` VALUES (406, 259, '公司规定的违约金比例不应超过每日千分之一。', '违约金比例过高', '识别到我方需承担违约金比例超过每日千分之一，建议降低到每日千分之一以下。', 'INFO', 1, 1, '2025-08-13 16:40:16', '2025-08-13 16:40:16');
INSERT INTO `review_prompt` VALUES (407, 260, '公司规定的诉讼管辖法院应符合规定', '公司规定的诉讼管辖法院应符合规定', '识别到诉讼管辖法院，建议确认是否符合公司法务规定。', 'INFO', 1, 1, '2025-08-13 16:46:08', '2025-08-13 16:46:08');
INSERT INTO `review_prompt` VALUES (408, 261, '公司规定的首付款比例不应超过合同总金额的30%', '首付款过高', '识别到首付款比例高于合同总金额的30%，建议首付款比例降低至30%以下。', 'INFO', 1, 1, '2025-08-13 16:49:01', '2025-08-13 16:49:01');
INSERT INTO `review_prompt` VALUES (409, 262, '公司规定的合同应约定明确的支付渠道', '付款渠道缺失', '建议约定明确的支付渠道（如：银行转账、汇票、支票、本票、承兑汇票、电汇、支付宝、微信支付、Paypal、Paytm、有赞等）。', 'INFO', 1, 1, '2025-08-13 16:51:54', '2025-08-13 16:51:54');
INSERT INTO `review_prompt` VALUES (410, 25, '公司规定的知识产权归属应符合实际需求', '知识产权归属对方', '交付成果知识产权权属约定中，知识产权部分或全部归属对方，建议与法务确认是否符合实际需求。', 'INFO', 1, 1, '2025-08-13 17:08:04', '2025-08-13 17:08:04');
INSERT INTO `review_prompt` VALUES (411, 48, '公司规定的合同应包含完整的收款银行信息', '收款银行信息不完整', '建议补充完整的收款银行信息，包括账户名称、开户行以及银行账户。', 'INFO', 1, 1, '2025-08-13 17:13:03', '2025-08-13 17:13:03');
INSERT INTO `review_prompt` VALUES (412, 30, '公司规定的合同应包含验收标准条款', '验收标准缺失', '建议补充验收标准相关条款。条款示例：\"交货前检验：卖方应在货物装运前向检验机构申请对货物的品质、规格、数境、重员、包装、安全、卫生及健康要求根据本合同中的规定[条款14.1.1]，或[xx国家标准/行业标准]，进行检验。', 'INFO', 1, 1, '2025-08-13 17:15:38', '2025-08-13 17:15:38');
INSERT INTO `review_prompt` VALUES (413, 30, '公司规定的实物采购合同应包含质保期条款', '质保期缺失', '未识别到质保期约定，具体实物采购建议添加质保期相关条款，软件、平台、音频、技术等产品或服务可不添加。', 'INFO', 1, 1, '2025-08-13 17:17:24', '2025-08-13 17:17:24');
INSERT INTO `review_prompt` VALUES (414, 45, '税费承担方缺失', '税费承担方缺失', '建议约定明确的含税情况及税费承担方。', 'INFO', 1, 1, '2025-08-14 11:33:38', '2025-08-14 11:33:38');
INSERT INTO `review_prompt` VALUES (415, 45, '合作方为个人，无法扣税', '合作方为个人，无法扣税', '合作方为个人，建议与法务确认是否需要约定代扣代缴义务。', 'INFO', 1, 1, '2025-08-14 11:36:36', '2025-08-14 11:36:36');
INSERT INTO `review_prompt` VALUES (416, 264, '合同中不存在关联交易。', '合同中不存在关联交易。', '合同中不存在关联交易。', 'INFO', 1, 1, '2025-08-14 11:45:59', '2025-08-14 11:45:59');
INSERT INTO `review_prompt` VALUES (417, 264, '合同中虽有多个我方主体公司，但属于同一方，故不是关联交易。', '合同中虽有多个我方主体公司，但属于同一方，故不是关联交易。', '合同中虽有多个我方主体公司，但属于同一方，故不是关联交易。', 'INFO', 1, 1, '2025-08-14 11:47:24', '2025-08-14 11:47:24');
INSERT INTO `review_prompt` VALUES (418, 264, '合同中含有多个我方主体公司，存在关联交易风险。', '内部交易风险', '合同中含有多个我方主体公司，存在关联交易风险。', 'INFO', 1, 1, '2025-08-14 11:53:26', '2025-08-14 11:53:26');
INSERT INTO `review_prompt` VALUES (419, 246, '建议约定明确的收付款时间，具体到“日”为颗粒度，如“x年x月x日”，“x月x日”，或满足特定条件后的“x工作日/自然日\"内。', '收付款时间缺失', '建议约定明确的收付款时间，具体到“日”为颗粒度，如“x年x月x日”，“x月x日”，或满足特定条件后的“x工作日/自然日\"内。', 'INFO', 1, 1, '2025-08-14 11:58:49', '2025-08-14 11:58:49');
INSERT INTO `review_prompt` VALUES (420, 40, '识别到我方损失赔偿范围过宽，承担间接损失、全部损失的赔偿责任；建议与法务明确损失赔偿范围，确认赔偿范围是否合理。', '损失赔偿范围过宽', '识别到我方损失赔偿范围过宽，承担间接损失、全部损失的赔偿责任；建议与法务明确损失赔偿范围，确认赔偿范围是否合理。', 'INFO', 1, 1, '2025-08-14 12:03:56', '2025-08-14 12:03:56');
INSERT INTO `review_prompt` VALUES (422, 261, '识别到定金比例高于合同总金额的 20%，超过部分不产生定金效力，建议结合实际业务需求，重新确定合理的定金比例', '定金金额不合理', '识别到定金比例高于合同总金额的 20%，超过部分不产生定金效力，建议结合实际业务需求，重新确定合理的定金比例', 'INFO', 1, 1, '2025-08-14 12:05:49', '2025-08-14 12:05:49');
INSERT INTO `review_prompt` VALUES (423, 265, '未识别到质保金金额', '质保金额缺失', '未识别到质保金金额，为保证买方所购产品质量，建议补充合理的质保金金额。', 'INFO', 1, 1, '2025-08-14 12:09:15', '2025-08-14 12:09:15');
INSERT INTO `review_prompt` VALUES (424, 265, '质保金返还的相关约定缺失或表述模糊。', '质保金返还约定缺失', '质保金返还的相关约定缺失或表述模糊。为保证卖方及时收回质保金，建议补充质保金返还条款。', 'INFO', 1, 1, '2025-08-14 12:10:18', '2025-08-14 12:10:18');
INSERT INTO `review_prompt` VALUES (425, 52, '对方有权单方解除', '对方有权单方解除', '识别到对方有权单方解除合同/协议/订单，建议与法务确认相关条款，以保证合同双方解除权对等。', 'INFO', 1, 1, '2025-08-14 12:12:17', '2025-08-14 12:12:17');
INSERT INTO `review_prompt` VALUES (426, 50, '识别到我方在未收到发票前需先行支付价款。', '先款后票', '识别到我方在未收到发票前需先行支付价款。为避免风险，请尽量要求对方先开具发票，我方再付款', 'INFO', 1, 1, '2025-08-14 12:15:01', '2025-08-14 12:15:01');

-- ----------------------------
-- Table structure for review_prompt_example
-- ----------------------------
DROP TABLE IF EXISTS `review_prompt_example`;
CREATE TABLE `review_prompt_example`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_version_id` bigint NOT NULL,
  `user_example` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `assistant_example` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_version`(`prompt_version_id`) USING BTREE,
  INDEX `idx_sort`(`prompt_version_id`, `sort_order`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_prompt_example
-- ----------------------------

-- ----------------------------
-- Table structure for review_prompt_version
-- ----------------------------
DROP TABLE IF EXISTS `review_prompt_version`;
CREATE TABLE `review_prompt_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_id` bigint NOT NULL,
  `version_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `is_published` tinyint(1) NOT NULL DEFAULT 0,
  `content_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prompt_version`(`prompt_id`, `version_code`) USING BTREE,
  INDEX `idx_prompt`(`prompt_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_prompt_version
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
