# 安卓接入

把本目录下的 Kotlin 文件拷进 `app/src/main/java/com/xiaoquexing/app/data/remote/`。

## 依赖（app/build.gradle.kts）

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
```

## Base URL

在 `local.properties` 或 `BuildConfig` 写：

```
XQX_API_BASE=https://你的域名/
```

调试期如果还是 `http://公网IP/`，需要在 debug 的 `AndroidManifest` 打开明文流量：

```xml
android:usesCleartextTraffic="true"
```

上线必须 HTTPS。短信登录走 `/api/v1/auth/sms/*`，记心情走 `/api/v1/records`，日历走 `/api/v1/stats/calendar`。

照片：`mediaSts` → OkHttp **PUT** 到返回的 `uploadUrl`（带 `Content-Type`）→ `mediaComplete` → `createRecord(..., media=[MediaRef(id)])`。每用户 200MB，单张 ≤5MB，一条最多 9 张。
