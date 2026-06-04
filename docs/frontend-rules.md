# weekend-go 前端开发规则

本规则基于《城市学习办公空间共建平台_软件需求说明书》和现有前端工程实践制定，所有前端 worker 必须遵守。规则编号格式为 `FR-{领域}-{序号}`。

---

## 1. 页面路由与权限规则（FR-Router）

### FR-Router-01 路由与页面需求对应

前端路由表必须与需求文档第13节"前端页面需求"一一对应：

| 页面 | 路由 | 需求对应 | 优先级 |
|------|------|---------|--------|
| 首页/地图列表页 | `/` | P-01, P-02, P-06 | P0 |
| 地点详情页 | `/places/:placeId` | D-01 ~ D-05 | P0 |
| 贡献选择页 | `/places/:placeId/contribute` | C-01, C-04, K-01, R-03, I-01 | P0 |
| 打卡页 | `/places/:placeId/contribute/checkin` | K-01 ~ K-05 | P0 |
| 写评价 / 上传照片页 | `/places/:placeId/contribute/review` | C-01, C-04, R-01 ~ R-05, I-01 ~ I-04 | P0/P1 |
| 个人中心页 | `/profile` | U-03, U-04 | P1 |
| 管理员审核工作台 | `/admin` | A-01 ~ A-05 | P1 |
| 登录/注册页 | `/login` | U-01, U-02 | P0 |

当前前端不再提供单独的“属性共建页”。产品口径为：打卡 = 到访记录 / 可选实时状态反馈，不作为主要共建入口，且当前不支持打卡上传图片；写评价 / 上传照片 = 主要共建入口，评价、评分、地点照片、Wi-Fi、插座、座位等客观属性都从这里进入，用于沉淀地点长期画像。管理员审核口径统一为评价和图片。

首页附近模式必须以浏览器定位坐标作为地图中心点；搜索/附近查询完成后，即使当前结果为空，也应保留地图基础视图，避免页面主体变成空白。

### FR-Router-02 路由 Meta 字段

所有路由必须定义 `meta` 字段：

```ts
interface RouteMeta {
  title: string;           // 页面标题，用于 document.title
  requiresAuth?: boolean;  // 是否需要登录
  requiresAdmin?: boolean; // 是否需要管理员角色
}
```

### FR-Router-03 未登录拦截

全局 `router.beforeEach` 守卫：
- 目标路由 `meta.requiresAuth === true` 且用户未登录 → 跳转 `/login?redirect=当前完整路径`
- 跳转前不阻断页面加载，通过导航守卫统一处理

### FR-Router-04 管理员权限拦截

全局 `router.beforeEach` 守卫：
- 目标路由 `meta.requiresAdmin === true` 且用户非管理员 → 跳转首页并显示权限不足提示
- 禁止仅在 UI 层隐藏管理员入口，必须依赖路由守卫和后端 `ROLE_ADMIN` 校验

### FR-Router-05 登录后重定向

登录成功后：
- 若 URL 存在 `redirect` 查询参数 → 自动跳转回该路径
- 若不存在 → 默认跳转首页 `/`

### FR-Router-06 已登录用户防回退

已登录用户访问 `/login` → 自动跳转首页 `/`，避免重复登录。

---

## 2. 认证与自动登录规则（FR-Auth）

### FR-Auth-01 应用启动自动验证

应用启动时（`main.ts`），若 `localStorage` 存在有效 token，自动调用 `weekendGoApi.me()` 向后端验证会话有效性。

### FR-Auth-02 自动登录成功

`me()` 返回 200 时：
- 刷新 `sessionStore.user` 为最新用户信息
- 保持登录状态，用户无感知

### FR-Auth-03 自动登录失败静默处理

`me()` 返回 401/403 或网络错误时：
- 调用 `sessionStore.clearSession()` 清除本地 token
- 静默进入未登录状态
- **不得**弹出错误提示阻断页面加载
- **不得**跳转登录页（用户可能只是在浏览公开页面）

