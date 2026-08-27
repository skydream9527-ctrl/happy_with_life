# HTML×Compose 1:1 设计对照表（M0-05 任务，2026-08-27）

> 范围：把 [`../design-reference/pages/`](../design-reference/pages/) 下 8 个 HTML 高保真
> 页面与 `app/src/main/java/com/xiaoquexing/app/ui/**` 下 Compose Screen **逐页 1:1**
> 对照，作为下一轮 M0-04 / M1 改源码时的真值表。
>
> 本文件**只产文档**，不触碰任何源码。
>
> **图例**
> - ✅ **对齐**：HTML 与 Compose 视觉一致（颜色 / 间距 / 圆角 / 字号在容差内）
> - 🟡 **部分对齐**：行为或样式有差异，但都"在风格上对"
> - ❌ **不对齐**：HTML 有，Compose 缺失 / 错了 / 写死成另一个值
> - 🆕 **HTML 没有**：Compose 实现中独有的，HTML 设计稿未给出
> - ⏸️ **Demo / 占位**：HTML 设计稿是"理想态"，Compose 已知按 Demo 实现

## 0. 阅读指引

| 章节 | 内容 |
|---|---|
| §1 | 页面映射总表（8 HTML → Compose Screen 列表） |
| §2 | 逐页 1:1 对照（home / record / timeline / album / share / plant-guide / achievements / profile） |
| §3 | 跨页面共享组件（status bar / 底部 nav / 卡片 / chip） |
| §4 | 不一致点清单（按影响面排序） |
| §5 | 需产品方 / 设计方决策的问题 |
| §6 | 下一轮接手建议 |

## 1. 页面映射总表

| # | HTML 页面 | Compose Screen | 路由（推断） | 状态 |
|---|---|---|---|---|
| 1 | `pages/home.html` | `ui/home/HomeScreen.kt` | `home` | 🟡 |
| 2 | `pages/record.html` | `ui/record/RecordScreen.kt` | `record` | 🟡 |
| 3 | `pages/timeline.html` | `ui/timeline/TimelineScreen.kt` | `timeline` | 🟡 |
| 4 | `pages/album.html` | `ui/album/AlbumScreen.kt` | `album` | ⏸️ Demo（2024 硬编码） |
| 5 | `pages/share.html` | `ui/share/ShareScreen.kt` | `record/{id}/share` 或 `timeline/{id}/share` | 🟡 |
| 6 | `pages/plant-guide.html` | `ui/profile/PlantGuideScreen.kt` | `profile/plant-guide` | 🟡 |
| 7 | `pages/achievements.html` | `ui/profile/AchievementScreen.kt` | `profile/achievement` | 🟡 |
| 8 | `pages/profile.html` | `ui/profile/ProfileScreen.kt` | `profile` | 🟡 |

底部 Tab 与「植物选择」是跨页组件，单独在 §3 处理。

## 2. 逐页 1:1 对照

> 每页用统一模板：组件 / HTML 写法（CSS token） / Compose 写法 / 一致性。
> 行号以代码当前状态为准，下一轮前可重核。

