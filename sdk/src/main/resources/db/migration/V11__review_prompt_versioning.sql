-- Review prompt versioning and examples

CREATE TABLE IF NOT EXISTS review_prompt_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prompt_id BIGINT NOT NULL,
  version_code VARCHAR(64) NOT NULL,
  is_published TINYINT(1) NOT NULL DEFAULT 0,
  content_text TEXT,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_prompt_version (prompt_id, version_code),
  KEY idx_prompt (prompt_id)
);

CREATE TABLE IF NOT EXISTS review_prompt_example (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prompt_version_id BIGINT NOT NULL,
  user_example TEXT,
  assistant_example TEXT,
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_version (prompt_version_id),
  KEY idx_sort (prompt_version_id, sort_order)
);

-- Optional mapping: per profile, pin specific prompt version
CREATE TABLE IF NOT EXISTS profile_prompt_version (
  profile_id BIGINT NOT NULL,
  prompt_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  PRIMARY KEY (profile_id, prompt_id),
  KEY idx_version (version_id)
);


