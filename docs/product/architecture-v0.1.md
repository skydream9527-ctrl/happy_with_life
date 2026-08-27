# 小确幸 App 技术架构方案 v0.1

---

## 一、技术选型

### 1.1 前端策略：Web Demo 先行 + Android 原生

产品采用**两阶段前端策略**：
1. **v0阶段 - Web Demo**：使用 React 技术栈快速开发浏览器预览版，用于验证核心交互、视觉风格和电子画册翻页体验；
2. **正式产品 - Android 原生**：使用 Kotlin + Jetpack Compose 开发最终 Android App，保证流畅的动画性能和原生体验。

后端架构保持不变（Go + PostgreSQL + Redis + OSS）。

### 1.2 Android 原生技术栈（正式APP）

| 层 | 选型 | 理由 |
|----|------|------|
| 语言 | **Kotlin** | Android 官方推荐语言，空安全、协程支持好、语法简洁 |
| UI 框架 | **Jetpack Compose** | 声明式 UI，开发效率高，动画 API 强大（适合植物生长动效和画册翻页动画），与 Kotlin 协程完美配合 |
| 设计系统 | **Material 3 (Material You)** | 现代化设计语言，动态取色支持，组件完善 |
| 状态管理 | **ViewModel + StateFlow + Compose State** | 官方推荐架构，生命周期感知，与 Compose 深度集成 |
| 依赖注入 | **Hilt (Dagger)** | Google 官方 DI 框架，编译时安全，与 Jetpack 组件深度集成 |
| 本地数据库 | **Room** | SQLite 封装，编译时 SQL 校验，支持 Flow 响应式查询，与 Kotlin 协程兼容 |
| 键值存储 | **DataStore (Preferences)** | 替代 SharedPreferences，协程支持，类型安全 |
| 网络请求 | **Retrofit + OkHttp** | Android 生态最成熟的 HTTP 客户端，拦截器、缓存、超时控制完善 |
| 图片加载 | **Coil** | Kotlin 优先的图片加载库，与 Compose 原生集成，内存缓存优秀，支持 GIF/WEBP |
| 序列化 | **Kotlinx Serialization** | Kotlin 原生序列化，与 Retrofit 配合好 |
| 导航 | **Navigation Compose** | 官方导航组件，类型安全参数，深链接支持 |
| 动画 | **Compose Animation API + Lottie** | 常规 UI 动画和植物生长微交互动画用 Compose 内置；复杂矢量动画（成就解锁、浇水特效等）用 Lottie（文件小、性能好、可交互控制） |
| 地图 | **高德地图 SDK** | 国内地图服务有资质，定位精准，POI 搜索完善 |
| 音频 | **ExoPlayer (Media3)** | Google 官方媒体播放器，支持录音播放、后台播放、音频焦点管理 |
| 后台任务 | **WorkManager** | 可靠的延迟任务调度，保证即使 App 退出也能完成数据同步、图片上传等 |
| 图片选择/拍照 | **Activity Result API + 系统相机/相册**（或 Photo Picker） | Android 官方推荐方式，兼容分区存储 |
| PDF 生成 | **PdfDocument (Android原生) + iText/PdfiumAndroid** | 画册导出 PDF 功能，原生 PdfDocument 绘制页面，Pdfium 处理复杂排版 |
| 图片拼接/处理 | **Android Canvas + Compose GraphicsLayer** | 画册长图生成、九宫格切图、照片滤镜效果 |
| 翻页效果 | **Compose 自定义 Gesture + GraphicsLayer** | 通过自定义 Modifier 实现书页弯曲、阴影、拖拽翻页效果，配合 GPU 加速保证 60fps |

### 1.3 Web Demo 技术栈（浏览器预览版）

| 层 | 选型 | 理由 |
|----|------|------|
| 框架 | **React 18** | 生态最成熟，组件化开发效率高，适合快速原型验证 |
| 语言 | **TypeScript** | 类型安全，减少原型阶段的低级错误 |
| 构建工具 | **Vite** | 启动极快，HMR 热更新迅速，适合快速迭代验证 |
| 样式 | **Tailwind CSS** | 原子化 CSS，快速搭建 UI，无需切换上下文写样式，原型阶段效率极高 |
| 动画 | **framer-motion** | React 生态最流行的动画库，声明式 API，支持手势拖拽、页面转场、翻页效果，非常适合验证画册翻页交互 |
| 路由 | **React Router v6** | 标准 React 路由方案 |
| 状态管理 | **Zustand** | 轻量级状态管理，API 简洁，原型阶段快速搭建 |
| 地图 | **高德地图 JS API 2.0** | 与移动端统一使用高德，Web 端快速集成地图标记 |
| 图片处理 | **browser-image-compression + react-image-crop** | 前端图片压缩和裁剪 |
| PDF 预览 | **react-pdf** | Web 端 PDF 预览验证画册导出效果 |
| 图表（心情统计） | **Recharts** | React 图表库，快速实现心情趋势等统计图表 |
| 数据持久化 | **LocalStorage + IndexedDB (Dexie.js)** | 本地存储模拟数据，无需后端即可完整演示原型 |