### 2.1 home.html ↔ HomeScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **页面背景** | `body { background: linear-gradient(180deg, #F0F7F1 0%, var(--qx-background) 240px); }` | `HomeScreen.kt:218-222` `listOf(Color(0xFFE8F3EA), Color(0xFFF4F9F2), Color(0xFFFBFAF5))` 渐变 | 🟡 HTML 渐变是 `#F0F7F1 → #FBFBF9`（即 `--qx-primary-50 → --qx-background`），Compose 自创 `#E8F3EA → #F4F9F2 → #FBFAF5` |
| **状态栏占位** | `<div class="ios-status-bar">` height 47px / min 47px | `Spacer` 或 `WindowInsets.statusBars` 隐式 | 🟡 Compose 未显式 47dp，依赖系统 inset |
| **大标题 + 副标题** | `<h1 class="ios-large-title">早安 ☀️</h1>` 34sp/Bold + `<p class="ios-nav-subtitle">2026年8月26日` 15sp/Regular | `HomeScreen.kt` 文本（具体行号需重核），推测 `MaterialTheme.typography.displayLarge` | ✅ 若 Compose 走 `displayLarge` 则与 CSS 34/Bold 一致 |
| **右上角图标按钮（植物选择 / 图鉴）** | `class="nav-icon-btn"` 22px 圆形 | `IconButton` | 🟡 缺显式尺寸字面量 |
| **植物主图（Hero SVG）** | `<svg class="plant-svg" width="140" height="160">` 内嵌 SVG | `HomeScreen.kt` 调 `PlantView`，由 `util/PlantRenderer.kt` Canvas 绘制 | ⏸️ 实现路径不同（HTML SVG vs Compose Canvas）；视觉对齐由 `PlantRenderer` 负责 |
| **植物名 + 阶段** | `<div class="ios-headline" style="color:var(--qx-primary-dark);">小确幸之树 · 成长期</div>` 17sp/600 | 应走 `MaterialTheme.typography.headlineSmall` + `colorScheme.primaryDark` | 🟡 Compose `primaryDark` 未命名（`GreenDark` 旧名） |
| **GP 进度条** | `<div class="ios-progress">` 6px 高 + `ios-progress-fill` 9999 圆角 | `LinearProgressIndicator` | 🟡 Material 默认是 4dp 高度；CSS 是 6px |
| **GP 进度文本** | `<span class="progress-label current">GP 680</span>` 13sp | 应走 `MaterialTheme.typography.bodySmall` | ✅ 13sp 一致 |
| **统计三栏** | `<div class="ios-stat-pill">` 12/8 padding，xl/xs 字号 | `HomeScreen.kt` 散落 `padding(12.dp / 8.dp)` | ❌ 字面量硬编码（参见 design-tokens.md §7） |
| **主按钮"记录小确幸"** | `class="ios-btn ios-btn-primary ios-btn-large"` 14/24 padding，17sp/600，14 圆角，shadow `0 4px 14px rgba(94,155,106,0.25), 0 2px 6px rgba(94,155,106,0.15)` | `Button` 默认 | ❌ Compose 未匹配 CSS 的 14/24 padding 与 14 圆角；阴影未对齐 |
| **最近记录卡** | `<div class="ios-record-card">` 9999px 内含 `.record-card-inner` | `HomeScreen.kt` 调 `RecordCard` 组件 | 🟡 `RecordCard` 自身未在本任务中详查（见 §3.4） |
| **记录卡：日期 + 心情** | `class="record-date"` 13sp/Regular + `record-mood` emoji | `RecordCard.kt` | 🟡 推测对齐 |
| **记录卡：照片 + 文字** | `class="record-photo"` 80×80 + `class="record-text-area"` | `PhotoGrid.kt` + `Text` | 🟡 |
| **记录卡：标签** | `class="record-tag"` 11sp/Medium 浅灰背景 | `TagChip.kt` | 🟡 见 design-tokens.md §2.3 关于 TagChip 文字色 |
| **底部 5 Tab** | `class="record-btn"` 48×48 圆形 + nav 5 项 | `AppNavigation.kt` | 🟡 HTML 用了 `backdrop-blur-2xl`，Compose 是 Material3 NavigationBar（实色） |

