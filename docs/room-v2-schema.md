# Room v2 数据模型与 v1→v2 迁移方案（Z0-02 设计稿）

- **状态**：设计已评审基线（本轮只交付设计与测试计划，不含实现；实现归 Z1-01）
- **日期**：2026-08-27
- **前置**：规则以 `android/docs/adr/ADR-001-domain-rules.md` 为准（下文引用 D1–D12）
- **现实现基线**：`AppDatabase.kt` version=1、`exportSchema=false`、`fallbackToDestructiveMigration()`（`AppDatabase.kt:34-58`）
- **构建验证约定**：本文档涉及的 migration 测试与 schema 导出仅在 GitHub Actions 上验证；本地不执行 Gradle。

---

## 0. 本方案对已识别风险的处理索引

| # | 风险（任务单列出） | 处理位置 |
|---|---|---|
| K1 | `GPCalculator` 与 PRD 两套 GP 公式 | 公式冻结在 ADR D1；v2 落库 `records.gpFinal + gpBreakdownJson`，重算归 `GpService`（§6 事务边界），公式常量单点维护 |
| K2 | 记录写入、植物加 GP、成就更新不在同一事务 | v2 植物不持有 GP（D7）；发布/编辑/删除收敛为单事务用例 + `RecomputeService`（§6） |
| K3 | 5 条 Demo 记录 116 GP 未同步给活动植物 | 迁移裁决「以记录集合为准」：space.totalGp = SUM(records.gpFinal)（§5 步骤 5e）；Demo 注入由 Z1-07 移除（ADR D12） |
| K4 | `fallbackToDestructiveMigration()` 可能清空用户数据 | v2 禁用 destructive fallback；提供显式 `MIGRATION_1_2` + 失败处理（§7、§8） |
| K5 | Photo Picker `content://` 重启后不可读 | `record_media.localPath`（App 私有目录副本）为唯一渲染来源，`sourceUri` 仅存溯源；`mediaStatus` 标记待复制/丢失（§2.2、§5 步骤 7） |
| K6 | `getRecordDays()` 对毫秒时间 `substr` | 统计改用 `records.occurredDateKey` 去重（§2.1、§9 I2） |
| K7 | 摄影师成就统计含照片记录数而非照片张数 | 成就条件类型 `PHOTO_COUNT`，按 `record_media(type=PHOTO)` 行数统计（ADR D9.3；§2.2） |
| K8 | 植物解锁全部按 GP | `plants.conditionType/conditionParam` 类型化（ADR D9.2；§2.8） |

补充核实的 v1 事实（影响迁移设计）：

- v1 实际注册的实体只有 `Record/PlantState/Achievement/Space`（`AppDatabase.kt:35`）；`Tag.kt` 虽有 `@Entity` 注解但**从未注册、无 DAO**，真实数据库中没有 `tags` 表。
- v1 未导出 schema JSON（`exportSchema=false`），**不存在版本 1 的 schema 文件**，migration 测试需手写 v1 DDL fixture（§10）。
- v1 `spaces` 表存在但记录无 `spaceId`，基本无数据。

---

## 1. 总览

### 1.1 命名与通用约定

- 表名/列名 snake_case；Kotlin 属性 camelCase + `@ColumnInfo` 显式命名，避免默认驼峰转下划线带来的漂移。
- 主键：`localId`（INTEGER PRIMARY KEY AUTOINCREMENT）。**localId 永不变更、永不复用**，服务端同步与外键一律引用它（serverId 仅作服务端身份映射）。
- 时间戳一律 epoch millis（INTEGER）；日期键 `occurredDateKey`/`dateKey` 为 epoch day（INTEGER，本地时区 `LocalDate.toEpochDay()`），修复 K6 类口径错误。
- 布尔用 INTEGER 0/1。
- 枚举存 TEXT（可读、可诊断），不用 ordinal。

### 1.2 通用同步列（sync columns）

| 列 | 类型 | 说明 |
|---|---|---|
| `server_id` | TEXT NULL | 服务端 UUID；唯一索引；NULL=未同步 |
| `created_at` | INTEGER NOT NULL | 本地创建时间（不变） |
| `updated_at` | INTEGER NOT NULL | 本地最后修改时间（每次写更新） |
| `deleted_at` | INTEGER NULL | 软删除墓碑（D6/D10）；查询一律过滤 |
| `sync_state` | INTEGER NOT NULL | 0=SYNCED，1=SYNC_PENDING，2=DELETE_PENDING，3=CONFLICT |
| `version` | INTEGER NOT NULL DEFAULT 0 | 本地每次修改 +1；服务端权威版本回填 |

本地纯内容表（`plants`、`achievement_definitions`、`daily_space_stats`、`outbox_events`）不需要完整同步列，逐表注明。

### 1.3 ER 关系（文字图）

```
users ──1:N── space_members ──N:1── spaces ──1:N── records ──1:N── record_media
  │                                   │              │
  │                                   │              └──1:N── record_tag_cross_ref ──N:1── tags
  │                                   ├──1:N── space_plants ──N:1── plants(目录)
  │                                   ├──1:N── plant_snapshots
  │                                   ├──1:N── daily_space_stats (PK 含 spaceId)
  │                                   └──1:N── albums ──1:N── album_pages
  └── authorId（records 上，见 §2.1）

achievement_definitions ──1:N── achievement_progress（unique(code, scopeKey)）
                            └──1:N── achievement_events（append-only）
outbox_events（无外键，独立于实体生命周期）
```