### FR-Auth-04 运行时 401 统一处理

任何 API 调用返回 401 时：
- 自动调用 `sessionStore.clearSession()`
- 跳转 `/login`
- 可通过 `router.currentRoute` 携带 `redirect` 参数

### FR-Auth-05 Token 持久化

- 使用 `localStorage` 持久化，key 为 `weekend-go-session`
- 禁止在组件中直接操作 `localStorage`，统一通过 `sessionStore`

### FR-Auth-06 角色安全

- 公开注册只能创建 `USER` 角色（后端已保证）
- 前端不得提供创建管理员账号的入口

---

## 3. 组件架构规则（FR-Component）

### FR-Component-01 Views 目录职责

`views/` 仅放置页面级组件：
- 每个组件对应一个路由
- 负责数据获取、页面级状态管理和布局
- 不负责可复用 UI 的详细实现

### FR-Component-02 Components 目录职责

`components/` 仅放置可复用 UI 组件：
- 纯展示组件（如 `MapView.vue`）
- 跨页面复用的交互组件（如 `ToastContainer.vue`、`LoadingSpinner.vue`、`EmptyState.vue`）
- 不得包含业务数据获取逻辑

### FR-Component-03 Composables 目录职责

`composables/` 放置可复用逻辑：
- 共享的异步操作封装（`useAsyncAction`）
- 共享的错误处理（`useApiError`）
- 认证相关逻辑（`useAuthRedirect`）
- 每个 composable 必须有对应的 `.test.ts`

### FR-Component-04 代码行数限制

单一 View 组件超过 **300 行**时，必须拆分为子组件或提取 composable。

### FR-Component-05 脚本风格

所有组件统一使用 `<script setup lang="ts">`，禁用 Options API。

---

## 4. API 调用与错误处理规则（FR-API）

### FR-API-01 统一 API 入口

所有 HTTP 调用必须通过 `weekendGoApi` 或 `apiClient`，禁止直接调用原生 `fetch`。

### FR-API-02 异步操作三态

每个异步操作必须维护三个响应式状态：
```ts
const loading = ref(false);
const error = ref<string | null>(null);
const data = ref<T | null>(null);
```

### FR-API-03 统一错误消息

错误处理使用 `useApiError` composable，将 `ApiError` 转换为人类可读消息：

| HTTP 状态码 | 前端提示消息 |
|-------------|-------------|
| 400 | 请求参数错误，请检查输入 |
| 401 | 登录已过期，请重新登录 |
| 403 | 权限不足，无法执行此操作 |
| 404 | 资源不存在或已被删除 |
| 409 | 操作冲突，请刷新后重试 |
| 415 | 文件格式不支持 |
| 502 | 外部位置服务暂时不可用 |
| 500 | 系统异常，请稍后重试 |
| 网络错误 | 网络异常，请检查连接 |

### FR-API-04 ~ FR-API-08 状态码处理

- **401**：自动登出 + 跳转登录页（FR-API-04）
- **403**：显示权限提示，保留当前页（FR-API-05）
- **404**：显示资源不存在提示（FR-API-06）
- **502**：显示"外部位置服务暂时不可用"（FR-API-07）
- **网络断开/超时**：显示"网络异常，请检查连接"（FR-API-08）

---

## 5. 状态管理规则（FR-State）

### FR-State-01 认证状态集中管理

使用 `sessionStore`（`services/session.ts`）统一管理认证状态，禁止组件直接读写 `localStorage`。

### FR-State-02 表单状态局部化

表单状态使用组件内 `ref`/`reactive`，提交成功后清空。复杂表单可提取为 composable。

### FR-State-03 数据缓存策略（第一版）

列表和详情数据不做全局缓存，每次路由进入重新获取。第一版简化，后续可引入 SWR 或 Pinia 缓存。

### FR-State-04 临时共享状态

跨组件临时状态（如 toast 消息、全局 loading）使用 provide/inject 或轻量级事件总线，不引入 Pinia/Vuex。

