-- V7: Adjust auto fulfillment schema: category_code, task_type binding, task_type.code and mapping table

-- 1) auto_fulfillment_template: add category_code and task_type_id
ALTER TABLE auto_fulfillment_template
  ADD COLUMN category_code VARCHAR(100) NULL;

-- backfill from legacy column if present
UPDATE auto_fulfillment_template
  SET category_code = contract_type
  WHERE category_code IS NULL AND contract_type IS NOT NULL;

-- ensure not null after backfill
ALTER TABLE auto_fulfillment_template
  MODIFY COLUMN category_code VARCHAR(100) NOT NULL;

ALTER TABLE auto_fulfillment_template
  ADD COLUMN task_type_id BIGINT NULL;

-- helpful indexes
ALTER TABLE auto_fulfillment_template
  ADD INDEX idx_template_category_code (category_code);

ALTER TABLE auto_fulfillment_template
  ADD INDEX idx_template_task_type_id (task_type_id);

-- 2) auto_fulfillment_task_type: add business code and unique index
ALTER TABLE auto_fulfillment_task_type
  ADD COLUMN code VARCHAR(100) NULL;

ALTER TABLE auto_fulfillment_task_type
  ADD UNIQUE INDEX uk_task_type_code (code);

-- 3) mapping table: task_type <-> keyword
CREATE TABLE IF NOT EXISTS auto_fulfillment_task_type_keyword (
  task_type_id BIGINT NOT NULL,
  keyword_id BIGINT NOT NULL,
  PRIMARY KEY (task_type_id, keyword_id)
);