---

## 2. Entity 清单和字段

### 2.1 `records`（记录主表）

| 列 | 类型 | 约束/默认 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `space_id` | INTEGER | NOT NULL, FK→spaces.local_id, **RESTRICT** | D3 额度与 D7 归属的空间 |
| `author_id` | INTEGER | NOT NULL, FK→users.local_id | D11 作者独占改删 |
| `content_text` | TEXT | NULL | ≤500 字（应用层校验） |
| `mood_tag` | TEXT | **NOT NULL** | 心情必选（ADR 原则 1，修复 R4） |
| `occurred_at` | INTEGER | NOT NULL | 发生时间（补记可改，D4） |
| `occurred_date_key` | INTEGER | NOT NULL | 发生日 epoch day；额度/分组/streak 唯一口径（D3/D2，修复 K6/R5） |
| `is_backdated` | INTEGER | NOT NULL DEFAULT 0 | 派生持久列 = occurredDateKey != createdAt 当日键（D4.5） |
| `gp_final` | INTEGER | NOT NULL DEFAULT 0 | 该条最终入账 GP（D1 封顶后） |
| `gp_breakdown_json` | TEXT | NULL | 明细快照（审计、服务端校准 diff、客诉排查） |
| `is_capped` | INTEGER | NOT NULL DEFAULT 0 | 当日额度封顶标记（UI 提示） |
| 同步列 ×6 | | | §1.2 |

天气等扩展字段不设列，未来经 `gp_breakdown_json` 旁的 `extra_json` 迁移位新增（v3），避免现在猜测。

### 2.2 `record_media`（媒体/附属内容，正规化，修复 K5/K7）

v1 拍平在 records 上的 `photoUris/voiceUri/music*/link*/location*` 六组列全部拆入本表。

| 列 | 类型 | 约束/默认 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `record_id` | INTEGER | NOT NULL, FK→records.local_id, **CASCADE** | 物理清理时级联 |
| `type` | TEXT | NOT NULL | `PHOTO` / `VOICE` / `MUSIC` / `LINK` / `LOCATION` |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 照片顺序（≤9），同类型内有序 |
| `local_path` | TEXT | NULL | **App 私有目录副本路径 = 渲染唯一来源**（相机图已是私有目录；相册图由复制任务落盘） |
| `source_uri` | TEXT | NULL | 原始 `content://` 或外链，仅溯源与修复重试，不用于渲染 |
| `remote_uri` | TEXT | NULL | OSS 地址（M4 上传后回填） |
| `media_status` | TEXT | NOT NULL DEFAULT `PENDING_COPY` | `READY` / `PENDING_COPY` / `MISSING`（复制失败占位，UI 显示占位图） |
| `mime_type` | TEXT | NULL | |
| `duration_ms` | INTEGER | NULL | VOICE 时长 |
| `width` / `height` | INTEGER | NULL | PHOTO 尺寸 |
| `title` | TEXT | NULL | LOCATION=地点名；MUSIC=歌名；LINK=标题。**DISTINCT_LOCATION_COUNT 按 title 去重统计**（K7/D9） |
| `subtitle` | TEXT | NULL | MUSIC=歌手；LOCATION=地址 |
| `extra_json` | TEXT | NULL | LINK 摘要/平台、MUSIC 专辑封面、LOCATION lat/lng、LINK OG 数据 |
| 同步列 ×6 | | | 媒体与记录同事务写入、同批同步 |

### 2.3 `tags` 与 `record_tag_cross_ref`

v1 的 CSV `statusTags` 拆为注册表 + 关联表（v1 tags 表实际不存在，见 §0）。

**tags**

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `scope` | TEXT | NOT NULL | `USER` / `SPACE`（共享空间自定义标签对成员可见，PRD 3.1.2） |
| `space_id` | INTEGER | NOT NULL DEFAULT **0** | 0 = USER 作用域哨兵。**有意不加 FK**（0 不指向任何空间），换表内校验，理由见 §3 注 |
| `kind` | TEXT | NOT NULL | `MOOD` / `STATUS` / `CUSTOM` |
| `name` | TEXT | NOT NULL | |
| `emoji` | TEXT | NOT NULL DEFAULT '' | |
| `color` | TEXT | NULL | |
| `use_count` | INTEGER | NOT NULL DEFAULT 0 | |
| 同步列 ×6 | | | SPACE 作用域标签需同步（M5） |

**record_tag_cross_ref**

| 列 | 类型 | 约束 |
|---|---|---|
| `record_id` | INTEGER | FK→records **CASCADE**，复合 PK |
| `tag_id` | INTEGER | FK→tags **CASCADE**，复合 PK |

### 2.4 `spaces` / `space_members` / `users`

**spaces**

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `name` | TEXT | NOT NULL | |
| `space_type` | TEXT | NOT NULL DEFAULT `PERSONAL` | `PERSONAL/COUPLE/FAMILY/FRIEND` |
| `is_default` | INTEGER | NOT NULL DEFAULT 0 | 唯一个人默认空间 |
| `total_gp` | INTEGER | NOT NULL DEFAULT 0 | **缓存列**，不变量 = SUM(records.gp_final AND deleted_at IS NULL)（D7），事务内维护、可全量重算修复 |
| 同步列 ×6 | | | |

