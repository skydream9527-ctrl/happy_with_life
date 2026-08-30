package com.xiaoquexing.app

import android.app.Application
import com.xiaoquexing.app.data.db.MigrationGuard
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XiaoQueXingApp : Application() {

    // 唯一依赖入口（ADR-002）；禁止再通过向下转型 Application 直接取仓库
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // 必须先于任何 Room 打开：v1 老用户迁移前留完整副本（room-v2-schema §8）
        MigrationGuard.backupV1IfPresent(this, DATABASE_NAME)
        container = AppContainer(this)

        CoroutineScope(Dispatchers.IO).launch {
            // 首启种子（用户/默认空间/植物目录/成就定义/标签注册表）
            val firstInstall = container.bootstrap.ensureSeeded()
            // Demo 记录只存在于 Debug 构建（Z1-07 / ADR D12：正式包首启零 Demo 记录）
            if (firstInstall && BuildConfig.DEBUG) {
                seedDemoRecords()
            }
            // 软删除记录的孤儿媒体清理（Z1-05）
            runCatching { container.mediaImporter.cleanupOrphanFiles() }
            runCatching { container.sessionRepository.restore() }
            runCatching { container.syncEngine.syncAll() }
        }
    }

    private suspend fun seedDemoRecords() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val demoRecords = listOf(
            Record(
                text = "今天阳光很好，和朋友一起在公园散步，看到了很多盛开的花朵，心情特别美丽～",
                moodTag = "开心",
                statusTags = "自然,聚会",
                gpEarned = 25,
                createdAt = now - dayMs * 0
            ),
            Record(
                text = "一个人在家做了一顿美味的意大利面，配着红酒看了部老电影，独处的时光也很治愈。",
                moodTag = "平静",
                statusTags = "美食,居家,电影",
                gpEarned = 30,
                createdAt = now - dayMs * 1
            ),
            Record(
                text = "收到了远方朋友寄来的明信片，上面写着想念我，瞬间泪目。",
                moodTag = "感动",
                statusTags = "想念",
                gpEarned = 15,
                createdAt = now - dayMs * 2
            ),
            Record(
                text = "完成了今天的跑步目标！5公里慢跑，虽然有点累但是特别有成就感，晚上奖励自己一块蛋糕🍰",
                moodTag = "兴奋",
                statusTags = "运动,美食",
                gpEarned = 28,
                createdAt = now - dayMs * 3
            ),
            Record(
                text = "读到一本特别棒的书，《小王子》里说'真正重要的东西，用眼睛是看不见的'，深有感触。",
                moodTag = "平静",
                statusTags = "阅读",
                gpEarned = 18,
                createdAt = now - dayMs * 4
            )
        )
        // 走事务种子：demo 的固定 GP 同样计入空间总分/当日额度/成就，
        // 从源头消除 v1「记录 116 GP 与植物 0 分」的分裂（ADR K3）
        demoRecords.forEach { container.recordRepository.seedRecordWithFixedGp(it) }
        // 冗余校验：首启后空间总分必须等于 116
        val total = container.recordRepository.getTotalGp().first()
        check(total == 116) { "Demo 种子后空间总分应为 116，实际 $total" }
    }

    companion object {
        private const val DATABASE_NAME = "xiaoquexing.db"
    }
}