### 1.4 后端（保持不变）
| 层 | 选型 | 理由 |
|----|------|------|
| 语言/框架 | **Go (Gin/Fiber)** | Go 性能好适合高并发I/O，编译型单文件部署方便 |
| API 风格 | **RESTful + WebSocket** | 常规 CRUD 用 REST；实时通知（合种共振、新成就等）用 WebSocket |
| 数据库 | **PostgreSQL 16** | 关系型主库，存储用户/空间/记录/成就/画册等结构化数据；JSONB 支持灵活标签存储 |
| 缓存 | **Redis 7** | 会话缓存、热点数据（植物状态）、排行榜/计数器、限流 |
| 对象存储 | **阿里云 OSS** | 照片、音频、Lottie动画文件、画册导出文件（PDF/图片）存储 |
| 搜索 | **Meilisearch** 或 PostgreSQL 全文搜索 | 记录全文检索、地点搜索。初期用PG全文搜索即可 |
| 消息队列 | **RabbitMQ** 或 **Redis Stream** | 异步任务：图片处理、推送通知、GP计算、成就检测、画册生成 |
| 图片处理 | **Go 原生 (imaging/bimg)** | 服务端生成多尺寸缩略图、图片压缩、水印处理 |
| PDF 生成（服务端） | **gofpdf / gofpdf2**（可选） | 如需要服务端生成高质量PDF画册，可在服务端渲染（v1.0先做端侧生成） |
| 推送 | **极光推送 / 厂商推送聚合** | Android 推送，接入小米/华为/OPPO/vivo/魅族厂商通道提升到达率 |

### 1.5 基础设施
| 模块 | 选型 | 理由 |
|------|------|------|
| 云服务商 | 阿里云（国内） | 用户初期在国内 |
| CDN | 阿里云 CDN | 照片/动画资源/画册资源加速分发 |
| 容器化 | Docker + 阿里云 SAE | 初期用 PaaS 简化运维 |
| CI/CD | GitHub Actions + Fastlane | Android 自动构建、测试、分发；Web Demo 自动部署 |
| 监控 | Sentry（错误）+ Prometheus+Grafana（指标） | 错误追踪和性能监控 |
| 日志 | ELK / Loki | 集中日志 |

---

## 二、系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│          Android App (Kotlin + Jetpack Compose)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ │
│  │ 记录模块  │ │ 花园视图  │ │ 成就系统  │ │ 回顾模块  │ │电子画册│ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──┬───┘ │
│  ┌────┴────────────┴────────────┴────────────┴───────────┴────┐ │
│  │        ViewModel + StateFlow 状态管理层 (Hilt注入)          │ │
│  └────────────────────┬───────────────────────────────────────┘ │
│  ┌────────────────────┴───────────────────────────────────────┐ │
│  │           本地数据层 (Room + DataStore + 文件系统)           │ │
│  │  · 离线记录缓存  · 植物状态缓存  · 照片本地缓存  · 画册缓存   │ │
│  └────────────────────┬───────────────────────────────────────┘ │
│  ┌────────────────────┴───────────────────────────────────────┐ │
│  │     API Client (Retrofit+OkHttp) + WebSocket (OkHttp)      │ │
│  └────────────────────┬───────────────────────────────────────┘ │
│  ┌────────────────────┴───────────────────────────────────────┐ │
│  │     后台任务 (WorkManager)  · 媒体处理 (Coil/ExoPlayer)     │ │
│  │     · 数据同步  · 图片上传  · 画册生成  · PDF导出            │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS / WSS
┌──────────────────────────┼──────────────────────────────────────┐
│                     API Gateway / LB                            │
│                  (Nginx / 阿里云SLB)                             │
└──────────────────────────┼──────────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────────┐
│                     服务层 (Go + Gin/Fiber)                      │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │用户服务  │ │记录服务  │ │花园服务  │ │成就服务  │ │画册服务  │  │
│  │注册/登录 │ │CRUD/标签│ │植物生长  │ │检测/解锁 │ │模板/生成 │  │
│  │空间管理  │ │GP计算   │ │共享空间  │ │徽章管理  │ │导出/分享 │  │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘  │
│  ┌────┴───────────┴───────────┴───────────┴───────────┴─────┐  │
│  │              通知/推送服务                                  │  │
│  │     (WebSocket推送 + 极光/厂商通道)                         │  │
│  └───────────────────┬───────────────────────────────────────┘  │
│  ┌───────────────────┴───────────────────────────────────────┐  │
│  │              媒体处理服务（异步/Worker）                      │  │
│  │     图片压缩/缩略图/EXIF  · 音频转码  · 画册渲染            │  │
│  └───────────────────────────────────────────────────────────┘  │
└───────────┬──────────────┬──────────────┬───────────────────────┘
            │              │              │
┌───────────┴──┐  ┌────────┴──────┐  ┌───┴──────────┐
│  PostgreSQL  │  │     Redis     │  │  对象存储 OSS │
│  ·用户/空间  │  │ ·会话/缓存    │  │ ·照片原图/压缩│
│  ·记录/标签  │  │ ·GP计数器     │  │ ·音频文件     │
│  ·成就/徽章  │  │ ·在线状态     │  │ ·Lottie动画   │
│  ·植物状态   │  │ ·限流/锁      │  │ ·画册导出文件 │
│  ·画册模板   │  │              │  │ ·用户头像     │
└──────────────┘  └───────────────┘  └──────────────┘
            │              │
┌───────────┴──────────────┴─────────────────────────────────────┐
│                    异步任务队列 (MQ)                             │
│  · 图片处理队列  · GP重算队列  · 成就检测队列                     │
│  · 推送队列      · 回顾生成队列 · 画册生成队列 · 导出队列         │
└────────────────────────────────────────────────────────────────┘
```

---

## 三、电子画册模块技术设计

电子画册是 App 的核心差异化功能，技术上涉及**模板引擎、自动排版算法、翻页交互动画、图片/PDF导出**四个关键子模块。

### 3.1 画册模板引擎

#### 3.1.1 模板定义结构
画册采用**数据驱动的模板引擎**设计，每种页面类型对应一个可配置的排版模板：

```kotlin
// 画册页面模板基类
sealed class AlbumPageTemplate {
    // 封面模板
    data class Cover(
        val plantImageRes: Int,           // 植物当前形态图片/Lottie
        val title: String,                 // "我的小确幸画册"
        val username: String,              // 用户名
        val momentCount: Int,              // 幸福瞬间数
        val stageRange: String?,           // 阶段范围文字
        val themeColor: Color,             // 主题色（从植物阶段获取）
        val decorationStyle: DecorationStyle // 装饰风格（花瓣/叶子/手绘线等）
    ) : AlbumPageTemplate()