**space_members**

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `space_id` | INTEGER | FK→spaces **CASCADE** | |
| `user_id` | INTEGER | FK→users **RESTRICT** | |
| `role` | TEXT | NOT NULL DEFAULT `MEMBER` | `OWNER/ADMIN/MEMBER`（D11） |
| `joined_at` | INTEGER | NOT NULL | |
| `contributed_gp` | INTEGER | NOT NULL DEFAULT 0 | 展示缓存，不参与阶段计算（D7.3） |
| 同步列 ×6 | | | |
| 唯一约束 | `UNIQUE(space_id, user_id)` | | |

**users**（v1 单机只有一行本地用户；M4 接账号后扩展）

| 列 | 类型 | 说明 |
|---|---|---|
| `local_id` | INTEGER PK | v1 固定 `local_user` 一行 |
| `display_name` TEXT NOT NULL | |
| `avatar_local_path` TEXT NULL | |
| `premium_expire_at` INTEGER NULL | 商业化占位（ADR Q7/Q12） |
| 同步列 ×6 | serverId 即账号 UUID |

### 2.5 `plants` / `space_plants` / `plant_snapshots`（D7/D8/D9）

**plants**（用户级目录 + 解锁状态，修复 K8）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `plant_type` | TEXT | PK | 枚举名（TREE/SAKURA/...），与渲染代码共用标识 |
| `display_name` / `emoji` | TEXT | NOT NULL | 图鉴展示 |
| `condition_type` | TEXT | NOT NULL | ADR D9.1 枚举（`DEFAULT/STREAK_DAYS/DISTINCT_LOCATION_COUNT/...`） |
| `condition_param` | INTEGER | NOT NULL | |
| `condition_sub_param` | TEXT | NULL | 如 `INVITE_CO_PLANT` |
| `is_unlocked` | INTEGER | NOT NULL DEFAULT 0 | 由 `AchievementEvaluator` 重估（D6.5 可回锁） |
| `unlocked_at` | INTEGER | NULL | |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | |
| `updated_at` | INTEGER | NOT NULL | 仅本地内容，无 serverId/syncState |

**space_plants**（空间内的植物实例——「载体」）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `space_id` | INTEGER | FK→spaces **CASCADE** | |
| `plant_type` | TEXT | FK→plants.plant_type **RESTRICT** | |
| `is_active` | INTEGER | NOT NULL DEFAULT 0 | 每空间至多 1（部分唯一索引，§3） |
| `started_at` | INTEGER | NOT NULL | |
| `ended_at` | INTEGER | NULL | 换植物时落时间 |
| 同步列 ×6 | | | |

**注意：不持有 totalGp**（D7，修复 R3/K2 的双真相来源）。阶段 = `PlantStage.fromGp(space.totalGp)` 运行时派生。

**plant_snapshots**（图鉴历史 + 画册时间轴，append-only）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `space_id` | INTEGER | FK→spaces **CASCADE** | |
| `plant_type` | TEXT | NOT NULL | |
| `event_type` | TEXT | NOT NULL | `STAGE_UP/STAGE_DOWN/PLANT_SWITCHED/PLANT_RETIRED/MIGRATED_BASELINE` |
| `stage` | INTEGER | NOT NULL | 事件后阶段 |
| `gp_at_event` | INTEGER | NOT NULL | 事件时空间 GP |
| `occurred_at` | INTEGER | NOT NULL | |
| `occurred_date_key` | INTEGER | NOT NULL | |
| 同步列 ×6 | | | |

### 2.6 `daily_space_stats`（额度缓存，D3）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `space_id` | INTEGER | 复合 PK, FK→spaces **CASCADE** | |
| `date_key` | INTEGER | 复合 PK | epoch day |
| `gp_total` | INTEGER | NOT NULL DEFAULT 0 | 该日已入账 GP（额度 = 100 − gp_total） |
| `record_count` | INTEGER | NOT NULL DEFAULT 0 | |
| `distinct_author_count` | INTEGER | NOT NULL DEFAULT 0 | 合种共振判定（D1.4，M5） |

纯派生缓存：发布/编辑/删除事务内维护；迁移与同步校准后全量重建。**禁止**成为第二真相来源（重建函数永远从 records 重算）。

### 2.7 成就三表（D9）

**achievement_definitions**（产品内容，随版本分发，不迁移用户态）

| 列 | 类型 | 约束 |
|---|---|---|
| `code` | TEXT | PK（稳定标识，与 v1 code 兼容） |
| `title` / `description` / `emoji` | TEXT | NOT NULL |
| `category` | TEXT | NOT NULL（`MILESTONE/EXPLORATION/SEASONAL`） |
| `is_hidden` | INTEGER | NOT NULL DEFAULT 0 |
| `condition_type` | TEXT | NOT NULL（D9.1 枚举） |
| `condition_param` | INTEGER | NOT NULL |
| `condition_sub_param` | TEXT | NULL |
| `reward_type` | TEXT | NULL（`SKIN/BADGE/PLANT/EMOTE`；**无 GP 奖励**，ADR Q2） |
| `reward_value` | TEXT | NULL |
| `sort_order` / `updated_at` | | |

