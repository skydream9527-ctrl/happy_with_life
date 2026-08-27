# Android 设计 Token（M0-02 任务，2026-08-27）

> 范围：把 [`../xiaoquexing-ios-redesign/colors_and_type.css`](../xiaoquexing-ios-redesign/colors_and_type.css)
> 与 8 个 HTML 高保真页面映射为 Android Compose 设计 Token 清单。
>
> 本轮**只输出设计规范**，不重构 `ui/theme/*` 与 `ui/components/*` 的代码。
> 任何 Compose 代码修改属于下一轮（应单独派 Z code/MiniMax Code 任务卡）。

## 0. 摘要

| 类别 | CSS Token 数 | Compose 已映射 | 缺失/未统一 |
|---|---:|---:|---:|
| 品牌主色 + 衍生 | 7 | 4 | `--qx-primary-lighter`、`--qx-primary-50`、`--qx-primary-200` |
| 中性灰阶 | 10 | 0 | `gray-50..900` 在 Compose 中没有命名 token，散落在多处硬编码 |
| 语义背景 | 6 | 3 | `background-elevated`、`background-grouped`、`surface-tertiary` 未命名 |
| 文字 | 6 | 3 | `foreground-tertiary/quaternary/inverse` 未独立命名 |
| 边框 | 4 | 1 | `border-subtle`、`separator`（半透明）未映射 |
| 状态色 | 4 | 1 | success/warning/info 未命名（仅 error 来自 Material） |
| 心情/强调 | 6 | 10 | Mood 颜色已有枚举，但**未与 CSS 一一对应** |
| 圆角 | 6 | 1 | 仅在 `RoundedCornerShape(18.dp)` 等处硬编码，无统一命名 |
| 阴影 | 6 | 0 | 全部以 `Color.Black.copy(alpha=0.04f)` 等方式散落硬编码 |
| 间距 | 8 | 0 | 全部以 `.padding(8.dp)` 等散落硬编码 |
| 字号 | 9 | 13 | Compose `Typography` 已定义，但**与 CSS 不完全一致**，且页面仍用 `.sp` 硬编码 |

详细清单见 §4 与 §5。

## 1. 来源

### 1.1 一级来源（CSS Token）

- [`../xiaoquexing-ios-redesign/colors_and_type.css`](../xiaoquexing-ios-redesign/colors_and_type.css)
  完整定义 9 个分类共 80+ 个 token。
- 8 个 HTML 高保真页面（`home/record/timeline/album/share/plant-guide/achievements/profile`）
  全部内联同一份 CSS 变量，**未发现任何页面覆写**。本文件视其为同一来源。

### 1.2 二级来源（当前 Compose）

- `ui/theme/Color.kt`（49 行）
- `ui/theme/Type.kt`（95 行）
- `ui/theme/Theme.kt`（97 行）
- `res/values/colors.xml`（22 个 `@color/*`，与 Compose Color **不一致**）
- `res/values/themes.xml`（XML 主题，父类为 `Theme.Material.Light.NoActionBar`）

### 1.3 三级来源（页面/组件中的硬编码）

- `ui/home/HomeScreen.kt`、`ui/components/MusicCard.kt`、`ui/components/ShareCardGenerator.kt`、
  `ui/components/AchievementBadge.kt`、`ui/components/TagChip.kt`、
  `ui/profile/ProfileScreen.kt`、`ui/profile/AchievementScreen.kt`、
  `ui/share/ShareScreen.kt`、`ui/album/AlbumViewerScreen.kt`、`ui/record/RecordScreen.kt`。
- 详细清单见 §5。

## 2. 品牌色与语义色

### 2.1 品牌主色

| 名称 | CSS Token | CSS 值 | Compose 已映射 | Compose 值 | 说明 |
|---|---|---|:---:|---|---|
| Brand Primary | `--qx-primary` | `#5E9B6A` | ✅ | `GreenPrimary = 0xFF5E9B6A` | 鼠尾草绿，主操作色 |
| Brand Primary Light | `--qx-primary-light` | `#7AB886` | ✅ | `GreenLight = 0xFF7AB886` | 次级强调 |
| Brand Primary Lighter | `--qx-primary-lighter` | `#A8D5B0` | ❌ | — | 应新增 `GreenLighter` |
| Brand Primary Dark | `--qx-primary-dark` | `#4A7D54` | ✅ | `GreenDark = 0xFF4A7D54` | 主操作按下 |
| Brand Primary 50 | `--qx-primary-50` | `#F0F7F1` | 🟡 | `Theme.Light.secondaryContainer = 0xFFF0F7F1`（内联） | 应命名为 `GreenContainerLowest` |
| Brand Primary 100 | `--qx-primary-100` | `#DCEFDF` | ✅ | `GreenContainer = 0xFFDCEFDF` | `primaryContainer` |
| Brand Primary 200 | `--qx-primary-200` | `#B9DFBF` | ❌ | — | 应新增 `GreenContainerMid` |
| Brand Primary Container（透明） | `--qx-primary-container` | `rgba(94,155,106,0.08)` | ❌ | — | 应用于 chip/二级按钮 |
| Brand Primary Container Strong | `--qx-primary-container-strong` | `rgba(94,155,106,0.12)` | ❌ | — | |

### 2.2 状态色（State）

| 名称 | CSS Token | CSS 值 | Compose 已映射 | 说明 |
|---|---|---|:---:|---|
| Success | `--state-success` | `#34C759` | ❌ | 应新增 `Success` |
| Warning | `--state-warning` | `#FF9500` | ❌ | 应新增 `Warning` |
| Error | `--state-error` | `#FF3B30` | ✅ | `Theme.Light.error = 0xFFFF3B30`（内联） |
| Info | `--state-info` | `#007AFF` | ❌ | 应新增 `Info` |

### 2.3 心情 / 强调（Mood / Accent）

| 名称 | CSS Token | CSS 值 | Compose `Mood*` | Compose 值 | 一致性 |
|---|---|---|---|---|---|
| Happy | `--mood-happy` | `#F5C842` | `MoodHappy` | `0xFFF5C842` | ✅ |
| Warm | `--mood-warm` | `#F4A261` | `OrangeAccent` | `0xFFF4A261` | ✅ |
| Excited | `--mood-excited` | `#FF7043` | `MoodExcited` | `0xFFFF7043` | ✅ |
| Calm | `--mood-calm` | `#7EC8CE` | `MoodCalm` | `0xFF7EC8CE` | ✅ |
| Love | `--mood-love` | `#F48FB1` | `PinkAccent` / `MoodTouched` | `0xFFF48FB1` | ✅ |
| Grateful | `--mood-grateful` | `#CE93D8` | `MoodGrateful` | `0xFFCE93D8` | ✅ |
| Touched（PRD 心情） | — | — | `MoodTouched` = `0xFFF48FB1` | 与 `--mood-love` 同色，命名混乱 |
| Miss | — | — | `MoodMiss = 0xFFB0ADA6` | 复用 `--qx-gray-400`；CSS 没有 |
| Tired | — | — | `MoodTired = 0xFF8A8780` | 复用 `--qx-gray-500`；CSS 没有 |
| Sad | — | — | `MoodSad = 0xFF7EC8CE` | 与 `--mood-calm` **同色**；命名不一致 |
| Angry | — | — | `MoodAngry = 0xFFFF6B5E` | CSS 没有 |
| Surprise | — | — | `MoodSurprise = 0xFFF4A261` | 与 `--mood-warm` **同色**；命名不一致 |
| Brown Trunk（植物素材） | — | — | `BrownTrunk = 0xFF8B6F4E` | 非 CSS 来源；视觉素材色 |

**问题：**
- `MoodSad` 与 `MoodCalm` 同色、`MoodSurprise` 与 `MoodWarm` 同色——违反"一种心情一色"原则。
- PRD §3.1.2 列了 9 种心情（开心/感动/平静/兴奋/满足/温暖/惊喜/放松/想念/感恩），但当前 `MoodTag` 枚举有 9 种：Happy / Calm / Excited / Touched / Miss / Tired / Sad / Angry / Surprise / Grateful；命名和数量都不完全对齐。
- 下一轮 M1 之前，应由 Z code 决定"是否按 PRD 重命名 + 重新分配颜色"。

