-- weekend-go local development seed data.
-- Scope: non-production demo records for the local weekend_go database.
-- Demo accounts use password: secret123

SET NAMES utf8mb4;
SET time_zone = '+08:00';

START TRANSACTION;

INSERT INTO users (username, password_hash, role, nickname, email, enabled) VALUES
  ('api-user-demo', '$2a$10$G5ESAzfaWKm/t6Oui5hCd.MvY7gX1T7jxGbpYFtE55sYLcNSJTRdC', 'USER', 'API User', 'api-user-demo@example.test', 1),
  ('api-admin-demo', '$2a$10$G5ESAzfaWKm/t6Oui5hCd.MvY7gX1T7jxGbpYFtE55sYLcNSJTRdC', 'ADMIN', 'API Admin', 'api-admin-demo@example.test', 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  nickname = VALUES(nickname),
  email = VALUES(email),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;

SET @demo_user_id = (SELECT id FROM users WHERE username = 'api-user-demo');
SET @demo_admin_id = (SELECT id FROM users WHERE username = 'api-admin-demo');

INSERT INTO places (
  amap_poi_id, name, address, longitude, latitude, amap_type, amap_type_code,
  province, city, district, source, workspace_status
) VALUES
  ('dev-poi-library-001', '周末图书馆自习区', '上海市徐汇区衡山路 100 号', 121.446601, 31.204210, '科教文化服务;图书馆;图书馆', '141200', '上海市', '上海市', '徐汇区', 'ADMIN_IMPORT', 'APPROVED'),
  ('dev-poi-cafe-002', '梧桐咖啡办公角', '上海市静安区南京西路 888 号', 121.459850, 31.229670, '餐饮服务;咖啡厅;咖啡厅', '050500', '上海市', '上海市', '静安区', 'ADMIN_IMPORT', 'APPROVED'),
  ('dev-poi-bookstore-003', '城市书店共享长桌', '上海市黄浦区福州路 300 号', 121.482120, 31.233550, '购物服务;专卖店;书店', '060900', '上海市', '上海市', '黄浦区', 'ADMIN_IMPORT', 'APPROVED')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  address = VALUES(address),
  longitude = VALUES(longitude),
  latitude = VALUES(latitude),
  amap_type = VALUES(amap_type),
  amap_type_code = VALUES(amap_type_code),
  province = VALUES(province),
  city = VALUES(city),
  district = VALUES(district),
  source = VALUES(source),
  workspace_status = VALUES(workspace_status),
  updated_at = CURRENT_TIMESTAMP;

SET @library_id = (SELECT id FROM places WHERE amap_poi_id = 'dev-poi-library-001');
SET @cafe_id = (SELECT id FROM places WHERE amap_poi_id = 'dev-poi-cafe-002');
SET @bookstore_id = (SELECT id FROM places WHERE amap_poi_id = 'dev-poi-bookstore-003');

DELETE FROM place_tags
WHERE user_id = @demo_admin_id
  AND place_id IN (@library_id, @cafe_id, @bookstore_id)
  AND audit_reason = 'demo seed';

DELETE FROM favorites
WHERE user_id = @demo_user_id
  AND place_id IN (@library_id, @cafe_id, @bookstore_id);

DELETE FROM place_images
WHERE user_id = @demo_user_id
  AND place_id IN (@library_id, @cafe_id, @bookstore_id)
  AND audit_reason <=> 'demo seed';

DELETE FROM reviews
WHERE user_id = @demo_user_id
  AND place_id IN (@library_id, @cafe_id, @bookstore_id)
  AND audit_reason <=> 'demo seed';

DELETE FROM checkins
WHERE user_id = @demo_user_id
  AND place_id IN (@library_id, @cafe_id, @bookstore_id)
  AND remark IN ('工作日上午座位充足。', '午后客流较多，建议早到。', '长桌还有空位。');

INSERT INTO workspace_profiles (
  place_id, quiet_score, wifi_score, socket_score, seat_score, cost_score,
  min_consumption, allow_long_stay, score, trust_level,
  approved_submission_count, contributor_count, last_contributed_at
) VALUES
  (@library_id, 4.8, 4.1, 3.8, 4.4, 4.9, 0, 'TRUE', 4.40, 'HIGH', 6, 4, NOW()),
  (@cafe_id, 3.7, 4.6, 4.2, 3.6, 3.2, 28, 'TRUE', 3.86, 'MEDIUM', 4, 3, NOW()),
  (@bookstore_id, 4.3, 3.8, 3.3, 4.0, 4.0, 0, 'UNKNOWN', 3.88, 'MEDIUM', 3, 2, NOW())
ON DUPLICATE KEY UPDATE
  quiet_score = VALUES(quiet_score),
  wifi_score = VALUES(wifi_score),
  socket_score = VALUES(socket_score),
  seat_score = VALUES(seat_score),
  cost_score = VALUES(cost_score),
  min_consumption = VALUES(min_consumption),
  allow_long_stay = VALUES(allow_long_stay),
  score = VALUES(score),
  trust_level = VALUES(trust_level),
  approved_submission_count = VALUES(approved_submission_count),
  contributor_count = VALUES(contributor_count),
  last_contributed_at = VALUES(last_contributed_at),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO checkins (place_id, user_id, crowd_level, noise_level, has_seat, remark, created_at) VALUES
  (@library_id, @demo_user_id, 'NORMAL', 'QUIET', 1, '工作日上午座位充足。', NOW() - INTERVAL 40 MINUTE),
  (@cafe_id, @demo_user_id, 'CROWDED', 'NORMAL', 0, '午后客流较多，建议早到。', NOW() - INTERVAL 70 MINUTE),
  (@bookstore_id, @demo_user_id, 'FREE', 'RELATIVELY_QUIET', 1, '长桌还有空位。', NOW() - INTERVAL 20 MINUTE);

INSERT INTO reviews (
  place_id, user_id, quiet_score, wifi_score, socket_score, comfort_score, cost_score,
  seat_score, min_consumption, allow_long_stay, suitable_scenes,
  content, audit_status, audited_by, audited_at, audit_reason
) VALUES
  (@library_id, @demo_user_id, 5.0, 4.0, 4.0, 4.5, 5.0, 4.5, 0, 'TRUE', JSON_ARRAY('READING', 'SELF_STUDY'), '安静程度很高，适合集中写作和备考。', 'APPROVED', @demo_admin_id, NOW(), 'demo seed'),
  (@cafe_id, @demo_user_id, 3.5, 4.5, 4.5, 4.0, 3.0, 3.5, 28, 'TRUE', JSON_ARRAY('REMOTE_WORK', 'LIGHT_MEETING'), '网络和插座不错，但下午噪音会升高。', 'APPROVED', @demo_admin_id, NOW(), 'demo seed'),
  (@bookstore_id, @demo_user_id, 4.0, 3.5, 3.0, 4.0, 4.0, 4.0, 0, 'UNKNOWN', JSON_ARRAY('READING'), '环境舒服，适合阅读，不太适合视频会议。', 'PENDING', NULL, NULL, NULL);

INSERT INTO place_images (
  place_id, user_id, image_url, description, audit_status, audited_by, audited_at, audit_reason
) VALUES
  (@library_id, @demo_user_id, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f', '图书馆阅读区示意图', 'APPROVED', @demo_admin_id, NOW(), 'demo seed'),
  (@cafe_id, @demo_user_id, 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085', '咖啡办公角示意图', 'APPROVED', @demo_admin_id, NOW(), 'demo seed'),
  (@bookstore_id, @demo_user_id, 'https://images.unsplash.com/photo-1519682337058-a94d519337bc', '书店长桌示意图', 'PENDING', NULL, NULL, NULL);

INSERT IGNORE INTO favorites (user_id, place_id) VALUES
  (@demo_user_id, @library_id),
  (@demo_user_id, @cafe_id);

INSERT INTO place_tags (place_id, tag_id, user_id, source, audit_status, audited_by, audited_at, audit_reason)
SELECT seeded.place_id, tags.id, @demo_admin_id, 'ADMIN_IMPORT', 'APPROVED', @demo_admin_id, NOW(), 'demo seed'
FROM (
  SELECT @library_id AS place_id, '安静' AS tag_name UNION ALL
  SELECT @library_id, '适合自习' UNION ALL
  SELECT @cafe_id, 'Wi-Fi稳定' UNION ALL
  SELECT @cafe_id, '适合远程办公' UNION ALL
  SELECT @bookstore_id, '适合阅读'
) seeded
JOIN tags ON tags.tag_name = seeded.tag_name AND tags.tag_group = 'WORKSPACE'
ON DUPLICATE KEY UPDATE
  source = VALUES(source),
  audit_status = VALUES(audit_status),
  audited_by = VALUES(audited_by),
  audited_at = VALUES(audited_at),
  audit_reason = VALUES(audit_reason),
  updated_at = CURRENT_TIMESTAMP;

COMMIT;
