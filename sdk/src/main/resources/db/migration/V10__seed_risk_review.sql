-- Seed initial risk review library based on provided JSON (partial illustrative; extend as needed)

-- Clause types
INSERT INTO review_clause_type (clause_code, clause_name, sort_order, enabled)
VALUES 
('主体', '合同主体', 10, 1),
('法律引用', '法律引用', 20, 1),
('财务条款', '财务条款', 30, 1),
('履行', '履行', 40, 1),
('验收', '验收', 50, 1),
('知识产权', '知识产权', 60, 1),
('违约责任', '违约责任', 70, 1),
('保密', '保密', 80, 1),
('不可抗力', '不可抗力', 90, 1),
('争议解决', '争议解决', 100, 1),
('合同解除', '合同解除', 110, 1),
('合同形式与生效', '合同形式与生效', 120, 1),
('其他', '其他', 130, 1)
ON DUPLICATE KEY UPDATE clause_name=VALUES(clause_name), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- Example points (争议解决-3566 或诉或裁)
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3566', '或诉或裁风险审查', '或诉或裁风险审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- Prompts for 3566
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '或诉或裁风险', '或诉或裁风险', '识别到合同的争议解决方式为或诉或裁。根据法律规定约定或诉或裁的，仲裁协议无效。为避免仲裁协议无效，建议明确争议解决方式，明确选择诉讼或仲裁其中一种途径作为争议解决方式。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3566'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '选择式文本未进行选择', '选择式文本未进行选择', '识别到合同以文本选择式约定了诉讼和仲裁等争议解决方式，但未进行选择，为避免仲裁协议无效，建议根据己方情况选择其中一项作为争议解决方式。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='3566'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- Example points (争议解决-3570 仲裁机构)
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3570', '仲裁机构审查', '仲裁机构审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '仲裁机构确认', '仲裁机构确认', '识别到仲裁机构……请确认仲裁机构是否填写准确，并符合己方要求。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3570'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '仲裁机构缺失', '仲裁机构缺失', '未识别到仲裁机构……为避免仲裁协议无效，建议补充约定唯一仲裁机构。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='3570'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

-- Example points (不可抗力-653)
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '653', '不可抗力条款审查', '不可抗力条款审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='不可抗力'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '不可抗力条款确认', '不可抗力条款确认', '识别到不可抗力条款。请确认当前约定是否符合交易需求……', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='653'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '不可抗力条款缺失', '不可抗力条款缺失', '未识别到不可抗力条款，建议补充明确。*注：……', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='653'
ON DUPLICATE KEY UPDATE name=VALUES(name), message=VALUES(message), status_type=VALUES(status_type), sort_order=VALUES(sort_order), enabled=VALUES(enabled);

INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同所称不可抗力是指本合同签署后发生的、本合同签署时不能预见的、其发生与后果是无法避免并无法克服的、妨碍任何一方全部或部分履约的所有事件，包括但不限于地震、台风、水灾、火灾、政策变化、战争、国际或国内运输中断、流行病、疫情，以及根据中国法律或一般国际商业惯例认作不可抗力的其他事件。\n声称发生不可抗力的一方应迅速书面通知其他各方，并在其后的十五(15)天内提供证明不可抗力发生及其持续时间的足够证据。\n如果发生不可抗力事件，影响一方履行其在本合同项下的义务，其可在不可抗力造成的延误期内中止合同履行，而不视为违约，由此造成的损失由合同各方根据法律规定和公平原则、诚实信用原则各自承担。\n如果发生不可抗力事件，各方应立即互相协商，以找到公平的解决办法，并且应尽一切合理努力将不可抗力的影响减少到最低限度。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='不可抗力条款缺失';

-- Default profile reflecting provided orderList (partial)
INSERT INTO review_profile (profile_code, profile_name, is_default, description)
VALUES ('default', '默认审核清单', 1, '由种子脚本生成，顺序参考提供的orderList')
ON DUPLICATE KEY UPDATE profile_name=VALUES(profile_name), is_default=VALUES(is_default), description=VALUES(description);

-- Insert a few profile items as example; extend in full per orderList
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 1
FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3649'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 2
FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3702'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- Dispute resolution examples
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 50
FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3566'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 51
FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3578'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 52
FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3570'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- ================= Additional points & prompts/actions =================