## 3. 浅色 / 深色主题映射

### 3.1 浅色

| CSS 用途 | CSS Token | CSS 值 | Compose `lightColorScheme` 字段 | Compose 值 |
|---|---|---|---|---|
| App background | `--qx-background` | `#FBFBF9` | `background` | `BackgroundLight = 0xFFFBFBF9` ✅ |
| Card / Surface | `--qx-surface` | `#FFFFFF` | `surface` | `CardBg = 0xFFFFFFFF` ✅ |
| Surface secondary（输入框/分组） | `--qx-surface-secondary` | `#F7F6F4` | `surfaceVariant` | `GreenBg = 0xFFF7F6F4` ✅ |
| Surface tertiary | `--qx-surface-tertiary` | `#F0EFEB` | ❌ | — |
| Background elevated | `--qx-background-elevated` | `#FFFFFF` | （同 surface） | — |
| Background grouped | `--qx-background-grouped` | `#F2F1EE` | ❌ | — |
| Primary | `--qx-primary` | `#5E9B6A` | `primary` | `GreenPrimary` ✅ |
| On primary | `--qx-primary-foreground` | `#FFFFFF` | `onPrimary` | `Color.White` ✅ |
| Primary container | `--qx-primary-100` | `#DCEFDF` | `primaryContainer` | `GreenContainer` ✅ |
| On primary container | — | — | `onPrimaryContainer` | `OnGreenContainer = 0xFF2D4A34` 🟡 应取自 CSS（`--qx-foreground`） |
| Secondary | `--qx-primary-light` | `#7AB886` | `secondary` | `GreenLight` ✅ |
| Secondary container | `--qx-primary-50` | `#F0F7F1` | `secondaryContainer` | `0xFFF0F7F1` 内联，未命名 |
| Tertiary | `--mood-warm` | `#F4A261` | `tertiary` | `OrangeAccent` ✅ |
| On background | `--qx-foreground` | `#1A1918` | `onBackground` | `TextPrimary = 0xFF1A1918` ✅ |
| On surface | `--qx-foreground` | `#1A1918` | `onSurface` | `TextPrimary` ✅ |
| On surface variant | `--qx-foreground-secondary` | `#6B6862` | `onSurfaceVariant` | `TextSecondary` ✅ |
| Error | `--state-error` | `#FF3B30` | `error` | `0xFFFF3B30` 内联 |
| On error | — | — | `onError` | `Color.White` ✅ |
| Outline | `--qx-gray-400` | `#B0ADA6` | `outline` | `0xFFB0ADA6` 内联，未命名 |
| Outline variant | `--qx-separator-opaque` | `#E5E4E0` | `outlineVariant` | `SeparatorColor = 0xFFE5E4E0` ✅ |
| Inverse surface | — | — | `inverseSurface` | `TextPrimary` 🟡（语义略不匹配） |
| Inverse on surface | — | — | `inverseOnSurface` | `BackgroundLight` ✅ |
| Surface tint | `--qx-primary` | `#5E9B6A` | `surfaceTint` | `GreenPrimary` ✅ |

### 3.2 深色

| 用途 | CSS 来源 | Compose `darkColorScheme` 字段 | Compose 值 | 说明 |
|---|---|---|---|---|
| App background | — | `background` | `DarkBackground = 0xFF161815` | CSS 无 dark 变量，**自创** |
| Card / Surface | — | `surface` | `DarkSurface = 0xFF1E211D` | 自创 |
| Surface variant | — | `surfaceVariant` | `DarkCard = 0xFF282B26` | 自创 |
| Primary | `--qx-primary-light` | `primary` | `GreenDarkPrimary = 0xFF7AB886` | 用 light 替代 |
| On primary | — | `onPrimary` | `0xFF10130F` | 自创 |
| Primary container | — | `primaryContainer` | `0xFF33503A` | 自创 |
| Secondary | `--qx-primary-light` | `secondary` | `GreenLight` | 同浅色 |
| Secondary container | — | `secondaryContainer` | `0xFF2A3D2E` | 自创 |
| Tertiary | `--mood-warm` | `tertiary` | `OrangeAccent` | 同浅色 |
| On background / on surface | — | `onBackground` / `onSurface` | `DarkTextPrimary = 0xFFE8E7E3` | 自创 |
| On surface variant | — | `onSurfaceVariant` | `DarkTextSecondary = 0xFFA0A29B` | 自创 |
| Outline | — | `outline` | `0xFF6B6862` | 自创 |
| Outline variant | — | `outlineVariant` | `0xFF3A3D37` | 自创 |
| Error | — | `error` | `0xFFFF6B5E` | 自创（与 iOS `#FF3B30` 不一致） |

**问题：**
- 深色主题完全在 Compose 中**自创**，没有 CSS dark 变量作为真值来源。
- 浅色 `onPrimaryContainer = 0xFF2D4A34` 是手挑，CSS 没有相同定义。
- `inverseSurface = TextPrimary` 把"反色 surface"指向文字色，语义不匹配（CSS 缺 `inverse*`）。

## 4. 背景、Surface、文字、边框

### 4.1 背景与 Surface

| 用途 | CSS | 建议 Compose Token | 当前 Compose | 一致性 |
|---|---|---|:---:|---|
| App background | `--qx-background` `#FBFBF9` | `QxBackground` | `BackgroundLight` | ✅ |
| Surface | `--qx-surface` `#FFFFFF` | `QxSurface` | `CardBg` | ✅ |
| Surface secondary（输入框） | `--qx-surface-secondary` `#F7F6F4` | `QxSurfaceSecondary` | `GreenBg` | ✅ 命名不一致 |
| Surface tertiary | `--qx-surface-tertiary` `#F0EFEB` | `QxSurfaceTertiary` | — | ❌ |
| Background elevated | `--qx-background-elevated` `#FFFFFF` | （同 surface） | — | — |
| Background grouped | `--qx-background-grouped` `#F2F1EE` | `QxBackgroundGrouped` | — | ❌ |

### 4.2 文字

| 用途 | CSS | 建议 Compose Token | 当前 Compose |
|---|---|---|:---:|
| 主文字 | `--qx-foreground` `#1A1918` | `QxForeground` | `TextPrimary` ✅ |
| 次文字 | `--qx-foreground-secondary` `#6B6862` | `QxForegroundSecondary` | `TextSecondary` ✅ |
| 三级文字 | `--qx-foreground-tertiary` `#8A8780` | `QxForegroundTertiary` | — |
| 占位/无障碍 | `--qx-foreground-quaternary` `#B0ADA6` | `QxForegroundQuaternary` | — |
| 反色 | `--qx-foreground-inverse` `#FFFFFF` | `QxForegroundInverse` | — |
| 品牌前景（按钮文字） | `--qx-primary-foreground` `#FFFFFF` | `QxPrimaryForeground` | `Color.White` 内联 🟡 |

### 4.3 边框

| 用途 | CSS | 建议 Compose Token | 当前 Compose |
|---|---|---|:---:|
| 弱边框（卡片） | `--qx-border-subtle` `rgba(0,0,0,0.06)` | `QxBorderSubtle` | — |
| 强边框 | `--qx-border-strong` `rgba(0,0,0,0.1)` | `QxBorderStrong` | — |
| 分隔线（半透明） | `--qx-separator` `rgba(60,60,67,0.12)` | `QxSeparator` | — |
| 分隔线（不透明） | `--qx-separator-opaque` `#E5E4E0` | `QxSeparatorOpaque` | `SeparatorColor` ✅ |

## 5. 字号、字重、圆角、间距、阴影、组件尺寸

### 5.1 字号与字重（CSS → Compose `Typography`）

