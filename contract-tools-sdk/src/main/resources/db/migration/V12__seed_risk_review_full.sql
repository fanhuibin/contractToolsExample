-- Full seed for contract risk review library (98 prompts, re-authored content)
-- This migration is idempotent. It does not drop tables and only upserts data for known points.

-- 0) Safety: ensure clause types exist (idempotent)
INSERT INTO review_clause_type (clause_code, clause_name, sort_order, enabled) VALUES
('主体','合同主体',10,1),
('法律引用','法律引用',20,1),
('财务条款','财务条款',30,1),
('履行','履行',40,1),
('验收','验收',50,1),
('知识产权','知识产权',60,1),
('违约责任','违约责任',70,1),
('保密','保密',80,1),
('不可抗力','不可抗力',90,1),
('争议解决','争议解决',100,1),
('合同解除','合同解除',110,1),
('合同形式与生效','合同形式与生效',120,1),
('其他','其他',130,1)
ON DUPLICATE KEY UPDATE clause_name=VALUES(clause_name), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- 1) Ensure points exist (idempotent)
-- 合同主体
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3649','己方主体名称规范性审查','己方主体名称规范性审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='合同主体'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3702','对方主体名称规范与风险审查','对方主体名称规范与风险审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='合同主体'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- 法律引用
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'605','法律引用有误风险提示','法律引用有误风险提示',1,1 FROM review_clause_type ct WHERE ct.clause_name='法律引用'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 财务条款（一至九）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'517','价款大小写一致性审查','价款大小写一致性审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3521','价款含税约定审查','价款含税约定审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3549','增值税税率审查','增值税税率审查',3,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'375','价款构成范围审查','价款构成范围审查',4,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'704','价款支付时间审查','价款支付时间审查',5,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3524','价款支付途径审查','价款支付途径审查',6,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'378','先款后票确认性审查','先款后票确认性审查',7,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3523','发票类型审查','发票类型审查',8,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3445','税收调整政策审查','税收调整政策审查',9,1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 履行
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'578','履行时间审查','履行时间审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='履行'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'689','履行地点审查','履行地点审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='履行'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 验收
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'369','验收与质量标准审查','验收与质量标准审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3388','验收时间审查','验收时间审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'370','验收不通过的处理措施审查','验收不通过的处理措施审查',3,1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 知识产权
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3423','知识产权归属风险审查','知识产权归属风险审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='知识产权'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'674','知产无瑕疵保证与侵权责任审查','知产无瑕疵保证与侵权责任审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='知识产权'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 保密
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'652','保密条款审查','保密条款审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='保密'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 违约责任
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'651','迟延履行违约责任审查','迟延履行违约责任审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'356','迟延支付违约责任审查','迟延支付违约责任审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'478','违约金金额合理性审查','违约金金额合理性审查',3,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'567','赔偿限额风险审查','赔偿限额风险审查',4,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'374','损失赔偿范围审查','损失赔偿范围审查',5,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3430','直接损失赔偿审查','直接损失赔偿审查',6,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3446','连带责任风险审查','连带责任风险审查',7,1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 不可抗力
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'653','不可抗力条款审查','不可抗力条款审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='不可抗力'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 争议解决
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3566','或诉或裁风险审查','或诉或裁风险审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3578','诉讼管辖法院审查','诉讼管辖法院审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3570','仲裁机构审查','仲裁机构审查',3,1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 合同解除
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3442','无理由通知单方解除合同风险审查','无理由通知单方解除合同风险审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='合同解除'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 合同形式与生效
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3568','合同生效条件缺失审查','合同生效条件缺失审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3520','合同编号缺失审查','合同编号缺失审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3518','签订日期审查','签订日期审查',3,1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3424','签订地点审查','签订地点审查',4,1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);