### 2.2 record.html ↔ RecordScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **页面背景** | `body` 默认（暖白） | 默认 | ✅ |
| **顶部导航（44px 居中）** | `<div class="record-nav">` 44px 高 + 返回按钮 | `TopAppBar` | 🟡 Compose 高度通常 64dp，CSS 44px（iOS 风） |
| **返回箭头** | `<svg width="12" height="20">` stroke 2.5 | `IconButton(Icon(...))` | 🟡 尺寸通常由 Material Icon 控制 |
| **页面标题** | `class="record-nav-title"` 17sp/600 主色 | `TopAppBar` 标题 | ✅ |
| **小节间距** | `<div class="record-section" style="margin-top: 20px;">` | `RecordScreen.kt` 中 `.padding(top = 20.dp)` 等 | ❌ 硬编码 |
| **小节标题"今天的心情是？"** | `class="record-section-label"` 13sp/600 次文字 | `Text` | 🟡 |
| **心情 chips 横向滚动** | 8 个 `class="ios-chip mood-happy selected"` 7/14 padding 13sp/500 9999 圆角 | `TagChip.kt` 9 种心情 | ❌ 数量不一致：HTML 8 个（含"放松"）；Compose `MoodTag` 9 种（含 `MoodSurprise`、`MoodAngry`） |
| **心情 chip 选中颜色** | `.mood-happy.selected { background: var(--mood-happy); color: #5D4A00; }` 等 5 种 | `TagChip.kt:47-48` `lum > 0.72f` 选白/黑文字 | 🟡 文字色采用亮度阈值，与 CSS 5 种明确规则不完全一致 |
| **状态 chips** | 14 个 `class="ios-chip"`（旅行/美食/运动/阅读/工作/学习/约会/家人/朋友/宠物/自然/电影/音乐/独处） | `RecordScreen.kt` `StatusTag` 12 种 | 🟡 HTML 14 个、Compose 12 个；缺"宠物"和"约会"或重复（详见 design-tokens.md §2.3 / §5.5） |
| **文字输入** | `<textarea class="ios-textarea">` 14/16 padding 17sp/400 14 圆角 `--qx-surface-secondary` 背景 | `OutlinedTextField` | ❌ Material 默认样式；CSS 的 17sp/14 圆角/14-16 padding 未对齐 |
| **照片缩略图** | `class="photo-thumb"` 3×3 网格 + `class="photo-delete"` | `PhotoGrid.kt` | 🟡 推测对齐 |
| **位置卡** | `class="location-card"` + emoji + 文本 + 删除 | `MusicCard.kt` 含 `LocationCard` | 🟡 `MusicCard` 同一组件含链接/音乐/地点三种变体，命名与 HTML 不一 |
| **添加工具栏** | 5 个 `class="toolbar-btn"` 圆形 56px，emoji 22px | `AddContentButton.kt` 5 个圆形按钮 | 🟡 推测 56dp 一致；emoji 替换为 Material Icon |
| **GP 预计卡** | `class="gp-card ios-card-inset" style="background: var(--qx-primary-container);"` | `RecordScreen.kt` | 🟡 Compose 是否使用 `--qx-primary-container`（透明绿 8%）未确认 |
| **发布按钮** | `class="ios-btn ios-btn-primary ios-btn-large" style="height: 56px;"` 14/24 padding 17sp/600 14 圆角 | `Button` | ❌ 56px 高度未与 CSS 强一致 |
| **成功提示（已记录）** | HTML 无对应 | `RecordScreen.kt:121-401` `Color.Black.copy(alpha=0.3f)` 弹窗 + `Color.White` 文字 | 🆕 Compose 独有 |

### 2.3 timeline.html ↔ TimelineScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **大标题"时间线"** | `class="ios-large-title"` 34/Bold | 应 `displayLarge` | ✅ |
| **副标题** | `class="ios-nav-subtitle"` 15/Regular | `bodyMedium` | ✅ |
| **分段控件** | `class="ios-segmented"` 2px padding + `--qx-gray-100` 背景，9999 圆角；选中态 `--qx-surface` 背景 | Material3 `SegmentedButton` 或自实现 | ❌ Compose `TimelineScreen.kt` 当前**无分段控件**（见 PROGRESS §2 已知缺口） |
| **日期分组头** | `class="timeline-date-header"` + `date-dot` 圆点 8×8 主色 | `TimelineScreen.kt` | 🟡 |
| **记录卡 1：照片 + 文字 + 标签** | `class="ios-record-card"` + 14/16 padding 卡片 | `RecordCard.kt` | 🟡 |
| **记录卡 1：心形 / 评论按钮** | `class="record-action-btn"` 16×16 图标 | `IconButton` | 🆕 HTML 有 ❤️/💬，Compose 当前**无**（按迭代计划 P1：共享空间未实现） |
| **记录卡 2：音乐附件** | `class="record-music"` emoji + 标题 + 副标题 | `MusicCard.kt` | 🟡 |
| **记录卡 3：地点附件** | `class="record-location"` | `MusicCard.kt` 内的 `LocationCard` | 🟡 |
| **记录卡 4：链接附件** | `class="record-link-card"` 封面 + 标题 | `MusicCard.kt` 内的 `LinkCard` | 🟡 |
| **标签 chip（心情 + 状态）** | `class="record-tag-chip"` 12/8 padding 13sp/500 9999 圆角 | `TagChip.kt` | 🟡 推测对齐 |
| **卡片间距** | `margin-bottom: 12px` + 卡片间 `16px` 边距 | `TimelineScreen.kt` 散落 | ❌ 硬编码 |
| **滚动容器** | `class="timeline-scroll"` + `--qx-background` | LazyColumn | ✅ |