| CSS class | CSS size / weight | Compose `Typography` 字段 | Compose size / weight | 一致性 |
|---|---|---|---|---|
| `.ios-large-title` | 34 / 700 | `displayLarge` | 34 / Bold | ✅ |
| `.ios-title-1` | 28 / 700 | `displayMedium` | 28 / Bold | ✅ |
| `.ios-title-2` | 22 / 600 | `headlineLarge` | 22 / Bold ❌（CSS 是 600，Compose 是 Bold 700） | 🟡 |
| `.ios-title-3` | 20 / 600 | `headlineMedium` | 20 / SemiBold | ✅ |
| `.ios-headline` | 17 / 600 | `headlineSmall` / `titleLarge` | 17 / SemiBold | ✅ |
| `.ios-body` | 15 / 400 | `bodyMedium` | 15 / Normal | ✅ |
| `.ios-callout` | 16 / 400 | `bodyLarge` | 17 ❌（CSS 16，Compose 17） | 🟡 |
| `.ios-subhead` | 13 / 400 | `bodySmall` | 13 / Normal | ✅ |
| `.ios-footnote` | 11 / 400 | `labelSmall` | 11 / Medium ❌（CSS 400，Compose 500 Medium） | 🟡 |
| `.ios-caption` | 10 / 500 + uppercase | `labelSmall` | 11 / Medium（被通用化） | 🟡 |
| CSS `--font-size-md` | 16 | （无对应 Material role） | — | ❌ |
| CSS `--font-size-2xl` | 22 | `headlineLarge` | 22 / Bold（见上） | 🟡 |
| CSS `--font-size-3xl` | 28 | `displayMedium` | 28 / Bold | ✅ |
| CSS `--font-size-hero` | 34 | `displayLarge` | 34 / Bold | ✅ |

**问题：**
- `headlineLarge` 用了 700（Bold）而非 CSS 的 600（SemiBold）。
- `bodyLarge` 用了 17 而非 16。
- `labelSmall` 用了 500（Medium）而非 400（Normal）。
- CSS 10px 的 caption 在 Compose 中**没有对应 10sp**，最接近是 11sp。
- `letter-spacing` CSS `-0.02em` / `-0.015em` / `-0.01em` 三档被 Compose 简化为 1 档（`-0.4..-0.1 sp`），等价但**非精确映射**。

### 5.2 圆角

| CSS Token | CSS 值 | Compose 出现 | 一致性 |
|---|---|---|:---:|
| `--radius-sm` | 6px | `RoundedCornerShape(8.dp)` 等位置硬编码 8（不一致） | ❌ |
| `--radius-md` | 10px | `RoundedCornerShape(12.dp)`、`8.dp`（MusicCard、AlbumViewerScreen、RecordCard） | 🟡 |
| `--radius-lg` | 14px | `RoundedCornerShape(14.dp)`（HomeScreen） | ✅ 仅一处 |
| `--radius-xl` | 18px | `RoundedCornerShape(18.dp)`（HomeScreen） | ✅ 仅一处 |
| `--radius-2xl` | 24px | — | ❌ |
| `--radius-full` | 9999px | `CircleShape` / `RoundedCornerShape(50)` | 🟡 命名不一致 |

**建议新增 Compose Token：**
```kotlin
object QxRadius {
  val sm: Dp = 6.dp
  val md: Dp = 10.dp
  val lg: Dp = 14.dp
  val xl: Dp = 18.dp
  val xxl: Dp = 24.dp
  val full: Dp = 9999.dp
}
```

### 5.3 间距

| CSS Token | CSS 值 | Compose 出现 |
|---|---|---|
| `--spacing-xs` | 4px | `Spacer(4.dp)` 散落 |
| `--spacing-sm` | 8px | `Spacer(8.dp)`、`.padding(8.dp)` 大量散落 |
| `--spacing-md` | 12px | 散落 |
| `--spacing-base` | 16px | 散落（最常用） |
| `--spacing-lg` | 20px | 散落 |
| `--spacing-xl` | 24px | 散落 |
| `--spacing-2xl` | 32px | 散落 |
| `--spacing-3xl` | 48px | 散落 |

**问题：** Compose 中**完全没有命名间距 token**，全部以 `.dp` 字面量散落。
**建议：** 下一轮新增：
```kotlin
object QxSpacing {
  val xs: Dp = 4.dp
  val sm: Dp = 8.dp
  val md: Dp = 12.dp
  val base: Dp = 16.dp
  val lg: Dp = 20.dp
  val xl: Dp = 24.dp
  val xxl: Dp = 32.dp
  val xxxl: Dp = 48.dp
}
```

### 5.4 阴影

