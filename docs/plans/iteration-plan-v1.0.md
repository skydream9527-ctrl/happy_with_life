# 小確幸 App：完整迭代與雙 Agent 開發計畫 v1.0

> 更新日期：2026-08-27
> 適用對象：Z code、MiniMax Code
> 構建約定：**只通過 GitHub Actions 打包與驗證，不要求本地執行 Gradle**。

## 1. 專案判斷與建議路線

「小確幸」是以「輕量記錄 → GP 成長 → 植物變化 → 回顧/畫冊 → 私密共建」為核心閉環的 Android 原生 App。現在已有一個約 7,000 行 Kotlin 的單機 Demo，視覺與主要頁面輪廓已具備，但產品文件把不少 Demo/占位能力誤標為完成。

不建議現在直接接後端或先做共享空間。正確順序是：

1. 先統一 PRD、數值與資料模型，建立 GitHub CI 門禁。
2. 把單人離線版做成資料可靠、可完整使用的 Beta。
3. 完成差異化核心「真實電子畫冊」。
4. 再接帳號、雲同步與共享空間，避免用錯誤模型反覆遷移。
5. 最後做地圖、回顧、上架與商業化。

建議里程碑：

| 里程碑 | 交付結果 | 建議週期 |
|---|---|---:|
| M0 基線可信 | 文件、資料模型、CI、測試基座一致 | 3–5 天 |
| M1 單人離線 Alpha | 記錄 CRUD、媒體、GP、植物、成就真正閉環 | 7–10 天 |
| M2 單人離線 Beta | 時間線、詳情、搜尋篩選、真實分享、設定/備份 | 7–10 天 |
| M3 畫冊 Beta | 真實聚合、翻閱、圖片/PDF 導出 | 10–15 天 |
| M4 雲端 Beta | 登入、媒體上傳、離線同步、衝突處理 | 12–18 天 |
| M5 共享空間 Beta | 邀請、成員、合種、互動、權限 | 12–18 天 |
| M6 回顧增強 | 地圖、心情統計、月/年回顧、提醒 | 8–12 天 |
| M7 上架候選版 | 安全、性能、無障礙、簽名、商店材料 | 7–10 天 |
| M8 增長/商業化 | 訂閱、AI 回顧、Widget、活動等 | 按數據決定 |

以兩個 Agent 並行、Z code 為關鍵路徑估算，到可上架 v1.0 約 12–16 週；M3 完成後即可先做封閉測試。

## 2. 現況盤點：哪些是真的，哪些只是 Demo

### 2.1 已有且可保留

- Android 原生工程：Kotlin、Jetpack Compose、Material 3、Navigation Compose、Room、Coroutines/Flow。
- 首頁、記錄、時間線、畫冊、我的、成就、植物選擇/圖鑑、分享等頁面骨架。
- 9 種植物 × 7 階段的 Canvas 繪製，這是現有最有價值的視覺資產。
- 本地記錄保存、GP 計算、植物 GP 累加、基礎成就進度。
- Photo Picker、系統相機、MediaRecorder 的接入代碼。
- 分享卡 Bitmap 生成與寫入 MediaStore。
- `.github/workflows/build.yml` 已可在 main/PR/手動觸發時構建 Debug APK 並上傳 Artifact。
- `design-reference/pages/` 有 8 個高保真 HTML 頁面，可作為 Compose 視覺基準；`colors_and_type.css` 已有完整設計 token。

### 2.2 部分完成，需要補成閉環