    // 时间轴页模板
    data class TimelinePage(
        val stages: List<StageSnapshot>,   // 各阶段数据
        val themeColor: Color
    ) : AlbumPageTemplate()

    // 心情合集页模板
    data class MoodPage(
        val moodGroups: List<MoodGroup>,   // 按心情分组的记录
        val moodStats: Map<String, Float>, // 心情占比统计
        val themeColor: Color
    ) : AlbumPageTemplate()

    // 标签精选页模板
    data class TagPage(
        val tagGroups: List<TagGroup>,     // 按标签分组的精选记录
        val themeColor: Color
    ) : AlbumPageTemplate()

    // 地点地图页模板
    data class MapPage(
        val locations: List<LocationMarker>, // 地图标记点
        val topPlaces: List<PlaceCard>,      // 常去地点卡片
        val locationCount: Int,
        val themeColor: Color
    ) : AlbumPageTemplate()

    // 月度/阶段回顾页模板（拼贴风）
    data class CollagePage(
        val items: List<CollageItem>,       // 拼贴元素（照片/文字/日期章）
        val period: String,                  // 时间段标题
        val layoutSeed: Long,                // 布局随机种子（保证同一批数据生成一致结果）
        val themeColor: Color
    ) : AlbumPageTemplate()
}
```

#### 3.1.2 模板主题系统
- **主题包**：每种主题包含配色方案、字体组合、装饰元素、纸张纹理
- **内置主题**：
  - `fresh_spring`（清新春日-嫩绿系）：默认主题，水彩叶子装饰
  - `warm_autumn`（温暖秋日-橙棕系）：落叶、手写字体
  - `sweet_pink`（甜蜜粉色-樱花系）：花瓣、丝带装饰
  - `ocean_blue`（海洋蓝调-蓝绿系）：贝壳、波浪装饰
  - `mono_ink`（水墨黑白-极简系）：会员专属，水墨风格
- 主题与植物阶段联动：不同生长阶段默认匹配不同主题色
- **Web Demo 阶段**：使用 CSS 变量 + Tailwind 配置定义主题，验证配色和排版效果

### 3.2 自动排版算法

画册的核心难点在于**自动将不规则数量的记录排版为美观的页面**，采用**网格+偏移**拼贴算法：

#### 3.2.1 拼贴布局算法（Collage Layout）
```
输入：某段时间的记录列表 N 条（含照片/文字/标签等信息）
输出：若干个拼贴页面，每个页面包含 m 个排版元素

算法流程：
1. 照片优先级排序：有照片的记录优先（照片是画册主角），按GP值降序、点赞数降序排列
2. 页面填充循环：
   a. 根据剩余记录数量选择页面布局模板（2图/3图/4图/5图/6图/8图布局）
   b. 从候选记录中选取对应数量的记录填充布局槽位
   c. 对每个元素应用随机微偏移（rotation -3°~+3°，offset 0~8dp），模拟手账拼贴感
   d. 随机添加装饰元素（胶带贴纸、手绘分割线、日期印章）
   e. 记录已使用的记录ID，继续填充下一页