| CSS Token | CSS 值 | Compose 出现 |
|---|---|---|
| `--shadow-xs` | `0 1px 2px rgba(0,0,0,0.03)` | — |
| `--shadow-sm` | `0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02)` | 卡片默认阴影（Material3 defaultElevation） |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,0.05), 0 1px 3px rgba(0,0,0,0.03)` | — |
| `--shadow-lg` | `0 8px 24px rgba(0,0,0,0.06), 0 2px 8px rgba(0,0,0,0.04)` | — |
| `--shadow-xl` | `0 16px 40px rgba(0,0,0,0.08), 0 4px 12px rgba(0,0,0,0.04)` | — |
| `--shadow-float` | `0 2px 8px rgba(0,0,0,0.04), 0 0 1px rgba(0,0,0,0.02)` | 记录卡按钮 |

**问题：** Compose 中**完全没有命名阴影 token**；`HomeScreen` 多处用
`Color.Black.copy(alpha = 0.04f)` 散落硬编码实现。
**建议：** 下一轮定义 `QxElevation` / `QxShadow` 包装 `Modifier.shadow`。

### 5.5 组件尺寸

| 组件 | CSS 尺寸 | Compose 出现 | 一致性 |
|---|---|---|:---:|
| 大头像 `.ios-avatar` | 64 × 64，圆 50% | — | ❌ 未实现 |
| 小头像 `.ios-avatar-sm` | 40 × 40 | — | ❌ |
| 列表行高 `.ios-list-row` | min-height 48px | ListItem / Card 内 | 🟡 部分 |
| List row icon | 32 × 32，10px 圆角 | 散落 | ❌ |
| Tag chip `.ios-chip` | 7px×14px padding, 13px 字号, 9999 圆 | `TagChip.kt` | 🟡 padding 不一致 |
| 主按钮 `.ios-btn` | 14×24 padding, 17px 字号, 14 圆角 | Material `Button` 默认 | ❌ |
| 大按钮 `.ios-btn-large` | 16 padding, 17px 字号 | — | ❌ |
| 输入框 `.ios-input` | 14×16 padding, 17px 字号, 14 圆角 | `OutlinedTextField` 默认 | ❌ |
| 多行输入 `.ios-textarea` | 14×16 padding, 17px 字号, 14 圆角, min 120 | `TextField` 默认 | ❌ |
| 进度条 `.ios-progress` | 6px 高，9999 圆 | `LinearProgressIndicator` 默认 | ❌ |
| 中央记录按钮 `.record-btn` | 48×48，圆形，16/6 阴影 | `AddContentButton.kt` | 🟡 |
| 状态栏 | 47px | `WindowCompat` | ✅ |
| 导航大标题 | 20px padding | `TopAppBar` 默认 | 🟡 |

## 6. CSS Token → Compose Token 映射（汇总表）

按 CSS token 列出"应映射的 Compose 名 + 当前是否已映射 + 缺什么"。

| CSS Token | 建议 Compose 名称 | 当前 Compose 状态 | 缺什么 |
|---|---|---|---|
| `--qx-primary` | `QxBrandPrimary` | `GreenPrimary` | 改名为 `QxBrandPrimary`，删除旧名 |
| `--qx-primary-light` | `QxBrandPrimaryLight` | `GreenLight` | 同上 |
| `--qx-primary-lighter` | `QxBrandPrimaryLighter` | — | 新增 |
| `--qx-primary-dark` | `QxBrandPrimaryDark` | `GreenDark` | 同上 |
| `--qx-primary-50` | `QxBrandPrimary50` | 内联 0xFFF0F7F1 | 命名 |
| `--qx-primary-100` | `QxBrandPrimary100` | `GreenContainer` | 命名 |
| `--qx-primary-200` | `QxBrandPrimary200` | — | 新增 |
| `--qx-primary-container` | `QxBrandContainer` | — | 新增（透明） |
| `--qx-gray-50..900` | `QxGrayNN` | — | 整套新增 |
| `--qx-background` | `QxBackground` | `BackgroundLight` | 改名 |
| `--qx-background-elevated` | `QxBackgroundElevated` | — | 新增 |
| `--qx-background-grouped` | `QxBackgroundGrouped` | — | 新增 |
| `--qx-surface` | `QxSurface` | `CardBg` | 改名 |
| `--qx-surface-secondary` | `QxSurfaceSecondary` | `GreenBg` | 改名 |
| `--qx-surface-tertiary` | `QxSurfaceTertiary` | — | 新增 |
| `--qx-foreground` | `QxForeground` | `TextPrimary` | 改名 |
| `--qx-foreground-secondary` | `QxForegroundSecondary` | `TextSecondary` | 改名 |
| `--qx-foreground-tertiary` | `QxForegroundTertiary` | — | 新增 |
| `--qx-foreground-quaternary` | `QxForegroundQuaternary` | — | 新增 |
| `--qx-foreground-inverse` | `QxForegroundInverse` | — | 新增 |
| `--qx-primary-foreground` | `QxPrimaryForeground` | `Color.White` 内联 | 命名 |
| `--qx-border-subtle` | `QxBorderSubtle` | — | 新增 |
| `--qx-border-strong` | `QxBorderStrong` | — | 新增 |
| `--qx-separator` | `QxSeparator` | — | 新增（半透明） |
| `--qx-separator-opaque` | `QxSeparatorOpaque` | `SeparatorColor` | 改名 |
| `--state-success` | `QxStateSuccess` | — | 新增 |
| `--state-warning` | `QxStateWarning` | — | 新增 |
| `--state-error` | `QxStateError` | 内联 0xFFFF3B30 | 命名 |
| `--state-info` | `QxStateInfo` | — | 新增 |
| `--mood-happy..grateful` | `QxMoodHappy` 等 | `MoodHappy` 等 | 命名规范化 |
| `--radius-sm..2xl` | `QxRadius.*` | 散落硬编码 | 整套新增 |
| `--radius-full` | `QxRadiusFull` | 散落 | 命名 |
| `--shadow-xs..xl` | `QxElevation.*` / `QxShadow.*` | 散落硬编码 | 整套新增 |
| `--spacing-xs..3xl` | `QxSpacing.*` | 散落硬编码 | 整套新增 |
| `--font-size-*` | `QxTextStyle.*` | `MaterialTheme.typography` + 散落 | 整合并对齐 CSS |

## 7. 当前 Android 中未统一的硬编码颜色与魔法数值

> 全部按"文件名:行号 → 字面量 → 期望对应 token"格式列出。
> 实际定位依据为 `rg "Color\(0xFF[0-9A-Fa-f]{6}\)|Color\.White|Color\.Black|Color\.Gray|Color\.LightGray"` 扫描结果
> 与本轮 M0-02 任务抽样（行号以代码当前状态为准，下一轮前可重核）。

### 7.1 硬编码颜色（应替换为 token）

| 文件 | 位置 | 字面量 | 期望 Token |
|---|---|---|---|
| `ui/theme/Theme.kt` | 18-67 | `Color(0xFFF0F7F1)`、`Color(0xFF33503A)`、`Color(0xFFDCEFDF)`、`Color(0xFF2A3D2E)`、`Color(0xFFFF3B30)`、`Color(0xFFFF6B5E)`、`Color(0xFFB0ADA6)`、`Color(0xFF10130F)` 等 10+ 处 | `QxBrandPrimary50/100`、`QxStateError` 等 |
| `ui/theme/Color.kt` | 9-49 | 12 个 `Color(0xFF...)` | 重命名为 `Qx*` 系列 |
| `ui/record/RecordScreen.kt` | 121-401 | `Color.Black.copy(alpha=0.3f)`、`Color.White`（多次） | `QxShadowFloat`、`QxForegroundInverse` |
| `ui/home/HomeScreen.kt` | 158-432 | `Color.Black.copy(alpha=0.04f/0.03f)`、`Color(0xFFE8F3EA)`、`Color(0xFFF4F9F2)`、`Color(0xFFFBFAF5)`、`Color(0xFFEBE9E5)`、`Color.White` | 渐变背景 → `QxBrandPrimary50/100`，阴影 → `QxShadowSm` |
| `ui/profile/ProfileScreen.kt` | 118-153 | `Color(0xFF5E9B6A)`、`Color(0xFFF4A261)`、`Color(0xFF7AB886)`、`Color(0xFF7EC8CE)`、`Color(0xFF9E9E9E)`、`Color(0xFFE91E63)` | `QxBrandPrimary`、`QxMoodWarm`、`QxBrandPrimaryLight`、`QxMoodCalm`、`QxGray500`、未命名（Pink） |
| `ui/profile/AchievementScreen.kt` | 144-213 | `Color(0xFFF5F5F5)`、`Color(0xFFFFF8E1)`、`Color(0xFFE0E0E0)`、`Color(0xFF9E9E9E)` | 锁定态背景应使用 `QxSurfaceSecondary` 等 |
| `ui/components/TagChip.kt` | 48 | `Color(0xFF3A3226)` | 文字色（亮背景上的暗文字）应统一 |
| `ui/components/AchievementBadge.kt` | 56-57 | `Color(0xFFFFD700).copy(alpha=0.3f)`、`Color.Gray.copy(alpha=0.2f)` | 成就高亮金色未命名 |
| `ui/components/MusicCard.kt` | 53-171 | `Color(0xFFF3E5F5)`、`Color(0xFFCE93D8)`、`Color(0xFFE3F2FD)`、`Color(0xFF90CAF9)`、`Color(0xFFFFF3E0)`、`Color.White` | 链接/地点/音乐三类卡片各硬编码一种 Material 100 色调，应建 `QxCardLinkBackground` 等 |
| `ui/components/ShareCardGenerator.kt` | 83-129 | `Color(0xFFE8F5E9)`、`Color(0xFFC8E6C9)`、`Color(0xFF757575)`、`Color(0xFF212121)`、`Color.White.copy(alpha=0.5f)` | 分享卡背景/文字应来自 token |
| `ui/share/ShareScreen.kt` | 156-204 | `Color(0xFF07C160)`（微信绿 × 2）、`Color(0xFFFF2442)`（小红书红）、`Color(0xFFE6162D)`（微博红）、`Color.White` | 第三方品牌色，建议建 `QxChannelWechat/Xiaohongshu/Weibo` 命名 |
| `ui/album/AlbumViewerScreen.kt` | 233-280 | `Color(0xFFF3E5F5)`、`Color.Gray`、`Color.LightGray` | 翻页指示器颜色 |
| `res/values/colors.xml` | 3-21 | 19 个 `@color/*` 与 Compose Color **不一致**（如 `green_primary = #4CAF50` 而 Compose 是 `#5E9B6A`） | 与 Compose 同步后只保留 launcher / 系统级需要的色 |
| `res/values/themes.xml` | 5 | `android:color/white` 硬编码 | 与 dark mode 兼容 |
| `ui/record/RecordScreen.kt` | 392-400 | `Color.White` 用于成功提示框 | `QxForegroundInverse` |

### 7.2 硬编码数值（间距/圆角/尺寸）

- 所有 `Spacer(Modifier.height(N.dp))` 与 `.padding(N.dp)` 中的 `N` 都是魔法数字。
- `RoundedCornerShape(8/12/14/18.dp)` 至少 4 种不同圆角直接硬编码。
- `Modifier.size(20.dp)`、`size(24.dp)`、`size(18.dp)` 等图标尺寸硬编码。
- `CardDefaults.cardElevation(defaultElevation = 0.dp)` 散落 5+ 处。

### 7.3 硬编码阴影/光效

- `HomeScreen.kt` 的 `ambientColor = Color.Black.copy(alpha=0.04f)` 与 `spotColor` 散落 3 处。
- `Box` / `Surface` 上的 elevation 与 shadow 常量未被任何 token 收口。

## 8. 后续 Compose 组件应遵守的命名与使用规则

> 本节是**给下一轮 M1 的硬约束**，写在文档里以便 review 时直接对照。
> 任何在 M1 起新增的 Compose 组件都必须先满足本节，再写实现。

### 8.1 命名

- 颜色：`Qx` 前缀 + 用途（`QxBrandPrimary`、`QxSurface`、`QxMoodCalm`）。
- 间距：`QxSpacing` object 内的 `xs/sm/md/base/lg/xl/xxl/xxxl`。
- 圆角：`QxRadius` object 内的 `sm/md/lg/xl/xxl/full`。
- 阴影：`QxElevation` object（与 Material3 ElevationTokens 对齐）+ 可选 `QxShadow` Modifier 扩展。
- 字号 / 字重：通过 `MaterialTheme.typography` 访问，不允许在页面里再写 `fontSize = X.sp`。
- 第三方品牌色：`QxChannelXxx`（如 `QxChannelWechat`）。

### 8.2 使用规则

1. **禁止**在 Composable 内直接 `Color(0xFF...)` / `Color.White` / `Color.Black` /
   `Color.Gray` / `Color.LightGray` / `Color.DarkGray`，全部走 token。
2. **禁止**在 Composable 内写 `X.dp` 的魔法数字：间距用 `QxSpacing.*`，圆角用 `QxRadius.*`，
   图标尺寸用 `QxIconSize.*`。
3. **禁止**在 `Color(0xFF...)` 出现 `alpha=` 任意值；需要透明时使用对应半透明 token
   （`QxBorderSubtle`、`QxSeparator`、`QxShadowFloat` 等）。
4. **必须**通过 `MaterialTheme.colorScheme` 访问 light/dark 颜色，**不要**直接引用
   `GreenPrimary` / `BackgroundLight` 等当前命名。
5. **必须**通过 `MaterialTheme.typography` 访问字号/字重/行高/字距。
6. 任何 `Text`/`Button` 等必须为浅色 + 深色各跑一次人工 smoke 截图（见 smoke-checklist.md）。
7. 任何新增 token 必须在 `android/docs/design-tokens.md` 同步登记，并标注对应 CSS 来源。
8. 任何被本文件 §7 列为硬编码的位置，**禁止**在 M1 之后新增类似硬编码。

### 8.3 Review checklist

- [ ] PR 中没有新增 `Color(0xFF...)` 字符串（除 `ui/theme/Color.kt` 与 `ui/theme/Theme.kt`）。
- [ ] PR 中没有新增 `Color.White` / `Color.Black` / `Color.Gray` / `Color.LightGray`。
- [ ] PR 中没有新增 `RoundedCornerShape(N.dp)` 字面量。
- [ ] PR 中没有新增 `.padding(N.dp)` / `Spacer(N.dp)` 数字字面量。
- [ ] PR 中所有 `Text` 走 `MaterialTheme.typography` 之一。
- [ ] 浅色 + 深色 8 个高保真页面在 PR 中附前后对比截图。

## 9. 已知未解决冲突

1. **CSS 与 Compose 中"心情颜色"不一致**：`MoodSad` 与 `MoodCalm` 同色，
   `MoodSurprise` 与 `MoodWarm` 同色。需产品方决定。
2. **深色模式自创值**：CSS 没有 dark token；Compose dark 调色板完全自创。
   需设计方补 dark 调色板后回写。
3. **iOS-only token**：`--qx-background-elevated`、`--qx-background-grouped`、
   `--qx-foreground-quaternary`、`--qx-foreground-inverse` 在 HTML 页面中**未实际被引用**，
   仅作为预留。是否纳入 Compose 待设计方确认。
4. **`labelSmall` 字重不一致**：CSS footnote 是 400 Normal，Compose 是 500 Medium。
5. **`headlineLarge` 字重不一致**：CSS title-2 是 600 SemiBold，Compose 是 700 Bold。
6. **`bodyLarge` 字号不一致**：CSS callout 是 16，Compose 是 17。
7. **`res/values/colors.xml` 与 Compose 颜色不一致**：`green_primary` 在 xml 是 `#4CAF50`、
   在 Compose 是 `#5E9B6A`。当前 xml 似乎未被代码使用，但 M7 商店截图会触发。
8. **Shape 系统未定义**：Material3 `shapes` 默认值是 4dp / 8dp / 16dp / 28dp，
   与 CSS `--radius-md/lg/xl/2xl` 10/14/18/24 **不对应**。

## 10. 下一轮建议（不在本轮任务范围）

- 新增 `QxColors` / `QxSpacing` / `QxRadius` / `QxElevation` 命名文件，
  并在 `ui/theme/Theme.kt` 中桥接 `MaterialTheme.colorScheme`。
- 按 §7 表格逐文件替换硬编码为 token；可分 PR（按文件拆分）。
- 与 Z code 一起决定：`MoodSad/MoodCalm` 与 `MoodSurprise/MoodWarm` 的去重策略。
- 与设计方补 dark 调色板。
- 同步 `res/values/colors.xml` 与 Compose 颜色。

---

# 附录：M0-04 接手执行手册（2026-08-27，由 M0-04 docs-only 轮次产出）

> **本附录定位**
> 本节是给"下一轮真实动手改源码的 Agent"准备的执行手册。
> 上一轮（M0-01/02/03）严格只动文档；本附录显式记录：
>
> - 为什么本轮**没有**改源码
> - 改源码时建议的 PR 拆分、命名、bridge 思路、compatibility 策略
> - 接手者必须先回答的问题与禁止动作
> - 验证步骤（包含本仓库**只走 GitHub Actions**的硬约束）
>
> 本附录本身仍是**文档**，**不**包含对源码的修改。
> 任何接手 Agent 必须在开工前**逐条**对照本节，并在 PR 描述里显式引用相关小节编号。

## 11. 本轮为什么没改源码（决策记录与红线）

### 11.1 原任务单的"禁止修改"清单

引用 [`第一轮Agent任务单.md`](../../../第一轮Agent任务单.md) 第 80–85 行：

> 禁止修改：
> - AppDatabase、Entity、DAO、Repository、ViewModel
> - GP/streak/achievement 逻辑
> - MediaPicker/FileUtil/Renderer
> - Gradle、Manifest、GitHub workflow
> - **任何正式功能代码**

### 11.2 受影响的源码文件

设计 Token 重构**必然**触碰：

- `ui/theme/Color.kt`、`ui/theme/Type.kt`、`ui/theme/Theme.kt`（新增 Qx 命名 + bridge）
- `ui/components/*`（10 个文件，按 §7.1 替换硬编码）
- `ui/home/HomeScreen.kt`、`ui/record/RecordScreen.kt`、`ui/timeline/TimelineScreen.kt`、
  `ui/album/AlbumScreen.kt`、`ui/album/AlbumViewerScreen.kt`、`ui/share/ShareScreen.kt`、
  `ui/profile/ProfileScreen.kt`、`ui/profile/AchievementScreen.kt`、
  `ui/profile/PlantSelectionScreen.kt`、`ui/profile/PlantGuideScreen.kt`（按 §7.1 替换）
- `res/values/colors.xml`（同步 §9.7）
- `res/values/themes.xml`（不与暗色冲突）

以上均属于"正式功能代码"。

### 11.3 红线（接手者必须遵守）

下列约束**任何**接手 PR 都不允许破坏：

1. **不**改 `AppDatabase` / `Entity` / `DAO` / `Repository` / `ViewModel` 任何文件。
2. **不**改 GP / streak / achievement 逻辑（`GPCalculator.kt`、`StreakCalculator.kt`、
   `AchievementEvaluator.kt`、`AchievementTrigger.kt`）。
3. **不**改 `MediaPicker.kt`、`FileUtil.kt`、`PhotoSaver.kt`、`ShareCardRenderer.kt`、
   `PlantRenderer.kt`。
4. **不**改 `app/build.gradle.kts`、`build.gradle.kts`、`settings.gradle.kts`、
   `gradle.properties`、`AndroidManifest.xml`、`res/xml/file_paths.xml`、
   `android/.github/workflows/build.yml`、`proguard-rules.pro`。
5. **不**把 `Color(0xFF...)` 字面量与 `RoundedCornerShape(N.dp)` 字面量**新增**到任何 Composable
   内（见 §8）。允许的"硬编码"只剩**已有 PR 引入的硬编码**——它们的处理方式见 §15 compatibility。
6. **不**在没有真实 GitHub Actions run 链接的情况下声称"CI 绿"。
7. **不**修改 `MoodTag` 枚举的命名与颜色映射——这是 Z code 决策项，参见 §18.2。
8. **不**在 M0-04 引入新依赖（无新库、新 BuildConfig、新 R8 配置）。

## 12. 建议的 PR 拆分（按风险从低到高）

> 每条 PR 都**必须**走 `迭代开发计划-v1.0.md` §7 任务卡格式，PR 描述引用本节。
> PR 之间**顺序合并**，不允许并行（避免冲突）。

### 12.1 PR-1：Qx 命名基础设施（**仅新增**文件，**不改**任何现有 Composable）

- 范围：
  - 新增 `ui/theme/QxColors.kt`
  - 新增 `ui/theme/QxSpacing.kt`
  - 新增 `ui/theme/QxRadius.kt`
  - 新增 `ui/theme/QxElevation.kt`
  - 新增 `ui/theme/QxTypography.kt`
  - 新增 `ui/theme/QxMood.kt`（把 `MoodHappy` 等的**别名**也放在这里，避免 §2.3 的命名混乱被放大）
- 允许的修改：
  - `ui/theme/Theme.kt` 增加 `object QxTheme { val colors; val spacing; val radius; val elevation; val typography; val mood }` 的访问入口
  - **不动**现有 `lightColorScheme` / `darkColorScheme` / `Typography` 的字段值
- 不允许的修改：
  - **不**删 `GreenPrimary` / `BackgroundLight` 等旧名（compatibility 留给 §15）
  - **不**改任何 Screen
- 验收：
  - `./gradlew assembleDebug` 通过
  - 所有现有 Composable **行为完全不变**（截图 diff 应为空）
  - 引入至少 1 个 `QxColors.*` 的使用示例（PR 自带 1 个 `Modifier.background(QxColors.surface)` 的实验性
    Composable，**只放在 PR 描述附带的 Kotlin scratch 文件中**，不进主包）

### 12.2 PR-2：bridge 现有 Color.kt 命名

- 范围：把 `Color.kt` 中的 `GreenPrimary` 等**改为 `QxColors.primary`** 的内部别名，**保留**旧名。
- 允许的修改：`ui/theme/Color.kt`、`ui/theme/Theme.kt`（使用 `QxColors.*` 替换字段值）
- 不允许：删除旧名
- 验收：调用方零改动；assembleDebug 通过

### 12.3 PR-3：替换 `ui/components/*` 的硬编码（按文件拆子 PR）

- 顺序（按使用面从窄到宽）：
  1. `TagChip.kt`、`AchievementBadge.kt`（仅 2 处硬编码）
  2. `AddContentButton.kt`、`RecordCard.kt`、`PhotoGrid.kt`、`PlantView.kt`（使用广）
  3. `MusicCard.kt`、`ShareCardGenerator.kt`（含品牌色）
  4. `VoiceRecorder.kt`（录音 UI，单独 PR 降低风险）
- 每个子 PR 完成后：
  - 走 §16 接手检查表
  - 浅色 + 深色对比截图（保留在 PR 评论里）
  - 任何"心情颜色重命名"必须在 PR 描述显式说"未触"，并把"为什么不动"链回 §18.2

### 12.4 PR-4：替换 `ui/home/*` / `ui/record/*`

- 范围：所有 `ui/home/` 与 `ui/record/` 文件的硬编码
- 备注：`HomeScreen.kt` 渐变背景（`Color(0xFFE8F3EA)` 等）应建 `QxGradients.heroPlant`
  等命名，**先在 PR 描述里列出来**，再下笔

### 12.5 PR-5：替换 `ui/timeline/*` / `ui/album/*` / `ui/share/*`

- 范围：3 个目录的所有文件
- 备注：`ShareScreen.kt` 的微信/朋友圈/小红书/微博品牌色应建 `QxChannelWechat` 等命名，
  并在 PR 描述里写明"仅命名替换，**不**接入真 action"——避免 reviewer 误判为"接入了分享"

### 12.6 PR-6：替换 `ui/profile/*`

- 范围：`ProfileScreen.kt`、`AchievementScreen.kt`、`PlantSelectionScreen.kt`、`PlantGuideScreen.kt`
- 备注：`ProfileScreen.kt` 第 153 行 `Color(0xFFE91E63)`（Pink）未在 CSS 中；建议新建
  `QxAccentPink` 并在 PR 描述显式说明"为兼容设计方补的命名"

### 12.7 PR-7：同步 `res/values/colors.xml` + `themes.xml`

- 范围：
  - `res/values/colors.xml` 中 `green_primary` 等与 Compose 颜色**不一致**的 19 个 `@color/*`
    应**删除**（未在代码中使用）
  - `themes.xml` 中 `android:color/white` 硬编码应改为
    `?android:attr/colorBackground` 或跟随 day/night 资源
- 备注：**不**改 `app/src/main/res/mipmap-*/`（M7 商店图标由 M7-01 处理）
- 验收：assembleDebug；视觉无回归

### 12.8 PR-8：移除旧 Color 命名（compatibility shim 拆除）

- **仅当** PR-2 之后所有调用方都已迁移到 `QxColors.*` 才允许执行
- 范围：删除 `GreenPrimary`、`BackgroundLight`、`CardBg`、`TextPrimary`、`TextSecondary`、
  `SeparatorColor`、`OnGreenContainer`、`GreenDark`、`GreenLight`、`GreenBg`、`GreenContainer`、
  `PinkAccent`、`YellowAccent`、`OrangeAccent`、`BrownTrunk`、`MoodHappy/MoodCalm/MoodExcited/...`、
  `GreenDarkPrimary`、`DarkBackground`、`DarkSurface`、`DarkCard`、`DarkTextPrimary`、
  `DarkTextSecondary`
- 验收：assembleDebug 通过且无 lint 警告
- 风险：可能引起 DI / 反射 / JSON 序列化异常，**必须**在 PR 描述里列出 grep 结果

## 13. Token 改名路径表（取自 §6 汇总表）

接手者必须**逐项**确认每条都有落点：

| CSS Token | Compose 新名 | 旧 Compose 名字 | 改名 PR | 删除旧名 PR |
|---|---|---|---|---|
| `--qx-primary` | `QxColors.primary` | `GreenPrimary` | PR-2 | PR-8 |
| `--qx-primary-light` | `QxColors.primaryLight` | `GreenLight` | PR-2 | PR-8 |
| `--qx-primary-lighter` | `QxColors.primaryLighter` | —（新增） | PR-1 | — |
| `--qx-primary-dark` | `QxColors.primaryDark` | `GreenDark` | PR-2 | PR-8 |
| `--qx-primary-50` | `QxColors.primary50` | Theme 内联 `0xFFF0F7F1` | PR-2 | — |
| `--qx-primary-100` | `QxColors.primary100` | `GreenContainer` | PR-2 | PR-8 |
| `--qx-primary-200` | `QxColors.primary200` | —（新增） | PR-1 | — |
| `--qx-primary-container` | `QxColors.primaryContainer` | —（新增） | PR-1 | — |
| `--qx-gray-50..900` | `QxColors.gray50..900` | —（新增） | PR-1 | — |
| `--qx-background` | `QxColors.background` | `BackgroundLight` | PR-2 | PR-8 |
| `--qx-background-elevated` | `QxColors.backgroundElevated` | — | PR-1 | — |
| `--qx-background-grouped` | `QxColors.backgroundGrouped` | — | PR-1 | — |
| `--qx-surface` | `QxColors.surface` | `CardBg` | PR-2 | PR-8 |
| `--qx-surface-secondary` | `QxColors.surfaceSecondary` | `GreenBg` | PR-2 | PR-8 |
| `--qx-surface-tertiary` | `QxColors.surfaceTertiary` | — | PR-1 | — |
| `--qx-foreground` | `QxColors.foreground` | `TextPrimary` | PR-2 | PR-8 |
| `--qx-foreground-secondary` | `QxColors.foregroundSecondary` | `TextSecondary` | PR-2 | PR-8 |
| `--qx-foreground-tertiary` | `QxColors.foregroundTertiary` | — | PR-1 | — |
| `--qx-foreground-quaternary` | `QxColors.foregroundQuaternary` | — | PR-1 | — |
| `--qx-foreground-inverse` | `QxColors.foregroundInverse` | — | PR-1 | — |
| `--qx-primary-foreground` | `QxColors.primaryForeground` | Theme 内联 `Color.White` | PR-2 | — |
| `--qx-border-subtle` | `QxColors.borderSubtle` | — | PR-1 | — |
| `--qx-border-strong` | `QxColors.borderStrong` | — | PR-1 | — |
| `--qx-separator` | `QxColors.separator` | — | PR-1 | — |
| `--qx-separator-opaque` | `QxColors.separatorOpaque` | `SeparatorColor` | PR-2 | PR-8 |
| `--state-success` | `QxColors.stateSuccess` | — | PR-1 | — |
| `--state-warning` | `QxColors.stateWarning` | — | PR-1 | — |
| `--state-error` | `QxColors.stateError` | Theme 内联 `0xFFFF3B30` | PR-2 | — |
| `--state-info` | `QxColors.stateInfo` | — | PR-1 | — |
| `--mood-happy..grateful` | `QxMood.happy..grateful` | `MoodHappy..grateful` | PR-2 | PR-8（**仅当 §18.2 决策后**） |
| `--radius-sm..2xl` | `QxRadius.sm..xxl` | 散落 | PR-3 起 | — |
| `--radius-full` | `QxRadius.full` | 散落 | PR-3 起 | — |
| `--shadow-xs..xl` | `QxElevation.xs..xl` | 散落 | PR-3 起 | — |
| `--spacing-xs..3xl` | `QxSpacing.xs..xxxl` | 散落 | PR-3 起 | — |
| `--font-size-*` | `QxTypography.*` | `MaterialTheme.typography` | PR-1 + PR-2 | — |

## 14. Bridge 思路（Theme.kt 怎么接）

### 14.1 推荐：双层结构

```kotlin
// ui/theme/QxColors.kt (新)
object QxColors {
  val primary            = Color(0xFF5E9B6A) // --qx-primary
  val primaryLight       = Color(0xFF7AB886) // --qx-primary-light
  val primaryLighter     = Color(0xFFA8D5B0) // --qx-primary-lighter
  val primaryDark        = Color(0xFF4A7D54) // --qx-primary-dark
  val primary50          = Color(0xFFF0F7F1) // --qx-primary-50
  val primary100         = Color(0xFFDCEFDF) // --qx-primary-100
  val primary200         = Color(0xFFB9DFBF) // --qx-primary-200
  val primaryContainer   = Color(0x335E9B6A) // --qx-primary-container, alpha 已折算