| 模組 | 真實狀態 | 主要缺口 |
|---|---|---|
| 記錄 | 約 60% | 無編輯/刪除/詳情/補記；心情沒有強制；錯誤處理弱 |
| 照片/相機 | 約 55% | Photo Picker URI 長期權限與私有存儲未處理；壓縮、EXIF、孤兒文件清理缺失 |
| 錄音 | 約 45% | 沒有真正播放狀態；60 秒上限未執行；生命週期與損壞文件處理不足 |
| 音樂/鏈接/地點 | 約 20% | 目前只是手輸文字；沒有 URL 解析、歌曲元數據、定位或 POI |
| GP/連續天數 | 約 55% | 實作公式與 PRD 不一致；跨日/DST、交易一致性、補記規則未測 |
| 植物 | 約 70% | 繪製完整；解鎖條件與 PRD 不一致，切換時 GP 語義不清，無階段快照 |
| 成就 | 約 45% | 部分計數會算錯；分享類未觸發；無獎勵入帳、解鎖事件與動畫 |
| 時間線 | 約 40% | 只按日期分組；點擊直接進分享，沒有詳情、搜尋、篩選與分頁 |
| 分享 | 約 45% | 保存圖片可用；分享渠道按鈕無動作，照片與 QR 仍為占位，無 Android Sharesheet |
| 深色模式 | 約 55% | 有 Theme，但大量頁面硬編碼淺色，需逐頁驗證 |

### 2.3 尚未真正實現

- 電子畫冊：列表、頁面內容、日期、歌曲、地點均為硬編碼 Demo；未持久化、未聚合真實記錄、未導出 PDF。
- 共享空間：只有 `Space`/`SpaceDao`，記錄沒有 `spaceId`，沒有成員、邀請、權限或 UI。
- 登入、後端、雲同步：工程內沒有 Retrofit、Hilt、WorkManager、後端服務或同步欄位的實際實現。
- 地圖、心情統計、月度/年度回顧、通知、會員、AI、Widget。
- 自動化測試：目前沒有 `test` 或 `androidTest` 測試檔。

### 2.4 必須先修正的「文件假完成」

- `PRD-v0.3.md` 標題仍寫 v0.1，且把「登入/雲同步/完整畫冊」勾成完成，與代碼不符。
- 舊版 `README.md` 把共享空間列為現有功能，但實際只有預留模型。
- 舊版 `PROGRESS.md` 同時出現「相機/錄音為占位」和「已不再占位」的衝突描述。
- 根 README 還停留在「待討論」，不能作為 Agent 的需求來源。

在 M0 完成前，以本文件的狀態判斷為準。

### 2.5 已定位的 P0/P1 技術風險

| 等級 | 問題 | 影響 | 第一責任任務 |
|---|---|---|---|
| P0 | `GPCalculator` 與 PRD 使用兩套公式，7 天/20 天雙倍、照片與文字加成都不同 | 同一筆記錄在客戶端、文件與未來服務端可能得到不同 GP | Z0-01、Z1-03 |
| P0 | 記錄寫入、植物加 GP、成就更新分三步且沒有 Room transaction | 中途失敗會出現「有記錄但植物沒長」或重試重複加分 | Z1-02 |
| P0 | 正式啟動會插入 5 條 Demo 記錄，但沒有把 116 GP 同步到活動植物 | 首次安裝即出現記錄總分和植物總分不一致 | Z1-07 |
| P0 | `fallbackToDestructiveMigration()` 會在未提供 migration 時清空使用者資料 | 上線後 schema 升級可能造成不可恢復的資料丟失 | Z0-02、Z1-01 |
| P1 | `MediaPicker` 在 Composable 建立時直接調 Activity `registerForActivityResult` | 某些生命週期時機可能註冊失敗或旋轉後丟失 callback | Z1-05、Z1-06 |
| P1 | Photo Picker 返回的 `content://` 直接長期寫入 Room，沒有複製或持久權限策略 | App/設備重啟後歷史圖片可能無法再讀 | Z1-05 |
| P1 | `getRecordDays()` 對毫秒整數做 `substr(createdAt, 1, 8)` | 記錄天數統計不等於真實自然日 | Z2-05 |
| P1 | 「攝影師」成就計算的是含照片記錄數，不是照片張數；植物解鎖全用 GP，與 PRD 條件不同 | 成就/圖鑑錯誤解鎖或永遠無法按產品規則解鎖 | Z1-03 |
| P1 | 時間線卡片點擊直接進分享，沒有記錄詳情；分享渠道按鈕沒有 action | 核心瀏覽與分享流程是假閉環 | M1-02、Z2-03 |
| P1 | 畫冊 ID 不查資料，所有頁面展示固定的 2024 Demo 內容 | 核心差異化功能目前不可用 | I3 全部 |