### 2.4 album.html ↔ AlbumScreen.kt + AlbumViewerScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **大标题"电子画册"** | `ios-large-title` | `AlbumScreen.kt` | ✅ |
| **创建画册 CTA banner** | `class="create-album-banner"` 9999 圆角（？） / `border-radius: var(--radius-xl)` (18px)，`linear-gradient(135deg, #A8D5B0 0%, #D4E8D0 30%, #F5EDD8 70%, #F9EFD8 100%)` | `AlbumScreen.kt` `Icon(Icons.Default.Add, contentDescription = "新建画册", tint = Color.White)` | ❌ Compose 端仅一个小图标按钮，**没有 banner 渐变与"开始创建"文案** |
| **画册封面卡 6 个** | `class="album-card album-cover-{summer/tree/food/travel/friends/morning}"` 渐变背景 + emoji 装饰 + 标题 + 日期范围 + 记录数 | `AlbumScreen.kt` | ⏸️ 画册 ID 不查数据库，HTML 6 个封面在 Compose 端是**2024 硬编码 Demo**，需对照原代码确认是否 6 张 |
| **"最新"徽章** | `class="album-featured-badge"` 10px ⭐ + "最新" 文本 | `AlbumScreen.kt` | 🟡 推测 `Badge` 或自实现 |
| **画册装饰 emoji** | `class="album-decoration"` 浮动 | 无对应 | 🆕 |
| **画册标题 / 副标题** | 17sp/600 + 13sp/Regular | `AlbumScreen.kt` | 🟡 |
| **画册翻页（Viewer）** | HTML 无对应（`share.html` 提供类似结构） | `AlbumViewerScreen.kt` 已实现 | 🟡 实现细节与 share 的"分享卡"高度重合，存在组件重复风险 |

### 2.5 share.html ↔ ShareScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **暗色顶部（status bar 文字白色）** | `class="status-bar-dark"` 9:41 + 信号/Wi-Fi/电池 | `ShareScreen.kt` | ❌ 暗色状态栏需 `WindowCompat` / `isAppearanceLightStatusBars` 控制 |
| **Modal 导航** | `class="modal-nav"` 44px 居中 + 关闭 X | `TopAppBar` | 🟡 |
| **分享卡（Hero）** | `class="share-card"` 9999 圆角 + 装饰 🍃🌸🍂✨ + 顶部栏 + 心情 + 照片 + 文字 + 标签 + 音乐条 + 底部 QR | `ShareScreen.kt` + `ShareCardGenerator.kt` | 🟡 卡片本体基本对齐；装饰 emoji 在 Compose 端由 `ShareCardGenerator` 重画 |
| **分享卡：底部 QR** | `class="card-qr"` 36×36 | `ShareCardGenerator.kt` | ⏸️ 当前是占位；按迭代计划需 Z2-03 实现 |
| **分享卡：植物图标 + Slogan** | "🌱 用小确幸记录生活" 居中 | `ShareCardGenerator.kt` | 🟡 |
| **iOS Share Sheet** | `class="share-sheet"` 圆角 24 顶 + grabber 居中 + 6 个图标（微信/朋友圈/微博/小红书/保存图片/复制链接） | `ShareScreen.kt` 6 个图标按钮 | 🟡 HTML 用 SVG + 平台色；Compose 用 `Icon` + 硬编码 `Color(0xFF07C160 / 0xFFFF2442 / 0xFFE6162D)` |
| **App icon 圆** | `class="app-icon-circle"` 56×56 圆形 主色背景 | `IconButton` 56dp | 🟡 |
| **Action 行（九宫格切图）** | `class="action-row"` 56px icon + 文本 + chevron | `ShareScreen.kt` | ⏸️ 9 宫格切图按迭代计划 I3 M3-04 |
| **保存图片按钮** | `class="app-icon-circle save"` 蓝色 `#007AFF` | `ShareScreen.kt` "保存图片"按钮 | ✅ 已接 `PhotoSaver.kt` 写 MediaStore |
| **微信/朋友圈/小红书/微博/Copy** | 6 个图标，点击**无 action** | `ShareScreen.kt:155-171` 同样无 action | 🟡 命名一致；行为缺失（已在 PROGRESS.md §2 列为 🟡） |