  val background         = Color(0xFFFBFBF9) // --qx-background
  val backgroundElevated = Color(0xFFFFFFFF) // --qx-background-elevated
  val backgroundGrouped  = Color(0xFFF2F1EE) // --qx-background-grouped
  val surface            = Color(0xFFFFFFFF) // --qx-surface
  val surfaceSecondary   = Color(0xFFF7F6F4) // --qx-surface-secondary
  val surfaceTertiary    = Color(0xFFF0EFEB) // --qx-surface-tertiary

  val foreground             = Color(0xFF1A1918) // --qx-foreground
  val foregroundSecondary    = Color(0xFF6B6862) // --qx-foreground-secondary
  val foregroundTertiary     = Color(0xFF8A8780) // --qx-foreground-tertiary
  val foregroundQuaternary   = Color(0xFFB0ADA6) // --qx-foreground-quaternary
  val foregroundInverse      = Color(0xFFFFFFFF) // --qx-foreground-inverse
  val primaryForeground      = Color(0xFFFFFFFF) // --qx-primary-foreground

  val borderSubtle   = Color(0x0F000000) // --qx-border-subtle, alpha=0.06
  val borderStrong   = Color(0x1A000000) // --qx-border-strong, alpha=0.10
  val separator      = Color(0x1F3C3C43) // --qx-separator, alpha=0.12
  val separatorOpaque = Color(0xFFE5E4E0) // --qx-separator-opaque

