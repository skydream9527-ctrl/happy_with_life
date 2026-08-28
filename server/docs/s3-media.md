# S3 媒体（Beta-1）

- 每用户 **200MB**，单张 **5MB**，一条最多 **9** 张。
- 客户端：`POST /media/sts` → `PUT uploadUrl` → `POST /media/complete` → 发布记录带 `mediaId`。
- `OSS_PROVIDER=mock`：PUT 打到本服务，文件落 `MEDIA_DATA_DIR`（内存模式除外）。
- `OSS_PROVIDER=aliyun`：签发 OSS V1 预签名 URL，complete 时 HEAD 校验。客户端不持有长期 AK。
- 未 complete 的 PHOTO 不能计 GP。已绑定记录的媒体不能直接 DELETE。
