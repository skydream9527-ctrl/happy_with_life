# 小确幸 (XiaoQueXing) - Android App

🌱 一款治愈系生活记录App。记录生活中的每个小确幸，看着植物随你的记录慢慢生长。

## 功能特性

- 📝 **多类型记录**：文字、照片、语音、音乐、链接、地点
- 😊 **心情标签**：9种心情 + 12种状态标签
- 🌿 **9种植物**：小确幸之树、樱花树、向日葵、仙人掌、多肉、藤蔓、玫瑰花丛、竹林、蘑菇
- 📈 **GP成长体系**：记录越多，植物长得越高，7个生长阶段
- 🤝 **共享空间**：情侣/家人/朋友一起养一棵植物
- 🏆 **成就系统**：17个成就徽章
- 📖 **电子画册**：自动生成精美回忆画册
- 📤 **分享功能**：生成精美分享卡片，一键分享到社交平台
- 🌱 **植物图鉴**：收集所有植物种类
- 🔋 **离线优先**：所有数据本地存储，无需网络也能用

## 技术栈

- **语言**：Kotlin
- **UI框架**：Jetpack Compose + Material3
- **导航**：Navigation Compose
- **数据库**：Room (SQLite)
- **异步**：Kotlin Coroutines + Flow
- **图片加载**：Coil
- **音视频**：ExoPlayer / Media3
- **相机**：CameraX
- **动画**：Lottie
- **分页**：Accompanist Pager
- **minSdk**：26 (Android 8.0)
- **targetSdk**：34 (Android 14)

## 项目结构

```
app/src/main/java/com/xiaoquexing/app/
├── MainActivity.kt              # 入口Activity
├── XiaoQueXingApp.kt            # Application（初始化Demo数据）
├── navigation/AppNavigation.kt  # 底部Tab导航+路由
├── ui/
│   ├── theme/                   # 主题/颜色/字体
│   ├── components/              # 通用组件（植物/卡片/按钮等）
│   ├── home/                    # 首页
│   ├── record/                  # 记录页
│   ├── timeline/                # 时间线
│   ├── album/                   # 电子画册
│   ├── profile/                 # 我的/植物选择/图鉴/成就
│   └── share/                   # 分享面板
├── data/
│   ├── db/                      # Room数据库+DAO
│   ├── entity/                  # 数据实体（Record/Plant/Achievement等）
│   ├── model/                   # UI模型（MoodTag/GPBreakdown等）
│   └── repository/              # 数据仓库
├── viewmodel/                   # ViewModel层
└── util/                        # 工具类（GP计算/植物Canvas渲染/分享卡片渲染）
```

## 在Android Studio中运行

1. 用Android Studio Hedgehog(2023.1.1)+打开项目根目录 `android/`
2. 等待Gradle同步完成（首次需要下载依赖）
3. 连接Android设备或启动模拟器（API 26+）
4. 点击Run即可

## 构建APK

用户通过GitHub CI/CD构建，可配置GitHub Actions自动打包。

本地构建命令：
```bash
./gradlew assembleDebug   # Debug包
./gradlew assembleRelease # Release包（需配置签名）
```

## 当前版本

**v0.1.0** (MVP Demo)
- 完整的UI框架和核心功能实现
- 9种植物Canvas绘制
- 本地Room数据库
- 预置5条Demo数据
- 分享卡片UI
- 成就墙UI

## 后续规划

- [ ] 接入后端API（Go服务，见架构方案文档）
- [ ] 相机拍照+相册选择完整实现
- [ ] 语音录制+播放完整实现
- [ ] 音乐识别/链接解析（对接网易云/QQ音乐/Spotify）
- [ ] 地点选择（高德/百度地图SDK）
- [ ] 登录注册+账号系统
- [ ] 共享空间实时同步（WebSocket）
- [ ] 植物切换动画（Lottie）
- [ ] 分享图片保存到相册
- [ ] 深色模式细节优化
- [ ] 通知提醒
- [ ] Widget小组件
- [ ] 生物识别解锁

## 文档

- PRD：见上级目录 `PRD-v0.1.md`（实际为v0.3内容）
- 技术架构方案：见上级目录 `架构方案-v0.1.md`
