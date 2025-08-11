-- Create table for contract extract rules stored as JSON
CREATE TABLE IF NOT EXISTS contract_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_type VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NULL,
  content LONGTEXT NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


