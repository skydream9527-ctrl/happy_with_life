# 小确幸（Happy With Life）Android

小确幸是一款以“轻量记录 → GP 成长 → 植物变化 → 回顾/画册 → 私密共建”为核心闭环的治愈系生活记录 App。

> 当前阶段：Android 原生单机 Demo 已进入 M1 数据可靠性基线，尚不是 v1.0。账号、云同步、共享空间和真实电子画册仍未完成。

## 当前可信基线

- 唯一仓库：<https://github.com/skydream9527-ctrl/happy_with_life>
- 主分支：`main`
- 构建方式：只通过 GitHub Actions；禁止在本地执行 Gradle 或打包。
- 已验证运行：[Android CI #33067314367](https://github.com/skydream9527-ctrl/happy_with_life/actions/runs/33067314367) 的 Unit tests、Android Lint、Assemble Debug APK 全部通过。
- Room v2 schema 已从上述 CI Artifact 核对后提交到 `app/schemas/`。

## 技术栈

- Kotlin、Jetpack Compose、Material 3、Navigation Compose
- Room（SQLite）、Coroutines/Flow、DataStore
- Coil、Media3、CameraX、Lottie
- minSdk 26、targetSdk 34、JDK 17
- 包名：`com.xiaoquexing.app`

## 目录

```text
.
├── .github/workflows/       # GitHub CI 与 APK 打包
├── agent-prompts/           # Z code、MiniMax Code、Gork 的独立提示词
├── app/                     # Android 应用、测试与 Room schema
├── design-reference/        # HTML/CSS 高保真设计参考
├── docs/
│   ├── adr/                 # 已冻结的领域与依赖注入决策
│   ├── plans/               # 完整迭代计划
│   ├── product/             # PRD 与总体架构
│   ├── reviews/             # 已完成工作审计
│   └── server/              # 前后端分离、阿里云与 API/同步规格
├── AGENTS.md                # 所有 Agent 的仓库边界和目录约束
├── PROGRESS.md              # 当前真实进度
└── README.md
```

## 已完成

- 9 种植物 × 7 阶段 Canvas 绘制，9 种心情与 12 种状态标签。
- 首页、记录、时间线、画册、我的、植物图鉴、成就墙和分享页 Compose 骨架。
- Room v2 的 17 张表、显式 `MIGRATION_1_2`、迁移前备份和 schema 基线。
- 记录发布、编辑、软删除事务，以及 GP、streak、植物阶段、成就和 Outbox 的一致性重算。
- `AppContainer + ViewModelFactory` 可测试依赖注入。
- `MediaImporter` 私有目录落盘、失败状态和孤儿文件清理基座。
- Debug Demo 数据隔离、JVM/Robolectric 测试基座、Lint 和 GitHub APK Artifact。

## 部分完成

- 媒体：Activity Result 生命周期、60 秒录音、损坏文件处理和真实播放状态仍需闭环。
- 记录：数据层支持编辑/软删除，详情、编辑和补记 UI 尚未完整接线。
- 时间线：已有分组展示，缺少详情、搜索、筛选和分页。
- 分享：卡片渲染与保存可用，渠道 action、真实照片接线和 QR 仍不完整。
- 画册：现有页面为硬编码 Demo，尚未按真实记录聚合或导出。
- 视觉：深色模式、无障碍、大字体和部分设计 Token 仍未系统收口。

## 尚未实现

- 登录/注册、Token、云同步、OSS 上传与服务端 API。
- 共享空间邀请、成员、权限、共同 GP 与冲突同步。
- 真实地图、月/年回顾、通知、Widget、订阅和 AI 回顾。

## 文档入口

- [当前进度](./PROGRESS.md)
- [PRD v0.3](./docs/product/PRD-v0.3.md)
- [总体技术架构](./docs/product/architecture-v0.1.md)
- [完整迭代计划](./docs/plans/iteration-plan-v1.0.md)
- [服务端与阿里云开发文档](./docs/server/README.md)
- [已完成工作审计](./docs/reviews/completed-work-audit-2026-08-27.md)
- [设计 Token](./docs/design-tokens.md)
- [HTML 与 Compose 差异](./docs/html-compose-diff.md)
- [人工 Smoke Checklist](./docs/smoke-checklist.md)
- [Agent 提示词索引](./agent-prompts/README.md)

## 开发规则

所有 Agent 必须先阅读 [AGENTS.md](./AGENTS.md)：只能在本仓库内工作，不得创建新目录；如缺少目录必须停止并报告。构建、测试、Lint 和 APK 打包一律交给 GitHub Actions。
