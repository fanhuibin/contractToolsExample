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

CREATE TABLE `contract_extract_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name` varchar(255) NOT NULL COMMENT '提取的文件名',
    `extracted_content` longtext NOT NULL COMMENT '提取的JSON内容',
    `extract_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提取时间',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_extract_time` (`user_id`, `extract_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同信息提取历史记录表';

CREATE TABLE IF NOT EXISTS template_design_record (
  id            varchar(64)  NOT NULL PRIMARY KEY,
  template_id   varchar(64)  NULL,
  file_id       varchar(64)  NULL,
  elements_json longtext     NULL,
  created_at    datetime     NULL,
  updated_at    datetime     NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 文件信息表（用于OnlyOffice预览与文件注册）
CREATE TABLE IF NOT EXISTS file_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(1024) NULL,
  store_path VARCHAR(1024) NULL,
  file_size BIGINT NULL,
  file_type VARCHAR(64) NULL,
  file_extension VARCHAR(32) NULL,
  file_md5 VARCHAR(64) NULL,
  status TINYINT DEFAULT 0,
  create_time DATETIME NULL,
  upload_time DATETIME NULL,
  update_time DATETIME NULL,
  onlyoffice_key VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模板ID唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_design_record_template_id ON template_design_record(template_id);

CREATE TABLE IF NOT EXISTS compare_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_id VARCHAR(64) NOT NULL UNIQUE,
  old_pdf_name VARCHAR(255) NOT NULL,
  new_pdf_name VARCHAR(255) NOT NULL,
  results_json LONGTEXT,
  created_at DATETIME NOT NULL
);
