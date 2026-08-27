# 已完成工作审计（2026-08-27）

> 审计对象：同步到 `happy_with_life/main` 前的 I1 实现基线；后续 CI 与目录整理状态以仓库根目录 `PROGRESS.md` 为准。
>
> 构建规则：只认 GitHub Actions 结果；本报告不宣称本地构建通过。

## 结论

上一轮已经从“M0 设计阶段”推进到“I1 核心数据层实现”，工作量明显超过最初任务单。当前最有价值的成果是：Room v2、显式迁移、事务化记录写入、可测试 DI、GP/streak 规则和测试基座已经落地。该批代码现已同步到目标仓库，并通过 GitHub Actions [#33067314367](https://github.com/skydream9527-ctrl/happy_with_life/actions/runs/33067314367)。

## 已完成

### Z code 方向

- 完成 `ADR-001-domain-rules.md`，冻结 GP、streak、补记、空间 GP、软删除、植物与同步规则。
- 完成 `ADR-002-dependency-injection.md`，以 `AppContainer + ViewModelFactory` 代替 ViewModel 向下转型全局 Application。
- Room 从 v1 升级到 v2，拆为 17 张表，覆盖用户、空间、记录、媒体、标签、植物、成就、画册、每日统计和 Outbox。
- 增加显式 `MIGRATION_1_2`，移除 `fallbackToDestructiveMigration()`。
- 增加 `MigrationGuard`，在迁移前保存 v1 数据库副本。
- 增加 `DataBootstrap`、`SeedData`，把产品定义种子与 Demo 数据分离。
- 记录发布、编辑、软删除改为 Room transaction，并级联重算每日额度、空间 GP、植物阶段和成就。
- GP 公式改为 ADR/PRD 口径；streak 改用 epoch day，规避固定减 24 小时的 DST 风险。
- 增加 `MediaImporter`，把 `content://` 图片复制到 App 私有目录，支持失败状态和孤儿清理。
- 增加 10 个 JVM/Robolectric 测试文件，覆盖 GP、streak、迁移、事务、编辑、媒体与迁移保护。
- GitHub Actions 已扩展为 unit test、lint、assemble 三个 Job，并上传报告、APK 和 Room schema Artifact。

### MiniMax Code 方向

- 修正 README/PROGRESS 对 Demo、画册、共享、后端和同步的错误完成声明。
- 完成 `design-tokens.md`，对 CSS 与 Compose 设计 Token 做系统映射。
- 完成 `html-compose-diff.md`，盘点 8 个 HTML 设计页与 Android Compose 的差异。
- 完成 `smoke-checklist.md`，覆盖 API 26/29/34、明暗模式、权限、导航和主要流程。

## 尚未完成或需要验证

| 优先级 | 项目 | 状态 |
|---|---|---|
| P0 | 当前全部变更的 GitHub CI 首跑 | 未运行；不能声称构建通过 |
| P0 | Room v2 schema JSON 固化入库 | workflow 可上传 Artifact，但 `app/schemas` 基线尚未出现 |
| P1 | README/PROGRESS/Smoke Checklist 更新到最新 I1 实现 | 仍有“首次 5 条 Demo”“fallback 仍存在”等旧描述 |
| P1 | 记录详情、编辑/删除 UI、补记日期选择 | Repository 已支持，UI 尚未闭环 |
| P1 | `MediaPicker` ActivityResult 生命周期改造 | 仍从 Composable 内创建对象并直接向 Activity 注册 launcher |
| P1 | 录音 60 秒自动停止与 Media3 真播放 | 尚未实现 |
| P1 | 电子画册真实聚合与导出 | 仍为 2024 硬编码 Demo |
| P1 | 分享真照片、系统 Sharesheet、真实 QR/下载链接 | 未完成 |
| P2 | 后端、登录、OSS、云同步、共享空间 | 本轮服务端规格开始设计，代码尚未创建 |

## 审查注意点

- `BuildConfig.DEBUG` 仍会在普通 Debug APK 首次启动插入 5 条 Demo。若希望测试包也验证真实空状态，应增加显式 `ENABLE_DEMO_FIXTURES` 开关，不能只依赖 `DEBUG`。
- Room v2 在模型层一次引入 17 张表，必须依赖 CI 的 Room schema 校验和迁移测试；未通过前不要合入 main。
- 当前 Outbox 已有本地模型，但服务端同步协议尚未冻结；不要让 Android 单方面定义最终 payload。
- 现有 `seedRecordWithFixedGp` 应限制在 Debug/Test source set 或加编译/运行时守卫，避免正式代码路径误用。
- 文档和代码产生了时间差，合并前必须以实际实现回写 README、PROGRESS、Smoke Checklist。

## 下一步顺序

1. 将当前工作区推送到功能分支并创建 Draft PR，让 GitHub CI 首跑。
2. 先修所有 compile/test/lint 问题，再决定是否合入 main。
3. Z code 完成媒体/录音可靠性和 schema 基线；MiniMax 完成记录详情/补记 UI与文档同步。
4. Gork 按 `docs/server/` 规格创建独立仓库 `xiaoquexing-server`。
5. 服务端 OpenAPI v1 首版稳定后，再由 Android 接 Retrofit/WorkManager 同步，避免双方各写一套协议。