**achievement_progress**

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` | INTEGER | PK AUTOINCREMENT | |
| `definition_code` | TEXT | FK→definitions.code **RESTRICT** | |
| `scope_key` | TEXT | NOT NULL | `u:{userLocalId}` 用户域 / `s:{spaceLocalId}` 空间域（解决 NULL 参与唯一索引问题） |
| `progress` | INTEGER | NOT NULL DEFAULT 0 | |
| `is_unlocked` | INTEGER | NOT NULL DEFAULT 0 | 可回锁（D6.4） |
| `unlocked_at` | INTEGER | NULL | 回锁时清空 |
| `last_evaluated_at` | INTEGER | NOT NULL | |
| 同步列 ×6 | | | |
| 唯一约束 | `UNIQUE(definition_code, scope_key)` | | |

**achievement_events**（append-only 审计/动画重放/服务端校准）

| 列 | 类型 | 说明 |
|---|---|---|
| `local_id` INTEGER PK | |
| `definition_code` TEXT NOT NULL | |
| `scope_key` TEXT NOT NULL | |
| `event_type` TEXT NOT NULL | `UNLOCKED/RELOCKED/SERVER_CALIBRATED` |
| `progress_before` / `progress_after` INTEGER NOT NULL | |
| `occurred_at` INTEGER NOT NULL | |
| `reason_json` TEXT NULL | 触发记录 localId、服务端 diff 等 |
| 同步列 ×6 | |

### 2.8 `albums` / `album_pages`（I3 预备，Z3-01 落地）

**albums**

| 列 | 类型 | 说明 |
|---|---|---|
| `local_id` INTEGER PK | |
| `space_id` INTEGER NOT NULL FK **RESTRICT** | |
| `title` TEXT NOT NULL | |
| `theme` TEXT NOT NULL DEFAULT `fresh_spring` | |
| `range_type` TEXT NOT NULL | `ALL/STAGE/DATE` |
| `stage_start` / `stage_end` INTEGER NULL | |
| `date_start` / `date_end` INTEGER NULL | epoch day |
| `entry_count` / `page_count` INTEGER NOT NULL DEFAULT 0 | |
| `layout_seed` INTEGER NOT NULL | 确定性排版（架构方案 3.2.1） |
| `entry_hash` TEXT NOT NULL | 记录集合哈希 → 过期判定 |
| `cover_local_path` TEXT NULL | |
| 同步列 ×6 | |

**album_pages**

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `local_id` INTEGER PK | | |
| `album_id` INTEGER | FK→albums **CASCADE** | |
| `page_index` INTEGER | NOT NULL | 与 album_id 组成唯一 |
| `page_type` TEXT | NOT NULL | `COVER/GROWTH_TIMELINE/MOOD/TAG/MAP/BGM/LINK/MONTHLY/BACK_COVER` |
| `payload_json` TEXT | NOT NULL | **Z code 生成的 page model；MiniMax 只渲染不查 DAO**（迭代计划 4.2） |
| `created_at` / `updated_at` | | |
| 唯一约束 | `UNIQUE(album_id, page_index)` | | |

### 2.9 `outbox_events`（M4 同步扩展位，本轮建表不接线）

| 列 | 类型 | 说明 |
|---|---|---|
| `id` INTEGER PK AUTOINCREMENT | |
| `entity_type` TEXT NOT NULL | `RECORD/RECORD_MEDIA/SPACE/SPACE_MEMBER/...` |
| `entity_local_id` INTEGER NOT NULL | |
| `operation` TEXT NOT NULL | `UPSERT/DELETE` |
| `payload_json` TEXT NULL | 上传快照 |
| `state` TEXT NOT NULL DEFAULT `PENDING` | `PENDING/IN_FLIGHT/DONE/FAILED` |
| `attempts` INTEGER NOT NULL DEFAULT 0 | |
| `last_error` TEXT NULL | |
| `next_retry_at` INTEGER NULL | 指数退避 |
| `created_at` INTEGER NOT NULL | |

**无外键**（刻意）：Outbox 必须独立于实体物理生命周期，避免级联吞事件。幂等键 = `(entity_type, entity_local_id, operation, version∈payload)`（D10.6）。

---

## 3. 索引与唯一约束汇总

| 表 | 索引/约束 | 类型 | 目的 |
|---|---|---|---|
| records | `(space_id, occurred_date_key)` | 普通 | 额度/streak/时间线按日查询 |
| records | `(space_id, occurred_at DESC)` | 普通 | 时间线倒序 |
| records | `(author_id, occurred_at DESC)` | 普通 | 「我的记录」（M5 共享时间线） |
| records | `(sync_state)` | 普通 | Outbox 扫描 pending（M4） |
| records | `(server_id)` | **唯一** | 同步幂等 upsert |
| record_media | `(record_id)` | 普通 | 记录详情加载 |
| record_media | `(type)` | 普通 | PHOTO_COUNT / MUSIC_SONG_COUNT 统计（K7） |
| record_media | `(server_id)` | 唯一 | 同步 |
| tags | `(scope, space_id, kind, name)` | **唯一** | 重名去重；space_id 用 0 哨兵避免 SQLite「NULL 互不相等」破坏唯一性（这也是 §2.3 不加 FK 的原因） |
| tags | `(server_id)` | 唯一 | 同步 |
| record_tag_cross_ref | `(record_id, tag_id)` | 复合 PK | |
| record_tag_cross_ref | `(tag_id)` | 普通 | 标签反查记录 |
| spaces | `(server_id)` | 唯一 | |
| spaces | `(is_default)` 部分 `WHERE is_default=1` | 唯一（迁移 SQL 建，**不进 @Entity**） | 默认空间唯一 |
| space_members | `(space_id, user_id)` | 唯一 | |
| space_plants | `(space_id)` 部分 `WHERE is_active=1` | 唯一（迁移 SQL 建，不进 @Entity） | 每空间一棵活动植物（D8.4） |
| plant_snapshots | `(space_id, occurred_at)` | 普通 | 时间轴页 |
| achievement_progress | `(definition_code, scope_key)` | 唯一 | |
| achievement_events | `(scope_key, event_type)` | 普通 | 徽章墙过滤 |
| albums | `(space_id, created_at DESC)` | 普通 | 画册列表 |
| albums | `(server_id)` | 唯一 | |
| album_pages | `(album_id, page_index)` | 唯一 | |
| daily_space_stats | `(space_id, date_key)` | 复合 PK | 额度 O(1) |
| outbox_events | `(state, next_retry_at)` | 普通 | WorkManager 拉取 |

注：Room 注解处理器会校验 @Entity 声明的索引；部分唯一索引（`WHERE` 子句）Room 注解不支持，只能在 Migration 中 `execSQL` 创建。原判断「校验只要求声明的索引存在、多出的索引不导致失败」**未经 CI 验证**。2026-08-27 实施修订（Z1-01）：MIGRATION_1_2 暂不创建这两条部分索引，唯一性由应用层保证（`retireActiveSpacePlant` 先退后插、`DataBootstrap` 事务内双检），T16 验证通过后再于后续版本补建——T16 现由 MigrationFromV1Test 承担（Room 打开迁移库即做 schema 校验，索引若被宽容即可安全补建部分索引）。

**软删除约定**：除 `outbox_events`、`achievement_events`（append-only，物理删仅限内部清理）外，业务查询一律 `WHERE deleted_at IS NULL`；物理 DELETE 仅允许出现在内部维护路径（如 cross_ref 重写），用户触发的删除一律置 `deleted_at`。

---

## 4. v1 → v2 字段映射

### 4.1 `records`（v1 `records`）

| v1 列 | v2 目标 | 变换规则 |
|---|---|---|
| `id` | `records.local_id` | 原值保留（保持外键/时间线稳定） |
| `text` | `content_text` | 直拷 |
| `mood_tag` | `mood_tag`（NOT NULL） | **NULL → 回填 `平静`**，回填条数写入迁移报告（ADR 原则 1；禁止丢行） |
| `status_tags`（CSV） | `tags` + `record_tag_cross_ref` | 逐段 trim、去空、去重；命中内置 STATUS 注册表→`kind=STATUS`，否则 `kind=CUSTOM, scope=USER`；空串→无行 |
| `photo_uris`（`\|` 分隔） | `record_media(type=PHOTO)` 每张一行 | `source_uri=uri, local_path=uri, media_status=PENDING_COPY`（content:// 由迁移后修复任务落盘，K5）；`sort_order` 按原顺序 |
| `voice_uri` / `voice_duration` | `record_media(type=VOICE)` | 相机/录音本就写私有目录 → `media_status=READY` |
| `music_title` / `music_artist` / `music_uri` | `record_media(type=MUSIC)` | `title/subtitle`，uri 入 `extra_json` |
| `link_url` / `link_title` / `link_summary` | `record_media(type=LINK)` | `title=link_title, extra_json{summary}` |
| `location_name` / `location_lat` / `location_lng` | `record_media(type=LOCATION)` | `title=location_name, extra_json{lat,lng}` |
| `gp_earned` | `gp_final` | 直拷（**不按新公式重算**：已入账 GP 属既成事实，公式变更只影响新记录；差异由服务端校准机制处理，D10.3） |
| `createdAt` | `occurred_at` 与 `created_at` 都 = 原值 | v1 无补记功能，发生时间即创建时间（R5） |
| （新） | `occurred_date_key` | 本地时区 epoch day(occurred_at)；迁移时设备时区即用户时区，可接受并记录在迁移报告 |
| （新） | `is_backdated` | 全部 0（occurred=created） |
| （新） | `space_id` / `author_id` | 默认个人空间 / 本地用户（§5 步骤 4） |
| （新） | `gp_breakdown_json` / `is_capped` | NULL / 0（历史明细不可考） |
| （新） | 同步列 | `server_id=NULL, sync_state=0, version=0` |

### 4.2 `plant_states`（v1）

| v1 | v2 目标 | 规则 |
|---|---|---|
| `plant_type` | `plants.plant_type` + `space_plants.plant_type` | 目录种子内置 9 行（含 D9.2 条件）；v1 行只贡献状态 |
| `is_unlocked` | `plants.is_unlocked` / `unlocked_at` | unlocked=1 → 保留解锁，`unlocked_at = planted_at`（v1 无精确时间） |
| `is_active` | `space_plants.is_active=1`（默认空间） | 每空间唯一活动行；v1 恰好至多一行 active |
| `planted_at` | `space_plants.started_at` | |
| `total_gp` | **丢弃** | K3/R3 裁决：GP 唯一来源是记录集合（D7）；迁移报告记录「植物 GP vs 记录总分」差值（Demo 用户为 0 vs 116） |
| `last_watered_at` | 丢弃 | 可由最近记录派生 |

### 4.3 `achievements`（v1）

| v1 | v2 目标 | 规则 |
|---|---|---|
| `code/title/description/emoji/requirement` | `achievement_definitions` 种子 | **不迁移**，以 v2 定义表为准（数值按 ADR D9.3 修正，如 singer=50 首不同歌曲） |
| `progress/is_unlocked/unlockedAt` | `achievement_progress`（scope_key=`u:1`） | 仅迁移 code 在 v2 定义表中存在的行；code 消失（如 music_collector 若合并）→ 丢弃并在报告记录数量 |
| （新） | `achievement_events` | 已解锁行合成一条 `UNLOCKED`（occurred_at=原 unlockedAt，reason=`migrated_from_v1`） |

### 4.4 `spaces`（v1）与 `tags`（不存在）

- v1 `spaces` 有行则映射入 v2（type 枚举直转，`is_default` 仅 PERSONAL 且为第一行时置 1）；为空则只建默认空间。
- v1 无 `tags` 表（§0），无迁移；v2 从 MoodTag/StatusTag 注册表种子。

### 4.5 无 v1 来源的表

`users`（本地用户一行）、`space_members`（默认空间 OWNER 一行）、`daily_space_stats`（重建）、`plant_snapshots`（合成 `MIGRATED_BASELINE` 一条）、`albums/album_pages`（空；v1 画册为硬编码无数据）、`outbox_events`（空）。

---

## 5. v1→v2 Migration 步骤（`MIGRATION_1_2`）

实现形态：`Migration(1, 2)`，Room 自动包在事务里执行；步骤内任何异常 → 整体回滚（§7）。

```
步骤 0（事务外，App 层） 备份：复制 xiaoquexing.db（及 -wal/-shm）到
                          files/db_backup/xiaoquexing-v1-{ts}.db（保留 7 天）