-- 合同主体 3649 己方主体名称规范性审查
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3649', '己方主体名称规范性审查', '己方主体名称规范性审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='合同主体'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '尾部己方主体为自然人', '尾部己方主体为自然人', '识别到尾部己方主体为自然人。请确认该名称是否准确。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '尾部己方主体名称不规范', '尾部己方主体名称不规范', '识别到尾部己方主体名称不规范。建议与营业执照核对并修改准确。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '尾部己方主体名称缺失', '尾部己方主体名称缺失', '未在尾部识别到己方主体名称。建议将己方主体名称补充完整。', 'ERROR', 3, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '己方主体名称不一致', '己方主体名称不一致', '识别到首部和尾部己方主体名称约定不一致。请核对并修改一致。', 'ERROR', 4, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '首部己方主体为自然人', '首部己方主体为自然人', '识别到首部己方主体为自然人。请确认该名称是否准确。', 'WARNING', 5, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '首部己方主体名称不规范', '首部己方主体名称不规范', '识别到首部己方主体名称不规范。建议与营业执照核对并修改准确。', 'ERROR', 6, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '首部己方主体名称缺失', '首部己方主体名称缺失', '未在首部识别到己方主体名称。建议将己方主体名称补充完整。', 'ERROR', 7, 1 FROM review_point p WHERE p.point_code='3649'
ON DUPLICATE KEY UPDATE message=VALUES(message), status_type=VALUES(status_type);

-- 合同主体 3702 对方主体名称规范与风险审查（仅关键几项，其他可后续补齐）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3702', '对方主体名称规范与风险审查', '对方主体名称规范与风险审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='合同主体'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name), algorithm_type=VALUES(algorithm_type);

INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '对方主体名称不一致', '对方主体名称不一致', '识别到首部和尾部对方主体名称约定不一致。请核对并修改一致。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3702'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '对方存在司法风险', '对方存在司法风险', '查询到该相对方存在法律诉讼或被执行记录。{司法状况2}', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='3702'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '对方存在经营异常', '对方存在经营异常', '查询到该相对方存在行政处罚或经营异常记录。{经营状况2}', 'ERROR', 3, 1 FROM review_point p WHERE p.point_code='3702'
ON DUPLICATE KEY UPDATE message=VALUES(message);