## 3. 需要產品方確認並凍結的規則

這些決策如果不先固定，Z code 後面會反覆改資料庫與同步協議：

1. **心情是否必選**：建議 v1.0 必選 1 個；「取消選中」不能讓已選心情回到空值。
2. **GP 公式**：建議以 PRD 為基線重新凍結。現有代碼照片 `+8/張`、7 天即達雙倍，PRD 是 `+3/張`、20 天達雙倍，差異很大。
3. **每日上限口徑**：建議按「記錄發生日期 + 空間」計算；補記不占今天額度，而占補記日期額度。
4. **植物 GP 語義**：建議 GP 屬於空間，不屬於植物。換植物只換展示；植物養成歷史用快照保存，避免記錄總 GP、當前植物 GP 分裂。
5. **解鎖條件**：建議支持條件類型，而非全部改成累計 GP；例如連續天數、不同地點數、共享空間事件。
6. **刪除記錄是否回退 GP**：建議回退並重算當日上限、植物階段與成就，且保留可同步的軟刪除事件。
7. **共享記錄權限**：建議只有作者可編輯/刪除；空間管理員只能管理成員，不能改他人內容。
8. **登入方式與服務區域**：中國大陸優先可用手機驗證碼；國際市場需額外決定 Google/Apple/Email。
9. **雲服務供應商**：Go + PostgreSQL 可沿用；OSS、短信、地圖與推送需按主要上架區域選型。
10. **Demo 數據策略**：建議正式包不寫入資料庫；使用 onboarding 示例卡或僅 Debug fixture。

若產品方暫時沒有答覆，按上述「建議」實作。

## 4. Agent 分工原則

### 4.1 Z code 專屬範圍（重要/複雜）

- 架構、依賴注入、Room schema/migration、交易一致性。
- GP、streak、成就規則、時間/時區與可重放事件。
- 媒體持久化、壓縮、錄音播放、資源回收。
- 畫冊資料模型、聚合/排版/快取、Bitmap/PDF 導出與性能。
- Go 後端、API、認證、OSS、WorkManager 同步、衝突解決。
- 共享空間權限、邀請、實時事件、資料安全。
- GitHub CI 質量門禁、release signing、R8、性能與安全。

### 4.2 MiniMax Code 適合範圍（邊界清楚）

- Compose 頁面與小元件、空/載入/錯誤狀態。
- 依照 HTML 設計稿落地 spacing、色彩、字體、暗色模式。
- 表單提示、對話框、篩選 UI、設定/關於/幫助頁。
- 畫冊各頁模板的純 UI，輸入只能是 Z code 定義好的 page model。
- 文案、strings 資源化、contentDescription、簡單 Compose UI 測試。

### 4.3 禁止並行修改的熱點

同一時間只能由 Z code 修改以下檔案；MiniMax 只能通過已定義 interface/model 使用：

- `AppDatabase.kt`、所有 entity/DAO/repository。
- `RecordViewModel.kt`、GP/streak/achievement 規則。
- `MediaPicker.kt`、`FileUtil.kt`、分享/畫冊 renderer。
- Gradle、Manifest、GitHub workflow、Proguard、簽名配置。
- 未來的 API DTO、sync worker、auth/token、後端 migration。

每個迭代先由 Z code 合入模型/interface，再讓 MiniMax 基於該提交做 UI，避免兩個 Agent 同時改一個大檔。

## 5. 詳細迭代計畫

## I0：基線可信與 GitHub 門禁（M0）

