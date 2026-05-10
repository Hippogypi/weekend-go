# weekend-go 进度记录

## 当前阶段

frontend verification

## 2026-05-10 auth-frontend-enhancement 合并

- 已合并 `auth-frontend-enhancement` 到 `main`。
- 审查确认：新增独立 `/login` 路由和 `LoginView.vue`，支持注册后自动登录；应用启动时自动验证 token（`restoreSession`）；全局 `beforeEach` 路由守卫处理未登录拦截、管理员拦截、已登录防回退；登录成功后按 `redirect` 参数回到原页面。
- 新增 `composables/` 目录：`useAsyncAction`、`useApiError`、`useAuthRedirect`、`useToast` 及对应的 `.test.ts`。
- 新增 `ToastContainer.vue` 全局通知组件。
- `ProfileView.vue` 移除内嵌登录表单，改为纯个人信息 + 收藏列表。
- `HomeView.vue`、`PlaceDetailView.vue`、`AdminReviewView.vue` 使用 composables 重构重复错误处理逻辑。
- 新增前端开发规则文档 `docs/frontend-rules.md`，覆盖路由、认证、组件架构、API 错误处理、状态管理、UI/UX、地图、安全、测试 9 大规则章节。
- 验证记录：`npm run test` 45 passed / 8 test files；`npm run build` 通过；`git diff --check` 通过；未提交真实密钥或敏感配置。
- 新增依赖 `jsdom`（用于路由守卫测试的 DOM 环境）。

## 已完成

- 已建立 `feature_list.json + progress.md + git worktree` 协作机制。
- 已完成项目协作规范、README 和第一版 feature 拆分。
- 已合并基础工程：`backend-bootstrap`、`frontend-bootstrap`、`database-schema-design`。
- 已完成本地高德地图 Key 配置与可用性验证；真实 Key 未提交到 git。
- 已合并第二批后端基础能力：`auth-and-role`、`amap-service-integration`、`database-mysql-verification`。
- 已合并 `place-discovery`，提供地点发现、搜索、详情和 places 表持久化能力；该 feature 不包含前端页面。
- 已合并 `backend-data-access-standardization`，将 places 持久化迁移到 Spring 管理的 `JdbcTemplate + TransactionTemplate`，并保留无数据库配置时的默认可测试行为。
- 已合并 `workspace-profile-contribution`、`checkin-current-status`、`reviews-favorites-images`，后端 P0 业务闭环基本完成。
- 已向本机真实 `weekend_go` 注入本地开发演示数据：演示账号、地点、公开属性、近期打卡、评价、图片和收藏。

## 进行中

- 准备真实后端、本地 MySQL、高德服务与前端浏览器端到端 smoke。

## 下一步

- 使用已注入的演示数据补充真实 MySQL + Amap + 浏览器端到端 smoke，覆盖登录、搜索、详情、共建、打卡、评价、收藏、图片和审核。

## 阻塞与风险

- 高德 Web 服务 Key 依赖公网出口 IP 白名单，网络变化时可能需要更新。
- `auth-persistence` 已保持当前注册、登录、退出、`/api/auth/me` 和鉴权过滤器契约不变；真实 MySQL 环境仍建议在后续联调中复验。
- 真实 MySQL 运行验证仍需在后续接口联调阶段持续补充。

## 2026-05-10 frontend-backend smoke fix

- 修复 `apiClient.ts` 中 `fetch` 绑定丢失导致的 `Illegal invocation` 错误。
- 后端 `SecurityConfig` 补充 CORS 配置，允许本地 dev server 跨域访问。
- 本地配置文件修正：`frontend/.env.local` 补 `/api` 后缀；`backend/application-local.yml` 补 `spring.datasource.url` 及凭据。
- 根目录 `.gitignore` 补充 `.codex/`。
- 验证结果：前端 `npm run test` 12 passed；后端 `mvnw test` 43 passed；浏览器端到端 `地点发现 -> 查询地点` 请求链路打通，前后端与本地 MySQL 联调成功。

## 2026-05-09 frontend-core-pages allocation

- 已启动 `frontend-core-pages`，分支为 `frontend-core-pages`，worktree 为 `.worktrees/frontend-core-pages`。
- 范围限制：只实现 Vue 前端页面、状态处理和后端接口联调；不修改后端业务功能，不提交真实密钥或本地敏感配置。
- 依赖基础：`frontend-bootstrap`、主要后端 P0 API、`postman-api-verification` 均已完成，可按 `docs/api` 的接口契约推进。
- 验收重点：搜索/附近地点、详情、登录注册、共建、打卡、评价、图片、收藏、管理员审核页面形成可演示闭环，且 `npm run test` 与 `npm run build` 通过。

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

## 2026-05-09 local-database-setup allocation

