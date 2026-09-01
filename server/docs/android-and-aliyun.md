# 安卓客户端 + 阿里云服务器

本环境不能替你开通阿里云账号或短信签名。服务器代码已经按一台 ECS 可部署来准备。

## 服务端在 ECS 上

1. 买一台 2 核 4G 的 ECS（Alibaba Cloud Linux 或 Ubuntu），安全组放行 **80**（以后再上 443）。
2. 安装 Docker 与 Compose 插件。
3. 把 `xiaoquexing-server` 仓库放到机器上。
4. 执行：

```bash
chmod +x deploy/scripts/first-boot.sh
./deploy/scripts/first-boot.sh
```

脚本会生成密钥、建库、跑 migration。当前这台 ECS 直接暴露 `http://47.94.102.221:8080/`（未挂 80 端口 Nginx）。

5. 浏览器或安卓访问 `http://47.94.102.221:8080/health/live` 应返回 `ok`。安全组需放行 **8080**。以后上 Nginx 再开 80/443。
6. 短信签名批下来后，把 `.env` 里 `APP_ENV=prod`、`SMS_PROVIDER=aliyun`，并填写签名和模板，再换成 `docker-compose.ecs.yml` 去连 RDS / Tair。

生产禁止内存库和 mock 短信。手机号只存哈希和密文，接口只回 `138****5678`。

## 安卓怎么连

1. 拷贝 `clients/android/` 三个 Kotlin 文件到 App 工程。
2. Base URL 写成 `http://47.94.102.221:8080/`（当前 App 默认）或以后换成 `https://api.你的域名/`。
3. 流程：

- `smsSend(phone)` → `smsVerify(phone, code, deviceId)` 得到 Token
- `me()` 得到账号、脱敏手机号、个人空间 ID
- 照片：`mediaSts` → PUT `uploadUrl` → `mediaComplete` → `createRecord`（带 `mediaId`）
- `calendar(spaceId)` 拉一个月的心情格子
- Token 过期用 `refresh`；不要把 refresh 写进日志

4. 请求头固定带 `X-Device-ID`、`X-Platform: android`。
