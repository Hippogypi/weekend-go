# weekend-go 项目总览

> 本文档描述 weekend-go 项目当前的完整技术架构、模块组织和接口定义。
> 最后更新：2026-06-05

---

## 1. 项目概述

weekend-go 是一个基于位置服务的**城市学习办公空间共建平台**。用户可发现附近适合学习、阅读、远程办公的地点，并通过共建机制补充安静度、Wi-Fi、插座、座位、消费等场景化信息。

### 核心功能

- **地点发现**：关键词搜索 + 附近定位搜索（高德地图）；附近模式定位后以用户位置为地图中心，搜索/附近完成后即使暂无结果也保留地图基础视图。
- **地点详情**：概况（属性评分）、评价（排序/点赞/回复）、问大家（Q&A）、去贡献
- **用户共建**：写评价 / 上传照片是主要共建入口，用于沉淀多维评分、地点照片、客观属性和长期画像；打卡用于记录到访，也可顺手补充拥挤度、噪音和空座等实时状态。
- **个人中心**：我的收藏、我的打卡、我的评价、昵称编辑
- **审核工作台**：管理员对待审核评价/图片进行通过/驳回操作

---

## 2. 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.5 |
| Java 版本 | Java | 17 |
| 数据库 | MySQL | 8.0 |
| 数据访问 | Spring JDBC（原生 JDBC，无 JPA/MyBatis） | — |
| 安全 | Spring Security + 自定义 Bearer Token | — |
| 验证 | Jakarta Validation | — |
| 外部服务 | 高德地图 Web 服务 API | — |
| 前端框架 | Vue | 3.5.13 |
| 路由 | Vue Router | 4.5.0 |
| 构建工具 | Vite | 6.x |
| 测试 | Vitest（前端）/ Spring Boot Test（后端） | — |

---

## 3. 项目目录结构

```text
weekend-go/
├── AGENTS.md                    # AI 协作规范
├── README.md                    # 项目简介
├── feature_list.json            # 机器可读 feature 清单
├── progress.md                  # 人类可读进度记录
├── database/
│   ├── schema.sql               # MySQL 建表脚本（14 张表）
│   ├── dev_seed.sql             # 本地开发演示数据
│   └── README.md                # 数据库说明
├── docs/
│   ├── api/                     # API 设计文档
│   ├── superpowers/             # 需求文档
│   ├── frontend-rules.md        # 前端开发规范
│   └── project-overview.md      # 本文档
├── backend/
│   ├── src/main/java/com/weekendgo/    # Java 源码
│   ├── src/main/resources/             # 配置文件
│   ├── src/test/java/                  # 单元测试
│   ├── pom.xml                         # Maven 配置
│   └── mvnw / mvnw.cmd                 # Maven Wrapper
└── frontend/
    ├── src/                       # Vue 源码
    ├── dist/                      # 构建产物
    ├── index.html                 # HTML 入口
    ├── vite.config.ts             # Vite 配置
    ├── tsconfig.json              # TypeScript 配置
    └── package.json               # NPM 配置
```

---

## 4. 后端架构

### 4.1 模块组织（按业务域划分）

