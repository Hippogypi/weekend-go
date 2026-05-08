# weekend-go 进度记录

## 当前阶段

auth persistence in progress

## 已完成

- 已建立 `feature_list.json + progress.md + git worktree` 协作机制。
- 已完成项目协作规范、README 和第一版 feature 拆分。
- 已合并基础工程：`backend-bootstrap`、`frontend-bootstrap`、`database-schema-design`。
- 已完成本地高德地图 Key 配置与可用性验证；真实 Key 未提交到 git。
- 已合并第二批后端基础能力：`auth-and-role`、`amap-service-integration`、`database-mysql-verification`。
- 已合并 `place-discovery`，提供地点发现、搜索、详情和 places 表持久化能力；该 feature 不包含前端页面。
- 已合并 `backend-data-access-standardization`，将 places 持久化迁移到 Spring 管理的 `JdbcTemplate + TransactionTemplate`，并保留无数据库配置时的默认可测试行为。

## 进行中

- `auth-persistence`：计划基于已标准化的数据访问样板，将认证用户从内存仓储迁移到 MySQL `users` 表。

## 下一步

- 为 `auth-persistence` 创建独立 worktree。
- 运行后端 baseline 测试。
- 分配 worker 实现 `auth-persistence`，coordinator 只负责范围说明、审查和合并。

## 阻塞与风险

- 高德 Web 服务 Key 依赖公网出口 IP 白名单，网络变化时可能需要更新。
- `auth-persistence` 需要保持当前注册、登录、退出、`/api/auth/me` 和鉴权过滤器契约不变。
- 真实 MySQL 运行验证仍需在后续接口联调阶段持续补充。
- 主仓库存在本地未跟踪 `.codex/` 配置目录，暂不纳入版本控制。

## 2026-05-09 backend-data-access-standardization coordinator review

- 已审查并合并 `backend-data-access-standardization` 到 `main`。
- 审查确认：后端新增 Spring 管理的 DataSource/JdbcTemplate 基础设施，仅在配置 `spring.datasource.url` 时启用。
- `place` 持久化已从直接 `DriverManager` 连接迁移为 `JdbcTemplate + TransactionTemplate`，保留现有接口行为。
- 默认无本地 MySQL 配置时仍使用 `UnconfiguredPlaceRepository`，为 `auth-persistence` 提供统一数据访问样板。
- 验证记录：worker 分支后端测试通过，23 tests, 0 failures；JSON 校验通过；敏感信息扫描未命中；业务代码未残留直接 `DriverManager/getConnection`。
