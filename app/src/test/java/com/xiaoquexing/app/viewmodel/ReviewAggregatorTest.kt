package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReviewAggregatorTest {
    @Test
    fun monthReview_countsDaysAndGp() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val records = listOf(
            Record(id = 1, text = "a", moodTag = "开心", gpEarned = 10, createdAt = cal.timeInMillis),
            Record(id = 2, text = "b", moodTag = "开心", gpEarned = 8, createdAt = cal.timeInMillis + 3600_000),
        )
        val month = ReviewAggregator.monthReview(records, 2026, 8)
        assertEquals(2, month.recordCount)
        assertEquals(18, month.totalGp)
        assertEquals(1, month.daysWritten)
        assertTrue(month.topMood.contains("开心"))
        assertEquals(31, month.heat.size)
    }

    @Test
    fun yearReview_hasTwelveMonths() {
        val year = ReviewAggregator.yearReview(emptyList(), 2026)
        assertEquals(12, year.months.size)
        assertEquals(0, year.totalGp)
    }
}
