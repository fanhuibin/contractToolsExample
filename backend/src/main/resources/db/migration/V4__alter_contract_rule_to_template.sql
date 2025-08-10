-- V4: Bind rules to templateId, remove unique on contract_type, add unique on template_id

-- 0) Ensure contract_rule has column `template_id`
SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'contract_rule'
    AND column_name = 'template_id'
);
SET @add_col_sql := IF(@col_exists = 0,
  'ALTER TABLE contract_rule ADD COLUMN template_id BIGINT NULL',
  'SELECT 1'
);
PREPARE stmt0 FROM @add_col_sql; EXECUTE stmt0; DEALLOCATE PREPARE stmt0;

-- 1) Ensure there are templates for each contract_type appearing in contract_rule
--    for which no template exists yet. Create minimal placeholder templates.
INSERT INTO contract_extract_template (
  name, type, contract_type, fields, creator_id, create_time, update_time, is_default, description
)
SELECT CONCAT('迁移模板-', s.contract_type) AS name,
       'system' AS type,
       s.contract_type,
       '[]' AS fields,
       'system' AS creator_id,
       NOW() AS create_time,
       NOW() AS update_time,
       FALSE AS is_default,
       '由V4迁移自动创建' AS description
FROM (
  SELECT DISTINCT cr.contract_type
  FROM contract_rule cr
  LEFT JOIN contract_extract_template cet ON BINARY cet.contract_type = BINARY cr.contract_type
  WHERE cet.id IS NULL
) s;

-- 2) Map contract_rule.template_id by default template first
UPDATE contract_rule cr
JOIN (
  SELECT t.contract_type, t.id
  FROM contract_extract_template t
  WHERE t.is_default = TRUE
) d ON BINARY d.contract_type = BINARY cr.contract_type
SET cr.template_id = d.id
WHERE cr.template_id IS NULL;

-- 3) Fallback: map by any template (min id) if still null
UPDATE contract_rule cr
JOIN (
  SELECT contract_type, MIN(id) AS id
  FROM contract_extract_template
  GROUP BY contract_type
) m ON BINARY m.contract_type = BINARY cr.contract_type
SET cr.template_id = m.id
WHERE cr.template_id IS NULL;

-- 4) Drop unique constraint on contract_type if exists
--    Original was created via `contract_type VARCHAR(100) UNIQUE` which creates a unique index named `contract_type` in MySQL
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'contract_rule' AND index_name = 'contract_type'
);
SET @drop_idx_sql := IF(@idx_exists > 0, 'ALTER TABLE contract_rule DROP INDEX contract_type', 'SELECT 1');
PREPARE stmt FROM @drop_idx_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Add unique constraint on template_id and an index
SET @uk_exists := (
  SELECT COUNT(1) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'contract_rule' AND constraint_name = 'uk_contract_rule_template_id'
);
SET @add_uk_sql := IF(@uk_exists = 0, 'ALTER TABLE contract_rule ADD CONSTRAINT uk_contract_rule_template_id UNIQUE (template_id)', 'SELECT 1');
PREPARE stmt2 FROM @add_uk_sql; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
-- The unique index itself is sufficient for performance, but create a named index if desired (will be redundant if same key)
-- CREATE INDEX idx_contract_rule_template_id ON contract_rule(template_id);


