# Database Schema

本目录提供 weekend-go 第一版 MySQL 数据库结构定义。当前 feature 只交付独立 schema，不初始化 Spring Boot、Vue 或数据库工程。

## 文件

- `schema.sql`：MySQL 8.x 可执行建表脚本，包含核心表、枚举、外键、索引和初始化字典数据。
- `dev_seed.sql`：本地开发演示数据脚本，包含演示账号、地点、公开属性、近期打卡、评价、图片、收藏和审核相关数据。

## 核心表关系

- `users` 保存普通用户和管理员账号，`role` 枚举为 `USER`、`ADMIN`。
- `places` 保存高德 POI 基础数据，使用 `amap_poi_id` 唯一去重；`workspace_status` 枚举为 `CANDIDATE`、`PENDING`、`APPROVED`、`REJECTED`。
- `workspace_profiles` 是地点公开聚合后的学习办公属性，与 `places` 一对一。
- `checkins` 保存打卡反馈，用 `created_at` 支持最近 2 小时状态聚合。
- `reviews`、`place_images`、`place_tags` 都带 `audit_status`，只应公开展示 `APPROVED` 数据。
- `reviews` 同时承载用户对地点的体验评分、文本评价和共建属性（如 `seat_score`、`allow_long_stay`、`suitable_scenes`）。
- `review_likes`、`review_replies` 支持评价的点赞和回复互动。
- `place_qa` 提供地点问答（问大家）功能。
- `favorites` 使用 `(user_id, place_id)` 唯一约束支持重复收藏冲突或幂等处理。
- `audit_logs` 记录管理员对评价、图片、标签和地点的审核动作。
- `tags` 与 `search_keywords` 是初始化字典表，不包含真实生产数据或密钥。

## 关键索引

- `places.uk_places_amap_poi_id`：高德 POI 去重。
- `places.idx_places_location`：附近查询的经纬度预过滤。
- `*_audit_status` 和 `(place_id, audit_status, created_at)`：管理员审核列表和公开内容查询。
- `checkins.idx_checkins_recent_status`：最近 2 小时拥挤度、噪音、空座统计。
- `favorites.uk_favorites_user_place`：收藏唯一性。
- `audit_logs.idx_audit_logs_target`：按被审核资源追踪审核历史。

## 状态枚举

脚本中的状态取值与需求文档保持一致：

- `workspace_status`：`CANDIDATE`, `PENDING`, `APPROVED`, `REJECTED`
- `audit_status`：`PENDING`, `APPROVED`, `REJECTED`, `DELETED`
- `crowd_level`：`FREE`, `NORMAL`, `CROWDED`, `FULL`
- `noise_level`：`QUIET`, `RELATIVELY_QUIET`, `NORMAL`, `NOISY`, `VERY_NOISY`
- `source`：`AMAP_SEARCH`, `USER_SUBMIT`, `ADMIN_IMPORT`
- `role`：`USER`, `ADMIN`

`allow_long_stay` 使用 `TRUE`、`FALSE`、`UNKNOWN` 表达需求文档中的 true / false / unknown。

## 执行方式

可在已有 MySQL 数据库中执行：

```bash
mysql -u <user> -p <database_name> < database/schema.sql
```

脚本不会创建真实用户账号、地点或任何密钥配置；只初始化平台标签和候选搜索词字典。

## 本地演示数据

`dev_seed.sql` 用于给本机 `weekend_go` 注入可直接联调前端的基础数据。它只包含非生产演示数据，可重复执行。

演示账号：

| 用户名 | 角色 | 演示密码 |
| --- | --- | --- |
| `api-user-demo` | `USER` | `secret123` |
| `api-admin-demo` | `ADMIN` | `secret123` |

导入命令示例：

```powershell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u <user> -p --default-character-set=utf8mb4 weekend_go -e "source E:/App/service_development/weekend-go/database/dev_seed.sql"
```

当前本机真实 `weekend_go` 已完成导入，验证结果：

- 演示账号：2 个。
- 演示地点：3 个。
- 公开 `workspace_profiles`：3 条。
- 最近 2 小时打卡：3 条。
- 审核通过评价：2 条。
- 审核通过图片：2 条。
- 演示收藏：2 条。

## 本地 weekend_go 开发库

`local-database-setup` 已在本机 `MySQL80` 服务上建立可持续使用的 `weekend_go` 开发库。数据库字符集和排序规则为 `utf8mb4` / `utf8mb4_0900_ai_ci`，与 schema 中的表定义兼容。

推荐本地初始化步骤：

```powershell
mysql --version
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS weekend_go CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'weekend_go'@'localhost' IDENTIFIED BY '<local-password>'; CREATE USER IF NOT EXISTS 'weekend_go'@'127.0.0.1' IDENTIFIED BY '<local-password>'; GRANT ALL PRIVILEGES ON weekend_go.* TO 'weekend_go'@'localhost'; GRANT ALL PRIVILEGES ON weekend_go.* TO 'weekend_go'@'127.0.0.1'; FLUSH PRIVILEGES;"
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u weekend_go -p --default-character-set=utf8mb4 weekend_go < database/schema.sql
```

导入后可用以下查询确认表和初始化字典数据：

```powershell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u weekend_go -p weekend_go -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='weekend_go'; SELECT COUNT(*) AS tag_count FROM tags; SELECT COUNT(*) AS search_keyword_count FROM search_keywords;"
```

当前 schema 验证结果：`table_count = 14`，`tag_count = 10`，`search_keyword_count = 9`。

## MySQL 8.0.43 验证记录

本地默认 `MySQL80` server 正在运行，但未提供可提交的非敏感账号密码；无密码访问 `root@localhost` 被拒绝。因此本 feature 使用同机 `mysqld 8.0.43` 在 worktree 下创建临时 `--initialize-insecure` 数据目录，并在 `127.0.0.1:33307` 短暂启动隔离实例完成真实建表验证。临时实例只用于执行 schema，不包含生产数据、真实账号密码或本地敏感配置。

已执行的核心验证：

```bash
mysql --protocol=TCP -h 127.0.0.1 -P 33307 -u root \
  -e "DROP DATABASE IF EXISTS weekend_go_verify; CREATE DATABASE weekend_go_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

mysql --protocol=TCP -h 127.0.0.1 -P 33307 -u root \
  --default-character-set=utf8mb4 weekend_go_verify \
  -e "source E:/App/service_development/weekend-go/.worktrees/database-mysql-verification/database/schema.sql"
```

验证结果：

- `schema.sql` 可在 MySQL 8.0.43 执行建表，导入退出码为 0。
- `information_schema.tables` 统计得到 14 张表。
- `information_schema.statistics` 覆盖所有核心表索引：`users`、`places`、`workspace_profiles`、`checkins`、`reviews`、`review_likes`、`review_replies`、`place_qa`、`place_images`、`tags`、`place_tags`、`favorites`、`audit_logs`、`search_keywords`。
- `information_schema.table_constraints` 确认外键约束已创建，包括地点、用户、审核员、标签、收藏和审核日志关系。
- 初始化字典数据验证通过：`tags` 为 10 条，`search_keywords` 为 9 条。