- 已启动 `local-database-setup`，用于建立可持续使用的本地 MySQL `weekend_go` 开发库。
- 范围限制：只处理本地数据库创建、`database/schema.sql` 导入、本地未提交配置和真实 MySQL 连接验证；不新增业务功能，不提交真实密码或本地敏感配置。
- 该 feature 是后续 `postman-api-verification` 和前端联调的前置基础。

## 2026-05-09 local-database-setup worker result

- 已确认本机 `mysql` client 为 MySQL 8.0.43，`MySQL80` 服务运行中。
- 已在本机真实 MySQL 建立 `weekend_go` 数据库，字符集/排序规则为 `utf8mb4` / `utf8mb4_0900_ai_ci`。
- 已创建本地应用账号 `weekend_go` 并授予 `weekend_go.*` 权限；真实密码只通过本机环境变量使用，未写入可提交文件。
- 已导入 `database/schema.sql`；验证得到 12 张表，`tags` 10 条，`search_keywords` 9 条。
- 已复制被 Git 忽略的 `backend/src/main/resources/application-local.yml`，内容保留 `${DB_USERNAME}` / `${DB_PASSWORD}` 占位。
- 真实 MySQL smoke：`AuthControllerTest` 在 `local` profile 下通过，Hikari 已建立 MySQL Connector/J 连接，5 tests、0 failures。
- smoke 验证写入的假用户已从本地 `users` 表清理，保留空的可持续开发库。
- 后端普通测试：`cd backend; .\mvnw.cmd test` 通过，43 tests、0 failures。
- 注意：沙箱内运行 Maven 时当前 JDK `java.security` 文件访问被拒绝，测试使用审批后的沙箱外命令执行；这不是项目代码问题。

## 2026-05-09 local-database-setup review fix

- coordinator 复跑 smoke 时未设置 `DB_USERNAME` / `DB_PASSWORD`，因此 `application-local.example.yml` 的 `change-me` fallback 导致真实 MySQL 认证失败；已将后端 README 和 feature notes 改为明确要求当前 shell 或用户级环境变量提供本地凭据。
- 已把 `database/README.md` 中新增的 schema 导入命令改为仓库根目录可执行的通用写法：`mysql ... weekend_go < database/schema.sql`。
- 修复后已在当前命令中显式设置 `DB_USERNAME=weekend_go` 和本地 `DB_PASSWORD` 复跑 `AuthControllerTest` local profile smoke，5 tests、0 failures；Hikari 建立 MySQL Connector/J 连接；测试用户已再次清理为 0。

## 2026-05-09 postman-api-verification allocation

- 已启动 `postman-api-verification`，用于整理第一版主要 REST API 的验证集合或等价接口测试说明。
- 范围限制：只做接口验证资产、调用顺序、环境变量说明和验证记录；不新增业务功能，不提交真实数据库密码、Amap Key 或登录 token。
- 该 feature 应基于已完成的 `local-database-setup`，覆盖账号、地点、共建、打卡、评价、收藏、图片和审核链路。

## 2026-05-09 postman-api-verification worker result

- 已新增 `docs/api/weekend-go.postman_collection.json`，使用 Postman collection 变量串起普通用户 token、管理员 token、`placeId`、共建提交 id、评价 id 和图片 id，不包含真实密码、Amap Key 或登录 token。
- 已新增 `docs/api/README.md`，记录环境变量、后端启动、管理员账号准备、Amap 不稳定时手动准备 `placeId`、调用顺序、清理 SQL、PowerShell smoke 示例和覆盖范围。
- Collection 覆盖 health、register/login/logout/me、place search/nearby/detail、workspace profile submit/public/approve/reject、checkin submit/current-status、review submit/list/audit、favorite get/add/delete/list、image submit/list/audit，以及未登录 401、普通用户访问 admin 403、不存在资源 404 或当前实现等价响应。
- 验证记录：`python -m json.tool feature_list.json` 通过；`python -m json.tool docs/api/weekend-go.postman_collection.json` 通过；`git diff --check` 通过；`cd backend; .\mvnw.cmd test` 通过，43 tests, 0 failures。
- 本地后端最小 HTTP smoke 已通过：`spring-boot:start` 启动后，`GET /api/health`、`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` 和无 token `GET /api/auth/me` 401 均符合预期；随后已执行 `spring-boot:stop`。
- 真实 MySQL + Amap 全链路 smoke 未完成：当前 worker 进程和 Windows 用户级环境未读到 `DB_USERNAME`、`DB_PASSWORD`、`AMAP_API_KEY`，且当前 worktree 不存在未跟踪的 `application-local.yml`；已在 API 文档中记录人工环境前置和替代步骤。

## 2026-05-09 postman-api-verification coordinator review

