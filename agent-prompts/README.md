# Agent 提示词索引

本目录是本项目唯一的 Agent 提示词存放位置。正式执行前，所有 Agent 都必须先阅读仓库根目录的 `AGENTS.md`。

| 文件 | 使用对象 | 当前用途 |
|---|---|---|
| `z-code.md` | Z code | 重要/复杂：媒体、录音、Room 与可靠性 |
| `minimax-code.md` | MiniMax Code | 独立 UI、设置/隐私/关于与文档同步 |
| `gork-server.md` | Gork | 独立阿里云服务端仓库 S0/S1 |
| `first-round-archive.md` | 仅供追溯 | 第一轮历史任务，不再直接执行 |

## 统一执行约束

- 唯一 Android 仓库：`https://github.com/skydream9527-ctrl/happy_with_life`
- 基线：最新 `main`；Agent 使用独立任务分支和 PR，除非用户明确要求直接提交 `main`。
- 工作边界：只在当前仓库根目录内工作，不访问父目录或相邻目录。
- 目录约束：不得创建任何新目录；缺少目录时停止并报告。
- 构建约束：只使用 GitHub Actions，不在本地执行 Gradle 或打包。
- 提交约束：精确暂存任务文件，禁止 `git add .` 和 `git add -A`。

建议顺序：Z code 先处理关键可靠性；MiniMax Code 可在不触碰数据/媒体核心文件的前提下并行；Gork 只在独立服务端仓库执行。
