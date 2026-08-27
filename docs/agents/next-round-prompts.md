# 下一轮 Z code / MiniMax Code 分工与提示词

> 共同分支基线：`codex/server-architecture-and-sync`。
>
> 共同约定：只认 GitHub Actions，不要求本地 Gradle 打包。
>
> 执行顺序：先让 Z code 完成 CI/数据层稳定；MiniMax 可并行做文档和独立设置页，但不得触碰 Z 的核心文件。

## 分工总表

| Agent | 本轮范围 | 禁止范围 |
|---|---|---|
| Z code | GitHub CI 首跑修复、Room schema 基线、迁移/事务测试、MediaPicker/录音可靠性 | 设置/关于/隐私页面、服务端实现 |
| MiniMax Code | 文档更新、设置/关于/隐私 UI、Profile 导航、暗色/无障碍 | DB/DAO/Repository/GP/DI/MediaPicker/Gradle/CI |
| Gork | 独立 `xiaoquexing-server` 的 S0/S1 | Android 代码 |

## 给 Z code 的提示词

```text
你负责“小确幸 Android”下一轮关键路径：CI 稳定、Room v2 基线、媒体与录音可靠性。

仓库：
https://github.com/skydream9527-ctrl/xiaoquexing-android

工作分支：
codex/server-architecture-and-sync

开始前必须阅读：

- docs/reviews/completed-work-audit-2026-08-27.md
- docs/adr/ADR-001-domain-rules.md
- docs/adr/ADR-002-dependency-injection.md
- docs/room-v2-schema.md
- 当前 GitHub Draft PR 的 Actions 日志

任务 A：让当前变更通过 GitHub CI

1. 检查 unit-test、lint、assemble 三个 Job。
2. 修复实际编译、测试、Room schema 或 lint 问题，不能跳过测试或关闭 lint 规则掩盖错误。
3. 生成并提交 app/schemas/.../2.json Room schema 基线。
4. 确认 MIGRATION_1_2 的结果与 Room Entity 完全一致。
5. 确认所有 JVM/Robolectric 测试在 GitHub Actions 通过。
6. 更新 docs/room-v2-schema.md 的 CI 状态与真实限制。

任务 B：媒体与录音可靠性

1. 重构 MediaPicker：不要在 Composable 生命周期中直接调用 Activity.registerForActivityResult；使用 rememberLauncherForActivityResult 或生命周期安全封装。
2. Photo Picker 选图后仍由 MediaImporter 复制私有目录；失败可重试，历史记录不能只依赖短期 content URI。
3. 相机取消/失败时删除空临时文件。
4. 录音最长 60 秒自动停止；过短或 recorder.stop 失败时删除损坏文件。
5. App 旋转、退后台、离开页面时安全停止/释放录音资源。
6. 使用 Media3 实现真实播放/暂停/进度/播放结束；同时只允许一个播放器。
7. 补单元或 Robolectric 测试；设备相关场景补入 smoke checklist 的待人工项。

允许修改：

- .github/workflows/build.yml
- app/build.gradle.kts（仅必要依赖/测试配置）
- app/schemas/
- data/db、data/repository、data/media、di
- media/MediaPicker.kt
- ui/components/VoiceRecorder.kt
- RecordViewModel.kt
- 与媒体接线直接相关的 RecordScreen.kt
- docs/room-v2-schema.md 和媒体相关测试

禁止修改：

- MiniMax 正在处理的 ProfileScreen、Settings/About/Privacy 页面和 AppNavigation 新路由
- 服务端 docs/server 文档和任何服务端实现
- 画册、共享空间、支付、AI

硬性约束：

- 禁止 fallbackToDestructiveMigration。
- 禁止删除/跳过失败测试。
- 禁止使用 git add . 或 git add -A；只提交本任务文件。
- 只认 GitHub Actions；没有 run URL 不得声称构建通过。
- 不把 secret、keystore 或真实用户数据提交到 GitHub。

交付：

- GitHub Actions run URL 与三个 Job 结果。
- 修改文件清单。
- 修复的失败原因。
- Room schema/migration 结论。
- 媒体/录音状态机和剩余设备测试项。
- 风险与下一轮建议。
```

## 给 MiniMax Code 的提示词

```text
你负责“小确幸 Android”下一轮独立 UI 与文档同步任务，不修改数据层和媒体核心。

仓库：
https://github.com/skydream9527-ctrl/xiaoquexing-android

工作基线：
codex/server-architecture-and-sync

开始前阅读：

- docs/reviews/completed-work-audit-2026-08-27.md
- README.md
- PROGRESS.md
- docs/smoke-checklist.md
- docs/design-tokens.md
- docs/html-compose-diff.md
- xiaoquexing-ios-redesign 中对应的 profile 设计（如果仓库没有该目录，只参考 design-tokens）

任务 A：文档同步到真实 I1 状态

更新 README.md、PROGRESS.md、docs/smoke-checklist.md：

- Room 已是 v2、17 张表、显式 migration，不再使用 destructive fallback。
- 已有发布/编辑/软删除事务和 GP/streak 新规则。
- Debug APK 首次启动仍可能插入 Demo，但 Release 不插入。
- 已有 MediaImporter，但 MediaPicker 生命周期、60 秒录音和播放仍待 Z 完成。
- 不要继续把旧行为写成“当前预期”。
- 不得声称 CI 通过，除非附真实 Actions run URL。

任务 B：设置/关于/隐私页面

实现：

1. SettingsScreen
   - 深色模式：跟随系统/浅色/深色；若持久化接口尚未提供，可以明确标记为 UI 预览状态，不伪造保存成功。
   - 通知、数据与隐私入口。
   - 显示当前版本，必须来自 BuildConfig，不硬编码 v1.0.0。
2. PrivacyScreen
   - 权限用途：相机、录音、照片。
   - 本地数据、未来云同步、位置和分享的说明。
   - 数据导出/账号注销未实现时清楚标“后续开放”，不能做无响应按钮。
3. AboutScreen
   - 产品简介、版本、开源许可/第三方组件入口、反馈占位说明。
4. ProfileScreen 和 AppNavigation 接线
   - 原 TODO 入口必须有真实路由或明确 disabled/coming soon 状态。

UI 要求：

- 使用现有 Compose design token，不新增散落硬编码颜色。
- 支持深色模式、1.3x 字体、TalkBack contentDescription。
- loading/empty/disabled 状态明确。
- 至少补 Compose UI 测试或可执行的 smoke case。

允许修改：

- README.md
- PROGRESS.md
- docs/smoke-checklist.md
- ui/profile/ 下的页面
- navigation/AppNavigation.kt
- 必要的 strings/theme UI 资源
- 独立 UI 测试

禁止修改：

- AppDatabase、Migrations、Entity、DAO、Repository
- GP、streak、AchievementEvaluator
- AppContainer/ViewModelFactory
- MediaPicker、MediaImporter、VoiceRecorder、RecordScreen、RecordViewModel
- Gradle、Manifest、GitHub workflow
- docs/server/

交付：

- 修改文件清单。
- 页面截图或录屏。
- 文档修正摘要。
- 无障碍/暗色/大字体检查结果。
- 尚未接入的真实能力，不得将占位写成已完成。
- GitHub PR 或 commit 信息。
```
