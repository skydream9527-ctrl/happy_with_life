# 阿里云资源清单（未创建）

本轮 **没有** 创建任何收费资源。下列项目需项目负责人在明确授权后自行开通。Dev / Staging / Prod 必须完全隔离。

## 必须（S0/S1 上线）

| 资源 | 规格建议 | 环境 | 备注 |
|---|---|---|---|
| ECS | 2 vCPU / 4 GB，Alibaba Cloud Linux 3 | Dev 1 台；Prod 至少 2 台跨可用区 | 绑定 RAM Role `xqx-server-role`，禁止长期 AccessKey |
| ACR | 个人/企业版 | 共享仓库、环境用 tag 隔离 | 镜像 tag = Git SHA |
| RDS PostgreSQL | Dev Basic；Prod 高可用跨 AZ | 每环境独立实例 | 仅 VPC 访问，库名 `xiaoquexing` |
| Tair/Redis | Dev 标准版；Prod 高可用 | 每环境独立 | 验证码与限流，非主数据 |
| VPC / 交换机 / 安全组 | 每环境一套 | API 只对 ALB；RDS/Tair 不对公网 | |
| RAM Role | `xqx-server-role` | ECS 默认凭据链 | 权限：STS AssumeRole、OSS、SMS、KMS Decrypt |
| KMS/密钥 | JWT、PHONE_ENCRYPTION_KEY、PHONE_HASH_PEPPER、DB 密码 | 每环境独立 | 不要写入 Git / 镜像 / 普通 .env |

## S3 及之后

| 资源 | 用途 |
|---|---|
| OSS 私有 Bucket | 照片/语音；禁止公共读 |
| RAM Role `xqx-mobile-upload-role` | 短期 STS，前缀限制 |
| 阿里云短信签名 + 模板 | 仅 Prod/Staging 真实发送；Dev 用 mock |
| ALB + 证书 | `api-dev.` / `api-staging.` / `api.` |
| WAF | 生产 |
| SLS + CloudMonitor | 日志与告警 |
| 备案域名 | API 域名 |

## 明确不做

- 不使用阿里云主账号 AccessKey
- 不在 GitHub Secrets 以外的地方保存长期 AK
- 不上 ACK / Kubernetes
- 不自建生产 PostgreSQL / Redis / MinIO
