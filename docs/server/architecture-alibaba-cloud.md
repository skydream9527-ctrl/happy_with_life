# 前后端分离与阿里云部署架构

## 1. 设计目标

- Android 离线可记录，联网后自动同步。
- 服务端是跨设备、共享空间、GP/成就和权限的最终权威。
- 媒体不经过 Go API 中转，降低 ECS 带宽和内存压力。
- MVP 控制运维复杂度，先用模块化单体；用户量和团队规模证明必要后再拆服务。
- 生产环境无单点：入口、API、数据库和缓存均有可恢复或高可用方案。
- 所有阿里云访问遵守最小权限，Android 和 GitHub 中不保存长期 AccessKey。

## 2. 总体拓扑

```text
Android App
  ├─ Room v2 + Outbox + WorkManager
  ├─ HTTPS REST / Sync API
  └─ OSS Android SDK + 短期 STS
          │
          ▼
Alibaba Cloud DNS
          │
          ▼
HTTPS ALB ── WAF（生产启用）
          │
          ├──────────────┐
          ▼              ▼
ECS-A / Docker      ECS-B / Docker
  xqx-api              xqx-api
  xqx-worker*           xqx-worker*
          │              │
          └──────┬───────┘
                 │ VPC 私网
      ┌──────────┼──────────┬──────────┐
      ▼          ▼          ▼          ▼
RDS PostgreSQL  Tair Redis  OSS 私有桶  SLS/CloudMonitor
  主数据/事务    验证码/限流  照片/音频   日志/指标/告警

* 第一阶段 API 与 Worker 同镜像、不同 command；低流量时可同容器进程部署。
```

## 3. 前后端职责边界

### Android 负责

- Compose UI、表单、媒体采集和本地文件管理。
- Room 作为离线缓存和本地操作真相；Outbox 保存待同步 mutation。
- 网络恢复后由 WorkManager push/pull。
- 本地乐观计算 GP、植物阶段和成就反馈，提升即时体验。
- 使用服务端签发的短期 STS 凭证直传 OSS。
- 收到服务端结果后校准 `serverId/version/gpFinal/plant/achievement`。

### 服务端负责

- 用户、手机号验证、Token、设备和会话管理。
- 空间、成员、邀请、角色与所有数据访问权限。
- 记录/媒体元数据/标签的云端持久化和版本控制。
- 幂等、冲突、软删除墓碑和增量同步游标。
- GP 每日上限、streak、植物阶段、成就的最终计算。
- OSS STS 签发、对象 Key 约束、上传完成校验、私有下载 URL。
- 共享互动、画册元数据、统计和自动回顾。
- 审计、日志、监控、备份、数据导出和账号注销。

### 共同契约

- `openapi/openapi.yaml`：REST API 唯一来源。
- `docs/domain-rules.md`：由 Android `ADR-001` 复制/链接并保持版本号。
- `migrations/`：PostgreSQL schema 唯一来源。
- `sync mutation schema`：使用 JSON Schema/OpenAPI 固化。
- `X-Request-ID`、`Idempotency-Key`、错误码、分页和时间格式必须统一。

## 4. 阿里云环境分层

### Dev：Gork 云端开发

- 1 台 ECS（2 vCPU / 4 GB 起步），Docker Compose。
- RDS PostgreSQL Basic 或独立 Dev 实例；禁止与生产共库。
- Tair/Redis Dev 实例；也可短期用容器 Redis，但不能带入生产。
- OSS Dev 私有 Bucket，生命周期 7–30 天。
- 域名 `api-dev.<domain>`，独立短信模板/测试白名单。

### Staging

- 1 台 ECS + 与生产同镜像/同启动方式。
- 独立 RDS/Tair/OSS；使用脱敏数据。
- 运行 API contract、migration、回滚和弱网同步验收。
- 域名 `api-staging.<domain>`，只允许测试账号。

### Production

- 单 Region、至少两个可用区。
- ALB 对外，两个 ECS 分布在不同可用区，只开放 ALB → API 端口。
- RDS PostgreSQL 高可用版、跨可用区主备；启用自动备份和 PITR。
- Tair/Redis 标准高可用，Redis 不是主数据，丢失后应可重建。
- OSS 私有 Bucket，服务端签发 STS/签名 URL；启用服务端加密和生命周期。
- WAF 防护 API 域名；CloudMonitor + SLS 监控告警。

