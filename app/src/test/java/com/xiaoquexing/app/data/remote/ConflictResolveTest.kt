package com.xiaoquexing.app.data.remote

import com.xiaoquexing.app.data.db.entity.SyncStates
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolveTest {
    @Test
    fun conflictState_isDistinctFromPending() {
        assertEquals(3, SyncStates.CONFLICT)
        assertEquals(1, SyncStates.SYNC_PENDING)
        assertEquals(0, SyncStates.SYNCED)
    }
}