目標：先讓「什麼是真的、什麼算完成」變得可驗證。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z0-01 | 建立 ADR：GP、植物歸屬、刪除回退、補記、空間、同步衝突規則 | ADR 能回答第 3 節 10 個問題；後續 PR 引用 ADR |
| Z0-02 | 梳理並設計 Room v2，不急著接後端但預留 `localId/serverId/spaceId/createdAt/updatedAt/deletedAt/syncState/version` | schema JSON 導出；不再使用 `fallbackToDestructiveMigration()`；有 v1→v2 migration 測試 |
| Z0-03 | 引入 Hilt 或明確的可測 DI，移除 ViewModel 對全局 `XiaoQueXingApp` 的硬依賴 | repository 可替換 fake；ViewModel 可做 JVM 測試 |
| Z0-04 | 強化 GitHub Actions | PR 執行 `testDebugUnitTest`、`lintDebug`、`assembleDebug`；上傳 APK、lint、test report；任一步失敗則 PR 紅燈 |
| Z0-05 | 建立測試基座與 fixture | 至少有 Room migration、GP、streak、Repository transaction 四類測試樣例 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M0-01 | 修正文檔版本與完成狀態 | README/PRD/PROGRESS 不再宣稱登入、同步、共享、畫冊已完成 |
| M0-02 | 把 CSS token 映射成 Compose design token 清單 | 顏色、圓角、spacing、字級有單一來源，不再新增魔法數字 |
| M0-03 | 建立人工 smoke checklist | API 26/29/34；淺/深色；小/大字體；首次啟動/有數據兩套場景 |

### GitHub 完成門禁

- CI 三項全部綠燈：unit test、lint、assembleDebug。
- APK Artifact 可下載；不用本地打包。
- PR 模板要求填：功能截圖、測試、資料遷移、風險與回滾方式。

## I1：單人離線核心閉環（M1）

目標：使用者能可靠地建立、查看、編輯、刪除一條小確幸，GP/植物/成就始終一致。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z1-01 | Room v2 migration 與正規化模型：Space、Record、Media、Tag/CrossRef、Plant、Achievement/Event | 所有記錄屬於一個預設個人空間；舊 APK 資料升級不丟失 |
| Z1-02 | 發布交易：記錄、GP、植物、成就事件原子寫入 | 任一子步失敗全部回滾；重試不重複加 GP |
| Z1-03 | GP/streak 規則重寫並凍結 | 與 ADR/PRD 一致；0/1/7/20 天、每日上限、補記、刪除、DST 有參數化測試 |
| Z1-04 | 完整 Record CRUD、詳情、補記（最多 365 天） | 新增/編輯/刪除/補記後首頁、時間線、GP、成就同步更新 |
| Z1-05 | 媒體持久化 | 選中照片複製到 App 管理目錄；處理 EXIF/壓縮；刪記錄後安全清孤兒文件 |
| Z1-06 | 錄音可靠化 | 最長 60 秒自動停止；Media3 真播放/暫停/進度；旋轉、返回、權限拒絕不崩潰 |
| Z1-07 | 初始化一致性 | Demo 只存在於 Debug fixture；正式首次啟動建立個人空間/預設植物但無假記錄 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M1-01 | 記錄表單完善：心情必選、500 字、最多 9 圖/5 標籤、錯誤提示、補記入口 | 錯誤可見且不丟草稿；按鈕 disabled 狀態正確 |
| M1-02 | 記錄詳情頁、編輯/刪除確認 UI | 時間線點擊進詳情，不再直接進分享 |
| M1-03 | 媒體狀態 UI | 拒權、錄音倒計時、播放、失敗重試、照片載入失敗均有狀態 |
| M1-04 | 首次使用/空資料頁 | 不注入假記錄也能清楚引導首次發布 |
| M1-05 | 無障礙第一輪 | 關鍵控件有 contentDescription；字體放大到 1.3x 不截斷核心操作 |

