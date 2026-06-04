# weekend-go 操作记录

> 本文件记录所有代码修改、配置变更和验证操作，按时间倒序排列。
> 所有 AI 窗口在执行修改后必须更新此文件。

---

## 2026-06-05 00:35

**角色**: coordinator
**操作类型**: 远程 `222` 同步后的文档对齐

### 背景

远程新增提交 `646560e 222`，内容涉及登录页背景图、首页重构、文件上传接口和前端图片上传调用。用户要求先不要提交到 Git，本轮仅同步远程、解决冲突、更新相关文档和验证。

### 同步与冲突处理

| 步骤 | 结果 |
|------|------|
| 本地保护 | 使用 stash 保存未提交改动 |
| 合入方式 | `git merge --ff-only origin/main`，本地 `main` 更新到 `646560e 222` |
| 冲突文件 | `ContributeReviewView.vue`、`HomeView.vue`、`PlaceDetailView.vue`、`ProfileView.vue` |
| 解决原则 | 保留 `222` 的上传/首页/登录页新能力，同时保留打卡/写评价入口口径和统一中文标签 |
| 暂存状态 | 冲突结算后已 `git restore --staged .`，当前无 staged 文件 |

### `222` 主要影响

| 文件/能力 | 说明 |
|-----------|------|
| `backend/src/main/java/com/weekendgo/upload/UploadController.java` | 新增 `POST /api/upload`，接收 multipart 文件并返回 `/uploads/{filename}` |
| `backend/src/main/java/com/weekendgo/common/config/WebConfig.java` | 新增 `/uploads/**` 静态资源映射 |
| `backend/src/main/resources/application.yml` | 配置 multipart 上传大小上限 10MB |
| `frontend/src/services/apiClient.ts`、`weekendGoApi.ts` | 新增 FormData 上传调用 |
| `frontend/src/views/ContributeReviewView.vue` | 写评价页从选择图片文件开始上传，并把返回路径随评价提交 |
| `frontend/public/login-bg.jpg`、`LoginView.vue`、`App.vue` | 登录页更新为背景图独立视觉 |
| `frontend/src/views/HomeView.vue` | 首页视觉结构大幅调整 |

### 文档更新

| 文件 | 说明 |
|------|------|
| `README.md` | 当前闭环和后端能力补充文件上传 |
| `docs/project-overview.md` | 新增 upload 模块、`POST /api/upload`、`/uploads/**` 和图片存储说明 |
| `docs/api/README.md` | 覆盖范围补充 upload 接口和最新验证 |
| `docs/service-development-report.md` | 课程报告更新评价图片上传链路和当前不足 |
| `docs/software-requirements-v2.md` | V2 需求更新图片上传、限制和后续扩展 |
| `feature_list.json` | `review-image-binding-frontend` note 记录真实文件上传升级 |
| `progress.md` | 记录本轮远程同步、冲突处理、文档更新和验证 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 前端测试 | `npm run test` 56 passed |
| 前端构建 | `npm run build` 通过 |
| 后端测试 | `.\mvnw.cmd test` 77 passed |
| 冲突标记 | `rg "<<<<<<<|=======|>>>>>>>"` 未命中 |
| 暂存区 | `git diff --cached --name-only` 为空 |

### 截图刷新

| 文件 | 说明 |
|------|------|
| `docs/screenshot/01-login.png` | 登录页，已反映 `222` 新背景图 |
| `docs/screenshot/02-home-discovery.png` | 地点发现首页，已反映 `222` 后首页结构 |
| `docs/screenshot/03-place-detail-overview.png` | 地点详情概况，使用当前有效地点 `100` |
| `docs/screenshot/04-place-detail-contribute.png` | 地点详情贡献入口，使用当前有效地点 `100` |
| `docs/screenshot/05-checkin.png` | 打卡页，使用当前有效地点 `100` |
| `docs/screenshot/06-review-upload.png` | 写评价 / 上传照片页，使用当前有效地点 `100` |
| `docs/screenshot/07-profile.png` | 个人中心 |
| `docs/screenshot/08-admin-dashboard.png` | 管理员审核工作台 |

### 风险与后续

- 本轮发现写评价页“添加地点照片”按钮在合并后文字挤压，已调整按钮宽度和字号并重截 `06-review-upload.png`。
- 本轮未提交 Git，仍保留工作区改动和本地日志文件。

---

## 2026-06-05 00:25

**角色**: coordinator
**操作类型**: 远程同步后文档与截图刷新

### 背景

