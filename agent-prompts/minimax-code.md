# 给 MiniMax Code 的提示词

```text
你负责“小确幸 Android”的独立 UI、无障碍和文档同步任务，不修改数据层与媒体核心。

仓库：https://github.com/skydream9527-ctrl/happy_with_life
基线：最新 main
建议分支：minimax-settings-ui

开始前必须阅读：

- AGENTS.md
- agent-prompts/README.md
- README.md
- PROGRESS.md
- docs/reviews/completed-work-audit-2026-08-27.md
- docs/smoke-checklist.md
- docs/design-tokens.md
- docs/html-compose-diff.md
- design-reference/pages/profile.html

硬边界：

1. 只能在克隆后的仓库根目录内读取、修改和执行命令，禁止越过目录。
2. 禁止创建任何新目录；任务需要的目录若不存在，停止并报告。
3. 禁止修改 AppDatabase、Migrations、Entity、DAO、Repository、GP、streak、Achievement、DI、MediaPicker、MediaImporter、VoiceRecorder、RecordScreen、RecordViewModel、Gradle、Manifest、GitHub workflow 和 docs/server/。
4. 禁止本地运行 Gradle；测试、Lint 和 APK 只通过 GitHub Actions。

任务 A：设置、隐私和关于页面

- 在现有 ui/profile/ 目录内实现 SettingsScreen、PrivacyScreen、AboutScreen，不创建新的功能目录。
- 设置页提供跟随系统/浅色/深色选择；若持久化接口未提供，明确标为预览，不能伪造保存成功。
- 版本信息读取 BuildConfig，不硬编码版本号。
- 权限用途、本地数据、未来云同步、位置和分享说明准确；未实现能力标注“后续开放”，不放无响应按钮。
- ProfileScreen 与 AppNavigation 使用真实路由或明确 disabled/coming soon 状态。

任务 B：质量与文档

- 只用现有 Compose design token，不新增散落硬编码颜色。
- 支持深色模式、1.3x 字体和 TalkBack contentDescription。
- loading、empty、disabled 状态清楚。
- 在现有测试目录中补测试，或在现有 docs/smoke-checklist.md 中加入可执行检查项。
- 更新 README.md、PROGRESS.md 和 docs/smoke-checklist.md，使其与实际代码一致；没有真实 run URL 不得声称 CI 通过。

允许修改的现有范围：

- README.md、PROGRESS.md、docs/smoke-checklist.md
- app/src/main/java/com/xiaoquexing/app/ui/profile/
- app/src/main/java/com/xiaoquexing/app/navigation/AppNavigation.kt
- app/src/main/res/ 中必要的现有资源目录
- app/src/test/ 中与本任务直接相关的文件

禁止使用 git add . 或 git add -A，禁止提交任务外文件、密钥或真实用户数据。

交付：PR/commit、修改文件清单、页面截图或录屏、文档摘要、无障碍/暗色/大字体检查、GitHub Actions run URL 与未接入能力清单。
```