### 驗收場景

- 文字、單圖、9 圖、錄音、混合內容均可保存並重啟後讀取。
- 無心情不能發布；只有照片 + 心情可以發布。
- 同日連發不超 100 GP；補記不錯算今天額度。
- 刪除一條記錄後 GP 與植物階段正確回退。
- Android 8/10/14 的媒體與存儲行為都通過 smoke test。

## I2：回憶、分享與本地 Beta（M2）

目標：完成可日常使用的單人版本，邀請 10–30 位測試者使用。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z2-01 | 時間線查詢：文本、心情、標籤、日期、是否有媒體 | 組合篩選可重現；大數據用 Room/Paging，不在 UI 全量 groupBy |
| Z2-02 | 真實分享卡渲染 | 載入真照片、正確植物階段、長文本排版；無占位 QR；生成失敗可恢復 |
| Z2-03 | Android Sharesheet | 使用 content URI 分享圖片；不硬編微信/微博 intent；未安裝 App 也不報錯 |
| Z2-04 | 本地資料導出/導入 | JSON + 媒體 ZIP；版本化 manifest；導入前預覽與衝突策略；錯誤不破壞原資料 |
| Z2-05 | 本地統計查詢 | 記錄天數、照片張數、不同地點數等口徑正確；修復對毫秒時間做 `substr` 的錯誤統計 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M2-01 | 時間線搜尋/篩選 UI、結果空態與清除條件 | 篩選狀態返回後保留；一鍵清空 |
| M2-02 | 分享樣式選擇、預覽、保存/分享反饋 | 分享按鈕均有真實結果，不保留無動作渠道圖標 |
| M2-03 | 設定、關於、隱私、資料導出入口 | 我的頁三個 TODO 全部有實際去向 |
| M2-04 | 暗色與多尺寸修正 | 所有 8 個現有頁面完成淺/深色截圖對照；無硬編碼淺色造成不可讀 |
| M2-05 | Beta 反饋入口與版本顯示 | 顯示 BuildConfig 版本，不硬編 `v1.0.0` |

### Beta 指標

- 核心崩潰率目標 ≥ 99.5% crash-free sessions。
- 記錄發布本地完成 < 1 秒；首頁可用內容 < 1.5 秒。
- 10 位測試者連用 7 天，至少 8 位能無協助完成「記錄 → 植物變化 → 回看 → 分享」。

## I3：真實電子畫冊（M3，核心差異化）

目標：把目前硬編碼 Demo 替換為真正由記錄生成、可離線翻閱和導出的畫冊。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z3-01 | Album/AlbumPage/PlantSnapshot schema 與 Repository | 畫冊重啟仍存在；可刪除；記錄變更時能判斷過期 |
| Z3-02 | 聚合與選材引擎 | 支持全部/階段/日期範圍；產出封面、成長、心情、標籤、地點、音樂、鏈接、月度、封底 page model |
| Z3-03 | 確定性排版算法 | 同一 `layoutSeed + entryHash` 重生成結果一致；0/1/多圖有合法布局 |
| Z3-04 | 快取與懶生成 | 首 3 頁先生成，其餘後台；內容未變直接讀快取；可取消/重試 |
| Z3-05 | 翻頁與圖片性能 | 相鄰頁預取；100 條記錄內畫冊 < 3 秒可進入翻閱；頁面切換無明顯卡頓 |
| Z3-06 | 圖片/PDF 導出 | 單頁、長圖分段、九宮格、PDF；WorkManager 顯示進度；50 頁不 OOM |
| Z3-07 | 導出安全與測試 | 超大圖降採樣、Bitmap 及時回收；Golden/快照與壓力測試 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M3-01 | 建立畫冊向導：範圍、主題、封面、確認 | 無記錄/範圍無資料/生成失敗有清楚提示 |
| M3-02 | 基於 page model 實現 9 類頁面模板 | Composable 不直接查 DAO；只渲染輸入模型 |
| M3-03 | 翻閱 UI：點擊/滑動、頁碼、目錄、沉浸模式、縮放 | 交互符合 PRD；TalkBack 可讀頁面摘要 |
| M3-04 | 導出選項與進度 UI | 後台/前台切換後仍能看到任務狀態與結果 |
| M3-05 | 兩套視覺主題 | 先交付「嫩綠手帳」「暖色回憶」，其餘主題延後 |