---

## 6. UI/UX 交互规则（FR-UX）

### FR-UX-01 Loading 状态

所有表单提交按钮：
- `loading` 状态下 `disabled`
- 显示"处理中..."代替原按钮文字

### FR-UX-02 操作反馈颜色

- 成功：绿色 `.notice.success`
- 失败：红色 `.notice.error`
- 警告：黄色 `.notice.warning`

### FR-UX-03 空状态

列表或数据为空时必须有明确提示，禁止显示空白。例如：
- "暂无收藏地点"
- "暂无近期反馈"
- "没有找到匹配地点"

### FR-UX-04 未登录操作引导

未登录用户点击需登录的操作（收藏、提交共建、打卡、评价）：
- 显示登录提示
- 提供"去登录"按钮，跳转 `/login?redirect=当前路径`
- 不得直接弹出 401 错误

### FR-UX-05 页面标题

通过路由 `meta.title` + `router.beforeEach` 动态设置 `document.title`，格式：
```
{页面标题} - weekend-go
```

### FR-UX-06 响应式断点

移动端断点为 `780px`（`base.css` 已定义），所有页面必须在此断点下正常可用。

---

## 7. 地图组件规则（FR-Map）

### FR-Map-01 Key 配置

高德 JS API Key 从 `import.meta.env.VITE_AMAP_JS_API_KEY` 读取，禁止写死在代码中。

### FR-Map-02 Marker 生命周期

`MapView.vue` 接收 `places` prop：
- 每次 `places` 变化时，先清除旧 marker，再添加新 marker
- 避免 marker 重复累积

### FR-Map-03 无数据兜底

地图在无数据时展示默认中心视野（如当前城市中心），不能报错或空白。

### FR-Map-04 Marker 交互

Marker 点击展示地点名称 infoWindow，支持点击跳转到 `/places/:placeId`。

---

## 8. 安全规则（FR-Security）

### FR-Security-01 密钥管理

禁止在代码中写死任何密钥、token、数据库密码。所有配置通过环境变量注入。

### FR-Security-02 环境变量规范

前端可访问的环境变量必须以 `VITE_` 前缀暴露。敏感配置（如后端数据库密码）留在后端，不得暴露给前端。

### FR-Security-03 XSS 防护

- Vue 模板默认转义已足够
- 禁止对用户提供的内容使用 `v-html`
- 如需富文本展示，必须经过后端审核和前端 DOMPurify 过滤（第一版不涉及富文本）

### FR-Security-04 管理员操作确认

管理员执行删除、驳回等不可逆操作时，必须有二次确认（如 `confirm()` 对话框）。

---

## 9. 测试规则（FR-Test）

### FR-Test-01 Composable 测试

新增 composable 必须附带 `.test.ts`，使用 Vitest + 内存 localStorage mock。

### FR-Test-02 View 组件测试

新增 View 组件鼓励测试关键交互（表单提交、路由跳转），不强制要求，但核心页面（登录、首页、详情）建议覆盖。

### FR-Test-03 API Mock

API 相关测试使用 mock fetcher，不得依赖真实后端服务。

### FR-Test-04 提交门槛

所有测试必须通过 `npm run test` 方可提交。

---

## 附录：快速检查清单

Worker 完成 feature 前，对照以下清单自查：

- [ ] 新增/修改的路由有完整的 `meta` 字段
- [ ] 需要登录的页面已设置 `meta.requiresAuth`
- [ ] 管理员页面已设置 `meta.requiresAdmin`
- [ ] 异步操作使用 `useAsyncAction` 或手动维护 loading/error/data
- [ ] 错误处理使用 `useApiError`，无重复的 `describeError` 逻辑
- [ ] 未直接操作 `localStorage`，认证通过 `sessionStore`
- [ ] 无写死的密钥或敏感配置
- [ ] 空状态有明确提示
- [ ] `npm run test` 通过
- [ ] `npm run build` 通过
