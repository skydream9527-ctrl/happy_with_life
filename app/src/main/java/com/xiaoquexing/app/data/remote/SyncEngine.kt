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
            val write = RecordWrite(
                spaceId = spaceId,
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

    private suspend fun bindLocalSpace(serverSpaceId: String) {
        val local = db.spaceDao().getDefaultSpace() ?: return
        if (local.serverId != serverSpaceId) {
            db.spaceDao().bindServerId(local.localId, serverSpaceId, System.currentTimeMillis())
        }
    }
}