2026-06-04 的产品口径调整、课程报告和截图补充开始前未先拉取远程更新。已在不丢失本地改动的前提下同步 `origin/main`，并基于同步后的当前版本重新做端到端盘点。

### 同步处理

| 步骤 | 结果 |
|------|------|
| `git fetch --prune origin` | 更新远程引用 |
| `git rev-list --left-right --count HEAD...origin/main` | 本地落后远程 1 个提交 |
| 远程提交 | `b0c0be9 111修复` |
| 本地保护 | 对已跟踪改动执行临时 stash |
| 合入方式 | `git merge --ff-only origin/main` |
| 恢复方式 | `git stash pop`，`HomeView.vue` 自动合并，无冲突 |

### 远程提交影响

| 文件 | 说明 |
|------|------|
| `frontend/src/views/HomeView.vue` | 搜索/附近完成后始终渲染地图；传入定位坐标作为地图中心点 |
| `frontend/src/components/MapView.vue` | 新增 `center` prop，地图默认中心优先使用定位坐标 |
| `frontend/.gitignore` | 忽略 `.env.local` |
| `frontend/package-lock.json` | 锁文件同步更新 |

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `docs/screenshot/01-login.png` ~ `08-admin-dashboard.png` | 覆盖 | 基于同步远程后的端到端服务重新截取 8 张页面素材 |
| `progress.md` | 修改 | 记录远程同步、地图行为和截图刷新 |
| `OPERATIONS_LOG.md` | 修改 | 记录本轮同步、验证和文档刷新 |
| `docs/project-overview.md` | 修改 | 更新最后更新日期、地图发现行为和最新验证数据 |
| `docs/service-development-report.md` | 修改 | 更新截图说明、运行展示和最新验证结果 |
| `docs/software-requirements-v2.md` | 修改 | 更新文档日期、地点发现需求和当前实现状态 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 服务状态 | MySQL80 运行中；后端 `/api/health` 返回 UP；前端 `5174` 可访问 |
| 浏览器 smoke | 普通用户：首页地图、详情、贡献入口、打卡、写评价、个人中心通过；管理员审核工作台通过 |
| API 状态 | 浏览器 smoke 期间未出现 API 4xx/5xx |
| 后端测试 | `.\mvnw.cmd test` 77 passed |
| 前端测试 | `npm run test` 56 passed |
| 前端构建 | `npm run build` 通过 |

---

## 2026-06-04 18:20

**角色**: coordinator
**操作类型**: 文档截图素材补充

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `docs/screenshot/01-login.png` ~ `08-admin-dashboard.png` | 新增 | 使用本地前端、后端和 MySQL 环境截取 8 张页面素材 |
| `docs/service-development-report.md` | 修改 | 将页面截图占位替换为 Markdown 图片引用；数据库关系图占位暂保留 |
| `docs/software-requirements-v2.md` | 修改 | 将页面截图占位替换为 Markdown 图片引用 |
| `progress.md` | 修改 | 记录截图素材补充情况 |
| `OPERATIONS_LOG.md` | 修改 | 记录本轮截图补充 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 截图生成 | 8 张 PNG 均已生成到 `docs/screenshot/` |
| 图片尺寸 | 均为 1365px 宽，非空文件 |
| 抽样视觉检查 | 首页和管理员审核截图内容正确 |

---

## 2026-06-04 14:45

**角色**: coordinator
**操作类型**: 课程报告与 V2 说明书草稿

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `docs/service-development-report.md` | 新增 | 生成服务开发技术课程实验报告 Markdown，按精简 6 章结构突出服务场景、资源分析、RESTful 设计、服务实现、运行展示和验证总结；截图均保留占位 |
| `docs/software-requirements-v2.md` | 新增 | 生成第二版软件需求说明书 Markdown，按当前实现更新业务边界、共建模型、接口范围、页面需求和实现状态 |
| `progress.md` | 修改 | 记录课程报告和 V2 说明书草稿生成情况 |
| `OPERATIONS_LOG.md` | 修改 | 记录本轮文档生成 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 文档存在性 | 两个新增 Markdown 文档已写入 `docs/` |
| 需求文档原文 | 未修改 `docs/城市学习办公空间共建平台_软件需求说明书.docx` |

---

## 2026-06-04 14:10

