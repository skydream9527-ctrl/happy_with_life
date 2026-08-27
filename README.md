# 小确幸 (XiaoQueXing) - Android App

🌱 一款治愈系生活记录App。记录生活中的每个小确幸，看着植物随你的记录慢慢生长。

> **本文件状态（M0-01 统一于 2026-08-27）**
>
> - 本仓库为 **Android 原生单机 Demo**，不是 v1.0。
> - "功能特性"中的多项是**产品愿景/规划**或**仅 Demo**，**真正完成**的项请看
>   [PROGRESS.md](./PROGRESS.md) 第"核心功能完成度"区块。
> - "构建 APK"只走 GitHub Actions，**本 README 不再给出本地 `./gradlew` 命令**；
>   编译是否通过以 [`.github/workflows/build.yml`](./.github/workflows/build.yml)
>   最近一次 main / PR run 为准，本仓库暂无公开 run 链接。
> - 文档与代码的"已知冲突"清单见本文末尾"已知文档冲突"。

## 1. 技术栈

- 语言：Kotlin
- UI 框架：Jetpack Compose + Material 3
- 导航：Navigation Compose
- 数据库：Room（SQLite）
- 异步：Kotlin Coroutines + Flow
- 图片加载：Coil
- 音视频：ExoPlayer / Media3
- 相机：CameraX
- 动画：Lottie（当前未实际接入，路径仅为占位）
- 分页：Accompanist Pager（当前未实际接入）
- minSdk = 26（Android 8.0）
- targetSdk = 34（Android 14）
- 包名：`com.xiaoquexing.app`

## 2. 项目结构

```
app/src/main/java/com/xiaoquexing/app/
├── MainActivity.kt              # 入口Activity
├── XiaoQueXingApp.kt            # Application（首次启动插入 5 条 Demo 记录 + 默认植物）
├── navigation/AppNavigation.kt  # 底部 Tab 导航 + 4 个二级页面路由
├── ui/
│   ├── theme/                   # 主题/颜色/字体（部分硬编码见 [design-tokens.md](./docs/design-tokens.md)）
│   ├── components/              # 通用组件（植物/卡片/按钮等；存在多处硬编码颜色）
│   ├── home/                    # 首页（含硬编码渐变与阴影 alpha）
│   ├── record/                  # 记录页（含 Color.White/Color.Black 硬编码）
│   ├── timeline/                # 时间线（点击直接进分享，无记录详情）
│   ├── album/                   # 电子画册（**当前为 2024 硬编码 Demo**）
│   ├── profile/                 # 我的/植物选择/图鉴/成就
│   └── share/                   # 分享面板（渠道按钮 action 未实现）
├── data/
│   ├── db/                      # Room 数据库 + DAO（包含 `Space` 表与 `SpaceDao`，**未启用**）
│   ├── entity/                  # 数据实体（Record/Plant/Achievement/Space 等）
│   ├── model/                   # UI 模型（MoodTag/GPBreakdown/ShareCardData 等）
│   └── repository/              # 数据仓库
├── viewmodel/                   # ViewModel 层
├── media/                       # Photo Picker / 相机 / MediaRecorder 封装
└── util/                        # 工具类（GP 计算 / 植物 Canvas 渲染 / 分享卡片渲染）
```

## 3. 真正完成（与代码直接对应）

- 9 种植物 × 7 阶段 Canvas 绘制（`util/PlantRenderer.kt`，1100+ 行）。
- 9 种心情枚举 + 12 种状态标签枚举（`data/model/MoodTag.kt`、`StatusTag.kt`）。
- Room 数据库与基础 DAO/Repository（`data/db/*`、`data/repository/*`）。
- 首页 / 记录 / 时间线 / 画册 / 我的 / 植物选择 / 植物图鉴 / 成就墙 / 分享页 Compose 骨架。
- Photo Picker + 系统相机 + `MediaRecorder` 接入（`media/MediaPicker.kt`）。
- 分享卡 Compose 渲染 + Bitmap 保存到 MediaStore（`util/PhotoSaver.kt`）。
- GitHub Actions 在 main / PR / 手动触发时执行 `assembleDebug` 并上传 APK Artifact。
- 单测基座：`util/GPCalculatorTest.kt`、`util/StreakCalculatorTest.kt`、
  `data/db/V1SchemaFixtureTest.kt`、`data/repository/RecordRepositoryTransactionTest.kt`。
  （其余模块仍**没有单测**，也不存在 `androidTest`。）

## 4. 部分完成（有 UI / 有数据，但存在已知缺口）