步骤 1  改名旧表：records→records_v1, plant_states→plant_states_v1,
                          achievements→achievements_v1, spaces→spaces_v1
步骤 2  建全部 v2 表 + §3 声明索引；建两条部分唯一索引
        （spaces.is_default=1、space_plants.is_active=1）
步骤 3  种子：users 本地用户；plants 目录（9 行 + D9.2 条件）；
        achievement_definitions（D9.3）；tags 注册表（Mood 9 + Status 12）
步骤 4  空间：迁移 v1 spaces 行；确保存在唯一 is_default=1 的个人空间
步骤 5  记录与派生：
   5a      逐行拷贝 records_v1 → records（§4.1 规则：mood 回填、dateKey、
          媒体/标签拆表；拆表行数与源字段段数一致才继续，否则抛错回滚）
   5b      space_members 写默认 OWNER 行
   5c      space_plants：v1 active 植物 → 默认空间活动行；其余已解锁植物仅
          留在 plants 目录（等用户再选用时建实例行）
   5d      achievements 进度迁移 + 合成 UNLOCKED 事件（§4.3）
   5e      派生重建（同一条 SQL，唯一真相来源）：
            UPDATE spaces SET total_gp =
              (SELECT COALESCE(SUM(gp_final),0) FROM records
               WHERE space_id=spaces.local_id AND deleted_at IS NULL);
          → Demo 用户得到 116（修复 K3 的既成不一致）
   5f      daily_space_stats 全量重建（GROUP BY space_id, occurred_date_key）
   5g      plant_snapshots 合成 MIGRATED_BASELINE
          （stage=PlantStage.fromGp(spaces.total_gp), gp_at_event=total_gp）
