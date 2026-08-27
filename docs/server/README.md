# 小确幸服务端文档入口

## 项目命名

| 项目 | 名称 |
|---|---|
| 服务端项目名 | **小确幸服务端** |
| GitHub 仓库名 | **`xiaoquexing-server`** |
| Go module 建议 | `github.com/skydream9527-ctrl/xiaoquexing-server` |
| API 进程名 | `xqx-api` |
| Worker 进程名 | `xqx-worker`（第一阶段可与 API 使用同一镜像） |
| Docker 镜像 | `xiaoquexing/xqx-api` |
| PostgreSQL 数据库 | `xiaoquexing` |
| Redis Key 前缀 | `xqx:` |
| API 域名 | `api.<你的已备案域名>` |

## 文档阅读顺序

1. [前后端分离与阿里云架构](./architecture-alibaba-cloud.md)
2. [服务端开发规格 v1](./development-spec-v1.md)
3. [OpenAPI 与同步协议纲要](./openapi-sync-contract-v1.md)
4. [交给 Gork 的首轮开发提示词](../../agent-prompts/gork-server.md)
5. [Android 已完成工作审计](../reviews/completed-work-audit-2026-08-27.md)

## 范围约束

- Android 与服务端是两个独立 GitHub 仓库、两个独立 CI/CD。
- v1 服务端采用**模块化单体**，不提前拆微服务、不上 Kubernetes。
- 服务端部署在阿里云 ECS；PostgreSQL、Redis、OSS 使用阿里云托管服务。
- Android 继续本地优先，服务端不能成为记录发布的同步阻塞点。
- OpenAPI 3.1 是前后端契约唯一来源；Android DTO 由契约生成或严格对照，不允许各自发明字段。
- 所有时间戳通过 API 使用 UTC RFC3339；“发生自然日”额外传 `occurredDate` 和 `timezone`，避免跨时区误算。
- 服务端 ID 使用 UUIDv7 字符串；Android 保留 Room `localId: Long`，通过 `serverId` 映射。
