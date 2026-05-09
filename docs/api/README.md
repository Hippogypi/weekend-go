# weekend-go API Verification

本目录提供 weekend-go 第一版主要 REST API 的接口验证资产。文件只包含占位变量和示例数据，不包含真实数据库密码、Amap Key、登录 token 或本地敏感配置。

## 文件

- `weekend-go.postman_collection.json`: Postman Collection v2.1，可直接导入 Postman。
- `README.md`: 本地环境、调用顺序、管理员账号准备、PowerShell 示例、清理方式和验证记录。

## 本地环境变量

Postman collection 使用以下变量：

| 变量 | 示例 | 说明 |
| --- | --- | --- |
| `baseUrl` | `http://localhost:8080` | 后端地址 |
| `userUsername` | `api-user-demo` | 普通用户用户名；重复运行前改名或先清理演示数据 |
| `userPassword` | `secret123` | 普通用户演示密码，至少 8 位 |
| `userNickname` | `API User` | 普通用户昵称 |
| `adminUsername` | `api-admin-demo` | 管理员用户名，可由 `database/dev_seed.sql` 注入 |
| `adminPassword` | `secret123` | 管理员演示密码，至少 8 位 |
| `userToken` | 自动写入 | 普通用户登录后由 collection test script 写入 |
| `adminToken` | 自动写入 | 管理员登录后由 collection test script 写入 |
| `keyword` | `library` | 关键词搜索参数 |
| `city` | `Beijing` | 关键词搜索城市 |
| `longitude` | `116.481488` | 周边搜索经度 |
| `latitude` | `39.990464` | 周边搜索纬度 |
| `placeId` | 自动写入或手动填写 | 地点 id，搜索成功时自动取第一条；Amap 不可用时手动填写 |
| `profileSubmissionId` | 自动写入 | 共建提交 id |
| `reviewId` | 自动写入 | 评价 id |
| `imageId` | 自动写入 | 图片 id |
| `missingPlaceId` | `999999999` | 不存在地点错误验证 |

## 启动后端

普通测试命令：

```powershell
cd backend
.\mvnw.cmd test
```

连接本地 MySQL `weekend_go` 启动后端时，不要在命令或文件中写真实密码。推荐在当前 shell 或 Windows 用户级环境变量中设置：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/weekend_go?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="<local-db-user>"
$env:DB_PASSWORD="<local-db-password>"
$env:AMAP_API_KEY="<local-amap-web-service-key>"
cd backend
.\mvnw.cmd spring-boot:run
```

如果需要可直接演示的账号和地点数据，先在仓库根目录执行 `database/dev_seed.sql`。脚本会注入 `api-user-demo` 和 `api-admin-demo` 两个演示账号，密码均为 `secret123`。

如果不准备调用 Amap，可不设置 `AMAP_API_KEY`，但 `/api/workspaces/search` 和 `/api/workspaces/nearby` 可能返回外部服务错误。此时可先在本地 MySQL 准备一个测试地点，然后在 Postman 环境中手动填写 `placeId`。

## 管理员账号准备

公开注册接口只创建 `USER`，不会创建 `ADMIN`。管理员链路验证前，先用 collection 中的普通注册请求或下面命令创建一个演示账号，再在本地数据库把它提升为管理员。

```powershell
$baseUrl = "http://localhost:8080"
$adminUsername = "api-admin"
$adminPassword = "secret123"

Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    username = $adminUsername
    password = $adminPassword
    nickname = "API Admin"
  } | ConvertTo-Json)
```

然后在本地 MySQL 中执行角色更新。命令会提示输入数据库密码，不要把密码写进仓库文件：

```powershell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u <local-db-user> -p weekend_go `
  -e "UPDATE users SET role='ADMIN' WHERE username='api-admin';"
```

完成后在 collection 中运行 `Auth / Login Admin` 获取 `adminToken`。

## 推荐调用顺序

1. `Health / Health Check`
2. `Auth / Register User`
3. `Auth / Login User`
4. `Auth / Get Me`
5. `Place Discovery / Search Workspaces` 或 `Place Discovery / Nearby Workspaces`
6. 如果搜索没有返回地点，手动准备地点并设置 `placeId`
7. `Place Discovery / Place Detail`
8. `Workspace Profile / Submit Profile Contribution`
9. `Auth / Login Admin`
10. `Workspace Profile / Approve Profile Contribution`
11. `Workspace Profile / Get Public Workspace Profile`
12. `Checkin / Submit Checkin`
13. `Checkin / Get Current Status`
14. `Reviews / Submit Review`
15. `Reviews / List Public Reviews Before Audit`
16. `Reviews / Admin Approve Review`
17. `Reviews / List Public Reviews After Audit`
18. `Favorites / Get Favorite Status`
19. `Favorites / Add Favorite`
20. `Favorites / List My Favorites`
21. `Favorites / Delete Favorite`
22. `Images / Submit Image`
23. `Images / List Public Images Before Audit`
24. `Images / Admin Approve Image`
25. `Images / List Public Images After Audit`
26. `Error Cases` 文件夹中的 401、403、404 验证
27. `Auth / Logout User`

## Amap 不稳定时的替代步骤

