# `xiaoquexing-server` 服务端开发规格 v1

## 1. 交付目标

服务端 v1 需要支持 Android 的以下闭环：

1. 手机验证码登录和多设备会话。
2. 个人空间、记录、媒体、标签、GP、植物和成就云同步。
3. 离线 mutation 幂等 push、增量 pull、版本冲突和软删除。
4. 私有 OSS 直传与下载授权。
5. 共享空间、成员邀请、作者权限和合种 GP。
6. 画册元数据、统计数据和分享 Token。
7. 数据导出、账号注销、日志、监控和阿里云部署。

## 2. 非目标

- 不在首版实现公开内容广场。
- 不在服务端代理照片/音频文件上传。
- 不做微服务、Kubernetes、复杂 MQ。
- 不做 AI 回顾、会员支付、实体画册。
- 不允许服务端依赖 Android Room 自增 ID；只接收 `clientLocalId` 作为回传映射。

## 3. 技术栈与质量标准

| 类别 | 选择 |
|---|---|
| 语言 | Go，使用当前受支持稳定版本并在 `go.mod`/CI 固定 |
| HTTP | Gin + 标准 `net/http` 中间件 |
| 数据库 | PostgreSQL，`pgx/v5` + `sqlc`（或等价类型安全查询层） |
| Migration | `golang-migrate`，SQL migration 入库 |
| 缓存 | Tair/Redis，`go-redis/v9` |
| API 契约 | OpenAPI 3.1 |
| ID | UUIDv7 字符串；数据库 `uuid` |
| 日志 | JSON stdout，SLS 采集 |
| 配置 | 环境变量 + typed config；secret 来自 KMS/ECS RAM Role |
| 镜像 | 多阶段 Dockerfile、非 root 用户、只读 rootfs 可运行 |
| 测试 | unit、repository integration、migration、API contract、race |

质量门禁：

- `go test ./...`
- `go test -race ./...`
- `golangci-lint run`
- OpenAPI lint + breaking change 检查
- migration up/down/preflight 测试
- Docker image build + vulnerability scan
- secret scan

## 4. 领域规则

服务端必须复制 Android `docs/adr/ADR-001-domain-rules.md` 到本仓库 `docs/domain-rules.md`，并写入 `DOMAIN_RULES_VERSION=1`。

硬性规则：

- 心情必选一个。
- GP 属于空间；`spaces.total_gp` 是缓存，记录集合可重算。
- 每日额度键为 `(spaceId, occurredDate)`，空间内全体成员合计最多 100 GP。
- streak 按空间和发生自然日计算，使用客户端提交的 timezone 验证/归一。
- 补记最多 365 天，编辑/删除会重算 GP/植物/成就。
- 删除是墓碑，墓碑优先于旧版本更新。
- 作者才能编辑/删除记录。
- 客户端可乐观计算，服务端返回值是最终结果。

## 5. PostgreSQL 数据模型

所有业务表包含 `created_at timestamptz`、`updated_at timestamptz`；需要同步的表包含 `version bigint not null default 1`、`deleted_at timestamptz null`。

### 5.1 身份与会话

#### `users`

- `id uuid pk`
- `display_name varchar(80)`
- `avatar_object_key text null`
- `status varchar(20)`：ACTIVE/PENDING_DELETE/DELETED
- `delete_requested_at timestamptz null`

#### `auth_identities`

- `id uuid pk`
- `user_id uuid fk users`
- `type varchar(20)`：PHONE
- `identifier_hash char(64) unique`
- `phone_encrypted bytea`
- `verified_at timestamptz`

手机号不明文索引；hash 用于查找，加密值仅用于必要业务。

#### `devices`

- `id uuid pk`
- `user_id uuid fk`
- `client_device_id varchar(128)`
- `platform varchar(20)`
- `app_version varchar(40)`
- `last_seen_at timestamptz`
- unique `(user_id, client_device_id)`

#### `refresh_tokens`

- `id uuid pk`
- `user_id/device_id`
- `family_id uuid`
- `token_hash char(64) unique`
- `expires_at/revoked_at/replaced_by`

### 5.2 空间与成员

#### `spaces`

- `id uuid pk`
- `name varchar(100)`
- `space_type`：PERSONAL/COUPLE/FAMILY/FRIEND
- `owner_id uuid`
- `total_gp bigint default 0`
- `active_plant_type varchar(30)`
- `timezone varchar(64)`

#### `space_members`

- `space_id/user_id` composite unique
- `role`：OWNER/ADMIN/MEMBER
- `status`：ACTIVE/LEFT/REMOVED
- `joined_at/left_at`
- `contributed_gp bigint`

#### `space_invites`

- `id uuid pk`
- `space_id/inviter_id`
- `token_hash char(64) unique`
- `expires_at/max_uses/used_count/revoked_at`

### 5.3 记录、媒体与标签

#### `records`

