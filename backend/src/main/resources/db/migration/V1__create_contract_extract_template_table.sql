-- 创建合同提取模板表
CREATE TABLE IF NOT EXISTS contract_extract_template (
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
) COMMENT='合同提取信息模板表';

-- 创建索引
CREATE INDEX idx_contract_extract_template_type ON contract_extract_template(type);
CREATE INDEX idx_contract_extract_template_contract_type ON contract_extract_template(contract_type);
CREATE INDEX idx_contract_extract_template_creator ON contract_extract_template(creator_id);
CREATE INDEX idx_contract_extract_template_default ON contract_extract_template(contract_type, is_default);