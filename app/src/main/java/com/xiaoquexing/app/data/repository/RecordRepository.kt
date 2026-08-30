package com.xiaoquexing.app.data.repository

import androidx.room.withTransaction
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.MediaStatus
import com.xiaoquexing.app.data.db.entity.MediaTypes
import com.xiaoquexing.app.data.db.entity.OutboxEventEntity
import com.xiaoquexing.app.data.db.entity.OutboxOps
import com.xiaoquexing.app.data.db.entity.PlantEventTypes
import com.xiaoquexing.app.data.db.entity.PlantSnapshotEntity
import com.xiaoquexing.app.data.db.entity.RecordEntity
import com.xiaoquexing.app.data.db.entity.RecordMediaEntity
import com.xiaoquexing.app.data.db.entity.RecordTagCrossRef
import com.xiaoquexing.app.data.db.entity.SyncStates
import com.xiaoquexing.app.data.db.entity.TagKinds
import com.xiaoquexing.app.data.db.entity.TagScopes
import com.xiaoquexing.app.data.db.relation.RecordWithDetails
import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.model.GPBreakdown
import com.xiaoquexing.app.util.DateKeys
import com.xiaoquexing.app.util.GPCalculator
import com.xiaoquexing.app.util.StreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate

/**
 * 记录仓库 v2：发布 / 软删除走单一 Room 事务（ADR-001 D5/D6、room-v2-schema §6）。
 *
 * 读 API 返回 v1 形状的领域 Record（媒体/标签重新拼回扁平字符串），
 * 让既有 UI 与 ViewModel 零改动；occurredAt 语义在实体层已就位，Z1-04 再放开编辑。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordRepository(private val db: AppDatabase) {

    private val recordDao = db.recordDao()
    private val spaceDao = db.spaceDao()
    private val userDao = db.userDao()
    private val tagDao = db.tagDao()
    private val mediaDao = db.mediaDao()
    private val dailyStatDao = db.dailyStatDao()
    private val outboxDao = db.outboxDao()
    private val plantDao = db.plantDao()
    private val evaluator = com.xiaoquexing.app.data.AchievementEvaluator(db)

    data class PublishResult(
        val recordId: Long,
        val earnedGp: Int,
        val streakDays: Int,
        val isCapped: Boolean
    )

    // ---- 读（UI 兼容） ----

    fun getAllRecords(): Flow<List<Record>> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            if (space == null) flowOf(emptyList())
            else recordDao.observeRecordDetailsInSpace(space.localId).map { list -> list.map(::toDomain) }
        }

    fun getRecentRecords(limit: Int = 5): Flow<List<Record>> =
        getAllRecords().map { it.take(limit) }

    fun getLatestRecord(): Flow<Record?> =
        getAllRecords().map { it.firstOrNull() }

    suspend fun getRecordById(id: Long): Record? =
        recordDao.getRecordDetailById(id)?.let(::toDomain)

    fun getTotalCount(): Flow<Int> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            if (space == null) flowOf(0) else recordDao.observeTotalCount(space.localId)
        }

    /** 当前默认空间总 GP（ADR D7） */
    fun getTotalGp(): Flow<Int> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            if (space == null) flowOf(0) else recordDao.observeTotalGp(space.localId)
        }

    /** 今日已得 GP：按「发生日期」口径（ADR D3），不再用 createdAt */
    fun getTodayGp(): Flow<Int> =
        spaceDao.observeDefaultSpace().flatMapLatest { space ->
            val key = LocalDate.now().toEpochDay().toInt()
            if (space == null) flowOf(0) else recordDao.observeGpOnDate(space.localId, key)
        }

    suspend fun calculateStreakDays(): Int {
        val space = spaceDao.getDefaultSpace() ?: return 0
        return StreakCalculator.calculate(recordDao, space.localId)
    }

    // ---- 写（事务用例） ----

    /**
     * 发布一条记录：记录 + 媒体 + 标签 + 当日额度 + 空间 GP + 植物阶段快照 +
     * 成就/植物解锁 + Outbox，全部在同一事务内（Z1-02）。任一步失败整体回滚。
     */
    suspend fun publish(draft: Record): PublishResult = db.withTransaction {
        val mood = requireNotNull(draft.moodTag) { "心情必选（ADR-001 原则 1）" }
        val now = System.currentTimeMillis()
        val space = spaceDao.getDefaultSpace() ?: error("默认空间未初始化，请先完成 DataBootstrap")
        val user = userDao.getFirstUser() ?: error("本地用户未初始化，请先完成 DataBootstrap")
        val scopeKey = "u:${user.localId}"

        val occurredAt = if (draft.createdAt > 0) draft.createdAt else now
        requireWithinBackdateWindow(occurredAt, now)
        val occurredDateKey = DateKeys.epochDay(occurredAt)
        val isBackdated = occurredDateKey != DateKeys.epochDay(now)
        // 阶段快照的 before 值取记录集合之和（唯一真相来源），不信任缓存列
        val prevTotal = recordDao.sumAllGp(space.localId)

        val entity = RecordEntity(
            spaceId = space.localId,
            authorId = user.localId,
            contentText = draft.text.ifBlank { null },
            moodTag = mood,
            occurredAt = occurredAt,
            occurredDateKey = occurredDateKey,
            isBackdated = isBackdated,
            gpFinal = 0,
            createdAt = now,
            updatedAt = now,
            syncState = SyncStates.SYNC_PENDING
        )
        val recordId = recordDao.insertRecord(entity)
        writeChildren(draft, recordId, now)

        // 连续系数 N = 含发生日在内的连续天数（D2.5）；额度 = 该发生日剩余（D3/D4）
        val keys = recordDao.distinctDateKeys(space.localId).toHashSet()
        val streakN = StreakCalculator.streakEndingAt(keys, occurredDateKey)
        val remaining = (GPCalculator.DAILY_GP_LIMIT -
            recordDao.sumGpOnDateExcluding(space.localId, occurredDateKey, recordId)).coerceAtLeast(0)

        val breakdown = GPCalculator.calculate(
            textLength = draft.text.length,
            photoCount = draft.getPhotoUriList().size,
            hasVoice = draft.voiceUri != null,
            hasMusic = draft.musicTitle != null,
            hasLink = draft.linkUrl != null,
            hasLocation = draft.locationName != null,
            hasMood = true,
            statusTagCount = draft.getStatusTagList().size,
            streakDays = streakN,
            isBackdated = isBackdated,
            remainingQuota = remaining
        )
        recordDao.updateGp(recordId, breakdown.finalGp, breakdown.isCapped, breakdownToJson(breakdown), now)

        recomputeDailyStat(space.localId, occurredDateKey)
        recomputeSpaceTotal(space.localId, now)
        recordStageChangeIfAny(space.localId, prevTotal, spaceDao.getTotalGp(space.localId), now, occurredDateKey)

        evaluator.evaluate(space.localId, scopeKey)
        appendOutbox(recordId, OutboxOps.UPSERT, now)

        val displayStreak = StreakCalculator.calculate(recordDao, space.localId)
        PublishResult(recordId, breakdown.finalGp, displayStreak, breakdown.isCapped)
    }

    /**
     * Demo/测试种子：跳过公式、按给定 GP 入账（ADR D12——正式包首启不插入 Demo 记录，
     * 该方法仅供 Debug fixture 与测试使用，Z1-07 后将加 BuildConfig.DEBUG 守卫）。
     */
    suspend fun seedRecordWithFixedGp(draft: Record): Long = db.withTransaction {
        val mood = requireNotNull(draft.moodTag) { "种子记录也必须有心情" }
        val now = System.currentTimeMillis()
        val space = spaceDao.getDefaultSpace() ?: error("默认空间未初始化")
        val user = userDao.getFirstUser() ?: error("本地用户未初始化")
        val scopeKey = "u:${user.localId}"
        val occurredAt = if (draft.createdAt > 0) draft.createdAt else now
        val dateKey = DateKeys.epochDay(occurredAt)
        val prevTotal = recordDao.sumAllGp(space.localId)

        val recordId = recordDao.insertRecord(
            RecordEntity(
                spaceId = space.localId,
                authorId = user.localId,
                contentText = draft.text.ifBlank { null },
                moodTag = mood,
                occurredAt = occurredAt,
                occurredDateKey = dateKey,
                gpFinal = draft.gpEarned,
                createdAt = occurredAt,
                updatedAt = now
            )
        )
        writeChildren(draft, recordId, now)
        recomputeDailyStat(space.localId, dateKey)
        recomputeSpaceTotal(space.localId, now)
        recordStageChangeIfAny(space.localId, prevTotal, spaceDao.getTotalGp(space.localId), now, dateKey)
        evaluator.evaluate(space.localId, scopeKey)
        recordId
    }

    /**
     * 编辑记录（ADR D5）：内容或发生时间变更后重算该条 GP（按目标发生日剩余额度，
     * 排除自身），日期变更时新旧两日额度都重算；级联重算空间 GP / 植物阶段 / 成就。
     * 同一事务内完成，任一步失败整体回滚。
     */
    suspend fun editRecord(recordId: Long, draft: Record): PublishResult = db.withTransaction {
        val raw = recordDao.getRawById(recordId) ?: error("记录 $recordId 不存在")
        require(raw.deletedAt == null) { "已删除的记录不能编辑（ADR D6.7）" }
        val mood = requireNotNull(draft.moodTag) { "心情必选（ADR-001 原则 1）" }
        val now = System.currentTimeMillis()
        val space = spaceDao.getDefaultSpace() ?: error("默认空间未初始化")
        val user = userDao.getFirstUser() ?: error("本地用户未初始化")
        val scopeKey = "u:${user.localId}"

        val occurredAt = if (draft.createdAt > 0) draft.createdAt else now
        requireWithinBackdateWindow(occurredAt, now)
        val newDateKey = DateKeys.epochDay(occurredAt)
        // D4.5：补记判定相对「创建当日」而非编辑当日
        val isBackdated = newDateKey != DateKeys.epochDay(raw.createdAt)
        val oldDateKey = raw.occurredDateKey
        val prevTotal = recordDao.sumAllGp(space.localId)

        // 1) 更新记录本体（版本 +1、待同步）
        recordDao.updateEditable(
            recordId = recordId,
            text = draft.text.ifBlank { null },
            mood = mood,
            occurredAt = occurredAt,
            dateKey = newDateKey,
            isBackdated = isBackdated,
            syncState = SyncStates.SYNC_PENDING,
            now = now
        )
        // 2) 重写子行（v1 离线物理删除；M4 接同步后改软删保留墓碑）
        mediaDao.deleteForRecord(recordId)
        recordDao.clearCrossRefs(recordId)
        writeChildren(draft, recordId, now)

        // 3) 重算 GP：额度排除自身（D5.1）
        val keys = recordDao.distinctDateKeys(space.localId).toHashSet()
        val streakN = StreakCalculator.streakEndingAt(keys, newDateKey)
        val remaining = (GPCalculator.DAILY_GP_LIMIT -
            recordDao.sumGpOnDateExcluding(space.localId, newDateKey, recordId)).coerceAtLeast(0)
        val breakdown = GPCalculator.calculate(
            textLength = draft.text.length,
            photoCount = draft.getPhotoUriList().size,
            hasVoice = draft.voiceUri != null,
            hasMusic = draft.musicTitle != null,
            hasLink = draft.linkUrl != null,
            hasLocation = draft.locationName != null,
            hasMood = true,
            statusTagCount = draft.getStatusTagList().size,
            streakDays = streakN,
            isBackdated = isBackdated,
            remainingQuota = remaining
        )
        recordDao.updateGp(recordId, breakdown.finalGp, breakdown.isCapped, breakdownToJson(breakdown), now)

        // 4) 级联重算：新旧两日额度、空间总分、阶段快照、成就
        recomputeDailyStat(space.localId, newDateKey)
        if (oldDateKey != newDateKey) recomputeDailyStat(space.localId, oldDateKey)
        recomputeSpaceTotal(space.localId, now)
        recordStageChangeIfAny(space.localId, prevTotal, recordDao.sumAllGp(space.localId), now, newDateKey)
        evaluator.evaluate(space.localId, scopeKey)
        appendOutbox(recordId, OutboxOps.UPSERT, now)

        val displayStreak = StreakCalculator.calculate(recordDao, space.localId)
        PublishResult(recordId, breakdown.finalGp, displayStreak, breakdown.isCapped)
    }

    /** 软删除并级联重算（ADR D6）：GP 回退、阶段可降级、成就可回锁，墓碑进 Outbox。 */
    suspend fun softDelete(localId: Long) = db.withTransaction {
        val raw = recordDao.getRawById(localId) ?: error("记录 $localId 不存在")
        val space = spaceDao.getDefaultSpace() ?: error("默认空间未初始化")
        val user = userDao.getFirstUser() ?: error("本地用户未初始化")
        val now = System.currentTimeMillis()
        val prevTotal = recordDao.sumAllGp(space.localId)

        recordDao.softDelete(localId, now)
        recomputeDailyStat(space.localId, raw.occurredDateKey)
        recomputeSpaceTotal(space.localId, now)
        // 删除属于用户主动移除贡献：阶段允许降级（ADR D6.3），写 STAGE_DOWN 快照
        recordStageChangeIfAny(space.localId, prevTotal, recordDao.sumAllGp(space.localId), now, raw.occurredDateKey)
        evaluator.evaluate(space.localId, "u:${user.localId}")
        appendOutbox(localId, OutboxOps.DELETE, now)
    }

    // ---- 内部：派生与映射 ----

    /** 补记窗口（ADR D4.1）：最早往前 365 天（按自然日），不允许未来时间。 */
    private fun requireWithinBackdateWindow(occurredAt: Long, now: Long) {
        val todayKey = DateKeys.epochDay(now)
        require(DateKeys.epochDay(occurredAt) >= todayKey - GPCalculator.BACKDATE_MAX_DAYS) {
            "补记最多往前 ${GPCalculator.BACKDATE_MAX_DAYS} 天"
        }
        require(occurredAt <= now) { "记录时间不能是未来（ADR D4.1）" }
    }

    private suspend fun writeChildren(draft: Record, recordId: Long, now: Long) {
        val media = mutableListOf<RecordMediaEntity>()

        draft.getPhotoUriList().forEachIndexed { index, uri ->
            val isAppFile = uri.startsWith("file:") || uri.startsWith("/")
            media += RecordMediaEntity(
                recordId = recordId, type = MediaTypes.PHOTO, sortOrder = index,
                localPath = if (isAppFile) uri else null,
                sourceUri = uri,
                mediaStatus = if (isAppFile) MediaStatus.READY else MediaStatus.PENDING_COPY,
                createdAt = now, updatedAt = now
            )
        }
        draft.voiceUri?.let { voice ->
            media += RecordMediaEntity(
                recordId = recordId, type = MediaTypes.VOICE, sortOrder = 0,
                localPath = voice, mediaStatus = MediaStatus.READY,
                durationMs = draft.voiceDuration, createdAt = now, updatedAt = now
            )
        }
        draft.musicTitle?.let { title ->
            media += RecordMediaEntity(
                recordId = recordId, type = MediaTypes.MUSIC, sortOrder = 0,
                title = title, subtitle = draft.musicArtist,
                extraJson = draft.musicUri?.let { "{\"uri\":${jsonEscape(it)}}" },
                mediaStatus = MediaStatus.READY, createdAt = now, updatedAt = now
            )
        }
        draft.linkUrl?.let { url ->
            val extra = draft.linkSummary?.let { "{\"summary\":${jsonEscape(it)}}" }
            media += RecordMediaEntity(
                recordId = recordId, type = MediaTypes.LINK, sortOrder = 0,
                sourceUri = url, title = draft.linkTitle, extraJson = extra,
                mediaStatus = MediaStatus.READY, createdAt = now, updatedAt = now
            )
        }
        draft.locationName?.let { name ->
            val extra = buildString {
                append('{')
                draft.locationLat?.let { append("\"lat\":$it,") }
                draft.locationLng?.let { append("\"lng\":$it") }
                append('}')
            }
            media += RecordMediaEntity(
                recordId = recordId, type = MediaTypes.LOCATION, sortOrder = 0,
                title = name, extraJson = extra, mediaStatus = MediaStatus.READY,
                createdAt = now, updatedAt = now
            )
        }
        if (media.isNotEmpty()) recordDao.insertMedia(media)

        val tagIds = draft.getStatusTagList()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { name ->
                val existing = tagDao.findTag(TagScopes.USER, 0, TagKinds.STATUS, name)
                    ?: tagDao.findTag(TagScopes.USER, 0, TagKinds.CUSTOM, name)
                if (existing != null) {
                    tagDao.incrementUseCount(existing.localId, now)
                    existing.localId
                } else {
                    tagDao.insert(
                        com.xiaoquexing.app.data.db.entity.TagEntity(
                            scope = TagScopes.USER, spaceId = 0, kind = TagKinds.CUSTOM,
                            name = name, createdAt = now, updatedAt = now
                        )
                    )
                }
            }
        if (tagIds.isNotEmpty()) {
            recordDao.insertCrossRefs(tagIds.map { RecordTagCrossRef(recordId = recordId, tagId = it) })
        }
    }

    private suspend fun recomputeDailyStat(spaceId: Long, dateKey: Int) {
        val recordCount = recordDao.countOnDate(spaceId, dateKey)
        if (recordCount == 0) {
            dailyStatDao.clearDay(spaceId, dateKey)
            return
        }
        dailyStatDao.upsert(
            com.xiaoquexing.app.data.db.entity.DailySpaceStatEntity(
                spaceId = spaceId,
                dateKey = dateKey,
                gpTotal = recordDao.sumGpOnDate(spaceId, dateKey),
                recordCount = recordCount,
                distinctAuthorCount = recordDao.countAuthorsOnDate(spaceId, dateKey)
            )
        )
    }

    private suspend fun recomputeSpaceTotal(spaceId: Long, now: Long) {
        spaceDao.setTotalGp(spaceId, recordDao.sumAllGp(spaceId), now)
    }

    private suspend fun recordStageChangeIfAny(
        spaceId: Long,
        beforeGp: Int,
        afterGp: Int,
        now: Long,
        dateKey: Int
    ) {
        val before = PlantStage.fromGp(beforeGp)
        val after = PlantStage.fromGp(afterGp)
        if (before == after) return
        val plantType = plantDao.getActiveSpacePlant(spaceId)?.plantType
            ?: com.xiaoquexing.app.data.entity.PlantType.TREE.name
        plantDao.insertSnapshot(
            PlantSnapshotEntity(
                spaceId = spaceId,
                plantType = plantType,
                eventType = if (after.ordinal > before.ordinal) PlantEventTypes.STAGE_UP else PlantEventTypes.STAGE_DOWN,
                stage = after.ordinal,
                gpAtEvent = afterGp,
                occurredAt = now,
                occurredDateKey = dateKey,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private suspend fun appendOutbox(recordId: Long, operation: String, now: Long) {
        outboxDao.insert(
            OutboxEventEntity(
                entityType = "RECORD",
                entityLocalId = recordId,
                operation = operation,
                createdAt = now
            )
        )
    }

    private fun breakdownToJson(b: GPBreakdown): String {
        val json = JSONObject()
        json.put("baseGp", b.baseGp)
        json.put("textBonus", b.textBonus)
        json.put("photoBonus", b.photoBonus)
        json.put("voiceBonus", b.voiceBonus)
        json.put("musicBonus", b.musicBonus)
        json.put("linkBonus", b.linkBonus)
        json.put("locationBonus", b.locationBonus)
        json.put("statusBonus", b.statusBonus)
        json.put("streakDays", b.streakDays)
        json.put("streakMultiplier", b.streakMultiplier.toDouble())
        json.put("isBackdated", b.isBackdated)
        json.put("rawTotal", b.rawTotal)
        json.put("finalGp", b.finalGp)
        json.put("isCapped", b.isCapped)
        return json.toString()
    }

    private fun toDomain(d: RecordWithDetails): Record {
        val photos = d.media.filter { it.type == MediaTypes.PHOTO }.sortedBy { it.sortOrder }
        val voice = d.media.firstOrNull { it.type == MediaTypes.VOICE }
        val music = d.media.firstOrNull { it.type == MediaTypes.MUSIC }
        val link = d.media.firstOrNull { it.type == MediaTypes.LINK }
        val location = d.media.firstOrNull { it.type == MediaTypes.LOCATION }
        val musicExtra = music?.extraJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val linkExtra = link?.extraJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val locationExtra = location?.extraJson?.let { runCatching { JSONObject(it) }.getOrNull() }

        return Record(
            id = d.record.localId,
            text = d.record.contentText ?: "",
            moodTag = d.record.moodTag,
            statusTags = d.tags.filter { it.kind != TagKinds.MOOD }.joinToString(",") { it.name },
            photoUris = photos.joinToString("|") { it.localPath ?: it.sourceUri ?: "" },
            voiceUri = voice?.localPath,
            voiceDuration = voice?.durationMs ?: 0,
            musicTitle = music?.title,
            musicArtist = music?.subtitle,
            musicUri = musicExtra?.opt("uri") as? String,
            linkUrl = link?.sourceUri,
            linkTitle = link?.title,
            linkSummary = linkExtra?.opt("summary") as? String,
            locationName = location?.title,
            locationLat = locationExtra?.let { if (it.has("lat")) it.optDouble("lat") else null },
            locationLng = locationExtra?.let { if (it.has("lng")) it.optDouble("lng") else null },
            gpEarned = d.record.gpFinal,
            createdAt = d.record.occurredAt,
            isBackdated = d.record.isBackdated
        )
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
