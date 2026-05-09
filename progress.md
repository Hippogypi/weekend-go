# weekend-go 进度记录

## 当前阶段

frontend and api verification planning

## 已完成

- 已建立 `feature_list.json + progress.md + git worktree` 协作机制。
- 已完成项目协作规范、README 和第一版 feature 拆分。
- 已合并基础工程：`backend-bootstrap`、`frontend-bootstrap`、`database-schema-design`。
- 已完成本地高德地图 Key 配置与可用性验证；真实 Key 未提交到 git。
- 已合并第二批后端基础能力：`auth-and-role`、`amap-service-integration`、`database-mysql-verification`。
- 已合并 `place-discovery`，提供地点发现、搜索、详情和 places 表持久化能力；该 feature 不包含前端页面。
- 已合并 `backend-data-access-standardization`，将 places 持久化迁移到 Spring 管理的 `JdbcTemplate + TransactionTemplate`，并保留无数据库配置时的默认可测试行为。
- 已合并 `workspace-profile-contribution`、`checkin-current-status`、`reviews-favorites-images`，后端 P0 业务闭环基本完成。

## 进行中

- 规划前端核心页面联调和接口验证：`frontend-core-pages`、`postman-api-verification`。

## 下一步

- 评估是否先启动 `frontend-core-pages`，再补 `postman-api-verification`，或两者并行推进。
- 后续联调阶段补充真实 MySQL 环境下的注册、登录、共建、打卡、评价、收藏、图片和审核端到端验证。

## 阻塞与风险

- 高德 Web 服务 Key 依赖公网出口 IP 白名单，网络变化时可能需要更新。
- `auth-persistence` 已保持当前注册、登录、退出、`/api/auth/me` 和鉴权过滤器契约不变；真实 MySQL 环境仍建议在后续联调中复验。
- 真实 MySQL 运行验证仍需在后续接口联调阶段持续补充。
- 主仓库存在本地未跟踪 `.codex/` 配置目录，暂不纳入版本控制。

## 2026-05-09 backend-data-access-standardization coordinator review

- 已审查并合并 `backend-data-access-standardization` 到 `main`。
- 审查确认：后端新增 Spring 管理的 DataSource/JdbcTemplate 基础设施，仅在配置 `spring.datasource.url` 时启用。
- `place` 持久化已从直接 `DriverManager` 连接迁移为 `JdbcTemplate + TransactionTemplate`，保留现有接口行为。
- 默认无本地 MySQL 配置时仍使用 `UnconfiguredPlaceRepository`，为 `auth-persistence` 提供统一数据访问样板。
- 验证记录：worker 分支后端测试通过，23 tests, 0 failures；JSON 校验通过；敏感信息扫描未命中；业务代码未残留直接 `DriverManager/getConnection`。
- 合并后后端完整测试通过：23 tests, 0 failures。

## 2026-05-09 auth-persistence allocation

- `auth-persistence` 状态更新为 `in-progress`。
- 该 feature 将在 `.worktrees/auth-persistence` 独立实现。
- 范围限制：只处理认证与用户持久化，不实现地点共建、打卡、评价、收藏、图片或前端页面。

## 2026-05-09 auth-persistence worker result

- 已新增 `JdbcUserAccountRepository`，使用 Spring 管理的 `JdbcTemplate + TransactionTemplate` 读写 MySQL `users` 表。
- 无 `spring.datasource.url` 时通过 `UserAccountRepositoryConfiguration` 保留 `InMemoryUserAccountRepository` fallback，默认测试/本地上下文可启动。
- 公开注册路径仍由 `AuthService` 固定创建 `USER`，密码仍通过既有 BCrypt encoder 哈希后写入仓储。
- 验证记录：`cd backend; .\mvnw.cmd test` 通过，28 tests, 0 failures；`python -m json.tool feature_list.json` 通过；`git diff --check` 通过。

## 2026-05-09 auth-persistence coordinator review

- 已审查并合并 `auth-persistence` 到 `main`。
- 审查确认：JDBC 用户仓储复用 Spring 管理的数据访问基础设施，未在业务仓储中直接创建 `DriverManager/getConnection`。
- 注册、登录、退出、`/api/auth/me`、鉴权过滤器和 USER/ADMIN 角色契约保持兼容。
- 验证记录：worker 分支后端测试通过，28 tests, 0 failures；JSON 校验通过；diff 空白检查通过；敏感信息扫描未命中。
- 剩余风险：本次未连接真实 MySQL 做注册登录端到端验证，后续联调阶段需要补充。

## 2026-05-09 feature date field

- `feature_list.json` 已新增 `completedAt` 字段，用于机器可读地记录 feature 完成日期。
- 已完成 feature 回填完成日期；未完成 feature 暂记为 `null`。
- `AGENTS.md` 已补充 `completedAt` 字段约定。

## 2026-05-09 P0 backend feature allocation

- 已决定并行启动 `workspace-profile-contribution`、`checkin-current-status`、`reviews-favorites-images`。
- 三个 feature 均依赖已完成的 `auth-and-role`、`auth-persistence` 和 `place-discovery`，当前依赖已满足。
- 分配边界：三项均只做后端能力，不实现前端页面；继续沿用 Spring 管理的 `JdbcTemplate + TransactionTemplate` 数据访问模式。
- 合并风险：三项可能都需要扩展地点详情或管理审核视图，worker 应尽量控制对共享 place/auth/common 代码的改动范围。

## 2026-05-09 P0 backend feature coordinator review

- 已审查并合并 `checkin-current-status`、`reviews-favorites-images`、`workspace-profile-contribution` 到 `main`。
- `checkin-current-status` 新增打卡提交和当前状态查询，最近 2 小时窗口聚合通过测试覆盖。
- `reviews-favorites-images` 新增评价、收藏、图片和审核接口，公开查询只暴露审核通过内容。
- `workspace-profile-contribution` 新增属性共建、审核和 workspace profile 聚合；地点详情和搜索结果兼容新增 nullable `workspaceProfile`。
- 合并冲突：仅 `GlobalExceptionHandler` 发生冲突，已手工保留 interaction、profile 和 `ResponseStatusException` 的异常映射。
- 验证记录：合并后三分支后端完整测试通过，43 tests, 0 failures；`feature_list.json` JSON 校验通过；diff 空白检查通过。
