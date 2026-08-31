# 小确幸服务端（xiaoquexing-server）

Go 模块化单体，对应 Android 仓库 [happy_with_life](https://github.com/skydream9527-ctrl/happy_with_life) 的云端权威。

本轮交付：**S0–S4**（登录、账号、心情、照片、**合种邀请链接**）+ 阿里云单机部署包。安卓拷贝 `clients/android`。

| | |
|---|---|
| Module | `github.com/skydream9527-ctrl/xiaoquexing-server` |
| API | `xqx-api` |
| Worker | `xqx-worker`（S0/S1 空转，等待 SIGTERM） |
| Domain rules | `docs/domain-rules.md`（`DOMAIN_RULES_VERSION=1`） |

## 本地开发

需要 Go 1.24.6。完整依赖用 Docker Compose：

```bash
docker compose -f docker-compose.dev.yml up postgres redis -d
export POSTGRES_DSN=postgres://xqx:xqx@127.0.0.1:5432/xiaoquexing?sslmode=disable
go run ./cmd/migrate up
go run ./cmd/api
```

无 Docker 的开发机（仅 Dev）可以：

```bash
export DEV_INMEMORY=true
export APP_ENV=dev
go run ./cmd/api
```

内存模式会在日志中明确警告，禁止用于 staging/prod。

Dev 默认 Mock SMS：向 `/api/v1/auth/sms/send` 发请求后，使用 `.env.example` 里的 `SMS_DEV_CODE`（默认 `123456`）调用 `/api/v1/auth/sms/verify`。日志只打印固定提示，不打印验证码或完整手机号。

## 接口

- `GET /health/live` 进程存活
- `GET /health/ready` PostgreSQL / Redis
- `GET /api/v1/meta`
- `POST /api/v1/auth/sms/send`
- `POST /api/v1/auth/sms/verify`
- `POST /api/v1/auth/token/refresh`
- `POST /api/v1/auth/logout`
- `GET/PATCH /api/v1/me` 账号（手机号脱敏）
- `GET /api/v1/stats/calendar` 每日心情
- `GET/POST /api/v1/spaces`、`PATCH /api/v1/spaces/{id}`
- `GET /api/v1/spaces/{id}/members`、`POST /api/v1/spaces/{id}/invites`
- `GET /api/v1/invites/{token}`、`POST /api/v1/invites/accept`
- `POST /api/v1/spaces/{id}/leave`、`DELETE /api/v1/spaces/{id}/members/{userId}`

合种：邀请只要链接，7 天 / 10 次，空间最多 6 人，个人空间不能邀请，创建者不能退出，只有作者能改自己的记录。不做已读。
- `GET/POST /api/v1/records`、`GET/PATCH/DELETE /api/v1/records/{id}`
- `POST /api/v1/sync/push`、`GET /api/v1/sync/pull`
- `POST /api/v1/media/sts`、`POST /api/v1/media/complete`、`GET /api/v1/media/{id}/download-url`
- `GET /api/v1/media/quota`、`DELETE /api/v1/media/{id}`

照片：每用户 200MB，单张 5MB，一条最多 9 张。`OSS_PROVIDER=mock` 时 PUT 到本服务；`aliyun` 时签发 OSS 预签名 URL。注销冷静期 24 小时。邀请只做链接（S4）。

契约：`openapi/openapi.yaml`。统一响应见开发规格。

## 测试

```bash
make test
make test-race
```

认证集成测试覆盖：Mock SMS 登录、错误验证码、刷新旋转、reuse 检测（吊销 token family）、登出。

## 部署

一台阿里云 ECS 起步：见 `docs/android-and-aliyun.md`，脚本 `deploy/scripts/first-boot.sh`。

连已有 RDS / Tair：`docs/deployment-runbook.md`。阿里云收费资源不会在本仓库里自动创建。

## 下一轮

- 冷静期满后的物理注销 / OSS 对象清理 Worker
- 成就入账与植物快照
- 正式阿里云短信与 OSS Bucket（需要 `SMS_ACCESS_KEY` / `OSS_ACCESS_KEY`，当前保持 Mock SMS 与 `OSS_PROVIDER=mock`）