### 2.6 plant-guide.html ↔ PlantGuideScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **顶部导航（带"我的"）** | `class="plant-nav-bar"` 44px 居中 | `TopAppBar` | 🟡 |
| **引导头卡** | `class="guide-header-card"` 9999 圆角 + 🌿 + 标题"植物图鉴" + "已收集 3/9" + 描述 + 进度 | `PlantGuideScreen.kt` | 🟡 |
| **解锁植物卡** | `class="plant-card plant-card-featured"` + "主植物"徽章 + 🌳 + 植物名 + 标签 + 阶段/记录/陪伴 三栏 + 描述 | `PlantGuideScreen.kt` | 🟡 |
| **未解锁分组** | `class="locked-section-header"` + 横线 + "待解锁" | `PlantGuideScreen.kt` | 🟡 |
| **未解锁植物卡** | `class="plant-card-locked"` + emoji + 🔒 + 名称 + 解锁条件 + chevron | `PlantGuideScreen.kt` | 🟡 |
| **解锁条件文案** | "连续记录30天解锁" / "在50个地点记录解锁" / "邀请1位好友合种解锁" / "情侣空间专属" | `PlantGuideScreen.kt` | 🟡 与 PRD §3.2.2a 一致；但实际解锁逻辑由 `plantRepo.checkUnlocks(totalGp)` 触发（仅 GP 触发，与 PRD 不完全一致，PROGRESS §2） |
| **9 种植物** | HTML 列 9 种（含 1 主植物、2 默认、6 锁定） | `PlantType` 9 种 | ✅ 数量一致 |

### 2.7 achievements.html ↔ AchievementScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **顶部导航** | `class="ach-nav"` 44px + 返回 + "我的" | `TopAppBar` | 🟡 |
| **成就摘要卡** | `class="summary-card"` 9999 圆角 + 渐变 `linear-gradient(145deg, #FFF4D6 0%, #FFE8B8 35%, #FFD89A 70%, #FFCD82 100%)` + 🏆 + "已解锁 8/15" + 进度 + 副标题 | `AchievementScreen.kt:144` `Color(0xFFF5F5F5)` 浅灰硬编码 | ❌ 完全不对齐：HTML 是金色渐变 + 🏆 + 进度；Compose 用灰色背景 + 无渐变 + 无摘要卡结构 |
| **分段控件** | "已解锁 / 未解锁" 2 段 | `AchievementScreen.kt` | 🟡 推测有 |
| **成就卡（8 个）** | `class="ach-card ach-{green/orange/pink/yellow/purple/blue/indigo/gold}"` 6 种色 + emoji + 名称 + 描述 + 日期；`ach-gold.featured` 占 2 列 + "稀有"徽章 | `AchievementScreen.kt:157-159` `Color(0xFFFFF8E1)` 金色高亮 + `Color(0xFFE0E0E0)` 灰色锁定 | 🟡 Compose 用统一金/灰 2 色，HTML 是 6 种语义色 |
| **稀有徽章** | "稀有" 在 `ach-gold.featured` 角 | `Badge` 或自实现 | 🟡 |
| **成就数量** | 8 个已解锁 + "更多惊喜" 暗示更多 | `Achievement.kt` 17 枚 | ❌ HTML 15、Compose 17、PROGRESS 17 — 数量互不一致 |