### 範圍控制

- v1 先做平面翻頁 + 陰影，不做高風險真實書頁彎曲。
- 沒有真實經緯度時，地點頁顯示地點列表，不生成假地圖。
- PDF 先保證清晰與穩定，矢量文字/印刷級排版可放 M8。

## I4：帳號、後端與離線同步（M4）

目標：在不破壞離線體驗的前提下，完成多設備資料備份與恢復。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z4-01 | 建立 Go 服務、PostgreSQL migration、OpenAPI | API/DB 版本化；生成 Android DTO；不手寫兩套不一致模型 |
| Z4-02 | 認證與 Token 安全 | 驗證碼限流；access/refresh rotation；refresh token 安全存儲；登出可撤銷 |
| Z4-03 | 媒體直傳 | 客戶端壓縮後簽名上傳；校驗 MIME/大小/hash；重試可續傳或冪等 |
| Z4-04 | Outbox + WorkManager 同步 | 本地先成功；有網自動同步；指數退避；可觀察 pending/failed |
| Z4-05 | 衝突與刪除 | 版本號 + author-only edit；soft delete/tombstone；跨設備不復活已刪記錄 |
| Z4-06 | 服務端 GP/成就校驗 | 客戶端樂觀展示，服務端權威；差異可校準且有審計事件 |
| Z4-07 | 可觀測性與安全 | 結構化日誌、request ID、錯誤追蹤、限流、私密媒體簽名 URL、備份恢復演練 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M4-01 | 登入/驗證碼/協議/登出/註銷 UI | 倒計時、錯碼、限流、網絡錯誤、冷靜期都有狀態 |
| M4-02 | 同步狀態 UI | 本地、同步中、失敗、已同步可辨識；可手動重試但不能重複提交 |
| M4-03 | 首次雲端合併引導 | 本地已有資料時明確選「合併/只保留雲端/取消」，危險操作二次確認 |

## I5：共享空間與合種（M5）

目標：完成情侶/家庭/好友私密空間，不把它做成公開社交廣場。

### Z code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| Z5-01 | Space/Member/Invite/Role/Event 模型與 API | 個人/情侶/家庭/好友人數上限由服務端校驗 |
| Z5-02 | 邀請鏈接/碼、接受、過期、撤銷、退出/移除 | token 一次性或限時；不可猜測；所有權轉移規則明確 |
| Z5-03 | 空間級 GP、植物與貢獻 | 每條記錄屬於空間；共振獎勵冪等；不做競爭排名 |
| Z5-04 | 權限與隱私 | 非成員不可讀；作者才能改刪；退出時個人內容帶走策略有測試 |
| Z5-05 | 互動 | Like/Comment API、冪等、刪除/審計；先輪詢/增量同步，必要時再 WebSocket |
| Z5-06 | 共享畫冊資料源 | 只聚合當前使用者有權查看的記錄；成員退出後權限立即生效 |

### MiniMax Code 任務

| ID | 開發點 | 驗收標準 |
|---|---|---|
| M5-01 | 空間列表/切換器/建立向導 | 首頁、記錄、時間線能清楚顯示當前空間 |
| M5-02 | 邀請、成員、紀念日、退出/移除 UI | 危險操作確認；權限不足不顯示操作入口 |
| M5-03 | 貢獻與共振反饋 | 使用溫和文案，不做名次；共振動畫可關閉 |
| M5-04 | Like/Comment 與共享時間線 UI | 分頁、失敗重試、已刪內容狀態正確 |

