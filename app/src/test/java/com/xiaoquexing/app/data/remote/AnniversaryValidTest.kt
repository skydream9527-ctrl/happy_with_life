package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnniversaryValidTest {
    @Test
    fun rejectsImpossibleDates() {
        assertFalse(AnniversaryStore.valid(0, 1))
        assertFalse(AnniversaryStore.valid(13, 1))
        assertFalse(AnniversaryStore.valid(2, 30))
        assertFalse(AnniversaryStore.valid(4, 31))
    }

    @Test
    fun acceptsCommonDates() {
        assertTrue(AnniversaryStore.valid(2, 29))
        assertTrue(AnniversaryStore.valid(5, 20))
        assertTrue(AnniversaryStore.valid(12, 31))
    }
}