### 2.8 profile.html ↔ ProfileScreen.kt

| 组件 / 区块 | HTML 写法 | Compose 写法 | 一致性 |
|---|---|---|:---:|
| **大标题"我的"** | `ios-large-title` | `ProfileScreen.kt` | ✅ |
| **页面背景** | `body { background: var(--qx-background-grouped); }` | 默认 | 🟡 HTML 用 grouped（`#F2F1EE`）；Compose 默认是 `--qx-background`（`#FBFBF9`） |
| **Profile 头卡** | `class="profile-header-card"` 9999 圆角 + 头像 72×72 + 渐变 `linear-gradient(135deg, var(--qx-primary-light), var(--qx-primary))` + 名称 + bio + 4 栏统计 | `ProfileScreen.kt` | 🟡 |
| **头像** | 72×72 圆形，emoji 34px | `Box(64.dp)` 圆形（`ios-avatar` CSS 是 64） | ❌ HTML 用 72，CSS `.ios-avatar` 用 64，Compose 不一致 |
| **4 栏统计** | 总记录 / 总 GP / 连续记录 / 植物 | `ProfileScreen.kt` 3 栏（`ProfileScreen.kt:117-153` 中 5 个菜单，3 个有 action） | 🟡 HTML 4 栏、Compose 3 栏（缺"植物"统计） |
| **分组 1：我的花园** | "我的植物" / "成就墙" / "植物图鉴" | 同 | 🟡 `ProfileScreen.kt:117-133` 3 项菜单，icon tint 是 `Color(0xFF5E9B6A / 0xFFF4A261 / 0xFF7AB886)` 硬编码 |
| **分组 2：社交** | "共享空间" / "分享 App" | "共享空间" + TODO 入口 | ❌ HTML "分享 App" 入口，Compose **无**；"共享空间" 与"数据导出" 入口为 TODO |
| **分组 3：其他** | "设置" / "帮助与反馈" / "关于 v1.0.0" | 3 个 TODO `onClick = { /* TODO */ }` | ❌ 完全无 action |
| **底部版本号** | `<div class="version-footer">小确幸 v1.0.0</div>` | 无 | 🆕 |
| **List row 高度** | `class="ios-list-row"` min-height 48px | `ListItem` | 🟡 |
| **chevron** | `<span class="chevron">›</span>` 主色 | `Icon(Icons.Default.ChevronRight, ...)` | ✅ |
| **icon 背景** | `class="ios-list-row-icon"` 32×32 10 圆角 | `Icon` 尺寸由 Material 控制 | 🟡 推测对齐 |

## 3. 跨页面共享组件

> 这些组件在多个 HTML 页面与 Compose Screen 间复用，需统一管理。

### 3.1 状态栏（Status Bar）

- **HTML**：`class="ios-status-bar"` height `env(safe-area-inset-top, 47px)` / min 47px（占位，不画内容）
- **Compose**：`WindowInsets.statusBars` 隐式；`themes.xml:6` `android:windowLightStatusBar = true`（浅色）
- 一致性：✅ 浅色时文字深；`Theme.kt:89` `isAppearanceLightStatusBars = !darkTheme` 与 xml 一致

### 3.2 底部 5 Tab 导航

- **HTML**：`<nav data-mobile-nav="global">` 半透明毛玻璃 `bg-white/78 backdrop-blur-2xl backdrop-saturate-200`，`rounded-[24px]`，`border border-black/[0.06]`，`shadow-[0_8px_32px_rgba(0,0,0,0.08),0_2px_8px_rgba(0,0,0,0.04)]`，h 58px
- **Compose**：`AppNavigation.kt` Material3 `NavigationBar`
- 一致性：❌ 完全不对齐——Material3 NavigationBar 是实色卡片，不是浮动毛玻璃；圆角、阴影、blur 都未对齐
- 严重度：P1（影响整 App 主导航视觉）