| 模块 | 真实状态 | 主要缺口 |
|---|---|---|
| 记录发布 | UI 完整，写入 Room | 60 秒录音上限未执行；AudioRecord 生命周期未完整；Photo Picker URI 长期权限未处理；压缩、EXIF、孤儿文件清理缺失；无编辑/删除/补记 |
| 时间线 | 按日期分组 | 卡片点击直接进分享页；无记录详情、搜索、筛选、分页 |
| 音乐/链接/地点 | UI 卡片存在 | **手动输入文字**，无 URL 解析、无音乐元数据、无真实定位或 POI |
| 录音 | 系统 API 接入 | 无真正播放状态、60s 上限、生命周期处理、损坏文件处理 |
| 分享 | Compose 渲染 + 保存图片可用 | 微信/朋友圈/小红书/微博 渠道按钮**无 action**；QR 占位；真实照片未走卡片渲染 |
| GP/连击/植物/成就 | 实现存在 | **与 PRD §3.2.1 公式不一致**（详见 [../PRD-v0.3.md](../PRD-v0.3.md) §8.1）；无端到端单测覆盖 |
| 深色模式 | Theme 完整 | 多数页面**硬编码浅色**（`HomeScreen`/`MusicCard`/`ProfileScreen`/`AlbumViewerScreen` 等） |
| 电子画册 | 翻页框架 + 9 类页面布局 | **内容为 2024 年硬编码 Demo**；不查数据库，不支持聚合、长图/PDF 导出 |

## 5. 规划 / Demo / 未实现

- 共享空间：只有 `Space` 实体与 `SpaceDao`，**记录无 `spaceId`、无成员/邀请/权限/UI**。
- 登录、注册、验证码、Token、登出可撤销：**0%**。
- 后端 API（Go + PostgreSQL）、OSS、推送：**0%**。
- 离线同步、Outbox、冲突合并、tombstone：**0%**。
- 地图视图、月度/年度回顾、通知/提醒、Widget、生物识别解锁：**0%**。
- AI 回顾、会员/订阅、画册主题包：**0%**。
- iOS 版本：未开工程。

## 6. 构建与验收约定

- 任何"编译通过"以 GitHub Actions run 为准。
- 当前 workflow 包含 3 个 job：`unit-test`（`testDebugUnitTest`）、`lint`（`lintDebug`）、
  `assemble`（`assembleDebug`，依赖前两者）。在所有 job 都绿、Artifact 可下载之前，
  不允许声称 CI 通过。
- **不要求本地执行 Gradle**；本 README 不再提供本地构建命令。
- 仓库根目录历史性 `apk/app-debug.apk` 仅为历史产物，是否对应最新代码以
  GitHub Actions Artifact 为准。

## 7. 文档

- 上级 PRD：[`../PRD-v0.3.md`](../PRD-v0.3.md)（已对齐标题版本号为 v0.3）。
- 上级架构方案：[`../架构方案-v0.1.md`](../架构方案-v0.1.md)。
- 迭代计划：[`../迭代开发计划-v1.0.md`](../迭代开发计划-v1.0.md)。
- 实时进度：[`./PROGRESS.md`](./PROGRESS.md)。
- 设计 Token：[`./docs/design-tokens.md`](./docs/design-tokens.md)。
- 人工 smoke checklist：[`./docs/smoke-checklist.md`](./docs/smoke-checklist.md)。
- Room v2 方案（Z code 产出）：[`./docs/room-v2-schema.md`](./docs/room-v2-schema.md)。
- ADR：[`./docs/adr/`](./docs/adr/)。

## 8. 已知文档冲突（M0-01 任务修复后仍存在的项）

1. `android/PROGRESS.md` "核心功能完成度" 中**真实拍照/录音 ✅ 85%** 与同表内
   **"音乐/链接/地点 90%"** 与本文档 §4 "录音 / 音乐 / 链接 / 地点" 描述**仍有歧义**：
   PROGRESS.md 把系统 API 接入视为 85%，但 [../迭代开发计划-v1.0.md](../迭代开发计划-v1.0.md)
   与本 README 视为"部分完成"。本仓库内两者并存，已在本 README §4 明确口径；
   下次重写 PROGRESS.md 时应统一。
2. `android/PROGRESS.md` 同时出现"相机/录音为占位实现"和"新增 MediaPicker 不再占位"
   两条相反叙述。M0-01 任务已尽量把后者（更接近当前代码）作为基准，前者保留为
   "Demo 早期"的历史快照，**但文档未做删除**。
3. 仓库根目录 `README.md` 与 `PRD-v0.3.md` 第六章"v1.0 MVP 全部勾选"之间存在
   视觉冲突；M0-01 任务已在两处补加"状态说明"段，**核心勾选内容未删**，留给后续
   文档轮次在 ADR 冻结后统一重写。
4. `android/README.md` 的"在 Android Studio 中运行"小节在 M0-01 之前版本存在，
   本版本已删除，不在 GitHub Actions 唯一通道上做本地兜底说明。