```
com.weekendgo
├── auth/                  # 认证与授权
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── AuthTokenStore.java
│   ├── BearerTokenAuthenticationFilter.java
│   ├── AuthenticatedUser.java
│   ├── UserAccount.java / UserRole.java
│   ├── JdbcUserAccountRepository.java / InMemoryUserAccountRepository.java
│   └── security/SecurityConfig.java
├── place/                 # 地点发现
│   ├── PlaceDiscoveryController.java
│   ├── PlaceDiscoveryService.java
│   ├── AmapService.java
│   ├── Place.java / PlaceResponse.java
│   ├── JdbcPlaceRepository.java / InMemoryPlaceRepository.java
│   └── ...
├── profile/               # 学习办公属性（Workspace Profile）
│   ├── WorkspaceProfileController.java
│   ├── WorkspaceProfileService.java
│   ├── WorkspaceProfile.java / AllowLongStay.java / TrustLevel.java
│   └── JdbcWorkspaceProfileRepository.java
├── checkin/               # 打卡与实时状态反馈
│   ├── CheckinController.java
│   ├── CheckinService.java
│   ├── CheckinRequest.java / CheckinResponse.java / CurrentStatusResponse.java
│   └── JdbcCheckinRepository.java / InMemoryCheckinRepository.java
├── interaction/           # 评价、图片、收藏、点赞、回复、审核
│   ├── InteractionController.java
│   ├── InteractionService.java
│   ├── ReviewRequest.java / ReviewResponse.java / MyReviewResponse.java
│   ├── ImageRequest.java / ImageResponse.java
│   ├── FavoriteResponse.java / FavoritePlaceResponse.java
│   ├── PendingAuditItem.java / AuditRequest.java / AuditStats.java
│   ├── ReviewReply.java / ReviewReplyRequest.java
│   └── JdbcInteractionRepository.java / InMemoryInteractionRepository.java
├── upload/                # 文件上传
│   └── UploadController.java
├── qa/                    # 问大家（Q&A）
│   ├── QaController.java
│   ├── QaService.java
│   ├── PlaceQa.java / QuestionRequest.java / AnswerRequest.java
│   └── JdbcQaRepository.java / InMemoryQaRepository.java
├── mapmarker/             # 地图标记
│   ├── MapMarkerController.java
│   ├── MapMarkerService.java
│   ├── MapMarkerResponse.java
│   └── JdbcMapMarkerRepository.java
├── admin/                 # 管理员接口（部分在 interaction 中）
│   ├── AdminController.java
│   └── AdminAuthController.java
├── amap/                  # 高德地图客户端
│   ├── AmapClient.java
│   ├── AmapConfiguration.java / AmapProperties.java
│   └── dto/...
├── common/                # 公共基础设施
│   ├── api/ApiResponse.java / ErrorResponse.java / PageResult.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── config/WebConfig.java
│   └── data/DataAccessConfiguration.java
└── health/                # 健康检查
    └── HealthController.java
```

### 4.2 设计模式

- **Repository 模式**：每个业务域定义 `*Repository` 接口，提供 `Jdbc*` 和 `InMemory*` 两种实现
- **配置回退**：`@ConditionalOnProperty(name = "spring.datasource.url")` 激活 JDBC 实现；无数据源时回退到内存实现
- **统一响应**：所有 Controller 返回 `ApiResponse<T>`（`{ success, code, message, data }`）
- **统一异常**：`GlobalExceptionHandler` 捕获业务异常并转换为标准错误响应

### 4.3 安全与认证

- **Token 机制**：自定义不透明 Token（非 JWT），内存存储，`wg_` 前缀 + Base64URL 编码
- **认证方式**：`Authorization: Bearer <token>`
- **会话策略**：`STATELESS`（无 Session）
- **密码加密**：`BCryptPasswordEncoder`
- **CORS**：开发环境允许所有来源 `*`

#### 公开接口（无需认证）

```
GET  /api/health
POST /api/auth/register
POST /api/auth/login
GET  /api/workspaces/nearby
GET  /api/workspaces/search
GET  /api/places/**
GET  /api/reviews/{reviewId}/replies
GET  /api/questions/{questionId}/answers
GET  /api/map/markers
```

#### 管理员接口（需 `ROLE_ADMIN`）

```
GET    /api/admin/audits/pending-list
GET    /api/admin/audits/stats
GET    /api/admin/auth-check
PATCH  /api/admin/reviews/{reviewId}/audit
PATCH  /api/admin/images/{imageId}/audit
```

---

## 5. 前端架构

### 5.1 目录组织

```
frontend/src/
├── views/                 # 页面级组件（9 个）
│   ├── HomeView.vue           # 地点发现首页
│   ├── PlaceDetailView.vue    # 地点详情（4 Tab）
│   ├── ContributeView.vue     # 贡献入口
│   ├── ContributeCheckinView.vue   # 打卡页
│   ├── ContributeReviewView.vue    # 写评价页
│   ├── ProfileView.vue        # 个人中心
│   ├── AdminDashboardView.vue # 审核工作台
│   ├── AdminReviewView.vue    # 旧版单条审核（未注册路由）
│   └── LoginView.vue          # 登录/注册
├── components/            # 可复用组件
│   ├── MapView.vue            # 高德地图封装，支持定位中心点与 marker 展示
│   └── ToastContainer.vue     # 全局通知容器
├── composables/           # 组合式函数
│   ├── useAsyncAction.ts      # 异步请求状态封装
│   ├── useApiError.ts         # 统一错误处理
│   ├── useToast.ts            # Toast 通知系统
│   └── useAuthRedirect.ts     # 登录后重定向
├── services/              # API 封装与状态管理
│   ├── apiClient.ts           # 底层 HTTP 客户端（fetch 封装）
│   ├── weekendGoApi.ts        # 业务 API（34 个方法）
│   ├── session.ts             # 认证状态中心（localStorage 持久化）
│   └── index.ts               # 统一导出
├── router/                # 路由
│   ├── index.ts               # 路由实例 + 导航守卫
│   ├── routes.ts              # 路由表（8 条路由）
│   └── routes.test.ts         # 路由测试
├── styles/
│   └── base.css               # 全局基础样式
├── App.vue                # 根布局（侧边栏 + 内容区 + Toast）
└── main.ts                # 入口（先恢复登录态，再挂载应用）
```

