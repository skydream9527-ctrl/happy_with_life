# 小確幸 App：第一輪 Agent 任務單

> **归档说明**：本文件仅用于追溯，不再直接执行。任何新任务必须使用本目录中的当前提示词，并遵守仓库根目录 `AGENTS.md`；禁止越过仓库边界或创建新目录。

> 執行順序：Z code 與 MiniMax Code 可並行開始，但必須使用獨立 branch/PR。
> 共同輸入：`docs/plans/iteration-plan-v1.0.md`。
> 構建約定：只認 GitHub Actions 結果，不要求本地打包。

## 交給 Z code（重要/複雜）

```text
你負責小確幸 App 的第一輪關鍵路徑任務 Z0-01、Z0-02。

目標：
1. 基於現有 PRD、架構文檔和 Kotlin/Room 實作，建立 ADR，凍結 GP、streak、
   補記、每日上限、刪除回退、植物 GP 歸屬、植物解鎖、共享權限與同步衝突規則。
2. 設計 Room v2 schema 與 v1→v2 migration 方案，但這一輪先提交設計與測試計畫，
   不要在產品規則未固定前大面積重寫 UI。

必讀：
- docs/plans/iteration-plan-v1.0.md（尤其第 2.5、3、4、I0 節）
- docs/product/PRD-v0.3.md
- docs/product/architecture-v0.1.md
- app/src/main/java/com/xiaoquexing/app/data/
- RecordViewModel.kt、GPCalculator.kt、StreakCalculator.kt、AchievementTrigger.kt

輸出：
- docs/adr/ADR-001-domain-rules.md
- docs/room-v2-schema.md
- 一份 entity/關係/索引圖或清楚的文字模型
- v1→v2 欄位映射、migration 步驟、回滾/失敗策略、測試矩陣
- 列出需要產品方確認的問題；若無答覆，採用迭代計畫第 3 節的建議值

Room v2 至少考慮：
- localId/serverId、spaceId、authorId
- createdAt/occurredAt/updatedAt/deletedAt
- syncState/version
- Record/Media/Tag/CrossRef 正規化
- Space/Member/Plant/PlantSnapshot/AchievementEvent/Album 的演進位置
- 唯一索引、外鍵、級聯與軟刪除

硬性約束：
- 禁止 fallbackToDestructiveMigration。
- GP 屬於空間，不讓記錄總 GP 與活動植物 GP 成為兩個不可校準的真相來源。
- 發布/編輯/刪除必須能用 transaction 和可重放事件保持一致。
- 時間規則必須覆蓋設備時區、DST、今天/昨天、補記與跨午夜。
- 不接後端、不做共享 UI、不做未要求的視覺重構。
- 不修改 MiniMax 正在處理的 README/PRD/PROGRESS/design token 文件。

驗收：
- ADR 能逐項回答迭代計畫第 3 節 10 個決策。
- schema 能支持 I1–I5 而不需要再次破壞性重構。
- 明確列出現有 v1 資料如何無損搬遷。
- 提交改動摘要、風險、待確認項與下一張建議任務卡。
```

## 交給 MiniMax Code（UI/文檔，邊界清楚）

```text
你負責小確幸 App 的第一輪任務 M0-01、M0-02、M0-03。

目標：
1. 把 README、PRD、PROGRESS 的版本與真實完成度修正一致，不能把 Demo/占位標成完成。
2. 將 design-reference/colors_and_type.css 映射成 Android Compose 設計 token 清單。
3. 建立 GitHub Artifact APK 的人工 smoke checklist。

必讀：
- docs/plans/iteration-plan-v1.0.md（尤其第 2、4.2、I0 節）
- README.md、PROGRESS.md、docs/product/PRD-v0.3.md
- design-reference/colors_and_type.css
- design-reference/pages/*.html
- app/src/main/java/com/xiaoquexing/app/ui/theme/

允許修改：
- README.md
- PRD-v0.3.md（只修版本、狀態與矛盾說明，不擅改核心產品規則）
- README.md
- PROGRESS.md
- docs/design-tokens.md
- docs/smoke-checklist.md

禁止修改：
- AppDatabase、entity、DAO、repository、ViewModel
- GP/streak/achievement 邏輯
- MediaPicker/FileUtil/renderer
- Gradle、Manifest、GitHub workflow
- 任何正式功能代碼

文檔必須誠實標示：
- 已有 GitHub Actions Debug APK 構建，但目前只跑 assembleDebug。
- 畫冊是硬編碼 Demo，不是完整電子畫冊。
- 共享空間只有預留模型。
- 登入、後端、雲同步為 0%。
- 音樂/鏈接/地點為手動輸入，不是平台解析或真實定位。
- 分享保存圖片已接入，但渠道 action、真照片、QR 尚未完成。

smoke checklist 至少覆蓋：
- API 26/29/34
- 首次啟動、空數據、已有數據
- 淺色/深色、字體 1.0x/1.3x
- 文字/照片/錄音記錄、權限允許/拒絕
- 首頁/時間線/畫冊/我的/分享主要導航
- 進程重啟後媒體可讀性與數據一致性

驗收：
- 四份既有文檔不再互相矛盾。
- design token 文件能逐項映射 CSS 與現有 Compose Theme，列出未統一的硬編碼顏色。
- checklist 可由非開發者下載 GitHub APK 後逐項執行。
- 提交改動摘要、未解決矛盾與下一步建議；不要聲稱 GitHub Actions 綠燈，除非有實際 run 連結。
```

## 合併順序

1. MiniMax 的文檔 PR 可先合入。
2. Z code 的 ADR/schema PR 必須在任何 Room v2 實作前合入。
3. 下一輪先派 Z0-03/Z0-04/Z0-05，再派 Z1-01/Z1-02/Z1-03。
4. MiniMax 要等 Z code 的新 model/interface 合入後，才開始 M1 記錄頁改造。