  val stateSuccess = Color(0xFF34C759) // --state-success
  val stateWarning = Color(0xFFFF9500) // --state-warning
  val stateError   = Color(0xFFFF3B30) // --state-error
  val stateInfo    = Color(0xFF007AFF) // --state-info

  // Neutral 灰阶（10 档，新增）
  val gray50  = Color(0xFFFBFBF9)
  val gray100 = Color(0xFFF5F4F2)
  val gray200 = Color(0xFFEBE9E5)
  val gray300 = Color(0xFFD8D6D1)
  val gray400 = Color(0xFFB0ADA6)
  val gray500 = Color(0xFF8A8780)
  val gray600 = Color(0xFF6B6862)
  val gray700 = Color(0xFF4A4844)
  val gray800 = Color(0xFF2D2B29)
  val gray900 = Color(0xFF1A1918)

  // Dark 调色板（自创，需要设计方补；详见 §9.2）
  val darkBackground        = Color(0xFF161815)
  val darkSurface           = Color(0xFF1E211D)
  val darkSurfaceVariant    = Color(0xFF282B26)
  val darkForeground        = Color(0xFFE8E7E3)
  val darkForegroundSecondary = Color(0xFFA0A29B)
  val darkPrimaryContainer  = Color(0xFF33503A)
  val darkSecondaryContainer = Color(0xFF2A3D2E)
  val darkOnPrimaryContainer = Color(0xFFDCEFDF)
  val darkOnBackground      = Color(0xFF10130F)
  val darkError             = Color(0xFFFF6B5E)
  val darkOutline           = Color(0xFF6B6862)
  val darkOutlineVariant    = Color(0xFF3A3D37)
}
```

```kotlin
// ui/theme/QxSpacing.kt (新)
object QxSpacing {
  val xs   = 4.dp
  val sm   = 8.dp
  val md   = 12.dp
  val base = 16.dp
  val lg   = 20.dp
  val xl   = 24.dp
  val xxl  = 32.dp
  val xxxl = 48.dp
}
```

```kotlin
// ui/theme/QxRadius.kt (新)
object QxRadius {
  val sm   = 6.dp
  val md   = 10.dp
  val lg   = 14.dp
  val xl   = 18.dp
  val xxl  = 24.dp
  val full = 9999.dp
}
```

```kotlin
// ui/theme/QxElevation.kt (新)
object QxElevation {
  val xs   = 1.dp
  val sm   = 2.dp
  val md   = 6.dp
  val lg   = 10.dp
  val xl   = 18.dp
  // 对应 shadow token 的"色 + 偏移 + 模糊"组合
  // 推荐用法：Modifier.shadow(QxElevation.md, shape = RoundedCornerShape(QxRadius.lg))
}
```

```kotlin
// ui/theme/QxMood.kt (新)
object QxMood {
  val happy    = Color(0xFFF5C842) // --mood-happy
  val warm     = Color(0xFFF4A261) // --mood-warm
  val excited  = Color(0xFFFF7043) // --mood-excited
  val calm     = Color(0xFF7EC8CE) // --mood-calm
  val love     = Color(0xFFF48FB1) // --mood-love
  val grateful = Color(0xFFCE93D8) // --mood-grateful
  // 注意：下列命名与 PRD §3.1.2 心情列表对齐，由 §18.2 决策后再启用
  val touched    = love        // PRD "感动"
  val miss       = Color(0xFFB0ADA6) // "想念"，复用 gray400
  val tired      = Color(0xFF8A8780) // "放松"，复用 gray500
  val sad        = calm        // 与 calm 同色，§2.3 已记录冲突
  val angry      = Color(0xFFFF6B5E) // 非 CSS 来源
  val surprise   = warm        // 与 warm 同色，§2.3 已记录冲突
  val brownTrunk = Color(0xFF8B6F4E) // 植物素材色，非 CSS
}
```

```kotlin
// ui/theme/QxTypography.kt (新)
// 把现有 Typography 复制为 val QxText = Typography(...)
// 并显式标注与 CSS 的 4 处差异（§5.1）：
//   - headlineLarge 改为 22sp / SemiBold
//   - bodyLarge 改为 16sp
//   - labelSmall 改为 11sp / Normal
//   - 暂不引入 10sp caption，等设计方确认
```

```kotlin
// ui/theme/Theme.kt (修改，但兼容)
private val LightColorScheme = lightColorScheme(
  primary = QxColors.primary,
  onPrimary = QxColors.primaryForeground,
  primaryContainer = QxColors.primary100,
  onPrimaryContainer = Color(0xFF2D4A34), // §3.1 标注：CSS 缺，先保留旧值
  secondary = QxColors.primaryLight,
  secondaryContainer = QxColors.primary50,
  // ...其余字段照搬旧值
)
```

### 14.2 不推荐：直接改 `MaterialTheme.colorScheme` 字段名

会破坏 §15 compatibility 承诺。**只**做字段值的替换，不改字段名。

## 15. Compatibility 策略（如何避免破坏旧调用方）

### 15.1 三阶段

1. **阶段 A（PR-1/2）**：保留所有旧名，作为 `QxColors` 的内部别名。例如：
   ```kotlin
   // ui/theme/Color.kt 改造后
   val GreenPrimary = QxColors.primary  // 旧名 = 新名（类型相同，零成本）
   val GreenLight   = QxColors.primaryLight
   // ...
   ```
2. **阶段 B（PR-3~6）**：**不**改调用方；只让 IDE 提示（用 `@Deprecated` 标记，**不**移除）。
3. **阶段 C（PR-8）**：删除旧名，依赖 `./gradlew lint` 找漏网。

### 15.2 `@Deprecated` 用法

```kotlin
@Deprecated(
  message = "使用 QxColors.primary。将在 M0-04 PR-8 删除。",
  replaceWith = ReplaceWith("QxColors.primary", "com.xiaoquexing.app.ui.theme.QxColors"),
  level = DeprecationLevel.WARNING,
)
val GreenPrimary: Color = QxColors.primary
```

- **不**用 `ERROR`，避免阻塞 CI。
- lint warning 应在 PR-3 ~ PR-6 阶段逐步减少。

### 15.3 Theme.kt 字段命名

**不**改 `lightColorScheme(primary = ...)` 的字段名（`primary` / `secondary` / `surface` 等是
Material 3 API）。只改字段**值**的来源。

## 16. 接手检查表（每个 PR 必跑）

- [ ] PR 标题遵守 `迭代开发计划-v1.0.md` §7 任务卡格式。
- [ ] PR 描述显式引用本附录相关小节编号（§11–§15）。
- [ ] `git grep "Color(0xFF"` 数量在 PR 前后**只能持平或减少**。
- [ ] `git grep "RoundedCornerShape([0-9]"` 数量同上。
- [ ] `git grep "\.padding([0-9][0-9]*\.dp"` 数量同上。
- [ ] `git grep "Color\.White|Color\.Black|Color\.Gray|Color\.LightGray|Color\.DarkGray"` 数量同上。
- [ ] 浅色 + 深色对比截图贴在 PR 评论（API 26/29/34 至少其中 1 台设备）。
- [ ] `./gradlew testDebugUnitTest` 通过（**注意**：本轮 M0-04 不得新增 test；只跑现成测试）。
- [ ] `./gradlew lintDebug` 通过；新增 warning 必须显式说明。
- [ ] `./gradlew assembleDebug` 通过；APK 可下载。
- [ ] **不**改 schema/DAO/Repository/ViewModel/Gradle/Manifest/workflow。
- [ ] 任何"接入了真 action / 真分享渠道"宣称必须**显式标 NO**。

## 17. 验证步骤（只走 GitHub Actions）

按 [`迭代开发计划-v1.0.md` §6](../../../../迭代开发计划-v1.0.md)：

1. PR 推上 main / PR。
2. 等 `build.yml` 三个 job 全部绿。
3. 下载 `app-debug` Artifact。
4. 由人工按 [`./smoke-checklist.md`](./smoke-checklist.md) 跑 §3–§9。
5. **不**在本机跑 `./gradlew`。
6. 在 PR 评论里贴上：
   - 三个 job 的 run 链接
   - APK SHA-256
   - smoke-checklist 的 §11 整体结论
7. 在 README / PROGRESS 中**只**在 run 链接已确认的情况下更新"CI 状态"。

## 18. 必须先回答的开放问题（不要在没回答前动手）

### 18.1 深色模式调色板

`design-tokens.md §3.2` 与 §9.2 已标注 dark 调色板完全自创，CSS 无对应。
**接手 PR-1 前必须**与设计方确认：
- dark `background` = `#161815` 是否仍是主色？
- dark `primary` 是否保持 `GreenDarkPrimary = #7AB886`？
- dark `error` 是否为 `#FF6B5E`（而非 iOS `#FF3B30`）？