### 5.2 状态管理

**未使用 Pinia/Vuex**，状态管理由两部分组成：

1. **`session.ts`** — 全局响应式认证状态
   - `token` / `user` / `isLoggedIn` / `isAdmin`
   - 持久化到 `localStorage`
   - 启动时自动 `restoreSession()` 验证 token 有效性

2. **`weekendGoApi.ts`** — 业务 API 集中封装
   - 通过 `ApiClient` 统一处理 baseUrl、Token 注入、响应解包
   - 定义全量 TypeScript 接口类型

### 5.3 路由表

| 路径 | 视图 | 需认证 | 需管理员 |
|------|------|--------|----------|
| `/` | HomeView | ✅ | — |
| `/places/:placeId` | PlaceDetailView | ✅ | — |
| `/places/:placeId/contribute` | ContributeView | ✅ | — |
| `/places/:placeId/contribute/checkin` | ContributeCheckinView | ✅ | — |
| `/places/:placeId/contribute/review` | ContributeReviewView | ✅ | — |
| `/profile` | ProfileView | ✅ | — |
| `/admin` | AdminDashboardView | ✅ | ✅ |
| `/login` | LoginView | — | — |

**导航守卫逻辑：**
1. 动态设置 `document.title`
2. 已登录访问 `/login` → 重定向到 `/`
3. 需认证未登录 → 重定向到 `/login?redirect={path}`
4. 需管理员非管理员 → Toast 提示并返回 `/`

---

## 6. 数据库设计

### 6.1 表清单（14 张）

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `users` | 用户账号 | `username`, `password_hash`, `role`, `nickname`, `enabled` |
| `places` | 地点（高德 POI） | `amap_poi_id`, `name`, `address`, `longitude`, `latitude`, `source`, `workspace_status` |
| `workspace_profiles` | 学习办公聚合属性 | `place_id`, `quiet_score`, `wifi_score`, `socket_score`, `seat_score`, `cost_score`, `score`, `trust_level` |
| `checkins` | 打卡与实时状态反馈 | `place_id`, `user_id`, `crowd_level`, `noise_level`, `has_seat`, `remark` |
| `reviews` | 评价（含共建属性） | `place_id`, `user_id`, `quiet_score`, `wifi_score`, `socket_score`, `comfort_score`, `cost_score`, `seat_score`, `content`, `audit_status`, `like_count`, `reply_count` |
| `review_likes` | 评价点赞 | `review_id`, `user_id` |
| `review_replies` | 评价回复 | `review_id`, `user_id`, `content` |
| `place_qa` | 地点问答 | `place_id`, `user_id`, `type`, `parent_id`, `content`, `answer_count` |
| `place_images` | 地点图片 | `place_id`, `user_id`, `review_id`, `image_url`, `description`, `audit_status` |
| `place_tags` | 地点标签 | `place_id`, `tag_id`, `user_id`, `source`, `audit_status` |
| `tags` | 标签字典 | `tag_name`, `tag_group`, `enabled`, `sort_order` |
| `favorites` | 用户收藏 | `user_id`, `place_id` |
| `audit_logs` | 审核日志 | `target_type`, `target_id`, `admin_id`, `action`, `reason` |
| `search_keywords` | 搜索关键词字典 | `keyword`, `enabled`, `sort_order` |

### 6.2 核心关系

```
users (1) ───< reviews (N) >─── places (1)
users (1) ───< checkins (N) >─── places (1)
users (1) ───< place_images (N) >─── places (1)
users (1) ───< place_qa (N) >─── places (1)
users (1) ───< favorites (N) >─── places (1)
users (1) ───< review_likes (N) >─── reviews (1)
users (1) ───< review_replies (N) >─── reviews (1)
places (1) ───(1) workspace_profiles
reviews (1) ───< place_images (N)  [review_id]
place_tags >─── tags
```

### 6.3 状态枚举

