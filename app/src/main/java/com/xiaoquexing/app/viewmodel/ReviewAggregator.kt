package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.model.MoodTag
import java.util.Calendar

data class MonthReview(
    val year: Int,
    val month: Int,
    val title: String,
    val recordCount: Int,
    val totalGp: Int,
    val daysWritten: Int,
    val topMood: String,
    val heat: List<Int>,
)

data class YearReview(
    val year: Int,
    val recordCount: Int,
    val totalGp: Int,
    val months: List<Pair<String, Int>>,
    val topMood: String,
)

object ReviewAggregator {
    fun monthReview(records: List<Record>, year: Int, month: Int): MonthReview {
        val inMonth = records.filter { inMonth(it.createdAt, year, month) }
        val daysInMonth = daysInMonth(year, month)
        val byDay = IntArray(daysInMonth)
        inMonth.forEach { rec ->
            val day = dayOfMonth(rec.createdAt)
            if (day in 1..daysInMonth) byDay[day - 1] += 1
        }
        val max = byDay.maxOrNull()?.coerceAtLeast(1) ?: 1
        val heat = byDay.map { ((it * 3) / max).coerceIn(0, 3) }
        return MonthReview(
            year = year,
            month = month,
            title = "${year}年${month}月",
            recordCount = inMonth.size,
            totalGp = inMonth.sumOf { it.gpEarned },
            daysWritten = byDay.count { it > 0 },
            topMood = topMood(inMonth),
            heat = heat,
        )
    }

    fun yearReview(records: List<Record>, year: Int): YearReview {
        val inYear = records.filter { yearOf(it.createdAt) == year }
        val monthGp = (1..12).map { m ->
            val gp = inYear.filter { monthOf(it.createdAt) == m }.sumOf { rec -> rec.gpEarned }
            "${m}月" to gp
        }
        return YearReview(
            year = year,
            recordCount = inYear.size,
            totalGp = inYear.sumOf { it.gpEarned },
            months = monthGp,
            topMood = topMood(inYear),
        )
    }

    private fun topMood(records: List<Record>): String {
        val name = records.mapNotNull { it.moodTag }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        if (name == null) return "还没有心情"
        val emoji = MoodTag.fromName(name)?.emoji.orEmpty()
        return if (emoji.isBlank()) name else "$emoji $name"
    }

    private fun calendarOf(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }
    private fun yearOf(ms: Long) = calendarOf(ms).get(Calendar.YEAR)
    private fun monthOf(ms: Long) = calendarOf(ms).get(Calendar.MONTH) + 1
    private fun dayOfMonth(ms: Long) = calendarOf(ms).get(Calendar.DAY_OF_MONTH)
    private fun inMonth(ms: Long, year: Int, month: Int) =
        yearOf(ms) == year && monthOf(ms) == month

    private fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
