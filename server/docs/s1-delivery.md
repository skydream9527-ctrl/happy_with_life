# S0–S2 交付摘要

## 已完成

- S0 脚手架：health/meta、Gin 中间件、Docker、CI、配置校验
- S1 认证：Mock SMS、Token 旋转/reuse、手机号加密
- S2 记录与同步：
  - 首次登录创建个人空间
  - REST 记录 CRUD 与 `sync/push` `sync/pull` 共用同一套 GP 事务
  - ADR-001 GP 公式、每日 100 上限、streak、植物阶段
  - mutation 幂等与 reuse 检测、墓碑删除、版本冲突
  - 作者才能改删；空间成员才能读

## 未完成

- S3 OSS STS
- S4 共享空间邀请
- S5 成就 HTTP / 画册
- S6 真实阿里云资源

## S3 任务清单

1. `POST /api/v1/media/sts` 与 complete/verify
2. object key 约束与配额
3. 下载短时 URL；Bucket 保持私有