| 枚举 | 取值 |
|------|------|
| `workspace_status` | `CANDIDATE`, `PENDING`, `APPROVED`, `REJECTED` |
| `audit_status` | `PENDING`, `APPROVED`, `REJECTED`, `DELETED` |
| `crowd_level` | `FREE`, `NORMAL`, `CROWDED`, `FULL` |
| `noise_level` | `QUIET`, `RELATIVELY_QUIET`, `NORMAL`, `NOISY`, `VERY_NOISY` |
| `allow_long_stay` | `TRUE`, `FALSE`, `UNKNOWN` |
| `source` | `AMAP_SEARCH`, `USER_SUBMIT`, `ADMIN_IMPORT` |
| `role` | `USER`, `ADMIN` |
| `trust_level` | `LOW`, `MEDIUM`, `HIGH` |

### 6.4 演示数据

`dev_seed.sql` 提供 2 个演示账号：

| 用户名 | 角色 | 密码 |
|--------|------|------|
| `api-user-demo` | `USER` | `secret123` |
| `api-admin-demo` | `ADMIN` | `secret123` |

---

## 7. 前后端接口对照表

### 7.1 认证接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `register` | POST | `/auth/register` | `POST /api/auth/register` | 注册 |
| `login` | POST | `/auth/login` | `POST /api/auth/login` | 登录，返回 Token |
| `logout` | POST | `/auth/logout` | `POST /api/auth/logout` | 注销 |
| `me` | GET | `/auth/me` | `GET /api/auth/me` | 获取当前用户信息 |
| `updateNickname` | PATCH | `/auth/me` | `PATCH /api/auth/me` | 修改昵称 |

### 7.2 地点发现接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `searchPlaces` | GET | `/workspaces/search` | `GET /api/workspaces/search` | 关键词搜索 |
| `nearbyPlaces` | GET | `/workspaces/nearby` | `GET /api/workspaces/nearby` | 附近搜索 |
| `placeDetail` | GET | `/places/{placeId}` | `GET /api/places/{placeId}` | 地点详情 |
| `workspaceProfile` | GET | `/places/{placeId}/workspace-profile` | `GET /api/places/{placeId}/workspace-profile` | 公开属性 |

### 7.3 打卡接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `submitCheckin` | POST | `/places/{placeId}/checkins` | `POST /api/places/{placeId}/checkins` | 提交打卡 |
| `currentStatus` | GET | `/places/{placeId}/current-status` | `GET /api/places/{placeId}/current-status` | 当前实时状态 |
| `myCheckins` | GET | `/me/checkins` | `GET /api/me/checkins` | 我的打卡历史 |

### 7.4 评价与互动接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `submitReview` | POST | `/places/{placeId}/reviews` | `POST /api/places/{placeId}/reviews` | 写评价 |
| `getReviews` | GET | `/places/{placeId}/reviews` | `GET /api/places/{placeId}/reviews` | 公开评价列表 |
| `likeReview` | POST | `/reviews/{reviewId}/likes` | `POST /api/reviews/{reviewId}/likes` | 点赞 |
| `unlikeReview` | DELETE | `/reviews/{reviewId}/likes` | `DELETE /api/reviews/{reviewId}/likes` | 取消点赞 |
| `getReplies` | GET | `/reviews/{reviewId}/replies` | `GET /api/reviews/{reviewId}/replies` | 获取回复 |
| `createReply` | POST | `/reviews/{reviewId}/replies` | `POST /api/reviews/{reviewId}/replies` | 回复评价 |
| `myReviews` | GET | `/me/reviews` | `GET /api/me/reviews` | 我的评价 |

### 7.5 图片接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `uploadFile` | POST | `/upload` | `POST /api/upload` | 上传图片文件，返回 `/uploads/{filename}` |
| `submitImage` | POST | `/places/{placeId}/images` | `POST /api/places/{placeId}/images` | 提交图片 |
| `images` | GET | `/places/{placeId}/images` | `GET /api/places/{placeId}/images` | 地点图片列表 |

当前写评价 / 上传照片页先通过 multipart `POST /api/upload` 上传本地图片文件，后端保存到 `backend/uploads/` 并返回 `/uploads/...` 路径；随后前端把该路径和图片描述随评价提交，图片记录仍进入审核流程。`POST /api/places/{placeId}/images` 保留为兼容的独立图片提交接口。

### 7.6 收藏接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `favoriteStatus` | GET | `/places/{placeId}/favorite` | `GET /api/places/{placeId}/favorite` | 收藏状态 |
| `addFavorite` | POST | `/places/{placeId}/favorite` | `POST /api/places/{placeId}/favorite` | 添加收藏 |
| `removeFavorite` | DELETE | `/places/{placeId}/favorite` | `DELETE /api/places/{placeId}/favorite` | 取消收藏 |
| `favorites` | GET | `/me/favorites` | `GET /api/me/favorites` | 我的收藏 |