-- 注意：避免不同连接字符集/排序规则导致的错误，改用数值 IN 比较
DELETE a FROM review_action a
JOIN review_prompt pr ON a.prompt_id = pr.id
JOIN review_point pt ON pr.point_id = pt.id
WHERE CAST(pt.point_code AS UNSIGNED) IN (3649,3702,605,517,3521,3549,375,704,3524,378,3523,3445,578,689,369,3388,370,3423,674,652,651,356,478,567,374,3430,3446,653,3566,3578,3570,3442,3568,3520,3518,3424,3437,3422);
DELETE pr FROM review_prompt pr
JOIN review_point pt ON pr.point_id = pt.id
WHERE CAST(pt.point_code AS UNSIGNED) IN (3649,3702,605,517,3521,3549,375,704,3524,378,3523,3445,578,689,369,3388,370,3423,674,652,651,356,478,567,374,3430,3446,653,3566,3578,3570,3442,3568,3520,3518,3424,3437,3422);

-- 2.2) Insert prompts (98 total)

-- 不可抗力 653 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled)
SELECT p.id,'不可抗力条款确认','不可抗力条款确认','已识别不可抗力条款，请确认情形列举、影响范围与恢复义务明确。','INFO',1,1 FROM review_point p WHERE p.point_code='653';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled)
SELECT p.id,'不可抗力条款缺失','不可抗力条款缺失','未见不可抗力安排，建议补充情形示例、通知与证明义务及后续处理。','WARNING',2,1 FROM review_point p WHERE p.point_code='653';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled)
SELECT p.id,'不可抗力通知与证明义务不明','不可抗力通知与证明义务不明','触发后的通知时限与证明材料未明确，建议补足流程性约定。','WARNING',3,1 FROM review_point p WHERE p.point_code='653';

INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'或诉或裁风险','或诉或裁风险','同时允许诉讼或仲裁可能致仲裁协议无效，建议仅选择一种方式并明确。','ERROR',1,1 FROM review_point p WHERE p.point_code='3566';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'选择式文本未进行选择','选择式文本未进行选择','争议解决以选择题呈现但未勾选，建议明确仅选诉讼或仲裁之一。','ERROR',2,1 FROM review_point p WHERE p.point_code='3566';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'争议解决条款内部矛盾','争议解决条款内部矛盾','同一合同内存在冲突路径，建议统一为单一争议解决方式。','ERROR',3,1 FROM review_point p WHERE p.point_code='3566';

INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'争议解决条款缺失','争议解决条款缺失','未识别到争议解决方式，建议补充并避免与仲裁条款冲突。','ERROR',1,1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'诉讼管辖法院确认','诉讼管辖法院确认','已约定诉讼方式，请核对管辖法院是否对己方有利且合法有效。','INFO',2,1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'诉讼管辖法院缺失','诉讼管辖法院缺失','选择诉讼但未指定管辖，建议补充并与专属管辖情形避冲突。','WARNING',3,1 FROM review_point p WHERE p.point_code='3578';

INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'仲裁机构确认','仲裁机构确认','已识别仲裁机构，请确认名称准确且唯一，避免无效风险。','ERROR',1,1 FROM review_point p WHERE p.point_code='3570';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'仲裁机构缺失','仲裁机构缺失','未指定仲裁机构，建议补充唯一机构并核对最新全称。','ERROR',2,1 FROM review_point p WHERE p.point_code='3570';

INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'首部己方主体名称缺失','首部己方主体名称缺失','未在合同首部识别到己方主体全称，建议补充营业执照一致的标准名称。','ERROR',1,1 FROM review_point p WHERE p.point_code='3649';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'尾部己方主体名称不规范','尾部己方主体名称不规范','尾部名称与登记信息不一致或缺少组织标识，建议统一至工商登记全称。','ERROR',2,1 FROM review_point p WHERE p.point_code='3649';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'己方主体名称不一致','己方主体名称不一致','首尾出现不同版本的己方名称，需核对并保持一致，以免影响效力与履行。','ERROR',3,1 FROM review_point p WHERE p.point_code='3649';

INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'对方主体名称缺失','对方主体名称缺失','未识别到对方主体标准全称，建议补充并与登记信息核验一致。','ERROR',1,1 FROM review_point p WHERE p.point_code='3702';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'对方主体名称不一致','对方主体名称不一致','首尾或条款内对方名称存在差异，建议统一到工商登记全称。','ERROR',2,1 FROM review_point p WHERE p.point_code='3702';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'对方主体名称规范性确认','对方主体名称规范性确认','已识别到对方主体名称，请确认与最新登记信息一致且无简写。','WARNING',3,1 FROM review_point p WHERE p.point_code='3702';

-- 法律引用 605 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'法律全称引用核验','法律全称引用核验','发现对失效法条的全称引用，建议统一为现行有效规范性文件的标准全称。','ERROR',1,1 FROM review_point p WHERE p.point_code='605';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'法律简称引用核验','法律简称引用核验','出现简写或旧简称，可能导致歧义，建议采用现行法统一简称。','ERROR',2,1 FROM review_point p WHERE p.point_code='605';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'旧法条仍在引用','旧法条仍在引用','文本仍引用已被新法替代的内容，建议更新为对应现行条款。','WARNING',3,1 FROM review_point p WHERE p.point_code='605';

-- 财务条款 517 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'大小写金额不一致','大小写金额不一致','同一金额的中文大写与数字小写不一致，建议以书面约定的优先规则修正。','ERROR',1,1 FROM review_point p WHERE p.point_code='517';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'大小写金额一致性确认','大小写金额一致性确认','已识别金额表述，请复核大写与小写一致性，避免结算争议。','INFO',2,1 FROM review_point p WHERE p.point_code='517';

-- 财务条款 3521 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'含税价确认','含税价确认','文本出现含税价表述，请确认税费口径、税率变化时的结算原则。','WARNING',1,1 FROM review_point p WHERE p.point_code='3521';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'是否含税未约定','是否含税未约定','未明确价款是否含税，建议同时列示含税价与不含税价并注明口径。','WARNING',2,1 FROM review_point p WHERE p.point_code='3521';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'含税与不含税表述不一致','含税与不含税表述不一致','不同条款对含税口径描述冲突，建议统一并明确税负承担规则。','ERROR',3,1 FROM review_point p WHERE p.point_code='3521';

-- 财务条款 3549 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'税率确认','税率确认','已识别增值税税率，请核对是否匹配纳税人身份及当前政策。','INFO',1,1 FROM review_point p WHERE p.point_code='3549';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'税率缺失','税率缺失','未明确适用税率，建议补充并注明随政策调整的处理方式。','WARNING',2,1 FROM review_point p WHERE p.point_code='3549';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'税率与纳税人身份不匹配','税率与纳税人身份不匹配','税率与纳税人类型或业务实情不符，建议调整为合规口径。','ERROR',3,1 FROM review_point p WHERE p.point_code='3549';

-- 财务条款 375 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'价款构成范围确认','价款构成范围确认','已列示价款包含的费用，请确认是否覆盖运输、安装、服务等必要成本。','INFO',1,1 FROM review_point p WHERE p.point_code='375';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'价款构成范围缺失','价款构成范围缺失','未明确价款包含范围，建议结合业务列明税费与各项支出归属。','WARNING',2,1 FROM review_point p WHERE p.point_code='375';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'价款构成范围表述不清','价款构成范围表述不清','费用口径与边界描述模糊，建议约定“不含/包含”清单与示例。','WARNING',3,1 FROM review_point p WHERE p.point_code='375';

-- 财务条款 704 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'支付时间确认','支付时间确认','已识别支付节点，请确认期限、条件与逾期处理一致且可执行。','INFO',1,1 FROM review_point p WHERE p.point_code='704';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'支付时间缺失','支付时间缺失','未约定支付时间或节点，建议明确具体日期或计算规则。','ERROR',2,1 FROM review_point p WHERE p.point_code='704';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'支付期限不明确','支付期限不明确','存在“及时/尽快”等模糊用语，建议换成可衡量的期限表述。','WARNING',3,1 FROM review_point p WHERE p.point_code='704';

