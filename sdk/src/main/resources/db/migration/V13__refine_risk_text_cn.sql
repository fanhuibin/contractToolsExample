-- 改写风险库中文表述（不改编码，不改结构）
-- 目的：统一表达方式，保持前后端与数据库联动一致
-- 设计：幂等执行；仅更新文本字段；point_code 不变；status/sort/enabled 不变

-- 1) 分类名改写（除“合同主体”外其余改写），按 clause_code 更新
UPDATE review_clause_type SET clause_name='法律条款引用' WHERE clause_code='法律引用';
UPDATE review_clause_type SET clause_name='价款与支付'   WHERE clause_code='财务条款';
UPDATE review_clause_type SET clause_name='履约安排'     WHERE clause_code='履行';
UPDATE review_clause_type SET clause_name='验收与质量'   WHERE clause_code='验收';
UPDATE review_clause_type SET clause_name='知识产权约定' WHERE clause_code='知识产权';
UPDATE review_clause_type SET clause_name='违约与赔偿'   WHERE clause_code='违约责任';
UPDATE review_clause_type SET clause_name='保密约定'     WHERE clause_code='保密';
UPDATE review_clause_type SET clause_name='不可抗力约定' WHERE clause_code='不可抗力';
UPDATE review_clause_type SET clause_name='争议处理'     WHERE clause_code='争议解决';
UPDATE review_clause_type SET clause_name='解除条款'     WHERE clause_code='合同解除';
UPDATE review_clause_type SET clause_name='生效与形式'   WHERE clause_code='合同形式与生效';
UPDATE review_clause_type SET clause_name='其他事项'     WHERE clause_code='其他';

-- 2) 风险点名称与算法类型改写（按 point_code）
-- 统一口径：
--   己方主体 -> 内部相对方； 对方主体 -> 外部相对方
--   若仅名称调整，algorithm_type 同步改写为相同短语
UPDATE review_point SET point_name='内部相对方名称规范核对', algorithm_type='内部相对方名称规范核对' WHERE point_code='3649';
UPDATE review_point SET point_name='外部相对方名称规范核对', algorithm_type='外部相对方名称规范核对' WHERE point_code='3702';
UPDATE review_point SET point_name='法规引用准确性核对',     algorithm_type='法规引用准确性核对'     WHERE point_code='605';
UPDATE review_point SET point_name='金额大小写一致性核验',   algorithm_type='金额大小写一致性核验'   WHERE point_code='517';
UPDATE review_point SET point_name='含税/不含税约定核对',     algorithm_type='含税/不含税约定核对'     WHERE point_code='3521';
UPDATE review_point SET point_name='税率约定核验',           algorithm_type='税率约定核验'           WHERE point_code='3549';
UPDATE review_point SET point_name='价款组成范围核对',       algorithm_type='价款组成范围核对'       WHERE point_code='375';
UPDATE review_point SET point_name='支付节点与期限核对',     algorithm_type='支付节点与期限核对'     WHERE point_code='704';
UPDATE review_point SET point_name='支付方式与账户核对',     algorithm_type='支付方式与账户核对'     WHERE point_code='3524';
UPDATE review_point SET point_name='先款后票条款核对',       algorithm_type='先款后票条款核对'       WHERE point_code='378';
UPDATE review_point SET point_name='发票类型约定核对',       algorithm_type='发票类型约定核对'       WHERE point_code='3523';
UPDATE review_point SET point_name='税率调整处理核对',       algorithm_type='税率调整处理核对'       WHERE point_code='3445';
UPDATE review_point SET point_name='履约期限核对',           algorithm_type='履约期限核对'           WHERE point_code='578';
UPDATE review_point SET point_name='履约/交付地点核对',      algorithm_type='履约/交付地点核对'      WHERE point_code='689';
UPDATE review_point SET point_name='验收标准与质量核对',     algorithm_type='验收标准与质量核对'     WHERE point_code='369';
UPDATE review_point SET point_name='验收时限核对',           algorithm_type='验收时限核对'           WHERE point_code='3388';
UPDATE review_point SET point_name='不合格处理措施核对',     algorithm_type='不合格处理措施核对'     WHERE point_code='370';
UPDATE review_point SET point_name='知识产权归属约定核对',   algorithm_type='知识产权归属约定核对'   WHERE point_code='3423';
UPDATE review_point SET point_name='知产无瑕疵保证与侵权责任核对', algorithm_type='知产无瑕疵保证与侵权责任核对' WHERE point_code='674';
UPDATE review_point SET point_name='保密义务与范围核对',     algorithm_type='保密义务与范围核对'     WHERE point_code='652';
UPDATE review_point SET point_name='逾期履行责任核对',       algorithm_type='逾期履行责任核对'       WHERE point_code='651';
UPDATE review_point SET point_name='逾期付款责任核对',       algorithm_type='逾期付款责任核对'       WHERE point_code='356';
UPDATE review_point SET point_name='违约金标准合理性核对',   algorithm_type='违约金标准合理性核对'   WHERE point_code='478';
UPDATE review_point SET point_name='赔偿上限约定核对',       algorithm_type='赔偿上限约定核对'       WHERE point_code='567';
UPDATE review_point SET point_name='损失赔偿范围核对',       algorithm_type='损失赔偿范围核对'       WHERE point_code='374';
UPDATE review_point SET point_name='直接损失赔偿约定核对',   algorithm_type='直接损失赔偿约定核对'   WHERE point_code='3430';
UPDATE review_point SET point_name='连带责任范围核对',       algorithm_type='连带责任范围核对'       WHERE point_code='3446';
UPDATE review_point SET point_name='不可抗力条款核对',       algorithm_type='不可抗力条款核对'       WHERE point_code='653';
UPDATE review_point SET point_name='诉讼/仲裁择一约定核对',  algorithm_type='诉讼/仲裁择一约定核对'  WHERE point_code='3566';
UPDATE review_point SET point_name='管辖法院约定核对',       algorithm_type='管辖法院约定核对'       WHERE point_code='3578';
UPDATE review_point SET point_name='仲裁机构约定核对',       algorithm_type='仲裁机构约定核对'       WHERE point_code='3570';
UPDATE review_point SET point_name='单方通知解除权约定核对', algorithm_type='单方通知解除权约定核对' WHERE point_code='3442';
UPDATE review_point SET point_name='生效条件约定核对',       algorithm_type='生效条件约定核对'       WHERE point_code='3568';
UPDATE review_point SET point_name='合同编号填写核对',       algorithm_type='合同编号填写核对'       WHERE point_code='3520';
UPDATE review_point SET point_name='签署日期核对',           algorithm_type='签署日期核对'           WHERE point_code='3518';
UPDATE review_point SET point_name='签署地点核对',           algorithm_type='签署地点核对'           WHERE point_code='3424';
UPDATE review_point SET point_name='主合同与附件优先顺序核对', algorithm_type='主合同与附件优先顺序核对' WHERE point_code='3437';
UPDATE review_point SET point_name='合同份数与留存核对',     algorithm_type='合同份数与留存核对'     WHERE point_code='3422';

