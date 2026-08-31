package com.xiaoquexing.app.data.remote

import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.SyncStates
import com.xiaoquexing.app.data.media.PhotoUploader
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class SyncReport(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val error: String? = null,
)

class SyncEngine(
    private val db: AppDatabase,
    private val api: ApiService,
    private val tokens: TokenStore,
    private val photos: PhotoUploader? = null,
) {
    @Volatile
    var lastReport: SyncReport = SyncReport()
        private set

    suspend fun syncAll(retries: Int = 3): SyncReport {
        var last = SyncReport()
        repeat(retries) { attempt ->
            last = runCatching { once() }.getOrElse {
                SyncReport(error = it.message)
            }
            lastReport = last
            if (last.error == null) return last
            if (attempt < retries - 1) {
                kotlinx.coroutines.delay(400L * (attempt + 1))
            }
        }
        return last
    }

    suspend fun pushPending(): Int = syncAll(retries = 1).pushed

    private suspend fun once(): SyncReport {
        val session = tokens.current() ?: return SyncReport()
        val spaceId = session.personalSpaceId.ifBlank { return SyncReport() }
        bindLocalSpace(spaceId)
        val pushed = push(spaceId)
        val pulled = pull()
        return SyncReport(pushed = pushed.first, pulled = pulled, conflicts = pushed.second)
    }

    private suspend fun push(spaceId: String): Pair<Int, Int> {
        val pending = db.recordDao().listPendingSync()
        val now = System.currentTimeMillis()
        var applied = 0
        var conflicts = 0
        for (row in pending) {
            val date = Instant.ofEpochMilli(row.occurredAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            if (row.syncState == SyncStates.DELETE_PENDING && !row.serverId.isNullOrBlank()) {
                val env = api.deleteRecord(row.serverId)
                if (env.error == null) {
                    db.recordDao().markSynced(row.localId, row.serverId, row.gpFinal, row.version, now)
                    applied++
                }
                continue
            }
            val media = runCatching { photos?.uploadForRecord(row.localId).orEmpty() }.getOrDefault(emptyList())
            val targetSpace = db.spaceDao().getById(row.spaceId)?.serverId?.takeIf { it.isNotBlank() } ?: spaceId
            val write = RecordWrite(
                spaceId = targetSpace,
                moodTag = row.moodTag,
                contentText = row.contentText.orEmpty(),
                timezone = ZoneId.systemDefault().id,
                occurredAt = Instant.ofEpochMilli(row.occurredAt).toString(),
                occurredDate = date,
                media = media,
                baseVersion = row.version.toLong(),
            )
            val env = if (row.serverId.isNullOrBlank()) {
                api.createRecord(UUID.randomUUID().toString(), write)
            } else {
                api.patchRecord(row.serverId, write)
            }
            val err = env.error
            if (err != null && (err.code == "CONFLICT" || err.code.contains("CONFLICT"))) {
                db.recordDao().setSyncState(row.localId, SyncStates.CONFLICT, now)
                conflicts++
                continue
            }
            val data = env.data
            if (data?.serverId != null) {
                val gp = data.authoritative?.gpFinal ?: row.gpFinal
                db.recordDao().markSynced(row.localId, data.serverId, gp, data.version.toInt(), now)
                applied++
            }
        }
        return applied to conflicts
    }

    private suspend fun pull(): Int {
        val env = api.syncPull(limit = 50)
        val changes = env.data?.changes.orEmpty()
        val now = System.currentTimeMillis()
        var n = 0
        for (change in changes) {
            if (change.entityType != "RECORD") continue
            val local = db.recordDao().findByServerId(change.serverId) ?: continue
            if (change.operation == "DELETE") {
                if (local.deletedAt == null) {
                    db.recordDao().softDelete(local.localId, now, SyncStates.SYNCED)
                    n++
                }
                continue
            }
            val remoteGp = change.payload?.gpFinal ?: local.gpFinal
            if (change.version >= local.version) {
                db.recordDao().markSynced(local.localId, change.serverId, remoteGp, change.version.toInt(), now)
                n++
            }
        }
        return n
    }

    fun observeConflicts() = db.recordDao().observeConflicts()
    fun observeConflictCount() = db.recordDao().observeConflictCount()

    suspend fun keepLocal(recordId: Long): SyncReport {
        val row = db.recordDao().getRawById(recordId) ?: return SyncReport(error = "记录不存在")
        val now = System.currentTimeMillis()
        val remoteVersion = row.serverId?.let { id ->
            runCatching { api.record(id).data?.version?.toInt() }.getOrNull()
        } ?: row.version
        db.recordDao().setVersionAndState(recordId, remoteVersion, SyncStates.SYNC_PENDING, now)
        return syncAll(retries = 2)
    }

    suspend fun keepCloud(recordId: Long): SyncReport {
        val row = db.recordDao().getRawById(recordId) ?: return SyncReport(error = "记录不存在")
        val serverId = row.serverId ?: return SyncReport(error = "没有云端版本")
        val remote = api.record(serverId).data ?: return SyncReport(error = "拉不到云端记录")
        db.recordDao().applyCloud(
            recordId = recordId,
            text = remote.contentText,
            mood = remote.moodTag,
            gpFinal = remote.gpFinal,
            version = remote.version.toInt(),
            now = System.currentTimeMillis(),
        )
        return SyncReport(pulled = 1)
    }

    private suspend fun bindLocalSpace(serverSpaceId: String) {
        val local = db.spaceDao().getDefaultSpace() ?: return
        if (local.serverId != serverSpaceId) {
            db.spaceDao().bindServerId(local.localId, serverSpaceId, System.currentTimeMillis())
        }
    }
}