- `id uuid pk`
- `client_local_id bigint null`（只用于该设备映射，不全局唯一）
- `space_id/author_id`
- `content_text varchar(500) null`
- `mood_tag varchar(30) not null`
- `occurred_at timestamptz not null`
- `occurred_date date not null`
- `occurred_timezone varchar(64) not null`
- `is_backdated boolean`
- `gp_final int`
- `gp_capped boolean`
- `gp_breakdown jsonb`
- `version bigint`
- `deleted_at`
- index `(space_id, occurred_at desc)`
- index `(author_id, updated_at desc)`

#### `record_media`

- `id uuid pk`
- `record_id uuid fk`
- `type`：PHOTO/VOICE/MUSIC/LINK/LOCATION
- `sort_order int`
- `object_key text null`
- `upload_status`：PENDING/READY/MISSING/DELETED
- `mime_type/size_bytes/sha256/width/height/duration_ms`
- `title/subtitle/source_url/extra jsonb`

#### `tags`

- `id uuid pk`
- `scope`：SYSTEM/USER/SPACE
- `owner_user_id/space_id`
- `kind`：STATUS/CUSTOM
- `name varchar(40)`
- partial unique index 按 scope/owner/kind/name

#### `record_tags`

- composite pk `(record_id, tag_id)`

### 5.4 GP、植物与成就

#### `daily_space_stats`

- composite pk `(space_id, occurred_date)`
- `gp_total int check 0..100`
- `record_count/distinct_author_count`
- 写记录时行锁保证上限。

#### `plant_definitions`

- `code pk`
- `condition_type/condition_value`
- `config jsonb`

#### `plant_snapshots`

- `id uuid pk`
- `space_id/plant_type/event_type/stage/gp_at_event`
- `occurred_at`

#### `achievement_definitions`

- `code pk`
- `scope/condition_type/condition_value/reward jsonb`

#### `achievement_progress/events`

- progress unique `(definition_code, scope_key)`
- event 包含 before/after、event_type、reason jsonb。

### 5.5 同步、画册与互动

#### `applied_mutations`

- `mutation_id uuid unique`
- `user_id/device_id`
- `request_hash char(64)`
- `response_json jsonb`
- 相同 mutationId 不同 hash 返回安全错误。

#### `change_log`

- `sequence bigserial pk`
- `entity_type/entity_id/space_id/version/op`
- `changed_at`
- payload 可存最小快照或由实体查询组装。

#### `albums/album_pages/album_shares`

- 保存范围、主题、layoutSeed、entryHash、页模型、分享 token hash 和过期时间。

#### `reactions/comments`

- 只对共享空间开放；所有查询验证成员权限。

## 6. API 模块

### S0：基础设施与健康

- `GET /health/live`
- `GET /health/ready`
- `GET /api/v1/meta`
- request ID、JSON error、panic recovery、timeout、body limit、CORS（默认关闭非必要来源）。

### S1：认证

- `POST /api/v1/auth/sms/send`
- `POST /api/v1/auth/sms/verify`
- `POST /api/v1/auth/token/refresh`
- `POST /api/v1/auth/logout`
- `DELETE /api/v1/account`

验证码必须有手机号/IP/设备/全局限流；无论手机号是否注册，对外响应避免账号枚举。

### S2：同步与记录

- `POST /api/v1/sync/push`
- `GET /api/v1/sync/pull?cursor=&limit=`
- `GET /api/v1/records?spaceId=&cursor=&limit=&tag=&mood=&from=&to=`
- `GET /api/v1/records/{id}`
- `POST/PATCH/DELETE /api/v1/records/{id}` 作为在线 REST 入口

所有 mutation 统一走应用服务层，不能 REST 和 sync 各写一套 GP 逻辑。

### S3：媒体

- `POST /api/v1/media/sts`
- `POST /api/v1/media/complete`
- `GET /api/v1/media/{id}/download-url`
- `DELETE /api/v1/media/{id}`

### S4：空间与共享

- `GET/POST /api/v1/spaces`
- `GET/PATCH/DELETE /api/v1/spaces/{id}`
- `GET /api/v1/spaces/{id}/members`
- `POST /api/v1/spaces/{id}/invites`
- `POST /api/v1/invites/{token}/accept`
- `DELETE /api/v1/spaces/{id}/members/{userId}`
- `POST /api/v1/spaces/{id}/leave`

### S5：植物、成就、统计和画册

- `GET /api/v1/spaces/{id}/plant`
- `PATCH /api/v1/spaces/{id}/plant`
- `GET /api/v1/achievements`
- `GET /api/v1/stats/mood`
- `GET /api/v1/reviews/monthly`
- `GET/POST/DELETE /api/v1/albums`
- `GET /api/v1/albums/{id}`
- `POST/DELETE /api/v1/albums/{id}/share`
- `GET /public/albums/{token}`

## 7. 统一响应与错误

成功：