3. 布局种子(seed)：同一批记录使用相同的随机种子，保证重复生成结果一致
4. 响应式适配：Compose 中使用 Density 计算像素，保证不同分辨率屏幕下排版比例一致
```

#### 3.2.2 分页策略
- **封面**：始终1页
- **时间轴页**：1-2页（根据阶段数量自适应，超过7个阶段压缩展示）
- **心情页**：2-4页（每种心情1个区块，根据心情种类分页）
- **标签页**：2-4页（每个状态标签1个区块，按记录量排序取Top标签）
- **地点地图页**：1页
- **拼贴回顾页**：根据记录量动态计算，每页4-8条记录，约每20-30条记录1页
- **封底**：1页
- 预估：100条记录约生成 12-18 页画册

#### 3.2.3 Web Demo 验证策略
- 使用 framer-motion 实现相同的翻页交互效果
- 使用 CSS Grid + transform rotate 实现拼贴布局
- 重点验证：翻页手感、排版美观度、不同数据量下的分页效果

### 3.3 翻页交互实现方案

#### 3.3.1 Compose 翻页实现（Android）
使用 Jetpack Compose 的**自定义手势 + GraphicsLayer** 实现书页翻转效果：

```kotlin
// 核心实现思路
@Composable
fun AlbumPageFlip(
    pages: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        // 当前页和下一页堆叠
        // 通过 graphicsLayer 实现3D翻转：
        //   - rotationY: 根据拖拽进度/动画进度计算 Y轴旋转角度 (0° 到 -180°)
        //   - cameraDistance: 设置透视距离，产生近大远小效果
        //   - transformOrigin: 设置旋转轴心为左侧边缘（右翻）/右侧边缘（左翻）
        // 拖拽手势检测：
        //   - detectDragGestures 捕获水平拖拽
        //   - 拖拽距离映射为翻页进度 (0f-1f)
        //   - 松手时根据速度/进度阈值决定是否完成翻页（animateTo 0或1）
        // 翻页阴影/光照：
        //   - 在翻转的页面上叠加渐变阴影，模拟页面弯曲的光影效果
        //   - 下一页随着翻转逐渐显现，配合半透明白色渐变模拟书页背面反光
    }
}
```

关键技术点：
- **3D翻转**：使用 `Modifier.graphicsLayer { rotationY = ...; cameraDistance = ... }` 实现透视翻页
- **书页弯曲效果**：可通过 `Modifier.drawWithContent` 配合 `Canvas` 的 `drawPath` 实现贝塞尔曲线弯曲，或简化为平面翻转+阴影（v1.0先做平面翻转，后续迭代加入弯曲）
- **60fps保证**：所有动画使用 Compose 动画 API，运行在 GPU 合成线程，不阻塞主线程
- **手势冲突处理**：禁用子页面的横向滚动，翻页区域内只响应翻页手势；照片缩放通过双击/捏合单独处理
- **页码指示器**：底部细线进度条 + 页码文字，翻页时平滑过渡

#### 3.3.2 Web Demo 翻页实现
- 使用 **framer-motion** 的 `useMotionValue` + `useTransform` + `AnimatePresence`
- 拖拽用 `drag="x"` + `dragConstraints` + `dragElastic`
- 翻页3D效果用 CSS `transform: rotateY()` + `perspective`
- 翻页动画用 `transition: { type: "spring", stiffness: 300, damping: 30 }` 模拟纸张弹性
- 重点验证：翻页手感和动画流畅度，为原生实现提供交互参考

### 3.4 PDF/图片导出方案

#### 3.4.1 Android 端导出实现
**导出为图片（长图/单页/九宫格）**：
```kotlin
// 核心流程
suspend fun exportAlbumAsImages(album: AlbumData, config: ExportConfig): List<Uri> {
    return when (config.type) {
        SINGLE_PAGE -> {
            // 逐页用 ComposeView 渲染到 OffscreenBuffer
            // 通过 drawToBitmap() 获取 Bitmap
            // 保存到 MediaStore
        }
        LONG_IMAGE -> {
            // 将所有页面竖向拼接为长图
            // 注意：页数过多时长图高度可能超限（如20页×1920px=38400px），
            // 需要分段绘制或提示用户页数过多
            // 使用 Android Canvas 逐页绘制拼接
        }
        GRID_9 -> {
            // 取画册前9页（或精选9页）切为3×3九宫格
            // 使用 Canvas 绘制网格线分割
            // 保存为9张独立图片
        }
    }
}
```

**导出为 PDF**：
```kotlin
suspend fun exportAlbumAsPDF(album: AlbumData, context: Context): Uri {
    // 使用 Android 原生 PdfDocument
    val pdfDocument = PdfDocument()
    val pageWidth = 595  // A4 宽度（points）
    val pageHeight = 842 // A4 高度（points）

    album.pages.forEachIndexed { index, page ->
        val pdfPage = pdfDocument.startPage(
            PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
        )
        val canvas = pdfPage.canvas
        // 将 Compose 页面内容绘制到 PdfDocument 的 Canvas
        // 方法1：使用 ComposeView 在后台线程渲染为 Bitmap，再 drawBitmap 到 PDF Canvas
        // 方法2：直接用 Android Canvas API 手动绘制 PDF 内容（文字、图片、图形）
        // v1.0 采用方法1保证视觉一致性；后续可优化为矢量绘制减小文件大小
        pdfDocument.finishPage(pdfPage)
    }

    // 写入文件
    val file = File(context.cacheDir, "小确幸画册_${System.currentTimeMillis()}.pdf")
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()
    return FileProvider.getUriForFile(...)
}
```

导出质量控制：
- 图片导出分辨率：1080×1920px（单页），质量90% JPEG / 无损 WEBP
- PDF 内嵌图片：高清 1600px 长边，保证打印清晰度
- 文件大小控制：单张图片约300-500KB，整本PDF约5-15MB（100条记录/15页）
- 导出进度：通过 WorkManager 通知栏显示进度，支持后台导出

#### 3.4.2 服务端辅助（可选，v1.2+）
- 高质量PDF生成（服务端使用 headless Chrome 渲染）
- 实体画册印制对接（生成印刷级PDF）
- 云端存储用户生成的画册（仅会员）

### 3.5 画册缓存与性能
- 画册生成结果缓存到 Room 数据库（blob 或文件路径），避免重复生成
- 缓存Key：spaceId + 记录范围(startEntryId~endEntryId) + 记录总数 + 主题
- 新记录发布后缓存失效，下次打开重新生成
- 生成过程在 WorkManager 后台线程执行，不阻塞UI
- 图片资源使用 Coil 加载，内存缓存+磁盘缓存双层缓存
- 翻页时预加载前后各1页内容，保证翻页流畅无白屏

---

## 四、核心数据模型

### 4.1 数据库核心表设计（PostgreSQL）

在原有表基础上，新增画册相关表：

```sql
-- 用户表（保持原有字段，略作扩展）
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone         VARCHAR(20) UNIQUE,
    email         VARCHAR(100) UNIQUE,
    nickname      VARCHAR(50) NOT NULL,
    avatar_url    TEXT,
    bio           TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    is_premium    BOOLEAN DEFAULT FALSE,
    premium_expire_at TIMESTAMPTZ,
    settings      JSONB DEFAULT '{}'
);