**角色**: coordinator
**操作类型**: 产品口径与入口文案对齐

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/services/displayLabels.ts` | 新增 | 统一地点状态、资料量、打卡状态、场景、久坐和审核状态的用户可见中文标签 |
| `frontend/src/services/displayLabels.test.ts` | 新增 | 按 TDD 覆盖系统枚举中文化；先红后绿 |
| `frontend/src/views/HomeView.vue` | 修改 | 首页去掉开发口吻文案，定位失败提示引导用户切换搜索，地点状态中文化 |
| `frontend/src/views/PlaceDetailView.vue` | 修改 | 贡献 tab 文案对齐：打卡 / 写评价上传照片；概况和评价中的系统枚举中文化 |
| `frontend/src/views/ContributeView.vue` | 修改 | 贡献选择页明确打卡和写评价/上传照片两类入口 |
| `frontend/src/views/ContributeCheckinView.vue` | 修改 | 打卡页弱化为到访记录，实时状态字段标记为可选 |
| `frontend/src/views/ContributeReviewView.vue` | 修改 | 写评价页明确承载上传照片和补充地点信息，提交状态中文化 |
| `frontend/src/views/ProfileView.vue` | 修改 | 我的打卡/评价中的系统枚举中文化 |
| `frontend/src/router/routes.ts` | 修改 | 路由标题同步为“打卡”和“写评价 / 上传照片” |
| `README.md`、`progress.md`、`docs/frontend-rules.md`、`docs/project-overview.md`、`feature_list.json` | 修改 | 同步产品口径：打卡不是主要共建入口；写评价/上传照片用于长期画像；打卡暂不支持图片 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 前端单元测试 | `npm run test` 通过 — 56 passed, 9 test files |
| JSON 校验 | `python -m json.tool feature_list.json` 通过 |
| 前端构建 | `npm run build` 通过 |
| 浏览器 smoke | 普通用户路径：首页、详情贡献 tab、打卡页、写评价页、个人中心通过；管理员 `/admin` 审核工作台通过 |
| diff 空白检查 | `git diff --check` 通过 |

---

## 2026-06-04 13:00

**角色**: coordinator
**操作类型**: 文档同步

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `README.md` | 修改 | 将项目阶段从 bootstrap 更新为 core feature complete / documentation sync，补充当前核心功能、目录结构和协作阶段说明 |
| `progress.md` | 修改 | 更新当前阶段，追加 2026-06-04 文档同步记录，并为早期 profile_submissions 记录补充后续重构说明 |
| `docs/frontend-rules.md` | 修改 | 将前端路由表同步到当前实际路由，说明属性共建已并入写评价流程，管理员入口为 `/admin` |
| `docs/api/README.md` | 修改 | 移除已废弃 `profile_submissions` 清理 SQL，补充当前 reviews、qa、map markers、admin workbench 覆盖范围 |
| `feature_list.json` | 修改 | 更新 currentPhase，并修正已完成 feature 中“等待启动”和旧 profile_submissions 口径的 notes |
| `OPERATIONS_LOG.md` | 修改 | 记录本轮文档同步 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| JSON 校验 | `python -m json.tool feature_list.json` 通过 |
| 旧引用复查 | 旧 bootstrap 阶段、旧前端页面路由、旧属性提交 API 和已删除表清理 SQL 已移除 |
| diff 空白检查 | `git diff --check` 通过 |

---

## 2026-05-15 20:30

**角色**: coordinator  
**操作类型**: 登录页独立布局执行

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/App.vue` | 修改 | 引入 `useRoute()`，模板 `v-if` 分支：`<div v-if="route.path === '/login'" class="login-shell">` 独立全屏渲染登录页，`v-else` 保留 `.app-shell` 侧边栏布局；新增 `<style scoped>` 定义 `.login-shell`（min-height: 100vh; padding: 28px） |
| `progress.md` | 修改 | 追加"2026-05-15 登录页独立布局"小节，记录需求、方案、修改文件、验证结果 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 前端单元测试 | ✅ 通过 — 53 passed, 8 test files |
| 前端构建 | ✅ 通过 — dist/ 生成成功 |

### 路由行为确认

- 未登录访问 `/` → 守卫重定向到 `/login` → **无侧边栏**
- 登录成功 → 跳回 `/` → **有侧边栏**
- 已登录访问 `/login` → 守卫拦截跳回 `/` → **有侧边栏**

---

## 2026-05-15 15:07

