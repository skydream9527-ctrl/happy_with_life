package com.xiaoquexing.app.data.remote

import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.SyncStates
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class SyncEngine(
    private val db: AppDatabase,
    private val api: ApiService,
    private val tokens: TokenStore,
) {
    suspend fun pushPending(): Int {
        val session = tokens.current() ?: return 0
        val spaceId = session.personalSpaceId.ifBlank { return 0 }
        bindLocalSpace(spaceId)
        val pending = db.recordDao().listPendingSync()
        val now = System.currentTimeMillis()
        var n = 0
        for (row in pending) {
            val date = Instant.ofEpochMilli(row.occurredAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            if (row.syncState == SyncStates.DELETE_PENDING && !row.serverId.isNullOrBlank()) {
                val env = api.deleteRecord(row.serverId)
                if (env.error == null) {
                    db.recordDao().markSynced(row.localId, row.serverId, row.gpFinal, row.version, now)
                    n++
                }
                continue
            }
            val write = RecordWrite(
                spaceId = spaceId,
                moodTag = row.moodTag,
                contentText = row.contentText.orEmpty(),
                timezone = ZoneId.systemDefault().id,
                occurredAt = Instant.ofEpochMilli(row.occurredAt).toString(),
                occurredDate = date,
                baseVersion = row.version.toLong(),
            )
            val env = if (row.serverId.isNullOrBlank()) {
                api.createRecord(UUID.randomUUID().toString(), write)
            } else {
                api.patchRecord(row.serverId, write)
            }
            val data = env.data
            if (data?.serverId != null) {
                val gp = data.authoritative?.gpFinal ?: row.gpFinal
                db.recordDao().markSynced(row.localId, data.serverId, gp, data.version.toInt(), now)
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