步骤 6  校验（失败即抛异常触发回滚）：
          - 各表行数：records、media 段数、cross_ref 关联完整
          - records 无 NULL mood_tag、无 NULL space_id/author_id
          - 每空间 active 植物 ≤1、is_default 空间 =1
          - SUM 派生值与 spaces.total_gp 一致
步骤 7  删旧表 records_v1/plant_states_v1/achievements_v1/spaces_v1；
        PRAGMA foreign_key_check 清空
步骤 8  （事务提交后，App 层异步）媒体修复任务：遍历
        media_status=PENDING_COPY 且 type=PHOTO 的行，把 content:// 复制到
        files/media/photos/{recordLocalId}_{sort}.jpg，成功→READY，
        失败（SecurityException/FileNotFound）→MISSING（UI 占位图，可重试）
```

配套工程要求（Z1-01 落地清单）：

- `@Database(version = 2, exportSchema = true)` + `room.schemaLocation` KSP 参数，schemas/1.json 由本迁移测试的手写 v1 DDL 生成基准后存档。
- `AppDatabase` 移除 `fallbackToDestructiveMigration()`（K4），改为 `.addMigrations(MIGRATION_1_2)`；未提供路径的版本升级直接失败进入 §7 流程。
- 迁移报告（行数、mood 回填数、GP 差值、丢弃项）写入 `files/db_backup/migration-report-1-2.json`，供客诉与审计。

---

## 6. 本地事务边界（Z1-02 的接口约定）

v2 之后「发布/编辑/删除」是**唯一允许改这些表的入口**，全部跑在 `withTransaction` 内，保证 K2 不再发生：

```
publishRecord(cmd)  单事务：
  insert records + record_media + cross_ref
  upsert daily_space_stats（gp_total += gpFinal；含封顶判断）
  update spaces.total_gp += gpFinal
  if 阶段变化 → insert plant_snapshots(STAGE_UP/DOWN)
  重估 achievement_progress（+UNLOCKED/RELOCKED 事件）、plants.is_unlocked
  insert outbox_events(RECORD UPSERT)

editRecord(id, cmd)  单事务：
  重算 gpFinal（目标发生日剩余额度，D5）→ update records/media/cross_ref
  重算新旧两个 (space, date) 的 daily_space_stats
  update spaces.total_gp（全量重算该空间，非增量，防漂移）
  阶段/成就/植物解锁重估 + 快照事件
  outbox UPSERT

