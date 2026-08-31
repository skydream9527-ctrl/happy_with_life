package com.xiaoquexing.app.data.remote

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

@Serializable
data class Anniversary(
    val id: Long,
    val title: String,
    val month: Int,
    val day: Int,
)

class AnniversaryStore(context: Context) {
    private val prefs = context.getSharedPreferences("xqx_anniversaries", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun list(): List<Anniversary> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching { json.decodeFromString<List<Anniversary>>(raw) }.getOrDefault(emptyList())
    }

    fun add(title: String, month: Int, day: Int): Anniversary? {
        if (!valid(month, day)) return null
        val name = title.trim().ifBlank { return null }
        val item = Anniversary(System.currentTimeMillis(), name.take(20), month, day)
        save(list() + item)
        return item
    }

    fun remove(id: Long) {
        save(list().filterNot { it.id == id })
    }

    fun today(now: Calendar = Calendar.getInstance()): List<Anniversary> =
        list().filter { it.month == now.get(Calendar.MONTH) + 1 && it.day == now.get(Calendar.DAY_OF_MONTH) }

    private fun save(items: List<Anniversary>) {
        prefs.edit().putString(KEY, json.encodeToString(items)).apply()
    }

    companion object {
        private const val KEY = "items"
        fun valid(month: Int, day: Int): Boolean {
            if (month !in 1..12 || day !in 1..31) return false
            val max = intArrayOf(0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[month]
            return day <= max
        }
    }
}