阿里云官方建议在 Layer 7 场景使用 ALB 连接 VPC 内 ECS，并可通过多可用区后端实现高可用；RDS PostgreSQL 高可用版采用主备架构并可跨可用区部署：

- [ECS 通过 ALB 对外提供服务](https://help.aliyun.com/en/cloud-network-well-architected-design/ecs-instances-in-the-vpc-are-accessed-through-the-public-network-through-layer-7-alb)
- [RDS PostgreSQL 高可用版](https://help.aliyun.com/en/rds/apsaradb-rds-for-postgresql/rds-high-availability-edition)

## 5. 服务端技术架构

### 代码形态：模块化单体

```text
xiaoquexing-server/
├── cmd/
│   ├── api/main.go
│   ├── worker/main.go
│   └── migrate/main.go
├── internal/
│   ├── auth/
│   ├── user/
│   ├── space/
│   ├── record/
│   ├── media/
│   ├── growth/
│   ├── achievement/
│   ├── album/
│   ├── sync/
│   ├── review/
│   ├── platform/       # PostgreSQL/Redis/OSS/SMS/KMS/SLS
│   └── transport/http/
├── migrations/
├── openapi/
├── configs/
├── deploy/
│   ├── docker/
│   ├── compose/
│   ├── nginx/
│   └── scripts/
├── tests/
├── Dockerfile
├── docker-compose.dev.yml
├── Makefile
└── go.mod
```

### Go 组件建议

- HTTP：Gin；中间件自行保持轻量。
- PostgreSQL：`pgx/v5`；查询可用 `sqlc` 生成，migration 使用 `golang-migrate`。
- Redis：`go-redis/v9`。
- 配置：环境变量 + 明确的 config struct；生产 secret 从 KMS/Secret Manager 或 ECS RAM Role 获取。
- 日志：结构化 JSON，输出 stdout；字段包含 requestId/userId/deviceId/route/status/latency/errorCode。
- API 文档：OpenAPI 3.1，CI 校验 breaking changes。
- 测试：单元测试 + PostgreSQL/Redis 集成测试（Testcontainers 或 CI service containers）。

## 6. 数据与一致性

- PostgreSQL 是云端主数据；Redis 只用于验证码、限流、短期缓存和分布式锁。
- 记录写入必须在一个 PostgreSQL transaction 中完成：幂等校验 → 记录/媒体/标签 → 当日 GP 行锁 → 空间 GP → 植物/成就事件 → change log。
- 每个 mutation 有全局唯一 `mutationId`，数据库建立唯一约束；客户端重试返回第一次结果。
- GP 额度通过 `(space_id, occurred_date)` 唯一行 + `SELECT ... FOR UPDATE` 串行化，避免共享成员并发突破 100。
- 更新带 `baseVersion`；服务器版本不一致返回 `409 CONFLICT` 和当前实体。
- 删除为 tombstone；pull 同步必须返回墓碑，禁止其他设备复活已删除记录。
- `change_log.sequence BIGSERIAL` 提供增量 cursor；cursor 是不透明字符串，客户端不能解析。

## 7. 媒体链路

```text
1. Android 向 API 请求 /media/sts，声明 mime/size/sha256。
2. API 校验配额与权限，通过 ECS RAM Role 调 STS AssumeRole。
3. API 返回 15–30 分钟临时凭证和受限 objectKeyPrefix。
4. Android 直接上传私有 OSS。
5. Android 调 /media/complete，API HeadObject 校验 size/hash/content-type。
6. API 将媒体状态从 PENDING 改 READY，并写 change log。
7. 下载使用短期签名 URL，不能把 Bucket 设为公共读。
```

阿里云明确建议移动端不要保存 AccessKey，而应向应用服务端请求 STS 临时凭证，再由移动端直传 OSS：

- [移动 App 直传 OSS](https://help.aliyun.com/en/oss/user-guide/set-up-direct-data-transfer-for-mobile-apps)
- [客户端直传 OSS 的 STS/签名 URL 方案](https://help.aliyun.com/en/oss/user-guide/uploading-objects-to-oss-directly-from-clients/)

## 8. 安全设计

- Access Token 15 分钟；Refresh Token 30 天并旋转，数据库只存 token hash。
- 短信验证码 5 分钟有效，Redis 存 hash；按手机号/IP/设备限流。
- 所有业务查询先解析 user，再验证 space membership；禁止只按 record ID 查询。
- ECS 只接受 ALB/运维网段流量；RDS/Tair/OSS 内网访问优先。
- ECS 绑定 RAM Role，Go SDK 使用默认凭据链，不在 GitHub、Docker 镜像或环境文件放长期 AccessKey。
- KMS 保存 JWT 私钥、数据库密码、第三方密钥；不同环境分 secret。
- 日志禁止记录手机号全文、验证码、Token、精确位置和记录正文。
- WAF、请求体大小限制、超时、并发限制、SQL 参数化和统一错误输出。
- 数据导出和注销为异步任务；注销冷静期结束后删除/匿名化并清 OSS 对象。

阿里云官方推荐 ECS 应用使用 RAM Role 代替硬编码 AccessKey：

- [保护账号并避免凭据泄露](https://help.aliyun.com/en/ecs/user-guide/protect-alibaba-cloud-accounts-and-prevent-credential-leakage)
- [RAM Role 概览](https://help.aliyun.com/en/ram/user-guide/ram-role-overview)

## 9. 可观测性与备份

- `/health/live` 只检查进程；`/health/ready` 检查 PostgreSQL/Redis 依赖。
- API 指标：请求量、p50/p95/p99、错误码、登录失败、同步积压、Outbox 延迟、OSS 失败率。
- SLS 采集 Docker stdout，日志 JSON 结构化并脱敏。
- CloudMonitor 对 ECS CPU/内存/磁盘、ALB 5xx/健康后端、RDS 连接/空间/慢 SQL、Redis 内存设置告警。
- RDS 自动备份 + 日志备份/PITR；每季度做一次恢复演练，而不是只确认“有备份”。
- OSS 开版本控制或合理生命周期；删除任务保留审计记录。

官方资料：

- [RDS PostgreSQL 自动备份和 PITR](https://help.aliyun.com/en/rds/apsaradb-rds-for-postgresql/back-up-an-apsaradb-rds-for-postgresql-instance)
- [Docker 日志采集到 SLS](https://help.aliyun.com/en/sls/collect-docker-container-text-logs)
- [ECS 监控与日志](https://help.aliyun.com/en/ecs/user-guide/monitoring-and-logging)

## 10. 部署与发布

### 镜像流

```text
GitHub PR
  -> go test / lint / OpenAPI check / migration test / docker build
  -> push image to Alibaba Cloud ACR
  -> staging ECS pull immutable tag (git SHA)
  -> smoke / migration preflight
  -> production approval
  -> ECS-A rolling replace
  -> health check healthy
  -> ECS-B rolling replace
  -> observe 15 minutes
```

- 镜像 Tag 必须包含 Git SHA，禁止只部署 `latest`。
- 数据库 migration 使用独立 Job，向前兼容；先扩展 schema，再发代码，最后清理旧字段。
- 回滚优先回滚镜像；不可逆 migration 必须有恢复方案。
- GitHub 部署凭据优先使用短期/OIDC 或最小权限 RAM 凭据；不得提交密钥。
- 阿里云 ECS 也支持从 Git 仓库拉取并以 Docker 构建部署，但生产推荐 ACR 不可变镜像： [ECS 部署应用](https://help.aliyun.com/en/ecs/user-guide/deploy-applications)。

## 11. 地域、域名与备案

- 若用户主要在中国大陆，选择同一大陆 Region 部署 ECS/RDS/Tair/OSS，减少跨区延迟和流量成本。
- ECS/RDS/Tair 必须在同一 VPC；生产跨两个可用区。
- API 域名解析到中国大陆服务器前完成 ICP/APP 备案；公开上线后按适用规则处理公安备案。
- 若暂时无法备案，可用中国香港 Region 做开发验证，但不能假设它与大陆正式合规和时延等价。

阿里云说明：域名（包括 API 域名）解析到中国大陆服务器并公开提供服务时需要完成 ICP 备案；中国香港或海外节点不要求大陆 ICP 备案： [ICP 备案流程](https://help.aliyun.com/en/icp-filing/basic-icp-service/user-guide/icp-filing-application-overview)。

## 12. 暂不采用

- ACK/Kubernetes：当前团队和流量不需要，先用 ECS + Docker；达到多服务、多团队、频繁扩缩容再评估。
- 微服务/MQ：v1 用模块化单体和数据库 Outbox；需要独立扩容/故障隔离后再拆。
- WebSocket：共享互动先用增量 pull + 推送唤醒；证明实时要求后再加入。
- 自建 PostgreSQL/Redis/MinIO：生产不采用，避免把备份、高可用和安全运维转嫁给小团队。
