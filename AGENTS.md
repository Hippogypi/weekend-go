# weekend-go AI 协作规则

本文件是 weekend-go 项目所有 AI 窗口长期遵守的协作规范。除非用户明确要求英文，否则默认使用中文交流；技术术语、分支名、feature id、命令和代码标识保留英文原文。

## 项目协作机制

本项目使用 `feature_list.json + progress.md + git worktree` 管理开发。

- `feature_list.json` 是机器可读任务清单，记录 feature 的 id、状态、分支、worktree、scope、依赖、验收标准和检查命令。
- `progress.md` 是人类可读进度记录，记录当前阶段、已完成事项、下一步、阻塞项和风险。
- git worktree 用于后续并行开发。一个 feature 对应一个 branch、一个 worktree、一个 AI thread。
- 当前尚未进入多 worktree 并行开发时，不要擅自创建 worktree。

## 角色职责

### Coordinator

- 维护 `feature_list.json` 和 `progress.md` 的全局视图。
- 拆分 feature，判断依赖关系和并行开发顺序。
- 决定何时创建 worktree、分配 worker、审查结果和合并分支。
- 审查 worker 对 feature 状态变更的建议，避免任务状态漂移。

### Worker

- 只在自己负责的 feature scope 内工作。
- 只更新自己 feature 相关的小节或状态建议，避免修改其他 feature 的内容。
- 不负责合并到 main，不擅自调整全局开发顺序。
- 发现需求、依赖或验收标准不清晰时，先向 coordinator 说明，不自行扩大范围。

## 开发边界

- 未经明确授权，不要创建或切换 git worktree。
- 未经明确授权，不要初始化 Vue 前端、Spring Boot 后端或数据库工程。
- 未经明确授权，不要实现业务功能。
- 不要修改 `docs/` 下的需求文档原文；如需提炼内容，应写入新的设计或计划文档。
- 不要随意重命名 feature id、branch 名或 worktree 路径。
- feature id、branch 名、worktree 路径统一使用英文 kebab-case。
- 修改代码或文档前先阅读相关文件，确认当前状态和已有约定。

## 文件维护规则

- `feature_list.json` 字段名使用英文，内容可以使用中文。
- `progress.md` 保持简洁，优先记录事实、下一步、阻塞项和风险。
- README 用于说明项目目标、目录结构和当前开发流程。
- 如果需求文档或关键文件无法读取，应说明原因，不要编造内容。

## 提交与验证

- 每个 feature 应有清晰的验收标准和检查命令。
- 修改完成后运行与本次变更匹配的检查命令。
- 不要回滚或覆盖他人未授权的改动。
- 发现工作区已有无关改动时，保持原样并在总结中说明。
