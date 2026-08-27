# 小确幸 Android 项目 - 开发进度

> **本文件状态（M0-01 统一于 2026-08-27）**
>
> - 旧版 PROGRESS.md 同时存在"相机/录音为占位实现"和"新增 MediaPicker 不再占位"
>   两条相反叙述。M0-01 任务以**更接近当前代码**的版本为基准，统一为
>   "系统 API 已接入、但播放/60s/生命周期未闭环"——即**部分完成**而非"占位"。
> - "核心功能完成度"区块重新标注，区分**真正完成**与**部分完成**。
> - 文件清单与最近一轮增量仅记录与代码状态直接对应的项，不复述未实现的规划。
> - 本文件不修改任何业务代码、不修改 schema/DAO/ViewModel/Gradle/Manifest/workflow。
> - 后续轮次以 [../迭代开发计划-v1.0.md](../迭代开发计划-v1.0.md) 的 I0–I8 为真值表。

## 1. 项目概况

- 包名：`com.xiaoquexing.app`
- 技术栈：Kotlin + Jetpack Compose + Material 3 + Navigation Compose + Room + Coroutines/Flow + Coil + ExoPlayer/Media3 + CameraX
- 阶段：**M0（基线可信）**——Android 原生单机 Demo，非 v1.0
- minSdk = 26，targetSdk = 34
- 主入口：`MainActivity.kt` + `XiaoQueXingApp.kt`（Application）
- 视觉基准：[`../xiaoquexing-ios-redesign/colors_and_type.css`](../xiaoquexing-ios-redesign/colors_and_type.css) + 8 个 HTML 页面
- 构建：[`./.github/workflows/build.yml`](./.github/workflows/build.yml) 三个 job（unit-test / lint / assemble），产物 `app-debug` Artifact
- 设计 Token：[`./docs/design-tokens.md`](./docs/design-tokens.md)
- 人工 smoke checklist：[`./docs/smoke-checklist.md`](./docs/smoke-checklist.md)

## 2. 核心功能完成度（M0-01 重写）

> 标尺：
> - ✅ **真正完成**：UI/逻辑/数据三层都可用，且行为与 [../PRD-v0.3.md](../PRD-v0.3.md) 当前文字一致
> - 🟡 **部分完成**：UI 或数据层有缺口，或行为与 PRD 不一致
> - ⏳ **Demo**：有界面但内容是硬编码
> - ❌ **未实现**：0% 进度
> - 📋 **规划**：仅在产品文档中存在

| 功能 | 状态 | 说明 |
|---|---|---|
| 9 种植物 × 7 阶段 Canvas 绘制 | ✅ | `util/PlantRenderer.kt`，`ui/components/PlantView.kt` |
| 9 种心情 + 12 种状态标签 | ✅ | 枚举与选择 UI 可用 |
| 首页 Compose 骨架 | 🟡 | UI 完整；`HomeScreen.kt` 大量硬编码颜色/阴影 alpha，未对齐 design token（详见 design-tokens.md） |
| 记录页 UI | 🟡 | 心情/状态选择可用；**心情必选未强制**；照片/语音/音乐/链接/地点按钮可见 |
| 文字记录保存 | 🟡 | 写入 Room；草稿保护、错误提示、500 字校验**未做** |
| 照片记录 | 🟡 | Photo Picker 接入；**URI 长期权限、复制到 App 私有目录、压缩、EXIF、孤儿文件清理**均未做 |
| 语音记录 | 🟡 | `MediaRecorder` API 接入；**无真正播放/暂停、60s 上限、生命周期、损坏文件处理** |
| 音乐/链接/地点卡片 | 🟡 | UI 卡片可用；**目前只接受手动输入文本**，无 URL 解析、无音乐元数据、无真实定位 |
| GP 计算 | 🟡 | 公式存在；**与 PRD §3.2.1 不一致**（照片 +8/张、7 天即双倍；PRD 写 +3/张、20 天双倍）—— 详见 PRD §8.1 |
| 连续天数 | 🟡 | `StreakCalculator` 实现；`getRecordDays()` 仍对毫秒整数做 `substr(createdAt, 1, 8)`——按迭代计划 P1 |
| 植物 GP 累加 | 🟡 | 与 `RecordRepository` 写入**分两步，无 Room transaction**；按迭代计划 P0 |
| 时间线 | 🟡 | 按日期分组；**点击直接进分享页**，无记录详情、搜索、筛选、分页 |
| 画册列表与翻页 | ⏳ Demo | 框架 + 9 类页面布局；**内容为 2024 年硬编码 Demo**，不查 Room、不聚合、不导出 |
| 植物选择 | ✅ | 9 种植物网格、解锁状态显示 |
| 植物图鉴 | 🟡 | 9 种植物展示；**已解锁状态仅由 GP 触发**，与 PRD 解锁条件不完全一致 |
| 成就墙 | 🟡 | 17 枚成就 + 进度条；**部分计数算法与 PRD 不一致**，分享类成就未触发 |
| 分享卡（Compose 渲染） | ✅ | `ui/components/ShareCardGenerator.kt` |
| 分享卡（Bitmap 保存） | ✅ | `util/PhotoSaver.kt` + `ui/share/ShareViewModel.kt` 写入 MediaStore |
| 分享渠道按钮 | ❌ | 微信/朋友圈/小红书/微博/Copy 按钮当前**无 action**；QR 为占位 |
| 共享空间 | 📋 规划 | `Space` 实体 + `SpaceDao` 已建；**记录无 `spaceId`、无成员/邀请/权限/UI** |
| 登录/账号 | ❌ | 0%；工程内无 Retrofit/Hilt/Auth/Token 任何实现 |
| 后端 / 云同步 | ❌ | 0% |
| 真实地图视图 | 📋 规划 | 仅有手动输入的"地点"字段；无地图 SDK 接入 |
| 月度/年度回顾 | 📋 规划 | 0% |
| 通知/Widget/生物识别 | 📋 规划 | 0% |
| 会员/订阅 | 📋 规划 | 0% |
| AI 回顾 | 📋 规划 | 0% |
| iOS 版本 | 📋 规划 | 未开工程 |