-- 财务条款 3524 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'支付途径确认','支付途径确认','已识别支付方式与账户，请确认开户名、账号、银行等要素准确。','INFO',1,1 FROM review_point p WHERE p.point_code='3524';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'支付途径缺失','支付途径缺失','未明确转账/票据/现金等支付方式，建议补充并约定变更流程。','WARNING',2,1 FROM review_point p WHERE p.point_code='3524';

-- 财务条款 378 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'先款后票确认','先款后票确认','约定先付款后开票，建议评估现金流与抵扣影响并设置保障措施。','WARNING',1,1 FROM review_point p WHERE p.point_code='378';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'先票后款确认','先票后款确认','若改为先票后款，需同步调整结算与验收流程，确保凭证闭环。','WARNING',2,1 FROM review_point p WHERE p.point_code='378';

-- 财务条款 3523 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'专用发票确认','专用发票确认','识别到专票约定，请确认开票主体资质与抵扣需求匹配。','WARNING',1,1 FROM review_point p WHERE p.point_code='3523';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'普通发票确认','普通发票确认','识别到普票约定，如需抵扣请评估是否需调整为专票。','WARNING',2,1 FROM review_point p WHERE p.point_code='3523';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'发票类型缺失','发票类型缺失','未明确发票类型，建议明确为专票或普票并注明开具条件。','WARNING',3,1 FROM review_point p WHERE p.point_code='3523';

-- 财务条款 3445 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'存在税收调整政策','存在税收调整政策','文本包含税率调整条款，建议核对调价机制及税负分担。','WARNING',1,1 FROM review_point p WHERE p.point_code='3445';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'税收调整政策缺失','税收调整政策缺失','未约定税率变化的处理原则，建议补充“价税分离/不变含税总价”等方案。','ERROR',2,1 FROM review_point p WHERE p.point_code='3445';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'税率调整责任约定不清','税率调整责任约定不清','调整触发条件与结算口径不清晰，建议明确基准日与生效逻辑。','WARNING',3,1 FROM review_point p WHERE p.point_code='3445';

-- 履行 578 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'履行时间确认','履行时间确认','已识别履行时间，请确认与工期/服务周期一致并可落地执行。','INFO',1,1 FROM review_point p WHERE p.point_code='578';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'履行时间缺失','履行时间缺失','未约定履行完成或交付的具体时间，建议明确日期或计算口径。','WARNING',2,1 FROM review_point p WHERE p.point_code='578';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'履行时间不完整','履行时间不完整','仅出现阶段性描述，缺少起止/验收节点，建议补全关键里程碑。','WARNING',3,1 FROM review_point p WHERE p.point_code='578';

-- 履行 689 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'履行地点确认','履行地点确认','已识别履行/交付地点，请确认与物流、施工或服务安排一致。','WARNING',1,1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'履行地点缺失','履行地点缺失','未明确履行地点，建议补充并约定变更流程与费用承担。','ERROR',2,1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'交付或服务地点不一致','交付或服务地点不一致','不同条款对交付地与服务地描述不一致，建议统一口径。','WARNING',3,1 FROM review_point p WHERE p.point_code='689';

-- 验收 369 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收与质量标准确认','验收与质量标准确认','已识别验收/质量标准，请确认与行业标准或技术协议一致。','INFO',1,1 FROM review_point p WHERE p.point_code='369';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收与质量标准缺失','验收与质量标准缺失','未约定验收口径或质量标准，建议引用国家/行业或双方约定的更高标准。','WARNING',2,1 FROM review_point p WHERE p.point_code='369';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收标准表述不清','验收标准表述不清','存在“合格/满足需求”等模糊表述，建议转为可检验的明确条款。','WARNING',3,1 FROM review_point p WHERE p.point_code='369';

