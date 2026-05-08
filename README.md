# weekend-go

weekend-go 是一个基于位置服务的城市学习办公空间共建平台。项目目标是帮助用户发现附近适合学习、阅读、远程办公或临时办公的地点，并通过用户共建补充安静度、Wi-Fi、插座、座位、消费、实时打卡、评价、图片和标签等场景化信息。

当前项目处于 `parallel bootstrap ready` 阶段。`backend-bootstrap` 分支已初始化 Spring Boot 后端工程；前端和业务功能仍由后续 feature 或其他 worktree 处理。

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
- `backend/`：Spring Boot 后端工程目录，包含基础启动类、统一响应骨架、健康检查和本地配置示例。
- `frontend/`：后续 Vue 前端工程目录，目前尚未初始化。

## 当前开发流程

本项目采用 `feature_list.json + progress.md + git worktree` 机制管理开发。

1. coordinator 根据需求文档拆分 feature，并维护 `feature_list.json`。
2. coordinator 在 `progress.md` 中记录当前阶段、已完成事项、下一步、阻塞项和风险。
3. 进入并行开发阶段后，每个 feature 对应一个 branch、一个 worktree、一个 AI thread。
4. worker 只在自己负责的 worktree 和 feature scope 内实现，不负责合并到 main。
5. coordinator 负责审查 worker 结果、更新总览、处理合并和后续拆分。

当前阶段暂不创建 git worktree，暂不实现业务功能，暂不初始化 Vue 或 Spring Boot 工程。