## 3. 单测覆盖

- `util/GPCalculatorTest.kt` ✅
- `util/StreakCalculatorTest.kt` ✅
- `data/db/V1SchemaFixtureTest.kt` ✅
- `data/repository/RecordRepositoryTransactionTest.kt` ✅
- `androidTest`（UI 测试）：**0%**
- 集成测试：未建立基座

## 4. Demo 数据策略

- 当前 `XiaoQueXingApp.kt` 首次启动会插入 **5 条 Demo 记录 + 默认植物**。
- **按迭代计划 M0 结论**：正式首次启动应**不写入**任何记录，仅建立个人空间与默认植物。
  Demo 仅在 Debug fixture 注入。
- 当前实现尚未做此隔离，是 M0 阶段可接受范围；进入 M1 后由 Z code 改造
  （Z1-07 初始化一致性，PRD §8 第 4 行）。

## 5. 已知 P0/P1 风险（来自 [../迭代开发计划-v1.0.md](../迭代开发计划-v1.0.md) §2.5）

| 等级 | 风险 | 责任任务 |
|---|---|---|
| P0 | GPCalculator 与 PRD 公式不一致 | Z0-01、Z1-03 |
| P0 | 记录/植物/成就三步写无 Room transaction | Z1-02 |
| P0 | 首次启动 5 条 Demo 116 GP 不同步到活动植物 | Z1-07 |
| P0 | `fallbackToDestructiveMigration()` 在未提供 migration 时清空用户数据 | Z0-02、Z1-01 |
| P1 | `MediaPicker` 在 Composable 构造时注册 ActivityResult | Z1-05、Z1-06 |
| P1 | Photo Picker `content://` 长期写入 Room 无持久权限策略 | Z1-05 |
| P1 | `getRecordDays()` 对毫秒整数 `substr` | Z2-05 |
| P1 | "摄影师" 成就算法为含照片记录数；植物解锁全用 GP | Z1-03 |
| P1 | 时间线卡片点击进分享（无详情）；分享渠道按钮无 action | M1-02、Z2-03 |
| P1 | 画册 ID 不查数据库，全部 2024 硬编码 | I3 全部 |

## 6. 文档任务变更记录（M0-01）

- 本轮（2026-08-27）统一四份文档对项目阶段、CI 范围、画册、共享空间、登录、音乐/链接/地点、录音、分享、测试的描述。
- 旧版 PROGRESS.md 的"已知问题"小节被 §2/§4/§5 替代；其中 ✅ 项目视为已部分完成而非
  "占位实现"。
- 文件清单小节被 `android/README.md §2` 与 `android/README.md §3` 替代，本文件不再重复。
- 后续轮次以 [`../迭代开发计划-v1.0.md`](../迭代开发计划-v1.0.md) 的 I0–I8 为真值表，
  本文件仅做状态汇总与差异标注。

## 7. 文档任务变更记录（M0-04 docs-only，2026-08-27）

- 用户回复"你看著辦"后，本轮**没有**改源码，原因是原 [`第一轮Agent任务单.md`](../../第一轮Agent任务单.md) 第 80–85 行明文禁止"任何正式功能代码"；改 `ui/theme/*` 与 `ui/*/Screen.kt` 属于禁区。
- 在 [`./docs/design-tokens.md`](./docs/design-tokens.md) 追加**附录 §11–§18**：包含 PR 拆分（8 个）、Token 改名路径表、Bridge 思路（QxColors/QxSpacing/QxRadius/QxElevation/QxMood/QxTypography 全部 Kotlin 草稿）、Compatibility 三阶段策略、接手检查表、验证步骤（只走 GitHub Actions）、必须先回答的 4 个开放问题（深色调色板 / Mood 去重 / 资源色 / 字体）。
- 接手者开工前**必须**逐条对照该附录，并在 PR 描述里显式引用相关小节编号。
- 任何后续 PR 若违反 §11.3 红线或 §16 接手检查表，**不**允许合入。

## 8. 文档任务变更记录（M0-05 docs-only，2026-08-27）

- 新增 [`./docs/html-compose-diff.md`](./docs/html-compose-diff.md)（8 个 HTML 页面 × Compose Screen 逐页 1:1 对照表）。
- **核心结论**：
  - 1 个 ❌ **严重不一致**（S-1 底部 5 Tab 浮动毛玻璃 vs Material3 NavigationBar 实色）
  - 1 个 ❌ 严重不一致（S-2 画册 CTA banner / S-3 成就摘要卡）
  - 11 个 🟡 **中度不一致**（主按钮 / textarea / 头像 / 4 栏 vs 3 栏 / 时间线分段 / chip 颜色规则 / 进度条 6px / ❤️💬 按钮 等）
  - 8 个 🟡 轻度不一致（状态栏 / 顶导 / chip 数量 / 成就数量 / 背景 / 渐变 等）
- **6 个需产品方/设计方决策**的问题已写进 diff §5（与 design-tokens.md §9 同步）。
- 接手 M0-04 的 PR 描述**必须**引用本表对应行号，否则不合入。
- diff §4 列表已**按影响面排序**，PR 优先级 S-* > M-* > L-*。