-- 验收 3388 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收时间条款','验收时间条款','已识别验收时限，请确认与交付计划匹配并预留整改周期。','WARNING',1,1 FROM review_point p WHERE p.point_code='3388';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'未约定验收时间','未约定验收时间','未见验收期限或触发条件，建议加入具体日期或触发事件。','WARNING',2,1 FROM review_point p WHERE p.point_code='3388';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收时间与履行安排冲突','验收时间与履行安排冲突','验收节点早于交付完成或与工期冲突，建议统一计划。','WARNING',3,1 FROM review_point p WHERE p.point_code='3388';

-- 验收 370 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收不通过的处理措施确认','验收不通过的处理措施确认','已设置不通过后的整改/重验措施，请确认时限与费用承担明确。','INFO',1,1 FROM review_point p WHERE p.point_code='370';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'验收不通过的处理措施缺失','验收不通过的处理措施缺失','未约定不合格处理流程，建议补充整改、重验与解除等路径。','WARNING',2,1 FROM review_point p WHERE p.point_code='370';

-- 知识产权 3423 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'知识产权归属确认','知识产权归属确认','已识别成果归属安排，请确认权利范围、地域、期限及费用口径。','WARNING',1,1 FROM review_point p WHERE p.point_code='3423';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'知识产权归属表述不清','知识产权归属表述不清','归属、许可与使用权描述模糊，建议以清单化方式明确。','WARNING',2,1 FROM review_point p WHERE p.point_code='3423';

-- 知识产权 674 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'知产无瑕疵保证确认','知产无瑕疵保证确认','已约定不存在侵犯第三方权利的保证，请确认补救措施完备。','INFO',1,1 FROM review_point p WHERE p.point_code='674';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'知产无瑕疵保证缺失','知产无瑕疵保证缺失','未约定不侵权保证与违约处理，建议补充替换、许可或退费等方案。','ERROR',2,1 FROM review_point p WHERE p.point_code='674';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'侵权责任未约定','侵权责任未约定','未明确被主张侵权时的责任承担与协助义务，建议完善条款。','WARNING',3,1 FROM review_point p WHERE p.point_code='674';

-- 保密 652 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'保密条款确认','保密条款确认','已识别保密条款，请核对保密信息范围、义务主体、期限与例外。','WARNING',1,1 FROM review_point p WHERE p.point_code='652';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'保密条款缺失','保密条款缺失','未见保密约定，建议补充范围、期限、例外及违约责任。','ERROR',2,1 FROM review_point p WHERE p.point_code='652';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'保密期限或范围不明','保密期限或范围不明','仅泛化描述“商业秘密”，建议补充可识别的范围与期限。','WARNING',3,1 FROM review_point p WHERE p.point_code='652';

-- 违约责任 651 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'迟延履行违约责任确认','迟延履行违约责任确认','已设置逾期责任，请核对计算口径、上限与继续履行安排。','WARNING',1,1 FROM review_point p WHERE p.point_code='651';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'迟延履行违约责任缺失','迟延履行违约责任缺失','未约定逾期履行的处理方式，建议补充违约金与补救措施。','ERROR',2,1 FROM review_point p WHERE p.point_code='651';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'迟延违约金标准不合理','迟延违约金标准不合理','违约金比例明显偏高或缺乏上限，建议结合业务下调并设封顶。','ERROR',3,1 FROM review_point p WHERE p.point_code='651';

-- 违约责任 356 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'迟延支付违约责任确认','迟延支付违约责任确认','已设置逾期付款的违约条款，请确认比例、宽限与解除权一致。','ERROR',1,1 FROM review_point p WHERE p.point_code='356';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'迟延支付违约责任缺失','迟延支付违约责任缺失','未明确逾期付款的处理，建议补充违约金、停供与解除等路径。','ERROR',2,1 FROM review_point p WHERE p.point_code='356';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'逾期后处理措施缺失','逾期后处理措施缺失','缺少逾期后的通知、催告与分期整改安排，建议完善流程。','WARNING',3,1 FROM review_point p WHERE p.point_code='356';

