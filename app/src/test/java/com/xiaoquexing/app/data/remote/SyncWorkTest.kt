package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkTest {
    @Test
    fun uniqueWorkNames_areStable() {
        assertEquals("xqx_sync_once", SyncWork.ONCE)
        assertEquals("xqx_sync_periodic", SyncWork.PERIODIC)
    }
}