-- 空间表（保持不变）
CREATE TABLE spaces (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('personal', 'couple', 'family', 'friend')),
    plant_type      VARCHAR(50) NOT NULL DEFAULT 'lucky_tree',
    plant_stage     INT NOT NULL DEFAULT 0,
    total_gp        INT NOT NULL DEFAULT 0,
    owner_id        UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invite_code     VARCHAR(20) UNIQUE,
    is_active       BOOLEAN DEFAULT TRUE
);

-- 空间成员表（保持不变）
CREATE TABLE space_members (
    space_id    UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    contributed_gp INT NOT NULL DEFAULT 0,
    PRIMARY KEY (space_id, user_id)
);

-- 植物状态快照（保持不变）
CREATE TABLE plant_snapshots (
    id          BIGSERIAL PRIMARY KEY,
    space_id    UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    stage       INT NOT NULL,
    total_gp    INT NOT NULL,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 记录表（保持不变）
CREATE TABLE entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id      UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id),
    content_text  TEXT,
    content_audio JSONB,
    location      GEOGRAPHY(Point, 4326),
    location_name VARCHAR(200),
    location_addr TEXT,
    mood_tag      VARCHAR(30) NOT NULL,
    state_tags    VARCHAR(30)[],
    custom_tags   VARCHAR(50)[],
    occurred_at   TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    gp_earned     INT NOT NULL DEFAULT 0,
    is_backfill   BOOLEAN DEFAULT FALSE,
    weather       VARCHAR(50),
    metadata      JSONB DEFAULT '{}'
);
CREATE INDEX idx_entries_space_time ON entries(space_id, occurred_at DESC);
CREATE INDEX idx_entries_user_time ON entries(user_id, occurred_at DESC);
CREATE INDEX idx_entries_location ON entries USING GIST(location);
CREATE INDEX idx_entries_mood ON entries(mood_tag);
CREATE INDEX idx_entries_tags ON entries USING GIN(state_tags);

-- 照片表（保持不变）
CREATE TABLE photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id    UUID NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    url_origin  TEXT NOT NULL,
    url_thumb   TEXT NOT NULL,
    url_medium  TEXT NOT NULL,
    width       INT,
    height      INT,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 成就定义表（保持不变，新增画册相关成就）
CREATE TABLE achievement_defs (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    icon_url        TEXT,
    category        VARCHAR(30) NOT NULL,
    reward_type     VARCHAR(30),
    reward_value    JSONB,
    condition_type  VARCHAR(50) NOT NULL,
    condition_params JSONB NOT NULL
);

-- 用户成就表（保持不变）
CREATE TABLE user_achievements (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    space_id        UUID REFERENCES spaces(id) ON DELETE CASCADE,
    achievement_id  VARCHAR(50) NOT NULL REFERENCES achievement_defs(id),
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, achievement_id, space_id)
);