地点发现接口依赖 Amap Web Service Key、外网访问和 Amap IP 白名单。若搜索接口返回 `EXTERNAL_SERVICE_ERROR`、`INVALID_USER_IP` 或网络错误，可使用本地数据库准备演示地点：

```powershell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u <local-db-user> -p weekend_go `
  -e "INSERT INTO places (amap_poi_id, name, address, longitude, latitude, category, district, source, workspace_status) VALUES ('POSTMAN_DEMO_PLACE', 'Postman Demo Workspace', 'Demo Road', 116.300000, 39.900000, 'Library', 'Demo District', 'ADMIN_IMPORT', 'CANDIDATE') ON DUPLICATE KEY UPDATE name=VALUES(name); SELECT id FROM places WHERE amap_poi_id='POSTMAN_DEMO_PLACE';"
```

把查询出的 `id` 填入 Postman 环境变量 `placeId`，即可继续共建、打卡、评价、收藏、图片和审核链路。

## 清理演示数据

以下 SQL 只删除本验证文档建议创建的演示数据。执行前确认当前数据库是本地开发库：

```powershell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u <local-db-user> -p weekend_go `
  -e "DELETE FROM audit_logs WHERE target_type IN ('PROFILE_SUBMISSION','REVIEW','IMAGE') AND actor_user_id IN (SELECT id FROM users WHERE username IN ('api-admin')); DELETE FROM favorites WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'api-user-%' OR username='api-admin'); DELETE FROM place_images WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'api-user-%' OR username='api-admin'); DELETE FROM reviews WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'api-user-%' OR username='api-admin'); DELETE FROM checkins WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'api-user-%' OR username='api-admin'); DELETE FROM profile_submissions WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'api-user-%' OR username='api-admin'); DELETE FROM workspace_profiles WHERE place_id IN (SELECT id FROM places WHERE amap_poi_id='POSTMAN_DEMO_PLACE'); DELETE FROM places WHERE amap_poi_id='POSTMAN_DEMO_PLACE'; DELETE FROM users WHERE username LIKE 'api-user-%' OR username='api-admin';"
```

## PowerShell smoke 示例

以下示例验证健康检查、注册、登录和鉴权。不输出真实数据库密码或 token：

```powershell
$baseUrl = "http://localhost:8080"
$username = "api-user-$(Get-Date -Format yyyyMMddHHmmss)"
$password = "secret123"

Invoke-RestMethod -Method Get -Uri "$baseUrl/api/health"

Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    username = $username
    password = $password
    nickname = "API User"
  } | ConvertTo-Json)

$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    username = $username
    password = $password
  } | ConvertTo-Json)

$token = $login.data.token
Invoke-RestMethod -Method Get -Uri "$baseUrl/api/auth/me" -Headers @{
  Authorization = "Bearer $token"
}
```

## 覆盖范围

Collection 覆盖：

- health: `GET /api/health`
- auth: `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`
- place discovery: `GET /api/workspaces/search`、`GET /api/workspaces/nearby`、`GET /api/places/{placeId}`
- workspace profile: `POST /api/places/{placeId}/profile-submissions`、`GET /api/places/{placeId}/workspace-profile`、`POST /api/admin/profile-submissions/{submissionId}/approve`、`POST /api/admin/profile-submissions/{submissionId}/reject`
- checkin: `POST /api/places/{placeId}/checkins`、`GET /api/places/{placeId}/current-status`
- reviews: `POST /api/places/{placeId}/reviews`、`GET /api/places/{placeId}/reviews`、`PATCH /api/admin/reviews/{reviewId}/audit`
- favorites: `GET/POST/DELETE /api/places/{placeId}/favorite`、`GET /api/me/favorites`
- images: `POST /api/places/{placeId}/images`、`GET /api/places/{placeId}/images`、`PATCH /api/admin/images/{imageId}/audit`
- common errors: 未登录 401、普通用户访问 admin 403、不存在地点/共建/评价/图片 404 或当前实现等价响应

## 本次验证记录

- `python -m json.tool feature_list.json`: 通过。
- `python -m json.tool docs/api/weekend-go.postman_collection.json`: 通过。
- `git diff --check`: 通过。
- `cd backend; .\mvnw.cmd test`: 通过，43 tests, 0 failures。
- `cd backend; .\mvnw.cmd -DskipTests spring-boot:start`: 通过，后端本地启动成功。
- PowerShell `Invoke-RestMethod` 最小 HTTP smoke: 通过，覆盖 `GET /api/health`、`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` 和无 token `GET /api/auth/me` 返回 401。
- `cd backend; .\mvnw.cmd -DskipTests spring-boot:stop`: 通过，已停止本地后端。
- 真实 MySQL + Amap 全链路 smoke: 未完成。当前 worker 进程和 Windows 用户级环境未读到 `DB_USERNAME`、`DB_PASSWORD`、`AMAP_API_KEY`，且当前 worktree 不存在未跟踪的 `backend/src/main/resources/application-local.yml`。因此没有稳定凭据可连接真实 `weekend_go`，也没有可用于 Amap 搜索的 key；请按本文档准备环境后运行完整 collection。
