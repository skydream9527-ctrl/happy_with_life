# Android 联调

本轮只接 Mock 短信与 Mock OSS。正式阿里云短信/OSS 未配置时不要假装发送成功。

## 起服务端

```bash
cd server
export APP_ENV=dev
export DEV_INMEMORY=true
export SMS_PROVIDER=mock
export SMS_DEV_CODE=123456
export OSS_PROVIDER=mock
go run ./cmd/api
```

当前线上：`http://47.94.102.221:8080/`  
模拟器打本机：`http://10.0.2.2:8080/`（`local.properties` 里写 `xqx.api.base=http://10.0.2.2:8080/`）

## 登录

1. `POST /api/v1/auth/sms/send` `{ "phone":"13800138000", "deviceId":"..." }`
2. `POST /api/v1/auth/sms/verify` 验证码 `123456`
3. 保存 `accessToken` / `refreshToken` / `userId`
4. `GET /api/v1/me` 取 `personalSpaceId`，写入本地默认空间的 `server_id`

## 记一条

本地先写入 Room（`sync_state=SYNC_PENDING`），有 token 后再：

`POST /api/v1/records`  
Header：`Authorization`、`Idempotency-Key`、`X-Device-ID`  
Body：`spaceId` 用云端个人空间 ID。

服务端 `authoritative.gpFinal` 回写本地。

## 照片

`POST /media/sts` → OkHttp PUT `uploadUrl`（带返回的 headers）→ `POST /media/complete` → 创建记录时带 `media:[{mediaId}]`。

## 同步

- 出站：`POST /sync/push` 或单条 `POST /records`
- 入站：`GET /sync/pull?cursor=&limit=`