-- 连续记录表（保持不变）
CREATE TABLE streaks (
    space_id      UUID PRIMARY KEY REFERENCES spaces(id) ON DELETE CASCADE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_entry_date DATE,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 互动表（保持不变）
CREATE TABLE interactions (
    id          BIGSERIAL PRIMARY KEY,
    entry_id    UUID NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('like', 'comment')),
    content     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 自定义标签表（保持不变）
CREATE TABLE custom_tags (
    id          BIGSERIAL PRIMARY KEY,
    space_id    UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    name        VARCHAR(50) NOT NULL,
    color       VARCHAR(10) DEFAULT '#4CAF50',
    usage_count INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(space_id, name)
);

-- ===== 新增：电子画册相关表 =====

-- 画册表（记录用户生成的画册元数据）
CREATE TABLE albums (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(100) NOT NULL DEFAULT '我的小确幸画册',
    theme           VARCHAR(30) NOT NULL DEFAULT 'fresh_spring',
    stage_start     INT,                    -- 起始生长阶段（null表示从最早）
    stage_end       INT,                    -- 结束生长阶段（null表示到当前）
    date_start      DATE,                   -- 起始日期（null表示不限）
    date_end        DATE,                   -- 结束日期（null表示不限）
    entry_count     INT NOT NULL DEFAULT 0, -- 画册包含的记录数
    page_count      INT NOT NULL DEFAULT 0, -- 画册总页数
    cover_url       TEXT,                   -- 封面图片OSS地址
    pdf_url         TEXT,                   -- PDF文件OSS地址（云端备份时）
    layout_seed     BIGINT NOT NULL,        -- 排版随机种子
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_shared       BOOLEAN DEFAULT FALSE   -- 是否已分享
);
CREATE INDEX idx_albums_space ON albums(space_id, created_at DESC);
CREATE INDEX idx_albums_user ON albums(user_id, created_at DESC);

-- 画册分享表（分享出去的画册链接）
CREATE TABLE album_shares (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id    UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    share_token VARCHAR(50) UNIQUE NOT NULL,
    share_type  VARCHAR(20) NOT NULL DEFAULT 'link', -- link/image/pdf
    expires_at  TIMESTAMPTZ,
    view_count  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 4.2 本地数据库（Room）核心 Entity

从 Isar (Dart) 迁移到 Room (Kotlin)：

```kotlin
// 本地缓存的记录（离线优先）
@Entity(
    tableName = "local_entries",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["space_id", "occurred_at"])
    ]
)
data class LocalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "content_text") val contentText: String?,
    @ColumnInfo(name = "photo_local_paths") val photoLocalPaths: List<String>, // TypeConverter
    @ColumnInfo(name = "photo_server_urls") val photoServerUrls: List<String>, // TypeConverter
    @ColumnInfo(name = "audio_local_path") val audioLocalPath: String?,
    @ColumnInfo(name = "audio_server_url") val audioServerUrl: String?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "mood_tag") val moodTag: String,
    @ColumnInfo(name = "state_tags") val stateTags: List<String>, // TypeConverter
    @ColumnInfo(name = "custom_tags") val customTags: List<String>, // TypeConverter
    @ColumnInfo(name = "occurred_at") val occurredAt: Long, // timestamp millis
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "gp_earned") val gpEarned: Int = 0,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "is_pending_delete") val isPendingDelete: Boolean = false
)

// 本地植物状态缓存
@Entity(
    tableName = "local_plant_states",
    indices = [Index(value = ["space_id"], unique = true)]
)
data class LocalPlantState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "plant_type") val plantType: String,
    @ColumnInfo(name = "stage") val stage: Int,
    @ColumnInfo(name = "total_gp") val totalGp: Int,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long,
    @ColumnInfo(name = "unlocked_achievements") val unlockedAchievements: List<String>
)

// 本地画册缓存
@Entity(tableName = "local_albums")
data class LocalAlbum(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "theme") val theme: String,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "layout_seed") val layoutSeed: Long,
    @ColumnInfo(name = "cover_path") val coverPath: String?, // 本地封面图片路径
    @ColumnInfo(name = "cache_dir") val cacheDir: String, // 画册页面缓存目录
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "entry_hash") val entryHash: String // 记录内容哈希，用于判断缓存是否有效
)
```

---

## 五、关键流程设计

### 5.1 记录发布流程（离线优先）

```
用户点击「发布」
    │
    ├─ 1. 本地保存记录到 Room（isSynced=false）
    │     └─ 立即返回成功，更新UI（植物浇水动画本地先播）
    │
    ├─ 2. WorkManager 启动后台同步任务
    │     ├─ 2a. 上传照片到 OSS（逐张，带进度通知）
    │     ├─ 2b. 上传音频到 OSS（如有）
    │     ├─ 2c. 调服务端 API 创建记录（带所有媒体URL）
    │     ├─ 2d. 服务端计算 GP、更新连续天数、检测成就
    │     └─ 2e. 返回最终GP/成就/植物状态
    │
    ├─ 3. 收到服务端响应
    │     ├─ 更新本地记录 isSynced=true，写入serverId
    │     ├─ 更新本地植物状态缓存（Room）
    │     ├─ 如果有新成就 → 展示解锁动画（Lottie）
    │     └─ 更新植物到最终形态（GP可能因服务端计算有微调）
    │
    └─ 4. 网络异常处理
          ├─ 记录保存在本地，标注 isSynced=false
          ├─ WorkManager 网络恢复时自动重试（退避策略）
          └─ 本地GP用乐观计算先展示，服务端确认后校准
```

### 5.2 GP 计算流程（服务端，保持不变）

```
记录创建请求到达
    │
    ├─ 1. 计算基础GP = 10
    ├─ 2. 计算内容加成（文字/照片/音频/地点/标签）
    ├─ 3. 查询当前streak
    │     ├─ 如果昨天有记录 → streak+1
    │     ├─ 如果今天已有记录 → streak不变
    │     └─ 如果昨天没有记录 → streak重置为1
    ├─ 4. 连续性加成 = min(1 + streak * 0.05, 2.0)
    ├─ 5. 补记×0.8
    ├─ 6. 特殊事件加成
    ├─ 7. 最终GP = min(各项加成后总和, 100)
    ├─ 8. 原子更新：space.total_gp += GP, streaks更新
    ├─ 9. 判断植物阶段变化 → 创建plant_snapshot + 推送通知
    ├─ 10. 成就检测（异步MQ）
    └─ 11. 共享空间特殊处理 → 更新contributed_gp，检测合种共振
```

### 5.3 植物阶段判定（保持不变）

```go
func calcStage(totalGP int) int {
    switch {
    case totalGP < 50:    return 0
    case totalGP < 200:   return 1
    case totalGP < 500:   return 2
    case totalGP < 1500:  return 3
    case totalGP < 4000:  return 4
    case totalGP < 10000: return 5
    default:              return 6
    }
}
```

植物动画用 Lottie 实现，每个阶段一个 Composition，通过 API 下发阶段号 + 状态（正常/需要关爱/节日皮肤），客户端 Lottie 动态属性更新颜色/切换 Composition。

### 5.4 电子画册生成流程

```
用户点击「生成画册」→ 选择范围（全部/阶段/时间）→ 点击生成
    │
    ├─ 1. 查询范围内的记录数据（从本地Room优先，不足则拉取服务端）
    │     ├─ 记录基本信息（文字/标签/日期/GP）
    │     ├─ 照片（优先本地缓存路径，否则Coil加载服务端URL）
    │     ├─ 植物各阶段快照（从plant_snapshots获取）
    │     ├─ 心情统计、标签统计、地点数据
    │
    ├─ 2. 画册数据预处理（协程后台线程 Dispatchers.Default）
    │     ├─ 2a. 计算画册主题色（根据植物当前阶段）
    │     ├─ 2b. 筛选精选记录（按GP+有照片+有文字排序）
    │     ├─ 2c. 分组：心情分组、标签分组、月份分组
    │     ├─ 2d. 计算布局种子（spaceId+时间范围 hash）
    │     └─ 2e. 生成分页计划（每页放哪些记录）
    │
    ├─ 3. 渲染画册页面（Compose off-screen rendering）
    │     ├─ 封面页渲染
    │     ├─ 时间轴页渲染
    │     ├─ 心情合集页渲染（1-2页）
    │     ├─ 标签精选页渲染（1-2页）
    │     ├─ 地点地图页渲染
    │     ├─ 拼贴回顾页循环渲染（直到记录全部展示）
    │     └─ 封底页渲染
    │     注：页面可懒加载，先生成前3页即可进入翻阅模式，
    │         后台继续生成剩余页面
    │
    ├─ 4. 进入翻页浏览模式
    │     └─ 左右滑动/点击翻页，Lottie/Compose动画
    │
    ├─ 5. 用户点击「导出」
    │     ├─ 选择导出类型（单页图/长图/九宫格/PDF）
    │     ├─ WorkManager 后台执行导出（通知栏显示进度）
    │     ├─ 逐页渲染为 Bitmap
    │     ├─ 根据类型拼接/写入PDF
    │     ├─ 保存到 MediaStore / 缓存目录
    │     └─ 弹出分享面板
    │
    └─ 6. 缓存画册到本地（Room + 文件目录）
          └─ 下次进入画册直接从缓存加载，数据未变化时不重新生成
```

---

## 六、API 设计（核心接口）

### 6.1 认证（保持不变）
```
POST   /api/v1/auth/phone/send-code
POST   /api/v1/auth/phone/verify
POST   /api/v1/auth/refresh
DELETE /api/v1/auth/logout
```

### 6.2 空间（保持不变）
```
GET    /api/v1/spaces
POST   /api/v1/spaces
GET    /api/v1/spaces/:id
PATCH  /api/v1/spaces/:id
DELETE /api/v1/spaces/:id
POST   /api/v1/spaces/:id/invite
POST   /api/v1/spaces/join
GET    /api/v1/spaces/:id/members
```

### 6.3 记录（保持不变）
```
POST   /api/v1/entries
GET    /api/v1/entries?space_id=&cursor=&limit=&tag=
GET    /api/v1/entries/:id
PATCH  /api/v1/entries/:id
DELETE /api/v1/entries/:id
POST   /api/v1/entries/:id/like
DELETE /api/v1/entries/:id/like
POST   /api/v1/entries/:id/comments
```

### 6.4 成就（保持不变）
```
GET    /api/v1/achievements
GET    /api/v1/achievements/available
```

### 6.5 回顾（保持不变）
```
GET    /api/v1/review/monthly?space_id=&month=
GET    /api/v1/review/yearly?space_id=&year=
GET    /api/v1/stats/mood?space_id=&from=&to=
GET    /api/v1/entries/map?space_id=&bbox=
```

### 6.6 电子画册（新增）
```
# 获取画册生成所需的聚合数据（端侧生成时使用）
GET    /api/v1/albums/data?space_id=&stage_start=&stage_end=&date_start=&date_end=
       # 返回：范围内记录列表、植物快照、心情统计、标签统计、地点数据

# 保存画册元数据（用户生成后上传记录）
POST   /api/v1/albums
       # Body: {space_id, theme, stage_start, stage_end, date_start, date_end, entry_count, page_count, layout_seed}

# 获取我的画册列表
GET    /api/v1/albums?space_id=&cursor=&limit=

# 获取画册详情
GET    /api/v1/albums/:id

# 删除画册
DELETE /api/v1/albums/:id

# 创建分享链接
POST   /api/v1/albums/:id/share
       # Body: {type: "link"|"image"|"pdf", expires_in_days}

# 访问分享画册（公开接口，无需登录）
GET    /api/v1/albums/shared/:token

# 服务端PDF生成（可选，v1.2+）
POST   /api/v1/albums/:id/export-pdf
       # 异步任务，完成后返回PDF下载URL
```

### 6.7 媒体上传（保持不变）
```
POST   /api/v1/media/sign-upload   # 获取OSS直传签名
```

---

## 七、安全与性能

### 7.1 安全策略（保持原有策略，补充画册相关）
- **认证**：JWT + Refresh Token，Access Token 15分钟，Refresh Token 30天
- **传输**：全链路 HTTPS/TLS 1.3
- **存储**：OSS签名URL（7天有效期），客户端定期刷新
- **隐私**：
  - 共享空间成员身份验证，加入需邀请码+创建者审批
  - 位置信息精确到100米（可选精确）
  - 画册分享链接可设置过期时间，支持撤销分享
- **防刷**：
  - 每日GP上限100，服务端强制校验
  - 接口限流：单用户60次/分钟
  - 图片内容审核（阿里云内容安全）
  - 画册导出频率限制：每本画册每日最多导出10次

### 7.2 性能优化
- **离线优先**：记录先写Room，WorkManager后台同步
- **图片策略**：
  - Android端上传前压缩（长边1600px，质量85%）
  - 服务端生成多尺寸（200px/600px/1600px）
  - Coil内存+磁盘双缓存，画册浏览时预加载相邻页
- **分页**：所有列表接口游标分页，单次20条
- **缓存**：
  - 植物状态Redis缓存；记录Room本地分页缓存
  - 画册生成结果本地缓存，基于entryHash判断是否需要重新生成
  - Lottie动画资源CDN+本地缓存
- **画册生成优化**：
  - 端侧生成，不占用服务端资源
  - 懒加载：先生成前3页进入浏览，后台渲染剩余页面
  - 后台线程（Dispatchers.Default）执行排版计算，不阻塞UI
  - Bitmap 复用 + inBitmap 减少GC
- **GP计算**：异步不阻塞主流程，成就推送通知

### 7.3 数据备份与导出
- 用户数据每日自动备份
- 用户可一键导出：JSON元数据+照片+画册PDF打包zip
- 账号注销30天冷静期后物理删除

---

## 八、动画资源设计

### 8.1 Lottie 动画资源
从 Rive 改为 Lottie（Android 生态更成熟，Compose 支持更好）：

每棵植物一个 `.lottie/json` 文件，包含：
- **植物形态**：各阶段视觉（种子/发芽/幼苗/成长/茂盛/大树/神木）
- **状态切换动画**：浇水触发生长动画、阶段过渡动画
- **微交互**：微风摇曳、呼吸感、花瓣飘落
- **成就动画**：成就解锁庆祝特效（粒子+卡片翻转）
- **浇水特效**：水滴/阳光粒子特效

### 8.2 动画资源清单（v1.0）
| 资源 | 用途 | 文件大小预估 |
|------|------|------------|
| lucky_tree.json | 默认小确幸之树（含7个阶段状态） | ~300KB |
| achievement_unlock.json | 成就解锁通用动画 | ~150KB |
| watering_effect.json | 浇水特效 | ~80KB |
| page_flip.json | 画册翻页辅助元素（书页粒子等） | ~50KB |
| 合计 | | ~580KB |

> Web Demo 阶段：使用 CSS 动画 + framer-motion + Lottie-Web 实现等效动画效果验证。

---

## 九、开发排期建议

### 阶段零：Web Demo 快速验证（2-3周）
- React + TypeScript + Vite + Tailwind 项目搭建
- 核心交互原型实现：植物视图、记录流程、时间线回顾
- **电子画册翻页交互原型**：framer-motion 实现翻页动画、拼贴排版、主题切换
- 视觉风格确认（植物形象、配色、画册排版风格）
- 内部测试 + 小范围用户体验测试（5-10人）
- 输出：交互定稿文档 + 视觉规范文档 + 画册交互验收报告
- **里程碑：Web Demo 评审通过，进入 Android 原生开发**

### 阶段一：Android 基础框架搭建（2周）
- Android 项目搭建（Kotlin + Jetpack Compose + Material3）
- Hilt 依赖注入配置、Navigation 导航、主题系统
- Room 数据库设计 + DataStore 配置
- Retrofit + OkHttp 网络层封装、Repository 层搭建
- 后端项目搭建（Go + Gin）、数据库设计、用户认证
- OSS 媒体直传打通
- CI/CD 流水线搭建

### 阶段二：核心记录功能（2周）
- 记录创建/编辑/删除（文字+照片，Compose 表单）
- 图片选择+拍照+压缩（Coil加载+本地缓存）
- 心情/状态标签选择器（流式布局+动画选中）
- Room 本地存储 + WorkManager 离线同步机制
- 服务端记录 API + GP 计算逻辑
- 地理位置获取（高德定位）

### 阶段三：植物生长系统（2周）
- Lottie 动画集成（植物各阶段动画播放+状态切换）
- 首页植物状态展示（Compose 绘制）
- 浇水/生长动画效果（粒子特效）
- 连续天数 streak 逻辑
- 植物阶段状态管理（StateFlow 响应式更新）

### 阶段四：电子画册模块（3周）
- 画册模板引擎实现（封面/时间轴/心情/标签/地图/拼贴页）
- 自动排版算法（拼贴布局+随机种子+装饰元素）
- Compose 自定义翻页交互（手势+3D翻转动画）
- 画册浏览界面（页码/目录/全屏沉浸）
- 图片导出（单页/长图/九宫格，WorkManager 后台）
- PDF 导出（PdfDocument 绘制）
- 系统分享集成

### 阶段五：成就+基础回顾（2周）
- 成就定义+解锁逻辑+Lottie庆祝动画
- 时间线视图（Compose LazyColumn 瀑布流）
- 基础数据统计（心情饼图用 Canvas 绘制）
- 徽章墙展示

### 阶段六：打磨+测试+上线（2周）
- UI细节打磨、主题完善、暗色模式适配
- 性能优化（启动速度、列表流畅度、画册翻页帧率）
- 全链路测试（单元测试+UI测试+弱网测试）
- 应用商店上架准备（图标/截图/描述/隐私政策）
- 内测+Bug修复

**总计：约13-15周（含Web Demo 2-3周 + Android开发 11-12周）到 v1.0 上线**

---

## 十、技术风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| Compose 翻页动画性能问题 | 画册翻页卡顿影响核心体验 | 早期做翻页动画原型验证；先用Web Demo验证交互方案；Android端v1.0先实现平面翻页+阴影，复杂弯曲效果后续迭代；使用 GPU 渲染层（graphicsLayer）保证60fps |
| Web Demo 与原生体验差异大 | 验证结果无法直接指导原生开发 | Web Demo 的交互规范、动画参数、布局比例严格标注，作为Android开发参考；核心动画曲线、时长参数保持一致 |
| Room 离线同步冲突 | 多设备/离线编辑数据不一致 | "最后写入胜出"+版本号机制；记录只作者本人可编辑，避免多人编辑冲突 |
| PDF/图片导出内存溢出 | 大画册导出时OOM | 分页渲染Bitmap，不一次性加载所有页面到内存；使用 inBitmap 复用；导出过程在 WorkManager 中执行并设置内存警告；限制单次导出最大页数（50页），超出分页导出 |
| OSS成本 | 照片/PDF存储费用增长 | 图片压缩+多尺寸+CDN缓存；免费用户1GB配额；PDF云端备份仅会员可用，免费用户仅本地存储 |
| Android 厂商推送限制 | 推送到达率低 | 接入厂商推送通道（小米/华为/OPPO/vivo/魅族）；极光推送聚合 |
| 地图合规 | 国内地图需资质 | 使用高德地图SDK（有资质），隐私合规弹窗 |
| Lottie 动画包体积 | 植物动画文件较大增加APK体积 | Lottie文件压缩；动画资源放在CDN按需下载（非首次启动必须）；使用dotLottie压缩格式；基础植物包内置，其他植物种类按需下载 |
