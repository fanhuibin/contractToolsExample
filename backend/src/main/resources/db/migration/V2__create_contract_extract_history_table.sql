CREATE TABLE `contract_extract_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name` varchar(255) NOT NULL COMMENT '提取的文件名',
    `extracted_content` longtext NOT NULL COMMENT '提取的JSON内容',
    `extract_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提取时间',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_extract_time` (`user_id`, `extract_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同信息提取历史记录表';