## I6：地圖、統計、自動回顧與提醒（M6）

### Z code 任務

- Z6-01：合規定位/POI 搜索、權限分級、模糊位置與地圖聚合。
- Z6-02：心情頻率/趨勢、活動關聯、時區正確的統計查詢。
- Z6-03：月度/年度回顧生成器，保證同一月份可重算且不重複通知。
- Z6-04：WorkManager 本地提醒；若需雲推送再接廠商/FCM 聚合。
- Z6-05：自定義標籤與空間標籤同步、合併/重命名規則。

### MiniMax Code 任務

- M6-01：地圖/列表切換、聚合點、位置隱私提示。
- M6-02：心情統計卡、趨勢圖、無數據與數據不足狀態。
- M6-03：月度/年度回顧故事頁與分享入口。
- M6-04：提醒頻率、安靜時段、紀念日設定頁。

## I7：上架候選版（M7）

### Z code 任務

- Z7-01：GitHub Actions 增加 release workflow；簽名只存在 GitHub Encrypted Secrets，不提交 keystore。
- Z7-02：開啟 R8/minify，補 Proguard，產出 mapping；Debug/Release 使用不同 endpoint 與日誌策略。
- Z7-03：Room/媒體加密策略、備份/恢復、註銷刪除、隱私清單與第三方 SDK 審計。
- Z7-04：Macrobenchmark/Baseline Profile；冷啟動、首頁、列表、畫冊、導出壓測。
- Z7-05：崩潰與 ANR 監控、服務端告警、灰度/回滾、最低支持版本策略。
- Z7-06：API 26/29/34 與主流廠商機型矩陣；離線、弱網、磁盤滿、權限拒絕、進程被殺測試。

### MiniMax Code 任務

- M7-01：正式 App icon、啟動畫面、商店截圖/介紹、版本更新文案。
- M7-02：TalkBack、動態字體、對比度、觸控區、RTL 基礎檢查。
- M7-03：隱私政策、服務協議、權限用途、數據導出/註銷說明頁。
- M7-04：全頁面視覺回歸；按 HTML 設計基準提交前後截圖。

### 上架 DoD

- GitHub release workflow 綠燈並產出已簽名 AAB/APK、mapping、SBOM/依賴清單與測試報告。
- P0/P1 缺陷為 0；P2 有明確延期決策。
- 連續 7 天 Beta crash-free users ≥ 99.5%，無資料丟失案例。
- 隱私、權限、註銷、導出、備份恢復已實機驗收。

## I8：用數據決定的後續迭代（M8）

以下不要提前阻塞 v1.0：

- 音樂平台深度搜索/播放；先保留分享鏈接與元數據。
- AI 月報/年度故事、情緒洞察；必須明示 AI、可關閉、敏感內容不作醫療判斷。
- 會員/訂閱、雲存儲配額、畫冊主題包。
- Widget、鎖屏快捷、Wear OS。
- 季節活動、限定植物/皮膚、實體畫冊印刷。
- iOS 版本；先凍結 OpenAPI 與共享 domain model，再另開工程。

## 6. GitHub Actions 標準

本專案不要求 Agent 在本地安裝 Java/Android SDK。所有「編譯成功」只能由 GitHub Actions 結果聲明。

### PR workflow

1. `./gradlew testDebugUnitTest`
2. `./gradlew lintDebug`
3. `./gradlew assembleDebug`
4. 後續加入 `connectedDebugAndroidTest`（Firebase Test Lab 或自建 emulator job）。
5. 上傳 APK、JUnit XML、HTML test report、lint HTML/XML。
6. 依賴與 Gradle cache 使用官方 action；固定 major version，定期升級。

### Release workflow

1. 只允許 tag 或手動審批觸發。
2. 從 GitHub Secrets 解密簽名，構建 `bundleRelease` 與必要 APK。
3. 執行 unit/lint/UI/smoke，再生成 SHA-256。
4. 上傳 AAB/APK、mapping、native symbols、release notes。
5. 先 internal/closed track，人工確認後再逐步發布。