如果设计方有新值，**先**改 `design-tokens.md` §3.2，**再**动 `QxColors`。

### 18.2 Mood 命名与去重

`design-tokens.md §2.3` 与 §9.1 标注 `MoodSad` 与 `MoodCalm` 同色、`MoodSurprise` 与 `MoodWarm` 同色。
**Z code** 必须在 `MoodTag` 枚举层面决策（不在 M0-04 范围）：
- 是改 PRD §3.1.2 的心情列表？
- 还是新增颜色（向设计方申请）？
- 还是合并枚举值（向产品方申请）？

M0-04 PR-1 ~ PR-7 **只**做命名迁移，**不**改枚举本身。

### 18.3 资源（drawable / mipmap / Coil）色

- `res/drawable/ic_launcher_foreground.xml` 是否需要随品牌色重画？属于 M7-01。
- `res/values/colors.xml` 中 `@color/green_primary = #4CAF50` 与 Compose 不一致，是否删除？见 PR-7。
- Coil 加载远程 URL（未来场景）需要的占位色与错误色，是否要进 `QxColors`？当前 0% 远程图，**暂不**加。

### 18.4 字体

`Type.kt` 未指定 `fontFamily`，使用系统默认。CSS `--font-family` 指定 SF Pro / PingFang / Noto Sans。
是否要把 `fontFamily` 显式化？属于 M7-01 启动屏范畴，**不**在 M0-04 解决。

## 19. 附录 A：原始下一轮建议（保留）

> 本节是 M0-02 任务原 §10 内容；保留以便与历史 diff 对照。
> 实质性的下一轮建议以 §11–§18 为准。

- 新增 `QxColors` / `QxSpacing` / `QxRadius` / `QxElevation` 命名文件，
  并在 `ui/theme/Theme.kt` 中桥接 `MaterialTheme.colorScheme`。
- 按 §7 表格逐文件替换硬编码为 token；可分 PR（按文件拆分）。
- 与 Z code 一起决定：`MoodSad/MoodCalm` 与 `MoodSurprise/MoodWarm` 的去重策略。
- 与设计方补 dark 调色板。
- 同步 `res/values/colors.xml` 与 Compose 颜色。