softDeleteRecord(id)  单事务：
  deleted_at=now, sync_state=DELETE_PENDING
  级联重算：daily_space_stats、spaces.total_gp（全量重算）、
  阶段（可降级 → STAGE_DOWN）、成就回锁、植物解锁重估（D6）
  outbox DELETE（墓碑事件）
```

一致性不变量（`RecomputeService.assertInvariants()`，debug 构建每次写后跑）：

1. `spaces.total_gp == SUM(records.gp_final WHERE 未删除 AND space_id=?)`
2. `daily_space_stats.gp_total == SUM(同上 GROUP BY date_key)`
3. `records.gp_final <= 100 −（同日更早记录已占额度）`（按发布顺序）
4. 每空间至多一棵 `is_active` 植物
5. `achievement_progress.is_unlocked == (progress >= condition_param)`（对可求值条件）

---

## 7. 迁移失败处理

1. **事务原子性**：SQLite DDL 可回滚，`MIGRATION_1_2` 任一步抛错 → Room 回滚整个迁移 → 数据库停留在 v1 完好状态。
2. **失败呈现**：捕获 `MigrationContainer` 抛出的异常后，App 进入「升级失败」页：展示错误摘要 + 「重试」「导出数据（Z2-04 的本地导出，v1 版本先提供原始 db 复制到 Downloads 的降级方案）」；**绝不静默清库**（K4）。
3. **重试上限**：连续 3 次失败 → 停止自动重试，只保留人工操作入口，并把备份路径展示给用户。
4. **崩溃循环保护**：失败计数存 DataStore；达到上限后启动时直接进失败页，不再尝试 openDatabase（避免崩溃循环把 -wal 刷坏）。
5. **可诊断性**：迁移报告 + 失败堆栈写入本地日志文件（随反馈入口上传，M2）。

## 8. 数据回滚策略

- **备份先行**：§5 步骤 0 的 v1 库副本保留 7 天或直至两次成功启动。
- **数据级回滚**（App 不降级，Android 禁止 APK 回退）：迁移反复失败时，用户可选择「恢复到升级前数据」——用备份库覆盖当前库并以 v1 结构进入**只读安全模式**（可浏览/导出，不可写），等待修复版本；或「以空库继续」（需二次确认 + 已提示备份位置，属用户显式授权的破坏性操作）。
- **半升级状态**：迁移事务回滚后不存在半升级 schema；但 App 层备份文件写入失败时**必须中止迁移**（无备份不迁移）。
- **远期**：M4 接服务端后，回滚兜底从「本地备份」升级为「云端恢复」。

---

## 9. 测试矩阵（GitHub Actions `testDebugUnitTest`，Z0-05/Z1-01 建档）

前置：v1 无 schema JSON，测试以 `MigrationTestHelper.createDatabase` + **手写 v1 DDL fixture**（对照 `Record.kt`/`PlantState.kt`/`Achievement.kt`/`Space.kt` 的注解逐列生成）构造 v1 库。

| # | 用例 | 断言 |
|---|---|---|
| T1 | 空库 v1→v2 | 仅种子数据；默认空间/用户/OWNER 存在；无记录 |
| T2 | 全新安装 v2 | schema 校验通过；种子同 T1 |
| T3 | 黄金数据集迁移（含全部媒体类型、标签、9 图记录） | 逐字段映射正确（§4.1）；媒体行数=段数；cross_ref 完整 |
| T4 | `mood_tag` 为 NULL 的行 | 回填 `平静`；报告计数=实际数 |
| T5 | `status_tags` 边界：空串/尾逗号/重复段/未知名 | 无空标签行；CUSTOM 归类正确；无重复 cross_ref |
| T6 | `photo_uris` 0 张与 9 张 | 0/9 行 PHOTO；sort_order 连续；全部 PENDING_COPY |
| T7 | Demo 不一致数据（records 合 116 GP、植物 totalGp=0） | `spaces.total_gp=116`；植物表无 GP 列；baseline 快照 `stage=SPROUT`（116 ∈ [50,200)，`PlantStage.fromGp(116)`） |
| T8 | v1 含 spaces 行 | 映射正确；默认空间唯一性索引生效 |
| T9 | 迁移中途失败注入（步骤 5a 段数不一致） | 抛错；重开库仍为 v1 完整数据（行数不变） |
| T10 | 迁移后 FK 完整性 | `PRAGMA foreign_key_check` 空 |
| T11 | 唯一约束：重复 server_id / 双活动植物 / 重复 (code,scope) / 重复标签名 | 全部被拒 |
| T12 | 软删除排除：删除一条当日已 90 GP 记录后 | 额度重算、total_gp 重算、阶段可降级、成就可回锁（D6 全链） |
| T13 | 额度口径：同日第二条跨午夜插入 / 补记昨天 | 占 (space, occurred_date_key) 而非今天（D3/D4） |
| T14 | streak：DST 日（如 America/Santiago 切换日）+ 本地时区变更 | epoch day 口径不跳天（D2.6） |
| T15 | 事务原子性：publish 中成就重估抛错 | 记录/媒体/统计全部回滚，无「有记录没长植物」 |
| T16 | 部分 unique 索引与 Room schema 校验共存 | `openDatabase` 校验通过（§3 注） |
| T17 | 查询计划：额度/时间线/streak 查询走 (space_id, occurred_date_key) | `EXPLAIN QUERY PLAN` 无全表扫 |
| T18 | 不变量断言器：手工制造 total_gp 漂移 | `assertInvariants()` 报错（防 K2 复发） |
| T19 | 迁移幂等语义：对已 v2 库误跑 MIGRATION_1_2 入口 | 拒绝/无操作（版本保护） |
| T20 | outbox 联动：publish/edit/delete 各产生正确事件且无外键级联吞事件 | 事件存在、state=PENDING |

---

## 10. 对 I1–I5 的扩展能力分析

| 迭代 | 需求 | v2 支撑 | 缺口/后续 |
|---|---|---|---|
| **I1** 单人闭环 | CRUD、补记 365 天、媒体持久化、事务一致、初始化 | `occurred_at + occurred_date_key` 支撑补记与额度；`record_media.local_path/source_uri/media_status` 支撑复制与占位；§6 事务用例；`spaces/space_members/users` 种子即初始化（无 Demo 记录，D12） | Z1-05 实现复制任务；Z1-02 实现事务用例 |
| **I2** 回顾/分享/统计 | 搜索筛选、真实分享卡、导出导入、正确统计 | `occurred_date_key` 去重=记录天数（K6）；media type/title 支撑照片张数、不同歌曲、不同地点（K7）；软删除保证导出一致；`achievement_events` 支撑 SHARE_COUNT 埋点 | 全文搜索如需可另加 FTS4 虚拟表（不动主表，无破坏迁移） |
| **I3** 电子画册 | 聚合、确定性排版、缓存过期、翻阅/导出 | `albums/album_pages(payload_json)` 明确 Z code 出 model、MiniMax 渲染的边界；`layout_seed + entry_hash` 过期判定；`plant_snapshots` 供生长时间轴页 | Z3-02 聚合引擎与 page model 定义 |
| **I4** 云同步 | 登录、Outbox、冲突、删除、服务端权威 GP | 全实体同步列 + `outbox_events`（幂等键就绪）；墓碑优先（D10.5）；`gp_breakdown_json` 支撑服务端校准 diff 审计 | Z4-04 接 WorkManager；users 表接账号后扩展 |
| **I5** 共享空间 | 成员、权限、合种、贡献、互动 | `space_members.role + records.author_id` 支撑作者独占改删（D11）；`contributed_gp` 温和展示；`distinct_author_count` 支撑共振幂等（D1.4）；`tags.scope=SPACE`；成就 `scope_key=s:{id}` 空间域 | Z5-01 服务端 API；互动（like/comment）表 v3 增补——字段模式与本表族一致（同步列 + 双外键），无破坏性重构 |

结论：I1–I5 均不需要再发生「改主键/改真相来源」级别的破坏性重构；预期新增表（interactions、invites、FTS）走正常 version+1 增量迁移。

---

## 11. 仍需产品确认的问题

ADR 附 B 的 Q1–Q12 全部继续有效（默认值已在本文档与 ADR 中冻结执行）。schema 层新增两项：

| # | 问题 | 当前设计取舍 |
|---|---|---|
| S1 | 历史记录的 `gp_earned` 迁移时**不按新公式重算**（既成事实），是否接受新旧公式 GP 混存于同一空间？ | 接受；差异留给 M4 服务端校准。若产品要求统一重算，迁移 5e 后追加重算步骤即可（代价是用户可见分数跳变） |
| S2 | 画册是否需要服务端备份（会员权益）？影响 albums 是否进 Outbox | 已预留同步列；不确认则 I3 先纯本地 |

---

## 12. 实现交接清单（Z1-01 输入）

> **2026-08-27 进度**：第 1–5 项已随 Z1-01/Z1-02/Z1-03 落地（17 张表实体、DAO、`MIGRATION_1_2`、`DataBootstrap`、publish/softDelete 事务用例与 `AchievementEvaluator`）。**Z1-04/Z1-05/Z1-07 轮追加**：`editRecord` 事务（§6 三用例齐备）+ 365 天/未来时间窗口校验（D4.1）；`MediaImporter`（content:// 落盘、MISSING、孤儿清理，K5）+ `MigrationGuard` v1 迁移前备份（§8 前半）；Demo 记录已由 `BuildConfig.DEBUG` 守卫（ADR D12）；`RecomputeService.assertInvariants`（T18）已接入事务/编辑/删除测试。测试矩阵新增覆盖：T13（补记额度口径）、T18、T20（编辑也产生 UPSERT）；§7 的失败页与重试上限仍待与 M1-02 详情页一起交付。待 CI 首跑后：提交生成的 `app/schemas/.../2.json` 基线；T14（DST 注入）、T17（查询计划）随后补齐。

1. ~~按 §2 建 17 张表的 Entity/DAO~~（已完成，`data/db/entity/` + `data/db/dao/`）。
2. ~~手写 v1 DDL fixture -> 产出 schemas 基准~~（fixture 就绪；2.json 由 CI 生成后提交）。
3. ~~实现 `MIGRATION_1_2`（§5）~~（已完成，`data/db/Migrations.kt`；备份与失败页归 §7/§8 的 App 层工作，随 Z1-04 落地）。
4. ~~实现 §6 三个事务用例~~（publish/softDelete 已实现；editRecord 随 Z1-04 编辑功能落地）。
5. ~~建 T1–T20 测试（§9）~~（部分完成，见上方进度注）。
6. ~~移除 `fallbackToDestructiveMigration()`~~（已于 Z0-03 轮随 DI 改造落地，ADR-002）；移除 `XiaoQueXingApp` Demo 注入（Z1-07 范围，ADR D12；当前 Demo 已改走 `seedRecordWithFixedGp` 事务种子并带 116 GP 一致性校验）。
