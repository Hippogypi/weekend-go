# weekend-go

weekend-go 是一个基于位置服务的城市学习办公空间共建平台。项目目标是帮助用户发现附近适合学习、阅读、远程办公或临时办公的地点，并通过用户共建补充安静度、Wi-Fi、插座、座位、消费、实时打卡、评价、图片和标签等场景化信息。

当前项目处于 `first bootstrap merged` 阶段，已完成协作规范、数据库 schema、Spring Boot 后端骨架和 Vue 前端骨架初始化，尚未实现具体业务功能。

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
├── backend/
├── frontend/
└── docs/
    └── 城市学习办公空间共建平台_软件需求说明书.docx
```

- `AGENTS.md`：所有 AI 窗口长期遵守的协作规则。
- `feature_list.json`：机器可读 feature 清单，由 coordinator 维护。
- `progress.md`：人类可读项目进度记录，由 coordinator 维护总览。
- `docs/`：需求文档和后续设计文档。需求文档原文不得随意修改。
- `database/`：MySQL schema 和数据库说明。
- `backend/`：Spring Boot 后端工程骨架，包含统一响应、异常处理和健康检查。
- `frontend/`：Vue 前端工程骨架，包含基础布局、路由占位和 API client。

## 本地高德地图配置

项目本地开发需要区分两个高德 Key：

- `Web端(JS API)` Key：供 Vue 前端加载高德地图 JS API 使用，应写入 `frontend/.env.local`。
- `Web服务` Key：供 Spring Boot 后端调用高德 REST API 使用，应写入 `backend/src/main/resources/application-local.yml` 或本机环境变量。

真实 Key、数据库密码和本地覆盖配置不得提交到 Git。当前本地验证结果：

- 前端 JS API Key 已可从 `http://127.0.0.1:5173` 加载高德脚本。
- 后端 Web服务 Key 已通过高德行政区查询接口验证。
- 后端 Web服务 Key 的 IP 白名单绑定的是公网出口 IP；如果网络环境变化并出现 `INVALID_USER_IP`，需要在高德控制台更新白名单。

## 当前开发流程

本项目采用 `feature_list.json + progress.md + git worktree` 机制管理开发。

1. coordinator 根据需求文档拆分 feature，并维护 `feature_list.json`。
2. coordinator 在 `progress.md` 中记录当前阶段、已完成事项、下一步、阻塞项和风险。
3. 进入并行开发阶段后，每个 feature 对应一个 branch、一个 worktree、一个 AI thread。
4. worker 只在自己负责的 worktree 和 feature scope 内实现，不负责合并到 main。
5. coordinator 负责审查 worker 结果、更新总览、处理合并和后续拆分。

当前阶段已进入多 worktree 协作。后续业务功能应继续按 feature 拆分，在独立 branch/worktree 中实现并由 coordinator 审查合并。