-- 3) 提示（prompt）用词统一与关键条目精确改写
-- 3.1 关键条目：内部/外部相对方（精确 Key 改写）
-- 3649 己方 -> 内部相对方
UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='首部未见内部相对方名称', pr.name='首部未见内部相对方名称'
WHERE pt.point_code='3649' AND pr.prompt_key='首部己方主体名称缺失';

UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='尾部内部相对方名称不规范', pr.name='尾部内部相对方名称不规范'
WHERE pt.point_code='3649' AND pr.prompt_key='尾部己方主体名称不规范';

UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='内部相对方名称前后不一致', pr.name='内部相对方名称前后不一致'
WHERE pt.point_code='3649' AND pr.prompt_key='己方主体名称不一致';

-- 3702 对方 -> 外部相对方
UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='未见外部相对方名称', pr.name='未见外部相对方名称'
WHERE pt.point_code='3702' AND pr.prompt_key='对方主体名称缺失';

UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='外部相对方名称前后不一致', pr.name='外部相对方名称前后不一致'
WHERE pt.point_code='3702' AND pr.prompt_key='对方主体名称不一致';

UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='外部相对方名称已识别请核对', pr.name='外部相对方名称已识别请核对'
WHERE pt.point_code='3702' AND pr.prompt_key='对方主体名称规范性确认';

-- 3.2 用词一致性（批量替换）：己方主体/对方主体 -> 内部/外部相对方（key/name/message）
UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key=REPLACE(pr.prompt_key,'己方主体','内部相对方'),
    pr.name=REPLACE(pr.name,'己方主体','内部相对方'),
    pr.message=REPLACE(pr.message,'己方主体','内部相对方')
WHERE pt.point_code IN ('3649');

UPDATE review_prompt pr
JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key=REPLACE(pr.prompt_key,'对方主体','外部相对方'),
    pr.name=REPLACE(pr.name,'对方主体','外部相对方'),
    pr.message=REPLACE(pr.message,'对方主体','外部相对方')
WHERE pt.point_code IN ('3702');

-- 3.3 可选：对若干通用条目的措辞微调（示例）
-- 法律引用（605）：核验 -> 核对；“旧法条仍在引用” -> “仍引用已失效条文”
UPDATE review_prompt pr JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='法律全称引用核对', pr.name='法律全称引用核对'
WHERE pt.point_code='605' AND pr.prompt_key='法律全称引用核验';

UPDATE review_prompt pr JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='法律简称引用核对', pr.name='法律简称引用核对'
WHERE pt.point_code='605' AND pr.prompt_key='法律简称引用核验';

UPDATE review_prompt pr JOIN review_point pt ON pr.point_id=pt.id
SET pr.prompt_key='仍引用已失效条文', pr.name='仍引用已失效条文'
WHERE pt.point_code='605' AND pr.prompt_key='旧法条仍在引用';

-- 保持 message 精简一致：如需更细化，可在后续迁移补充


