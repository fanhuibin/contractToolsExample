-- V6: Auto-fulfillment dictionaries (task types and keywords) with id-based relations

CREATE TABLE IF NOT EXISTS auto_fulfillment_task_type (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auto_fulfillment_keyword (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auto_fulfillment_task_keyword (
  task_type_id BIGINT NOT NULL,
  keyword_id BIGINT NOT NULL,
  PRIMARY KEY (task_type_id, keyword_id),
  KEY idx_kw (keyword_id),
  CONSTRAINT fk_task_type FOREIGN KEY (task_type_id) REFERENCES auto_fulfillment_task_type(id) ON DELETE CASCADE,
  CONSTRAINT fk_keyword FOREIGN KEY (keyword_id) REFERENCES auto_fulfillment_keyword(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed task types (root categories)
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (1, NULL, '开票履约', 1),
  (2, NULL, '付款履约', 2),
  (3, NULL, '收款履约', 3),
  (4, NULL, '到期提醒', 4),
  (5, NULL, '事件触发', 5)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Children under 开票履约
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (101, 1, '预付款开票', 1),
  (102, 1, '进度款开票', 2),
  (103, 1, '验收款开票', 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Children under 付款履约
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (201, 2, '应付任务-预付款', 1),
  (202, 2, '应付任务-进度款', 2),
  (203, 2, '应付任务-尾款', 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Children under 收款履约
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (301, 3, '应收任务-预收款', 1),
  (302, 3, '应收任务-尾款', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Children under 到期提醒
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (401, 4, '合同到期提醒', 1),
  (402, 4, '服务到期提醒', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Children under 事件触发
INSERT INTO auto_fulfillment_task_type (id, parent_id, name, sort_order) VALUES
  (501, 5, '验收合格提醒', 1),
  (502, 5, '货物送达提醒', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Keywords
INSERT INTO auto_fulfillment_keyword (id, name) VALUES
  (1001, '首笔款'), (1002, '合同生效'), (1003, '预付款'), (1004, '订金'),
  (1101, '终验'), (1102, '验收报告'), (1103, '交付完成'), (1104, '质检合格'),
  (1201, '支付'), (1202, '付款'), (1203, '转账'), (1204, '汇出'),
  (1301, '期限届满'), (1302, '到期日'), (1303, '终止日'), (1304, '续约'),
  (1401, '当...时'), (1402, '经确认后'), (1403, '达到...条件')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Mappings
INSERT INTO auto_fulfillment_task_keyword (task_type_id, keyword_id) VALUES
  (101,1001),(101,1002),(101,1003),(101,1004),
  (103,1101),(103,1102),(103,1103),(103,1104),
  (201,1201),(201,1202),(201,1203),(201,1204),
  (202,1201),(202,1202),(202,1203),(202,1204),
  (203,1201),(203,1202),(203,1203),(203,1204),
  (401,1301),(401,1302),(401,1303),(401,1304),
  (501,1401),(501,1402),(501,1403)
ON DUPLICATE KEY UPDATE keyword_id=VALUES(keyword_id);