### 7.7 问大家（Q&A）接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `createQuestion` | POST | `/places/{placeId}/questions` | `POST /api/places/{placeId}/questions` | 提问 |
| `getQuestions` | GET | `/places/{placeId}/questions` | `GET /api/places/{placeId}/questions` | 获取问题列表 |
| `createAnswer` | POST | `/questions/{questionId}/answers` | `POST /api/questions/{questionId}/answers` | 回答 |
| `getAnswers` | GET | `/questions/{questionId}/answers` | `GET /api/questions/{questionId}/answers` | 获取回答列表 |

### 7.8 管理员审核接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `auditReview` | PATCH | `/admin/reviews/{reviewId}/audit` | `PATCH /api/admin/reviews/{reviewId}/audit` | 审核评价 |
| `auditImage` | PATCH | `/admin/images/{imageId}/audit` | `PATCH /api/admin/images/{imageId}/audit` | 审核图片 |
| `pendingList` | GET | `/admin/audits/pending-list` | `GET /api/admin/audits/pending-list` | 待审核列表 |
| `auditStats` | GET | `/admin/audits/stats` | `GET /api/admin/audits/stats` | 审核统计 |

### 7.9 其他接口

| 前端方法 | HTTP | 前端路径 | 后端路径 | 说明 |
|----------|------|----------|----------|------|
| `mapMarkers` | GET | `/map/markers` | `GET /api/map/markers` | 地图标记点 |

首页附近模式会调用 `GET /api/map/markers`，并将浏览器定位坐标传给地图组件作为中心点；关键词搜索仍调用 `GET /api/workspaces/search` 发现候选地点。

---

## 8. 开发环境配置

### 8.1 后端配置

```yaml
# backend/src/main/resources/application-local.yml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  datasource:
    url: jdbc:mysql://localhost:3306/weekend_go?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}

weekend-go:
  amap:
    api-key: <your-amap-web-service-key>
```

### 8.2 前端配置

```
# frontend/.env.local
VITE_API_BASE_URL=http://localhost:8080/api
VITE_AMAP_JS_API_KEY=<your-amap-js-api-key>
VITE_AMAP_SECURITY_JS_CODE=<your-amap-security-code>
```

### 8.3 高德 Key 说明

需要两个独立的高德 Key：
- **Web端(JS API) Key**：前端加载地图组件使用 → `frontend/.env.local`
- **Web服务 Key**：后端调用高德 REST API 使用 → `backend/application-local.yml`

---

## 9. 常用命令

### 后端

```bash
# 编译
cd backend && ./mvnw.cmd compile

# 运行（local profile，PowerShell 需用 --%）
cd backend && ./mvnw.cmd spring-boot:run --% -Dspring-boot.run.profiles=local

# 测试
cd backend && ./mvnw.cmd test
```

### 前端

```bash
# 开发服务器
cd frontend && npm run dev

# 构建
cd frontend && npm run build

# 测试
cd frontend && npm run test
```

### 数据库

```powershell
# 导入 schema
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p weekend_go -e "source database/schema.sql"

# 导入演示数据
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p weekend_go -e "source database/dev_seed.sql"
```

---

## 10. 测试覆盖

| 层级 | 测试文件 | 数量 |
|------|----------|------|
| 后端单元测试 | `backend/src/test/java/...` | 77 tests |
| 前端单元测试 | `frontend/src/**/*.test.ts` | 56 tests |

最近一次本地端到端 smoke 覆盖普通用户首页地图、地点详情、贡献入口、打卡、写评价、个人中心，以及管理员审核工作台；浏览器流程期间未出现 API 4xx/5xx。

---

## 11. 注意事项

1. **Token 存储**：后端使用内存存储 Token（`ConcurrentHashMap`），重启后所有登录态失效
2. **前端 baseUrl**：`VITE_API_BASE_URL` 需包含 `/api` 前缀，与后端 Controller 的 `@RequestMapping` 对应
3. **审核状态**：公开接口只返回 `APPROVED` 状态的数据，管理员可查看 `PENDING`
4. **图片存储**：当前支持本地文件上传，文件保存到后端 `uploads/` 目录并通过 `/uploads/**` 静态访问；数据库仍保存图片 URL/路径和描述
5. **Workspace Profile**：代码从 `reviews` 表实时聚合计算，不直接读写 `workspace_profiles` 表
