-- V8: Seed business categories, task types, keywords mapping, and system templates

-- Seed parent categories as root task types (no parent_id), with business codes
-- invoice_fulfillment, payment_fulfillment, collection_fulfillment, expiry_reminder, event_trigger
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order, code)
VALUES
  (1000, NULL, '开票履约', 1, 'invoice_fulfillment'),
  (2000, NULL, '付款履约', 2, 'payment_fulfillment'),
  (3000, NULL, '收款履约', 3, 'collection_fulfillment'),
  (4000, NULL, '到期提醒', 4, 'expiry_reminder'),
  (5000, NULL, '事件触发', 5, 'event_trigger')
ON DUPLICATE KEY UPDATE name=VALUES(name), sort_order=VALUES(sort_order), code=VALUES(code);

-- Children leaves with codes
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order, code)
VALUES
  (1101, 1000, '预付款开票', 1, 'prepayment_invoice'),
  (1102, 1000, '进度款开票', 2, 'progress_invoice'),
  (1103, 1000, '验收款开票', 3, 'acceptance_invoice'),
  (2101, 2000, '应付任务-预付款', 1, 'payable_prepayment'),
  (2102, 2000, '应付任务-进度款', 2, 'payable_progress_payment'),
  (2103, 2000, '应付任务-尾款', 3, 'payable_final_payment'),
  (3101, 3000, '应收任务-预收款', 1, 'receivable_prepayment'),
  (3102, 3000, '应收任务-尾款', 2, 'receivable_final_payment'),
  (4101, 4000, '合同到期提醒', 1, 'contract_expiry_reminder'),
  (4102, 4000, '服务到期提醒', 2, 'service_expiry_reminder'),
  (5101, 5000, '验收合格提醒', 1, 'acceptance_passed_reminder'),
  (5102, 5000, '货物送达提醒', 2, 'goods_delivered_reminder')
ON DUPLICATE KEY UPDATE name=VALUES(name), sort_order=VALUES(sort_order), code=VALUES(code);

-- Seed keywords
INSERT INTO auto_fulfillment_keyword (id, name) VALUES
  (90001, '首笔款'), (90002, '合同生效'), (90003, '预付款'), (90004, '订金'),
  (90011, '终验'), (90012, '验收报告'), (90013, '交付完成'), (90014, '质检合格'),
  (90021, '支付'), (90022, '付款'), (90023, '转账'), (90024, '汇出'),
  (90031, '期限届满'), (90032, '到期日'), (90033, '终止日'), (90034, '续约'),
  (90041, '当...时'), (90042, '经确认后'), (90043, '达到...条件')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Map keywords to task types
-- prepayment_invoice
INSERT IGNORE INTO auto_fulfillment_task_type_keyword (task_type_id, keyword_id) VALUES
  (1101, 90001), (1101, 90002), (1101, 90003), (1101, 90004);

-- acceptance_invoice
INSERT IGNORE INTO auto_fulfillment_task_type_keyword (task_type_id, keyword_id) VALUES
  (1103, 90011), (1103, 90012), (1103, 90013), (1103, 90014);

-- payable_* (2101/2102/2103)
INSERT IGNORE INTO auto_fulfillment_task_type_keyword (task_type_id, keyword_id) VALUES
  (2101, 90021), (2101, 90022), (2101, 90023), (2101, 90024),
  (2102, 90021), (2102, 90022), (2102, 90023), (2102, 90024),
  (2103, 90021), (2103, 90022), (2103, 90023), (2103, 90024);

-- contract_expiry_reminder
INSERT IGNORE INTO auto_fulfillment_task_type_keyword (task_type_id, keyword_id) VALUES
  (4101, 90031), (4101, 90032), (4101, 90033), (4101, 90034);

-- acceptance_passed_reminder & goods_delivered_reminder
INSERT IGNORE INTO auto_fulfillment_task_type_keyword (task_type_id, keyword_id) VALUES
  (5101, 90041), (5101, 90042), (5101, 90043),
  (5102, 90041), (5102, 90042), (5102, 90043);

-- Initialize system templates for each leaf
-- Minimal generic fields; front-end can edit later
INSERT INTO auto_fulfillment_template (name, type, category_code, contract_type, fields, creator_id, create_time, update_time, is_default, description, task_type_id)
VALUES
  ('预付款开票-系统模板', 'system', 'invoice_fulfillment', 'invoice_fulfillment', '["合同编号","合同名称","甲方名称","乙方名称","开票金额","开票时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 1101),
  ('进度款开票-系统模板', 'system', 'invoice_fulfillment', 'invoice_fulfillment', '["合同编号","合同名称","甲方名称","乙方名称","开票金额","开票时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 1102),
  ('验收款开票-系统模板', 'system', 'invoice_fulfillment', 'invoice_fulfillment', '["合同编号","合同名称","甲方名称","乙方名称","开票金额","验收时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 1103),
  ('应付-预付款-系统模板', 'system', 'payment_fulfillment', 'payment_fulfillment', '["合同编号","合同名称","付款金额","付款条件","预计付款时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 2101),
  ('应付-进度款-系统模板', 'system', 'payment_fulfillment', 'payment_fulfillment', '["合同编号","合同名称","付款比例","付款节点","预计付款时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 2102),
  ('应付-尾款-系统模板', 'system', 'payment_fulfillment', 'payment_fulfillment', '["合同编号","合同名称","尾款金额","结算条件","预计付款时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 2103),
  ('应收-预收款-系统模板', 'system', 'collection_fulfillment', 'collection_fulfillment', '["合同编号","合同名称","应收金额","收款条件","预计收款时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 3101),
  ('应收-尾款-系统模板', 'system', 'collection_fulfillment', 'collection_fulfillment', '["合同编号","合同名称","应收尾款","结算条件","预计收款时间"]', 'system', NOW(), NOW(), false, '系统初始化模板', 3102),
  ('合同到期提醒-系统模板', 'system', 'expiry_reminder', 'expiry_reminder', '["合同编号","合同名称","到期日","提醒提前天数"]', 'system', NOW(), NOW(), false, '系统初始化模板', 4101),
  ('服务到期提醒-系统模板', 'system', 'expiry_reminder', 'expiry_reminder', '["服务名称","到期日","提醒提前天数"]', 'system', NOW(), NOW(), false, '系统初始化模板', 4102),
  ('验收合格提醒-系统模板', 'system', 'event_trigger', 'event_trigger', '["合同编号","合同名称","事件","提醒对象","提醒方式"]', 'system', NOW(), NOW(), false, '系统初始化模板', 5101),
  ('货物送达提醒-系统模板', 'system', 'event_trigger', 'event_trigger', '["合同编号","合同名称","事件","提醒对象","提醒方式"]', 'system', NOW(), NOW(), false, '系统初始化模板', 5102);


