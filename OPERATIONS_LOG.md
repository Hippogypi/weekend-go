# weekend-go 操作记录

> 本文件记录所有代码修改、配置变更和验证操作，按时间倒序排列。
> 所有 AI 窗口在执行修改后必须更新此文件。

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

1. **application-local.yml 包含真实高德 API Key**：`backend/src/main/resources/application-local.yml` 第 9 行硬编码了 `api-key: c5445443dd77303953b7b5e86a5d3913`。该文件虽在 `backend/.gitignore` 中，但因已被 git 跟踪，仍会被提交。建议从 git 历史中移除或轮换 Key。
2. **dev_seed.sql 重复执行会产生重复数据**：当前 seed 使用 `INSERT INTO` 而非 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE`，重复执行会导致 users/places 主键冲突（但其他关联表使用 INSERT IGNORE）。不影响开发使用，但记录为已知问题。

