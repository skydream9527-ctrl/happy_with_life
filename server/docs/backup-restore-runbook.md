# 备份与恢复 Runbook

主数据只在 RDS PostgreSQL。Redis 丢失可以重建（验证码会失效，用户需重新登录）。

## 备份

- 开启 RDS 自动备份与 PITR。
- Dev：每日；Prod：至少每 12 小时全量 + WAL 连续归档。
- 备份保留：Dev 7 天；Prod ≥ 14 天。
- OSS 对象备份不在 S0/S1 范围；S3 起启用跨区域复制或生命周期。

## 恢复演练（Staging 必须先做）

1. 新建临时 RDS 实例。
2. 从指定时间点恢复。
3. 将 Staging API 的 `POSTGRES_DSN` 指向恢复实例。
4. 检查 `/health/ready`、用测试账号走 SMS mock 登录。
5. 确认 `users` / `refresh_tokens` 行数与演练记录一致。

## 账号注销

`DELETE /api/v1/account` 仅将状态置为 `PENDING_DELETE` 并吊销会话。冷静期后的物理删除/匿名化属于 S6，不要在本轮手工 truncate。