### 3.3 中央"记录"按钮

- **HTML**：`class="record-btn"` 48×48 圆形，主色渐变 `linear-gradient(135deg, var(--qx-primary), var(--qx-primary-dark))`，阴影 `0 4px 16px rgba(94,155,106,0.35), 0 2px 6px rgba(94,155,106,0.2)`，margin-top: -18px 抬升
- **Compose**：Material3 `NavigationBarItem` 默认中央按钮（不存在"抬升 18px"的硬规范）
- 一致性：🟡 中央按钮位置 + 圆形存在；抬升、渐变、阴影未对齐
- 严重度：P2

### 3.4 记录卡（共用 RecordCard / .ios-record-card）

- **HTML**：`class="ios-record-card"` background `--qx-surface` + `border-radius: var(--radius-xl)` 18px + 0.5px border `--qx-border-subtle` + `box-shadow: var(--shadow-sm)`
- **Compose**：`ui/components/RecordCard.kt`
- 一致性：🟡 推测对齐
- 严重度：P1

### 3.5 标签 Chip

- **HTML**：`class="ios-chip"` 7/14 padding，13sp/500，9999 圆角，背景 `--qx-gray-100`；selected 背景 `--qx-primary` 文字白
- **HTML mood 变体**：`mood-happy.selected { background: var(--mood-happy); color: #5D4A00; }` 等 5 种
- **Compose**：`ui/components/TagChip.kt` `lum > 0.72f` 选白/黑文字
- 一致性：🟡 5 种 mood 显式规则 vs 亮度阈值；HTML 明确 5 种，Compose 通用

### 3.6 列表行（iOS Grouped List）

- **HTML**：`class="ios-group-card"` + `class="ios-list-row"` 12/16 padding min-height 48px
- **Compose**：Material3 `ListItem` 或 `Card`
- 一致性：🟡 推测对齐

### 3.7 头像（Avatar）

- **HTML / CSS**：`class="ios-avatar"` 64×64，`class="ios-avatar-sm"` 40×40
- **profile.html 用 72×72**（特殊尺寸）
- **Compose**：`ProfileScreen.kt` `Box(64.dp)` 圆
- 一致性：❌ profile.html 用 72，CSS 模板是 64；Compose 用 64。三方不一

## 4. 不一致点清单（按影响面排序）

> 本节是 M0-04 / M1 落地时的优先行动表。

### 4.1 严重（影响主导航 / 整体视觉）

| # | 位置 | HTML 期望 | Compose 现状 | 修复建议 PR |
|---|---|---|---|---|
| S-1 | 底部 5 Tab | 浮动毛玻璃 24 圆角 | Material3 `NavigationBar` 实色 | PR-3（NavBar 改造） |
| S-2 | 记录 CTA banner（album） | 金绿渐变 + "开始创建" 文案 | 仅小图标 | PR-3 |
| S-3 | 成就摘要卡（achievements） | 金色渐变 + 🏆 + 进度 | 灰色 + 无渐变 + 无摘要结构 | PR-6 |

### 4.2 中（影响单页视觉）

| # | 位置 | HTML 期望 | Compose 现状 | 修复建议 PR |
|---|---|---|---|---|
| M-1 | 主按钮"记录小确幸" | 14/24 padding, 17/600, 14 圆角, 自定义阴影 | Material 默认 | PR-3 |
| M-2 | 文字输入 textarea | 14/16 padding, 17/400, 14 圆角, `--qx-surface-secondary` 背景 | OutlinedTextField | PR-4 |
| M-3 | 发布按钮 | height 56px | Material 默认 | PR-4 |
| M-4 | 头像尺寸 | 72×72（profile） | 64×64 | PR-6 |
| M-5 | profile 4 栏统计 | 4 项 | 3 项（缺"植物"） | PR-6 |
| M-6 | profile 底部版本号 | "小确幸 v1.0.0" | 无 | PR-6 |
| M-7 | 暗色分享页状态栏 | 暗色文字 | 由主题控制，可能未深色 | PR-5 |
| M-8 | 时间线分段控件 | iOS Segmented | **无** | PR-5 |
| M-9 | 心情 chip 选中文字色 | 5 种明确规则 | 亮度阈值通用 | PR-3 |
| M-10 | GP 进度条高度 | 6px | 4px（Material） | PR-3 |
| M-11 | 时间线 ❤️/💬 按钮 | 有 | **无**（共享空间未实现） | 不在本轮修复；plan P1 |

