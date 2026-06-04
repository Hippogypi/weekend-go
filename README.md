# weekend-go

weekend-go 是一个基于位置服务的城市学习办公空间共建平台。项目目标是帮助用户发现附近适合学习、阅读、远程办公或临时办公的地点，并通过用户评价/上传照片沉淀安静度、Wi-Fi、插座、座位、消费和场景标签等长期画像；打卡用于记录到访，也可顺手补充实时人流、噪音和座位状态。

当前项目处于 `core feature complete / product wording alignment` 阶段。第一版核心闭环已经完成：用户注册登录、地点发现、地点详情、打卡、评价/上传照片、文件上传、收藏、问大家、评价点赞/回复、个人中心和管理员审核工作台均已有前后端实现，并通过本地 MySQL 联调 smoke、后端测试、前端测试和前端构建验证。首页附近模式会以浏览器定位坐标作为地图中心点，搜索/附近完成后即使暂无结果也保留地图基础视图。当前重点是保持产品口径、文档、feature 记录和实际代码一致。

## 技术方向

- 后端：Spring Boot
- 前端：Vue
- 数据库：MySQL
- 外部位置服务：高德地图 Web 服务 API
- 接口验证：Postman

## 目录结构

```text
weekend-go/
├── AGENTS.md
├── README.md
├── feature_list.json
├── progress.md
├── OPERATIONS_LOG.md
├── e2e-test-report.md
├── backend/
├── database/
├── frontend/
└── docs/
    ├── 城市学习办公空间共建平台_软件需求说明书.docx
    ├── project-overview.md
    ├── service-development-report.md
    ├── software-requirements-v2.md
    ├── frontend-rules.md
    ├── screenshot/
    └── api/
```

- `AGENTS.md`：所有 AI 窗口长期遵守的协作规则。
- `feature_list.json`：机器可读 feature 清单，由 coordinator 维护。
- `progress.md`：人类可读项目进度记录，由 coordinator 维护总览。
- `OPERATIONS_LOG.md`：跨窗口共享的修改、验证和风险记录。
- `e2e-test-report.md`：本地端到端联调验证报告。
- `docs/`：需求文档、项目总览、课程报告、第二版需求说明、截图素材、前端规则和 API 验证文档。需求文档原文不得随意修改。
- `database/`：MySQL schema 和数据库说明。
- `backend/`：Spring Boot 后端工程，包含认证授权、地点发现、共建聚合、打卡、评价互动、文件上传、问大家、地图标记和管理员审核接口。
- `frontend/`：Vue 前端工程，包含登录页、地点发现、详情标签页、贡献流程、个人中心和管理员工作台。

## 本地高德地图配置

项目本地开发需要区分两个高德 Key：

- `Web端(JS API)` Key：供 Vue 前端加载高德地图 JS API 使用，应写入 `frontend/.env.local`。
- `Web服务` Key：供 Spring Boot 后端调用高德 REST API 使用，应写入 `backend/src/main/resources/application-local.yml` 或本机环境变量。

真实 Key、数据库密码和本地覆盖配置不得提交到 Git。当前本地验证结果：

- 前端 JS API Key 已可从本地 Vite dev server 加载高德脚本，常用地址为 `http://127.0.0.1:5173` 或验证时指定的其他端口。
- 后端 Web服务 Key 已通过高德行政区查询接口验证。
- 后端 Web服务 Key 的 IP 白名单绑定的是公网出口 IP；如果网络环境变化并出现 `INVALID_USER_IP`，需要在高德控制台更新白名单。

## 当前开发流程

本项目采用 `feature_list.json + progress.md + git worktree` 机制管理开发。

1. coordinator 根据需求文档拆分 feature，并维护 `feature_list.json`。
2. coordinator 在 `progress.md` 中记录当前阶段、已完成事项、下一步、阻塞项和风险。
3. 进入并行开发阶段后，每个 feature 对应一个 branch、一个 worktree、一个 AI thread。
4. worker 只在自己负责的 worktree 和 feature scope 内实现，不负责合并到 main。
5. coordinator 负责审查 worker 结果、更新总览、处理合并和后续拆分。

第一版核心 feature 已全部合并到 `main`。后续新增功能或较大重构仍应按 feature 拆分，在独立 branch/worktree 中实现并由 coordinator 审查合并；小型文档同步或验证记录可由 coordinator 直接在主工作区处理。
