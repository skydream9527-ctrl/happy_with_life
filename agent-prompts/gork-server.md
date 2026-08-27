# 给 Gork 的服务端首轮开发提示词

以下内容可直接复制给 Gork：

```text
你负责在项目负责人已经准备好的“小确幸”独立服务端仓库中开发，并在阿里云开发环境中完成首轮可运行交付。

项目名称：小确幸服务端
GitHub 仓库名：xiaoquexing-server
Go module：github.com/skydream9527-ctrl/xiaoquexing-server
API 进程名：xqx-api
Worker 进程名：xqx-worker

现有 Android 仓库：
https://github.com/skydream9527-ctrl/happy_with_life

你必须先通过上述 GitHub 地址只读查看 Android 仓库中的：

1. docs/server/README.md
2. docs/server/architecture-alibaba-cloud.md
3. docs/server/development-spec-v1.md
4. docs/server/openapi-sync-contract-v1.md
5. docs/adr/ADR-001-domain-rules.md
6. docs/room-v2-schema.md

本轮只完成 S0“服务端脚手架”和 S1“数据库/认证基座”。不要一次实现共享空间、完整同步、画册或 AI。

工作边界（优先级最高）：

- 只能在项目负责人提供的 xiaoquexing-server 仓库根目录内写文件或执行命令，禁止本地访问或修改 Android 仓库、父目录和相邻目录；上面列出的 Android GitHub 文档是唯一允许的外部只读输入。
- 禁止创建任何新目录。下列目录必须由项目负责人预先创建；若有任一目录不存在，停止开发并只返回缺失目录清单。
- 只能在预建目录内创建任务所需文件；不得移动、重命名或删除范围外文件。
- 构建、测试和镜像验证只通过服务端仓库的 GitHub Actions，不在 Android 工作目录执行任何命令。

一、必须预先存在的项目结构

- .github/workflows/
- cmd/api/
- cmd/worker/
- cmd/migrate/
- internal/auth/
- internal/user/
- internal/space/
- internal/record/
- internal/media/
- internal/growth/
- internal/achievement/
- internal/sync/
- internal/platform/postgres/
- internal/platform/redis/
- internal/platform/aliyun/
- internal/transport/http/
- migrations/
- openapi/
- deploy/docker/
- deploy/compose/
- deploy/scripts/
- docs/
- tests/

技术要求：

- Go 当前稳定且受支持版本，在 go.mod 和 CI 中固定。
- Gin。
- PostgreSQL：pgx/v5；优先 sqlc 生成查询。
- migration：golang-migrate SQL 文件。
- Redis：go-redis/v9。
- OpenAPI 3.1 是接口唯一来源。
- JSON 结构化日志输出 stdout。
- 所有 ID 使用 UUIDv7。
- 时间戳使用 UTC RFC3339；记录另带 occurredDate 与 IANA timezone。

二、完成 S0

实现：

- GET /health/live
- GET /health/ready
- GET /api/v1/meta
- request ID 中间件
- panic recovery
- timeout
- body size limit
- 统一成功/错误 JSON
- typed config + 启动配置校验
- PostgreSQL/Redis 连接和优雅关闭
- 非 root 多阶段 Dockerfile
- docker-compose.dev.yml
- Makefile 或等价任务入口
- .env.example，只包含变量名和安全示例，不能有真实密钥

GitHub Actions 必须运行：

- go test ./...
- go test -race ./...
- golangci-lint
- OpenAPI lint
- migration test
- docker build
- secret scan

三、完成数据库基线

按照 development-spec-v1.md 创建 migration，至少包含：

- users
- auth_identities
- devices
- refresh_tokens
- spaces
- space_members
- space_invites
- records
- record_media
- tags
- record_tags
- daily_space_stats
- plant_definitions
- plant_snapshots
- achievement_definitions
- achievement_progress
- achievement_events
- applied_mutations
- change_log
- albums
- album_pages
- album_shares

要求：

- 外键、唯一约束和核心索引完整。
- migration 支持空库 up/down 测试。
- 不使用 ORM 自动建表。
- PostgreSQL 是唯一云端主数据；Redis 不是主数据。

四、完成认证基座

实现：

- POST /api/v1/auth/sms/send
- POST /api/v1/auth/sms/verify
- POST /api/v1/auth/token/refresh
- POST /api/v1/auth/logout

认证要求：

- Access Token 15 分钟。
- Refresh Token 30 天、rotation、reuse detection。
- 数据库只存 refresh token hash。
- 验证码 5 分钟，只在 Redis 存 hash。
- 按手机号、IP、设备限流。
- Dev 默认使用 Mock SMS provider，并在日志中只输出固定测试提示，不能打印真实验证码。
- 提供 Aliyun SMS provider 接口和配置，但没有阿里云权限时不要伪造已发送成功。
- 手机号加密存储，另存不可逆 hash 做查找。
- 不能泄露账号是否已注册。

五、阿里云开发环境

目标部署：Alibaba Cloud ECS + Docker Compose。

外部服务：

- RDS PostgreSQL
- Tair/Redis
- 私有 OSS Bucket
- 阿里云短信
- SLS/CloudMonitor

安全要求：

- ECS 使用 RAM Role，不在代码、GitHub、镜像或 .env 中保存长期 AccessKey。
- 不使用阿里云主账号 AccessKey。
- RDS/Tair 优先仅 VPC 内访问。
- OSS 必须私有。
- Dev、Staging、Prod 配置完全隔离。
- 不要在没有用户明确授权和凭据时创建收费资源；先输出资源清单和部署步骤。

请生成：

- deploy/compose/docker-compose.ecs.yml
- deploy/scripts/deploy.sh
- deploy/scripts/rollback.sh
- docs/alibaba-cloud-resource-checklist.md
- docs/deployment-runbook.md
- docs/backup-restore-runbook.md

部署脚本必须使用不可变镜像 Tag（Git SHA），禁止只依赖 latest。

六、禁止事项

- 不修改 Android 仓库代码。
- 不拆微服务。
- 不上 Kubernetes/ACK。
- 不自建生产 PostgreSQL、Redis 或 MinIO。
- 不把照片上传到 Go API 再转 OSS。
- 不硬编码 AccessKey、数据库密码、JWT 私钥、短信密钥。
- 不声称阿里云资源已部署，除非提供真实资源 ID、访问地址和健康检查结果。
- 不实现文档范围外的 AI、支付、公开社交广场。

七、验收

- 新仓库 README 能让新开发者启动 Dev 环境。
- GitHub Actions 全绿。
- 空库 migration up/down/up 通过。
- health/live 和 health/ready 语义正确。
- Mock SMS 登录、刷新、退出、Token rotation/reuse 集成测试通过。
- Docker 容器以非 root 运行并支持优雅关闭。
- OpenAPI 与实际路由一致。
- 没有真实 secret 进入 Git 历史。
- 交付改动摘要、架构决策、未完成项、风险和下一轮 S2 任务清单。
```