-- 法律引用 605
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '605', '法律引用有误风险提示', '法律引用有误风险提示', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='法律引用'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '法律全称引用有误', '法律全称引用有误', '2021年1月1日民法典生效后，《中华人民共和国民法通则》等已失效，建议调整为《中华人民共和国民法典》。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='605'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'REPLACE', '中华人民共和国民法典', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='法律全称引用有误'
ON DUPLICATE KEY UPDATE action_message=VALUES(action_message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '法律简称引用有误', '法律简称引用有误', '2021年1月1日民法典生效后，《民法通则》《合同法》《担保法》《物权法》《民法总则》等已失效，建议调整为《中华人民共和国民法典》。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='605'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'REPLACE', '中华人民共和国民法典', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='法律简称引用有误'
ON DUPLICATE KEY UPDATE action_message=VALUES(action_message);

-- 保密 652
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '652', '保密条款审查', '保密条款审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='保密'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '保密条款确认', '保密条款确认', '识别到保密条款。请确认当前约定是否符合交易需求，建议关注保密信息范围、保密义务人、期限、违约责任等。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='652'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '保密条款缺失', '保密条款缺失', '未识别到保密条款，建议补充完善。*注：商业合作过程中，建议约定范围/主体/期限/例外/违约责任等。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='652'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同中所指的保密信息是指属于任何一方及其关联企业所有的，并被该方视为秘密的技术、财务、商业或任何其它方面的信息；其不为公众所知悉，能带来经济效益，具有实用性并被采取了保护措施，且仅为执行本合同之目的而使用，应予保密，不得披露。但已经被公众知悉的信息或者经公权力机关依职权需要调阅的信息，不在此列。\n双方及双方的相关人员对合作内容及本合同的具体内容负有保密责任。相关人员包括但不限于合同方及其关联企业的董事、监事、高级管理人员、雇员、咨询者、代理人、顾问。\n保密义务的期限为合同履行的整个期间及合同终止后的____年内；本合同如有任何部分被视为无效或不可执行，均不影响保密条款的有效性。\n未经对方事先书面同意，任何一方不得将双方的合作内容及本合同中涉及的商业秘密披露给任何第三方，否则给对方造成的损失应承担赔偿责任。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='保密条款缺失'
ON DUPLICATE KEY UPDATE action_message=VALUES(action_message);

-- 知识产权 3423/674（节选）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3423', '知识产权归属风险审查', '知识产权归属风险审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='知识产权'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '知识产权归属确认', '知识产权归属确认', '识别到此处有关于知识产权归属的约定，请确认对此归属的约定是否有异议。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3423'
ON DUPLICATE KEY UPDATE message=VALUES(message);

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '674', '知产无瑕疵保证与侵权责任审查', '知产无瑕疵保证与侵权责任审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='知识产权'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '知产侵权责任缺失', '知产侵权责任缺失', '未识别到知识产权的侵权责任约定，建议补充。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='674'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '甲方保证产品及相关文档均不侵犯任何第三方……（略）', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='知产侵权责任缺失';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '知产无瑕疵保证与侵权责任确认', '知产无瑕疵保证与侵权责任确认', '识别到保证知识产权不存在侵权等瑕疵与侵权责任相关约定，建议确认是否符合交易需求。', 'INFO', 2, 1 FROM review_point p WHERE p.point_code='674'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '知产无瑕疵保证与侵权责任缺失', '知产无瑕疵保证与侵权责任缺失', '未识别到保证知识产权无瑕疵、不侵权及其违约责任约定，建议补充。', 'ERROR', 3, 1 FROM review_point p WHERE p.point_code='674'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '乙方向甲方提供的产品及服务……（略）', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='知产无瑕疵保证与侵权责任缺失';

-- 履行 578/689（节选）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '578', '履行时间审查', '履行时间审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='履行'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行时间不明确', '履行时间不明确', '未识别到明确的履行时间……为避免争议，建议补充明确。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='578'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行时间未填写完整', '履行时间未填写完整', '识别到履行时间未填写完整……', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='578'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行时间确认', '履行时间确认', '识别到履行时间相关约定。请确认是否满足交易需求。', 'INFO', 3, 1 FROM review_point p WHERE p.point_code='578'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行时间缺失', '履行时间缺失', '未识别到履行时间相关约定……建议补充明确具体的履行时间。', 'WARNING', 4, 1 FROM review_point p WHERE p.point_code='578'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '交付时间： ___年___月___日。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='履行时间缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '服务时间：___年，自___年___月___日起至___年___月___日止。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='履行时间缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '完工时间：___年___月___日前。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='履行时间缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '689', '履行地点审查', '履行地点审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='履行'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '交付地点未填写完整', '交付地点未填写完整', '识别到交付地点未填写完整，建议根据交易需求填写完整。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='689'
ON DUPLICATE KEY UPDATE message=VALUES(message);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '交付地点确认', '交付地点确认', '识别到交付地点，建议确认当前约定是否符合交易需求。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行地点未填写完整', '履行地点未填写完整', '识别到履行地点未填写完整，建议根据交易需求填写完整。', 'WARNING', 3, 1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行地点确认', '履行地点确认', '识别到履行地点，建议确认当前约定是否符合交易需求。', 'WARNING', 4, 1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '履行地点缺失', '履行地点缺失', '未识别到履行地点。为避免争议，建议根据交易需求明确具体的履行地点。', 'ERROR', 5, 1 FROM review_point p WHERE p.point_code='689';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '交付地点：_________ 。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='履行地点缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '服务地点：_________ 。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='履行地点缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '工程地点：_________ 。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='履行地点缺失';

-- 验收 3388/369/370（节选）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3388', '验收时间审查', '验收时间审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '未约定验收时间', '未约定验收时间', '未识别到验收时间相关约定，为避免争议，建议明确规定验收的具体日期或条件。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3388';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '甲方应在验收流程启动后【 】日内完成验收，并出具验收报告。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定验收时间';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '甲方应在收到验收邀请后【 】个工作日内按照双方确认的验收标准进行验收。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定验收时间';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '甲方应在收到乙方交付的产品/服务后的【 】天内进行验收。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定验收时间';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '369', '验收与质量标准审查', '验收与质量标准审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '验收与质量标准缺失', '验收与质量标准缺失', '未识别到验收或质量标准……为维护权益，建议明确验收/质量标准。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='369';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '质量标准：必须达到最新国家和行业强制性质量标准……', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='验收与质量标准缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '质量标准：质量标准必须达到最新国家和行业……（长文略）', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='验收与质量标准缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '验收标准：应当按照国家/地方/行业标准及甲方要求的标准进行验收。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='验收与质量标准缺失';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '验收或质量标准确认', '验收或质量标准确认', '识别到验收或质量标准，请确认是否满足实际交易需求。', 'INFO', 2, 1 FROM review_point p WHERE p.point_code='369';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '370', '验收不通过的处理措施审查', '验收不通过的处理措施审查', 3, 1 FROM review_clause_type ct WHERE ct.clause_name='验收'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '验收不通过的处理措施确认', '验收不通过的处理措施确认', '识别到验收不通过的处理措施。请确认处理措施是否符合己方需求。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='370';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '验收不通过的处理措施缺失', '验收不通过的处理措施缺失', '未识别到验收不通过的处理措施，建议补充完善。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='370';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '若验收未通过，乙方应于___日内整改，并再次申请验收；仍不通过，甲方有权解除合同并要求违约责任。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='验收不通过的处理措施缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '第二次验收仍未通过，甲方有权解除合同，乙方承担违约责任。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='验收不通过的处理措施缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '货物验收不合格，买方有权拒收并提出书面异议，卖方负责退换货并承担相关费用。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='验收不通过的处理措施缺失';

-- 合同形式与生效 3424/3518/3520/3568（节选）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3424', '签订地点审查', '签订地点审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '签订地点缺失', '签订地点缺失', '未约定合同签订地点。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3424';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '签订地点：', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='签订地点缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3518', '签订日期审查', '签订日期审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '签订日期确认', '签订日期确认', '识别到合同签订日期已填写，建议按照实际签订日期进行填写。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3518';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '签订日期缺失', '签订日期缺失', '未识别到合同签订日期。建议补充明确的合同签订日期。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='3518';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同于__年__月__日在__签订。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='签订日期缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '本合同签订日期：__年__月__日。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='签订日期缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3520', '合同编号缺失审查', '合同编号缺失审查', 3, 1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '合同编号未填写完整', '合同编号未填写完整', '识别到合同编号未填写完整。建议将合同编号填写完整。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='3520';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '合同编号缺失', '合同编号缺失', '未识别到合同编号。建议补充明确的合同编号。', 'INFO', 2, 1 FROM review_point p WHERE p.point_code='3520';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3568', '合同生效条件缺失审查', '合同生效条件缺失审查', 4, 1 FROM review_clause_type ct WHERE ct.clause_name='合同形式与生效'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '合同生效条件缺失', '合同生效条件缺失', '未识别到合同生效条件约定，建议补充明确。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3568';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同自双方签字盖章之日起生效。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='合同生效条件缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '双方约定，本合同自___年___月___日起生效。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='合同生效条件缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '本合同于双方法定代表人或其授权代表签字并加盖公章之日起生效，至合同规定内容执行完毕后终止。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='合同生效条件缺失';

-- 违约责任（节选） 651/356/478/567/374/3430/3446
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '651', '迟延履行违约责任审查', '迟延履行违约责任审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '迟延履行违约责任确认', '迟延履行违约责任确认', '识别到迟延履行违约责任，建议确认当前约定是否符合交易需求。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='651';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '迟延履行违约责任缺失', '迟延履行违约责任缺失', '未识别到迟延履行违约责任。为避免争议，建议补充。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='651';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '每逾期一日，应按合同总金额___‰承担违约金……', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='迟延履行违约责任缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '分段逾期违约金标准：___日内___‰；___日以上___‰；超过___日___%。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='迟延履行违约责任缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '一方每逾期履行一日，应承担___‰违约金……不足弥补损失的仍应赔偿。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='迟延履行违约责任缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '356', '迟延支付违约责任审查', '迟延支付违约责任审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '迟延支付违约责任确认', '迟延支付违约责任确认', '识别到付款方逾期付款违约责任，请确认是否符合我方利益。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='356';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '迟延支付违约责任缺失', '迟延支付违约责任缺失', '未识别到付款方逾期付款违约责任，建议按需完善。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='356';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '未按时支付货款时，每迟交一天按应付未付款项的_____承担违约金。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='迟延支付违约责任缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '逾期超过 _____仍未支付的，收款方有权解除合同。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='迟延支付违约责任缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '478', '违约金金额合理性审查', '违约金金额合理性审查', 3, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '累计计收违约金过高', '累计计收违约金过高', '识别到累计计收日违约金标准过高，建议结合实际需求降低比例。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='478';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '违约金标准确认', '违约金标准确认', '识别到违约金相关约定，请确认是否超出用户配置的最高限。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='478';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '违约金标准过高', '违约金标准过高', '识别到该项违约金标准过高，建议降低金额比例。', 'ERROR', 3, 1 FROM review_point p WHERE p.point_code='478';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '违约金金额未填写完整', '违约金金额未填写完整', '识别到违约金条款未填写完整，建议将金额或计算方式补充完整。', 'ERROR', 4, 1 FROM review_point p WHERE p.point_code='478';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '567', '赔偿限额风险审查', '赔偿限额风险审查', 4, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '赔偿限额确认', '赔偿限额确认', '识别到合同约定了赔偿限额，请确认是否符合实际利益需求。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='567';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '374', '损失赔偿范围审查', '损失赔偿范围审查', 5, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '损失赔偿包含间接损失', '损失赔偿包含间接损失', '识别到损失赔偿范围包含间接损失等广泛表述，建议谨慎评估。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='374';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '损失赔偿未明确包含间接损失', '损失赔偿未明确包含间接损失', '识别到仅直接损失/实际损失等表述，作为相对方请确认覆盖性。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='374';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3430', '直接损失赔偿审查', '直接损失赔偿审查', 6, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '直接损失赔偿确认', '直接损失赔偿确认', '识别到仅对直接损失赔偿的条款，请确认是否接受。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3430';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3446', '连带责任风险审查', '连带责任风险审查', 7, 1 FROM review_clause_type ct WHERE ct.clause_name='违约责任'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '连带责任风险', '连带责任风险', '识别到连带责任条款，若未明确责任范围/顺序/分摊方式可能引发争议。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3446';

-- 财务条款（节选） 517/3521/3549/375/704/3524/378/3523/3445
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '517', '价款大小写一致性审查', '价款大小写一致性审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '大小写金额不一致', '大小写金额不一致', '该笔款大小写金额不一致，请确认金额是否有误。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='517';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3521', '价款含税约定审查', '价款含税约定审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '价款含税约定缺失', '价款含税约定缺失', '请确认合同价款是否含税，建议同时载明含税价和不含税价。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3521';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3549', '增值税税率审查', '增值税税率审查', 3, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '税率未填写完整', '税率未填写完整', '增值税税率未填写，请结合交易情形与纳税人类型填写符合要求的税率。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='3549';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '税率确认', '税率确认', '识别到税率，请确认是否准确。', 'INFO', 2, 1 FROM review_point p WHERE p.point_code='3549';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '税率缺失', '税率缺失', '未识别到税率约定，建议补充。', 'WARNING', 3, 1 FROM review_point p WHERE p.point_code='3549';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '增值税税率：____%', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='税率缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '375', '价款构成范围审查', '价款构成范围审查', 4, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '价款构成范围确认', '价款构成范围确认', '识别到价款包含的费用/支出范围，请结合情形确认是否接受。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='375';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '价款构成范围缺失', '价款构成范围缺失', '未识别到价款包含范围，建议明确或约定费用承担。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='375';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '【通用】本合同价款包括__方履行本合同义务所需的全部费用……', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '【货物买卖合同】本合同总金额已包含……', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '【不动产租赁合同】以上定价含租金、物业管理费……', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action4', 'COPY', '【设备租赁合同】包含但不限于使用费、折旧费、运输费……', 4, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action5', 'COPY', '【系统（含软硬件）采买合同】采购总价包含……', 5, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action6', 'COPY', '【建设工程合同】工程总价包括……', 6, 1 FROM review_prompt pr WHERE pr.prompt_key='价款构成范围缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '704', '价款支付时间审查', '价款支付时间审查', 5, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '价款支付确认', '价款支付确认', '识别到价款支付的约定，请确认支付时间与期限是否明确且符合需求。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='704';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '价款支付缺失', '价款支付缺失', '未识别到价款如何支付的约定，建议补充支付时间与期限。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='704';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '一次性支付：合同签订后___个工作日内一次性全额支付。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='价款支付缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '分期支付：第一次___%；第二次___%；第三次___%。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='价款支付缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '周期性付款：按___（月/季/半年/年）支付，每次到期前___日付款。', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='价款支付缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3524', '价款支付途径审查', '价款支付途径审查', 6, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '支付途径未填写完整', '支付途径未填写完整', '识别到价款的支付途径未填写完整。', 'INFO', 1, 1 FROM review_point p WHERE p.point_code='3524';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '支付途径确认', '支付途径确认', '识别到价款的支付途径，建议确认是否符合需求。', 'INFO', 2, 1 FROM review_point p WHERE p.point_code='3524';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '支付途径缺失', '支付途径缺失', '未识别到支付途径（银行转账/电汇/票据等），建议补充明确。', 'WARNING', 3, 1 FROM review_point p WHERE p.point_code='3524';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '付款方以_____(①银行汇票②银行汇款③支票④现金等）方式支付。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='支付途径缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '付款方式：现金□ 转账□ 支票□ 其他□', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='支付途径缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '支付途径：_____', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='支付途径缺失';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '378', '先款后票确认性审查', '先款后票确认性审查', 7, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '先款后票确认', '先款后票确认', '识别到付款方需在收到发票前先行支付价款，如为付款方请确认是否接受。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='378';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3523', '发票类型审查', '发票类型审查', 8, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '专用发票确认', '专用发票确认', '识别到发票类型为增值税专用发票……请根据需求与纳税人类型确认。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3523';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '发票类型未选择', '发票类型未选择', '未识别到发票类型（专票或普票），建议明确。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='3523';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '发票类型缺失', '发票类型缺失', '未识别到发票类型约定，建议明确专票或普票。', 'WARNING', 3, 1 FROM review_point p WHERE p.point_code='3523';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '发票类型：_________（增值税专用发票/增值税普通发票）', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='发票类型缺失';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '普通发票确认', '普通发票确认', '识别到发票类型为增值税普通发票……如受票方为一般纳税人，建议确认是否需要专票。', 'WARNING', 4, 1 FROM review_point p WHERE p.point_code='3523';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3445', '税收调整政策审查', '税收调整政策审查', 9, 1 FROM review_clause_type ct WHERE ct.clause_name='财务条款';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '存在税收调整政策', '存在税收调整政策', '合同中税率调整条款可能导致费用分配不明确，建议关注。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3445';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '税收调整政策缺失', '税收调整政策缺失', '未识别到税收调整政策。', 'ERROR', 2, 1 FROM review_point p WHERE p.point_code='3445';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '如税务机关调整税率，甲方有权据此调整应付金额……', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='税收调整政策缺失';

-- 合同解除 3442
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3442', '无理由通知单方解除合同风险审查', '无理由通知单方解除合同风险审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='合同解除';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '无理由通知单方解除合同风险', '无理由通知单方解除合同风险', '识别到无理由的通知解除权，请确认是否接受。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3442';

-- 其他 3422/3437（节选）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3422', '合同份数审查', '合同份数审查', 1, 1 FROM review_clause_type ct WHERE ct.clause_name='其他';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '未约定合同份数', '未约定合同份数', '未约定合同份数。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3422';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同一式【 】份，合同各方各执一份，均具有同等法律效力。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定合同份数';

INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3437', '主合同和（附件或补充协议）优先级确认审查', '主合同和（附件或补充协议）优先级确认审查', 2, 1 FROM review_clause_type ct WHERE ct.clause_name='其他';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '主合同和（附件或补充协议）的优先级确认', '主合同和（附件或补充协议）的优先级确认', '请确认本合同或本条款的优先级是否高于其他合同或条款。', 'WARNING', 1, 1 FROM review_point p WHERE p.point_code='3437';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '未约定主合同和（附件或补充协议）的优先级', '未约定主合同和（附件或补充协议）的优先级', '未约定主合同和（附件或补充协议）的优先级。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='3437';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '本合同与其补充协议冲突时，以补充协议为准。', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定主合同和（附件或补充协议）的优先级';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '本合同与附件冲突时，以本合同为准。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定主合同和（附件或补充协议）的优先级';

-- 争议解决 3578（剩余 prompts/actions）
INSERT INTO review_point (clause_type_id, point_code, point_name, algorithm_type, sort_order, enabled)
SELECT ct.id, '3578', '诉讼管辖法院审查', '诉讼管辖法院审查', 3, 1 FROM review_clause_type ct WHERE ct.clause_name='争议解决'
ON DUPLICATE KEY UPDATE point_name=VALUES(point_name);
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '争议解决条款缺失', '争议解决条款缺失', '未识别到争议解决条款……建议选择诉讼或仲裁其中一种方式，避免或诉或裁。', 'ERROR', 1, 1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '双方同意，因本协议而产生的争议……向____人民法院提起诉讼。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='争议解决条款缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action2', 'COPY', '双方同意……提交____仲裁委员会仲裁……', 2, 1 FROM review_prompt pr WHERE pr.prompt_key='争议解决条款缺失';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action3', 'COPY', '（1）依法向______________ 人民法院起诉。\n（2）提交_________________仲裁委员会仲裁……', 3, 1 FROM review_prompt pr WHERE pr.prompt_key='争议解决条款缺失';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '未约定诉讼解决争议', '未约定诉讼解决争议', '识别到未约定以诉讼方式解决争议，如选择诉讼方式，建议添加并明确管辖法院。', 'WARNING', 2, 1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '……协商不成时任何一方均可向        人民法院提起诉讼。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='未约定诉讼解决争议';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '诉讼管辖法院确认', '诉讼管辖法院确认', '识别到诉讼管辖法院，请确认是否符合己方要求（优先选择己方所在地）。', 'INFO', 3, 1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_prompt (point_id, prompt_key, name, message, status_type, sort_order, enabled)
SELECT p.id, '诉讼管辖法院缺失', '诉讼管辖法院缺失', '识别到约定诉讼但未明确管辖法院，建议补充。', 'WARNING', 4, 1 FROM review_point p WHERE p.point_code='3578';
INSERT INTO review_action (prompt_id, action_id, action_type, action_message, sort_order, enabled)
SELECT pr.id, 'action1', 'COPY', '……协商不成时任何一方均可向        人民法院提起诉讼。', 1, 1 FROM review_prompt pr WHERE pr.prompt_key='诉讼管辖法院缺失';

-- ================= Default profile orderList completion (按给定顺序) =================
-- 注意：以下序号可按需要微调，确保整体相对顺序与提供的orderList一致

-- 合同主体
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 1 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3649'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 2 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同主体' AND pt.point_code='3702'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 法律引用
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 10 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='法律引用' AND pt.point_code='605'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 财务条款
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 20 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='517'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 21 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3521'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 22 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3549'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 23 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='375'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 24 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='704'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 25 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3524'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 26 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='378'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 27 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3523'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 28 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='财务条款' AND pt.point_code='3445'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 履行
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 30 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='履行' AND pt.point_code='578'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 31 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='履行' AND pt.point_code='689'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 验收
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 40 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='369'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 41 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='3388'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 42 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='验收' AND pt.point_code='370'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 知识产权
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 50 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='知识产权' AND pt.point_code='3423'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 51 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='知识产权' AND pt.point_code='674'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 保密
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 60 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='保密' AND pt.point_code='652'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 违约责任
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 70 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='651'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 71 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='356'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 72 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='478'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 73 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='567'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 74 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='374'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 75 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='3430'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 76 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='违约责任' AND pt.point_code='3446'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 不可抗力
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 80 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='不可抗力' AND pt.point_code='653'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 争议解决（3566/3578/3570 已在前文插入部分，此处补充排序）
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 90 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3566'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 91 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3578'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 92 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='争议解决' AND pt.point_code='3570'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 合同解除
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 100 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同解除' AND pt.point_code='3442'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 合同形式与生效
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 110 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3568'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 111 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3520'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 112 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3518'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 113 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='合同形式与生效' AND pt.point_code='3424'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);

-- 其他
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 120 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='其他' AND pt.point_code='3437'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);
INSERT INTO review_profile_item (profile_id, clause_type_id, point_id, sort_order)
SELECT pf.id, ct.id, pt.id, 121 FROM review_profile pf, review_clause_type ct, review_point pt
WHERE pf.profile_code='default' AND ct.clause_name='其他' AND pt.point_code='3422'
ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order);


