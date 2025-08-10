-- V5: Create auto fulfillment template and history tables

-- auto_fulfillment_template (aligned to contract_extract_template)
CREATE TABLE IF NOT EXISTS auto_fulfillment_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type VARCHAR(20) NOT NULL COMMENT '模板类型：system-系统模板，user-用户模板',
    contract_type VARCHAR(50) NOT NULL COMMENT '合同类型',
    fields TEXT NOT NULL COMMENT '提取字段列表，JSON格式',
    creator_id VARCHAR(50) COMMENT '创建者ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否默认模板',
    description TEXT COMMENT '描述'
) COMMENT='自动履约任务模板表';

-- indexes
CREATE INDEX idx_auto_ful_template_type ON auto_fulfillment_template(type);
CREATE INDEX idx_auto_ful_template_contract_type ON auto_fulfillment_template(contract_type);
CREATE INDEX idx_auto_ful_template_creator ON auto_fulfillment_template(creator_id);
CREATE INDEX idx_auto_ful_template_default ON auto_fulfillment_template(contract_type, is_default);

-- auto_fulfillment_history (aligned to contract_extract_history)
CREATE TABLE IF NOT EXISTS auto_fulfillment_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    extracted_content LONGTEXT NOT NULL COMMENT '识别的JSON结果',
    extract_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '识别时间',
    user_id VARCHAR(100) DEFAULT NULL COMMENT '用户ID',
    PRIMARY KEY (id),
    KEY idx_auto_ful_hist_user_time (user_id, extract_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动履约任务识别历史表';