### 分支規則

- `main` 必須保護，禁止直接 push。
- Z code 與 MiniMax Code 各用獨立 feature branch/PR。
- CI 綠燈 + 至少一次 review 才能合入。
- 有 migration/API/security 的 PR 必須由 Z code 最終 review。

## 7. 每個任務交給 Agent 時的固定格式

不要把整個迭代一次丟給一個 Agent。每次只派一張 0.5–2 天的任務卡：

```text
任務 ID：Z1-03
目標：統一 GP/streak 規則並建立測試。
輸入文件：迭代開發計劃-v1.0.md、ADR-001、PRD 對應章節。
允許修改：列出具體目錄/檔案。
禁止修改：另一 Agent 正在工作的檔案。
必做：實作、測試、migration/兼容處理、文檔更新。
驗收：列出 Given/When/Then 與 GitHub Actions job。
交付：改動摘要、風險、未完成項、PR 連結/commit。
不要做：未在任務中的順手重構與新功能。
```

### 給 Z code 的通用補充

```text
這是關鍵路徑任務。先讀現有實作與資料遷移風險，再寫測試固定行為。
不能用 fallbackToDestructiveMigration，不能以 Demo 數據掩蓋空狀態，
不能只讓畫面可見而忽略進程重啟、重試、冪等、時區與資料一致性。
構建結果以 GitHub Actions 為準，不要求本地打包。
```

### 給 MiniMax Code 的通用補充

```text
只實作已定義 model/interface 之上的 UI，不改 Room schema、DAO、Repository、
GP/成就規則、同步協議或 Gradle。設計以 `design-reference/` 與 Compose token 為準。
必須覆蓋 loading/empty/error/disabled/permission-denied 狀態，並補 contentDescription。
構建結果以 GitHub Actions 為準，不要求本地打包。
```

## 8. 統一 Definition of Done

任何任務只有同時滿足以下條件才可標「完成」：

- 不再使用硬編碼 Demo 數據宣稱功能完成。
- 正常、空、錯誤、權限拒絕、重試、進程重啟至少覆蓋相關場景。
- 核心邏輯有單元/整合測試，核心畫面有最小 UI 測試或 smoke case。
- GitHub Actions unit + lint + assemble 全綠。
- 有資料模型變更時包含 migration 與升級測試，不清庫偷過。
- 有異步/同步操作時考慮冪等、取消、重試、離線與重入。
- 沒有新增明文 secret、私密日誌或過度權限。
- 文檔與完成度同步更新；PR 列出已知限制。
- UI 提供 loading/empty/error，暗色模式可讀，關鍵控件可被 TalkBack 識別。

## 9. 第一批可立即派發的任務順序

按下面順序派發即可，先不要啟動後端與共享空間：

1. **Z code：Z0-01 + Z0-02**——凍結規則與 Room v2 方案。
2. **MiniMax Code：M0-01 + M0-02**——修正文檔，整理設計 token；不得動資料層。
3. **Z code：Z0-03 + Z0-04 + Z0-05**——可測架構、GitHub CI、測試基座。
4. **Z code：Z1-01 + Z1-02 + Z1-03**——資料遷移、交易與核心規則。
5. **MiniMax Code：M1-01 + M1-02 + M1-04**——基於新 model 完成記錄 UI。
6. **Z code：Z1-05 + Z1-06 + Z1-07**——媒體與初始化可靠化。
7. **MiniMax Code：M1-03 + M1-05**——媒體狀態與無障礙。
8. 完成 M1 驗收後，才開始 I2；完成 M2 Beta 反饋後，才正式投入 I3 畫冊。

這個順序把 Z code 放在架構和一致性的關鍵路徑，MiniMax Code 始終接收穩定接口做 UI，能最大程度降低雙 Agent 並行產生的合併衝突和返工。
