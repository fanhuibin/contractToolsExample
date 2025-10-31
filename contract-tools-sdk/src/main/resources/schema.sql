-- ============================================
-- 合同工具集数据库表结构初始化脚本
-- 山西肇新科技有限公司
-- ============================================

-- 文件信息表
CREATE TABLE IF NOT EXISTS `file_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '存储文件名',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件路径（兼容旧字段）',
  `store_path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `file_extension` VARCHAR(20) DEFAULT NULL COMMENT '文件扩展名',
  `file_md5` VARCHAR(64) DEFAULT NULL COMMENT '文件MD5值',
  `status` INT DEFAULT 0 COMMENT '状态：0-正常，1-删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `upload_time` DATETIME DEFAULT NULL COMMENT '上传时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `onlyoffice_key` VARCHAR(100) DEFAULT NULL COMMENT '文档编辑器密钥',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '所属模块（用于区分文件来源）',
  PRIMARY KEY (`id`),
  INDEX `idx_file_md5` (`file_md5`),
  INDEX `idx_module` (`module`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 模板设计记录表
CREATE TABLE IF NOT EXISTS `template_design_record` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
  `template_code` VARCHAR(100) DEFAULT NULL COMMENT '模板编码（多个版本共用）',
  `template_name` VARCHAR(255) DEFAULT NULL COMMENT '模板名称',
  `version` VARCHAR(20) DEFAULT NULL COMMENT '版本号（如：1.0, 1.1, 2.0）',
  `template_id` VARCHAR(64) DEFAULT NULL COMMENT '旧字段，保留兼容性',
  `file_id` VARCHAR(64) DEFAULT NULL COMMENT '文件ID',
  `elements_json` LONGTEXT DEFAULT NULL COMMENT '元素JSON数据',
  `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿, PUBLISHED-已发布, DISABLED-已禁用, DELETED-已删除',
  `description` TEXT DEFAULT NULL COMMENT '模板描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  INDEX `idx_template_code` (`template_code`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板设计记录表';