-- 违约责任 478 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'违约金标准确认','违约金标准确认','已识别违约金比例，请评估与交易风险匹配并避免显失公平。','ERROR',1,1 FROM review_point p WHERE p.point_code='478';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'违约金金额未填写完整','违约金金额未填写完整','计算基数或比例缺失，建议补充清晰的计算方式与封顶规则。','ERROR',2,1 FROM review_point p WHERE p.point_code='478';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'违约金标准过高','违约金标准过高','违约金设置偏高，建议按实际风险与行业惯例调整为合理区间。','ERROR',3,1 FROM review_point p WHERE p.point_code='478';

-- 违约责任 567 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'赔偿限额确认','赔偿限额确认','存在赔偿上限条款，请评估是否覆盖可预见损失及例外情形。','ERROR',1,1 FROM review_point p WHERE p.point_code='567';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'赔偿限额未约定','赔偿限额未约定','未设置赔偿上限，建议结合业务规模与保险安排确定限额。','WARNING',2,1 FROM review_point p WHERE p.point_code='567';

-- 违约责任 374 (3)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'损失赔偿包含间接损失','损失赔偿包含间接损失','条款包含“全部损失/预期利益”等表述，建议评估范围是否过宽。','WARNING',1,1 FROM review_point p WHERE p.point_code='374';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'损失赔偿未明确包含间接损失','损失赔偿未明确包含间接损失','仅出现“直接损失/实际损失”等表述，如为相对方请确认是否足够。','WARNING',2,1 FROM review_point p WHERE p.point_code='374';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'律师费/差旅费承担不明','律师费/差旅费承担不明','费用承担未细化，建议明确举证标准、范围与计取方式。','WARNING',3,1 FROM review_point p WHERE p.point_code='374';

-- 违约责任 3430 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'直接损失赔偿确认','直接损失赔偿确认','仅对直接损失赔偿的表述已识别，建议结合业务评估可接受性。','WARNING',1,1 FROM review_point p WHERE p.point_code='3430';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'仅赔直接损失的风险','仅赔直接损失的风险','如排除了间接损失，可能无法覆盖必要费用，建议审慎评估。','WARNING',2,1 FROM review_point p WHERE p.point_code='3430';

-- 违约责任 3446 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'连带责任风险','连带责任风险','存在连带责任约定，未限定范围或顺序可能加重责任负担。','ERROR',1,1 FROM review_point p WHERE p.point_code='3446';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'连带责任范围不清','连带责任范围不清','未明确连带范围、分担与追偿机制，建议细化以降低争议。','WARNING',2,1 FROM review_point p WHERE p.point_code='3446';

-- 法律引用已在上方 (3)

-- 合同解除 3442 (2)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'无理由通知解除权确认','无理由通知解除权确认','存在单方通知解除条款，建议评估冷静期、补偿与未履行费用分配。','ERROR',1,1 FROM review_point p WHERE p.point_code='3442';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'解除流程与期限未约定','解除流程与期限未约定','解除条件、通知路径与生效时间未细化，建议补充流程规范。','WARNING',2,1 FROM review_point p WHERE p.point_code='3442';

-- 合同形式与生效 3568/3520/3518/3424 (8)
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'合同生效条件缺失','合同生效条件缺失','未明确生效触发点，建议约定签署/盖章/审批或特定日期生效。','WARNING',1,1 FROM review_point p WHERE p.point_code='3568';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'生效条件表述不清','生效条件表述不清','生效条件存在多义或冲突，建议统一并避免前后矛盾。','WARNING',2,1 FROM review_point p WHERE p.point_code='3568';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'合同编号缺失','合同编号缺失','未设置合同编号，建议补充并与内控系统一致便于归档检索。','INFO',1,1 FROM review_point p WHERE p.point_code='3520';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'合同编号未填写完整','合同编号未填写完整','编号位数或规则不完整，建议按公司编号体系补齐。','INFO',2,1 FROM review_point p WHERE p.point_code='3520';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'签订日期确认','签订日期确认','已识别签订日期，请确认不早于实际签署且与审批流一致。','WARNING',1,1 FROM review_point p WHERE p.point_code='3518';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'签订日期缺失','签订日期缺失','未见签订日期，建议补充明确并避免倒签风险。','WARNING',2,1 FROM review_point p WHERE p.point_code='3518';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'签订地点缺失','签订地点缺失','未约定签订地点，建议补充并与印章/签署主体所在地匹配。','WARNING',1,1 FROM review_point p WHERE p.point_code='3424';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'签订地点确认','签订地点确认','已识别签订地点，请确认与双方签署安排相符。','WARNING',2,1 FROM review_point p WHERE p.point_code='3424';