### 4.3 轻（仅影响细节）

| # | 位置 | HTML 期望 | Compose 现状 | 修复建议 PR |
|---|---|---|---|---|
| L-1 | 状态栏占位 | 47px 显式 | 系统 inset | 接受现状 |
| L-2 | 顶部导航高度 | 44px（iOS） | 64dp（Material） | 接受现状（Material3 TopAppBar） |
| L-3 | 状态 chip 数量 | 14 个 | 12 个 | 需产品方补：宠物/约会归属 |
| L-4 | 心情 chip 数量 | 8 个 | 9 种 | 需产品方补：HTML 缺哪个、Compose 哪个多余 |
| L-5 | 成就数量 | 15 个（HTML） | 17 枚（PROGRESS） | 需设计方补：HTML 漏列、Compose 多列 |
| L-6 | 圆形"+" 按钮 | 56px | 推测 56dp | PR-3 |
| L-7 | profile 背景 | `--qx-background-grouped` | 默认 `--qx-background` | PR-6 |
| L-8 | 画册封面渐变 | 6 种 | 推测硬编码 | PR-5 |

## 5. 需产品方 / 设计方决策的问题

> 本节与 design-tokens.md §9 同步；在未决策前不要动源码。

1. **状态 chip 数量** —— HTML 14 个 vs Compose 12 个：缺哪两个？多哪两个？
   候选差集：HTML 独有"宠物 🐱" / Compose 独有"放松"或重名。
2. **心情 chip 数量** —— HTML 8 个 vs Compose 9 种：哪个多余？
3. **成就数量** —— HTML 15 vs Compose 17：是否补"双向奔赴"等 4 个未列入 HTML？
4. **profile 4 栏 vs 3 栏** —— 缺"植物"统计，是 HTML 设计多余还是 Compose 漏实现？
5. **profile 头像尺寸** —— HTML 72 / CSS 64 / Compose 64：哪个是规范？
6. **profile 底部版本号** —— HTML "v1.0.0" 硬编码；Compose 是否要显示 BuildConfig 版本？（按迭代计划 M2-05）
7. **画册封面卡** —— HTML 6 个 + 渐变 + 装饰 emoji；Compose 当前是 Demo；下一轮 I3 之前无需对齐。
8. **M0-02 已记录的**：MoodSad/MoodCalm 同色、MoodSurprise/MoodWarm 同色（design-tokens.md §2.3）。
9. **深色调色板** —— HTML 无 dark，Compose 自创（design-tokens.md §3.2）。
10. **中央"记录"按钮抬升 18px** —— HTML 用 `margin-top: -18px`；Material3 NavigationBarItem 不支持相同视觉效果。

## 6. 下一轮接手建议

1. **本表是 M0-04（设计 Token 重构）PR 描述的"对照清单"**。每个 PR 改动哪个页面，
   必先回到 §2 / §3 找到对应行，再判断一致性。
2. **M0-04 PR 拆分（design-tokens.md §12）**：本表可作为"哪些页面的哪些组件要优先 token 化"的依据。
3. **新增页面或组件时**，先在 §2 追加新行；本表是滚动更新的真值表。
4. **M1 期间**（记录 UI 改造）若发现 HTML 与 Compose 行为差异，按以下优先级处理：
   - HTML 是设计真值，且 PRD 支持 → 改 Compose
   - HTML 与 PRD 冲突 → 标"待确认"（design-tokens.md §9 / 本文档 §5）
   - Compose 独有且 PRD 不反对 → 标"🆕 Compose 独有"保留

## 7. 版本

- v0.1（2026-08-27，M0-05 任务首次产出）
- 下次更新：M0-04 PR-1 完成后（QxColors 落地后回填）
