# 部署 Runbook

没有阿里云账号时，本仓库不能替你创建 ECS / RDS。单机起步用 `deploy/scripts/first-boot.sh`。

## 前置

1. 完成本仓库 CI 全绿。
2. 单机：ECS + Docker。正式：再看 `docs/alibaba-cloud-resource-checklist.md` 的 RDS / Tair / RAM Role。
3. 密钥只放在机器 `.env` 或 KMS，禁止入库。

## 一台 ECS 首次部署

```bash
./deploy/scripts/first-boot.sh
curl -fsS http://127.0.0.1/health/live
curl -fsS http://127.0.0.1/health/ready
```

安卓把 Base URL 设为 `http://<公网IP>/`。上线前换成 HTTPS 域名。

## 连 RDS / Tair

```bash
export IMAGE_TAG=$(git rev-parse HEAD)
# 其余密钥从环境注入
./deploy/scripts/deploy.sh
```

`deploy.sh` 使用 Git SHA 作为镜像 tag，拒绝 `latest`。

## 数据库

```bash
POSTGRES_DSN=... MIGRATIONS_PATH=file:///app/migrations xqx-migrate up
```

生产禁止 destructive reset。先在 Staging 跑 up/down/up。

## 健康语义

- `/health/live`：进程存活，负载均衡用。
- `/health/ready`：PostgreSQL + Redis 可连；失败时应从 ALB 摘除。

## 回滚

```bash
IMAGE_TAG=<previous-green-sha> ./deploy/scripts/rollback.sh
```

回滚镜像不自动 downgrade schema。需要 schema 回滚时在维护窗口执行 `xqx-migrate down` 并确认 Android 兼容。

## 配置隔离

Dev / Staging / Prod 使用独立 RDS、Tair、OSS、短信签名、JWT 密钥和 `REDIS_KEY_PREFIX`。