- 已审查并合并 `postman-api-verification` 到 `main`。
- 审查确认：接口验证资产未包含真实数据库密码、Amap Key 或登录 token；collection 覆盖第一版主要 API 和常见错误链路。
- 验证记录：`feature_list.json` JSON 校验通过；Postman collection JSON 校验通过；diff 空白检查通过；后端完整测试通过，43 tests, 0 failures。
- 剩余风险：真实 MySQL + Amap 全链路仍依赖本机 local profile、Amap Key 和公网 IP 白名单，后续前端联调时继续复核。
## 2026-05-10 frontend-core-pages worker result

- 已实现 Vue 核心页面联调闭环：地点搜索/附近查询、地点详情、登录注册、属性共建、打卡反馈、评价提交与公开展示、图片提交与公开展示、收藏/取消收藏/收藏列表、管理员审核入口。
- 已新增前端业务 API 封装与 session store；API base URL 继续使用 `VITE_API_BASE_URL` fallback 到 `http://localhost:8080/api`，未写入真实密钥、token、DB 密码或 Amap Key。
- 已补充 loading、empty、error、未登录和权限不足提示；管理员页支持按返回 id 审核 profile submission、review 和 image。
- 已补充前端测试覆盖 `ApiClient` 响应解包、PATCH/DELETE、业务 API URL/body、Bearer token 和 session 持久化/异常清理。
- 验证通过：`cd frontend; npm run test`，4 test files / 12 tests passed；`cd frontend; npm run build` 通过。
- 遗留风险：本次仅完成前端构建与单元测试，未在真实 MySQL + Amap + 后端运行环境下做浏览器端到端 smoke；管理员待审核列表后端当前没有 list pending 接口，因此页面提供按提交返回 id 审核的入口。

## 2026-05-10 map-marker-display allocation

- 已启动 `map-marker-display`，分支为 `map-marker-display`，worktree 为 `.worktrees/map-marker-display`。
- 范围限制：只在前端地点发现页面集成高德地图 JS API，以 marker 展示搜索/附近结果；不修改后端接口契约，不提交真实密钥或本地敏感配置。
- 依赖基础：`frontend-core-pages`（地点发现页面已完成）、`place-discovery`（后端已返回 longitude/latitude）。
- 验收重点：地图容器正确加载、marker 按经纬度定位、点击可查看地点名称、`npm run test` 与 `npm run build` 通过。

## 2026-05-10 map-marker-display coordinator review

- 已审查并合并 `map-marker-display` 到 `main`。
- 审查确认：新增 `MapView.vue` 组件动态加载高德 JS API，搜索/附近结果以 marker 形式展示在地图上；marker 可点击显示地点名称；地图在无数据时也展示默认中心视野。
- 修复内容：补充 `watch immediate: true` 确保首次挂载即加载地图；调整 `v-if` 条件确保搜索后始终显示地图；dev server 绑定 `127.0.0.1` 以兼容高德白名单。
- 验证记录：`npm run test` 12 passed；`npm run build` 通过；浏览器端到端地图与 marker 渲染正常。
- 剩余事项：高德 Web 端 Key 白名单需包含 `127.0.0.1` 或留空，以消除 `INVALID_USER_DOMAIN` 控制台提示。

## 2026-05-10 frontend-core-pages coordinator review

- 已审查并合并 `frontend-core-pages` 到 `main`。
- 审查确认：改动集中在 `frontend/` 与本 feature 进度记录；未修改后端业务功能，未提交真实密钥、数据库密码、Amap Key 或 token。
- 功能覆盖：地点搜索/附近查询、详情、登录注册、属性共建、打卡、评价、图片、收藏、收藏列表和管理员按 id 审核入口。
- 验证记录：`npm run test` 通过，4 test files / 12 tests；`npm run build` 通过；`git diff --check` 通过。
- 剩余风险：尚未做真实 MySQL + Amap + 浏览器端到端 smoke；管理员待审核列表依赖后端后续补充 pending list API。

## 2026-05-10 local-dev-seed-data

- 已新增 `database/dev_seed.sql`，用于给本地真实 `weekend_go` 注入可重复执行的非生产演示数据。
- 已建立 `docs/frontend-rules.md` 前端开发规则体系，覆盖 9 大规则章节。
- 已完成前端认证体验增强：独立登录页、自动登录、路由守卫、共享 composables。
- 已直接导入本机真实 MySQL `weekend_go`，未把真实数据库凭据写入仓库。
- 演示账号：`api-user-demo` / `api-admin-demo`，演示密码均为 `secret123`。
- 验证结果：演示账号 2 个、演示地点 3 个、`workspace_profiles` 3 条、最近 2 小时打卡 3 条、审核通过评价 2 条、审核通过图片 2 条、演示收藏 2 条。
- 已更新 `database/README.md` 和 `docs/api/README.md`，记录导入方式、演示账号和用途。
