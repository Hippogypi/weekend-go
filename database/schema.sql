-- weekend-go database schema for MySQL 8.x
-- Scope: first-version core resources for the city study/workspace sharing platform.

SET NAMES utf8mb4;
SET time_zone = '+08:00';
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  nickname VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  avatar_url VARCHAR(512) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_phone (phone),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和管理员账号';

CREATE TABLE IF NOT EXISTS places (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  amap_poi_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(255) NULL,
  longitude DECIMAL(10, 6) NOT NULL,
  latitude DECIMAL(10, 6) NOT NULL,
  amap_type VARCHAR(255) NULL,
  amap_type_code VARCHAR(32) NULL,
  province VARCHAR(64) NULL,
  city VARCHAR(64) NULL,
  district VARCHAR(64) NULL,
  source ENUM('AMAP_SEARCH', 'USER_SUBMIT', 'ADMIN_IMPORT') NOT NULL DEFAULT 'AMAP_SEARCH',
  workspace_status ENUM('CANDIDATE', 'PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_places_amap_poi_id (amap_poi_id),
  KEY idx_places_workspace_status (workspace_status),
  KEY idx_places_city_district (city, district),
  KEY idx_places_location (longitude, latitude),
  KEY idx_places_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点基础信息和高德 POI 去重数据';

CREATE TABLE IF NOT EXISTS workspace_profiles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  quiet_score DECIMAL(2, 1) NULL,
  wifi_score DECIMAL(2, 1) NULL,
  socket_score DECIMAL(2, 1) NULL,
  seat_score DECIMAL(2, 1) NULL,
  cost_score DECIMAL(2, 1) NULL,
  min_consumption INT UNSIGNED NULL,
  allow_long_stay ENUM('TRUE', 'FALSE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  score DECIMAL(3, 2) NULL,
  trust_level ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'LOW',
  approved_submission_count INT UNSIGNED NOT NULL DEFAULT 0,
  contributor_count INT UNSIGNED NOT NULL DEFAULT 0,
  last_contributed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_workspace_profiles_place_id (place_id),
  KEY idx_workspace_profiles_score (score),
  KEY idx_workspace_profiles_trust_level (trust_level),
  CONSTRAINT fk_workspace_profiles_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_workspace_profiles_quiet_score CHECK (quiet_score IS NULL OR quiet_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_workspace_profiles_wifi_score CHECK (wifi_score IS NULL OR wifi_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_workspace_profiles_socket_score CHECK (socket_score IS NULL OR socket_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_workspace_profiles_seat_score CHECK (seat_score IS NULL OR seat_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_workspace_profiles_cost_score CHECK (cost_score IS NULL OR cost_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_workspace_profiles_score CHECK (score IS NULL OR score BETWEEN 1.0 AND 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点聚合后的公开学习办公属性';

CREATE TABLE IF NOT EXISTS checkins (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  crowd_level ENUM('FREE', 'NORMAL', 'CROWDED', 'FULL') NOT NULL,
  noise_level ENUM('QUIET', 'RELATIVELY_QUIET', 'NORMAL', 'NOISY', 'VERY_NOISY') NOT NULL,
  has_seat TINYINT(1) NOT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_checkins_place_created (place_id, created_at),
  KEY idx_checkins_user_created (user_id, created_at),
  KEY idx_checkins_recent_status (place_id, created_at, crowd_level, noise_level, has_seat),
  CONSTRAINT fk_checkins_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_checkins_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点实时打卡与近期状态反馈';

CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  quiet_score DECIMAL(2, 1) NOT NULL,
  wifi_score DECIMAL(2, 1) NOT NULL,
  socket_score DECIMAL(2, 1) NOT NULL,
  comfort_score DECIMAL(2, 1) NOT NULL,
  cost_score DECIMAL(2, 1) NOT NULL,
  seat_score DECIMAL(2, 1) NULL,
  min_consumption INT UNSIGNED NULL,
  allow_long_stay ENUM('TRUE', 'FALSE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  suitable_scenes JSON NULL,
  content VARCHAR(1000) NOT NULL,
  like_count INT UNSIGNED NOT NULL DEFAULT 0,
  reply_count INT UNSIGNED NOT NULL DEFAULT 0,
  audit_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'DELETED') NOT NULL DEFAULT 'PENDING',
  audited_by BIGINT UNSIGNED NULL,
  audited_at DATETIME NULL,
  audit_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reviews_place_status_created (place_id, audit_status, created_at),
  KEY idx_reviews_user_created (user_id, created_at),
  KEY idx_reviews_audit_status (audit_status),
  KEY idx_reviews_hot (place_id, audit_status, like_count, created_at),
  CONSTRAINT fk_reviews_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_auditor
    FOREIGN KEY (audited_by) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT chk_reviews_quiet_score CHECK (quiet_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_reviews_wifi_score CHECK (wifi_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_reviews_socket_score CHECK (socket_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_reviews_comfort_score CHECK (comfort_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_reviews_cost_score CHECK (cost_score BETWEEN 1.0 AND 5.0),
  CONSTRAINT chk_reviews_seat_score CHECK (seat_score IS NULL OR seat_score BETWEEN 1.0 AND 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户对地点学习办公体验的评价';

CREATE TABLE IF NOT EXISTS review_likes (
  review_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (review_id, user_id),
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价点赞记录';

CREATE TABLE IF NOT EXISTS review_replies (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  review_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_review_replies_review (review_id, created_at),
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价回复';

CREATE TABLE IF NOT EXISTS place_qa (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  type ENUM('QUESTION','ANSWER') NOT NULL,
  parent_id BIGINT UNSIGNED NULL,
  content VARCHAR(1000) NOT NULL,
  answer_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_place_qa_place (place_id, type, created_at),
  KEY idx_place_qa_parent (parent_id, type, created_at),
  FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES place_qa(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点问答（问大家）';

CREATE TABLE IF NOT EXISTS place_images (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  review_id BIGINT UNSIGNED NULL,
  image_url VARCHAR(512) NOT NULL,
  description VARCHAR(500) NULL,
  audit_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'DELETED') NOT NULL DEFAULT 'PENDING',
  audited_by BIGINT UNSIGNED NULL,
  audited_at DATETIME NULL,
  audit_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_place_images_place_status_created (place_id, audit_status, created_at),
  KEY idx_place_images_user_created (user_id, created_at),
  KEY idx_place_images_audit_status (audit_status),
  KEY idx_place_images_review_id (review_id),
  CONSTRAINT fk_place_images_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_place_images_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_place_images_auditor
    FOREIGN KEY (audited_by) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_place_images_review
    FOREIGN KEY (review_id) REFERENCES reviews (id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户上传的地点图片记录';

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tag_name VARCHAR(32) NOT NULL,
  tag_group VARCHAR(32) NOT NULL DEFAULT 'WORKSPACE',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name_group (tag_name, tag_group),
  KEY idx_tags_group_enabled (tag_group, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台学习办公场景标签字典';

CREATE TABLE IF NOT EXISTS place_tags (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  place_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NULL,
  source ENUM('AMAP_SEARCH', 'USER_SUBMIT', 'ADMIN_IMPORT') NOT NULL DEFAULT 'USER_SUBMIT',
  audit_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'DELETED') NOT NULL DEFAULT 'PENDING',
  audited_by BIGINT UNSIGNED NULL,
  audited_at DATETIME NULL,
  audit_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_place_tags_place_tag_user (place_id, tag_id, user_id),
  KEY idx_place_tags_place_status (place_id, audit_status),
  KEY idx_place_tags_tag_status (tag_id, audit_status),
  KEY idx_place_tags_user_created (user_id, created_at),
  CONSTRAINT fk_place_tags_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_place_tags_tag
    FOREIGN KEY (tag_id) REFERENCES tags (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_place_tags_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_place_tags_auditor
    FOREIGN KEY (audited_by) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点与学习办公标签的共建关系';

CREATE TABLE IF NOT EXISTS favorites (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  place_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorites_user_place (user_id, place_id),
  KEY idx_favorites_place_created (place_id, created_at),
  CONSTRAINT fk_favorites_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_favorites_place
    FOREIGN KEY (place_id) REFERENCES places (id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏地点关系';

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  target_type ENUM('REVIEW', 'PLACE_IMAGE', 'PLACE_TAG', 'PLACE') NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  admin_id BIGINT UNSIGNED NOT NULL,
  action ENUM('APPROVED', 'REJECTED', 'DELETED') NOT NULL,
  reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_logs_target (target_type, target_id),
  KEY idx_audit_logs_admin_created (admin_id, created_at),
  CONSTRAINT fk_audit_logs_admin
    FOREIGN KEY (admin_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员审核动作记录';

CREATE TABLE IF NOT EXISTS search_keywords (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  keyword VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_keywords_keyword (keyword),
  KEY idx_search_keywords_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='高德候选地点搜索词配置';

INSERT INTO tags (tag_name, tag_group, enabled, sort_order) VALUES
  ('安静', 'WORKSPACE', 1, 10),
  ('较安静', 'WORKSPACE', 1, 20),
  ('插座多', 'WORKSPACE', 1, 30),
  ('Wi-Fi稳定', 'WORKSPACE', 1, 40),
  ('适合自习', 'WORKSPACE', 1, 50),
  ('适合阅读', 'WORKSPACE', 1, 60),
  ('适合远程办公', 'WORKSPACE', 1, 70),
  ('适合小组讨论', 'WORKSPACE', 1, 80),
  ('适合视频会议', 'WORKSPACE', 1, 90),
  ('可久坐', 'WORKSPACE', 1, 100)
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  sort_order = VALUES(sort_order),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO search_keywords (keyword, enabled, remark, sort_order) VALUES
  ('咖啡', 1, '候选学习办公空间搜索词', 10),
  ('图书馆', 1, '候选学习办公空间搜索词', 20),
  ('书店', 1, '候选学习办公空间搜索词', 30),
  ('茶馆', 1, '候选学习办公空间搜索词', 40),
  ('自习室', 1, '候选学习办公空间搜索词', 50),
  ('阅览室', 1, '候选学习办公空间搜索词', 60),
  ('书吧', 1, '候选学习办公空间搜索词', 70),
  ('共享办公', 1, '候选学习办公空间搜索词', 80),
  ('商务中心', 1, '候选学习办公空间搜索词', 90)
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  remark = VALUES(remark),
  sort_order = VALUES(sort_order),
  updated_at = CURRENT_TIMESTAMP;

UPDATE audit_logs SET target_type = 'REVIEW' WHERE target_type = 'PROFILE_SUBMISSION';

SET FOREIGN_KEY_CHECKS = 1;