-- 其他 3437/3422 (4)
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3437','主合同和（附件或补充协议）优先级确认审查','主合同和（附件或补充协议）优先级确认审查',1,1 FROM review_clause_type ct WHERE ct.clause_name='其他'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'主合同与附件优先级确认','主合同与附件优先级确认','存在主合同与附件并行的情形，建议明确冲突时的适用顺序。','WARNING',1,1 FROM review_point p WHERE p.point_code='3437';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'主合同与附件优先级未约定','主合同与附件优先级未约定','未约定优先级，建议补充“以本合同/以补充协议为准”等表述。','WARNING',2,1 FROM review_point p WHERE p.point_code='3437';
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id,'3422','合同份数审查','合同份数审查',2,1 FROM review_clause_type ct WHERE ct.clause_name='其他'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'未约定合同份数','未约定合同份数','未明确合同份数与流向，建议补充并与归档制度一致。','WARNING',1,1 FROM review_point p WHERE p.point_code='3422';
INSERT INTO review_prompt (point_id,prompt_key,name,message,status_type,sort_order,enabled) SELECT p.id,'合同份数确认','合同份数确认','已识别份数约定，请确认各方留存数量一致且具同等效力。','INFO',2,1 FROM review_point p WHERE p.point_code='3422';

-- 3) Default profile and order list (full sequence)
INSERT INTO review_profile (profile_code, profile_name, is_default, description)
VALUES ('default','默认审核清单',1,'由V12全量种子生成，顺序参考提供的orderList')
ON DUPLICATE KEY UPDATE profile_name=VALUES(profile_name), is_default=VALUES(is_default), description=VALUES(description);

-- 合同主体
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 1 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3649'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 2 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3702'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 法律引用
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 10 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='法律引用' AND pt.point_code='605'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 财务条款（按顺序）
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 20 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='517'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 21 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3521' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 22 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3549' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 23 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='375'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 24 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='704'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 25 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3524' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 26 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='378'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 27 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3523' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 28 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3445' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 履行
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 30 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='履行' AND pt.point_code='578' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 31 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='履行' AND pt.point_code='689' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 验收
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 40 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='369'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 41 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='3388' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 42 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='370'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 知识产权
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 50 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='知识产权' AND pt.point_code='3423' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 51 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='知识产权' AND pt.point_code='674'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 保密
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 60 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='保密' AND pt.point_code='652'  ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 违约责任
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 70 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='651' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 71 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='356' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 72 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='478' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 73 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='567' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 74 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='374' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 75 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='3430' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 76 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='3446' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 不可抗力
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 80 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='不可抗力' AND pt.point_code='653' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 争议解决
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 90 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3566' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 91 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3578' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 92 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3570' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 合同解除
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 100 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同解除' AND pt.point_code='3442' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 合同形式与生效
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 110 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3568' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 111 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3520' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 112 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3518' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 113 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3424' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 其他
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 120 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='其他' AND pt.point_code='3437' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order) SELECT pf.id, ct.id, pt.id, 121 FROM review_profile pf, review_clause_type ct, review_point pt WHERE pf.profile_code='default' AND ct.clause_name='其他' AND pt.point_code='3422' ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);


