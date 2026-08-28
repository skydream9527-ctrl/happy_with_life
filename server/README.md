# 小确幸服务端（xiaoquexing-server）

Go 模块化单体，对应 Android 仓库 [happy_with_life](https://github.com/skydream9527-ctrl/happy_with_life) 的云端权威。

本轮交付：**S0–S2 + 账号资料/心情日历 + 阿里云单机部署包**。安卓拷贝 `clients/android`。共享邀请、OSS 直传和画册仍未做。

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
- `GET /api/v1/spaces`、`GET /api/v1/spaces/{id}`、`GET /api/v1/spaces/{id}/plant`
- `GET/POST /api/v1/records`、`GET/PATCH/DELETE /api/v1/records/{id}`
- `POST /api/v1/sync/push`、`GET /api/v1/sync/pull`

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

## 下一轮 S3

- `POST /api/v1/media/sts` 与 complete verify
- Android 直传私有 OSS，服务端不见长期 AK
