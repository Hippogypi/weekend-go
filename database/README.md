# Database Schema

本目录提供 weekend-go 第一版 MySQL 数据库结构定义。当前 feature 只交付独立 schema，不初始化 Spring Boot、Vue 或数据库工程。

## 文件

- `schema.sql`：MySQL 8.x 可执行建表脚本，包含核心表、枚举、外键、索引和初始化字典数据。

## 核心表关系

- `users` 保存普通用户和管理员账号，`role` 枚举为 `USER`、`ADMIN`。
- `places` 保存高德 POI 基础数据，使用 `amap_poi_id` 唯一去重；`workspace_status` 枚举为 `CANDIDATE`、`PENDING`、`APPROVED`、`REJECTED`。
- `workspace_profiles` 是地点公开聚合后的学习办公属性，与 `places` 一对一。
- `profile_submissions` 保存用户提交的原始共建属性，默认 `audit_status = PENDING`，审核通过后再参与聚合。
- `checkins` 保存打卡反馈，用 `created_at` 支持最近 2 小时状态聚合。
- `reviews`、`place_images`、`place_tags` 都带 `audit_status`，只应公开展示 `APPROVED` 数据。
- `favorites` 使用 `(user_id, place_id)` 唯一约束支持重复收藏冲突或幂等处理。
- `audit_logs` 记录管理员对属性、评价、图片、标签和地点的审核动作。
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
