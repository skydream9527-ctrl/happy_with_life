# OpenAPI 与离线同步契约纲要 v1

> 本文件冻结语义；Gork 应将其转成 `openapi/openapi.yaml`。字段名使用 lowerCamelCase，数据库列使用 snake_case。

## 1. Headers

- `Authorization: Bearer <accessToken>`
- `X-Request-ID`：客户端可传，服务端保证响应返回。
- `X-Device-ID`：安装级稳定随机 ID，不使用硬件 ID。
- `Idempotency-Key`：在线写 API 必填；sync mutation 使用 body `mutationId`。
- `X-App-Version`、`X-Platform: android`

## 2. 时间和 ID

- server ID：UUIDv7 字符串。
- 时间戳：UTC RFC3339，例如 `2026-08-27T10:20:30.123Z`。
- 发生日期：`YYYY-MM-DD`。
- 时区：IANA，例如 `Asia/Shanghai`。
- 客户端本地 ID：`clientLocalId`，只用于回传映射，不参与权限和全局查找。

## 3. Push 请求

```json
{
  "batchId": "019...",
  "mutations": [
    {
      "mutationId": "019...",
      "dependsOnMutationId": null,
      "entityType": "RECORD",
      "operation": "UPSERT",
      "clientLocalId": 123,
      "serverId": null,
      "baseVersion": 0,
      "occurredAt": "2026-08-27T10:00:00Z",
      "occurredDate": "2026-08-27",
      "timezone": "Asia/Shanghai",
      "payload": {
        "spaceId": "019...",
        "contentText": "今天阳光很好",
        "moodTag": "开心",
        "statusTags": ["自然"],
        "media": []
      }
    }
  ]
}
```

规则：

- 单批最多 100 条和 1 MB，不含二进制。
- mutationId 永不复用。
- 新实体 `serverId=null/baseVersion=0`。
- 更新必须传 serverId/baseVersion。
- DELETE payload 可空，但必须带 serverId/baseVersion。

## 4. Push 响应

```json
{
  "data": {
    "results": [
      {
        "mutationId": "019...",
        "status": "APPLIED",
        "clientLocalId": 123,
        "serverId": "019...",
        "version": 1,
        "authoritative": {
          "gpFinal": 25,
          "spaceTotalGp": 116,
          "plantStage": 1,
          "unlockedAchievements": []
        },
        "error": null
      }
    ]
  },
  "meta": {"requestId": "...", "serverTime": "..."}
}
```

status：`APPLIED/DUPLICATE/CONFLICT/REJECTED/RETRYABLE`。

## 5. Pull 响应

```json
{
  "data": {
    "changes": [
      {
        "sequence": "opaque",
        "entityType": "RECORD",
        "operation": "UPSERT",
        "serverId": "019...",
        "version": 3,
        "spaceId": "019...",
        "payload": {}
      }
    ],
    "nextCursor": "opaque-signed-cursor",
    "hasMore": false
  },
  "meta": {"requestId": "...", "serverTime": "..."}
}
```

change operation：`UPSERT/DELETE/ACCESS_REVOKED`。

## 6. 冲突规则

- `baseVersion == server.version`：允许更新，version +1。
- baseVersion 过旧：返回 `CONFLICT` + 当前 server entity。
- server 已删除：返回 `RECORD_DELETED`，墓碑胜出。
- 非作者修改：返回 `SPACE_FORBIDDEN`。
- 服务端不自动合并正文/标签；Android 显示冲突并允许“保留云端/复制成新记录”。

## 7. OSS STS 响应

`POST /api/v1/media/sts`

请求包含 recordId、mediaId、mimeType、sizeBytes、sha256。响应：

```json
{
  "data": {
    "accessKeyId": "temporary",
    "accessKeySecret": "temporary",
    "securityToken": "temporary",
    "expiration": "2026-08-27T10:30:00Z",
    "region": "oss-cn-...",
    "bucket": "private-bucket",
    "endpoint": "https://...",
    "objectKey": "prod/users/.../sha.jpg"
  }
}
```

该响应只能短时存在内存，禁止日志和落盘。服务端 ECS 自身使用 RAM Role，不配置长期 AccessKey。

## 8. Android Outbox 映射

| Room 字段 | API 字段 |
|---|---|
| outbox.eventId | mutationId |
| entityType | entityType |
| entityLocalId | clientLocalId |
| operation | operation |
| record.serverId | serverId |
| record.version | baseVersion |
| record.occurredAt | occurredAt |
| record.occurredDateKey | occurredDate（由 LocalDate 生成） |
| record.syncState | 客户端状态，不上传 |

Android 与 Gork 完成首版 OpenAPI 后，Z code 必须逐字段复核该映射，再开始 Retrofit/WorkManager 实现。