**角色**: coordinator  
**操作类型**: E2E 报告问题修复 + 布局优化计划确认

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/App.vue` | 修改 | 导航栏"审核"链接路由：`/admin/reviews` → `/admin`，修复 404 |
| `e2e-test-report.md` | 修改 | 标记导航路由错误为已修复，注明修复文件 |
| `progress.md` | 修改 | 追加"2026-05-15 E2E 测试报告问题修复"小节，记录问题、修复、验证事实 |

### 验证结果

| 检查项 | 结果 |
|--------|------|
| 前端单元测试 | ✅ 通过 — 53 passed, 8 test files |
| 前端构建 | ✅ 通过 — dist/ 生成成功 |

### 已确认计划（待执行）

- **登录页独立布局**：用户要求登录页（`/login`）与首页分开，登录成功后才能看到带侧边栏的首页。
- 方案：App.vue 条件渲染——`/login` 路由独立全屏，其余路由保留 `.app-shell` 侧边栏布局。
- 预计改动文件：`frontend/src/App.vue`
- 计划记录于 `progress.md`，文档更新规则已同步说明。

### 工作区状态说明

- 存在 1 个无关未提交改动：`backend/src/main/java/com/weekendgo/common/exception/GlobalExceptionHandler.java` 被修改（添加 `exception.printStackTrace()` 和错误消息增强）。按 AGENTS.md 规则保持原样。

---

## 2026-05-14 23:23

**角色**: coordinator  
**操作类型**: 项目现状确认 + 问题发现

### 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/weekendgo/interaction/InteractionService.java` | 修改 | 补充 `MyReviewResponse` 构造函数参数：seatScore/minConsumption/allowLongStay/suitableScenes/likeCount/replyCount/liked |
| `backend/src/main/java/com/weekendgo/interaction/MyReviewResponse.java` | 修改 | 新增上述 7 个字段的 record 定义 |
| `database/schema.sql` | 修改 | 移除 `ALTER TABLE ... ADD CONSTRAINT IF NOT EXISTS` 中的 `IF NOT EXISTS`（MySQL 不支持） |

### Git 提交
- Commit: `47bef89` — `fix: 补充 MyReviewResponse 共建/互动字段 + 修正 schema MySQL 语法`

### 清理的临时文件
- `backend/backend.log`
- `db_columns.txt`
- `schema_tables.txt`
- `login-status.png`
- `page-status.png`
- `place-detail-fixed.png`

### 发现的问题
- `database/schema.sql` 第 339–343 行存在冗余 `ALTER TABLE place_images ADD COLUMN review_id ...`，与 `CREATE TABLE place_images` 中的字段定义重复，导致 MySQL 导入报错 `Duplicate column name 'review_id'`。
- 待修复。

---

## 2026-05-14 23:23

**角色**: coordinator  
**操作类型**: 初始化操作日志

- 新建 `OPERATIONS_LOG.md`，建立全窗口共享的操作记录机制。

---

## 2026-05-14 23:35

**角色**: coordinator  
**操作类型**: 数据库-后端-前端完整检查

### 修复文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `database/schema.sql` | 删除冗余段 | 移除第 339–343 行重复的 `ALTER TABLE place_images ADD COLUMN review_id ...`，该段与 `CREATE TABLE place_images` 中的字段定义冲突，导致 MySQL 导入报错 `Duplicate column name 'review_id'` |

### 新建文件

| 文件 | 说明 |
|------|------|
| `OPERATIONS_LOG.md` | 全窗口共享操作记录文件（本文件） |

### Git 提交
- Commit: `5c9805d` — `fix: 移除 schema.sql 中 place_images.review_id 的重复 ALTER TABLE 定义`

### 检查结果汇总

| 检查项 | 结果 |
|--------|------|
| 数据库 schema 导入 | ✅ 通过 — 14 张表正确创建 |
| 数据库 dev_seed 导入 | ✅ 通过 — users×2, places×3, profiles×3, checkins×3, reviews×3, images×3, favorites×2 |
| 后端单元测试 | ✅ 通过 — 77 tests, 0 failures, BUILD SUCCESS |
| 前端单元测试 | ✅ 通过 — 53 passed, 8 test files |
| 前端构建 | ✅ 通过 — dist/ 生成成功 |
| 后端 local profile 启动 | ✅ 通过 — /api/health、注册、登录均正常 |

### 发现的问题

1. **application-local.yml 包含真实高德 API Key**：`backend/src/main/resources/application-local.yml` 第 9 行曾硬编码真实 Key（此处已脱敏）。该文件虽在 `backend/.gitignore` 中，但若已被 git 跟踪，仍可能被提交。建议从 git 历史中移除或轮换 Key。
2. **dev_seed.sql 重复执行会产生重复数据**：当前 seed 使用 `INSERT INTO` 而非 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE`，重复执行会导致 users/places 主键冲突（但其他关联表使用 INSERT IGNORE）。不影响开发使用，但记录为已知问题。