```json
{
  "data": {},
  "meta": {"requestId": "...", "serverTime": "2026-08-27T10:00:00Z"}
}
```

错误：

```json
{
  "error": {
    "code": "RECORD_VERSION_CONFLICT",
    "message": "记录已在其他设备更新",
    "details": {},
    "retryable": false
  },
  "meta": {"requestId": "..."}
}
```

错误码至少包含：

- `AUTH_REQUIRED/TOKEN_EXPIRED/REFRESH_REUSED`
- `SMS_RATE_LIMITED/SMS_CODE_INVALID`
- `SPACE_FORBIDDEN/SPACE_MEMBER_LIMIT`
- `RECORD_INVALID/RECORD_VERSION_CONFLICT/RECORD_DELETED`
- `MUTATION_ID_REUSED/MUTATION_DEPENDENCY_MISSING`
- `MEDIA_QUOTA_EXCEEDED/MEDIA_VERIFY_FAILED`
- `SYNC_CURSOR_EXPIRED`
- `INTERNAL_ERROR/SERVICE_UNAVAILABLE`

## 8. 同步事务要求

### Push

1. 校验 auth/device/request size。
2. 对 mutationId 查询 `applied_mutations`。
3. 已应用且 hash 一致：返回原 response；hash 不同：拒绝。
4. 校验 membership/author/baseVersion。
5. 对目标 `(space,date)` stats 行加锁。
6. 执行业务写入、GP/成就/植物重算。
7. 插入 change_log。
8. 保存 applied mutation response。
9. commit 后返回 serverId/version/authoritative state。

批量 mutation 第一版采用“单条独立事务、逐条结果”，避免一条坏数据回滚整批；支持 `dependsOnMutationId` 解决先建记录再绑定媒体。

### Pull

- cursor 绑定 user 和 change sequence，签名或服务端保存。
- 只返回当前用户有权限的空间变更。
- 返回 tombstone。
- `hasMore/nextCursor` 必须稳定；limit 默认 100，上限 500。
- 成员被移除后，pull 返回空间访问撤销事件，客户端清理/隔离本地缓存。

## 9. 阿里云接入开发点

### OSS/STS

- ECS 绑定 `xqx-server-role`，只允许 AssumeRole 和必要 OSS 操作。
- `xqx-mobile-upload-role` 的 session policy 限制 Bucket、用户前缀、操作和大小。
- object key：`env/users/{userId}/records/{recordId}/{mediaId}/{sha256}.{ext}`。
- Bucket 私有；禁止 Android 持有长期 AK。

### 短信

- 使用阿里云 SMS SDK/OpenAPI。
- Dev 使用白名单/Mock provider；Prod 才真实发送。
- 记录阿里云 requestId，但日志不打印验证码或完整手机号。

### RDS/Tair

- 仅 VPC 内访问，不配置公网白名单。
- 连接池限制与 RDS 最大连接数匹配。
- Redis Key 带环境前缀：`xqx:{env}:...`。
- Redis 故障时登录验证码可返回服务暂不可用，但已登录记录 API 不应因非关键缓存失败而全部不可用。

## 10. 分阶段开发任务

| 阶段 | 负责人建议 | 交付物 | 退出条件 |
|---|---|---|---|
| S0 脚手架 | Gork | Go 工程、Docker、CI、config、health、统一错误、OpenAPI 基线 | CI 绿；容器 health ready |
| S1 DB/Auth | Gork | PostgreSQL migration、用户/设备/Token、短信 provider/Mock、限流 | 登录/刷新/重放/限流集成测试通过 |
| S2 Record/Sync | Gork，Z code 审协议 | record/media/tag、mutation 幂等、push/pull、GP/streak | 双设备离线冲突测试通过 |
| S3 OSS | Gork | STS、complete verify、download URL、配额 | Android 可直传且服务端不见 AK |
| S4 Space | Gork | 空间/成员/邀请/权限/共享 GP | 越权测试、邀请过期/撤销通过 |
| S5 Growth/Album | Gork | 植物、成就、统计、画册元数据/分享 | 与 ADR 金丝雀用例结果一致 |
| S6 Deploy/Ops | Gork | ECS/ACR/ALB/RDS/Tair/OSS、SLS、告警、备份恢复文档 | staging 部署、回滚、恢复演练通过 |

## 11. 服务端 Definition of Done

- OpenAPI 与实现一致；所有新增接口含示例和错误码。
- migration 可从空库启动，也可从前一版本升级；无自动 destructive reset。
- 写 API 支持 Idempotency-Key 或 mutationId。
- 所有 space scoped 查询都有权限测试。
- Token、验证码、手机号、记录正文不进入日志。
- 单测/集成/race/lint/OpenAPI/Docker/secret scan 全绿。
- 镜像为不可变 SHA tag，非 root 运行，无源码和凭据。
- Staging 完成部署、数据库升级、回滚和备份恢复演练。
- Android 契约测试通过后才能发布 `/api/v1`。
